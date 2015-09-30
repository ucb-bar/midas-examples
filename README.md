# Strober Examples
This repository contains [Strober](https://github.com/ucb-bar/strober.git) and its examples including:
+ [Chisel tutorial](https://github.com/ucb-bar/chisel-tutorial.git)
  + GCD, Parity, ShiftRegister, EnableShiftRegister, ResetShiftRegister, Stack, MemorySearch, Risc, Router
  + RiscSRAM: Implementation of Risc with SeqMem
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


## <a name="step1"></a> STEP 1: Testing Simulation Mapping
Strober maps any Chisel deisgns to a general simulation framework, which can be hosted on any hardware platform. 
The simulation wrapper generator(<code>SimWrapper</code>) and the custom transforms in the Chisel backend automatically 
generate additional hardware logics for token-based simulation provinding RTL state snapshotting and I/O recording.

To test simulation mapping, you need to provide a tester using <code>SimWrapperTester</code>. 
However, you can use the same tester transactions used in the original tester to reduce programmers' burden.

In this repository, you can test the simulation mapping of Chisel tutorial examples on Chisel emulator by running:

    $ make <design_name>-sim-cpp

Also, to test it on verilog simulation, run:

    $ make <design_name>-sim-v

where \<design_name\> is the name of the designs listed above.

Similary, you can test a signle test or benchmark of riscv-mini by running:

    $ make <test_name>-sim-cpp
    $ make <test_name>-sim-v
    
The following commands test all isa tests of riscv-mini:

    $ make Tile-sim-isa-cpp
    $ make Tile-sim-isa-v
    
Finally, these commands test all benchmakrs of riscv-mini:

    $ make Tile-sim-bmarks-cpp
    $ make Tile-sim-bmarks-v

This step generates the samples of each test. You can test these samples by jumping directly to [STEP 4](#step4).

## <a name="step2"</a> STEP 2: Testing Platform Mapping
Strober also maps a general simulation framework to a specific platform. For now, we support Xilinx zynq boards where 
a target deisgn runs on FPGA and its testbench runs on the ARM core. To test platform mapping, you should provide 
a test extending <code>SimAXI4WrapperTester</code>, which has AXI bus functional models, but you can reuse the test 
transactions as in [STEP 1](#step1).

You can test platform mapping by replacing <code>sim</code> with <code>axi</code>. For Chisel tutorial:
    
    $ make <design_name>-axi-cpp
    $ make <design_name>-axi-v
    
For riscv-mini:
    
    $ make <test_name>-axi-cpp
    $ make <test_name>-axi-v
    $ make Tile-axi-isa-cpp
    $ make Tile-axi-isa-v
    $ make Tile-axi-bmarks-cpp
    $ make Tile-axi-bmarks-v  

This step also generates samples. You can test these samples by jumping directly to [STEP 4](#step4).

## <a name="step3"></a> STEP 3: Generating and Running FPGA Simulators
Now it's time to run FPGA simulatators. First, Chisel generates verilog for FPGA and a parameter header for the simulation driver by running:

    $ make <design_name>-param

It also generates mapping file used by the simulation driver. Then, you're ready to run the FPGA tool flow:
    
    $ make <design_name>-fpga
    
It takes some time, so be patient. Also, you have to compile the simulation driver:

    $ make <design_name>-zynq
    
For each design, you should provide a testbench like those in <code>testbenches/</code>.

Now in <code>results/</code>, you have the following files

    boot.bin
    <design_name>-zynq
    <design_name>.map
    <design_name>.chain
    
Copy <code>boot.bin</code> to the SD card to initiate your FPGA simlator. Next copy <code>\<design_name\>-zynq</code>, 
<code>\<design_name\>.map</code>, and <code>\<design_name\>.chain</code> to your zynq board. 
On zynq board, just run <code>\<design_name\>-zynq</code>, or for [riscv-mini](https://github.com/donggyukim/riscv-mini.git),
copy hex files in <code>riscv-tests/</code> or <code>riscv-bmarks/</code>, and then run:

    ./Tile-zynq +loadmem=<hex_file>

## <a name="step4"></a> STEP 4: Replay Samples


## Reference Simulations
