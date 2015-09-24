import sbt._
import Keys._

object StroberBuild extends Build {
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.11.6",
    scalacOptions ++= Seq("-deprecation","-unchecked")
  )
  lazy val chisel    = Project("chisel", base=file("chisel"))
  lazy val strober   = Project("strober", base=file("strober")) dependsOn chisel
  lazy val tutorial  = Project("tutorial", base=file("tutorial/examples")) dependsOn chisel
  lazy val junctions = Project("junctions", base=file("junctions")) dependsOn chisel
  lazy val mini      = Project("riscv-mini", base=file("riscv-mini")) dependsOn junctions
  lazy val root      = Project("strober-examples", base=file("."), settings=settings) dependsOn (strober, tutorial, mini)
}
