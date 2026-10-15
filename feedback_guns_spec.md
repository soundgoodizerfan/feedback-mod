# Feedback — Guns

**Working document, not one of the three.** Scopes firearms as first-class content, the way
`feedback_vehicles_spec.md` scopes cars: something the player *makes with the tech*, not another
machine on the way to more machines. **Draft. Steps 1 to 3 of §11 are built; the rest is not.
Prior art is licence-checked, cloned and read** (§9). Where this document and `feedback_philosophy.md` disagree, the philosophy wins.

Written 2026-10-12 from `feedback_philosophy.md`, `feedback_mechanics.md` and
`feedback_vehicles_spec.md`. No source was read for it, so every reference to built code below is
as those documents describe it and should be checked before it is leaned on.

---

## 1. Rulings already made

Settled by the author, 2026-10-12. Not reopened here.

1. **Weapons are in scope.** The reason is the point, not a footnote: a tech mod whose machines
   only ever make more machines, or more ore to feed more machines, has no outputs. Cars are
   one answer, guns are another. This is philosophy §14's **empowerment axis** (*technology being
   industrialized onto the player*), which that section parks as **[OPEN]** until the industrial
   framework settles. Guns are its first concrete content, so §10 here proposes edits to §14
   rather than the other way round.
2. **Feel is the target, TacZ's architecture is not -- and the reason is the opposite of the
   one this document first gave.** TacZ's guns feel good, and their stats are *fixed*: a gun is
   a row in a global registry keyed by id. The author tried a TacZ gun **craftable from different
   materials for different stats** as an addon, and it was miserable, because the architecture
   cannot express it. *Ours must.* Every figure of a gun is a function of its design and its
   materials, so there is no finite table to look it up in. §2 is the answer. (Corrected
   2026-10-12; the first draft said TacZ's stats change on the fly, which is wrong, and §2 had
   been built on that error.)
3. **Guns mount on vehicles**, and that is a goal from the start rather than a later bolt-on.
4. **The player designs the chamber**, the way they design an engine on the Engine Stand
   (`feedback_vehicles_spec.md` §6). Real-world-style blueprints spawn in villages; the player
   may make their own.
5. **Guns wear with proper use** (author, 2026-10-12). A hand-held gun is a tool, the vehicles
   precedent (`feedback_vehicles_spec.md` §1.4) and the hand hammer's. See §6.
6. **Bad guns can hurt the shooter** (author, 2026-10-12). The severity ladder's *hazard* tier
   exists for hand-held guns. A badly made gun can injure its user. See §3.2.
7. **Bullets break only things like glass** (author, 2026-10-12). See §3.5.
8. **Blueprints are inspired by real guns, recognisably, without being them** (author,
   2026-10-12) -- enough that a player can tell what it is drawn from, not enough to hold the
   project legally liable. See §4.3.
9. **`Pu` is chamber pressure too** (author, 2026-10-12). A unit is a unit: air in a vessel and
   gas in a chamber are the same fundamental quantity, and a difference of scale is not a
   difference of dimension. No new unit, no rescaling. See §10.

---

## 2. Architecture: a gun is a baked design, not a stack of modifiers

This is the answer to ruling 2, and everything else hangs off it.

> **All of a gun's figures are computed once, from its design and its materials, and stored on
> the item. At runtime a gun has a small, fixed set of mutable state and nothing else.**

It is the shape the vehicles spec already settled on for engines: *Engine Stand → Blueprint →
Engine item that carries its design*. The gun is the same chain.

| | Lives where | Changes when |
| :--- | :--- | :--- |
| **Design** (chamber, barrel, bore, materials, parts) | the Blueprint and the gun item | never — a different design is a different item |
| **Baked figures** (chamber rating, muzzle energy, impulse, cycle time, spread floor) | a record derived from the design, on the item | never — derived once, at print |
| **Runtime state** | the item | per shot / per tick-on-read, see below |

**Runtime state is exactly four things:** the round in the chamber and the magazine count,
**barrel heat**, **condition**, and the fire-mode position. Barrel heat is a stamp plus a tick,
computed on read — `ItemHeat`'s pattern (`feedback_philosophy.md` §9: *workpieces carry state
too*) — so a gun in a chest cools in a chest and nothing ticks a held item. Condition is the
dimensionless percentage the machines already use.

**Attachments are not modifiers.** A suppressor, a longer barrel, a better stock are *physical
parts of the design* (philosophy §4: upgrades are things you could point at). Fitting one makes a
**new baked record**, derived through the same function as the original — never `+10% stability`
added to a running total. This is what makes the runtime cheap and the numbers legible: there is
one derivation, and it is a pure function of design and material, so it can be unit-tested and
shown on a spec sheet.

### 2.1 What TacZ does, and why it cannot do this

Read 2026-10-12 (`../TACZ-reference`, `1.20.1`; code only, nothing taken):

- **A gun item stores an id and nothing about its numbers.** `GunItemDataAccessor` keeps
  `GunId`, fire mode, ammo count, attachments and a heat counter in NBT; every stat is looked up
  by `GunId` into `CommonGunIndex` → `GunData`, authored once per gun in a pack.
- **Attachments are modifiers on top of that fixed row**: addend / percent / multiplier per
  property (`AttachmentPropertyManager.eval`), folded into an `AttachmentCacheProperty` that is
  rebuilt whenever attachments change. So the stack exists, but it is *variation around a
  constant*; the base is never derived.
- **Everything else is keyed on the same id**: the client index, display instance, models,
  animations, sounds, even the recoil curve. 

That is why material-crafted stats were so hard: a "steel-barrelled" and a "bronze-barrelled"
variant are different rows, and a design space with a material per part is an unbounded set of
rows, each needing its own model, sounds and animation entries. **This is read from the code and
inferred, not from the addon**; the author's experience is the evidence it hurts.

So the rule for ours, stated as a constraint on every system, not only the stat sheet:

> **Nothing keys on a gun's id.** The id names a *design family*; the item carries its own
> baked record; and everything downstream -- the model, the sound, the animation, the tooltip,
> JEI -- is composed from the record's parts and figures, never looked up whole.

That is also philosophy §11 (no identity checks) pointed at our own gun. Concretely it asks:

- **A gun is parts.** A receiver, a barrel, a chamber, a stock, a sight, each a part with a
  material. The model is assembled from part models at named attachment nodes (TacZ already
  does this for *attachments*, with Bedrock models mounted at nodes -- the same idea, extended
  to the whole gun), and a material shows as a texture or tint, as plates already do.
- **Sounds are chosen from figures** (§7), not from an id.
- **Datapack blueprints are designs, not guns.** A pack adds a design; it never adds a stat
  row, because there is no stat row to add.

**Rejected:** a per-gun stat table keyed on id (philosophy §11, and the reason above); a
runtime modifier stack *as the primary mechanism* -- attachments are design parts that re-run
the derivation (§2), not percentages added to a constant; an `AmmoType` enum (vehicles spec §7
found the same defect in `EngineType`).

---

## 3. The physical model, and where it stops

Philosophy §3's test applies at every line: a variable is here only if it makes a decision.
This is **interior ballistics reduced to the three things a player can act on**, and exterior
ballistics reduced to *speed, drop and spread*.

### 3.1 A round is a case, a charge and a projectile

A cartridge is an item carrying three figures, each read from a material, none from an id:

- **Case** — a volume (`Mb`) and a material (strength, from `yield_strength`).
- **Charge** — a propellant: how much, and how fast and how hot it burns. The same two numbers
  `Fuel` already carries for fuels (`temperature`, `duration`, `spread`), read the same way —
  gunpowder gets a material file, not a branch.
- **Projectile** — mass (`Mu`), and its material's `hardness` and `density`.

A cartridge is never "a 9 mm round." It is a record of those three, and a chamber accepts it by
**fit**: case dimensions inside the chamber's. That keeps ruling 4 honest — a pack's cartridge
fits a chamber it was never listed in, because nothing was ever listed.

### 3.2 Chamber pressure, and the rating

The charge burning in the case volume gives a **chamber pressure** in `Pu`. The chamber's
**rating** comes from its material and wall — the engine's rule, literally (vehicles spec §6:
*materials as a stress ceiling is §17's rule already*): `yield_strength` and a discrete wall
(thin / standard / thick).

- **Under the rating** — fine, and the gun is as good as the figures say.
- **Over it** — *force that cannot go into the work goes into the machine* (CLAUDE.md hard
  rule). The overpressure is spent on the gun as condition loss, plus kick and blast the player
  feels. It degrades; it does not delete the gun. A gun that refused to fire past a limit would
  be a hard gate with no physical impossibility behind it.

**Settled (author, 2026-10-12): the top of the severity ladder exists** (philosophy §6,
*hazard*). A chamber past its rating by enough can let go and hurt the shooter. The rule that
machines *degrade and never break* is about machines; a gun in hand is a tool, and a badly made
one can injure its user. Two things keep this from being arbitrary:

- **It is a band, not a coin.** Like every other band in the mod it is a width: a thin wall of
  a soft metal and a heavy charge sits far inside it, a rating missed by a little only wears.
  Which side of the line a gun falls on is read off its own spec sheet, so a player who looked
  was never surprised.
- **It is about the design, not about uptime** (CLAUDE.md: wear only from misuse). A sound gun
  fired a hundred thousand times never goes off in the hand; a bad one can on the first shot.
  That is the same "a worn machine is evidence of a specific mistake" the machines already make,
  and it should read that way in the tooltip.

**[OPEN]** what it does to the shooter and to the gun -- the first cut is damage and a spent
gun (condition to its floor, never removed from the world), not a destroyed one.

