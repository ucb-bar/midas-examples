#include <stack>
#include "simif_zynq.h"

class Stack_t: simif_zynq_t
{
public:
  Stack_t(int argc, char** argv, size_t size_):
    simif_zynq_t(argc, argv, true), size(size_) {}
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
      poke(io_pop,    pop);
      poke(io_push,   push);
      poke(io_en,     enable);
      poke(io_dataIn, dataIn);
      step(1);
      expect(io_dataOut, dataOut);
    }

    return exitcode();
  }
private:
  const size_t size;
};

int main(int argc, char** argv) 
{
  Stack_t Stack(argc, argv, 8);
  return Stack.run();
}
