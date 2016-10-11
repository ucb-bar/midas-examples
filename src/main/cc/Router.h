#include <sstream>
#include "simif.h"

class Router_t: public virtual simif_t
{
public:
  Router_t(int n_): n(n_), out_chunks(
    OUTPUT_CHUNKS[io_outs_0_bits_body] +
    OUTPUT_CHUNKS[io_outs_0_bits_header] +
    OUTPUT_CHUNKS[io_outs_0_valid]
  ) {}
  void run() {
    wr(0, 1);
    rd(0, 1);
    rt(0, 1);
  }
private:
  const size_t n;
  const size_t out_chunks;
  void rd(biguint_t addr, biguint_t data) {
    biguint_t zero = 0;
    biguint_t one = 1;
    poke(io_in_valid,        zero);
    poke(io_writes_valid,    zero);
    poke(io_reads_valid,     one);
    poke(io_replies_ready,   one);
    poke(io_reads_bits_addr, addr);
    while (!peek(io_replies_valid)) step(1);
    expect(io_replies_bits,  data);
  }
  void wr(biguint_t addr, biguint_t data) {
    biguint_t zero = 0;
    biguint_t one = 1;
    poke(io_in_valid,         zero);
    poke(io_reads_valid,      zero);
    poke(io_writes_valid,     one);
    poke(io_writes_bits_addr, addr);
    poke(io_writes_bits_data, data);
    step(1);
  }
  bool isAnyValidOuts() {
    for (size_t i = 0 ; i < n ; i++) {
      if (peek(io_outs_0_valid + i * out_chunks)) return true;  
    }
    return false;
  }
  void rt(biguint_t header, biguint_t body) {
    for (size_t i = 0 ; i < n ; i++) {
      poke(io_outs_0_ready + i, 1);
    }
    biguint_t zero = 0;
    biguint_t one = 1;
    poke(io_reads_valid,    zero);
    poke(io_writes_valid,   zero);
    poke(io_in_valid,       one);
    poke(io_in_bits_header, header);
    poke(io_in_bits_body,   body);
    size_t i = 0;
    do {
      step(1); i += 1;
    } while (!isAnyValidOuts() && i < 10);
    expect(i < 10, "FIND VALID OUT");
  }
};
