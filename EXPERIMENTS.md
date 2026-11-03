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

## Template

### YYYY-MM-DD: <title>
- **Question:**
- **Hypothesis:**
- **Setup:** (machine, JDK, data size, distribution, JMH settings)
- **Result:** (numbers + link to raw data in `bench/`)
- **What I learned:**
- **Next step:**
