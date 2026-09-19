# Feedback — TODO

**Done items are deleted, not checked off.** When something closes, remove its `- [ ]` line (and
its own sub-bullets) from this file entirely rather than marking it `- [x]`. This file is a
worklist, not a changelog — history lives in git and in the design docs' own "corrected/rejected"
notes, not here.

Working checklist. Source of truth for *what's next*; `feedback_philosophy.md` stays source of truth for *why*.

**Controllers have their own spec now: `feedback_controller_spec.md`.** Tier 1 (Punch Card) is
built — the program-graph model (`control/program/`), the Controller and Programmer blocks
(`control/controller/`, `control/programmer/`), and the `Switchable`/`DataNode` widening it
needed on `ClutchBlockEntity`. Tiers 2-4 of the medium progression are still just the document.
Replaces the old `controller_spec.txt`, which is deleted.

**First real playtest found three things wrong with that first pass**, all fixed the same day:
the Greater/Less palette buttons silently no-op'd (a typeKey mismatch between
`ProgrammerScreen`'s `AddNode` and `ProgrammerBlockEntity`'s switch); linking a sensor needed an
undiscoverable second click after placing the card (placing now auto-consumes a pending Data
Connector selection, §3); and the fixed-grid card layout the spec cut on purpose turned out to
be a real usability regression against the mockup, so free drag-and-drop is built after all
(§8). Number ports also gained `Quantity` typing (§2.1) so a comparator can't silently mix Tu
against Su — ports are colour-coded per quantity in the GUI now, not just Number/Boolean.

---

## Pick-up list — costed, start cold

Candidates weighed on 2026-09-12 and deliberately not taken that night. Each one is scoped
here so a fresh session can start on it without re-deriving the scope. Ordered by value, not
by size.

- [ ] **Decided: air gets a unit, `mB`.** Same volume unit fluids already use, on the grounds a
  gas system is coming and volume is how the mod already measures a gas-shaped quantity (1 mB =
  1 litre, §17). Closes the old `[OPEN]` at `BellowsBlockEntity.getStrength`, which was returning
  an air figure through `St` (a force-linkage unit) one-to-one, invisible while both were `float`.
  Not started: `Su`/`St` at the bellows/firebox/pressure-vessel call sites need retyping to `mB`,
  and `core/unit/Units`'s checklist followed for the new unit.

- [ ] **Decided: per-dimension ambient temperature.** `FTuning.AMBIENT_TU` is one global constant
  (20 Tu) today; `Heat.equilibrium`'s leak term already takes an ambient figure, so this is a
  lookup-by-dimension away, not a new mechanic. Real effect confirmed by the numbers: it narrows
  the gap between a vessel's equilibrium and its fire's own ceiling and gets there faster — it
  does **not** let a fire exceed its own flame temperature, ambient's share of equilibrium is only
  ~4% at current conductance/leak ratios. Genuine second answer to "get closer to the ceiling"
  (insulate at home vs. site the foundry in the Nether) rather than a strictly-better move, so it
  passes §5. Not started.

- [ ] **Decided: graphite is an ore, not (only) a thermal process.** `thermal_process/graphite.json`
  already exists (2200–2600 Tu, coal in) but nothing built reaches that — blaze rod fully blown
  caps at 2100 Tu, and real graphitization needs ~2500–3000°C, well past any flame. Resolution:
  natural graphite is a mined material (real-world graphite deposits are mined, not synthesized),
  Slice 2-reachable, used as a refractory upgrade (crucible lining, kiln/furnace ceiling). The
  existing 2200+ Tu process becomes *synthetic* graphite, reassigned to Slice 10's arc furnace —
  an electric arc isn't a flame and isn't capped by any fuel's temperature, which is what actually
  clears the gate honestly rather than by lowering the number. Two viable routes to one material
  (§5), and synthetic graphite being the purer feedstock for Slice 10's electrodes / Slice 12's
  moderator is real chemistry, not invented. **Not started — no worldgen exists yet at all,** so
  this is a booked decision, not a scoped task: needs an ore block, a deposit, and `feedback_mechanics.md`
  §4-style writeup before any code. Check GTCEu for graphite/electrode prior art first (rule at
  the top of this file) — check the licence, clone outside the repo, read design not code.

- [ ] **Real models and textures**, for every block in both beats. Art, and the user's call —
  not something to start unprompted.

- [ ] **Real models and textures**, for every block in both beats. Art, and the user's call —
  not something to start unprompted.

**No longer true:** beat 2 is committed (`96cea49`), and the GPL relicence with it (`c6f0a8f`).
The tree is clean, so a wide refactor is now safe to start.

---

## 0. Housekeeping

---

## 1. Decide first: build or spec

Slice 1 is fully drafted and every open decision in it is closed. Two ways forward, and this choice gates everything below.

- **Build slice 1.** Philosophy §20 argues emergent behavior can't be validated on paper. Slice was scoped small enough to implement (12 items, 7 machines, 2 process variables). Finds the problems no document will.
- **Write document 2 first.** Safer, but doc 2 written without a running slice means inventing numbers with nothing to test them against.

Recommendation: build. Write doc 2 from what the build forces you to decide.

**Decided: build.** Scaffold is done; next is beat 1 — Hand Crank, Water Wheel, Shaft, Crank Linkage, Mechanical Hammer, and the plate -> foil -> scrap overrun chain.

---

## 2. Project scaffold

Platform decided: **NeoForge, Minecraft 1.21.1.**

### Library decisions

Dev environment loads **JEI**, **PonderLib**, **Jade** and **Flywheel** out of `run/mods`, synced by the `syncDevMods` Gradle task. **Flywheel is now a shipped dependency** — `compileOnlyApi` on the API, `jarJar(runtimeOnly)` on the implementation, pinned to `1.0.4` with the range `[1.0.0,2.0)`. The rest are still dev-only.

Also decided in passing: **`src/generated/resources` is committed.** Datagen output is reviewable, and a diff on it is the cheapest way to see what a registry change actually did.

---

## 3. Document 2 — mechanics

File exists now: `feedback_mechanics.md`. Only section written: Experience (XP) — speculative, ahead of any code, per its own status note. Everything below is still not started. Philosophy defers to it **by name** in these places; each is a debt.

**Units & arithmetic**
- [ ] Exact ranges and arithmetic for every unit (§17)
- [ ] `Eu` — how voltage and current behave across a wire (§17)
- [ ] `Su` load figures; linkage `St` pairs per machine (§17)

