lazy val settings = Seq(
  scalaVersion := "2.11.7",
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
lazy val hardfloat  = project in file("rocket-chip/hardfloat") settings settings dependsOn chisel
lazy val rocketchip = project in file("rocket-chip") settings settings dependsOn hardfloat
lazy val midas      = project settings settings dependsOn rocketchip
lazy val mini       = project in file("riscv-mini") settings settings dependsOn rocketchip
lazy val root       = project in file(".") settings settings dependsOn (midas, mini)
