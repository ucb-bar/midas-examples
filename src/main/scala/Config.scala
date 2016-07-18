package StroberExamples

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

class ZynqConfig extends Config(new SimConfig ++ new Config(
  (key, site, here) => key match {
    case MasterNastiKey => NastiParameters(32, 32, 12)
    case SlaveNastiKey  => NastiParameters(64, 32, 6)
    // case MemMaxCycles    => 256
  })
)