**Thermal** — the first two are now *implemented*, so doc 2 has something to describe rather than invent
- [ ] Write both of the above up in doc 2, from the code rather than from scratch
- [ ] Vanilla vessel bands — smoker ceiling, Crude Blast Furnace floor, plain furnace span (§15)
**Instruments & control**
- [ ] The six apparatus properties as numbers — range, resolution, accuracy, control, response, stability (§8)
- [ ] Drift and recalibration mechanism (§8, §9)
- [ ] **[OPEN]** Hysteresis needs the controller to read its own output state back. Cleanest: actuator state is just another readable source. Confirm — without it a deadband can't be expressed and relay flapping stops being the player's fault (§13)
- [ ] Data links: 16-block range, client-drawn cable, cable item cost, no path check (§13)

**Process**
- [ ] Per-energy sensor/actuator pairings, full matrix (§10)
- [ ] Overrun band widths per machine and per material (§6)
- [ ] Noise model — irreducible floor, learnable-in-aggregate, distribution narrows but never collapses (§8)

---

## 3b. Beat 1 — in progress

Built and compiling: rotation engine (`RotationNode` / `RotationNetwork` / `RotationPropagator`), Shaft, Hand Crank, Water Wheel, and the three copper overrun items. Placeholder art throughout (vanilla textures).

### Information layer — Jade, JEI, and the debug helmet

Governed by §8's *what you need is free, what you have is a cost*. Write these together; they are one design, not three features.

- [ ] **[OPEN] Calipers on an untouched ingot show nothing**, because an unworked item carries no `WORK_REQUIRED`. The client now has the deformation table (JEI sync), so they *could* read `0 / 14 Fu`. Low value — that figure is a requirement and JEI already gives it away for free
- [ ] Real textures
### Rendering — decisions taken while building

- **`ENTITYBLOCK_ANIMATED`, not `RenderShape.INVISIBLE`.** Both keep the block out of the chunk mesh, which is the thing that matters: Flywheel's `skipVanillaRender` suppresses only the *block entity renderer*, so without this the block would be drawn twice, once still and once spinning. But vanilla also gates block-breaking particles on `INVISIBLE` specifically, and a shaft that shatters silently loses a sense for nothing. Create uses `ENTITYBLOCK_ANIMATED` throughout for the same reason.
- **A fallback renderer is not optional.** Leaving the chunk mesh means the block entity is the *only* thing drawing a shaft, so with Flywheel's backend disabled every shaft in the world would simply vanish. `RotatingRenderer` guards on `VisualizationManager.supportsVisualization` exactly as Create's `KineticBlockEntityRenderer` does.
- **CPU transforms, not a rotation shader.** Create spins on the GPU with its own `rotating.vert`, which is an *asset* and All Rights Reserved even though Create's code is MIT. Flywheel's built-in `TRANSFORMED` instance type with a per-frame transform is still one draw call for the whole shaft run; a GPU-side clock is a later optimisation, not the entry price.
- **`0.3f` degrees per tick per RPM is now `RotationNode.DEGREES_PER_TICK_PER_RPM`,** and both the visual and the fallback extrapolate with it. The renderer runs per frame and the simulation per tick, so the two have to agree or a slow shaft lurches around the angle the simulation actually holds.
- **The Clutch gained a client ticker** it never had — it does all its work on right click and needs no server tick, but without a client tick its angle never advanced and it sat frozen in the middle of a run that was plainly turning, which reads as disengaged when it is not.
- **Nothing visual was verified.** `./gradlew build` passes and a dedicated server boots clean with no client-only class loaded, which is all that can be checked without a window. Still unconfirmed by eye: that the four blocks actually turn, that none is drawn twice, that none has vanished, that the speed on screen matches the RPM Jade reports, and that the fallback draws correctly with Flywheel's backend set to `OFF`.
- **The whole block spins, base and all.** `Models.block(state)` takes the block's own model, which for the Hand Crank includes its mounting plate and for the Clutch its housing. Correct art would split the turning part from the fixed one, which means partial models and real textures. Placeholder art, placeholder motion.

### Information layer — decisions taken while building

- **The requirement/state split is enforced in one file**, `client/Readout.java`. §8's rule is a rule about *wording*, and wording copied between a tooltip and a HUD drifts until one of them quietly starts printing a figure. Requirements and equipment specs are formatted at their call sites instead, because there is no rule about them to enforce.
- **Strain is on the free side of the line, and that is deliberate.** "Straining" / "Overloaded" names no figure — it is the same adjective the smoke and creak already give at the sources, and without it a stalled factory is indistinguishable from a broken mod. There is no puzzle in a problem you cannot locate.
- **Jade reads live figures through its own server data**, not through the block's render sync. The `Su` a `RotationNode` carries is a snapshot from the last recalculation, and target speed and inertia are never synced at all. Adjectives from stale figures would usually be right; the helmet could not be, and §8 says an instrument may become wrong but may never overstate its certainty. Consequence: **on a server without Jade, the HUD says nothing rather than guessing.**
- **The deformation table is shipped to the client by our own packet**, on `OnDatapackSyncEvent`, rather than through JEI's hooks. It is a missing-data problem, not a JEI problem — solving it in JEI would mean every future display has to solve it again. JEI is then an optional *reader* of a plain list.
- **JEI's cards are pushed from the runtime, not from `registerRecipes`.** JEI starts from the vanilla recipe sync, which lands in the configuration phase, while our table arrives in the play phase — so `registerRecipes` would see an empty table on a first join, and registering in both places would double every card.
- **The Debug Helmet implements `Equipable` rather than extending `ArmorItem`.** ArmorItem would demand a registered armour material and an armour-layer texture, then render a missing-texture helmet, all to describe an item that gives no protection and should be invisible.
- **Sneak-clicking the Mechanical Hammer now extracts** rather than printing a report. Placing a block against its face while sneaking never worked and still does not; nothing regressed, but it is a behaviour change worth knowing about.
- **Not built, deliberately: any instrument.** There are still no calipers and no thermometer. `Readout.instrumented()` is a boolean standing where a per-quantity, per-resolution question belongs; the call sites are already shaped for the real answer.

### Slice discrepancies — reconciled

`feedback_slice_01.md` has been updated to match what is running. Kept here as a record of what moved and why:

- **Copper work 30 → 14 / 9 / 20 Fu.** 30 made the overshoot lesson impossible to express; the plate→foil step has to cost less than one strong blow or the short throw cannot skip it.
- **"Either crank works on copper" → the strong crank cannot make a plate at all.** Much better lesson, and it makes gearing a product selector rather than a speed setting.
- **"Thin plate"** removed from the chain; it was never in the roster.
- **"About 8 ticks, there is a plate"** dropped. One blow per revolution puts a plate at five blows on the gentle setting, two on the strong one.
- **"Fifteen hammer swings"** → five, since a hand hammer's 3 St against copper's hardness 1 lands 3 Fu a swing.
- **`Minimum force N St`** → `Hardness N` on every process card. One material property doing both jobs (§17).
- **Hand crank drives nothing** is now stated in the slice as an `[OPEN]`, with the flywheel as the interesting resolution, rather than reading as a balance bug.
- **Four lessons added** to the slice's table — all four emerged from building rather than from the draft.

