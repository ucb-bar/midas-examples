#include "simif_zedboard.h"

class ResetShiftRegister_t: simif_zedboard_t
{
public:
  ResetShiftRegister_t(std::vector <std::string> args): 
    simif_zedboard_t(args, "ResetShiftRegister", true, true) { }
  int run() {
    std::vector<uint32_t> ins(4, 0);
    int k = 0;
    for (int i = 0 ; i < 16 ; i++) {
      uint32_t in    = rand_next(2);
      uint32_t shift = rand_next(2);
      if (shift == 1)
        ins[k % 5] = in;
      poke("ResetShiftRegister.io_in",    in);
      poke("ResetShiftRegister.io_shift", shift);
      step(1);
      if (shift)
        k++;
      int expected = 0;
      if (t > 4) expected = ins[(k + 1) % 4];
      expect("ResetShiftRegister.io_out", expected);
    }
    return 0; 
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  ResetShiftRegister_t ResetShiftRegister;
  return ResetShiftRegister.run();
}
