$(OBJ_CORE_MACROS): \
		src/addons/core-generator/rocketchip/tools/generate-macros \
		$(OBJ_CORE_MEMORY_CONF)
	@mkdir -p $(dir $@)
	$< --json $@ --conf $(word 2, $^)

$(OBJ_CORE_SIM_MACRO_FILES): $(CMD_FIRRTL_MACRO_COMPILER) $(OBJ_CORE_MACROS)
	$< -m $(abspath $(filter %.macros.json,$^)) -v $(abspath $@)
