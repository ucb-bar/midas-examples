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

# Generate bitstream
board     ?= zedboard
board_dir := $(fpga_dir)/fpga-zynq/$(board)
bitstream := fpga-images-$(board)/boot.bin

strober   := $(addsuffix -strober, $(designs))
fpga      := $(addsuffix -fpga,    $(designs))

$(addprefix $(gen_dir)/, $(addsuffix -const.h, $(designs))): $(gen_dir)/%-const.h: $(scala_srcs)
	mkdir -p $(res_dir)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) "run strober $* $(gen_dir)" 
	cd $(gen_dir) && cp ZynqShim.v ZynqShim.$*.v
	cd $(gen_dir) && cp $*.map $(res_dir)
	if [ -a $(gen_dir)/$*.chain ]; \
	then cd $(gen_dir) && cp $*.chain $(res_dir); \
	fi;

$(strober): %-strober: $(gen_dir)/%-const.h
	
$(fpga): %-fpga: $(gen_dir)/%-const.h
	cd $(board_dir); $(MAKE) clean ; time $(MAKE) $(bitstream) DESIGN=$* CONFIG=$*; \
	cp $(bitstream) $(res_dir)

# Compile driver
zynq       := $(addsuffix -zynq, $(designs))
csrc_dir   := $(base_dir)/src/main/cc
driver_dir := $(base_dir)/strober/csrc
driver_src := $(wildcard $(driver_dir)/*.cc)
driver_obj := $(addprefix $(gen_dir)/, $(notdir $(patsubst %.cc, %.o, $(driver_src))))
CXX := arm-xilinx-linux-gnueabi-g++
CXXFLAGS := $(CXXFLAGS) -static -O2 -std=c++11 -Wall -I$(driver_dir) -DMEMMODEL=$(MEMMODEL)

$(zynq): %-zynq: $(gen_dir)/%-const.h $(csrc_dir)/%.cc $(driver_src)
	mkdir -p $(res_dir)
	$(CXX) $(CXXFLAGS) -include$< -o $(res_dir)/$(notdir $@) $(word 2, $^) $(driver_src)

clean:
	rm -rf $(gen_dir) $(res_dir)

.PHONY: $(strober) $(fpga) $(zynq) clean
