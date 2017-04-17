package strober
package examples

import chisel3._
import chisel3.util._

class Risc extends Module {
  val io = IO(new Bundle {
    val isWr   = Input(Bool())
    val wrAddr = Input(UInt(width=8))
    val wrData = Input(UInt(width=32))
    val boot   = Input(Bool())
    val valid  = Output(Bool())
    val out    = Output(UInt(width=32))
  })
  val file = Mem(256, UInt(32.W))
  val code = Mem(256, UInt(32.W))
  val pc   = Reg(init=UInt(0, 8))
  
  val add_op :: imm_op :: Nil = Enum(UInt(), 2)

  val inst = code(pc)
  val op   = inst(31,24)
  val rci  = inst(23,16)
  val rai  = inst(15, 8)
  val rbi  = inst( 7, 0)

  val ra = Mux(rai === 0.U, 0.U, file(rai))
  val rb = Mux(rbi === 0.U, 0.U, file(rbi))
  val rc = Wire(UInt(32.W))

  io.valid := Bool(false)
  io.out   := 0.U
  rc       := 0.U

  when (io.isWr) {
    code(io.wrAddr) := io.wrData
  } .elsewhen (io.boot) {
    pc := 0.U
  } .otherwise {
    switch(op) {
      is(add_op) { rc := ra + rb }
      is(imm_op) { rc := (rai << 8.U) | rbi }
    }
    io.out := rc
    when (rci === 255.U) {
      io.valid := Bool(true)
    } .otherwise {
      file(rci) := rc
    }
    pc := pc + 1.U
  }
}
