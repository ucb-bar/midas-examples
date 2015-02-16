#include "simif_zedboard.h"

class Risc_t: simif_zedboard_t
{
public:
  Risc_t(std::vector<std::string> args): 
    simif_zedboard_t(args, "Risc", true, true) { }

  virtual int run() {
    std::vector<uint32_t> app;
    app.push_back(I(1, 1, 0, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 255, 1, 0));
    wr(0, 0);
    for (int addr = 0 ; addr < app.size() ; addr++) {
      wr(addr, app[addr]);
    }
    boot();
    int k = 0;
    do {
      tick(); k += 1;
    } while (peek("Risc.io_valid") == 0 && k < 10);
    expect(k < 10, "TIME LIMIT");
    expect("Risc.io_out", 4);
    return 0;
  }
private:
  void wr(uint32_t addr, uint32_t data) {
    poke("Risc.io_isWr", 1);
    poke("Risc.io_wrAddr", addr);
    poke("Risc.io_wrData", data);
    step(1);
  }
  void boot() {
    poke("Risc.io_isWr", 0);
    poke("Risc.io_boot", 1);
    step(1);
  }
  void tick() {
    poke("Risc.io_isWr", 0);
    poke("Risc.io_boot", 0);
    step(1);
  }
  uint32_t I(uint32_t op, uint32_t rc, uint32_t ra, uint32_t rb) {
    return (op << 24 | rc << 16 | ra << 8 | rb);
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  Risc_t Risc(args);
  return Risc.run();
}
