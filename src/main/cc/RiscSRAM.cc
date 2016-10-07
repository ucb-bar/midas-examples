#include "Risc.h"

class RiscSRAM_t: public Risc_t
{
public:
  RiscSRAM_t(int argc, char** argv): Risc_t(argc, argv) {}
protected:
  virtual void init(app_t& app) {
    long_app(app);
  }
};

int main(int argc, char** argv) 
{
  RiscSRAM_t RiscSRAM(argc, argv);
  return RiscSRAM.run(40, 400);
}
