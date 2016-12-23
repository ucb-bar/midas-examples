include Makefrag

PLATFORM ?= zynq
# PLATFORM ?= catapult
DEBUG ?=
LOADMEM ?=
LOGFILE ?=
WAVEFORM ?=
BOARD ?=
SAMPLE ?=
ARGS ?=
DRIVER ?=

debug = $(if $(DEBUG),DEBUG=$(DEBUG),)
loadmem = $(if $(LOADMEM),LOADMEM=$(LOADMEM),)
logfile = $(if $(LOGFILE),LOGFILE=$(LOGFILE),)
waveform = $(if $(WAVEFORM),WAVEFORM=$(WAVEFORM),)
sample = $(if $(SAMPLE),SAMPLE=$(SAMPLE),)
args = $(if $(ARGS),ARGS="$(ARGS)",)

# Desings
tutorial := GCD Parity ShiftRegister ResetShiftRegister EnableShiftRegister Stack Risc
examples := RiscSRAM PointerChaser
mini     := Tile
designs  := $(tutorial) $(examples) $(mini)

publishLocal:
	cd $(base_dir)/firrtl && $(SBT) $(SBT_FLAGS) publishLocal
	cd $(base_dir)/chisel && $(SBT) $(SBT_FLAGS) publishLocal

# Tests
verilator = $(addsuffix -verilator, $(designs))
$(verilator): %-verilator:
	$(MAKE) -C $(base_dir) -f test.mk verilator PLATFORM=$(PLATFORM) DESIGN=$* $(debug)

verilator_test = $(addsuffix -verilator-test, $(designs))
$(verilator_test): %-verilator-test:
	$(MAKE) -C $(base_dir) -f test.mk verilator-test PLATFORM=$(PLATFORM) DESIGN=$* \
	$(debug) $(loadmem) $(logfile) $(waveform) $(sample) $(args)

vcs = $(addsuffix -vcs, $(designs))
$(vcs): %-vcs:
	$(MAKE) -C $(base_dir) -f test.mk vcs PLATFORM=$(PLATFORM) DESIGN=$* $(debug)

vcs_test = $(addsuffix -vcs-test, $(designs))
$(vcs_test): %-vcs-test:
	$(MAKE) -C $(base_dir) -f test.mk vcs-test PLATFORM=$(PLATFORM) DESIGN=$* \
	$(debug) $(loadmem) $(logfile) $(waveform) $(sample) $(args)

vcs_replay_compile = $(addsuffix -vcs-replay-compile, $(designs))
$(vcs_replay_compile): %-vcs-replay-compile:
	$(MAKE) -C $(base_dir) -f replay.mk vcs PLATFORM=$(PLATFORM) DESIGN=$*

vcs_replay = $(addsuffix -vcs-replay, $(designs))
$(vcs_replay): %-vcs-replay:
	$(MAKE) -C $(base_dir) -f replay.mk vcs-replay PLATFORM=$(PLATFORM) DESIGN=$* \
	$(sample) $(logfile) $(waveform)

# FPGA
$(PLATFORM) = $(addsuffix -$(PLATFORM), $(designs))
$($(PLATFORM)): %-$(PLATFORM):
	$(MAKE) -C $(base_dir) -f fpga.mk $(PLATFORM) PLATFORM=$(PLATFORM) DESIGN=$* DRIVER=$(DRIVER) $(if $(BOARD),board=$(BOARD),)

fpga = $(addsuffix -fpga, $(designs))
$(fpga): %-fpga:
	$(MAKE) -C $(base_dir) -f fpga.mk fpga PLATFORM=$(PLATFORM) DESIGN=$* $(if $(BOARD),board=$(BOARD),)

# Clean
design_mostlyclean = $(addsuffix -mostlyclean, $(designs))
$(design_mostlyclean): %-mostlyclean:
	$(MAKE) -C $(base_dir) -f test.mk mostlyclean PLATFORM=$(PLATFORM) DESIGN=$*
	$(MAKE) -C $(base_dir) -f replay.mk mostlyclean PLATFORM=$(PLATFORM) DESIGN=$*

design_clean = $(addsuffix -clean, $(designs))
$(design_clean): %-clean:
	$(MAKE) -C $(base_dir) -f test.mk clean PLATFORM=$(PLATFORM) DESIGN=$*
	$(MAKE) -C $(base_dir) -f replay.mk clean PLATFORM=$(PLATFORM) DESIGN=$*

mostlyclean: $(design_mostlyclean)

clean: $(design_clean)

.PHONY: publishLocal
.PHONY: $(verilator) $(verilator_test) $(vcs) $(vcs_test)
.PHONY: $(zynq) $(fpga)
.PHONY: $(vcs_replay)
.PHONY: $(design_mostlyclean) $(design_clean) mostlyclean clean
