basedir := $(abspath .)
srcdir  := $(basedir)/src/main/scala/
tutdir  := $(basedir)/tutorial/examples
minidir := $(basedir)/riscv-mini/src/main/scala/designs
gendir  := $(basedir)/generated
logdir  := $(basedir)/logs
resdir  := $(basedir)/results
strober := $(wildcard $(basedir)/strober/src/main/scala/*.scala)
srcs    := $(wildcard $(srcdir)/*.scala)
VPATH   := $(srcdir):$(tutdir):$(minidir):$(gendir):$(logdir)

# Designs
tut  := GCD Parity Stack Router Risc RiscSRAM \
        ShiftRegister ResetShiftRegister EnableShiftRegister MemorySearch
mini := Core Tile

# Chisel Flags
C_FLAGS := --targetDir $(gendir) --genHarness --compile --test --vcd --vcdMem --debug 
V_FLAGS := $(C_FLAGS) --v
FPGA_FLAGS := --targetDir $(gendir) --backend fpga --configDump
VCS_FLAGS := --targetDir $(gendir) --backend null --test

include Makefrag-fpga
include Makefrag-tut
include Makefrag-mini
include Makefrag-replay

$(tut) $(mini): %: %-fpga %-zedboard

tut:
	$(basedir)/scripts/run-tutorial.py

clean:
	rm -rf $(gendir) $(logdir) $(resdir) 
	$(MAKE) -C $(vcs_sim_rtl_dir) clean
	$(MAKE) -C $(vcs_sim_gl_syn_dir) clean
	$(MAKE) -C $(vcs_sim_gl_par_dir) clean
	$(MAKE) -C $(vcs_sim_gl_par_dir) clean
	$(MAKE) -C pt-pwr clean

cleanall: clean
	rm -rf project/target target
	$(MAKE) -C vlsi/dc-syn clean
	$(MAKE) -C vlsi/icc-par clean
	$(MAKE) -C chisel clean	

.PHONY: $(tut) $(mini) clean cleanall
