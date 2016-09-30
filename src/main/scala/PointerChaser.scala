package StroberExamples

import Chisel._
import junctions._
import cde.{Parameters, Field}

case object MemSize extends Field[Int]
case object NMemoryChannels extends Field[Int]
case object CacheBlockBytes extends Field[Int]
case object CacheBlockOffsetBits extends Field[Int]

// This module computes the sum of a simple singly linked-list, where each
// node consists of a pointer to the next node and a 64 bit SInt
// Inputs: (Decoupled) start address: the location of the first node in memory
// Outputs: (Decoupled) result: The sum of the list
class PointerChaser(implicit val p: Parameters) extends Module with HasNastiParameters {
  val io = IO(new Bundle {
    val nasti = new NastiIO
    val result = Decoupled(SInt(width = p(MIFDataBits)))
    val startAddr = Flipped(Decoupled(UInt(width = p(MIFAddrBits))))
  })
  
  val memoryIF = io.nasti
  val busy = Reg(init = Bool(false))
  val resultReg = Reg(init = SInt(0))
  val resultValid = Reg(init = Bool(false))
  
  val startFire = io.startAddr.valid && ~busy
  val doneFire =  io.result.valid && io.result.ready

  when (!resultValid && !busy) {
    busy := startFire
  }.elsewhen(doneFire) {
    busy := Bool(false)
  }

  io.startAddr.ready := !busy && !reset

  io.result.bits := resultReg
  io.result.valid := resultValid

  val rFire = memoryIF.r.valid && memoryIF.r.ready
  val nextAddrAvailable = rFire && !memoryIF.r.bits.last

  // Need to add an extra cycle of delay between when we learn we are on
  // the last node and when the final sum is computed. Since the address beat
  // is returned first
  val isFinalNode = Reg(init = Bool(false))
  // next node addr == 0 -> terminal node
  when (nextAddrAvailable) {
    isFinalNode := memoryIF.r.bits.data === UInt(0)
  }

  when (rFire && memoryIF.r.bits.last){
    resultValid := isFinalNode
    resultReg := resultReg + memoryIF.r.bits.data.asSInt()
  }.elsewhen (doneFire) {
    resultValid := Bool(false)
    resultReg := SInt(0)
  }
  
  val arFire = memoryIF.ar.ready && memoryIF.ar.valid

  val arRegAddr = Reg(init = UInt(0))
  val arValid = Reg(init = Bool(false))

  when (startFire | (nextAddrAvailable && memoryIF.r.bits.data != UInt(0))) {
    arValid := Bool(true)
    arRegAddr := Mux(startFire, io.startAddr.bits, memoryIF.r.bits.data)
  }.elsewhen(arFire) {
    arValid := Bool(false)
  }

  memoryIF.ar.bits := NastiWriteAddressChannel(
    id = UInt(0),
    len = UInt(1),
    size = bytesToXSize(UInt(p(MIFDataBits)/8)),
    addr = arRegAddr)
  memoryIF.ar.valid := arValid
  memoryIF.r.ready := Bool(true)

  memoryIF.w.valid := Bool(false)
  memoryIF.aw.valid := Bool(false)
  memoryIF.b.ready := Bool(true)

  //TODO: Figure out how to prevent chisel from optimizing these parameters away
  println("Number of Channels: " + p(NMemoryChannels))
  println("Cache Block Size: " + p(CacheBlockBytes))
  println("Number of Channels: " + p(NMemoryChannels))

  println("MemSize " + p(MemSize))
  println("MIFDataBits " + p(MIFDataBits))
  println("MIFDataBeats " + p(MIFDataBeats))
}
