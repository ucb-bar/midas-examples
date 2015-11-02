package StroberExample

import Chisel._
import Designs._
import TutorialExamples._
import strober._

object StroberExample {
  def main(args: Array[String]) {
    val (chiselArgs, testArgs) = args.tail partition (_.head != '+')
    val snapCheck = !(testArgs exists (_ contains "+nosnapcheck"))
    val simParams = SimParams()
    val nastiParams = NastiParams(simParams)
    val res = args(0) match {
      case "GCDWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new GCD)(simParams))(c => new GCDSimSimTests(c))
      case "ParityWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Parity)(simParams))(c => new ParitySimTests(c))
      case "ShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new ShiftRegister)(simParams))(c => new ShiftRegisterSimTests(c))
      case "EnableShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new EnableShiftRegister)(simParams))(c => new EnableShiftRegisterSimTests(c))
      case "ResetShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new ResetShiftRegister)(simParams))(c => new ResetShiftRegisterSimTests(c))
      case "StackWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Stack(8))(simParams))(c => new StackSimTests(c))
      case "MemorySearchWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new MemorySearch)(simParams))(c => new MemorySearchSimTests(c))
      case "RouterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Router)(simParams))(c => new RouterSimTests(c))
      case "RiscWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Risc)(simParams))(c => new RiscSimTests(c))
      case "RiscSRAMWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new RiscSRAM)(simParams))(c => new RiscSRAMSimTests(c))
      case "TileWrapper" => 
        chiselMainTest(chiselArgs, () => SimWrapper(new Tile)(SimParams(mini.Config.params)))(c => new TileSimTests(c, testArgs, snapCheck))

      case "GCDNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new GCD)(nastiParams))(c => new GCDNASTIShimTests(c))
      case "ParityNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new Parity)(nastiParams))(c => new ParityNASTIShimTests(c))
      case "ShiftRegisterNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new ShiftRegister)(nastiParams))(c => new ShiftRegisterNASTIShimTests(c))
      case "EnableShiftRegisterNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new EnableShiftRegister)(nastiParams))(c => new EnableShiftRegisterNASTIShimTests(c))
      case "ResetShiftRegisterNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new ResetShiftRegister)(nastiParams))(c => new ResetShiftRegisterNASTIShimTests(c))
      case "StackNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new Stack(8))(nastiParams))(c => new StackNASTIShimTests(c))
      case "MemorySearchNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new MemorySearch)(nastiParams))(c => new MemorySearchNASTIShimTests(c))
      case "RouterNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new Router)(nastiParams))(c => new RouterNASTIShimTests(c))
      case "RiscNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new Risc)(nastiParams))(c => new RiscNASTIShimTests(c))
      case "RiscSRAMNASTIShim" =>
        chiselMainTest(chiselArgs, () => NASTIShim(new RiscSRAM)(nastiParams))(c => new RiscSRAMNASTIShimTests(c))
      case "TileNASTIShim" => 
        chiselMainTest(chiselArgs, () => NASTIShim(new Tile)(NastiParams(SimParams(mini.Config.params))))(c => new TileNASTIShimTests(c, testArgs, snapCheck))
 
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
        chiselMainTest(args.tail, () => Module(new Tile)(mini.Config.params))(c => new Replay(c, testArgs))

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
        chiselMainTest(args.tail, () => Module(new mini.Tile)(mini.Config.params))(c => new mini.TileTester(c, testArgs))
      case _ =>
    }
  }
}
