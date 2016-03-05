package StroberExamples

import Chisel._
import cde.{Parameters, Config}
import strober._
import junctions._

class SimConfig extends Config(
  (key, site, here) => key match {
    case SampleNum    => 30 
    case TraceMaxLen  => 1024
    case DaisyWidth   => 32
    case ChannelLen   => 16
    case ChannelWidth => 32
  }
)

class NastiConfig extends Config(new SimConfig ++ new Config(
  (key, site, here) => key match {
    case NastiKey => try {
      site(NastiType) match {
        case NastiMaster => NastiParameters(32, 32, 12)
        case NastiSlave  => NastiParameters(64, 32, 6)
      }
    } catch {
      case e: cde.ParameterUndefinedException => 
        throw new scala.MatchError(key)
    } 
    case NastiAddrSizeBits => 10

    case MIFAddrBits     => 32
    case MIFDataBits     => 16 << 3
    case MIFTagBits      => 5
    case MIFDataBeats    => 1
    case LineSize        => 16
    case MemAddrSizeBits => 28
    case MemMaxCycles    => 256
  })
)

class MiniSimConfig extends Config(new SimConfig ++ new mini.MiniConfig)
class MiniNastiConfig extends Config(new NastiConfig ++ new mini.MiniConfig)
