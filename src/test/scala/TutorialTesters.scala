package StroberExamples

import strober._
import examples._

import chisel3.iotesters.PeekPokeTests
import chisel3.UInt

trait GCDTests extends PeekPokeTests {
  val (a, b, z) = (64, 48, 16)
  def tests(dut: GCD) {
    do {
      val first = if (t == 0) 1 else 0;
      poke(dut.io.a, a)
      poke(dut.io.b, b)
      poke(dut.io.e, first)
      step(1)
    } while (t <= 1 || peek(dut.io.v) == 0)
    expect(dut.io.z, z)
  }
}

class GCDSimTests(c: SimWrapper[GCD]) 
    extends SimWrapperTester(c) with GCDTests {
  tests(c.target)
}

class GCDZynqTests(c: ZynqShim[SimWrapper[GCD]]) 
    extends ZynqShimTester(c) with GCDTests {
  tests(c.sim.target)
}

trait ParityTests extends PeekPokeTests {
  def tests(dut: Parity) {
    var isOdd = 0
    for (t <- 0 until 10) {
      val bit = rnd.nextInt(2)
      poke(dut.io.in, bit)
      step(1)
      expect(dut.io.out, isOdd)
      isOdd = (isOdd + bit) % 2
    }
  }
}

class ParitySimTests(c: SimWrapper[Parity]) 
    extends SimWrapperTester(c) with ParityTests {
  tests(c.target)
}

class ParityZynqTests(c: ZynqShim[SimWrapper[Parity]])
    extends ZynqShimTester(c) with ParityTests {
  tests(c.sim.target)
}

trait ShiftRegisterTests extends PeekPokeTests {
  def tests(dut: ShiftRegister) {
    val reg = Array.fill(4){ 0 }
    for (t <- 0 until 64) {
      val in = rnd.nextInt(2)
      poke(dut.io.in, in)
      step(1)
      if (t >= 4) expect(dut.io.out, reg(3))
      for (i <- 3 to 1 by -1) reg(i) = reg(i-1)
      reg(0) = in
    }
  }
}

class ShiftRegisterSimTests(c: SimWrapper[ShiftRegister]) 
    extends SimWrapperTester(c) with ShiftRegisterTests {
  tests(c.target)
}

class ShiftRegisterZynqTests(c: ZynqShim[SimWrapper[ShiftRegister]]) 
    extends ZynqShimTester(c) with ShiftRegisterTests {
  tests(c.sim.target)
}

trait ResetShiftRegisterTests extends PeekPokeTests {
  def tests(dut: ResetShiftRegister) {
    val ins = Array.fill(5){ 0 }
    var k = 0
    for (n <- 0 until 16) {
      val in    = rnd.nextInt(16)
      val shift = rnd.nextInt(2)
      if (shift == 1) ins(k % 5) = in
      poke(dut.io.in,    in)
      poke(dut.io.shift, shift)
      step(1)
      expect(dut.io.out, (if (n < 4) 0 else ins((k + 1) % 5)))
      if (shift == 1) k += 1
    }
  }
}

class ResetShiftRegisterSimTests(c: SimWrapper[ResetShiftRegister]) 
    extends SimWrapperTester(c) with ResetShiftRegisterTests {
  tests(c.target)
}

class ResetShiftRegisterZynqTests(c: ZynqShim[SimWrapper[ResetShiftRegister]])
    extends ZynqShimTester(c) with ResetShiftRegisterTests {
  tests(c.sim.target)
}

trait EnableShiftRegisterTests extends PeekPokeTests {
  def tests(dut: EnableShiftRegister) {
    val reg = Array.fill(4){ 0 }
    for (t <- 0 until 16) {
      val in    = rnd.nextInt(2)
      val shift = rnd.nextInt(2)
      poke(dut.io.in,    in)
      poke(dut.io.shift, shift)
      step(1)
      expect(dut.io.out, reg(3))
      if (shift == 1) {
        for (i <- 3 to 1 by -1)
          reg(i) = reg(i-1)
        reg(0) = in
      }
    }
  }
}

