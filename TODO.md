# Feedback — TODO

**File hold only work still to do. Cannot contain anything done.** No
`- [x]`, no "built and verified" sections, no decision records, no history. Item close: delete its line (and sub-bullets) in same change. Decisions and reasons go in design docs; history live in git. Only `- [ ]` items, plus minimum context to start one cold.

Source of truth for *what's next*; `feedback_philosophy.md` stay source of truth for *why*.

---

## Pick-up list

- [ ] **Deposits: surface indicators, prospecting instrument, more veins** (mechanics §9.5). Graphite, vanilla's ores, tin, zinc and lead built; kaolinite still no survival source.
- [ ] **Real models and textures** for every block and item. User's call — never start unprompted. With them: cogs' diagonal large-to-small meshing, wear shown as block state
  (battered/spent), split turning part from fixed base on Hand Crank and Clutch.

---

## Document 2 — mechanics (`feedback_mechanics.md`)

Philosophy defer to it by name for each.

- [ ] Exact ranges and arithmetic for every unit (§17)
- [ ] `Su` load figures; linkage `St` pairs per machine (§17)
- [ ] Thermal model and item heat, written up from code
- [ ] Vanilla vessel bands — smoker ceiling, Crude Blast Furnace floor, plain furnace span (§15)
- [ ] Six apparatus properties as numbers — range, resolution, accuracy, control, response, stability (§8)
- [ ] Drift and recalibration mechanism (§8, §9)
- [ ] Data links: 16-block range, client-drawn cable, cable item cost, no path check (§13)
- [ ] Per-energy sensor/actuator pairings, full matrix (§10)
- [ ] Overrun band widths per machine and material (§6)
- [ ] Noise model — irreducible floor, learnable-in-aggregate, narrows never collapses (§8)

## Document 3 — content

- [ ] Seed from slice 1's 12 items / 7 machines, then slice 2

---

## Electricity (`feedback_mechanics.md` §6)

