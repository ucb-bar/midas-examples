###########################
#     Strober Tests       #
###########################

base_dir := $(abspath ..)
test_dir := $(abspath .)
gen_dir  := $(test_dir)/generated-src
out_dir  := $(test_dir)/outputs

DEBUG ?=
LOADMEM ?=
LOGFILE ?=
WAVEFORM ?=
ARGS ?=

include $(base_dir)/Makefrag-strober

$(out_dir)/dramsim2_ini:
	$(MAKE) -C $(src_dir) OUT_DIR=$(out_dir) $@

$(out_dir)/%.chain:
	$(MAKE) -C $(src_dir) DESIGN=$* GEN_DIR=$(gen_dir)/$* OUT_DIR=$(out_dir) $@

debug := $(if $(DEBUG),-debug,)
loadmem = $(if $(LOADMEM),+loadmem=$(abspath $(LOADMEM)),)
prefix = $(notdir $(basename $(if $(LOADMEM),$(LOADMEM),$1)))
logfile = $(if $(LOGFILE),$(abspath $(LOGFILE)),$(call prefix,$1)-$2.log)
waveform = $(if $(WAVEFORM),$(abspath $(WAVEFORM)),$(call prefix,$1).$2)

# Compile verilator
verilator := $(addsuffix -verilator, $(designs))

$(out_dir)/V%$(debug): $(testbench_dir)/%-emul.cc $(testbench_dir)/%.h $(gen_dir)/%/ZynqShim.v $(src_cc) $(src_h)
	$(MAKE) -C $(src_dir) verilator$(debug) DESIGN=$* GEN_DIR=$(gen_dir)/$* OUT_DIR=$(out_dir) TESTBENCH=$<

$(verilator): %-verilator: $(out_dir)/V%$(debug) $(out_dir)/%.chain $(out_dir)/dramsim2_ini
	mkdir -p $(out_dir)
	cd $(out_dir) && ./$(notdir $<) $(ARGS) $(loadmem) \
	+dramsim +waveform=$(call waveform,$*,vcd) 2> $(call logfile,$*,verilator)

# Compile VCS
vcs := $(addsuffix -vcs, $(designs))

$(out_dir)/%$(debug): $(testbench_dir)/%-emul.cc $(testbench_dir)/%.h $(gen_dir)/%/ZynqShim.v $(src_cc) $(src_h)
	$(MAKE) -C $(src_dir) vcs$(debug) DESIGN=$* GEN_DIR=$(gen_dir)/$* OUT_DIR=$(out_dir) TESTBENCH=$<

$(vcs): %-vcs: $(out_dir)/%$(debug) $(out_dir)/%.chain $(out_dir)/dramsim2_ini
	mkdir -p $(out_dir)
	cd $(out_dir) && ./$(notdir $<) $(ARGS) $(loadmem) \
	+dramsim +waveform=$(call waveform,$*,vpd) 2> $(call logfile,$*,vcs)

clean:
	rm -rf $(gen_dir) $(out_dir) *.hex

.PHONY: $(verilator) $(vcs) clean
