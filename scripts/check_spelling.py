#!/usr/bin/env python3
"""Flag British spellings in player-facing lang values (American is standard). Keys are ids and never checked.
Usage: check_spelling.py [--fix]. Exit 1 when any are found (and not fixed)."""
import json, re, sys, pathlib
WORDS = {"armour": "armor", "armoured": "armored", "unarmoured": "unarmored", "colour": "color", "grey": "gray", "metre": "meter",
         "litre": "liter", "centre": "center", "tyre": "tire", "behaviour": "behavior", "favour": "favor", "honour": "honor",
         "neighbour": "neighbor", "flavour": "flavor", "harbour": "harbor", "labour": "labor", "vapour": "vapor", "sulphur": "sulfur",
         "aluminium": "aluminum", "oxidise": "oxidize", "realise": "realize", "analyse": "analyze", "grey": "gray", "defence": "defense",
         "licence": "license", "offence": "offense", "catalogue": "catalog", "fibre": "fiber", "lustre": "luster", "plough": "plow"}
pat = re.compile(r"\b(" + "|".join(sorted(WORDS, key=len, reverse=True)) + r")(s|ing|ed)?\b", re.I)
def swap(m):
    w = WORDS[m.group(1).lower()] + (m.group(2) or "")
    return w.capitalize() if m.group(1)[0].isupper() else w
fix = "--fix" in sys.argv
bad = 0
for f in pathlib.Path("src/main/resources/assets").glob("*/lang/en_us.json"):
    lines = f.read_text().split("\n")
    for i, line in enumerate(lines):
        m = re.match(r'(\s*"[^"]+":\s*)(".*)', line)
        if m and pat.search(m.group(2)):
            if fix:
                lines[i] = m.group(1) + pat.sub(swap, m.group(2))
            else:
                bad += 1
                print(f"{f}:{i+1}: {pat.search(m.group(2)).group(0)}")
    if fix:
        f.write_text("\n".join(lines)); json.loads(f.read_text())
sys.exit(1 if bad else 0)
