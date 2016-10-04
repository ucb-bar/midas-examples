#include "Risc.h"

class RiscSRAM_t: public Risc_t
{
public:
  RiscSRAM_t(std::vector<std::string> args): Risc_t(args) {}
protected:
  virtual void init(app_t& app) {
    long_app(app);
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  RiscSRAM_t RiscSRAM(args);
  return RiscSRAM.run(40, 400);
}
