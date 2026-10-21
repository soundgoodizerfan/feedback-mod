# Feedback — Handoff: gun and ammunition options

**Working document, not one of the three.** Written 2026-10-21 for whoever builds the gun part options and the ammunition options next. It is a handoff, not a spec: the design is in `docs/feedback_guns_spec.md` §13 (read it first), and this says what is built, where it lives, how to add an option without breaking a rule, and what to do in what order. When something here goes stale, fix it in the same change.

## 0. Before you touch anything

1. Read `CLAUDE.md`, `feedback_philosophy.md` and `feedback_mechanics.md` **in full**. A hook only checks that they were read; the rules inside are what the review will check. Then read `docs/feedback_guns_spec.md` (all of §2, §3, §12, §13) and `docs/feedback_modular_items_spec.md` §4.1, §4.2, §8.
2. **Ask the author whether there is a mod to study before building anything non-trivial** (CLAUDE.md, *Before building a feature, ask about prior art*). TacZ was read for this (guns spec §13.2, `THIRD-PARTY-LICENSES.md`); Point Blank has no public source. Do not re-read TacZ for attachments; its result is in §13.2.
3. **Do not run game tests.** Verify by `./gradlew compileJava`, then `./build-and-deploy.sh` once the author confirms it works, then hand the author a short in-game checklist. Do not push; do not commit unless asked.
4. The working tree is shared and dirty. Never `git stash`, `reset`, `checkout` or `clean`.
5. Breaking changes are fine: rename or restructure ids, NBT and components freely; write no migrations or aliases.

## 1. The one rule that governs every option

**A gun's option never changes a figure the round owns, and a round's option never changes a figure the gun owns.** The table is in guns spec §13.1; the short form:

| Gun owns | Round owns | Neither writes (computed from both) |
| :--- | :--- | :--- |
| bore, barrel length and tube, wall and chamber metal (so the rating), action and cycle, feed, sights, grip, stock, mass and balance, heat mass and shed, muzzle devices, mounts | case (metal, volume), charge (what, how much, blend), projectile (mass, metal, shape, jacket), payload, shot count | chamber pressure, muzzle speed and energy, impulse and kick, report (blast from the gas, crack from the speed), heat per shot, penetration, pattern spread, fit |

**The test, applied before writing the option:** name the figure it changes and its column. An option may change only its own side's inputs to a third-column figure (a vent changes *where* pressure is relieved, never what the charge makes). **Make this impossible to break by construction** rather than by review: give gun options a schema with no field that names a round figure, and round options a schema with no field that names a gun figure. See §4 for the recommended shape.

Existing options were audited 2026-10-21: vent, brake and shroud are clean. The suppressor was a blur and is fixed (§2).

## 2. What is built (all compile-only unless stated; nothing here has been played)

**Updated 2026-10-21: a gun is a modular tool type (modular spec 8.3, 8.4).** `GunDesign`, the Stand and `GunAssembly` are gone; read `weapon/GunSpec` (the physics, `GunSpec.of(stack)`) wherever this section says `GunDesign`. A new fitting is a `gun_option` row, a part form, a machining step, a *role* in `modular_type/gun.json` (one part item per role) and lang; it may add a trait (`trait`, `params` on the row) or change figures. The Gun Table is the Tool Table with a type filter. `GunGameTests.gunsBakeTheSameThroughTheCore` no longer exists; `theTableMakesTheGunTheParts` pins that a table-made gun carries the reference bake of its parts.

