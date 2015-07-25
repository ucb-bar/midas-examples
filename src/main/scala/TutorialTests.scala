package Designs

import Chisel._
import strober._
import TutorialExamples._
import scala.collection.mutable.{Stack => ScalaStack}

class GCDSimWrapperTests(c: SimWrapper[GCD]) extends SimWrapperTester(c) {
  val (a, b, z) = (64, 48, 16)
  do {
    val first = if (t == 0) 1 else 0
    pokePort(c.target.io.a, a)
    pokePort(c.target.io.b, b)
    pokePort(c.target.io.e, first)
    step(1)
  } while (t <= 1 || peekPort(c.target.io.v) == 0)
  expectPort(c.target.io.z, z)
}

class GCDSimAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[GCD]]) extends SimAXI4WrapperTester(c) {
  val (a, b, z) = (64, 48, 16)
  writeMem(0x2000, BigInt(
    "deadbeef10041004" + "20042004beafdead" + 
    "aabbbeef10041004" + "20042004beafaabb" + 
    "deadbaab10041004" + "20042004baabdead" + 
    "bababeef10041004" + "20042004beafbaba", 16))
  do {
    val first = if (t == 0) 1 else 0
    pokePort(c.sim.target.io.a, a)
    pokePort(c.sim.target.io.b, b)
    pokePort(c.sim.target.io.e, first)
    step(1)
  } while (t <= 1 || peekPort(c.sim.target.io.v) == 0)
  expectPort(c.sim.target.io.z, z)
  println("hello -> %x".format(readMem(0x2000)))
}

class ParityWrapperTests(c: SimWrapper[Parity]) extends SimWrapperTester(c) {
  var isOdd = 0
  for (t <- 0 until 10) {
    val bit = rnd.nextInt(2)
    pokePort(c.target.io.in, bit)
    step(1)
    isOdd = (isOdd + bit) % 2;
    expectPort(c.target.io.out, isOdd)
  }
}

class ParityAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Parity]]) extends SimAXI4WrapperTester(c) {
  var isOdd = 0
  for (t <- 0 until 10) {
    val bit = rnd.nextInt(2)
    pokePort(c.sim.target.io.in, bit)
    step(1)
    isOdd = (isOdd + bit) % 2;
    expectPort(c.sim.target.io.out, isOdd)
  }
}

class ShiftRegisterWrapperTests(c: SimWrapper[ShiftRegister]) extends SimWrapperTester(c) {  
  val reg = Array.fill(4){ 0 }
  for (t <- 0 until 64) {
    val in = rnd.nextInt(2)
    pokePort(c.target.io.in, in)
    step(1)
    for (i <- 3 to 1 by -1)
      reg(i) = reg(i-1)
    reg(0) = in
    if (t >= 4) expectPort(c.target.io.out, reg(3))
  }
}

class ShiftRegisterAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[ShiftRegister]]) 
  extends SimAXI4WrapperTester(c) {  
  val reg = Array.fill(4){ 0 }
  for (t <- 0 until 64) {
    val in = rnd.nextInt(2)
    pokePort(c.sim.target.io.in, in)
    step(1)
    for (i <- 3 to 1 by -1)
      reg(i) = reg(i-1)
    reg(0) = in
    if (t >= 4) expectPort(c.sim.target.io.out, reg(3))
  }
}

class EnableShiftRegisterWrapperTests(c: SimWrapper[EnableShiftRegister]) extends SimWrapperTester(c) {  
  val reg = Array.fill(4){ 0 }
  for (t <- 0 until 16) {
    val in    = rnd.nextInt(16)
    val shift = rnd.nextInt(2)
    pokePort(c.target.io.in,    in)
    pokePort(c.target.io.shift, shift)
    step(1)
    if (shift == 1) {
      for (i <- 3 to 1 by -1)
        reg(i) = reg(i-1)
      reg(0) = in
    }
    expectPort(c.target.io.out, reg(3))
  }
}

class EnableShiftRegisterAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[EnableShiftRegister]]) 
  extends SimAXI4WrapperTester(c) {  
  val reg = Array.fill(4){ 0 }
  for (t <- 0 until 16) {
    val in    = rnd.nextInt(16)
    val shift = rnd.nextInt(2)
    pokePort(c.sim.target.io.in,    in)
    pokePort(c.sim.target.io.shift, shift)
    step(1)
    if (shift == 1) {
      for (i <- 3 to 1 by -1)
        reg(i) = reg(i-1)
      reg(0) = in
    }
    expectPort(c.sim.target.io.out, reg(3))
  }
}

class ResetShiftRegisterWrapperTests(c: SimWrapper[ResetShiftRegister]) extends SimWrapperTester(c) {  
  val ins = Array.fill(5){ 0 }
  var k   = 0
  for (n <- 0 until 16) {
    val in    = rnd.nextInt(16)
    val shift = rnd.nextInt(2)
    if (shift == 1) 
      ins(k % 5) = in
    pokePort(c.target.io.in,    in)
    pokePort(c.target.io.shift, shift)
    step(1)
    if (shift == 1)
      k = k + 1
    expectPort(c.target.io.out, (if (n < 3) 0 else ins((k + 1) % 5)))
  }
}

class ResetShiftRegisterAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[ResetShiftRegister]]) 
  extends SimAXI4WrapperTester(c) {  
  val ins = Array.fill(5){ 0 }
  var k   = 0
  for (n <- 0 until 16) {
    val in    = rnd.nextInt(16)
    val shift = rnd.nextInt(2)
    if (shift == 1) 
      ins(k % 5) = in
    pokePort(c.sim.target.io.in,    in)
    pokePort(c.sim.target.io.shift, shift)
    step(1)
    if (shift == 1)
      k = k + 1
    expectPort(c.sim.target.io.out, (if (n < 3) 0 else ins((k + 1) % 5)))
  }
}

class StackWrapperTests(c: SimWrapper[Stack]) extends SimWrapperTester(c) {  
  var nxtDataOut = 0
  var dataOut = 0
  val stack = new ScalaStack[Int]()

  for (t <- 0 until 16) {
    val enable  = rnd.nextInt(2)
    val push    = rnd.nextInt(2)
    val pop     = rnd.nextInt(2)
    val dataIn  = rnd.nextInt(256)

    if (enable == 1) {
      dataOut = nxtDataOut
      if (push == 1 && stack.length < c.target.depth) {
        stack.push(dataIn)
      } else if (pop == 1 && stack.length > 0) {
        stack.pop()
      }
      if (stack.length > 0) {
        nxtDataOut = stack.top
      }
    }

    pokePort(c.target.io.pop,    pop)
    pokePort(c.target.io.push,   push)
    pokePort(c.target.io.en,     enable)
    pokePort(c.target.io.dataIn, dataIn)
    step(1)
    expectPort(c.target.io.dataOut, dataOut)
  }
}

class StackAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Stack]]) extends SimAXI4WrapperTester(c) {  
  var nxtDataOut = 0
  var dataOut = 0
  val stack = new ScalaStack[Int]()

  for (t <- 0 until 16) {
    val enable  = rnd.nextInt(2)
    val push    = rnd.nextInt(2)
    val pop     = rnd.nextInt(2)
    val dataIn  = rnd.nextInt(256)

    if (enable == 1) {
      dataOut = nxtDataOut
      if (push == 1 && stack.length < c.sim.target.depth) {
        stack.push(dataIn)
      } else if (pop == 1 && stack.length > 0) {
        stack.pop()
      }
      if (stack.length > 0) {
        nxtDataOut = stack.top
      }
    }

    pokePort(c.sim.target.io.pop,    pop)
    pokePort(c.sim.target.io.push,   push)
    pokePort(c.sim.target.io.en,     enable)
    pokePort(c.sim.target.io.dataIn, dataIn)
    step(1)
    expectPort(c.sim.target.io.dataOut, dataOut)
  }
}

