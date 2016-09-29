package examples

import chisel3._
import chisel3.util._

class ReadCmd extends Bundle {
  val addr = UInt(width = 32);
}

class WriteCmd extends ReadCmd {
  val data = UInt(width = 32)
}

class Packet extends Bundle {
  val header = UInt(width = 8)
  val body   = Bits(width = 64)
}

class RouterIO(n: Int) extends Bundle {
  override def cloneType = new RouterIO(n).asInstanceOf[this.type]
  val reads   = new DeqIO(new ReadCmd())
  val replies = new EnqIO(UInt(width = 8))
  val writes  = new DeqIO(new WriteCmd())
  val in      = new DeqIO(new Packet())
  val outs    = Vec(n, new EnqIO(new Packet()))
}

class Router extends Module {
  val depth = 32
  val n     = 4
  val io    = new RouterIO(n)
  val tbl   = Mem(depth, UInt(width = BigInt(n).bitLength))
  io.reads.init()
  io.replies.init()
  io.writes.init()
  io.in.init()
  io.outs foreach (out => out.init())
  when(io.reads.valid && io.replies.ready) { 
    io.replies enq tbl(io.reads.deq().addr)
  } .elsewhen(io.writes.valid) { 
    val cmd = io.writes.deq()
    tbl(cmd.addr) := cmd.data
  } .elsewhen(io.in.valid) {
    val idx = tbl(io.in.bits.header)
    when (io.outs(idx).ready) {
      io.outs(idx) enq io.in.deq()
    }
  }
}
