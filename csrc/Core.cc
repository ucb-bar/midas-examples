#include <fstream>
#include <map>
#include "simif_zynq.h"

class Core_t: simif_zynq_t
{
public:
  Core_t(std::vector<std::string> args):
    simif_zynq_t(args, "Core", false) {
    mem = new uint8_t[(1 << 23) - 1]; // 8MB
    for (auto &arg: args) {
      if (arg.find("+max-cycles=") == 0) {
        max_cycles = atoi(arg.c_str()+12);
      }
    } 
  }

  ~Core_t() {
    delete[] mem;
  }

  int run() {
    load_mem();
    uint64_t tohost = 0;
    poke_port("Core.io_stall", 0);
    do {
      uint64_t iaddr = peek_port("Core.io_icache_addr").uint();
      uint64_t daddr = (peek_port("Core.io_dcache_addr").uint() >> 2) << 2;
      uint64_t data  = peek_port("Core.io_dcache_din").uint();
      uint64_t dwe   = peek_port("Core.io_dcache_we").uint();
      bool ire = peek_port("Core.io_icache_re") == 1;
      bool dre = peek_port("Core.io_dcache_re") == 1;

      step(1);

      if (dwe > 0) {
        write_mem(daddr, data, dwe);
      } else if (ire) {
        poke_port("Core.io_icache_dout", read_mem(iaddr));
      } else if (dre) {
        poke_port("Core.io_dcache_dout", read_mem(daddr));
      }

      tohost = peek_port("Core.io_host_tohost").uint();
    } while (tohost == 0 && cycles() <= max_cycles);
    int exitcode = tohost >> 1;
    if (exitcode) {
      fprintf(stdout, "*** FAILED *** (code = %d) after %llu cycles\n", exitcode, cycles());
    } else if (cycles() > max_cycles) {
      fprintf(stdout, "*** FAILED *** (timeout) after %llu cycles\n", cycles());
    } else {
      fprintf(stdout, "*** PASSED *** after %llu cycles\n", cycles());
    }
    return exitcode;
  }

private:
  uint8_t *mem;
  uint64_t max_cycles;

  void load_mem() {
    std::ifstream file(loadmem.c_str());
    std::string line;
    if (file) {
      uint8_t *m = (uint8_t *) mem;
      while (getline(file, line)) {
        for (ssize_t i = line.length()-2, j = 0 ; i >= 0 ; i -= 2, j++) {
          m[j] = (parse_nibble(line[i]) << 4) | parse_nibble(line[i+1]);
        }
        m += line.length()/2;
      }
    }  
  }

  uint32_t read_mem(size_t addr) {
    uint32_t data = 0;
    for (size_t i = 0 ; i < 4 ; i++) {
      data |= mem[addr+i] << (8 * i);
    } 
    return data;
  }

  void write_mem(size_t addr, uint32_t data, size_t mask = 0xf) {
    for (ssize_t i = 3 ; i >= 0 ; i--) {
      if ((mask >> i) & 0x1) {
        mem[addr+i] = (uint8_t) (data >> (8 * i)) & 0xff;
      }
    }
  }
};

int main(int argc, char** argv) {
  std::vector<std::string> args(argv + 1, argv + argc);
  Core_t Core(args);
  return Core.run();
}
