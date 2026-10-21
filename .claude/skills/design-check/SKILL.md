---
name: design-check
description: Check the current diff against Feedback's hard design rules (no identity checks, switch-not-dial, wear only from misuse, units as types, licence/assets). Use before committing or when asked to review a change for design conformance.
disable-model-invocation: true
---

# design-check

Review `git diff HEAD` (plus untracked files) against the rules in `CLAUDE.md` "Hard design rules" and "Conventions". Report violations only; do not edit.

## Check

1. **Identity checks.** A player-facing mechanic that routes or sorts on item/block/fluid identity. Code that merely looks like one (`== Items.`, `getItem() ==`, `Fluids.LAVA`, id-string matches) is only a finding when the player can see or exploit the difference; the rule is about experience, not source (user ruling 2026-10-20). Prefer tags and properties for modded equivalents.
2. **Machine names a recipe.** A `machine/` class referencing a specific recipe, product, or material by name.
3. **Continuous knobs.** A new setting that is a free scalar rather than a small set of named choices.
4. **Controller as dial.** Any proportional/analog output outside `vehicle/`.
5. **Wear.** Wear accrued by uptime or correct use (exceptions: Grinding Wheel grit, Hand Hammer durability). Machines must degrade, never refuse to run.
6. **Hard gate for precision.** "Requires tier N" or a gate on precision rather than physical impossibility.
7. **Units.**
   - New quantity as bare `float`/`int` across an interface or `FTuning` constant instead of a `core/unit/` type.
   - Unit unwrapped into a local, or wrapper held in a field (wrap at accessor).
   - `Mu` handed to Sable without `Units.kpg`.
   - Minted a unit for wear/condition (it is a percentage).
8. **Thermal.** Flat-rate heating instead of difference-driven; exponential item cooling.
9. **Assets.** New texture without a `THIRD-PARTY-LICENSES.md` row; any Create asset; anything from the Futureazoo repo (CC BY-NC-SA).
10. **Docs.** Behaviour that contradicts a design doc without the doc being fixed in the same change. Adding `[OPEN]` is fine; silently resolving one is not.
11. **Prior art.** Code adapted from another mod without a site comment and a `THIRD-PARTY-LICENSES.md` entry.

## Output

One line per finding: `path:line: <rule #> <problem>. <fix>.` Then one line: pass/fail. If clean, say so.
