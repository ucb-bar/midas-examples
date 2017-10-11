//See LICENSE for license details.

#include "simif_zynq.h"
#include "Parity.h"

class Parity_zynq_t:
  public simif_zynq_t,
  public Parity_t { };

int main(int argc, char** argv) 
{
  Parity_zynq_t Parity;
  Parity.init(argc, argv, true);
  Parity.run();
  return Parity.finish();
}