### Decisions taken while building, worth revisiting

- **Propagation is a full rebuild, not Create's incremental update.** Flood fill the run, find the strongest source, walk outward. O(n) per change instead of O(change), which is worse on paper and fine at our scale — and it removes the need for Create's "flicker score", because a rebuild cannot loop. Revisit only if a profiler complains.
- **Inertia is implemented.** Speed belongs to the network, not the block; each node keeps a *ratio*. The network has a target speed and a current speed chasing it, at `(capacity − load) ÷ inertia` RPM per tick — real angular acceleration, not a fudge. Create has no equivalent: its networks snap to speed, which is why its flywheel visibly coasts while the network does not.
- **Shaft loss is implemented, as `Su`.** Each shaft charges bearing friction to the network. Loss is `Su` rather than `RPM` because a rigid shaft turns at one speed along its length — a speed difference between its ends is torsion, not loss, and what friction eats is torque. Because `Su` sums network-wide this is automatically distance-independent: moving a machine nearer the generator saves nothing, since every bearing turns either way. **Sprawl costs, distance doesn't.**
- **All numbers live in `core/FTuning.java`.** Everything is fiction until playtesting, so the point is turnaround: one file to edit, not twelve block entities. Becomes a config when the figures start meaning something.

### Found by building, not by designing

- **Headroom became a real decision.** A network at 98% of capacity (three hammers on a 256 Su wheel) takes **81 seconds** to reach speed, because acceleration is surplus torque over inertia and there is almost no surplus. Two hammers reach speed in two seconds. Nobody installed this — it falls out of the acceleration relationship — and it is §5's test passing: the same capital-vs-attention trade, in a third unrelated place. **Keep it.** Possibly soften the magnitude; do not remove the shape.
- **Shaft loss scales with RPM.** Resolved, and it turned out to fix two bugs at the root. A stopped shaft costs nothing, so an unpowered run is no longer reported as overstressed. And a network now accelerates until surplus torque runs out, so it **finds its own top speed**: a hand crank rated 32 RPM drives 3 shafts at 32, 5 shafts at 24, 10 shafts at 12. Deliberately unlike a hard per-shaft Su cap, which produces the absurdity of a generator being *too good* for its own shafting.
- **Overstress means something narrower now.** Only when a source is present *and* static load exceeds capacity before anything turns. A run with no source is unpowered, not overloaded; a run that merely cannot reach its target speed is not faulty, it is just slow.
- **Accelerating force deliberately ignores friction**, with the terminal speed applied as a clamp instead. Using live surplus is more literal but behaves badly — surplus reaches zero exactly at terminal speed, so a run creeps the last revolution for fifteen seconds.
- **Bearings are the obvious first physical upgrade** — a bushed or greased shaft with lower `Su` cost. Fits "upgrades are physical components" exactly. Not built.
- **Water wheel speed scales with how many sides have flowing water**, so siting it is a decision rather than a placement. Not from the slice doc — an invention, and cheap to remove.

---

## 3c. The Hand Hammer, and the recipe pass — built, unverified

Slice 1's item budget listed a hand hammer from the first draft and nothing ever implemented
it, which left §7's sharpest promise — *manual production stays theoretically possible* —
with nothing behind it in the first ten minutes. It also left a genuine circular dependency:
a copper plate needs a Mechanical Hammer, a Mechanical Hammer needs copper plates.

### Decisions taken while building

- **The grid, not a block in the world.** A block route needed a surface to hit, which is the
  bootstrap problem again, and it would have been a fifth sneak-click handler immediately
  after four were deleted. The grid also earns something the block could not: **shift-click
  runs a whole stack straight through plate into foil into scrap**, which is beat 1's entire
  lesson delivered by the player's own hand before they own a machine. Left sharp on purpose.
- **A vanilla `RecipeType`, and §2's rule survives it.** Everything else in the mod is a
  datapack table specifically so that no *machine* owns a recipe list — a machine that knew
  when it was finished would stop being this mod. A crafting table is vanilla's recipe
  machine and the player is the one doing the work, and a player is allowed to know what they
  are making. The result slot previewing the next blow is the player gaining information,
  which §7 is in favour of; the hammer on the anvil still cannot tell.
- **Steel is out of reach by hand with no rule about hands.** 3 St under hardness 15 lands
  nothing, so the recipe never appears. §7's hard gate arriving as arithmetic.
- **A wasted swing costs nothing.** The Mechanical Hammer wears on force that could not go
  into the work; a hand hammer has no equivalent, because the grid never offers the craft. You
  cannot mis-swing at something you were never able to lift.
- **The noise is an event, not a getter.** GregTech plays its tool sound from inside
  `getCraftingRemainingItem`, which works, but it is a side effect hidden in a getter that both
  logical sides call. `ItemCraftedEvent` fires once, when a craft actually happened, and hands
  over the grid to look at.
- **The level reaches `assemble` by ThreadLocal.** A workpiece computes its temperature from a
  stamp and a tick, so a blow needs a clock, and `assemble` is the one place vanilla does not
  pass one — `CraftingMenu.slotChangedCraftingGrid` holds the level and drops it. NeoForge has
  the identical problem with the crafting player and solves it the identical way
  (`CommonHooks.craftingPlayer`). Worth knowing the workaround is theirs, not an invention.
- **Bootstrap is vanilla-only, by necessity.** The Hand Hammer is 5 cobblestone and 2 sticks.
  Everything downstream can then cost worked copper, which is what the slice always claimed:
  plates are a real intermediate because they go into the machines.
- **The Bimetallic Strip is literally bimetallic** — 2 copper foil, 2 iron nuggets. That it
  consumes *foil* is the nicest accident in the pass: beat 1's first overrun mistake is the
  material beat 2's only sensor is built out of, so the wasted plate was never wasted.
- **Unlocks are three grouped advancements**, not nineteen. Bootstrap on cobblestone, beat 1
  on copper ingot, beat 2 on brick. §8 says a requirement is published data, so there is
  nothing to protect by staging them finely.

---

## 4. Document 3 — content

Downstream of doc 2. The roster: every item, material, machine, fitting, process.

- [ ] Seed from slice 1's 12 items / 7 machines
- [ ] Then slice 2

---

## 4b. Next session — start here

Verified working in game: rotation network with inertia and coasting, shafts that visibly turn (Flywheel, jar-in-jar), Clutch + Timer, Mechanical Hammer with the overrun chain, Calipers, JEI process cards, Jade in adjectives, Debug Helmet.

Cogs, the gearbox, `St` gearing and the hammer's force ceiling are now verified in game too.

