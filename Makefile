base_dir = $(abspath .)

include Makefrag-strober

publishLocal:
	cd $(base_dir)/firrtl && $(SBT) $(SBT_FLAGS) publishLocal
	cd $(base_dir)/chisel && $(SBT) $(SBT_FLAGS) publishLocal

.PHONY: publishLocal
