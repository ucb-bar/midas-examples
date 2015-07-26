base_dir = $(abspath ..)
vlsi_dir = $(base_dir)/vlsi
sim_dir  = .

CONFIG := VLSI
default: $(MODEL).$(CONFIG)

include $(vlsi_dir)/Makefrag
include $(base_dir)/Makefrag
include $(sim_dir)/Makefrag

clean:
	rm -rf *.$(CONFIG) *.$(CONFIG).csrc *.$(CONFIG).daidir
