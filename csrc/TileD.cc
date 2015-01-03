#include "debug_api.h"
#include <fstream>
#include <sstream>
#include <iostream>
#include <stdlib.h>

class Tile_t: debug_api_t
{
public:
  Tile_t(int argc, char** argv): debug_api_t("Tile", false) {
    for (int i = 0 ; i < argc ; i++) {
      std::string arg = argv[i];
      if (arg.substr(0, 12) == "+max-cycles=") {
        timeout = atoll(argv[i]+12);
      } else if (arg.substr(0, 9) == "+loadmem=") {
        filename = argv[i]+9;
      }
    }
    testname = filename.substr(filename.rfind("/")+1);
    load_mem();
  }
  void serve_mem() {
    static bool memrw = false;
    static uint64_t memtag = 0;
    static uint64_t memaddr = 0;
    if (peekq_valid("Tile.io_mem_req_cmd_bits_rw") &&
        peekq_valid("Tile.io_mem_req_cmd_bits_tag") &&
        peekq_valid("Tile.io_mem_req_cmd_bits_addr")) {    
      memrw   = peekq("Tile.io_mem_req_cmd_bits_rw");
      memtag  = peekq("Tile.io_mem_req_cmd_bits_tag");
      memaddr = peekq("Tile.io_mem_req_cmd_bits_addr");
      step(1, false);
      if (!memrw) {
        uint64_t memdata = read(memaddr);
        pokeq("Tile.io_mem_resp_bits_data", memdata);
        pokeq("Tile.io_mem_resp_bits_tag", memtag);
      }
    }
    if (peekq_valid("Tile.io_mem_req_data_bits_data")) {
      write(memaddr, peekq("Tile.io_mem_req_data_bits_data"));
    }
  }
  void run() {
    do {
      serve_mem();
      step(1, false);
    } while (peek("Tile.io_htif_host_tohost") == 0 && t < timeout);
    uint64_t tohost = peek("Tile.io_htif_host_tohost");
    std::ostringstream reason;
    std::ostringstream result;
    if (t > timeout) {
      reason << "timeout";
      result << "FAILED";
    } else if (tohost != 1) {
      reason << "tohost = " << tohost;
      result << "FAILED";
    } else {
      reason << "tohost = " << tohost;
      result <<"PASSED";
    }
    std::cout << "ISA: " << testname << std::endl;
    std::cout << "*** " << result.str() << " *** (" << reason.str() << ") ";
    std::cout << "after " << t << " simulation cycles" << std::endl;
  }
private:
  std::map<uint64_t, uint64_t> mem;
  std::string testname;
  std::string filename;
  uint64_t timeout;

  void load_mem() {
    std::ifstream in(filename.c_str());
    if (!in) {
      std::cerr << "could not open " << filename << std::endl;
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
  Tile_t Tile(argc, argv);
  Tile.run();
  return 0;
}
