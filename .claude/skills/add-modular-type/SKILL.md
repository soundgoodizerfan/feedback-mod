---
name: add-modular-type
description: Add a new modular type (a tool, weapon, armour piece, worn curio, hand tool or the like made at the Tool Table from parts) to Feedback. Use whenever the user wants a new thing the Tool Table should make, a new family, a new part item, or to turn a fixed item into a modular one.
argument-hint: <type id> <what it is> [family]
---

# add-modular-type

**Read `docs/feedback_modular_items_spec.md` sections 2 (the model), 4.2 (Tool Table), 5 (types) and the sub-section nearest your type (5.2 armour, 5.5 hand tools, 5.6 gauntlets) first.** The type is data; the bake and the item are code only when no existing family fits.

## First decision: reuse a family or add one

A type file picks a `family` (`ModularType.Family`: DIG, BLADE, MACE, SPEAR, BOW, CROSSBOW, SHIELD, ARMOUR, SHEARS, GUN, CHARGE, HEAD, DISH, HAND, GAUNTLET). Ask what the finished item *does*, then ask whether an existing family's bake already says it:

- Reuses a family (a new sword-like blade, a second hand tool, another armour slot): **data + one registry line + lang + a model**. Do not add Java.
- Does something no family bakes (a worn curio, a new figure): add a **family** (below). A family is one `case` in `ModularBake.bake`, not a new pipeline. Widen an existing family before forking one.

Guns, charges and dishes have their own bakes and tables; do not model a new type on them unless it is one.

## The files, in order

1. **Type file** `src/main/resources/data/feedback/modular_type/<id>.json`. Copy the nearest sibling (`hand_hammer.json` for a one-head tool, `helmet.json` for armour with a lining, `shield.json` for variants, `gauntlet.json` for a one-role worn piece). Fields: `family`, `slot` (optional, defaults MAINHAND; only matters for equipment attributes), `wear_role`, `primary`, `wear_litres`, `strike`, `roles`, `patterns`, `defaults`, `vanilla`. A role has `name`, `forms` (the part item's form id), `litres`, `layer`, and for a shaped part a `part` block: `item`, `shape` (cells), `stock` (forms of raw stock; `*_planks`-style wildcards match the end of the form name, so `*_carpet` takes every carpet), optional `variants`. A role with no `part` is raw stock (a handle: `stick`, `rod`, `bone`). `patterns` are the role letters the item may be built from; `ToolTableAssembly` infers the type from the parts, so **a type's shaped part item must be unique to it**. Reusing another type's part item makes the table ambiguous and the load-time audit fails.
2. **Part item** (one per shaped role): add its id to the `PARTS` list in `registry/FModular.java`. That is the whole registration.
3. **Item** in `registry/FModular.java`: `ITEMS.register("<id>", () -> new ModularXxx(ModularStacks.properties()))`. **The item id is the type id.** Use `ModularItem` if nothing special is needed, else a subclass (see `ModularHandTool`, `ModularArmor`, `ModularGauntlet`). Forward `getName` to `ModularStacks.name`, `appendHoverText` to `ModularTooltip.add`, `inventoryTick` to `ModularStacks.tick` (`ModularItem` already does).
4. **Lang** (`assets/feedback/lang/en_us.json`): `item.feedback.<id>`, `item.feedback.<part>`, `form.feedback.<part>` (`"%s Gauntlet Plate"`), `modular.feedback.desc.part.<part>` (the Tool Table's blurb). Any new figure: `modular.feedback.fig.<name>` and `.tip`. A variant: `modular.feedback.archetype.<variant>`.
5. **Models** (`assets/feedback/models/item/`): `<id>.json` and `<part>.json`, `item/generated` (or `item/handheld` for a held tool), layers `feedback:item/modular/<id>_0`, `_1`. Placeholder art may reuse an existing texture; say so in the report.
6. **Tint and creative tab** are automatic for anything `FModular.items()` returns.

## Adding a family (only if the bake is new)

- `ModularType.Family`: add the constant with a one-line javadoc.
- `ModularFigures.Fig`: append new figures at the **end** with a javadoc each. Figures are stored by ordinal, so order matters only for saved items, and breaking changes are accepted (CLAUDE.md).
- `ModularBake.bake`: one `case` in the family `switch`. Sum `mass(role, part, material)` over parts, then write figures. Take the wear step from `worn`. Use the shared curves (`GearTuning.saturate` is the saturating `x / (x + half)`), and **put each new constant in `ModularTuning` or `FTuning`, never inline**.
- `ModularCause.java`: add the new figure to the `switch` that says which part a figure is down to, or the tooltip's "caused by" breaks (the switch is exhaustive and will not compile without it).
- Anything that reads vanilla components from figures (`ModularStacks.write`) needs a `case` only if the type sets attributes or a tool component.

## Items worn as a curio

A worn modular item is a Curios item: add its id to the slot's `data/curios/tags/item/<slot>.json` (`hands`, `bracer`, `belt`, `back`). To make combat read it, `CombatGear.worn` must include it (`s.getItem() instanceof Xxx`) and `CombatGear.of` must sum its figure. Add a `CurioRenderers` entry (`reg(item, ...)`) for the worn look, tinted from the design's plate (`ModularStacks.design(stack).part("P").material()`).

## Rules that bite

- **Properties, not ids** (digest, hard rule). Decide behaviour from material traits and figures (`Material.Trait.FIBROUS`, `Grip.insulates`), never `material == wool`. A glove is a gauntlet with a fibrous plate, not a type of its own.
- **Do not duplicate a fixed item.** If a fixed item (`GloveItem`, `HandHammerItem`) is being made modular, delete it, its recipe, its advancement, its lang and every reference in the same change. Breaking changes are fine; no aliases.
- **Broken is not gone.** A modular item at its last point stops working (`ModularStacks.broken`) and can be reworked. Check it wherever the item gives an effect.
- **No numbers on the player.** Figures read as adjectives on the tooltip unless a figure is a thing the player acts on (digest, philosophy 8).
- **Edit the docs in the same change:** spec section (`feedback_modular_items_spec.md`), `docs/source_layout.md` if a file or package is new, a wiki page for the player-visible behaviour, and `TODO.md`.

## Verify

1. `./gradlew compileJava`. The `ModularCause` and family `switch` errors are the checklist for a new family.
2. The load-time consistency check (`ToolDesigns.consistency`) runs at datapack load and fails on a part shape two types share or a role form no material has. Read its output in the log when the client starts.
3. **Do not run game tests or `runGameTestServer`** unless asked (CLAUDE.md). Hand the user an in-game checklist: shape each part at the Tool Table, assemble, read the name and tooltip, then use the item and break it.
4. Run `./build-and-deploy.sh` only after the user confirms it works, and only when asked to (it pushes the test pack).

## Report

State: the family (reused or new), the files touched, the placeholder art, the figures added, what was not checked, and the in-game checklist.
