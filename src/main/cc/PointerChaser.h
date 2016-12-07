#include "simif.h"
#include "sim_mem.h"

class PointerChaser_t: virtual simif_t
{
public:
  PointerChaser_t(int argc, char** argv): mem(this, argc, argv) {
    address = 64;
    result = 1176;
    std::vector<std::string> args(argv + 1, argv + argc);
    for (auto &arg: args) {
      if (arg.find("+address=") == 0) {
        address = atoi(arg.c_str() + 9);
      }
      if (arg.find("+result=") == 0) {
        result = atoi(arg.c_str() + 9);
      }
    }
  }

  void run() {
    mem.init();
    target_reset(0);

    poke(io_startAddr_bits, address);
    poke(io_startAddr_valid, 1);
    do {
      step(1);
    } while (!peek(io_startAddr_ready));
    poke(io_startAddr_valid, 0);
    poke(io_result_ready, 1);
    do {
      step(1, false);
      while(!done()) mem.tick();
    } while (!peek(io_result_valid));
    expect(io_result_bits, result);
  }
private:
  sim_mem_t mem;
  uint64_t address;
  biguint_t result; // 64 bit
};
