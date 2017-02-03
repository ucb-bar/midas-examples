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

$(gen_dir)/$(DESIGN).v: $(scala_srcs)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run replay $(DESIGN) $(patsubst $(base_dir)/%,%,$(dir $@))"

replay_h = $(simif_dir)/sample.h $(wildcard $(simif_dir)/replay/*.h)
replay_cc = $(simif_dir)/sample.cc $(wildcard $(simif_dir)/replay/*.cc)

# Replay with RTL
$(gen_dir)/$(DESIGN)-rtl: $(gen_dir)/$(DESIGN).v $(replay_cc) $(replay_h)
	$(MAKE) -C $(simif_dir) $@ DESIGN=$(DESIGN) GEN_DIR=$(gen_dir) \
	TARGET_VERILOG=$< REPLAY_BINARY=$@

vcs-rtl: $(gen_dir)/$(DESIGN)-rtl

replay-rtl: $(gen_dir)/$(DESIGN)-rtl
	mkdir -p $(out_dir)
	cd $(gen_dir) && ./$(notdir $<) +sample=$(sample) +verbose \
	+waveform=$(call waveform,$@) 2> $(call logfile,$@)

# PLSI
include Makefile.project
CORE_GENERATOR_ADDON_DIR = $(plsi_dir)/src/addons/core-generator/$(DESIGN)
OBJ_TECH_DIR = $(plsi_dir)/obj/technology/$(TECHNOLOGY)
OBJ_SYN_DIR = $(plsi_dir)/obj/syn-$(DESIGN)-$(CORE_CONFIG)-$(SOC_CONFIG)-$(TECHNOLOGY)-$(MAP_CONFIG)-$(SYN_CONFIG)

plsi_files = $(addprefix $(CORE_GENERATOR_ADDON_DIR)/, vars.mk rules.mk \
	$(addprefix src/, replay.v replay.macros.v replay.macros.json))
$(plsi_files): $(CORE_GENERATOR_ADDON_DIR)/%: $(base_dir)/scripts/plsi/%
	mkdir -p $(dir $@)
	cp $< $@

plsi_rules = syn-verilog check-syn
$(plsi_rules): $(plsi_files) $(CORE_GENERATOR_ADDON_DIR)/src/$(DESIGN).v
	$(MAKE) -C $(plsi_dir) $@ CORE_GENERATOR=$(DESIGN) CORE_DIR="$(base_dir)"

$(CORE_GENERATOR_ADDON_DIR)/src/$(DESIGN).v: $(gen_dir)/$(DESIGN).v
	mkdir -p $(dir $@)
	cp $< $@

syn_verilog = $(OBJ_SYN_DIR)/synopsys-dc-workdir/results/$(DESIGN).mapped.v
syn_match_points = $(OBJ_SYN_DIR)/synopsys-dc-workdir/reports/$(DESIGN).fmv_matched_points.rpt
$(syn_verilog): syn-verilog
$(syn_match_points): check-syn

match_translator = $(simif_dir)/../resources/translator.py
match_file = $(gen_dir)/$(DESIGN).match
$(match_file): $(syn_match_points) $(match_translator)
	$(match_translator) --output $@ --match $<

ifneq ($(filter $(MAKECMDGOALS),$(gen_dir)/$(DESIGN)-syn vcs-syn replay-syn),)
-include $(OBJ_TECH_DIR)/makefrags/vars.mk
$(OBJ_TECH_DIR)/makefrags/vars.mk: syn-verilog
endif

# Replay with Post-Synthesis
$(gen_dir)/$(DESIGN)-syn: $(syn_verilog) $(replay_cc) $(replay_h) $(OBJ_TECH_DIR)/makefrags/vars.mk
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
