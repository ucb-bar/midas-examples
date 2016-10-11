#include "simif_zynq.h"
#include "ShiftRegister.h"

class ShiftRegister_zynq_t:
  public ShiftRegister_t,
  public simif_zynq_t { };

int main(int argc, char** argv) 
{
  ShiftRegister_zynq_t ShiftRegister;
  ShiftRegister.init(argc, argv, true);
  ShiftRegister.run();
  return ShiftRegister.finish();
}
