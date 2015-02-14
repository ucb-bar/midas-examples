basedir := $(abspath .)
srcdir  := $(basedir)/src/main/scala/
tutdir  := $(basedir)/tutorial/examples
minidir := $(basedir)/riscv-mini/src/main/scala/designs
csrcdir := $(basedir)/csrc
gendir  := $(basedir)/generated
logdir  := $(basedir)/logs
resdir  := $(basedir)/results
zeddir  := $(basedir)/fpga-zynq/zedboard
fesvrdir := $(basedir)/fesvr
bitstream := fpga-images-zedboard/boot.bin
designs := GCD Parity Stack Router Risc RiscSRAM \
	ShiftRegister ResetShiftRegister EnableShiftRegister MemorySearch
VPATH   := $(srcdir):$(tutdir):$(minidir):$(gendir):$(logdir)
memgen  := $(basedir)/scripts/fpga_mem_gen
C_FLAGS := --targetDir $(gendir) --genHarness --compile --test --vcd --debug --configDump
V_FLAGS := $(C_FLAGS) --v
FPGA_FLAGS := --targetDir $(gendir) --backend fpga --configDump
CXX := arm-xilinx-linux-gnueabi-g++
CXXFLAGS := $(CXXFLAGS) -O2 -std=c++11 -I$(fesvrdir)/fesvr 
LDFLAGS  := $(LDFLAGS) -L$(fesvrdir) -Wl,-rpath,/usr/local/lib -lfesvr -lpthread

default: GCD

cpp     := $(addsuffix Strober.cpp, $(designs))
harness := $(addsuffix Strober-harness.v, $(designs))
v       := $(addsuffix Strober.v, $(designs) Core Tile)
fpga    := $(addsuffix -fpga, $(designs) Core Tile)
param   := $(addsuffix .h, $(designs) Core Tile TileD)
driver  := $(addsuffix -zedboard, $(designs) Core Tile TileD)
samples := $(addprefix $(resdir)/, $(addsuffix .sample, $(designs)))

replay_cpp := $(addsuffix .cpp, $(designs))
replay_v   := $(addsuffix .v,   $(designs))

cpp: $(cpp)
harness: $(harness)
replay-cpp: $(replay_cpp)
replay-v: $(replay_v)

$(designs) Core Tile TileD: %: %-fpga %-zedboard

$(cpp): %Strober.cpp: %.scala 
	mkdir -p $(logdir) $(resdir)
	sbt "run $(basename $@) $(C_FLAGS)" | tee $(logdir)/$@.out

$(harness): %Strober-harness.v: %.scala 
	mkdir -p $(logdir) $(resdir)
	sbt "run $*Strober $(V_FLAGS)" | tee $(logdir)/$*Strober.v.out

$(samples): $(resdir)/%.sample: %Strober.cpp
	cp $(gendir)/$(notdir $@) $@

$(replay_cpp): %.cpp: $(resdir)/%.sample %.scala
	mkdir -p $(logdir) $(gendir)
	cp $< $(gendir)/
	sbt "run $(basename $@) $(C_FLAGS)" | tee $(logdir)/$@.out

$(replay_v): %.v: $(resdir)/%.sample %.scala
	mkdir -p $(logdir) $(gendir)
	cp $< $(gendir)/
	sbt "run $(basename $@) $(V_FLAGS)" | tee $(logdir)/$@.out

$(v): %Strober.v: %.scala
	mkdir -p $(logdir) $(resdir)
	sbt "run $(basename $@) $(FPGA_FLAGS)"
	if [ -a $(gendir)/$(basename $@).conf ]; then \
          $(memgen) $(gendir)/$(basename $@).conf >> $(gendir)/$(basename $@).v; \
        fi
	cd $(gendir) ; cp $*.io.map $*.chain.map $(resdir)

$(fpga): %-fpga: %Strober.v
	cd $(zeddir); make clean; make $(bitstream) DESIGN=$*; cp $(bitstream) $(resdir)

$(gendir)/%-zedboard.h: %Strober.v
	echo "#ifndef __$*_H" > $@
	echo "#define __$*_H" >> $@
	sed -r 's/\(([A-Za-z0-9_]+),([A-Za-z0-9_]+)\)/#define \1 \2/' $(gendir)/$*Strober.prm >> $@
	echo "#endif // __$*_H" >> $@

