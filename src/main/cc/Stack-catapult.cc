#include "simif_catapult.h"
#include "Stack.h"

class Stack_catapult_t:
  public simif_catapult_t,
  public Stack_t
{
public:
  Stack_catapult_t(size_t size): Stack_t(size) { }
};

int main(int argc, char** argv) 
{
  Stack_catapult_t Stack(8);
  Stack.init(argc, argv, true);
  Stack.run();
  return Stack.finish();
}
