package StroberExamples

import chisel3.Module
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import scala.sys.process.stringSeqToProcess
import java.io.File

abstract class PointerChaserTestSuite(
    seed: Long = System.currentTimeMillis,
    N: Int = 5) extends TestSuiteCommon {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val script = new File("scripts", "generate_memory_init.py")
  val loadmem = new File(testDir, "init.hex")
  if (!loadmem.exists) assert(Seq(script.toString, "--output_file", loadmem.toString).! == 0)

  val tp = cde.Parameters.root((new PointerChaserConfig).toInstance)

  def runTests(backend: String, debug: Boolean = false) {
    val target = compile(new PointerChaser(seed)(tp), backend, debug)
    val vcd = if (backend == "vcs") "vpd" else "vcd"
    behavior of s"$target in $backend"
    val results = (1 to N) map (math.pow(2, _).toInt) map { latency =>
      val sample = new File(replayGenDir, s"$target-$latency-$backend.sample")
      val logFile = Some(new File("outputs", s"$target-$backend-$latency.log"))
      val waveform = Some(new File("outputs", s"$target-${latency}.${vcd}"))
      val args = Seq(s"+latency=$latency", s"+sample=${sample.getAbsolutePath}", "+fastloadmem")
      Future(latency ->
        run(target, backend, debug, Some(new File("init.hex")), logFile, waveform, args)
      )
    }
    Await.result(Future.sequence(results), Duration.Inf) foreach { case (latency, exitcode) =>
      it should s"pass latency: $latency" in { assert(exitcode == 0) }
    }
    if (p(strober.EnableSnapshot)) {
      val replays = (1 to N) map (math.pow(2, _).toInt) map { latency =>
        val sample = new File(replayGenDir, s"$target-$latency-$backend.sample")
        Future(latency -> replay(target, "vcs", Some(sample)))
      }
      Await.result(Future.sequence(replays), Duration.Inf) foreach { case (latency, exitcode) =>
        it should s"replay latency: $latency in vcs" in { assert(exitcode == 0) }
      }
    }
  }
  compileReplay(new PointerChaser(seed)(tp), "vcs")
  runTests("verilator")
  runTests("vcs")
  println(s"[SEED] ${seed}")
}
class PointerChaserTests extends PointerChaserTestSuite()
