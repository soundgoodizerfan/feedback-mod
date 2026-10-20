# Feedback — TODO

**This file holds only work still to do. It cannot contain anything that has been done.** No
`- [x]`, no "built and verified" sections, no decision records, no history. When an item closes,
delete its line (and its sub-bullets) in the same change. Decisions and their reasons go in the
design docs; history lives in git. Only `- [ ]` items, plus the minimum context to start one cold.

Source of truth for *what's next*; `feedback_philosophy.md` stays source of truth for *why*.

---

## Pick-up list

- [ ] **Graphite is an ore, not (only) a thermal process** (decided). `thermal_process/graphite.json`
  (2200–2600 Tu) is unreachable by any flame (blaze rod blown caps at 2100). Natural graphite: a
  mined, Slice 2 refractory (crucible lining, kiln/furnace ceiling). The 2200+ Tu process becomes
  *synthetic* graphite on Slice 10's arc furnace. Needs worldgen (none exists), an ore block, a
  deposit, and a `feedback_mechanics.md` §4-style writeup first. Ask about GTCEu prior art first.
- [ ] **Real models and textures** for every block and item. The user's call — never start
  unprompted. With them: cogs' diagonal large-to-small meshing, wear shown as block state
  (battered/spent), split turning part from fixed base on Hand Crank and Clutch.

---

## Document 2 — mechanics (`feedback_mechanics.md`)

Philosophy defers to it by name for each of these.

- [ ] Exact ranges and arithmetic for every unit (§17)
- [ ] `Su` load figures; linkage `St` pairs per machine (§17)
- [ ] Thermal model and item heat, written up from the code
- [ ] Vanilla vessel bands — smoker ceiling, Crude Blast Furnace floor, plain furnace span (§15)
- [ ] The six apparatus properties as numbers — range, resolution, accuracy, control, response, stability (§8)
- [ ] Drift and recalibration mechanism (§8, §9)
- [ ] Data links: 16-block range, client-drawn cable, cable item cost, no path check (§13)
- [ ] Per-energy sensor/actuator pairings, full matrix (§10)
- [ ] Overrun band widths per machine and per material (§6)
- [ ] Noise model — irreducible floor, learnable-in-aggregate, narrows but never collapses (§8)

## Document 3 — content

- [ ] Seed from slice 1's 12 items / 7 machines, then slice 2

---

## Electricity (`feedback_mechanics.md` §6)

**One wire block, any metal** (approved 2026-10-20; breaking change, no migration):
- [ ] Replace `copper_cable` with one `wire` block: material in the block entity, `MadeOf` on the item, tint and name from the material, creative tab lists every material with `ampacity`
- [ ] Wire recipes output a stack carrying `MadeOf`

**Material ingredient** (approved 2026-10-20; the recipe system has no data-driven way to ask "any item whose material has property P over x"):
- [ ] NeoForge `ICustomIngredient`: form or tag plus a material predicate (property, min/max); `getItems()` enumerates matching stacks from the material table so the recipe book and JEI cycle through them
- [ ] JEI: ingredient slot tooltip states the predicate ("any wire, ampacity at least x")
- [ ] Material table reaches the client before ingredients resolve (check `ClientMaterials` ordering)

**See in a client** (built and game-tested, never seen by eye):
- [ ] Heating coil under a crucible on a fine dynamo; copper coil wearing when cranked
- [ ] Motor-generator set (MV line, fine motor, shaft, coarse dynamo) stepping down to LV; motor slows under load
- [ ] Electroplating: hanging by face (top anode, side cathode) reads; tooltip
- [ ] Controller switching a coil through a breaker; tin fuse blowing on an overloaded line

