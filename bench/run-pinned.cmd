@echo off
rem Runs JMH pinned to two efficiency cores (E-cores), so Windows can't move the benchmark around
rem and other programs (browser, apps) are less likely to run on the same cores.
rem Mask 60 = logical CPUs 5 and 6 on an Intel Core Ultra 5 125U. See EXPERIMENTS.md (2026-11-04, step 2)
rem for why E-cores and not P-cores. On another machine, check the core layout first and change the mask.
rem Usage (from the project folder): bench\run-pinned.cmd BinarySearchBench -rf csv -rff bench\out.csv
start "" /affinity 60 /wait /b java -jar target\benchmarks.jar %*
