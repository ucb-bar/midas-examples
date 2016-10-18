###########################
#     FPGA Simulators     #
###########################

base_dir := $(abspath ..)
fpga_dir := $(abspath .)
gen_dir  := $(fpga_dir)/generated-src
out_dir  := $(fpga_dir)/outputs

include $(base_dir)/Makefrag-strober

CONFIG ?= Strober
MEMMODEL ?= 1

# Compile driver
export CXX := arm-xilinx-linux-gnueabi-g++
export CXXFLAGS := $(CXXFLAGS) -static -O2 -DMEMMODEL=$(MEMMODEL)

zynq := $(addsuffix -zynq, $(designs))

$(out_dir)/%-zynq: $(testbench_dir)/%-zynq.cc $(gen_dir)/%/ZynqShim.v
	$(MAKE) -C $(src_dir) zynq DESIGN=$* GEN_DIR=$(gen_dir)/$* OUT_DIR=$(out_dir) TESTBENCH=$<

$(zynq): %-zynq: $(out_dir)/%-zynq

# Generate bitstream
board     ?= zedboard
board_dir := $(fpga_dir)/fpga-zynq/$(board)
bitstream := fpga-images-$(board)/boot.bin

strober   := $(addsuffix -strober, $(designs))
fpga      := $(addsuffix -fpga,    $(designs))

$(strober): %-strober: $(gen_dir)/%-const.h
	mkdir -p $(out_dir)
	cd $(gen_dir) && cp ZynqShim.v ZynqShim.$*.v
	
$(fpga): %-fpga: $(gen_dir)/%/ZynqShim.v
	cd $(board_dir); $(MAKE) clean ; time $(MAKE) $(bitstream) DESIGN=$* CONFIG=$*; \
	cp $(bitstream) $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: $(zynq) $(fpga) clean
