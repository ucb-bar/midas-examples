lazy val commonSettings = Seq(
  scalaVersion := "2.11.12",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scalatest" %% "scalatest" % "2.2.4",
    "org.json4s" %% "json4s-native" % "3.5.3"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases"),
    Resolver.mavenLocal)
)

lazy val rocketchip = RootProject(file("rocket-chip"))
lazy val mdf        = RootProject(file("barstools/mdf/scalalib"))
lazy val barstools  = (project in file("barstools/macros"))
  .settings(commonSettings)
  .dependsOn(mdf, rocketchip)
lazy val midas      = project
  .settings(commonSettings)
  .dependsOn(barstools)
lazy val mini       = (project in file("riscv-mini"))
  .settings(commonSettings)
  .dependsOn(midas)
lazy val root       = (project in file("."))
  .settings(commonSettings)
  .dependsOn(midas, mini)
