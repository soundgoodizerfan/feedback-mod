#!/usr/bin/env python3
"""List crafting-grid recipes that can match the same grid contents.

Shaped recipes are trimmed and compared in both mirror orientations; shapeless ones by ingredient set
(a shaped and a shapeless recipe collide when they have the same cell count). Two ingredients overlap
when equal, or when one is a tag that lists the other item (only feedback tags are resolved; vanilla
tags are treated as opaque, so a vanilla tag against a bare item is not flagged).
Exit 1 when any collision is found."""
import glob, itertools, json, os, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src")
DIRS = [f"{ROOT}/{d}/resources/data" for d in ("main", "generated")]


def load(pattern):
    for d in DIRS:
        yield from glob.glob(f"{d}/{pattern}")


tags = {}  # "feedback:name" -> set of item ids
for f in load("feedback/tags/item/*.json"):
    vals = json.load(open(f))["values"]
    tags["feedback:" + os.path.basename(f)[:-5]] = {v["id"] if isinstance(v, dict) else v for v in vals}


def ing(i):
    """Ingredient json -> frozenset of tokens: item ids, or '#tag' (plus its resolved items)."""
    if isinstance(i, list):
        return frozenset().union(*(ing(x) for x in i))
    if "item" in i:
        return frozenset([i["item"]])
    t = i["tag"]
    return frozenset(["#" + t, *tags.get(t, ())])


def shaped(r):
    rows = r["pattern"]
    grid = [[ing(r["key"][c]) if c != " " else None for c in row.ljust(max(map(len, rows)))] for row in rows]
    rows_used = [i for i, row in enumerate(grid) if any(row)]
    cols_used = [j for j in range(len(grid[0])) if any(row[j] for row in grid)]
    grid = [row[cols_used[0]:cols_used[-1] + 1] for row in grid[rows_used[0]:rows_used[-1] + 1]]
    return grid, [row[::-1] for row in grid]


def matchable(xs, ys):
    """True when xs and ys pair off one to one with every pair overlapping (backtracking)."""
    if not xs:
        return True
    return any(overlap(xs[0], y) and matchable(xs[1:], ys[:i] + ys[i + 1:]) for i, y in enumerate(ys))


def overlap(a, b):
    return (a is None and b is None) or (a is not None and b is not None and bool(a & b))


recipes = []  # (name, kind, data)
for f in load("*/recipe/*.json"):
    try:
        r = json.load(open(f))
    except ValueError:
        continue
    t = r.get("type")
    n = f"{f.split('/data/')[1]}"
    if t == "minecraft:crafting_shaped":
        recipes.append((n, "shaped", shaped(r)))
    elif t == "minecraft:crafting_shapeless":
        recipes.append((n, "shapeless", [ing(i) for i in r["ingredients"]]))

hits = []
for (an, ak, ad), (bn, bk, bd) in itertools.combinations(recipes, 2):
    if an.split("/", 1)[1] == bn.split("/", 1)[1]:  # same path in another namespace: an override, not a clash
        continue
    if ak == "shaped" and bk == "shaped":
        for ga in ad:
            gb = bd[0]
            if len(ga) == len(gb) and len(ga[0]) == len(gb[0]) and all(
                    overlap(x, y) for ra, rb in zip(ga, gb) for x, y in zip(ra, rb)):
                hits.append((an, bn))
                break
    elif ak == "shapeless" and bk == "shapeless":
        if len(ad) == len(bd) and matchable(ad, bd):
            hits.append((an, bn))
    else:
        s, l = (ad, bd) if ak == "shaped" else (bd, ad)
        cells = [c for row in s[0] for c in row if c is not None]
        if len(cells) == len(l) and matchable(cells, l):
            hits.append((an, bn))

for a, b in hits:
    print(f"{a}  <->  {b}")
print(f"{len(hits)} collisions in {len(recipes)} grid recipes", file=sys.stderr)
sys.exit(1 if hits else 0)