- **Gun baked from a design.** `weapon/GunDesign` (record: bore, barrel band, wall, material, sight, launcher, parts, layout, built barrel geometry, fittings) bakes once into `weapon/GunFigures` via `bake(materials, Cartridge)`. The bake is a pure function; nothing keys on an id. Fittings are a **data table**: `data/feedback/gun_option/*.json` loads into `GunOptions` (`GunOption` record, synced by `GunOptionSyncPayload`); `GunDesign.fittings` maps an option id to its metal, and `effects()` folds the fitted options' multipliers into the bake. The four shipped are suppressor, brake, vent, shroud. The Stand draws one row per option slot, the muzzle slot cycling bare/suppressor/brake. `Launcher` (chamber, coil), `Sight` (iron, reflex, scope), `Barrel`/`Wall` bands, `Parts` (slide, trigger, grip, receiver, ...). Other gun constants live in `core/FTuning` (`GUN_*`); an option's own figures live in its JSON.
- **Round** is `weapon/Cartridge`: `boreMm`, `caseMaterial`, `charges` (a blend, at most two kinds), `projectile`, optional `payload`, `shot`, `stored` (a charged jar round). Loaded at the Reloading Press (`ReloadingPressBlockEntity`, `ReloadingPressMenu`). `weapon/GunAmmo` is the ammunition's machinery; `weapon/CartridgeItem`, `MagazineItem`, `Magazine`.
- **Suppressor blast/crack split** (`GunDesign.bake`, `FTuning.GUN_CRACK_SHARE`, `SOUND_MS`, `GUN_CRACK_RAMP`): `report = 0.6 * blast + 0.4 * crack`; the fitting acts on blast only, crack comes from muzzle speed against 343 m/s. A bare supersonic gun reports 1.0. **The subsonic load that makes a suppressed gun actually quiet is not made.** `GunGameTests.fittingsArePartsThatTrade` still asserts the old ordering and should still hold; confirm.
- **Gun assembly** goes through `modular/ModularRecipe` and `weapon/GunAssembly` (a gun blueprint plus machined parts assembles; a built gun plus one fitting part, or one or two plates for a sight, refits). `data/feedback/modular_type/gun.json` is the type (family `gun`, `blueprint: true`, ten roles: barrel, receiver, action, trigger, grip, feed, sight, muzzle, vent, shroud; **no `part` blocks, so the modular variant machinery does not apply to gun roles**). Parts are machined (`data/feedback/machining/gun_barrel_*.json` and others) into shared part items (`FModular.PARTS`, each carrying a `MadeOf`).
- **The Gun Table** (`weapon/GunTableBlock`, `GunTableBlockEntity`, `GunTableMenu`; screen `client/GunTableScreen`) replaces the Assembly Bench (guns spec §13.6, **step 1**). Blueprint slot, 4x4 parts grid, one round slot, one output slot; the output comes from the same recipe code; with a round loaded the screen shows the gun's spec sheet against that round (chamber against rating, speed, energy, report, kick, spread, cycle), figures through `Readout.figure`. The spec sheet scrolls, and lists the missing part roles (`GunAssembly.missing`, a bitmask over the type's role order synced by a `DataSlot`; it matches by form, so a missing coil winding or grip wrap shows only "Not enough parts yet."). **Step 2, shaping gun parts at the table, is not built.**
- **Charger** (`machine/charger/ChargerBlock`, `ChargerBlockEntity`; was "Round Charger", renamed 2026-10-21) charges jar rounds (`Cartridge.Stored`: volts and a tick, leaking on its own). Batteries are not built.

## 3. Backlog, in the author's order

Order settled by the author (guns spec §13.7): ownership fix and Gun Table first (done above), **then ammunition shape and jacket, then launchers**. Gun part options are the next feature the author named. Do them as separate changes; each one named below is a candidate, none is promised.

### 3.1 Gun part options (the gun's figures only)