class MemorySearchWrapperTests(c: SimWrapper[MemorySearch]) extends SimWrapperTester(c) {
  val list = c.target.elts.map(int(_)) 
  val n = 8
  val maxT = n * (list.length + 3)
  for (k <- 0 until n) {
    val target = rnd.nextInt(16)
    pokePort(c.target.io.en,     1)
    pokePort(c.target.io.target, target)
    step(1)
    pokePort(c.target.io.en,     0)
    do {
      step(1)
    } while (peekPort(c.target.io.done) == 0 && t < maxT)
    val addr = peekPort(c.target.io.address).toInt
    expect(addr == list.length || list(addr) == target, 
           "LOOKING FOR " + target + " FOUND " + addr)
  }
}

class MemorySearchAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[MemorySearch]]) 
  extends SimAXI4WrapperTester(c) {
  val list = c.sim.target.elts.map(int(_)) 
  val n = 8
  val maxT = n * (list.length + 3)
  for (k <- 0 until n) {
    val target = rnd.nextInt(16)
    pokePort(c.sim.target.io.en,     1)
    pokePort(c.sim.target.io.target, target)
    step(1)
    pokePort(c.sim.target.io.en,     0)
    do {
      step(1)
    } while (peekPort(c.sim.target.io.done) == 0 && t < maxT)
    val addr = peekPort(c.sim.target.io.address).toInt
    expect(addr == list.length || list(addr) == target, 
           "LOOKING FOR " + target + " FOUND " + addr)
  }
}

class RouterWrapperTests(c: SimWrapper[Router]) extends SimWrapperTester(c) {  
  def rd(addr: Int, data: Int) = {
    pokePort(c.target.io.in.valid,        0)
    pokePort(c.target.io.writes.valid,    0)
    pokePort(c.target.io.reads.valid,     1)
    pokePort(c.target.io.replies.ready,   1)
    pokePort(c.target.io.reads.bits.addr, addr)
    step(1)
    expectPort(c.target.io.replies.bits, data)
  }
  def wr(addr: Int, data: Int)  = {
    pokePort(c.target.io.in.valid,         0)
    pokePort(c.target.io.reads.valid,      0)
    pokePort(c.target.io.writes.valid,     1)
    pokePort(c.target.io.writes.bits.addr, addr)
    pokePort(c.target.io.writes.bits.data, data)
    step(1)
  }
  def isAnyValidOuts(): Boolean = {
    for (out <- c.target.io.outs)
      if (peekPort(out.valid) == 1)
        return true
    false
  }
  def rt(header: Int, body: Int)  = {
    for (out <- c.target.io.outs)
      pokePort(out.ready, 1)
    pokePort(c.target.io.reads.valid,    0)
    pokePort(c.target.io.writes.valid,   0)
    pokePort(c.target.io.in.valid,       1)
    pokePort(c.target.io.in.bits.header, header)
    pokePort(c.target.io.in.bits.body,   body)
    var i = 0
    do {
      step(1)
      i += 1
    } while (!isAnyValidOuts() && i < 10)
    expect(i < 10, "FIND VALID OUT")
  }
  rd(0, 0)
  wr(0, 1)
  rd(0, 1)
  rt(0, 1)
}

class RouterAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Router]]) extends SimAXI4WrapperTester(c) {  
  def rd(addr: Int, data: Int) = {
    pokePort(c.sim.target.io.in.valid,        0)
    pokePort(c.sim.target.io.writes.valid,    0)
    pokePort(c.sim.target.io.reads.valid,     1)
    pokePort(c.sim.target.io.replies.ready,   1)
    pokePort(c.sim.target.io.reads.bits.addr, addr)
    step(1)
    expectPort(c.sim.target.io.replies.bits, data)
  }
  def wr(addr: Int, data: Int)  = {
    pokePort(c.sim.target.io.in.valid,         0)
    pokePort(c.sim.target.io.reads.valid,      0)
    pokePort(c.sim.target.io.writes.valid,     1)
    pokePort(c.sim.target.io.writes.bits.addr, addr)
    pokePort(c.sim.target.io.writes.bits.data, data)
    step(1)
  }
  def isAnyValidOuts(): Boolean = {
    for (out <- c.sim.target.io.outs)
      if (peekPort(out.valid) == 1)
        return true
    false
  }
  def rt(header: Int, body: Int)  = {
    for (out <- c.sim.target.io.outs)
      pokePort(out.ready, 1)
    pokePort(c.sim.target.io.reads.valid,    0)
    pokePort(c.sim.target.io.writes.valid,   0)
    pokePort(c.sim.target.io.in.valid,       1)
    pokePort(c.sim.target.io.in.bits.header, header)
    pokePort(c.sim.target.io.in.bits.body,   body)
    var i = 0
    do {
      step(1)
      i += 1
    } while (!isAnyValidOuts() && i < 10)
    expect(i < 10, "FIND VALID OUT")
  }
  rd(0, 0)
  wr(0, 1)
  rd(0, 1)
  rt(0, 1)
}

