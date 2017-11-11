# MIDAS Examples
This repository demonstrates an example use of [MIDAS](https://github.com/ucb-bar/midas-release) for simple RTL designs including:
+ [Chisel tutorial](https://github.com/ucb-bar/chisel-tutorial)
  + GCD, Parity, ShiftRegister, EnableShiftRegister, ResetShiftRegister, Stack, Risc
+ More simple examples:
  + RiscSRAM: Implementation of Risc with SeqMem
  + PointerChaser: Simple pointer chaser following a random list.
+ [riscv-mini](https://github.com/ucb-bar/riscv-mini)
  + Tile: the top module of riscv-mini
  + ISA: RISCV RV32I
  + 3 stage pipeline with caches
  + Passes all rv32ui tests except FENCE.I
  + Passes all rv32mi tests except timer
  + Runs RISC-V benchmarks: median, multiply, qsort, towers, vvadd

## <a name="step0"></a> STEP 0: Get Started
To initiate the project, run the following commands:

    $ git clone https://github.com/ucb-bar/midas-examples
    $ cd strober-example
    # initialize the submodules
    $ ./setup
    # publish chisel & firrtl to local maven
    $ make publishLocal

With the following `make` commands, you can enable RTL state snapshotting with `STROBER=1`. Also, `MACRO_LIB=1` is required for power modeling to transform technology-independent macro blocks to technology-dependent macro blocks.

## <a name="step1"></a> STEP 1: Run Verilator/VCS Tests
First of all, you need to write simulation drivers. Examples are given in [src/main/cc](src/main/cc). To increase code reuse for tests and [FPGA simulation](step2), write a header file with virtual base class `simif_t`(`src/main/cc/<design>.h`). Also, add your design in [Makefile](Makefile) in the main directory. For Verilator/VCS tests, just write a wrapper and the main function with `simif_emul_t`(`src/main/cc/<design>-emul.cc`). 

For Verilator tests, run:

     $ make <design>-verilator-test [STROBER=1] [MACRO_LIB=1] [LOADMEM=<hexfile>] [ARGS="<simulation specific arguments>"]

Chisel generated files and test binaries are in `generated-src`, while log files, waveform files, sample snapshot files are in `output`. VCD files are not dumped by default, but you can get VCD files with `<design>-verilator-tests-debug` as the `make` target.

For VCS tests, run:

     $ make <design>-vcs-test [STROBER=1] [MACRO_LIB=1] [LOADMEM=<hexfile>] [ARGS="<testbench specific arguments>"]

You can get VPD files with `<design>-vcs-tests-debug` as the `make` target.

We can also take advantage of `sbt` for tests. Test wrappers are written in [src/test/scala](src/test/scala). For individual tests, run `sbt testOnly <test name>`. For integration tests, run `sbt test`.

## <a name="step2"></a> STEP 2: Run FPGA Simulation
First, write a wrapper and the main function with `simif_zynq_t`(`src/main/cc/<design>-zynq.cc`) by reusing the header file in [Verilator/VCS tests](step1).

To generate `boot.bin` for FPGA Simulation, run:

    $ make <design>-fpga [STROBER=1] [BOARD=<zybo|zedboard|zc706>]

The default FPGA board is `zedboard`, but you can also select `zybo` or `zc706`.

To synthesize the design for FPGA, run:

    $ make <design>-fpga
    
Finally, to compile the compile FPGA simulation driver, run:

    $ make <design>-zynq
    
Copy `/output/zynq/<design>/boot.bin` to the SD card to program FPGA. Also, you should copy the following files in `output/zynq/<design>` to the board (refer to [ucb-bar/fpga-zynq](https://github.com/ucb-bar/fpga-zynq#b--getting-files-on--off-the-board)):

    * <design>-zynq: driver executable
    * <design>.chain: scan chain information (only for Strober)
    
To execute simulation in the FPGA board, run:

    $ ./<design>-zynq [+loadmem=<hexfile>] [+sample=<sample file>]

## <a name="step3"></a> STEP 3: Replay RTL Sample Snapshots (Optional for Strober)
If you enable state snapshotting in [STEP 1](step1) or [STEP 2](step2), you will get random RTL state snapshots at the end of simulation (`<design>.sample` by default). To replay RTL sample snapshots, run:

    $ make <design>-replay-rtl SAMPLE=<sample file> [MACRO_LIB=1]
    
`MACRO_LIB=1` is not necessary if it's not enabled in the previous steps.

For power modeling, `MACRO_LIB=1` is required in this step as well as the previous steps. The following commands interact with [HAMMER](https://github.com/ucb-bar/hammer.git) to run proper CAD tools. For power estimation with RTL simulation, run:

    $ make <design>-replay-rtl-pwr SAMPLE=<sample file> MACRO_LIB=1
    
For power estimation with post-synthesis simulation, run:

    $ make <design>-replay-syn SAMPLE=<sample file> MACRO_LIB=1
