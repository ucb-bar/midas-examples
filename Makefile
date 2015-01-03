basedir := $(abspath .)
srcdir  := $(basedir)/src/main/scala/
tutdir  := $(basedir)/tutorial/examples
minidir := $(basedir)/riscv-mini/src/main/scala/designs
csrcdir := $(basedir)/csrc
gendir  := $(basedir)/generated
logdir  := $(basedir)/logs
resdir  := $(basedir)/results
zeddir  := $(basedir)/fpga-zynq/zedboard
bitstream := fpga-images-zedboard/boot.bin
designs := GCD Parity Stack Router Risc RiscSRAM FIR2D \
	ShiftRegister ResetShiftRegister EnableShiftRegister MemorySearch
VPATH   := $(srcdir):$(tutdir):$(minidir):$(gendir):$(logdir)
memgen  := $(basedir)/scripts/fpga_mem_gen
C_FLAGS := --targetDir $(gendir) --genHarness --compile --test --vcd --debug --configDump
V_FLAGS := $(C_FLAGS) --v
FPGA_FLAGS := --targetDir $(gendir) --backend fpga --configDump
CXX := arm-xilinx-linux-gnueabi-g++
CXXFLAGS := -static -O2 -std=c++11

default: GCD

cpp     := $(addsuffix Shim.cpp, $(designs))
harness := $(addsuffix Shim-harness.v, $(designs))
v       := $(addsuffix Shim.v, $(designs) Core Tile)
fpga    := $(addsuffix -fpga, $(designs) Core Tile)
driver  := $(addsuffix -zedboard, $(designs) Core Tile TileD)

replay_cpp := $(addsuffix .cpp, $(designs))
replay_v   := $(addsuffix .v,   $(designs))

cpp: $(cpp)
harness: $(harness)
replay-cpp: $(replay_cpp)
replay-v: $(replay_v)

$(designs) Core Tile TileD: %: %-fpga %-zedboard

$(cpp): %Shim.cpp: %.scala 
	mkdir -p $(logdir)
	sbt "run $(basename $@) $(C_FLAGS)" | tee $(logdir)/$@.out

$(harness): %Shim-harness.v: %.scala 
	mkdir -p $(logdir) $(resdir)
	sbt "run $*Shim $(V_FLAGS)" | tee $(logdir)/$*Shim.v.out

$(resdir)/%.snap:
	make -j $*Shim-harness.v
	cp $(gendir)/$(notdir $@) $@

$(replay_cpp): %.cpp: $(resdir)/%.snap %.scala
	mkdir -p $(logdir)
	cp $< $(gendir)/
	sbt "run $(basename $@) $(C_FLAGS)" | tee $(logdir)/$@.out

$(replay_v): %.v: $(resdir)/%.snap %.scala
	mkdir -p $(logdir)
	cp $< $(gendir)/
	sbt "run $(basename $@) $(V_FLAGS)" | tee $(logdir)/$@.out

$(v): %Shim.v: %.scala
	rm -rf $(gendir)/$@
	mkdir -p $(logdir) $(resdir)
	sbt "run $(basename $@) $(FPGA_FLAGS)"
	if [ -a $(gendir)/$(basename $@).conf ]; then \
          $(memgen) $(gendir)/$(basename $@).conf >> $(gendir)/$(basename $@).v; \
        fi
	cd $(gendir) ; cp $*Shim.prm $*.io.map $*.chain.map $(resdir)

$(fpga): %-fpga: %Shim.v
	cd $(zeddir); make clean; make $(bitstream) DESIGN=$*; cp $(bitstream) $(resdir)

$(driver): %-zedboard: $(csrcdir)/%.cc $(csrcdir)/debug_api.cc $(csrcdir)/debug_api.h
	mkdir -p $(resdir)
	cd $(resdir); $(CXX) $(CXXFLAGS) $^ -o $@

