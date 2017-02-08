package strober.examples

import chisel3._
import chisel3.util._

class RiscSRAM extends Module {
  val io = IO(new Bundle {
    val isWr   = Bool(INPUT)
    val wrAddr = UInt(INPUT, 8)
    val wrData = UInt(INPUT, 32)
    val boot   = Bool(INPUT)
    val valid  = Bool(OUTPUT)
    val out    = UInt(OUTPUT, 32)
  })
  val fileMem = SeqMem(128, UInt(width = 32))
  val codeMem = SeqMem(128, UInt(width = 32))

  val idle :: fetch :: decode :: ra_read :: rb_read :: rc_write :: Nil = Enum(UInt(), 6)
  val state = RegInit(idle)

  val add_op :: imm_op :: Nil = Enum(UInt(), 2)
  val pc       = RegInit(UInt(0, 8))
  val raData   = Reg(UInt(width=32))
  val rbData   = Reg(UInt(width=32))

  val code = codeMem.read(pc, !io.isWr)
  when(io.isWr) {
    codeMem.write(io.wrAddr, io.wrData)
  }

  val inst = Reg(UInt(width=32))
  val op   = inst(31,24)
  val rci  = inst(23,16)
  val rai  = inst(15, 8)
  val rbi  = inst( 7, 0)
  val ra   = Mux(rai === UInt(0), UInt(0), raData)
  val rb   = Mux(rbi === UInt(0), UInt(0), rbData)

  io.out   := Mux(op === add_op, ra + rb, Cat(rai, rbi))
  io.valid := state === rc_write && rci === UInt(255)

  val file_wen = state === rc_write && rci =/= UInt(255)
  val file_addr = Mux(state === decode, rai, rbi)
  val file = fileMem.read(file_addr, !file_wen)
  when(file_wen) {
    fileMem.write(rci, io.out)
  }

  switch(state) {
    is(idle) {
      when(io.boot) {
        state := fetch
      }
    }
    is(fetch) {
      pc    := pc + UInt(1)
      inst  := code
      state := decode
    }
    is(decode) {
      state := Mux(op === add_op, ra_read, rc_write)
    }
    is(ra_read) {
      raData := file
      state  := rb_read
    }
    is(rb_read) {
      rbData := file
      state  := rc_write
    }
    is(rc_write) {
      when(!io.valid) {
        state := fetch
      }
    }
  }
}
