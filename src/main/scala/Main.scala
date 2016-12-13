package strober.examples

import midas._
import cde.Parameters.root
import java.io.File

object StroberExamples extends App {
  lazy val modName = args(1)
  lazy val dirPath = args(2)
  lazy val platform = args(3)
  def dut = modName match {
    case "Tile"  =>
      new mini.Tile(root((new mini.MiniConfig).toInstance))
    case "PointerChaser" =>
      new PointerChaser()(root((new PointerChaserConfig).toInstance))
    case "RiscSRAM" => new RiscSRAM
    case "Stack" => new examples.Stack(8)
    case _ =>
      Class.forName(s"examples.${modName}")
           .getConstructors.head
           .newInstance()
           .asInstanceOf[chisel3.Module]
  }
  def midasParams = root((platform match {
    case "zynq"     => new midas.ZynqConfig
    case "catapult" => new midas.CatapultConfig
  }).toInstance)
  args.head match {
    case "midas" =>
      MidasCompiler(dut, new File(dirPath))(midasParams)
    case "strober" =>
      MidasCompiler(dut, new File(dirPath))(
        midasParams alter Map(midas.EnableSnapshot -> true))
    case "replay" =>
      strober.replay.Compiler(dut, new File(dirPath))
  }
}
