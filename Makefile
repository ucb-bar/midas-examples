base_dir := $(abspath .)
include $(base_dir)/Makefrag-strober

wrappers := sim nasti

$(addsuffix -tut-cpp, $(wrappers)): %-tut-cpp:
	$(SBT) $(SBT_FLAGS) "testOnly StroberExamples.TutorialSuite -- -Db=c -Dw=$*"

$(addsuffix -tut-v, $(wrappers)): %-tut-v:
	$(SBT) $(SBT_FLAGS) "testOnly StroberExamples.TutorialSuite -- -Db=v -Dw=$*"

$(addsuffix -asm-cpp, $(wrappers)): %-asm-cpp:
	$(SBT) $(SBT_FLAGS) ";\
	testOnly StroberExamples.MiniISATests -- -Db=c -Dw=$*;\
	testOnly StroberExamples.ReplayISATests -- -Db=c;\
	testOnly StroberExamples.ReplayISATests -- -Db=v"

$(addsuffix -asm-v, $(wrappers)): %-asm-v:
	$(SBT) $(SBT_FLAGS) ";\
	testOnly StroberExamples.MiniISATests -- -Db=v -Dw=$*;\
	testOnly StroberExamples.ReplayISATests -- -Db=c;\
	testOnly StroberExamples.ReplayISATests -- -Db=v"

$(addsuffix -bmark-cpp, $(wrappers)): %-bmark-cpp:
	$(SBT) $(SBT_FLAGS) ";\
	testOnly StroberExamples.MiniBmarkTests -- -Db=c -Dw=$*;\
	testOnly StroberExamples.ReplayBmarkTests -- -Db=c;\
	testOnly StroberExamples.ReplayBmarkTests -- -Db=v"

$(addsuffix -bmark-v, $(wrappers)): %-bmark-v:
	$(SBT) $(SBT_FLAGS) ";\
	testOnly StroberExamples.MiniBmarkTests -- -Db=v -Dw=$*;\
	testOnly StroberExamples.ReplayBmarkTests -- -Db=c;\
	testOnly StroberExamples.ReplayBmarkTests -- -Db=v"

clean:
	rm -rf test-*

.PHONY: clean