**Open:**
- [ ] **[OPEN]** Sag curve and source current cap, blocked lower-voltage source, amps per sink per wire (§6.2)
- [ ] **[OPEN]** Band anchors at HV/UHV; series-stack capping (§6.3)
- [ ] **[OPEN]** Shape of the under-voltage slow-down; over-voltage per-unit scale (§6.5)
- [ ] **[OPEN]** No coil reaches smelting heat (steam-engine dynamo holds a vessel near 290 Tu). Decide when played.
- [ ] **[OPEN]** Cell never depletes (real cell eats its anode). Decide with the chemistry slice (§6.6).
- [ ] **[OPEN]** Electrostatic machine's band — built at LV because anchors are open.
- [ ] **[OPEN]** Leyden jar on the same run as a sink steals from it; check in play whether that reads as a visible mistake.
- [ ] **[OPEN]** Electromagnet ignores Curie point (`curie_temperature` already in material table); hot iron pulled as if cold.
- [ ] **[OPEN]** Motor over its band only spins faster; should the winding take over-voltage as condition and heat?
- [ ] **[OPEN]** Motor drives one way only; reversing (swap leads) unbuilt.
- [ ] **[OPEN]** A plated coat is read by modular tools and armour only (corrosion, armour skin hardness); optical sorter and plated contact conductivity still read nothing.
- [ ] Electrolytic cell's `[OPEN]`s (§6.14): conductive electrolyte, concentration instrument, current density burning a deposit, gas capture.
- [ ] **[OPEN]** Breaker has one trip rating (a copper cable's); discrete ratings wait on a second cable metal.
- [ ] **[OPEN]** Mend a degraded wire, or replace only? Built replace-only.
- [ ] **[OPEN]** Bare conductor shorting to neighbours (§6.4 stage 1): only the shock is built.
- [ ] **[OPEN]** Voltage rating per band on a wire; insulation as an item rather than a cable flag.
- [ ] **[OPEN]** Parallel sources: current routed along each sink's shortest path can overload one wire block. Revisit if a layout shows it.

---

## Rotation and wear

- [ ] **Flywheel: see in a client** — rides a Mechanical Hammer's blows, coasts a run down slower.
  **[OPEN]** rim speed limit from its metal (`yield_strength` over density): over it the rim strains and wears, never bursts.
- [ ] **Bearing as its own part.** The built one (a second `ShaftBlock` at lower drag) is the wrong
  shape — a bearing is a support the shaft turns in. Support-block vs unsupported-span rule
  undecided (`docs/feedback_machine_shop_spec.md` §4.1).
- [ ] **[OPEN] Does gearing need an ongoing cost?** Gearing *down* to reach a hardness floor is free apart from the cogs (§17).
- [ ] **[OPEN] No repair.** Condition only falls; recovery is break-and-replace. Likely answer: a
  replaceable **Hammer Head** item, the same slot a better head would raise the ceiling with.
- [ ] **Overspeed trip has no natural trigger.** Nothing in the roster reaches 48 RPM except by
  `driveExternally`; wait for a bigger source.

---

## Heat, vessels, materials

- [ ] **[OPEN] Per-material heat capacity** (TFC has it; deferred). Worth it once a player must choose which of two hot things to carry first.
- [ ] **Flint and steel takes steel**, so the Nether (blaze rod) opens at the end of slice 1. Not done.
- [ ] **Replace vanilla furnace/smoker/blast furnace in generated structures** (villages etc.) with Feedback's own — a structure-processor swap, not a mixin.
- [ ] **[OPEN] No steam bucket.** Add only when something must carry steam by hand.
- [ ] **[OPEN] Steam engine has flat output, not a throttle.**
- [ ] **[OPEN] Pump head.** Fluids have no pressure model; revisit if pumping against a column becomes a decision.
- [ ] **Nothing needs elevated pressure yet** — no `ThermalProcess` has a pressure field.
- [ ] **[OPEN] Bad alloy mix can't be corrected** — no dilution or partial extraction; crucible is stuck.
- [ ] **[OPEN] Dipped item that can't stand the temperature** has no consequence beyond being hot.
- [ ] **Casting recipes missing:** `cast_iron`, `tin_ingot`, `zinc_ingot`, `ceramic`, `kaolinite`, `firebrick`, `graphite`.
- [ ] **No advancement unlocks** for cast iron, brass, bronze, tin, zinc, glass, ceramic, kaolinite, firebrick, lead, copper tubing, quartz glass, graphite.
- [ ] **No survival source for tin, zinc, lead or kaolinite** — waits on worldgen (`docs/feedback_progression_roadmap.md` §4).
- [ ] **Glass tubing.** `Casting` already keys on mold shape (bullet mold); needs a tube mold and a `casting/` entry.

---

## Fittings and data links

- [ ] **`Adapter` has no concrete implementer.**
- [ ] **Removing a fitting** (crowbar-style interaction).
- [ ] **Temperature sensor has no crafting recipe.**
- [ ] Refractory lining: not honoured by `ThermalVesselBlockEntity`, no Jade line.
- [ ] **Data link overlay unverified by eye** — attach a sensor, link it, read Jade, confirm `DataNodeRenderer` draws through walls.
- [ ] Link hover/selection colours (CFM's success/error/default, selected-node highlight).
- [ ] `DataNode.getMaxNodeLinks()` cap of 4 is a made-up number.

---

## Machine shop

- [ ] **Removal overruns are spoil-only** (`steel_swarf`); a degrade tier wants named items (document 3).
- [ ] **Clutch's data-node overlay may not draw under Flywheel.** Harmless until a node box is needed.
- [ ] **Threaded rod** (rolled vs cut) and **Gear Cutter** — see `docs/feedback_machine_shop_spec.md` §6.
- [ ] **Drill Press auto-feed**: Crank Linkage + Clutch already does it — probably just document it.

---

## Hazards and catch-up (`feedback_mechanics.md` §7, §8)

- [ ] **See it.** Hot ingot bare-handed and with tongs; hot crucible bare and gloved; shut boiler
  under charcoal weeping, scalding, bursting; bare live wire; crucible over a lit firebox left
  out of range. Retune `CONTACT_*_TU`, `GLOVE_EXPOSURE`, `TONGS_EXPOSURE`, `BOILER_PU_PER_TU_OVER`, `BOILER_SEEP_*`.
- [ ] **Relief valve** fitting on the boiler (`getPressure()` is `Pu` now): weeps at a set pressure, never reaches danger, paid in steam and a scald. Relief only, no regulator (ruling 2026-10-13).
- [ ] **Pressure instrument** for the boiler and the Pressure Vessel.
- [ ] **Tongs and gloves**: no unlock advancement, no JEI entry, no Jade line.
- [ ] **A hot piece does not ignite what it touches** (philosophy 9 permits it).
- [ ] **[OPEN]** Galvanometer on a bare live wire shocks the hand; an insulated probe is nicer. Decide with the instruments.
- [ ] Offline catch-up not applied to rotation/electric runs, Pressure Vessel leak, Condenser tank. Add if one visibly needs it.
- [ ] Improved Furnace has its own second `Heat.tick` path; its catch-up burns the single fuel slot only. Look when next opened.

---

## Guns (`docs/feedback_guns_spec.md`)

- [ ] **The mixer** — a machine mixing powders by physical operation; the Reloading Press then
  takes a finished charge (`CHARGE`/`CHARGE_B`, `Cartridge.charges`). No blend recipe until it exists.
- [ ] **Animation library** — compare before committing to GeckoLib (spec §7.2).
- [ ] **See in a client** — collapsed gun parts (names, tint, machining), Stand drawings and fitting rows, Assembly Bench screen, Gun Metal JEI cards, jar round tooltip, held reload.

---

## Playtest notes -- 2026-10-15

- [ ] **Re-check by eye: a tube on a ball bearing swings freely** (the self-join crash fix probably cured it).

**Vehicle building rework: frames assemble into cells** (`docs/feedback_vehicles_spec.md` §4, *Bridging to Sable -- cells, again*). Spec written; code not started.
- [ ] Read <https://deepwiki.com/ryanhcode/sable/1-overview> (rate-limited 2026-10-15) before code.
- [ ] **[OPEN] Cell state encoding**: the plane family again, or 2x2x2 octants for physics with exact shapes per position for entities. Decide before code.
- [ ] Prune unused vertices and loose parts on assembly -- `BlockSubLevelAssemblyListener.afterMove` is the hook; under the voxel route loose blocks simply are not gathered.

**Later programmers (write down, do not build):**
- [ ] Cards made of cards (functions); a delay card; addressable memory; loops. **[OPEN]** against philosophy 13 "capability does not tier" and the stateless-controller ruling -- see `docs/feedback_controller_spec.md` §8.
- [ ] A `NOT` gate (one input) on the Logic card. Greater/Less could become one Compare card the same way, if wanted.

## Playtest notes -- 2026-10-16


**Vehicles -- ideas** (author, 2026-10-16):
- [ ] Later experiment: a tire as its own rolling body of 1/16 boxes on a motored hinge (spec §5 *Guard rays*). Bumpy by construction.
- [ ] Hub with three points: the axis, plus two locked to the frame (a real hub), so drive transfer has a fixed frame of reference.
- [ ] **See in a client:** gizmo click and drag, alignment lines, Select box, Extrude, the symmetry screen and mirrored edits, the Pivot Setter on a clicked selection. Main Assembly-style editing beyond this still waits on the author's friend's description.

**Modular items** (author, 2026-10-16):
- [ ] **Modifiers** beyond tips and coats: more choices per part, not stats. Coat **friction** has no reader; armour **corrosion** has no consumer (wearing plate under water); the cell sizes a modular piece as one ingot (size by baked mass); a tool made without a tip cannot be given one.
- [ ] **Rule (author)**: every measured quantity gets a top "exact truth" precision tier, built or not, which the debug helmet reads. Write into philosophy §8 / mechanics, then make the helmet read modular figures in full.
- [ ] Explain the adjectives ("head-heavy" etc.) somewhere a player finds them.
- [ ] A JEI pass over everything else (fuel, alloy, dip still have no page; fuel now reads materials, mechanics 5.7).

**Materials** (author, 2026-10-16):
- [ ] **Material forms by tag**: fuel moved onto materials (mechanics 5.7) and modded logs/planks lost the `#minecraft:logs`/`planks` route; let a material form name a tag so `Material.find` reaches modded items. Also bamboo has no combustion yet (needs its own yields).
- [ ] **Wrought iron** as its own material; decide what plain `iron` is (today generic iron, hardness 6). Cast iron's niche waits on cookware (§18a parked).
- [ ] Tempered steel's figures (hardness 20, toughness 40, service 400) are placeholders between steel and hardened steel; it generates no forms yet.

## Vehicles

- [ ] **Controls: see in a client.** Driver's Seat, Math card, throttle, steered hinges and hub
  brakes are built and compile, never played. Check: sit, a W card opens the throttle; a `D - A`
  Math card into the frame's steer channel turns kingpins both ways, the same way on each side;
  parked steering barely moves, rolling it does; a full brake skids an iron-padded hub, half does
  not; a braked driven hub stalls the engine. Retune `STEER_*`, `BRAKE_CLAMP_SU`.
  - [ ] **[OPEN]** Card targets are block positions; check a program survives lifting and lowering a car.
  - [ ] **[OPEN]** Steering ratio: a banded choice on the card (spec ruling) or the Math card's scalar.
  - [ ] **[OPEN]** Brake pad wear: braking is correct use, so wearing pads would be a fourth wear exception. Needs a ruling.
  - [ ] Power steering (assist `Su` from the drivetrain).

  Still owed after that: springs and shocks, a differential (a straight axle turns both wheels
  as one rod today, so they scrub once steering exists), a gearbox. Not yet
  played: wheelspin in the air, a stall on a slope, neutral downhill.
- [ ] **Guns on vehicles** (`docs/feedback_guns_spec.md` §8). Wants a mount. The car drives now; after controls.

## Modular items (`docs/feedback_modular_items_spec.md`)

Tools, melee and ranged weapons, armour as baked designs; vanilla tools and armour replaced. Build order in spec §10.

- [ ] **Gun migration, what is left** (spec §8): in-game check of the collapsed parts and of assembling and refitting through the core (see the checklist in the migration commit); repair of a gun by its stock (spec §8.2, **[OPEN]**); the Gun Stand screen still lists nothing from `modular_type/gun.json` (it is drawn by hand); the part forms in the Gun Stand and JEI show the shared part item without a material.
- [ ] Impact pendulum (toughness) and strike post (damage per blow) as blocks (spec 6).
- [ ] Spec §11 `[OPEN]`s: section and as-built part shape; pry bar and prospector's pick; thrown trident damage, a crossbow's draw time and a lining's heat (vanilla reads constants).
- [ ] **See in a client:** assemble each type at the table and the bench, shift-use the grindstone for the edge, tip and stud a piece, plate one, break one, mend it, find vanilla gear converted in a chest and on a zombie. Tint and sprites are placeholder.
- [ ] **Later, footnote only:** rework vanilla enchanting to enchant *parts* rather than whole items (author, 2026-10-15).

## Approved ideas, not yet built

Written down at the author's word; prior-art question still unasked for each.

- [ ] **Shield as a thermal and kinetic sink**: a blow ends as heat in the plate; Nether fixed-Tu sources make plate material a choice. **[OPEN]** `ItemHeat` or `ThermalBody` while raised.
- [ ] **Electrified perimeter**: fence wire over rating hurts what touches it, by properties (wet, metal armour, grounded), never mob id; rain raises conduction. **[OPEN]** damage type, how a fence reads touching.
- [ ] **Spring weapons from stored work**: spring steel stores `Fu`, released through `GunDesign.Launcher`. **[OPEN]** a `GunLayout` or a separate design.
- [ ] **Incandescence** — not to build yet. Above ~525 Tu a hot body emits block light in bands: melts ice, stops spawns; insulated vessels stay dark. Which blocks and bands still unasked.
- [ ] **Villager commissions** — later. Smith orders parts *by spec*, pays by closeness; repeat orders saturate.
- [ ] **Refined redstone superconductor** — late, hard line. Chain: purify, blend to stoichiometry (mixer), calcine, pack in sheath and draw/roll, draw-anneal cycles (path-dependent), reaction anneal under `Pu`. No drop up to critical current, then quenches and runs away. **[OPEN]** sheath metal, critical Tu.
