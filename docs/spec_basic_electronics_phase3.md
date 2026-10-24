# Basic Electronics, phase 3: solder, traces, instruments, phosphor screen

> Status: build spec, 2026-10-23, written before the build (section "As built" at the end records what changed). From the approved Slice 8 brainstorm (`TODO.md`, "Slice 8, Basic Electronics"). Rulings carried from phases 1 and 2: **resistance is never modelled** (no resistor, no voltage-drop part, ever); no tiers; no identity checks. Companion: `feedback_mechanics.md` 6.20. Prior art: none taken (GregTech's flat-fluid solder tax was read in phase 1 and rejected). The phosphor screen is compendium `docs/speculative_physics_inspo_doc.md` 3.1 (glowstone's luminous trait).

## Why these four together

Phases 1 and 2 built parts that sense. Phase 3 builds the first thing that is *made*: a board with traces and joints, which phase 4's Basic Electronic Circuit will eat as a quality input (user ruling 2026-10-23: properties are eaten by later machines). Alongside it, the instruments that look at all of it: a component tester for the phase 2 parts and for boards, and an oscilloscope that draws on a glowstone phosphor screen. Nothing here unlocks anything; the circuit is phase 4.

Minecraft lens. Solder is tin and lead, both already metals with a melting point, so the alloy comes from the existing alloy table and mixer, not a recipe list. The twist the world adds is the glowstone screen: glowstone is already `luminous`, and a phosphor is exactly a luminous powder; a sealed glass envelope with it coated inside glows where a signal lands. No other twist; said so rather than invented.

## Scope (this phase only)

### (a) Solder and joint quality

