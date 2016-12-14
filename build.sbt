val defaultVersions = Map(
  "chisel3" -> "3.1-SNAPSHOT",
  "firrtl" -> "1.1-SNAPSHOT")

lazy val commonSettings = Seq(
  scalaVersion := "2.11.7",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "org.scalatest" %% "scalatest" % "2.2.4"
  ),
  libraryDependencies ++= (Seq("chisel3", "firrtl") map { dep: String =>
    "edu.berkeley.cs" %% dep % sys.props.getOrElse(s"${dep}Version", defaultVersions(dep))
  }),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases"),
    Resolver.mavenLocal)
)

lazy val subModSettings = commonSettings ++ Seq(
  fork := true,
  javaOptions ++= (Seq("chisel3", "firrtl") map { dep: String =>
    s"-D${dep}Version=%s".format(sys.props getOrElse (s"${dep}Version", defaultVersions(dep)))
  })
)

lazy val rootSettings = commonSettings ++ Seq(
  name := "strober-examples",
  version := "1.0-SANPSHOT"
)

lazy val cde       = project in file("rocket-chip/context-dependent-environments") settings commonSettings
lazy val hardfloat = project in file("rocket-chip/hardfloat") settings commonSettings
lazy val rocket    = project in file("rocket-chip") settings commonSettings dependsOn (cde, hardfloat)
lazy val strober   = project settings commonSettings dependsOn rocket
lazy val midasmem  = project in file("midas-memory-model") settings commonSettings dependsOn strober
lazy val mini      = project in file("riscv-mini") settings commonSettings dependsOn rocket
lazy val root      = project in file(".") settings rootSettings dependsOn (mini, midasmem)