Barrel: rifled against smoothbore, twist band, a replaceable **liner** (wear lands on it), water jacket (a fluid jacket the barrel's heat goes into: a `ThermalBody` coupling; boils to steam). Muzzle: compensator, **choke** (shotguns: cylinder, modified, full), bayonet lug and blade (a modular blade part, tool bake). Action: **adjustable gas port** (two settings; gas-operated actions take cycling energy from the gas). Stock and grip: stock length band, **recoil pad** (the pad metal sets how much kick the hand takes), bipod (kick into the ground only when set). Fire control: safety, burst, set trigger, hair trigger (a hazard with a physical cause). Sights: rangefinder, thermal scope (needs a **battery**), laser. Launchers: **pneumatic** (a pressure reservoir, `Pu`; the round is a projectile with no charge), spring (stored `Fu`; the approved spring-weapon idea, `TODO.md`), steam. A launcher declares which round it takes: that is a *fit*, not a rule.

### 3.2 Ammunition options (the round's figures only)

Projectile **shape, forged** (author, 2026-10-21: by rhythm, like a part, modular spec §4.1, `modular/ForgeRhythm`): round ball, pointed, flat nose, hollow point (more damage, less penetration), solid slug. **Jacket and core** (two metals: the core's hardness gates penetration, its density sets mass). Shot size band for shells. **Charge loads**: subsonic, standard, +P (raises pressure toward the *case's and gun's* ratings). **Case**: metal, rimfire against centrefire (cheap, low rated), caseless, paper hull. **Payloads** (a material with traits carried in the bullet): tracer and incendiary exist; add explosive filler (reads the filler's `oxygen_balance`), net or rope, flare, ender shard, slime, potion. Payload effects are looked up from a **registry keyed by material trait** (the `modular/ModularTraits` shape) when the bullet lands; a pack adds a trait and an effect, and no item kind. The **subsonic load** is the first thing to build here: it is what makes the suppressor fix visible.

## 4. How to add an option (recommended shape; the author has not ruled)

**Built 2026-10-21** (compile-only, unplayed): fittings are a data table, `data/feedback/gun_option/*.json`, loaded by `GunOptions` (a `SimpleJsonResourceReloadListener`, a record with a `Codec` and a `StreamCodec`, synced to the client). To add a fitting: a JSON row, a part form (`form_type/`), a machining step, a `gun.json` role if it needs one, lang `gun.feedback.fitting.<id>`. A new *effect channel* is a field on `GunOption` plus a line in `GunDesign.effects()`. A gun option's record has fields only for gun-owned inputs (e.g. `kick_gas_cut`, `heat_leak_mult`, `muzzle_report_mult`, `mass_mb`, `slot`, `fits_launchers`) and **none for any round figure**; a round option's record has none for any gun figure. The bake reads the active options and applies them; nothing keys on an id. A new figure channel is a code change on purpose, so the ownership column is decided when the channel is added. Rejected, and why: a generic `{addend, percent, multiplier}` modifier list with scripts, which is what TacZ does and is how a grip ends up setting a bullet alight (guns spec §13.2).

Whichever route: every option is **discrete** (named choices, never a free scalar), **a physical part** with a mass and a cost in stock, **read by property**, and has a **trade** (never a strict upgrade). It re-runs the bake. Figures go in `FTuning`. Units are types (`Pu`, `Mu`, `Tu`...; CLAUDE.md, *Units are types*). Display through `Readout.figure`. Each adjective in a tooltip is glossed on Shift from the constant that decides it.

## 5. Things that will bite

- **`GunGameTests.gunsBakeTheSameThroughTheCore`** pins that a gun assembled from parts bakes the same `GunFigures` as the paper design. A new option must keep that true: if the part assembly and the paper design disagree about an option, the test fails. Run it only if the author asks (CLAUDE.md), but design so it would pass.
- **Philosophy 11, no identity checks for the player**: compatibility is by property (fits the bore, thread, rail, launcher), never by id lists. TacZ's allow-lists are the thing not to copy.
- **Wear only from misuse.** Guns are tools and wear with use (guns spec §6), a liner wears from firing; overpressure is misuse and is reported separately.
- **Hazard band**: a bad gun can hurt the shooter, only through its own spec sheet, never uptime (guns spec §3.2).
- **The wiki** (`wiki/*.md`) is player-facing and goes stale: it still describes the Assembly Bench in several pages; add or update an article for anything a player sees.
- **Placeholder art**: reuse existing textures; any new copied file gets a row in `THIRD-PARTY-LICENSES.md` in the same change. No Create assets, ever.
- **JEI**: a new part form or round should show in `compat/jei/`.

## 6. Open, for the author

- Whether projectile shape is made at the Reloading Press or the Tool Table (shape is forged; where the anvil is).
- The battery: capacity, whether it leaks, and what besides a thermal scope it powers.
- Gun part variants through the modular variant machinery (guns spec §13.6 step 2) or through options above: the gun roles have no `part` blocks today.

## 7. Where to look

`weapon/` (GunDesign, GunFigures, GunAssembly, Cartridge, GunAmmo, ReloadingPress*, GunTable*, GunShots, GunSound, GunTooltip, GunCondition, GunHazard), `client/GunTableScreen.java`, `data/feedback/modular_type/gun.json`, `data/feedback/machining/`, `data/feedback/form_type/`, `core/FTuning.java` (`GUN_*`), `gametest/GunGameTests.java`, `modular/ForgeRhythm.java`, `modular/ModularTraits.java`, `docs/feedback_guns_spec.md` §13, `TODO.md` *Guns* and *Modular items*.
