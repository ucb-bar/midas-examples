#include "simif_zynq.h"
#include "RiscSRAM.h"

class RiscSRAM_zynq_t:
  public simif_zynq_t,
  public RiscSRAM_t { };

int main(int argc, char** argv) 
{
  RiscSRAM_zynq_t RiscSRAM;
  RiscSRAM.init(argc, argv, true);
  RiscSRAM.run(40, 400);
  return RiscSRAM.finish();
}
