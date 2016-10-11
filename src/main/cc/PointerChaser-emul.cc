#include "simif_emul.h"
#include "PointerChaser.h"

class PointerChaser_emul_t:
  public simif_emul_t,
  public PointerChaser_t
{
public:
  PointerChaser_emul_t(int argc, char** argv):
    PointerChaser_t(argc, argv) { }
};

int main(int argc, char** argv)
{
  PointerChaser_emul_t PointerChaser(argc, argv);
  PointerChaser.init(argc, argv, true);
  PointerChaser.run();
  return PointerChaser.finish();
}
