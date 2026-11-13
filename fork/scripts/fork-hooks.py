#!/usr/bin/env python3
"""List and check the fork's hook lines in upstream-owned files.

A hook is a line in an upstream file that carries the marker `FORK-HOOK(<feature>)`. The
registry `fork/hooks.txt` records how many hooks each feature has in each file. After a merge
from upstream, a hook that the merge dropped shows up here instead of on a device.

    fork/scripts/fork-hooks.py           check the tree against the registry (exit 1 on a difference)
    fork/scripts/fork-hooks.py --write   rewrite the registry from the tree
    fork/scripts/fork-hooks.py --list    print every hook line with file and line number
"""
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REGISTRY = ROOT / "fork" / "hooks.txt"
MARKER = re.compile(r"FORK-HOOK\(([a-z0-9-]+)\)")
# Fork-owned places: a marker there is documentation, not a hook into upstream's code.
OWNED = ("fork/", "docs/", "UX_GAP_WORKLOG.md", "CLAUDE.md")


def is_owned(path: str) -> bool:
    return path.startswith(OWNED) or "/fork/" in path


def scan():
    files = subprocess.run(
        ["git", "grep", "-l", "-I", "FORK-HOOK(", "--", "."],
        cwd=ROOT, capture_output=True, text=True,
    ).stdout.split("\n")
    counts, lines = Counter(), []
    for path in sorted(p for p in files if p and not is_owned(p)):
        for number, text in enumerate((ROOT / path).read_text(errors="replace").splitlines(), 1):
            for feature in MARKER.findall(text):
                counts[(feature, path)] += 1
                lines.append(f"{feature}\t{path}:{number}\t{text.strip()}")
    return counts, lines


def read_registry():
    counts = Counter()
    if REGISTRY.exists():
        for row in REGISTRY.read_text().splitlines():
            if row and not row.startswith("#"):
                feature, path, count = row.split("\t")
                counts[(feature, path)] = int(count)
    return counts


def main() -> int:
    counts, lines = scan()
    if "--list" in sys.argv:
        print("\n".join(lines))
        return 0
    if "--write" in sys.argv:
        header = "# feature<TAB>file<TAB>hook lines. Written by fork/scripts/fork-hooks.py --write.\n"
        rows = [f"{f}\t{p}\t{n}" for (f, p), n in sorted(counts.items())]
        REGISTRY.write_text(header + "\n".join(rows) + "\n")
        print(f"{sum(counts.values())} hook lines in {len({p for _, p in counts})} files")
        return 0
    expected = read_registry()
    problems = []
    for key in sorted(set(expected) | set(counts)):
        have, want = counts.get(key, 0), expected.get(key, 0)
        if have != want:
            problems.append(f"{key[0]}\t{key[1]}\tregistry {want}, tree {have}")
    if problems:
        print("Fork hooks differ from fork/hooks.txt:\n" + "\n".join(problems))
        return 1
    print(f"ok: {sum(counts.values())} hook lines in {len({p for _, p in counts})} files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
