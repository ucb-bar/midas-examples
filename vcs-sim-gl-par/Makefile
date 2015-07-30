base_dir = $(abspath ..)
vlsi_dir = $(base_dir)/vlsi
icc_dir  = $(vlsi_dir)/icc-par
res_dir  = $(icc_dir)/current-icc/results
rpt_dir  = $(icc_dir)/current-icc/reports
sim_dir  = .

CONFIG := VLSI
default: $(MODEL).$(CONFIG)

include $(vlsi_dir)/Makefrag
include $(base_dir)/Makefrag
include $(sim_dir)/Makefrag

clean:
	rm -rf *.$(CONFIG) *.$(CONFIG).csrc *.$(CONFIG).daidir *.match *.key sdfAnnotateInfo
