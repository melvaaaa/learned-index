package learnedindex;

import java.util.Arrays;
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
 * Time per lookup as a function of array size.
 *
 * <p>Queries are existing keys in random order, so consecutive lookups do not reuse each other's
 * cache lines. Run: {@code java -jar target/benchmarks.jar BinarySearchBench -rf csv -rff bench/binary-search.csv}
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
public class BinarySearchBench {

    private static final int QUERY_COUNT = 1 << 20; // power of two, so the index wraps with a mask
    private static final int QUERY_MASK = QUERY_COUNT - 1;

    @Param({"1000", "4000", "16000", "64000", "256000", "1000000", "4000000", "10000000"})
    public int size;

    private long[] keys;
    private long[] queries;
    private int next;

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

    @Benchmark
    public int mine() {
        return BinarySearch.find(keys, queries[next++ & QUERY_MASK]);
    }

    /** The JDK's implementation as a reference point. */
    @Benchmark
    public int jdk() {
        return Arrays.binarySearch(keys, queries[next++ & QUERY_MASK]);
    }
}
