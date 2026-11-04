# Lab notebook

One entry per experiment, newest at the bottom. Failed attempts stay in.

## 2026-11-04: Baseline, binary search vs. array size

**Question.** Binary search is O(log n). Going from 1,000 to 10 million keys means about 10 → 23 steps, so the textbook says it should take about 2.3× longer. Is that what really happens?

**Hypothesis.** O(log n) only holds while the array fits in the CPU cache. Once it doesn't, each step has to fetch data from RAM, which is much slower, so the time should grow much faster than log2(n).

**Setup.**
- Laptop: Intel Core Ultra 5 125U (fast and slow cores, 12 MB L3 cache), 15.5 GB RAM, Windows 11, plugged in
- JDK 27 (project compiled for Java 25), JMH 1.37: 3 × 1 s warm-up, 5 × 1 s measurement, 2 forks
- Keys: sorted `long`s with random gaps of 1–100
- Queries: 2^20 keys that exist, in random order, so one search can't profit from the cache of the last one
- Compared my `BinarySearch.find` with the JDK's `Arrays.binarySearch` (same algorithm)
- Not controlled: which core runs the benchmark, other programs, CPU temperature

**Result.** Raw data: `bench/binary-search.csv`, plot: `bench/binary-search.png`.

| keys | array size | JDK | mine |
|---|---|---|---|
| 1,000 | 8 KB | 52 ± 1 ns | 52 ± 1 ns |
| 64,000 | 512 KB | 92 ± 2 ns | 139 ± 34 ns |
| 1,000,000 | 8 MB | 191 ± 13 ns | 374 ± 112 ns |
| 10,000,000 | 80 MB | 653 ± 23 ns | 909 ± 119 ns |

- Up to about 64,000 keys the JDK curve follows log2(n) quite well.
- Between 1 million (8 MB) and 4 million keys (32 MB) the time jumps by about 2.5×. That is where the array stops fitting into the 12 MB L3 cache.
- From 1,000 to 10 million keys: 2.3× more steps, but **12.6× more time** (JDK).

**What I learned.**
- The hypothesis holds. O(log n) counts steps, but a step that has to go to RAM costs many times more than one that hits the cache. For big arrays, *where* the data is matters more than how many steps there are.
- My measurement is not reliable yet. "mine" and "jdk" are the same algorithm and were equal at 1,000 keys, but at 1 million keys "mine" is almost 2× slower and has error bars of ±30%. That can't come from the code.
- JMH ran "jdk" first and "mine" second. A possible reason: the laptop gets warmer during the run and clocks down, or Windows moves the benchmark from a fast core to a slow one. Before I compare the B-tree and the learned index, the measurement has to be fixed.

**Next step.** Pin the benchmark to one fast core, run "mine" and "jdk" in the opposite order, and check if the difference and the big error bars go away.

## 2026-11-04: Step 2, making the measurement reliable

**Question.** In the baseline, "mine" and "jdk" (same algorithm) differed by up to 2×. That can't come from the code. Where does the noise come from, and how do I get rid of it?

**Hypothesis.** My CPU has 2 fast cores (P-cores) and 10 slower ones (E-cores). Windows moves the benchmark between them. If I pin the benchmark to one P-core, the noise should go away.

**Setup.**
- Same benchmark and settings as the baseline
- Read the core layout with the Windows function `GetLogicalProcessorInformationEx`: P-cores are logical CPUs 0–1 and 10–11, the rest are E-cores
- Pinned with `start /affinity` (checked that the JMH child processes inherit the pinning)
- Three runs: not pinned (baseline), pinned to one P-core (CPUs 10–11), pinned to two E-cores (CPUs 5–6)
- In the background: the Claude desktop app and the Opera browser, together about half a core of CPU time

**Result.** Raw data: `bench/binary-search*.csv`, plots: `bench/binary-search*.png`.

| run | typical error bar (median) | biggest error bar | average gap mine vs. jdk | biggest gap |
|---|---|---|---|---|
| not pinned | 6% | 30% | 39% | 96% |
| one P-core | 29% | 185% | 44% | 168% |
| two E-cores | **5%** | 26% | **3%** | **8%** |

- **The hypothesis was wrong.** Pinning to a fast core made the measurement much *worse*.
- Pinning to two slow cores fixed it: "mine" and "jdk" now agree at every size within a few percent, which is what you expect from the same algorithm.
- On the E-cores the absolute times are a bit higher at large sizes (e.g. 816 ns instead of 653 ns at 10 million keys). The shape of the curve stays the same.

**What I learned.**
- For a benchmark, a quiet core matters more than a fast core. My best explanation: Windows prefers to run the apps I'm using (browser, desktop app) on the fast cores. When I pin the benchmark there, it has to share the core with them. When it's not pinned, it at least can move away. On the E-cores it mostly has the cores to itself. I didn't prove this directly (I didn't record which program ran on which core), so it stays an explanation for now.
- Measurements need a check like "mine vs. jdk": two versions that *must* be equal. Without it I wouldn't have noticed how bad the first measurement was.

**Next step.** All further benchmarks run with `bench\run-pinned.cmd` (pinned to the two E-cores). Then: build the B-tree.

