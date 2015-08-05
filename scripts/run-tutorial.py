#!/usr/bin/env python

from subprocess import call
from os.path import isfile

tutorial = ["GCD", "Parity", "Stack", "Router", "Risc", "RiscSRAM",
            "ShiftRegister", "ResetShiftRegister", "EnableShiftRegister", "MemorySearch"]

call(["make", "-C", "vcs-sim-rtl", "clean"])
call(["make", "-C", "vcs-sim-gl-syn", "clean"])
call(["make", "-C", "vcs-sim-gl-par", "clean"])
call(["make", "-C", "pt-pwr", "clean"])
call(["make", "-C", "vlsi/dc-syn", "clean"])
call(["make", "-C", "vlsi/icc-par", "clean"])

for t in tutorial:
  sample = t + ".sample"
  if not (isfile("results/" + sample) or isfile("generated/" + sample)):
    call(["make", t + "Wrapper"])
  call(["rm", "vlsi/dc-syn/current-dc"])
  call(["rm", "vlsi/icc-syn/current-icc"])
  call(["rm", "vlsi/icc-syn/current-iccdp"])
  call(["make", t + "-pwr"])
  call(["make", t + "-test-pwr"])