Also verified since: wear (battered/spent bands, spent-hammer-stops-steel, over-gear cap), calipers
condition reading, and the whole of beat 2 heat, one steel ingot made start to finish — see §4c.

### Cogs and a gearbox — built and verified in game

Small Cog, Large Cog and Gearbox exist, along with the two rules that make gearing mean something. `./gradlew build` passes and the mod loads on a dedicated server; **nothing is verified by eye yet** — see the unchecked items below.

- [ ] Real models. Both cogs are a placeholder disc on a shaft and the gearbox is a copper box with three shafts through it
- [ ] **[OPEN] Does gearing need an ongoing cost?** Narrowed by building, not closed — see §17. The referral above makes *speed* cost; a machine's working draw is still a flat figure, so gearing *down* to reach a hardness floor remains free apart from the cogs
- Taking Create's two cog sizes wholesale is fine and deliberate — the rotation layer is openly Create-inspired, their code is MIT, and the mod's originality is in overrun and instrumentation rather than in inventing a third cog. **Their assets are All Rights Reserved: models and textures must be ours.**

#### Decisions taken while building the cogs

- **Cogs mesh face to face, not diagonally.** Create requires large-to-small to be diagonal because its large cog is visibly bigger than one block and its teeth reach the corner. Every block here is still a one-metre cube of placeholder art, so a diagonal rule would be one the player cannot see and therefore cannot learn. Revisit with real models — it is the one part of cogs that *is* engine work, since the propagator would have to scan the twelve diagonal neighbours as well as the six faces.
- **A cog inherits the axis of whatever it is placed against**, sneak to override. Clicked-face placement is right for a shaft, whose axis is the direction you are building, and wrong for a cog, which almost always goes onto the line you just clicked or beside the cog you just placed. Without this, meshing two cogs meant fighting the placement rule.
- **In line along the axis is not meshing.** Two cogs stacked on one shaft are bolted to it and turn as one; only cogs set side by side in the same plane engage teeth. Both cases fall out of the same two methods and neither needed a special rule.
- **The gearbox reverses anything opposite and agrees or disagrees by corner.** Its faces are driven off a common crown, so the signs are geometry rather than preference — and there is no consistent rule where every ninety-degree turn reverses, because the three axis pairs cannot all be negative at once. Taking the sign from the face's own axis direction is consistent by construction. Getting the direction you want is therefore a placement problem, which is the intent: the gearbox has no setting on it (§3), and a player who needs the other way round adds a cog.
- **The gearbox does not visibly turn.** It is a housing; its gears are inside it. No visualizer, no fallback renderer, and it stays in the chunk mesh — which also means it is the one rotating block that cannot vanish when Flywheel's backend is off.
- **One block entity type serves both cog sizes.** Size is a property of the block, not of its state, so drag and inertia are read off the block rather than stored and kept in step.
- **A large cog is the closest thing beat 1 has to a flywheel** — four times a small cog's inertia — which was not the point of building it and is worth watching. §4b's hand-crank-drives-nothing `[OPEN]` wanted a flywheel as its interesting resolution, and one may have arrived early by accident.

### Wear, and the one rule behind it — built and verified in game

Closed two `[OPEN]`s that turned out to be one question. **Force that cannot go into the work
goes into the machine.**

- [ ] **[OPEN] There is no repair, and no way back.** Condition only ever falls, and the only
  recovery is breaking the block and placing a new one — which works, and is unsatisfying for
  something the mod otherwise treats as a physical object. The right answer is almost certainly a
  **Hammer Head** item: an upgrade you could point at (§4), replaced rather than repaired, and
  the same slot a *better* head would eventually go in to raise the ceiling. Deliberately not
  built tonight — it is new content, not a fix, and it wants the roster to stop moving first
#### Reading wear — built and verified in game

**Calipers, by right-clicking the machine.** Decided in conversation; the reasoning is now in
§8 as *Some readings are free; some cost an action*. Verified: a stopped hammer reports a figure
on the action bar, a driven one refuses, and the calipers no longer end up inside the hammer.

- [ ] **[OPEN] Wear has no visual.** It is a Jade line and nothing else. A block state at the
  battered and spent bands would make it readable without the HUD, which is where a free sense
  belongs; queued behind real models, since there is nothing to batter yet
- [ ] **[OPEN] Only the hammer wears — and now only half of that is open.** The *read* side is
  general: `machine/Wearing` exists, and a second wearing machine costs one `implements` and
  nothing at the instrument. The *write* side is still `MechanicalHammerBlockEntity.wear()`, private,
  with `HAMMER_`-prefixed constants. That split was deliberate rather than lazy — the consequence
  of a wasted blow is the machine's own business (a bellows takes the same surplus as nothing at
  all), so there is no shared `wear(float)` to write until a second machine disagrees about what
  absorbing force *means*

---

## 4c. Beat 2 — heat — built and verified in game

All of beat 2 compiles, boots on a dedicated server, loads its four datapack tables, and has now been played: one steel ingot made start to finish.

- [ ] Real models. Every beat 2 block is a vanilla texture on a box

### Numbers, simulated but not played

Worked out arithmetically and recorded here because they are the whole balance argument:

| | small crucible | large crucible |
| :--- | ---: | ---: |
| time constant | 40 t | 400 t |
| equilibrium, no air | 1172 Tu | 1172 Tu |
| equilibrium, full air | 1758 Tu | 1758 Tu |
| climb rate in band, full air | 7.9 Tu/t | 0.79 Tu/t |
| swing per 20 t read interval | **159 Tu, peaks 1599** | 16 Tu, peaks 1456 |

Steel's window was 1420–1480 Tu and iron burned above 1540 at the time this table was measured; both have since moved down 70 Tu (to 1350–1410 / 1470) because the Crude Blast Furnace, rebuilt as an independent block with its own fuel table, could not reliably reach the old window even fully bellows-blown on its hottest fuel (blaze rod, 1400 Tu unblown) -- see the "total conversion" section below. The table's own figures are frozen at the old window and not re-simulated. So a bare charcoal fire **cannot** make steel at any patience (1172 Tu, a real hard gate), air is the only way across, and the same thermostat that holds a large crucible in the window drives a small one straight past the spoil point. Carburizing's 5 Tu/t heating limit then makes the small crucible reset its hold on every stroke — it is not *locked out*, it needs a gentler, carefully geared draught, which is §7's rule that precision never gates hard.

### Decisions taken while building beat 2

