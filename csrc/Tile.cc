#include <fstream>
#include "simif_zedboard.h"

class Tile_t: simif_zedboard_t
{
public:
  Tile_t(std::vector<std::string> args):
    simif_zedboard_t(args, "Tile", false, false) 
  {
    step_size = 50; 
  }
  int run() {
    load_mem();
    uint64_t tohost = 0;
    do {
      step(step_size);
      tohost = peek("Tile.io_htif_host_tohost").uint();
    } while (tohost == 0 && !timeout());
    int exitcode = tohost >> 1;
    if (exitcode) {
      fprintf(stderr, "*** FAILED *** (code = %d) after %llu cycles\n", exitcode, cycles());
    } else if (timeout()) {
      fprintf(stderr, "*** FAILED *** (timeout) after %llu cycles\n", cycles());
    } else {
      fprintf(stderr, "*** PASSED *** after %llu cycles\n", cycles());
    }
    return exitcode;
  }
};

int main(int argc, char** argv) {
  std::vector<std::string> args(argv + 1, argv + argc);
  Tile_t Tile(args);
  return Tile.run();
}