$(driver): %-zedboard: $(csrcdir)/%.cc $(gendir)/%-zedboard.h
	mkdir -p $(resdir)
	cd $(fesvrdir) ; ./configure --host=arm-xilinx-linux-gnueabi ; make
	cp $(fesvrdir)/libfesvr.so $(resdir)/
	$(CXX) $(CXXFLAGS) $(LDFLAGS) -include$(word 2, $^) $< $(csrcdir)/api.cc -o $(resdir)/$(notdir $@)

tests_isa_dir  := $(basedir)/riscv-mini/tests/isa
timeout_cycles := 10000
include riscv-mini/Makefrag-sim

core_asm_c = $(addprefix Core., $(addsuffix .cpp.out, $(asm_p_tests)))
$(core_asm_c): Core.%.cpp.out: $(tests_isa_dir)/%.hex $(minidir)/Core.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run CoreStrober $(C_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles) +verbose" \
        | tee $(logdir)/$(notdir $@)
core_asm_c: $(core_asm_c)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(core_asm_c)); echo;

core_asm_v = $(addprefix Core., $(addsuffix .v.out, $(asm_p_tests)))
$(core_asm_v): Core.%.v.out: $(tests_isa_dir)/%.hex $(minidir)/Core.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run CoreStrober $(V_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
core_asm_v: $(core_asm_v)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(core_asm_v)); echo;

tile_asm_c = $(addprefix Tile., $(addsuffix .cpp.out, $(asm_p_tests)))
$(tile_asm_c): Tile.%.cpp.out: $(tests_isa_dir)/%.hex $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run TileStrober $(C_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tile_asm_c: $(tile_asm_c)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(tile_asm_c)); echo;

tile_asm_v = $(addprefix Tile., $(addsuffix .v.out, $(asm_p_tests)))
$(tile_asm_v): Tile.%.v.out: $(tests_isa_dir)/%.hex $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run TileStrober $(V_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tile_asm_v: $(tile_asm_v)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(tile_asm_v)); echo;

$(resdir)/Tile.%.sample: Tile.%.cpp.out
	mkdir -p $(resdir)
	cp $(gendir)/$(notdir $@) $@

tile_replay_cpp = $(addprefix Tile., $(addsuffix .cpp.replay, $(asm_p_tests)))
$(tile_replay_cpp): Tile.%.cpp.replay: $(resdir)/Tile.%.sample $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run Tile $(C_FLAGS) +loadmem=$(tests_isa_dir)/$*.hex +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tile_replay_cpp: $(tile_replay_cpp)

tile_replay_v = $(addprefix Tile., $(addsuffix .v.replay, $(asm_p_tests)))
$(tile_replay_v): Tile.%.v.replay: $(resdir)/Tile.%.sample $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run Tile $(V_FLAGS) +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tile_replay_v: $(tile_replay_v)

tiled_asm_c = $(addprefix TileD., $(addsuffix .cpp.out, $(asm_p_tests)))
$(tiled_asm_c): TileD.%.cpp.out: $(tests_isa_dir)/%.hex $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run TileDStrober $(C_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles) +verbose" \
        | tee $(logdir)/$(notdir $@)
tiled_asm_c: $(tiled_asm_c)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(tiled_asm_c)); echo;

tiled_asm_v = $(addprefix TileD., $(addsuffix .v.out, $(asm_p_tests)))
$(tiled_asm_v): TileD.%.v.out: $(tests_isa_dir)/%.hex $(minidir)/Tile.scala
	mkdir -p $(logdir)
	cd $(basedir) ; sbt "run TileDStrober $(V_FLAGS) +loadmem=$< +max-cycles=$(timeout_cycles)" \
        | tee $(logdir)/$(notdir $@)
tiled_asm_v: $(tiled_asm_v)
	@echo; perl -ne 'print " [$$1] $$ARGV \t$$2\n" if /\*{3}(.{8})\*{3}(.*)/' \
	$(addprefix $(logdir)/, $(tiled_asmd_v)); echo;

TileDStrober-v:
	mkdir -p $(logdir) $(resdir)
	sbt "run $(basename $@) $(FPGA_FLAGS)"
	if [ -a $(gendir)/$(basename $@).conf ]; then \
          $(memgen) $(gendir)/$(basename $@).conf >> $(gendir)/$(basename $@).v; \
        fi
	cd $(gendir) ; cp TileStrober.prm Tile.io.map Tile.chain.map $(resdir)

TileD-fpga: TileDStrober-v
	cd $(zeddir); make clean; make $(bitstream) DESIGN=Tile; cp $(bitstream) $(resdir)

clean:
	rm -rf $(gendir) $(logdir) $(resdir) 

cleanall:
	rm -rf project/target target
	$(MAKE) -C chisel clean	

.PHONY: all cpp v $(fpga) clean cleanall
