#include "simif_zynq.h"

class GCD_t: simif_zynq_t
{
public:
  GCD_t(std::vector<std::string> args): 
    simif_zynq_t(args, "GCD", true) { }

  virtual int run() {
    uint32_t a = 64, b = 48, z = 16; //test vectors
    do {
      uint32_t first = 0;
      if (cycles() == 0) first = 1;
      poke_port("GCD.io_a", a);
      poke_port("GCD.io_b", b);
      poke_port("GCD.io_e", first);
      step(1);
    } while (cycles() <= 1 || peek_port("GCD.io_v") == 0);
    expect_port("GCD.io_z", z);
    return 0;
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  GCD_t GCD(args);
  return GCD.run();
}
