import sbt._
import Keys._

object StroberBuild extends Build {
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-deprecation","-unchecked")
  )
  lazy val chisel    = project in file("riscv-mini/chisel")
  lazy val firrtl    = project in file("riscv-mini/firrtl")
  lazy val cde       = project in file("riscv-mini/cde") dependsOn chisel
  lazy val junctions = project in file("riscv-mini/junctions") dependsOn cde
  lazy val interp    = project in file("riscv-mini/interp") dependsOn firrtl
  lazy val testers   = project in file("riscv-mini/testers") dependsOn (chisel, interp)
  lazy val tutorial  = project dependsOn testers
  lazy val widgets   = project in file("strober/src/main/scala/widgets") dependsOn (junctions)
  lazy val memModel  = project in file("midas-memory-model") dependsOn (widgets)
  lazy val strober   = project dependsOn (memModel, testers)
  lazy val mini      = project in file("riscv-mini") dependsOn (junctions, testers)
  lazy val root      = project in file(".") settings (settings:_*) dependsOn (
    tutorial, strober, mini % "compile->compile;test->test")
}
