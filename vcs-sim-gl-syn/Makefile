base_dir = $(abspath ..)
vlsi_dir = $(base_dir)/vlsi
dc_dir   = $(vlsi_dir)/dc-syn
res_dir  = $(dc_dir)/current-dc/results
rpt_dir  = $(dc_dir)/current-dc/reports
sim_dir  = .

CONFIG := VLSI
default: $(MODEL).$(CONFIG)

include $(vlsi_dir)/Makefrag
include $(base_dir)/Makefrag
include $(sim_dir)/Makefrag

clean:
	rm -rf *.vpd *.$(CONFIG) *.$(CONFIG).csrc *.$(CONFIG).daidir *.match *.key
