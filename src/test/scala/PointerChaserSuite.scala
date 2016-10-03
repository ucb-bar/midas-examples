package StroberExamples

import chisel3.Module
import chisel3.iotesters._
import mini._
import strober.{SimWrapper, ZynqShim, StroberCompiler}
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.reflect.ClassTag
import java.io.File
import sys.process.stringSeqToProcess

import TestParams.{chaserParam, simParam, zynqParam}

abstract class PointerChaserTestSuite[+T <: Module : ClassTag](
    dutGen: => T,
    backend: String,
    N: Int = 10) extends org.scalatest.FlatSpec {
  val dutName = implicitly[ClassTag[T]].runtimeClass.getSimpleName
  val dir = new File(s"test-outs/$dutName/PointerChaser") ; dir.mkdirs
  val args = Array("--targetDir", dir.toString)
  val dut = dutName match {
    case "PointerChaser" =>
      val chiselArgs = args ++ Array(
        "--backend", backend, "--v", "--genHarness", "--compile")
      chiselMain(chiselArgs, () => dutGen)
    case _ =>
      StroberCompiler compile (args, dutGen, backend)
  }
  val vcs = backend == "vcs"
  behavior of s"$dutName in $backend"

  import scala.concurrent.duration._
  import ExecutionContext.Implicits.global

  val script = new File("scripts", "generate_memory_init.py")
  val loadmem = new File(dir, "init.hex")
  if (!loadmem.exists) Seq(script.toString, "--output_file", loadmem.toString).!

  val results = (1 to 6) map (math.pow(2, _).toInt) map { latency =>
    val logFile = Some(new File(dir, s"latency-$latency-$backend.log"))
    val waveform = Some(new File(dir, s"latency-$latency.%s".format(if (vcs) "vpd" else "vcd")))
    val testArgs = new PointerChaserArgs(loadmem, logFile, latency)
    Future(latency -> (dut match {
      case _: PointerChaser =>
        val testCmd = new File(dir, s"%s$dutName".format(if (vcs) "" else "V"))
        Driver.run(
          () => dutGen.asInstanceOf[PointerChaser],
          testCmd,
          waveform
        )(m => new PointerChaserTester(m, testArgs))
      case _: SimWrapper[_] =>
        (StroberCompiler test (
          args,
          dutGen.asInstanceOf[SimWrapper[PointerChaser]],
          backend,
          waveform)
        )(m => new PointerChaserSimTester(m, testArgs))
      case _: ZynqShim[_] =>
        (StroberCompiler test(
          args,
          dutGen.asInstanceOf[ZynqShim[SimWrapper[PointerChaser]]],
          backend,
          waveform)
        )(m => new PointerChaserZynqTester(m, testArgs))
    }))
  }
  Await.result(Future.sequence(results), Duration.Inf) foreach {
    case (latency, pass) => it should s"pass latency $latency" in { assert(pass) }
  }
}

/*
class PointerChaserCppTests extends PointerChaserTestSuite(
  new PointerChaser()(chaserParam), "verilator")
class PointerChaserVCSTests extends PointerChaserTestSuite(
  new PointerChaser()(chaserParam), "vcs")
*/
/* class PointerChaserSimCppTests extends PointerChaserTestSuite(
  SimWrapper(new PointerChaser()(chaserParam))(simParam), "verilator") */
/* class PointerChaserSimVCSTests extends PointerChaserTestSuite(
  SimWrapper(new PointerChaser()(chaserParam))(simParam), "vcs") */
/* class PointerChaserZynqCppTests extends PointerChaserTestSuite(
  ZynqShim(new PointerChaser()(chaserParam))(zynqParam), "verilator") */
/* class PointerChaserZynqVCSTests extends PointerChaserTestSuite(
  ZynqShim(new PointerChaser()(chaserParam))(zynqParam), "vcs") */
