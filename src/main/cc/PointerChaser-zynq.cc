//See LICENSE for license details.

#include "simif_zynq.h"
#include "PointerChaser.h"

class PointerChaser_zynq_t:
  public simif_zynq_t,
  public PointerChaser_t
{
public:
  PointerChaser_zynq_t(int argc, char** argv):
    PointerChaser_t(argc, argv) { }
};

int main(int argc, char** argv)
{
  PointerChaser_zynq_t PointerChaser(argc, argv);
  PointerChaser.init(argc, argv, true);
  PointerChaser.run();
  return PointerChaser.finish();
}
