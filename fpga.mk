###########################
#     FPGA Simulators     #
###########################

DESIGN ?= Tile
PLATFORM ?= zynq
STROBER ?=
MACRO_LIB ?=
DRIVER ?=

include Makefrag
include Makefrag-plsi

verilog = $(gen_dir)/$(shim).v
header = $(gen_dir)/$(DESIGN)-const.h
strober = $(if $(STROBER),strober,midas)
macro_lib = $(if $(MACRO_LIB),$(technology_macro_lib),)

$(verilog) $(header): $(scala_srcs) publish $(macro_lib)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run $(strober) $(DESIGN) $(patsubst $(base_dir)/%,%,$(dir $@)) $(PLATFORM) $(macro_lib)"

verilog: $(verilog)

$(out_dir)/$(DESIGN).chain: $(gen_dir)/$(shim).v
	$(if (wildcard $(gen_dir)/$(shim).v),cp $(gen_dir)/$(DESIGN).chain $@,)

# Compile driver
ifeq ($(PLATFORM),zynq)
host = arm-xilinx-linux-gnueabi

# Compile gmp
GMP_VERSION ?= 6.1.2
gmp_src_dir := $(base_dir)/output/$(PLATFORM)/gmp-$(GMP_VERSION)
gmp_build_dir := $(gmp_src_dir)/build
gmp_install_dir := $(gmp_src_dir)/install
gmp_lib := $(gmp_install_dir)/lib/libgmp.so
gmp := $(out_dir)/libgmp.so.10

$(base_dir)/output/$(PLATFORM)/gmp-$(GMP_VERSION).tar.bz2:
	mkdir -p $(dir $@)
	cd $(dir $@) && wget https://gmplib.org/download/gmp/gmp-$(GMP_VERSION).tar.bz2

$(gmp_src_dir): $(base_dir)/output/$(PLATFORM)/gmp-$(GMP_VERSION).tar.bz2
	cd $(dir $<) && tar -xf $<

$(gmp_lib): $(gmp_src_dir)
	mkdir -p $(gmp_build_dir)
	cd $(gmp_build_dir) && \
	../configure --prefix=$(gmp_install_dir) --host=$(host) && \
	$(MAKE) && $(MAKE) install

$(gmp): $(gmp_lib)
	cp $< $@

export AR := $(host)-ar
export CXX := $(host)-g++
export CXXFLAGS := $(CXXFLAGS) -I$(gmp_install_dir)/include
export LDFLAGS := -L$(gmp_install_dir)/lib -Wl,-rpath,/usr/local/lib
endif

$(out_dir)/$(DESIGN)-$(PLATFORM): $(driver_dir)/$(DESIGN)-$(PLATFORM).cc \
	$(simif_cc) $(simif_h) $(header) $(gmp)
	mkdir -p $(out_dir)/build
	cp $(header) $(out_dir)/build/
	$(MAKE) -C $(simif_dir) $(PLATFORM) DESIGN=$(DESIGN) \
	GEN_DIR=$(out_dir)/build OUT_DIR=$(out_dir) DRIVER="$< $(DRIVER)"

$(PLATFORM): $(out_dir)/$(DESIGN)-$(PLATFORM) $(out_dir)/$(DESIGN).chain

ifeq ($(PLATFORM),zynq)
# Generate bitstream
board     ?= zedboard
board_dir := $(base_dir)/platforms/$(PLATFORM)/$(board)
bitstream := fpga-images-$(board)/boot.bin

$(board_dir)/src/verilog/$(DESIGN)/$(shim).v: $(verilog)
	$(MAKE) -C $(board_dir) clean DESIGN=$(DESIGN)
	mkdir -p $(dir $@)
	cp $< $@

fpga: $(board_dir)/src/verilog/$(DESIGN)/$(shim).v
	mkdir -p $(out_dir)
	$(MAKE) -C $(board_dir) $(bitstream) DESIGN=$(DESIGN)
	cp $(board_dir)/$(bitstream) $(out_dir)/
endif

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: $($(PLATFORM)) $(fpga) clean
