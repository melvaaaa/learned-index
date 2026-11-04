"""Plots for step 6: what each key distribution looks like, and how fast each structure is on it.

Usage:  python analysis/plot_distributions.py bench
Reads:  bench/distributions.csv (JMH), bench/distributions-shape.csv (DistributionStats)
Writes: bench/distributions.png, bench/distributions-shape.png
"""

import csv
import sys
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt

ORDER = ["uniform", "lognormal", "quadratic", "clusters", "outlier"]
METHODS = ["binarySearch", "btree16", "learned1024", "learned65536"]


def plot_times(bench):
    times = defaultdict(dict)  # distribution -> method -> (ns, error)
    with open(bench / "distributions.csv", newline="") as f:
        for row in csv.DictReader(f):
            method = row["Benchmark"].rsplit(".", 1)[-1]
            times[row["Param: distribution"]][method] = (float(row["Score"]), float(row["Score Error (99.9%)"]))

    fig, ax = plt.subplots(figsize=(9, 5))
    width = 0.2
    for m, method in enumerate(METHODS):
        xs = [d + (m - 1.5) * width for d in range(len(ORDER))]
        ns = [times[dist][method][0] for dist in ORDER]
        err = [times[dist][method][1] for dist in ORDER]
        ax.bar(xs, ns, width, yerr=err, capsize=2, label=method)
    ax.set_xticks(range(len(ORDER)), ORDER)
    ax.set_ylabel("time per lookup (ns)")
    ax.set_title("10 million keys: time per lookup by data distribution")
    ax.grid(True, axis="y", alpha=0.3)
    ax.legend()
    fig.tight_layout()
    fig.savefig(bench / "distributions.png", dpi=150)


def plot_shapes(bench):
    points = defaultdict(list)
    with open(bench / "distributions-shape.csv", newline="") as f:
        for row in csv.DictReader(f):
            points[row["distribution"]].append((int(row["key"]), int(row["position"])))

    fig, axes = plt.subplots(1, len(ORDER), figsize=(15, 3.2), sharey=True)
    for ax, dist in zip(axes, ORDER):
        keys, positions = zip(*points[dist])
        lo, hi = min(keys), max(keys)
        ax.plot([(k - lo) / (hi - lo) for k in keys], [p / positions[-1] for p in positions], ".", markersize=2)
        ax.plot([0, 1], [0, 1], "k--", linewidth=0.8)  # a perfect straight line
        ax.set_title(dist)
        ax.set_xlabel("key (scaled to 0..1)")
    axes[0].set_ylabel("position (scaled to 0..1)")
    fig.suptitle("What the data looks like: the closer to the dashed line, the easier for a learned index")
    fig.tight_layout()
    fig.savefig(bench / "distributions-shape.png", dpi=150)


def main():
    bench = Path(sys.argv[1] if len(sys.argv) > 1 else "bench")
    plot_shapes(bench)
    plot_times(bench)
    print(f"saved {bench / 'distributions-shape.png'} and {bench / 'distributions.png'}")


if __name__ == "__main__":
    main()