- **Heat flows on a difference, and the flat-rate version is the trap worth recording.** A fire delivering a flat `Work/t` is the obvious first implementation and it looks fine until insulation exists: the vessel settles at `supply ÷ leak`, so halving the leak *doubles* the final temperature and insulating a crucible makes it run away. The slice's "large insulated crucible wins" became "large insulated crucible burns the batch". Driving on `(fire − vessel)` fixes it by construction and pays for lava for free.
- **Lava needed no code.** It is a fire fixed at 1200 Tu, so a crucible over lava sits just under 1200 Tu forever — the most stable heat source in the game, and permanently too cool for steel. The slice asked for exactly that and no rule was written.
- **Item cooling is linear, reversing an exponential.** See `Heat.cooled`. The argument is game design, not physics: an exponential never arrives, needs an arbitrary floor, and hides the working window in a flat tail.
- **The bellows' force ceiling is its own long throw**, so the linkage's gear-advantage clamp bites immediately. A bellows holds what it holds; squeezing harder finds no more air inside it. One mechanism, two honest answers — gearing buys force on the hammer and stroke rate here.
- **The strip reads every 20 ticks**, and that lag is load-bearing rather than an optimisation. A sensor with no response time would make the loop tighter than any physical part could be, which would quietly delete the problem the block exists to hand the player. Response time is one of §8's six apparatus properties and this is the first place it has cost anything.
- **A free sense has a range.** The adjective scale ends at 1600 Tu, and iron's spoil point is 1540 — no, *above* what the eye resolves cleanly. A player watching the glow cannot see themselves crossing the line that ruins the batch. That is not a trick; it is why the thermometer exists.
- **Fuel is a table, not a constant**, and vanilla burn times are deliberately not a fallback — a burn time counts items smelted and carries no temperature.

### Read TerraFirmaCraft, and the licence is not Create's

Reference checkout at `../TFC-reference`. **TFC is EUPL-1.2 — strong copyleft, unlike Create's MIT, but adaptable.** EUPL-1.2's Article 5 Appendix lists GPL v3 as a Compatible Licence, so a derivative may be distributed under GPL-3.0 — which is what Feedback now is. The condition is that the election is *recorded* at the site. It is still read for *design* only, because the working rule stays stricter than the law; every place it informed one is named in `THIRD-PARTY-LICENSES.md`.

What came of reading it:

- **Confirmed, not derived:** TFC stores item heat as `(capacity, lastTemperature, lastTick)` and computes it lazily. That is what was already built here, arrived at independently. Worth knowing the shape is load-tested rather than clever.
- **Changed:** item cooling went exponential → linear, after seeing TFC's `adjustTemp`.
- **Changed:** the adjective scale went from 6 wide bands to 10 plus a range limit, after seeing TFC's `Heat` enum. Six bands put steel's entire window inside one adjective, which makes the free sense useless rather than coarse — and a gap that is merely "hot or not" is a wall, not a gap.
- **Changed:** fuel became a datapack table, shaped like TFC's `Fuel`. Which is also the shape §15 already wanted.
- **[OPEN] Not taken: per-material heat capacity.** TFC gives every item its own. Deferred because slice 1 has one hot material, so it creates no choice — it becomes worth having the moment a player must decide *which* of two hot things to carry first.
- **[OPEN] Not taken: catch-up for unloaded devices.** TFC has a calendar and burns fuel across time skips. Our *items* get this free from the stamp; our *blocks* do not, so a crucible in an unloaded chunk freezes. Known gap.

### Still open after beat 2

- [ ] **[OPEN] The Crude Blast Furnace is not built.** §15's vanilla rework — thermal bands on the furnace, smoker and blast furnace via mixin, and the fallback smelting constant — is a whole separate subsystem and was left out of this pass rather than done badly. Beat 2 works without it: the crucible route stands alone. What is missing is the *lesson* — that the demanding material was available the whole time, by hand, before any instrument. That is §7's sharpest claim in the slice and it currently has nothing to land on
- [ ] **[OPEN] Nothing removes heat, deliberately** — and the player has no way to ask for cooling. That is slice 2's damper, earned by withholding it
- [ ] **[OPEN] Steel has no further thermal overrun.** Once it is steel it sits in the fire indefinitely. Iron burning is the only thermal overrun in the slice, where the mechanical chain has plate → foil → scrap. Probably fine — one beat has to teach that overrun is sometimes simply a loss — but worth a second look
---

## 4d. The fitting system — infrastructure landed, nothing concrete attached yet

The thermometer **should** become one eventually. Right now it is a carried instrument, which
is the right call for one instrument and the wrong shape for five.

Build the infrastructure **before** it is necessary, for the same reason `Instrument` was
written before any instrument existed: a high fixed cost paid once beats no fixed cost and a
medium variable cost paid per feature, and the crossover is earlier than it feels. The
thermometer then becomes one class that touches no display code, exactly as it did the first
time.

---

## 4d-1. Data links — tech demo built

Philosophy's own §"how data gets to the controller" already committed to the shape: no cable
block, no network, a controller reads a nearby data-offering block and the client draws a
cosmetic link line. What was missing was doc-2-level mechanics and any code. This pass built
the mechanics and the code, minus the line.

- [ ] **Not taken (this pass): the hover/selection feedback.** CFM's success/error/default link
  colours and "this node is currently selected" highlight are real polish, not load-bearing for
  the tech demo's actual claim (a link is invisible except with the tool out). Every node
  renders the same colour regardless of connector-selection state; queued, not forgotten
- [ ] **Unverified by eye, and doubly so for the renderer.** Nobody has yet loaded a client,
  attached a sensor, linked it to a debug controller, and read Jade — nor confirmed the
  overlay actually draws through a wall rather than merely compiling. No headless display was
  available to smoke-test client rendering this session; `DataNodeRenderer`/
  `DeferredDataRenderer` are unverified beyond matching their reference method-for-method