### 3.3 Muzzle energy, speed, and the barrel

Work done on the projectile is chamber pressure acting over the bore area for the length of the
barrel. Speed follows from energy and mass. So a **longer barrel is more speed and more weight**,
a **bigger bore is more energy and more recoil**, and neither dominates — the non-domination
check §17 asks for (*neither throw may dominate*): every gain in energy is paid in impulse,
mass, heat or propellant. **Barrel length is banded** (short / medium / long), per §3's
named-options rule and the vehicles spec's tire-width precedent; it is a choice made at the
Stand, not something dialled in the field.

**Bore, case length and wall are the engine's scalars, and pass the engine's argument**
(vehicles spec §3): the best value moves with what the gun is *for* — close quarters, range,
stopping a creeper, mounting on a car. **[OPEN]** step size, until the Stand exists to try it.

### 3.4 Heat is where the core pitch lives

The barrel is a thermal body: each shot is `Work` in, a leak to ambient out, divided by thermal
mass — `Heat`'s difference-driven model unchanged (CLAUDE.md: *a vessel approaches its fire's
temperature and can never pass it*). Consequences, none of them a new rule:

- **Spread opens as the barrel heats** — a soft consequence, never a lockout.
- **Wear accelerates when hot** (see §6).
- **Cook-off.** A round left chambered in a barrel hot enough ignites on its own. This is the
  mod's whole differentiator in a gun: *the machine does not know the process is complete, and
  continued operation acts on the finished output* (CLAUDE.md, *The core pitch*). The output —
  an unfired round — is perishable to continued heat. Nobody wrote a "cook-off feature"; it is
  the cartridge's charge (it has an ignition temperature as `Fuel` already does) meeting the
  barrel's temperature.

Heat must have **two answers** (philosophy §5): a heavy barrel (mass, capital, weight — buys
sustained fire) against a light one (handy, hot after one magazine). A cooling fitting is a
third, later.

### 3.5 What a hit does

Damage is **kinetic energy** at the target. Penetration through a block or armour reads the
projectile's `hardness` against the target's, the same shape as the existing blow/hardness gate
(§17: a hard gate, below which nothing happens), and surplus carries through rather than
stopping politely. **[OPEN]** how entities express a material — vanilla armour points are not a
hardness. The first cut can be energy into the vanilla damage pipeline and nothing finer.

**Settled (author, 2026-10-12): bullets break only things like glass.** The rule is the same
hardness comparison as above, not a list: a projectile breaks a block whose material is
**brittle and below a hardness floor**, and nothing else. Glass qualifies because of what it is,
so a pack's pane does too and nobody wrote a tag for it. Stone, metal and wood are marked and
unbroken. It also settles the griefing question, which was the other half of the objection.

**The marking figure is `toughness`** (mechanics §5.6), read as the threshold a projectile's
momentum has to beat -- glass is hard and cannot take a blow, so its toughness is low and
almost anything breaks it. Create Big Cannons does the same two-figure split (§3.6): momentum
against a block's *toughness* decides whether it breaks; the block's *hardness* against the
projectile's own decides bounce and shatter. **[OPEN]** to check against the material files
that glass's `toughness` really is the lowest rather than assuming it.

### 3.6 What Create Big Cannons does (read 2026-10-12, `create-v6-1.21.1`, code only)

CBC is the nearest thing to a *physical* chamber and charge that exists for this ecosystem. Its
shape is a column of blocks (propellant, projectile, empty barrel) walked in order when a cannon
fires, with every figure on a datapack row per material or per propellant. Nothing is taken;
each finding is a design reading.

| CBC does | Where | Here |
| :--- | :--- | :--- |
| **A propellant has *strength* and *stress* as two independent numbers**, plus `added_recoil`, `added_spread` and an `explosion_power` for when it goes wrong. What a charge *delivers* and what it *does to the barrel* are separate. | `BigCannonPropellantPropertiesComponent` | **Take it.** It is the engineering decision black against smokeless powder actually is: same energy, different peak pressure. A propellant material carries total energy and a burn rate; peak `Pu` is derived from those and the case volume, and the *rating* is what it is read against. |
| **Barrel material carries a safe-stress ceiling, a weight, a minimum spread and a spread reduction per barrel length**; spread only falls to the floor. | `BigCannonMaterialProperties` | **Take it.** A floor that better equipment narrows toward and never collapses is philosophy §8's noise floor; here it comes from bore and material, not a stat. |
| **Failure has a *mode*, `rupture` or `fragment`**, set per material. | same | **Take the idea, reject the flag.** Whether a failed chamber splits or throws fragments is the material's *character*: ductile metals open up, brittle ones shatter. Derived from `ductility` and `toughness`, so a pack's metal fails the way it is. |
| **Failure is a *chance***: `overloadBurstChance` 0.5, `barrelChargeBurstChance` 0.2, `squibChance` 0.25, all config floats. | `CBCCfgFailure` | **Reject.** A band read off the spec sheet, with the noise floor sitting inside it (§3.2). A tuning constant is exactly the dial philosophy §3 distrusts, and it makes "was I unlucky or was it built wrong" unanswerable. |
| **Squib: too little charge for the barrel length and the projectile sticks.** `mayGetStuck(charges, barrelTravelled)` is `minimumVelocityPerBarrel > charges / barrel`. | `BigCannonMaterialProperties.mayGetStuck` | **Take it, and notice what it is.** It is philosophy §6's *underheat* direction: the ladder is "nothing happens / degrades / spoils / hazard" in *either* direction. An undercharged round that lodges in the bore is the other half of the pressure story, and the next round into an obstructed barrel is §3.2's hazard arriving unasked. **[OPEN]** how a lodged round is cleared; the first cut is a tool and a lost turn. |
| **Gaps in the propellant column risk incomplete ignition** (`interruptedIgnitionChance`). | `fireShot` | **Not applicable** to a loaded cartridge. Noted because a *hand-loaded* case with an air gap is a real hangfire; if the Reloading Press can seat a bad round, this is what the bad round does. |
| **Muzzle velocity *is* the charge count**: `projectile.shoot(…, chargesUsed, spread)` takes it as speed. Barrel length changes only spread and squib, never speed. | `fireShot` | **Depart on purpose.** §3.3's pressure over bore area over barrel length makes length matter to speed, which is the whole trade a long barrel is. CBC's shape is a game abstraction we have the material system to improve on. |
| **Penetration: momentum (mass × incident velocity, with a bonus above a speed) against the block's `toughness` decides whether it breaks; the block's `hardness` against the projectile's `penetration` decides bounce and shatter; the projectile loses mass as it goes.** Incidence angle matters. | `calculateBlockPenetration` | **Take the two-figure split and the angle.** Matches `toughness` for the break threshold (§3.5), and `hardness` already gates blows (§17). Mass lost on penetration is a physical consequence, no new rule. |
| **Block properties default from vanilla's own destroy speed**, overridable per block in a datapack. | `BlockArmorPropertiesHandler` | **Take the fallback.** A block no material claims reads from a vanilla figure, the way plates fall back to `sable:mass` (vehicles spec §4). |
| **Drag is `formDrag × mediumDensity × speed` (× speed again if quadratic), clamped to speed; gravity is constant; medium density adds fluid drag.** | `AbstractCannonProjectile.getDragForce` | **Take it, and note it falls out of units we have.** Medium density is `Mu/mB`, so air against water needs no table, and a bullet in water slows because the medium *is* denser. |
| **Sound and plume scale with charge**: power = clamp(charges / 16), the pitch *drops* with power (semitone shift `2 − 8·power ± random`), volume 10 + 30·power, blast-wave audible distance ∝ charges, with a distance cull on who receives it. | `fireShot` | **Take the principle.** One re-pitched sound scaled by a figure is the cheap version; §7's layers are the expensive one. Pitch falling as power rises is correct and worth keeping. |
| **Recoil is `projectile.addedRecoil + propellant.recoil` × a config scale**, applied to the mount as a force. | `fireShot`, `controller.onRecoil` | **Take the interface, reject the number.** The mount taking a force vector at the breech is the right seam for §8; the magnitude is momentum, not a constant. |
| **Catastrophic failure explodes in proportion to the charges used**, and blocks break unless griefing is disabled. | `fail`, `CBCCfgFailure.failureExplosionPower` | **Scale with the charge, yes; world damage, no.** §3.2's hazard is to the shooter and the gun. |

**What this changes in this document:**

- §3.1 gains a figure: a propellant is **energy and burn rate**, not one number. §4.1's new
  `Material` fields are therefore `burn_rate` (and `ignition_temperature` if `Fuel`'s is not
  enough), alongside energy.
- §3.2's hazard band needs *both* directions: overpressure past the rating, and the squib that
  makes the next shot one.
- §3.5's break threshold is `toughness`; its bounce and shatter are `hardness` against the
  projectile's.

---

## 4. Designing a chamber: the Stand, blueprints, villages

### 4.1 The bench

A **Gun Stand** in the Engine Stand's shape: paper slot, discrete settings, a **sample slot**
that names the material from any form of a metal (`Material.find`, never an id), live readout
of the baked figures, Print. Every material figure comes from `material/*.json`; anything a gun
needs that a material lacks is **added to `Material`** (the standing rule, vehicles spec §6).

Likely new fields: a propellant's burn rate and ignition temperature (or reuse `Fuel`'s),
`barrel_erosion` for how fast a bore wears under hot gas. Both optional, both absent-means-
unmeasured.

### 4.2 Blueprints

