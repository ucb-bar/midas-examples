#include "simif_catapult.h"
#include "RiscSRAM.h"

class RiscSRAM_catapult_t:
  public simif_catapult_t,
  public RiscSRAM_t { };

int main(int argc, char** argv) 
{
  RiscSRAM_catapult_t RiscSRAM;
  RiscSRAM.init(argc, argv, true);
  RiscSRAM.run(40, 400);
  return RiscSRAM.finish();
}
