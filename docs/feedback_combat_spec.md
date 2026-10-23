# Combat system -- working spec

**Status: Draft.** Working document, not one of the three core documents. Written 2026-10-21 from roadmap item 4 (`docs/feedback_roadmap_for_the_next_interaction.md`). Built since: all ten phases of `docs/feedback_combat_implementation_plan.md` §4 are committed (compile-only or unplayed in part; see `docs/feedback_overnight_checklist.md`). Where it disagrees with `feedback_philosophy.md`, the philosophy wins.

## 1. The question

Adopt a semi-novel combat system, and which. Maintainers split: one favours Better Combat (attack animations, combos, swept hitboxes), the author leans to Project War Dance (posture, parry, stagger). Brief: find a middle ground.

## 2. What Feedback already has

Modular melee (`docs/feedback_modular_items_spec.md` 2.1, 5.4) bakes, per weapon, from part material and geometry: **reach** (total length), **swing speed** (falls with moment of inertia about the hand), **impact** (head mass times swing speed), bite, strike, joint load. Variants carry a signature trait (claymore overhead sweep, rapier flurry, dagger backstab, spear jab/charge). Hand tools use `HandSwing` tap/blow/heavy with hunger per St. Vanilla attack is a single click, one cone, cooldown-gated.

Gap: every weapon swings the same way. Geometry changes the numbers but not the motion or the hitbox. That is the one thing a combat layer could add.

## 3. Prior art (licences checked before any code was read; none read)

### Better Combat (Zsolt Molnar / ZsoltMolnarrr, "Daedelus" on CurseForge)
- Melee combos, per-attack animations, upswings, multi-target swept hits using oriented boxes (OBB/SAT) instead of vanilla's point pick.
- Data: `weapon_attributes/<item>.json` per item (or a `parent` preset such as claymore): hold style, `attack_range`, list of attacks each with damage multiplier, `hitbox` shape, `angle`, conditions, animation, sounds. A `fallback_compatibility.json` can assign by item id; 1.21 also allows an item data component carrying a preset.
- Versions: repo has a `1.21.1` branch with `neoforge/`, so a NeoForge 1.21.1 build exists.
- **Licence: All Rights Reserved** (LICENSE file, copyright 2022 Zsolt Molnar, on both default and 1.21.1 branches; GitHub reports no SPDX licence). Code and assets: no adapting, no bundling, no jarJar. Design (the idea of data-defined attack lists and swept hitboxes) is free to take; code is not even to be read for adaptation. `THIRD-PARTY-LICENSES.md` rule "read the design, write the code" is stricter than needed here.
- Mods that change attack range are listed incompatible: our baked reach would fight it unless we feed it through its own attribute, which is a compat task.

### Project: War Dance (Jackiecrazy)
- Posture bar per entity (size and armour), filled by hits; full bar = stun (armour stripped, cannot move or attack). Parry window about 0.5 s reflects damage and pushes posture; sneaking blocks all directions. Attack variants by state (standing, sprint, fall, aerial, counter). Also dodge, kicks, grapple, style/adrenaline/spirit meters, skills.
- Versions: 1.20.1 (Forge, NeoForge); no 1.21.1 build found. Source public (GitHub Jackiecrazy/WarDance).
- **Licence: CC BY-NC 3.0** (CurseForge page; sites list NC 4.0). Non-commercial, and NC is incompatible with GPL-3.0 code; assets are not ours to take. Design only. No 1.21.1 build, so no compat route either.

### Verdict on prior art
Neither can supply code. Better Combat is usable only as an optional runtime compat mod; War Dance only as ideas.

## 4. Fit with Feedback's rules

