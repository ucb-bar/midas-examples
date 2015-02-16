#include "simif_zedboard.h"

class Parity_t: simif_zedboard_t
{
public:
  Parity_t(std::vector<std::string> args): 
    simif_zedboard_t(args, "Parity", true, true) { }
  int run() {
    uint32_t isOdd = 0; 
    for (int i = 0 ; i < 10 ; i++) {
      uint32_t bit = rand_next(2);
      poke("Parity.io_in", bit);
      step(1);
      isOdd = (isOdd + bit) % 2;
      expect("Parity.io_out", isOdd);
    }
    return 0; 
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  Parity_t Parity(args);
  return Parity.run();
}
