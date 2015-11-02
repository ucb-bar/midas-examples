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

  int run(size_t trace_len = TRACE_MAX_LEN) {
    set_trace_len(trace_len);
    set_mem_cycles(100);
    size_t tohost_id = get_out_id("Tile.io_htif_host_tohost");
    uint32_t tohost = 0;
    uint64_t start_time = timestamp(); 
    do {
      step(trace_len);
      tohost = peek_port(tohost_id);
    } while (tohost == 0 && cycles() <= max_cycles);
    uint64_t end_time = timestamp(); 
    double sim_time = (double) (end_time - start_time) / 1000000.0;
    double sim_speed = (double) cycles() / sim_time / 1000000.0;
    fprintf(stdout, "time elapsed: %.1f s, simulation speed = %.2f MHz\n", sim_time, sim_speed);
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
  return Tile.run(128);
}
