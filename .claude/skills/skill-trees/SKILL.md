---
name: skill-trees
description: Design and build content for Feedback's combat skill trees (data/feedback/skill_tree/*.json, cast techniques in combat/cast/). Use whenever the user wants a new skill, a new cast ability, a new tree or path, rebalancing a tree, or asks what a tree should be about. Covers what makes a path a path, the cast/rider/support split, the schema, and the files to touch.
argument-hint: <tree or technique idea>
---

# skill-trees

Read `docs/design_digest.md` (required before any `src/main` edit) and `docs/feedback_skill_content_spec.md` (§14 is the test every node must pass). The War Dance mod is the reference for *shape only*: licence is unclear (CC BY-NC against our GPL), so no code, text, names or assets, mechanic shapes restated from scratch. Source is cloned at `~/Documents/GitHub/WarDance-reference` for reading.

## Seven trees, one pool each (the current model, 2026-10-24)

**The full definitions are in `docs/feedback_skill_trees.md`. Read it before adding or moving a node.** The summary:

- A **tree** is a theme and has its own pool of points. There are no tabs, no sub-trees and no pools shared between trees.
- A tree covers **every domain**: gun, parry, fist, weapon, grip, movement, support, status. It is never a "melee tree", a "parry tree" or a "fist tree".
- A tree differs from the others in **how** it uses those domains. Its style is the one-line definition, e.g. Brute overpowers with every domain, Anchor holds ground with every domain, Veil acts unseen with every domain.
- The seven: **Brute** (overpower), **Anchor** (hold ground), **Veil** (act unseen), **Onslaught** (relentless rhythm), **Observation** (see and reach), **Proficiency** (survive), **Chaos** (break things).
- Proficiency is the survivability tree: heals, stamina, self-buffs. Support lives there.
- A domain a tree lacks must be stated in its definition, not left silent.

Points: each tree earns from one physical act (see the "Points" section of the definitions doc).

Before adding a node, ask which tree's style it serves, and which domain it fills. If two trees could own it equally, it belongs in neither yet.

A new tree is a new `skill_tree/<id>.json`, `skill.feedback.tree.<id>` in lang, a row in the definitions doc, and a pool in `SkillEarning`. Pool id is the tree id. A node's lang key is `skill.feedback.node.<tree>.<id>`, and its ownership key is `tree/node`. Moving a node between trees changes its ownership key (a respec is needed) but not its unlock id, so slotted casts keep working. `requires` stays inside one tree: a tree must stand on its own roots.

**Rule (user, 2026-10-24): a TREE never requires equipment. A single SKILL may.** Never gate a whole tree on an item, a glove, a gun or a gear grant; a node may check for one (a gun in hand, a hot item, a glove that makes it better) and say so in its description. Gear may *improve* a skill (a glove improves the grab) but the skill is what allows it. A tree is for anyone; its nodes can have prerequisites in the world.

The trees and their domains are in `docs/feedback_skill_trees.md`. `docs/feedback_skill_content_spec.md` §15 is the older per-tree record and is being replaced by it.

## Three kinds of node (mix them; supports are a minority)

1. **Cast** (the point): a technique the player *slots to a cast key and pops on purpose*. A distinct, visible set piece with its own stamina cost, rearm clock and physical cause. Registered in `Casts.ALL`. Aim for most of a tree's *interest* to live here.
2. **Rider** (stem payoff): changes an action that is already happening (a parry, a kick, a pistol-whip). Often an exclusive variant of one stem, so a stem forks into *which* payoff (`exclusive` tag, respec to swap).
3. **Support**: a plain figure nudge (`{"figure": ..., "amount": ...}`) that fills a limb and gives points a place to go. Fine, but a *minority* of any tree (spec §14.1). Never a node whose whole content is a number if a cast could stand there.

## Do not duplicate an item trait or an attunement

