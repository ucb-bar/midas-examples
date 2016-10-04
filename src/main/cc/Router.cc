#include <sstream>
#include "simif_zynq.h"

class Router_t: simif_zynq_t
{
public:
  Router_t(std::vector<std::string> args, int n_): 
    simif_zynq_t(args, "Router", true), n(n_) {}
  int run() {
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
    poke("Router.io_in_valid",        zero);
    poke("Router.io_writes_valid",    zero);
    poke("Router.io_reads_valid",     one);
    poke("Router.io_replies_ready",   one);
    poke("Router.io_reads_bits_addr", addr);
    while (!peek("Router.io_replies_valid")) step(1);
    expect("Router.io_replies_bits",  data);
  }
  void wr(biguint_t addr, biguint_t data) {
    biguint_t zero = 0;
    biguint_t one = 1;
    poke("Router.io_in_valid",         zero);
    poke("Router.io_reads_valid",      zero);
    poke("Router.io_writes_valid",     one);
    poke("Router.io_writes_bits_addr", addr);
    poke("Router.io_writes_bits_data", data);
    step(1);
  }
  bool isAnyValidOuts() {
    for (int i = 0 ; i < n ; i++) {
      std::string path = "Router.io_outs_" + std::to_string(i) + "_valid";
      if (peek(path)) return true;  
    }
    return false;
  }
  void rt(biguint_t header, biguint_t body) {
    for (int i = 0 ; i < n ; i++) {
      std::string path =  "Router.io_outs_" + std::to_string(i) + "_ready";
      poke(path, 1);
    }
    biguint_t zero = 0;
    biguint_t one = 1;
    poke("Router.io_reads_valid",    zero);
    poke("Router.io_writes_valid",   zero);
    poke("Router.io_in_valid",       one);
    poke("Router.io_in_bits_header", header);
    poke("Router.io_in_bits_body",   body);
    size_t i = 0;
    do {
      step(1); i += 1;
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
