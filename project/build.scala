import sbt._
import Keys._

object StroberBuild extends Build {
  val chiselVersion = "3.0-BETA-SNAPSHOT"
  val firrtlVersion = "0.2-BETA-SNAPSHOT"
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.11.7",
    libraryDependencies += "edu.berkeley.cs" %% "chisel3" % chiselVersion,
    resolvers ++= Seq(Resolver.sonatypeRepo("snapshots"), Resolver.sonatypeRepo("releases"))
  )
  lazy val testlib = settingKey[Unit]("compile test libraries")
  lazy val compileTestLib = Seq(testlib := {
    val dir = s"${baseDirectory.value}/src/main/cc"
    val make = Seq("make", "-C", dir)
    assert((make :+ s"$dir/utils/libemul.a").! == 0)
    assert((make :+ s"$dir/utils/libreplay.a").! == 0)
  })
  lazy val firrtl    = project
  lazy val tutorial  = project
  lazy val cde       = project in file("riscv-mini/cde") dependsOn firrtl
  lazy val junctions = project in file("riscv-mini/junctions") dependsOn cde settings (settings:_*) 
  lazy val mini      = project in file("riscv-mini") dependsOn junctions
  lazy val strober   = project dependsOn junctions
  lazy val memModel  = project in file("midas-memory-model") dependsOn strober
  lazy val root      = project in file(".") dependsOn (tutorial, mini, memModel)
}
