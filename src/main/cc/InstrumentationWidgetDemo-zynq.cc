//See LICENSE for license details.

#include "simif_zynq.h"
#include "InstrumentationWidgetDemo.h"

class InstrumentationWidgetDemo_zynq_t:
  public simif_zynq_t,
  public InstrumentationWidgetDemo_t { };

int main(int argc, char** argv)
{
  InstrumentationWidgetDemo_zynq_t InstrumentationWidgetDemo;
  InstrumentationWidgetDemo.init(argc, argv, true);
  InstrumentationWidgetDemo.run();
  return InstrumentationWidgetDemo.finish();
}
