package Designs

import Chisel._
import daisy._
import mini._
import TestCommon._

class CoreDaisyTests(c: DaisyShim[Core], args: Array[String]) extends DaisyTester(c, false) {
  def runTests(maxcycles: Int, verbose: Boolean) = {
    poke(c.target.io.stall, 1)
    pokeAt(c.target.dpath.regFile.regs, 0, 0)
    step(1)
    poke(c.target.io.stall, 0)
    var prev_pc = BigInt(0)
    do {
      val iaddr = peek(c.target.io.icache.addr)
      val daddr = (peek(c.target.io.dcache.addr) >> 2) << 2
      val data  = peek(c.target.io.dcache.din)
      val dwe   = peek(c.target.io.dcache.we)
      val ire   = peek(c.target.io.icache.re) == 1
      val dre   = peek(c.target.io.dcache.re) == 1

      step(1)

      if (dwe > 0) {
        HexCommon.writeMem(daddr, data, dwe)
      } else if (ire) {
        val inst = HexCommon.readMem(iaddr)
        poke(c.target.io.icache.dout, inst)
      } else if (dre) {
        val data = HexCommon.readMem(daddr)
        poke(c.target.io.dcache.dout, data)
      }

      val pc = peek(c.target.dpath.ew_pc)
      if (verbose && pc != prev_pc) {
        val inst   = UInt(peek(c.target.dpath.ew_inst), 32)
        val wb_en  = peek(c.target.ctrl.io.ctrl.wb_en)
        val wb_val = 
          if (wb_en == 1) peek(c.target.dpath.regWrite) 
          else peekAt(c.target.dpath.regFile.regs, rd(inst)) 
        println("[%x] %s -> RegFile[%d] = %x".format(
                pc, instStr(inst), rd(inst), wb_val))
        prev_pc = pc
      }
    } while (peek(c.target.io.host.tohost) == 0 && t < maxcycles)
    val tohost = peek(c.target.io.host.tohost)
    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    ok &= tohost == 1
    println("*** %s *** (%s) after %d simulation cycles".format(
            if (ok) "PASSED" else "FAILED", reason, t))
  }

  val (filename, maxcycles, verbose) = HexCommon.parseOpts(args)
  HexCommon.loadMem(filename)
  runTests(maxcycles, verbose)
}

class TileDaisyTests(c: DaisyShim[Tile], args: Array[String]) extends DaisyTester(c, false, false) {
  def runTests(maxcycles: Int, verbose: Boolean) {
    pokeAt(c.target.core.dpath.regFile.regs, 0, 0)
    var prev_pc = BigInt(0)
    do {
      step(10)
      val pc = peek(c.target.core.dpath.ew_pc)
      if (verbose && pc != prev_pc) {
        val inst   = UInt(peek(c.target.core.dpath.ew_inst), 32)
        val wb_en  = peek(c.target.core.ctrl.io.ctrl.wb_en)
        val wb_val = 
          if (wb_en == 1) peek(c.target.core.dpath.regWrite) 
          else peekAt(c.target.core.dpath.regFile.regs, rd(inst)) 
        println("[%x] %s -> RegFile[%d] = %x".format(
                pc, instStr(inst), rd(inst), wb_val))
        prev_pc = pc
      }
    } while (peek(c.target.io.htif.host.tohost) == 0 && t < maxcycles)

    val tohost = peek(c.target.io.htif.host.tohost)
    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    ok &= tohost == 1
    println("*** %s *** (%s) after %d simulation cycles".format(
            if (ok) "PASSED" else "FAILED", reason, t))
  }

  val (filename, maxcycles, verbose) = HexCommon.parseOpts(args)
  // slowLoadMem(filename)
  fastLoadMem(filename)
  runTests(maxcycles, verbose)
}

class TileReplay(c: Tile, args: Array[String]) extends DaisyReplay(c, false) {
  private var memrw   = false
  private var memtag  = BigInt(0)
  private var memaddr = BigInt(0)
  private var memcycles = -1
  def tickMem {
    if (memcycles > 0) {
      poke(c.io.mem.req_cmd.ready, 0)
      poke(c.io.mem.req_data.ready, 0)
      poke(c.io.mem.resp.valid, 0)
      memcycles -= 1
    } else if (memcycles < 0) {
      if (peek(c.io.mem.req_cmd.valid) == 1) {
        memrw   = if (peek(c.io.mem.req_cmd.bits.rw) == 1) true else false
        memtag  = peek(c.io.mem.req_cmd.bits.tag)
        memaddr = peek(c.io.mem.req_cmd.bits.addr)
        // Memread
        if (!memrw) { 
          memcycles = 2
          poke(c.io.mem.req_cmd.ready, 1)
        }
      }
      if (peek(c.io.mem.req_data.valid) == 1) {
        val data = peek(c.io.mem.req_data.bits.data)
        poke(c.io.mem.req_cmd.ready, 1)
        poke(c.io.mem.req_data.ready, 1)
        HexCommon.writeMem(memaddr, data)
        memcycles = 1
      }
    } else {
      if (!memrw) {
        val read = HexCommon.readMem(memaddr)
        poke(c.io.mem.resp.bits.data, read)
        poke(c.io.mem.resp.bits.tag, memtag)
        poke(c.io.mem.resp.valid, 1)
      }
      memcycles -= 1
    }
    step(1)
    poke(c.io.mem.req_cmd.ready, 0)
    poke(c.io.mem.req_data.ready, 0)
    poke(c.io.mem.resp.valid, 0)
  }

  def runTest(maxcycles: Int, verbose: Boolean) {
    pokeAt(c.core.dpath.regFile.regs, 0, 0)
    var prev_pc = BigInt(0)
    do {
      tickMem
      val pc     = peek(c.core.dpath.ew_pc)
      if (verbose && pc != prev_pc) {
        val inst   = UInt(peek(c.core.dpath.ew_inst), 32)
        val wb_en  = peek(c.core.ctrl.io.ctrl.wb_en)
        val wb_val = 
        if (wb_en == 1) peek(c.core.dpath.regWrite) 
        else peekAt(c.core.dpath.regFile.regs, rd(inst))
        println("[%x] %s -> RegFile[%d] = %x".format(
                pc, instStr(inst), rd(inst), wb_val))
        prev_pc = pc
      }
    } while (peek(c.io.htif.host.tohost) == 0 && t < maxcycles)

    val tohost = peek(c.io.htif.host.tohost)
    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    ok &= tohost == 1
    println("*** %s *** (%s) after %d simulation cycles".format(
            if (ok) "PASSED" else "FAILED", reason, t))
  }

  override def read(addr: BigInt, tag: BigInt) {
    if (isTrace) println("READ %x, %x".format(addr, tag))
    memtag  = tag
    memaddr = addr
    memcycles = 10
  }

  override def loadMem(mem: List[(BigInt, BigInt)]) {
    HexCommon.clearMem
    for ((addr, data) <- mem) {
      if (isTrace) println("MEM[%x] <- %08x".format(addr, data))
      HexCommon.writeMem(addr, data)
    }
  }

  override def run {
    t = 0
    val (filename, maxcycles, verbose) = HexCommon.parseOpts(args)
    runTest(maxcycles, verbose)
    if (!ok) throw FAILED
  }
}
