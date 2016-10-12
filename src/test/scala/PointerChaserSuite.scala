package StroberExamples

import chisel3.Module
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import scala.sys.process.stringSeqToProcess
import java.io.File

abstract class PointerChaserTestSuite(backend: String, N: Int = 10) extends TestSuiteCommon {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val script = new File("scripts", "generate_memory_init.py")
  val loadmem = new File(testDir, "init.hex")
  if (!loadmem.exists) assert(Seq(script.toString, "--output_file", loadmem.toString).! == 0)

  val tp = cde.Parameters.root((new PointerChaserConfig).toInstance)
  val target = compile(new PointerChaser()(tp), backend)
  val vcd = if (backend == "vcs") "vpd" else "vcd"

  val results = (1 to 5) map (math.pow(2, _).toInt) map { latency =>
    val logFile = Some(new File("results", s"PointerChaser-$backend-$latency.log"))
    val waveform = Some(new File("results", s"PointerChaser-${latency}.${vcd}"))
    val args = Seq(s"+latency=$latency")
    Future(latency -> run(
      target,
      backend,
      loadmem=Some(new File("init.hex")),
      logFile=logFile,
      waveform=waveform,
      args=args)
    )
  }
  Await.result(Future.sequence(results), Duration.Inf) foreach { case (latency, exitcode) =>
    it should s"pass latency $latency" in { assert(exitcode == 0) }
  }
}

class PointerChaserCppTests extends PointerChaserTestSuite("verilator")
class PointerChaserVCSTests extends PointerChaserTestSuite("vcs")
