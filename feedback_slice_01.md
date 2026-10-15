# Feedback — Vertical Slice 1

> **A working document, not one of the three.** This is the first playable slice, built by the method in `feedback_philosophy.md` §20. It is deliberately concrete: real materials, real machines, real numbers.
>
> It feeds the other two documents rather than replacing them. The rules it discovers become **document 2 (mechanics)**; the objects it names become **document 3 (content)**. Nothing here overrides the philosophy — where this document and the philosophy disagree, the philosophy is right and this slice is wrong.
>
> **Every number here is a placeholder.** They exist so the slice can be reasoned about and played, not because they are balanced. Philosophy §19 still holds: exact values are not decided.
>
> **This document is revised as the slice is built.** Where it once said 30 Fu to a plate, it now says 14, because 30 made the overshoot lesson impossible to express. Where it said either crank works on copper, it now says the strong one cannot make a plate at all, because that turned out to be far better. Git holds the earlier versions; nothing here is preserved out of sentiment. The live figures are in `core/FTuning.java` and the deformation datapack — **where this document and the code disagree, the code is what is running and this document is what is wrong.**

---

## Scope

Two beats, one gimmick each.

> **Beat 1 — repetition.** *A machine will do this forever, and it does not know when to stop.*
>
> **Beat 2 — heat.** *You cannot control what you cannot see.*

Neither beat contains a controller. The player is the controller, until a bent strip of metal takes over the one job simple enough to hand off.

**One energy network, two process variables.** Mechanical power is the only distributed energy system in the slice — shafts, one generator, one consumer. Temperature is a *process variable*, not a network: heat comes from burning something in a firebox directly under the vessel. This satisfies §20's "one energy system" honestly while still allowing beat 2, and it defers the entire thermal-transport layer.

**Explicitly out of scope:** electricity, chemistry, spatial, fluids as a system, separation, covers as a general mechanism, the node-based controller from §13, multiblocks, and any second energy network.

**Budget:** 12 items, 7 machines, 2 process variables. Counted at the end.

---

## Beat 1 — Repetition

### The problem

The player wants copper plates. Plates are a real intermediate — they go into the machines and instruments that the rest of the slice needs — so the demand is structural rather than a fetch quest.

The process specification, per §5:

```
COPPER PLATE

Input          1 × Copper Ingot
Process        Mechanical deformation
Work           14 Fu
Hardness       1
Output         1 × Copper Plate
```

Note what the recipe does not mention: any machine at all.

Note also what it does not separately state: how hard a blow has to be. Hardness does that job as well — it is the floor below which nothing lands, *and* the divisor for how much of a bigger blow gets through (§17). Copper's hardness of 1 means every blow lands in full, which is exactly what makes it dangerous to hit hard.

### Doing it by hand

A hand hammer swings at about 3 St. Against copper's hardness of 1 that is 3 Fu a swing, so five swings make a plate — and the **eighth** swing overshoots it into foil, not the sixth. (Got wrong here once, and worth recording why it was tempting: "one more swing than it took to reach the last stage" assumes every stage costs about the same and starts from zero, but it does not — the fifth swing lands 15 Fu against a 14 Fu requirement, so 1 Fu carries into the plate, and the plate itself costs 9 more. Three swings at 3 Fu clears that, landing on the eighth. Confirmed by `gametest/HandDeformationGameTests`, `TODO.md`.) It works, and it is exactly as fiddly as counting swings sounds.

This is §7's *possible* column, introduced in the first ten minutes and never taken away. A player can hand-hammer plates for the entire game if they want to. Nobody will.

**It swings in a crafting grid.** Hammer and workpiece in any crafting table, and one craft is one blow — the ingot comes back out of the result slot with 3 more `Fu` on it, five times, and then it is a plate. The hammer loses a point of durability each swing, and each swing **clangs**: the Mechanical Hammer's own anvil sound, quieter, because the two are landing the same blow and should be recognisable as the same thing. There is no block to place and no minigame; hand work is meant to be tedious rather than skilful, because a hand route that was *fun* would compete with the machine instead of motivating it.

Two things fall out of that choice, and both were the reason for it:

- **~~Shift-click is the lesson.~~ Corrected 2026-10-15 (playtest).** The plan was that shift-click crafts until the input runs out, running a careless stack through plate, foil and scrap. The hammer works one piece at a time, so the first craft consumes the input and shift-click is one blow. The tempting part was a lesson delivered by the player's own hand; what is left is the player's own counting, which nothing does for them.
- **Steel needs no rule to be out of reach.** 3 St against steel's hardness of 15 lands nothing at all, so the recipe simply does not appear. That is §7's hard gate arriving by arithmetic rather than by a tier check, and nobody wrote a rule about hands.

The bootstrap this fixes was real and was load-bearing: a copper plate needed a Mechanical Hammer, and a Mechanical Hammer needs copper plates. Without a hand route that is a hard gate standing exactly where the philosophy insists there is no physical impossibility.

