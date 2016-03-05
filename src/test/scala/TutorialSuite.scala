package StroberExamples

import Chisel._
import strober._
import TutorialExamples._
import org.scalatest.fixture.{FlatSpec, ConfigMapFixture}
import org.scalatest.BeforeAndAfter
import cde.Parameters

trait TestSuiteCommon {
  protected val outDir = new java.io.File("test-outs")
  protected val logDir = new java.io.File("test-logs")
  protected val dumpDir = new java.io.File("test-dumps")
  private val baseArgs = Array(
    "--targetDir", outDir.getPath.toString,
    /* "--minimumCompatibility", "3.0", */"--genHarness",
    "--compile", "--compileInitializationUnoptimized")
  private def testArgs(b: String) = baseArgs ++ Array("--backend", b)
  private def debugArgs(b: String) = testArgs(b) ++ Array("--debug", "--vcd")

  def elaborate[T <: Module](c: => T, b: String, debug: Boolean=false): T = {
    val args = if (debug) debugArgs(b) else testArgs(b)
    chiselMain(args, () => c)
  }

  def elaborateCpp[T <: Module](c: => T, debug: Boolean=false) = elaborate(c, "c", debug)

  def elaborateVerilog[T <: Module](c: => T, debug: Boolean=false) = elaborate(c, "v", debug)

  def replaySamples[T <: Module](c: T, sample: String, dump: String, log: String) = {
    assert((new Replay(c, new ReplayArgs(Sample.load(sample), Some(dump), Some(log)))).finish)
  }

  if (!logDir.exists) logDir.mkdir
  if (!dumpDir.exists) dumpDir.mkdir
}

class TutorialSuite extends FlatSpec with ConfigMapFixture with BeforeAndAfter with TestSuiteCommon {
  def checkStrober[T <: Module](c: T, args: StroberTestArgs) {
    assert(c match {
      case w: SimWrapper[_] => w.target match {
        case _: GCD => (new GCDSimTests(
          w.asInstanceOf[SimWrapper[GCD]], args)).finish
        case _: Parity => (new ParitySimTests(
          w.asInstanceOf[SimWrapper[Parity]], args)).finish
        case _: ShiftRegister => (new ShiftRegisterSimTests(
          w.asInstanceOf[SimWrapper[ShiftRegister]], args)).finish
        case _: ResetShiftRegister => (new ResetShiftRegisterSimTests(
          w.asInstanceOf[SimWrapper[ResetShiftRegister]], args)).finish
        case _: EnableShiftRegister => (new EnableShiftRegisterSimTests(
          w.asInstanceOf[SimWrapper[EnableShiftRegister]], args)).finish
        case _: MemorySearch => (new MemorySearchSimTests(
          w.asInstanceOf[SimWrapper[MemorySearch]], args)).finish
        case _: Stack => (new StackSimTests(
          w.asInstanceOf[SimWrapper[Stack]], args)).finish
        case _: Router => (new RouterSimTests(
          w.asInstanceOf[SimWrapper[Router]], args)).finish
        case _: Risc => (new RiscSimTests(
          w.asInstanceOf[SimWrapper[Risc]], args)).finish
        case _: RiscSRAM => (new RiscSRAMSimTests(
          w.asInstanceOf[SimWrapper[RiscSRAM]], args)).finish
      }
      case w: NastiShim[_] => w.sim.asInstanceOf[SimWrapper[_]].target match {
        case _: GCD => (new GCDNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[GCD]]], args)).finish
        case _: Parity => (new ParityNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[Parity]]], args)).finish
        case _: ShiftRegister => (new ShiftRegisterNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[ShiftRegister]]], args)).finish
        case _: ResetShiftRegister => (new ResetShiftRegisterNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[ResetShiftRegister]]], args)).finish
        case _: EnableShiftRegister => (new EnableShiftRegisterNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[EnableShiftRegister]]], args)).finish
        case _: MemorySearch => (new MemorySearchNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[MemorySearch]]], args)).finish
        case _: Stack => (new StackNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[Stack]]], args)).finish
        case _: Router => (new RouterNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[Router]]], args)).finish
        case _: Risc => (new RiscNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[Risc]]], args)).finish
        case _: RiscSRAM => (new RiscSRAMNastiShimTests(
          w.asInstanceOf[NastiShim[SimWrapper[RiscSRAM]]], args)).finish
      }
    })
  }

  def runTests[M <: Module](c: => M) {
    val name = c.getClass.getSimpleName
    val sample = s"${outDir.getPath}/${name}.sample"
    behavior of s"${name}"
    it should "pass Strober Tests" in { configMap =>
      val b = configMap("b").asInstanceOf[String]
      val w = configMap("w").asInstanceOf[String]
      val log = s"${logDir.getPath}/${name}-${w}-${b}.log"
      val dump = b match {
        case "c" => s"${dumpDir.getPath}/${name}-${w}.vcd"
        case "v" => s"${dumpDir.getPath}/${name}-${w}.vpd"
      }
      implicit val p = Parameters.root((w match {
        case "sim"   => new SimConfig
        case "nasti" => new NastiConfig}).toInstance)
      checkStrober(elaborate(w match {
        case "sim"   => SimWrapper(c)
        case "nasti" => NastiShim(c)
      }, b, true), new StroberTestArgs(Some(sample), Some(dump), Some(log)))
    }
    it should s"replay samples from Strober" in { configMap =>
      val b = configMap("b").asInstanceOf[String]
      val w = configMap("w").asInstanceOf[String]
      replaySamples(elaborateCpp(Module(c), true), sample,
        s"${dumpDir.getPath}/${name}-${w}-${b}-replay.vcd",
        s"${logDir.getPath}/${name}-${w}-${b}-replay.log")
      replaySamples(elaborateVerilog(Module(c), true), sample, 
        s"${dumpDir.getPath}/${name}-${w}-${b}-replay.vpd",
        s"${logDir.getPath}/${name}-${w}-${b}-replay.log")
    }
  }

  before { 
    Sample.clear
    transforms.clear
  }
  runTests(new GCD)
  runTests(new Parity)
  runTests(new ShiftRegister)
  runTests(new ResetShiftRegister)
  runTests(new EnableShiftRegister)
  runTests(new MemorySearch)
  runTests(new Stack(8))
  // runTests(new Router)
  runTests(new Risc)
  // runTests(new RiscSRAM)
}
