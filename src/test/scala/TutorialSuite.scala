package StroberExamples

import strober._
import examples._

import chisel3.Module
import chisel3.iotesters._
import scala.reflect.ClassTag

trait TestSuiteCommon extends org.scalatest.FlatSpec {
  protected val outDir = new java.io.File("test-outs") ; outDir.mkdirs
  private def baseArgs(dir: java.io.File, vcs: Boolean) =
    Array("--targetDir", dir.getPath.toString, "--genHarness", "--compile",
          "--test", "--noUpdate") ++ (if (vcs) Array("--vcs") else Nil)

  implicit val p = cde.Parameters.root((new ZynqConfig).toInstance)

  def simTest[T <: Module : ClassTag](c: => T, vcs: Boolean = false)(
      tester: SimWrapper[T] => SimWrapperTester[T]) {
    test(SimWrapper(c), tester, implicitly[ClassTag[T]].runtimeClass.getSimpleName, vcs)
  }

  def zynqTest[T <: Module : ClassTag](c: => T, vcs: Boolean = false)(
      tester: ZynqShim[SimWrapper[T]] => ZynqShimTester[SimWrapper[T]]) {
    test(ZynqShim(c), tester, implicitly[ClassTag[T]].runtimeClass.getSimpleName, vcs)
  }

  def test[T <: Module : ClassTag](dutGen: => T, tester: T => StroberTester[T], target: String, vcs: Boolean) {
    val dir = new java.io.File(s"${outDir}/${target}")
    val sim = if (vcs) "vcs" else "verilator"
    val circuit = StroberCompiler(baseArgs(dir, vcs), dutGen)
    s"${target} in ${circuit.name}" should s"pass $sim" in {
      chiselMainTest(baseArgs(dir, vcs), () => dutGen)(tester)
    }
  }

  /* def replaySamples[T <: Module](c: T, sample: String, dump: String, log: String) = {
    assert((new Replay(c, new ReplayArgs(Sample.load(sample), Some(dump), Some(log)))).finish)
  } */
}

class TutorialSimVeriTests extends TestSuiteCommon {
  simTest(new GCD)(c => new GCDSimTests(c))
  simTest(new Parity)(c => new ParitySimTests(c))
  simTest(new ShiftRegister)(c => new ShiftRegisterSimTests(c))
  simTest(new ResetShiftRegister)(c => new ResetShiftRegisterSimTests(c))
  simTest(new EnableShiftRegister)(c => new EnableShiftRegisterSimTests(c))
  simTest(new Stack(8))(c => new StackSimTests(c))
  simTest(new Router)(c => new RouterSimTests(c))
  simTest(new Risc)(c => new RiscSimTests(c))
  simTest(new RiscSRAM)(c => new RiscSRAMSimTests(c))
}

class TutorialSimVCSTests extends TestSuiteCommon {
  simTest(new GCD, true)(c => new GCDSimTests(c))
  simTest(new Parity, true)(c => new ParitySimTests(c))
  simTest(new ShiftRegister, true)(c => new ShiftRegisterSimTests(c))
  simTest(new ResetShiftRegister, true)(c => new ResetShiftRegisterSimTests(c))
  simTest(new EnableShiftRegister, true)(c => new EnableShiftRegisterSimTests(c))
  simTest(new Stack(8), true)(c => new StackSimTests(c))
  simTest(new Router, true)(c => new RouterSimTests(c))
  simTest(new Risc, true)(c => new RiscSimTests(c))
  simTest(new RiscSRAM, true)(c => new RiscSRAMSimTests(c))
}


class TutorialZynqVeriTests extends TestSuiteCommon {
  zynqTest(new GCD)(c => new GCDZynqTests(c))
  zynqTest(new Parity)(c => new ParityZynqTests(c))
  zynqTest(new ShiftRegister)(c => new ShiftRegisterZynqTests(c))
  zynqTest(new ResetShiftRegister)(c => new ResetShiftRegisterZynqTests(c))
  zynqTest(new EnableShiftRegister)(c => new EnableShiftRegisterZynqTests(c))
  zynqTest(new Stack(8))(c => new StackZynqTests(c))
  zynqTest(new Router)(c => new RouterZynqTests(c))
  zynqTest(new Risc)(c => new RiscZynqTests(c))
  zynqTest(new RiscSRAM)(c => new RiscSRAMZynqTests(c))
}

class TutorialZynqVCSTests extends TestSuiteCommon {
  zynqTest(new GCD, true)(c => new GCDZynqTests(c))
  zynqTest(new Parity, true)(c => new ParityZynqTests(c))
  zynqTest(new ShiftRegister, true)(c => new ShiftRegisterZynqTests(c))
  zynqTest(new ResetShiftRegister, true)(c => new ResetShiftRegisterZynqTests(c))
  zynqTest(new EnableShiftRegister, true)(c => new EnableShiftRegisterZynqTests(c))
  zynqTest(new Stack(8), true)(c => new StackZynqTests(c))
  zynqTest(new Router, true)(c => new RouterZynqTests(c))
  zynqTest(new Risc, true)(c => new RiscZynqTests(c))
  zynqTest(new RiscSRAM, true)(c => new RiscSRAMZynqTests(c))
}
