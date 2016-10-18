import sbt._
import Keys._

object StroberBuild extends Build {
  lazy val testlib = settingKey[Unit]("compile test libraries")
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-deprecation","-unchecked"),
    testlib := {
      val dir = s"${baseDirectory.value}/strober/src/main/cc"
      val make = Seq("make", "-C", dir)
      assert((make :+ s"$dir/utils/libemul.a").! == 0)
      assert((make :+ s"$dir/utils/libreplay.a").! == 0)
    }
  )
  lazy val chisel    = project in file("riscv-mini/chisel")
  lazy val firrtl    = project in file("riscv-mini/firrtl")
  lazy val interp    = project in file("riscv-mini/interp") dependsOn firrtl
  lazy val testers   = project in file("riscv-mini/testers") dependsOn (chisel, interp)
  lazy val tutorial  = project dependsOn testers
  lazy val cde       = project in file("riscv-mini/cde") dependsOn chisel
  lazy val junctions = project in file("riscv-mini/junctions") dependsOn cde
  lazy val mini      = project in file("riscv-mini") dependsOn (junctions, testers)
  lazy val strober   = project dependsOn (firrtl, junctions)
  lazy val memModel  = project in file("midas-memory-model") dependsOn strober
  lazy val root      = project in file(".") settings (settings:_*) dependsOn (
    tutorial, mini % "compile->compile;test->test", memModel)
}
