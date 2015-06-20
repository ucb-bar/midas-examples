#include <sstream>
#include "simif_zynq.h"

class MemorySearch_t: simif_zynq_t
{
public:
  MemorySearch_t(std::vector<std::string> args): 
    simif_zynq_t(args, "MemorySearch", true) {}
  int run() {
    std::vector<uint32_t> list;
    list.push_back(0);
    list.push_back(4);
    list.push_back(15);
    list.push_back(14);
    list.push_back(2);
    list.push_back(5);
    list.push_back(13);
    int n = 8;
    int maxT = n * (list.size() + 3);
    for (int i = 0 ; i < n ; i++) {
      uint32_t target = rand_next(16);
      poke_port("MemorySearch.io_en", 1);
      poke_port("MemorySearch.io_target", target);
      step(1);
      poke_port("MemorySearch.io_en", 0);
      do {
        step(1);
      } while(peek_port("MemorySearch.io_done") == 0 && cycles() < maxT);
      uint32_t addr = peek_port("MemorySearch.io_address").uint();
      std::ostringstream oss;
      oss << "LOOKING FOR " << target  << " FOUND " << addr;
      expect(addr == list.size() | list[addr] == target, oss.str().c_str());
    }
    return 0; 
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  MemorySearch_t MemorySearch(args);
  return MemorySearch.run();
}
