package StroberExamples

import chisel3.Module
import chisel3.iotesters.Driver
import mini._
import strober.{SimWrapper, ZynqShim, StroberCompiler}
import sys.process.stringSeqToProcess
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import java.io.File
import TestParams.{miniParam, simParam, zynqParam}

object TestParams {
  val miniParam = cde.Parameters.root((new MiniConfig).toInstance)
  val simParam = cde.Parameters.root((new SimConfig).toInstance)
  val zynqParam = cde.Parameters.root((new ZynqConfig).toInstance)
}

abstract class MiniTestSuite[+T <: Module : ClassTag](
    dutGen: => T,
    backend: String,
    testType: TestType,
    latency: Int = 8,
    snapshot: Boolean = false,
    N: Int = 10) extends org.scalatest.FlatSpec {
  val dutName = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  val dir = new File(s"test-outs/$dutName/Tile/$testType") ; dir.mkdirs
  val args = Array("--targetDir", dir.toString)
  val dut = StroberCompiler compile (args, dutGen, backend, snapshot)
  val vcs = backend == "vcs"
  behavior of s"$dutName in $backend"

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global
  val results = testType.tests.zipWithIndex sliding (N, N) map { subtests =>
    val subresults = subtests map { case (t, i) =>
      val loadmem = getClass.getResourceAsStream(s"/mini/$t.hex")
      val logFile = Some(new File(dir, s"$t-$backend.log"))
      val waveform = Some(new File(dir, s"$t.%s".format(if (vcs) "vpd" else "vcd")))
      val testArgs = new MiniTestArgs(loadmem, logFile, false, testType.maxcycles, latency)
      Future(t -> (dut match {
        case _: SimWrapper[_] =>
          (StroberCompiler test (
            args,
            dutGen.asInstanceOf[SimWrapper[Tile]],
            backend,
            waveform,
            snapshot)
          )(m => new TileSimTests(m, testArgs))
        case _: ZynqShim[_] =>
          (StroberCompiler test(
            args,
            dutGen.asInstanceOf[ZynqShim[SimWrapper[Tile]]],
            backend,
            waveform,
            snapshot)
          )(m => new TileZynqTests(m, testArgs))
      }))
    }
    Await.result(Future.sequence(subresults), Duration.Inf)
  }
  results.flatten foreach { case (name, pass) => it should s"pass $name" in { assert(pass) } }

  /*
  def replaySamples(t: TestType) {
    property("riscv-mini should replay the following samples") { configMap =>
      implicit val p = cde.Parameters.root((new MiniConfig).toInstance)
      val b = configMap("b").asInstanceOf[String]
      val simulator = b match {
        case "c" => "Chisel Emulator" 
        case "v" => "Verilog Simulator"
      }
      val (dir, tests, maxcycles) = t match {
        case ISATests   => (new File("riscv-mini/riscv-tests/isa"), isaTests, 15000L)
        case BmarkTests => (new File("riscv-mini/riscv-bmarks"), bmarkTests, 500000L)
      }
      Given(s"${t} on ${simulator}")
      val dut = elaborate(Module(new Tile), b, true)
      val dutName = dut.getClass.getSimpleName
      tests.zipWithIndex map {case (name, i) =>
        val sample = Sample.load(s"${outDir.getPath}/${name}.sample")
        val dump = b match {
          case "c" => Some(s"${dumpDir.getPath}/${dutName}-${name}-replay.vcd")
          case "v" => Some(s"${dumpDir.getPath}/${dutName}-${name}-replay.vpd")
        }
        val log = Some(s"${logDir.getPath}/${dutName}-${name}-replay-${b}.log")
        val args = new ReplayArgs(sample, dump, log)
        name -> (testers(i % N) !! new TileReplay(dut, args))
      } foreach {case (name, f) =>
        f.inputChannel receive {case pass: Boolean =>
          Then(s"should pass ${name}") 
          assert(pass)
        }
      }
    }
  } */
}

class MiniSimCppSimpleTests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "verilator", SimpleTests)
// class MiniSimCppISATests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "verilator", ISATests)
// class MiniSimCppBmarkTests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "verilator", BmarkTests)
class MiniSimVCSSimpleTests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "vcs", SimpleTests)
// class MiniSimVCSISATests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "vcs", ISATests)
// class MiniSimVCSBmarkTests extends MiniTestSuite(SimWrapper(new Tile(miniParam))(simParam), "vcs", BmarkTests)
class MiniZynqCppSimpleTests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "verilator", SimpleTests)
// class MiniZynqCppISATests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "verilator", ISATests)
// class MiniZynqCppBmarkTests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "verilator", BmarkTests)
class MiniZynqVCSSimpleTests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "vcs", SimpleTests)
// class MiniZynqVCSISATests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "vcs", ISATests)
// class MiniZynqVCSBMarkTests extends MiniTestSuite(ZynqShim(new Tile(miniParam))(zynqParam), "vcs", BmarkTests)
