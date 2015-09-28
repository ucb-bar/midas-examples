package Designs

import Chisel._
import strober._
import mini._

class Tile extends mini.Tile {
  MemReqCmd.ready(io.mem.req_cmd.ready)
  MemReqCmd.valid(io.mem.req_cmd.valid)
  MemReqCmd.addr(io.mem.req_cmd.bits.addr)
  MemReqCmd.tag(io.mem.req_cmd.bits.tag)
  MemReqCmd.rw(io.mem.req_cmd.bits.rw)

  MemData.ready(io.mem.req_data.ready)
  MemData.valid(io.mem.req_data.valid)
  MemData.bits(io.mem.req_data.bits.data)
 
  MemResp.ready(io.mem.resp.ready)
  MemResp.valid(io.mem.resp.valid)
  MemResp.data(io.mem.resp.bits.data)
  MemResp.tag(io.mem.resp.bits.tag)
}

class TileWrapperTests(c: SimWrapper[Tile], args: Array[String]) extends SimWrapperTester(c, false) with MemCommon with TileTests {
  private val blockSize = c.target.bBytes
  private val mem = new MagicMem(blockSize)
  def readMem(addr: Int, s: Int = blockSize) = 
    mem.read(addr, s)
  def writeMem(addr: Int, data: BigInt, mask: BigInt = (1 << blockSize) - 1) = 
    mem.write(addr, data, mask)
  def loadMem(start: Int, test: Seq[UInt]) = 
    mem.loadMem(start, test)
  def loadMem(testname: String) = 
    mem.loadMem(testname)
  val (dir, tests, maxcycles, verbose) = parseOpts(args)
  override def step(n: Int) {
    cycles += n
    super.step(n)
  }
  start(dir, tests, maxcycles, verbose)
  def tick(n: Int, verbose: Boolean) {
    (0 until n) foreach (_ => tick(c.target, verbose))
  }
  def regFile(x: Int) = peekAt(c.target.core.dpath.regFile.regs, x)
  def runTests(maxcycles: Int, verbose: Boolean) = {
    ok &= run(c.target, maxcycles, verbose)
  }
}

class TileAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Tile]], args: Array[String]) extends SimAXI4WrapperTester(c, false) with TileTests {
  def readMem(addr: Int, s: Int = 0) = 
    super[SimAXI4WrapperTester].readMem(addr)
  def writeMem(addr: Int, data: BigInt, mask: BigInt = 0) = 
    super[SimAXI4WrapperTester].writeMem(addr, data)
  override def loadMem(testname: String) = 
    super[SimAXI4WrapperTester].slowLoadMem(testname) 
  def loadMem(start: Int, test: Seq[UInt]) = {
    val nwords = (c.memBlockSize >> 2)
    for (i <- 0 until ((test.size / nwords) + 1)) {
      var data = BigInt(0)
      for (k <- 0 until nwords) {
        val idx = i * nwords + k
        val inst = (if (idx < test.size) test(idx) else UInt(0)).litValue()
        data |= inst << (32 * k)
      }
      writeMem(start + i * c.memBlockSize, data)
    }
  } 
  def tick(n: Int, verbose: Boolean) {
    cycles += n
    step(n)
  }
  def regFile(x: Int) = peekAt(c.sim.target.core.dpath.regFile.regs, x)
  def runTests(maxcycles: Int, verbose: Boolean) = {
    ok &= run(c.sim.target, maxcycles, verbose)
  }
  val (file, tests, maxcycles, verbose) = parseOpts(args)
  start(file, tests, maxcycles, verbose)
}
