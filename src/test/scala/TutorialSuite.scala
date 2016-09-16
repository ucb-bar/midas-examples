package StroberExamples

import strober.{SimWrapper, ZynqShim, StroberCompiler}
import strober.testers.{StroberTester, SimWrapperTester, ZynqShimTester}
import examples._
import chisel3.Module
import chisel3.iotesters.{Driver, chiselMainTest}

import scala.reflect.ClassTag
import java.io.File

abstract class TestSuiteCommon extends org.scalatest.FlatSpec {
  protected val outDir = new File("test-outs") ; outDir.mkdirs
  private def baseArgs(dir: File) = Array("--targetDir", dir.getPath.toString)
  private def testArgs(dir: File, backend: String) = baseArgs(dir) ++
    Array("--backend", backend, "--v", "--compile", "--genHarness", "--test", "--vpdmem")

  def test[T <: Module : ClassTag](
      dutGen: => T,
      tester: T => StroberTester[T],
      dir: File,
      backend: String) {
    it should s"pass strober test" in {
      StroberCompiler(baseArgs(dir), dutGen, backend)(tester)
    }
  }

  def replaySamples[T <: Module](dutGen: => T, dir: File, sample: File, backend: String) {
    val log = new File(dir, s"replay-$backend.log")
    it should s"replay samples in $backend" in {
      chiselMainTest(testArgs(dir, backend), () => dutGen)(c => backend match {
        case "glsim" =>
          new strober.testers.GateLevelReplay(c, sample, Some(log))
        case _ =>
          new strober.testers.RTLReplay(c, sample, Some(log))
      })
    }
  }
}

abstract class SimTestSuite[+T <: Module : ClassTag](
    c: => T,
    backend: String)
   (tester: SimWrapper[T] => SimWrapperTester[T]) extends TestSuiteCommon {
  implicit val p = cde.Parameters.root((new SimConfig).toInstance)
  val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  val dir = new File(outDir, s"SimWrapper/$target/$backend") ; dir.mkdirs
  val sample = new File(dir, s"$target.sample")
  behavior of s"[SimWrapper] $target in $backend"
  test(SimWrapper(c), tester, dir, backend)
  replaySamples(c, dir, sample, "verilator")
  replaySamples(c, dir, sample, "vcs")
  replaySamples(c, dir, sample, "glsim")
}

abstract class ZynqTestSuite[+T <: Module : ClassTag](
    c: => T,
    backend: String)
   (tester: ZynqShim[SimWrapper[T]] => ZynqShimTester[SimWrapper[T]]) extends TestSuiteCommon {
  implicit val p = cde.Parameters.root((new ZynqConfig).toInstance)
  val target = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  val dir = new File(outDir, s"ZynqShim/$target/$backend") ; dir.mkdirs
  val sample = new File(dir, s"$target.sample")
  behavior of s"[ZynqShim] $target in $backend"
  test(ZynqShim(c), tester, dir, backend)
  replaySamples(c, dir, sample, "verilator")
  replaySamples(c, dir, sample, "vcs")
  replaySamples(c, dir, sample, "glsim")
}

