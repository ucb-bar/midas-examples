package strober
package examples

import chisel3.Module
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import java.io.File

abstract class MiniTestSuite(
    platform: midas.PlatformType,
    plsi: Boolean = false,
    debug: Boolean = false,
    latency: Int = 8,
    N: Int = 10) extends TestSuiteCommon(platform) {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val target = "Tile"
  val tp = cde.Parameters.root((new mini.MiniConfig).toInstance)

  def runTests(backend: String, testType: mini.TestType) {
    compile(backend, debug)
    val vcd = if (backend == "verilator") "vcd" else "vpd"
    behavior of s"${testType.toString} in $backend"
    val results = testType.tests sliding (N, N) map { subtests =>
      val subresults = subtests map { t =>
        val sample = Some(new File(outDir, s"$t.$backend.sample"))
        val loadmem = Some(new File("hex", s"$t.hex"))
        val logFile = Some(new File(outDir, s"$t.$backend.out"))
        val waveform = Some(new File(outDir, s"$t.$vcd"))
        val args = Seq(s"+latency=$latency")
        Future(t -> run(backend, debug, sample, loadmem, logFile, waveform, args))
      }
      Await.result(Future.sequence(subresults), Duration.Inf)
    }
    results.flatten foreach { case (name, exitcode) =>
      if (isCmdAvailable(backend)) {
        it should s"pass $name" in { assert(exitcode == 0) }
      } else {
        ignore should s"pass $name" in { }
      }
    }
    if (p(midas.EnableSnapshot)) {
      val replays = testType.tests sliding (N, N) map { subtests =>
        val subreplays = subtests map { t =>
          val sample = new File(outDir, s"$t.$backend.sample")
          Future(t -> runReplay("rtl", Some(sample)))
        }
        Await.result(Future.sequence(subreplays), Duration.Inf)
      }
      replays.flatten foreach { case (name, exitcode) =>
        if (isCmdAvailable("vcs")) {
          it should s"replay $name with rtl" in { assert(exitcode == 0) }
        } else {
          ignore should s"replay $name with rtl" in { }
        }
      }
    }
    if (p(midas.EnableSnapshot) && plsi) {
      val replays = testType.tests sliding (N, N) map { subtests =>
        val subreplays = subtests map { t =>
          val sample = new File(outDir, s"$t.$backend.sample")
          Future(t -> runReplay("syn", Some(sample)))
        }
        Await.result(Future.sequence(subreplays), Duration.Inf)
      }
      replays.flatten foreach { case (name, exitcode) =>
        if (isCmdAvailable("vcs")) {
          it should s"replay $name with syn" in { assert(exitcode == 0) }
        } else {
          ignore should s"replay $name with syn" in { }
        }
      }
    }
  }
  clean
  midas.MidasCompiler(new mini.Tile(tp), genDir)
  strober.replay.Compiler(new mini.Tile(tp), genDir)
  compileReplay("rtl" +: (if (plsi) Seq("syn") else Seq()))
  runTests("verilator", mini.SimpleTests)
  runTests("vcs", mini.SimpleTests)
  runTests("verilator", mini.ISATests)
  runTests("verilator", mini.BmarkTests)
  runTests("vcs", mini.ISATests)
  runTests("vcs", mini.BmarkTests)
}

class MiniZynqTests extends MiniTestSuite(midas.Zynq)
class MiniCatapultTests extends MiniTestSuite(midas.Catapult)
