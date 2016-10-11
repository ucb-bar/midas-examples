#include "simif_emul.h"
#include "Tile.h"

class Tile_emul_t:
  public simif_emul_t,
  public Tile_t
{
public:
  Tile_emul_t(int argc, char** argv):
    Tile_t(argc, argv) { }
};

int main(int argc, char** argv) {
  Tile_emul_t Tile(argc, argv);
  Tile.init(argc, argv, false);
  Tile.run(128);
  return Tile.finish();
}
