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
- [ ] **[OPEN]** Nothing reads a plated coat yet: corrosion (`corrosion_resistance`), optical sorter, plated contact conductivity.
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
  undecided (`feedback_machine_shop_spec.md` §4.1).
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
- [ ] **No survival source for tin, zinc, lead or kaolinite** — waits on worldgen (`feedback_progression_roadmap.md` §4).
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
- [ ] **Threaded rod** (rolled vs cut) and **Gear Cutter** — see `feedback_machine_shop_spec.md` §6.
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

## Guns (`feedback_guns_spec.md`)

- [ ] **The mixer** — a machine mixing powders by physical operation; the Reloading Press then
  takes a finished charge (`CHARGE`/`CHARGE_B`, `Cartridge.charges`). No blend recipe until it exists.
- [ ] **Animation library** — compare before committing to GeckoLib (spec §7.2).
- [ ] **See in a client** — Stand drawings and fitting rows, Assembly Bench screen, Gun Metal JEI cards, jar round tooltip, held reload.

---

## Playtest notes -- 2026-10-15

- [ ] **Re-check by eye: a tube on a ball bearing swings freely** (the self-join crash fix probably cured it).

**Vehicle building rework: frames assemble into cells** (`feedback_vehicles_spec.md` §4, *Bridging to Sable -- cells, again*). Spec written; code not started.
- [ ] Read <https://deepwiki.com/ryanhcode/sable/1-overview> (rate-limited 2026-10-15) before code.
- [ ] **[OPEN] Cell state encoding**: the plane family again, or 2x2x2 octants for physics with exact shapes per position for entities. Decide before code.
- [ ] Move a selected vertex voxel by voxel (gizmo arrows or arrow keys). Fold into the new editor rather than patch the old.
- [ ] Prune unused vertices and loose parts on assembly -- `BlockSubLevelAssemblyListener.afterMove` is the hook; under the voxel route loose blocks simply are not gathered.

**Later programmers (write down, do not build):**
- [ ] Cards made of cards (functions); a delay card; addressable memory; loops. **[OPEN]** against philosophy 13 "capability does not tier" and the stateless-controller ruling -- see `feedback_controller_spec.md` §8.
- [ ] A `NOT` gate (one input) on the Logic card. Greater/Less could become one Compare card the same way, if wanted.

## Playtest notes -- 2026-10-16


**Vehicles -- ideas** (author, 2026-10-16):
- [ ] Later experiment: a tire as its own rolling body of 1/16 boxes on a motored hinge (spec §5 *Guard rays*). Bumpy by construction.
- [ ] Hub with three points: the axis, plus two locked to the frame (a real hub), so drive transfer has a fixed frame of reference.
- [ ] Tools to place points precisely: snap parallel, align to another edge (joins the voxel-move item above).

**Modular items** (author, 2026-10-16):
- [ ] **Tips**: a hard material on the working edge of a tougher body -- strength without brittleness, little of it used. Not Silent Gear's (no tip on armour). Natural diamond gear becomes steel gear diamond-tipped where it has an edge; diamond is not a metal and is not forged.
- [ ] **Coatings and modifiers**, tied to material properties (plating already exists as a component -- a first reader). More choices per part, not stats.
- [ ] Changing an edge only at a grindstone (sneak-use), costing durability -- not scrolled on the spot.
- [ ] **Parts as one item each** (`feedback:pickaxe_head`, material in a component), replacing per-material part items; guns collapse the same way when they join the core. Clears JEI.
- [ ] **Blueprint table for tools**, like the Gun Stand: optional except for complex types (crossbow); predicts the figures before crafting.
- [ ] **Rule (author)**: every measured quantity gets a top "exact truth" precision tier, built or not, which the debug helmet reads. Write into philosophy §8 / mechanics, then make the helmet read modular figures in full.
- [ ] Explain the adjectives ("head-heavy" etc.) somewhere a player finds them.
- [ ] JEI for the modular system is thin; then a JEI pass over everything (fuel, alloy, dip still have no page).

**Materials** (author, 2026-10-16):
- [ ] **Fuel into the material files**: combustion figures on `material/*.json`, shared with `propellant` where the physics is the same; retire the `fuel/` table. Ask prior art first.
- [ ] **Wrought iron** as its own material; decide what plain `iron` is (today generic iron, hardness 6). Cast iron's niche waits on cookware (§18a parked).
- [ ] Tempered steel's figures (hardness 20, toughness 40, service 400) are placeholders between steel and hardened steel; it generates no forms yet.

## Vehicles

