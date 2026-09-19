# Feedback — Machine Shop Spec

> **Status: DRAFT — working document, not one of the three canonical documents.**
>
> Scopes `feedback_progression_roadmap.md`'s "Slice 3 — Machine Shop" into something buildable.
> Same relationship `feedback_controller_spec.md` has to the roadmap's controller-medium
> progression: the roadmap names the roster, this document says how it actually works.
>
> **Naming note:** this is not `feedback_slice_03.md`. The roadmap's own §0.1 already warns
> against confusing its "Slice N" content packages with `feedback_slice_01.md`'s "a built,
> playable vertical increment" — giving this document a slice-numbered name would immediately
> recreate the exact collision that note exists to prevent.
>
> **All numbers below are placeholder**, same convention `feedback_slice_01.md` used —
> concrete enough to implement against, not concrete enough to be balance. Exact Fu costs,
> hardness thresholds, and RPM/Su figures are a tuning pass after something is running, not a
> decision made here.
>
> **Prior art: not yet checked.** GTCEu's Lathe is a flat instant-recipe machine, not a
> simulation — confirmed by memory of reading it for the fitting system, not reconfirmed this
> session. TFC and Create were not checked for a turning/drilling/grinding mechanic before
> writing this. Per `CLAUDE.md`'s prior-art rule, **ask before starting the removal-family
> machines** (§3) — the deformation-family ones (§2) are extending an existing, already-original
> mechanic, so there's nothing to check prior art against.

---

## 0. The one new idea underneath everything here

Every existing process in the mod (hammer deformation, casting, quenching) either **reshapes**
material or **changes its state**. Nothing yet **removes** it. Turning, drilling, and grinding
are all the same new primitive wearing three costumes: material is subtracted rather than moved,
and hardness gates whether the subtraction can happen at all. That primitive is the only genuinely
new mechanic in this whole document — everything else below is an extension of something already
running.

This also means the removal family gets the mod's actual differentiator for free, the same way
beat 2's crucible did: **a lathe, drill press, or grinder doesn't know when it's done.** It keeps
cutting past the target, and continued operation ruins an already-good part — thins a shaft to
nothing, drills through the far wall, grinds a hardened edge soft again. Nobody has to invent an
overrun consequence for these three; the existing philosophy produces one automatically, which is
itself a small piece of evidence the mechanic belongs here.

---

## 1. Two families, not three unrelated machines

| Family | Members | Model |
| --- | --- | --- |
| **Deformation** (extend existing) | Wire Drawer, Rolling Mill, Mechanical Press | `Fu` vs `hardness`, no mass lost — the hammer's own model, driven continuously instead of by discrete blows |
| **Removal** (new) | Drill Press, Lathe, Grinding Wheel | `Fu` vs `hardness`, mass *is* lost, gated by tool hardness exceeding workpiece hardness |

Getting this split right matters for correctness, not just organization: **wire drawing is not
material removal.** Pulling stock through a die reduces cross-section by plastic flow — the same
family as rolling a plate thinner — and produces no swarf. Calling it a removal process would be
inventing physics that isn't true. Same for rolling and pressing. Cutting threads is the one
genuinely ambiguous case (see §2.3) and is called out explicitly rather than silently picked.

---

## 2. Deformation-family machines — extend `DeformationTable`, no new engine

All three below are "the hammer's model, minus the hammer." Confirm the exact shape of
`DeformationTable` and its Fu-accumulator item component against the current code before writing
JSON — this section describes required behavior, not a verified schema.

### 2.1 Wire Drawer

