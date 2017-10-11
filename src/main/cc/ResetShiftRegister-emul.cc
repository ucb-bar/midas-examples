//See LICENSE for license details.

#include "simif_emul.h"
#include "ResetShiftRegister.h"

class ResetShiftRegister_emul_t:
  public simif_emul_t,
  public ResetShiftRegister_t { };

int main(int argc, char** argv) 
{
  ResetShiftRegister_emul_t ResetShiftRegister;
  ResetShiftRegister.init(argc, argv, true);
  ResetShiftRegister.run();
  return ResetShiftRegister.finish();
}
