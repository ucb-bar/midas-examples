package Designs

import Chisel._
import strober._
import mini._
import junctions.{MemReqCmd, MemData, MemResp}

class Tile extends mini.Tile {
  SimMemIO(io.mem)
}

class TileSimTests(c: SimWrapper[Tile], args: Array[String], snapCheck: Boolean) extends SimWrapperTester(c, false, snapCheck) with MemTests {
  val cmdHandler = new DecoupledSink(c.target.io.mem.req_cmd,
    (cmd: MemReqCmd) => new TestMemReq(peek(cmd.addr).toInt, peek(cmd.tag), peek(cmd.rw) != 0))
  val dataHandler = new DecoupledSink(c.target.io.mem.req_data,
    (data: MemData) => new TestMemData(peek(data.data)))
  val respHandler = new DecoupledSource(c.target.io.mem.resp,
    (resp: MemResp, in: TestMemResp) => {reg_poke(resp.data, in.data) ; reg_poke(resp.tag, in.tag)})
  val mem = new TileMem(cmdHandler.outputs, dataHandler.outputs, respHandler.inputs, 16)
  preprocessors += mem
  cmdHandler.process()
  dataHandler.process()
  respHandler.process()
  def regFile(x: Int) = peekAt(c.target.core.dpath.regFile.regs, x)
  def loadMem(testname: String) = mem.loadMem(testname)
  def loadMem(test: Seq[UInt]) = mem.loadMem(test)
  def runTests(maxcycles: Int, verbose: Boolean) {
    cycles = 0
    ok &= run(c.target.io.htif.host, maxcycles, verbose)
  }
  val (file, tests, maxcycles, verbose) = parseOpts(args)
  start(file, tests, maxcycles, verbose)
}

class TileNASTIShimTests(c: NASTIShim[SimWrapper[Tile]], args: Array[String], snapCheck: Boolean) extends NASTIShimTester(c, false, snapCheck) with MemTests {
  def read(addr: Int) = 
    super[NASTIShimTester].readMem(addr)
  def write(addr: Int, data: BigInt) = 
    super[NASTIShimTester].writeMem(addr, data)
  override def loadMem(testname: String) = 
    super[NASTIShimTester].slowLoadMem(testname) 
  def loadMem(test: Seq[UInt]) = { /* do nothing ... */ }
  override def run(host: HostIO, maxcycles: Int, verbose: Boolean) = {
    var tohost = BigInt(0)
    val startTime = System.nanoTime
    do {
      step(traceLen)
      tohost = peek(host.tohost)
    } while (tohost == 0 && t < maxcycles)
    val endTime = System.nanoTime
    val simTime = (endTime - startTime) / 1000000000.0
    val simSpeed = t / simTime
    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    val ok = tohost == 1
    println("*** %s *** (%s) after %d simulation cycles".format(
            if (ok) "PASSED" else "FAILED", reason, t))
    println("Time elapsed = %.1f s, Simulation Speed = %.2f Hz".format(simTime, simSpeed))
    ok
  }
  def regFile(x: Int) = peekAt(c.sim.target.core.dpath.regFile.regs, x)
  def runTests(maxcycles: Int, verbose: Boolean) = {
    ok &= run(c.sim.target.io.htif.host, maxcycles, verbose)
  }
  val (file, tests, maxcycles, verbose) = parseOpts(args)
  start(file, tests, maxcycles, verbose)
}
