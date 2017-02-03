package strober
package examples

import chisel3.Module
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import scala.sys.process.stringSeqToProcess
import java.io.File

abstract class PointerChaserTestSuite(
    platform: midas.PlatformType,
    plsi: Boolean = false,
    debug: Boolean = true,
    seed: Long = System.currentTimeMillis,
    N: Int = 5) extends TestSuiteCommon(platform) {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val target = "PointerChaser"
  val tp = cde.Parameters.root((new PointerChaserConfig).toInstance)

  val script = new File("scripts", "generate_memory_init.py")
  val loadmem = new File("init.hex")
  if (!loadmem.exists) assert(Seq(script.toString, "--output_file", loadmem.getAbsolutePath).! == 0)

  def runTests(backend: String) {
    compile(backend, debug)
    val vcd = if (backend == "vcs") "vpd" else "vcd"
    behavior of s"$target in $backend"
    val results = (1 to N) map (math.pow(2, _).toInt) map { latency =>
      val sample = Some(new File(outDir, s"$target.$latency.$backend.sample"))
      val logFile = Some(new File(outDir, s"$target.$latency.$backend.out"))
      val waveform = Some(new File(outDir, s"$target.$latency.$vcd"))
      val args = Seq(s"+latency=$latency", "+fastloadmem")
      Future(latency -> run(backend, debug, sample, Some(loadmem), logFile, waveform, args))
    }
    Await.result(Future.sequence(results), Duration.Inf) foreach { case (latency, exitcode) =>
      if (isCmdAvailable(backend)) {
        it should s"pass latency: $latency" in { assert(exitcode == 0) }
      } else {
        ignore should s"pass latency: $latency" in { }
      }
    }
    if (p(midas.EnableSnapshot)) {
      val replays = (1 to N) map (math.pow(2, _).toInt) map { latency =>
        val sample = new File(outDir, s"$target.$latency.$backend.sample")
        Future(latency -> runReplay("rtl", Some(sample)))
      }
      Await.result(Future.sequence(replays), Duration.Inf) foreach { case (latency, exitcode) =>
        if (isCmdAvailable("vcs")) {
          it should s"replay latency: $latency with rtl" in { assert(exitcode == 0) }
        } else {
          ignore should s"replay latency: $latency with rtl" in { }
        }
      }
    }
    if (p(midas.EnableSnapshot) && plsi) {
      val replays = (1 to N) map (math.pow(2, _).toInt) map { latency =>
        val sample = new File(outDir, s"$target.$latency.$backend.sample")
        Future(latency -> runReplay("syn", Some(sample)))
      }
      Await.result(Future.sequence(replays), Duration.Inf) foreach { case (latency, exitcode) =>
        if (isCmdAvailable("vcs")) {
          it should s"replay latency: $latency with syn" in { assert(exitcode == 0) }
        } else {
          ignore should s"replay latency: $latency with syn" in { }
        }
      }
    }
  }
  clean
  midas.MidasCompiler(new PointerChaser(seed)(tp), genDir)
  strober.replay.Compiler(new PointerChaser(seed)(tp), genDir)
  compileReplay("rtl" +: (if (plsi) Seq("syn") else Seq()))
  runTests("verilator")
  runTests("vcs")
  println(s"[SEED] ${seed}")
}

class PointerChaserZynqTests extends PointerChaserTestSuite(midas.Zynq, true)
class PointerChaserCatapultTests extends PointerChaserTestSuite(midas.Catapult)
