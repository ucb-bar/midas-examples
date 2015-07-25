base_dir = $(abspath ..)
vlsi_dir = $(base_dir)/vlsi
icc_dir  = $(vlsi_dir)/icc-par
res_dir  = $(icc_dir)/current-icc/results
sim_dir  = .

CONFIG = VLSI
default: $(MODEL).$(CONFIG)

include $(base_dir)/Makefrag
include $(sim_dir)/Makefrag

clean:
	rm -rf *.$(CONFIG) *.$(CONFIG).csrc *.$(CONFIG).daidir
