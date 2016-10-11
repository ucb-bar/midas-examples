#include "simif_emul.h"
#include "Parity.h"

class Parity_emul_t:
  public simif_emul_t,
  public Parity_t { };

int main(int argc, char** argv) 
{
  Parity_emul_t Parity;
  Parity.init(argc, argv, true);
  Parity.run();
  return Parity.finish();
}
