basedir := $(abspath .)
srcdir  := $(basedir)/src/main/scala/
tutdir  := $(basedir)/tutorial/examples
minidir := $(basedir)/riscv-mini/src/main/scala/designs
gendir  := $(basedir)/generated
logdir  := $(basedir)/logs
resdir  := $(basedir)/results
strober_dir := $(basedir)/strober/src/main/scala
VPATH   := $(srcdir):$(tutdir):$(minidir):$(gendir):$(logdir)

# Designs
tut  := GCD Parity Stack Router Risc RiscSRAM \
        ShiftRegister ResetShiftRegister EnableShiftRegister MemorySearch
mini := Core Tile TileD

# Chisel Flags
C_FLAGS := --targetDir $(gendir) --genHarness --compile --test --vcd --debug --configDump
V_FLAGS := $(C_FLAGS) --v
FPGA_FLAGS := --targetDir $(gendir) --backend fpga --configDump

include Makefrag-fpga
include Makefrag-tut
include Makefrag-mini

$(tut) $(mini): %: %-fpga %-zedboard

clean:
	rm -rf $(gendir) $(logdir) $(resdir) 

cleanall:
	rm -rf project/target target
	$(MAKE) -C chisel clean	

.PHONY: $(tut) $(mini) clean cleanall
