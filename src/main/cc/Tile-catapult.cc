#include "simif_catapult.h"
#include "Tile.h"

class Tile_catapult_t:
  public simif_catapult_t,
  public Tile_t
{
public:
  Tile_catapult_t(int argc, char** argv):
    Tile_t(argc, argv) { }
};

int main(int argc, char** argv) {
  Tile_catapult_t Tile(argc, argv);
  Tile.init(argc, argv, false);
  Tile.run(128);
  return Tile.finish();
}
