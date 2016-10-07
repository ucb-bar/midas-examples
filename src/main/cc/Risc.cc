#include "Risc.h"

int main(int argc, char** argv) 
{
  Risc_t Risc(argc, argv);
  return Risc.run(4, 10);
}
