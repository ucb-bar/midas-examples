package StroberExamples

import examples._
import chisel3.Module
import scala.reflect.ClassTag
import scala.sys.process.stringSeqToProcess
import java.io.File

abstract class TestSuiteCommon extends org.scalatest.FlatSpec {
  def target: String
  lazy val genDir = new File(new File("generated-src"), target)
  lazy val outDir = new File(new File("output"), target)

  // implicit val p = cde.Parameters.root((new midas.ZynqConfig).toInstance)
  // implicit val p = cde.Parameters.root((new ZynqConfigWithMemModel).toInstance)
  implicit val p = cde.Parameters.root((new midas.ZynqConfigWithSnapshot).toInstance)
  // implicit val p = cde.Parameters.root((new ZynqConfigWithMemModelAndSnapshot).toInstance)

  def clean {
    assert(Seq("make", s"$target-clean").! == 0)
  }

  def compile(b: String, debug: Boolean = false) {
    assert(Seq("make", s"$target-$b", "DEBUG=%s".format(if (debug) "1" else "")).! == 0)
  }

  def run(backend: String,
          debug: Boolean = false,
          sample: Option[File] = None,
          loadmem: Option[File] = None,
          logFile: Option[File] = None,
          waveform: Option[File] = None,
          args: Seq[String] = Nil) = {
    val cmd = Seq("make", s"$target-$backend-test",
      "DEBUG=%s".format(if (debug) "1" else ""),
      "SAMPLE=%s".format(sample map (_.toString) getOrElse ""),
      "LOADMEM=%s".format(loadmem map (_.toString) getOrElse ""),
      "LOGFILE=%s".format(logFile map (_.toString) getOrElse ""),
      "WAVEFORM=%s".format(waveform map (_.toString) getOrElse ""),
      "ARGS=\"%s\"".format(args mkString " "))
    println("cmd: %s".format(cmd mkString " "))
    cmd.!
  }

  def compileReplay(dutGen: => Module, b: String) {
    if (p(midas.EnableSnapshot)) {
      strober.replay.Compiler(dutGen, genDir)
      assert(Seq("make", s"$target-$b-replay-compile").! == 0)
    }
  }

  def replay(backend: String, sample: Option[File] = None) = {
    Seq("make", s"${target}-${backend}-replay",
      "SAMPLE=%s".format(sample map (_.toString) getOrElse "")).!
  }
}

abstract class TutorialSuite[T <: Module : ClassTag](dutGen: => T) extends TestSuiteCommon {
  val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  def runTest(b: String) {
    compile(b, true)
    val sample = Some(new File(outDir, s"$target.$b.sample"))
    behavior of s"$target in $b"
    it should s"pass strober test" in { assert(run(b, true, sample) == 0) }
    if (p(midas.EnableSnapshot)) {
      it should "replay samples in vcs" in { assert(replay("vcs", sample) == 0) }
    }
  }
  clean
  midas.MidasCompiler(dutGen, genDir)
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
