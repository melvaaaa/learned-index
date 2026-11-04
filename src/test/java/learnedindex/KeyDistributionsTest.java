package learnedindex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every distribution must give sorted, distinct keys, and every index must still find every key on them:
 * hard data may make the learned index slow, but never wrong.
 */
class KeyDistributionsTest {

    static Iterable<String> names() {
        return KeyDistributions.NAMES;
    }

    @ParameterizedTest
    @MethodSource("names")
    void keysAreSortedAndDistinct(String name) {
        for (int n : new int[] {0, 1, 2, 99, 100, 101, 10_000}) {
            long[] keys = KeyDistributions.generate(name, n, 1);
            assertEquals(n, keys.length);
            for (int i = 1; i < n; i++) {
                assertTrue(keys[i - 1] < keys[i], name + ", n=" + n + ", i=" + i);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("names")
    void allIndexesFindEveryKeyAndRejectOthers(String name) {
        long[] keys = KeyDistributions.generate(name, 20_000, 2);
        StaticBTree tree = new StaticBTree(keys, 16);
        LearnedIndex[] learned = {new LearnedIndex(keys, 1), new LearnedIndex(keys, 1024)};
        for (int i = 0; i < keys.length; i++) {
            assertEquals(i, tree.find(keys[i]), "B-tree, key " + keys[i]);
            for (LearnedIndex index : learned) {
                assertEquals(i, index.find(keys[i]), "learned, key " + keys[i]);
            }
        }
        Random rnd = new Random(3);
        for (int q = 0; q < 5_000; q++) {
            long key = rnd.nextLong();
            int expected = BinarySearch.find(keys, key);
            assertEquals(expected, tree.find(key), "B-tree, key " + key);
            for (LearnedIndex index : learned) {
                assertEquals(expected, index.find(key), "learned, key " + key);
            }
        }
    }

    @Test
    void unknownNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> KeyDistributions.generate("nope", 10, 1));
    }
}
