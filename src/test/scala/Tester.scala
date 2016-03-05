package StroberExamples

import strober._
import junctions._
import mini._
import TutorialExamples._

class GCDSimTests(c: SimWrapper[GCD], 
    args: StroberTestArgs) extends SimWrapperTester(c, args) with GCDTests {
  tests(c.target)
}
class ParitySimTests(c: SimWrapper[Parity], 
    args: StroberTestArgs) extends SimWrapperTester(c, args) with ParityTests {
  tests(c.target)
}
class ShiftRegisterSimTests(c: SimWrapper[ShiftRegister], 
    args: StroberTestArgs) extends SimWrapperTester(c, args) with ShiftRegisterTests {
  tests(c.target)
}
class ResetShiftRegisterSimTests(c: SimWrapper[ResetShiftRegister],
    args: StroberTestArgs) extends SimWrapperTester(c, args) with ResetShiftRegisterTests {
  tests(c.target)
}
class EnableShiftRegisterSimTests(c: SimWrapper[EnableShiftRegister],
    args: StroberTestArgs) extends SimWrapperTester(c, args) with EnableShiftRegisterTests {
  tests(c.target)
}
class MemorySearchSimTests(c: SimWrapper[MemorySearch],
    args: StroberTestArgs) extends SimWrapperTester(c, args) with MemorySearchTests {
  tests(c.target)
}
class StackSimTests(c: SimWrapper[Stack],
    args: StroberTestArgs) extends SimWrapperTester(c, args) with StackTests {
  tests(c.target)
}
class RouterSimTests(c: SimWrapper[Router],
    args: StroberTestArgs)  extends SimWrapperTester(c, args) with RouterTests {
  tests(c.target)
}
class RiscSimTests(c: SimWrapper[Risc],
    args: StroberTestArgs) extends SimWrapperTester(c, args) with RiscTests {
  tests(c.target)
}
class RiscSRAMSimTests(c: SimWrapper[RiscSRAM],
    args: StroberTestArgs) extends SimWrapperTester(c, args) with RiscSRAMTests {
  tests(c.target)
}
class GCDNastiShimTests(c: NastiShim[SimWrapper[GCD]], 
    args: StroberTestArgs) extends NastiShimTester(c, args) with GCDTests {
  tests(c.sim.target)
}
class ParityNastiShimTests(c: NastiShim[SimWrapper[Parity]], 
    args: StroberTestArgs) extends NastiShimTester(c, args) with ParityTests {
  tests(c.sim.target)
}
class ShiftRegisterNastiShimTests(c: NastiShim[SimWrapper[ShiftRegister]], 
    args: StroberTestArgs) extends NastiShimTester(c, args) with ShiftRegisterTests {
  tests(c.sim.target)
}
class ResetShiftRegisterNastiShimTests(c: NastiShim[SimWrapper[ResetShiftRegister]],
    args: StroberTestArgs) extends NastiShimTester(c, args) with ResetShiftRegisterTests {
  tests(c.sim.target)
}
class EnableShiftRegisterNastiShimTests(c: NastiShim[SimWrapper[EnableShiftRegister]],
    args: StroberTestArgs) extends NastiShimTester(c, args) with EnableShiftRegisterTests {
  tests(c.sim.target)
}
class MemorySearchNastiShimTests(c: NastiShim[SimWrapper[MemorySearch]],
    args: StroberTestArgs) extends NastiShimTester(c, args) with MemorySearchTests {
  tests(c.sim.target)
}
class StackNastiShimTests(c: NastiShim[SimWrapper[Stack]],
    args: StroberTestArgs) extends NastiShimTester(c, args) with StackTests {
  tests(c.sim.target)
}
class RouterNastiShimTests(c: NastiShim[SimWrapper[Router]],
    args: StroberTestArgs)  extends NastiShimTester(c, args) with RouterTests {
  tests(c.sim.target)
}
class RiscNastiShimTests(c: NastiShim[SimWrapper[Risc]],
    args: StroberTestArgs) extends NastiShimTester(c, args) with RiscTests {
  tests(c.sim.target)
}
class RiscSRAMNastiShimTests(c: NastiShim[SimWrapper[RiscSRAM]],
    args: StroberTestArgs) extends NastiShimTester(c, args) with RiscSRAMTests {
  tests(c.sim.target)
}