**Copper is chosen because it is forgiving and abundant** (§6, §7). It is nearly impossible to actually destroy, and the player has stacks of it. The slice needs a material the player can afford to waste while learning that waste is possible.

### The machine that does not stop

The **Mechanical Hammer** strikes whatever is inside. It does not take rotation — it takes **strokes**, because a hammer goes up and down and a shaft goes round and round.

Converting one into the other is a separate component the player places:

```
CRANK LINKAGE
Converts rotation to reciprocation.
Two variants: short throw, long throw.
```

```
MECHANICAL HAMMER
Strength   12 / 3 St      (short throw / long throw)
Stroke     one blow per cycle; resets between blows
Load       80 Su
```

A hammer strikes **discretely**. Between blows it is travelling back up and applying nothing at all. That dead time is free early on and becomes expensive later, for reasons that only show up in the convergence.

Both figures are printed on the machine. Which one the player gets is decided by the crank they bolted to it (§17), and the choice is binary — there is no throw number to tune, deliberately (§3).

**A new linkage comes set to the long throw**, the gentle one, so a player who has never heard of overrun can make the thing they were aiming for. Driven that way the hammer lands 3 Fu a blow: a plate on the fifth, and three more blows before it is foil.

Then they find the other setting, and it is four times faster.

```
long throw    ingot · ingot · ingot · ingot · PLATE · · · foil
short throw   ingot · FOIL
```

Short throw lands 12 Fu a blow — nearly a whole plate's worth — so the second blow finishes the plate *and carries straight through it into foil in the same stroke*. Plate is not merely quick to pass at that setting. It is **unreachable**.

That is the slice's sharpest lesson and it needs no text to deliver it. The two settings are not fast and slow versions of one process. They make **different products**, chosen by a lever arm rather than by any recipe selector — a short-throw hammer is a foil machine. The player who wanted plates and fitted the strong crank gets a chest full of foil and has to work out why.

There is an invariant underneath it worth knowing while tuning (§17): **the reaction window is the strength ratio.** For the strong setting to skip a state, that state must cost less than one strong blow — which caps the gentle setting's grace at the ratio between the two, in blows. `12 / 3` can never give more than four.

```
ingot → plate → foil → scrap
```

**Every one of those is a real item**, and the foil is genuinely useful — it goes into the thermometer in beat 2. So the first overrun the player ever experiences is a *sidegrade* (§6), which is the correct lesson to teach first: the machine is not malfunctioning, and the extra state is not damage. It is a place further along the same physical axis. The player is the one who decided which point on that axis they wanted.

Only past foil does it become scrap. The punishment exists, but it is not the introduction.

### Reading the workpiece

A part-worked ingot **looks** part-worked. It is visibly flattening, the player can see roughly how far along it is, and they can see when it has gone too far. Mechanical deformation announces itself, and the player needs no instrument to avoid ruining an ingot (§8).

So beat 1 deliberately ships **no measuring tool at all.** The player's eyes are sufficient for the problem beat 1 poses, and handing them an instrument here would teach the wrong lesson about what instruments are for.

### The clutch and the timer

The obvious fix. Power the hammer, wait, cut power.

It takes **two blocks**, not one, and the split is the point (§8, §13). Stopping a shaft and deciding when to stop it are different jobs, so they are different things you bolt together:

```
CLUTCH          engages or releases the shaft run in front of it
TIMER           winds, runs for that long, lets go
```

The Clutch is the mechanical actuator. It knows nothing — it holds the output shaft or it doesn't. The Timer is control at its crudest: it can start and stop a supply, and that is the whole of its vocabulary. Neither can measure anything.

Keeping them apart costs the slice one block and buys two things. A player can throw the clutch by hand before ever building a timer, which is the honest introduction to *you are the controller*. And when slice 2's controller arrives it **replaces the Timer** rather than introducing a new idea — the clutch cannot tell the difference, because §13 allows a controller no output a timer does not already have.

**And a clutch does not stop the hammer.** It stops *driving* it. Whatever is downstream keeps turning on its own momentum until friction takes it, so blows keep landing after the timer lets go — fewer each time, but landing. Nothing in the clutch implements that; disengaging simply splits the run into two networks and the far one has no source any more.

So the timer is a bet (§8), and a worse one than it looks. The hammer's rate depends on the rotation it is getting, a water wheel's speed wanders with how much water is against it, shaft friction rises with speed, and the coast-down adds a few uncounted blows on the end. The same wind buys a different number of blows on different days: *mostly plates, sometimes foil, occasionally scrap.*

The player's first instinct — wind it short and accept a few under-worked ingots — is correct, and it is their first encounter with trading yield for safety.

### The anvil is a container, and that changes nothing

The hammer exposes its anvil as a single slot, so a vanilla hopper can feed it and a vanilla hopper can empty it. That is the mod's only item-handling ambition: **moving items is vanilla's job**, and a mod about what happens *to* a workpiece has no business inventing a second pipe network.

