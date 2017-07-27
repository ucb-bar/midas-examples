lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scalatest" %% "scalatest" % "2.2.4"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases"),
    Resolver.mavenLocal)
)

lazy val firrtl     = project
lazy val chisel     = project dependsOn firrtl
lazy val hardfloat  = (project in file("rocket-chip/hardfloat"))
  .settings(commonSettings)
  .dependsOn(chisel)
lazy val rocketchip = (project in file("rocket-chip"))
  .settings(commonSettings)
  .dependsOn(hardfloat)
lazy val mdf        = RootProject(file("barstools/mdf/scalalib"))
lazy val barstools  = (project in file("barstools/macros"))
  .settings(commonSettings)
  .dependsOn(chisel, mdf)
lazy val midas      = project
  .settings(commonSettings)
  .dependsOn(rocketchip, barstools)
lazy val mini       = (project in file("riscv-mini"))
  .settings(commonSettings)
  .dependsOn(rocketchip)
lazy val root       = (project in file("."))
  .settings(commonSettings)
  .dependsOn(midas, mini)