A gun design becomes a **Blueprint item**, copyable, the Punch Card / Engine Blueprint shape.
Crafting the blueprint with the material's ingots yields the gun, which carries its baked
record.

### 4.3 Villages

Ruling 4: real-life models' blueprints spawn in villages, and the player makes their own.

- **Weaponsmith** is the natural home — vanilla already has the profession and its chests.
  Loot-table entries and/or trades; a table, not code.
- **A found blueprint is a design, not a recipe.** It can be copied, reprinted and *read* in the
  Stand — which is what makes "make your own" a continuum rather than a fork: open the village
  design, change the barrel band, print the variant. This is philosophy §5's *objectively bad
  and still valid* in miniature.
- **Inspired, recognisably, and not the thing** (author, 2026-10-12). Someone who knows guns
  should look at a village blueprint and say what it is drawn from. Nobody should be able to
  point at it and say it *is* that. What that means in practice:
  - **Take** the mechanism, the silhouette's logic, the role and the era -- a short-recoil
    pistol, a bolt-action rifle with a box magazine, a pump shotgun. Mechanisms and shapes
    dictated by function are not what a name protects.
  - **Do not take** a manufacturer's name, a model designation, a logo, a marking, or a
    slogan, and do not trace a specific model's outline. Names are descriptive and invented
    ("Service Auto," "Marksman Bolt"), with the era and the nation's flavour doing the rest.
  - **Figure sets are plausible, not copied.** A blueprint's numbers come out of the same
    derivation as a player's, so they land near the real thing because the physics does.
  - **This is a design stance and not legal advice.** Whether a given blueprint is far enough
    from its source is a question for someone qualified, per blueprint, before release; the
    stance above makes that review short and does not replace it.

---

## 5. Ammunition is made, not found

The chain composes systems that already exist, which is what makes it cheap:

| Part | Made by | Already exists |
| :--- | :--- | :--- |
| Case | drawn from sheet — a deformation step | deformation family; brass is a Slice 1/2 alloy |
| Projectile | cast in a mold | `MoldItem`, casting, lead |
| Charge | a propellant material | vanilla gunpowder (creepers — philosophy §2's phenomena, before any chemistry exists) |
| Loaded round | a **Reloading Press**: seat projectile and charge in a case | new, a physical operation |

A press that *seats whatever is placed in it* never names a cartridge (CLAUDE.md: machines
define operations, not recipes). Over-seating a charge — a press that does not know it is done —
is an overrun, and a loaded round that is over-pressure for its own case is the spec sheet
disagreeing with the player, which is a diagnostic JEI can show.

**Progression falls out of pressure, not out of a tier.** Black powder is low `Pu`; smokeless
needs chemistry the roadmap puts later (`feedback_progression_roadmap.md`, the nitric-acid era).
A low-pressure chamber is cheap and a high-pressure one needs a stronger material — *the
recipe is not locked, the process is difficult* (§7), and which axis this advances is
**materials and chemistry**, then **machine shop** (a barrel is a drilled and bored blank —
`feedback_machine_shop_spec.md`'s removal primitive, not re-read for this document; precision
there is a soft gate on spread, never a lock).

---

## 6. Wear

**Settled (author, 2026-10-12): guns wear with proper use.** CLAUDE.md names two accepted
exceptions to *wear only from misuse* (grinding-wheel grit, hand-hammer durability) and
vehicles were ruled **tools** that wear with use (vehicles spec §1.4). Hand-held guns join
the vehicles' side of the line, which is the third such ruling and so a pattern now rather than
a carve-out: *a tool wears with use; a machine wears with misuse.* CLAUDE.md's hard-rules entry
should say so when this is built. Don't extend it past tools.

- **A hand-held gun is a tool** and wears from use — bore erosion from rounds fired, faster when
  hot. Consistent with vanilla, where tools wear.
- **Overpressure is misuse** and stays visibly distinct in the tooltip: *worn* against
  *damaged by overpressure*, the same two lines the vehicles spec asks for (§7 there).
- **A gun on a Stand or fixed mount is a machine again** and wears only from misuse — the
  vehicles spec's own boundary, not a new one.

Condition is a percentage that multiplies the spec sheet directly (philosophy §8) and bottoms
out above zero. A worn gun is a *worse* gun, never an empty one.

---

## 7. Feel

The failure mode to design against is **a gun that is correct and dead**. Feel is a
presentation-and-latency problem on top of a physical model, so it gets its own contract —
and the contract's central rule is the one thing that keeps feel from being a pile of
hand-tuned constants:

> **Every feel parameter is derived from a physical figure.** A heavy gun feels heavy because
> its impulse is large. There is no `recoilKick = 3.5` anywhere.

| Feel | Driven by | Not |
| :--- | :--- | :--- |
| Camera kick | impulse (projectile momentum + gas) | a per-gun constant |
| Sound weight and tail | muzzle energy, barrel length, suppression | a per-gun sample choice only |
| Muzzle flash, smoke | charge, barrel heat | |
| Cycle feel | cycle time from the design | |
| Spread bloom | barrel heat, condition, held-vs-braced | |
| Vehicle shove | the same impulse, applied to the body | |

### 7.1 What TacZ does for feel (read 2026-10-12, 1.20.1, code only)

Findings, and what each becomes here. None of this code is taken; each is a design reading.

| TacZ does | Where | Here |
| :--- | :--- | :--- |
| **Fire rate is wall-clock milliseconds, not ticks.** Cooldown is `interval − (now − lastShot)`; shots are scheduled on a separate executor, so a cyclic rate is not locked to the 50 ms tick. | `LocalPlayerShoot.doShoot`, `getCoolDown` | **Take the principle.** A tick-quantised rate makes every gun feel the same and caps rpm at 1200. Several shots per tick must be legal, each with a sub-tick timestamp. |
| **Client predicts, server validates by timestamp.** The client sends `now − baseTimestamp`; the server accepts within ±300 ms (plus two tick-times of slack), checks cooldown against that timestamp, and re-syncs the base on a miss. | `LivingEntityShoot.shoot` | **Take it.** Matches §7's latency rule; the window is the cost of honest lag. |
| **Recoil is a per-shot spline over time**: keyframes `(time, [lo, hi])`, each value drawn at random in its range, cubic-interpolated, scaled by modifiers (aiming ÷ `min(√zoom, 1.5)`, prone × a data factor). | `GunRecoil`, `CameraSetupEvent` | **Take the shape, not the numbers.** Keyframe *shape* is shared; amplitude comes from impulse (§7); `[lo, hi]` is the noise floor (§8). |
| **Recoil really moves the camera**: each frame's delta is subtracted from the player's actual pitch and yaw, so the aim is *displaced*, not decorated. | `applyCameraRecoil` | **Take it.** Recoil that only shakes the view is what a dead gun does. |
| **Spread is five stances**: stand, move, sneak, lie, aim, one value each. | `InaccuracyType` | **Agrees with §3's named options.** Values here are derived from the design, and heat and condition widen all five. |
| **Second-order dynamics smooth FOV, aim and jump sway; Perlin noise drives fire sway.** | `FirstPersonRenderGunEvent`, `CameraSetupEvent` | **Take it.** Cheap and a large share of what "feels good." No new rule. |
| **One shoot sound**, pitch 0.9–1.025 jitter, volume 0.8; audible distance = config × a per-gun multiplier, with its own for suppressed; dry-fire debounced until the trigger is released; a concurrency limit per sound. | `SoundPlayManager` | **Take the debounce, jitter and concurrency limit.** Audible distance is a *figure* (muzzle energy, suppression), and the sound is layered (§7). No layering found in what was read. |
| **Entity per bullet**, swept ray over the tick's movement (no tunnelling), friction, gravity, a `pierce` count, damage from a *distance table*, shooter velocity added (not vertical on the ground). | `EntityKineticBullet` | **Proven at its scale; reopens §7's swept-segment [OPEN].** Damage here is energy and penetration is a hardness comparison, not a table and a count. |
| **Heat is a counter** with an overheat *lock*: fire is refused until it has cooled. | `LivingEntityHeat`, `tickHeat` | **Rejected.** Hard lockout with no physical impossibility behind it. §3.4's thermal body with soft consequences. |
| **Per-gun Lua scripts** (shoot, tick_heat and others) as the escape hatch. | `ModernKineticGunScriptAPI` | **Rejected.** Pack authors extend by adding *designs and materials*, never by running code in the shot path. |
| **Animation state machine** with discrete inputs (draw, shoot, reload, bolt, inspect, walk, run, idle...) over Bedrock-format models. | `GunAnimationConstant`, `BedrockGunModel` | **A state machine with discrete inputs is the right shape.** Animations key on *action and part*, not on a gun id (§2.1). |

**Latency is the thing most worth getting right and the easiest to get wrong.**

- **Fire is predicted on the client.** Flash, sound, recoil and tracer play on the frame the
  button goes down; the server validates (cooldown, ammo, pose) and resolves the hit, and the
  client reconciles. Waiting for the server makes every gun feel like it is on a bad ping.
- **No entity per bullet** (recommended). The server **steps the projectile as swept segments**
  each tick — gravity and drag, a block clip and an entity box sweep per segment — and the
  client draws the tracer. Cheap, cannot tunnel, no entity-sync cost. **[OPEN]** against an
  entity per projectile, which is simpler to render and worse for rate of fire.
- **Mounted guns inherit the body's velocity.** Muzzle speed plus the Sable body's velocity at
  the muzzle — free, and correct.

**There is an irreducible noise floor** (philosophy §8) and it is *good for feel*: a deterministic
recoil pattern is learned once and then ignored. Spread has a floor that better equipment
narrows and never collapses.

**Information.** Philosophy §8's free/costs split lands cleanly: a **round count is free**, because
a gun is an item and *an item's reading is passive* (§8, *Some readings are free*); barrel heat is
an adjective — shimmer, smoke, glowing — and a number costs a thermometer, which already reads
items passively. Instruments never lie, so no HUD may print precision the player has not bought.

**Presentation needs an animation path** (first-person reload, chamber, sights). See §7.2.

**Sights** are a part and a discrete choice (iron / optic / none), not a zoom slider.

### 7.2 The animation path, and GeckoLib (read 2026-10-12, `1.21.1`, MIT, code only)

**What it is.** A keyframe animation engine. Models are Bedrock-format `*.geo.json` baked at
resource reload into a `BakedGeoModel` (a record of `GeoBone`s holding `GeoCube`s); animations
are keyframe files with 30-odd easings, Molang expressions, and sound, particle and custom
keyframes; an `AnimationController` plays one animation at a time and a model may have several
controllers running concurrently.

**What it gives that matters here:**

- **Concurrent controllers.** A reload on one controller, a bolt cycle on another, and a walk
  bob on a third, without a hand-rolled blend. This is most of an animation system.
- **Animation speed is a function.** `setAnimationSpeedHandler(Function<T, Double>)` scales
  playback per animatable. A reload that plays at the speed the design's cycle time says is
  §7's rule (*every feel parameter is derived*) with no extra machinery.
- **Keyframed events.** A sound keyframe in the reload fires the bolt-click on the frame it
  looks right, instead of a timer that drifts from it.
- **Per-stack state.** Animation state is keyed on a stack id held in a data component
  (`AnimatableIdCache`), so two guns animate independently with no per-gun-id table -- which
  is §2.1's rule, honoured by the library.
- **A local trigger exists.** `AnimatableManager.tryTriggerAnimation` is callable on the client,
  so a predicted shot can start its animation on the frame the button goes down. *Read from the
  signature; not tried.*

**What it does not give, and §2.1 needs:**

- **Runtime composition.** `GeoModel.getBakedModel` reads `GeckoLibCache.getBakedModels()` by
  `ResourceLocation` and throws if the path is not under `geo/`: models are *asset files*. A gun
  assembled from parts has to override `getBakedModel` and return a `BakedGeoModel` built in
  code from the parts' bones. The types are public records, so it is possible; it is also off
  the intended path and **untried**. A **bone-name contract** is ours to write: parts must
  expose the same named bones (receiver, barrel, magazine, bolt, ...) so one animation set
  plays on any assembly.
- **First-person pose.** Nothing in it positions a held item for aiming down sights, applies
  sway or smooths FOV; `GeoItemRenderer` is a block-entity-without-level renderer that applies
  the item's display transform. TacZ did all of that itself (`FirstPersonRenderGunEvent`), with
  second-order dynamics (§7.1). That part is ours either way.
- **Sub-tick time.** Transitions are measured in ticks. Animation *playback* is smooth between
  ticks, but the fire timing that §7.1 puts in milliseconds is not the library's business.

**What it costs.** A **runtime dependency** (a hard dependency like Sable, or a jarJar). It is
MIT, so the licence is not the obstacle; the cost is a second library whose version must track
NeoForge's, and the 1.21.1 branch tracks both Forge and NeoForge from one tree. The deploy
script already pins non-jarJar runtime deps through packwiz, so it is a few lines there.

**Recommendation, not a ruling.**

1. **Not yet.** Step 1 (§11) needs none of it: camera recoil, FOV smoothing, sound, tracer and
   timing are all independent of the animation library, and a gun with a static model and a
   fire kick is enough to judge feel. Deciding a dependency before there is a reload to
   animate is paying fixed cost with nothing to spend it on.
2. **Then probably yes.** A multi-step reload with synced sounds is the first thing that
   cannot be done honestly with item-model transforms, and writing a keyframe engine,
   easings, blending and event keyframes is what TacZ had to do and is a large block of code
   that is not this mod's subject.
3. **Whichever way, the bone-name contract and the part-composition rule (§2.1) come first.**
   They are the design, and they cost the same with or without the library.

**[OPEN]** (author) take GeckoLib as a dependency when the first reload wants animating, or
write a smaller engine -- **widened 2026-10-14:** the author is not sure GeckoLib is the best
library for guns at all, so the alternatives get compared before anything is committed to. And a spike worth doing before committing: build one `BakedGeoModel`
from two part models in code and animate it, which settles the one thing above that is
unverified and that the whole recommendation leans on.

---

## 8. Vehicles

Ruling 3. Three rules fall straight out of existing vehicle rulings, and nothing is new:

- **A mounted gun is a block in the vehicle's base**, read by tag and property (vehicles spec
  §4), lifted with the car.
