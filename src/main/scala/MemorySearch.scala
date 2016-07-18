package examples

import chisel3._

class MemorySearch extends Module {
  val io = new Bundle {
    val target  = UInt(INPUT,  4)
    val en      = Bool(INPUT)
    val done    = Bool(OUTPUT)
    val address = UInt(OUTPUT, 3)
  }
  val index = Reg(init = UInt(0, width = 3))
  val elts  = Vec(UInt(0), UInt(4), UInt(15), UInt(14),
                  UInt(2), UInt(5), UInt(13))
  val elt   = elts(index)
  val over  = !io.en && ((elt === io.target) || (index === UInt(7)))
  when (io.en) {
    index := UInt(0)
  } .elsewhen (!over) {
    index := index + UInt(1)
  }
  io.done    := over
  io.address := index
}
