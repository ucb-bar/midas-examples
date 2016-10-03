package StroberExamples

import chisel3.Module
import mini._
import cde.Parameters.root
import strober.{SimWrapper, ZynqShim, StroberCompiler, ReplayCompiler, EnableSnapshot}
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import java.io.File
import TestParams.{miniParam, simParam, zynqParam}

object TestParams {
  val chaserParam = root((new PointerChaserConfig).toInstance)
  val miniParam = root((new MiniConfig).toInstance)
  val simParam = root((new SimConfig).toInstance)
  val zynqParam = root((new ZynqConfig).toInstance)
}

abstract class MiniTestSuite[+T <: Module : ClassTag](
    dutGen: => T,
    backend: String,
    testType: TestType,
    latency: Int = 8,
    N: Int = 10) extends org.scalatest.FlatSpec {
  val dutName = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  val dir = new File(s"test-outs/Tile/$dutName-$backend/$testType") ; dir.mkdirs
  val args = Array("--targetDir", dir.toString)
  val dut = StroberCompiler compile (args, dutGen, backend)
  val vcd = if (backend == "verilator") "vcd" else "vpd"
  behavior of s"$dutName in $backend"

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global
  val results = testType.tests.zipWithIndex sliding (N, N) map { subtests =>
    val subresults = subtests map { case (t, i) =>
      val loadmem = getClass.getResourceAsStream(s"/mini/$t.hex")
      val logFile = Some(new File(dir, s"$t-$backend.log"))
      val waveform = Some(new File(dir, s"$t-$backend.$vcd"))
      val sampleFile = Some(new File(dir, s"$t-$backend.sample"))
      val testArgs = new MiniTestArgs(loadmem, logFile, false, testType.maxcycles, latency)
      Future(t -> (dut match {
        case _: SimWrapper[_] =>
          (StroberCompiler test (
            args,
            dutGen.asInstanceOf[SimWrapper[Tile]],
            backend,
            waveform)
          )(m => new TileSimTests(m, testArgs, sampleFile))
        case _: ZynqShim[_] =>
          (StroberCompiler test(
            args,
            dutGen.asInstanceOf[ZynqShim[SimWrapper[Tile]]],
            backend,
            waveform)
          )(m => new TileZynqTests(m, testArgs, sampleFile))
      }))
    }
    Await.result(Future.sequence(subresults), Duration.Inf)
  }
  results.flatten foreach { case (name, pass) => it should s"pass $name" in { assert(pass) } }

  def replaySample(b: String) {
    val vcd = if (b == "verilator") "vcd" else "vpd"
    val target = ReplayCompiler compile (args, new Tile(miniParam), backend)
    val results = testType.tests.zipWithIndex sliding (N, N) map { subtests =>
      val subresults = subtests map { case (t, i) =>
        val sampleFile = new File(dir, s"$t-$backend.sample")
        val logFile = Some(new File(dir, s"$t-$backend-replay-$b.log"))
        val waveform = Some(new File(dir, s"$t-$backend-replay-$b.$vcd"))
        Future(t -> ReplayCompiler.test(args, new Tile(miniParam), b, waveform)(c => b match {
          case "glsim" =>
            new strober.testers.GateLevelReplay(c, sampleFile, logFile)
          case _ =>
            new strober.testers.RTLReplay(c, sampleFile, logFile)
        }))
      }
      Await.result(Future.sequence(subresults), Duration.Inf)
    }
    results.flatten foreach { case (name, pass) => it should s"replay $name in $b" in { assert(pass) } }
  }

  Seq(/*"verilator",*/ "vcs"/*, "glsim"*/) foreach replaySample
}

class MiniSimCppSimpleTests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "verilator", SimpleTests)
class MiniSimCppISATests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "verilator", ISATests)
class MiniSimCppBmarkTests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "verilator", BmarkTests)
class MiniSimVCSSimpleTests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "vcs", SimpleTests)
class MiniSimVCSISATests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "vcs", ISATests)
class MiniSimVCSBmarkTests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "vcs", BmarkTests)
class MiniZynqCppSimpleTests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "verilator", SimpleTests)
class MiniZynqCppISATests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "verilator", ISATests)
class MiniZynqCppBmarkTests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "verilator", BmarkTests)
class MiniZynqVCSSimpleTests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "vcs", SimpleTests)
class MiniZynqVCSISATests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "vcs", ISATests)
class MiniZynqVCSBMarkTests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "vcs", BmarkTests)
