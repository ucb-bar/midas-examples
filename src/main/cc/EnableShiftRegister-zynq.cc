//See LICENSE for license details.

#include "simif_zynq.h"
#include "EnableShiftRegister.h"

class EnableShiftRegister_zynq_t:
  public simif_zynq_t,
  public EnableShiftRegister_t { };

int main(int argc, char** argv) 
{
  EnableShiftRegister_zynq_t EnableShiftRegister;
  EnableShiftRegister.init(argc, argv, true);
  EnableShiftRegister.run();
  return EnableShiftRegister.finish();
}
