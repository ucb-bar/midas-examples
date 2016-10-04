# Strober Examples
This repository contains [Strober](https://github.com/ucb-bar/strober.git) and its examples including:
+ [Chisel tutorial](https://github.com/ucb-bar/chisel-tutorial.git)
  + GCD, Parity, ShiftRegister, EnableShiftRegister, ResetShiftRegister, Stack, Risc, Router
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
    $ git submodule init
    $ git submodule update
    
Note that the <b>vlsi</b> repository is not available outside Berkeley due to the license contract. 

In addtion, you need to download the isa tests and benchmarks for riscv-mini by the following commands:

    $ cd riscv-mini
    $ git submodule update --init --recursive

You can also enable/disable snapshotting by changing a Chiesl parameter(`EnableSnapshot`).

## <a name="step1"></a> STEP 1: Test
First, launch `sbt` in the main directory. If you run `test`, it will run all tests.
Or, just run `testOnly StroberExample.<test_name>` to run a specific test only.

## <a name="step2"></a> STEP 2: Run FPGA Simulation
You need to write a C++ testbench to run FPGA Simulation(`src/main/cc`). Next, move to `strober-fpga`:

    $ cd strober-fpga

To generate verilog, run:

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