- **The trigger is an actuator, and an actuator is a switch** — which is exactly what firing is.
  Seat → controller → trigger, always (vehicles spec, *Controls*). No exception to philosophy §13
  is needed here; fire is open or closed.
- **Recoil is applied to the Sable body** through the same `ForceTotal` channel springs use —
  conservation of momentum, not a canned shove. A heavy gun on a light buggy moves the buggy,
  which is §5's trade arriving unasked.

**Aim** is a steering-shaped problem: an actuator turning a shaft to an angle, and the linkage
is whatever the player builds. **Fixed forward guns first**; a turret is that plus a bearing
(vehicles spec §5), later.

**Ammunition feed** is an item inventory the mount reads, so a belt or a hopper works without a
rule for either.

---

## 9. Prior art and open questions

**Asked and answered:** TacZ's feel is the reference; its architecture is not (ruling 2). The
author approved all three below, 2026-10-12, subject to licences. Licences checked 2026-10-12,
cloned outside the repo. **TacZ read 2026-10-12 (§2.1, §7.1); Create Big Cannons read
2026-10-12 (§3.6); GeckoLib read 2026-10-12 (§7.2).** All three done.

| Source | Clone | Code | Assets | For |
| :--- | :--- | :--- | :--- | :--- |
| **TacZ** (`MCModderAnchor/TACZ`) | `../TACZ-reference`, `1.20.1` | GPL-3.0 | **CC BY-NC-ND 4.0** | feel only: camera, sound layering, animation timing -- not its data model |
| **Create Big Cannons** (`Cannoneers-of-Create/CreateBigCannons`) | `../CreateBigCannons-reference`, `create-v6-1.21.1` | MIT | **CC BY-NC-SA 4.0** | a *physical* chamber and charge |
| **GeckoLib** (`bernie-g/geckolib`) | `../GeckoLib-reference`, `1.21.1` | MIT | n/a | first-person animation, if it earns a dependency |

- **Every asset above is off limits**, from all three, permanently (CLAUDE.md: assets are always
  no). NC and ND each rule them out on their own; CBC's textures and models are CC BY-NC-SA, the
  same licence as Futureazoo's, which CLAUDE.md already forbids. Code only, and read-the-design
  rather than copy-the-code is still the working rule.
- **TacZ has no `1.21.1` branch.** The newest is `1.20.1` (others: `1.18.2`, `1.19.2`), so the
  standing rule *branch must match the target* cannot be met. It is a **reading** of feel, which
  survives a version gap; nothing structural should be inferred from API shapes that differ
  between 1.20.1 and 1.21.1 (data components, `CustomPacketPayload` networking). Whether a
  1.21.1 port exists elsewhere has not been checked.
- **TacZ's code is GPL-3.0**, and Feedback is GPL-3.0-or-later. Compatible in the direction that
  matters, but if any of its code were ever adapted the combined work would be bound to
  GPL-3.0 as written and not "or later" -- a decision for `LICENSING.md`, not an accident. It is
  also why TacZ is read for design only.
- **CBC's branch** `create-v6-1.21.1` was picked because its `gradle.properties` says
  `minecraft_version=1.21.1` (mod version 5.11.7 there). Its default branch, `multiversion`,
  was not read for which versions it carries. **CBC is MIT for code**, so adapting is legal,
  and the working rule stays *read the design, write the code*. Nothing was taken;
  `THIRD-PARTY-LICENSES.md` needs a row only if something is.
- **GeckoLib** is MIT (`modLicense=MIT`, `LICENSE` on `main`). It would be a **runtime
  dependency** (a jarJar, or a hard dependency like Sable), which is a bigger decision than a
  reading: not made. Read 2026-10-12: §7.2 -- what it gives, what it does not, and a
  recommendation to defer until the first reload wants animating.

Per CLAUDE.md, record in `THIRD-PARTY-LICENSES.md` per site **when something is actually taken**
from any of them, with what was and was not.

**Open, collected:**

- §3.2 — what a hazard-tier failure does to the shooter and the gun.
- §3.6 — how a lodged (squibbed) round is cleared; whether `burn_rate` needs a unit.
- §3.3 — step size of bore, case length and wall.
- §3.5 — how a living entity expresses a material for penetration; which figure marks *brittle*.
- §4.3 — each blueprint reviewed before release (design stance recorded, legal review not done).
- §7 — swept-segment projectiles against entities.
- §7 — the animation path.
- §7.2 — GeckoLib as a dependency, and the build-a-`BakedGeoModel`-from-parts spike.
- §9 — whether a TacZ 1.21.1 port exists.
- §10 — the empowerment axis: how strong a gun may be, early, against vanilla mobs.

---

## 10. What this asks of the other documents

Per CLAUDE.md, a document that this one contradicts changes in the same change as the code, and
none is built yet, so these are **proposals**:

