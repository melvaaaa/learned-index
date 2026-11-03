package learnedindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** Checks {@link StaticBTree#find} against {@link BinarySearch#find}, which is already tested against the JDK. */
class StaticBTreeTest {

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
    void rejectsNodeSizeBelowTwo() {
        assertThrows(IllegalArgumentException.class, () -> new StaticBTree(new long[] {1, 2, 3}, 1));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 8, 16, 64})
    void emptyArray(int nodeSize) {
        assertEquals(-1, new StaticBTree(new long[0], nodeSize).find(42));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 8, 16, 64})
    void keysOutsideTheRangeAndBetweenElements(int nodeSize) {
        long[] a = {2, 4, 6, 8, 10, 12, 14, 16, 18, 20};
        StaticBTree tree = new StaticBTree(a, nodeSize);
        assertEquals(-1, tree.find(Long.MIN_VALUE));
        assertEquals(-1, tree.find(1));
        assertEquals(-1, tree.find(21));
        assertEquals(-1, tree.find(Long.MAX_VALUE));
        for (long key = 3; key <= 19; key += 2) {
            assertEquals(-1, tree.find(key), "key " + key);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 8, 16, 64})
    void extremeKeyValues(int nodeSize) {
        long[] a = {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE};
        StaticBTree tree = new StaticBTree(a, nodeSize);
        for (int i = 0; i < a.length; i++) {
            assertEquals(i, tree.find(a[i]), "key " + a[i]);
        }
    }

    /** Sizes around multiples and powers of the node size are where off-by-one errors in the levels show up. */
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 8, 16, 64})
    void everyKeyIsFoundForAllSmallSizes(int nodeSize) {
        Random rnd = new Random(nodeSize);
        for (int n = 1; n <= 300; n++) {
            long[] a = randomSortedKeys(rnd, n);
            StaticBTree tree = new StaticBTree(a, nodeSize);
            for (int i = 0; i < n; i++) {
                assertEquals(i, tree.find(a[i]), "n=" + n + ", i=" + i);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 8, 16, 64})
    void randomQueriesMatchBinarySearch(int nodeSize) {
        Random rnd = new Random(100 + nodeSize);
        for (int round = 0; round < 30; round++) {
            long[] a = randomSortedKeys(rnd, 1 + rnd.nextInt(50_000));
            StaticBTree tree = new StaticBTree(a, nodeSize);
            long max = a[a.length - 1] + 100;
            for (int q = 0; q < 2_000; q++) {
                long key = Math.floorMod(rnd.nextLong(), max);
                assertEquals(BinarySearch.find(a, key), tree.find(key), "key " + key);
            }
        }
    }

    @Test
    void heightAndExtraMemory() {
        long[] a = randomSortedKeys(new Random(7), 1000);
        StaticBTree tree = new StaticBTree(a, 10);
        assertEquals(3, tree.height()); // levels of 1000, 100 and 10 keys
        assertEquals(8L * (100 + 10), tree.extraBytes());
    }
}
