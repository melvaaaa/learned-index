package learnedindex;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Learned index with 1, 1,024 and 65,536 segments vs. binary search and the B-tree (node size 16),
 * on the same keys and queries as the other benchmarks.
 *
 * <p>Run: {@code bench\run-pinned.cmd LearnedIndexBench -rf csv -rff bench\learned-index.csv}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class LearnedIndexBench {

    private static final int QUERY_COUNT = 1 << 20; // power of two, so the index wraps with a mask
    private static final int QUERY_MASK = QUERY_COUNT - 1;

    @State(Scope.Benchmark)
    public static class Data {
        @Param({"1000", "64000", "1000000", "10000000"})
        public int size;

        long[] keys;
        long[] queries;
        int next;

        @Setup(Level.Trial)
        public void setUp() {
            Random rnd = new Random(42);
            keys = new long[size];
            long k = 0;
            for (int i = 0; i < size; i++) {
                k += 1 + rnd.nextInt(100);
                keys[i] = k;
            }
            queries = new long[QUERY_COUNT];
            for (int i = 0; i < QUERY_COUNT; i++) {
                queries[i] = keys[rnd.nextInt(size)];
            }
        }

        long nextQuery() {
            return queries[next++ & QUERY_MASK];
        }
    }

    @State(Scope.Benchmark)
    public static class Learned {
        @Param({"1", "1024", "65536"})
        public int segments;

        LearnedIndex index;

        @Setup(Level.Trial)
        public void setUp(Data data) {
            index = new LearnedIndex(data.keys, segments);
        }
    }

    @State(Scope.Benchmark)
    public static class Tree {
        StaticBTree tree;

        @Setup(Level.Trial)
        public void setUp(Data data) {
            tree = new StaticBTree(data.keys, 16);
        }
    }

    @Benchmark
    public int binarySearch(Data data) {
        return BinarySearch.find(data.keys, data.nextQuery());
    }

    @Benchmark
    public int btree16(Data data, Tree tree) {
        return tree.tree.find(data.nextQuery());
    }

    @Benchmark
    public int learned(Data data, Learned learned) {
        return learned.index.find(data.nextQuery());
    }
}
