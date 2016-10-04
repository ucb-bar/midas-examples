#include "simif_zynq.h"

class Parity_t: simif_zynq_t
{
public:
  Parity_t(std::vector<std::string> args): 
    simif_zynq_t(args, true) { }
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
  std::vector<std::string> args(argv + 1, argv + argc);
  Parity_t Parity(args);
  return Parity.run();
}
