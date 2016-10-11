#include "simif_zynq.h"
#include "Stack.h"

class Stack_zynq_t:
  public simif_zynq_t,
  public Stack_t
{
public:
  Stack_zynq_t(size_t size): Stack_t(size) { }
};

int main(int argc, char** argv) 
{
  Stack_zynq_t Stack(8);
  Stack.init(argc, argv, true);
  Stack.run();
  return Stack.finish();
}
