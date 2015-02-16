#include <fstream>
#include <sstream>
#include <iostream>
#include <stdlib.h>
#include "simif_zedboard.h"

class TileD_t: simif_zedboard_t
{
public:
  TileD_t(std::vector<std::string> args):  
    simif_zedboard_t(args, "TileD", false, false) { }
  void serve_mem() {
    static bool memrw = false;
    static uint64_t memtag = 0;
    static uint64_t memaddr = 0;
    if (peekq_valid("TileD.io_mem_req_cmd_bits_rw") &&
        peekq_valid("TileD.io_mem_req_cmd_bits_tag") &&
        peekq_valid("TileD.io_mem_req_cmd_bits_addr")) {    
      memrw   = peekq("TileD.io_mem_req_cmd_bits_rw");
      memtag  = peekq("TileD.io_mem_req_cmd_bits_tag");
      memaddr = peekq("TileD.io_mem_req_cmd_bits_addr");
      step(1);
      if (!memrw) {
        uint64_t memdata = read(memaddr);
        pokeq("TileD.io_mem_resp_bits_data", memdata);
        pokeq("TileD.io_mem_resp_bits_tag", memtag);
      }
    }
    if (peekq_valid("TileD.io_mem_req_data_bits_data")) {
      write(memaddr, peekq("TileD.io_mem_req_data_bits_data"));
    }
  }
  int run() {
    load_mem();
    uint64_t tohost = 0;
    do {
      serve_mem();
      step(1);
      tohost = peek("TileD.io_htif_host_tohost");
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
      std::cerr << "could not open " << loadmem << std::endl;
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

  uint64_t read(uint64_t addr) {
    uint64_t data = 0;
    for (int i = 0 ; i < 4 ; i++) {
      data |= mem[addr+i] << (8*i);
    }
    return data;
  }

  void write(uint64_t addr, uint64_t data) {
    for (int i = 3 ; i >= 0 ; i--) {
      mem[addr+i] = (data >> (8*i)) & 0xff;
    }
  }
};

int main(int argc, char** argv) {
  std::vector<std::string> args(argv + 1, argv + argc);
  TileD_t TileD(args);
  return TileD.run();
}
