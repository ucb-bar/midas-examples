package Designs

import Chisel._
import strober._
import mini._
import TestCommon._
import scala.collection.mutable.{ArrayBuffer, Queue => ScalaQueue}

class CoreWrapperTests(c: SimWrapper[Core], args: Array[String]) extends SimWrapperTester(c, false) {
  def runTests(maxcycles: Int, verbose: Boolean) = {
    pokeAt(c.target.dpath.regFile.regs, 0, 0)
    pokePort(c.target.io.stall, 0)
    var prevpc = BigInt(0)
    var tohost = BigInt(0)
    do {
      val iaddr = peekPort(c.target.io.icache.addr)
      val daddr = (peekPort(c.target.io.dcache.addr) >> 2) << 2
      val data  = peekPort(c.target.io.dcache.din)
      val dwe   = peekPort(c.target.io.dcache.we)
      val ire   = peekPort(c.target.io.icache.re) == 1
      val dre   = peekPort(c.target.io.dcache.re) == 1

      step(1)

      if (dwe > 0) {
        HexCommon.writeMem(daddr, data, dwe)
      } else if (ire) {
        val inst = HexCommon.readMem(iaddr)
        pokePort(c.target.io.icache.dout, inst)
      } else if (dre) {
        val data = HexCommon.readMem(daddr)
        pokePort(c.target.io.dcache.dout, data)
      }

      val pc = peek(c.target.dpath.ew_pc)
      if (verbose && pc != prevpc) {
        val inst   = UInt(peek(c.target.dpath.ew_inst), 32)
        val wb_en  = peek(c.target.ctrl.io.ctrl.wb_en)
        val wb_val = 
          if (wb_en == 1) peek(c.target.dpath.regWrite) 
          else peekAt(c.target.dpath.regFile.regs, rd(inst)) 
        println("[%x] %s -> RegFile[%d] = %x".format(
                pc, instStr(inst), rd(inst), wb_val))
        prevpc = pc
      }
      tohost = peekPort(c.target.io.host.tohost)
    } while (tohost == 0 && t < maxcycles)

    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    ok &= tohost == 1
    println("*** %s *** (%s) after %d simulation cycles".format(
            if (ok) "PASSED" else "FAILED", reason, t))
  }

  val (filename, maxcycles, verbose) = HexCommon.parseOpts(args)
  HexCommon.loadMem(filename)
  runTests(maxcycles, verbose)
}

class CoreAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Core]], args: Array[String]) 
  extends SimAXI4WrapperTester(c, false) {
  def runTests(maxcycles: Int, verbose: Boolean) = {
    pokeAt(c.sim.target.dpath.regFile.regs, 0, 0)
    pokePort(c.sim.target.io.stall, 0)
    var prevpc = BigInt(0)
    var tohost = BigInt(0)
    do {
      val iaddr = peekPort(c.sim.target.io.icache.addr)
      val daddr = (peekPort(c.sim.target.io.dcache.addr) >> 2) << 2
      val data  = peekPort(c.sim.target.io.dcache.din)
      val dwe   = peekPort(c.sim.target.io.dcache.we)
      val ire   = peekPort(c.sim.target.io.icache.re) == 1
      val dre   = peekPort(c.sim.target.io.dcache.re) == 1

      step(1)

      if (dwe > 0) {
        HexCommon.writeMem(daddr, data, dwe)
      } else if (ire) {
        val inst = HexCommon.readMem(iaddr)
        pokePort(c.sim.target.io.icache.dout, inst)
      } else if (dre) {
        val data = HexCommon.readMem(daddr)
        pokePort(c.sim.target.io.dcache.dout, data)
      }

      val pc = peek(c.sim.target.dpath.ew_pc)
      if (verbose && pc != prevpc) {
        val inst   = UInt(peek(c.sim.target.dpath.ew_inst), 32)
        val wb_en  = peek(c.sim.target.ctrl.io.ctrl.wb_en)
        val wb_val = 
          if (wb_en == 1) peek(c.sim.target.dpath.regWrite) 
          else peekAt(c.sim.target.dpath.regFile.regs, rd(inst)) 
        println("[%x] %s -> RegFile[%d] = %x".format(
                pc, instStr(inst), rd(inst), wb_val))
        prevpc = pc
      }
      tohost = peekPort(c.sim.target.io.host.tohost)
    } while (tohost == 0 && t < maxcycles)

    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    ok &= tohost == 1
    println("*** %s *** (%s) after %d simulation cycles".format(
            if (ok) "PASSED" else "FAILED", reason, t))
  }

  val (filename, maxcycles, verbose) = HexCommon.parseOpts(args)
  HexCommon.loadMem(filename)
  runTests(maxcycles, verbose)
}

