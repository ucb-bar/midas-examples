#########################
# STEP4: Sample Replays #
#########################

base_dir   = $(abspath ..)
replay_dir = $(abspath .)
gen_dir    = $(replay_dir)/generated-outs
vcs_sim_rtl_dir    = $(replay_dir)/vcs-sim-rtl
vcs_sim_gl_syn_dir = $(replay_dir)/vcs-sim-gl-syn
vcs_sim_gl_par_dir = $(replay_dir)/vcs-sim-gl-par
pt_pwr_dir = $(replay_dir)/pt-pwr

include $(base_dir)/Makefrag-strober

CONFIG := VLSI

replay_cpp    := $(addsuffix -replay-cpp,    $(tut) $(mini))
replay_v      := $(addsuffix -replay-v,      $(tut) $(mini))
replay_rtl    := $(addsuffix -replay-rtl,    $(tut) $(mini))
replay_gl_syn := $(addsuffix -replay-gl-syn, $(tut) $(mini))
replay_gl_par := $(addsuffix -replay-gl-par, $(tut) $(mini))
replay_pwr    := $(addsuffix -replay-pwr,    $(tut) $(mini))

vcs_sim_rtl    := $(addprefix $(vcs_sim_rtl_dir)/,    $(addsuffix .$(CONFIG), $(tut) $(mini)))
vcs_sim_gl_syn := $(addprefix $(vcs_sim_gl_syn_dir)/, $(addsuffix .$(CONFIG), $(tut) $(mini)))
vcs_sim_gl_par := $(addprefix $(vcs_sim_gl_par_dir)/, $(addsuffix .$(CONFIG), $(tut) $(mini)))

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
sample = $(if $(SAMPLE), $(SAMPLE), $(replay_dir)/$(1).sample)
ifndef SAMPLE
$(replay_dir)/%.sample: $(base_dir)/test-outs/%.sample
	cp $< $@
endif

N ?= 6
# Replay on Chisel Emulator
$(replay_cpp): %-replay-cpp: $(call sample,%)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) "run replay $* $(gen_dir) c $< none none $(N)"

# Replay on Verilog Simulation
$(replay_v): %-replay-v: $(call sample,%)
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) "run replay $* $(gen_dir) v $< none none $(N)"

# Replay on VCS RTL Simulation 
$(replay_rtl): %-replay-rtl: $(call sample,%) $(vcs_sim_rtl_dir)/%.$(CONFIG) 
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run replay $* $(vcs_sim_rtl_dir) null $< none $(word 2, $^) $(N) --noInlineMem"

# Replay on Post-Synthesis Simulation
$(replay_gl_syn): %-replay-gl-syn: $(call sample,%) $(addprefix $(vcs_sim_gl_syn_dir)/, %.match %.$(CONFIG))
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run replay $* $(vcs_sim_gl_syn_dir) null $< $(word 2, $^) $(word 3, $^) $(N) --noInlineMem"

# Replay on Post-Place-and-Route Simulation
$(replay_gl_par): %-replay-gl-par: $(call sample,%) $(addprefix $(vcs_sim_gl_par_dir)/, %.match %.$(CONFIG))
	cd $(base_dir) && $(SBT) $(SBT_FLAGS) \
	"run replay $* $(vcs_sim_gl_par_dir) null $< $(word 2, $^) $(word 3, $^) $(N) --noInlineMem"

# Average Power from Replays
$(replay_pwr): %-replay-pwr:
	$(MAKE) -C $(pt_pwr_dir) MODEL=$* pt_prefix=$(if $(pt_prefix),$(pt_prefix),$*)
