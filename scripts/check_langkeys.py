#!/usr/bin/env python3
"""List lang keys the datapack needs but en_us.json lacks. Exit 1 when any are missing.
Covers modular_type (role name+tip, variant name+desc, trait desc, type name) and gun_option (item name by form).
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
    for rid, role in d.get("roles", {}).items():
        want(f"modular.feedback.role.{role['name']}", f"{n} role {rid}")
        want(f"modular.feedback.role.{role['name']}.tip", f"{n} role {rid}")
        part = role.get("part") or {}
        for v in part.get("variants", []):
            want(f"modular.feedback.variant.{v['name']}", f"{n} variant")
            want(f"modular.feedback.desc.variant.{v['name']}", f"{n} variant")
            for t in traits(v):
                want(f"modular.feedback.desc.trait.{t}", f"{n} variant {v['name']}")
for f in sorted((R / "data/feedback/gun_option").glob("*.json")):
    d = json.loads(f.read_text())
    for t in traits(d):
        want(f"modular.feedback.desc.trait.{t}", f"gun_option {f.stem}")
missing = {k: w for k, w in need.items() if k not in lang}
for k, w in sorted(missing.items()):
    print(f"{k}  ({w})")
print(f"{len(missing)} missing of {len(need)} checked", file=sys.stderr)
sys.exit(1 if missing else 0)
