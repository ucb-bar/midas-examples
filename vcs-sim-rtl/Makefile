base_dir = $(abspath ..)
vlsi_dir = $(base_dir)/vlsi
sim_dir  = .
generated_dir = $(vlsi_dir)/generated-src

CONFIG = VLSI
default: $(MODEL).$(CONFIG)

include $(base_dir)/Makefrag
include $(sim_dir)/Makefrag

clean:
	rm -rf *.$(CONFIG) *.$(CONFIG).csrc *.$(CONFIG).daidir
