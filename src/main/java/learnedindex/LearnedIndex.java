package learnedindex;

/**
 * A learned index: straight lines predict where a key sits in the sorted array.
 *
 * <p>With {@code segments == 1} it is a single line fitted to all (key, position) pairs. With more segments it
 * is a two-stage model as in Kraska et al.: a root line picks a segment, and each segment has its own line
 * fitted to the keys the root sends there.
 *
 * <p>For every segment the index remembers how far its line is off at worst, below and above. A lookup
 * predicts a position, then binary-searches only that window. Because the window is computed from the same
 * predictions as the lookup, every key that is in the array is always found, however bad the lines are. Bad
 * lines only make the window, and so the search, bigger.
 */
public final class LearnedIndex {

    private final long[] keys;
    private final int segments;
    /** Root line, already scaled from "position" to "segment number". */
    private final double rootSlope;
    private final double rootIntercept;
    private final double[] slope;
    private final double[] intercept;
    private final int[] minError;
    private final int[] maxError; // maxError < minError marks a segment without keys

    /**
     * @param sorted   keys in ascending order, no duplicates. Not copied, so don't change it afterwards.
     * @param segments number of lines in the second stage, at least 1
     */
    public LearnedIndex(long[] sorted, int segments) {
        if (segments < 1) {
            throw new IllegalArgumentException("segments must be at least 1, was " + segments);
        }
        this.keys = sorted;
        this.segments = segments;
        Line root = Line.fit(sorted, 0, sorted.length);
        double scale = (double) segments / Math.max(1, sorted.length);
        this.rootSlope = root.slope * scale;
        this.rootIntercept = root.intercept * scale;
        this.slope = new double[segments];
        this.intercept = new double[segments];
        this.minError = new int[segments];
        this.maxError = new int[segments];

        // The root line is increasing, so each segment gets a contiguous run of keys.
        int start = 0;
        for (int s = 0; s < segments; s++) {
            int end = start;
            while (end < sorted.length && segmentOf(sorted[end]) == s) {
                end++;
            }
            fitSegment(s, start, end);
            start = end;
        }
        if (start != sorted.length) {
            throw new IllegalStateException("keys were not assigned to segments in order");
        }
    }

    private void fitSegment(int s, int start, int end) {
        Line line = Line.fit(keys, start, end);
        slope[s] = line.slope;
        intercept[s] = line.intercept;
        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        for (int i = start; i < end; i++) {
            int error = i - predict(s, keys[i]);
            lo = Math.min(lo, error);
            hi = Math.max(hi, error);
        }
        minError[s] = lo; // stays MAX_VALUE > MIN_VALUE for an empty segment
        maxError[s] = hi;
    }

    /** Returns the position of {@code key} in the sorted array, or {@code -1} if the key is not there. */
    public int find(long key) {
        int s = segmentOf(key);
        if (maxError[s] < minError[s]) {
            return -1; // no key was ever sent to this segment
        }
        int p = predict(s, key);
        int lo = Math.max(0, p + minError[s]);
        int hi = Math.min(keys.length - 1, p + maxError[s]);
        return BinarySearch.find(keys, key, lo, hi);
    }

    private int segmentOf(long key) {
        if (segments == 1) {
            return 0;
        }
        double s = rootSlope * (double) key + rootIntercept;
        return (int) Math.max(0, Math.min(segments - 1, s)); // a NaN becomes 0
    }

    private int predict(int s, long key) {
        double p = slope[s] * (double) key + intercept[s];
        // Round instead of cutting off: 4999.9999 should mean position 5000, not 4999.
        return (int) (Math.max(0, Math.min(keys.length - 1, p)) + 0.5);
    }

    /** Largest search window over all segments, in keys. */
    public int maxWindow() {
        int max = 0;
        for (int s = 0; s < segments; s++) {
            if (maxError[s] >= minError[s]) {
                max = Math.max(max, maxError[s] - minError[s] + 1);
            }
        }
        return max;
    }

    /** Average search window when every key is looked up once, in keys. */
    public double averageWindow() {
        if (keys.length == 0) {
            return 0;
        }
        long sum = 0;
        for (long key : keys) {
            int s = segmentOf(key);
            int p = predict(s, key);
            int lo = Math.max(0, p + minError[s]);
            int hi = Math.min(keys.length - 1, p + maxError[s]);
            sum += hi - lo + 1;
        }
        return (double) sum / keys.length;
    }

    /** Extra memory for the model, in bytes (two doubles and two ints per segment, plus the root line). */
    public long extraBytes() {
        return 24L * segments + 16;
    }

    /** A least-squares line position = slope * key + intercept. */
    private record Line(double slope, double intercept) {

        static Line fit(long[] keys, int start, int end) {
            int n = end - start;
            if (n == 0) {
                return new Line(0, 0);
            }
            // Centred sums keep the numbers small, which matters when keys are huge.
            double meanX = 0;
            double meanY = 0;
            for (int i = start; i < end; i++) {
                meanX += (double) keys[i] / n;
                meanY += (double) i / n;
            }
            double sxy = 0;
            double sxx = 0;
            for (int i = start; i < end; i++) {
                double dx = (double) keys[i] - meanX;
                sxy += dx * (i - meanY);
                sxx += dx * dx;
            }
            double slope = sxx > 0 ? sxy / sxx : 0;
            return new Line(slope, meanY - slope * meanX);
        }
    }
}
