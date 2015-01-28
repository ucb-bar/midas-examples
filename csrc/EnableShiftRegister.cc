#include <fesvr/simif_zedboard.h>

class EnableShiftRegister_t: simif_zedboard_t
{
public:
  EnableShiftRegister_t(std::vector<std::string> args, bool _log):
    simif_zedboard_t(args, _log) {
      prefix = "EnableShiftRegister";
    }
 
  virtual int run() {
    std::vector<uint32_t> reg(4, 0);
    for (int i = 0 ; i < 16 ; i++) {
      uint32_t in    = rand_next(2);
      uint32_t shift = rand_next(2);
      simif_t::poke("EnableShiftRegister.io_in",    in);
      simif_t::poke("EnableShiftRegister.io_shift", shift);
      step(1);
      if (shift) {
        for (int j = 3 ; j > 0 ; j--) {
          reg[j] = reg[j-1];
        }
        reg[0] = in;
      }
      expect("EnableShiftRegister.io_out", reg[3]);
    }
    return 0; 
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  EnableShiftRegister_t EnableShiftRegister(args, true);
  return EnableShiftRegister.run();
}
