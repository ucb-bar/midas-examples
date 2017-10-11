package midas
package examples

import midas._
import config.Parameters.root
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
    case _ =>
      Class.forName(s"strober.examples.${modName}")
           .getConstructors.head
           .newInstance()
           .asInstanceOf[chisel3.Module]
  }
  def midasParams = root((platform match {
    case "zynq"     => new midas.ZynqConfig
  }).toInstance)
  args.head match {
    case "midas" =>
      MidasCompiler(dut, new File(dirPath))(midasParams)
    case "strober" =>
      val lib = if (args.size > 4) Some(new File(args(4))) else None
      MidasCompiler(dut, new File(dirPath), lib)(
        midasParams alterPartial { case midas.EnableSnapshot => true })
    case "replay" =>
      val lib = if (args.size > 3) Some(new File(args(3))) else None
      strober.replay.Compiler(dut, new File(dirPath), lib)
  }
}