- [ ] **Controls -- a player at the wheel.** Cars drive (playtest 2026-10-16): engine, drive
  lines, tires, axles all work in a client. What is left is driving them from a seat. Author's rulings 2026-10-14: a **Driver's Seat** modelled on Aeronautics' Linked
  Typewriter (whole keyboard in, linked to the ordinary controller -- **no separate vehicle
  controller block**; whatever the controller needs, it gets); steering is a **steered hinge**
  (servo-driven kingpin, player-built tie rods); brakes are a **hub setting** (material sets
  friction); the **full analog card set**.

  *Built, compiles, not yet played in a client:*
  - `control/program/MathNode` (+ - x / min max, literal right side); `SensorNode.channel`,
    `ActuatorNode.channel`; `Quantity.CONTROL`; port typing widened (`ProgramGraph.wireError`:
    Number from sensors and math, actuator takes either); `ProgramEvaluator` writes Boolean or
    Float through `ActuatorWriter`.
  - `control/Analog` (vehicle parts set to a level, by channel) and `control/ChannelSource`
    (a node with several readings). `ControllerBlockEntity` reads sensors per card (channel) and
    writes Float only to an `Analog` target -- a number into a factory switch moves nothing.
  - Programmer: links a seat (quantity CONTROL, default key W) and Analog targets; the link reply
    carries the channel. `ProgrammerScreen`: Math card (click centre cycles op, literal box),
    seat sensor row 2 = "key: W" (click, press a key), analog actuator row 2 cycles its input.
  - `vehicle/seat/`: `DriverSeatBlock` (use to sit), `DriverSeatBlockEntity` (DataNode +
    ChannelSource; held keys ramp 0-1 over `SEAT_RAMP_TICKS`), `DriverSeatEntity` (Create seat
    shape; in `data/sable/tags/entity_type/retain_in_sub_level.json` so it stays in the plot),
    `SeatKeyPayload`, `client/DriverSeatKeys` (captures keys while seated; sneak, Escape, chat,
    F-keys kept). Registered in `FVehicles` (`SEAT`, `SEAT_ITEM`, `DRIVER_SEAT_ENTITY`,
    `SEAT_RIDE`), payload in `Feedback`, creative tab, placeholder slab model, recipe, loot, lang.
  - Engine is `Analog`: channel 0 throttle; torque and target speed scale by
    `ENGINE_IDLE_FRACTION + (1 - idle) x throttle`; saved; defaults open, so stands are unchanged.

  *Next, in order:*
  1. **Frame as `DataNode` + `Analog`**: channel 0 steer (-1..1), 1 brake (0..1).
  2. **Steered hinge**: `Frame.Pivot` gains `steers`; Pivot Setter cycles hinge -> steered hinge
     -> ball -> locked -> plain. `FrameBodies.pivots()`/`anchors()` keep a vertex -> handle map
     for steered pivots; each tick `setMotor(ConstraintJointAxis.ANGULAR_X, level x
     STEER_MAX_ANGLE, stiffness, damping, true, armTorque)` -- max force is the driver's arm, so
     steering is heavy at rest (spec *Steering*); power assist later.
  3. **Brakes**: `Frame.Tire` gains a brake material (Pivot Setter sneak-use with an off-hand
     block on a hub). In `Drivetrain.physicsTick`, a free hub's longitudinal friction is capped at
     `brake x friction x clamp / h`; a driven hub's brake is a resisting push on the shaft.
  4. A game test: a seat key held -> card -> throttle opens; steer level -> a steered car's
     heading changes. Build near origin (memory: sable-physics-game-tests).
  5. Docs: spec §5 *Controls*/*Steering* as built; philosophy §13 note that arithmetic cards
     exist and only `Analog` parts take a number; `feedback_controller_spec.md` new card; CLAUDE.md
     package rows (`vehicle/seat/`, `control/Analog`).
  6. Then: run all game tests, `./build-and-deploy.sh`, try it in a client.

  Still owed after that: springs and shocks, a differential (a straight axle turns both wheels
  as one rod today, so they scrub once steering exists), power steering, a gearbox. Not yet
  played: wheelspin in the air, a stall on a slope, neutral downhill.
- [ ] **Guns on vehicles** (`feedback_guns_spec.md` §8). Wants a mount. The car drives now; after controls.

## Modular items (`feedback_modular_items_spec.md`)

Tools, melee and ranged weapons, armour as baked designs; vanilla tools and armour replaced. Build order in spec §10.

- [ ] Spec §10 step 8: guns migrated onto the core. Waits until the core has been played.
- [ ] Impact pendulum (toughness) and strike post (damage per blow) as blocks (spec 6).
- [ ] Spec §11 `[OPEN]`s: section and as-built part shape; pry bar and prospector's pick; thrown trident damage, a crossbow's draw time and a lining's heat (vanilla reads constants).
- [ ] **See in a client:** assemble each type at the table and the bench, scroll the edge, break one, mend it, find vanilla gear converted in a chest and on a zombie. Tint and sprites are placeholder.
- [ ] **Later, footnote only:** rework vanilla enchanting to enchant *parts* rather than whole items (author, 2026-10-15).

## Approved ideas, not yet built

Written down at the author's word; prior-art question still unasked for each.

- [ ] **Shield as a thermal and kinetic sink**: a blow ends as heat in the plate; Nether fixed-Tu sources make plate material a choice. **[OPEN]** `ItemHeat` or `ThermalBody` while raised.
- [ ] **Electrified perimeter**: fence wire over rating hurts what touches it, by properties (wet, metal armour, grounded), never mob id; rain raises conduction. **[OPEN]** damage type, how a fence reads touching.
- [ ] **Spring weapons from stored work**: spring steel stores `Fu`, released through `GunDesign.Launcher`. **[OPEN]** a `GunLayout` or a separate design.
- [ ] **Incandescence** — not to build yet. Above ~525 Tu a hot body emits block light in bands: melts ice, stops spawns; insulated vessels stay dark. Which blocks and bands still unasked.
- [ ] **Villager commissions** — later. Smith orders parts *by spec*, pays by closeness; repeat orders saturate.
- [ ] **Refined redstone superconductor** — late, hard line. Chain: purify, blend to stoichiometry (mixer), calcine, pack in sheath and draw/roll, draw-anneal cycles (path-dependent), reaction anneal under `Pu`. No drop up to critical current, then quenches and runs away. **[OPEN]** sheath metal, critical Tu.
