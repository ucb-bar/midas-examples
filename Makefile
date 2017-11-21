include Makefrag

PLATFORM ?= zynq
LOADMEM ?=
LOGFILE ?=
WAVEFORM ?=
BOARD ?=
SAMPLE ?=
ARGS ?=
DRIVER ?=
MACRO_LIB ?=

loadmem = $(if $(LOADMEM),LOADMEM=$(LOADMEM),)
logfile = $(if $(LOGFILE),LOGFILE=$(LOGFILE),)
waveform = $(if $(WAVEFORM),WAVEFORM=$(WAVEFORM),)
sample = $(if $(SAMPLE),SAMPLE=$(SAMPLE),)
args = $(if $(ARGS),ARGS="$(ARGS)",)
board = $(if $(BOARD),BOARD=$(BOARD),)

# Desings
designs := GCD Parity ShiftRegister ResetShiftRegister EnableShiftRegister \
	InstrumentationWidgetDemo Stack Risc RiscSRAM PointerChaser Tile

# Tests
verilator = $(addsuffix -verilator, $(designs))
$(verilator): %-verilator:
	$(MAKE) -C $(base_dir) -f test.mk verilator PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)

verilator_test = $(addsuffix -verilator-test, $(designs))
$(verilator_test): %-verilator-test:
	$(MAKE) -C $(base_dir) -f test.mk verilator-test PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)
	$(loadmem) $(logfile) $(waveform) $(sample) $(args)

verilator_debug = $(addsuffix -verilator-debug, $(designs))
$(verilator_debug): %-verilator-debug:
	$(MAKE) -C $(base_dir) -f test.mk verilator-debug PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)

verilator_test_debug = $(addsuffix -verilator-test-debug, $(designs))
$(verilator_test_debug): %-verilator-test-debug:
	$(MAKE) -C $(base_dir) -f test.mk verilator-test-debug PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)
	$(loadmem) $(logfile) $(waveform) $(sample) $(args)

vcs = $(addsuffix -vcs, $(designs))
$(vcs): %-vcs:
	$(MAKE) -C $(base_dir) -f test.mk vcs PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)

vcs_debug = $(addsuffix -vcs-debug, $(designs))
$(vcs_debug): %-vcs-debug:
	$(MAKE) -C $(base_dir) -f test.mk vcs-debug PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)

vcs_test = $(addsuffix -vcs-test, $(designs))
$(vcs_test): %-vcs-test:
	$(MAKE) -C $(base_dir) -f test.mk vcs-test PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)
	$(loadmem) $(logfile) $(waveform) $(sample) $(args)

vcs_test_debug = $(addsuffix -vcs-test-debug, $(designs))
$(vcs_test_debug): %-vcs-test-debug:
	$(MAKE) -C $(base_dir) -f test.mk vcs-test-debug PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)
	$(loadmem) $(logfile) $(waveform) $(sample) $(args)

# FPGA
$(PLATFORM) = $(addsuffix -$(PLATFORM), $(designs))
$($(PLATFORM)): %-$(PLATFORM):
	$(MAKE) -C $(base_dir) -f fpga.mk $(PLATFORM) PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB) DRIVER=$(DRIVER) $(board)

fpga = $(addsuffix -fpga, $(designs))
$(fpga): %-fpga:
	$(MAKE) -C $(base_dir) -f fpga.mk fpga PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB) $(board)

# Replays
vcs_rtl = $(addsuffix -vcs-rtl, $(designs))
$(vcs_rtl): %-vcs-rtl:
	$(MAKE) -C $(base_dir) -f replay.mk vcs-rtl PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)

replay_rtl = $(addsuffix -replay-rtl, $(designs))
$(replay_rtl): %-replay-rtl:
	$(MAKE) -C $(base_dir) -f replay.mk replay-rtl PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB) \
	$(sample) $(logfile) $(waveform)

replay_rtl_pwr = $(addsuffix -replay-rtl-pwr, $(designs))
$(replay_rtl_pwr): %-replay-rtl-pwr:
	$(MAKE) -C $(base_dir) -f replay.mk replay-rtl-pwr PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB) \
	$(sample) $(logfile) $(waveform)

vcs_syn = $(addsuffix -vcs-syn, $(designs))
$(vcs_syn): %-vcs-syn:
	$(MAKE) -C $(base_dir) -f replay.mk vcs-syn PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB)

replay_syn = $(addsuffix -replay-syn, $(designs))
$(replay_syn): %-replay-syn:
	$(MAKE) -C $(base_dir) -f replay.mk replay-syn PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB) \
	$(sample) $(logfile) $(waveform)

vcs_par = $(addsuffix -vcs-par, $(designs))
$(vcs_par): %-vcs-par:
	$(MAKE) -C $(base_dir) -f replay.mk vcs-par PLATFORM=$(PLATFORM) DESIGN=$*

replay_par = $(addsuffix -replay-par, $(designs))
$(replay_par): %-replay-par:
	$(MAKE) -C $(base_dir) -f replay.mk replay-par PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB) \
	$(sample) $(logfile) $(waveform)

syn_pwr = $(addsuffix -syn-pwr, $(designs))
$(syn_pwr): %-syn-pwr:
	$(MAKE) -C $(base_dir) -f replay.mk syn-pwr PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB) \
	$(sample) $(logfile) $(waveform) PRIMETIME_RTL_TRACE=$(PRIMETIME_RTL_TRACE)

par_pwr = $(addsuffix -par-pwr, $(designs))
$(par_pwr): %-par-pwr:
	$(MAKE) -C $(base_dir) -f replay.mk par-pwr PLATFORM=$(PLATFORM) DESIGN=$* MACRO_LIB=$(MACRO_LIB) \
	$(sample) $(logfile) $(waveform) PRIMETIME_RTL_TRACE=$(PRIMETIME_RTL_TRACE)

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

.PHONY: $(verilator) $(verilator_test) $(vcs) $(vcs_test) $($(PLATFORM)) $(fpga)
.PHONY: $(vcs_rtl) $(replay_rtl) $(vcs_syn) $(replay_syn) $(vcs_par) $(replay_par)
.PHONY: $(design_mostlyclean) $(design_clean) mostlyclean clean
