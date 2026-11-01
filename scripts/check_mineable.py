#!/usr/bin/env python3
"""List feedback blocks (by blockstate file) absent from every minecraft:mineable/* tag."""
import glob, json, os, sys
root = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src")
blocks = set()
for d in ("main", "generated"):
    for f in glob.glob(f"{root}/{d}/resources/assets/feedback/blockstates/*.json"):
        blocks.add(os.path.basename(f)[:-5])
tagged = set()
for d in ("main", "generated"):
    for f in glob.glob(f"{root}/{d}/resources/data/minecraft/tags/block/mineable/*.json"):
        for v in json.load(open(f))["values"]:
            v = v["id"] if isinstance(v, dict) else v
            if v.startswith("feedback:"):
                tagged.add(v[9:])
# Intended tool-less: instant-break plants, fluids, soft seeps, unbreakable creative sources/chassis.
OK = {"crude_oil", "crude_pool", "seep", "chassis_cell", "copper_flower", "zinc_violet", "bladder_campion"}
missing = sorted(b for b in blocks - tagged
                 if b not in OK and not b.endswith("_crop") and not b.startswith("creative_"))
print("\n".join(missing))
print(f"{len(missing)} of {len(blocks)} blocks have no mineable tool", file=sys.stderr)
sys.exit(1 if missing else 0)
