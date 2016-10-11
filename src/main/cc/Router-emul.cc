#include "simif_emul.h"
#include "Router.h"

class Router_emul_t:
  public simif_emul_t,
  public Router_t
{
public:
  Router_emul_t(int n): Router_t(n) { }
};

int main(int argc, char** argv) 
{
  Router_emul_t Router(4);
  Router.init(argc, argv, true);
  Router.run();
  return Router.finish();
}
