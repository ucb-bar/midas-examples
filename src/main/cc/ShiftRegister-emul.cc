//See LICENSE for license details.

#include "simif_emul.h"
#include "ShiftRegister.h"

class ShiftRegister_emul_t:
  public simif_emul_t,
  public ShiftRegister_t { };

int main(int argc, char** argv) 
{
  ShiftRegister_emul_t ShiftRegister;
  ShiftRegister.init(argc, argv, true);
  ShiftRegister.run();
  return ShiftRegister.finish();
}
