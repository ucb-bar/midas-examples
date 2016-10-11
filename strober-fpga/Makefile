###########################
#     FPGA Simulators     #
###########################

base_dir := $(abspath ..)
fpga_dir := $(abspath .)
gen_dir  := $(fpga_dir)/generated-src
res_dir  := $(fpga_dir)/results

include $(base_dir)/Makefrag-strober

CONFIG ?= Strober
MEMMODEL ?= 1

CXX := arm-xilinx-linux-gnueabi-g++
CXXFLAGS := $(CXXFLAGS) -static -O2 -std=c++11 -Wall -I$(driver_dir) -I$(src_dir) -DMEMMODEL=$(MEMMODEL)

# Compile driver
zynq := $(addsuffix -zynq, $(designs))
zynq_binaries := $(addprefix $(res_dir)/, $(designs))
zynq_src = $(src_dir)/$(1)-

$(zynq_binaries): $(res_dir)/%: $(src_dir)/%-zynq.cc $(gen_dir)/%/ZynqShim.v \
	$(driver_cpp) $(driver_h) $(src_dir)/%.h
	$(CXX) $(CXXFLAGS) -include $(gen_dir)/$*/$*-const.h -o $@ $< $(driver_cpp)

$(zynq): %-zynq: $(res_dir)/%

# Generate bitstream
board     ?= zedboard
board_dir := $(fpga_dir)/fpga-zynq/$(board)
bitstream := fpga-images-$(board)/boot.bin

strober   := $(addsuffix -strober, $(designs))
fpga      := $(addsuffix -fpga,    $(designs))

$(strober): %-strober: $(gen_dir)/%-const.h
	mkdir -p $(res_dir)
	cd $(gen_dir) && cp ZynqShim.v ZynqShim.$*.v
	
$(fpga): %-fpga: $(gen_dir)/%/ZynqShim.v
	cd $(board_dir); $(MAKE) clean ; time $(MAKE) $(bitstream) DESIGN=$* CONFIG=$*; \
	cp $(bitstream) $(res_dir)

clean:
	rm -rf $(gen_dir) $(res_dir)

.PHONY: $(strober) $(fpga) $(zynq) clean
