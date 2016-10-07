#include "simif_zynq.h"

class EnableShiftRegister_t: simif_zynq_t
{
public:
  EnableShiftRegister_t(int argc, char** argv):
    simif_zynq_t(argc, argv, true) { }
 
  virtual int run() {
    std::vector<uint32_t> reg(4, 0);
    for (int i = 0 ; i < 16 ; i++) {
      uint32_t in    = rand_next(2);
      uint32_t shift = rand_next(2);
      poke(io_in,    in);
      poke(io_shift, shift);
      step(1);
      expect(io_out, reg[3]);
      if (shift) {
        for (int j = 3 ; j > 0 ; j--) reg[j] = reg[j-1];
        reg[0] = in;
      }
    }
    return exitcode();
  }
};

int main(int argc, char** argv) 
{
  EnableShiftRegister_t EnableShiftRegister(argc, argv);
  return EnableShiftRegister.run();
}
