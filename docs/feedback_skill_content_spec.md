# Skill tree content -- working spec

**Status: Built 2026-10-24, unplayed** (everything except Hitch and Conduct, which stay [OPEN]; see TODO.md for the play checklist). Written 2026-10-24 after the skill tree screen restyle. Follows `docs/feedback_combat_implementation_plan.md` 2.7 (pools, trees, `skill_tree/*.json`) and `docs/feedback_combat_spec.md`. Where it disagrees with `feedback_philosophy.md`, the philosophy wins. Node ids and tuning figures are placeholders until played. Built deviations: Pry Open and Last Stand sit in the parry tree; the rearm record is server-only; the bayonet part's blade figures are read from its material hardness; Wall Smash and Throw read a stand-in armour mass (`BodyMass.armour`); item models for the coin form were copied by hand because `runData` fails on a missing `substrate` texture.

**Read §14 first if short on time:** it says what this system is for. War Dance is the reference for *shape*; the point of the system is the Feedback ecosystem under it.

## 1. The problem

The four trees (`parry`, `evasion`, `gun_fu`, `melee_and_tether`, 22 nodes) are mostly flat figure nudges: `long_roll` +1 block, `extra_iframes` +2, `wider_window` +2, `blade_edge` +0.1 stagger. A point spent buys a bigger number and no new decision. Each tree funnels to one end unlock (`riposte`, `wall_kick`, `spin`), so each has one answer, which breaks the digest's "a problem with one answer is not a problem".

Goal: a node should change *what the player does*, or give a second correct way to do something the first node already does. Figure nodes stay, but as the cheap support tier under real techniques, not the bulk of a tree.

## 2. Prior art (design only)

**Project: War Dance** (Jackiecrazy), read 2026-10-24 at `origin/rework` (commit `3f44e2c`, 1.20.1; no 1.21.1 branch). Licence is unclear across its own files: `gradle.properties` says MIT, `LICENSE.txt` is the Forge MDK's LGPL 2.1, and the CurseForge page, per the author, says **CC BY-NC**. NC cannot sit in a GPL-3.0 work, so **no code, text, names or assets taken**; only mechanic shapes, restated here from scratch. Skill names below are Feedback's own. See `THIRD-PARTY-LICENSES.md`.

Patterns taken (shapes, not content):

1. **Parent with swappable payload.** One trigger, rearm rule and state machine; each variant overrides only the payoff. The player picks the payoff, the trigger stays fixed.
2. **Rearm paid in play.** A skill's cooldown shortens when the player does the thing the skill is about, so aggression recharges an aggressive skill and caution a defensive one.
3. **Two-stage marks.** Set a mark, build it, choose when to cash it in.
4. **Reads a state, not a number.** Target stagger, an empty off hand, airborne, sprinting, how the last blow landed.
5. **The price rides on the variant.** The risky version leaves the player exposed when it fails.
6. **Chained stances.** Each move's condition is the previous move's result.

Rejected: spirit/might/adrenaline meters as a universal currency, percent-per-lost-health scaling, skill "effectiveness" dials, magic skills with no physical cause (curses, petrify, time stop), and any identity check on a skill or item. Costs here are physical acts, which is the digest's rule for upgrades and for the world/player-body split in the combat plan.

## 3. Structure changes

### 3.1 Stems and variants

A **stem** is an unlock node that opens a trigger (`riposte`). Its **variants** are sibling nodes that require the stem and share a new `exclusive` tag, so only one per tag may be owned at a time. The trigger and rearm live in the stem; the payoff lives in the variant. Respec already refunds every node, so swapping is a respec. This is the Tetra "mutually exclusive improvement group" Feedback already borrowed, applied to skills.

Schema: `Node` gains `Optional<String> exclusive`. `Skills.buy` refuses a node whose `exclusive` tag matches an owned node in the same tree. The screen draws the sibling group joined by a bracket.

[OPEN] whether a second variant should be buyable at a premium, so players can own two, or only by respec. Default: respec only, which is what makes the choice a choice.

### 3.2 Rearm kinds

Each stem names one rearm rule from a closed set (discrete, per the digest), kept in the tree JSON as `"rearm"`:

| Rearm | Meaning |
| :--- | :--- |
| `time` | A fixed number of ticks. |
| `strike` | Spent until the player lands a melee hit; the hit rearms it (the technique is a reward for aggression). |
| `parry` | Spent until the player completes a parry. |
| `stagger` | Spent until the player staggers a target. |

The rule is per stem, not per variant: variants differ in payoff, not in clock. A stem may also take the player's own earlier rearm rule as a sibling variant when the choice is the point (see 4.1, Answer).

### 3.3 New figures

Add as `Figure` entries only where the number is the whole effect and the choice is not in the figure. Likely few: `rearm_ticks` (time rearm length, negative shortens), `kick_wall_bonus`. Everything else is an unlock.

