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
    tracelen: Int = 128,
    N: Int = 10) extends TestSuiteCommon(platform, plsi) {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val target = "Tile"
  val tp = config.Parameters.root((new mini.MiniConfig).toInstance)

  def runTests(backend: String, testType: mini.TestType) {
    compile(backend, debug)
    behavior of s"${testType.toString} in $backend"
    val dump = if (backend == "verilator") "vcd" else "vpd"
    val results = testType.tests sliding (N, N) map { subtests =>
      val subresults = subtests map { t =>
        val loadmem = Some(new File("hex", s"$t.hex"))
        val sample = Some(new File(outDir, s"$t.$backend.sample"))
        val logFile = Some(new File(outDir, s"$t.$backend.out"))
        val waveform = Some(new File(outDir, s"$t.$backend.$dump"))
        val args = Seq(s"+mm_MEM_LATENCY=$latency", s"+tracelen=$tracelen")
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
    if (isCmdAvailable(backend) && p(midas.EnableSnapshot)) {
      replayBackends foreach { replayBackend =>
        testType.tests foreach { name =>
          if (isCmdAvailable("vcs")) {
            val sample = new File(outDir, s"$name.$backend.sample")
            val exitcode = runReplay(replayBackend, Some(sample))
            it should s"replay $name with $replayBackend" in { assert(exitcode == 0) }
          } else {
            ignore should s"replay $name with $replayBackend" in { }
          }
        }
      }
    }
  }
  clean
  midas.MidasCompiler(new mini.Tile(tp), genDir, if (plsi) Some(lib) else None)
  strober.replay.Compiler(new mini.Tile(tp), genDir, if (plsi) Some(lib) else None)
  compileReplay
  runTests("verilator", mini.SimpleTests)
  runTests("vcs", mini.SimpleTests)
  runTests("verilator", mini.ISATests)
  runTests("verilator", mini.BmarkTests)
  runTests("vcs", mini.ISATests)
  runTests("vcs", mini.BmarkTests)
}

class MiniZynqTests extends MiniTestSuite(midas.Zynq, true)
