import sbt._
import Keys._

object StroberBuild extends Build {
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-deprecation","-unchecked"),
    libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.3.15")
  )
  lazy val chisel    = project in file("riscv-mini/chisel")
  lazy val firrtl    = project in file("riscv-mini/firrtl")
  lazy val cde       = project in file("riscv-mini/cde") dependsOn chisel
  lazy val junctions = project in file("riscv-mini/junctions") dependsOn cde
  lazy val interp    = project in file("riscv-mini/interp") dependsOn firrtl
  lazy val testers   = project in file("riscv-mini/testers") dependsOn (chisel, interp)
  lazy val strober   = project dependsOn (junctions, testers)
  lazy val tutorial  = project dependsOn testers
  lazy val mini      = project in file("riscv-mini") dependsOn (junctions, testers)
  lazy val root      = project in file(".") settings (settings:_*) dependsOn (
    tutorial, strober, mini % "compile->compile;test->test")
}
