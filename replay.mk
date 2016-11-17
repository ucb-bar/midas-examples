##################
# Sample Replays #
##################

DESIGN ?= Tile

include Makefrag
include Makefrag-strober

SAMPLE ?= $(out_dir)/$(DESIGN).sample
LOGFILE ?=
WAVEFORM ?=

sample = $(abspath $(SAMPLE))
prefix = $(notdir $(basename $(SAMPLE)))
logfile = $(if $(LOGFILE),$(abspath $(LOGFILE)),$(out_dir)/$(prefix).$1.replay)
waveform = $(if $(WAVEFORM),$(abspath $(WAVEFORM)),$(out_dir)/$(prefix).$1)

# Replay on VCS
$(gen_dir)/$(DESIGN)-replay: $(gen_dir)/$(DESIGN).v
	$(MAKE) -C $(simif_dir) vcs-replay DESIGN=$(DESIGN) GEN_DIR=$(gen_dir)

vcs: $(gen_dir)/$(DESIGN)-replay

vcs-replay: $(gen_dir)/$(DESIGN)-replay $(sample) $(simif_cc) $(simif_h)
	cd $(gen_dir) && ./$(notdir $<) +sample=$(sample) +verbose \
	+waveform=$(call waveform,vpd) 2> $(call logfile,vcs)

mostlyclean:
	rm -rf $(gen_dir)/$(DESIGN)-replay $(gen_dir)/$(DESIGN)-replay.csrc $(gen_dir)/$(DESIGN)-replay.daidir
	rm -rf $(out_dir)

clean:
	rm -rf $(gen_dir) $(out_dir)

.PHONY: vcs vcs-replay claen
