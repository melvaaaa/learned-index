package learnedindex;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * For each distribution: how many segments the learned index really uses and how big its search windows
 * get. Also writes a sample of (key, position) points per distribution, to plot what the data looks like.
 *
 * <p>Run: {@code java -cp target/classes learnedindex.DistributionStats bench}
 * writes {@code bench/distributions-stats.txt} and {@code bench/distributions-shape.csv}.
 */
public final class DistributionStats {

    private static final int SAMPLE_POINTS = 2_000;

    private DistributionStats() {}

    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "bench");
        int n = DistributionBench.SIZE;
        try (PrintWriter stats = new PrintWriter(Files.newBufferedWriter(dir.resolve("distributions-stats.txt")));
                PrintWriter shape = new PrintWriter(Files.newBufferedWriter(dir.resolve("distributions-shape.csv")))) {
            stats.printf("%d keys per distribution%n%n", n);
            stats.printf("%-10s %9s %14s %14s %11s%n", "data", "segments", "used segments", "avg window", "max window");
            shape.println("distribution,key,position");

            for (String name : KeyDistributions.NAMES) {
                long[] keys = KeyDistributions.generate(name, n, 42);
                for (int segments : new int[] {1, 1024, 65536}) {
                    LearnedIndex index = new LearnedIndex(keys, segments);
                    stats.printf("%-10s %9d %14d %14.1f %11d%n", name, segments, index.usedSegments(),
                            index.averageWindow(), index.maxWindow());
                }
                for (int j = 0; j < SAMPLE_POINTS; j++) {
                    int i = (int) ((long) j * (n - 1) / (SAMPLE_POINTS - 1));
                    shape.printf("%s,%d,%d%n", name, keys[i], i);
                }
            }
        }
        System.out.println("wrote " + dir.resolve("distributions-stats.txt") + " and " + dir.resolve("distributions-shape.csv"));
    }
}
