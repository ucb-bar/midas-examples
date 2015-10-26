package Designs

import Chisel._
import strober._
import TutorialExamples._

class GCDSimSimTests(c: SimWrapper[GCD]) extends SimWrapperTester(c) with GCDTests {
  tests(c.target)
}

class GCDNASTIShimTests(c: NASTIShim[SimWrapper[GCD]]) extends NASTIShimTester(c) with GCDTests {
  tests(c.sim.target)
}

class ParitySimTests(c: SimWrapper[Parity]) extends SimWrapperTester(c) with ParityTests {
  tests(c.target)
}

class ParityNASTIShimTests(c: NASTIShim[SimWrapper[Parity]]) extends NASTIShimTester(c) with ParityTests {
  tests(c.sim.target)
}

class ShiftRegisterSimTests(c: SimWrapper[ShiftRegister]) extends SimWrapperTester(c) with ShiftRegisterTests { 
  tests(c.target) 
}

class ShiftRegisterNASTIShimTests(c: NASTIShim[SimWrapper[ShiftRegister]]) extends NASTIShimTester(c) with ShiftRegisterTests {
  tests(c.sim.target)  
}

class EnableShiftRegisterSimTests(c: SimWrapper[EnableShiftRegister]) extends SimWrapperTester(c) with EnableShiftRegisterTests {  
  tests(c.target)
}

class EnableShiftRegisterNASTIShimTests(c: NASTIShim[SimWrapper[EnableShiftRegister]]) extends NASTIShimTester(c) with EnableShiftRegisterTests {
  tests(c.sim.target)  
}

class ResetShiftRegisterSimTests(c: SimWrapper[ResetShiftRegister]) extends SimWrapperTester(c) with ResetShiftRegisterTests {
  tests(c.target)  
}

class ResetShiftRegisterNASTIShimTests(c: NASTIShim[SimWrapper[ResetShiftRegister]]) extends NASTIShimTester(c) with ResetShiftRegisterTests {  
  tests(c.sim.target)
}

class StackSimTests(c: SimWrapper[Stack]) extends SimWrapperTester(c) with StackTests { 
  tests(c.target)
}

class StackNASTIShimTests(c: NASTIShim[SimWrapper[Stack]]) extends NASTIShimTester(c) with StackTests {
  tests(c.sim.target)  
}

class MemorySearchSimTests(c: SimWrapper[MemorySearch]) extends SimWrapperTester(c) with MemorySearchTests {
  tests(c.target)
}

class MemorySearchNASTIShimTests(c: NASTIShim[SimWrapper[MemorySearch]]) extends NASTIShimTester(c) with MemorySearchTests {
  tests(c.sim.target)
}

class RouterSimTests(c: SimWrapper[Router]) extends SimWrapperTester(c) with RouterTests {
  tests(c.target)  
}

class RouterNASTIShimTests(c: NASTIShim[SimWrapper[Router]]) extends NASTIShimTester(c) with RouterTests {
  tests(c.sim.target) 
}

class RiscSimTests(c: SimWrapper[Risc]) extends SimWrapperTester(c) with RiscTests {
  tests(c.target)
}

class RiscNASTIShimTests(c: NASTIShim[SimWrapper[Risc]]) extends NASTIShimTester(c) with RiscTests { 
  tests(c.sim.target) 
}
