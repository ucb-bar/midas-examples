val defaultVersions = Map(
  "chisel3" -> "3.1-SNAPSHOT",
  "firrtl" -> "1.1-SNAPSHOT")

lazy val commonSettings = Seq(
  scalaVersion := "2.11.7",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
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

lazy val tutorial = project settings subModSettings
lazy val mini     = project in file("riscv-mini") settings subModSettings
lazy val strober  = project settings commonSettings dependsOn mini
lazy val midasmem = project in file("midas-memory-model") settings commonSettings dependsOn strober
lazy val root     = project in file(".") settings rootSettings dependsOn (tutorial, midasmem)
