package StroberExamples

import strober._
import examples._

import chisel3.Module
import chisel3.iotesters._
import scala.reflect.ClassTag

trait TestSuiteCommon extends org.scalatest.FlatSpec {
  protected val outDir = new java.io.File("test-outs") ; outDir.mkdirs
  private def baseArgs(dir: java.io.File) = Array("--targetDir", dir.getPath.toString, 
    "--genHarness", "--compile", "--test", "--noUpdate")

  implicit val p = cde.Parameters.root((new ZynqConfig).toInstance)

  def simTest[T <: Module : ClassTag](c: => T)(
      tester: SimWrapper[T] => SimWrapperTester[T]) {
    test(SimWrapper(c), tester, implicitly[ClassTag[T]].runtimeClass.getSimpleName)
  }

  def zynqTest[T <: Module : ClassTag](c: => T)(
      tester: ZynqShim[SimWrapper[T]] => ZynqShimTester[SimWrapper[T]]) {
    test(ZynqShim(c), tester, implicitly[ClassTag[T]].runtimeClass.getSimpleName)
  }

  def test[T <: Module : ClassTag](dutGen: => T, tester: T => StroberTester[T], target: String) {
    val dir = new java.io.File(s"${outDir}/${target}")
    val circuit = StroberCompiler(baseArgs(dir), dutGen)
    s"${target} in ${circuit.name}" should "pass verilator" in {
      chiselMainTest(baseArgs(dir), () => dutGen)(tester)
    }
  }

  /* def replaySamples[T <: Module](c: T, sample: String, dump: String, log: String) = {
    assert((new Replay(c, new ReplayArgs(Sample.load(sample), Some(dump), Some(log)))).finish)
  } */
}

class TutorialSimTests extends TestSuiteCommon {
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

class TutorialZynqTests extends TestSuiteCommon {
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