/*
class TileWrapperTests(c: SimWrapper[Tile], args: Array[String]) extends SimWrapperTester(c, false, false) {
  stepSize = 10
  def runTests(maxcycles: Int, verbose: Boolean) {
    pokeAt(c.target.core.dpath.regFile.regs, 0, 0)
    var prevpc = BigInt(0)
    var tohost = BigInt(0)
    do {
      step(stepSize)
      val pc = peek(c.target.core.dpath.ew_pc)
      if (verbose && pc != prevpc) {
        val inst   = UInt(peek(c.target.core.dpath.ew_inst), 32)
        val wb_en  = peek(c.target.core.ctrl.io.ctrl.wb_en)
        val wb_val = 
          if (wb_en == 1) peek(c.target.core.dpath.regWrite) 
          else peekAt(c.target.core.dpath.regFile.regs, rd(inst)) 
        println("[%x] %s -> RegFile[%d] = %x".format(
                pc, instStr(inst), rd(inst), wb_val))
        prevpc = pc
      }
      tohost = peek(c.target.io.htif.host.tohost)
    } while (tohost == 0 && t < maxcycles)

    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    ok &= tohost == 1
    println("*** %s *** (%s) after %d simulation cycles".format(
            if (ok) "PASSED" else "FAILED", reason, t))
  }

  val (filename, maxcycles, verbose) = HexCommon.parseOpts(args)
  prefix = "." + filename.split('/').last.split('.').head
  HexCommon.loadMem(filename)
  runTests(maxcycles, verbose)
}

class TileReplay(c: Tile, args: Array[String]) extends Replay(c, false) {
  def tickMem {
    if (peek(c.io.mem.req_cmd.valid) == 1) {
      mem.rw enqueue (peek(c.io.mem.req_cmd.bits.rw) == 1)
      mem.tag enqueue peek(c.io.mem.req_cmd.bits.tag).toInt
      mem.addr enqueue peek(c.io.mem.req_cmd.bits.addr).toInt
      if (!mem.rw.head) {
        poke(c.io.mem.req_cmd.ready, 1)
      } else {
        poke(c.io.mem.req_cmd.ready, 0)
      }
      poke(c.io.mem.req_data.ready, 0)
      poke(c.io.mem.resp.valid, 0)
    }
    if (peek(c.io.mem.req_data.valid) == 1) {
      val data = peek(c.io.mem.req_data.bits.data)
      poke(c.io.mem.req_cmd.ready, 1)
      poke(c.io.mem.req_data.ready, 1)
      poke(c.io.mem.resp.valid, 0)
      // mem.data enqueue peek(c.io.mem.req_data.bits.data)
      mem.write(mem.addr.head, data)
    }
    if (mem.cycles == 0) {
      val addr = mem.addr.dequeue
      val tag = mem.tag.dequeue
      if (!mem.rw.dequeue) {
        val read = mem.read(addr)
//println("[READ] addr: %x, data: %x".format(addr, read))
        poke(c.io.mem.resp.bits.data, read)
        poke(c.io.mem.resp.bits.tag, tag)
        poke(c.io.mem.resp.valid, 1)
      } else {
        // mem.write(addr, mem.data.dequeue)
      }
    }
    mem.tick
    step(1)
    poke(c.io.mem.req_cmd.ready, 0)
    poke(c.io.mem.req_data.ready, 0)
    poke(c.io.mem.resp.valid, 0)
  }

  var sampleIdx = 0
  def runTest(args: Seq[Any]) {
    val maxcycles = args(0) match { case int: Int => int }
    val verbose  = args(1) match { case bool: Boolean => bool }
    pokeAt(c.core.dpath.regFile.regs, 0, 0)
    var prevpc = BigInt(0)
    var tohost = BigInt(0)
    do {
      tickMem
      val pc = peek(c.core.dpath.ew_pc)
      if (verbose && pc != prevpc) {
        val inst   = UInt(peek(c.core.dpath.ew_inst), 32)
        val wb_en  = peek(c.core.ctrl.io.ctrl.wb_en)
        val wb_val = 
          if (wb_en == 1) peek(c.core.dpath.regWrite) 
          else peekAt(c.core.dpath.regFile.regs, rd(inst))
        println("[%x] %s -> RegFile[%d] = %x".format(
                pc, instStr(inst), rd(inst), wb_val))
        prevpc = pc
      }
      tohost = peek(c.io.htif.host.tohost)
    } while (tohost == 0 && t < maxcycles)

    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    ok &= tohost == 1
    println("Sample %d -> *** %s *** (%s) after %d simulation cycles".format(
            sampleIdx, if (ok) "PASSED" else "FAILED", reason, t))
    sampleIdx += 1
  }

  override def run(args: Seq[Any]) {
    t = 0
    runTest(args)
    if (!ok) throw FAILED
  }

  override def begin {
    val (path, maxcycles, verbose) = HexCommon.parseOpts(args)
    val prefix = c.name + "." + path.split('/').last.split('.').head
    doTest(prefix, maxcycles, verbose) 
  }
}
*/
