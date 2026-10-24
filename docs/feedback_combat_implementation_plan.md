# Combat system -- implementation plan

**Status: Draft, working document (not one of the three).** Follows `docs/feedback_combat_spec.md` §9 (the author's direction). Written 2026-10-21 from a grilling session; every ruling below is the author's unless marked **[OPEN]**. Code facts come from `docs/feedback_combat_integration_map.md` (read it; its unverified line numbers are flagged there). Built since: phases 1-10 of §4 are all committed (swept hitbox, `CombatState`, parry, roll, Tether and grapple, stagger, kick and grab, skill trees, Curios gear, dual wield, gun-fu, procedural poses); much is unplayed, see `docs/feedback_overnight_checklist.md`.

## 0. Rulings so far (grill rounds 1-6)

| Topic | Ruling |
| --- | --- |
| Parry vs block | **Different verbs.** Parry is an active reaction, on its own **dedicated key**. Block stays the passive raise (vanilla input, shields, weapons) for when you reckon a swing is coming. |
| Failed parry | **Recovery lockout.** The active window is nested inside a longer committed animation; a whiff or late press leaves you open for the remainder. |
| Roll / dodge | **Dedicated key**, direction from movement input, **i-frames from a window**, then recovery, with a cooldown. Gear and skills change distance and window. |
| Impact frames | **Flash, camera punch, FOV pop, sound, sparks.** No hit-stop: tried and removed (see 2.6). On a confirmed parry the local client gets the flash and camera kick; the server applies a short real stun to the attacker. Bystanders see flash, sparks and sound. |
| Netcode | **Server decides, client predicts.** Window padded for ping (capped). Client plays the raise pose at once; the server's verdict drives the feel payload. |
| Skill tree | **Spend-points, one pool per tree**, each pool earned by doing that thing (parry points from parries; gun-fu points from *any* gun use, never one prerequisite). Points kept on death. **Basic moves free (parry, roll); advanced moves unlock with points** (riposte, wall-kick, gun-fu moves). |
| Mobs | **Players only for now.** Mobs are parryable and staggerable (mass-based); humanoid mobs adopting the kit later go through the same API. |
| Animation | **Procedural poses from baked weapon figures.** No authored clips for weapon swings. Roll/kick/grab with no weapon geometry may need something else later. **[OPEN]** |
| Held state | One abstract **Tether** (anchor + rope + vulnerability). The harpoon is *one source*; grapples, nets and snares are others. Grab and grapple are the same system. |
| Gear | All four routes: new Curios slots (bracer, sash/belt, back); loot-only relics with one qualitative perk; craftable gear baked from materials; items that *teach a technique*. |
| Risk curve | **Window scales hard with the weapon.** A dagger parries in a sliver and recovers fast; a greatsword has a wide window and a huge lockout. Weapon choice is a playstyle. Derived from baked mass/reach, not a per-weapon table. |
| Parry cone | **Frontal cone only**, width from the weapon (reach, head width). Flanking and fire from behind still hurt, so facing and position matter. |
| Kick | **Both:** guard break/stagger against a blocking target, knockback and shove otherwise (walls, ledges, lava). Has a cooldown. |
| Grapple | **Both directions, by mass.** The lighter body moves toward the heavier along the tether: swing to a wall, a goblin flies to you, a golem pulls you in. Falls out of Tether plus the mass stand-in. |
| Gun-fu first move | **Pistol whip into a free shot.** A **melee-stance key** (default `Z`, held): while held, the gun's fire button (left or right by hand) *swings* it as a melee weapon instead of firing. On a staggered hit the gun chambers a shot at the target. The author binds this to mouse 4; not everyone has side buttons, so it needs a sane default. Guns thus count as melee through a *stance*, not a separate weapon. |
| Launch trees | **Parry, Evasion, Gun-fu, Melee by weapon family + Tether** (the last holds spear/blade/hammer/shield branches keyed off baked head properties, plus grapple, grab, kick). |
| Exemplar relic | **The Feedbacker**: a gauntlet that punches *any* projectile back at **increased speed** (not the default parry). A bracer/hands-slot item, named for the Ultrakill reference. Per the loot ruling below it is craftable by someone clever enough. |
| Loot ruling | **Loot is tier-skipping; nothing found is uncraftable, only uncraftable *now*.** Written into `feedback_philosophy.md` §14 ("Loot is tier-skipping..."). So every relic needs a real craftable route, which constrains the Curios work below (§2.8): no item exists only as loot. |
| Feedbacker limit | A returned projectile's **impulse is checked against the gauntlet's joint rating** (from its baked parts and materials). Arrows and bullets are easy; a ghast fireball is a hard punch; a cannonball or Sable body wrecks the gauntlet (wear by misuse). No entity list. |
| Stance key | **Hold** (default `Z`): the gun fires nothing and swings as a bludgeon (damage from baked mass and barrel); a staggering hit chambers a free shot. Release and it is a gun again. Dual wield: each hand independent. An explicit exception to `GunClient`'s cancel of attack/use clicks. **Built (step 1):** `GunClient.STANCE`, `GunSwingPayload`, `weapon/GunBludgeon` (damage from baked `gunMass` only; barrel has no baked figure, so mass stands for it; constants `FTuning.GUN_BLUDGEON_*`). The "staggering hit chambers a free shot" half is **not built**: stagger does not exist; the hook is marked in `GunBludgeon.swing`. |
| Skill farming | **Diminishing returns per target** (and per parried attack source): a small recent-targets record per player. |
| Dual wield | **Per-hand traits:** `ModularTraits.onHurt` and `MaterialPerks` resolve for whichever hand's weapon dealt the blow; off-hand weapons count fully. |
| Prior art | **Better Combat**: All Rights Reserved, design only, optional presentation compat at most. **War Dance**: CC BY-NC, design only. **Just Parry**: no public source, 1.20.1 only, abandoned; notes in `docs/feedback_just_parry_notes.md`; design only. |

### 0.1 A ruling that bends philosophy -- read this

The skill tree gates *moves* behind points (basic free, advanced unlocked). `feedback_philosophy.md` §7 says only physical impossibility is a hard gate and "if a proposal needs 'requires tier N,' it's wrong." The author's reasoning: the philosophy-pure alternative (everything attemptable, just fails) teaches nothing, unlike a gun that blows up, which teaches by showing the cause; failing a move you cannot do yet only says you cannot do it, and grinding failures is not fun.

**Author's wording for the philosophy (round 4):** *adherence to physics-based systems only applies to the world. The player still gets to work like a video-game character, if the alternative trades being logically consistent for feeling good.* This is broader than a skill-gate exception: it is a **world / player-body split**. The world (materials, machines, factories, hazards) resolves on truth; the player's own body (moves, techniques, feel) may follow game logic where consistency would cost the fun.

**[OPEN]** to settle when the philosophy is edited (a change to §7, argued there, not here): how far the split reaches. The danger the author's wording leaves open: "the player is a video-game character" could be cited to justify anything (free health regen, tool-less mining). The guard to propose alongside it: **the split covers what the player's *body* does (move set, timing, feel, learned technique); it never lets the player bypass what the *world* does** (a skill does not make a machine, a material or a recipe work differently, and no skill substitutes for a physical input). Skill gates stay scoped to combat moves. Philosophy §1 and §2 (identity) are not touched.

## 1. Where the code stands (what the plan builds on)

From the map, the facts that shape every choice:

- **No custom melee path.** Melee is vanilla click and cooldown, attributes baked from the design; every weapon trait hooks `LivingIncomingDamageEvent` in `ModularTraits.onHurt`, **main hand only**.
- (Was: a buckler `Parry` trait and a kite `Deflect` through `LivingShieldBlockEvent` only. Folded: see 2.2.)
- **No entity attachments, no capabilities, no mob AI goals.** Per-player state is static server maps (`WielderState`, `HELD`, `NEXT_OK`), never synced, never persisted.
- Curios is only the built-in `hands` slot; no custom slot files, no renderer.
- The client has no time or tick control; gun recoil writes player rotation directly. (NeoForge's `ViewportEvent.ComputeCameraAngles` and `ComputeFovModifierEvent` exist and are the planned hooks; **verify on 1.21.1 before relying on them**.)
- `ThrownWeapon.HELD` is a bare id set with no owner or source; one detach clears the id even if a second harpoon is in.
- There is no head-shape enum; `Edge` is the grind, head kind is only the part item's path.

## 2. Architecture

New package `combat/` (own subpackages as content arrives; no empty scaffolding). Registers in `registry/FCombat`. Rule used throughout (the author's, "one system for more than the thing it was made for"): **no weapon, gear piece or move gets its own code path; each is data feeding a shared mechanism.**

### 2.1 State: attachments, not static maps

Add NeoForge **data attachments** (`AttachmentType`), the first in the mod:

- `CombatState` on `LivingEntity`: phase (`IDLE / PARRY_ACTIVE / PARRY_RECOVERY / ROLL / ROLL_RECOVERY / STAGGER`), ticks left, i-frame ticks, stagger. **Synced** to tracking clients (the feel and the pose read it). Transient: not persisted, cleared on respawn.
- `SkillPools` on `Player`: `Map<Tree, int>` plus unlocked node ids. **Persisted, `copyOnDeath`, synced to the owner only.**
- `Tethers` on `LivingEntity`: see 2.5. Server-side, synced for the rope render.

Why: static maps are never synced and lose state on reload; the whole feel layer needs the client to *know* the phase. `HELD`, `WielderState` and `NEXT_OK` migrate onto these as the work touches them, not in one sweep (no migration code; breaking changes are fine).

### 2.2 Parry

- Key `parry` (new `KeyMapping`, client). Press sends `ParryPayload` (client -> server) carrying the client's tick. Client immediately plays the raise pose (prediction).
- Server: if `IDLE` and the held item can parry, set `PARRY_ACTIVE` for `W` ticks then `PARRY_RECOVERY` for `R` ticks (the lockout). `W` and `R` come from the **baked weapon**, not a table of weapons: window narrows and recovery lengthens with mass and reach (physical: a long heavy blade is slow to bring round); **[OPEN]** the exact curve and the base numbers. Ping pad = `min(pingTicks, cap)` added to `W` on the server side only.
- Resolution happens in `LivingIncomingDamageEvent` at **HIGHEST** priority, before `ModularTraits`, `MaterialPerks`, `Flurry`, `ThrownWeapon`. If the defender is `PARRY_ACTIVE`, the hit falls in the defensive arc (a cone from facing, **[OPEN]** angle by head shape), and the damage type is parryable: cancel the event and run `ParryResult`:
  - attacker: **stagger** scaled by impact over its mass (2.4);
  - defender's weapon: **joint load and wear** by the force absorbed (philosophy 6, misuse-only wear: a parry that works costs the weapon little, a parry of far more force than the weapon was built for costs a lot);
  - `ParryFeelPayload` to players in range (2.6).
- **Block is unchanged.** The buckler `Parry` trait and kite `Deflect` are folded onto this one window logic and are now data: `ParryStats.of` reads "this shield parries" (`parry`: `parry_ticks` replaces the curve window, `cone` the width, `knock` throws the attacker back) or "this shield deflects projectiles back" (`deflect`: projectiles only, turned at `return_speed`). A shield with neither trait (tower, plain) does not parry. The traits keep only their passive-raise costs (buckler narrow arc and `capacity_mult`, kite `melee_mult`); the raise-window full block, the shield-block knockback and the kite's passive projectile return are gone, with `projectile_wear`.
- Projectiles: a parried arrow/bullet/thrown weapon is *redirected* along the look vector with a speed gain and an inaccuracy term (design idea from Just Parry's notes, ours to tune). This is the gun-fu hook.
- Damage-type reactions (from Just Parry's notes): parry is **not** a blanket negate. Parryable = melee, projectile, thrown. Not parryable = fire, magic, explosion (**[OPEN]**: fall damage -> a roll-out? a hazard-type table, `core/hazard/Hazards`' three damage types are the starting vocabulary).

### 2.3 Roll

Key `roll`; direction = movement input at press (forward if none). Server sets `ROLL` (i-frames for `I` ticks, `LivingIncomingDamageEvent` cancels while it holds), applies a displacement impulse, then `ROLL_RECOVERY`, then a cooldown. Distance, `I` and recovery come from gear (Curios figures, 2.7) and skills; base is free. Client predicts the movement. i-frames also cancel **hazard contact** unless ruled otherwise (**[OPEN]**: a roll through a fire block).

### 2.4 Stagger and the swept hitbox

- **Hitbox first. Decided by the spike (2026-10-21, compiles, unplayed): it RIDES vanilla; it does not replace it.** `combat/hit/`: `SweptVolume` (pure geometry: a thick line from the eye along the look, length = `entityInteractionRange()` which the bake already sets from `Fig.REACH`, half-width = `0.2 + 0.6 x Fig.BALANCE`, stopped by blocks; the width is a stand-in until a head variant carries a real `width`) and `MeleeSweep` (server). Two entries, one switch (`ModularTuning.MELEE_SWEEP`):
  1. **Picked target.** `AttackEntityEvent` (LOW, server): the volume's *other* living targets are hurt before vanilla's own hit, while the cooldown is still charged; vanilla then does the primary (crit, sweep, knockback, enchantments, reset). Extra targets get vanilla's damage formula minus crit (attack attribute, `EnchantmentHelper.modifyDamage`, `0.2 + s^2 x 0.8`), through `hurt` with the player's attack source, so `LivingIncomingDamageEvent` and every trait and perk fire per target.
  2. **Empty swing.** The client's pick is client-side and decides the target before the server hears (`MultiPlayerGameMode.attack` sends the picked entity), so a volume that reaches what the pick missed needs the client to say so: `PlayerInteractEvent.LeftClickEmpty` (client) runs the *same* `SweptVolume` over client entities and sends `MeleeSweepPayload` only if it expects a hit; the server re-runs it, hurts, and resets the attack ticker as the client did on a miss. That shared pure function is the prediction.
  - **Why not replace.** Replacing means cancelling `AttackEntityEvent` and reimplementing crit, sweep, knockback, enchant damage, stat and cooldown reset; vanilla's pick also drives the crosshair, the attack packet and every other mod's attack hook. Riding keeps all of that and "damage and cooldown stay vanilla's" is literal. Cost: the crosshair outline does not show volume-only targets, and a weapon cannot *miss* a picked target.
  - **Not covered:** `LeftClickBlock` (swinging at a wall with a zombie beside it; not predicted, mining must not trigger it), arcs (the volume is a line, not a swept arc), Sable bodies, off-hand. Stacking: `ModularSword` is a `SwordItem`, so vanilla's own sweep and the claymore's `Overhead` sweep still run and may overlap the volume's extra hits (i-frames absorb vanilla's smaller one; `Overhead` re-sweeps around each extra target). Decide in play whether `Overhead` should yield.
- **Stagger** = impact against target mass: `impulse = f(swing figures)`; a target's mass stand-in is bounding volume times a density (`Mu`, `Density`), with a per-entity override for the heavy cases. Effect: a brief attack cancel and slowdown on the target (attribute modifier with an expiry), longer when the impulse is large against the mass. Shields convert excess into joint load and heat (the shield-as-sink idea in `TODO.md`). Not a stat bar, not shown as one. Kills during stagger credit the staggerer.

### 2.5 Tether (the abstract held state)

One record on the entity: `Tether(sourceId, anchor, ropeLength, reelRate, vulnerability, release)`. `anchor` is an entity, a block or a point; `vulnerability` is the damage-taken multiplier the harpoon's held wound uses today.

- The harpoon's `onIncomingDamage` multiplier moves to a **generic damage listener over `Tethers`**; effective multiplier is the **max over tethers**, not the product, which also closes the two-harpoons gap.
- The harpoon becomes the first source; a **grapple** (player's own tether, anchor = block or entity, reel pulls the player) is the second, and **grab** is a short-rope, no-reel tether between two entities. A net or snare is a tether with a ground anchor and `ropeLength` near zero.
- Server runs the rope (distance constraint with spring-limited reel); client renders it. **[OPEN]** Sable bodies as anchors (no, for v1).
- **Built (phase 5a):** `combat/tether/` holds `Tether` (sourceId = the source's uuid, `Anchor` = `OnEntity`/`OnBlock`/`AtPoint`, `Release(stopDistance, maxTicks)`) and `Tethers` (the attachment `FCombat.TETHERS`, synced, not persisted; one tether per source; `vulnerability()` is the max). `ThrownWeapon` attaches on stick, reads reel rate and release from the tether, removes it on detach or removal. The old `HELD` id set is gone. The rope render still draws owner to weapon and does not read the attachment yet.
- **Next sources, not built.** Each constructs a `Tether` and runs its own rope; `Tethers` stays a list and a damage listener.
  - *Grapple:* anchor `OnBlock` (hit block) or `OnEntity`; held body = the player (or the lighter body by mass), `reelRate` from the tool's baked figures, `vulnerability` 1, `release` on arrival or on the player letting go; source id = the grapple projectile or a per-use uuid.
  - *Grab:* anchor `OnEntity` (the grabber), `ropeLength` small, `reelRate` 0, `vulnerability` above 1 for a held limb (figure open), release on the grabber's stagger or a struggle count; source = the grabber's uuid.
  - *Net:* anchor `AtPoint` where it landed, `ropeLength` near zero, `reelRate` 0, `vulnerability` 1, release by time or by the target's strength against the net's material.
  - *Snare:* as a net but anchored `OnBlock` to the trap block; the block entity is the source and removes the tether when it is broken or sprung.

### 2.6 Feel (client)

A `ParryFeelPayload` (server -> nearby clients; strongest for the two parties) triggers, locally, all keyed off one `Feel` record (so a heavy parry and a roll-cancel are the same code with different figures):

- **No hit-stop.** A render-target view freeze was built and removed. Hit-stop was tried and removed (author ruling, 2026-10-21): Ultrakill stops the world but not the player, which cannot be done in multiplayer, and a render-copy freeze of the local view only reads as an FPS hitch. The impact-frame flash, camera punch, FOV pop, sound and sparks carry the feel; a stronger flash or shake may stand in. No world or view freeze. Why it was tempting: Ultrakill's hitstop is the feel bar; the trap is that it freezes the world around a player who keeps moving, which a server cannot do for one client.
- **Impact frame:** a full-screen flash layer (`GuiLayer`, like `GunHud`'s) with a 1-2 frame inverted/white pulse, and an edge accent.
- **Camera:** short decaying roll and FOV pop via `ViewportEvent.ComputeCameraAngles` / `ComputeFovModifierEvent` (not a rotation write, so the crosshair does not move; the gun recoil's rotation write is the thing to avoid copying).
- **Sound and particles:** a `FSounds` event per figure band (light/heavy), sparks scaled by impact. Placeholder sounds aliased to vanilla in `sounds.json` per the UI-sounds convention.
- Local player gets all of it; bystanders get flash/sparks/sound only.

### 2.7 Skill pools and trees

- `skill_tree/*.json` (datapack, `SimpleJsonResourceReloadListener`, synced like the other tables): `{tree, nodes:[{id, cost, requires:[], effect}]}`. `effect` is either a **figure modifier** (window, recovery, roll distance, stagger, redirect inaccuracy; units typed per `core/unit`) or a **move unlock** (`riposte`, `wall_kick`, `gun_fu_*`). Nothing per-weapon; weapon-type skills key off *tags and baked properties* ("has a head heavier than N", "is a gun"), never an item id (identity rule).
- Pools accrue from **events by tree tag**: parry success -> parry pool; any `GunShots` hit or kill -> gun pool; roll-through-attack -> evasion pool. **[OPEN]** award sizes, diminishing returns per target (hinted in the grill; avoids farming one dummy).
- Screen: a `GuiSkin.PANEL` tree screen. Respec **[OPEN]**.

### 2.8 Curios gear

Curios becomes the combat gear layer.

- **Slots** via `data/curios/slots` + entity config: `bracer`, `belt` (sash), `back` (and leave `hands` as is). Real slots so combat gear does not fight armour.
- **Craftable gear baked from materials**, `GloveItem`'s pattern generalised: parts + materials -> figures stored on the stack (`ModularBake`/`ModularFigures`): bracer sets parry window width and joint rating, sash sets roll distance and recovery, back piece sets stagger mass/carry. Reads `material/*.json`; no per-item rows.
- **Relics:** found early, *craftable later* (philosophy §14, loot is tier-skipping): curios with one qualitative perk through the existing `perks` map (`Material.Perk`/`MaterialPerks`), plus a `grants` marker for techniques. Each relic has a craft route (a recipe or a bake from a part list) that is simply out of reach early. The first is **the Feedbacker** (punch any projectile back faster).
- **Technique-teaching gear:** a `grants` list on the item (a grapple curio grants `tether.grapple`; a sash grants `roll`'s advanced variant). Move unlock checks `SkillPools unlocked OR gear grants`.
- Rendering: `ICurioRenderer` per slot, placeholder models, after the mechanics.

## 3. Rules compliance (check against `CLAUDE.md` hard rules)

- **No identity checks:** moves and skills key off baked properties and tags. Satisfied.
- **Wear only by misuse:** parry wear scales with force *beyond the weapon's build*; a correct parry is cheap, a bad one (far over the rating) costs a lot. Recovery lockout is a *time* penalty, not wear. Satisfied.
- **No hard gate except physics:** the one deliberate exception is 0.1; scoped to the player's own combat moves.
- **Discrete options:** windows, recoveries and costs are bands with stated figures, not free dials.
- **Truth tier:** every combat figure prints through `Readout.figure` and is glossed on Shift; the debug helmet reads exact window/recovery/stagger.
- **Instruments:** a read-only combat readout item is conceivable; not planned.
- **Unit types:** window/recovery in ticks (no type, stated: `Work`, condition and ticks have none), stagger in `Mu`/impulse, rope length in blocks. No new unit minted; **[OPEN]** whether impulse needs one (check `core/unit/Units` checklist before adding).

## 4. Phases (build order, each leaves the game playable)

1. **Spike: swept hitbox.** Prove a server-side swept volume beside vanilla pick and client prediction of the pick. Compile + in-game check only. Decides whether the rest uses it or rides vanilla attacks.
2. **State + feel skeleton.** `CombatState` attachment, key mappings, payloads, and the *feel layer driven by a debug command* (so impact frames can be tuned without any combat existing). Cheapest place to get the Ultrakill feel right before anything depends on it.
3. **Parry.** Window/recovery from baked figures, HIGHEST-priority resolver, redirect, wear, feel wired. Buckler/kite fold in.
4. **Roll.** Same state machine.
5. **Tether refactor.** Harpoon moves onto it (fixes the two-harpoon gap), then grapple.
6. **Stagger + mass.** Then kick and grab on top of tether.
7. **Skill pools and trees.** After there are moves to unlock.
8. **Curios slots and gear.** Craftable first, then relics and teaching gear.
9. **Dual wield** (two main-hand-only hooks must become per-hand) and **gun-fu**.
10. **Presentation:** procedural poses; optional Better Combat compat only if wanted.

Each step is its own spec before building ("we can work on how to spec out individual coding sections later"). Ask the prior-art question before each; known prior art: War Dance (design), Just Parry (design), Better Combat (feel), Create's grapple-ish and any grapple mod the author names.

## 5. Open grill list (answers still owed; recommendation in italics)

1. **Parry numbers.** Built (`combat/parry/ParryTuning`, tune in play). Heft = `MASS * (1 + 0.5 * REACH)`, mapped 0.5 (dagger) to 6 (greatsword) onto 0..1; window is 3 to 7 ticks (tightened from 4 to 12, 2026-10-24) and recovery 10 to 35 ticks, both linear in it. Ping pad is `latency / 50` ticks, capped at 2, on the window only. A parry that lands frees the fighter at once. Wear: weapon rating is `MASS * 60` J; a blow within it costs 1 point, over it costs `1 + ceil((over - 1) * 4)`. Stagger: `impact / bbox-mass * 16` ticks, 4 to 40, 60% slow (mass stand-in = box volume * 120). Projectiles turn along the look at 1.5x speed, inaccuracy 2, owner becomes the defender. Roll (`combat/roll/RollTuning`): 8 i-frames (the whole roll phase), 12 recovery, 30 cooldown (a parry also waits on it), speed 1.36 b/t for about 3 blocks on ground. **Unplayed.**
2. **Parry cone**: frontal, width from the weapon (decided). Built as a half-angle: 35 degrees plus 55 times BALANCE (a spear about 35, a maul about 90); a shield is a fixed 75. Whether gear widens it is still owed.
3. **Parry arrows and bullets?** *Yes, redirect; it is the gun-fu hook.*
4. **Parry hazards** (fire, magic, explosion, fall). *No; fall becomes a roll-out.*
5. **Kicking:** guard-break plus knockback with a cooldown (decided). Still owed: damage, stagger figures, cooldown length, hunger cost, whether it needs a free hand/footwear.
6. **Grab without an item:** is there one? *No; grab needs a tether source, gloves count as that source only for a short-rope grab.*
7. **Roll vs hazards** and vs a tether (does rolling break a tether?). *i-frames ignore hazard ticks; roll breaks a tether only on a heavy tether.*
8. **Dual wield:** per-hand trait hooks built: a blow's hand rides on its damage source (`modular/StrikeSource`; none means main hand), and `ModularTraits.onHurt/onDeath`, `MaterialPerks`, Mail, Butcher and the Overhead/LineCut/Fork secondary hits resolve and wear the stack that struck. The off hand strikes via right click (`client/OffhandStrikeClient.takesUseClick`: off hand a modular blade/mace/spear, main hand empty or such a weapon with no use animation, not mid-use, aimed block has no `useWithoutItem` of its own; held repeats at vanilla's 4-tick use delay) and an unbound rebindable key (`client/OffhandStrikeClient`, `net/OffhandStrikePayload`, `combat/hit/OffhandStrike`: its own cooldown, one target, no sweep). Still owed: whether both hands can parry, and the parry window when two weapons differ (*the faster hand's*); off-hand sweep volume and crit; the off-hand swing is not a vanilla attack so enchant/sprint-knockback/`AttackEntityEvent` listeners do not see it; main-hand-only hooks left alone on purpose: left-click-block, block break (tools), `SpearCharge`; `Protection` grip reads the main hand only.
9. **PvP:** same rules? *Yes; server-authoritative already.*
10. **Skill tree list:** which trees (parry, evasion, gun, melee-by-weapon-type, tether?) and point sizes. *Few trees, one pool each; award by event.*
11. **Respec:** free, materials, never? *Materials, craftable.*
12. **Default keys and controller.** *Parry `G` (V is the gun fire mode, R reload), roll `ALT`; gamepad not planned.*
13. **Gear numbers:** how much can a bracer move the window (philosophy 14: difficulty flat, novelty up)? *Gear narrows the ratio of window to enemy speed; never makes parry free.*
14. **Philosophy §7 wording** for 0.1: author's wording recorded; the guard on how far it reaches is still to be argued.
15. **Echo shard / perks:** any perk on combat gear shares `perks`; does `perks` need a gear scope? *Add a `scope` field.*
16. **Procedural pose limits** for roll/kick/grab without weapons.
17. ~~Replace or ride~~: rides (spike 1, §2.4).
18. **Mob mass stand-in** acceptable as a flagged exception.
