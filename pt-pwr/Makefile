base_dir = $(abspath ..)
vlsi_dir = $(base_dir)/vlsi
pt_dir   = .
vcs_sim_gl_par_dir = $(base_dir)/vcs-sim-gl-par

CONFIG := VLSI
default: $(MODEL).$(CONFIG)

include $(vlsi_dir)/Makefrag
include $(vlsi_dir)/icc-par/Makefrag
include Makefrag

clean:
	rm -rf build* current*
