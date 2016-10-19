#include "simif.h"

class Tile_t: virtual simif_t
{
public:
  Tile_t(int argc, char** argv) {
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

  void run(size_t trace_len = TRACE_MAX_LEN) {
    set_tracelen(trace_len);
#ifdef MEMMODEL_0_readLatency
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
    double sim_speed = (double) cycles() / sim_time / 1000.0;
    if (sim_speed > 1000.0) {
      fprintf(stderr, "time elapsed: %.1f s, simulation speed = %.2f MHz\n", sim_time, sim_speed / 1000.0);
    } else {
      fprintf(stderr, "time elapsed: %.1f s, simulation speed = %.2f KHz\n", sim_time, sim_speed);
    }
    int code = tohost >> 1;
    if (code) {
      fprintf(stderr, "*** FAILED *** (code = %d) after %" PRIu64 " cycles\n", code, cycles());
    } else if (cycles() > max_cycles) {
      fprintf(stderr, "*** FAILED *** (timeout) after %" PRIu64 " cycles\n", cycles());
    } else {
      fprintf(stderr, "*** PASSED *** after %" PRIu64 " cycles\n", cycles());
    }
    expect(!code, NULL);
  }

private:
  uint64_t max_cycles;
  size_t latency;
};
