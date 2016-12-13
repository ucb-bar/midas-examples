#include "simif.h"
#include "sim_mem.h"

class Tile_t: virtual simif_t
{
public:
  Tile_t(int argc, char** argv): mem(this, argc, argv) {
    max_cycles = 10000000L;
    std::vector<std::string> args(argv + 1, argv + argc);
    for (auto &arg: args) {
      if (arg.find("+max-cycles=") == 0) {
        max_cycles = atoi(arg.c_str()+12);
      }
    }
  }

  void run(size_t trace_len = TRACE_MAX_LEN) {
    set_tracelen(trace_len);
    mem.init();
    uint32_t tohost = 0;
    uint64_t start_time = timestamp(); 
    target_reset();
    do {
      step(trace_len, false);
      while(!done() || mem.target_fire()) mem.tick();
      tohost = peek(io_host_tohost);
    } while(tohost == 0 && cycles() <= max_cycles);
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
  sim_mem_t mem;
  uint64_t max_cycles;
};
