#include "simif_emul.h"
#include "Stack.h"

class Stack_emul_t:
  public simif_emul_t,
  public Stack_t
{
public:
  Stack_emul_t(size_t size): Stack_t(size) { }
};

int main(int argc, char** argv) 
{
  Stack_emul_t Stack(8);
  Stack.init(argc, argv, true);
  Stack.run();
  return Stack.finish();
}
