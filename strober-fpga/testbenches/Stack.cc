#include <stack>
#include "simif_zynq.h"

class Stack_t: simif_zynq_t
{
public:
  Stack_t(std::vector<std::string> args, size_t size_): 
    simif_zynq_t(args, "Stack", true), size(size_) {}
  int run() {
    std::stack<uint32_t> stack;
    uint32_t nextDataOut = 0; 
    for (int i = 0 ; i < 16 ; i++) {
      uint32_t enable = rand_next(2);
      uint32_t push   = rand_next(2);
      uint32_t pop    = rand_next(2);
      uint32_t dataIn = rand_next(256);
      uint32_t dataOut = nextDataOut;
  
      if (enable) {
        if (stack.size()) nextDataOut = stack.top();
        if (push && stack.size() < size) {
          stack.push(dataIn);
        } else if (pop && stack.size() > 0) {
          stack.pop();
        }
      }
      poke("Stack.io_pop",  pop);
      poke("Stack.io_push", push);
      poke("Stack.io_en",   enable);
      poke("Stack.io_dataIn", dataIn);
      step(1);
      expect("Stack.io_dataOut", dataOut);
    }

    return 0; 
  }
private:
  const size_t size;
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  Stack_t Stack(args, 8);
  return Stack.run();
}