## 4. Content by tree

Cost scale follows the existing trees: 20 to 30 for a first node, 40 to 60 for a technique, 70 to 90 for a capstone.

### 4.1 Parry (stem `riposte`, rearm `parry`)

`riposte` already exists as a bare unlock at `Parrying.java:248`. Make it the stem, and its payoff one of three:

- **Throwback.** A parry made with the off hand empty returns the attacker's stagger doubled, and opens the next blow as a free counter. Cost: you need a free hand, so no shield or dual wield while this is your pick.
- **Slip.** A parry at the very edge of the window (the last tick) turns into a step-through: the attacker's blow whiffs and the player ends up behind them. Cost: the late tick is the narrow one, and a missed slip leaves normal recovery.
- **Answer.** The first blow after a clean parry is blocked outright; the guard rearms only when the player strikes, not by time. Cost: it rewards fighting, and a player who waits has no extra protection.

Support figure nodes stay: `wider_window`, `quick_recovery`, `tight_redirect`, `hard_stagger`. They sit under the stem. Add **Pry Open** (melee tree, 4.3) and **Last Stand** (4.7) to the parry pool as techniques that need a parry to start.

### 4.2 Evasion

- `long_roll`, `extra_iframes` stay as the support tier.
- **Wall kick** (existing) becomes a stem. Variants: **Wall Smash** (a kick that sends a foe into a wall adds the weight of worn armour to the stagger) and **Wall Rebound** (the kick carries the player off the wall for a second kick without touching ground).
- **Breach Kick.** A kick on an already-staggered target knocks it down; on a steady one it only shoves. Reads the target's stagger state. This is the lesson "kick what is already broken."

### 4.3 Melee (branched by baked family, never item id)

Replace the three flat per-weapon nodes (`blade_edge`, `mace_weight`, `spear_reach`) with state-reading ones:

- **Reach Stances** (two named choices, exclusive): *Far End* (strikes landing at the tip of reach hit harder) and *Close Work* (strikes landing inside half reach swing faster). Neither is the correct one; it depends on the weapon and the fight.
- **Pry Open.** A strike that lands on a blocked or parried weapon binds that hand for a short window. A parried heavy blow disables the shield instead. Cost: it only works on an opening the opponent gave you.
- Existing unlocks (`blade_flourish`, `mace_quake`, `spear_vault`) stay.

### 4.4 Tether

- **Throw.** Pull a staggered target in on the tether and throw it past you. You are in reach on release, which is the price.
- Keep `fast_reel` as support.

### 4.5 Gun-fu

- **Marked Rounds.** A hit leaves a mark stack on the target (a count, shown on the target). The next melee hit spends the stacks as extra damage. Cross-tree on purpose: it asks the player to use both halves of their kit. Two-stage mark, cash in when ready.
- **Dodge Charge.** Dodging through an attack speeds up the next draw or chamber. The smallest clean node; reads a roll that actually crossed an attack.
- Existing `gun_fu_pistol_whip`, `chamber`, `dual`, `spin` stay.

### 4.6 Dual wield (new small branch, in melee)

- **Feint.** Cancel a main-hand swing into an off-hand strike; the target flinches. Fits the LMB main / RMB off split in `OffhandStrike`.
- **Draw Switch.** Swapping to a weapon of a different baked family resets the swing timer once. Cost: you carry two kinds of weapon.

### 4.7 Last Stand (pool: parry, as a state)

Taking a hit at low health makes the next dodge a free parry window. A state, not a percentage: "low health" is a named line, not a scalar that scales.

### 4.8 Unarmed (new tree, `fist`)

A stance chain keyed on movement state, using the Feedbacker punch and kick as the base:

- **Jab.** Standing strike with a little extra reach.
- **Lunge** (needs Jab). A sprinting strike that guards briefly and staggers harder.
- **Sweep** (needs Jab). A standing arc hit across a wide sector.
- **Chop** (needs Lunge). Falling strike that drives the target down.
- **Uppercut** (needs Chop). Rising strike that launches.

Each move's condition is the previous move's result, so the decision is *which state to be in next*. Pool earned from landed unarmed blows (the Feedbacker, hands, kicks), per `SkillEarning`.

[OPEN] whether the chain is one exclusive ladder or all unlock together (default: unlock together, with the chain as a combo bonus, not a gate). Needs a play pass.

## 5. Hooks required (check each against current code before building)

