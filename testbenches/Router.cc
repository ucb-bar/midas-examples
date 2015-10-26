#include <sstream>
#include "simif_zynq.h"

class Router_t: simif_zynq_t
{
public:
  Router_t(std::vector<std::string> args, int n_): 
    simif_zynq_t(args, "Router", true), n(n_) {}
  int run() {
    rd(0, 0);
    wr(0, 1);
    rd(0, 1);
    rt(0, 1);
    return 0;
  }
private:
  const int n;
  void rd(biguint_t addr, biguint_t data) {
    biguint_t zero = 0;
    biguint_t one = 1;
    poke_port("Router.io_in_valid",        zero);
    poke_port("Router.io_writes_valid",    zero);
    poke_port("Router.io_reads_valid",     one);
    poke_port("Router.io_replies_ready",   one);
    poke_port("Router.io_reads_bits_addr", addr);
    step(1);
    expect_port("Router.io_replies_bits",  data);
  }
  void wr(biguint_t addr, biguint_t data) {
    biguint_t zero = 0;
    biguint_t one = 1;
    poke_port("Router.io_in_valid",         zero);
    poke_port("Router.io_reads_valid",      zero);
    poke_port("Router.io_writes_valid",     one);
    poke_port("Router.io_writes_bits_addr", addr);
    poke_port("Router.io_writes_bits_data", data);
    step(1);
  }
  bool isAnyValidOuts() {
    for (int i = 0 ; i < n ; i++) {
      std::ostringstream path;
      path <<  "Router.io_outs_" << i << "_valid";
      if (peek_port(path.str()) == 1) return true;  
    }
    return false;
  }
  void rt(biguint_t header, biguint_t body) {
    for (int i = 0 ; i < n ; i++) {
      std::ostringstream path;
      path <<  "Router.io_outs_" << i << "_ready";
      poke_port(path.str(), 1);
    }
    biguint_t zero = 0;
    biguint_t one = 1;
    poke_port("Router.io_reads_valid",    zero);
    poke_port("Router.io_writes_valid",   zero);
    poke_port("Router.io_in_valid",       one);
    poke_port("Router.io_in_bits_header", header);
    poke_port("Router.io_in_bits_body",   body);
    int i = 0;
    do {
      step(1);
      i += 1;
    } while (!isAnyValidOuts() && i < 10);
    expect(i < 10, "FIND VALID OUT");
  }
};

int main(int argc, char** argv) 
{
  std::vector<std::string> args(argv + 1, argv + argc);
  Router_t Router(args, 4);
  return Router.run();
}
