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
 * All three structures on 10 million keys from each distribution in {@link KeyDistributions}.
 *
 * <p>Run: {@code bench\run-pinned.cmd DistributionBench -rf csv -rff bench\distributions.csv}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class DistributionBench {

    static final int SIZE = 10_000_000;
    private static final int QUERY_COUNT = 1 << 20; // power of two, so the index wraps with a mask
    private static final int QUERY_MASK = QUERY_COUNT - 1;

    @State(Scope.Benchmark)
    public static class Data {
        @Param({"uniform", "lognormal", "quadratic", "clusters", "outlier"})
        public String distribution;

        long[] keys;
        long[] queries;
        int next;

        @Setup(Level.Trial)
        public void setUp() {
            keys = KeyDistributions.generate(distribution, SIZE, 42);
            Random rnd = new Random(43);
            queries = new long[QUERY_COUNT];
            for (int i = 0; i < QUERY_COUNT; i++) {
                queries[i] = keys[rnd.nextInt(SIZE)];
            }
        }

        long nextQuery() {
            return queries[next++ & QUERY_MASK];
        }
    }

    @State(Scope.Benchmark)
    public static class Structures {
        StaticBTree tree;
        LearnedIndex learned1024;
        LearnedIndex learned65536;

        @Setup(Level.Trial)
        public void setUp(Data data) {
            tree = new StaticBTree(data.keys, 16);
            learned1024 = new LearnedIndex(data.keys, 1024);
            learned65536 = new LearnedIndex(data.keys, 65536);
        }
    }

    @Benchmark
    public int binarySearch(Data data) {
        return BinarySearch.find(data.keys, data.nextQuery());
    }

    @Benchmark
    public int btree16(Data data, Structures s) {
        return s.tree.find(data.nextQuery());
    }

    @Benchmark
    public int learned1024(Data data, Structures s) {
        return s.learned1024.find(data.nextQuery());
    }

    @Benchmark
    public int learned65536(Data data, Structures s) {
        return s.learned65536.find(data.nextQuery());
    }
}
