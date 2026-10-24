# Basic Electronics, phase 4: the Basic Electronic Circuit

> Status: BUILT 2026-10-23, unplayed (spec written, build followed in the same session; "As built" at the end records what changed). From the approved Slice 8 brainstorm (`TODO.md`). Rulings carried: **resistance is never modelled**; no tiers; no identity checks; **the circuit is a property-bearing part carried as a data component, not a tier key, and later machines eat its properties as quality inputs** (user ruling 2026-10-23). Companion: `feedback_mechanics.md` 6.21. Prior art: none taken beyond phase 1's GregTech read (yield ladder; circuit tier = voltage tier rejected).

## Why

Phase 3 made a board whose joints are graded and nothing read the grade. Phase 4 mounts parts on it and turns the result into a part with figures. It must be consumed by something now (digest: an unread figure is a trap) and must never gate.

Minecraft lens: none beyond what the earlier phases said; the parts are the phase 2 detectors and the phase 1 crystal plate. Saying so rather than inventing one.

## Scope

1. **Circuit component** (`process/Circuit`): `elements`, `speed`, `noise` (0-1, floored), `joint`, `limitTu`, `ratingAmps`. Reusable accessor `Circuit.of(stack)`; no circuit reads as `Circuit.NONE`, never as a refusal.
2. **Assembly is physical.** Parts are mounted in the crafting grid by the existing board recipe (board with no solder plus one part, up to `CIRCUIT_MAX_PARTS`), pasted, then soldered by the existing iron or reflow oven. `Soldering.finish` returns a circuit when parts are mounted. Part classes by property or tag: rectifying crystal (`electrical.rectifying`), relay and coherer (item tags), clock (a `crystal_plate` with enough `piezo_coupling`).
3. **Joint and failure.** Good joint: clean. Cold joint: extra noise. Lifted traces: a dead circuit (elements 0), parts lost. That closes phase 3's "no dead-part failure".
4. **Overrun.** Heat past the traces' limit lifts them (`Circuit.scorch`, on the inventory tick and on fitting; the oven also accepts a finished circuit and bakes it). Current past `ratingAmps` through a consumer fuses an element by chance. Sense: component tester, thermometer. Stop: take it out of the heat or the line.
5. **Consumer: the cat's whisker.** Fit a circuit on the contact. Noise multiplies the hunt band's spread, elements add reach, speed shortens the hold. Works without.
6. **Instruments.** `Circuit` is a `Testable`; the component tester reads one held in the other hand (elements, speed as `Quantity.RESPONSE`, noise at the condition step, joint).

Out of scope: any second consumer (controller card, sensor fitting, machine yield), rework of a cold circuit, a part's own wear feeding noise, a clock's frequency, a pump.

## Hard design constraints (digest)

- No resistance. No tier, no "requires circuit N", nothing refuses to run without one.
- No identity checks: parts by property or tag.
- Discrete options: part classes and joint grades are named; figures are derived from material properties, never player-dialled.
- Wear only by misuse: heat, over-current. Idling and correct use cost nothing.
- Irreducible noise floor (`CIRCUIT_NOISE_FLOOR`); a better circuit narrows the spread, never removes it.
- The world resolves on truth: the tester only reads.

## Files

New: `process/Circuit`, `item/CircuitItem`, `data/feedback/tags/item/circuit_{relays,coherers}.json`, `wiki/Basic_Electronic_Circuit.md`. Changed: `process/Board` (`parts`, `Part`, `Kind`), `process/Soldering`, `item/{BoardRecipe, BoardItem, SolderingIronItem, ComponentTesterItem}`, `machine/oven/ReflowOvenBlockEntity`, `machine/detector/{WhiskerBlock, WhiskerBlockEntity}`, `instrument/Testable`, `FTuning`, `FTags`, `FDataComponents`, `FItems`, lang.

## [OPEN]

- All `FTuning` phase 4 figures are placeholders; the circuit item reuses the plate sprite.
- A part's own condition does not reach noise (relay and coherer are blocks; their item carries none).
- A cold circuit cannot be reworked; no creative-tab sample; the whisker is the only consumer.
- The clock only steadies noise; no frequency consumer yet.
- Nothing but the oven re-heats a finished circuit.
- No `runData` run; the item model is hand-written.

## As built

Built as specified. The spec was written after the build in this session rather than before it (the brief asked for first). Compiles green; not played.
