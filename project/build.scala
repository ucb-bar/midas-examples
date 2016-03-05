import sbt._
import Keys._

object StroberBuild extends Build {
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-deprecation","-unchecked"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-actors" % scalaVersion.value,
      "org.scalatest" % "scalatest_2.11" % "2.2.4" % Test)
  )
  lazy val chisel    = Project("chisel",     base=file("riscv-mini/chisel"))
  lazy val cde       = Project("cde",        base=file("riscv-mini/cde")) dependsOn chisel
  lazy val junctions = Project("junctions",  base=file("riscv-mini/junctions")) dependsOn cde
  lazy val strober   = Project("strober",    base=file("strober")) dependsOn junctions
  lazy val tutorial  = Project("tutorial",   base=file("tutorial/examples")) dependsOn chisel
  lazy val mini      = Project("riscv-mini", base=file("riscv-mini")) dependsOn junctions
  lazy val root      = Project("strober-examples", base=file("."), settings=settings) dependsOn (
    strober, tutorial, mini % "compile->compile;test->test")
}
