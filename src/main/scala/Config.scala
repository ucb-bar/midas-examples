package StroberExamples

import strober._
import junctions._
import cde._
import midas_widgets._
import dram_midas._

case object PAddrBits extends Field[Int]

class PointerChaserConfig extends Config(
  (key, site, here) => key match {
    case PAddrBits => 32
    case MIFDataBits => 64
    case CacheBlockBytes => Dump("CACHE_BLOCK_BYTES", 64)
    case CacheBlockOffsetBits => chisel3.util.log2Up(here(CacheBlockBytes))
    case MIFAddrBits => site(PAddrBits) - site(CacheBlockOffsetBits)
    case MIFDataBeats => 8
    case MIFTagBits => 3
    case MemSize => Dump("MEM_SIZE", BigInt(1 << 30)) // 1 GB
    case NMemoryChannels => Dump("N_MEM_CHANNELS", 1)
    case NastiKey => {
      Dump("MEM_STRB_BITS", site(MIFDataBits) / 8)
      NastiParameters(
        dataBits = Dump("MEM_DATA_BITS", site(MIFDataBits)),
        addrBits = Dump("MEM_ADDR_BITS", site(PAddrBits)),
        idBits = Dump("MEM_ID_BITS", site(MIFTagBits)))
    }
  }
)

class SimConfig extends Config(
  (key, site, here) => key match {
    case TraceMaxLen  => 1024
    case DaisyWidth   => 32
    case SRAMChainNum => 1
    case ChannelLen   => 16
    case ChannelWidth => 32
  }
)

class ZynqConfig extends Config(new SimConfig ++ new Config(
  (key, site, here) => key match {
    case CtrlNastiKey => NastiParameters(32, 32, 12)
    case SlaveNastiKey  => NastiParameters(64, 32, 6)
    case MemModelKey => Some(new LatencyPipeConfig(new BaseParams(maxReads = 16, maxWrites = 16)))
    //case MemModelKey => None
    // case MemMaxCycles    => 256
  })
)


class WithLBPipe extends Config(
  (pname,_,_) => pname match {
    case MemModelKey => Some(new LatencyPipeConfig(new BaseParams(maxReads = 16, maxWrites = 16)))
  }
)

class ZynqConfigWithMemModel extends Config(new WithLBPipe ++ new ZynqConfig)

