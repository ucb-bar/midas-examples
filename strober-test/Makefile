###########################
#     Strober Tests       #
###########################

base_dir := $(abspath ..)
test_dir := $(abspath .)
gen_dir  := $(test_dir)/generated-src
res_dir  := $(test_dir)/results

include $(base_dir)/Makefrag-strober

MEMMODEL ?= 1
DEBUG ?=
LOADMEM ?=
ARGS ?=

CXXFLAGS := $(CXXFLAGS) -std=c++11 -Wall -I$(driver_dir) -I$(src_dir) -I$(test_dir)/dramsim2 \
	-DMEMMODEL=$(MEMMODEL)
LDFLAGS := $(LDFLAGS) -L$(abspath $(test_dir)) -lstdc++ -lpthread -ldramsim

# compile DRAMSim2
DRAMSIM_OBJS := $(patsubst %.cpp, %.o, $(wildcard $(test_dir)/dramsim2/*.cpp))
$(DRAMSIM_OBJS): %.o: %.cpp
	$(CXX) $(CXXFLAGS) -DNO_STORAGE -DNO_OUTPUT -Dmain=nomain -c -o $@ $<
$(test_dir)/libdramsim.a: $(DRAMSIM_OBJS)
	ar rcs $@ $^

$(res_dir)/dramsim2_ini: $(test_dir)/dramsim2_ini
	mkdir -p $(res_dir)
	cd $(res_dir) && ln -s $<

driver_files := $(default_driver_files) mmio mm mm_dramsim2 context simif_emul
loadmem = $(if $(LOADMEM),+loadmem=$(abspath $(LOADMEM)),)
prefix = $(if $(LOADMEM),$(notdir $(basename $(LOADMEM))),$1)
waveform = +waveform=$(call prefix,$1).$2
logfile = $(call prefix,$1)-$2.log

# Compile verilator
VERILATOR := verilator --cc --exe
VERILATOR_FLAGS := --top-module ZynqShim --assert -Wno-STMTDLY -O3 \
	-CFLAGS "$(CXXFLAGS)" -LDFLAGS "$(LDFLAGS)" $(if $(DEBUG),--trace,)
verilator := $(addsuffix -verilator, $(designs))
ifdef DEBUG
verilator_binaries := $(addprefix $(res_dir)/V, $(addsuffix -debug, $(designs)))
else
verilator_binaries := $(addprefix $(res_dir)/V, $(designs))
endif

$(verilator_binaries): $(res_dir)/V%$(if $(DEBUG),-debug,): $(src_dir)/%-emul.cc $(gen_dir)/%/ZynqShim.v \
	$(test_dir)/libdramsim.a $(driver_cpp) $(driver_h) $(src_dir)/%.h $(res_dir)/dramsim2_ini
	$(VERILATOR) $(VERILATOR_FLAGS) -Mdir $(gen_dir)/$* \
	-CFLAGS "-include $(gen_dir)/$*/$*-const.h -include $(gen_dir)/$*/VZynqShim.h" \
	-o $@ $< $(word 2, $^) $(driver_cpp)
	$(MAKE) -C $(gen_dir)/$* -f VZynqShim.mk

$(verilator): %-verilator: $(res_dir)/V%$(if $(DEBUG),-debug,)
	cd $(res_dir) && \
	./$(notdir $<) +dramsim $(loadmem) $(call waveform,$*,vcd) $(ARGS) 2> $(call logfile,$*,verilator)

# Compile VCS
VCS := vcs -full64
VCS_FLAGS := -quiet -timescale=1ns/1ps +v2k +rad +vcs+initreg+random \
	+vc+list +define+CLOCK_PERIOD=0.5 +vcs+lic+wait -e vcs_main \
	-cpp $(CXX) -CFLAGS "$(CXXFLAGS) -I$(VCS_HOME)/include -DVCS" -LDFLAGS "$(LDFLAGS)" \
	$(if $(DEBUG), -debug_pp +define+DEBUG,)
vcs := $(addsuffix -vcs, $(designs))
ifdef DEBUG
vcs_binaries := $(addprefix $(res_dir)/, $(addsuffix -debug, $(designs)))
else
vcs_binaries := $(addprefix $(res_dir)/, $(designs))
endif

$(vcs_binaries): $(res_dir)/%$(if $(DEBUG),-debug,): $(gen_dir)/%/ZynqShim.v $(src_dir)/%-emul.cc \
	$(test_dir)/libdramsim.a $(driver_v) $(driver_cpp) $(driver_h) $(src_dir)/%.h  $(res_dir)/dramsim2_ini
	$(VCS) $(VCS_FLAGS) -Mdir=$(gen_dir)/$*/$*.csrc -CC "-include $(gen_dir)/$*/$*-const.h" \
	-o $@ $< $(gen_dir)/$*/$*-const.vh $(driver_v) $(driver_cpp) $(word 2, $^) $(word 3, $^)

$(vcs): %-vcs: $(res_dir)/%$(if $(DEBUG),-debug,)
	cd $(res_dir) && \
	./$(notdir $<) +dramsim $(loadmem) $(call waveform,$*,vpd) $(ARGS) 2> $(call logfile,$*,vcs)

clean:
	rm -rf $(gen_dir) $(res_dir)

.PHONY: $(verilator) $(vcs) clean
