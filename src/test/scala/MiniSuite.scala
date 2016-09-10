package StroberExamples

import chisel3.Module
import chisel3.iotesters.Driver
import strober.{SimWrapper, ZynqShim, StroberCompiler}
import mini.{Tile, MiniConfig, MiniTestArgs, RiscVTests}
import sys.process.stringSeqToProcess
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import java.io.File

object TestParams {
  implicit def p = cde.Parameters.root((new MiniConfig).toInstance)
  val simParam = cde.Parameters.root((new SimConfig).toInstance)
  val zynqParam = cde.Parameters.root((new ZynqConfig).toInstance)
}
import TestParams.{p, simParam, zynqParam}

abstract class MiniTestSuite[+T <: Module : ClassTag](
    dutGen: () => T, backend: String, N: Int = 10) extends org.scalatest.FlatSpec with RiscVTests {
  val dutName = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  val testDir = new File(outDir, s"$dutName/Tile")
  val args = Array("--targetDir", testDir.toString)
  val dut = StroberCompiler compile (args, dutGen(), backend)
  val vcs = backend == "vcs"
  behavior of s"$dutName in $backend"

  def runTests(testType: TestType) = {
    val (dir, tests, maxcycles) = testType match {
      case ISATests   => (new File("riscv-mini/riscv-tests/isa"), isaTests, 15000L)
      case BmarkTests => (new File("riscv-mini/riscv-bmarks"), bmarkTests, 1500000L)
    }
    assert(dir.exists)
    import scala.concurrent.duration._
    import ExecutionContext.Implicits.global
    val results = tests.zipWithIndex sliding (N, N) map { subtests => Future {
      val subresults = subtests map {case (t, i) =>
        val loadmem = new File(dir, s"$t.hex")
        val logFile = Some(new File(testDir, s"$t-$backend.log"))
        val waveform = Some(new File(testDir, s"$t.%s".format(if (vcs) "vpd" else "vcd")))
        val testArgs = new MiniTestArgs(loadmem, logFile, false, maxcycles) // latency)
        if (!loadmem.exists) {
          assert(Seq("make", "-C", dir.getPath.toString, s"$t.hex",
                     """'RISCV_GCC=$(RISCV_PREFIX)gcc -m32'""").! == 0)
        }
        Future { t -> (dut match {
          case _: SimWrapper[_] =>
            StroberCompiler.test(args, dutGen().asInstanceOf[SimWrapper[Tile]], backend)(
              m => new TileSimTests(m, testArgs))
          case _: ZynqShim[_] =>
            StroberCompiler.test(args, dutGen().asInstanceOf[ZynqShim[SimWrapper[Tile]]], backend)(
              m => new TileZynqTests(m, testArgs))
        })}
      }
      Await.result(Future.sequence(subresults), Duration.Inf)
    } }
    Await.result(Future.sequence(results), Duration.Inf).flatten foreach {
      case (name, pass) => it should s"pass $name" in { assert(pass) }
    }
  }

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
  }

  after {
    testers foreach (_ ! TestFin)
    Tester.close
  } */

  // runTests(ISATests)
  runTests(BmarkTests)
}

class MiniSimCppTests extends MiniTestSuite(() => SimWrapper(new Tile(p))(simParam), "verilator")
class MiniSimVCSTests extends MiniTestSuite(() => SimWrapper(new Tile(p))(simParam), "vcs")
class MiniZynqCppTests extends MiniTestSuite(() => ZynqShim(new Tile(p))(zynqParam), "verilator")
class MiniZynqVCSTests extends MiniTestSuite(() => ZynqShim(new Tile(p))(zynqParam), "vcs")