**Open:**
- [ ] **[OPEN]** Sag curve and source current cap, blocked lower-voltage source, amps per sink per wire (§6.2)
- [ ] **[OPEN]** Band anchors at HV/UHV; series-stack capping (§6.3)
- [ ] **[OPEN]** Shape of under-voltage slow-down; over-voltage per-unit scale (§6.5)
- [ ] **[OPEN]** No coil reach smelting heat (steam-engine dynamo hold vessel near 290 Tu). Decide when played.
- [ ] **[OPEN]** Cell never deplete (real cell eat anode). Decide with chemistry slice (§6.6).
- [ ] **[OPEN]** Electrostatic machine band — built at LV because anchors open.
- [ ] **[OPEN]** Leyden jar on same run as sink steal from it; check in play whether read as visible mistake.
- [ ] **[OPEN]** Electromagnet ignore Curie point (`curie_temperature` already in material table); hot iron pulled as if cold.
- [ ] **[OPEN]** Motor over band only spin faster; should winding take over-voltage as condition and heat?
- [ ] **[OPEN]** Motor drive one way only; reversing (swap leads) unbuilt.
- [ ] **[OPEN]** Plated coat read by modular tools and armour only (corrosion, armour skin hardness); optical sorter and plated contact conductivity still read nothing.
- [ ] Electrolytic cell's `[OPEN]`s (§6.14): conductive electrolyte, concentration instrument, current density burning deposit, gas capture.
- [ ] **[OPEN]** Breaker has one trip rating (copper cable's); discrete ratings wait on second cable metal.
- [ ] **[OPEN]** Mend degraded wire, or replace only? Built replace-only.
- [ ] **[OPEN]** Bare conductor shorting to neighbours (§6.4 stage 1): only shock built.
- [ ] **[OPEN]** Voltage rating per band on wire; insulation as item, not cable flag.
- [ ] **[OPEN]** Parallel sources: current routed along each sink's shortest path can overload one wire block. Revisit if layout show it.

---

## Rotation and wear

- [ ] **Flywheel: [OPEN]** rim speed limit from its metal (`yield_strength` over density): over it rim strain and wear, never burst.
- [ ] **Bearing as own part.** Built one (second `ShaftBlock` at lower drag) wrong
  shape — bearing is support shaft turns in. Support-block vs unsupported-span rule
  undecided (`docs/feedback_machine_shop_spec.md` §4.1).
- [ ] **[OPEN] Gearing need ongoing cost?** Gearing *down* to reach hardness floor free apart from cogs (§17).
- [ ] **[OPEN] No repair.** Condition only fall; recovery is break-and-replace. Likely answer: replaceable **Hammer Head** item, same slot better head would raise ceiling with.
- [ ] **Overspeed trip no natural trigger.** Nothing in roster reach 48 RPM except by
  `driveExternally`; wait for bigger source.

---

## Heat, vessels, materials

- [ ] **[OPEN] Per-material heat capacity** (TFC has it; deferred). Worth it once player must choose which of two hot things carry first.
- [ ] **Replace vanilla furnace/smoker/blast furnace in generated structures** (villages etc.) with Feedback's own — structure-processor swap, not mixin.
- [ ] **[OPEN] No steam bucket.** Add only when something must carry steam by hand.
- [ ] **[OPEN] Steam engine flat output, not throttle.**
- [ ] **[OPEN] Pump head.** Fluids no pressure model; revisit if pumping against column becomes decision.
- [ ] **Nothing need elevated pressure yet** — no `ThermalProcess` has pressure field.
- [ ] **[OPEN] Bad alloy mix can't be corrected** — no dilution or partial extraction; crucible stuck.
- [ ] **[OPEN] Dipped item that can't stand temperature** has no consequence beyond hot.
- [ ] **[OPEN] Ceramic, kaolinite, firebrick, graphite have no casting:** none melt (they are `thermal_process` products), so no fluid to pour. What shape them (mould of wet clay before firing?) undecided.
- [ ] **No advancement unlocks** for cast iron, brass, bronze, tin, zinc, glass, ceramic, kaolinite, firebrick, lead, copper tubing, quartz glass, graphite.
- [ ] **[OPEN] Vanilla noise-carved iron and copper veins still generate vanilla ore** (shipping noise settings copy declined; mechanics §9.6).
- [ ] **No survival source for kaolinite** — wait on a deposit file, not worldgen (mechanics §9; `docs/feedback_progression_roadmap.md` §4).
- [ ] **Glass tubing.** `Casting` already key on mold shape (bullet mold); need tube mold and `casting/` entry.

---

## Fittings and data links

- [ ] **`Adapter` has no concrete implementer.**
- [ ] **Removing fitting** (crowbar-style interaction).
- [ ] Refractory lining: no Jade line (crucible has none either).
- [ ] Link hover/selection colours (CFM's success/error/default, selected-node highlight).
- [ ] `DataNode.getMaxNodeLinks()` cap of 4 made-up number.

---

## Machine shop

- [ ] **Removal overruns spoil-only** (`steel_swarf`); degrade tier want named items (document 3).
- [ ] **Clutch's data-node overlay may not draw under Flywheel.** Harmless until node box needed.
- [ ] **Threaded rod** (rolled vs cut) and **Gear Cutter** — see `docs/feedback_machine_shop_spec.md` §6.
- [ ] **Drill Press auto-feed**: Crank Linkage + Clutch already do it — probably just document.

---

## Hazards and catch-up (`feedback_mechanics.md` §7, §8)

- [ ] **Tongs and gloves**: no unlock advancement, no JEI entry, no Jade line.
- [ ] **Hot piece does not ignite what it touches** (philosophy 9 permit it).
- [ ] **[OPEN]** Galvanometer on bare live wire shock hand; insulated probe nicer. Decide with instruments.
- [ ] Offline catch-up not applied to rotation/electric runs, Pressure Vessel leak, Condenser tank. Add if one visibly need it.
- [ ] Improved Furnace has own second `Heat.tick` path; its catch-up burn single fuel slot only. Look when next opened.

---

## Guns (`docs/feedback_guns_spec.md`)

- [ ] **Box-feed part for bolt and lever guns.** Bolt or lever with no feed part now `Feed.SINGLE` (one cartridge loaded by hand into chamber, `GunSpec.of`). Magazine-fed bolt/lever rifle can't say so until box-feed part role exists (cylinder/tube/belt's sibling); slides and automatics with no feed part still default `BOX`.
- [ ] **The mixer** — machine mixing powders by physical operation; Reloading Press then
  takes finished charge (`CHARGE`/`CHARGE_B`, `Cartridge.charges`). No blend recipe until it exists.
- [ ] **Animation library** — compare before committing to GeckoLib (spec §7.2).

---

## Programmers

**Later programmers (write down, do not build):**
- [ ] Cards made of cards (functions); delay card; addressable memory; loops. **[OPEN]** against philosophy 13 "capability does not tier" and stateless-controller ruling -- see `docs/feedback_controller_spec.md` §8.
- [ ] `NOT` gate (one input) on Logic card. Greater/Less could become one Compare card same way, if wanted.

---

## Vehicle ideas

(author, 2026-10-16)
- [ ] Later experiment: tire as own rolling body of 1/16 boxes on motored hinge (spec §5 *Guard rays*). Bumpy by construction.
- [ ] Hub with three points: axis, plus two locked to frame (real hub), so drive transfer has fixed frame of reference.

---

## Materials

- [ ] **Material forms by tag**: fuel moved onto materials (mechanics 5.7) and modded logs/planks lost `#minecraft:logs`/`planks` route; let material form name tag so `Material.find` reach modded items. Also bamboo no combustion yet (need own yields).
- [ ] Tempered steel figures (hardness 20, toughness 40, service 400) placeholders between steel and hardened steel.

## Vehicles

- [ ] **Controls: see braking in client.** Everything else on controls seen. Check: full
  brake skid iron-padded hub, half does not; braked driven hub stall engine. Retune
  `STEER_*`, `BRAKE_CLAMP_SU`.
  - [ ] **[OPEN]** Card targets are block positions; check program survive lifting and lowering car.
  - [ ] **[OPEN]** Steering ratio: banded choice on card (spec ruling) or Math card's scalar.
  - [ ] **[OPEN]** Brake pad wear: braking correct use, so wearing pads would be fourth wear exception. Need ruling.
  - [ ] Power steering (assist `Su` from drivetrain).

  Still owed after: springs and shocks, differential (straight axle turn both wheels
  as one rod today, so they scrub once steering exists), gearbox.
- [ ] **Guns on vehicles** (`docs/feedback_guns_spec.md` §8). Want mount. Car drive now; after controls.

**Vehicles beyond cars -- ideas** (author, 2026-10-20, from reading Create Simulated, `../Simulated-reference`; write down, do not build). Each carries its Minecraft lens in the line.
- [ ] **Named assembly failure, what is left.** The Lift names its refusals (empty, too big, locked, on a vehicle, blocked); still silent: frame assembly skipping a cell that would land on a block, and bearing-specific reasons ("frame touches two bases", "bearing has no far side").
- [ ] **Balance gauge, what is left.** The Lift shows total `Mu` and centre of mass on use; still owed: lift or thrust against weight once flying parts exist.
- [ ] **Clipboard copy of settings, what is left.** Hinges, tires, brakes and suspension already copy across a Symmetry mirror or a Welder selection; cards have no copy.
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

## Modular items (`docs/feedback_modular_items_spec.md`)

Tools, melee and ranged weapons, armour as baked designs; vanilla tools and armour replaced. Build order in spec §10.

- [ ] Impact pendulum (toughness) and strike post (damage per blow) as blocks (spec 6).
- [ ] Spec §11 `[OPEN]`s: section and as-built part shape; pry bar and prospector's pick; a crossbow's draw time and a lining's heat (vanilla reads constants).
- [ ] **Twin-headed tools, what is left** (spec §5.3, built 2026-10-20): wear is one pool for both edges (only the head used should wear: needs per-head damage state); a tip sets on the first head only; family names (`family` is still the bake's enum).
- [ ] **Part variants, what is left** (spec §12, built 2026-10-20; raw-stock roles 2026-10-21): no variant return swarf (the stock-to-litres mismatch is not modelled); a rim variant is a trade in stock and mass only (a heavier shield slows nothing yet); armour studs and a lining have no variants; the Blueprint Table draws standard shapes only.
- [ ] **Forging by rhythm, what is left** (spec §4.1, played at the Tool Table since 2026-10-21): the Mechanical Hammer's pattern time (instant), its need of a hand hammer in the slot, and a cam pattern of its own (spec §4.1); deviation from the Press and grinding (§4); more patterns and more parts; a gametest of `ForgeRhythm.judge` if wanted.
- [ ] **Scale loss in the Hearth** (spec 4.2, 2026-10-21): stock left in a pan above its material's `oxidation_temperature` is only refused by the table once past its window; it should lose a share (overrun on the finished stock). Needs a flaw channel for raw stock. Hearth art is the Firebox's brick, placeholder.
- [ ] **Tool Table art** (spec §4.2): the six part items (`tool_tip`, `tool_binding`, `sword_guard`, `sword_pommel`, `shield_rim`, `crossbow_prod`) borrow existing sprites (gem lens, leather strap, the modular sword, shield and crossbow layers) tinted by material and want art of their own.
- [ ] **Gun and ammunition options** (author, 2026-10-21; spec §13): as many choices as tools have, each changing only figures its own side owns (gun: barrel, muzzle, action, launcher, sight, stock, fire control; round: projectile shape and jacket, charge load, case, payload). **Suppressor blast/crack split is built; the subsonic load is a light charge count, labelled on the Gun Table. Fittings are a data table (`gun_option/*.json`, verified in game).** Then the Gun Table, then ammunition shape and jacket, then the pneumatic, spring and steam launchers. Open questions in §13.7.
- [ ] **Signature behaviours, what is left** (spec §5.4, played and working 2026-10-21): the spear's hitbox inflation, attacking through non-solid blocks and a custom first-person pose; prospector's range per handle length and tint per ore family; bleed credits no one.
- [ ] **Archetypes, what is left** (spec §12, built 2026-10-21): open helm's wide vision and hauberk's bleed; the sledge does not mine stone faster; sprinting while drawing a shortbow; a one-handed pistol crossbow.
- [ ] **Enchantments on parts, not tools** (author, 2026-10-21): the trident is now a spear head variant and the mace a maul head variant, but enchant tags are per item, so every spear takes Loyalty/Riptide and every maul takes Density/Breach for now. Later an enchantment sits on a part, and only a trident head can carry the trident's, only a mace head the mace's. Spec §12 merge note.
- [ ] **Katana's trick** (author, 2026-10-21): an edge harder than its spine (hardened-steel pairing). Per-variant dig tags beyond "digs nothing" (a variant that digs a different tag) are not built.
- [ ] **Enchanting rework, first slice left out** (`docs/feedback_enchanting_spec.md`, built 2026-10-23, unplayed): the table, `PoleGame`, three effect rows (`pole_effect/`) and the MYSTIC skin exist. Not built:
  - [ ] Heat erasing a part's poles above its poling point (`[OPEN]` hook; `ItemHeat` stamps lazily, so no cheap place yet).
  - [ ] Reagent slot and fuel/XP slot: XP levels only (3 per session), no reagent per effect; XP as a stored `XPu` charge needs a creative source first.
  - [ ] Capacity budget and the second-pole rule: one pole per part, a poled part is refused.
  - [ ] Ward's grip kind on a handle (less kick) and its mass cost on a plate; keen's brittleness is a forging flaw per level, not a toughness figure of its own.
  - [ ] Thrown weapons: poled `ignite_target` / `extra_damage` apply to melee blows only (the thrown path shares `MaterialPerks.affect`, which takes perks, not the stack).
  - [ ] Material `poling` blocks: no material file has one yet, so all use the fallback (cold end of the working window, one loop for all, `Material.poling()`). Materials with a `poling` block should differ.
  - [ ] Block entity: the table keeps nothing and returns the part on close; no book model. Structure replacement of vanilla tables (villages) is the structure-processor item above.
  - [ ] Villager enchanted-book trades and loot books left as vanilla. Whether a poled part returning from a taken-apart tool is too generous is `[OPEN]`.
  - [ ] A poled gun part (barrel) gains non-empty `dims`, which `Machining` reads as machined; only shaped tool/armour parts are meant to be poled for now.
  - [ ] Every constant in `ModularTuning.POLE_*` is `[OPEN]` until played; no unit test (the repo has no test scaffolding).

## Approved ideas, not yet built

Written down at author's word; prior-art question still unasked for each.

- [ ] **Spring weapons from stored work** (BUILT 2026-10-23, unplayed; guns spec 13.4): `Launcher.SPRING`, role `U` main spring, fires dry rounds. Left: melee/thrown spring uses, spring set on overload, own art for `main_spring` (borrows an iron nugget sprite).
- [ ] **Steam launcher** (BUILT 2026-10-23, unplayed; guns spec 13.4): `Launcher.STEAM`, role `O` steam chest, filled by right-clicking a boiler. Left: fill capped by source pressure, chest that can burst, HUD gauge, own art for `steam_chest`.
- [ ] **Demolition charge and explosive rounds** (BUILT 2026-10-23, unplayed; guns spec 13.4a). Charge is now a modular type (`charge.json`, Blueprint and Tool Table, unplayed); Blueprint Table has name search on types and materials (unplayed); filler role lists every powder (redstone included) and should hide what the bake refuses. Left: thrown grenade (impact or delay fuse, an entity), fragmentation from casing, more fuse kinds, damp duds, own art (borrows TNT's block texture).
- [ ] **Incandescence** — not build yet. Above ~525 Tu hot body emits block light in bands: melts ice, stops spawns; insulated vessels stay dark. Which blocks and bands still unasked.
- [ ] **Villager commissions** — later. Smith orders parts *by spec*, pays by closeness; repeat orders saturate.
- [ ] **Refined redstone superconductor** — late, hard line. Chain: purify, blend to stoichiometry (mixer), calcine, pack in sheath and draw/roll, draw-anneal cycles (path-dependent), reaction anneal under `Pu`. No drop up to critical current, then quenches and runs away. **[OPEN]** sheath metal, critical Tu.
- [ ] **Fluid temperature**: `FluidStack` carries temperature stamp (data component, `ItemHeat`'s pattern: stamp plus tick, cooling computed on demand, so tanks, pipes, unloaded chunks cool free). Fluid ids stay for phases only; phase change is `ThermalProcess` entry ("water + x Tu = steam"), shown in JEI like any other. One Feedback merge function on fill: mass-weighted average temperature, used by every tank and pipe. `spent_steam` goes away once built (engine exhausts steam at lower Tu; engine needs steam above working Tu). Boiler and condenser move onto `thermal_process` table. No mod has per-stack fluid heat, so no prior art to ask. **[OPEN]** pipe heat loss (none, or `Conductance` leak); how third-party tanks and JEI see component; boiler's pressure model against table-driven boil.
- [ ] **Thrown weapons, tuning** (spec §12): `[OPEN]` figures in `ModularTuning` (throw speed, spin, tumble floor, reel).
- [ ] **Novel gun options, brainstormed 2026-10-21** (rows in `data/feedback/gun_option/`; gun-owned figures only, handoff section 1): **Eddy Sleeve** (BUILT, 2026-10-21; `eddy` field on `GunOption`, drag scaled by sleeve metal's ampacity; muzzle sleeve: eddy drag on conducting or magnetic bullet turns kick into sleeve heat; redstone winding drags hardest; dead weight on lead, overheats at cadence), **Sculk Jacket** (barrel jacket stores vibration for tight cold groups; spoils above sculk's oxidation temperature), **Resonance Weight** (three thread positions tune barrel node; worse than bare when mismatched with muzzle speed), **Heavy-core counterweight** (BUILT 2026-10-22, unplayed; `GunCounterweight`: kick x0.85, cycle x1.25, wear raises kick to x1.5; plus bipod and selector rows on existing traits) (reciprocating dense mass cancels kick, slows cycle, slaps receiver if stroke cut short), **Honey hydraulic buffer** (viscous recoil damper, hot honey thins; need fluid in fitting), **Glowstone reticle** (battery-free lit sight, by `luminous` trait), **Redstone magnetic sear** (zero-standby latch; wait on battery ruling), **Phantom-membrane obturator** (seals loose chamber, cuts blowback; burns when hot), **Thermal-fit sleeve** (BUILT, 2026-10-22, unplayed; `GunThermalFit` trait on `gun_option/thermal_fit.json`: loose cold, tightest at 0.4 of ignition, binds past 0.7 by cutting chamber rating; one expansion for every metal). Rejected: ender shutter (free lunch).
- [ ] **Guns as tool type, what left** (built and played; `docs/feedback_modular_items_spec.md` 8.3, 8.4, 12). **Not built:** fan costs flat, not growing per shot; repair of gun by stock ([OPEN]); swapping fitting for another of its slot need Rework "take apart" (old refit gave displaced part back); fitting that only adds trait (bipod, selector) has schema (`trait`, `params` on `GunOption`) but no row yet; barrel at table made at variant's nominal length and wall, only machines cut it to size.
- [ ] **Gunfire as vibration** (BUILT 2026-10-23, unplayed): three game events by `report` (quiet 8 / loud 32 / thunderous 64 blocks; thresholds `FTuning.GUN_QUIET_REPORT`, `GUN_THUNDEROUS_REPORT` placeholders), in `vibrations` + `warden_can_listen`; player and mob shooters. Left: play-check sculk/warden ranges; sculk frequency (comparator) unmapped for custom events; subsonic load still not made. [OPEN] are three classes enough, or does a heavy rifle want its own tier?
- [ ] **Jar baton** (BUILT 2026-10-23, unplayed; wiki/Jar_Baton.md): `JarBatonItem`, charge drawn from a Leyden jar by use-on-block (`BATON_VOLTS` component), whole charge spent on next hit via `Shock` + `Exposure`, `CombatTicker.stagger`; overrun (above `BATON_RATING_VOLTS` or holder in water) shocks holder, gloves protect. Tuning placeholders in `FTuning`. Left: own art (borrows thermionic valve sprite), no leak while held, no modular-tool-type fit, no JEI/recipe-book check, runData not run. [OPEN] should a held baton leak like a jar?
- [ ] **Echo shard armour: footstep warning** (candidate, spec 2.1a): echo shard armour piece hears nearby footsteps as sculk sensor does. Not ruled in.
- [ ] **More material perks** (author, 2026-10-21): `perks` map (`Material.Perk`, `modular/MaterialPerks`) has first batch of spec 2.1a (silver, blaze rod, prismarine, echo shard, and second batch). Every material that earns one should get one, qualitative only (stat stays stat). Example given: breeze rod heads count lighter for handling.- [ ] **Nether-hot guns on mobs** (BUILT 2026-10-23, unplayed; `docs/spec_armed_skeletons.md`): carriers widened to `AbstractSkeleton` (wither skeleton at `FTuning.WITHER_SKELETON_GUN_CHANCE`, placeholder); mob shots already ride `GunCondition`/`AmbientTable`, so a Nether gun starts hot and cooks off sooner. `[OPEN]` strays/bogged armed too; difficulty scaling; whether the goal should refuse a too-hot gun (player has no such check).