It is worth being explicit that this does not smuggle in completion detection. An extractor takes whatever is lying on the anvil, worked or not, because nothing in the mod can ask whether a workpiece is finished — the hammer does not know what it is making and the item does not announce that it has arrived. So a hopper is not a sensor bolted to the machine; it is a second machine running at its own rate, and the player's problem becomes the **race between how fast work goes in and how fast product comes out**. Pull eagerly and you bank half-worked ingots. Pull late and the plate is foil.

Which is the same bet the timer already is, arriving on the other side of the machine and costing something different — capital and layout rather than attention. §5's test passing, in a place nobody installed it.

And because a part-worked item carries its own `Fu` (§9), a hopper pulling one out early has not ruined it. It can go back in, from a chest or by hand, and carry on from where it stopped.

### Calipers, and what they are actually for

Late in beat 1 — **after** the timer, not before — the player wants something their eyes cannot give them: not to avoid scrap, but to hit **foil deliberately and every time.**

```
CALIPERS
Held tool. Measuring a workpiece reports  16 / 20 Fu
```

That is the distinction §8 draws between an instrument that *reveals* and one that *refines*. Calipers refine. The player could always see the ingot flattening; what they could not do was aim. Hitting the narrow sidegrade band on purpose (§6) is the first thing in the game that requires a number rather than a glance.

It matters that this is the **weaker** of the slice's two instruments and arrives first. When the thermometer shows up in beat 2, the player already understands what an instrument is — and then meets a quantity they cannot see at all, which is a much bigger event. Calipers make the thermometer land harder by being the small version of it.

### Mechanical infrastructure

Rotation has to come from somewhere and travel through something.

```
HAND CRANK      12 Su     while the player holds Right Click
WATER WHEEL    256 Su     continuous, needs flowing water
SHAFT                     free to install, not free to turn
```

Connecting a second machine is not an unlock. It is an arithmetic problem: 256 Su of supply, 80 Su per hammer. Three hammers is fine. Four is not. Nothing says *requires tier 2*; the supply simply is not there.

**Shafts are not free either.** Every one charges its bearing friction to the network, and that cost rises with speed. This has a consequence nobody has to be told: a network accelerates until its surplus torque runs out, so a long shaft run **finds its own top speed**. A crank rated 32 RPM turns three shafts at 32, five at 24, and ten at 12. Nothing refuses; it just goes slower. This is deliberately not a per-shaft throughput cap, which would produce the absurdity of a generator being *too good* for its own shafting.

So *slow and wide* and *fast and narrow* become two real answers to the same problem, and nobody was asked to choose between them (§3).

### The crank cannot drive the hammer

Twelve Su against eighty. This is not an oversight and the player is meant to run into it.

**[OPEN]** — but it does mean the hand crank currently drives *nothing at all* in the slice, which makes it a component whose entire purpose is to fail. That is one lesson too thin to justify a block. The interesting resolution is a **flywheel**: a weak source spinning up a heavy mass that discharges in bursts is exactly how a treadle hammer works, it would make inertia a thing the player builds with rather than merely observes, and it would give the crank a real job. Out of scope for this slice; recorded so it is not mistaken for a balance bug.

This is where `Su` and `RPM` earn their keep (§17), and the player learns that infrastructure is a thing that exists before they learn anything about control.

---

## Beat 2 — Heat

### The problem

Steel. The player has iron in quantity, and a reason to want something better.

```
STEEL

Input          1 × Iron Ingot, 1 × Charcoal
Process        Carburizing
Temperature    1350–1410 Tu
Hold           600 t (30 s)
Max heating    5 Tu/t
Output         1 × Steel Ingot

Overshoot past ~1470 Tu → Burnt Iron (spoil)
Undershoot         → unchanged Iron Ingot (nothing happens)
```

**Iron is chosen for the same reason copper was: the player has piles of it.** Per §7, the cost of ignorance is paid in raw material, and that lesson only lands if the player can afford to pay it. Burning thirty iron ingots to make four steel is a formative experience. Burning thirty of something rare is a quit-the-modpack experience.

Note the asymmetry in the failure modes, per §6: undershooting does *nothing*, overshooting *destroys*. The player can fail safely in one direction and expensively in the other, which teaches them which way to err long before they can measure anything.

### Why copper did not teach this

Copper's window was "hot." Steel's window is 60 Tu wide and has a rate limit on the way in. The player's furnace can absolutely reach 1450 Tu — **the heat was never the problem** (§7). They simply have no idea what temperature they are at.

So they do the only thing available: guess, wait, pull it out, look at it. Six stacks in, two ingots out. The recipe was never locked and the player is not being punished. They are paying for information they do not have.

### The Crude Blast Furnace gets them most of the way there

The vanilla blocks are real thermal vessels now (§15), and one of them is genuinely good at this. The Crude Blast Furnace (from vanilla, given the word crude now to make space for a more sophisticated multiblock later down the line) has a high floor, a high ceiling, and enough thermal mass to sit near a temperature on its own.

So the player *can* make steel here, by hand, before owning a single instrument. It takes standing there, watching the glow, and accepting a poor conversion rate — but it works, and it should feel like a real accomplishment rather than a stopgap.