tests_isa_dir  := $(basedir)/riscv-mini/tests/isa
timeout_cycles := 10000
include riscv-mini/Makefrag-sim

core_asm_c = $(addprefix Core., $(addsuffix .cpp.out, $(asm_p_tests)))
$(core_asm_c): Core.%.cpp.out: $(tests_isa_dir)/%.hex $(minidir)/Core.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run CoreShim $(C_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
core_asm_c: $(core_asm_c)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(core_asm_c)); echo;

core_asm_v = $(addprefix Core., $(addsuffix .v.out, $(asm_p_tests)))
$(core_asm_v): Core.%.v.out: $(tests_isa_dir)/%.hex $(minidir)/Core.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run CoreShim $(V_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
core_asm_v: $(core_asm_v)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(core_asm_v)); echo;

tile_asm_c = $(addprefix Tile., $(addsuffix .cpp.out, $(asm_p_tests)))
$(tile_asm_c): Tile.%.cpp.out: $(tests_isa_dir)/%.hex $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run TileShim $(C_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tile_asm_c: $(tile_asm_c)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(tile_asm_c)); echo;

tile_asm_v = $(addprefix Tile., $(addsuffix .v.out, $(asm_p_tests)))
$(tile_asm_v): Tile.%.v.out: $(tests_isa_dir)/%.hex $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run TileShim $(V_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tile_asm_v: $(tile_asm_v)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(tile_asm_v)); echo;

tile_replay_cpp = $(addprefix Tile., $(addsuffix .cpp.replay, $(asm_p_tests)))
$(tile_replay_cpp): Tile.%.cpp.replay: Tile.%.cpp.out $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run Tile $(C_FLAGS) +max-cycles=$(timeout_cycles) +verbose" \
        | tee $(logdir)/$(notdir $@)
tile_replay_cpp: $(tile_replay_cpp)

tile_replay_v = $(addprefix Tile., $(addsuffix .v.replay, $(asm_p_tests)))
$(tile_replay_v): Tile.%.v.replay: Tile.%.v.out $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run Tile $(V_FLAGS) +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tile_replay_v: $(tile_replay_v)

tile_suffix = $(shell date +%Y-%m-%d_%H-%M-%S)
Tile.cpp:
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run Tile $(C_FLAGS) +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)-$(tile_suffix).out

Tile.v:
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run Tile $(V_FLAGS) +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)-$(tile_suffix).out

tiled_asm_c = $(addprefix TileD., $(addsuffix .cpp.out, $(asm_p_tests)))
$(tiled_asm_c): TileD.%.cpp.out: $(tests_isa_dir)/%.hex $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run TileDShim $(C_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles) +verbose" \
        | tee $(logdir)/$(notdir $@)
tiled_asm_c: $(tiled_asm_c)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(tiled_asm_c)); echo;

tiled_asm_v = $(addprefix TileD., $(addsuffix .v.out, $(asm_p_tests)))
$(tiled_asm_v): TileD.%.v.out: $(tests_isa_dir)/%.hex $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run TileDShim $(V_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tiled_asm_v: $(tiled_asm_v)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(tiled_asmd_v)); echo;

TileDShim-v:
	mkdir -p $(logdir) $(resdir)
	sbt "run $(basename $@) $(FPGA_FLAGS)"
	if [ -a $(gendir)/$(basename $@).conf ]; then \
          $(memgen) $(gendir)/$(basename $@).conf >> $(gendir)/$(basename $@).v; \
        fi
	cd $(gendir) ; cp TileShim.prm Tile.io.map Tile.chain.map $(resdir)

TileD-fpga: TileDShim-v
	cd $(zeddir); make clean; make $(bitstream) DESIGN=Tile; cp $(bitstream) $(resdir)

clean:
	rm -rf $(gendir) $(logdir) $(resdir) 

cleanall:
	rm -rf project/target target
	$(MAKE) -C chisel clean	

.PHONY: all cpp v $(v) $(fpga) clean cleanall
