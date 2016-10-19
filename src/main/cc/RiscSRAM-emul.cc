#include "simif_emul.h"
#include "RiscSRAM.h"

class RiscSRAM_emul_t:
  public simif_emul_t,
  public RiscSRAM_t { };

int main(int argc, char** argv) 
{
  RiscSRAM_emul_t RiscSRAM;
  RiscSRAM.init(argc, argv, true);
  RiscSRAM.run(40, 400);
  return RiscSRAM.finish();
}