- [ ] **Not built:** removing a fitting (a crowbar-equivalent interaction); a crafting recipe
  for either new item (both are creative-tab-reachable only for now, same as most of this
  session's casting work); wiring `Fittable` into `ThermalVesselBlockEntity`; any second
  concrete `SensorFitting`, `UpgradeFitting`, or `AdapterFitting`; `DataNode.getMaxNodeLinks()`'s
  cap of 4 is a made-up number, not tuned to anything
---

## 4e. The Crude Blast Furnace and vanilla thermal bands — superseded, see §4f

**This whole section describes a mixin pass that has since been ripped out and replaced.** Every
mixin it names below was deleted on 2026-09-13, the same day it was first actually played: the
first real in-game test found that redoing a vanilla block's GUI from inside a mixin meant
fighting one vanilla-internals assumption at a time, and each one only surfaced by crashing
(`Slot.index` never set on a substituted slot, `canPlaceItem` hardcoded per index, an arrow baked
into the background texture rather than drawn dynamically, and more). §4f is the replacement —
three independent blocks, no vanilla class touched at all — and is the current source of truth for
this system. The bullets below are kept as a record of what was *learned* (the food/metal split,
the recipe pooling, the Smoker's ceiling) which all carried over into §4f unchanged; only the
*implementation* they describe is gone.

- [ ] **Unverified by eye.** Nothing confirmed yet: that a lit furnace actually climbs and shows
  fire/light, that a Blast Furnace clears steel's window on bellows air the way the crucible does,
  that a Smoker genuinely refuses to smelt ore no matter the fuel, that food left too long
  anywhere burns, and that `logs` alone really cannot smelt ore in a plain furnace
- [ ] **[OPEN] The fuel slot still accepts anything vanilla considers fuel.** `canPlaceItem` was
  not touched, so an item with no `FuelTable` entry can still be inserted and will simply sit
  there inertly rather than being rejected. Cosmetic UX gap, not a correctness one
- [ ] **[OPEN] The heat gauge has no hover tooltip.** Calipers and the thermometer both narrate a
  reading through chat; the gauge is silent about which band it's showing beyond the colour.
  Probably fine — it's the free sense, not an instrument — but worth a second look once a fitting
  can actually sit on one of these and something needs to react to a number
- [ ] Real models — none of this changed any art, and none needed to

---

## 4f. Total conversion: independent vessel blocks — built and verified in game

2026-09-13. Feedback stopped reworking vanilla's Furnace/Smoker/Blast Furnace in place and started
replacing them outright — `feedback:furnace`, `feedback:smoker`, `feedback:blast_furnace`, three
new blocks with vanilla's own textures (placeholder art, unchanged) and none of vanilla's classes
in their hierarchy. Vanilla's own three blocks are still in the game, inert: their crafting
recipes are disabled (`data/minecraft/recipe/{furnace,smoker,blast_furnace}.json`, overridden with
a `neoforge:false` condition), so a normal playthrough never obtains one. This is a genuinely
different mod now, not a rebalance of vanilla's, and that is the direction this was pointed at on
purpose (see the session log if the phrase "total conversion" needs the context).

**Verified by a player, in game, same session:** placing all three, cooking food to completion in
the Smoker (previously impossible, see below), making steel in the Blast Furnace with a bellows
and a blaze rod, pulling a workpiece out and reading its temperature by hovering it.

- [ ] **[OPEN] The fuel slot still accepts anything `FuelTable` recognises but nothing vanilla
  doesn't** — the inverse of the old mixin pass's gap. A `FuelSlot.mayPlace` override already
  exists (`ThermalVesselMenu`); nothing further needed unless a fuel item wants to be *rejected*
  for a reason `FuelTable` itself does not already encode.
- [ ] **[OPEN] Making the raw vanilla furnace/smoker/blast furnace block itself inert** (rather
  than merely uncraftable) was considered and deliberately not done — it would mean mixin-patching
  `newBlockEntity`/`getTicker`/`openContainer` on three vanilla block classes, exactly the fragile,
  one-crash-at-a-time surgery this whole rewrite exists to get away from, to guard against an edge
  case (another mod or a datapack handing the player a vanilla furnace) that disabling the
  recipe already covers for a normal playthrough.
- [ ] **Planned: replace vanilla furnace/smoker/blast furnace occurrences in generated structures**
  (villages etc.) with Feedback's own equivalents. Not mixin-patching block behavior — this is a
  worldgen/structure-processor swap, a different technique from the item above and not blocked by
  the reasoning that ruled that one out.

---

## 5. Slice 2

Scope already known. Three things arrive together, each making the others necessary:

- [ ] **Blaze Rod** — Nether opens at the end of slice 1 (flint and steel takes steel)

---

## 5a. Boiler and Steam Engine — built and verified in game

Recipes now exist (`recipe/boiler.json`, `recipe/steam_engine.json`, `recipe/copper_tubing.json`),
unlocked by `advancement/recipes/steel_machines.json` on first steel ingot. Playtested.

Not required by slice 1 or 2. Built now anyway, on the same corollary that justified `Instrument`
and `fitting/` before either had a concrete use: the fixed cost is small today and grows the
longer it waits. This is the mod's first working bridge between two energy types (philosophy
§10) — `machine/boiler/` (`ThermalBody` → Steam) and `machine/steamengine/` (Steam → `Su`/`Rpm`).

**Two blocks, not one, and this was a mid-session correction, not the first draft.** The first
pass fused boiler and turbine into a single block, on the theory that `ThermalVesselBlockEntity`
already makes that exact simplification over the crucible-plus-firebox pair. Wrong here: a
furnace is always its own fire, but what makes the boiler's heat is precisely the axis philosophy
§10 says has to stay open. Quoting the correction, because it belongs in the design record
verbatim rather than paraphrased: *"there shouldn't be anything stopping you from using
electricity to heat a coil to heat steam to turn a shaft inside a magnet to make less electricity
than you started with, other than your own mind telling you that's stupid. It also means that
ANY form of heat is a viable form of steam creation, which gives the player the option to
explore."* A fused block forecloses that by construction — only whatever the class author
imagined can fire it. Two blocks joined by a real fluid do not, and cost the same to build once
Steam is a real `Fluid` anyway. See `THIRD-PARTY-LICENSES.md`'s GregTech CEu Modern section and
`FTuning`'s `--- the boiler ---` doc for the full account of what was read to get here.

Open, deliberately:
- [ ] **[OPEN] No steam bucket.** Same call the molten metals already made and for the same
  reason (see `FFluids`) — a real fluid today, a bucket only once something actually needs to
  carry it by hand rather than through the `Capabilities.FluidHandler` both blocks expose.
- [ ] **[OPEN] Flat output, not a throttle.** `STEAM_ENGINE_CAPACITY_SU`/`RPM` are a threshold,
  not a curve — seen running in game, not just simulated
- [ ] **[OPEN] The exploding boiler.** §14 names GTCEu's over-restricted boiler explosion as good
  precedent for a genuine hard-gate consequence. No failure state exists to explode into yet.

---

## 5b. Cast iron, brass, bronze — from `feedback_progression_roadmap.md`'s Slice 1/2 roster

Closes three of the "essentially the already-implemented content" gaps an audit turned up
(roadmap's Slice 1 claim overstated what was actually built — see the audit that prompted this
pass). Full design writeup: `feedback_mechanics.md` §4.

**Verified in game** — alloy pouring (`AlloyMix`/`AlloyTable`) and casting both playtested.

- [ ] **[OPEN] No way to correct a bad alloy mix.** `AlloyMix#resolve` goes empty on an
  off-ratio mix rather than exploding or refusing the pour, but there's no partial extraction or
  dilution path either — a badly-mixed crucible is stuck until a new one is built. Worth a real
  mechanic once something demands it; not solved speculatively here.
- [ ] `brass_ingot` and `bronze_ingot` have casting recipes now (`casting/`); `cast_iron`,
  `tin_ingot`, `zinc_ingot` still have none. No blockstates/models or advancement unlocks for any
  of the five yet — creative-tab/`/give`-only for the three still unrecipe'd.

## 5c. Glass, ceramic, firebrick — more of the same Slice 1/2 roster gap

No new mechanic needed for any of these three — all reuse infrastructure that already existed
before this pass. Explicit user calls, after checking TFC's actual (much bigger) systems for
prior art: glass stays a plain meltable/castable material rather than TFC's glassblowing
minigame; ceramic fires in the existing crucible/vessel rather than a new Pit-Kiln-style block;
firebrick gets its own raw material (`kaolinite`) rather than being ordinary ceramic fired hotter.

**Verified in game.**

- [ ] `glass_ingot` has a casting recipe now (`casting/glass_ingot.json`); `ceramic`,
  `kaolinite`, `firebrick` still have none. No blockstates/models or advancement unlocks for any
  of the four yet.

## 5d. Slice 2 materials — graphite, lead, copper tubing, quartz glass

First real Slice 2 (not Slice 1) content. Scoped to materials only, deliberately, after the
roster turned out bigger and more varied than §5b/§5c (real new machines — kiln, heat exchanger,
pressure vessel, annealing furnace).

**Resolved: annealing needs no new mechanic.** It's `require_cooling: true` plus a tight
`max_rate` — the same two knobs tempering already has, both turned at once. See
`feedback_mechanics.md` §"`max_heating` → `max_rate`" and `ThermalProcess`'s own javadoc. No
annealing datapack entry exists yet; that's content, not design.

**Verified in game.**

- [ ] `lead_ingot` and `quartz_glass_ingot` have casting recipes now (`casting/`); `copper_tubing`
  already had its own shaped recipe. `graphite` still has none. No blockstates/models or
  advancement unlocks yet for any of the three.
- [ ] Remaining Slice 2 gap: glass tubing. `MoldItem` casts one shape (an ingot) — see its own
  class doc. A second mold shape needs `Casting`/`CastingTable` widened to key on (fluid, mold
  shape) rather than fluid alone, since the same molten glass would need to produce a different
  result depending on which mold is used. Not started.

## 5e. Kiln, Annealing Furnace, Heat Exchanger, Pressure Vessel — built and verified in game

Compiles, `./gradlew build` and a `runGameTestServer` boot both come back clean, and all four have
now been placed and played: Kiln fires ceramic/firebrick, Annealing Furnace holds tempering's
band, Heat Exchanger moves heat between two vessels, Pressure Vessel pumps up on Bellows air and
explodes past critical.

Prior art: PneumaticCraft: Repressurized's `IAirHandler`/`PressureTier` (GPL-3.0, this project's
own licence; see `THIRD-PARTY-LICENSES.md`) — `pressure = air / volume`, and risk climbing
linearly from a danger threshold to a certain-explosion critical one. User's call, not the usual
"ask first" default: named directly rather than surveyed.

