#include "simif_catapult.h"
#include "GCD.h" // from strober-examples

class GCD_catapult_t:
  public simif_catapult_t,
  public GCD_t { };

int main(int argc, char** argv)
{
  GCD_catapult_t GCD;
  GCD.init(argc, argv, true);
  GCD.run();
  return GCD.finish();
}
