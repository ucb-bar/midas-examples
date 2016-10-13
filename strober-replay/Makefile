##################
# Sample Replays #
##################

base_dir   = $(abspath ..)
replay_dir = $(abspath .)
gen_dir    = $(replay_dir)/generated-src
res_dir    = $(replay_dir)/results
vcs_sim_rtl_dir    = $(replay_dir)/vcs-sim-rtl
vcs_sim_gl_syn_dir = $(replay_dir)/vcs-sim-gl-syn
vcs_sim_gl_par_dir = $(replay_dir)/vcs-sim-gl-par
pt_pwr_dir = $(replay_dir)/pt-pwr

DEBUG :=

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

SAMPLE ?=
sample = $(if $(SAMPLE),$(abspath $(SAMPLE)),$(res_dir)/$(1).sample)
waveform = $(notdir $(basename $(if $(SAMPLE), $(SAMPLE), $(1)))).vpd
ifndef SAMPLE
$(base_dir)/strober-test/results/%.sample:
	$(MAKE) -C $(base_dir)/strober-test $*-verilator

$(res_dir)/%.sample: $(base_dir)/strober-test/results/%.sample
	cp $< $@
endif

N ?= 6

# Replay on Verilator
# $(replay_cpp): %-replay-cpp: $(call sample,%) $(gen_dir)/%.v

# Replay on VCS
VCS_FLAGS := $(VCS_FLAGS) -debug_pp +vpi -P $(driver_dir)/vpi.tab -e vcs_main +define+CLOCK_PERIOD=0.5 

$(vcs_binaries): $(res_dir)/%: $(gen_dir)/%.v $(gen_dir)/%.vfrag $(replay_cpp) $(replay_h) $(replay_v)
	rm -rf $(gen_dir)/$*.csrc $(res_dir)/$*.daidir
	$(VCS) $(VCS_FLAGS) -Mdir=$(gen_dir)/$*.csrc +define+VFRAG=\"$(word 2, $^)\" \
	-o $@ $< $(replay_v) $(replay_cpp)

$(replay_vcs): %-replay-vcs: $(res_dir)/% $(call sample,%)
	cd $(res_dir) && \
	./$(notdir $<) +sample=$(word 2, $^) +verbose +waveform=$(call waveform, $*)

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
	rm -rf $(gen_dir) $(res_dir)

.PHONY: $(replay_vcs) claen
