#include "simif_zedboard.h"
#include <iostream>

class GCD_t: simif_zedboard_t
{
public:
  GCD_t(std::vector<std::string> args, std::string prefix): 
    simif_zedboard_t(args, prefix, true, false) { }

  virtual int run() {
    uint32_t a = 64, b = 48, z = 16; //test vectors
std::cout << "hey!" << std::endl;
    do {
      uint32_t first = 0;
      if (cycles() == 0) first = 1;
      simif_t::poke("GCD.io_a", a);
      simif_t::poke("GCD.io_b", b);
      simif_t::poke("GCD.io_e", first);
      step(1);
    } while (cycles() <= 1 || simif_t::peek("GCD.io_v") == 0);
    expect("GCD.io_z", z);
    return 0;
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
std::cout << "hey!" << std::endl;
  GCD_t GCD(args, "GCD");
  return GCD.run();
}
