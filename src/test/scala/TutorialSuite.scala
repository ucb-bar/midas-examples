package strober
package examples

import chisel3.Module
import scala.reflect.ClassTag
import scala.sys.process.stringSeqToProcess
import java.io.File
import midas.{Zynq, Catapult}

abstract class TestSuiteCommon(platform: midas.PlatformType) extends org.scalatest.FlatSpec {
  def target: String
  val platformName = platform.toString.toLowerCase
  lazy val genDir = new File(new File(new File("generated-src"), platformName), target)
  lazy val outDir = new File(new File(new File("output"), platformName), target)

  implicit val p = cde.Parameters.root((platform match {
    case Zynq     => new midas.ZynqConfigWithSnapshot
    case Catapult => new midas.CatapultConfigWithSnapshot
  }).toInstance)
  // implicit val p = cde.Parameters.root((new ZynqConfigWithMemModelAndSnapshot).toInstance)

  def clean {
    assert(Seq("make", s"$target-clean").! == 0)
  }

  def compile(b: String, debug: Boolean = false) {
    assert(Seq("make", s"$target-$b",
              s"PLATFORM=$platformName",
               "DEBUG=%s".format(if (debug) "1" else "")).! == 0)
  }

  def run(backend: String,
          debug: Boolean = false,
          sample: Option[File] = None,
          loadmem: Option[File] = None,
          logFile: Option[File] = None,
          waveform: Option[File] = None,
          args: Seq[String] = Nil) = {
    val cmd = Seq("make", s"$target-$backend-test",
      s"PLATFORM=$platformName",
      "DEBUG=%s".format(if (debug) "1" else ""),
      "SAMPLE=%s".format(sample map (_.toString) getOrElse ""),
      "LOADMEM=%s".format(loadmem map (_.toString) getOrElse ""),
      "LOGFILE=%s".format(logFile map (_.toString) getOrElse ""),
      "WAVEFORM=%s".format(waveform map (_.toString) getOrElse ""),
      "ARGS=%s".format(args mkString " "))
    println("cmd: %s".format(cmd mkString " "))
    cmd.!
  }

  def compileReplay(dutGen: => Module, b: String) {
    if (p(midas.EnableSnapshot)) {
      strober.replay.Compiler(dutGen, genDir)
      assert(Seq("make", s"$target-$b-replay-compile", s"PLATFORM=$platformName").! == 0)
    }
  }

  def replay(backend: String, sample: Option[File] = None) = {
    Seq("make", s"${target}-${backend}-replay", s"PLATFORM=$platformName",
      "SAMPLE=%s".format(sample map (_.toString) getOrElse "")).!
  }
}

abstract class TutorialSuite[T <: Module : ClassTag](
    dutGen: => T,
    platform: midas.PlatformType) extends TestSuiteCommon(platform) {
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

class GCDZynqTest extends TutorialSuite(new GCD, Zynq)
class ParityZynqTest extends TutorialSuite(new Parity, Zynq)
class ShiftRegisterZynqTest extends TutorialSuite(new ShiftRegister, Zynq)
class ResetShiftRegisterZynqTest extends TutorialSuite(new ResetShiftRegister, Zynq)
class EnableShiftRegisterZynqTest extends TutorialSuite(new EnableShiftRegister, Zynq)
class StackZynqTest extends TutorialSuite(new Stack, Zynq)
class RiscZynqTest extends TutorialSuite(new Risc, Zynq)
class RiscSRAMZynqTest extends TutorialSuite(new RiscSRAM, Zynq)

class GCDCatapultTest extends TutorialSuite(new GCD, Catapult)
class ParityCatapultTest extends TutorialSuite(new Parity, Catapult)
class ShiftRegisterCatapultTest extends TutorialSuite(new ShiftRegister, Catapult)
class ResetShiftRegisterCatapultTest extends TutorialSuite(new ResetShiftRegister, Catapult)
class EnableShiftRegisterCatapultTest extends TutorialSuite(new EnableShiftRegister, Catapult)
class StackCatapultTest extends TutorialSuite(new Stack, Catapult)
class RiscCatapultTest extends TutorialSuite(new Risc, Catapult)
class RiscSRAMCatapultTest extends TutorialSuite(new RiscSRAM, Catapult)
