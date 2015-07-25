package StroberExample

import Chisel._
import Designs._
import TutorialExamples._
import mini.Core
import strober._

object StroberExample {
  def main(args: Array[String]) {
    val (chiselArgs, testArgs) = args.tail partition (_.head != '+')
    val matchFile = if (args(1) contains "+match=") Some(args(1).substring(7)) else None
    val res = args(0) match {
      case "GCDWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new GCD))(
          c => new GCDSimWrapperTests(c))
      case "ParityWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Parity))(
          c => new ParityWrapperTests(c))
      case "ShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new ShiftRegister))(
          c => new ShiftRegisterWrapperTests(c))
      case "EnableShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new EnableShiftRegister))(
          c => new EnableShiftRegisterWrapperTests(c))
      case "ResetShiftRegisterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new ResetShiftRegister))(
          c => new ResetShiftRegisterWrapperTests(c))
      case "StackWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Stack(8)))(
          c => new StackWrapperTests(c))
      case "MemorySearchWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new MemorySearch))(
          c => new MemorySearchWrapperTests(c))
      case "RouterWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Router))(
          c => new RouterWrapperTests(c))
      case "RiscWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new Risc))(
          c => new RiscWrapperTests(c))
      case "RiscSRAMWrapper" =>
        chiselMainTest(chiselArgs, () => SimWrapper(new RiscSRAM))(
          c => new RiscSRAMWrapperTests(c))
      case "CoreWrapper" => 
        chiselMainTest(chiselArgs, () => SimWrapper(new Core, mini.Config.params))(
          c => new CoreWrapperTests(c, testArgs))
      case "TileWrapper" => 
        chiselMainTest(chiselArgs, () => SimWrapper(new Tile, mini.Config.params))(
          c => new TileWrapperTests(c, testArgs))

      case "GCDAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new GCD))(
          c => new GCDSimAXI4WrapperTests(c))
      case "ParityAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new Parity))(
          c => new ParityAXI4WrapperTests(c))
      case "ShiftRegisterAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new ShiftRegister))(
          c => new ShiftRegisterAXI4WrapperTests(c))
      case "EnableShiftRegisterAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new EnableShiftRegister))(
          c => new EnableShiftRegisterAXI4WrapperTests(c))
      case "ResetShiftRegisterAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new ResetShiftRegister))(
          c => new ResetShiftRegisterAXI4WrapperTests(c))
      case "StackAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new Stack(8)))(
          c => new StackAXI4WrapperTests(c))
      case "MemorySearchAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new MemorySearch))(
          c => new MemorySearchAXI4WrapperTests(c))
      case "RouterAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new Router))(
          c => new RouterAXI4WrapperTests(c))
      case "RiscAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new Risc))(
          c => new RiscAXI4WrapperTests(c))
      case "RiscSRAMAXI4Wrapper" =>
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new RiscSRAM))(
          c => new RiscSRAMAXI4WrapperTests(c))
      case "CoreAXI4Wrapper" => 
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new Core, mini.Config.params))(
          c => new CoreAXI4WrapperTests(c, testArgs))
      case "TileAXI4Wrapper" => 
        chiselMainTest(chiselArgs, () => SimAXI4Wrapper(new Tile, mini.Config.params))(
          c => new TileAXI4WrapperTests(c, testArgs))
 
      case "GCD" =>
        chiselMainTest(args.tail, () => Module(new GCD))(c => new Replay(c, matchFile))
      case "Parity" =>
        chiselMainTest(args.tail, () => Module(new Parity))(c => new Replay(c, matchFile))
      case "ShiftRegister" =>
        chiselMainTest(args.tail, () => Module(new ShiftRegister))(c => new Replay(c, matchFile))
      case "ResetShiftRegister" =>
        chiselMainTest(args.tail, () => Module(new ResetShiftRegister))(c => new Replay(c, matchFile))
      case "EnableShiftRegister" =>
        chiselMainTest(args.tail, () => Module(new EnableShiftRegister))(c => new Replay(c, matchFile))
      case "MemorySearch" =>
        chiselMainTest(args.tail, () => Module(new MemorySearch))(c => new Replay(c, matchFile))
      case "Stack" =>
        chiselMainTest(args.tail, () => Module(new Stack(8)))(c => new Replay(c, matchFile))
      case "Risc" =>
        chiselMainTest(args.tail, () => Module(new Risc))(c => new Replay(c, matchFile))
      case "RiscSRAM" =>
        chiselMainTest(args.tail, () => Module(new RiscSRAM))(c => new Replay(c, matchFile))
      case "Router" =>
        chiselMainTest(args.tail, () => Module(new Router))(c => new Replay(c, matchFile))
      case "Core" =>
        chiselMainTest(args.tail, () => Module(new Core)(mini.Config.params))(c => new Replay(c, matchFile))
      case "Tile" =>
        chiselMainTest(args.tail, () => Module(new Tile)(mini.Config.params))(c => new Replay(c, matchFile))
      case _ =>
    }
  }
}
