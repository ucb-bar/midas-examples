#include "simif_zynq.h"

class ShiftRegister_t: simif_zynq_t
{
public:
  ShiftRegister_t(std::vector<std::string> args): 
    simif_zynq_t(args, "ShiftRegister", true) {}

  int run() {
    std::vector<uint32_t> reg(4, 0);
    for (int i = 0 ; i < 64 ; i++) {
      uint32_t in = rand_next(2);
      poke_port("ShiftRegister.io_in", in);
      step(1);
      for (int j = 3 ; j > 0 ; j--) {
        reg[j] = reg[j-1];
      }
      reg[0] = in;
      if (cycles() >= 4) expect_port("ShiftRegister.io_out", reg[3]);
    } 
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  ShiftRegister_t ShiftRegister(args);
  return ShiftRegister.run();
}
