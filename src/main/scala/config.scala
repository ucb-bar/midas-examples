package StroberExample

import Chisel._
import cde.{Parameters, View}
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
    case NastiKey => site(NastiName) match {
      case "Master" => NastiParameters(32, 32, 12)
      case "Slave"  => NastiParameters(64, 32, 6)
    } 
    case NastiAddrSizeBits => 10

    case MIFAddrBits     => 32
    case MIFDataBits     => 16 << 3
    case MIFTagBits      => 5
    case MIFDataBeats    => 1
    case LineSize        => 16
    case MemAddrSizeBits => 28
    case MemMaxCycles    => 256
  }
  def apply(params: Parameters = Parameters.empty) = params alter mask
}
