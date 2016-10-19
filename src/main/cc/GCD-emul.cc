#include "simif_emul.h"
#include "GCD.h"

class GCD_emul_t:
  public simif_emul_t,
  public GCD_t { };

int main(int argc, char** argv)
{
  GCD_emul_t GCD;
  GCD.init(argc, argv, true);
  GCD.run();
  return GCD.finish();
}
