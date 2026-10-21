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
- [ ] **[OPEN] Ceramic, kaolinite, firebrick and graphite have no casting:** none of them melts (they are `thermal_process` products), so there is no fluid to pour. What shapes them (a mould of wet clay before firing?) is undecided.
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
- [ ] **See in a client** — collapsed gun parts (names, tint, machining), Gun Table screen, Gun Metal JEI cards, jar round tooltip, held reload.

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

**Vehicles beyond cars -- ideas** (author, 2026-10-20, from reading Create Simulated, `../Simulated-reference`; write down, do not build). Each carries its Minecraft lens in the line.
- [ ] **Named assembly failure.** The Lift (and frame assembly) says *why* it refused: "frame touches two bases", "bearing has no far side". Simulated's assembler lever shows the exception.
- [ ] **Balance gauge.** While the Lift holds a frame: centre of mass, total `Mu`, and (once flying parts exist) lift or thrust against weight. A blueprint predicts, an instrument measures.
- [ ] **Clipboard copy of settings** for steered hinges, hub brakes and cards, so a symmetric car is set once.
- [ ] **Terrain friction.** A tire reads friction from the block under it (material file or block property), not a constant: ice, blue ice, slime (canon, best grip), soul sand, honey, mud. Vanilla already gives these blocks different friction, so the world supplies the lens. A tire with no ground under it goes to a lifted-up state with no force (wheelspin in the air).
- [ ] **Tire from data.** Any item whose material and form qualify is a tire, no tire id. Hold the figures in the material file.
- [ ] **Hot-air or steam envelope.** Flood-filled airtight enclosure (airtight as a material property with a leak rate per face, not a tag-only yes/no). Lift = (ambient air density - gas density) x `Mb` x g, so the burner is a `HeatSource` burning real fuel and the envelope a `ThermalBody`. Fill and empty lag the heater (Simulated: 9 s). A breach leaks and shows a gust. Heaters on one balloon merge. Steam is the second gas, from the Boiler. The Nether's ambient (`AmbientTable`) and altitude change the lift. Overrun: a burner left on overheats and wears the fabric. **[OPEN]** how a hole is repaired; whether lift should depend on altitude pressure.
- [ ] **Propeller.** Blade count and radius come from what is built (Simulated's sail count, thrust ~ sails^1.5). Add what it lacks: thrust falls as airspeed nears tip speed, and a named pitch band (feather, coarse, fine) that stalls when wrong. Overspeed tears blades.
- [ ] **Thrusters.** Burn fuel, so they wait for petrochemistry (`docs/feedback_progression_roadmap.md` Slice 7). No fuel-free thruster. **[OPEN]** liquid or solid propellant; whether thrust vectoring is a hinge on the nozzle.
- [ ] **Wings and fins.** Lift and drag per welded plate from its normal and airspeed squared, the way `Frame.vdrag` already reads drag; stall past an angle. The same plates as rudders and keels for boats.
- [ ] **Gyro stabiliser and passive levelling.** A spinning flywheel resists tilt (`Inertia`: heavier and faster is steadier, with spin-up time and shaft drag). A pendulum hinge keeps a thrust axis upright, as Simulated's gyroscopic and smart propellers do, by geometry only.
- [ ] **Hulls and ballast.** Mean density under 1 `Mu/mB` floats; a sealed cell is `Mb` of air displacing water; ballast fills in named levels; depth crush reuses `PressureBurst`.
- [ ] **Vehicle instruments.** Altimeter (Y and `Pu`), airspeed, attitude, tachometer, depth gauge, each a carried instrument or a fitted `Sensor`, and a `DataNode` feeding cards. Autopilot is then only cards (analog is allowed in vehicles). Simulated has altitude, velocity and gimbal sensors.
- [ ] **Hitch and docking.** A second base is a second body (trailers, already listed). A hitch has a rated load and wears or snaps when overloaded. Docking is a mechanical coupling carrying a rated load; a shaft may cross a docked pair through a clutch. **Not** Simulated's energy and fluid pass-through, which would make electricity a universal currency.
- [ ] **Rope and winch**, and a **spring** that snaps when overextended (Simulated: 0.75 s).
- [ ] **Leak marker.** Particles or heat shimmer at an envelope or hull breach.
- [ ] **Wheel suspension strength as a banded choice** (soft / stock / stiff), not Simulated's free scroll; ties to the springs and shocks already owed.

## Modular items (`docs/feedback_modular_items_spec.md`)

Tools, melee and ranged weapons, armour as baked designs; vanilla tools and armour replaced. Build order in spec §10.

- [ ] Impact pendulum (toughness) and strike post (damage per blow) as blocks (spec 6).
- [ ] Spec §11 `[OPEN]`s: section and as-built part shape; pry bar and prospector's pick; a crossbow's draw time and a lining's heat (vanilla reads constants).
- [ ] **Twin-headed tools, what is left** (spec §5.3, built 2026-10-20): wear is one pool for both edges (only the head used should wear: needs per-head damage state); a tip sets on the first head only; the Blueprint Table and JEI card for the twin type are unchecked in a client; family names (`family` is still the bake's enum).
- [ ] **Part variants, what is left** (spec §12, built 2026-10-20; raw-stock roles 2026-10-21, compile only): no variant return swarf (the stock-to-litres mismatch is not modelled); a rim variant is a trade in stock and mass only (a heavier shield slows nothing yet); armour studs and a lining have no variants; the Blueprint Table draws standard shapes only.
- [ ] **Forging by rhythm, what is left** (spec §4.1, played at the Tool Table since 2026-10-21): the Mechanical Hammer's pattern time (instant), its need of a hand hammer in the slot, and a cam pattern of its own (spec §4.1); deviation from the Press and grinding (§4); more patterns and more parts; a gametest of `ForgeRhythm.judge` if wanted.
- [ ] **Scale loss in the Hearth** (spec 4.2, 2026-10-21): stock left in a pan above its material's `oxidation_temperature` is only refused by the table once past its window; it should lose a share (overrun on the finished stock). Needs a flaw channel for raw stock. Hearth art is the Firebox's brick, placeholder.
- [ ] **Tool Table art** (spec §4.2): the six part items (`tool_tip`, `tool_binding`, `sword_guard`, `sword_pommel`, `shield_rim`, `crossbow_prod`) borrow existing sprites (gem lens, leather strap, the modular sword, shield and crossbow layers) tinted by material and want art of their own.
- [ ] **Gun and ammunition options** (author, 2026-10-21; spec §13): as many choices as tools have, each changing only figures its own side owns (gun: barrel, muzzle, action, launcher, sight, stock, fire control; round: projectile shape and jacket, charge load, case, payload). **Suppressor blast/crack split is built; the subsonic load is a light charge count, labelled on the Gun Table. Fittings are a data table (`gun_option/*.json`, verified in game).** Then the Gun Table, then ammunition shape and jacket, then the pneumatic, spring and steam launchers. Open questions in §13.7.
- [ ] **Signature behaviours, what is left** (spec §5.4, played and working 2026-10-21): the spear's hitbox inflation, attacking through non-solid blocks and a custom first-person pose; prospector's range per handle length and tint per ore family; bleed credits no one.
- [ ] **Archetypes, what is left** (spec §12, built 2026-10-21, compile only): open helm's wide vision and hauberk's bleed; the sledge does not mine stone faster; sprinting while drawing a shortbow; a one-handed pistol crossbow; confirm in a client that `LivingShieldBlockEvent` fires for blows the vanilla angle test would pass (tower and buckler depend on it); confirm brigandine's stud count; run the load-time audit on real data and read the log.
- [ ] **Enchantments on parts, not tools** (author, 2026-10-21): the trident is now a spear head variant and the mace a maul head variant, but enchant tags are per item, so every spear takes Loyalty/Riptide and every maul takes Density/Breach for now. Later an enchantment sits on a part, and only a trident head can carry the trident's, only a mace head the mace's. Spec §12 merge note.
- [ ] **Katana's trick** (author, 2026-10-21): an edge harder than its spine (hardened-steel pairing). Per-variant dig tags beyond "digs nothing" (a variant that digs a different tag) are not built.
- [ ] **Later, footnote only:** rework vanilla enchanting to enchant *parts* rather than whole items (author, 2026-10-15).

## Approved ideas, not yet built

Written down at the author's word; prior-art question still unasked for each.

- [ ] **Shield as a thermal and kinetic sink**: a blow ends as heat in the plate; Nether fixed-Tu sources make plate material a choice. **[OPEN]** `ItemHeat` or `ThermalBody` while raised.
- [ ] **Electrified perimeter**: fence wire over rating hurts what touches it, by properties (wet, metal armour, grounded), never mob id; rain raises conduction. **[OPEN]** damage type, how a fence reads touching.
- [ ] **Spring weapons from stored work**: spring steel stores `Fu`, released through a new launcher part beside the chamber and the coil winding.
- [ ] **Incandescence** — not to build yet. Above ~525 Tu a hot body emits block light in bands: melts ice, stops spawns; insulated vessels stay dark. Which blocks and bands still unasked.
- [ ] **Villager commissions** — later. Smith orders parts *by spec*, pays by closeness; repeat orders saturate.
- [ ] **Refined redstone superconductor** — late, hard line. Chain: purify, blend to stoichiometry (mixer), calcine, pack in sheath and draw/roll, draw-anneal cycles (path-dependent), reaction anneal under `Pu`. No drop up to critical current, then quenches and runs away. **[OPEN]** sheath metal, critical Tu.
- [ ] **Fluid temperature**: a `FluidStack` carries a temperature stamp (data component, `ItemHeat`'s pattern: stamp plus tick, cooling computed on demand, so tanks, pipes and unloaded chunks cool for free). Fluid ids stay for phases only; phase change is a `ThermalProcess` entry ("water + x Tu = steam"), shown in JEI like any other. One Feedback merge function on fill: mass-weighted average temperature, used by every tank and pipe. `spent_steam` goes away once built (engine exhausts steam at lower Tu; engine needs steam above working Tu). Boiler and condenser move onto the `thermal_process` table. No mod has per-stack fluid heat, so no prior art to ask. **[OPEN]** pipe heat loss (none, or `Conductance` leak); how third-party tanks and JEI see the component; boiler's pressure model against a table-driven boil.
- [ ] **Thrown weapons, tuning** (spec §12): the `[OPEN]` figures in `ModularTuning` (throw speed, spin, tumble floor, reel).
- [ ] **Novel gun options, brainstormed 2026-10-21** (rows in `data/feedback/gun_option/`; gun-owned figures only, handoff section 1): **Eddy Sleeve** (BUILT, compile only, 2026-10-21; `eddy` field on `GunOption`, drag scaled by the sleeve metal's ampacity; muzzle sleeve: eddy drag on a conducting or magnetic bullet turns kick into sleeve heat; a redstone winding drags hardest; dead weight on lead, overheats at cadence), **Sculk Jacket** (barrel jacket stores vibration for tight cold groups; spoils above sculk's oxidation temperature), **Resonance Weight** (three thread positions tune the barrel node; worse than bare when mismatched with muzzle speed), **Heavy-core counterweight** (reciprocating dense mass cancels kick, slows cycle, slaps the receiver if the stroke is cut short), **Honey hydraulic buffer** (viscous recoil damper, hot honey thins; needs a fluid in a fitting), **Glowstone reticle** (battery-free lit sight, by the `luminous` trait), **Redstone magnetic sear** (zero-standby latch; waits on the battery ruling), **Phantom-membrane obturator** (seals a loose chamber, cuts blowback; burns when hot), **Thermal-fit sleeve** (bore clearance closes as the barrel heats). Rejected: ender shutter (a free lunch).
- [ ] **Guns as a tool type, what is left** (built, compile only, nothing played; `docs/feedback_modular_items_spec.md` 8.3, 8.4, 12): play it -- Gun Table (4x4 slots, round slot, spec sheet in the left column), Blueprint Table scrolling rows for a gun, part shaping of receiver/barrel/action variants, fittings added in Rework, the blowback / lockup / gas / braced / open bolt / belt / fan / double barrel behaviours, designation names, calibre classes at the Press, JEI cards. **Not built:** the *off hand* trait (a handgun in each hand); fan costs are flat, not growing per shot; repair of a gun by stock ([OPEN]); swapping a fitting for another of its slot needs Rework "take apart" (the old refit gave the displaced part back); a fitting that only adds a trait (a bipod, a selector) has the schema (`trait`, `params` on `GunOption`) but no row yet; the barrel at the table is made at its variant's nominal length and wall, and only the machines cut it to size.
