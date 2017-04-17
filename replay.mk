##################
# Sample Replays #
##################

DESIGN ?= Tile
PLATFORM ?= zynq

include Makefrag

SAMPLE ?= $(out_dir)/$(DESIGN).sample
LOGFILE ?=
WAVEFORM ?=

sample = $(abspath $(SAMPLE))
prefix = $(notdir $(basename $(SAMPLE)))
logfile = $(if $(LOGFILE),$(abspath $(LOGFILE)),$(out_dir)/$(prefix)-$1.out)
waveform = $(if $(WAVEFORM),$(abspath $(WAVEFORM)),$(out_dir)/$(prefix)-$1.vpd)

verilog = $(gen_dir)/$(DESIGN).v
macros = $(gen_dir)/$(DESIGN).macros.v
testbench = $(vsrc_dir)/replay.v
$(verilog) $(macros): $(scala_srcs) publish
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run replay $(DESIGN) $(patsubst $(base_dir)/%,%,$(dir $@))"

replay_h = $(simif_dir)/sample.h $(wildcard $(simif_dir)/replay/*.h)
replay_cc = $(simif_dir)/sample.cc $(wildcard $(simif_dir)/replay/*.cc)

# Replay with RTL
$(gen_dir)/$(DESIGN)-rtl: $(verilog) $(macro) $(testbench) $(replay_cc) $(replay_h)
	$(MAKE) -C $(simif_dir) $@ DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) REPLAY_BINARY=$@

vcs-rtl: $(gen_dir)/$(DESIGN)-rtl

replay-rtl: $(gen_dir)/$(DESIGN)-rtl
	mkdir -p $(out_dir)
	cd $(gen_dir) && ./$(notdir $<) +sample=$(sample) +verbose \
	+waveform=$(call waveform,$@) 2> $(call logfile,$@)

# PLSI
include Makefile.project
CORE_GENERATOR_ADDON = $(plsi_dir)/src/addons/core-generator/$(DESIGN)
SYN_FORMAL_ADDON = $(plsi_dir)/src/addons/formal/formality
OBJ_TECH_DIR = $(plsi_dir)/obj/technology/$(TECHNOLOGY)
OBJ_MAP_DIR = $(plsi_dir)/obj/map-$(DESIGN)-$(CORE_CONFIG)-$(SOC_CONFIG)-$(TECHNOLOGY)-$(MAP_CONFIG)
OBJ_SYN_DIR = $(plsi_dir)/obj/syn-$(DESIGN)-$(CORE_CONFIG)-$(SOC_CONFIG)-$(TECHNOLOGY)-$(MAP_CONFIG)-$(SYN_CONFIG)

plsi_files = $(addprefix $(CORE_GENERATOR_ADDON)/, vars.mk rules.mk src/replay.v)
$(plsi_files): $(CORE_GENERATOR_ADDON)/%: $(base_dir)/scripts/plsi/%
	mkdir -p $(dir $@)
	cp $< $@

design_files = $(addprefix $(CORE_GENERATOR_ADDON)/src/, $(DESIGN).v $(DESIGN).conf)
$(design_files): $(CORE_GENERATOR_ADDON)/src/%: $(gen_dir)/%
	mkdir -p $(dir $@)
	cp $< $@

plsi_rules = syn-verilog check-syn
$(plsi_rules): $(plsi_files) $(design_files)
	$(MAKE) -C $(plsi_dir) $@ CORE_GENERATOR=$(DESIGN) CORE_DIR="$(base_dir)"

ifneq ($(filter $(MAKECMDGOALS),$(gen_dir)/$(DESIGN)-syn vcs-syn replay-syn),)
include $(SYN_FORMAL_ADDON)/syn-vars.mk
-include $(OBJ_TECH_DIR)/makefrags/vars.mk
$(OBJ_TECH_DIR)/makefrags/vars.mk: syn-verilog
endif

syn_verilog = $(OBJ_SYN_DIR)/synopsys-dc-workdir/results/$(DESIGN).mapped.v
syn_macros = $(OBJ_MAP_DIR)/plsi-generated/$(DESIGN).macros_for_synthesis.v
syn_match_points = $(OBJ_SYN_DIR)/synopsys-dc-workdir/reports/$(DESIGN).fmv_matched_points.rpt
syn_svf_txt = $(OBJ_SYN_DIR)/synopsys-dc-workdir/formality_svf/svf.txt

$(syn_verilog) $(syn_macros): syn-verilog
$(syn_match_points) $(syn_svf_txt): check-syn

fm_match = $(rsrc_dir)/replay/fm-match.py
fm_macro = $(rsrc_dir)/replay/fm-macro.py
match_file = $(gen_dir)/$(DESIGN).match
$(match_file): $(syn_match_points) $(syn_svf_txt) $(fm_match) $(fm_macro)
	cd $(gen_dir) && \
	$(fm_match) --match $@ --report $< --svf $(word 2, $^) && \
	$(fm_macro) --match $@ --conf $(gen_dir)/$(DESIGN).conf \
	--paths $(gen_dir)/$(DESIGN).macro.paths \
	--ref $(macros) --impl $(syn_macros) $(TECHNOLOGY_VERILOG_FILES)

# Replay with Post-Synthesis
$(gen_dir)/$(DESIGN)-syn: $(syn_verilog) $(test_bench) $(replay_cc) $(replay_h) $(OBJ_TECH_DIR)/makefrags/vars.mk
	$(MAKE) -C $(simif_dir) $@ DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) \
	TARGET_VERILOG="$< $(TECHNOLOGY_VERILOG_FILES)" REPLAY_BINARY=$@ \
	VCS_FLAGS="+nospecify +evalorder"

vcs-syn: $(gen_dir)/$(DESIGN)-syn

replay-syn: $(gen_dir)/$(DESIGN)-syn $(match_file)
	mkdir -p $(out_dir)
	cd $(gen_dir) && ./$(notdir $<) +sample=$(sample) +verbose \
	+match=$(match_file) +waveform=$(call waveform,$@) 2> $(call logfile,$@)

mostlyclean:
	rm -rf $(gen_dir)/$(DESIGN)-rtl $(gen_dir)/$(DESIGN)-syn
	rm -rf $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: vcs-rtl replay-rtl vcs-syn replay-syn
.PHONY: $(plsi_rules)
.PHONY: mostlyclean clean