class GCDSimCppTest extends SimTestSuite(new GCD, "verilator")(c => new GCDSimTests(c))
class GCDSimVCSTest extends SimTestSuite(new GCD, "vcs")(c => new GCDSimTests(c))
class ParitySimCppTest extends SimTestSuite(new Parity, "verilator")(c => new ParitySimTests(c))
class ParitySimVCSTest extends  SimTestSuite(new Parity, "vcs")(c => new ParitySimTests(c))
class ShiftRegisterSimCppTest extends SimTestSuite(new ShiftRegister, "verilator")(c => new ShiftRegisterSimTests(c))
class ShiftRegisterSimVCSTest extends SimTestSuite(new ShiftRegister, "vcs")(c => new ShiftRegisterSimTests(c))
class ResetShiftRegisterSimCppTest extends SimTestSuite(new ResetShiftRegister, "verilator")(c => new ResetShiftRegisterSimTests(c))
class ResetShiftRegisterSimVCSTest extends SimTestSuite(new ResetShiftRegister, "vcs")(c => new ResetShiftRegisterSimTests(c))
class EnableShiftRegisterSimCppTest extends SimTestSuite(new EnableShiftRegister, "verilator")(c => new EnableShiftRegisterSimTests(c))
class EnableShiftRegisterSimVCSTest extends SimTestSuite(new EnableShiftRegister, "vcs")(c => new EnableShiftRegisterSimTests(c))
class StackSimCppTest extends SimTestSuite(new Stack(8), "verilator")(c => new StackSimTests(c))
class StackSimVCSTest extends SimTestSuite(new Stack(8), "vcs")(c => new StackSimTests(c))
class RouterSimCppTest extends SimTestSuite(new Router, "verilator")(c => new RouterSimTests(c))
class RouterSimVCSTest extends SimTestSuite(new Router, "vcs")(c => new RouterSimTests(c))
class RiscSimCppTest extends SimTestSuite(new Risc, "verilator")(c => new RiscSimTests(c))
class RiscSimVCSTest extends SimTestSuite(new Risc, "vcs")(c => new RiscSimTests(c))
class RiscSRAMSimCppTest extends SimTestSuite(new RiscSRAM, "verilator")(c => new RiscSRAMSimTests(c))
class RiscSRAMSimVCSTest extends SimTestSuite(new RiscSRAM, "vcs")(c => new RiscSRAMSimTests(c))

class GCDZynqCppTest extends ZynqTestSuite(new GCD, "verilator")(c => new GCDZynqTests(c))
class GCDZynqVCSTest extends ZynqTestSuite(new GCD, "vcs")(c => new GCDZynqTests(c))
class ParityZynqCppTest extends ZynqTestSuite(new Parity, "verilator")(c => new ParityZynqTests(c))
class ParityZynqVCSTest extends ZynqTestSuite(new Parity, "vcs")(c => new ParityZynqTests(c))
class ShiftRegisterZynqCppTest extends ZynqTestSuite(new ShiftRegister, "verilator")(c => new ShiftRegisterZynqTests(c))
class ShiftRegisterZynqVCSTest extends ZynqTestSuite(new ShiftRegister, "vcs")(c => new ShiftRegisterZynqTests(c))
class ResetShiftRegisterZynqCppTest extends ZynqTestSuite(new ResetShiftRegister, "verilator")(c => new ResetShiftRegisterZynqTests(c))
class ResetShiftRegisterZynqVCSTest extends ZynqTestSuite(new ResetShiftRegister, "vcs")(c => new ResetShiftRegisterZynqTests(c))
class EnableShiftRegisterZynqCppTest extends ZynqTestSuite(new EnableShiftRegister, "verilator")(c => new EnableShiftRegisterZynqTests(c))
class EnableShiftRegisterZynqVCSTest extends ZynqTestSuite(new EnableShiftRegister, "vcs")(c => new EnableShiftRegisterZynqTests(c))
class StackZynqCppTest extends ZynqTestSuite(new Stack(8), "verilator")(c => new StackZynqTests(c))
class StackZynqVCSTest extends ZynqTestSuite(new Stack(8), "vcs")(c => new StackZynqTests(c))
class RouterZynqCppTest extends ZynqTestSuite(new Router, "verilator")(c => new RouterZynqTests(c))
class RouterZynqVCSTest extends ZynqTestSuite(new Router, "vcs")(c => new RouterZynqTests(c))
class RiscZynqCppTest extends ZynqTestSuite(new Risc, "verilator")(c => new RiscZynqTests(c))
class RiscZynqVCSTest extends ZynqTestSuite(new Risc, "vcs")(c => new RiscZynqTests(c))
class RiscSRAMZynqCppTest extends ZynqTestSuite(new RiscSRAM, "verilator")(c => new RiscSRAMZynqTests(c))
class RiscSRAMZynqVCSTest extends ZynqTestSuite(new RiscSRAM, "vcs")(c => new RiscSRAMZynqTests(c))
