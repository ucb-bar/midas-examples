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
  args(0) match {
    case "midas" =>
      implicit val p = platform match {
        case "zynq"     => root((new midas.ZynqConfig).toInstance)
        case "catapult" => root((new midas.CatapultConfig).toInstance)
      }
      // implicit val p = root((new ZynqConfigWithMemModel).toInstance)
      MidasCompiler(dut, new File(dirPath))
    case "strober" =>
      implicit val p = platform match {
        case "zynq"     => root((new midas.ZynqConfigWithSnapshot).toInstance)
        case "catapult" => root((new midas.CatapultConfigWithSnapshot).toInstance)
      }
      // implicit val p = root((new ZynqConfigWithMemModelAndSnapshot).toInstance)
      MidasCompiler(dut, new File(dirPath))
    case "replay" =>
      strober.replay.Compiler(dut, new File(dirPath))
  }
}
