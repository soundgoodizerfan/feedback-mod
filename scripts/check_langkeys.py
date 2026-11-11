#!/usr/bin/env python3
"""List lang keys the datapack needs but en_us.json lacks. Exit 1 when any are missing.
Covers modular_type (role name+tip, role dims, variant name+desc, trait desc, type name) and gun_option (item name by form).
Add a rule below when a new data folder gets player-facing text."""
import json, pathlib, sys
R = pathlib.Path("src/main/resources")
lang = json.loads((R / "assets/feedback/lang/en_us.json").read_text())
need = {}  # key -> where
def want(key, where):
    need.setdefault(key, where)
def traits(v):
    t = v.get("trait"); return [x for x in (t.split(",") if isinstance(t, str) else t or [])]
for f in sorted((R / "data/feedback/modular_type").glob("*.json")):
    d = json.loads(f.read_text()); n = f.stem
    for des in d.get("names", {}).get("designations", []):
        prefix = "gun" if d.get("family") == "gun" else "armour"  # every non-gun type names its builds under armour.*
        want(f"{prefix}.feedback.designation.{des['name']}", f"{n} designation")
    for rid, role in d.get("roles", {}).items():
        want(f"modular.feedback.role.{role['name']}", f"{n} role {rid}")
        want(f"modular.feedback.role.{role['name']}.tip", f"{n} role {rid}")
        for dim, ax in role.get("axes", {}).items():
            want(f"modular.feedback.axis.{dim}", f"{n} role {rid} axis")
            if ax.get("bands"):  # a banded axis is a plate-style measure the bench explains
                want(f"modular.feedback.axis.{dim}.tip", f"{n} role {rid} axis")
            for b in ax.get("bands", []):
                want(f"modular.feedback.band.{b['name']}", f"{n} axis {dim} band")
                for t in traits(b):
                    want(f"modular.feedback.desc.trait.{t}", f"{n} axis {dim} band {b['name']}")
        part = role.get("part") or {}
        for v in part.get("variants", []):
            want(f"modular.feedback.variant.{v['name']}", f"{n} variant")
            want(f"modular.feedback.desc.variant.{v['name']}", f"{n} variant")
            if rid == d.get("wear_role") and d.get("names", {}).get("designations") and d.get("family") not in ("gun", "flintlock", "spring_gun"):  # named in an automatic build name
                want(f"modular.feedback.archetype.{v['name']}", f"{n} variant")
            for t in traits(v):
                want(f"modular.feedback.desc.trait.{t}", f"{n} variant {v['name']}")
for f in sorted((R / "data/feedback/gun_option").glob("*.json")):
    d = json.loads(f.read_text())
    for t in traits(d):
        want(f"modular.feedback.desc.trait.{t}", f"gun_option {f.stem}")
for f in sorted((R / "data/feedback/material").glob("*.json")):
    want(f"material.feedback.{f.stem}", "material")
for f in sorted((R / "data/feedback/gun_option").glob("*.json")):
    want(f"gun.feedback.fitting.{f.stem}", "gun_option")
missing = {k: w for k, w in need.items() if k not in lang}
for k, w in sorted(missing.items()):
    print(f"{k}  ({w})")
print(f"{len(missing)} missing of {len(need)} checked", file=sys.stderr)
sys.exit(1 if missing else 0)
