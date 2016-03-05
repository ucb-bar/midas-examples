#!/usr/bin/env python

import sys
import os
import os.path
import re
import csv
from subprocess import Popen, call
import time

if __name__ == '__main__':
  design  = str(sys.argv[1])
  sample  = os.path.abspath(str(sys.argv[2]) if (len(sys.argv) > 2) else design + ".sample")
  prefix  = os.path.splitext(os.path.basename(sample))[0]
  job_num = int(sys.argv[2]) if (len(sys.argv) > 2) else 6

  """ read samples """
  sample_size = 0
  with open(sample) as f:
    for line in f:
      if line[0:8] == "0 cycle:":
        sample_size += 1

  """ launch PrimeTime PX """
  for k in range((sample_size+job_num-1)/job_num+1):
    ps = list()
    startTime = time.clock()

    for job in range(job_num):
      num = k*job_num + job
      pt_prefix = prefix + "_" + str(num)
      if os.path.exists("vcs-sim-gl-par/" + pt_prefix + ".saif"):
        ps.append(Popen(["make", design + "-replay-pwr", "pt_prefix=" + pt_prefix], stdout=open(os.devnull, 'wb')))
        time.sleep(1)

    while any(p.poll() == None for p in ps):
      pass

    endTime = time.clock()

    print "[pt-pwr] samples " + str(k*job_num) + " ~ " + str((k+1)*job_num-1) + " are done"
    print "[pt-pwr] time = " + str(endTime - startTime) + " secs"

  """ read power reports """
  modules = list()
  sample_pwr = dict()
  pwr_regex = re.compile(r"""
    \s*([\w_]*(?:\s*\([\w_]*\))?)\s*                 # Module name
    ([-\+]?[0-9]*\.?[0-9]+(?:[eE][-\+]?[0-9]+)?)\s+  # Int Power
    ([-\+]?[0-9]*\.?[0-9]+(?:[eE][-\+]?[0-9]+)?)\s+  # Switch Power
    ([-\+]?[0-9]*\.?[0-9]+(?:[eE][-\+]?[0-9]+)?)\s+  # Leakage Power
    ([-\+]?[0-9]*\.?[0-9]+(?:[eE][-\+]?[0-9]+)?)\s+  # Total Power
    ([0-9]+\.[0-9]+)                                 # Percent
    """, re.VERBOSE)

  for k in range(sample_size):
    path = "pt-pwr/current-pt/reports/" + prefix +"_" + str(k) + ".power.avg.max.report"
    if os.path.exists(path):
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
              if not module in sample_pwr:
                modules.append(module)
                sample_pwr[module] = list()
              sample_pwr[module].append(total_pwr)

  """ dump power """
  with open(prefix + "-pwr.csv", "w") as csvfile:
     writer = csv.writer(csvfile)
     for m in modules:
       writer.writerow([m] + sample_pwr[m])
