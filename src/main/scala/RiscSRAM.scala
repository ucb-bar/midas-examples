package Designs

import Chisel._
import strober._

class RiscSRAM extends Module {
  val io = new Bundle {
    val isWr   = Bool(INPUT)
    val wrAddr = UInt(INPUT, 8)
    val wrData = UInt(INPUT, 32)
    val boot   = Bool(INPUT)
    val valid  = Bool(OUTPUT)
    val out    = UInt(OUTPUT, 32)
  }
  val fileMem = SeqMem(UInt(width = 32), 256)
  val codeMem = SeqMem(UInt(width = 32), 256)

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

  debug(op)
  debug(rci)
  debug(rai)
  debug(rbi)

  io.out   := Mux(op === add_op, ra + rb, Cat(rai, rbi))
  io.valid := fileState === rc_write && rci === UInt(255)

  val file_wen = fileState === rc_write && rci != UInt(255)
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

trait RiscSRAMTests extends Tests {
  def tests(c: RiscSRAM) {
    def wr(addr: UInt, data: UInt)  = {
      poke(c.io.isWr,   1)
      poke(c.io.wrAddr, addr.litValue())
      poke(c.io.wrData, data.litValue())
      step(1)
    }
    def boot()  = {
      poke(c.io.isWr, 0)
      poke(c.io.boot, 1)
      step(1)
    }
    def tick()  = {
      poke(c.io.isWr, 0)
      poke(c.io.boot, 0)
      step(1)
    }
    def I (op: UInt, rc: Int, ra: Int, rb: Int) = 
      Cat(op, UInt(rc, 8), UInt(ra, 8), UInt(rb, 8))
    val app  = Array(
      I(c.imm_op,   1, 0, 1), // r1 <- 1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 2), // r1 <- 2
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 3), // r1 <- 3
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 4), // r1 <- 4
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 5), // r1 <- 5
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 6), // r1 <- 6
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 7), // r1 <- 7
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 8), // r1 <- 8
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 9), // r1 <- 9
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   1, 0, 10), // r1 <- 10
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 1), // r1 <- 1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 2), // r1 <- 2
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 3), // r1 <- 3
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 4), // r1 <- 4
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 5), // r1 <- 5
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 6), // r1 <- 6
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 7), // r1 <- 7
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 8), // r1 <- 8
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 9), // r1 <- 9
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.imm_op,   2, 0, 10), // r1 <- 10
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op,   2, 2, 1), // r1 <- r1 + r1
      I(c.add_op, 255, 1, 0)) // rh <- r1
    wr(UInt(0), Bits(0)) // skip reset
    for (addr <- 0 until app.length) 
      wr(UInt(addr), app(addr))
    boot()
    var k = 0
    do {
      tick(); k += 1
    } while (peek(c.io.valid) == 0 && k < 400)
    expect(k < 400, "TIME LIMIT")
    expect(c.io.out, 40)
  }
}

class RiscSRAMTester(c: RiscSRAM) extends Tester(c) with RiscSRAMTests {
  tests(c)  
}

class RiscSRAMWrapperTests(c: SimWrapper[RiscSRAM]) extends SimWrapperTester(c) with RiscSRAMTests {
  tests(c.target)  
}

class RiscSRAMAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[RiscSRAM]]) extends SimAXI4WrapperTester(c) with RiscSRAMTests { 
  tests(c.sim.target)
}
