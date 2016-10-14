package StroberExamples

import strober.{StroberCompiler, ZynqShim, EnableSnapshot}
import examples._
import chisel3.Module
import scala.reflect.ClassTag
import scala.sys.process.stringSeqToProcess
import java.io.File

abstract class TestSuiteCommon extends org.scalatest.FlatSpec {
  val testDir = new File("strober-test")
  val genDir = new File(testDir, "generated-src") ; genDir.mkdirs
  val resDir = new File(testDir, "results") ; resDir.mkdirs
  val replayDir = new File("strober-replay")

  implicit val p = cde.Parameters.root((new ZynqConfig).toInstance)

  def compile[T <: Module : ClassTag](dut: => T, backend: String, debug: Boolean = false) = {
    val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
    val compArgs = Array("--targetDir", (new File(genDir, target)).toString)
    val binary = new File(resDir, s"%s$target%s".format(
      if (backend == "vcs") "" else "V", if (debug) "-debug" else ""))
    val cmd = Seq("make", "-C", testDir.toString, binary.getAbsolutePath) ++
      (if (debug) Seq("DEBUG=1") else Nil)
    StroberCompiler compile (compArgs, ZynqShim(dut))
    assert(cmd.! == 0)
    target
  }

  def run(target: String,
          backend: String,
          debug: Boolean = false,
          loadmem: Option[File] = None,
          logFile: Option[File] = None,
          waveform: Option[File] = None,
          args: Seq[String] = Nil) = {
    val cmd = Seq("make", "-C", testDir.toString, s"$target-$backend") ++
      (if (debug) Seq("DEBUG=1") else Nil) ++
      (loadmem match { case None => Nil case Some(p) => Seq(s"LOADMEM=$p") }) ++
      (logFile match { case None => Nil case Some(p) => Seq(s"LOGFILE=$p") }) ++
      (waveform match { case None => Nil case Some(p) => Seq(s"WAVEFORM=$p") }) ++
      Seq("ARGS=%s".format(args mkString " "))
    println("cmd: %s".format(cmd mkString " "))
    cmd.!
  }

  def compileReplay[T <: Module : ClassTag](dutGen: => T, b: String) = {
    val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
    if (p(EnableSnapshot)) {
      strober.replay.Compiler(dutGen, new File(replayDir, "generated-src"))
      val resDir = new File(replayDir, "results") ; resDir.mkdir
      val binary = new File(resDir, s"%s$target".format(if (b == "vcs") "" else "V"))
      assert(Seq("make", "-C", replayDir.toString, binary.getAbsolutePath).! == 0)
    }
    target
  }

  def replay(target: String, backend: String, sample: Option[File] = None) = {
    Seq("make", "-C", replayDir.toString, s"${target}-replay-${backend}",
      sample match { case None => "" case Some(p) => s"SAMPLE=${p.getAbsolutePath}" }).!
  }
}

abstract class TutorialSuite[T <: Module : ClassTag](dutGen: => T) extends TestSuiteCommon {
  def runTest(b: String) {
    val target = compile(dutGen, b, true)
    val sample = new File(resDir, s"$target-$b.sample")
    behavior of s"$target in $b"
    it should s"pass strober test" in {
      assert(run(target, b, true, args=Seq(s"+sample=${sample.getAbsolutePath}")) == 0)
    }
    if (p(EnableSnapshot)) {
      it should "replay samples in vcs" in {
        assert(replay(target, "vcs", Some(sample)) == 0)
      }
    }
  }
  compileReplay(dutGen, "vcs")
  runTest("verilator")
  runTest("vcs")
}

class GCDTests extends TutorialSuite(new GCD)
class ParityTests extends TutorialSuite(new Parity)
class ShiftRegisterTests extends TutorialSuite(new ShiftRegister)
class ResetShiftRegisterTests extends TutorialSuite(new ResetShiftRegister)
class EnableShiftRegisterTests extends TutorialSuite(new EnableShiftRegister)
class StackTests extends TutorialSuite(new Stack(8))
class RouterTests extends TutorialSuite(new Router)
class RiscTests extends TutorialSuite(new Risc)
class RiscSRAMTests extends TutorialSuite(new RiscSRAM)
