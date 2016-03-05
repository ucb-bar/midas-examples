package StroberExamples

import Chisel._
import strober._
import mini._
import org.scalatest._
import scala.actors.Actor._
import sys.process.stringSeqToProcess
import java.io.File

abstract class MiniSuite(N: Int = 6) extends fixture.PropSpec with fixture.ConfigMapFixture
    with GivenWhenThen with BeforeAndAfter with TestSuiteCommon with RiscVTests {
  private case class TestRun(c: Module, sample: Option[String], args: MiniTestArgs)
  private case class TileReplay(c: Tile, args: ReplayArgs)
  private case object TestFin
  private val testers = List.fill(N){ actor { loop { react {
    case TestRun(c, sample, args) => c match {
      case w: SimWrapper[_] => 
        val dut = w.asInstanceOf[SimWrapper[Tile]]
        sender ! (try {
          (new TileSimTests(dut, sample, args)).finish
        } catch {
          case _: Throwable => false
        })
      case w: NastiShim[_] => 
        val dut = w.asInstanceOf[NastiShim[SimWrapper[Tile]]]
        sender ! (try {
          (new TileNastiShimTests(dut, sample, args)).finish
        } catch {
          case _: Throwable => false
        })
      case _ => sender ! false
    }
    case TileReplay(c, args) => sender ! (try {
      (new Replay(c, args)).finish
    } catch {
      case _: Throwable => false
    })
    case TestFin => exit()
  } } } }

  def runTests(t: TestType) {
    property("riscv-mini on strober should run the following tests") { configMap =>
      val w = configMap("w").asInstanceOf[String]
      val b = configMap("b").asInstanceOf[String]
      implicit val p = w match {
        case "sim"   => cde.Parameters.root((new MiniSimConfig).toInstance)
        case "nasti" => cde.Parameters.root((new MiniNastiConfig).toInstance)
      }
      val dut = elaborate(w match {
        case "sim"  => SimWrapper(new Tile)
        case "nasti" => NastiShim(new Tile)
      }, b, true)
      val dutName = dut.getClass.getSimpleName
      val (dir, tests, maxcycles) = t match {
        case ISATests   => (new File("riscv-mini/riscv-tests/isa"), isaTests, 15000L)
        case BmarkTests => (new File("riscv-mini/riscv-bmarks"), bmarkTests, 500000L)
      }
      Given(t.toString)
      tests.zipWithIndex map {case (name, i) =>
        val loadmem = s"${dir.getPath}/${name}.hex"
        val sample = Some(s"${outDir.getPath}/${name}.sample")
        val dump = b match {
          case "c" => Some(s"${dumpDir.getPath}/${dutName}-${name}.vcd")
          case "v" => Some(s"${dumpDir.getPath}/${dutName}-${name}.vpd")
        }
        val log = Some(s"${logDir.getPath}/${dutName}-${name}-${b}.log") 
        val args = new MiniTestArgs(loadmem, maxcycles, dump, log)
        if (!(new java.io.File(loadmem).exists)) {
          assert(Seq("make", "-C", dir.getPath.toString, s"${name}.hex",
                     """'RISCV_GCC=$(RISCV_PREFIX)gcc -m32'""").! == 0)
        }
        name -> (testers(i % N) !! new TestRun(dut, sample, args))
      } foreach {case (name, f) => 
        f.inputChannel receive {case pass: Boolean =>
          Then(s"should pass ${name}") 
          assert(pass)
        }
      }
    }
  }

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
  }
}

class MiniISATests extends MiniSuite {
  runTests(ISATests)
}

class MiniBmarkTests extends MiniSuite {
  runTests(BmarkTests)
}

class ReplayISATests extends MiniSuite {
  replaySamples(ISATests)
}

class ReplayBmarkTests extends MiniSuite {
  replaySamples(BmarkTests)
}
