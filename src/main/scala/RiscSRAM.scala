package examples

import chisel3._
import chisel3.util._

class RiscSRAM extends Module {
  val io = new Bundle {
    val isWr   = Bool(INPUT)
    val wrAddr = UInt(INPUT, 8)
    val wrData = UInt(INPUT, 32)
    val boot   = Bool(INPUT)
    val valid  = Bool(OUTPUT)
    val out    = UInt(OUTPUT, 32)
  }
  val fileMem = SeqMem(128, UInt(width = 32))
  val codeMem = SeqMem(128, UInt(width = 32))

  val idle :: decode :: ra_read :: rb_read :: rc_write :: Nil = Enum(UInt(), 5)
  val code_read :: code_write :: Nil = Enum(UInt(), 2)
  val fileState = RegInit(idle)
  val codeState = RegInit(code_write)

  val add_op :: imm_op :: Nil = Enum(UInt(), 2)
  val pc       = RegInit(UInt(0, 8))
  val raData   = Reg(UInt(width=32))
  val rbData   = Reg(UInt(width=32))

  val code_wen = codeState === code_write && io.isWr
  val code = codeMem.read(pc, !code_wen)
  when(code_wen) {
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
  io.valid := fileState === rc_write && rci === UInt(255)

  val file_wen = fileState === rc_write && rci =/= UInt(255)
  val file_addr = Mux(fileState === decode, rai, rbi)
  val file = fileMem.read(file_addr, !file_wen)
  when(file_wen) {
    fileMem.write(rci, io.out)
  }

  switch(codeState) {
    is(code_write) {
      when(io.boot) {
        inst := code
        codeState := code_read
      }
    }
    is(code_read) {
      // execute
    }
  }
  
  switch(fileState) {
    is(idle) {
      when(io.boot) {
        fileState := decode
      }
    }
    is(decode) {
      fileState := Mux(op === add_op, ra_read, rc_write)
    }
    is(ra_read) {
      raData    := file
      fileState := rb_read
    }
    is(rb_read) {
      rbData    := file
      fileState := rc_write
    }
    is(rc_write) {
      when(!io.valid) {
        fileState := decode
        pc := pc + UInt(1)
        inst := code
      }
    }
  }
}
