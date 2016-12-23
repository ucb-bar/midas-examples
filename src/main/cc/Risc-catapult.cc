#include "simif_catapult.h"
#include "Risc.h"

class Risc_catapult_t:
  public simif_catapult_t,
  public Risc_t { };

int main(int argc, char** argv) 
{
  Risc_catapult_t Risc;
  Risc.init(argc, argv, true);
  Risc.run(4, 10);
  return Risc.finish();
}
