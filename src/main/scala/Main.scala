package StroberExamples

import Chisel._
import strober._
import scala.actors.Actor._
import sys.process.stringSeqToProcess

class Tile(implicit p: cde.Parameters) extends mini.Tile()(p) {
  if (core.useNasti) {
  } else {
    SimMemIO(io.mem)
  }
}

object StroberExamples {
  def main(args: Array[String]) {
    val modName = args(1)
    val dirName = args(2)
    def mod(implicit p: cde.Parameters) = modName match {
      case "Tile"  => new Tile
      case "Stack" => new TutorialExamples.Stack(8)
      case _ => 
        Class.forName(s"TutorialExamples.${modName}").getConstructors.head.newInstance().asInstanceOf[Module]
    }
    args(0) match {
      case "strober" => {
        val chiselArgs = Array(//"--minimumCompatibility", "3.0", 
          "--backend", "fpga", "--targetDir", dirName, "--configDump")
        implicit val p = modName match {
          case "Tile" => 
            cde.Parameters.root((new MiniNastiConfig).toInstance)
          case _ => 
            cde.Parameters.root((new NastiConfig).toInstance)
        }
        chiselMain(chiselArgs, () => NastiShim(mod))
      }
      case "vlsi" => {
        val chiselArgs = Array("--minimumCompatibility", "3.0", 
          "--v", "--targetDir", dirName, "--configInstance", args(3), 
          "--noInlineMem", "--genHarness", "--debug", "--vcd")
        implicit val p = cde.Parameters.root((new mini.MiniConfig).toInstance)
        chiselMain(chiselArgs, () => Module(mod))
      }
      case "replay" => {
        val b = args(3)
        val chiselArgs = Array("--minimumCompatibility", "3.0", 
          "--backend", b, "--targetDir", dirName,
          "--compile", "--compileInitializationUnoptimized",
          "--genHarness", "--test", "--vcd", "--vcdMem", "--debug") ++ (args drop 8)
        implicit val p = cde.Parameters.root((new mini.MiniConfig).toInstance)
        // elaborate the design first
        val dut = chiselMain(chiselArgs, () => Module(mod))
        // should load samples after design elaboration
        val sample = Sample.load(args(4))
        val prefix = (new java.io.File(args(4)).getName split '.').head
        val matchFile = args(5) match { case "none" => None case f => Some(f) }
        val testCmd = args(6) match { case "none" => None case cmd => Some(cmd) }
        val N = args(7).toInt
        // runs snapshot replays in parallel
        case object ReplayFin
        val replays = List.fill(N){ actor { loop { react {
          case args: ReplayArgs => sender ! (try {
            (new Replay(dut, args)).finish
          } catch {
            case _: Throwable => false
          })
          case ReplayFin => exit()
        } } } }
        val logDir = new java.io.File(s"${dirName}/logs")
        if (!logDir.exists) logDir.mkdirs
        sample.zipWithIndex map {case (sample, idx) =>
          // assign seperate dump & log files to each snapshot in sample
          val vcd  = s"${dirName}/${prefix}_${idx}_pipe.vcd"
          val vpd  = s"${dirName}/${prefix}_${idx}.vpd"
          val saif = s"${dirName}/${prefix}_${idx}.saif"
          val log  = s"${logDir.getPath}/${prefix}_${idx}.log"
          val (cmd, dump) = testCmd match {
            case None if b == "c" => (None, Some(vcd))
            case None if b == "v" => (None, Some(vcd))
            case Some(c) if matchFile == None =>
              (Some(List(c, s"+vpdfile=${vpd}") mkString " "), None)
            case Some(c) if matchFile != None =>
              Seq("rm", "-rf", vcd, vpd).!
              val pipe = List(c, s"+vpdfile=${vpd}", s"+vcdfile=${vcd}") mkString " "
              (Some(List("vcd2saif", "-input", vcd, "-output", saif, 
                         "-pipe", s""""${pipe}" """) mkString " "), None)
          }
          idx -> (replays(idx % N) !! (new ReplayArgs(Seq(sample), dump, Some(log), matchFile, cmd)))
        } map {case (idx, f) => 
          f.inputChannel receive {case pass: Boolean => idx -> pass}
        } foreach {case (idx, pass) => if (!pass) ChiselError.error(s"SAMPLE #${idx} FAILED")}
        replays foreach (_ ! ReplayFin)
      }
    }
  }
}