- **Properties, not ids.** Better Combat's native assignment is per item id/preset. Feedback must not hand-author a JSON per weapon: baked designs have no rows. The data component route is the only fit.
- **Wear/force goes into the thing.** War Dance's posture is a stat bar that stuns. Feedback's version should be a consequence of physics already baked: a blow's impact against the target's mass and a shield's/weapon's joint.
- **Discrete options, flat difficulty, no new gate.** Combos and parry timing must not become a skill gate that locks a weapon; the forging rhythm minigame (modular spec 4.1) already sets the precedent that timing games are for making things, and combat feel should stay a vanilla-ish click.
- **No new input if avoidable** (modular spec 5.4 lens): sneak, sprint, charge, shield already exist.

## 5. Middle ground: what to take as design

Drive attack shape from the bake, with no per-weapon rows:

| Bake figure | Drives | Rule sketch |
| --- | --- | --- |
| reach | hitbox length and attack range | the swept box is as long as the weapon; a spear stabs a thin deep box, a sword sweeps a wide shallow arc |
| head shape (a part geometry enum: edge, point, blunt, hooked) | box width and angle | edge = arc, point = line, blunt = short wide, as a lookup on the existing part kind, not on an item |
| moment of inertia | swing duration and recovery | already is swing speed; reuse as animation length |
| head mass | knockback and a posture-like `stagger` | impact over target mass; heavy hits stagger light mobs |
| joint load | what a bad parry or a hit on a shield costs | the existing joint, not a new bar |

Posture, the physical way: **stagger** is not a bar but the existing impact figure compared with the target's `Mu` and armour. A blow whose impact beats the target's mass-scaled threshold interrupts it (a short hurt-stun); a shield (a baked design too) turns excess impact into joint load and heat, matching the approved "shield as thermal and kinetic sink" idea in `TODO.md`. This keeps parry/posture as a consequence, never a stat.

**Minecraft lens:** vanilla gives the cooldown charge, sneak, sprint, crit-on-fall. Key every attack form off those (charged full swing = sweep, sprint = lunge, fall = crit/overhead). The twist the world adds: mobs have a hitbox, not a mass, so the target's `Mu` must come from its bounding box volume times a flagged density (flagged exception, the way redstone is). **[OPEN]**: that stand-in.

## 6. Options and costs

| Option | What | Cost | Risk |
| --- | --- | --- | --- |
| A. Do nothing | vanilla click | none | weapon geometry stays invisible |
| B. Own swept hitbox only | server-side swept OBB per attack from reach and head shape, vanilla animation; multi-target; spear charge already planned | medium: attack event or custom ray, client prediction of picking (the awkward part noted in modular spec 5.4) | small, ours, fits rules |
| C. B plus procedural animation | client pose from swing figures via `IClientItemExtensions` arc driven by inertia; no Blender assets | medium-high, art-light | animation polish ceiling is lower than Better Combat's |
| D. Better Combat as optional compat | ship a data component or item-tag preset per head shape/variant so a baked weapon picks a Better Combat preset; reach fed through its attribute | low-medium; needs 1.21.1 NeoForge build to be current; ARR licence means dependency only, never bundled, never in our jar | its animations are per-preset, not per-geometry; two hitbox systems if both on; licence of linking GPL code to an ARR mod unclear for distribution (not bundled, so likely fine, needs a ruling) |
| E. War Dance style posture/parry | stagger layer on impact vs mass, shield sink | medium; design-only | balance; must not become a stat bar or a skill gate |

## 7. Recommendation

1. Build **B** first: swept hitbox from baked reach and head shape, multi-target, no animation work. It is the part that makes geometry matter and needs nothing external.
2. Add **stagger** from impact against target mass (E, physical version) once B lands; shields convert it to joint load and heat.
3. Add **C** procedural poses per head shape after; leave authored animation out.
4. Offer **D** as an optional compat only if the maintainers still want Better Combat's look: presets selected by head shape via a data component, strictly optional, never a dependency, not before B.
5. Do not copy either mod's data model (per-item JSON; stat bars).

## 8. [OPEN]

