#include <stack>
#include "simif_zynq.h"

class Stack_t: simif_zynq_t
{
public:
  Stack_t(std::vector<std::string> args, int size_): 
    simif_zynq_t(args, "Stack", true), size(size_) {}
  int run() {
    std::stack<uint32_t> stack;
    uint32_t nextDataOut = 0; 
    uint32_t dataOut = 0; 
    for (int i = 0 ; i < 16 ; i++) {
      uint32_t enable = rand_next(2);
      uint32_t push   = rand_next(2);
      uint32_t pop    = rand_next(2);
      uint32_t dataIn = rand_next(256);
  
      if (enable) {
        dataOut = nextDataOut;
        if (push && stack.size() < size) {
          stack.push(dataIn);
        } else if (pop && stack.size() > 0) {
          stack.pop();
        }
        if (stack.size()) {
          nextDataOut = stack.top();
        }
      }
      poke_port("Stack.io_pop",  pop);
      poke_port("Stack.io_push", push);
      poke_port("Stack.io_en",   enable);
      poke_port("Stack.io_dataIn", dataIn);
      step(1);
      expect_port("Stack.io_dataOut", dataOut);
    } 
  }
private:
  const int size;
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  Stack_t Stack(args, 8);
  return Stack.run();
}
