package Designs

import Chisel._
import strober._
import TutorialExamples._

class GCDSimWrapperTests(c: SimWrapper[GCD]) extends SimWrapperTester(c) with GCDTests {
  tests(c.target)
}

class GCDSimAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[GCD]]) extends SimAXI4WrapperTester(c) with GCDTests {
  tests(c.sim.target)
}

class ParityWrapperTests(c: SimWrapper[Parity]) extends SimWrapperTester(c) with ParityTests {
  tests(c.target)
}

class ParityAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Parity]]) extends SimAXI4WrapperTester(c) with ParityTests {
  tests(c.sim.target)
}

class ShiftRegisterWrapperTests(c: SimWrapper[ShiftRegister]) extends SimWrapperTester(c) with ShiftRegisterTests { 
  tests(c.target) 
}

class ShiftRegisterAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[ShiftRegister]]) extends SimAXI4WrapperTester(c) with ShiftRegisterTests {
  tests(c.sim.target)  
}

class EnableShiftRegisterWrapperTests(c: SimWrapper[EnableShiftRegister]) extends SimWrapperTester(c) with EnableShiftRegisterTests {  
  tests(c.target)
}

class EnableShiftRegisterAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[EnableShiftRegister]]) extends SimAXI4WrapperTester(c) with EnableShiftRegisterTests {
  tests(c.sim.target)  
}

class ResetShiftRegisterWrapperTests(c: SimWrapper[ResetShiftRegister]) extends SimWrapperTester(c) with ResetShiftRegisterTests {
  tests(c.target)  
}

class ResetShiftRegisterAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[ResetShiftRegister]]) extends SimAXI4WrapperTester(c) with ResetShiftRegisterTests {  
  tests(c.sim.target)
}

class StackWrapperTests(c: SimWrapper[Stack]) extends SimWrapperTester(c) with StackTests { 
  tests(c.target)
}

class StackAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Stack]]) extends SimAXI4WrapperTester(c) with StackTests {
  tests(c.sim.target)  
}

class MemorySearchWrapperTests(c: SimWrapper[MemorySearch]) extends SimWrapperTester(c) with MemorySearchTests {
  tests(c.target)
}

class MemorySearchAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[MemorySearch]]) extends SimAXI4WrapperTester(c) with MemorySearchTests {
  tests(c.sim.target)
}

class RouterWrapperTests(c: SimWrapper[Router]) extends SimWrapperTester(c) with RouterTests {
  tests(c.target)  
}

class RouterAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Router]]) extends SimAXI4WrapperTester(c) with RouterTests {
  tests(c.sim.target) 
}

class RiscWrapperTests(c: SimWrapper[Risc]) extends SimWrapperTester(c) with RiscTests {
  tests(c.target)
}

class RiscAXI4WrapperTests(c: SimAXI4Wrapper[SimWrapper[Risc]]) extends SimAXI4WrapperTester(c) with RiscTests { 
  tests(c.sim.target) 
}
