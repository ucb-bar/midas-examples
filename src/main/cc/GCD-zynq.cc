//See LICENSE for license details.

#include "simif_zynq.h"
#include "GCD.h"

class GCD_zynq_t:
  public simif_zynq_t,
  public GCD_t { };

int main(int argc, char** argv) 
{
  GCD_zynq_t GCD;
  GCD.init(argc, argv, true);
  GCD.run();
  return GCD.finish();
}
