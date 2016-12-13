###########################
#     FPGA Simulators     #
###########################

DESIGN ?= Tile
PLATFORM ?= zynq

include Makefrag

$(gen_dir)/$(shim).v: $(scala_srcs)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) "run strober $(DESIGN) $(dir $@) $(PLATFORM)"

$(out_dir)/$(DESIGN).chain: $(gen_dir)/$(shim).v
	cp $(gen_dir)/$(DESIGN).chain $@

# Compile driver
export AR := arm-xilinx-linux-gnueabi-ar
export CXX := arm-xilinx-linux-gnueabi-g++
#export CXXFLAGS := $(CXXFLAGS) -static -O2

$(out_dir)/$(DESIGN)-$(PLATFORM): $(testbench_dir)/$(DESIGN)-$(PLATFORM).cc \
	$(testbench_dir)/$(DESIGN).h $(gen_dir)/$(shim).v $(simif_cc) $(simif_h)
	mkdir -p $(gen_dir)/$(PLATFORM)
	cp $(gen_dir)/$(DESIGN)-const.h $(gen_dir)/$(PLATFORM)
	$(MAKE) -C $(simif_dir) $(PLATFORM) DESIGN=$(DESIGN) \
	GEN_DIR=$(gen_dir)/$(PLATFORM) OUT_DIR=$(out_dir) TESTBENCH=$<

$(PLATFORM): $(out_dir)/$(DESIGN)-$(PLATFORM) $(out_dir)/$(DESIGN).chain

# Generate bitstream
board     ?= zedboard
board_dir := $(base_dir)/midas-$(PLATFORM)/$(board)
bitstream := fpga-images-$(board)/boot.bin

$(board_dir)/src/verilog/$(DESIGN)/$(shim).v: $(gen_dir)/$(shim).v
	$(MAKE) -C $(board_dir) clean DESIGN=$(DESIGN)
	mkdir -p $(dir $@)
	cp $< $@

fpga: $(board_dir)/src/verilog/$(DESIGN)/$(shim).v
	mkdir -p $(out_dir)
	$(MAKE) -C $(board_dir) $(bitstream) DESIGN=$(DESIGN)
	cp $(board_dir)/$(bitstream) $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: $($(PLATFORM)) $(fpga) clean
