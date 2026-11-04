package learnedindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Checks {@link LearnedIndex#find} against {@link BinarySearch#find}, which is already tested against the JDK. */
class LearnedIndexTest {

    private static long[] randomSortedKeys(Random rnd, int n) {
        long[] a = new long[n];
        long k = rnd.nextInt(1000);
        for (int i = 0; i < n; i++) {
            k += 1 + rnd.nextInt(100);
            a[i] = k;
        }
        return a;
    }

    @Test
    void rejectsZeroSegments() {
        assertThrows(IllegalArgumentException.class, () -> new LearnedIndex(new long[] {1, 2, 3}, 0));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 16, 1000})
    void emptyArray(int segments) {
        assertEquals(-1, new LearnedIndex(new long[0], segments).find(42));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 16, 1000})
    void keysOutsideTheRangeAndBetweenElements(int segments) {
        long[] a = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
        LearnedIndex index = new LearnedIndex(a, segments);
        assertEquals(-1, index.find(Long.MIN_VALUE));
        assertEquals(-1, index.find(1));
        assertEquals(-1, index.find(21));
        assertEquals(-1, index.find(Long.MAX_VALUE));
        for (long key = 3; key <= 19; key += 2) {
            assertEquals(-1, index.find(key), "key " + key);
        }
    }

    /** Keys spread over the whole long range: a line through them is very steep and very flat at once. */
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 16, 1000})
    void extremeKeyValues(int segments) {
        long[] a = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
        LearnedIndex index = new LearnedIndex(a, segments);
        for (int i = 0; i < a.length; i++) {
            assertEquals(i, index.find(a[i]), "key " + a[i]);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 16, 1000})
    void everyKeyIsFoundForAllSmallSizes(int segments) {
        Random rnd = new Random(segments);
        for (int n = 1; n <= 300; n++) {
            long[] a = randomSortedKeys(rnd, n);
            LearnedIndex index = new LearnedIndex(a, segments);
            for (int i = 0; i < n; i++) {
                assertEquals(i, index.find(a[i]), "n=" + n + ", i=" + i);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 16, 1000})
    void randomQueriesMatchBinarySearch(int segments) {
        Random rnd = new Random(100 + segments);
        for (int round = 0; round < 30; round++) {
            long[] a = randomSortedKeys(rnd, 1 + rnd.nextInt(50_000));
            LearnedIndex index = new LearnedIndex(a, segments);
            long max = a[a.length - 1] + 100;
            for (int q = 0; q < 2_000; q++) {
                long key = Math.floorMod(rnd.nextLong(), max);
                assertEquals(BinarySearch.find(a, key), index.find(key), "key " + key);
            }
        }
    }

    /** Keys that lie exactly on a line must be predicted exactly. */
    @Test
    void evenlySpacedKeysGiveWindowOfOne() {
        long[] a = new long[10_000];
        for (int i = 0; i < a.length; i++) {
            a[i] = 1_000 + 7L * i;
        }
        LearnedIndex index = new LearnedIndex(a, 1);
        assertEquals(1, index.maxWindow());
        assertEquals(1.0, index.averageWindow());
    }

    @Test
    void moreSegmentsGiveSmallerWindows() {
        long[] a = randomSortedKeys(new Random(3), 100_000);
        double one = new LearnedIndex(a, 1).averageWindow();
        double many = new LearnedIndex(a, 1024).averageWindow();
        assertTrue(many < one, "1 segment: " + one + ", 1024 segments: " + many);
    }
}