## 2026-11-04: Step 3, B-tree vs. binary search

**Question.** Binary search jumps around the whole array, and for big arrays almost every jump is a cache miss. A B-tree keeps keys that are searched together next to each other. Is it faster, and which node size is best?

**Expectation from theory.** A cache line is 64 bytes = 8 `long` keys. So a node of 8 keys can be loaded with one memory access. Bigger nodes mean fewer levels, but more data per node. The textbook answer would be: 8 keys per node is best.

**Setup.**
- `StaticBTree`: read-only B+ tree built from the sorted array. No pointers: the i-th key of a level stands for the i-th block of the level below. Inside a node, it counts the keys ≤ the query instead of stopping early, so there are no hard-to-predict branches.
- Tested against `BinarySearch` with node sizes 2, 3, 8, 16, 64 (27 tests, all pass)
- Same keys, queries and JMH settings as before, pinned to two E-cores (`bench\run-pinned.cmd`)
- Node sizes 8, 16, 32, 64; array sizes 1,000 to 10 million

**Result.** Raw data: `bench/btree.csv`, plot: `bench/btree.png`.

| keys | binary search | B-tree 8 | B-tree 16 | B-tree 32 | B-tree 64 |
|---|---|---|---|---|---|
| 1,000 | 54 ns | **38 ns** | 40 ns | 40 ns | 50 ns |
| 64,000 | 107 ns | **65 ns** | 70 ns | 78 ns | 93 ns |
| 1,000,000 | 256 ns | 231 ns | **180 ns** | 184 ns | 216 ns |
| 10,000,000 | 804 ns | 619 ns | 455 ns | **435 ns** | 524 ns |

Levels at 10 million keys: 8 (node size 8), 6 (16), 5 (32), 4 (64).

- The B-tree beats binary search at every size and every node size.
- At 10 million keys the best B-tree is **1.8× faster** than binary search (435 vs. 804 ns).
- **The textbook answer is only right for small arrays.** Up to 64,000 keys, 8 keys per node is best. For big arrays, 16 and 32 are clearly better than 8.

**What I learned.**
- For small arrays everything is in the cache anyway. Then the work *inside* a node matters, and small nodes win.
- For big arrays the number of RAM accesses matters most. Node size 16 or 32 needs fewer levels (6 or 5 instead of 8). Each extra level is another trip to RAM, which costs more than scanning a few more keys.
- Why is a node of 16 keys (two cache lines) not twice as expensive as one of 8? Two possible reasons, both not tested yet: (1) Intel CPUs often fetch cache lines in pairs (128 bytes), so the second line comes almost for free. (2) Java puts a 16-byte header in front of each array, so my 8-key nodes are usually not aligned to cache lines and already touch two lines.
- At 64 keys per node, scanning 512 bytes per node gets more expensive than the levels it saves.
- Extra memory for the B-tree: about 1/(node size − 1) of the array, e.g. ~5 MB for 10 million keys with node size 16.

**Next step.** Build the learned index and compare it with the B-tree with node size 16 or 32.

## 2026-11-04: Step 4, learned index

**Question.** Instead of searching, can a straight line *predict* where a key is? And how many lines do I need to beat the B-tree?

**Idea.** In a sorted array, position grows with the key. If the keys are spread evenly, a line `position ≈ slope × key + intercept` is a good guess. The index remembers how far the line is off at worst, then only binary-searches that small window. With one line the window can be big. With many lines (one per segment), each line only has to fit a small part of the data, so the windows should get much smaller.

**Setup.**
- `LearnedIndex`: least-squares lines. With 1 segment: one line for everything. With more segments: a root line picks the segment, then the segment's own line predicts the position (two-stage model as in Kraska et al.).
- Every key in the array is always found, because the window is computed from the same predictions as the lookup. A bad line only makes the window bigger, never wrong.
- First version cut predictions off (4999.9999 became 4999). Switched to rounding, otherwise perfectly evenly spaced keys still got a window of 2 instead of 1.
- 23 new tests, 59 in total, all pass. Same keys, queries and JMH settings as before, pinned to two E-cores.
- Compared: binary search, B-tree (node size 16), learned index with 1, 1,024 and 65,536 segments
- Window sizes and memory from `LearnedIndexStats`: `bench/learned-index-stats.txt`

**Result.** Raw data: `bench/learned-index.csv`, plot: `bench/learned-index.png`.

| keys | binary search | B-tree 16 | learned, 1 line | learned, 1,024 | learned, 65,536 |
|---|---|---|---|---|---|
| 1,000 | 54 ns | 40 ns | 34 ns | **16 ns** | 20 ns |
| 64,000 | 107 ns | 70 ns | 63 ns | 47 ns | **26 ns** |
| 1,000,000 | 257 ns | 189 ns | 195 ns | **98 ns** | 111 ns |
| 10,000,000 | 844 ns | 466 ns | 728 ns | 221 ns | **183 ns** |

Average search window (keys) and model size at 10 million keys:

| | 1 line | 1,024 lines | 65,536 lines | B-tree 16 |
|---|---|---|---|---|
| avg window | 1,937 | 67 | 9 | – |
| extra memory | ~0 KB | 24 KB | 1.5 MB | 5.1 MB |

