package StroberExamples

import strober._
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
    case "strober" =>
      implicit val p = root((new ZynqConfig).toInstance)
      StroberCompiler compile (Array("--targetDir", dirPath), ZynqShim(dut))
    case "replay" =>
      replay.Compiler(dut, new File(dirPath))
  }
}
