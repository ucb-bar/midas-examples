package StroberExamples

import strober.{SimWrapper, ZynqShim}
import strober.testers.{SimWrapperTester, ZynqShimTester, FastLoadMem}
import mini.{Tile, MiniTests, MiniTestArgs}
import sys.process.stringSeqToProcess
import java.io.File
import collection.mutable.{Queue => ScalaQueue}

import junctions._
import chisel3.util._
import chisel3.iotesters.{AdvTester, AdvTests}

case class PointerChaserArgs(
  loadmem: File,
  logFile: Option[File],
  memlatency: Int = 5,
  maxcycles: Long = 10000L,
  addr: BigInt = 64,
  result: BigInt = 1176)

trait PointerChaserTests extends AdvTests {
  implicit def bigIntToInt(x: BigInt) = x.toInt

  def run(
          addrQueue: ScalaQueue[BigInt],
          addr: BigInt,
          resultQueue: ScalaQueue[BigInt],
          result: BigInt,
          maxcycles: Long): Boolean = {
    addrQueue enqueue addr
    eventually(resultQueue.nonEmpty, maxcycles)
    if (resultQueue.isEmpty) {
      println(s"No return from pointer chaser")
      false
    } else {
      val ret = resultQueue.dequeue
      println(s"Pointer chaser returns $ret, expected ${result}")
      ret == result
    }
  }
}

class PointerChaserTester(c: PointerChaser, args: PointerChaserArgs)
    extends AdvTester(c, false, logFile = args.logFile) with PointerChaserTests {
  val startAddrHandler = DecoupledSource(c.io.startAddr)
  val resultHandler = DecoupledSink(c.io.result)

  val arHandler = new DecoupledSink(c.io.nasti.ar, (ar: NastiReadAddressChannel) =>
    new mini.TestNastiReadAddr(peek(ar.id), peek(ar.addr), peek(ar.size), peek(ar.len)))
  val awHandler = new DecoupledSink(c.io.nasti.aw, (aw: NastiWriteAddressChannel) =>
    new mini.TestNastiWriteAddr(peek(aw.id), peek(aw.addr), peek(aw.size), peek(aw.len)))
  val wHandler = new DecoupledSink(c.io.nasti.w, (w: NastiWriteDataChannel) =>
    new mini.TestNastiWriteData(peek(w.data), peek(w.last) != BigInt(0)))
  val rHandler = new DecoupledSource(c.io.nasti.r,
    (r: NastiReadDataChannel, in: mini.TestNastiReadData) =>
      {reg_poke(r.id, in.id) ; reg_poke(r.data, in.data) ; reg_poke(r.last, in.last)})

  val nasti = new mini.NastiMem(
    arHandler.outputs, rHandler.inputs,
    awHandler.outputs, wHandler.outputs,
    args.memlatency, c.nastiXDataBits/8, verbose=true)
 
  nasti loadMem args.loadmem
  preprocessors += nasti
  
  if (!run(startAddrHandler.inputs, args.addr,
           resultHandler.outputs, args.result, args.maxcycles)) fail
}

class PointerChaserSimTester(c: SimWrapper[PointerChaser], args: PointerChaserArgs)
    extends SimWrapperTester(c, false, None, args.logFile) with PointerChaserTests {
  val startAddrHandler = DecoupledSource(c.target.io.startAddr)
  val resultHandler = DecoupledSink(c.target.io.result)

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
    args.memlatency, c.target.nastiXDataBits/8, verbose=true)
 
  nasti loadMem args.loadmem
  preprocessors += nasti

  if (!run(startAddrHandler.inputs, args.addr,
           resultHandler.outputs, args.result, args.maxcycles)) fail
}

class PointerChaserZynqTester(c: ZynqShim[SimWrapper[PointerChaser]], args: PointerChaserArgs)
    extends ZynqShimTester(c, false, None, args.logFile, FastLoadMem) with PointerChaserTests {
  val startAddrHandler = DecoupledSource(c.sim.target.io.startAddr)
  val resultHandler = DecoupledSink(c.sim.target.io.result)

  setMemLatency(args.memlatency)
  loadMem(args.loadmem)
  
  if (!run(startAddrHandler.inputs, args.addr,
           resultHandler.outputs, args.result, args.maxcycles)) fail
}
