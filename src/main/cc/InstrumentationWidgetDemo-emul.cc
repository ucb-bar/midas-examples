//See LICENSE for license details.

#include "simif_emul.h"
#include "InstrumentationWidgetDemo.h"

class InstrumentationWidgetDemo_emul_t:
  public simif_emul_t,
  public InstrumentationWidgetDemo_t { };

int main(int argc, char** argv)
{
  InstrumentationWidgetDemo_emul_t InstrumentationWidgetDemo;
  InstrumentationWidgetDemo.init(argc, argv, true);
  InstrumentationWidgetDemo.run();
  return InstrumentationWidgetDemo.finish();
}
