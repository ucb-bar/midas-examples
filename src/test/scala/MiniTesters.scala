package StroberExamples

import junctions._
import midas_widgets._
import dram_midas.MidasMemModel
import strober.{SimWrapper, ZynqShim}
import strober.testers.{SimWrapperTester, ZynqShimTester}
import mini.{Tile, MiniTests, MiniTestArgs}
import java.io.File

class TileSimTests(c: SimWrapper[Tile], args: MiniTestArgs, sample: Option[File] = None)
    extends SimWrapperTester(c, false, sample, args.logFile) with MiniTests {
  val arHandler = new DecoupledSink(c.target.io.nasti.ar, (ar: NastiReadAddressChannel) =>
    new mini.TestNastiReadAddr(peek(ar.id), peek(ar.addr), peek(ar.size), peek(ar.len)))
  val awHandler = new DecoupledSink(c.target.io.nasti.aw, (aw: NastiWriteAddressChannel) =>
    new mini.TestNastiWriteAddr(peek(aw.id), peek(aw.addr), peek(aw.size), peek(aw.len)))
  val wHandler = new DecoupledSink(c.target.io.nasti.w, (w: NastiWriteDataChannel) =>
    new mini.TestNastiWriteData(peek(w.data), peek(w.last) != BigInt(0)))
  val rHandler = new DecoupledSource(c.target.io.nasti.r,
    (r: NastiReadDataChannel, in: mini.TestNastiReadData) =>
      {reg_poke(r.id, in.id) ; reg_poke(r.data, in.data) ; reg_poke(r.last, in.last)})
  val nasti = new mini.NastiMem(
    arHandler.outputs, rHandler.inputs,
    awHandler.outputs, wHandler.outputs,
    args.memlatency, c.target.icache.nastiXDataBits/8, verbose=args.verbose)

  nasti loadMem args.loadmem
  preprocessors += nasti

  setTraceLen(128)
  if (!run(c.target.io.host, args.maxcycles)) fail
}

class TileZynqTests(c: ZynqShim[SimWrapper[Tile]], args: MiniTestArgs, sample: Option[File] = None)
    extends ZynqShimTester(c, false, sample, args.logFile) with MiniTests {
  setTraceLen(128)

  c.widgets foreach {
    case w: MidasMemModel =>
      writeCR(w, "writeMaxReqs", 8)
      writeCR(w, "writeLatency", args.memlatency)
      writeCR(w, "readMaxReqs", 8)
      writeCR(w, "readLatency", args.memlatency)
    case w: SimpleLatencyPipe =>
      writeCR(w, "LATENCY", args.memlatency)
    case _ =>
  }

  loadMem(args.loadmem)

  var tohost = BigInt(0)
  val startTime = System.nanoTime
  do {
    step(traceLen)
    tohost = peek(c.sim.target.io.host.tohost)
  } while (tohost == 0 && t < args.maxcycles)
  val endTime = System.nanoTime
  val simTime = (endTime - startTime) / 1000000000.0
  val simSpeed = cycles / simTime
  val reason = if (cycles < args.maxcycles) s"tohost = ${tohost}" else "timeout"
  expect(tohost == 1 && cycles < args.maxcycles, 
    s"*** ${reason} *** after ${cycles} simulation cycles:")
  println("Time elapsed = %.1f s, Simulation Speed = %.2f Hz".format(simTime, simSpeed))
}
