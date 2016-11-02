##################
# Sample Replays #
##################

base_dir   = $(abspath ..)
replay_dir = $(abspath .)
gen_dir    = $(replay_dir)/generated-src
out_dir    = $(replay_dir)/outputs
vcs_sim_rtl_dir    = $(replay_dir)/vcs-sim-rtl
vcs_sim_gl_syn_dir = $(replay_dir)/vcs-sim-gl-syn
vcs_sim_gl_par_dir = $(replay_dir)/vcs-sim-gl-par
pt_pwr_dir = $(replay_dir)/pt-pwr

SAMPLE ?=
LOGFILE ?=
WAVEFORM ?=

include $(base_dir)/Makefrag-strober

CONFIG := VLSI

#replay_cpp    := $(addsuffix -replay-cpp,    $(designs))
replay_vcs    := $(addsuffix -replay-vcs,    $(designs))
replay_rtl    := $(addsuffix -replay-rtl,    $(designs))
replay_gl_syn := $(addsuffix -replay-gl-syn, $(designs))
replay_gl_par := $(addsuffix -replay-gl-par, $(designs))
replay_pwr    := $(addsuffix -replay-pwr,    $(designs))

vcs_sim_rtl    := $(addprefix $(vcs_sim_rtl_dir)/,    $(addsuffix .$(CONFIG), $(designs)))
vcs_sim_gl_syn := $(addprefix $(vcs_sim_gl_syn_dir)/, $(addsuffix .$(CONFIG), $(designs)))
vcs_sim_gl_par := $(addprefix $(vcs_sim_gl_par_dir)/, $(addsuffix .$(CONFIG), $(designs)))

# VCS Simulators
$(vcs_sim_rtl): $(vcs_sim_rtl_dir)/%.$(CONFIG):
	cd $(vcs_sim_rtl_dir) && $(MAKE) MODEL=$*

$(vcs_sim_gl_syn): $(vcs_sim_gl_syn_dir)/%.$(CONFIG):
	cd $(vcs_sim_gl_syn_dir) && $(MAKE) MODEL=$*

$(vcs_sim_gl_par): $(vcs_sim_gl_par_dir)/%.$(CONFIG):
	cd $(vcs_sim_gl_par_dir) && $(MAKE) MODEL=$*

# Name Mapping
$(vcs_sim_gl_syn_dir)/%.match:
	cd $(vcs_sim_gl_syn_dir) && $(MAKE) $(notdir $@) MODEL=$*

$(vcs_sim_gl_par_dir)/%.match:
	cd $(vcs_sim_gl_par_dir) && $(MAKE) $(notdir $@) MODEL=$*

sample = $(if $(SAMPLE),$(abspath $(SAMPLE)),$(gen_dir)/$(1).sample)
prefix = $(notdir $(basename $(call sample,$1)))
logfile = $(if $(LOGFILE),$(abspath $(LOGFILE)),$(call prefix,$1)-$2.log)
waveform = $(if $(WAVEFORM),$(abspath $(WAVEFORM)),$(call prefix,$1).$2)

ifndef SAMPLE
$(gen_dir)/%.sample:
	if [ ! -a $(base_dir)/strober-test/outputs/$*.sample ]; then \
	$(MAKE) -C $(base_dir)/strober-test $*-verilator; \
	fi
	cp $(base_dir)/strober-test/outputs/$*.sample $@
endif

# Replay on Verilator
# $(replay_cpp): %-replay-cpp: $(call sample,%) $(gen_dir)/%.v

# Replay on VCS
$(out_dir)/%-replay: $(gen_dir)/%.v $(simif_cc) $(simif_h)
	$(MAKE) -C $(simif_dir) vcs-replay DESIGN=$* GEN_DIR=$(gen_dir) OUT_DIR=$(out_dir)

$(replay_vcs): %-replay-vcs: $(out_dir)/%-replay $(call sample,%)
	cd $(out_dir) && \
	./$(notdir $<) +sample=$(word 2, $^) +verbose +waveform=$(call waveform,$*,vpd) 2> $(call logfile,$*,vcs)

# Replay on Post-Synthesis Simulation
#$(replay_gl_syn): %-replay-gl-syn: $(call sample,%) $(addprefix $(vcs_sim_gl_syn_dir)/, %.match %.$(CONFIG))
#	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
#	"run replay $* $(vcs_sim_gl_syn_dir) null $< $(word 2, $^) $(word 3, $^) $(N) --noInlineMem"

# Replay on Post-Place-and-Route Simulation
#$(replay_gl_par): %-replay-gl-par: $(call sample,%) $(addprefix $(vcs_sim_gl_par_dir)/, %.match %.$(CONFIG))
#	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
#	"run replay $* $(vcs_sim_gl_par_dir) null $< $(word 2, $^) $(word 3, $^) $(N) --noInlineMem"

# Average Power from Replays
#$(replay_pwr): %-replay-pwr:
#	$(MAKE) -C $(pt_pwr_dir) MODEL=$* pt_prefix=$(if $(pt_prefix),$(pt_prefix),$*)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: $(replay_vcs) claen
