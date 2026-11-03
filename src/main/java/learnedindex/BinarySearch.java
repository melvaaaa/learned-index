package learnedindex;

/**
 * Binary search over a sorted array: the baseline every other index in this project is measured against.
 */
public final class BinarySearch {

    private BinarySearch() {}

    /**
     * Returns the position of {@code key} in {@code sorted}, or {@code -1} if the key is not there.
     *
     * <p>Contract: {@code sorted} is in ascending order and contains no duplicates.
     */
    public static int find(long[] sorted, long key) {
        int lo = 0;
        int hi = sorted.length - 1;
        while (lo <= hi) {
            // Unsigned shift instead of (lo + hi) / 2: the sum can overflow for very large arrays.
            // The JDK had exactly this bug until 2006.
            int mid = (lo + hi) >>> 1;
            long value = sorted[mid];
            if (value < key) {
                lo = mid + 1;
            } else if (value > key) {
                hi = mid - 1;
            } else {
                return mid;
            }
        }
        return -1;
    }
}
