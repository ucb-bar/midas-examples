#include "simif_emul.h"
#include "EnableShiftRegister.h"

class EnableShiftRegister_emul_t:
  public simif_emul_t,
  public EnableShiftRegister_t { };

int main(int argc, char** argv) 
{
  EnableShiftRegister_emul_t EnableShiftRegister;
  EnableShiftRegister.init(argc, argv, true);
  EnableShiftRegister.run();
  return EnableShiftRegister.finish();
}
