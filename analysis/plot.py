"""Plot time per lookup vs. array size from a JMH CSV file.

Usage:  python analysis/plot.py bench/binary-search.csv
Needs:  pip install -r analysis/requirements.txt
"""

import csv
import math
import sys
from collections import defaultdict
from pathlib import Path

import matplotlib.pyplot as plt


def load(path):
    """Returns {label: [(size, ns, error), ...]} sorted by size.

    The label is the benchmark name plus every parameter other than size, e.g. "btree nodeSize=16".
    """
    series = defaultdict(list)
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            name = row["Benchmark"].rsplit(".", 1)[-1]
            extra = [
                f"{col[len('Param: '):]}={value}"
                for col, value in row.items()
                if col.startswith("Param: ") and col != "Param: size" and value
            ]
            name = " ".join([name, *extra])
            series[name].append(
                (int(row["Param: size"]), float(row["Score"]), float(row["Score Error (99.9%)"]))
            )
    return {name: sorted(points) for name, points in series.items()}


def main():
    csv_path = Path(sys.argv[1] if len(sys.argv) > 1 else "bench/binary-search.csv")
    series = load(csv_path)

    fig, ax = plt.subplots(figsize=(8, 5))
    for name, points in series.items():
        sizes, ns, err = zip(*points)
        ax.errorbar(sizes, ns, yerr=err, marker="o", capsize=3, label=name)

    # Pure log2(n) growth, anchored at the smallest size: what the textbook predicts.
    anchor = series.get("mine") or series.get("binarySearch") or next(iter(series.values()))
    n0, t0, _ = anchor[0]
    sizes = [p[0] for p in anchor]
    ax.plot(sizes, [t0 * math.log2(n) / math.log2(n0) for n in sizes], "k--", label="log2(n), scaled")

    ax.set_xscale("log")
    ax.set_xlabel("array size (number of keys)")
    ax.set_ylabel("time per lookup (ns)")
    ax.set_title(f"Time per lookup ({csv_path.stem})")
    ax.grid(True, which="both", alpha=0.3)
    ax.legend()

    out = csv_path.with_suffix(".png")
    fig.tight_layout()
    fig.savefig(out, dpi=150)
    print(f"saved {out}")


if __name__ == "__main__":
    main()
