//See LICENSE for license details.

#include "simif.h"
#include "endpoints/sim_mem.h"
#include "endpoints/fpga_memory_model.h"

class Tile_t: virtual simif_t
{
public:
  Tile_t(int argc, char** argv) {
    max_cycles = 10000000L;
    std::vector<std::string> args(argv + 1, argv + argc);
    for (auto &arg: args) {
      if (arg.find("+max-cycles=") == 0) {
        max_cycles = atoi(arg.c_str()+12);
      }
    }
#ifdef NASTIWIDGET_0
    endpoints.push_back(new sim_mem_t(this, argc, argv));
#endif

#ifdef MEMMODEL_0
    fpga_models.push_back(new FpgaMemoryModel(
        this,
        // Casts are required for now since the emitted type can change...
        AddressMap(MEMMODEL_0_R_num_registers,
                   (const unsigned int*) MEMMODEL_0_R_addrs,
                   (const char* const*) MEMMODEL_0_R_names,
                   MEMMODEL_0_W_num_registers,
                   (const unsigned int*) MEMMODEL_0_W_addrs,
                   (const char* const*) MEMMODEL_0_W_names),
        argc, argv, "memory_stats.csv"));
#endif
  }

  void run(size_t step_size) {
    for (auto e: fpga_models) {
      e->init();
    }

    uint32_t tohost = 0;
    uint64_t start_time = timestamp(); 
    target_reset();
    do {
      step(step_size, false);
      bool _done;
      do {
        _done = done();
        for (auto e: endpoints) {
          _done &= e->done();
          e->tick();
        }
      } while(!_done);
      tohost = peek(io_host_tohost);
    } while(tohost == 0 && cycles() <= max_cycles);
    uint64_t end_time = timestamp(); 
    double sim_time = diff_secs(end_time, start_time);
    double sim_speed = ((double)cycles()) / sim_time / 1000.0;
    if (sim_speed > 1000.0) {
      fprintf(stderr, "time elapsed: %.1f s, simulation speed = %.2f MHz\n", sim_time, sim_speed / 1000.0);
    } else {
      fprintf(stderr, "time elapsed: %.1f s, simulation speed = %.2f KHz\n", sim_time, sim_speed);
    }
    int code = tohost >> 1;
    if (code) {
      fprintf(stderr, "*** FAILED *** (code = %d) after %llu cycles\n", code, (long long)cycles());
    } else if (cycles() > max_cycles) {
      fprintf(stderr, "*** FAILED *** (timeout) after %llu cycles\n", (long long)cycles());
    } else {
      fprintf(stderr, "*** PASSED *** after %llu cycles\n", (long long)cycles());
    }
    expect(!code, NULL);
  }

private:
  std::vector<endpoint_t*> endpoints;
  std::vector<FpgaModel*> fpga_models;
  uint64_t max_cycles;
};
