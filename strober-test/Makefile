###########################
#     Strober Tests       #
###########################

base_dir := $(abspath ..)
test_dir := $(abspath .)
gen_dir  := $(test_dir)/generated-src
res_dir  := $(test_dir)/results

MEMMODEL ?= 1
DEBUG ?=
LOADMEM ?=
LOGFILE ?=
WAVEFORM ?=
ARGS ?=

include $(base_dir)/Makefrag-strober

CXXFLAGS := $(CXXFLAGS) -I$(src_dir) -I$(test_dir)/dramsim2 -DMEMMODEL=$(MEMMODEL)
LDFLAGS := $(LDFLAGS) -L$(abspath $(test_dir)) -ldramsim

# compile DRAMSim2
DRAMSIM_OBJS := $(patsubst %.cpp, %.o, $(wildcard $(test_dir)/dramsim2/*.cpp))
$(DRAMSIM_OBJS): %.o: %.cpp
	$(CXX) $(CXXFLAGS) -DNO_STORAGE -DNO_OUTPUT -Dmain=nomain -c -o $@ $<
$(test_dir)/libdramsim.a: $(DRAMSIM_OBJS)
	ar rcs $@ $^

$(res_dir)/dramsim2_ini: $(test_dir)/dramsim2_ini
	mkdir -p $(res_dir)
	cd $(res_dir) && ln -s $<

driver_files := $(default_files) mmio mm mm_dramsim2 context simif simif_emul
loadmem = $(if $(LOADMEM),+loadmem=$(abspath $(LOADMEM)),)
prefix = $(if $(LOADMEM),$(notdir $(basename $(LOADMEM))),$1)
logfile = $(if $(LOGFILE),$(abspath $(LOGFILE)),$(call prefix,$1)-$2.log)
waveform = +waveform=$(if $(WAVEFORM),$(abspath $(WAVEFORM)),$(call prefix,$1).$2)

# Compile verilator
VERILATOR_FLAGS := $(VERILATOR_FLAGS) --top-module ZynqShim $(if $(DEBUG),--trace,)

$(verilator_binaries): $(res_dir)/V%$(if $(DEBUG),-debug,): $(src_dir)/%-emul.cc $(gen_dir)/%/ZynqShim.v \
	$(test_dir)/libdramsim.a $(driver_cpp) $(driver_h) $(src_dir)/%.h $(res_dir)/dramsim2_ini
	$(VERILATOR) $(VERILATOR_FLAGS) -Mdir $(gen_dir)/$* \
	-CFLAGS "-include $(gen_dir)/$*/$*-const.h -include $(gen_dir)/$*/VZynqShim.h" \
	-o $@ $< $(word 2, $^) $(driver_cpp)
	$(MAKE) -C $(gen_dir)/$* -f VZynqShim.mk

$(verilator): %-verilator: $(res_dir)/V%$(if $(DEBUG),-debug,) $(res_dir)/%/%.chain
	cd $(res_dir) && \
	./$(notdir $<) +dramsim $(loadmem) $(call waveform,$*,vcd) $(ARGS) 2> $(call logfile,$*,verilator)

# Compile VCS
VCS_FLAGS := $(VCS_FLAGS) +vc+list -e vcs_main +define+CLOCK_PERIOD=0.5 \
	$(if $(DEBUG), -debug_pp +define+DEBUG,)

$(vcs_binaries): $(res_dir)/%$(if $(DEBUG),-debug,): $(gen_dir)/%/ZynqShim.v $(src_dir)/%-emul.cc \
	$(test_dir)/libdramsim.a $(driver_v) $(driver_cpp) $(driver_h) $(src_dir)/%.h  $(res_dir)/dramsim2_ini
	rm -rf $(gen_dir)/$*/$*.csrc $(res_dir)/$.daidir
	$(VCS) $(VCS_FLAGS) -Mdir=$(gen_dir)/$*/$*.csrc -CC "-include $(gen_dir)/$*/$*-const.h" \
	-o $@ $< $(gen_dir)/$*/$*-const.vh $(driver_v) $(driver_cpp) $(word 2, $^) $(word 3, $^)

$(vcs): %-vcs: $(res_dir)/%$(if $(DEBUG),-debug,) $(res_dir)/%/%.chain
	cd $(res_dir) && \
	./$(notdir $<) +dramsim $(loadmem) $(call waveform,$*,vpd) $(ARGS) 2> $(call logfile,$*,vcs)

clean:
	rm -rf $(gen_dir) $(res_dir) DVEfiles libdramsim.a *.hex

.PHONY: $(verilator) $(vcs) clean