class TileSimTests(c: SimWrapper[Tile], sampleFile: Option[String], args: MiniTestArgs) 
    extends SimWrapperTester(c, new StroberTestArgs(
      sampleFile, args.dumpFile, args.logFile, args.testCmd, args.verbose)) with MiniTests {
  lazy val cmdHandler = new DecoupledSink(c.target.io.mem.req_cmd,
    (cmd: MemReqCmd) => new TestMemReq(peek(cmd.addr).toInt, peek(cmd.tag), peek(cmd.rw) != 0))
  lazy val dataHandler = new DecoupledSink(c.target.io.mem.req_data,
    (data: MemData) => new TestMemData(peek(data.data)))
  lazy val respHandler = new DecoupledSource(c.target.io.mem.resp,
    (resp: MemResp, in: TestMemResp) => {reg_poke(resp.data, in.data) ; reg_poke(resp.tag, in.tag)})
  lazy val mem = new TileMem(
    cmdHandler.outputs, dataHandler.outputs, respHandler.inputs, 
    if (args.verbose) Some(log) else None, 5, c.target.icache.bBytes)

  lazy val arHandler = new DecoupledSink(c.target.io.nasti.ar, (ar: NastiReadAddressChannel) =>
    new TestNastiReadAddr(peek(ar.id), peek(ar.addr), peek(ar.size), peek(ar.len)))
  lazy val awHandler = new DecoupledSink(c.target.io.nasti.aw, (aw: NastiWriteAddressChannel) =>
    new TestNastiWriteAddr(peek(aw.id), peek(aw.addr), peek(aw.size), peek(aw.len)))
  lazy val wHandler = new DecoupledSink(c.target.io.nasti.w, (w: NastiWriteDataChannel) =>
    new TestNastiWriteData(peek(w.data), peek(w.last) != BigInt(0)))
  lazy val rHandler = new DecoupledSource(c.target.io.nasti.r,
    (r: NastiReadDataChannel, in: TestNastiReadData) =>
      {reg_poke(r.id, in.id) ; reg_poke(r.data, in.data) ; reg_poke(r.last, in.last)})
  lazy val nasti = new NastiMem(
    arHandler.outputs, rHandler.inputs,
    awHandler.outputs, wHandler.outputs,
    if (args.verbose) Some(log) else None, 5, c.target.icache.nBytes)

  if (c.target.core.useNasti) {
    nasti loadMem args.loadmem
    preprocessors += nasti
    arHandler.process()
    awHandler.process()
    rHandler.process()
    wHandler.process()
  } else {
    mem loadMem args.loadmem
    preprocessors += mem
    cmdHandler.process()
    dataHandler.process()
    respHandler.process()
  }
  setTraceLen(16)
  if (!run(c.target.io.htif.host, args.maxcycles, Some(log))) fail
}

class TileNastiShimTests(c: NastiShim[SimWrapper[Tile]], sampleFile: Option[String], 
    args: MiniTestArgs) extends NastiShimTester(c, new StroberTestArgs(
      sampleFile, args.dumpFile, args.logFile, args.testCmd, args.verbose)) with MiniTests {
  setTraceLen(16)
  setMemCycles(5)
  loadMem(args.loadmem)

  var tohost = BigInt(0)
  val startTime = System.nanoTime
  do {
    step(traceLen)
    tohost = peek(c.sim.target.io.htif.host.tohost)
  } while (tohost == 0 && t < args.maxcycles)
  val endTime = System.nanoTime
  val simTime = (endTime - startTime) / 1000000000.0
  val simSpeed = cycles / simTime
  val reason = if (cycles < args.maxcycles) s"tohost = ${tohost}" else "timeout"
  expect(tohost == 1 && cycles < args.maxcycles, 
    s"*** ${reason} *** after ${cycles} simulation cycles:")
  log.println("Time elapsed = %.1f s, Simulation Speed = %.2f Hz".format(simTime, simSpeed))
}
