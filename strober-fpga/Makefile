###########################
#     FPGA Simulators     #
###########################

base_dir := $(abspath ..)
fpga_dir := $(abspath .)
gen_dir  := $(fpga_dir)/generated-src
out_dir  := $(fpga_dir)/outputs

include $(base_dir)/Makefrag-strober

CONFIG ?= Strober

# Compile driver
export CXX := arm-xilinx-linux-gnueabi-g++
export CXXFLAGS := $(CXXFLAGS) -static -O2

zynq := $(addsuffix -zynq, $(designs))

$(out_dir)/%-zynq: $(testbench_dir)/%-zynq.cc $(gen_dir)/%/ZynqShim.v
	$(MAKE) -C $(src_dir) zynq DESIGN=$* GEN_DIR=$(gen_dir)/$* OUT_DIR=$(out_dir) TESTBENCH=$<

$(zynq): %-zynq: $(out_dir)/%-zynq $(out_dir)/%.chain

# Generate bitstream
board     ?= zedboard
board_dir := $(fpga_dir)/midas-zynq/$(board)
bitstream := fpga-images-$(board)/boot.bin

strober   := $(addsuffix -strober, $(designs))
fpga      := $(addsuffix -fpga,    $(designs))

$(strober): %-strober: $(gen_dir)/%/ZynqShim.v
	
$(board_dir)/src/verilog/%/ZynqShim.v: $(gen_dir)/%/ZynqShim.v
	mkdir -p $(dir $@)
	cp $< $@

$(fpga): %-fpga: $(board_dir)/src/verilog/%/ZynqShim.v
	mkdir -p $(out_dir)
	$(MAKE) -C $(board_dir) clean DESIGN=$*
	$(MAKE) -C $(board_dir) $(bitstream) DESIGN=$*
	cp $(board_dir)/$(bitstream) $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: $(zynq) $(fpga) clean