- **Kiln and Annealing Furnace are two more `VesselKind` entries**, nothing else — same class as
  Furnace/Smoker/Blast Furnace, differing only by `FTuning`'s mass/leak/ceiling triple (§15's "one
  wall, not two" pattern, now used a third and fourth time). Kiln's ceiling (`KILN_CEILING_TU`,
  1330) sits just under steel's floor (1350) so it fires ceramic and firebrick but is hard-gated
  away from steelmaking with no whitelist anywhere. Annealing Furnace's ceiling
  (`ANNEALING_FURNACE_CEILING_TU`, 380) sits under tempering's own 400 the same way the Smoker's
  sits under `FOOD_MAX_TU` — it cannot overheat a batch past hardening temperature at all, which
  is what makes it a genuinely different answer to controlled cooling than a Crucible with a
  Damper (§5): safety traded for the Crucible route's actual control over the rate. Deliberately
  **not** given Damper support — `DamperBlock`'s own doc already scopes it to the Crucible family
  ("the Furnace/Smoker/Blast Furnace family never grew a neighbour-scan for insulation either"),
  and this follows that existing line.
- **Heat Exchanger is new**: a plain block, faced like a Shaft, that moves heat between whatever
  `ThermalBody` sits on either side of its axis every tick — `Heat.exchange(a, b, conductance)`,
  a new fifth method on `Heat` alongside `tick`/`equilibrium`/`cooled`/`ticksAbove`, hot side to
  cold side, no identity check on either end. This is the *passive* half of `feedback_philosophy.md`
  §10's exchanger only — a genuinely pumped, powered, heat-uphill version (with its own
  both-directions-at-once overrun) is named there as real future work and not built here.
- **Pressure Vessel is new**: a sealed block that reads `Pu` (new unit, `core/unit/Pu.java`) as
  `amount / FTuning.PRESSURE_VESSEL_VOLUME`, fed by a Bellows through the same `Blown` interface a
  Firebox already answers — no new actuator, no identity check on the compressor either. Above
  `PRESSURE_VESSEL_DANGER_PU` it risks exploding every tick, at odds climbing linearly to a
  certain explosion at `PRESSURE_VESSEL_CRITICAL_PU`. **This closes §5a's long-open "exploding
  boiler" item** — not for the Boiler itself (untouched this pass), but for Feedback's own
  equipment in general, which is what `feedback_philosophy.md` §14's GregTech precedent actually
  asked for.
- [ ] **Not built:** any recipe or process that actually *needs* elevated pressure — same
  "infrastructure before the concrete use" precedent as the Boiler and the fitting system.
  Nothing in `ThermalProcess` has a pressure field yet.
