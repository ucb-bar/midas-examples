#include "Risc.h"

class RiscSRAM_t: public Risc_t
{
protected:
  virtual void init_app(app_t& app) {
    long_app(app);
  }
};
