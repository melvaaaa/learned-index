package learnedindex;

import java.util.List;
import java.util.Random;

/**
 * Sorted, distinct key sets with different shapes, from "almost a straight line" to "very hard for a line".
 */
public final class KeyDistributions {

    public static final List<String> NAMES = List.of("uniform", "lognormal", "quadratic", "clusters", "outlier");

    private KeyDistributions() {}

    public static long[] generate(String name, int n, long seed) {
        Random rnd = new Random(seed);
        long[] keys = new long[n];
        switch (name) {
            case "uniform" -> {
                // Random gaps of 1 to 100: the keys from all earlier experiments. Almost a straight line.
                long k = 0;
                for (int i = 0; i < n; i++) {
                    k += 1 + rnd.nextInt(100);
                    keys[i] = k;
                }
            }
            case "lognormal" -> {
                // Mostly small gaps, sometimes very big ones (heavy tail). Locally bumpy.
                long k = 0;
                for (int i = 0; i < n; i++) {
                    k += 1 + (long) Math.exp(2 * rnd.nextGaussian());
                    keys[i] = k;
                }
            }
            case "quadratic" -> {
                // key = i^2: dense at the start, sparse at the end. Position grows like sqrt(key), a curve.
                for (int i = 0; i < n; i++) {
                    keys[i] = (long) i * i;
                }
            }
            case "clusters" -> {
                // 100 dense groups, far apart. Most of the key range is empty.
                int perCluster = Math.max(1, n / 100);
                long k = 0;
                for (int i = 0; i < n; i++) {
                    if (i % perCluster == 0) {
                        k = (long) (i / perCluster) * 1_000_000_000_000L;
                    }
                    k += 1 + rnd.nextInt(10);
                    keys[i] = k;
                }
            }
            case "outlier" -> {
                // The uniform keys, but the last key is huge. One key out of millions.
                long k = 0;
                for (int i = 0; i < n; i++) {
                    k += 1 + rnd.nextInt(100);
                    keys[i] = k;
                }
                if (n > 0) {
                    keys[n - 1] = 1L << 60;
                }
            }
            default -> throw new IllegalArgumentException("unknown distribution: " + name);
        }
        return keys;
    }
}
