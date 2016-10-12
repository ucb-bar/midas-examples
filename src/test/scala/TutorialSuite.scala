package StroberExamples

import strober.{StroberCompiler, ZynqShim, ReplayCompiler, EnableSnapshot}
import examples._
import chisel3.Module
import scala.reflect.ClassTag
import scala.sys.process.stringSeqToProcess
import java.io.File

abstract class TestSuiteCommon extends org.scalatest.FlatSpec {
  val testDir = new File("strober-test")
  val genDir = new File(testDir, "generated-src") ; genDir.mkdirs
  val resDir = new File(testDir, "results") ; resDir.mkdirs

  implicit val p = cde.Parameters.root((new ZynqConfig).toInstance)

  def compile[T <: Module : ClassTag](dut: => T, backend: String) = {
    val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
    val compArgs = Array("--targetDir", (new File(genDir, target)).toString)
    StroberCompiler compile (compArgs, ZynqShim(dut))
    assert(Seq("make", "-C", testDir.toString, (new File(resDir,
      s"%s$target".format(if (backend == "vcs") "" else "V")).getAbsolutePath)).! == 0)
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
      Seq("ARGS=\"%s\"".format(args mkString " "))
    println("cmd: %s".format(cmd mkString " "))
    cmd.!
  }
}

abstract class TutorialSuite[T <: Module : ClassTag](
    dutGen: => T, backend: String) extends TestSuiteCommon {
  it should "pass strober test" in {
    assert(run(compile(dutGen, backend), backend, true) == 0)
  }
}

class GCDCppTest extends TutorialSuite(new GCD, "verilator")
class GCDVCSTest extends TutorialSuite(new GCD, "vcs")
class ParityCppTest extends TutorialSuite(new Parity, "verilator")
class ParityVCSTest extends  TutorialSuite(new Parity, "vcs")
class ShiftRegisterCppTest extends TutorialSuite(new ShiftRegister, "verilator")
class ShiftRegisterVCSTest extends TutorialSuite(new ShiftRegister, "vcs")
class ResetShiftRegisterCppTest extends TutorialSuite(new ResetShiftRegister, "verilator")
class ResetShiftRegisterVCSTest extends TutorialSuite(new ResetShiftRegister, "vcs")
class EnableShiftRegisterCppTest extends TutorialSuite(new EnableShiftRegister, "verilator")
class EnableShiftRegisterVCSTest extends TutorialSuite(new EnableShiftRegister, "vcs")
class StackCppTest extends TutorialSuite(new Stack(8), "verilator")
class StackVCSTest extends TutorialSuite(new Stack(8), "vcs")
class RouterCppTest extends TutorialSuite(new Router, "verilator")
class RouterVCSTest extends TutorialSuite(new Router, "vcs")
class RiscCppTest extends TutorialSuite(new Risc, "verilator")
class RiscVCSTest extends TutorialSuite(new Risc, "vcs")
class RiscSRAMCppTest extends TutorialSuite(new RiscSRAM, "verilator")
class RiscSRAMVCSTest extends TutorialSuite(new RiscSRAM, "vcs")
