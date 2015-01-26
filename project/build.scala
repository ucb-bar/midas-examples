import sbt._
import Keys._

object DebugBuild extends Build {
  lazy val chisel   = Project("chisel", base=file("chisel"))
  lazy val strober  = Project("strober", base=file("strober")).dependsOn(chisel)  
  lazy val tutorial = Project("tutorial", base=file("tutorial/examples")).dependsOn(chisel)
  lazy val mini     = Project("riscv-mini", base=file("riscv-mini")).dependsOn(chisel)
  lazy val root     = Project("strober-examples", base=file(".")).dependsOn(strober, tutorial, mini)
}