- **Philosophy §14, *Empowerment is a parallel axis*** — its **[OPEN]** gains a first candidate.
  The existing text (*technology being industrialized onto the player*, not a Doom Slayer leap)
  is the constraint a gun has to meet: it should read as the same craft as the factory, which is
  why §2 and §5 here insist a gun is assembled from existing systems.
- **Philosophy §6** — the severity ladder's *hazard* tier gets its first hand-held case, if
  §3.2 resolves to yes.
- **`feedback_mechanics.md` and `core/unit/Pu`** — chamber pressure is `Pu`, **settled** (author,
  2026-10-12): it is the same fundamental quantity as the air in a Pressure Vessel and a unit is
  a unit, whatever the scale. The scale gap is the same shape as the engine's RPM against the
  block network's, and was accepted there for the same reason. What it asks is small: `Pu`'s
  javadoc says *air*, so it should say *pressure of a gas* when the first chamber reads it, and
  any readout or formatter must not assume vessel-sized figures. No new unit: energy is `Work`,
  speed and drop are vehicle-style internal floats that never cross a system seam.

---

## 11. Build order

Feel carries the most risk, so it comes **first**, but with the data model already in place — a
feel prototype on a hard-coded constant would teach the wrong lesson and then be rewritten.

1. **A fire loop on a baked record.** One hand-authored gun, a `GunDesign` → baked-figures
   function, client-predicted fire, swept-segment projectile, kick and sound driven from the
   figures. Judge the feel before anything else exists.
   **Built 2026-10-12, game-tested, felt in game and approved by the author.** `weapon/`: `GunDesign` bakes into
   `GunFigures`; `GunShots` steps a swept segment a tick on the server; `client/GunClient`
   predicts fire, plays sound, and kicks the real view. A starter gun (iron, 9 mm,
   medium barrel, standard wall) is in the creative tab -- an ordinary `feedback:gun` carrying the
   starter design; the separate `feedback:test_pistol` item was removed 2026-10-15 so no gun has
   an id of its own. Game tests (`GunGameTests`): figures
   trade as §3 promises (wall against rating and weight, barrel length against speed, spread and
   weight, bore against energy and kick, metal against rating); the cycle is wall-clock and
   tolerates jitter without letting a shot through early; a shot lands on a zombie five blocks
   out. Found by building it:

   - **Baking is lazy, for the creative starter gun only.** The figures are baked on the server
     the first time a held gun is ticked without them (`GunItem.inventoryTick`), because the
     creative tab cannot rely on the material table. A gun made at the Stand (step 2) arrives
     baked; the lazy path stays for the creative stack.
   - **The cycle falls out at about 200 ms** (300 rpm) for the starter gun, from impulse; the
     figures put a 9 mm iron pistol near 300 m/s and 440 Work. Every constant is a placeholder
     in `FTuning`.
   - **Chamber rating needed no new unit.** `rating = yield × wall / (bore / 2)` is thin-wall
     hoop stress turned round, and it lands on the same numeric scale as the peak pressure only
     because `Pu` and a material's `yield_strength` were given one. Lead is over its rating
     (about 9×) with black powder, copper just over, iron and steel well under -- the right
     order, and what makes §3.2 a decision rather than a number.
   - **A GameTest's barrier shell is real.** Every test's 1×1×1 `empty` structure is wrapped in
     barriers; a shot fired from inside it hits the barrier at the muzzle (the clip starts inside
     a collider and reports a hit at its own start). Tests that shoot stand outside it.
   - **Rate tolerance first drafted too loose.** A flat 100 ms early allowance on a 200 ms cycle
     let a shot arrive 10 ms after the last. It is now the smaller of 100 ms and a quarter of
     the cycle, and the schedule books from arrival. Timestamp sync (§7.1) replaces it.
   - **Catching up is a flam.** The first client loop kept the gun's rhythm exactly: a frame
     that arrived late fired late, and the next shot was still due on the original schedule, so
     a hitch became two shots close together -- and a long hitch became up to three in one
     frame. Heard in play as a drum flam. Now one shot per frame at most, and the gap never
     falls below three quarters of a cycle; a hitch costs the long-run rate instead. This is
     the wall-clock rate of §7.1 meeting the frame rate it is polled at, and it will matter more
     as cycles shorten: a gun faster than the frame time needs several shots per frame by
     design, with sub-tick timestamps (§7.1), not by catching up.
   - **Not in this step, so not judged:** tracer, shooter-velocity inheritance (a server-side
     player's `deltaMovement` is not trusted to be their motion), layered sound (the shot is
     one re-pitched vanilla sound), ammunition (it fires forever), fire modes (held is
     automatic), glass breaking, and overpressure having any consequence (the figure is shown
     on the tooltip and nothing reads it yet).
