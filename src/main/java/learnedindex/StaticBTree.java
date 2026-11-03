package learnedindex;

import java.util.ArrayList;
import java.util.List;

/**
 * A read-only B+ tree built once from a sorted array.
 *
 * <p>Layout: level 0 is the sorted array itself, cut into blocks of {@code nodeSize} keys. Each level above
 * stores the first key of every block of the level below, so it is {@code nodeSize} times smaller. The top
 * level has at most {@code nodeSize} keys. There are no pointers: the i-th key of a level stands for the
 * i-th block of the level below, so the child's position is just {@code i * nodeSize}.
 *
 * <p>A lookup visits one block per level, so it touches about log_nodeSize(n) blocks instead of the
 * log_2(n) scattered positions of a binary search. Each block is a few neighbouring cache lines.
 */
public final class StaticBTree {

    private final int nodeSize;
    /** levels[0] = the sorted keys, levels[levels.length - 1] = the top level. */
    private final long[][] levels;

    /**
     * @param sorted   keys in ascending order, no duplicates. Not copied, so don't change it afterwards.
     * @param nodeSize keys per node, at least 2
     */
    public StaticBTree(long[] sorted, int nodeSize) {
        if (nodeSize < 2) {
            throw new IllegalArgumentException("nodeSize must be at least 2, was " + nodeSize);
        }
        this.nodeSize = nodeSize;

        List<long[]> built = new ArrayList<>();
        long[] level = sorted;
        built.add(level);
        while (level.length > nodeSize) {
            long[] above = new long[(level.length + nodeSize - 1) / nodeSize];
            for (int i = 0; i < above.length; i++) {
                above[i] = level[i * nodeSize];
            }
            built.add(above);
            level = above;
        }
        this.levels = built.toArray(new long[0][]);
    }

    /** Returns the position of {@code key} in the sorted array, or {@code -1} if the key is not there. */
    public int find(long key) {
        if (levels[0].length == 0) {
            return -1;
        }
        int pos = 0; // the top level is a single node starting at 0
        for (int lvl = levels.length - 1; lvl >= 0; lvl--) {
            long[] keys = levels[lvl];
            int start = pos * nodeSize;
            int end = Math.min(start + nodeSize, keys.length);

            // Position of the last key <= key inside this node. Counting instead of stopping early keeps the
            // loop free of hard-to-predict branches; the node is small and already in cache after the first load.
            int count = 0;
            for (int j = start + 1; j < end; j++) {
                count += keys[j] <= key ? 1 : 0;
            }
            pos = start + count;
        }
        return levels[0][pos] == key ? pos : -1;
    }

    /** Extra memory for the levels above the sorted array, in bytes. */
    public long extraBytes() {
        long total = 0;
        for (int lvl = 1; lvl < levels.length; lvl++) {
            total += 8L * levels[lvl].length;
        }
        return total;
    }

    public int height() {
        return levels.length;
    }
}