class EnableShiftRegisterSimTests(c: SimWrapper[EnableShiftRegister]) 
    extends SimWrapperTester(c) with EnableShiftRegisterTests {
  tests(c.target)
}

class EnableShiftRegisterZynqTests(c: ZynqShim[SimWrapper[EnableShiftRegister]])
    extends ZynqShimTester(c) with EnableShiftRegisterTests {
  tests(c.sim.target)
}

trait StackTests extends PeekPokeTests {
  def tests(dut: Stack) {
    var nxtDataOut = 0
    val stack = new collection.mutable.Stack[Int]()

    for (t <- 0 until 16) {
      val enable  = rnd.nextInt(2)
      val push    = rnd.nextInt(2)
      val pop     = rnd.nextInt(2)
      val dataIn  = rnd.nextInt(256)
      val dataOut = nxtDataOut

      if (enable == 1) {
        if (stack.length > 0)
          nxtDataOut = stack.top
        if (push == 1 && stack.length < dut.depth) {
          stack.push(dataIn)
        } else if (pop == 1 && stack.length > 0) {
          stack.pop()
        }
      }

      poke(dut.io.pop,    pop)
      poke(dut.io.push,   push)
      poke(dut.io.en,     enable)
      poke(dut.io.dataIn, dataIn)
      step(1)
      expect(dut.io.dataOut, dataOut)
    }
  }
}

class StackSimTests(c: SimWrapper[Stack]) 
    extends SimWrapperTester(c) with StackTests {
  tests(c.target)
}

class StackZynqTests(c: ZynqShim[SimWrapper[Stack]])
    extends ZynqShimTester(c) with StackTests {
  tests(c.sim.target)
}

trait RouterTests extends PeekPokeTests {
  def rd(dut: Router, addr: Int, data: Int) = {
    poke(dut.io.in.valid,        0)
    poke(dut.io.writes.valid,    0)
    poke(dut.io.replies.ready,   1)
    poke(dut.io.reads.valid,     1)
    poke(dut.io.reads.bits.addr, addr)
    while (peek(dut.io.replies.valid) == 0) step(1)
    expect(dut.io.replies.bits, data)
  }
  def wr(dut: Router, addr: Int, data: Int)  = {
    poke(dut.io.in.valid,         0)
    poke(dut.io.reads.valid,      0)
    poke(dut.io.writes.valid,     1)
    poke(dut.io.writes.bits.addr, addr)
    poke(dut.io.writes.bits.data, data)
    step(1)
  }
  def isAnyValidOuts(dut: Router) = {
    dut.io.outs.toSeq exists (out => peek(out.valid) == 1)
  }
  def rt(dut: Router, header: Int, body: Int)  = {
    dut.io.outs foreach (out => poke(out.ready, 1))
    poke(dut.io.reads.valid,    0)
    poke(dut.io.writes.valid,   0)
    poke(dut.io.in.valid,       1)
    poke(dut.io.in.bits.header, header)
    poke(dut.io.in.bits.body,   body)
    var i = 0
    do {
      step(1) ; i+= 1
    } while (!isAnyValidOuts(dut) && i < 10)
    expect(i < 10, "FIND VALID OUT")
  }
  def tests(dut: Router) {
    wr(dut, 0, 1)
    rd(dut, 0, 1)
    rt(dut, 0, 1)
  }
}

class RouterSimTests(c: SimWrapper[Router])
    extends SimWrapperTester(c) with RouterTests {
  tests(c.target)
}

class RouterZynqTests(c: ZynqShim[SimWrapper[Router]])
    extends ZynqShimTester(c) with RouterTests {
  tests(c.sim.target)
}