Items already carry behaviour: **modular traits** (`modular/trait/*.java`, registered in `ModularTraits`: flurry, dash, backstab, overhead, flourish, throw, hook, spear_charge, lunge, slam, kick, trample, bulwark, braced, parry, deflect, blocking, fan, gun_braced, and the rest) and **attunements** (`data/feedback/attunement/*.json`: heat, impulse, edge, momentum, mark, hold, wager, grudge, ...). The split is **what the item does versus what the player learned**: a trait or attunement is the *item's* behaviour, for anyone holding it; a skill is the *body's*, usable with any suitable item. A skill must not be the same effect as a trait or attunement under another name (two systems on one space, and players will not know which one fired). Before adding a node: grep `ModularTraits` and the attunement roster (`docs/feedback_enchant_roster.md`) for the same shape (a dash, a fan, a backstab, a knockback, a mark, a momentum scale). If one exists, either make the skill do something the item cannot (a different trigger, a combo with another skill, a cost on the body) or **improve the trait** (a skill that lets a fan-trait gun fan longer or steadier is fine; a skill that is a second fan is not). Also avoid reusing a trait's *name* for a different thing (the tower shield's trait is `bulwark`, so the parry tree is Stalwart).

## The test for every node (spec §14.1)

Which Feedback system does it eat? Allowed answers: baked gun figures (impulse, heat, spread, cook-off, condition), item/barrel heat and `HotContact`, `BodyMass` and worn armour, material hardness and weapon wear, `Shock`, tethers and grabs, modular reach, player-made ammo, gear grants and attunements. A node a plain combat mod could ship verbatim is suspect. At least two thirds of technique nodes must name such a system in their one-line description. Hard rules: no identity checks (families, tags, forms, figures only, never an item id); discrete choices not dials; **one meter only, stamina** (user ruling 2026-10-24), every other cost is a physical act (heat, ammo, wear, a held hand, a rearm that needs an act); a cost the player can ignore by waiting is not a cost.

## Making a cast pop

A cast should be something the player *tells a friend about*. Checklist:
- A **moment**: a windup, an arc, a landing, a burst, not an instant stat. Particles and a distinct sound are part of the design, not polish (the Coin Flip shipped invisible and had to be redone).
- A **read on the world**: damage, stagger or range come from a baked figure (mass, impulse, temperature, hardness, fall height), never a flat number alone.
- A **decision** in the cost: stamina plus a rearm that needs an act (`strike`, `parry`, `stagger`) or time. Use act rearms to chain casts into a fight (Whirl rearms on a stagger); use `time` for burst. A strike rearm also self-recovers after 30 s out of a fight.
- A **counterplay or risk**: a hot barrel cooks off, a landing leaves you open, a throw needs a stagger.
- Feel before numbers: constants are `[OPEN]` stand-ins until played; flag them.

## Schema (data/feedback/skill_tree/<tree>.json)

`{"tree": id, "nodes": [{id, cost, requires:[ids], effect, branch?, exclusive?, rearm?}]}`. `effect` is exactly one of `{"figure": F, "amount": n}` or `{"unlock": "<move>"}`. Figures are the `SkillTree.Figure` enum (add one there plus stream codec is automatic, plus a lang `skill.feedback.figure.<name>` line). `requires` is same-tree only; the **first** requirement is the node's parent for layout (the tree screen lays out top-down by first requirement, a parent centred over its children). `exclusive`: nodes sharing a tag cannot both be owned. `needs_trait`: a modular trait id (the same ids as `ModularTraits`, e.g. `dash`) the held weapon or worn armour must have; enforced centrally in `Skills.unlocked`, so casts and riders both go quiet in the wrong hands, while `Skills.owned` still says the player has it (slotting works regardless). The HUD chip shows "wrong gear" and the skill screen names the trait. Put it on a node, never a tree. `rearm`: `time|strike|parry|stagger`, else inherited from the first requirement that has one, else `time`. Unlock ids are move names; `Skills.unlocked(who, move)` is true for a bought unlock **or** worn gear granting `feedback:<move>`. Skills gate **moves only**, never a machine or a recipe.

