package StroberExamples

import chisel3.Module
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import java.io.File

abstract class MiniTestSuite(latency: Int = 8, N: Int = 10) extends TestSuiteCommon {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val tp = cde.Parameters.root((new mini.MiniConfig).toInstance)

  def runTests(backend: String, testType: mini.TestType, debug: Boolean = false) {
    val target = compile(new mini.Tile(tp), backend, debug)
    val vcd = if (backend == "verilator") "vcd" else "vpd"
    behavior of s"${testType.toString} in $backend"
    val results = testType.tests sliding (N, N) map { subtests =>
      val subresults = subtests map { t =>
        val sample = new File(replayOutDir, s"$t-$backend.sample")
        val loadmem = Some(new File("hex", s"$t.hex"))
        val logFile = Some(new File("outputs", s"$t-$backend.log"))
        val waveform = Some(new File("outputs", s"$t-$backend.$vcd"))
        val args = Seq(s"+latency=$latency", s"+sample=${sample.getAbsolutePath}")
        Future(t -> run(target, backend, debug, loadmem, logFile, waveform, args))
      }
      Await.result(Future.sequence(subresults), Duration.Inf)
    }
    results.flatten foreach { case (name, exitcode) =>
      it should s"pass $name" in { assert(exitcode == 0) } }
    if (p(midas.EnableSnapshot)) {
      val replays = testType.tests sliding (N, N) map { subtests =>
        val subreplays = subtests map { t =>
          val sample = new File(replayOutDir, s"$t-$backend.sample")
          Future(t -> replay(target, "vcs", Some(sample)))
        }
        Await.result(Future.sequence(subreplays), Duration.Inf)
      }
      replays.flatten foreach { case (name, exitcode) =>
        it should s"replay $name in vcs" in { assert(exitcode == 0) } }
    }
  }
  compileReplay(new mini.Tile(tp), "vcs")
  runTests("verilator", mini.SimpleTests, true)
  /* runTests("verilator", mini.ISATests)
  runTests("verilator", mini.BmarkTests)
  runTests("vcs", mini.ISATests)
  runTests("vcs", mini.BmarkTests) */
}
class MiniTests extends MiniTestSuite()
