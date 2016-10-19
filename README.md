# Strober Examples
This repository contains [Strober](https://github.com/ucb-bar/strober.git) and its examples including:
+ [Chisel tutorial](https://github.com/ucb-bar/chisel-tutorial.git)
  + GCD, Parity, ShiftRegister, EnableShiftRegister, ResetShiftRegister, Stack, Risc
+ More simple examples:
  + RiscSRAM: Implementation of Risc with SeqMem
  + PointerChaser: Simple pointer chaser following a random list.
+ [riscv-mini](https://github.com/donggyukim/riscv-mini.git)
  + Tile: the top module of riscv-mini
  + ISA: RISCV RV32I
  + 3 stage pipeline with caches
  + Passes all rv32ui tests except FENCE.I
  + Passes all rv32mi tests except timer
  + Runs riscv benchmarks: median, multiply, qsort, towers, vvadd

## <a name="step0"></a> STEP 0: Getting Started
To initiate the project, run the following commands:

    $ git clone https://github.com/donggyukim/strober-examples.git
    $ cd strober-example
    $ git submodule update --init --recursive
    # pulish chisel & firrtl to local maven
    $ make publishLocal
    
Note that the <b>vlsi</b> repository is not available outside Berkeley due to the license contract. 

In addtion, you need to download the isa tests and benchmarks for riscv-mini by the following commands:

    $ cd riscv-mini
    $ git submodule update --init --recursive

You can also enable/disable snapshotting by changing a Chiesl parameter(`EnableSnapshot`).

## <a name="step1"></a> STEP 1: Test
First of all, you need to write a C++ testbench. Examples are given in `src/main/cc`.
In order to reuse test benches for [FPGA simulation](step2), write a testbench in
a header file with virtual base class `simif_t`(`src/main/cc/<design>.h`).
Also, add your design in `Makefrag-strober` in the main directory.
For testing, just write a wrapper properly with `simif_emul_t`(`src/main/cc/<design>-emul.cc`).

Next, move to `strober-test`. To run tests with verilator, run:

     $ make <design>-verilator [LOADMEM=<hexfile>] [DEBUG=1] [ARGS="<testbench specific arguments>"]

The Chisel generated files are in `generated-src`, while simulator binaries, log files, waveforms are in `results`.
Waveforms are not dumped by default, but you can get waveforms by running with `DEBUG=1`.

To run tests with vcs, run:

     $ make <design>-vcs [LOADMEM=<hexfile>] [DEBUG=1] [ARGS="<testbench specific arguments>"]
     
For integration tests, run `sbt test` in the main directory.

## <a name="step2"></a> STEP 2: Run FPGA Simulation
You should want to resuse the testbench written for [tests](step1), even for FPGA simulation.
Thus, write a proper wrapper with `simif_zynq_t`(`src/main/cc/<design>-zynq.cc`).

Next, move to `strober-fpga`. To generate verilog, run:

    $ make <design>-strober
    
To synthesize the design for FPGA, run:

    $ make <design>-fpga
    
Finially, to compile the driver with the testbench, run:

    $ make <design>-zynq
    
You need to copy `results/boot.bin` to the SD card to program FPGA.
Also, you should copy the following files in `results` to the board:

    * <design>-zynq: driver executable
    * <design>.chain: scan chain & I/O trace information(optional)
    
To execute simulation on FPGA, run:

    $ ./<design>-zynq [+loadmem=<hexfile>]

## <a name="step2"></a> TODO: Replay Samples