- [ ] **[OPEN] Air still has no unit** (the pick-up list's oldest open item). Feeding a Pressure
  Vessel through `Blown#addAir` sharpens the question rather than answering it — the same
  `St`-typed figure now drives combustion draught *and* accumulates as a stored gas quantity, two
  different physical roles through one ambiguous number.
- [ ] Real models — placeholder vanilla textures/models throughout, reusing Smoker's for the
  Kiln and the Blast Furnace's for the Annealing Furnace (deliberate duplication, same standing
  as Furnace's own reuse of `minecraft:block/furnace`).

## 5f. Improved Furnace, Fittable wiring, and Dip — built and verified in game

Compiles, `./gradlew build` and a `runGameTestServer` boot both come back clean, and it's been
played: sensor fitting mounts on the Improved Furnace, glazing works in both vessel types, the
heat-gated dip behaves.

- **`Fittable` is wired into `ThermalVesselBlockEntity`**, closing §4d-1's oldest "not built"
  line. `TemperatureSensorFitting` was holder-specific (`CrucibleBlockEntity` only) and is now
  widened to take any `BlockEntity` + `ThermalBody` pair — two plain fields rather than a generic
  bound, since no existing class is typed as the intersection and an `instanceof` chain can't
  produce one without an unchecked cast. `TemperatureSensorItem` now attaches to *any* `Fittable`
  `ThermalBody`, no identity check on which block.
- **Improved Furnace is a new, steel-tier `VesselKind`**: 3x the plain Furnace's mass, half its
  leak ("more stable... less loss", the user's own framing), no ceiling (still the generalist).
  It is the vessel `hasThermowell` actually belongs to now — the flag sat unused on plain
  `FURNACE` since before `Fittable` existed anywhere but the Crucible; moved rather than doubled,
  so exactly one vessel means "designed to be measured" per era. **Deliberately still short of
  the Crucible**: no alloying (`AlloyMix` stays Crucible-only), no insulation or Damper (both
  Crucible-family per `DamperBlock`'s own scoping) — steadier and observable, but a sealed
  appliance, never a component. Recipe upgrades a plain `feedback:furnace` with 8 steel ingots.
- **`Dip` and `DipTable` are new** (`process/Dip.java`, directory `data/feedback/dip/`), and
  `machine/dip/Dipping.java` is a new `PlayerInteractEvent.RightClickBlock` interaction, deliberately
  **not** the same table as `Quench`/`Casting` — see `Dip`'s own class doc for why. Two different
  behaviours depending on what's clicked:
  - **A `MoltenVessel`'s tank**: only acts when a `Dip` entry actually matches (fluid, held item,
    and now — user-requested, added the same session — an optional `min_temperature`/
    `max_temperature` band checked against the vessel's *live* `getTemperature()`, since fluid
    sitting in a tank has no melting physics of its own and "holding the right fluid" and "that
    fluid is currently hot enough to use" are different facts). Same drain-and-check shape
    `MoldItem` already uses against `CastingTable`. A vessel with no matching entry passes the
    click through untouched on purpose, so a Crucible mid-melt never steals a click from
    `MoldItem`'s own fill interaction, which answers the same event first. **Crucible and Thermal
    Vessel both dip identically** — no identity check on which `MoltenVessel` it is, confirmed
    rather than assumed (`CrucibleBlock.useItemOn` inserts raw material only when nothing else
    claimed the click first, so a non-matching dip attempt on a Crucible correctly falls through
    to that, not a bug).
  - **Plain water or lava, world blocks**: no table at all — an unconditional, instant `ItemHeat`
    snap to `AMBIENT_TU` or `FIRE_TU_LAVA`, the same instant-on-contact model `Quenching` already
    uses for a dropped ingot. This is the first hands-on way to *add* heat with no machine at all
    (philosophy 7's manual route, landing somewhere it was never installed — §5's own test).
- **Ceramic glazing is the first `Dip` entry** (`dip/glazed_ceramic.json`): ceramic dipped in a
  vessel holding molten glass becomes `glazed_ceramic`. No invented material — real ceramic
  glazes are glass coatings, so molten glass (already built) is the glaze.
- [ ] **Not built: electroplating.** Named by the user as the actual reason to build `Dip` now
  rather than later (same "infrastructure before necessity" corollary as `Instrument`/`fitting/`)
  but nothing here does it — no electricity system exists yet for it to plug into (roadmap Slice
  4/5).
- [ ] **[OPEN] Nothing checks whether a dipped item "can stand" the temperature.** The user's own
  framing, left honestly unresolved: dipping something in lava that cannot survive lava heat just
  reads as very hot, with no destroy/spoil consequence invented here. Whatever that should do is a
  property of what reads the temperature (a `ThermalProcess`'s `spoil_temperature`, if one is
  watching), not a rule worth inventing speculatively.
- [ ] Real models — none of this changed any art.

---

## 5g. Machine shop — `feedback_machine_shop_spec.md` — built, unverified in game

All ten items in the spec's own §5 build order are in: Bearings, Valve, Wire Drawer/Rolling
Mill/Mechanical Press, Rod, Steel Cable, Piston, Drill Press, Lathe, Grinding Wheel, Pulley +
belt. `./gradlew build` is clean after every stage; nobody has yet placed any of these blocks in
a running client and watched them work. See the spec doc's own "Decisions taken while building"
section for what had to be resolved along the way -- a new `Operation.ROLL`/`Operation.REMOVE`,
the new `removal/` datapack table and `Removing`/`Removal`/`RemovalTable`, the Lathe's `SensorFitting`
wiring, the Grinding Wheel's wears-under-correct-use exception, and the Pulley's belt-as-a-second-
network design. Also fixed in passing: a pre-existing `Deformation.STREAM_CODEC` bug (seven fields
into a six-field `StreamCodec.composite`) that predates this pass and was only surfaced by it.

- [ ] **Playtest all ten in a running client/server.** Nothing here has been placed and watched;
  in particular the Pulley's belt-as-a-second-network model (§6a of the spec) and the Wire
  Drawer/Drill Press/Lathe UX conventions (sneak-toggle for a discrete setting, item-typed slot
  routing) are design-on-paper until somebody actually plays them.
- [ ] **No real models or textures** for any of the ~13 new blocks or ~15 new items -- all
  placeholder vanilla textures per the spec's own numbers-are-placeholder framing.
- [ ] **Threaded rod** (§2.3's `[OPEN]`) — rolled on the Press vs. cut on the Lathe — deliberately
  not built; see the spec's own §6.
- [ ] **Gear Cutter** — deliberately not scoped; see the spec's own §6.

---

## 6. Long-range open questions

From `feedback_philosophy.md` §18. Not urgent; do not answer early — answering these now means inventing the tech tree, which is the trap.

- [ ] Separation mechanisms mapped to the actual material roster (§11)
- [ ] Machine roster + physical upgrade path per process family (§14)
- [ ] How far vanilla reinterpretation goes beyond smelting (§15)
- [ ] Create compatibility — ship it or not, and how deep (§16)
- [ ] Local conversion adapters: electrical to mechanical, electrical to thermal (§10)
- [ ] Visual architecture of each factory era (§14)
- [ ] Where each exotic phenomenon first appears and how it becomes engineered (§2, §14)
- [ ] Full era arc; where the "detect before you can act" bridges land (§14)
- [ ] Personal empowerment curve (§14)

---

## 7. Parked

- **Food as a real thermal process** (§18a) — cooking meat to different degrees for different hunger/saturation. Fits the model almost too well. Scope creep against a slice about metal. Worst case, a good addon. **Not now.**

---

## Running check

From §5. Apply to every new system before building it:

> **When a trade the mod already believes in shows up somewhere it was never installed, the systems are right. When every interesting choice has to be placed by hand, they are not yet.**

Ask of anything new: *does this create a second viable answer to a problem that already had one?*
