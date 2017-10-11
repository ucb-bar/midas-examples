//See LICENSE for license details.

#include "simif_zynq.h"
#include "Tile.h"

class Tile_zynq_t:
  public simif_zynq_t,
  public Tile_t
{
public:
  Tile_zynq_t(int argc, char** argv):
    Tile_t(argc, argv) { }
};

int main(int argc, char** argv) {
  Tile_zynq_t Tile(argc, argv);
  Tile.init(argc, argv, false);
  Tile.run(128);
  return Tile.finish();
}
