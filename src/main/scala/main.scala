package StroberExample

import Chisel._
import Designs._
import TutorialExamples._
import strober._

object StroberExample {
  def main(args: Array[String]) {
    val (chiselArgs, testArgs) = args.tail partition (_.head != '+')
    val snapCheck = !(testArgs exists (_ contains "+nosnapcheck"))
    implicit val p = NastiParams(SimParams())
    val res = args(0) match {
      case "GCDWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new GCD))(c => new GCDSimSimTests(c))
      case "ParityWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Parity))(c => new ParitySimTests(c))
      case "ShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new ShiftRegister))(c => new ShiftRegisterSimTests(c))
      case "EnableShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new EnableShiftRegister))(c => new EnableShiftRegisterSimTests(c))
      case "ResetShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new ResetShiftRegister))(c => new ResetShiftRegisterSimTests(c))
      case "StackWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Stack(8)))(c => new StackSimTests(c))
      case "MemorySearchWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new MemorySearch))(c => new MemorySearchSimTests(c))
      case "RouterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Router))(c => new RouterSimTests(c))
      case "RiscWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Risc))(c => new RiscSimTests(c))
      case "RiscSRAMWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new RiscSRAM))(c => new RiscSRAMSimTests(c))
      case "TileWrapper" => 
        chiselMainTest(chiselArgs, () => SimWrapper(new Tile()(mini.Config.params)))(c => new TileSimTests(c, testArgs, snapCheck))

      case "GCDNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new GCD))(c => new GCDNastiShimTests(c))
      case "ParityNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new Parity))(c => new ParityNastiShimTests(c))
      case "ShiftRegisterNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new ShiftRegister))(c => new ShiftRegisterNastiShimTests(c))
      case "EnableShiftRegisterNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new EnableShiftRegister))(c => new EnableShiftRegisterNastiShimTests(c))
      case "ResetShiftRegisterNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new ResetShiftRegister))(c => new ResetShiftRegisterNastiShimTests(c))
      case "StackNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new Stack(8)))(c => new StackNastiShimTests(c))
      case "MemorySearchNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new MemorySearch))(c => new MemorySearchNastiShimTests(c))
      case "RouterNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new Router))(c => new RouterNastiShimTests(c))
      case "RiscNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new Risc))(c => new RiscNastiShimTests(c))
      case "RiscSRAMNastiShim" =>
        chiselMainTest(chiselArgs, () => NastiShim(new RiscSRAM))(c => new RiscSRAMNastiShimTests(c))
      case "TileNastiShim" => 
        chiselMainTest(chiselArgs, () => NastiShim(new Tile()(mini.Config.params)))(c => new TileNastiShimTests(c, testArgs, snapCheck))
 
      case "GCDReplay" =>
        chiselMainTest(args.tail, () => Module(new GCD))(c => new Replay(c, testArgs))
      case "ParityReplay" =>
        chiselMainTest(args.tail, () => Module(new Parity))(c => new Replay(c, testArgs))
      case "ShiftRegisterReplay" =>
        chiselMainTest(args.tail, () => Module(new ShiftRegister))(c => new Replay(c, testArgs))
      case "ResetShiftRegisterReplay" =>
        chiselMainTest(args.tail, () => Module(new ResetShiftRegister))(c => new Replay(c, testArgs))
      case "EnableShiftRegisterReplay" =>
        chiselMainTest(args.tail, () => Module(new EnableShiftRegister))(c => new Replay(c, testArgs))
      case "MemorySearchReplay" =>
        chiselMainTest(args.tail, () => Module(new MemorySearch))(c => new Replay(c, testArgs))
      case "StackReplay" =>
        chiselMainTest(args.tail, () => Module(new Stack(8)))(c => new Replay(c, testArgs))
      case "RiscReplay" =>
        chiselMainTest(args.tail, () => Module(new Risc))(c => new Replay(c, testArgs))
      case "RiscSRAMReplay" =>
        chiselMainTest(args.tail, () => Module(new RiscSRAM))(c => new Replay(c, testArgs))
      case "RouterReplay" =>
        chiselMainTest(args.tail, () => Module(new Router))(c => new Replay(c, testArgs))
      case "TileReplay" =>
        chiselMainTest(args.tail, () => Module(new Tile()(mini.Config.params)))(c => new Replay(c, testArgs))

      case "GCD" =>
        chiselMainTest(args.tail, () => Module(new GCD))(c => new GCDTester(c))
      case "Parity" =>
        chiselMainTest(args.tail, () => Module(new Parity))(c => new ParityTester(c))
      case "ShiftRegister" =>
        chiselMainTest(args.tail, () => Module(new ShiftRegister))(c => new ShiftRegisterTester(c))
      case "ResetShiftRegister" =>
        chiselMainTest(args.tail, () => Module(new ResetShiftRegister))(c => new ResetShiftRegisterTester(c))
      case "EnableShiftRegister" =>
        chiselMainTest(args.tail, () => Module(new EnableShiftRegister))(c => new EnableShiftRegisterTester(c))
      case "MemorySearch" =>
        chiselMainTest(args.tail, () => Module(new MemorySearch))(c => new MemorySearchTester(c))
      case "Stack" =>
        chiselMainTest(args.tail, () => Module(new Stack(8)))(c => new StackTester(c))
      case "Risc" =>
        chiselMainTest(args.tail, () => Module(new Risc))(c => new RiscTester(c))
      case "RiscSRAM" =>
        chiselMainTest(args.tail, () => Module(new RiscSRAM))(c => new RiscSRAMTester(c))
      case "Router" =>
        chiselMainTest(args.tail, () => Module(new Router))(c => new RouterTester(c))
      case "Tile" =>
        chiselMainTest(args.tail, () => Module(new mini.Tile()(mini.Config.params)))(c => new mini.TileTester(c, testArgs))
      case _ =>
    }
  }
}
