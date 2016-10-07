#include "simif_zynq.h"

class Parity_t: simif_zynq_t
{
public:
  Parity_t(int argc, char** argv):
    simif_zynq_t(argc, argv, true) { }
  int run() {
    uint32_t is_odd = 0;
    for (int i = 0 ; i < 10 ; i++) {
      uint32_t bit = rand_next(2);
      poke(io_in, bit);
      step(1);
      expect(io_out, is_odd);
      is_odd = (is_odd + bit) % 2;
    }
    return exitcode();
  }
};

int main(int argc, char** argv) 
{
  Parity_t Parity(argc, argv);
  return Parity.run();
}
