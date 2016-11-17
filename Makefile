include Makefrag

DEBUG ?=
LOADMEM ?=
LOGFILE ?=
WAVEFORM ?=
BOARD ?=
SAMPLE ?=
ARGS ?=

debug = $(if $(DEBUG),DEBUG=$(DEBUG),)
loadmem = $(if $(LOADMEM),LOADMEM=$(LOADMEM),)
logfile = $(if $(LOGFILE),LOGFILE=$(LOGFILE),)
waveform = $(if $(WAVEFORM),WAVEFORM=$(WAVEFORM),)
sample = $(if $(SAMPLE),SAMPLE=$(SAMPLE),)
args = $(if $(ARGS),ARGS=$(ARGS),)

# Desings
tutorial := GCD Parity ShiftRegister ResetShiftRegister EnableShiftRegister Stack Risc
examples := RiscSRAM PointerChaser
mini := Tile
designs := $(tutorial) $(examples) $(mini)

publishLocal:
	cd $(base_dir)/firrtl && $(SBT) $(SBT_FLAGS) publishLocal
	cd $(base_dir)/chisel && $(SBT) $(SBT_FLAGS) publishLocal

# Tests
verilator = $(addsuffix -verilator, $(designs))
$(verilator): %-verilator:
	$(MAKE) -C $(base_dir) -f test.mk verilator DESIGN=$* $(debug)

verilator_test = $(addsuffix -verilator-test, $(designs))
$(verilator_test): %-verilator-test:
	$(MAKE) -C $(base_dir) -f test.mk verilator-test DESIGN=$* \
	$(debug) $(loadmem) $(logfile) $(waveform) $(sample) $(args)

vcs = $(addsuffix -vcs, $(designs))
$(vcs): %-vcs:
	$(MAKE) -C $(base_dir) -f test.mk vcs DESIGN=$* $(debug)

vcs_test = $(addsuffix -vcs-test, $(designs))
$(vcs_test): %-vcs-test:
	$(MAKE) -C $(base_dir) -f test.mk vcs-test DESIGN=$* \
	$(debug) $(loadmem) $(logfile) $(waveform) $(sample) $(args)

vcs_replay_compile = $(addsuffix -vcs-replay-compile, $(designs))
$(vcs_replay_compile): %-vcs-replay-compile:
	$(MAKE) -C $(base_dir) -f replay.mk vcs DESIGN=$*

vcs_replay = $(addsuffix -vcs-replay, $(designs))
$(vcs_replay): %-vcs-replay:
	$(MAKE) -C $(base_dir) -f replay.mk vcs-replay DESIGN=$* $(sample) $(logfile) $(waveform)

# FPGA
zynq = $(addsuffix -zynq, $(designs))
$(zynq): %-zynq:
	$(MAKE) -C $(base_dir) -f fpga.mk zynq DESIGN=$* $(if $(BOARD),board=$(BOARD),)

fpga = $(addsuffix -fpga, $(designs))
$(fpga): %-fpga:
	$(MAKE) -C $(base_dir) -f fpga.mk fpga DESIGN=$* $(if $(BOARD),board=$(BOARD),)

# Clean
design_mostlyclean = $(addsuffix -mostlyclean, $(designs))
$(design_mostlyclean): %-mostlyclean:
	$(MAKE) -C $(base_dir) -f test.mk mostlyclean DESIGN=$*
	$(MAKE) -C $(base_dir) -f replay.mk mostlyclean DESIGN=$*

design_clean = $(addsuffix -clean, $(designs))
$(design_clean): %-clean:
	$(MAKE) -C $(base_dir) -f test.mk clean DESIGN=$*
	$(MAKE) -C $(base_dir) -f replay.mk clean DESIGN=$*

mostlyclean: $(design_mostlyclean)

clean: $(design_clean)

.PHONY: publishLocal
.PHONY: $(verilator) $(verilator_test) $(vcs) $(vcs_test)
.PHONY: $(zynq) $(fpga)
.PHONY: $(vcs_replay)
.PHONY: $(design_mostlyclean) $(design_clean) mostlyclean clean
