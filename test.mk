###########################
#     Strober Tests       #
###########################

DESIGN ?= Tile
PLATFORM ?= zynq

include Makefrag

LOADMEM ?=
SAMPLE ?=
LOGFILE ?=
WAVEFORM ?=
STROBER ?=
MACRO_LIB ?= 1
ARGS ?= +fastloadmem +mm_MEM_LATENCY=10

strober = $(if $(STROBER),strober,midas)
loadmem = $(if $(LOADMEM),+loadmem=$(abspath $(LOADMEM)),)
benchmark = $(notdir $(basename $(if $(LOADMEM),$(notdir $(LOADMEM)),$(DESIGN))))
sample = $(if $(SAMPLE),$(abspath $(SAMPLE)),$(out_dir)/$(benchmark).sample)
logfile = $(if $(LOGFILE),$(abspath $(LOGFILE)),$(out_dir)/$(benchmark).$1.out)
waveform = $(if $(WAVEFORM),$(abspath $(WAVEFORM)),$(out_dir)/$(benchmark).$1)

include Makefrag-plsi
macro_lib = $(if $(MACRO_LIB),$(technology_macro_lib),)

$(gen_dir)/$(shim).v: $(scala_srcs) publish $(macro_lib)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run $(strober) $(DESIGN) $(patsubst $(base_dir)/%,%,$(dir $@)) $(PLATFORM) $(macro_lib)"

# Compile Verilator
$(gen_dir)/V$(DESIGN) $(gen_dir)/V$(DESIGN)-debug: $(gen_dir)/V$(DESIGN)%: \
	$(driver_dir)/$(DESIGN)-emul.cc $(driver_dir)/$(DESIGN).h $(gen_dir)/$(shim).v $(simif_cc) $(simif_h)
	$(MAKE) -C $(simif_dir) verilator$* DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) DRIVER=$<
verilator: $(gen_dir)/V$(DESIGN)
verilator-debug: $(gen_dir)/V$(DESIGN)-debug

# Run Veriltor test
verilator-test verilator-test-debug: verilator-test%: $(gen_dir)/V$(DESIGN)%
	mkdir -p $(out_dir)
	cd $(gen_dir) && ./$(notdir $<) $(ARGS) $(loadmem) +dramsim +sample=$(sample) \
	+waveform=$(call waveform,vcd) 2> $(call logfile,verilator)

# Compile VCS
$(gen_dir)/$(DESIGN) $(gen_dir)/$(DESIGN)-debug: $(gen_dir)/$(DESIGN)%: \
	$(driver_dir)/$(DESIGN)-emul.cc $(driver_dir)/$(DESIGN).h $(gen_dir)/$(shim).v $(simif_cc) $(simif_h)
	$(MAKE) -C $(simif_dir) vcs$* DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) DRIVER=$<
vcs: $(gen_dir)/$(DESIGN)
vcs-debug: $(gen_dir)/$(DESIGN)-debug

# Run VCS test
vcs-test vcs-test-debug: vcs-test%: $(gen_dir)/$(DESIGN)%
	mkdir -p $(out_dir)
	cd $(gen_dir) && ./$(notdir $<) $(ARGS) $(loadmem) +dramsim +sample=$(sample) \
	+waveform=$(call waveform,vpd) 2> $(call logfile,vcs)

mostlyclean:
	rm -rf $(gen_dir)/V$(DESIGN) $(gen_dir)/V$(DESIGN).csrc
	rm -rf $(gen_dir)/V$(DESIGN)-debug $(gen_dir)/V$(DESIGN)-debug.csrc
	rm -rf $(gen_dir)/$(DESIGN)$ $(gen_dir)/$(DESIGN).csrc $(gen_dir)/$(DESIGN).daidir
	rm -rf $(gen_dir)/$(DESIGN)-debug $(gen_dir)/$(DESIGN)-debug.csrc $(gen_dir)/$(DESIGN)-debug.daidir
	rm -rf $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: verilator verilator-debug verilator-test verilator-test-debug
.PHONY: vcs vcs-debug vcs-test vcs-test-debug mostlyclaen clean