class RiscWrapperTests(c: SimWrapper[Risc]) extends SimWrapperTester(c) {  
  def wr(addr: UInt, data: UInt)  = {
    pokePort(c.target.io.isWr,   1)
    pokePort(c.target.io.wrAddr, addr.litValue())
    pokePort(c.target.io.wrData, data.litValue())
    step(1)
  }
  def boot()  = {
    pokePort(c.target.io.isWr, 0)
    pokePort(c.target.io.boot, 1)
    step(1)
  }
  def tick()  = {
    pokePort(c.target.io.isWr, 0)
    pokePort(c.target.io.boot, 0)
    step(1)
  }
  def I (op: UInt, rc: Int, ra: Int, rb: Int) = 
    Cat(op, UInt(rc, 8), UInt(ra, 8), UInt(rb, 8))
  val app  = Array(I(c.target.imm_op,   1, 0, 1), // r1 <- 1
                   I(c.target.add_op,   1, 1, 1), // r1 <- r1 + r1
                   I(c.target.add_op,   1, 1, 1), // r1 <- r1 + r1
                   I(c.target.add_op, 255, 1, 0)) // rh <- r1
  wr(UInt(0), Bits(0)) // skip reset
  for (addr <- 0 until app.length) 
    wr(UInt(addr), app(addr))
  boot()
  var k = 0
  do {
    tick(); k += 1
  } while (peekPort(c.target.io.valid) == 0 && k < 10)
  expect(k < 10, "TIME LIMIT")
  expectPort(c.target.io.out, 4)
}

class RiscAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Risc]]) extends SimAXI4WrapperTester(c) {  
  def wr(addr: UInt, data: UInt)  = {
    pokePort(c.sim.target.io.isWr,   1)
    pokePort(c.sim.target.io.wrAddr, addr.litValue())
    pokePort(c.sim.target.io.wrData, data.litValue())
    step(1)
  }
  def boot()  = {
    pokePort(c.sim.target.io.isWr, 0)
    pokePort(c.sim.target.io.boot, 1)
    step(1)
  }
  def tick()  = {
    pokePort(c.sim.target.io.isWr, 0)
    pokePort(c.sim.target.io.boot, 0)
    step(1)
  }
  def I (op: UInt, rc: Int, ra: Int, rb: Int) = 
    Cat(op, UInt(rc, 8), UInt(ra, 8), UInt(rb, 8))
  val app  = Array(I(c.sim.target.imm_op,   1, 0, 1), // r1 <- 1
                   I(c.sim.target.add_op,   1, 1, 1), // r1 <- r1 + r1
                   I(c.sim.target.add_op,   1, 1, 1), // r1 <- r1 + r1
                   I(c.sim.target.add_op, 255, 1, 0)) // rh <- r1
  wr(UInt(0), Bits(0)) // skip reset
  for (addr <- 0 until app.length) 
    wr(UInt(addr), app(addr))
  boot()
  var k = 0
  do {
    tick(); k += 1
  } while (peekPort(c.sim.target.io.valid) == 0 && k < 10)
  expect(k < 10, "TIME LIMIT")
  expectPort(c.sim.target.io.out, 4)
}
