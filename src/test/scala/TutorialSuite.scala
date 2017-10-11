package strober
package examples

import chisel3.Module
import scala.reflect.ClassTag
import scala.sys.process.{stringSeqToProcess, ProcessLogger}
import java.io.File

abstract class TestSuiteCommon(
    platform: midas.PlatformType,
    plsi: Boolean) extends org.scalatest.FlatSpec {
  def target: String
  val platformName = platform.toString.toLowerCase
  val replayBackends = "rtl" +: (if (plsi) Seq("syn"/*, TODO: "par"*/) else Seq())
  lazy val genDir = new File(new File(new File("generated-src"), platformName), target)
  lazy val outDir = new File(new File(new File("output"), platformName), target)
  val lib = new File("plsi/obj/technology/saed32/plsi-generated/all.macro_library.json")
  if (plsi) assert(Seq("make", "-f", "replay.mk", lib.getAbsolutePath, "MACRO_LIB=1").! == 0)

  implicit def toStr(f: File): String = f.toString replace (File.separator, "/")

  implicit val p = config.Parameters.root((platform match {
    case midas.Zynq => new midas.ZynqConfigWithSnapshot
  }).toInstance)

  def clean {
    assert(Seq("make", s"$target-clean", s"PLATFORM=$platformName").! == 0)
  }

  def isCmdAvailable(cmd: String) =
    Seq("which", cmd) ! ProcessLogger(_ => {}) == 0

  def compile(b: String, debug: Boolean = false) {
    if (isCmdAvailable(b)) {
      assert(Seq("make",
                 s"$target-$b%s".format(if (debug) "-debug" else ""),
                 s"PLATFORM=$platformName").! == 0)
    }
  }

  def run(backend: String,
          debug: Boolean = false,
          sample: Option[File] = None,
          loadmem: Option[File] = None,
          logFile: Option[File] = None,
          waveform: Option[File] = None,
          args: Seq[String] = Nil) = {
    val cmd = Seq("make",
      s"$target-$backend-test%s".format(if (debug) "-debug" else ""),
      s"PLATFORM=$platformName",
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
    tracelen: Int = 8,
    plsi: Boolean = false) extends TestSuiteCommon(platform, plsi) {
  val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  val args = Seq(s"+tracelen=$tracelen")
  def runTest(b: String) {
    behavior of s"$target in $b"
    compile(b, true)
    val sample = Some(new File(outDir, s"$target.$b.sample"))
    if (isCmdAvailable(b)) {
      it should s"pass strober test" in {
        assert(run(b, true, sample, args=args) == 0)
      }
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
  midas.MidasCompiler(dutGen, genDir, if (plsi) Some(lib) else None)
  strober.replay.Compiler(dutGen, genDir, if (plsi) Some(lib) else None)
  compileReplay
  runTest("verilator")
  runTest("vcs")
}

class GCDZynqTest extends TutorialSuite(new GCD, midas.Zynq, 3, true)
class ParityZynqTest extends TutorialSuite(new Parity, midas.Zynq)
class ShiftRegisterZynqTest extends TutorialSuite(new ShiftRegister, midas.Zynq)
class ResetShiftRegisterZynqTest extends TutorialSuite(new ResetShiftRegister, midas.Zynq)
class EnableShiftRegisterZynqTest extends TutorialSuite(new EnableShiftRegister, midas.Zynq)
class StackZynqTest extends TutorialSuite(new Stack, midas.Zynq, 8, true)
class RiscZynqTest extends TutorialSuite(new Risc, midas.Zynq, 64)
class RiscSRAMZynqTest extends TutorialSuite(new RiscSRAM, midas.Zynq, 64, true)