- With enough segments the learned index wins clearly. At 10 million keys: **183 ns, 2.5× faster than the B-tree and 4.6× faster than binary search**, with less than a third of the B-tree's extra memory.
- One line alone is not enough for big arrays (728 ns, almost as slow as binary search).
- More segments is not always better: at 1 million keys, 1,024 segments (98 ns) beat 65,536 (111 ns).

**What I learned.**
- One line at 10 million keys still leaves a window of ~1,900 keys (15 KB). Searching it takes ~11 steps, and those are exactly the expensive steps: in a normal binary search the first steps are cached anyway, the *last* steps go to RAM. One line only saves the cheap steps.
- The model itself has to fit in the cache. 1,024 segments are 24 KB, always in cache. 65,536 segments are 1.5 MB, bigger than the L2 cache, so looking up the segment's line can itself be a cache miss. That's why the bigger model only pays off when the array is so big that the smaller windows save more RAM trips than the model costs.
- These keys are friendly for a learned index: random gaps of 1–100 are almost a straight line. The real test is data that is *not* like a line.

**Next step.** Step 5 is mostly done by this run (time and memory on evenly spread keys). So next: step 6, data that breaks the learned index.

## 2026-11-04: Step 6, data that breaks the learned index

**Question.** So far the keys were almost a straight line, the best case for a learned index. What happens on data that is not? And does the B-tree care at all?

**Expectation.** The B-tree doesn't look at the values, only at their order, so its speed should not depend on the distribution. The learned index should get slower the less the data looks like a line, because its windows get bigger.

**Setup.**
- 5 distributions with 10 million keys each (`KeyDistributions`), shapes in `bench/distributions-shape.png`:
  - **uniform:** random gaps of 1–100 (the data from before)
  - **lognormal:** mostly small gaps, sometimes huge ones
  - **quadratic:** key = i², dense at the start, sparse at the end, so position follows a curve
  - **clusters:** 100 dense groups, far apart
  - **outlier:** the uniform keys, but the last key is 2^60
- 11 new tests: every distribution is sorted and distinct, and all structures still find every key on it. 70 tests in total, all pass.
- Window sizes and used segments from `DistributionStats`: `bench/distributions-stats.txt`
- Same JMH settings, pinned to two E-cores

**Result.** Raw data: `bench/distributions.csv`, plot: `bench/distributions.png`.

| data | binary search | B-tree 16 | learned 1,024 | learned 65,536 | avg window (65,536) | used segments (65,536) |
|---|---|---|---|---|---|---|
| uniform | 1175 ns | 556 ns | 234 ns | **178 ns** | 9 | 65,536 |
| lognormal | 1157 ns | 508 ns | 575 ns | **250 ns** | 39 | 63,780 |
| quadratic | 1063 ns | 469 ns | 302 ns | **175 ns** | 467 | 53,249 |
| clusters | 994 ns | 500 ns | **320 ns** | 323 ns | 197 | 100 |
| outlier | 997 ns | **541 ns** | 728 ns | 717 ns | 1,937 | 2 |

- **The B-tree really doesn't care:** 469–556 ns on all five, within the error bars.
- **One single key breaks the learned index.** With the outlier, the root line has to reach from ~500 million to 2^60. All other keys land on almost the same spot, so only 2 of 65,536 segments get used. The learned index falls back to "one line" speed (717 ns) and is now **slower than the B-tree**.
- **Clusters:** only 100 of 65,536 segments get used (one per cluster), so more segments don't help at all (320 vs. 323 ns). It still beats the B-tree, because inside each cluster the keys are evenly spread again.
- **Lognormal:** with 1,024 segments the learned index is slower than the B-tree (575 vs. 508 ns, the error bars almost touch). With 65,536 it wins again.
- Binary search was noisier and slower in this run than before (±20%, ~1,000–1,175 ns instead of ~840 ns). It ran first. The big differences between the structures are much larger than this noise, the small ones (lognormal, B-tree vs. learned 1,024) are not.

**What I learned.**
- The learned index depends on *two* things: whether each segment's data is close to a line, and whether the root line spreads the keys over the segments. The outlier and the clusters attack the second one. That is the weak spot: the root is a single least-squares line, and one extreme value can pull it completely off.
- Least squares is very sensitive to outliers. A fix could be a root model that ignores extreme keys (e.g. built on the middle 99.9% of the keys, with the rest searched separately), or segments of equal size instead of a root line. Not tried yet.
- The B-tree's worst case is the same as its best case. The learned index can be 3× faster or 1.3× slower than the B-tree, depending on data you might not control.

**Open question.** Quadratic has an average window of 467 keys, but it is as fast as uniform with a window of 9. I can't explain this yet. Next idea: measure time per lookup grouped by window size.

## Template

### YYYY-MM-DD: <title>
- **Question:**
- **Hypothesis:**
- **Setup:** (machine, JDK, data size, distribution, JMH settings)
- **Result:** (numbers + link to raw data in `bench/`)
- **What I learned:**
- **Next step:**
