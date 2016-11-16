package StroberExamples

import midas._
import midas.widgets._
import dram_midas._
import junctions._
import cde._

class PointerChaserConfig extends Config(
  (key, site, here) => key match {
    case MemSize => Dump("MEM_SIZE", BigInt(1 << 30)) // 1 GB
    case NMemoryChannels => Dump("N_MEM_CHANNELS", 1)
    case CacheBlockBytes => Dump("CACHE_BLOCK_BYTES", 64)
    case CacheBlockOffsetBits => chisel3.util.log2Up(here(CacheBlockBytes))
    case NastiKey => {
      Dump("MEM_STRB_BITS", 64 / 8)
      NastiParameters(
        dataBits = Dump("MEM_DATA_BITS", 64),
        addrBits = Dump("MEM_ADDR_BITS", 32),
        idBits = Dump("MEM_ID_BITS", 3))
    }
  }
)

class WithLBPipe extends Config(
  (pname, _, _) => pname match {
    case MemModelKey => Some((p: Parameters) => new MidasMemModel(
      new LatencyPipeConfig(new BaseParams(maxReads = 16, maxWrites = 16)))(p))
  }
)

class ZynqConfigWithMemModel extends Config(
  new WithLBPipe ++ new midas.ZynqConfig)
class ZynqConfigWithMemModelAndSnapshot extends Config(
  new WithLBPipe ++ new midas.ZynqConfigWithSnapshot)
