#include "simif_zynq.h"

class Tile_t: simif_zynq_t
{
public:
  Tile_t(int argc, char** argv): simif_zynq_t(argc, argv, false)
  {
    max_cycles = -1;
    latency = 16;
    std::vector<std::string> args(argv + 1, argv + argc);
    for (auto &arg: args) {
      if (arg.find("+max-cycles=") == 0) {
        max_cycles = atoi(arg.c_str()+12);
      }
      if (arg.find("+latency=") == 0) {
        latency = atoi(arg.c_str()+9);
      }
    }
  }

  int run(size_t trace_len = TRACE_MAX_LEN) {
    set_tracelen(trace_len);
#if MEMMODEL
    write(MEMMODEL_0_readMaxReqs, 8);
    write(MEMMODEL_0_writeMaxReqs, 8);
    write(MEMMODEL_0_readLatency, latency);
    write(MEMMODEL_0_writeLatency, latency);
#else
    write(MEMMODEL_0_LATENCY, latency);
#endif
    uint32_t tohost = 0;
    uint64_t start_time = timestamp(); 
    do {
      step(trace_len);
      tohost = peek(io_host_tohost);
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
  size_t latency;
};

int main(int argc, char** argv) {
  Tile_t Tile(argc, argv);
  return Tile.run(128);
}
