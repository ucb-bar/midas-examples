package StroberExamples

import midas._
import cde.Parameters.root
import java.io.File

object StroberExamples extends App {
  val modName = args(1)
  val dirPath = args(2)
  def dut = modName match {
    case "PointerChaser" =>
      new PointerChaser()(root((new PointerChaserConfig).toInstance))
    case "Tile"  =>
      new mini.Tile(root((new mini.MiniConfig).toInstance))
    case "Stack" =>
      new examples.Stack(8)
    case _ =>
      Class.forName(s"examples.${modName}")
           .getConstructors.head
           .newInstance()
           .asInstanceOf[chisel3.Module]
  }
  args(0) match {
    case "midas" =>
      implicit val p = root((new midas.ZynqConfig).toInstance)
      // implicit val p = root((new ZynqConfigWithMemModel).toInstance)
      MidasCompiler(dut, new File(dirPath))
    case "strober" =>
      implicit val p = root((new midas.ZynqConfigWithSnapshot).toInstance)
      // implicit val p = root((new ZynqConfigWithMemModelAndSnapshot).toInstance)
      MidasCompiler(dut, new File(dirPath))
    case "replay" =>
      strober.replay.Compiler(dut, new File(dirPath))
  }
}
