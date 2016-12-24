###########################
#     FPGA Simulators     #
###########################

DESIGN ?= Tile
PLATFORM ?= zynq
STROBER ?= 1
# STROBER ?= 1
DRIVER ?=

include Makefrag

strober = $(if $(STORBER),strober,midas)

$(gen_dir)/$(shim).v: $(scala_srcs)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run $(strober) $(DESIGN) $(patsubst $(base_dir)/%,%,$(dir $@)) $(PLATFORM)"

verilog: $(gen_dir)/$(shim).v

$(out_dir)/$(DESIGN).chain: $(gen_dir)/$(shim).v
	$(if (wildcard $(gen_dir)/$(shim).v),cp $(gen_dir)/$(DESIGN).chain $@,)

# Compile driver
ifeq ($(PLATFORM),zynq)
export AR := arm-xilinx-linux-gnueabi-ar
export CXX := arm-xilinx-linux-gnueabi-g++
#export CXXFLAGS := $(CXXFLAGS) -static -O2
endif
ifeq ($(PLATFORM),catapult)
export AR := lib
export CXX := cl
endif

$(out_dir)/$(DESIGN)-$(PLATFORM): $(driver_dir)/$(DESIGN)-$(PLATFORM).cc \
	$(driver_dir)/$(DESIGN).h $(gen_dir)/$(shim).v $(simif_cc) $(simif_h)
	$(MAKE) -C $(simif_dir) $(PLATFORM) DESIGN=$(DESIGN) \
	GEN_DIR=$(gen_dir) OUT_DIR=$(out_dir) \
	DRIVER=$(if $(filter-out sh.exe,$(SHELL)),"$< $(DRIVER)",$<\ $(DRIVER))

$(PLATFORM): $(out_dir)/$(DESIGN)-$(PLATFORM) $(out_dir)/$(DESIGN).chain

ifdef ($(PLATFORM),zynq)
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
endif

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: $($(PLATFORM)) $(fpga) clean