- **[OPEN]** Mob mass stand-in (bounding volume times density) and whether the flagged exception is acceptable.
- **[OPEN]** Whether the maintainers accept B+C over Better Combat compat; the question for the author is whether animation polish matters more than staying row-free.
- **[OPEN]** Linking Feedback (GPL-3.0) to an All Rights Reserved mod as a soft dependency: ruling in `LICENSING.md` needed before D.
- **[OPEN]** Head-shape enum: reuse part kinds or add a `head` field to `ModularType` forms.
- **[OPEN]** Interaction with `HandSwing` tap/blow/heavy: hand tools stay out of this; only weapons use swept hitboxes.
- **[OPEN]** Attack speed haste scaling (roadmap item 5) lands in the same attack path.

## 9. Author's direction (2026-10-21) -- what combat should *add*

Written down at the author's word; nothing here is built or specced in detail. Sections 5-7 above are the agent's first pass and are superseded where they disagree.

### 9.1 Why not just Better Combat

Better Combat changes how swinging *looks and feels* (haptic, impactful, and the author admits it feels good) but adds no depth beyond dual wielding. If anything it removes nuance: hold-click replaces tapping on cooldown refresh for max damage. Adopting it alone would make combat feel better and play shallower. Feel is wanted, but as a layer over depth, not instead of it.

### 9.2 Wanted depth (Project: War Dance as the model, design only; CC BY-NC, see §3)

- **Rolls and dodges.**
- **Parries** (see 9.3, the headline feel target).
- **Dual wielding.**
- **Posture** -- maybe; the author is unsure how it works. Section 5's physical stagger (impact against target mass) is the candidate version.
- **Kicking and grabbing.** Grabbing is *not* a free hook-on-anything: it needs an item. Grapples are the interesting case (a grapple as the item that makes a grab possible).
- **Skills -- the biggest part.** A combat skill tree. Includes per-weapon-type skills and gun-fu (close-quarters gun technique). **[OPEN]** how a tree fits the rules: a skill must unlock *technique*, never a hard gate on capability (philosophy 7, "Precision is never a hard gate"; "if a proposal needs 'requires tier N,' it's wrong"), and nothing keyed on item id. Candidate shape: a skill changes a physical figure or opens a move the body could always attempt badly, in the same way a tool design does, rather than gating content.

### 9.3 Parry feel target (the most important part, "not really")

Parry must feel *really* good -- Ultrakill as the bar: impact frames (flash on the successful parry; no hit-stop, see below), screen and camera punch, sound and particles that sell the connection. A parry that merely negates damage is a failure of this item. Hit-stop was tried and removed (author ruling, 2026-10-21): Ultrakill stops the world but not the player, which cannot be done in multiplayer, and a render-copy freeze of the local view only reads as an FPS hitch. The impact-frame flash, camera punch, FOV pop, sound and sparks carry the feel; a stronger flash or shake may stand in. No world or view freeze.

Reference to study: **Just Parry** (CurseForge, `just-parry`), which the author says is **Creative Commons 4.0** and does this well. Before reading its code: confirm the exact CC variant (BY / BY-SA / BY-NC / ND) from its repository, since NC and ND clash with GPL and BY-SA direction is one-way; per CLAUDE.md, check the licence first, clone outside the repo on the 1.21.1 branch, have `prior-art-reader` report design only. **[OPEN]** licence variant; whether 1.21.1 NeoForge build exists.

What the feel work likely includes (a design list, not a decision): timing window and its reward, parry flash/impact frame, attacker stagger, sound, camera shake, the effect on a projectile (deflect/return -- gun-fu tie-in). Mechanically a parry should be physical where it can: block with a held weapon, the force going into the weapon (joint load, wear by misuse) rather than a free negate, consistent with the shield-as-sink idea in `TODO.md`.

### 9.4 Revised plan shape

1. Own swept hitbox (§7 step 1) stays the foundation: depth needs real hit geometry.
2. Parry + dodge/roll + stagger next, with the feel layer (flash, shake, sound) built *with* the parry, not after.
3. Dual wielding, kick, grab/grapple.
4. Skill tree last of the structure work, once there are techniques for it to modify.
5. Better Combat only as optional presentation compat (D), if at all; animation polish is not the priority over depth.
