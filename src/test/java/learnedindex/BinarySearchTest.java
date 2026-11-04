package learnedindex;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Checks {@link BinarySearch#find} against the JDK's {@link Arrays#binarySearch}, which serves as the oracle.
 */
class BinarySearchTest {

    /** Translates the JDK result (negative when absent) into this project's contract (-1 when absent). */
    private static int expected(long[] sorted, long key) {
        int i = Arrays.binarySearch(sorted, key);
        return i >= 0 ? i : -1;
    }

    /** Sorted, distinct keys with random gaps, so neighbouring keys are not simply 0, 1, 2, ... */
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
    void emptyArray() {
        assertEquals(-1, BinarySearch.find(new long[0], 42));
    }

    @Test
    void singleElement() {
        long[] a = {7};
        assertEquals(0, BinarySearch.find(a, 7));
        assertEquals(-1, BinarySearch.find(a, 6));
        assertEquals(-1, BinarySearch.find(a, 8));
    }

    @Test
    void firstAndLastElement() {
        long[] a = {2, 4, 6, 8, 10};
        assertEquals(0, BinarySearch.find(a, 2));
        assertEquals(4, BinarySearch.find(a, 10));
    }

    @Test
    void keysOutsideTheRange() {
        long[] a = {2, 4, 6, 8, 10};
        assertEquals(-1, BinarySearch.find(a, 1));
        assertEquals(-1, BinarySearch.find(a, 11));
        assertEquals(-1, BinarySearch.find(a, Long.MIN_VALUE));
        assertEquals(-1, BinarySearch.find(a, Long.MAX_VALUE));
    }

    @Test
    void keysBetweenElements() {
        long[] a = {2, 4, 6, 8, 10};
        for (long key = 3; key <= 9; key += 2) {
            assertEquals(-1, BinarySearch.find(a, key), "key " + key);
        }
    }

    @Test
    void extremeKeyValues() {
        long[] a = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
        for (int i = 0; i < a.length; i++) {
            assertEquals(i, BinarySearch.find(a, a[i]), "key " + a[i]);
        }
    }

    @Test
    void everyKeyIsFoundInArraysOfAllSmallSizes() {
        Random rnd = new Random(1);
        for (int n = 1; n <= 200; n++) {
            long[] a = randomSortedKeys(rnd, n);
            for (int i = 0; i < n; i++) {
                assertEquals(i, BinarySearch.find(a, a[i]), "n=" + n + ", i=" + i);
            }
        }
    }

    @Test
    void rangeSearchOnlyLooksInsideTheRange() {
        long[] a = {2, 4, 6, 8, 10, 12};
        assertEquals(2, BinarySearch.find(a, 6, 1, 3));
        assertEquals(1, BinarySearch.find(a, 4, 1, 1));
        assertEquals(-1, BinarySearch.find(a, 2, 1, 5)); // present, but left of the range
        assertEquals(-1, BinarySearch.find(a, 12, 0, 4)); // present, but right of the range
        assertEquals(-1, BinarySearch.find(a, 6, 3, 2)); // empty range
    }

    @Test
    void randomQueriesMatchTheJdk() {
        Random rnd = new Random(2);
        for (int round = 0; round < 50; round++) {
            long[] a = randomSortedKeys(rnd, 1 + rnd.nextInt(10_000));
            long max = a[a.length - 1] + 100;
            for (int q = 0; q < 1_000; q++) {
                long key = Math.floorMod(rnd.nextLong(), max);
                assertEquals(expected(a, key), BinarySearch.find(a, key), "key " + key);
            }
        }
    }
}
