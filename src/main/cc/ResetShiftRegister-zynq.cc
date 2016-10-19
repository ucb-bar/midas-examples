#include "simif_zynq.h"
#include "ResetShiftRegister.h"

class ResetShiftRegister_zynq_t:
  public simif_zynq_t,
  public ResetShiftRegister_t { };

int main(int argc, char** argv) 
{
  ResetShiftRegister_zynq_t ResetShiftRegister;
  ResetShiftRegister.init(argc, argv, true);
  ResetShiftRegister.run();
  return ResetShiftRegister.finish();
}
