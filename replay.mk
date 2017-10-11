##################
# Sample Replays #
##################

DESIGN ?= Tile
PLATFORM ?= zynq

include Makefrag

SAMPLE ?= $(out_dir)/$(DESIGN).sample
LOGFILE ?=
WAVEFORM ?=
MACRO_LIB ?=

sample = $(abspath $(SAMPLE))
benchmark = $(notdir $(basename $(SAMPLE)))
logfile = $(if $(LOGFILE),$(abspath $(LOGFILE)),$(out_dir)/$(benchmark).$1.out)
waveform = $(if $(WAVEFORM),$(abspath $(WAVEFORM)),$(out_dir)/$(benchmark).$1)

include Makefrag-plsi
macro_lib = $(if $(MACRO_LIB),$(technology_macro_lib),)

verilog = $(gen_dir)/$(DESIGN).v
macros = $(gen_dir)/$(DESIGN).macros.v
testbench = $(vsrc_dir)/replay.v
$(verilog) $(macros): $(scala_srcs) publish $(macro_lib)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run replay $(DESIGN) $(patsubst $(base_dir)/%,%,$(dir $@)) $(macro_lib)"

replay_h = $(simif_dir)/sample/sample.h $(wildcard $(simif_dir)/replay/*.h)
replay_cc = $(simif_dir)/sample/sample.cc $(wildcard $(simif_dir)/replay/*.cc)

replay_sample = $(rsrc_dir)/replay/replay-samples.py
estimate_power = $(rsrc_dir)/replay/estimate-power.py

# Replay with RTL
$(gen_dir)/$(DESIGN)-rtl: $(verilog) $(macros) $(testbench) $(replay_cc) $(replay_h)
	$(MAKE) -C $(simif_dir) $@ DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) REPLAY_BINARY=$@

vcs-rtl: $(gen_dir)/$(DESIGN)-rtl

replay-rtl: $(gen_dir)/$(DESIGN)-rtl
	mkdir -p $(out_dir)
	cd $(gen_dir) && ./$(notdir $<) +sample=$(sample) +verbose +waveform=$(call waveform,vpd) 2> $(call logfile,vcs)

# Find match points
fm_match = $(rsrc_dir)/replay/fm-match.py
fm_macro = $(rsrc_dir)/replay/fm-macro.py
match_file = $(gen_dir)/$(DESIGN).match
$(match_file): $(syn_match_points) $(syn_svf_txt) $(fm_match) $(fm_macro)
	cd $(gen_dir) && \
	$(fm_match) --match $@ --report $< --svf $(word 2, $^) && \
	$(fm_macro) --match $@ --paths $(gen_dir)/$(DESIGN).macros.path \
	--ref $(macros) --impl $(TECHNOLOGY_VERILOG_SIMULATION_FILES)

match: $(match_file)

replay-rtl-pwr: $(gen_dir)/$(DESIGN)-rtl
	mkdir -p $(out_dir)
	$(replay_sample) --sim $< --sample $(sample) --dir $(out_dir)/$@
	$(estimate_power) --design $(DESIGN) --sample $(sample) \
	--make $(DESIGN)-syn-pwr DESIGN=$(DESIGN) PLATFORM=$(PLATFORM) PRIMETIME_RTL_TRACE=1 \
	--output-dir $(out_dir)/$@ --obj-dir $(OBJ_SYN_DIR) --trace-dir $(TRACE_SYN_DIR)

# Replay with Post-Synthesis
$(gen_dir)/$(DESIGN)-syn: $(syn_verilog) $(test_bench) $(replay_cc) $(replay_h)
	$(MAKE) -C $(simif_dir) $@ DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) \
	TARGET_VERILOG="$< $(TECHNOLOGY_VERILOG_SIMULATION_FILES)" REPLAY_BINARY=$@ \
	VCS_FLAGS="+nospecify +evalorder"

vcs-syn: $(gen_dir)/$(DESIGN)-syn

replay-syn: $(gen_dir)/$(DESIGN)-syn $(match_file)
	mkdir -p $(out_dir)
	$(replay_sample) --sim $< --match $(word 2, $^) --sample $(sample) --dir $(out_dir)/$@
	$(estimate_power) --design $(DESIGN) --sample $(sample) \
	--make $(DESIGN)-syn-pwr DESIGN=$(DESIGN) PLATFORM=$(PLATFORM) \
	--output-dir $(out_dir)/$@ --obj-dir $(OBJ_SYN_DIR) --trace-dir $(TRACE_SYN_DIR)

# Replay with Post-Place-and-Route (PAR)
$(gen_dir)/$(DESIGN)-par: $(par_verilog) $(test_bench) $(replay_cc) $(replay_h) $(par_sdf)
	$(MAKE) -C $(simif_dir) $@ DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) \
	TARGET_VERILOG="$< $(TECHNOLOGY_VERILOG_SIMULATION_FILES)" REPLAY_BINARY=$@ \
	VCS_FLAGS="+neg_tchk +sdfverbose -negdelay -sdf max:$(DESIGN):$(par_sdf)"

vcs-par: $(gen_dir)/$(DESIGN)-par

replay-par: $(gen_dir)/$(DESIGN)-par $(match_file)
	mkdir -p $(out_dir)
	$(replay_sample) --sim $< --match $(word 2, $^) --sample $(sample) --dir $(out_dir)/$@
	$(estimate_power) --design $(DESIGN) --sample $(sample) \
	--make $(DESIGN)-par-pwr DESIGN=$(DESIGN) PLATFORM=$(PLATFORM) \
	--output-dir $(out_dir)/$@ --obj-dir $(OBJ_PAR_DIR) --trace-dir $(TRACE_PAR_DIR)

mostlyclean:
	rm -rf $(gen_dir)/$(DESIGN)-rtl $(gen_dir)/$(DESIGN)-syn $(gen_dir)/$(DESIGN)-par
	rm -rf $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: vcs-rtl replay-rtl
.PHONY: vcs-syn replay-syn
.PHONY: vcs-par replay-par
.PHONY: $(plsi_rules)
.PHONY: mostlyclean clean
