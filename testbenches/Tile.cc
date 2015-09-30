#include "simif_zynq.h"
#include <sys/time.h>

static inline uint64_t timestamp() {
  struct timeval tv;
  gettimeofday(&tv,NULL);
  return 1000000L * tv.tv_sec + tv.tv_usec;
}

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
    uint64_t start_time = timestamp(); 
    do {
      step(1);
      tohost = peek_port("Tile.io_htif_host_tohost").uint();
    } while (tohost == 0 && cycles() <= max_cycles);
    uint64_t end_time = timestamp(); 
    int exitcode = tohost >> 1;
    if (exitcode) {
      fprintf(stdout, "*** FAILED *** (code = %d) after %llu cycles\n", exitcode, cycles());
    } else if (cycles() > max_cycles) {
      fprintf(stdout, "*** FAILED *** (timeout) after %llu cycles\n", cycles());
    } else {
      fprintf(stdout, "*** PASSED *** after %llu cycles\n", cycles());
    }
    double sim_time = (double) (end_time - start_time) / 1000000.0;
    double sim_speed = (double) cycles() / sim_time / 1000.0;
    fprintf(stdout, "time elapsed: %.1f s, simulation speed = %.2f KHz\n", sim_time, sim_speed);
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
