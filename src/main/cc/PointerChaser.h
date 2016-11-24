#include "simif.h"
#ifndef ENABLE_MEMMODEL
#include "mm.h"
#include "mm_dramsim2.h"
#endif

class PointerChaser_t: virtual simif_t
{
public:
  PointerChaser_t(int argc, char** argv) {
    max_cycles = -1;
    latency = 16;
    address = 64;
    result = 1176;
#ifndef ENABLE_MEMMODEL
    bool dramsim = false;
    uint64_t memsize = 1L << 32;
    const char* loadmem = NULL;
#endif
    std::vector<std::string> args(argv + 1, argv + argc);
    for (auto &arg: args) {
      if (arg.find("+max-cycles=") == 0) {
        max_cycles = atoi(arg.c_str() + 12);
      }
      if (arg.find("+latency=") == 0) {
        latency = atoi(arg.c_str() + 9);
      }
      if (arg.find("+address=") == 0) {
        address = atoi(arg.c_str() + 9);
      }
      if (arg.find("+result=") == 0) {
        result = atoi(arg.c_str() + 9);
      }
#ifndef ENABLE_MEMMODEL
      if (arg.find("+dramsim") == 0) {
        dramsim = true;
      }
      if (arg.find("+memsize=") == 0) {
        memsize = strtoll(arg.c_str() + 9, NULL, 10);
      }
      if (arg.find("+loadmem=") == 0) {
        loadmem = arg.c_str() + 9;
      }
#endif
    }
#ifndef ENABLE_MEMMODEL
    mem = dramsim ? (mm_t*) new mm_dramsim2_t : (mm_t*) new mm_magic_t;
    mem->init(memsize, MEM_DATA_BITS / 8, 64);
    if (loadmem) {
      fprintf(stderr, "[sw loadmem] %s\n", loadmem);
      void* mems[1];
      mems[0] = mem->get_data();
      ::load_mem(mems, loadmem, MEM_DATA_BITS / 8, 1);
    }
#endif
  }

  ~PointerChaser_t() {
#ifndef ENABLE_MEMMODEL
    delete mem;
#endif
  }

#ifndef ENABLE_MEMMODEL
  virtual void step(int n) {
    for (size_t i = 0 ; i < n ; i++) {
      poke(io_nasti_aw_ready,    mem->aw_ready());
      poke(io_nasti_ar_ready,    mem->aw_ready());
      poke(io_nasti_w_ready,     mem->w_ready());
      poke(io_nasti_b_valid,     mem->b_valid());
      poke(io_nasti_b_bits_id,   mem->b_id());
      poke(io_nasti_b_bits_resp, mem->b_resp());
      poke(io_nasti_r_valid,     mem->r_valid());
      poke(io_nasti_r_bits_id,   mem->r_id());
      poke(io_nasti_r_bits_resp, mem->r_resp());
      poke(io_nasti_r_bits_last, mem->r_last());

      biguint_t r_data(
        (const uint32_t*) mem->r_data(),
        MEM_DATA_BITS / (8 * sizeof(uint32_t)));
      poke(io_nasti_r_bits_data, r_data);

      biguint_t w_data;
      peek(io_nasti_w_bits_data, w_data);
      mem->tick(
        false,
        peek(io_nasti_ar_valid),
        peek(io_nasti_ar_bits_addr),
        peek(io_nasti_ar_bits_id),
        peek(io_nasti_ar_bits_size),
        peek(io_nasti_ar_bits_len),

        peek(io_nasti_aw_valid),
        peek(io_nasti_aw_bits_addr),
        peek(io_nasti_aw_bits_id),
        peek(io_nasti_aw_bits_size),
        peek(io_nasti_aw_bits_len),

        peek(io_nasti_w_valid),
        peek(io_nasti_w_bits_strb),
        w_data.get_data(),
        peek(io_nasti_w_bits_last),

        peek(io_nasti_r_ready),
        peek(io_nasti_w_ready)
      );

      simif_t::step(1);
    }
  }
#endif

  void run() {
#ifdef ENABLE_MEMMODEL
#ifdef MEMMODEL_0_readLatency
    write(MEMMODEL_0_readMaxReqs, 8);
    write(MEMMODEL_0_writeMaxReqs, 8);
    write(MEMMODEL_0_readLatency, latency);
    write(MEMMODEL_0_writeLatency, latency);
#else
    write(MEMMODEL_0_LATENCY, latency);
#endif
#endif
    target_reset(0);

    poke(io_startAddr_bits, address);
    poke(io_startAddr_valid, 1);
    do {
      step(1);
    } while (!peek(io_startAddr_ready));
    poke(io_startAddr_valid, 0);
    poke(io_result_ready, 1);
    do {
      step(1);
    } while (!peek(io_result_valid));
    expect(io_result_bits, result);
  }
private:
  uint64_t max_cycles;
  uint64_t address;
  biguint_t result; // 64 bit
  size_t latency;
#ifndef ENABLE_MEMMODEL
  mm_t* mem;
#endif
};
