package StroberExamples

import examples._
import chisel3.Module
import scala.reflect.ClassTag
import scala.sys.process.stringSeqToProcess
import java.io.File

abstract class TestSuiteCommon extends org.scalatest.FlatSpec {
  val srcDir = new File("strober/src/main/cc")
  val testDir = new File("strober-test")
  val testGenDir = new File(testDir, "generated-src") ; testGenDir.mkdirs
  val testOutDir = new File(testDir, "outputs"); testOutDir.mkdirs
  val replayDir = new File("strober-replay")
  val replayGenDir = new File(replayDir, "generated-src") ; replayGenDir.mkdirs
  val replayOutDir = new File(replayDir, "outputs"); replayOutDir.mkdirs

  // implicit val p = cde.Parameters.root((new midas.ZynqConfig).toInstance)
  // implicit val p = cde.Parameters.root((new ZynqConfigWithMemModel).toInstance)
  implicit val p = cde.Parameters.root((new midas.ZynqConfigWithSnapshot).toInstance)
  // implicit val p = cde.Parameters.root((new ZynqConfigWithMemModelAndSnapshot).toInstance)

  def compile[T <: Module : ClassTag](dut: => T, b: String, debug: Boolean = false) = {
    val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
    val genDir = new File(testGenDir, target)
    val binary = new File(testOutDir, s"%s${target}%s".format(
      if (b == "verilator") "V" else "", if (debug) "-debug" else ""))
    val cmd = Seq("make", "-C", testDir.toString, binary.getAbsolutePath,
                  "DEBUG=%s".format(if (debug) "1" else ""))
    midas.MidasCompiler(dut, genDir)
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
    val cmd = Seq("make", "-C", testDir.toString, s"$target-$backend",
      "DEBUG=%s".format(if (debug) "1" else ""),
      "LOADMEM=%s".format(loadmem map (_.toString) getOrElse ""),
      "LOGFILE=%s".format(logFile map (_.toString) getOrElse ""),
      "WAVEFORM=%s".format(waveform map (_.toString) getOrElse ""),
      "ARGS=%s".format(args mkString " "))
    println("cmd: %s".format(cmd mkString " "))
    cmd.!
  }

  def compileReplay[T <: Module : ClassTag](dutGen: => T, b: String) = {
    val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
    if (p(midas.EnableSnapshot)) {
      val binary = new File(replayOutDir, s"%s$target-replay".format(if (b == "verilator") "V" else ""))
      val cmd = Seq("make", "-C", replayDir.toString, binary.getAbsolutePath)
      strober.replay.Compiler(dutGen, replayGenDir)
      assert(cmd.! == 0)
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
    val sample = new File(replayOutDir, s"$target-$b.sample")
    behavior of s"$target in $b"
    it should s"pass strober test" in {
      assert(run(target, b, true, args=Seq(s"+sample=${sample.getAbsolutePath}")) == 0)
    }
    if (p(midas.EnableSnapshot)) {
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
class RiscTests extends TutorialSuite(new Risc)
class RiscSRAMTests extends TutorialSuite(new RiscSRAM)
