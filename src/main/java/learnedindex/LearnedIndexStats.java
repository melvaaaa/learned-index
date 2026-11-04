package learnedindex;

import java.util.Random;

/**
 * Prints how big the learned index's search windows are and how much memory it needs, for the same keys
 * as {@link LearnedIndexBench}.
 *
 * <p>Run: {@code java -cp target/classes learnedindex.LearnedIndexStats > bench/learned-index-stats.txt}
 */
public final class LearnedIndexStats {

    private LearnedIndexStats() {}

    public static void main(String[] args) {
        int[] sizes = {1_000, 64_000, 1_000_000, 10_000_000};
        int[] segmentCounts = {1, 1024, 65536};
        System.out.printf("%10s %9s %14s %11s %12s%n", "keys", "segments", "avg window", "max window", "model KB");
        for (int size : sizes) {
            long[] keys = keys(size);
            for (int segments : segmentCounts) {
                LearnedIndex index = new LearnedIndex(keys, segments);
                System.out.printf("%10d %9d %14.1f %11d %12.1f%n",
                        size, segments, index.averageWindow(), index.maxWindow(), index.extraBytes() / 1024.0);
            }
            StaticBTree tree = new StaticBTree(keys, 16);
            System.out.printf("%10d %9s %14s %11s %12.1f   (B-tree, node size 16)%n",
                    size, "-", "-", "-", tree.extraBytes() / 1024.0);
        }
    }

    /** Same keys as in the benchmarks: seed 42, random gaps of 1 to 100. */
    private static long[] keys(int size) {
        Random rnd = new Random(42);
        long[] keys = new long[size];
        long k = 0;
        for (int i = 0; i < size; i++) {
            k += 1 + rnd.nextInt(100);
            keys[i] = k;
        }
        return keys;
    }
}