This is the slice being honest about §7. The demanding material was available the whole time. What the player lacked was never heat.

### The thermometer

Copper foil, a sealed tube, and something that expands.

```
THERMOMETER  (crude)
Range        0–2000 Tu
Accuracy     ±25 Tu
Resolution   25 Tu
Update       every 20 t
```

It controls nothing. It is a window (§8).

And it is immediately, obviously insufficient — ±25 Tu against a 60 Tu window means the player can be at 1450 or 1425 or 1475 and cannot tell. **But it is enough to work by hand.** Heat, look, wait, look, adjust, look. Miserable, slow, and it produces steel reliably enough to be worth doing.

That is the progression moment, and no recipe unlocked. The player gained *information*, and information became capability.

### And then they discover they cannot point it at anything useful

The Crude Blast Furnace is sealed, and everything good about it follows from being closed (§15).

**Built differently from this draft, and the change is worth keeping — twice now.** The thermometer is *not* a fitting — it is a carried instrument, exactly like the calipers, because reading is passive and costs no hands (§3). The wall was first enforced from the other side: a vessel declares whether it can be got at, through `ThermalBody.hasThermowell()`, and a sealed one refused to be read by **any** instrument, full stop.

That first version was tempting because it made "sealed" and "measurable" the same fact asked two ways, and it was wrong for conflating two actions that cost two different things. Watching a gauge is free and continuous; sticking a thermometer through the door for one reading is a real, costed action — the same distinction the calipers already draw between reading an item in hand and reading a machine you have to walk up and touch (§8). A sealed vessel can still be dipped into by hand. What it refuses is being *watched*: no ambient reading, no HUD card, no sensor fitting ever auto-attaching itself to it. That is exactly what a controller needs and a one-off dip cannot give it.

**This is the wall the slice is actually built around**, and it survives the correction, just narrower than first stated. The player's best thermal vessel and their first *automated loop* are mutually exclusive, and no amount of iron fixes it. They can check the thing that works, by hand, whenever they're standing in front of it. They cannot leave it running unattended and trust a reading. They cannot automate the thing they can watch for free.

The way out is not a better furnace. It is a vessel that was *designed to be measured* — which is the crucible, and which is why it exists.

```
CRUCIBLE
Thermal mass   high, tunable by size
Instrument     accepts covers — it has a thermowell
Batch          larger than the blast furnace
Speed          slower than the blast furnace
```

The crucible is not an upgrade. It is **worse at the thing the blast furnace is good at**, and it is the only one of the two that can ever be part of a loop.

### The actuator

A **Bellows** driven by the mechanical network — the first place the two beats touch. It squeezes, so like the hammer it wants strokes rather than rotation, and it needs a linkage of its own.

```
POWERED BELLOWS
Air per stroke   10 / 30      (short throw / long throw)
Load             40 Su
```

Note the numbers run the other way. On the hammer, the short throw was the strong option; on the bellows, the **long** throw is the useful one, because a bellows wants to move air, not to hit something.

The player has already met this component and now finds it means the opposite thing. That is the first time in the slice that one part composes two ways, and it is the moment a crank stops being a required adapter and starts being a choice.

It has no target temperature. It has no thermometer. Given power it blows air; given none it stops. That is the entire device.

And that is the **only** actuator in the slice. Nothing removes heat. The bellows is the player's single verb and it points one way.

This is deliberate, and it is the slice's most productive frustration. Overshoot the window and there is nothing to do but wait, on a vessel chosen for stability and therefore equally stubborn about coming back down (§10 — thermal stops slowly whether or not you want it to). The player cannot ask for cooling. They can only stop asking for heat.

### Closing the loop

```
Thermometer → Controller → Bellows
```

There is **no controller in this slice.** There does not need to be one, and putting one here would waste it.

Beat 2 has exactly one condition to watch, and a single condition needs no logic. A **bimetallic strip** — a sensor fitting that trips at one fixed temperature — wired straight to the bellows is a thermostat, and it closes the loop with no programmable block anywhere:

```
below its trip point   →  bellows on
above it               →  bellows off
```

The trip point is set by the strip the player crafted. Changing it means crafting a different one.

This is the honest first automation, and it keeps the real controller in reserve for the moment it is genuinely needed — when one condition stops being enough (§13). Until then, and for the whole of the slice before this point, **the player is the controller.** That is the job every later tier is trying to take off their hands.

The temperature now oscillates in a band. Nothing in the system knows that 1450 Tu matters. The furnace does not, the bellows does not, the thermometer does not. **The player built the only thing that knows** (§8).

And then it does not quite work, which is the real lesson. The bellows is strong and the thermometer updates every 20 ticks, so the loop overshoots — it reads 1415, fires the bellows, and by the next reading it is at 1490. The player has good enough information and still cannot hold the condition.

That is §8's second half, delivered as a bug the player has to fix rather than a paragraph they have to read: **knowing the state is not enough; you have to be able to respond to it.**

