#include "simif_zynq.h"

class PointerChaser_t: simif_zynq_t
{
public:
  PointerChaser_t(std::vector<std::string> args):
      simif_zynq_t(args, "PointerChaser", false) {
    max_cycles = -1;
    latency = 16;
    address = 64;
    result = 1176;
    for (auto &arg: args) {
      if (arg.find("+max-cycles=") == 0) {
        max_cycles = atoi(arg.c_str()+12);
      }
      if (arg.find("+latency=") == 0) {
        latency = atoi(arg.c_str()+9);
      }
      if (arg.find("+address=") == 0) {
        address = atoi(arg.c_str()+9);
      }
      if (arg.find("+result=") == 0) {
        result = atoi(arg.c_str()+9);
      }
    }
  }

  int run() {
#if MEMMODEL
    poke_channel(MEMMODEL_0_readMaxReqs, 8);
    poke_channel(MEMMODEL_0_writeMaxReqs, 8);
    poke_channel(MEMMODEL_0_readLatency, latency);
    poke_channel(MEMMODEL_0_writeLatency, latency);
#else
    poke_channel(MEMMODEL_0_LATENCY, latency);
#endif
    poke("PointerChaser.io_startAddr_bits", address);
    poke("PointerChaser.io_startAddr_valid", 1);
    do {
      step(1);
    } while (!peek("PointerChaser.io_startAddr_ready"));
    poke("PointerChaser.io_startAddr_valid", 0);
    poke("PointerChaser.io_result_ready", 1);
    do {
      step(1);
    } while (!peek("PointerChaser.io_result_valid"));
    expect("PointerChaser.io_result_bits", result);
    fprintf(stdout, "Runs %llu cycles\n", cycles());
    return 0;
  }
private:
  uint64_t max_cycles;
  uint64_t address;
  uint64_t result;
  size_t latency;
};

int main(int argc, char** argv)
{
  std::vector<std::string> args(argv + 1, argv + argc);
  PointerChaser_t PointerChaser(args);
  return PointerChaser.run();
}
