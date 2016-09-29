#include "simif_zynq.h"

class GCD_t: simif_zynq_t
{
public:
  GCD_t(std::vector<std::string> args): 
    simif_zynq_t(args, "GCD", true) { }

  virtual int run() {
    uint32_t a = 64, b = 48, z = 16; //test vectors
    do {
      poke("GCD.io_a", a);
      poke("GCD.io_b", b);
      poke("GCD.io_e", cycles() == 0 ? 1 : 0);
      step(1);
    } while (cycles() <= 1 || peek("GCD.io_v") == 0);
    expect("GCD.io_z", z);
    return 0;
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  GCD_t GCD(args);
  return GCD.run();
}