The fixes available are all physical, per §4: a smaller bellows, a faster thermometer, a wider deadband, or more thermal mass.

### Thermal mass becomes a choice

The last of those is the interesting one, and it is where beat 2 stops being a tutorial.

```
SMALL CRUCIBLE    heats fast, cools fast, hard to hold steady
LARGE CRUCIBLE    heats slow, cools slow, very stable
INSULATION        (upgrade) slows both directions further
```

Neither is better. Steel wants 30 seconds inside a 60 Tu band, so the large insulated crucible wins — its sluggishness is the feature, and it will hold the band almost by itself once it gets there. A process that needs to *move* fast would prefer the small one.

This is §4's rule in its purest available form. The upgrade is not *Crucible Mk II, +50% speed.* It is **more mass and better insulation**, and whether that helps depends entirely on what the player is doing.

### The blast furnace never becomes obsolete

Once the loop is running, the player owns two ways to make steel, and neither one wins (§15).

|       | Crucible + loop | Crude Blast Furnace |
| :---  | :---            |                :--- |
| Speed | slow            | fast                |
| Batch | large           | small               |
| Costs | capital, infrastructure, a control loop | **standing there** |

A minute of AFK, or thirty seconds of full attention. The right answer changes depending on what else the player is doing, and it keeps changing for the rest of the game.

This is the slice's quiet thesis, and it is worth being deliberate about: **the player did not upgrade past the blast furnace. They bought the option to walk away from it.** Automation in Feedback purchases attention, not capability — which is §7's whole *possible / reliable / economical* distinction arriving as a thing the player feels rather than reads.

### The taste of path dependence

Once the player can hold a temperature, one more step is available — and it is the hook into everything after the slice:

```
HARDENED STEEL

Input       1 × Steel Ingot (at ≥ 1400 Tu)
Process     Quench
Requires    Cooling rate ≥ 200 Tu/t
Output      1 × Hardened Steel  — hard, brittle

Cooled slower → ordinary Steel Ingot. No loss, no progress.
```

The *same ingot* at the *same final temperature* is a different item depending on how it got there. Nothing about the end state distinguishes them.

This is §7's history-sensitivity, and it is the first requirement in the slice that cannot be satisfied by getting a number right. It is also real metallurgy, which is the point of §2 — the mod did not invent this rule, it noticed it.

**Built with no machine at all.** A quench tank was drafted and cut: it added a place to stand and nothing else, because the whole decision is *whether the ingot is still hot enough when it lands*, and that decision is the walk from the crucible, which already exists. So the player throws the ingot into any water, anywhere. The `cooling rate ≥ 200 Tu/t` in the spec above is not stored as a figure either — nothing in the game cools an ingot at any rate between "water" and "air", so a stored threshold would be a knob over a choice with no middle. What is stored is the temperature floor the ingot must still be above when it hits the water, which is the part the player can get wrong.

**Deliberately left dangling:** hardened steel is brittle, and brittleness is a problem.

---

## The fantastical thread

Per §2, this cannot be a footnote. In this slice it lives in the most mundane possible place: **the fuel.**

Every heat source the player meets is also a small anomaly, and none of them are explained.

**Charcoal** — mundane, and the slice's honest noise source (§8). Its quality genuinely varies, so the firebox wanders, so timers drift. The irreducible noise floor enters the game as *the fuel is not always the same*, which is both true to life and free to implement. The variance is rolled **once per piece of fuel and held for its whole burn**, which is what makes it learnable in aggregate rather than merely jittery: a batch runs hot or cool and the player can feel that and respond.

Fuel is a **datapack table**, not a constant — `(ingredient, duration, temperature, spread)`. Temperature and duration are independent on purpose, so charcoal burns hot and briefly, wood cool and briefly, coal hot and long, and *what do I feed it* is a real question with no dominant answer. A single "fuel quality" scalar would have collapsed the pair into a ladder with a correct top (§3). Vanilla burn times are deliberately **not** a fallback: a vanilla burn time counts items smelted, which is a fact about a furnace and carries no temperature, and a fire whose temperature was guessed would make every hard gate in beat 2 quietly negotiable.

**Lava** — the player has seen it since day one and never asked. It is 1200 Tu and **it does not cool.** Not slowly. At all. A bucket of it under a crucible is an infinite, perfectly stable heat source that is too cool for steel and too hot to turn off. No explanation is offered. The player just notices that the most stable thermal environment available to them is a rock that should have frozen centuries ago.

Lava carries this thread alone, and that is enough, because the payoff is the slice's exit.

None of it is characterized. There is no lore entry and no analysis machine. There is only a player who now owns a thermometer and has started pointing it at things — which is the correct first step of §2's sequence, **observe → exploit**, and the entire rest of the chain is somebody else's slice.

### The slice ends at the Nether door

Flint and steel takes **steel**. Steel is beat 2. So the Nether is not reachable until the player has done everything above, which gives the slice a diegetic ending rather than an arbitrary one: *you have learned to control heat, and the reward is a door.*