| Hook | Where it hangs | State |
| :--- | :--- | :--- |
| `exclusive` tag and refusal | `SkillTree.Node`, `Skills.buy`, screen bracket | New |
| Rearm kinds and a per-player rearm record | `SkillPools` or a `CombatState` field, synced to owner | New |
| Throwback / Slip / Answer payoffs | `Parrying` payout, near `Parrying.java:248` | `riposte` exists as a stub unlock; verify what it does today |
| Breach Kick | `Kicking`, reads target stagger from `CombatTicker` | Verify the stagger read |
| Wall Smash / Wall Rebound | `WallKicking`, `Kicking` | Wall kick exists; verify the wall-hit event |
| Reach Stances | `MeleeMoves` / `MeleeSweep`, hit distance known to the sweep | Verify |
| Pry Open | `Parrying` on the attacker side | New |
| Throw | `Tether` / `Grabbing` | Verify grab release hook |
| Marked Rounds | Gun hit event plus a mark stack on the target | New |
| Dodge Charge | `Rolling` crossed-attack read, gun draw timer | New |
| Feint | `OffhandStrike` swing cancel | Verify |
| Draw Switch | `Wielding` family change | Verify |
| Last Stand | `CombatState` plus damage event | New |
| `fist` tree and stance reads | New earn kind in `SkillEarning`; `CombatPhase` for sprint/fall/rise | New |

## 6. Hard-rule check