2. **The Stand and the Blueprint** — print a gun from settings; spec sheet in JEI.
   **Built 2026-10-12, game-tested (`standPrintsAndTheRecipeBuildsTheGun`), not yet used in
   game; the JEI spec sheet built 2026-10-14, not yet seen in a running client.** A separate block, not a mode on the Engine Stand
   (the author's ruling: the settings differ, and a vehicle stand holding gun settings is the
   wrong coupling). `weapon/GunStandBlock`/`GunStandBlockEntity`/`GunStandMenu`,
   `net/GunStandPayload`, `client/GunStandScreen`, `GunBlueprintItem`, `GunRecipe`; items
   `feedback:gun` and `feedback:gun_blueprint`, block `feedback:gun_stand` (creative tab).
   Settings: bore (1 mm steps, 5 to 20 -- step size still [OPEN]), barrel band, wall band; the
   sample slot sets the material. What it settled:

   - **The blueprint carries the figures.** The Stand bakes at print, on the server, from the
     server's own material table, and writes design and figures onto the paper together; the
     recipe copies both onto the gun. A client names a material, it does not vouch for it: a
     material that is not a printable metal prints nothing and the paper stays paper. The cost
     of baking at print is that a blueprint printed before a tuning change keeps its old
     figures; a gun is a *record*, and re-baking on load is the alternative if that bites.
   - **Cost is a flat price by band, as proposed:** barrel short 2, medium 3, long 4 ingots,
     plus wall thin 0, standard 1, thick 2 (`FTuning.GUN_INGOTS_*`; a design is never free). A
     pistol's metal is tiny at Minecraft scale, so converting mass to ingots would price every
     handgun at under one; a band price keeps the long-barrel-against-weight trade readable.
     Placeholder, [OPEN] like the engine's.
   - **The screen shows the chamber in red** when the charge is over the rating, so a bad gun is
     seen on the Stand and not found by firing it.
   - **The JEI spec sheet is a card per gun metal** (`GunMetalCategory`, 2026-10-14). Every
     design is its own, so there are no guns to list; the Stand's first question is the barrel's
     metal, and each card answers it with one reference design (9 mm, medium, standard wall)
     baked in that metal: chamber rating against a nominal gunpowder peak (red when under),
     muzzle work, gun mass. Free and exact, as a requirement is (philosophy 8).
   - **Shared with the Engine Stand by copying, not by a helper.** The sample-slot rule is five
     lines (`Material.find` filtered by "a metal, weighed, with a yield strength and an ingot");
     a shared helper would have touched `EngineDesign` for nothing. The two files are the same
     shape on purpose, and the third stand is when to factor them.
   - **Lead is the projectile, named once** (`GunDesign.bake(Map)`), until step 3.
   - **The screen is a blueprint, and a gun is four parts (2026-10-13, author's sketch).** The
     gun is drawn on a grid and reshapes with the design; click a part to edit it. Barrel holds
     bore, length and wall; slide holds the sight; trigger holds the launcher; grip holds nothing
     yet. **Each part has its own material** (§2's "a gun is parts", first cut): the sample slot
     sets the *selected* part's metal, so the screen applies it and sends the edit, and the
     server no longer sets material on insert -- it vouches for every part's metal at print, as
     it did for the one. The barrel's metal is still the only one the chamber's rating reads.
     The other three are metal in the mass (slide 0.15, trigger 0.03, grip 0.12 mB, the old
     0.3 mB frame split) and in the price: each costs one ingot of its own metal, the slide also
     pays for its sight, and the recipe counts ingots per metal. A design saved before parts
     reads as all one metal. **[OPEN]** what a part's metal should do beyond mass and price:
     nothing reads `gunMass` in play yet, so a copper grip is today a cost and a tooltip line.
     Kick against the gun's mass (recoil velocity is impulse over mass) is the obvious first
     reader and would make a heavy grip a real trade; not done here because it moves every
     gun's kick.
3. **Ammunition chain** — case, cast projectile, propellant material, Reloading Press.
   **Built 2026-10-12, game-tested (`aRoundIsFiguresAndTheGunFitsIt`, `pressSeatsWhatIsInIt`,
   `magazinesLoadAndReloadSwaps`, `theMoldShapePicksTheCasting`), not yet felt in game.** The
   author's rulings: a **magazine item**, the press a **block with a lever**, projectiles made
   **both cast and hammered**. What exists:

   - **A round is a record of three materials and a count** (`weapon/Cartridge`: bore, case
     material, propellant material, charge, projectile material). No item id anywhere; the
     component rides on `feedback:cartridge`. A gun takes only its own bore (the fit rule).
   - **Propellant is a material figure.** `Material` gained an optional `propellant`
     (`peak_pressure` in `Pu`, `burn_rate` per metre of barrel); `gunpowder` is the first, with
     vanilla gunpowder as its `powder` form. Peak pressure is the propellant's times charge over
     the nominal charge (4, max 8). **Burn rate is a real trade**: the fraction burned by the
     muzzle is `burn_rate x barrel length`, so black powder (4.25) gives a short barrel 0.43 of
     its work and a long one all of it. This is the spec 3.6 "strength and stress as two
     numbers" lesson, with burn rate acting where Create Big Cannons has barrel length.
   - **Per-round bake.** `GunDesign.bake(materials, Cartridge)` derives the gun's figures for the
     round it is fed, at fire time on both sides; the `gun_figures` on the item is now the
     *reference load* (gunpowder at nominal, lead) and is what the Stand and tooltip show.
   - **Case drawn from plate, bullet hammered from nugget.** New form types `case` and `bullet`
     (kinds metal, so every metal gets both, generated); steps `plate_to_case` (draw, the wire
     drawer's operation) and `nugget_to_bullet` (blow, the hand hammer's). **Found by building
     it:** the first draft had `ingot_to_bullet`, and every existing hammer test then made a
     bullet instead of a plate -- two `blow` steps from one form, so a hammer picked whichever
     the table listed first. That is the collision `Operation` was made to avoid; a form with
     two blow outputs is not allowed, so the bullet starts from a form of its own.
   - **Cast in a bullet mold.** `Casting` gained a `mold` shape (default `ingot`) and
     `MoldItem` a shape; the same melt makes one ingot in the ingot mold and eight bullets in
     the bullet mold. Castings for lead, copper, brass, bronze, gold and iron.
   - **The Reloading Press** (`ReloadingPressBlock`): slots case, charge, projectile, output, and
     a **die set** -- the bore it seats for, stepped -- which is where a cartridge gets its
     bore, since a blank case and bullet have none. It seats whatever is in it; it never knows
     a charge is enough, only that a case takes at most 8. The screen shows the round's charge
     against its own case rating, red when over, which is the spec sheet disagreeing with the
     player and nothing more yet.
   - **Magazine and reload.** `Magazine` is a record on both a Magazine item and a Gun; loading
     is a crafting-grid recipe (`MagazineLoadRecipe`, one magazine plus rounds, one bore, ten
     most); R swaps the fullest matching magazine in the inventory into the gun, the old rounds
     coming back as a magazine, and books 1.4 s during which the gun does not fire. An empty
     gun clicks. The server fires what *its* magazine says is next; the client names only aim.
   - **Follow-up built the same day** (`theChamberFeedsFromTheMagazine`,
     `overpressureIsDesignAndSpendsTheGun`, `castingSurvivesTheWire`):
     - **Chambered round.** `GunAmmo`: a trigger pull fires the chambered round (or the
       magazine's top if none) and the chamber refills from the magazine, so a reload keeps the
       chambered round and a gun reads `n in the magazine, 1 chambered`. State is components on
       the stack (`chamber`), nothing else.
     - **Unloading.** Using a magazine empties it into cartridges; sneak-using a gun ejects its
       magazine and chambered round.
     - **HUD.** `client/GunHud`: rounds held (magazine plus chamber) over capacity, spare rounds
       of the held gun's bore carried, and a state word (reloading, empty, spent). The count goes
       red when the next round is over its chamber's rating, so the spec sheet is read at the
       moment of firing.
     - **JEI.** A casting card (mold shape, melt amount, the figure it cools to; the casting
       table now syncs to the client) and a Reloading Press card per propellant (nominal peak
       pressure, burn rate, charge limits; case and bullet slots cycle every form the table has).
       The press has no recipe to list, so the card is what a charge *is*, not what it makes.
     - **Overpressure consequence** (`GunHazard`, the first cut §3.2 asked for). The ratio is the
       round's peak pressure over the chamber's rating, or over its own case's, whichever it
       exceeds more. Over the chamber: the gun takes `(ratio - 1) x 0.15` damage per shot,
       spent at 1 (a spent gun is still an item and refuses the trigger). Past ratio 1.5 the
       shooter takes `(ratio - 1.5) x 6` hp, capped at 16, so one bad shot hurts and does not kill
       an unhurt player. The only randomness is a 25% noise floor on the size. A sound gun is
       never touched however often it fires; copper under black powder only wears. **Vented
       pressure delivers nothing**: a chamber past its rating does work at the pressure it
       held, not the pressure it was given, so a lead gun is weak as well as dangerous.
     - **Found by building it.** The case rating's first draft used a 0.4 mm wall, which put
       *every nominal round* at 6.8 times its own case. The rating is now yield times a wall
       that is a fraction of the bore's radius (the bore cancels: a case is drawn to scale), so
       brass's 5 holds 3.75 against gunpowder's 3 and an overcharge is the thing that exceeds it.
   - **Observation closed (author, 2026-10-12): guns work near Sable.** The mock-player hang
     above was a test artefact; the test stays on its creative-player workaround.
   - *Closed 2026-10-14:* the bullet mold is **two drilled iron plates hinged on stick handles**
     -- the cavities are what the drill press makes, so a mold comes through the machine shop
     like the gun's own parts, where the placeholder was four ingots round an ingot mold. Iron,
     because it melts above everything the casting table pours into a bullet mold. *Also closed:* bullet
     material in a hit (step 4, 2026-10-14), wear (step 4), and **reload as a held action**
     (2026-10-14, the author's ruling): R is held, not tapped. A gun with its own feed loads a
     loose round a beat (`GUN_RELOAD_ROUND_MS`) while R is down and stops with what is in when it
     comes up; a magazine swap happens only once R has been held its whole `GUN_RELOAD_MS`, so
     letting go early keeps the old magazine. Firing or putting the gun away ends either. The
     server runs it (`GunShots.reloadStart/Stop/Tick`, a player tick); the client says only that
     R is down. The old swap-then-lockout booked the cycle clock, which also meant a reload could
     not be broken off to shoot -- the lockout is gone, and shooting is how it is broken off.
4. **Heat, spread bloom, cook-off, condition.**
   **Built 2026-10-12, game-tested (`theBarrelHeatsWearsAndBlooms`, `aHotChamberCooksOff`,
   `aCannonPaysInHeatAndAPistolDoesNot`), not yet felt in game.** `weapon/GunCondition`; the
   author ruled **cook-off can hurt the shooter**. What it is:

   - **The barrel is `ItemHeat`'s stamp and tick**, unchanged: a hot gun cools in a holster, a
     chest or an unloaded chunk because nothing ticks it. It cools at the gun's own rate through
     a new `ItemHeat.Cooling` seam (mass over leak), so a heavy barrel heats less per shot *and*
     cools slower -- two answers to one problem (§3.4: capital and weight against handiness).
     A thermometer reads the same figure the gun's own logic does, because there is one reading.
     **Trap found:** `ItemHeat.set` drops anything under the warm threshold (60 Tu), which would
     have thrown away every shot's few Tu from cold; guns stamp the components directly.
   - **Heat per shot** is the work the bullet got times 0.3, over the gun's thermal mass
     (`12 Work/Tu per Mu` of its own metal). A steel pistol survives a ten-round dump; a 20 mm
     netherite cannon with a double charge cooks off in about nine shots. That is the cost the
     two-taps-a-warden gun was missing. **Retuned 2026-10-13 from times 2**, which put about 28 Tu
     into a pistol a shot and smoked it in nine; most of what is not the bullet's leaves with the
     gas, and a real steel barrel warms a few degrees a shot.
   - **Bloom** widens spread up to 2.5 times at the propellant's ignition temperature -- a soft
     consequence, never a lockout. **Wear** is bore erosion per work delivered, faster when hot,
     and multiplies the sheet (less energy, wider spread), bottoming out at 0.4 condition: a
     worse gun, never an empty one. Overpressure damage stays its own tooltip line.
   - **Cook-off.** A new `ignition_temperature` on `Material.Propellant` (gunpowder 300 Tu). A
     chambered round in a barrel past it goes off by itself, along the holder's look, and hurts
     them (2 hp per nominal charge, capped at 8, the noise floor on it). Gated by the gun's own
     cycle, so a barrel that stays hot **runs away** at the gun's rate until the rounds are out
     or the player ejects the chamber (sneak-use). Nobody wrote a runaway rule: the chamber
     refills from the magazine and the barrel does not cool in a cycle.
   - **The HUD says "cool, warm, hot, SMOKING"**, not a number: a hand can tell warm from hot,
     the figure is the thermometer's (philosophy 8). The barrel also smokes between shots.
   - **Not built:** barrel heat as a rating for *sustained* fire on a vehicle mount (step 5),
     a cooling fitting, and a heavy-barrel *design* option on the Stand beyond wall and length.
   **Penetration and glass, built 2026-10-12** (`aBulletBreaksGlassAndStopsAtStone`,
   `aHeavyRoundPassesThroughWhatItKills`), the author's second pick after heat:
   - **Glass `toughness` checked against the material files, as §3.5 left open.** Glass, ice,
     blue ice, glowstone and redstone are 1 (the lowest); amethyst and obsidian 2; stone 6. The
     marker is `GUN_BREAK_TOUGHNESS_MAX = 1.5`, so glass, ice and their panes and stained
     variants break (by *what they are*: the material table maps each of those items to glass),
     and stone, wood, metal and obsidian do not. A break costs `toughness x 40` Work, and the
     bullet flies on slower; the shooter must be allowed to build there.
   - **Surplus carries through entities.** A bullet that out-damages what the target has left
     gives up only that much and flies on, so a 20 mm round through a zombie is still going.
     Entities express a material by their health, which is [OPEN] §3.5's first cut and is
     honest about being nothing finer.
   - **Built 2026-10-14, the bullet's own material in a hit** (`hardnessDecidesGlancesAndArmour`;
     the author's ruling, blocks and armour):
     - **Breaking is momentum, paid in work.** A brittle block breaks when the bullet's momentum
       beats `toughness x 0.5` kg m/s, and the bullet then gives up the work it always did; the
       toughness ceiling is unchanged, so still only glass-like things break. A pistol round
       carries about 2; a cell-charged lead slug out of a coil does not break glass.
     - **A softer bullet glances off a harder face** when it comes in shallower than about 70
       degrees off the normal, keeping half its energy, at most twice. Block hardness is the
       material's, or vanilla's destroy time where no material claims the block (CBC's fallback,
       §3.6). Lead glances off nearly everything; a bullet harder than the face does not glance.
       Shatter of a brittle *bullet* is not built.
     - **Armour is read off the armour's materials** -- an iron chestplate is iron, hardness 6 --
       and a target's armour is its hardest piece. A bullet harder than that puts
       `1 - armour / bullet` of the hit through as `feedback:bullet_pierce`, a damage type in
       `minecraft:bypasses_armor`; the rest meets the armour as any hit does. Steel through iron
       is 0.6; lead through anything is nothing; diamond (100) stops every metal there is. Armour
       no material claims is never pierced. This is §3.5's first cut for how an entity expresses
       a material; a mob's own hide is still nothing but its health.

   **Handling, built 2026-10-12** (`stanceAndSightsHandle`), the author's third pick, with the
   ruling that **aiming down sights** is part of it:
   - **ADS.** The use key aims: the FOV eases to the sight's at the sight's own pace and the
     gun owns the key (no placing). The server narrows spread to 0.4 only for a shooter on the
     ground and not sprinting (`GunStance`: crouch, moving, sprint, airborne multiply it, the
     five stances as multipliers, spec 7.1). The client says only that it is aiming.
   - **Sight is a design band** (`GunDesign.Sight`: iron, reflex, scope) with a price in ingots
     and a trade: more zoom, a slower raise. It does not change what the gun does, only what
     the player can do with it; a design with no sight reads as iron.
   - **Fire modes** semi, burst (3) and auto are a component on the gun, switched by V; burst
     ends with a beat. **Recoil climbs** over a streak of shots and settles after a pause, and
     aiming takes the edge off it. Unloading is now a key (U), because sneak-use collides with
     aiming on the same button.
   - **Sound is layers** (blast, a thump only a big charge has, the action's clack) from the
     figures. The blast is now **the author's own 9 mm recording** (`sounds/gun/shot_9mm.ogg`,
     registered as `feedback:gun.shot_9mm`, the author's own so no third-party row); it plays at
     its own pitch near a pistol's power and falls with the charge. The thump and clack are
     still vanilla.
   - **Rejected: shooter velocity added to the bullet and a per-bullet tracer entity.** The
     velocity is under 2 percent of a muzzle speed, which makes no decision. The tracer is
     points along the swept segment, not an entity.
   **Blaze powder as a second propellant, and smoke at the hand, built 2026-10-12**
   (`blazePowderIsAHotterFasterCharge`):
   - **Blaze powder** is its own material (not the blaze rod's `powder` form: that stays a
     rod's, and a whole rod is not a charge). Peak 4.5 Pu against gunpowder's 3, burn rate 8
     against 4.25, ignition 220 against 300: it does more work in a short barrel and wants a
     stronger case, and it cooks off sooner. The trade is the point -- strong and quick against
     a barrel that has to be treated. No code names it; the press accepts the powder form of
     any propellant.
   - **A hot gun smokes where it is held**, for every player the client can see, rate and weight
     following the barrel's heat as a fraction of ignition (a wisp at 0.6, a plume at 0.9). The
     HUD word and the smoke are one figure read two ways.
   **Blends, tracers, incendiaries and the coil launcher, built 2026-10-12**
   (`blendsAreReadOffTheirOxygen`, `pressSeatsABlendAndAPayload`, `aCoilThrowsAMagneticSlug`,
   `coilGunsAreWoundFromRedstone`, `anIncendiaryRoundSetsAlight`), from reading the author's
   compendium (`speculative_physics_inspo_doc.md`) against what was built:
   - **Blaze powder correction.** The compendium (§4.2) gives it as carbon and sulfur, a fuel
     with no oxidizer, and gunpowder (§1.6) as nitrate, sulfur and carbon. Blaze powder as a
     *standalone* propellant, as shipped the turn before, contradicted that. A charge is now a
     **blend**: each material has an `oxygen_balance` (gunpowder +0.5, an oxidizer surplus;
     blaze powder -1, fuel that needs one) and a potential. The blend burns completely when the
     sum is not negative, and short of that the fraction burned falls to a fizzle floor. So
     blaze powder alone barely does anything; four gunpowder with two blaze powder is hotter
     and faster than gunpowder alone and over a brass case's rating; too much blaze starves it.
     Burn rate averages by count, ignition is the easiest ingredient's.
   - **Blends are not a recipe.** The press has two charge slots and seats the ratio it is
     given. The author ruled that a mixer *recipe type* should own this and does not exist yet;
     flagged in `TODO.md` §6a and in the press's source. Rounds written before blends read as
     one-part blends.
   - **Payloads are read by property** (a fifth press slot): a **luminous** material in the
     bullet is a tracer (a bright line instead of the faint glint every round leaves), a
     **flammable** one is incendiary (sets the target alight, and fire where it lands if fire
     may be there). Glowstone and redstone dust are luminous and blaze powder is flammable by
     their own `traits`; no id is named. Blaze powder is therefore both a fuel and a payload,
     by the slot it is put in.
   - **The Stand makes a coil as well as a chamber** (the author: *the Stand should make every
     kind of projectile weapon*). `GunDesign.Launcher` is the one fork that changes what the
     gun *is*; the shapes of a chamber gun are a later pass and options of their own.
   - **How a coil is derived, and why nothing new was needed.** A coil's drive is **redstone
     dust**, consolidated redstone being the compendium's ambient superconductor (§3.3). The
     field's pressure is *a pressure*: it goes as the square of the drive and is held by the tube
     with the same hoop-stress rule as a chamber, so `Pu`, the rating, the overpressure ratio,
     the hazard and the Stand's red readout all carry over unchanged. Work goes as the drive,
     the coil's length (stages, saturating) and how well the slug couples: a slug that is not
     `separation.magnetic` barely does (iron and steel couple, lead does not -- the property
     the material system already had). A winding is wound from redstone *blocks* on top of the
     metal, more for length.
   - **A coil quenches, never cooks off.** No propellant, so no cook-off. Above its quench
     temperature (redstone's `ignition_temperature`, reused as that figure) the winding reverts
     and carries nothing until it has cooled: the compendium's torch failure mode, and a hard
     gate with a physical cause.
   - **Not built:** the Stand's gun shapes, shotguns (a cartridge that carries a pellet count
     and a pellet shot), a ghast-style vented breech, suppressors and gunfire as a vibration,
     slime armour, and a mixer.
   **A coil's drive from the electrical network, built 2026-10-14** (`aChargedRoundDrivesACoil`).
   The author's ruling: **the charge rides in the round, not in the gun.** Mechanics 6.9 refused
   item batteries as a charging currency for tools, and a gun holding a capacitor would have been
   one; a round that is its own small jar is ammunition, which the player already makes, counts
   and spends.
   - **A dry round is a jar.** The press seats a case and a slug with no charge as a jar round
     (`Cartridge.stored`: volts and the tick they were stamped). It does not know whether dry was
     meant, only what it was given, and says so.
   - **The Round Charger** (`machine/charger/`) is a sink on a wire: while a round in it holds less
     than the line it asks for an amp, and what it is delivered raises the stack's volts by amps
     over its capacitance (two amp-ticks a volt a round), up to what it sees and never past. It has
     no rating and never wears: it is a terminal, and an over-high line charges the rounds harder,
     which arrives as field pressure against the winding when fired -- the chamber's overpressure
     rule, unchanged.
   - **Volts are the drive, by the dust's own square law.** `GUN_COIL_NOMINAL_VOLTS` (MV, 6 V) is
     a nominal redstone charge, so a steam-engine dynamo on its fine winding fills a round to
     nominal, a zinc/copper cell (1.1 V) to under a fifth of its work, and a hand-cranked fine
     dynamo past nominal into a thin winding's rating. Which source charged it is a real choice
     with a consequence on the spec sheet, and no exchange rate between `Eu` and `Work` exists
     anywhere: the round holds volts, the coil turns volts into field.
   - **A charged round leaks, linearly, wherever it is**, a stamp and a tick like a workpiece's
     heat (`GUN_ROUND_LEAK_VOLTS_PER_TICK`: nominal to flat in 6000 t, five minutes), so rounds
     go flat in a chest and nothing ticks them. Charge before a fight, not the week before.
   - **Reading it costs an instrument.** Volts are state: the tooltip says *flat, weakly charged,
     charged, crackling*, and the figure only with an electroscope carried (philosophy 8).
   - **Redstone dust stays** as the second answer: it never leaks and needs no wire, and is spent
     as material. Capital and a network against a consumable.
   - **Trap:** rounds charged at different moments carry different stamps and so do not stack;
     accepted, as hot ingots already do not.
5. **Vehicle mount** — block, trigger actuator, recoil into the body.
6. **Village blueprints** — loot and trades, a table.
7. **Sights, attachments as design parts, the animation pass** -- where §7.2's dependency
   decision and bone-name contract land, and the composition spike should run before it.
   **Sights and attachments built 2026-10-14** (`fittingsArePartsThatTrade`); **the animation pass
   deferred** by the author, who is not sure GeckoLib is the right library for guns and wants the
   alternatives compared first (§7.2's [OPEN] stands, wider). The author's rulings: all four
   fittings, set on the Stand and refittable afterwards.
   - **A fitting is a part of the design, never a modifier** (§2). `GunDesign.fittings` maps each
     `Fitting` to the metal it is made of; the bake reads it, and fitting or removing one makes a
     new baked record. Each is machined as a form per metal and assembled like any part, and
     weighs what it is made of. Four, each a trade:
     - **Suppressor** (turned from a cylinder): report 0.35, so the sound is quieter and carries
       less far, and the shot is a short-range vibration; the gas's heat stays in the gun, 1.6
       times the heat a shot.
     - **Muzzle brake** (drilled from a magazine tube): throws 70% of the gas's share of the
       recoil sideways -- less kick, a shorter cycle -- and is 1.6 times as loud. One muzzle, so a
       gun has a suppressor or a brake.
     - **Relief vent** (drilled from a nugget): the Ghast's pressure-release vent (compendium
       §4.4) -- the vented breech §11 step 4 owed. It opens at 0.9 of the chamber's rating, so the
       chamber never sees more and overpressure goes out of the port, not into the gun or the hand;
       it leaks 10% of every shot. A case can still fail on its own rating: the vent is in the
       chamber, not the round.
     - **Barrel shroud** (rolled from a drilled plate): fins, 1.8 times the leak -- the cooling
       fitting step 4 owed. Its metal is thermal mass as well, so it cools faster *and* heats less
       a shot.
   - **Three new figures** carry what the fittings do to everything downstream: `report` (sound
     volume, which in vanilla is also how far it carries, and the vibration), `heatFactor` and
     `leakFactor`. Absent reads as 1, so a gun from before fittings is unchanged.
   - **Gunfire is a vibration** (owed since step 4): every shot is `feedback:gunshot`, radius 32,
     or `feedback:gunshot_quiet`, radius 8, when the report is under 0.5 -- in
     `minecraft:vibrations` and `warden_can_listen`. A suppressor is how to shoot near a warden.
   - **Refit in a grid** (`GunRefitRecipe`): a built gun and one fitting part of any gun metal make
     the gun with it fitted; whatever it displaced comes back in the grid where the new part was.
     One plate or two make it a reflex sight or a scope, as the Stand builds them, and the old
     sight's plates come back in the receiver's metal (a sight's plates were never told apart by
     metal). The gun keeps its rounds, heat and wear, and re-bakes. A part can be swapped, not
     simply removed: taking a suppressor off means fitting a brake, or making a new gun.
   - **On the Stand** the muzzle (open, suppressor, brake) is a barrel row; the vent and the
     shroud are receiver rows. On paper a fitting is in the barrel's metal; the part assembled
     into it decides what it really is. Not drawn on the blueprint yet, and not seen in a client.

Package would be `weapon/`, registering in `registry/FWeapons`, created when there is
something to put in it (CLAUDE.md: don't scaffold empty ones).

## 12. The gun maker, second pass (author's rulings, 2026-10-13)

Asked for: actions, magazine capacity and types (revolver, belt), shotguns, four fundamental
types, grip material in kick and weight, and every part machined before assembly. Rulings:

1. **Fundamental type is the first choice on the Stand** -- marksman rifle, shotgun, pistol,
   LMG -- and decides which parts exist and which actions and feeds are offered. Figures stay
   derived. Prior art: SilentGear's `GearType` (MIT, `1.21.1` branch, read 2026-10-13, design
   only): a type decides which part slots a gear item has. **Not taken:** SilentGear's
   materials carry per-part, per-gear-type stat rows (`"attack_speed/axe": -0.1`) combined by
   average/add/multiply. That is §2.1's row-per-gun problem one level down; our material files
   stay physical figures and every gun figure is derived.
2. **Six actions, each with or without a hammer**: break (hammer), lever (hammer), bolt
   (striker), pump (hammer), self-loading (hammer), automatic (striker). Manual actions cycle
   by hand and slowly; self-loading is one shot a press; only automatic fires held down. The
   fire-mode toggle offers only what the action allows. **A hammer action may take a revolver
   cylinder.**
3. **Magazines are items crafted separately**, a capacity band per recipe; any box-fed gun of
   the bore takes any of them. Feed types: box magazine, tube, cylinder, internal, belt; a
   break action holds one round per barrel.
4. **Shotguns fire shot shells made at the press** with a shot die: the projectile metal cast
   as shot, pellet count derived from bore and projectile mass. No magazine; capacity follows
   action and barrel (break: one per barrel; pump and lever: a tube under the barrel, as long
   as the barrel is). *Built:* there is no die toggle -- the press seats what is put in it, so a
   nugget in the projectile slot is formed into shot and a bullet seats as a bullet
   (`Cartridge.shot`). Pellets are the bore's projectile volume cut into 8 mm buckshot, capped
   at 12, sharing the shot's energy at the same speed in a cone at least 4 degrees wide. A
   shotgun fires shells and nothing else fires them; **a shotgun also takes a bullet of its bore
   as a slug** (author, 2026-10-14) -- only an open bore throws a pattern, but anything throws one
   projectile.
5. **Grip material affects kick and weight.** Kick follows recoil velocity, impulse over the
   gun's mass, so a heavy gun kicks less; the grip's `friction` scales how much of it the hand
   lets through.
6. **Every part is machined, then assembled**, through the form system. A part form carries
   its own geometry as state the way an ingot carries `Fu`, and the machine changes it while it
   runs: a drill grows a barrel's bore, a lathe thins its outside, a cutoff (grinding) wheel
   shortens it. The machine does not know the target, so a barrel drilled too long is past its
   bore, and drilled until the wall is gone it is swarf -- the core pitch, on a gun part.
7. **The world resolves on the part, not the blueprint** (philosophy §8). A gun bakes from the
   geometry of the parts actually assembled. The blueprint keeps the discrete choices (type,
   action, feed, sight) and a target bore, length and wall to aim at, with no tolerance check:
   any barrel assembles, and a sloppy one is a worse gun, never a refused one (§7). A round
   looser than its bore leaks gas (less energy, more spread); tighter overpressures. Calipers
   read a part.

*Built (stage e):* eight new form types per metal -- `gun_barrel` (turned rod, drilled),
`receiver` (ingot, drilled), `slide` (plate, ground), `bolt` (rod, ground), `trigger_group`
(drilled plate, ground), `grip` (ingot, turned), `cylinder` (ground ingot, drilled),
`magazine_tube` (rod, drilled). A gun barrel carries `BarrelGeometry` (bore, outside, length);
datapack `machining/` rows say a drill grows its bore, a lathe thins its outside and a grinding
wheel shortens it, so much per Fu, reckoned as a removal is (force over hardness, a softer tool
does nothing). `Removing` asks `Machining` when no form step answers, so no machine changed.
Assembly is the blueprint plus `GunDesign.assembly()` -- barrel (two on a break-action
shotgun), receiver, slide on a self-loading or automatic pistol or bolt on any other non-break
action, trigger group, grip, cylinder or magazine tube for those feeds, plates for a sight --
and the gun takes each part's metal off its item and the barrel's real shape off its
component. The blueprint's bore, barrel and wall are targets only. A round chambers within 1 mm
of the real bore; looser loses work (to a floor) and widens spread, tighter raises peak
pressure. Calipers read bore, wall and length. *Closed 2026-10-14 (author's ruling): organic
grips are turned, leather wraps.* A grip carries no load, so it needs no yield strength
(`GunDesign.gripMaterial`): the `grip` form type now covers organics, wood turns one from a
stick, bone from a bone, bamboo from a stalk, on the lathe like a metal grip; organics with no
business being a grip (wool, paper, leather, honey, slime, charcoal, nether wood) exclude it, as
the form system already asks of a nonsense form. Leather is not a grip: any organic that is
fibrous or sticky, put in the assembly grid, **wraps** the grip, and the hand holds the wrap, so
its friction is the grip's -- leather 0.6 against wood's 0.4 is less kick. Honey is the stickiest
wrap there is. *Closed 2026-10-14:* a coil with a scope and a tube can need more than nine grid
slots, so the **Assembly Bench** is a crafting table four slots square (`AssemblyBenchMenu`). It
asks the same recipe manager the table does, so it builds a gun from `GunRecipe` and makes
anything a table makes; it keeps nothing, and what is left in the grid comes back on close. Not
seen in a running client yet. *Closed 2026-10-14:* a double's second barrel is its own part
(`GunDesign.SecondBarrel`, metal and real shape); both tubes always weigh, and the round count
says which is loaded -- two rounds in fires the first, one fires the second (`GunDesign.firing`),
so a mismatched pair shoots two different shots. A cylinder or a tube weighs what it is made of
(`Parts.feed`).

*Built (stage f):* the Stand draws a pistol (slide, frame as receiver, grip; a cylinder
ahead of the frame for a revolver) or a long gun (receiver, bolt, stock; a tube under the
barrel, a box or belt under the receiver, a second barrel and a hinge for a double, a fore-end
on a pump, a bipod on an LMG). The action's part is labelled slide or bolt and is absent on a
break action; the sight row moved to the receiver. Not checked in a running client yet.

**Build stages**, each a commit: (a) grip kick and weight; (b) type, action, hammer, fire
modes by action; (c) feeds and magazine capacity, revolver, belt; (d) shot shells and
shotguns; (e) part forms with geometry, the machining table, assembly from parts; (f) the
Stand drawing each type's parts.
