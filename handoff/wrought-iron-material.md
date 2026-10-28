# wrought-iron-material

Base: `main`.

## Goal
Playtest backlog [14]: vanilla iron is wrought iron (lang keys already say so). The material system should agree, and an elemental iron material should exist.

## Files
Read first: `src/main/resources/data/feedback/material/iron.json`, `cast_iron.json` (sibling iron alloy, for stat shape), `src/main/resources/assets/feedback/lang/en_us.json:3-12` (the Wrought Iron names), `docs/design_digest.md` if present.
Edit: `material/iron.json` and everything naming the id `feedback:iron` (grep `feedback:iron\b` and `"iron"` under `src/main/resources/data` and `src/main/java`; the material display lang key).
Create: `material/elemental_iron.json` plus its lang key.

## Spec
a. Rename the vanilla-iron material to `wrought_iron` (file `material/wrought_iron.json`, id `feedback:wrought_iron`, display "Wrought Iron"). Fix every reference (recipes, tags, ore_vein, loot, Java id constants, gametests). Keep its `forms` pointing at the minecraft items.
b. Add `material/elemental_iron.json`: pure iron, same JSON shape as iron.json. Pure iron: density 7.87, melting point 1538 C, soft and ductile (lower hardness and yield than wrought, higher ductility), magnetic with curie 770, conductivity higher than wrought (about 10 percent IACS, wrought has slag inclusions). Wrought iron: fibrous slag-bearing, tough, lower conductivity, weldable, rusts less than pure iron's reputation suggests but keep corrosion_resistance modest. Use `cast_iron.json` as the brittle end. Check the existing file for what unit/scale each field uses before choosing numbers. Give elemental iron `forms` only for items that already exist; leave the rest empty and say so.
c. Loose iron (the loose-rock / raw pickup of iron ore) drops elemental iron nuggets, not wrought iron nuggets. Find how loose rocks name their drop (grep `loose_rock`, `indicator` in `src/main/java` and data). If it needs an item that does not exist, STOP and write the question below instead of inventing one.
d. In the report, give the final stat table (old iron, wrought, elemental, cast) with one line of reasoning each.
Material properties live in JSON; never hardcode them in Java.

## Out of scope
New textures, new items, any other material, `src/generated/**`, docs.

## Done when
- [ ] `./gradlew compileJava` passes
- [ ] `grep -rn "feedback:iron\b" src` finds nothing stale
- [ ] `./gradlew runServer` boots if you can run it; say if not

## Questions

## Report
