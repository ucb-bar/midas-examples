//See LICENSE for license details.

#include "simif_emul.h"
#include "Risc.h"

class Risc_emul_t:
  public simif_emul_t,
  public Risc_t { };

int main(int argc, char** argv) 
{
  Risc_emul_t Risc;
  Risc.init(argc, argv, true);
  Risc.run(4, 10);
  return Risc.finish();
}