trait RiscTests extends PeekPokeTests {
  def wr(dut: Risc, addr: Int, data: BigInt) {
    poke(dut.io.isWr, 1)
    poke(dut.io.boot, 0)
    poke(dut.io.wrAddr, addr)
    poke(dut.io.wrData, data)
    step(1)
  }
  def boot(dut: Risc) {
    poke(dut.io.isWr, 0)
    poke(dut.io.boot, 1)
    step(1)
  }
  def tick(dut: Risc) {
    poke(dut.io.isWr, 0)
    poke(dut.io.boot, 0)
    step(1)
  }
  def I (op: UInt, rc: Int, ra: Int, rb: Int) =
    op.litValue() << 24 | BigInt(rc) << 16 | BigInt(ra) << 8 | BigInt(rb)

  def tests(dut: Risc) {
    val app  = Array(I(dut.imm_op,   1, 0, 1), // r1 <- 1
                     I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
                     I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
                     I(dut.add_op, 255, 1, 0)) // rh <- r1
    wr(dut, 0, 0) // skip reset
    for ((data, addr) <- app.zipWithIndex) 
      wr(dut, addr, data)
    boot(dut)
    var k = 0
    do {
      tick(dut); k += 1
    } while (peek(dut.io.valid) == 0 && k < 10)
    expect(k < 10, "TIME LIMIT")
    expect(dut.io.out, 4)
  }
}

class RiscSimTests(c: SimWrapper[Risc]) 
    extends SimWrapperTester(c) with RiscTests {
  tests(c.target)
}

class RiscZynqTests(c: ZynqShim[SimWrapper[Risc]])
    extends ZynqShimTester(c) with RiscTests {
  tests(c.sim.target)
}

trait RiscSRAMTests extends PeekPokeTests {
  def wr(dut: RiscSRAM, addr: Int, data: BigInt) {
    poke(dut.io.isWr, 1)
    poke(dut.io.boot, 0)
    poke(dut.io.wrAddr, addr)
    poke(dut.io.wrData, data)
    step(1)
  }
  def boot(dut: RiscSRAM) {
    poke(dut.io.isWr, 0)
    poke(dut.io.boot, 1)
    step(1)
  }
  def tick(dut: RiscSRAM) {
    poke(dut.io.isWr, 0)
    poke(dut.io.boot, 0)
    step(1)
  }
  def I (op: UInt, rc: Int, ra: Int, rb: Int) =
    op.litValue() << 24 | BigInt(rc) << 16 | BigInt(ra) << 8 | BigInt(rb)

  def tests(dut: RiscSRAM) {
    val app  = Array(
      I(dut.imm_op,   1, 0, 1), // r1 <- 1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 2), // r1 <- 2
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 3), // r1 <- 3
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 4), // r1 <- 4
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 5), // r1 <- 5
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 6), // r1 <- 6
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 7), // r1 <- 7
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 8), // r1 <- 8
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 9), // r1 <- 9
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   1, 0, 10), // r1 <- 10
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.add_op,   1, 1, 1), // r1 <- r1 + r1
      I(dut.imm_op,   2, 0, 1), // r2 <- 1
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 2), // r2 <- 2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 3), // r2 <- 3
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 4), // r2 <- 4
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 5), // r2 <- 5
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 6), // r2 <- 6
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 7), // r2 <- 7
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 8), // r2 <- 8
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 9), // r2 <- 9
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.imm_op,   2, 0, 10), // r2 <- 10
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op,   2, 2, 1), // r2 <- r2 + r2
      I(dut.add_op, 255, 1, 0)) // rh <- r1
    wr(dut, 0, 0) // skip reset
    for ((data, addr) <- app.zipWithIndex)
      wr(dut, addr, data)
    boot(dut)
    var k = 0
    do {
      tick(dut); k += 1
    } while (peek(dut.io.valid) == 0 && k < 400)
    expect(k < 400, "TIME LIMIT")
    expect(dut.io.out, 40)
  }
}

class RiscSRAMSimTests(c: SimWrapper[RiscSRAM])
    extends SimWrapperTester(c) with RiscSRAMTests {
  tests(c.target)
}

class RiscSRAMZynqTests(c: ZynqShim[SimWrapper[RiscSRAM]])
    extends ZynqShimTester(c) with RiscSRAMTests {
  tests(c.sim.target)
}
