#include "Risc.h"

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  Risc_t Risc(args);
  return Risc.run(4, 10);
}
