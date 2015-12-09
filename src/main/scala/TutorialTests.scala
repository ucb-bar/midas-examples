package Designs

import Chisel._
import strober._
import TutorialExamples._

class GCDSimSimTests(c: SimWrapper[GCD]) extends SimWrapperTester(c) with GCDTests {
  tests(c.target)
}

class GCDNastiShimTests(c: NastiShim[SimWrapper[GCD]]) extends NastiShimTester(c) with GCDTests {
  tests(c.sim.target)
}

class ParitySimTests(c: SimWrapper[Parity]) extends SimWrapperTester(c) with ParityTests {
  tests(c.target)
}

class ParityNastiShimTests(c: NastiShim[SimWrapper[Parity]]) extends NastiShimTester(c) with ParityTests {
  tests(c.sim.target)
}

class ShiftRegisterSimTests(c: SimWrapper[ShiftRegister]) extends SimWrapperTester(c) with ShiftRegisterTests { 
  tests(c.target) 
}

class ShiftRegisterNastiShimTests(c: NastiShim[SimWrapper[ShiftRegister]]) extends NastiShimTester(c) with ShiftRegisterTests {
  tests(c.sim.target)  
}

class EnableShiftRegisterSimTests(c: SimWrapper[EnableShiftRegister]) extends SimWrapperTester(c) with EnableShiftRegisterTests {  
  tests(c.target)
}

class EnableShiftRegisterNastiShimTests(c: NastiShim[SimWrapper[EnableShiftRegister]]) extends NastiShimTester(c) with EnableShiftRegisterTests {
  tests(c.sim.target)  
}

class ResetShiftRegisterSimTests(c: SimWrapper[ResetShiftRegister]) extends SimWrapperTester(c) with ResetShiftRegisterTests {
  tests(c.target)  
}

class ResetShiftRegisterNastiShimTests(c: NastiShim[SimWrapper[ResetShiftRegister]]) extends NastiShimTester(c) with ResetShiftRegisterTests {  
  tests(c.sim.target)
}

class StackSimTests(c: SimWrapper[Stack]) extends SimWrapperTester(c) with StackTests { 
  tests(c.target)
}

class StackNastiShimTests(c: NastiShim[SimWrapper[Stack]]) extends NastiShimTester(c) with StackTests {
  tests(c.sim.target)  
}

class MemorySearchSimTests(c: SimWrapper[MemorySearch]) extends SimWrapperTester(c) with MemorySearchTests {
  tests(c.target)
}

class MemorySearchNastiShimTests(c: NastiShim[SimWrapper[MemorySearch]]) extends NastiShimTester(c) with MemorySearchTests {
  tests(c.sim.target)
}

class RouterSimTests(c: SimWrapper[Router]) extends SimWrapperTester(c) with RouterTests {
  tests(c.target)  
}

class RouterNastiShimTests(c: NastiShim[SimWrapper[Router]]) extends NastiShimTester(c) with RouterTests {
  tests(c.sim.target) 
}

class RiscSimTests(c: SimWrapper[Risc]) extends SimWrapperTester(c) with RiscTests {
  tests(c.target)
}

class RiscNastiShimTests(c: NastiShim[SimWrapper[Risc]]) extends NastiShimTester(c) with RiscTests { 
  tests(c.sim.target) 
}
