#!/usr/bin/env python

import sys
import os.path
import re
import csv
from subprocess import call

modules = list()
sample_pwr = dict()

def read_power_rpt(prefix, first):
  pwr_regex = re.compile(r"""
    \s*([\w_]*)\s+(?:\([\w_]*\))?\s*                 # Module name
    ([-\+]?[0-9]*\.?[0-9]+(?:[eE][-\+]?[0-9]+)?)\s+  # Int Power
    ([-\+]?[0-9]*\.?[0-9]+(?:[eE][-\+]?[0-9]+)?)\s+  # Switch Power
    ([-\+]?[0-9]*\.?[0-9]+(?:[eE][-\+]?[0-9]+)?)\s+  # Leakage Power
    ([-\+]?[0-9]*\.?[0-9]+(?:[eE][-\+]?[0-9]+)?)\s+  # Total Power
    ([0-9]+\.[0-9]+)                                 # Percent
    """, re.VERBOSE)
  path = "pt-pwr/current-pt/reports/" + prefix + ".power.avg.max.report"
  find = False
  for line in open(path):
    if not find:
      tokens = line.split()
      find = len(tokens) > 0 and tokens[0] == "Hierarchy"
    else:
      pwr_matched = pwr_regex.search(line)
      if pwr_matched:
        module     = pwr_matched.group(1)
        int_pwr    = pwr_matched.group(2)
        switch_pwr = pwr_matched.group(3)
        leak_pwr   = pwr_matched.group(4)
        total_pwr  = pwr_matched.group(5)
        percent    = pwr_matched.group(6)
        # print module, int_pwr, switch_pwr, leak_pwr, total_pwr, percent
        if module.find("clk_gate") < 0:
          if first:
            modules.append(module)
            sample_pwr[module] = list()
          sample_pwr[module].append(total_pwr)         

if __name__ == '__main__':
  design     = str(sys.argv[1]) 
  sample_num = int(sys.argv[2])
  # call(["make", "-C", "pt-pwr", "clean"])
  first = True
  for i in range(sample_num):
    path = os.path.abspath("results/" + design + "_" + str(i) + ".sample")
    if os.path.isfile(path):
      call(["make", design + "-pwr", "SAMPLE=" + path])
      read_power_rpt(design, first)
      first = False
  with open("results/" + design + "-pwr.csv", "w") as csvfile:
     writer = csv.writer(csvfile)
     for m in modules:
       writer.writerow([m] + sample_pwr[m])
