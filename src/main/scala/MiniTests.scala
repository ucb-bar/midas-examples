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

class TileWrapperTests(c: SimWrapper[Tile], args: Array[String]) extends SimWrapperTester(c, false) {
  def tickMem {
    val rw = peekPort(c.target.io.mem.req_cmd.bits.rw) == 1 
    val tag = peekPort(c.target.io.mem.req_cmd.bits.tag)
    val addr = peekPort(c.target.io.mem.req_cmd.bits.addr) << 2
    val mask = peekPort(c.target.io.mem.req_cmd.bits.mask)
    val data = peekPort(c.target.io.mem.req_data.bits.data)
    val cmd_val = peekPort(c.target.io.mem.req_cmd.valid) == 1
    val data_val = peekPort(c.target.io.mem.req_data.valid) == 1
    val resp_rdy = peekPort(c.target.io.mem.resp.ready) == 1
 
    if (!rw && cmd_val && resp_rdy) {
      val data = HexCommon.readMem(addr)
      pokePort(c.target.io.mem.req_cmd.ready, 1)
      pokePort(c.target.io.mem.resp.valid, 1)  
      pokePort(c.target.io.mem.resp.bits.data, data)
      pokePort(c.target.io.mem.resp.bits.tag, tag)
    } else if (rw && cmd_val && data_val) {
      val data = peekPort(c.target.io.mem.req_data.bits.data)
      HexCommon.writeMem(addr, data, mask)
      pokePort(c.target.io.mem.req_cmd.ready, 1)
      pokePort(c.target.io.mem.req_data.ready, 1)
    } 

    step(1)

    pokePort(c.target.io.mem.req_cmd.ready, 0)
    pokePort(c.target.io.mem.req_data.ready, 0)
    pokePort(c.target.io.mem.resp.valid, 0)  
  }

  def runTests(maxcycles: Int, verbose: Boolean) {
    var prevpc = BigInt(0)
    var tohost = BigInt(0)
    pokeAt(c.target.core.dpath.regFile.regs, 0, 0)
    do {
      tickMem
      val pc = peek(c.target.core.dpath.ew_pc)
      if (verbose && pc != prevpc) {
        val inst   = UInt(peek(c.target.core.dpath.ew_inst), 32)
        val wb_en  = peek(c.target.core.ctrl.io.ctrl.wb_en)
        val wb_val = 
          if (wb_en == 1) peek(c.target.core.dpath.regWrite) 
          else peekAt(c.target.core.dpath.regFile.regs, rd(inst)) 
        println("[%h] %s -> RegFile[%d] = %h".format(
                pc, instStr(inst), rd(inst), wb_val))
        prevpc = pc
      }
      tohost = peekPort(c.target.io.htif.host.tohost) 
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

class TileAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Tile]], args: Array[String]) 
  extends SimAXI4WrapperTester(c, false) {

  def runTests(maxcycles: Int, verbose: Boolean) {
    var prevpc = BigInt(0)
    var tohost = BigInt(0)
    pokeAt(c.sim.target.core.dpath.regFile.regs, 0, 0)
    do {
      step(1)
      val pc = peek(c.sim.target.core.dpath.ew_pc)
      if (verbose && pc != prevpc) {
        val inst   = UInt(peek(c.sim.target.core.dpath.ew_inst), 32)
        val wb_en  = peek(c.sim.target.core.ctrl.io.ctrl.wb_en)
        val wb_val = 
          if (wb_en == 1) peek(c.sim.target.core.dpath.regWrite) 
          else peekAt(c.sim.target.core.dpath.regFile.regs, rd(inst)) 
        println("[%h] %s -> RegFile[%d] = %h".format(
                pc, instStr(inst), rd(inst), wb_val))
        prevpc = pc
      }
      tohost = peekPort(c.sim.target.io.htif.host.tohost) 
    } while (tohost == 0 && t < maxcycles)
    val reason = if (t < maxcycles) "tohost = " + tohost else "timeout"
    ok &= tohost == 1
    println("*** %s *** (%s) after %d simulation cycles".format(
            if (ok) "PASSED" else "FAILED", reason, t))
  }

  val (filename, maxcycles, verbose) = HexCommon.parseOpts(args)
  // loadMem(filename)
  slowLoadMem(filename)
  runTests(maxcycles, verbose)
}
