$(OBJ_CORE_MACROS): \
		src/addons/core-generator/rocketchip/tools/generate-macros \
		$(OBJ_CORE_MEMORY_CONF) \
		$(CMD_PSON2JSON)
	@mkdir -p $(dir $@)
	$< -o $@ $^

$(OBJ_CORE_SIM_MACRO_FILES): $(CMD_PCAD_MACRO_COMPILER) $(OBJ_CORE_MACROS)
	$< -m $(filter %.macros.json,$^) -v $@
