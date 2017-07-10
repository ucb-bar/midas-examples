package strober
package examples

import chisel3.Module
import scala.reflect.ClassTag
import scala.sys.process.{stringSeqToProcess, ProcessLogger}
import java.io.File
import midas.{Zynq, Catapult}

abstract class TestSuiteCommon(
    platform: midas.PlatformType,
    plsi: Boolean) extends org.scalatest.FlatSpec {
  def target: String
  val platformName = platform.toString.toLowerCase
  val replayBackends = "rtl" +: (if (plsi) Seq("syn") else Seq())
  lazy val genDir = new File(new File(new File("generated-src"), platformName), target)
  lazy val outDir = new File(new File(new File("output"), platformName), target)

  implicit def toStr(f: File): String = f.toString replace (File.separator, "/")

  implicit val p = config.Parameters.root((platform match {
    case Zynq     => new midas.ZynqConfigWithSnapshot
    case Catapult => new midas.CatapultConfigWithSnapshot
  }).toInstance)

  def clean {
    assert(Seq("make", s"$target-clean", s"PLATFORM=$platformName").! == 0)
  }

  def isCmdAvailable(cmd: String) =
    Seq("which", cmd) ! ProcessLogger(_ => {}) == 0

  def compile(b: String, debug: Boolean = false) {
    if (isCmdAvailable(b)) {
      assert(Seq("make", s"$target-$b",
                s"PLATFORM=$platformName",
                 "DEBUG=%s".format(if (debug) "1" else "")).! == 0)
    }
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
      "SAMPLE=%s".format(sample map toStr getOrElse ""),
      "LOADMEM=%s".format(loadmem map toStr getOrElse ""),
      "LOGFILE=%s".format(logFile map toStr getOrElse ""),
      "WAVEFORM=%s".format(waveform map toStr getOrElse ""),
      "ARGS=%s".format(args mkString " "))
    if (isCmdAvailable(backend)) {
      println("cmd: %s".format(cmd mkString " "))
      cmd.!
    } else 0
  }

  def compileReplay {
    if (p(midas.EnableSnapshot) && isCmdAvailable("vcs")) {
      replayBackends foreach (b =>
        assert(Seq("make", s"$target-vcs-$b", s"PLATFORM=$platformName").! == 0))
    }
  }

  def runReplay(b: String, sample: Option[File] = None) = {
    if (isCmdAvailable("vcs")) {
      Seq("make", s"$target-replay-$b", s"PLATFORM=$platformName",
        "SAMPLE=%s".format(sample map (_.toString) getOrElse "")).!
    } else 0
  }
}

abstract class TutorialSuite[T <: Module : ClassTag](
    dutGen: => T,
    platform: midas.PlatformType,
    plsi: Boolean = false) extends TestSuiteCommon(platform, plsi) {
  val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  def runTest(b: String) {
    behavior of s"$target in $b"
    compile(b, true)
    val sample = Some(new File(outDir, s"$target.$b.sample"))
    if (isCmdAvailable(b)) {
      it should s"pass strober test" in { assert(run(b, true, sample) == 0) }
      if (p(midas.EnableSnapshot)) {
        replayBackends foreach { replayBackend =>
          if (isCmdAvailable("vcs")) {
            it should s"replay samples with $replayBackend" in {
              assert(runReplay(replayBackend, sample) == 0)
            }
          } else {
            ignore should s"replay samples with $replayBackend" in { }
          }
        }
      }
    } else {
      ignore should s"pass strober test" in { }
    }
  }
  clean
  midas.MidasCompiler(dutGen, genDir)
  strober.replay.Compiler(dutGen, genDir)
  compileReplay
  runTest("verilator")
  runTest("vcs")
}

class GCDZynqTest extends TutorialSuite(new GCD, Zynq, true)
class ParityZynqTest extends TutorialSuite(new Parity, Zynq)
class ShiftRegisterZynqTest extends TutorialSuite(new ShiftRegister, Zynq)
class ResetShiftRegisterZynqTest extends TutorialSuite(new ResetShiftRegister, Zynq)
class EnableShiftRegisterZynqTest extends TutorialSuite(new EnableShiftRegister, Zynq)
class StackZynqTest extends TutorialSuite(new Stack, Zynq, true)
class RiscZynqTest extends TutorialSuite(new Risc, Zynq)
class RiscSRAMZynqTest extends TutorialSuite(new RiscSRAM, Zynq, true)

class GCDCatapultTest extends TutorialSuite(new GCD, Catapult)
class ParityCatapultTest extends TutorialSuite(new Parity, Catapult)
class ShiftRegisterCatapultTest extends TutorialSuite(new ShiftRegister, Catapult)
class ResetShiftRegisterCatapultTest extends TutorialSuite(new ResetShiftRegister, Catapult)
class EnableShiftRegisterCatapultTest extends TutorialSuite(new EnableShiftRegister, Catapult)
class StackCatapultTest extends TutorialSuite(new Stack, Catapult)
class RiscCatapultTest extends TutorialSuite(new Risc, Catapult)
class RiscSRAMCatapultTest extends TutorialSuite(new RiscSRAM, Catapult)
