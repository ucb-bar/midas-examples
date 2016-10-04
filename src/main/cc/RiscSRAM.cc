#include "simif_zynq.h"

class RiscSRAM_t: simif_zynq_t
{
public:
  RiscSRAM_t(std::vector<std::string> args): 
    simif_zynq_t(args, "RiscSRAM", true) {}
  int run() {
    std::vector<uint32_t> app;
    app.push_back(I(1, 1, 0, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 2));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 3));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 4));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 5));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 6));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 7));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 8));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 9));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 1, 0, 10));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(0, 1, 1, 1));
    app.push_back(I(1, 2, 0, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 2));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 3));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 4));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 5));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 6));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 7));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 8));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 9));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(1, 2, 0, 10));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 2, 2, 1));
    app.push_back(I(0, 255, 1, 0));
    wr(0, 0);
    for (size_t addr = 0 ; addr < app.size() ; addr++) {
      wr(addr, app[addr]);
    }
    boot();
    int k = 0;
    do {
      tick(); k += 1;
    } while (peek("RiscSRAM.io_valid") == 0 && k < 400);
    expect(k < 400, "TIME LIMIT");
    expect("RiscSRAM.io_out", 40);
    return 0;
  }
private:
  void wr(uint32_t addr, uint32_t data) {
    poke("RiscSRAM.io_isWr", 1);
    poke("RiscSRAM.io_wrAddr", addr);
    poke("RiscSRAM.io_wrData", data);
    step(1);
  }
  void boot() {
    poke("RiscSRAM.io_isWr", 0);
    poke("RiscSRAM.io_boot", 1);
    step(1);
  }
  void tick() {
    poke("RiscSRAM.io_isWr", 0);
    poke("RiscSRAM.io_boot", 0);
    step(1);
  }
  uint32_t I(uint32_t op, uint32_t rc, uint32_t ra, uint32_t rb) {
    return op << 24 | rc << 16 | ra << 8 | rb;
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  RiscSRAM_t RiscSRAM(args);
  return RiscSRAM.run();
}