## Adding a cast: files, in order

1. **Node** in the tree JSON: `{"id":"x","cost":60,"requires":["parent"],"effect":{"unlock":"x_move"}}`, optional `"rearm"`.
2. **Act** in its own file `combat/cast/<Name>.java` (a `static boolean act(ServerPlayer)`, with its own state maps and event hooks; `Casts.java` keeps only `ALL`, firing, `later`, `near`, `ahead`, `stagger`), registered in `Casts.ALL` as `<Name>::act`: it returns whether it happened (a failed act costs nothing), added to `ALL` as `new Cast("x_move", staminaCost, act)`; use `new Cast(id, cost, false, act)` when the move has its own cooldown (kick, grab) so it takes no rearm. Delayed beats: `later(p, ticks, runnable)`. Stagger a target through `stagger(p, target, joules)` (mass-based, and it rearms `stagger` casts). `Casts.fire` spends stamina and the rearm *after* the act, so a stagger the act deals does not rearm it at once.
3. **Server-only state** (arcs, landings, pending bursts) in `Casts` or a sibling class, keyed by player UUID, cleaned on tick; nothing client-visible but the effects. The HUD/skill screen read only `CastState` (slots, stamina, waits).
4. **Lang** (`assets/feedback/lang/en_us.json`): `skill.feedback.node.<tree>.<nodeId>`, `skill.feedback.move.<move>`, `skill.feedback.move.<move>.desc` (what it does, what it reads, its price; the screen shows the desc). Without the node key the id shows raw.
5. **Docs**: one line in `docs/feedback_skill_content_spec.md` under the tree's section naming the system it eats; `TODO.md` if scope changed.
6. **Verify**: `./gradlew compileJava -q`; JSON parses (`python3 -c "import json;json.load(open(...))"`) for the tree and lang; every node in the tree has a node lang key; every `requires` id exists in the same tree; exclusive groups have at least two members. Then ask the user to play it with a short checklist. Deploy (`./build-and-deploy.sh`) only after they confirm. Do not run game tests unless asked.

## Adding a rider or a support

Rider: the hook lives where the action happens (`Parrying`, `Kicking`, `GunBludgeon`, `MeleeStances`, `SkillEarning.onDamaged`). Gate with `Skills.unlocked(p, "move")`, spend with `Rearms.spend(p, "move")`, and listen for rearm with `Rearms.on(p, Rearm.X)` where the rearming act happens. Support: just the JSON plus the figure being *read* (add the reader in `Skills` if the figure is new).

## Adding a tree

New `skill_tree/<id>.json`; lang `skill.feedback.tree.<id>` (and a short aspect line, `skill.feedback.tree.<id>.desc`, for a tab tooltip once those exist); a pool in `SkillEarning` (what earns its points: a physical act in that aspect, never grinding); a row in this skill's path table. Give it a root cast early so the first purchase is exciting, and fork into at least two limbs with different *kinds* of play, not different numbers.

## Pitfalls that have bitten

- **An invisible cast is a failed cast**: the coin was a particle nobody could find. Show where things are, in-world.
- **Heavy things must not zero the damage**: the coin's mass scaled a 70-damage round to 3. Clamp any "loss" so a cast is a gain or a mild cost, never a nerf.
- **A held key must not repeat a swing**: no hold-to-attack (user ruling). Edge-detect presses on the client.
- **Contextual triggers get accidentally fired**: wall kick needs a fresh tap, sprint momentum and a wall faced. Make gestures deliberate.
- **Cast keys collide**: defaults Z, Y, G; check the controls screen for red brackets before adding keys. Prefer slots over new keys.
- **Layout**: a stem with many direct children makes a wide fan; insert a support node as a hub so a limb groups its children.
- **Pushing, committing**: do not push unless asked (then use `gh`); do not commit unless asked.
