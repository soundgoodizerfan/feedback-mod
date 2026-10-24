# Basic Electronics, phase 2: coherer, cat's whisker, relay

> Status: BUILT 2026-10-23, unplayed (spec written first, then built; section "As built" at the end records what changed). From the approved Slice 8 brainstorm (`TODO.md`). Rulings carried from phase 1: **resistance is never modelled** (no resistor part, no voltage-drop part, ever); no tiers. Companion: `feedback_mechanics.md` 6.18. Prior art: none taken; the coherer is compendium `docs/speculative_physics_inspo_doc.md` 3.3, the whisker and relay are textbook parts.

## Why these three

Phase 1 gave electricity an instrument for plating. Phase 2 gives it the first **detector** and the first **bridge**. Today the only things that sense an electrical event are the galvanometer and the current sensor fitting, and both need a wire carrying current. The coherer and the whisker hear an event that is *not on the wire at all* (a spark), and the relay lets a small current in one run switch a big one in another. Together: a detector in a coil run, a relay on its output, a load behind that.

Minecraft lens: every spark source already in the mod (a breaker opening under load, an electrostatic generator turn) becomes audible to nearby parts, as a vibration is to a sculk sensor. That is the same machinery (game events and listeners) and gives the "wireless" detector real range for nothing. The cost is that your own factory's arcs trigger your detectors: a coherer beside a flapping breaker welds itself. No twist beyond that; saying so rather than inventing one.

## Scope (this phase only)

1. **Spark**: one new game event, `feedback:spark`, radius `FTuning.SPARK_RANGE_BLOCKS`. Emitted (`core/electric/Sparks`) by a breaker or relay opening under at least `SPARK_MIN_AMPS`, and by an electrostatic generator turn. Not a new energy type: no creative source.
2. **Redstone coherer** (`machine/detector/`): a wire that is open until it hears a spark, then welded shut (a latch); a tap frees it.
3. **Cat's whisker** (`machine/detector/`): a wire probing a crystal; a momentary detector whose sensitivity is a hunt.
4. **Relay** (`machine/relay/`): contacts moved by an adjacent electromagnet's field.
5. **Material property** `electrical.rectifying` (0-1), materials `galena` and `pyrite`, quartz gets 0.3.

Out of scope: solder, traces, oscilloscope, circuit item, phosphor screen (phases 3 and 4); a deposit for galena or pyrite; AC or frequency.

## Plug points (read before choosing)

- **Run membership.** A detector or relay is an `ElectricNode` with `Role.WIRE` whose `conducting()` flips, exactly as the breaker's does; flipping calls `ElectricPropagator.reconnect`. So the detector's output is simply the run beyond it: a sink there (a lamp-like load, an electromagnet, a motor) is the signal. No new network.
- **Sensing.** Everything is already a `DataNode`; a `CurrentSensorFitting` on the cable beyond a detector links to a controller through the data connector, so a program can act on "the coherer fired" with no new sensor code (6.7).
- **Stopping.** Coherer: the tap. Controllers reach it as a `Switchable` whose only effect is the tap (a controller can switch it off, never on: only a spark welds). Whisker: opens by itself. Relay: cut the coil run (breaker, fuse, controller card).
- **Bridge to controller and data nodes.** The relay needs no controller code: a coil run can be fed by a detector, or by a breaker a controller switches (controller to Eu), and the contacts' run carries a current sensor fitting (Eu to controller). Both directions are built from parts that already exist.
- **Not joining the coil.** The propagator joins every touching node. A relay must touch its magnet without joining its run, so `ElectricMember` gained `joins(other)` (default true; both sides must agree), and the relay refuses an `ElectromagnetBlockEntity`.

## Hard design constraints (digest)

- No resistance: none of the three adds a `dropPerAmp`. (The breaker's sag is its own existing rule; the relay does not copy it.)
- No tiers: a part's reach comes from what the player does (the hunt, the tap), not a level.
- Switch, not dial: coherer and relay are two-state; the whisker is two-state with a fixed hold. Its sensitivity is a property of a spot found by the player, the hunt being a minigame (scalars are fine inside one).
- Wear only by misuse, each with a stated overrun, and never refusal:
  - **Coherer**: welded tube carrying more than `COHERER_RATING_AMPS` sinters; condition falls to a floor; a tap frees it with probability `condition`. Weld and tap are free.
  - **Whisker**: current above `WHISKER_FUSE_AMPS` through the closed contact fuses the spot under the whisker (it hears nothing after); the line opens. Other spots are untouched; re-seating the crystal draws fresh spots.
  - **Relay**: opening under load pits the contacts by amps broken (the breaker's rule); carrying more than the cable rating wears them per tick over; worn contacts let go late (release succeeds with probability `condition` each tick), never refuse. Closing, holding and idling are free.
- Sense and stop for each part are stated in its class javadoc and in 6.18.
- **No identity checks**: a whisker's crystal is any item whose material has `electrical.rectifying`; galena, pyrite, quartz are data. A modded crystal with the property works.
- The world resolves on truth: the band the hunt prints is the true sensitivity plus noise; nothing reads back into the calculation.
- Irreducible noise floor: the hunt band is always noisy; the relay and coherer release are probabilistic only when worn.

## Files

New: `core/electric/Sparks`; `machine/detector/{SparkDetectorBlockEntity, CohererBlock(Entity), WhiskerBlock(Entity)}`; `machine/relay/{RelayBlock, RelayBlockEntity}`. Changed: `ElectricMember` (`joins`), `ElectricPropagator.connects`, `BreakerBlockEntity` and `ElectrostaticGeneratorBlockEntity` (emit), `FWeapons` (game event register), `Material.Electrical` and `MaterialProperty` (`rectifying`), `FTuning`, registries, data (materials, recipes, loot, models, lang).

## [OPEN]

- All `FTuning` figures in the phase 2 block are placeholders.
- Art: every block reuses a vanilla texture; the welded coherer and closed relay have a second placeholder model.
- Galena and pyrite have no deposit (items exist, creative tab only); quartz works in survival at 0.3.
- Whisker crystal is not consumed by fusing; whether a fully burnt crystal should be spent.
- Spark strength is one class; whether a heavy arc should reach further, or a quiet one not at all.
- Coherer and controller: a controller can only tap. Whether a physical clapper (a hammer or linkage that taps on its own) is wanted.
- Relay: a single magnet is found among six neighbours (the strongest field wins); no sided armature.

## As built

Built as specified; no spec rule had to give. One engine change beyond the parts: `joins` on `ElectricMember`. Not played, `runData` not run (assets hand-written).
