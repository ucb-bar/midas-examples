#include "simif_zynq.h"

class ShiftRegister_t: simif_zynq_t
{
public:
  ShiftRegister_t(int argc, char** argv):
    simif_zynq_t(argc, argv, true) {}

  int run() {
    std::vector<uint32_t> reg(4, 0);
    for (int i = 0 ; i < 64 ; i++) {
      uint32_t in = rand_next(2);
      poke(io_in, in);
      step(1);
      if (cycles() >= 4) expect(io_out, reg[3]);
      for (int j = 3 ; j > 0 ; j--) reg[j] = reg[j-1];
      reg[0] = in;
    } 
    return exitcode();
  }
};

int main(int argc, char** argv) 
{
  ShiftRegister_t ShiftRegister(argc, argv);
  return ShiftRegister.run();
}
