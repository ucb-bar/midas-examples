#include "simif_zynq.h"

class ResetShiftRegister_t: simif_zynq_t
{
public:
  ResetShiftRegister_t(std::vector <std::string> args): 
    simif_zynq_t(args, "ResetShiftRegister", true) { }
  int run() {
    std::vector<uint32_t> ins(5, 0);
    int k = 0;
    for (int i = 0 ; i < 16 ; i++) {
      uint32_t in    = rand_next(16);
      uint32_t shift = rand_next(2);
      if (shift == 1) ins[k % 5] = in;
      poke("ResetShiftRegister.io_in",    in);
      poke("ResetShiftRegister.io_shift", shift);
      step(1);
      expect("ResetShiftRegister.io_out", cycles() < 4 ? 0 : ins[(k + 1) % 5]);
      if (shift == 1) k++;
    }
    return 0; 
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  ResetShiftRegister_t ResetShiftRegister(args);
  return ResetShiftRegister.run();
}
