# Basic Electronics, phase 1: crystal resonator and mass probe

> Status: build spec, 2026-10-23. From the approved Slice 8 brainstorm (`TODO.md`, "Slice 8, Basic Electronics"; roadmap §2 Slice 8). Nothing here is built. Rulings from the user, same day: **resistance is never modelled** (no resistor part, no voltage-drop part, ever); a glowstone phosphor screen is wanted, but later (phase 3).

## Why this first

Slice 5's electrolytic cell and electroplating already deposit a measurable quantity (`Qu`, thickness in `Plating`) that **no instrument reads** (`feedback_mechanics.md` §6.15 [OPEN]: "Thickness is figure no instrument measures"). A quartz-crystal microbalance reads exactly that, from a real physical effect (resonant frequency falls as mass loads the crystal). That is the roadmap's condition for amethyst's third appearance (a resonance that tracks a real quantity). It is also the cheapest slice-8 piece: it reuses the cell, `Plating`, and the instrument ladder, and needs no new energy type.

Minecraft lens: budding amethyst grows clusters, which is hydrothermal crystal growth, the same process that makes real synthetic quartz. Quartz (`material/quartz.json`) and amethyst (`material/amethyst.json`, `kind: crystal`) are both already materials. No new physics; no creative source needed (no new energy type).

## Scope (this phase only)

1. **Crystal plate**: a cut piece of a `crystal`-kind material, in discrete named cut classes. Cut by existing shop operations (grinding wheel / lathe / press: read `docs/feedback_machine_shop_spec.md` and `feedback_mechanics.md` §5 for the form step table). **No new machine.** Add a `form` and, if needed, a data component; do not add a per-item cutting recipe (digest: machines define operations).
2. **Mass probe**: crystal plate + a metal backing, crafted. The backing is what makes it conduct, so the probe hangs on the electrolytic cell's side exactly like a plated piece (§6.15: "only metal conducts"). Reuse that cathode path; do not fork it.
3. **Reading**: while the probe hangs it plates; its frequency shift is the deposited `Qu`. Show it as a new `Quantity` through `Instruments.format`/`Readout.figure` (digest: every quantity has a truth tier; never a fixed `%.1f`). Hand-held or in Jade; follow how the galvanometer/thermometer expose theirs.

Out of scope: coherer, cat's-whisker, solder, relay, scope, phosphor screen, Basic Electronic Circuit item (phases 2 to 4).

## Hard design constraints (digest; the reader must still read it)

- **Cut class is a discrete choice with a real trade**, not a scalar. Proposed: a thin cut is fine-grained but its range is short; a thick cut reads more mass but coarser. Three named classes at most. The resolution is the instrument's resolution step: it must come out of `Instrument` tiers, not a hidden stat.
- **Overrun is the core pitch.** A film thicker than the cut's range damps the crystal: the reading **pegs at its ceiling and stays wrong**, and the plating keeps growing underneath (the existing spall rule at `PLATING_SPALLS_ABOVE_QU` still applies). A probe left in dead-reads and its coat spalls. The player who trusts a pegged reading loses the piece. That is "an instrument is a yield technology, not a key": nothing is unlocked, the cell works without it.
- **Wear only by misuse.** Over-drive (hung on a supply above what the cell is rated for, i.e. existing over-voltage rule) cracks the crystal: condition falls, readings get noisier, not refusal. No uptime wear.
- **The world resolves on truth.** The reading never feeds back into plating. A bad probe costs reproducibility, never success.
- **Irreducible noise floor**: even the best cut reads with nonzero noise.
- **No identity checks**: pick crystals by `kind == crystal` and properties (hardness, density, a piezo-ish flag if one is needed), not by id, so modded crystals work. If a new material property is needed (piezoelectric coupling), put it in `material/*.json` and add it to quartz and amethyst (memory: route properties through the material system).
- **Units**: derive; a frequency unit is allowed only if the `Units` checklist is followed (one file in `core/unit/` plus a lang key). Prefer showing the shift as `Qu` through the existing quantity and keep frequency internal if no player decision uses it.
- No resistance in any form.

## Files likely touched

Read before editing: `docs/design_digest.md` (the dev gate needs it), `feedback_mechanics.md` §5 (forms), §6.14, §6.15, §8 (instruments); then `machine/electrolytic/`, `process/` plating code and the `Plating` component, `instrument/Quantity.java`, `Instrument`, `Instruments`, `Readout`, `data/feedback/material/quartz.json` and `amethyst.json`, `data/feedback/form/`, `compat/jade/`. Match existing registration patterns (`registry/`).

## Deliverables

- Code compiles (`./gradlew compileJava`).
- Data: crystal-plate form(s) and material fields; recipes for probe.
- Lang keys, tooltips glossed from the deciding constants (`Readout.gloss`), a wiki page (see `wiki/`, e.g. `Jar_Baton.md`, original behaviour plus new), JEI shows the probe if other items do.
- `feedback_mechanics.md`: new §6.17 "Mass probe" (what it is, the overrun, what was tempting and wrong), and update `docs/source_layout.md` and the Slice 8 line in `TODO.md` (mark phase 1 BUILT, unplayed). Fix any doc this contradicts in the same change.
- `THIRD-PARTY-LICENSES.md`: extend the GregTech CEu Modern section: read for early-circuit design on branch `1.21`; **taken**: component slots as property tags and the yield-ladder idea; **rejected**: circuit tier = voltage tier, wafer/boule chain, SMD multipliers, cleanroom gate, flat-fluid solder tax. No code copied.
- Then `./build-and-deploy.sh` once compile is green, and an **in-game checklist** for the user (what to do, what they should see). Do not run game tests (CLAUDE.md). Do not commit or push.

## [OPEN] for the agent to report back, not silently settle

- How cut class is chosen with existing machines (which removal operation, and whether class is a form or a component).
- Whether a probe can be re-used after a coat (clean it? plate it away?) or is single-use.
- Whether the probe reads in Jade, in hand, or both.
- Whether a piezo property belongs on the material or is `kind == crystal` alone.
