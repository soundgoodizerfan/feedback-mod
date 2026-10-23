---
name: add-unit
description: Add a new physical unit type to Feedback's core/unit package (record, lang key, codecs, readout). Use when a new quantity such as Pu, Qu or Eu needs to become a type.
disable-model-invocation: true
argument-hint: <UnitName> <symbol> <dimension, e.g. "pressure">
---

# add-unit

Arguments: `$ARGUMENTS`. The authoritative checklist is the javadoc on `src/main/java/io/github/soundgoodizerfan/feedback/core/unit/Units.java`; read it first. Also read `feedback_philosophy.md` and `feedback_mechanics.md` (the edit hook requires it).

## Steps

1. Confirm the unit is in philosophy §17 or ask whether it should be. Do not mint units for wear/condition (a percentage).
2. Write `core/unit/<Name>.java`: a record, one component, `implements Unit`, `raw()` and `unitKey()` one line each. Base units name the component `value`; compounds name it for the dimension. Copy `Su` (base) or `Drag` (compound). Include the GPL header the sibling files carry.
3. Add `feedback.unit.<name>` to `src/main/resources/assets/feedback/lang/en_us.json`.
4. If it appears in a datapack table: use `Units.codec`/`intCodec` and `Units.streamCodec`/`intStreamCodec`. Never hand-roll.
5. If a player reads it as a figure: add a `Quantity` and format through `Readout.figure`.
6. No arithmetic on the record. No registration. Do not seal `Unit`.
7. Update the unit count and list in `docs/source_layout.md` (`core/unit/` row).
8. `./gradlew build`.
