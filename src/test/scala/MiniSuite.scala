package StroberExamples

import mini.Tile
import chisel3.Module
import strober.{SimWrapper, ZynqShim, StroberCompiler}
import scala.reflect.ClassTag
import sys.process.stringSeqToProcess
import java.io.File

abstract class MiniTestSuite(N: Int = 6) extends org.scalatest.FlatSpec with mini.RiscVTests {
  require(N > 0)
  import akka.pattern.ask
  import scala.concurrent.duration._
  implicit val timeout = akka.util.Timeout(10 days)
  private case class TestRun(c: Module, args: mini.MiniTestArgs)
  private case object TestFin
  private val system = akka.actor.ActorSystem("riscv-mini")
  private val testers = List.fill(N){
    akka.actor.ActorDSL.actor(system)(new akka.actor.ActorDSL.ActWithStash {
      override def receive = {
        case TestRun(dut, args) => sender ! (
          try {
            dut match {
              case sim: SimWrapper[Tile] => 
                (new TileSimTests(sim, args)).finish
              case zynq: ZynqShim[SimWrapper[Tile]] =>
                (new TileZynqTests(zynq, args)).finish
            }
          } catch {
            case x: Exception => x.printStackTrace
          }
        )
        case TestFin => context.stop(self)
      }
    })
  }

  def runTests[T <: Module : ClassTag](mod: => T, t: TestType, vcs: Boolean = false) {
    val targetDir = s"$outDir/${implicitly[ClassTag[T]].runtimeClass.getSimpleName}_Tile"
    val args = Array("--targetDir", targetDir, "--genHarness", "--compile", "--noUpdate") ++ 
               (if (vcs) Array("--vcs") else Nil)
    val (dir, tests, maxcycles) = t match {
      case ISATests   => (new File("riscv-mini/riscv-tests/isa"), isaTests, 15000L)
      case BmarkTests => (new File("riscv-mini/riscv-bmarks"), bmarkTests, 1500000L)
    }
    StroberCompiler compile (args, mod)
    val dut = chisel3.iotesters.chiselMain(args, () => mod)
    val sim = if (vcs) "vcs" else "verilator"
    assert(dir.exists)
    behavior of s"${dut.name} in $sim"
    tests.zipWithIndex map { case (t, i) => 
      val loadmem = s"$dir/$t.hex"
      val logFile = Some(s"$targetDir/$t-$sim.log")
      val waveform = Some(s"$targetDir/$t.%s".format(if (vcs) "vpd" else "vcd"))
      val testCmd = List(s"$targetDir/%s${dut.name}".format(if (vcs) "" else "V"))
      val args = new mini.MiniTestArgs(loadmem, maxcycles, logFile, waveform, testCmd, true)
      if (!(new File(loadmem).exists)) {
        assert(Seq("make", "-C", dir.getPath.toString, s"$t.hex",
                   """'RISCV_GCC=$(RISCV_PREFIX)gcc -m32'""").! == 0)
      }
      t -> (testers(i % N) ? new TestRun(dut, args))
    } foreach { case (name, f) =>
      scala.concurrent.Await.result(f, timeout.duration) match { case pass: Boolean =>
        it should s"pass $name" in { assert(pass) }
      }
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
}

/*
class MiniISATests extends MiniSuite {
  runTests(ISATests)
}
*/
/*
class MiniSimVeriBmarkTests extends MiniTestSuite {
  val param = cde.Parameters.root((new SimConfig).toInstance)
  runTests(SimWrapper(new Tile(p))(param), BmarkTests)
}

class MiniSimVCSBmarkTests extends MiniTestSuite {
  val param = cde.Parameters.root((new SimConfig).toInstance)
  runTests(SimWrapper(new Tile(p))(param), BmarkTests, true)
}

class MiniZynqVeriBmarkTests extends MiniTestSuite {
  val param = cde.Parameters.root((new ZynqConfig).toInstance)
  runTests(ZynqShim(new Tile(p))(param), BmarkTests)
}

class MiniZynqVCSBmarkTests extends MiniTestSuite {
  val param = cde.Parameters.root((new ZynqConfig).toInstance)
  runTests(ZynqShim(new Tile(p))(param), BmarkTests, true)
}
*/
/*
class ReplayISATests extends MiniSuite {
  replaySamples(ISATests)
}

class ReplayBmarkTests extends MiniSuite {
  replaySamples(BmarkTests)
}
*/
