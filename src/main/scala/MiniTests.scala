package Designs

import Chisel._
import strober._
import mini._
import TestCommon._
import scala.collection.mutable.{ArrayBuffer, Queue => ScalaQueue}

class CoreStroberTests(c: Strober[Core], args: Array[String]) extends StroberTester(c, false) {
  def runTests(maxcycles: Int, verbose: Boolean) = {
    poke(c.target.io.stall, 1)
    pokeAt(c.target.dpath.regFile.regs, 0, 0)
    step(1)
    poke(c.target.io.stall, 0)
    var prevpc = BigInt(0)
    var tohost = BigInt(0)
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
      tohost = peek(c.target.io.host.tohost)
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

class TileStroberTests(c: Strober[Tile], args: Array[String]) extends StroberTester(c, false, false) {
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
  // slowLoadMem(filename)
  fastLoadMem(filename)
  runTests(maxcycles, verbose)
}

class TileDTests(c: Strober[TileD], args: Array[String]) extends StroberTester(c, false) {
  private var memrw   = false
  private var memtag  = BigInt(0)
  private var memaddr = BigInt(0)
  def serveMem {
    if (peekqValid(c.target.io.mem.req_cmd.bits.rw) && 
        peekqValid(c.target.io.mem.req_cmd.bits.tag) && 
        peekqValid(c.target.io.mem.req_cmd.bits.addr)) {
      memrw   = if (peekq(c.target.io.mem.req_cmd.bits.rw) == 1) true else false
      memtag  = peekq(c.target.io.mem.req_cmd.bits.tag)
      memaddr = peekq(c.target.io.mem.req_cmd.bits.addr)
      step(1)
      // Memread
      if (!memrw) { 
        val read = HexCommon.readMem(memaddr)
        pokeq(c.target.io.mem.resp.bits.data, read)
        pokeq(c.target.io.mem.resp.bits.tag, memtag)
      }
    }
    if (peekqValid(c.target.io.mem.req_data.bits.data) && memrw) {
      val data = peekq(c.target.io.mem.req_data.bits.data)
      HexCommon.writeMem(memaddr, data)
    } 
  }

  def runTests(maxcycles: Int, verbose: Boolean) {
    pokeAt(c.target.core.dpath.regFile.regs, 0, 0)
    var prevpc = BigInt(0)
    var tohost = BigInt(0)
    do {
      serveMem
      step(1)
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
  HexCommon.loadMem(filename)
  runTests(maxcycles, verbose)
}

class TileReplay(c: Tile, args: Array[String]) extends Replay(c, false) {
  private var memcycles = -1
  def tickMem {
    if (memcycles > 0) {
      // In progress
      poke(c.io.mem.req_cmd.ready, 0)
      poke(c.io.mem.req_data.ready, 0)
      poke(c.io.mem.resp.valid, 0)
      memcycles -= 1
    } else if (memcycles < 0) {
      // Ready to have handle mem requests
      if (peek(c.io.mem.req_cmd.valid) == 1) {
        val memrw = (peek(c.io.mem.req_cmd.bits.rw) == 1)
        val memtag = peek(c.io.mem.req_cmd.bits.tag)
        val memaddr = peek(c.io.mem.req_cmd.bits.addr)
        pushMemReq(memaddr, memtag, memrw)
        if (isMemReqRead) poke(c.io.mem.req_cmd.ready, 1)
      }
      if (peek(c.io.mem.req_data.valid) == 1) {
        val data = peek(c.io.mem.req_data.bits.data)
        poke(c.io.mem.req_cmd.ready, 1)
        poke(c.io.mem.req_data.ready, 1)
        HexCommon.writeMem(getMemAddr, data)
      }
      if (hasMemReq) {
        memcycles = if (isMemReqRead) HexCommon.readCycles else HexCommon.writeCycles
      }
    } else {
      // Finish mem requests
      val (addr, tag, memrw) = popMemReq
      if (!memrw) {
        val read = HexCommon.readMem(addr)
        poke(c.io.mem.resp.bits.data, read)
        poke(c.io.mem.resp.bits.tag, tag)
        poke(c.io.mem.resp.valid, 1)
      } 
      memcycles -= 1
    }
    step(1)
    poke(c.io.mem.req_cmd.ready, 0)
    poke(c.io.mem.req_data.ready, 0)
    poke(c.io.mem.resp.valid, 0)
  }

  var sampleIdx = 0
  def runTest(maxcycles: Int, verbose: Boolean) {
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

  override def loadMem(mem: List[(BigInt, BigInt)]) {
    HexCommon.clearMem
    for ((addr, data) <- mem) {
      if (isTrace) println("MEM[%x] <- %08x".format(addr, data))
      HexCommon.writeMem(addr, data, 0xffff)
    }
  }

  override def run {
    t = 0
    memcycles = -1
    val (path, maxcycles, verbose) = HexCommon.parseOpts(args)
    runTest(maxcycles, verbose)
    if (!ok) throw FAILED
  }

  override def begin {
    val (path, maxcycles, verbose) = HexCommon.parseOpts(args)
    val filename = c.name + "." + path.split('/').last.split('.').head + ".sample"
    loadMem(path)
    doTest(filename) 
  }
}
