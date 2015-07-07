#include "simif_zynq.h"

class Tile_t: simif_zynq_t
{
public:
  Tile_t(std::vector<std::string> args): simif_zynq_t(args, "Tile", false) { 
    max_cycles = -1;
    for (auto &arg: args) {
      if (arg.find("+max-cycles=") == 0) {
        max_cycles = atoi(arg.c_str()+12);
      }
    }
  }

  int run() {
    uint64_t tohost = 0;
    do {
      step(1);
      tohost = peek_port("Tile.io_htif_host_tohost").uint();
    } while (tohost == 0 && cycles() <= max_cycles);
    int exitcode = tohost >> 1;
    if (exitcode) {
      fprintf(stdout, "*** FAILED *** (code = %d) after %llu cycles\n", exitcode, cycles());
    } else if (cycles() > max_cycles) {
      fprintf(stdout, "*** FAILED *** (timeout) after %llu cycles\n", cycles());
    } else {
      fprintf(stdout, "*** PASSED *** after %llu cycles\n", cycles());
    }
    return exitcode;
  }

private:
  uint64_t max_cycles;
};

int main(int argc, char** argv) {
  std::vector<std::string> args(argv + 1, argv + argc);
  Tile_t Tile(args);
  return Tile.run();
}
