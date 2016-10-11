#include "simif_zynq.h"
#include "Router.h"

class Router_zynq_t:
  public simif_zynq_t,
  public Router_t
{
public:
  Router_zynq_t(int n): Router_t(n) { }
};

int main(int argc, char** argv) 
{
  Router_zynq_t Router(4);
  Router.init(argc, argv, true);
  Router.run();
  return Router.finish();
}