1. **Two solder alloys as materials.** `solder_leaded` (tin and lead, `perks.toxic`) and `solder_free` (tin and a little copper). Each is a `material/*.json` with `composition`, melting point (the **liquidus**) and `molten.viscosity`; an `alloy/*.json` entry makes the molten fluid in a crucible or the mixer; a `casting/*.json` entry makes the ingot. The new `solder` trait marks a material as solder (a property, not an id).
2. **Joint quality is a function of Tu against the liquidus** (`process/Soldering.grade`, pure): peak Tu below the liquidus, or not held above it for `SOLDER_DWELL_TICKS`, is a cold joint (**intermittent**); within the alloy's **window** above liquidus is a good joint; above the window the flux has burnt (cold again); above what the trace or substrate survives the trace **lifts**. The window is `SOLDER_WINDOW_NUMERATOR_TU / molten.viscosity`: a thinner melt wets better, so lead solder (viscosity 2) forgives more than lead-free (4), and the liquidus is simply the material's `melting_point` (lead 183, lead-free 227). Nothing new is added to the material schema for this beyond the trait.
3. **Lead is hazardous.** Working a toxic solder poisons whoever is within reach while it is hot (`core/hazard/Fumes`, off the material's `toxic` perk; poison never kills). Lead-free is clean and forgives less. Note: the brief named `core/hazard/Exposure`; that class is the shock-exposure multiplier, not a toxin path, so the fume rule is a sibling class rather than a stretch of it.
4. **Two routes to heat a joint, one grading function.**
   - **Hand iron** (attention): a Soldering Iron is heated at any `HeatSource` block (right-click it), keeps its heat by `ItemHeat`, and is held on a pasted board (board in off hand, hold use). The joint is graded on release from the iron's Tu then and how long it was held. Hot iron cools into the window as it works; the skill is when to let go. Each held tick draws heat from the iron.
   - **Reflow oven** (capital): a block on a fire (`HeatSource.below`) that is a `ThermalBody` holding a batch of boards. Each board follows the oven's Tu with a lag (its own thermal mass); the batch's mass is added to the oven's, so a cold batch cools it. Boards are graded when taken out, from each board's peak Tu and time above liquidus. More boards at once, no attention, but the oven must be built and fired, and an oven left hot cooks the batch.
5. **Failed joints perish as rework, not refusal.** The machine always does its job and the solder is always spent. A cold joint leaves the board `COLD`; it can be run again (rework). A lifted trace returns the bare substrate: wire and solder are lost, the substrate is reusable. Never a refusal to run.

### (b) Two ways to lay traces

1. **Substrate** is a form (`substrate`), of any `ceramic`-kind material, ground from a brick (`brick` to `substrate`, `grind`). Insulation and heat tolerance are read from the substrate's own material (`electrical.insulation`, `thermal.service_temperature`), so a modded ceramic works. Reuses the existing brick and ceramic materials. There is no polymer material in the mod, so the ceramic/polymer split is `[OPEN]`; the form's `kinds` list is the one line to widen.
2. **Etched copper**: substrate plus fine wire (any `fine_wire` form of a metal; the wire drawer makes it) in a crafting grid. Fine, power-capable (`ETCHED_RATING_AMPS`), and the capital route (the wire drawer, the draws). The acid is `[OPEN]`: no acid exists in the mod, so the etch step is the crafting.
3. **Printed paste**: substrate plus a dust of a material with the `paste` trait (redstone dust). Quantised (`PRINT_STEPS` discrete widths, never a free value), lossy, signal only (`PRINTED_RATING_AMPS` is tiny), and the same granular weak-link model as the coherer (phase 2): a printed trace sinters when over-driven (`PRINTED_RATING_AMPS`, applied by phase 4's circuit, not here). Cheap, hand-made.
4. A board carries a data component `board`: trace route, trace material, substrate material, solder material (once pasted) and joint grade. A pasted board is a board plus a solder ingot, crafted.

### (c) Instruments

1. **Component tester** (carried item, an `Instrument`): right-click a placed part, or a board in the off hand. It shows the part's response through `Instruments.format` at its own resolution: the whisker's spot sensitivity, the coherer's weld state and condition, the relay's closed state and condition, a board's joint and trace. New `Quantity.RESPONSE` (0 to 1, appended) for sensitivity; condition uses the existing `CONDITION`. The helmet reads everything at the truth tier like anything. A part opts in through a small `Testable` interface; the tester knows no part by name.
2. **Oscilloscope** (block, an `Instrument` for `Quantity.CURRENT`): put it against a cable (any `ElectricNode`) and a phosphor screen. It does not join the run (it is not an `ElectricMember`). It samples the cable's current into a short history; right-click prints the trace as a row of bars quantised to its resolution, signed. Over `SCOPE_FULL_SCALE_AMPS` the reading **pegs at the ceiling**, exact and wrong, as the mass probe does. *Sense:* the cable's current. *Stop:* nothing needed; it is passive, and taking it away stops it.

### (d) Glowstone phosphor screen

1. **Smallest vacuum route: hot-seal.** A phosphor envelope (glass block plus glowstone dust, crafted) is placed on a fire. It is a `ThermalBody` of glass (service temperature from the `glass` material). Right-click with an empty hand while it is hot **pinches it off**: it records the Tu it was sealed at. A sealed envelope's gas follows the gas law, so its gauge pressure is `(T + 273) / (T_seal + 273) - 1`, negative once it cools: hotter sealing gives the better vacuum. It is a `PressureHolder`, so the existing pressure gauge reads it (negative is vacuum). No pump, no new pressure source. Unsealed it is at atmosphere.
2. **The screen glows only under a good vacuum** (gauge at or below `SCREEN_MIN_VACUUM_PU`, negative); with air in the envelope the beam scatters and nothing lights. Glow is four discrete levels (0 to 3) driven by an adjacent oscilloscope's sample; block light follows the level.
3. **Overrun, both ways it can go wrong.** (i) **Reheating a sealed envelope** past its seal temperature turns vacuum into pressure; past the danger line the chance of letting go climbs each tick, certain at critical (the shared `PressureBurst` ladder, same as the vessel and boiler), and past the glass's service temperature it cracks outright. Either shatters the block (vacuum lost, nothing returned). (ii) **Burn-in**: a screen held at full glow accumulates burn; burn lowers the brightest level it can reach, permanently, down to dead. Wear only by misuse (a static full-scale signal left on). *Sense:* the pressure gauge. *Stop:* take the signal away; cool the envelope.

Out of scope: the Basic Electronic Circuit item and anything that consumes a board's grade (phase 4), dead-part failure (there are no mounted parts yet), a spatial trace drawn on the screen (a block entity renderer), a pump and hard vacuum, a polymer substrate.

## Hard design constraints (digest)

- No resistance: nothing adds a drop. Trace "rating" is a current the *circuit* will tolerate, read in phase 4, not a series element.
- No tiers: reach comes from physics (liquidus against Tu, vacuum from seal heat), and the window from viscosity.
- Discrete options: trace route, print widths, joint grade and glow level are named choices. The only analog inputs are inside the attention loops (iron timing, seal moment), where scalars are fine.
- Wear only by misuse: burn-in, reheat burst, over-hot oven. Reflow, soldering, idling and a correct seal cost nothing. Consumables (solder) are spent by use, which is the accepted consumable rule, not wear.
- The world resolves on truth: the instruments never feed the grade. A bad reading costs reproducibility only.
- Noise floor: the tester's resolution and the scope's quantisation are the floor; neither is exact short of the truth tier.
- No identity checks: solder is the `solder` trait, paste the `paste` trait, substrate a `ceramic` kind, the coating a `luminous` material.

## Files

New: `process/Board`, `process/Soldering`, `core/hazard/Fumes`, `instrument/Testable`, `item/{BoardItem, BoardRecipe, SolderingIronItem, ComponentTesterItem}`, `machine/oven/{ReflowOvenBlock, ReflowOvenBlockEntity}`, `machine/phosphor/{PhosphorScreenBlock, PhosphorScreenBlockEntity, OscilloscopeBlock, OscilloscopeBlockEntity}`. Changed: `Quantity`, `Material.Trait`, `FTuning`, registries, `Coherer/Whisker/RelayBlockEntity` (Testable), data (materials, alloys, castings, form type and form, recipes, models, lang), fluids.

## [OPEN]

- All `FTuning` figures in the phase 3 block are placeholders; art is placeholder everywhere (vanilla textures, the plate sprite for substrates and boards).
- No acid: the etch step is the crafting. No polymer substrate material. No pump or hard vacuum.
- No dead-part failure (nothing is soldered on yet); phase 4 reads the joint grade.
- The scope shows a history row on click and lights the screen as a whole; a spatial trace on the screen face needs a block entity renderer.
- Lead-free "weaker" is a narrower window only; a brittle-joint life under vibration waits for phase 4's circuit.
- A hot-sealed vacuum is partial (about 0.6 of an atmosphere at best), enough to glow, not a hard vacuum.
- Fume rule uses poison; whether gloves or a mask should stop it is open.

## As built

- **Superseded 2026-10-23:** the hand iron is no longer hold-and-release; it opens a drag minigame (`SolderScreen`, mechanics 6.22, `docs/spec_soldering_minigame.md`). The grade function and the oven are unchanged.

Built as specified, `compileJava` green; not played, `runData` not run (models hand-written under `src/generated`). Where the build differed from the spec:

- The `Quantity.RESPONSE` addition needed one case in `ProgrammerScreen`'s colour switch.
- `PRINT_STEPS` was dropped: nothing in this phase reads a print width, so printed quantisation is a property phase 4 will give the circuit, not a constant here.
- Fume dosing happens at the player on iron release (once per joint) and at the oven centre every `FUME_INTERVAL_TICKS` while any toxic board is above liquidus.
- The iron's touch-down Tu is held in a server-side map keyed by player (marked `shortcut:` in code), not on the stack.
- The solder materials `exclude` every form but the ingot so no unmodelled plate, wire or block items are generated.
