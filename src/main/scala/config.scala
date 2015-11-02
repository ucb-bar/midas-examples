package StroberExample

import Chisel._
import strober._
import junctions._

object SimParams {
  val mask = (key: Any, site: View, here: View, up: View) => key match {
    case SampleNum    => 30 
    case TraceMaxLen  => 1024
    case DaisyWidth   => 32
    case ChannelLen   => 16
    case ChannelWidth => 32
  }
  def apply(params: Parameters = Parameters.empty) = params alter mask
}

object NastiParams {
  val mask = (key: Any, site: View, here: View, up: View) => key match {
    case NASTIAddrBits => site(NASTIName) match {
      case "Master" => 32
      case "Slave"  => 32
    }
    case NASTIDataBits => site(NASTIName) match {
      case "Master" => 32 
      case "Slave"  => 64
    }
    case NASTIIdBits => site(NASTIName) match {
      case "Master" => 12
      case "Slave"  => 6
    }
    case NASTIAddrSizeBits => 10

    case MIFAddrBits     => 32
    case MIFDataBits     => 16 << 3
    case MIFTagBits      => 5
    case MIFDataBeats    => 1
    case MemBlockBytes   => 16
    case MemAddrSizeBits => 28
    case MemMaxCycles    => 256
  }
  def apply(params: Parameters = Parameters.empty) = params alter mask
}
