#include "simif.h"

class Parity_t: virtual simif_t
{
public:
  void run() {
    uint32_t is_odd = 0;
    for (int i = 0 ; i < 10 ; i++) {
      uint32_t bit = rand_next(2);
      poke(io_in, bit);
      step(1);
      expect(io_out, is_odd);
      is_odd = (is_odd + bit) % 2;
    }
  }
};
