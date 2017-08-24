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
    tracelen: Int = 128,
    seed: Long = System.currentTimeMillis,
    N: Int = 5) extends TestSuiteCommon(platform, plsi) {
  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val target = "PointerChaser"
  val tp = config.Parameters.root((new PointerChaserConfig).toInstance)

  val script = new File("scripts", "generate_memory_init.py")
  val loadmem = new File("init.hex")
  if (!loadmem.exists) assert(Seq(script.toString, "--output_file", loadmem.getAbsolutePath).! == 0)

  def runTests(backend: String) {
    compile(backend, debug)
    behavior of s"$target in $backend"
    val dump = if (backend == "vcs") "vpd" else "vcd"
    val results = (1 to N) map (math.pow(2, _).toInt) map { latency =>
      val sample = Some(new File(outDir, s"$target-$backend-$latency.sample"))
      val logFile = Some(new File(outDir, s"$target-$backend-$latency.out"))
      val waveform = Some(new File(outDir, s"$target-$backend-$latency.$dump"))
      val args = Seq(s"+mm_MEM_LATENCY=$latency", "+fastloadmem", s"+tracelen=$tracelen")
      Future(latency -> run(backend, debug, sample, Some(loadmem), logFile, waveform, args))
    }
    Await.result(Future.sequence(results), Duration.Inf) foreach { case (latency, exitcode) =>
      if (isCmdAvailable(backend)) {
        it should s"pass latency: $latency" in { assert(exitcode == 0) }
      } else {
        ignore should s"pass latency: $latency" in { }
      }
    }
    if (isCmdAvailable(backend) && p(midas.EnableSnapshot)) {
      replayBackends foreach { replayBackend =>
        (1 to N) map (math.pow(2, _).toInt) foreach { latency =>
          if (isCmdAvailable("vcs")) {
            val sample = new File(outDir, s"$target-$backend-$latency.sample")
            val exitcode = runReplay(replayBackend, Some(sample))
            it should s"replay latency: $latency with $replayBackend" in { assert(exitcode == 0) }
          } else {
            ignore should s"replay latency: $latency with $replayBackend" in { }
          }
        }
      }
    }
  }
  clean
  midas.MidasCompiler(new PointerChaser(seed)(tp), genDir, if (plsi) Some(lib) else None)
  strober.replay.Compiler(new PointerChaser(seed)(tp), genDir, if (plsi) Some(lib) else None)
  compileReplay
  runTests("verilator")
  runTests("vcs")
  println(s"[SEED] ${seed}")
}

class PointerChaserZynqTests extends PointerChaserTestSuite(midas.Zynq)
// class PointerChaserCatapultTests extends PointerChaserTestSuite(midas.Catapult)
