package StroberExamples

import chisel3.Module
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import java.io.File

abstract class MiniTestSuite(backend: String, latency: Int = 8, N: Int = 10) extends TestSuiteCommon {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val tp = cde.Parameters.root((new mini.MiniConfig).toInstance)
  val target = compile(new mini.Tile(tp), backend)
  val vcd = if (backend == "verilator") "vcd" else "vpd"

  def runTests(testType: mini.TestType) {
    behavior of testType.toString
    val results = testType.tests.zipWithIndex sliding (N, N) map { subtests =>
      val subresults = subtests map { case (t, i) =>
        val loadmem = Some(new File("hex", s"$t.hex"))
        val logFile = Some(new File("results", s"$t-$backend.log"))
        val waveform = Some(new File("results", s"$t-$backend.$vcd"))
        Future(t -> run(target, backend, loadmem=loadmem, logFile=logFile, waveform=waveform))
      }
      Await.result(Future.sequence(subresults), Duration.Inf)
    }
    results.flatten foreach { case (name, exitcode) =>
      it should s"pass $name" in { assert(exitcode == 0) } }
  }
  runTests(mini.ISATests)
  runTests(mini.BmarkTests)
}

class MiniCppTests extends MiniTestSuite("verilator")
class MiniVCSTests extends MiniTestSuite("vcs")
