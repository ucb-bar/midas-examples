#include "simif_zynq.h"
#include "Risc.h"

class Risc_zynq_t:
  public simif_zynq_t,
  public Risc_t { };

int main(int argc, char** argv) 
{
  Risc_zynq_t Risc;
  Risc.init(argc, argv, true);
  Risc.run(4, 10);
  return Risc.finish();
}
