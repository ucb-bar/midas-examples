#include "simif_catapult.h"
#include "PointerChaser.h"

class PointerChaser_catapult_t:
  public simif_catapult_t,
  public PointerChaser_t
{
public:
  PointerChaser_catapult_t(int argc, char** argv):
    PointerChaser_t(argc, argv) { }
};

int main(int argc, char** argv)
{
  PointerChaser_catapult_t PointerChaser(argc, argv);
  PointerChaser.init(argc, argv, true);
  PointerChaser.run();
  return PointerChaser.finish();
}
