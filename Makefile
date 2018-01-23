include Makefrag

export PLATFORM ?= zynq
export LOADMEM ?=
export LOGFILE ?=
export WAVEFORM ?=
export BOARD ?=
export SAMPLE ?=
export ARGS ?=
export DRIVER ?=
export MACRO_LIB ?=

# Desings
designs := GCD Parity ShiftRegister ResetShiftRegister EnableShiftRegister \
	Stack Risc RiscSRAM PointerChaser Tile

# Tests
verilator = $(addsuffix -verilator, $(designs))
$(verilator): %-verilator:
	$(MAKE) -C $(base_dir) -f test.mk verilator DESIGN=$*

verilator_test = $(addsuffix -verilator-test, $(designs))
$(verilator_test): %-verilator-test:
	$(MAKE) -C $(base_dir) -f test.mk verilator-test DESIGN=$*

verilator_debug = $(addsuffix -verilator-debug, $(designs))
$(verilator_debug): %-verilator-debug:
	$(MAKE) -C $(base_dir) -f test.mk verilator-debug DESIGN=$*

verilator_test_debug = $(addsuffix -verilator-test-debug, $(designs))
$(verilator_test_debug): %-verilator-test-debug:
	$(MAKE) -C $(base_dir) -f test.mk verilator-test-debug DESIGN=$*

vcs = $(addsuffix -vcs, $(designs))
$(vcs): %-vcs:
	$(MAKE) -C $(base_dir) -f test.mk vcs DESIGN=$*

vcs_debug = $(addsuffix -vcs-debug, $(designs))
$(vcs_debug): %-vcs-debug:
	$(MAKE) -C $(base_dir) -f test.mk vcs-debug DESIGN=$*

vcs_test = $(addsuffix -vcs-test, $(designs))
$(vcs_test): %-vcs-test:
	$(MAKE) -C $(base_dir) -f test.mk vcs-test DESIGN=$*

vcs_test_debug = $(addsuffix -vcs-test-debug, $(designs))
$(vcs_test_debug): %-vcs-test-debug:
	$(MAKE) -C $(base_dir) -f test.mk vcs-test-debug DESIGN=$*

# FPGA
$(PLATFORM) = $(addsuffix -$(PLATFORM), $(designs))
driver = $($(PLATFORM))
$(driver): %-$(PLATFORM):
	$(MAKE) -C $(base_dir) -f fpga.mk $(PLATFORM) DESIGN=$*

fpga = $(addsuffix -fpga, $(designs))
$(fpga): %-fpga:
	$(MAKE) -C $(base_dir) -f fpga.mk fpga DESIGN=$*

# Replays
vcs_rtl = $(addsuffix -vcs-rtl, $(designs))
$(vcs_rtl): %-vcs-rtl:
	$(MAKE) -C $(base_dir) -f replay.mk vcs-rtl DESIGN=$*

replay_rtl = $(addsuffix -replay-rtl, $(designs))
$(replay_rtl): %-replay-rtl:
	$(MAKE) -C $(base_dir) -f replay.mk replay-rtl DESIGN=$*

replay_rtl_pwr = $(addsuffix -replay-rtl-pwr, $(designs))
$(replay_rtl_pwr): %-replay-rtl-pwr:
	$(MAKE) -C $(base_dir) -f replay.mk replay-rtl-pwr DESIGN=$*

vcs_syn = $(addsuffix -vcs-syn, $(designs))
$(vcs_syn): %-vcs-syn:
	$(MAKE) -C $(base_dir) -f replay.mk vcs-syn DESIGN=$*

replay_syn = $(addsuffix -replay-syn, $(designs))
$(replay_syn): %-replay-syn:
	$(MAKE) -C $(base_dir) -f replay.mk replay-syn DESIGN=$*

vcs_par = $(addsuffix -vcs-par, $(designs))
$(vcs_par): %-vcs-par:
	$(MAKE) -C $(base_dir) -f replay.mk vcs-par DESIGN=$*

replay_par = $(addsuffix -replay-par, $(designs))
$(replay_par): %-replay-par:
	$(MAKE) -C $(base_dir) -f replay.mk replay-par DESIGN=$*

syn_pwr = $(addsuffix -syn-pwr, $(designs))
$(syn_pwr): %-syn-pwr:
	$(MAKE) -C $(base_dir) -f replay.mk syn-pwr DESIGN=$* PRIMETIME_RTL_TRACE=$(PRIMETIME_RTL_TRACE)

par_pwr = $(addsuffix -par-pwr, $(designs))
$(par_pwr): %-par-pwr:
	$(MAKE) -C $(base_dir) -f replay.mk par-pwr DESIGN=$* PRIMETIME_RTL_TRACE=$(PRIMETIME_RTL_TRACE)

# Clean
design_mostlyclean = $(addsuffix -mostlyclean, $(designs))
$(design_mostlyclean): %-mostlyclean:
	$(MAKE) -C $(base_dir) -f test.mk mostlyclean
	$(MAKE) -C $(base_dir) -f replay.mk mostlyclean

design_clean = $(addsuffix -clean, $(designs))
$(design_clean): %-clean:
	$(MAKE) -C $(base_dir) -f test.mk clean
	$(MAKE) -C $(base_dir) -f replay.mk clean

mostlyclean: $(design_mostlyclean)

clean: $(design_clean)

.PHONY: $(verilator) $(verilator_test) $(vcs) $(vcs_test) $(driver) $(fpga)
.PHONY: $(vcs_rtl) $(replay_rtl) $(vcs_syn) $(replay_syn) $(vcs_par) $(replay_par)
.PHONY: $(design_mostlyclean) $(design_clean) mostlyclean clean
