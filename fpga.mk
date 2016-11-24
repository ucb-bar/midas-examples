###########################
#     FPGA Simulators     #
###########################

PLATFORM ?= zynq

include Makefrag
include Makefrag-strober

# Compile driver
export AR := arm-xilinx-linux-gnueabi-ar
export CXX := arm-xilinx-linux-gnueabi-g++
#export CXXFLAGS := $(CXXFLAGS) -static -O2

$(out_dir)/$(DESIGN)-$(PLATFORM): $(testbench_dir)/$(DESIGN)-$(PLATFORM).cc $(testbench_dir)/$(DESIGN).h $(gen_dir)/$(shim).v
	mkdir -p $(gen_dir)/$(PLATFORM)
	cp $(gen_dir)/$(DESIGN)-const.h $(gen_dir)/$(PLATFORM)
	$(MAKE) -C $(simif_dir) $(PLATFORM) DESIGN=$(DESIGN) \
	GEN_DIR=$(gen_dir)/$(PLATFORM) OUT_DIR=$(out_dir) TESTBENCH=$<

$(PLATFORM): $(out_dir)/$(DESIGN)-$(PLATFORM) $(out_dir)/$(DESIGN).chain

# Generate bitstream
board     ?= zedboard
board_dir := $(fpga_dir)/midas-$(PLATFORM)/$(board)
bitstream := fpga-images-$(board)/boot.bin

$(board_dir)/src/verilog/$(DEISGN)/$(shim).v: $(gen_dir)/$(shim).v
	mkdir -p $(dir $@)
	cp $< $@

fpga: $(board_dir)/src/verilog/$(shim).v
	mkdir -p $(out_dir)
	$(MAKE) -C $(board_dir) clean DESIGN=$(DESIGN)
	$(MAKE) -C $(board_dir) $(bitstream) DESIGN=$(DESIGN)
	cp $(board_dir)/$(bitstream) $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: $($(PLATFORM)) $(fpga) clean