On the other side is a **Blaze Rod** — a solid object, cool to hold, that releases more heat than its mass can account for. It burns hotter than charcoal and, far more disturbingly, it burns *steadier*. It is the first thing the player's newly-built physical intuition flatly cannot accommodate, and they now own exactly the instrument required to confirm that it makes no sense.

That belongs to slice 2. It is named here only so the slice is built to arrive at it.

---

## What slice 2 opens on

Three things arrive together, and each one makes the others necessary. That is why none of them are here.

**Tempering.** Hardened steel is brittle and the fix is a third controlled thermal step — but tempering is not a *hold*, it is a **controlled cool**. The player needs to bring a temperature down deliberately, at a rate, which is the first thing in the game they have no way to ask for. **First pass built** (`TODO.md` §5): `ThermalProcess` gained one boolean, `requireCooling`, rather than a rate field — philosophy 13 leaves no continuous rate anywhere to store, so the only lever is the Damper's timing, not a number.

**The damper.** Which is the way to ask for it. A vent that bleeds heat off on demand, and the slice's one-way bellows finally gets an opposite. **First pass built**, crucible-only (matches Insulation's scope), unverified by eye — see `TODO.md` §5.

**The controller.** Because a bellows and a damper are two actuators and two conditions — *heat when low, vent when high* — and a bimetallic strip cannot express that. One condition has stopped being enough, which is exactly the moment §13 says a controller should arrive. Tier 1 built; the Damper above needed no controller-side change, since its generic actuator card already targets any `Switchable`.

Slice 1 earns all three by withholding them. The player ends it able to hold a temperature and unable to steer one, and they should feel precisely which of those they are missing.

---

## The convergence

The two beats meet in one process, which is how the slice ends:

```
STEEL PLATE

Input          1 × Steel Ingot
Process        Hot working
Temperature    900–1100 Tu   ← must hold DURING the work
Work           60 Fu
Hardness       15
Output         1 × Steel Plate

Below 900 Tu → too cold, work does nothing, hammer wears
```

**Both halves of that last line are now built**, and the second half turned out to be one
rule rather than a hammer-specific one. Force that cannot go into the work goes into the
machine: the workpiece is too cold, or the blow is under the hardness floor, or the drive is
geared past the hammer's `24 St` ceiling. All three used to be silent — the first two did
nothing at all and the third was a silent clamp — which meant the three most instructive
mistakes in the slice were the three the game said nothing about.

Wear scales the hammer's whole spec sheet, both throws and the ceiling, down to half. Nothing
breaks and nothing stops: a battered head still hits, it just puts less of the blow into the
work, so copper carries on yielding to a spent hammer and steel quietly stops clearing its
hardness floor. Which material notices first is the material's business (§7 — the failure is
soft, and it is the material that decides).

**A hammer beating air takes nothing.** Nothing resists it, so there is no shock to do the
damage — and wear that accrued on idling would be an uptime tax rather than a diagnosis.
Running empty is already answered by overrun: leave the plate there and it becomes foil, then
scrap. The punishment for walking away belongs to the workpiece, not the machine.

Nothing heats the anvil. The **workpiece carries its own heat** (§9).

```
STEEL INGOT, hot
Temperature   set when it leaves the crucible, decays toward ambient
Below 900 Tu  simply too cold to work — heat wasted, material intact
```

**There is no separate Hot Steel Ingot item, and there should not be.** Heat is two data components on the ordinary ingot — the temperature it was stamped at, and the tick that happened on — so nothing "reverts"; the figure computed from those two falls below 900 Tu and the hammer stops being able to do anything with it. That is also what makes §9's *hot wherever it is* literally true: the ingot cools in a chest, in a hopper, on the floor and in an unloaded chunk, because no code has to remember to cool it. The item budget was always right to list twelve items and not thirteen.

So the player heats the ingot in the crucible, pulls it out glowing, and carries it to the hammer with a clock running. That is what forging is, and it is the most physical thing in the slice: **you are not only the controller now, you are the conveyor.**

The clock is **linear** — a flat 1.5 Tu/t, not Newton's exponential. That was a deliberate reversal after reading TerraFirmaCraft, and the argument is game design rather than physics: an exponential never arrives, needs an arbitrary floor to stop it, and puts almost all of the interesting time in the first few seconds followed by a long flat tail in which nothing happens. A constant rate makes *"about eighteen seconds before it is unworkable, and the window opens after eleven"* a true sentence the player can plan against, which is the entire point of making the workpiece carry its heat. The rate not depending on temperature is physically wrong and creates no decision, which is the standing test.

**And forging takes several heats, which nobody designed.** The window is about 130 ticks wide and sixty Fu is thirty blows, so one heat is rarely enough — but `Fu` lives on the stack and temperature does not, so a part-worked ingot goes back in the fire and comes out to be finished. That is exactly how forging works, and it fell out of two components having different lifetimes.

Three things fall out of it immediately, none of which needed designing.

