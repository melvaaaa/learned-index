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
 * B-tree with different node sizes vs. binary search, on the same keys and queries.
 *
 * <p>The node size is a parameter of {@link Tree} only, so the binary search runs once per array size.
 * Run: {@code bench\run-pinned.cmd BTreeBench -rf csv -rff bench\btree.csv}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class BTreeBench {

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
    public static class Tree {
        @Param({"8", "16", "32", "64"})
        public int nodeSize;

        StaticBTree tree;

        @Setup(Level.Trial)
        public void setUp(Data data) {
            tree = new StaticBTree(data.keys, nodeSize);
        }
    }

    @Benchmark
    public int binarySearch(Data data) {
        return BinarySearch.find(data.keys, data.nextQuery());
    }

    @Benchmark
    public int btree(Data data, Tree tree) {
        return tree.tree.find(data.nextQuery());
    }
}
