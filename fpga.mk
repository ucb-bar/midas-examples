###########################
#     FPGA Simulators     #
###########################

include Makefrag
include Makefrag-strober

# Compile driver
export CXX := arm-xilinx-linux-gnueabi-g++
export CXXFLAGS := $(CXXFLAGS) -static -O2

$(out_dir)/$(DESIGN)-zynq: $(testbench_dir)/$(DESIGN)-zynq.cc $(testbench_dir)/$(DESIGN).h $(gen_dir)/ZynqShim.v
	$(MAKE) -C $(simif_dir) zynq DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) OUT_DIR=$(out_dir) TESTBENCH=$<

zynq: $(out_dir)/$(DESIGN)-zynq $(out_dir)/$(DESIGN).chain

# Generate bitstream
board     ?= zedboard
board_dir := $(fpga_dir)/midas-zynq/$(board)
bitstream := fpga-images-$(board)/boot.bin

$(board_dir)/src/verilog/$(DEISGN)/ZynqShim.v: $(gen_dir)/ZynqShim.v
	mkdir -p $(dir $@)
	cp $< $@

fpga: $(board_dir)/src/verilog/ZynqShim.v
	mkdir -p $(out_dir)
	$(MAKE) -C $(board_dir) clean DESIGN=$(DESIGN)
	$(MAKE) -C $(board_dir) $(bitstream) DESIGN=$(DESIGN)
	cp $(board_dir)/$(bitstream) $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: $(zynq) $(fpga) clean
