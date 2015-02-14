#include <fstream>
#include "api.h"

class Core_t: API_t
{
public:
  Core_t(std::vector<std::string> args):
    API_t(args, "Core", false, false) { }
  int run() {
    load_mem();
    poke("Core.io_stall", 1);
    step(1);
    poke("Core.io_stall", 0);
    uint64_t tohost = 0;
    do {
      uint64_t iaddr = peek("Core.io_icache_addr");
      uint64_t daddr = (peek("Core.io_dcache_addr") >> 2) << 2;
      uint64_t data  = peek("Core.io_dcache_din");
      uint64_t dwe   = peek("Core.io_dcache_we");
      bool ire = peek("Core.io_icache_re") == 1;
      bool dre = peek("Core.io_dcache_re") == 1;

      step(1);

      if (dwe > 1) {
        write_mem(daddr, data, dwe);
      } else if (ire) {
        uint64_t inst = read_mem(iaddr);
        poke("Core.io_icache_dout", inst);
      } else if (dre) {
        uint64_t data = read_mem(daddr);
        poke("Core.io_dcache_dout", data);
      }
      tohost = peek("Core.io_host_tohost"); 
    } while (tohost == 0 && !timeout());

    int exitcode = tohost >> 1;
    if (exitcode) {
      fprintf(stderr, "*** FAILED *** (code = %d) after %llu cycles\n", exitcode, cycles());
    } else if (timeout()) {
      fprintf(stderr, "*** FAILED *** (timeout) after %llu cycles\n", cycles());
    } else {
      fprintf(stderr, "*** PASSED *** after %llu cycles\n", cycles());
    }
    return exitcode;
  }
private:
  std::map<uint64_t, uint64_t> mem;

  void load_mem() {
    std::ifstream in(loadmem.c_str());
    if (!in) {
      fprintf(stderr, "could not open %s\n", loadmem.c_str());
      exit(-1);
    }

    std::string line;
    int i = 0;
    while (std::getline(in, line)) {
      #define parse_nibble(c) ((c) >= 'a' ? (c)-'a'+10 : (c)-'0')
      uint64_t base = (i * line.length()) / 2;
      uint64_t offset = 0;
      for (int k = line.length() - 2 ; k >= 0 ; k -= 2) {
        uint64_t addr = base + offset;
        uint64_t data = (parse_nibble(line[k]) << 4) | parse_nibble(line[k+1]);
        mem[addr] = data;
        offset += 1;
      }
      i += 1;
    }
  }

  uint64_t read_mem(uint64_t addr) {
    uint64_t data = 0;
    for (int i = 0 ; i < 4 ; i++) {
      data |= mem[addr+i] << (8*i);
    }
    return data;
  }

  void write_mem(uint64_t addr, uint64_t data, uint64_t mask) {
    for (int i = 3 ; i >= 0 ; i--) {
      if (((mask >> i) & 1) > 0) {
        mem[addr+i] = (data >> (8*i)) & 0xff;
      }
    }
  }
};

int main(int argc, char** argv) {
  std::vector<std::string> args(argv + 1, argv + argc);
  Core_t Core(args);
  return Core.run();
}
