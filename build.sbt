lazy val commonSettings = Seq(
  scalaVersion := "2.11.12",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scalatest" %% "scalatest" % "3.0.1",
    "org.json4s" %% "json4s-native" % "3.5.3",
    "edu.berkeley.cs" %% "chisel3" % "3.0.2"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases"))
)

lazy val lib        = project
  .settings(commonSettings)
lazy val mdf        = (project in file("barstools/mdf/scalalib"))
lazy val barstools  = (project in file("barstools/macros"))
  .settings(commonSettings)
  .dependsOn(mdf)
lazy val midas      = project
  .settings(commonSettings)
  .dependsOn(barstools, lib)
lazy val mini       = (project in file("riscv-mini"))
  .settings(commonSettings)
  .dependsOn(midas)
lazy val root       = (project in file("."))
  .settings(commonSettings)
  .dependsOn(midas, mini)