**Placement becomes a decision.** A hammer across the workshop from the crucible loses the heat in transit. The player will move the hammer next to the fire, and they will work out why without being told.

**Speed matters as well as force.** Steel is stubborn enough that even an adequate blow barely registers — a 20 St hammer against hardness 15 lands one or two Fu at a time, so sixty Fu is *dozens* of blows and all of them have to fall before the ingot drops out of its temperature window. The player needs a high stroke rate **and** the short crank, and short throw at high `RPM` is the heaviest load the mechanical network has ever been asked for. The convergence squeezes force, speed and `Su` budget at once.

Copper never taught this, because copper never resisted. A soft material hides the difference between a strong blow and a fast one; a hard material separates them and demands both.

**Failure stays honest.** Falling out of the window wastes the heat and returns the ingot, matching beat 2's asymmetry exactly (§6). The player loses a trip to the fire, not the steel.

Temperature and mechanical work, coupled, in one process.

### The strategy nobody designed

Between blows, the hammer is resetting and the steel is cooling. That dead time was invisible until the workpiece started carrying a clock, and now it is the most expensive thing in the build.

The obvious answer is brute force: short throw, high `RPM`, enough `Su` to sustain it. Buy your way out.

The other answer is **two hammers.** Strike in one, move the ingot to the second, strike again while the first resets, move it back. The player interleaves the strokes by hand and works at roughly twice the rate — on a mechanical network that never had to grow at all.

Nothing in the mod was built for this. It is a consequence of three separate facts — strokes take time, workpieces cool, and items can be moved — meeting in a way nobody arranged. That is §5's composition test paying out inside the first slice.

And notice what the two routes actually cost. The brute-force route spends **capital**: a bigger wheel, more shafting, more `Su`. The two-hammer route spends **attention**: a second cheap machine and a player willing to stand between them shuffling hot metal. Which is exactly the trade §15 put between the crucible and the blast furnace, arriving here on its own, in a completely different system, because nobody stopped it.

This is the first **coupled** difficulty in the game (§7), and it is the slice's graduation: the player is no longer running two independent systems. They are operating a process.

And it is where the linkage stops being a curiosity. Steel's hardness is 15. The hammer delivers **12 / 3** — so the long throw is hopeless, and the short throw is *still not enough.* No amount of RPM substitutes for force (§17).

Note how completely the material has inverted the lesson. On copper the gentle crank was the *right* one and the strong crank destroyed the work; on steel the gentle crank does nothing at all and force is the only thing that matters. The player does not re-learn a rule here — they learn that the rule was never about the crank.

The player needs both the short crank **and** a better hammer, and they will almost certainly try the hammer first, because that is what every other mod has trained them to do. Discovering that the expensive new machine also needs re-gearing is the lesson landing properly: **the equipment was never the whole answer.**

That is §14's difficulty curve in one component. The new material did not demand new equipment so much as it demanded understanding the equipment.

---

## Budget check

**Items (12):** Copper Plate · Copper Foil · Copper Scrap · Steel Ingot · Burnt Iron · Hardened Steel · Steel Plate · Hand Hammer · Calipers · Thermometer · Bimetallic Strip · Shaft
*(Copper Ingot, Iron Ingot, Charcoal and Lava are vanilla. Blaze Rod is slice 2.)*
*(The Hand Hammer was on this list from the first draft and was the last of the twelve to be built. The Bimetallic Strip is a block rather than an item as built — see below — so the count holds only because the Hand Hammer replaced it.)*

**Machines (7 new):** Hand Crank · Water Wheel · Crank Linkage (short/long throw) · Mechanical Hammer · Crucible (small/large/insulated) · Firebox · Powered Bellows · Timer
*(No controller and no damper. One actuator, one condition — see §13 and "What slice 2 opens on".)*
*(Furnace, Smoker and Crude Blast Furnace are reworked vanilla blocks, not additions — see §15.)*

**Process variables (2):** `Fu`/`St` mechanical work · `Tu` temperature

