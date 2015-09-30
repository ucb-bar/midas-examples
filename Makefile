base_dir := $(abspath .)
tut_dir  := $(base_dir)/tutorial/examples
mini_dir := $(base_dir)/riscv-mini
gen_dir  := $(base_dir)/generated-src
log_dir  := $(base_dir)/logs
res_dir  := $(base_dir)/results
strober  := $(wildcard $(base_dir)/strober/src/main/scala/*.scala)

SBT       = sbt
SBT_FLAGS = 

# Designs
tut  := GCD Parity Stack Router Risc RiscSRAM \
        ShiftRegister ResetShiftRegister EnableShiftRegister MemorySearch
mini := Tile

# Chisel Flags
C_FLAGS := --targetDir $(gen_dir) --genHarness --compile --test --vcd --vcdMem --debug 
V_FLAGS := $(C_FLAGS) --v
FPGA_FLAGS := --targetDir $(gen_dir) --backend fpga --configDump 
VCS_FLAGS := --targetDir $(gen_dir) --backend null --noInlineMem --test

# VCS
CONFIG = VLSI
vcs_sim_rtl_dir    := $(base_dir)/vcs-sim-rtl
vcs_sim_gl_syn_dir := $(base_dir)/vcs-sim-gl-syn
vcs_sim_gl_par_dir := $(base_dir)/vcs-sim-gl-par
vcs_sim_rtl        := $(addprefix $(vcs_sim_rtl_dir)/,    $(addsuffix .$(CONFIG), $(tut) $(mini)))
vcs_sim_gl_syn     := $(addprefix $(vcs_sim_gl_syn_dir)/, $(addsuffix .$(CONFIG), $(tut) $(mini)))
vcs_sim_gl_par     := $(addprefix $(vcs_sim_gl_par_dir)/, $(addsuffix .$(CONFIG), $(tut) $(mini)))

# riscv-mini
isa_dir     = $(mini_dir)/riscv-tests/isa
bmarks_dir  = $(mini_dir)/riscv-bmarks
simple_args = +simple +verbose +max-cycles=500
isa_args    = +isa=$(isa_dir) +verbose +max-cycles=3000
bmarks_args = +bmarks=$(bmarks_dir) +max-cycles=500000
include $(mini_dir)/Makefrag-tests

# Rules
include Makefrag-sim
include Makefrag-axi
include Makefrag-fpga
include Makefrag-repl
include Makefrag-ref

$(tut) $(mini): %: %-fpga %-zedboard

tut:
	$(base_dir)/scripts/run-tutorial.py

NUM ?= 30
mini:
	$(base_dir)/scripts/run-multi-samples.py Tile $(NUM)

clean:
	rm -rf $(gen_dir) $(log_dir) $(res_dir) 
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

.PHONY: tut mini $(tut) $(mini) clean cleanall