- **Input:** a rod (see §4.4 — rod itself needs a source, this machine can't be first).
- **Operation:** continuous draw, `Fu` accumulated per tick from network RPM rather than per
  blow, same `Fu ÷ hardness` gate hammering already uses. No swarf.
- **Output tiers:** wire, then fine wire — a second draw pass on wire, matching the existing
  plate → foil chain shape (each pass costs less than the one before, same reason the slice
  notes give for 14/9/20 Fu on copper).
- **Discrete choice:** draw speed, fast/slow — fast risks snapping the wire (misuse, same
  "force that can't go into the work goes into the machine," except here what absorbs it is the
  wire itself, which just fails rather than degrading — a dropped attempt, not a wear tick).
- **Milestone dependency:** fine copper wire from this machine is what Slice 4 (Electricity)
  actually needs — this is the roadmap's stated Slice 3 milestone.

### 2.2 Rolling Mill

- **Operation:** identical shape to the Mechanical Hammer's plate-making, but continuous force
  from a driven roller pair instead of discrete blows. If the hammer's Fu-per-blow model is
  `blow force ÷ hardness`, the mill's is `torque-derived force per tick ÷ hardness` — same
  arithmetic, different delivery.
- **Output:** rod (a genuine new intermediate other machines need — see §4.4), and thinner
  sheet variants if a use for them turns up.
- **Why it exists separately from the Hammer at all:** capital-vs-attention again — a mill run
  off the network works unattended once geared correctly, where the hammer needs either
  automation (Clutch) or a hand on the crank. §5's "two correct approaches" test, not a second
  way to make the same plate for no reason — confirm there's a real throughput or unattended-ness
  difference before building both, or this fails that test.

### 2.3 Mechanical Press

- **Operation:** one large stamping stroke rather than many light blows — same Fu/hardness gate,
  different force curve (high force, single application, like the strong crank vs. gentle crank
  distinction already established for the hand crank).
- **Threaded rod — the one genuinely open modeling choice:** real threading has two industrial
  methods that are *actually different processes*, not two names for one thing:
  - **Cut threads** (lathe, single-point tool, removes material) — belongs in §3, not here.
  - **Roll threads** (die pressed against a rotating blank, plastic deformation, no material
    lost, historically stronger threads because the grain isn't cut) — belongs here, on the
    Press.
  
  **Recommendation: ship rolled threads on the Press first.** It needs no new engine (pure
  deformation-family), and it gives threaded rod a real reason to want *this* machine rather than
  the Lathe. Cut threading on the Lathe can be a second, harder-to-reach route later, which
  would then be a genuine capital/precision tradeoff (§5) rather than a duplicate — flag as
  **[OPEN]**, don't build both in the same pass.

### 2.4 Steel Cable

No new mechanic. Multiple wires (§2.1's output) twisted together — a crafting-grid recipe, same
standing as the Hand Hammer's bootstrap recipes. Not worth a machine.

---

## 3. Removal-family machines — new `RemovalTable`, one new primitive

### 3.1 The shared primitive

- **New datapack table, `removal/` — shaped like the other four** (`SimpleJsonResourceReloadListener`
  over a record with `Codec`/`StreamCodec`, no vanilla `RecipeType`, per the settled precedent in
  `CLAUDE.md` §3). Fields needed: input item, target output item, total `Fu` of removal required,
  and the workpiece's own hardness (however `DeformationTable` already sources that today —
  don't invent a second hardness lookup).
- **The hard gate:** the cutting tool's own hardness rating must exceed the workpiece's hardness,
  or the operation accumulates zero `Fu` — the identical shape to "3 St under hardness 15 lands
  nothing," just on a tool property instead of a blow property. This is what makes grinding a
  real second answer rather than a strictly-better one: it exists specifically for the hardness
  band no tool bit clears.
- **Progress is stored the same way deformation progress already is** — an accumulated-`Fu`
  component on the item, read by Calipers, **not** `ItemHeat`'s stamp-and-tick pattern. That
  pattern exists for state that changes with elapsed time (cooling); removal only changes when
  work is actually applied, exactly like the hammer's existing accumulator. Using the wrong one
  of these two patterns here would be a real correctness bug, not a style choice — said twice
  because it's the one place this document could get copied wrong.
- **Waste:** removal produces recoverable scrap/swarf, feeding back into smelting — real
  machining scrap recycling, and it satisfies §7's "failed batches must consume their inputs" in
  the case where a cut goes wrong (the removed mass is real material, not deleted).
- **Overrun, once per machine below:** the target-diameter/target-depth is exactly the switch
  the existing measurement/control/actuation split was already built for. Continuing past it is
  not a special case to code — it's just what happens when nobody stops the machine, which is
  the whole point.

### 3.2 Drill Press — build this one first

Cheapest of the three: axial feed only, no rotating-workpiece bookkeeping.

- Bit rotates (driven by the network), workpiece is stationary, `Fu` accumulates toward a hole
  depth threshold. Same `Fu`/hardness gate as §3.1.
- **Discrete feed choice:** slow/fast, trading speed for bit-snap risk — fast feed against a
  workpiece near the bit's hardness ceiling can bind and snap the bit (misuse → wear, absorbed
  by the bit, not the machine body).
- **Overrun here is mild, deliberately:** drilling through the far side just finishes the hole.
  The real failure mode is a bound, stalled bit spiking network `Su` — reuses the existing
  overstress read (zero speed network-wide) with no new engine work.
- **Piston tie-in (§4.5):** a Piston can be the automatic axial feed actuator here — Switchable,
  driven off a controller — giving the same capital-vs-attention split the mill gets over the
  hammer: hand-feed with right-click, or wire a piston to a timer/sensor.

### 3.3 Lathe

- Stock chucked and spun by the network; a mounted cutting tool removes material toward a target
  diameter. Same gate, now continuous over the whole cut rather than a single depth.
- **Discrete cut choice:** light/heavy pass — heavy is faster but coarser (worse achievable
  precision, tying into §8's apparatus-property language rather than inventing a new stat).
- **This is Calipers' real second job.** It already exists as a carried instrument reading a
  figure by right-click (§8, "some readings are free, some cost an action"); reading a
  workpiece's live accumulated-removal `Fu` as a diameter is the same interaction, new target.
  Feed that reading through a comparator to a Clutch (both already built) and the lathe
  auto-stops at target — or don't, and watch it by eye. Nothing new needed on the control side.
- **Tool bit wears** — `machine/Wearing`'s second implementer (the first is the Hammer). This is
  the exact shape TODO §"Only the hammer wears" already asked for: "a second wearing machine
  costs one `implements` and nothing at the instrument." The bit is a physical, replaceable
  component (§"upgrades are physical components"), not a machine-wide condition figure.

### 3.4 Grinding Wheel

- Same removal primitive, but the wheel's effective hardness ceiling is set high on purpose —
  above any lathe/drill bit's rating — so it's the only route that clears hardened tool steel.
  This is what makes it non-optional rather than a finishing upgrade: a hardened blank is a hard
  gate for the Lathe and not for this.
- **Discrete grit choice:** coarse/fine — coarse removes material faster, fine gives better
  achievable precision and a better surface. Both wear the wheel; don't model grit as a
  continuous number.
- **The wheel is `Wearing`'s third implementer** and is consumed by use, same as the abrasive
  reality it's modeling — not a machine condition that degrades from misuse only, because grit
  loss under correct use is real and load-bearing here, unlike the Hammer's "wear only from
  misuse" rule. State this divergence explicitly at the class site when it's built, since it's a
  real exception to a rule stated elsewhere as settled.
- **Thermal tie-in, the nice accident:** grinding generates real interface heat. Overrun grinding
  a part that was deliberately hardened (Slice 2's tempering) can heat it back past its own
  tempering window and soften it — the mod's central differentiator landing a third time, for
  free, in a system that has nothing to do with a crucible. Model as a small `ThermalBody` bump
  proportional to grinding `Fu` applied per tick; exact figures are a tuning question, not a
  design one.

---

## 4. Transmission and support parts

### 4.1 Bearings

Already named in `TODO.md` as "the obvious first physical upgrade" — closes this out rather than
inventing anything. A bushed/greased Shaft variant with a lower `Su` loss constant. Fits "upgrades
are physical components you could point at" exactly: point at the bearing, not a percentage.

### 4.2 Pulley + belt

The one genuine new engine piece outside the removal primitive: a second transmission type
alongside Shaft/Cog, implementing the same `RotationNode` contract so the existing propagator
doesn't need to know it exists as a special case.

- **The reason to build this at all, not just a cosmetic long shaft:** a belt can **slip**. A
  Shaft's overstress behavior is already all-or-nothing (the whole network reads zero speed at
  once). A belt gives mechanical transmission a *second*, genuinely different failure character —
  it sheds excess load by slipping rather than stalling the network, at the cost of wearing the
  belt itself (`Wearing`'s fourth implementer) — the same "distinct failure character per cutoff
  type" the hard design rules already require of breakers/clutches/thermal mass, just arriving in
  the mechanical domain for the first time.
- Speed ratio between two different-diameter pulleys, same shape as gear ratio through a Gearbox
  — reuses the existing ratio concept, doesn't invent a second one.
- **[OPEN]:** exact slip threshold and belt wear rate are a tuning pass once something is
  running, same as everywhere else in this document.

### 4.3 Valve

No new mechanic — `Switchable` and `Blown` already exist (built for the Bellows/Firebox/Pressure
Vessel chain). A Valve is a block that gates a `Blown` connection and answers `Switchable`,
nothing else. Ship it whenever a fluid/air line actually needs a manual or controller-driven cutoff.

### 4.4 Rod — the missing intermediate

Not in the roadmap's own machine list, but both the Wire Drawer (§2.1) and Lathe (§3.3) need a
rod-shaped blank as input and nothing currently makes one. **Source it from the Rolling Mill
(§2.2)** rather than inventing a fourth machine for it — one more output on a machine already
being built, not new scope.

### 4.5 Piston

Generalizes a mechanism that already exists rather than adding a new one: the Bellows' own
crank-linkage (its "long throw" gear-advantage clamp, per `TODO.md` §4c) *is* a piston. Pulling
it out into its own `Switchable`, network-driven linear actuator gives it a second real use
immediately — the Drill Press's automatic feed (§3.2) — which is exactly the "capital-vs-attention"
pairing §5 asks every new mechanic to produce somewhere.

---

## 5. Build order

Ordered by how much new engine each needs, cheapest first — not by roadmap milestone importance.

1. **Bearings** — a Shaft variant, no new table, no new gate.
2. **Valve** — reuses `Switchable`/`Blown` wholesale.
3. **Wire Drawer, Rolling Mill, Mechanical Press** — one shared extension of `DeformationTable`
   plus continuous-force delivery instead of discrete blows. Build together; they're one change,
   not three.
4. **Rod** as a Rolling Mill output, unblocking the Wire Drawer and (later) the Lathe.
5. **Steel Cable** — pure crafting recipe once wire exists.
6. **Piston** — pull the Bellows' existing linkage into its own actuator.
7. **Drill Press** — the removal primitive's cheapest, first real test. Ask about prior art
   first (see the status banner) before starting this one specifically.
8. **Lathe** — same primitive, adds the live-diameter Calipers/Clutch control loop.
9. **Grinding Wheel** — same primitive again, adds the thermal tie-in and the "wears under
   correct use" exception.
10. **Pulley + belt** — the one item here that's real new engine work (a second `RotationNode`
    implementor with slip behavior) rather than an extension; scheduled last on purpose.

---

## 6a. Decisions taken while building

- **New `Operation.ROLL`**: the Rolling Mill's ingot-to-rod pass is not `BLOW` -- it would collide
  with the existing ingot-to-plate `Deformation` entry the moment both existed for the same input
  item, since `DeformationTable#find` matches on `(operation, input)` and returns the first hit.
  Rolling is also a real, distinct physical action (continuous compression between a driven roller
  pair, producing an elongated cross-section) rather than a flat blow, so a third `Operation` value
  is the honest fix, not a workaround -- the same reasoning that justified `DRAW` in the first
  place (see that enum's own class doc).
- **Wire Drawer / Rolling Mill / Mechanical Press are `RotationNode`s directly**, not
  `Reciprocating` machines behind a `CrankLinkageBlockEntity`. They sit on the network exactly like
  a Gearbox -- a plain block coupling on every face, drawing a flat Su load -- because "continuous
  force from network RPM" means the machine itself is the thing riding the shaft, not a stroke
  counter feeding it. Every tick the network is turning above the stopped threshold, each calls the
  same `Deforming.apply` the Hammer and hand tools already share. This reuses 100% of the existing
  overshoot/cascade/hot-working logic; nothing new was written in `process/`.
- **The Press does not literally count strokes.** §2.3 describes "one large stamping stroke rather
  than many light blows"; building a second stroke-accumulator to get that flavour literally would
  duplicate `CrankLinkageBlockEntity`'s own accumulator for no mechanical difference. Instead the
  Press applies a very high `St` every tick (`FTuning.MECHANICAL_PRESS_ST`), which against a
  plate-sized `work` figure clears a whole deformation stage in one or two ticks once driven --
  the same gameplay outcome (fast, coarse, unattended) without a second engine. Flagged here in
  case a later pass wants literal single-stroke-per-revolution timing for a different reason (e.g.
  a distinct sound/animation beat); nothing about the data model would need to change.
- **Wire Drawer draw speed** is a genuine hard-gate/risk pair rather than a pure throughput knob:
  slow (`WIRE_DRAWER_ST_SLOW = 6`) is always safe but never clears steel's hardness of 15; fast
  (`= 18`) clears it but risks an outright snap each tick (`WIRE_SNAP_CHANCE`). Copper (hardness 1)
  works at either setting. This means steel wire is only reachable by accepting the risk, not by
  waiting it out with a safe setting -- consistent with §7's "the recipe is not locked, the process
  is difficult" rather than a gate that would need a tier check.
- **Rod and wire are per-material (copper and steel), not one generic item.** The existing roster
  already treats copper and steel as separate materials with separate deformation chains (plate,
  foil); giving rod/wire the same split rather than inventing a shared "rod" item keeps one
  material system instead of two.
- **`Removal`/`RemovalTable`/`Removing` mirror `Deformation`/`DeformationTable`/`Deforming`
  exactly**, minus a temperature band (removal has no hot-working requirement named anywhere in
  this spec) and minus an `Operation` field on the table itself -- turning, drilling and grinding
  are one physical primitive competing with nothing else for the same input item, unlike a hammer's
  blow versus a die's draw, so `RemovalTable#find` matches on the input alone. `Operation.REMOVE`
  still exists, purely so `FDataComponents.WORK_OPERATION` can tell removal progress apart from
  deformation progress on the same item -- the same "switching operation forfeits pending work"
  rule `Deforming` already enforces between `BLOW`/`DRAW`/`ROLL`.
- **The gate moved up a level, on purpose.** `Deformation`'s hardness is both floor and divisor
  (blow force ÷ hardness). `Removal` keeps hardness as the divisor for its own arithmetic
  (`cutFrom`, drive force ÷ hardness) but the floor is now a *second*, separate hardness: the
  tool's own rating, compared directly against the workpiece's (`canBeCutBy`). This is the spec's
  own language taken literally -- "the cutting tool's own hardness rating must exceed the
  workpiece's" is a tool-vs-material comparison, not a force-vs-material one.
- **The Drill Press's bit is a fixed constant (`FTuning.DRILL_BIT_HARDNESS`), not a physical,
  swappable item, this pass.** The spec's own §3.3 explicitly names the Lathe's tool bit as
  `Wearing`'s *second* implementer (the Hammer is first) -- which only holds if the Drill Press's
  bit isn't a third, competing implementation. A real bit component (insertable, wearable,
  upgradeable) is real future work and a natural companion to the Lathe's, not scope this stage
  needed to claim first.
- **Drill Press feed is discrete and externally triggered, not continuous like the deformation
  family.** The bit spins continuously off the network (drag/load like any `RotationNode`), but
  cutting only advances on a `push()` -- one from an empty-hand right click (hand feed), or one
  from a `Piston` wired to a controller (automatic feed). This is what actually uses the Piston
  built the previous stage, and reads §3.2's "hand-feed with right-click, or wire a piston to a
  timer/sensor" literally rather than as flavor text.
- **A snapped bit (fast feed, `BIT_SNAP_CHANCE`) costs the push and nothing else** -- no item is
  destroyed, no condition figure moves, because there is no physical bit item yet to absorb it
  (see above). Recorded as a placeholder rather than silently equivalent to the eventual Lathe/
  Grinding Wheel behavior.
- **Swarf is one generic item (`feedback:metal_swarf`), not per-material**, dropped into the world
  with a flat chance per landed cut and smelted back into an `iron_nugget`'s worth of metal via a
  plain vanilla `minecraft:smelting` recipe (which Feedback's own vessels already honor through
  `VanillaFallback`). A precise per-material swarf roster is real content work for document 3, not
  a blocker for proving the removal primitive works.
- **The Lathe implements `SensorFitting` directly, unsided, rather than becoming a `Fittable`
  holder with a mounted sensor.** `SidedFitting` is sealed to `{SensorFitting, AdapterFitting}`,
  but `SensorFitting` itself is `non-sealed`, so any class may implement it outright -- the same
  move `ClutchBlockEntity`/`DamperBlockEntity` already make for plain `DataNode` (a whole block
  answering the contract directly rather than hosting a fitting that answers it). This is genuinely
  "nothing new needed on the control side": `ControllerBlockEntity`/`DebugControllerBlockEntity`
  already resolve any `DataNode` reference and cast to `SensorFitting` if it fits, so a Lathe reads
  through `Read Sensor` -> comparator -> Clutch with zero changes to `control/`. `getPickItem()`
  returns `ItemStack.EMPTY` since the Lathe is never a removable fitting -- that method only makes
  sense for the `Fittable`/sided-attachment path this class deliberately bypasses.
- **The Lathe's tool bit is a real item with its own `FDataComponents.TOOL_CONDITION`** (a new,
  fourth data component), not a block-level float -- the spec's own §3.3 says explicitly "not a
  machine-wide condition figure." Wear follows the Hammer's exact rule (misuse only: a cut the
  bit's own hardness cannot clear wears it; a landed cut never does), just relocated from a field
  on the block entity to a component on the stack, and the bit's *effective* hardness scales with
  its own condition the same way the Hammer's `getStrength()` scales with its condition.
- **No bit-condition tooltip was added.** Calipers reads a `Wearing` block's condition via
  `CalipersItem#onItemUseFirst`; the Lathe's bit deliberately isn't `Wearing` (see above), and a
  generic "read `TOOL_CONDITION` off any held item" display is real but separate scope, not
  required to prove the primitive or the wear rule both work. Left as a gap rather than silently
  built partway.
- **Grinding Wheel condition has no floor above zero** (`GRINDING_WHEEL_CONDITION_FLOOR = 0`,
  against the Hammer's 0.5 and the Lathe bit's 0.4), and wear fires on every *landed* grind, not
  only on a failed one -- the stated exception the spec calls for, recorded at the class site per
  its own instruction. At condition zero the wheel's effective hardness (`condition x rating`) is
  also zero, so it simply stops clearing anything -- a real hard gate (§7), not the block breaking
  or disappearing, keeping faith with the mod's general "degrades, never breaks" rule even for a
  genuinely consumable part.
- **The thermal bump recomputes `driveForce.value()` as a stand-in for "Fu removed this tick"**
  rather than having `Removing.Cut` return the delivered amount. `Removal#cutFrom` already computes
  exactly this number internally and then discards it; recomputing it at the call site (same
  formula, same inputs) was simpler than widening `Cut`'s shape for every caller to gain a field
  only the Grinding Wheel uses. Revisit if a second caller ever wants the same figure.
- **`hardened_steel` (hardness 25) is the removal-table content proving the wheel's ceiling is
  real**: both the Drill Press (`DRILL_BIT_HARDNESS = 18`) and the Lathe (`LATHE_BIT_HARDNESS =
  20`) fail `canBeCutBy` against it; only the wheel (`GRINDING_WHEEL_HARDNESS = 30`) clears it.
- **A belt pair is deliberately never merged into one `RotationNetwork`.** A rigid Shaft run has
  one speed by construction, and overstress reads as zero across the whole thing at once -- exactly
  the all-or-nothing failure the spec asks a belt to *not* have. So `PulleyBlockEntity` couples
  locally to its own shaft exactly like a Cog (an ordinary `RotationPropagator` member, no special
  case), and the cross-network belt link is handled entirely inside the block entity's own
  `tickServer`: once a tick, whichever side's local network currently has zero capacity of its own
  reads the *other* side's live network speed, applies the diameter ratio, caps it against
  `FTuning.BELT_CAPACITY_SU` (slipping rather than stalling past that point), and pushes the result
  onto its own network with a plain `RotationNetwork#setTargetRpm` -- no rebuild, no merge, and
  `RotationPropagator` never has to know belts exist, per the spec's own instruction.
- **Accepted limitation, stated rather than solved: a belt between two already-independently-driven
  segments does nothing.** The rule above ("only the zero-capacity side ever writes") means that if
  both linked networks already have their own source, neither ever overwrites the other -- a safe,
  graceful no-op rather than the two sides fighting over whose target wins every tick. The common
  case (a driven segment feeding an otherwise-unpowered one) works correctly; synchronizing two
  already-powered lines through a belt is real future work the spec's own `[OPEN]` items didn't ask
  for and this pass doesn't invent an answer to.
- **Belt linking reuses the data connector's exact shape**, not a new mechanism: `BeltLinkManager`
  is `control.data.DataLinkManager` with `DataNode` swapped for `PulleyBlockEntity` and no in-world
  cable, and `BeltItem` is `DataConnectorItem` with the same swap. Two pulleys know each other's
  `BlockPos`; nothing in the world is a belt any more than anything in the world is a data wire.
- **Pulley wear (the fourth `Wearing` implementer) is proportional to Su slipped, not to whether a
  cut landed** -- there is no "misuse" for a belt to speak of, only load. This is the same
  "real wear under correct use" the Grinding Wheel already established, arriving a second time in
  the mechanical domain rather than the removal family.
- **Steel Cable uses steel wire, not copper wire**, despite copper wire being the one named
  milestone dependency (Slice 4/Electricity). The item is literally named "Steel Cable"; a
  load-bearing mechanical cable is steel's job, not copper's, and nothing in the spec says
  otherwise. This needed `steel_rod`/`steel_wire` deformation entries (hot-working, same 900-1100
  Tu band as `steel_plate`) that the spec's own roster didn't ask for by name but which Rod's
  build-order item implies once "both the Wire Drawer and the Lathe need rod" is taken seriously
  for steel as well as copper.

- **Bearings**: `ShaftBlock` now takes `Drag`/`Inertia` in its constructor (the same shape
  `CogBlock` already uses for its two sizes) rather than `ShaftBlockEntity` hardcoding
  `FTuning.SHAFT_DRAG_SU_PER_RPM`. The Bearing is a second `ShaftBlock` registration at a lower
  drag, sharing `ShaftBlockEntity`'s block entity type the same way the two cog sizes share one.
  Inertia is untouched -- a bearing cuts friction, not mass.
- **Valve**: engaged state lives on the blockstate (`OPEN`, like `DamperBlock`), not a BE field,
  so it needs no hand-rolled sync. `Blown#addAir` forwards to whatever `Blown` sits on the
  block's `FACING` face when open; closed, the air is simply lost, matching `Blown#addAir`'s own
  "anything over the buffer is lost to the room" rule one block earlier.
- **Pre-existing bug fixed in passing**: `Deformation.STREAM_CODEC` tried to `StreamCodec.composite`
  seven fields, past the six `StreamCodec` actually supports (`Function6`). This predates this
  spec -- the record has carried `min_temperature`/`max_temperature` plus `operation` since before
  this pass -- and was never hit because nothing had forced a clean rebuild since `operation` was
  added. Fixed by folding the temperature band into one `Tu[2]` slot rather than forking a
  seven-argument composite by hand. Recorded here because build-breaking bugs found while doing
  unrelated work are still worth a line, not a silent fix.

## 6. Open questions carried forward

- **[OPEN]** Threaded rod: rolled (Press, §2.3) shipped first; cut threading (Lathe) as a second,
  harder route — don't build both without confirming there's a real tradeoff, not a duplicate.
- **[OPEN]** Pulley belt-slip threshold and belt wear rate — tuning, not design.
- **[OPEN]** Grinding Wheel's thermal-bump-per-Fu figure — tuning, not design.
- **[OPEN]** Gear Cutter, named in the roadmap's machine list, is deliberately **not** scoped
  above. Cogs already exist via a plain crafting recipe and nothing here demands a precision
  route to them yet — revisit only if a concrete use for a *better* cog (less backlash, less
  `Su` loss) shows up, rather than building a machine to duplicate an existing item.
- **[OPEN]** Prior art for the removal primitive itself (§3.1) — not checked this session, and
  it's the one piece of this document that's a genuinely new mechanic rather than an extension.