**Added since, over budget and deliberately (3):** Small Cog · Large Cog · Gearbox
*(§17 gave the force problem a second answer — re-gear the drive rather than fit a shorter throw — and an answer the player cannot buy a part for is not an answer. Cogs are also what make the Mechanical Hammer's `St` ceiling mean anything: without gearing, no drive ever reaches it. The gearbox is the smallest thing that lets a gear train turn a corner, which a shaft-only factory otherwise forbids.)*

**Beat 2, as built, differs from the roster above (4 blocks over):** Firebox · Small Crucible · Large Crucible · Insulation · Powered Bellows · Bimetallic Strip.

The draft counted "Crucible (small/large/insulated)" as one machine. It is three blocks — two crucibles and an insulation block — and that is the right shape rather than padding: §4 wants an upgrade to be a physical component you could point at, and insulation is the most literal possible case. You build it *around* the vessel, the vessel counts its neighbours, and how well it works depends on how much of it you covered. There is no upgrade slot, no tier and no percentage.

The Bimetallic Strip was a fitting in the draft and is a block here, for the same reason the thermometer stopped being one: fittings are a system the slice does not otherwise have, and inventing one to hold two items would be a system built for its own sake.

Within §20's budget as drafted, and seven blocks over it as built. Nothing in it exists to pad the tech tree.

---

## What the slice teaches

Mapped to the philosophy, because if a beat teaches nothing it should be cut:

| Lesson | Where it lands | Philosophy |
| :--- | :--- | :--- |
| Machines perform operations, not recipes | Hammer has no recipe list | §4 |
| Machines do not know when to stop | Plate → foil → scrap | §1, §6 |
| Overrun is sometimes another process | Foil is useful | §6 |
| Manual production is real but miserable | Five hammer swings, per plate, forever | §7 |
| Timers automate before precision exists | The plate timer | §8 |
| The recipe is not locked; the process is hard | Steel, attempted blind | §7 |
| Ignorance is paid in raw material | Six stacks, two ingots | §7 |
| Measurement is separate from control | Thermometer controls nothing | §8 |
| Knowing is not enough — you must respond | The overshooting loop | §8 |
| Upgrades are physical | Thermal mass, insulation | §4 |
| Better machines improve old work | 20 St hammer trivializes copper | §4, §14 |
| Process history matters, not just state | Quenching | §7 |
| Infrastructure has real limits | 256 Su, 80 Su per hammer | §10 |
| Distance is free; sprawl is not | Shaft friction rises with speed, so a long run finds a lower top speed | §10, §17 |
| Noise is real and learnable | Charcoal quality | §8 |
| Strange things are useful before understood | Lava | §2 |
| Senses give adjectives; numbers cost instruments | Glow vs. thermometer, flattening vs. calipers | §8 |
| Revealing beats refining | Calipers refine; the thermometer reveals | §8 |
| You are the controller until something replaces you | No logic block in the whole slice | §13 |
| Motion has a shape, and force is geared not bought | Crank throw sets St on hammer and bellows | §10, §17 |
| The strong setting is not the good setting | Short throw skips plate entirely and makes foil | §3, §6 |
| Gearing selects a product, not a speed | Same hammer, same ingot, different output | §4, §6 |
| Momentum is a thing you build with | Spin-up, coasting, and a network too loaded to start | §9, §13 |
| Material carries state between machines | Hot ingot cooling in transit | §9 |
| Attention and capital are interchangeable | Two hammers vs. a bigger wheel | §5, §15 |
| Specialization is emergent, not declared | Smoker ceiling, blast furnace floor | §15 |
| Automation buys attention, not capability | Crucible vs. blast furnace | §7, §15 |
| A free sense has a range, like any bought one | Iron's spoil point is above the hottest colour the eye resolves | §8 |
| Forging takes several heats | `Fu` persists on the stack; temperature does not | §9 |
| A loop's period is a physical property | The strip reads every 20 t, so vessel mass decides whether that is fast enough | §8, §13 |

Twenty-nine lessons, twelve items, eleven new blocks, one actuator, and no controller. Every one of the philosophy's load-bearing ideas appears at least once, in play, without a single tooltip explaining it.

The last seven were not in the original draft. They arrived while the slice was being built, out of rules that were already there — which is §5's composition test passing on the mod's own design process, and the reason the slice is written to be implemented early rather than finished on paper.

The last three are worth singling out, because all three came from *simulating the numbers rather than reasoning about them*. The eye's range mattering was not visible until the adjective bands had boundaries; "forging takes several heats" fell out of `Fu` and temperature having different lifetimes, which nobody arranged; and the thermostat's period turned out to be the thing that decides whether a small crucible can make steel at all. The last is the sharpest: a large crucible swings 16 Tu between readings and a small one swings 159 Tu and peaks past the temperature that burns the batch. The *same loop*, on the *same fire*, works or destroys depending only on how heavy the pot is.

---

## Open decisions

- ~~Vanilla furnace~~ **Resolved** (§15) — reworked, not removed; beat 2 rebuilt around it.
- ~~Where the Controller comes from~~ **Resolved** (§13) — a tape writer and a tape reader, punched card in the early game. The cheap sensor being a boolean is now concrete too: it is a piece of bent metal that trips at one temperature.
- ~~Number or needle~~ **Resolved** (§8) — free senses report adjectives, instruments report figures, and each tier buys significant figures.
- ~~`Fu` progress display~~ **Resolved** (§8) — visibly flattened for free, `16 / 20 Fu` with calipers.
- ~~Blaze Rod availability~~ **Resolved** — flint and steel takes steel, so the Nether closes the slice and Blaze opens slice 2.

Still open:

- ~~What does a punched tape hold?~~ **Resolved** (§13) — everything a late-game controller can. Capability never tiers; only iteration cost and source count do.
- ~~Does the slice need a controller?~~ **No.** A bimetallic strip wired to the bellows carries beat 2; the tape pair belongs to slice 2.
- ~~Does the damper survive?~~ **No.** It is a second actuator, and a second actuator is what a controller is for. Both move to slice 2.
