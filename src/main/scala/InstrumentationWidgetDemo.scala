package midas
package examples

import chisel3._

import midas.endpoints.InstrumentationIO

class testBundle extends Bundle {
  val counter = Output(UInt(8.W))

  val in1 = Output(UInt(8.W))
  val in2 = Output(UInt(8.W))
  val test = Output(UInt(8.W))
  val enable = Output(UInt(8.W))
}

class testBundle2 extends Bundle {
  val test = Output(UInt(8.W))
  val test2 = Output(UInt(8.W))
}

// The actual design without any instrumentation stuff.
class RegisteredAdder extends Module {
  val io = IO(new Bundle {
    val in1  = Input(UInt(8.W))
    val in2  = Input(UInt(8.W))
    val enable = Input(Bool())
    val out = Output(UInt(8.W))
  })

  val out = RegInit(0.U(8.W))
  io.out := out

  when (io.enable) {
    out := io.in1 + io.in2
  }
}

// Design with top-level instrumentation.
class InstrumentationWidgetDemo extends Module {
  val io = IO(new Bundle {
    val in1  = Input(UInt(8.W))
    val in2  = Input(UInt(8.W))
    val enable = Input(Bool())
    val out = Output(UInt(8.W))

    val instrumentation = new InstrumentationIO({
      val b = new Bundle{
        val counter = Output(UInt(8.W))
        val in1 = Output(UInt(8.W))
        val in2 = Output(UInt(8.W))
        val test = Output(UInt(8.W))
        val enable = Output(UInt(8.W))
      }
      println(s"b.elem = ${b.elements}")
      b
    })
    //~ val instrumentation = new InstrumentationIO(new testBundle)
    val instrumentation2 = new InstrumentationIO(new testBundle2)
  })

  // The actual design.
  val design = Module(new RegisteredAdder)
  design.io.in1 := io.in1
  design.io.in2 := io.in2
  design.io.enable := io.enable
  design.io.out <> io.out

  // The instrumentation.

  // Useful counter for debugging info.
  val counter = chisel3.util.Counter(255)
  counter.inc()
  io.instrumentation.counter := counter.value
  io.instrumentation.in1 := io.in1
  io.instrumentation.in2 := io.in2
  io.instrumentation.enable := design.io.enable
  io.instrumentation.test := design.io.out

  io.instrumentation2.test := design.io.out
  io.instrumentation2.test2 := design.io.out + 1.U
}