- **No identity checks.** Weapon-specific nodes key on baked family or tag; Pry Open reads "weapon was blocked or parried", not an id.
- **Discrete options.** Stances, variants and rearm kinds are named choices; no dial gets a point-by-point slider.
- **Two correct answers.** Every stem has a payoff choice with a different cost; Reach Stances is a pair.
- **No hard gate.** Skills gate *moves* (the combat plan's world/player-body split), never a machine or recipe, as `Skills.unlocked` already says.
- **Meters.** One: stamina (user ruling 2026-10-24), spent by cast techniques (`combat/cast/`: three slots, keys Z/V/G, equipped from the skill screen). Nothing else. Costs are a held hand, a missed late tick, an exposed position, or a rearm that needs a specific act.

## 7. Order of work

1. Schema: `exclusive`, `rearm`, screen bracket. Small, unblocks everything.
2. Parry stem (3 payoffs): the first real test of stems and rearm.
3. Evasion and kick state reads (Breach Kick, Wall Smash).
4. Melee stances, Pry Open, then Throw.
5. Gun marks, Dodge Charge, dual wield pair.
6. `fist` tree, once 1 to 5 are played and the stance chain has a fair test.

Per CLAUDE.md, verify by play, not game tests: each step ends with a short in-game checklist.

## 8. Open

- **[OPEN]** Two variants of one stem owned at once (default: no).
- **[OPEN]** Fist chain as ladder or free set (default: free set, combo bonus).
- **[OPEN]** Whether support figure nodes should be folded into their stem as "ranks" so the screen is not a long list of +N nodes.
- **[OPEN]** Point sizes for the new `fist` pool and whether Last Stand earns from the parry pool or its own.

## 9. Minecraft lens

What the world changes about a technique that would be a flat move elsewhere (brainstorm skill, compendium-style). Where there is no twist, say so.

- **Wall Smash, Throw, Breach Kick: the terrain is the second hit.** A shoved or thrown target meets whatever stands there: a wall, a drop, lava, cactus, powder snow, water that douses, a cobweb that holds. A flat game scales the damage; here the *place* decides, so the same skill is a different tool in a cave, a base or a nether bridge. Feedback adds its own hazards: a hot workpiece or furnace face (`HotContact`), a live wire or fence post (`Shock`, `spec_electrified_fence.md`). A thrown target into a live fence is the intended payoff of Throw for a player who built the fence. No new damage code: the skill moves the body, the existing hazard code does the rest. That keeps the digest's "consequences of an item being hot follow anywhere" true for people.
- **Mass is already physical.** `BodyMass` gives every entity a stand-in mass from its bounding box, and the kick's knock is impact over mass. Wall Smash's "armour weight" and Throw's reach should read that mass and the worn armour's real weight, not a stat. A skeleton flies, an iron golem barely moves, a spider is a sweep target. Breach Kick on a mob that has no stagger state (a creeper?) needs a ruling: default, it only shoves.
- **Slip, Rebound and Dodge Charge lean on blocks:** a roll or wall kick needs a wall; a trench, a ladder or a door frame changes the geometry. Doorways are where Reach Stances' Close Work earns its place.
- **Reach and the held weapon are baked from geometry** (`feedback_modular_items_spec.md` 2.1). Far End and Close Work should read the baked reach and moment of inertia, so a long spear and a dagger want different stances by their own build. No twist beyond that; it is how the stance is keyed.
- **No twist found** for Last Stand and the rearm kinds; they are pure player-body state, which the combat plan already allows to follow game logic.

## 10. How a technique is learned

Points are one route. The combat plan already adds a second: worn gear that *grants* a move (`CombatGear.granting`, `Skills.unlocked`). Content guidance:

- **Teaching gear carries techniques, not stats.** A buckler could grant Answer, a grapple curio Throw, a sash Slip. Wearing it is the choice, and it competes with the same slot as other gear. No new system; the node's `unlock` id is the grant id.
- **Variants a pack author can add.** `exclusive` groups and rearm kinds live in the tree JSON, so a datapack can add a variant to a stem without Java, provided the hook for its payoff exists. Payoffs are therefore the closed set in Java; trees are open.
- **Challenge unlocks [OPEN].** A technique unlocked by a deed (survive a fall into a wall, parry a projectile in a doorway) instead of points would suit the physical-feel unlocks (Wall Rebound, Slip). Not adding it unless playtest shows points alone read as grind, since `SkillEarning` already ties points to doing.

## 11. Mobs

`Skills` returns no modifier and nothing unlocked for a non-player. The armed skeleton spec (`spec_armed_skeletons.md`) lets mobs fight with the kit. Do not give mobs trees. If a mob is to use a technique, it carries it as a behaviour on the mob (a skeleton that parries is the Throwback payoff for free), keyed on the same hook the player's variant uses. Keep that out of scope here, but build payoff hooks so they take an `Entity`, not a `Player`, where the cost is not a UI.

## 12. Presentation

- A node's description states its price in the same line as its gain ("Answer: first hit after a clean parry is blocked; rearms only when you strike"), because the price is the design.
- A stem's variants are drawn as a group on the screen, with the owned one lit.
- Cost labels for points follow the existing pools; no new currency.

## 13. What would make a skill a bad idea

Cut a proposed node if it fails any of these, and say why in the spec:

1. It is a number with no choice attached (fold it into a stem as a rank).
2. It is correct in every fight (no second answer).
3. Its cost is a meter or cooldown a player can ignore by waiting.
4. It needs an item id to work.
5. It does damage with no cause in the world (a curse, a spell).

## 14. What makes this Feedback and not a War Dance clone

Author's brief (2026-10-24): this is a War Dance-*inspired* system. The reason to build it in Feedback instead of playing someone else's spiritual successor is to let it feed on what Feedback already has. A tree that would work unchanged in War Dance has failed. Sections 4 to 8 are the shape; this section is the substance.

### 14.1 The test

For every node ask: **which Feedback system does this eat?** Allowed answers: baked gun figures (impulse, heat, spread, cook-off, condition), item or barrel heat and `HotContact`, `BodyMass` and worn armour weight, material hardness and weapon condition (parry wear), `Shock` and the electrified fence, tethers and grabs, modular baked reach and moment of inertia, ammunition made by the player, gear grants and attunements, armed skeletons. A node that answers "nothing, it is a number" is a support rank (§3.3) and must be a minority of a tree. **Target: at least two thirds of technique nodes name one such system in their one-line description.** A node War Dance could ship verbatim is suspect.

### 14.2 Guns are first-class, not a branch

War Dance has no guns; Feedback is a gun mod as much as a combat mod, so the gun tree is the strongest proof of identity and must not be four pistol-whip nodes. Principle: **a gun technique is the body using a physical figure the gun already carries**, so it reads the baked design and varies with the player's own build, not a flat bonus.

What the gun already exposes, from `feedback_guns_spec.md` and the code: baked `impulse` (momentum of projectile and gas, "what recoil and kick are made from", `GunFigures`), `gunMass`, barrel heat and its stamp-plus-tick state, spread that opens as the barrel heats, cook-off of a round left chambered in a hot barrel (`GunHazard`), chamber pressure against the rating, condition and wear, fire mode, the magazine and box feed, calibre classes, the bayonet lug, and the pistol-whip already in `GunBludgeon`.

Proposed gun nodes (tree `gun_fu`, regrouped into three stems; ids are proposals):

**Close work (stem `pistol_whip`, existing).** Variants, exclusive:
- **Brand.** A whip with a barrel above a heat line sears the target (`HotContact` figures), keyed on barrel temperature and not on any item. Price: you shot to get the barrel hot, and a hot barrel is a grip hazard to your own hand (`Grip`/`Handled`). The more you fire first, the harder it brands; the more you fire, the more it hurts to hold.
- **Muzzle Stagger.** A point-blank shot fired straight after a whip lands its impulse as stagger on the target, scaled by impulse over `BodyMass`, the same ratio the kick already uses. Heavy bore, bigger shove; small bore, nearly nothing. Rewards a calibre choice you made at the Gun Table.
- **Bayonet Drill** (needs a bayonet lug part). A thrust with the fixed blade rides the gun's cycle: the lunge is a melee blow using the blade's own baked figures. The part is the gate, not a tag on an item id.

**Recoil (new stem `braced`).** Recoil is momentum, and momentum moves you:
- **Recoil Vault.** A shot fired while airborne or against a surface pushes the player by the gun's baked impulse over the player's mass. A rifle launches you, a pocket pistol nudges. Hot barrel or low condition raises the cost. It is a movement tool for the player who built a heavy gun, so it feeds back into Gun Table choices.
- **Braced Fire.** Shooting from a wall kick, a rooftop edge, or while crouched against a block cuts spread opening from heat for the next burst. Reads terrain, not a number.

**Cycle (stem `chamber`, existing).**
- **Hot Clear.** Eject a chambered round without firing it when the barrel is near the cook-off line. Pays only where cook-off exists, so it is a skill that answers a Feedback hazard.
- **Dodge Charge** (4.5). Keep; it already crosses roll and gun.
- **Spent-Case Draw.** Reload during a roll, with the roll's recovery as the reload's time. Cross-system, small.

**Guns meeting the melee kit (the point of a shared pool):**
- **Return Fire.** A parry that turns a bullet (the Feedbacker does this in `Parrying`) sends it back carrying the round's own muzzle energy, not a multiple. Redirect inaccuracy already tunes it. The bullet is physical, so this is exact.
- **Marked Rounds** (4.5) stays, but its stack is a *powder burn* on the target, not an abstract mark: it persists a short time and only after a close shot, so it is a decision (get close) and not a debuff.

### 14.3 Heat and hot items, beyond guns

`ItemHeat`, `HeatSource` and `HotContact` exist for workpieces and barrels. One generalised technique covers both:
- **Searing Strike.** Any held item above a heat line sears on a landed blow: a forged blade pulled from the fire, a heated barrel. The line is temperature (`Tu`); nothing keys on id. A forge-fresh sword is a weapon for seconds, which makes forging and fighting one loop. Price: hot hand (`Grip`), and the item cools.

### 14.4 Wear, material and the parry

Parry already wears the weapon by the excess (`Parrying`, `WEAR_CLEAN`, per-rating wear). That is a Feedback fact War Dance does not have.
- **Roll With It.** A parry that takes the blow on the weapon's flat turns excess force into extra stagger on the attacker instead of weapon wear. Price: the late tick, as Slip. Reads weapon condition and hardness, so a worn blade wants it more.
- **Pry Open** (4.3) reads the *blocked* weapon's hardness: a soft or worn weapon binds longer.

### 14.5 Mass, terrain, hazards

Throw, Wall Smash and Breach Kick (§9) already use `BodyMass`, terrain, hot faces and live wire. Add:
- **Hitch.** Tether onto a moving vehicle body and be towed (vehicles spec). [OPEN] only if the tether can attach to a Sable body today.
- **Conduct.** A tether onto a live conductor shocks the pull-in target as well as the player; the player's `Protection.shockProofVolts` is the real gate, so a rubber-gloved player is safe. [OPEN] needs a live test; mark as speculative until `Shock` and `Tether` meet in a test world.

### 14.6 Gear, attunements and mobs

- Techniques come through `grants` on gear (§10), so crafting is part of learning. Attunements (`feedback_enchanting_spec.md`) may also grant a technique id on a weapon part; one new rule, not a new system.
- Armed skeletons (§11) are the opponent that makes gun and parry nodes testable; Return Fire and Brand are first demonstrated on them.

### 14.6a Technique plus stock: the coin shot (author's idea, 2026-10-24)

A skill that needs a physical consumable is not new here: Searing Strike needs a hot item, Bayonet Drill a lug part, Throwback an empty hand, Marked Rounds a close shot. The coin shot makes that pattern explicit, so it is the worked example for any later "skill plus item" idea.

**The problem the author named.** Making it only an item feels wrong (anyone can do it). Making it only a skill feels wrong (where does the coin come from?). Both is the answer, and the two halves do different jobs:

- **The skill is the move.** `Coin Flip` is a stem (rearm `strike`) that gives the player the toss and the timing to hit the coin in the air. Without it a coin is a coin: it can be dropped, but there is no move that lines a shot up on it. Anyone *holds* the stock; only the skilled can *use* it.
- **The item is the stock, and it is made.** A coin is a form of a material (`feedback_mechanics.md` §5): stamped from plate by hand (`hand_deformation`) or a press, never found. This matches "ammunition is made, not found" (`feedback_guns_spec.md` §5) and gives the metal economy another sink. No identity check: the coin *form* is what the move reads (a tag on the form, not an item id), so a modded coin of any material works.
- **Second route, optional.** A gear grant (§10) can teach the move: a gambler's glove or sash grants `coin_flip`, so a player who does not want to spend points can wear it instead.

**Material decides the ricochet.** The coin's material, not a skill number, sets what the shot does. Proposed reads, all from existing material figures:

| Coin property | Effect on the ricochet |
| :--- | :--- |
| Hardness | Higher keeps more of the bullet's energy through the bounce and stays bright; a soft coin deforms and dulls. |
| Density | Heavier coins take more of the round's momentum, so the redirected bullet loses more speed. |
| Hardness against the round's | A coin softer than the projectile punches through or flattens; a harder one rebounds. |

So lead is a dead coin, iron a bright one, copper the common middle. Choosing a coin is a real decision, which is the digest's "depth from stacking choices". The bullet's own baked figures (muzzle energy, mass) are the other half, so a heavy calibre and a soft coin do not rebound.

**Variants (exclusive, payoffs on one stem):**
- **Second Mark.** The ricochet redirects toward the nearest target in a narrow cone, with the angle fixed by the coin's hardness. Price: needs a second target in view.
- **Round the Corner.** The ricochet bends the shot around cover to a target the shooter cannot see directly. Price: the coin and the shooter's position decide it, and a bad angle wastes the round.

**Cost and consumption.** The coin is spent when struck. [OPEN] whether a hard coin is recoverable as a deformed coin (a salvage, loop back to the press) or always lost; default: lost, with the metal returning only by remelt, so the economy stays simple.

**Implementation, and the risk.** The coin should not be a real entity. Cheapest honest version: the flip sets a short timer and a computed arc on the player's `CombatState`; a bullet swept past within a small radius of the arc point is redirected. Bullets are already swept in `Parrying` (a bullet attributed to its shooter), so the geometry is plausible. Not verified: how a swept bullet picks its target today, whether redirect already carries a new owner, and whether a ricochet can hit a target the player cannot see. Check each against `GunShots` and `Parrying` before building. A real coin entity is the fallback only if the virtual one reads badly in play.

**Lens.** The world changes this twice: the metal is real (a coin of the wrong material is a bad shot), and the ricochet can meet terrain, since a coin flipped against a wall or ceiling is a different shot. No twist beyond that.

### 14.7 What stays out, on purpose

- **No spirit/might/adrenaline.** Every cost in this spec is a physical act (§2), because Feedback's identity is that the cause is visible.
- **No generic combo meters or style grades.** Players already get depth from stacking physical choices (calibre, barrel, material, tree picks).
- **No skill that cheats the world.** A technique changes the *body*; it does not make a machine, recipe, material or hazard behave differently. The world / player-body split of the combat plan is the guard.

### 14.8 Revised build order for identity

Do the gun stems early, not last. The parry stem (§7 step 2) shows the shape; the next step should be **Recoil Vault and Brand**, because they prove the system eats Feedback's figures. If those two feel good, the rest follows. If they do not, the premise needs rethinking before more nodes are written.

## 15. Tabs and trees (2026-10-24; BUILT unplayed for what is marked, [OPEN] for the rest)

**Rule (author, 2026-10-24): a tree never requires equipment; a single skill may.** Gear improves skills (a glove improves the grab); it never gates a tree.

**Built so far:** tab grouping (a tree's `"tab"` is its pool; sub-tabs on the screen), the split of every tab into its trees, and these new casts: Shadow step, Ambush, Walking fire, Muzzle flash, Dead eye, Hamstring, Dropkick. Also built: Mark and Fire (Marksman, needs a hammer-and-cylinder gun via `needs_trait: fan`), Brace, Shield wall (needs a blocking item), Reckoning, Reprisal. Not built: Wall Run, Vault, Smoke Step, Suppression's Overheat and Pin Down, Marksman's Ricochet and Called Shot, Point Blank, Cleave, Reel In, Swing, Bowling, Rising Tempo, Counter Stance, Stomp, Axe Kick, and the whole Gauntlet tree. Pool shared per tab: points a tab earns can go to any of its trees.

A tab is a school; it holds two or more trees, each with its own aspect and pool (see `.claude/skills/skill-trees/SKILL.md`). Every tree below lists the system its casts eat (§14.1). "Move" means a node that already exists and changes tree; "new" is unbuilt. Stamina and rearm figures are first guesses.

### Parry tab (resolution: be the wall)

- **Stalwart** (hold the line; wear is the resource). Move: `wider_window`, `quick_recovery`, `roll_with_it`, `pry_open`. New casts: **Brace** (plant your feet for a short time: knockback-immune by mass, every blow into the weapon's flat turns into stagger on the attacker, the weapon wears per blow, eats `BodyMass` and weapon hardness and wear); **Shield Wall** (a held shield or weapon bars a doorway cone: anything entering is staggered by its own speed times mass, eats stagger impulse and mass).
- **Counterstrike** (answer force with greater force). Move: `riposte`, `throwback`, `slip`, `answer`, `tight_redirect`, `hard_stagger`, `last_stand`, `second_wind`. New casts: **Reckoning** (the energy your weapon absorbed from parries in the last few seconds is released as one blow; eats the parry's excess-energy figure and the wear it caused, so it is bigger the harder you were hit); **Reprisal** (for a few seconds every parry throws a free counter, rearm `parry`).

### Evasion tab (subterfuge and momentum: never where the blow lands)

- **Acrobat** (terrain and momentum). Move: `long_roll`, `far_roll`, `extra_iframes`, `wall_kick`, `wall_smash`, `wall_rebound`, `quake_leap`. New casts: **Wall Run** (run along a wall while sprinting; eats the wall-kick momentum and whether a wall is faced); **Vault** (spring off an enemy's shoulders: leaps over a target and lands behind, stagger by the mass you push off).
- **Shadow** (the unseen strike). New casts: **Smoke Step** (a roll that leaves a cloud: mobs lose their target in it; eats gun smoke and spent powder, so it needs a fired round or a smoke charge made at the bench); **Shadow Step** (blink to behind a nearby target on a roll's i-frames, rearm `time`; eats roll i-frame figures and facing); **Ambush** (a blow from behind or unseen is a stagger; rider, eats facing and light). Supports: `i_frames`, `roll_distance`.

### Gun tab (precision and heat; three trees)

- **Suppression** (shock and awe: volume, area, pinning). Move: none. New casts: **Pin Down** (the root; built), **Walking Fire** (a burst while advancing, spread opens as the barrel heats); **Muzzle Flash** (a point-blank discharge staggers and blinds a cone by the round's impulse over `BodyMass`); **Overheat** (vent the barrel's heat as a burning cone: eats `GunCondition.temperature`, leaves the gun hot and at cook-off risk); **Pin Down** (rounds passing close to a mob slow it, eats swept-bullet proximity).
- **Marksman** (patience, the single shot, reading baked figures). Move: `braced`, `braced_fire`, `recoil_vault`. New casts: **Dead Eye** (hold a breath: time slows for you, your next shot is exact and its spread is zero, rearm `strike`; eats the gun's spread figure); **Ricochet** (a shot that bounces off up to N blocks of a hard material and finds a target, eats `hardnessOf` and glance angle); **Called Shot** (mark a target with a coin or a look; the next round curves to it). The **coin** nodes (`coin_flip`, `second_mark`, `round_the_corner`) live here as the trick-shot limb.
- **Close Quarters** (the gun as a body part). Move: `pistol_whip`, `chamber`, `dual`, `spin`, `brand`, `muzzle_stagger`, `bayonet`, `marked_rounds`, `return_fire`, `hot_clear`, `dodge_charge`, `spent_case_draw`. New cast: **Point Blank** (fire into a grabbed or staggered body for full impulse as stagger plus damage, eats grab and `BodyMass`).

### Melee and tether tab (dominance: the fight happens where you decide)

- **Weapon Master** (heft and reach: modular baked reach and moment of inertia). Move: `far_end`, `close_work`, `blade_flourish`, `mace_quake`, `spear_vault`, `searing_strike`, `forge_slam`, `whirl`, `draw_switch`, `feint`. New casts: **Hamstring** (built; replaces Skewer, which the `dash` trait already is); **Cleave** (an overhead mace blow that sends a ground shock by weight, eats mass and moment of inertia).
- **Grip** (control: hold, bend, throw). Move: `grab`, `firm_grip`, `iron_grip`, `throw`, `fast_reel`, `winch`. New casts: **Reel In** (haul a target to you staggered; eats the grapple's reel and `BodyMass`); **Swing** (grapple traction to swing across a gap, eats the rope and your momentum); **Bowling** (throw a grabbed mob into others; the thrown body staggers what it hits by its mass). Hitch and Conduct stay [OPEN] (§14.5).

### Fist tab (fervor: relentless, close, hands and feet; three trees)

- **Tempo** (chains; the cast is Pummel). Move: `jab`, `lunge`, `sweep`, `chop`, `uppercut`, `flurry`. New casts: **Rising Tempo** (each landed blow shortens the next; ends on a miss, rearm `strike`); **Counter Stance** (a stance where a blow taken is answered; eats the parry window).
- **Legwork** (kicks). Move: `kick`, `breach_kick`, `wall_force` (out of evasion; the kick root belongs here). New casts: **Dropkick** (airborne kick, stagger by fall and `BodyMass`); **Stomp** (a downed or staggered target takes the full mass; eats the stagger state); **Axe Kick** (an overhead heel that breaks a guard, eats shield hardness).
- **Gauntlet** (what bare hands can carry; **not** the Feedbacker specifically, and no equipment required for the tree). A *gauntlet accessory* is wanted that synergises well with the fist (to be designed: a worn piece, not a skill gate). Single skills here may require gear. New casts: **Shock Palm** (a punch that carries a charge: eats `Shock` and `Protection.shockProofVolts`, so a rubber glove on the target blunts it); **Punch Back** (a projectile punched with the gauntlet is aimed, eats the existing punch-projectiles grant and coin punch); **Heat Sink** (a held hot item grips through a glove: lets you fight with a forge-fresh blade, eats `Grip`/`Handled` and glove insulation).

### 15b. Floor: every tree has 10+ nodes and 4+ casts (2026-10-24, BUILT unplayed)

Added to reach the floor; every figure is an `[OPEN]` stand-in. Each cast names the system it eats (§14.1). Riders (R) are one-line changes inside their cast; supports (S) are figures.

- **Acrobat**: *Wall Run* (wall at your side, sprint), *Vault* (your mass pushed off the target), *Slide* (mass tripping everything swept).
- **Shadow**: *Smoke Step* (mob targeting, cloud), *Vanish* (worn armour mass shortens it), *Pocket Sand* (block underfoot: sand, dirt, stone); R Thick Smoke, Long Shadow, Grit; S Quiet Roll, Slip Past.
- **Stalwart**: *Challenge* (weapon wear pays for the noise), *Advance* (mass ratio shoves the line; shield-type item); S Settle, Keen Eye.
- **Counterstrike**: *Punish* (only a staggered target; rearm stagger), *Rebuke* (the blow you take, struck back at 60%).
- **Tempo (flurry)**: *Rising Tempo* (rearm strike, ends on a pause), *Frenzy* (attack speed up, then winded), *Haymaker* (windup; a whiff staggers you); S Catch Breath.
- **Legwork**: *Stomp* (staggered target, `BodyMass`), *Axe Kick* (breaks a raised guard); R Crush, Rending Heel (armour wear); S Wall Crush, Spring Heel.
- **Grip**: *Reel In* (`BodyMass` gate, reel nodes scale it), *Bowling* (a held body is the ball), *Swing* (rope length and momentum); S Vice.
- **Weapon Master**: *Cleave* (weapon damage as the weight, ground crack).
- **Suppression**: *Overheat* (`GunCondition.temperature`; scalds your hand via `HotContact`); R Wide Pin, Held Pin, Dazzle, Concussive, Scorch; S Steady Barrage.
- **Marksman**: *Called Shot* (target velocity, leads the round); S Patient Hand.
- **Close Quarters**: *Point Blank* (held or staggered body, round impulse), *Snap Volley* (magazine, heat, cycle), *Human Shield* (the held body takes your hits), *Disarm Shot* (disarms on fire, not confirmed hit: [OPEN]).

### What this needs

Tab grouping in `SkillTree` and `SkillTreeScreen` (a `"tab"` field and stacked trees), moving nodes between files, splitting pools in `SkillEarning`, lang names per tree and tab. Casts above are ideas, not specs: each needs its own §14.1 line before it is built.

### 15a. Overlap audit against traits and attunements (2026-10-24)

The rule: a skill is the body's behaviour, a trait or attunement the item's; a skill must not repeat one. Audit of what was built:

- **Fixed (duplicates removed).** *Fan the Hammer* cast removed: the `fan` trait (hammer and cylinder, crouch-hold) is the same act; replaced as the Suppression root by **Pin Down** (rounds that pass near a target slow it, hit or not; no trait does this). *Skewer* removed: the katana's `dash` trait is the same lunge; replaced by **Hamstring** (a cut that cripples, a control effect). *Ambush* no longer reads "from behind" (the dagger's `backstab`): it is now the opening after a Shadow Step.
- **Renamed to avoid collisions.** Tree *Bulwark* is now **Stalwart** (the tower shield's trait is `bulwark`); the fist cast *Flurry* is now **Pummel** and its tree **Tempo** (the rapier's `flurry` trait is a stacking bonus).
- **Kept, with the overlap noted (judged a different trigger or cost).** *Quake Leap* and *Dropkick* land a ring or a blow like the sabatons' `trample` and `slam`, but the trigger is a chosen move, not a fall or a mace swing. *Shield Wall* turns things back like the tower shield's `bulwark` bash, but it is an area held for three seconds against anything, not a sprint slam. *Brace* holds the line like the cuisses' `braced` (knockback x0.6), but is a chosen four-second stance with a stagger return. *Fist Lunge* reads movement like the javelin's `lunge` and the Momentum attunement; **watch this one**. *Braced Fire* extends the `gun_braced` trait's crouched-and-still bonus to bracing against terrain; **watch**. *Marked Rounds* (powder burn spent by a melee blow) shares a shape with the Mark attunement (a mark that the next blow spends); the cause differs, **watch**. *Searing Strike* vs Heat+: the skill reads real item temperature, the attunement sets a burn; **watch**. *Wall Kick*'s momentum vs the Momentum attunement: different (a gesture versus a damage scale).
- **No overlap found.** Coin Flip (the Wager attunement is a gimmick coin flip per hit, not a shot), Throw and Grab (the `throw` trait throws a held weapon), Reckoning, Reprisal, Dead Eye, Walking Fire, Muzzle Flash, Shadow Step, Pummel, Last Stand, Roll With It, Pry Open (the Hold attunement stops a disarm; Pry Open disarms a blocker).
