# Feedback — Design Philosophy

> **Document 1 of 3.** *Why* document. State what Feedback is, believes, which rules any proposal must satisfy. Stop short of mechanism on purpose.
>
> - **1 — Philosophy** *(this document)*: identity, principles, constraints. Worked examples only where they prove principle real.
> - **2 — Mechanics**: how each principle implemented. Units and arithmetic, sensor/actuator matrix, thermal model, energy networks, overrun band tuning, control node set.
> - **3 — Content**: roster. Every item, material, machine, fitting, process, what each does.
>
> Separation load-bearing, not tidiness. Mix together, philosophy get revised every time hypothetical copper furnace make no sense.
>
> Supersedes `feedback_notes_i.md` and `feedback_notes_ii.md`, write-ups of earlier design talks. Where the two disagreed, resolved here.
>
> **Status:** principles settled enough to constrain decisions. Numbers, names, rosters not settled, mostly not exist yet. Genuinely open questions marked **[OPEN]** inline, collected in final sections. Adding `[OPEN]` is legitimate design outcome; quietly resolving one is not.
>
> **Document revised as mod built. Intended.** Model surviving a year of playtesting unchanged mean either imagined perfectly before anything existed, or nobody watched what thing felt like. Building reveals errors reasoning never finds — §17's force and work treatment wrong until hammer existed to be wrong about — so when implementation contradicts document, document changes. What not change casually: **identity** in §1 and §2. Those are what mod *is*; revising them make a different mod.
>
> Corrections recorded, not tidied away. Section saying *got this wrong once, here why tempting* more useful than one always right, because error usually the reusable part.

---

## 1. What Feedback Is

Feedback = Minecraft technology mod about **building and controlling physical processes**, not unlocking recipe-specific machines.

Central mechanic, one sentence:

> **Machines do not recognize when a recipe is complete.**

Machine keeps performing physical action — heating, spinning, agitating, reacting — as long as it has input and power, even when output already finished. Nothing inside knows difference between "working" and "done."

Player has two separate problems, must solve both:

1. **Make the process happen.**
2. **Notice desired state reached, stop or redirect process.**

Every other system serves one of those two.

### What is actually novel here

Be precise about claim; nearby claim already taken. GregTech 6 has machines that idle and burn fuel forever unless manually covered or unplugged. "Machines that don't switch themselves off" not unclaimed.

Novel part is what happens next:

> **Continued operation past completion acts on the already-finished output**, not merely on the energy bill.

Output is *perishable to continued processing*. Overrun doesn't waste a process — it consumes the thing process just made. No existing tech mod does this. It is the differentiator. Keep this framing when describing mod to anyone.

### What the mod is not

Not a tech tree where each tier unlocks stronger machine doing same job. Progression in Feedback = growing ability to **observe, manipulate, control, scale, reproduce** physical processes. New tier that only makes same operation faster advanced nothing.

---

## 2. Scientific, Not Realistic

Minecraft's rules treated as actual laws of nature.

If strange thing demonstrably happens in Minecraft, it is **real phenomenon in setting**, potentially industrializable. Blazes, Ender Pearls, lightning answering Channeling trident, Creepers, Chorus Fruit — not fantasy exceptions bolted onto physical world. They *are* the physical world, nobody industrialized them yet.

Deliberate departure from TFC and GregTech, which mostly strip or route around Minecraft's fantastical elements for real-world physics. Feedback does opposite. It asks:

> **How would a scientifically-inclined mind industrialize this?**

Goal: new technology feels like **diegetic discovery** — something world always permitted, nobody built — not tech layer dropped on fantasy game.

### Real science is the method, not the target

Mod borrows scientific *habits*: measurement, controlled experiment, repeatability, process conditions, characterization, instrumentation, scale-up, engineering. Not science's completeness. Variable belongs in Feedback because it creates interesting engineering decision, never because reality has it. (See §3.)

### Magic is baked in from the beginning

Minecraft-exotic phenomena surface wherever player understands enough to notice, measure, exploit — not quarantined into early gimmick or final "magic tier."

Player does **not** need to understand phenomenon before exploiting it. **Recognizing and understanding are different events; first enough to act on.** Scientist handed Blaze Rod does not say *no theoretical framework, ignore.* They say *this incredibly hot — what use?*

Full pattern:

> **Observe → exploit → characterize → control → reproduce → engineer**

Blaze Rod model case. First Blaze tech can be genuinely primitive — Blaze Rod plus crude heat exchanger gives dramatically hotter working fluid, player has no idea why. Enough. Later discover it holds heat unusually well when insulated; later still measure emission rate, find relationship between state and temperature, stumble on bizarre interaction with another material, eventually understand enough to manufacture phenomenon deliberately.

Iron need not walk whole sequence; humans already understand iron. Blaze material, redstone, Ender phenomena, Sculk, Nether materials all can. Here compendium earns place — not as pile of late-game explanations, but as **reservoir of hypotheses about what engineer might notice** handling Minecraft's stranger materials. Natural Blaze Rod not disposable early fuel — existence proof motivating whole branch of thermal engineering.

Guiding question for any exotic material:

> **What does this impossible thing actually do, and what can we build once we understand it?**

### The compendium's role

User's own *A Speculative Physics and Biochemistry Compendium of Minecraft (Vanilla and Modded)* (`docs/speculative_physics_inspo_doc.md`) = **mindset reference and parts bin, not canon.**

Feedback not obliged to follow its lore. But its reasoning style — given this demonstrably happens, what smallest departure from known reality makes it work? — is exactly style mod should apply to own content. Being user's own work, ideas may be lifted wholesale. Several already have: Liquid Teleportant processing chain, and Redstone treated as deliberately flagged exception sitting *alongside* real periodic table, not forced into invented chemistry or dismissed as magic.

---

## 3. Priorities, and the Anti-Simulator Constraint

**Mechanical depth > playfulness > realism.** That order when they conflict.

Feedback abstracts aggressively wherever extra realism gives no interesting decision. Test for including any mechanic, variable, unit:

> **Does modeling this create a meaningful engineering choice for the player?**

If not, abstract away. "Real engineering has this variable" not an argument. Neither "more accurate." As scope constraint:

> **Feedback models a phenomenon when doing so creates an engineering decision — not because the phenomenon exists in reality.**

Mass earns place because density creates real separation problems. Pressure because it creates apparatus tradeoffs. Heating *rate* because thermal history matters. **Entropy does not**, nobody decides by entropy — nor, so far, enthalpy, specific heat, viscosity, field strengths, reaction constants. Electrical resistance is standing example of rule applied rather than discussed (§17).

System built from gameplay outward, reality as inspiration and consistency check, not specification.

Constraint has teeth, because premise makes it very easy to accidentally build process-engineering simulator nobody wants. Guardrails:

- **No vector physics.** Mechanical model scalar: how much, how fast, how strong, and — where mechanism genuinely cares — which rotational direction. Not 3D force resolution.
- **No molecular chemistry.** Chemistry = useful abstraction over reaction paths and byproducts, not general reaction simulator. (See §12.)
- **No derivative-unit sprawl.** Derive, don't invent: `Tu/t` and `mB/t` suffice. No bespoke unit per rate.
- **No physics homework.** Player never needs paper arithmetic to operate machine.
- **No chores wearing a decision's clothes.** Mechanic real, well-precedented, simply **not fun** gets cut anyway.
- **Discrete options, not continuous knobs.** Where setting would be free scalar, make small set of named choices. See below.

Last one needs own statement, clause most likely argued around:

> **Some mechanics suck. Being realistic does not save them, and another mod having shipped one does not either.**

**Electrical resistance is standing example.** Real, fundamental, several mods model it — and reasoning about it tedious every time. Does not exist here (§17). Path-checking data links same call: routing cable around wall is busywork, so links pass through walls (§13), while range limit stays because *placement* is genuine choice.

Do not over-apply. Test is about **decisions, not categories.** Shafts and pipes still real blocks player places, because laying out mechanical network is genuine spatial and budgetary puzzle (§10). What gets cut is not physical infrastructure — it is unfun *behaviour* that could be simulated inside it. Keep shaft. Skip fluid dynamics.

### Options, not micro-optimization

Related trap, mod unusually exposed: mechanic can create genuine decision and still be ruined by being *continuous*.

> **Prefer a small set of named options to a free scalar.**

Crank throw **short or long** is decision. Crank throw any number 1 to 64 is spreadsheet. Difference not depth — both give control over same physical quantity — scalar version has *correct answer* sitting somewhere in it, discrete version does not.

Correct answer is the problem, leaves only two ways to play. Grind out optimum and feel time wasted on a tenth of a second, or skip it and play on **knowing** factory needlessly suboptimal. Both worse than not having knob.

Mod already works this way almost everywhere — thermal mass small or large, insulation present or absent, instruments come in tiers, controllers come in tiers — naming rule makes it deliberate not accidental. Depth should come from **stacking choices** (§5), never tuning one finely.

Plain words, applicable without re-deriving:

> **A good choice is a design decision — where to put it, how hard to run it, what to spend. A bad choice is sitting and tuning a number.**

Best ones not presented at all. Choice game *offers* is menu; choice that is **consequence of the system** is something player worked out. Friction rising with speed is not difficulty setting, no screen mentions it — but quietly means long shaft run cannot turn as fast as short one, so *slow and wide* and *fast and narrow* become two real answers to same problem. Nobody asked to pick. Choice was there to be noticed.

Also difference between this and **hard limit**, same idea done badly. Per-component throughput cap produces absurdity of generator being *too good* for downstream parts, turns engineering question into shopping question. Cost that scales gives same caution without ever refusing anything.

Goal not to simulate reality. Goal: make Minecraft's physical processes **coherent enough players can reason about them and construct own solutions.**

---

## 4. Machines Are Physical Operations

Machine defines the **physical process it performs**, not list of recipes it knows.

Machine never says:

> "I know how to make Copper Plate."

It says, in effect:

> "I apply this operation to whatever is placed in me."

Heating, cooling, grinding, pressing, mixing, pumping, separating, filtering, reacting, phase-changing, cutting. Machine = way to repeatedly apply operation under defined conditions. No opinion on what you make, and — per §1 — no idea whether finished.

### Upgrades are physical components

Upgrade must be something person could point at on machine: enlarged vessel, thicker insulation, larger flywheel, better bearings, finer screen, heat-resistant lining, better seal, precision valve, pressure-rated vessel, more capable agitator, improved heating element.

Components must make sense for that machine and respect its material limits. **"+50% throughput" is not a design concept.** Question always: *what physical change makes this machine handle more throughput?*

Two real development axes: **throughput** and **precision/controllability**. Not recipe unlocks, not voltage gates.

### Better machines improve old work

Better saw cuts copper more efficiently. Does not exist because copper became tier-3 material.

New capabilities emerge because improved equipment makes previously impractical process *economical* — not because machine got longer recipe list. Advanced machine may process larger batches, hold tighter conditions, react faster, survive more severe conditions, offer better physical basis for instrumentation. Should not be same machine in more expensive metal.

### Observation is a separate layer

Machines don't get smarter as they get better. Sensing is distinct investment (§8), delivered via covers that attach to block face without consuming block space, competing for machine's limited attachment slots alongside power and redstone I/O.

Consequence deliberate: **sophisticated machine can be difficult to control if poorly instrumented; crude machine can become remarkably reliable once player learns to measure it.**

---

## 5. Recipes Are Process Specifications

JEI assumed essential. Recipe entry is **specification of what must physically happen**, not machine-specific unlock.

Process entry communicates whatever relevant of: inputs, outputs, required operation, temperature, pressure, mechanical requirements, duration, flow or concentration, tolerances, significant rates, byproducts, any other decisive physical condition.

Recipe answers:

> **"What must physically happen to this material?"**

not:

> "Which machine do I put this in?"

Because specification physical not machine-bound, **a process may have several valid implementations.** World doesn't care what player built. Cares whether conditions met.

### The requirement vocabulary is small

Nearly every process condition fits a handful of requirement types:

**State** (1497–1501 Tu) · **Threshold** (≥ 200 Pu) · **Rate** (heating ≤ 20 Tu/t) · **Duration** (maintain 600 t) · **Total quantity** (apply 500 Work) · **Throughput** (≥ 10 mB/t) · **Sequence** (heat → hold → cool) · **Composition** (reagent ≥ some Qu/mB) · **Direction** (force along the machine axis)

Note what's absent: **"requires tier 5."** No such condition, no place to put one. Tier not something process can require — description of what player happens to be able to satisfy.

### The recipe screen should be diagnostic

Both sides in same units, so JEI can show *why* something isn't working — process requirements beside apparatus player actually has:

```
REFINED [MATERIAL]                  YOUR APPARATUS
Heat to 1499-1501 Tu                Max temperature   1350 Tu
Maintain 600 t                      Control stability ±40 Tu
Max heating rate 25 Tu/t            Measurement       ±25 Tu
Atmosphere: reducing
```

Enormously more useful than **Requires Tier 5 Furnace**, player reads exactly which capability short — and here not the one they'd guess. Furnace cannot reach temperature *and* instrument could not confirm it if it could.

### Infinite ways to skin the cat

Major design goal: Create-like compositional sandbox, pushed further toward process engineering. Give physical constraints, components, process specifications, energy systems, sensors, actuators, control logic — let player build solutions.

Usually many ways to accomplish process. **Some should be objectively bad and still perfectly valid.** Game should not prescribe one intended chain where underlying physical problem admits alternatives.

Also supplies test for whether new mechanic earns place, different from §3's. §3 asks whether variable creates decision. This asks what decision **composes with**:

> **The more choices each system offers, the more they stack into things nobody designed. That is the difference between a tech mod and a science mod.**

Tech mod hands player sequence of better machines. Science mod hands parts whose interactions it never fully enumerated, lets them find combinations. Judge every mechanic partly on how many other mechanics it stacks with — system composing with three others worth more than one twice as deep composing with none.

### A problem with one answer is not a problem

> **An automation problem with a single correct answer is not a problem. It is a request that the player build somebody else's design.**

Sharpest form of everything above. Puzzle with one solution = construction manual with steps hidden; solving teaches nothing except what designer thought. Interesting question never *what is the answer* — it is **what do you value more.**

Feedback should keep producing situations where two approaches both correct and cost different things: capital against attention, throughput against reliability, capability against ability to walk away. Not because balance demands, but because only kind of question worth asking someone building factory.

### Emergent trades are the design's own test

Best such situations are ones nobody placed.

Crucible and Crude Blast Furnace trade capital against attention because that trade deliberately designed (§15). Two hammers versus bigger water wheel make **same trade**, nobody designed it — falls out of strokes taking time, workpieces cooling, items being movable, three unrelated rules meeting where nobody looked.

Gives project a check it can run:

> **When a trade the mod already believes in shows up somewhere it was never installed, the systems are right. When every interesting choice has to be placed by hand, they are not yet.**

Watch for deliberately as more systems added. Mechanic producing one of these worth more than its own depth suggests; mechanic never producing one — however elaborate — probably sits alone.

Three players making same material:

- **A** builds elaborate closed-loop furnace, precise sensors, automatic fuel control. Best throughput.
- **B** builds deliberately oversized furnace, crude thermostat, enormous thermal mass, sheer inertia holds temperature. Cheap, surprisingly robust.
- **C** babysits weird little experimental kiln by hand. Almost no infrastructure, half their life spent standing next to it.

**All three work.** Feedback should never ship a *[Material] Refinery™* — single block that is the answer. Ships components and constraints; player designs system. Crucially costs nothing to support: requirements independent of machine identity, so mod never enumerates valid combinations. Only checks conditions.

---

## 6. The Core Loop: Idle, Running, In-Window, Overrun

Every machine moves through four conceptual states:

- **Idle** — loaded, powered, waiting.
- **Running** — consuming energy, performing action, progress accumulating.
- **In-window** — desired result technically exists. Machine not told to stop.
- **Overrun** — continued action now acts on finished product.

### The severity ladder

Overrun not single "ruined" outcome, not always friendly sidegrade. Both extremes must exist:

| Tier | Outcome |
| :--- | :--- |
| **Sidegrade** | Different, still-useful output. Usually narrow band; hitting deliberately requires understanding machine. |
| **Degrade** | Same output, worse yield or stats. The "I was a little late" result. |
| **Spoil** | Total loss. No byproduct, no partial credit. |
| **Hazard** | Overrun damages machine, or produces something actively dangerous. |

**Ladder not specific to overrun.** It is outcome model for *getting a process wrong in any direction.* Underheat recipe and same four outcomes on table: nothing happens, material degrades, ruined outright, or something dangerous occurs. Overrun simply most interesting case, because player already had wanted thing and processed it into something else.

**Band width is tuning knob, mod's main difficulty curve.** Varies per machine — forgiving machine has long degrade band, no hazard band; exothermic reactor or pressurized vessel might jump almost directly from in-window to hazard — but far more importantly varies **per material, and tightens as game goes on.**

Copper forgiving. Very few ways to actively destroy copper; player mishandling mostly wastes time. Late-game material can be ruined by breathing on it wrong — fair, **because by time player handles it, they should know better.** Forgiveness is what gets scarce.

Far better difficulty curve than gated recipes, never tells player no. Quietly raises standard of care, and player's growing instrumentation keeps pace — which, as §14 argues, means game doesn't actually get harder. Also §7's economics in another form: tightening bands turn wasted raw material from early annoyance into serious cost.

GregTech 6 gives precedent for top of ladder being real: boilers genuinely explode with real blast radius when steam flow over-restricted.

### Overrun is sometimes just another process

Grinder taken past completion produces **coarse → fine → powder**. Not damage; legitimate technique for finer material, paid for in yield.

Strongest possible confirmation of §4. If machine performed discrete recipes, "keep grinding" meaningless. Because it performs physical operation, continued operation naturally walks material further along real state axis — whether that counts as overrun depends entirely on what player wanted.

Early sketches to build from:

- **Furnace** — past completion keeps absorbing heat. Narrow band work-hardens product into harder, more brittle variant (sidegrade); beyond it, slag (spoil).
- **Mixer** — overmixing shear-thins mixture, separates it back into components, or whips in air giving foam that feeds different chain (sidegrade).

---

## 7. Possible, Reliable, Economical

Most important progression rule in mod.

A **process has requirements.** An **apparatus has capabilities.** Recipe not locked merely because player lacks right apparatus. Instead distinguish three questions:

- **Possible** — can player physically cause this at all?
- **Reliable** — can they cause it consistently enough to depend on?
- **Economical** — can they cause it at sane rate, cost, maintenance burden?

Demanding process may be technically possible with crude equipment, manual work, guesswork, enormous attention. **Player should be allowed surprising things early if willing to suffer.**

> **The recipe is not locked. The process is difficult.**

### Hard gates and soft gates

> **A hard gate is physical impossibility. A soft gate is inability to reliably control.**

**Hard gates** — insufficient `St`, insufficient total `Work`, insufficient max temperature, insufficient pressure rating, insufficient flow capacity, wrong material entirely, vessel and material limits. Apparatus simply cannot reach required condition; no patience changes that.

**Soft gates** — inadequate measurement accuracy, coarse resolution, slow response, poor control granularity, unstable process, high sensitivity to disturbance. Apparatus can reach condition. Player cannot reliably *keep* it there, or cannot confirm they did.

Distinction matters enormously: stops "tier 5" creeping back through side door. Resolves apparent contradiction: **precision genuinely gates progression, and precision is never a lock.** Gates by making things unreliable and uneconomical, soft gate by definition.

Sharpest form:

> **An apparatus does not need to lack physical capability to be gated. It can lack sufficient knowledge or control.**

Worked example. Process needs 1499–1501 Tu. Furnace can physically sit at 1500 Tu all day. But thermometer resolution is 10 Tu, so you can't tell 1499, 1500, 1505. Heat not the problem. **Knowing** is the problem — fix is instrument, not bigger fire.

Note what soft gate does *not* do. Does not make process fail. If material really at 1500 Tu, **it works** — world doesn't consult your thermometer (§8). What you lose is not success but **reproducibility.** You can absolutely stumble into demanding process by luck, hand-hump startling amount of mod that way. Doing it second time on purpose takes much more luck, or better instrument.

Soft gate is therefore a **reproducibility gate**, which is why it belongs in *reliable* column, not *possible*.

### The gate is almost never reaching the condition

State bluntly, inverts how tech mods normally work: **difficulty rarely getting material where it needs to be. It is knowing how to get it there, and doing it again.**

Even miserable furnace with right fuel can get hot enough to bake almost anything. Heat never obstacle. Without thermometer, player has no way to know if window hit — finds out only way left, throwing things at wall and looking at what comes back.

Honest description of early brute-force attempt at demanding material:

> **You can absolutely make the hardest ingot in the mod. You will start with six stacks of raw material and end up with two ingots — unless you put real work into the *how*.**

Nothing locked. Recipe available whole time. Player simply paid for ignorance in raw material, correct currency for it.

Two things follow, both design requirements not observations.

**Failed batches must consume their inputs.** If failure cheap, brute force optimal and instrumentation a hobby. Waste *is* the gate. Only thing between "you may attempt anything" and "you may as well attempt everything."

**Raw material abundance is therefore per-material tuning knob** — far better than tech lock. Process on abundant input stays brute-forceable long; same process on scarce thing punishes guessing immediately. Designer sets how accessible process is by deciding what it eats, never who may attempt it.

Cleanest reading of whole system: **instrumentation is a yield technology.** Thermometer not a key, unlocks nothing. Bought because it turns six stacks into two ingots into six stacks into most of six stacks, pays for itself in conversion ratio like real capital investment.

### The control envelope

Every process occupies region in multidimensional space: temperature, pressure, composition, flow, time, atmosphere, force, whatever else relevant. Some regions enormous. Some pinholes.

> Copper: 190–230 Tu, any ordinary heating rate, almost any holding time.
>
> Something demanding: 1497–1501 Tu, specific atmosphere, specific heating history, 600 ± 20 t.

Player's equipment determines size and shape of region they can actually command — from *I can probably keep this vaguely hot*, to *I can hold approximately 1500 Tu*, to *I can hold 1499 ± 1 Tu as long as you like.*

**Recipes never get higher-tier. Player's reachable precision grows.** Progression is expansion of control envelope; "what tier am I?" better asked:

> **What level of physical reality can I reliably command?**

Consequence: process has **feasibility profile** not locked/unlocked bit. Same process might be *technically possible* by hand, *extremely difficult* with crude machine, *feasible* once instrumented, *efficient* once automated — cleanly separating **access** from **viability**. If player assembles unreasonable laboratory genuinely satisfying requirements far ahead of schedule, **mod should let them.** Real high-skill route through progression, not exploit.

### Difficulty is multidimensional

"Hard" not one thing. Process can be hard because:

- **Narrow** — very small acceptable range.
- **Fast-changing** — conditions must be adjusted quickly.
- **Long-duration** — conditions held for long time.
- **Coupled** — changing one variable moves another.
- **Poorly observable** — state that matters isn't directly visible.
- **Out of reach** — can measure fine; apparatus cannot get there. (Only kind that is hard gate.)
- **Unstable** — runs away unless actively controlled.
- **History-sensitive** — result depends on *how you got there*, not merely where you ended.

Last deserves emphasis: makes instrumentation matter beyond hitting a number. State-based process says *get this to molten temperature.* Path-dependent says *heat at roughly this rate, reach this temperature, hold, then cool no faster than this.* Second cannot be satisfied by luck, ties process requirements directly to thermal memory in §9 — without labelling anything "advanced recipe."

### Manual production stays possible

Most processes should remain theoretically reproducible by hand given enough knowledge, tools, workspace, attention. Manual rates tiny — one item at a time, or occasional success — so automation becomes necessary for *practical scale* rather than because recipe metaphysically machine-only.

"100% Feedback, no automation" run — call it Amish% — should be funny, miserable, technically viable for surprising share of game. Costs exactly as they should be: negligible throughput, constant attention, poor repeatability, no scaling, high waste.

Automation is therefore **a way of making physical processes practical, not a key that unlocks recipes.**

---

## 8. Measurement, Control, and Actuation Are Separate

Arguably central architecture of mod. Four distinct things exist, player assembles connections between them:

- **Process physics** — machine changes state per inputs and losses.
- **Measurement** — sensor observes some aspect of that state.
- **Control** — controller interprets measurement, decides what should happen.
- **Actuation** — actuator changes the physical system.

Worked example, whole philosophy in one line:

> **Thermometer → Controller → Heat Pump → Furnace**

Furnace has no target temperature. Heat pump has no target temperature. Thermometer controls nothing. Controller doesn't magically know what temperature process wants. **Player builds the feedback loop**, and resulting behavior — temperature oscillating inside chosen band — exists nowhere in components, only in assembly.

Generalizes past loop, close to thesis of entire mod:

> **No part holds all the wisdom — not even the controller. A machine is far more than the sum of its parts, because none of the parts work alone.**

Vessel doesn't know recipe. Sensor doesn't know what matters. Actuator doesn't know why asked. Controller doesn't know target. Every component deliberately, permanently ignorant of thing it participates in; competence belongs to arrangement. **Player is only thing in system that understands what is happening**, and that is not limitation of design — it is the design.

Without loop, same job done by hand: heat, observe, adjust, wait, observe, adjust. Entirely possible. Entirely tedious. That contrast is the mod.

### Accuracy vs. precision

Keep distinction explicit; players conflate, mod depends on difference.

- **Accuracy** = property of *instrument*: how close reported measurement to real state.
- **Precision** = property of *whole assembled system*: whether it can obtain good enough information and respond fast enough to hold desired state.

Therefore both true:

> If you cannot sense something, you cannot precisely control it.
>
> If you can sense it accurately but cannot react quickly enough, you still cannot precisely control it.

**Precision is emergent.** Never stat printed on machine, never recipe requirement.

### The world resolves on truth, not on readings

**Process succeeds or fails on actual physical conditions. Player's instruments never enter that calculation.** Sensor is window, not participant. Nothing in mod computes outcome from displayed value, no process cares how well it was watched.

Two consequences, everything else here depends on them.

**Luck is real and legitimate.** If vessel genuinely sat in window, batch works — whether player planned, guessed, or had no idea it happened. Player with crude equipment and enormous patience can hand-hump through surprising amount of mod by accident. Feature, §7's *possible* column doing its job.

**Variation is real, and some genuinely random.** Much of what player experiences as noise is own missing information — fuel quality, wear, ambient conditions, batch load, preheat are real hidden variables (§9) that instrument can turn into facts. Not all, and difference deliberate.

There must be **irreducible noise floor**. No apparatus, however good, reaches zero variance.

Most important tuning constraint in mod, exists to protect the second gimmick. If sufficiently well-built factory can become genuinely deterministic, correct endgame is measure everything once, write down numbers, run whole base on timers forever. Sensing becomes scaffold player discards. **Whole measurement-and-control layer would be phase rather than system.**

Noise makes instrumentation *permanent*. Single reading goes stale, so player can't measure once and schedule forever — must keep watching, the behavior mod is actually about.

Also honest. Real processes not meaningfully predictable in every part; cleanrooms approaching it exist, extraordinary and expensive achievements, not default state of workshop.

And cheap, which matters. Simulating dozen hidden variables to produce wobble player can't distinguish from `/dev/urandom` is large computation buying nothing. **§3's test applies to randomness exactly as to everything else:** model variable when player can act on it, let rest be noise. Preheat worth simulating because player can preheat. Thousand small things making real furnace wander not, no decision on other end.

Noise must **not** be arbitrary. Variation must be *learnable in aggregate* even where unpredictable per-instance: knowable center, spread responds to conditions player can influence, better equipment **narrows distribution without ever collapsing it.** Player should be able to say "this runs hot when fuel is poor" and be right, while never able to say exactly how hot this batch will run.

> **Better equipment buys a tighter distribution, never a guarantee.**

### Senses give adjectives. Numbers cost an instrument.

One rule covers every measurable quantity:

> **The player's own senses report qualitatively and for free. Numbers must be bought.**

No instrument, vessel reads *cold · room temperature · hot · really hot!!!* — genuinely useful, genuinely imprecise, exactly what person standing next to furnace knows. Part-worked ingot looks visibly flattened. Nothing hidden, nothing numeric.

Instruments convert adjectives to figures; each tier buys **significant figures**:

```
no instrument   "really hot!!!"
thermometer I    1000 Tu
thermometer II   1250 Tu
thermometer III  1256 Tu
thermometer IV   1256.3 Tu
```

Above those sits different kind of instrument: one reporting **change over time** not state — temperature climbing at 18 Tu/t. Not luxury tier. §5's requirement vocabulary contains *rate* requirements; player with no rate readout can't see whether violating one. **Delta instrument makes entire class of process requirement playable.**

Rule generalizes past temperature, no new machinery. Mechanical work qualitative for free — ingot looks flattened — numeric only with **calipers**, turning *kinda flattened* into `16 / 20 Fu`. Pressure, concentration, rest follow same shape.

### What you need is free. What you have is a cost.

Adjectives rule needs boundary, or collapses into hiding things for own sake. Boundary:

> **Knowing what a process requires is free. Knowing what your equipment is currently doing is not.**

Recipe is published data. Steel wants 1350–1410 Tu held 600 ticks, heated no faster than 25 Tu/t — handbook figure, every real metallurgist has it. Nothing to discover, nothing to gate. Recipe browser should state it in **exact units**, no instrument owned, nothing unlocked.

What costs is other half: *is my crucible in that band right now?* Requirements public; **state is measured**, measurement is what instruments are for.

Cleanest statement of §7 in document. Recipe never locked. Player can read exactly what steel needs day one, walk to furnace, still fail thirty times — knowing target and knowing where you are different problems, only second for sale.

Also settles whole information layer, otherwise argued case by case:

| | Free | Costs |
| --- | --- | --- |
| **Mechanism** — how a thing works | Ponder scenes, §8 above | — |
| **Requirement** — what a process needs | Recipe browser, exact figures | — |
| **Equipment spec** — what a machine can deliver | Printed on the block (§17) | — |
| **State** — what is happening right now | Adjectives only | Figures, per instrument tier |

Everything player could learn from manual is free. Everything learnable only by *looking at own factory* must be instrumented. Tooltip may say workpiece needs 14 Fu; only calipers may say it has 9.

### Instruments never lie

**Instrument may never display more figures than it can stand behind.** Device accurate to ±25 Tu reads `1250 Tu`, never `1256.3 Tu`. Display resolution bounded by accuracy, always.

> **A vague but entirely true answer beats a confident, smiling lie.**

Not only fairness rule; keeps cheap instruments *worth owning*. Crude thermometer reporting honestly at low resolution is real tool player keeps using all game. Crude thermometer reporting confident nonsense not worse tool — worthless, and teaches player to distrust entire instrument layer mod is built on.

**Miscalibration is not a lie, remains available.** Instrument drifted through wear (§9) is different: not claiming false precision, real physical object that aged, honest about how finely it resolves. Simply *offset*. Legitimate, and interesting exactly where fake precision isn't, because player can detect, reason about, fix it — by checking instrument against known physical reference, real and satisfying thing to do. Line:

> **An instrument may become wrong. It may never pretend to be more certain than it is.**

### Every quantity has a truth tier

**Decided (user ruling, 2026-10-20):** every measurable quantity has top precision tier reporting exact figure simulation holds, **whether or not any player-makeable instrument reaches it yet**, and every display of that quantity must be able to print it. Debug helmet reads every quantity at that tier, as do any figures no quantity covers yet (modular item's balance and swing, coat's thickness).

Not a player tier — §8's noise floor still says no purchasable apparatus reaches zero variance. Property of the *display*; needs stating because of how it was got wrong. Helmet always claimed perfect resolution, but display was written for the one instrument reading its quantity: hardness printed as scratch files' ladder, mass to balance's tenth of a Mu, machine's condition only as adjective, rest of tool's baked figures not at all. So helmet, a development view of truth, **silently inherited weakest instrument's precision**, nothing said so. Tier existing in interface but not display is not a tier. Rule also keeps instrument ladder honest from top: new instrument is coarser rounding of figure that already prints, never first time anyone sees it.

### Words are defined, and the definition is free

Every adjective mod shows stands for band of one figure — *head-heavy* is balance over 0.85, *bright red* temperature between two named lines, *straining* load over share of capacity. Holding Shift over tooltip says so, in figure and threshold that decide it, read from same constant word is judged by so two cannot drift. Hands over no state, only what word means: spec sheet of a free sense, same status as recipe's band (*what you need is free*, below).

### Some quantities are visible; some are not

Instruments don't all do same job, because quantities measured not equally observable to begin with.

Part-worked ingot **visibly** part-worked. Anybody sees it flattening, roughly how far along, whether gone too far. Mechanical deformation announces itself.

Temperature doesn't. Looking at furnace tells player there is fire somewhere. Entire content of observation, nearly useless.

So two instruments differ in kind:

- Where quantity already visible, instrument buys **precision** — calipers turn *kinda flattened* into `16 / 20 Fu`. Useful for *aiming*, not avoiding disaster, since player could already see disaster coming.
- Where quantity invisible, instrument buys **existence** — thermometer is difference between number and nothing at all.

Should drive how urgently each instrument needed, in what order wanted. **Instrument that reveals bigger event than one that refines**; design should not treat as same beat.

### Some readings are free; some cost an action

Second split, cutting across first, about *object being measured* not quantity.

An **item** should never require action to read. In player's hand or chest, holding instrument is enough; demanding right-click on top is keystroke with nothing inside — §3's test, applied to mechanic real, well-precedented, simply not fun. Calipers work by being owned.

A **machine** differs, and not as flavour. Measuring machine means getting down and putting instrument on it, and — part making it mechanic not mood — **you cannot measure a machine that is running.** Reading costs throughput. Player stops line, learns something, restarts, §5's trade somewhere nobody installed it: attention and downtime bought information, player preferring to keep producing may decline to know.

Gives rule, about price not kind of object:

> **A reading is passive when taking it is free, and an action when taking it costs something.**

Item readings free so passive. Machine readings cost stopped machine so act. Nothing decided per instrument, same calipers do both — the point, *one measurement* pointed at two things. Worn hammer head has mushroomed: wider and shorter than before, and reading how far metal deformed is exactly what calipers already do to workpiece. Tool not given second use. Pointed somewhere else.

**Precedent is the Thaumometer**, got this exact split right years ago. Point it at things in world to scan them, with right addons stop having to scan items — anything picked up scanned automatically. Nobody felt lost content, because pointing scanner at something in world is *fun* and remembering to swap hotbar before opening inventory is not. Rule above is that observation with price on it; price tells two cases apart.

**Wear has no unit, deliberately.** Plain percentage — 100% is machine as built, falls. §17's units exist so quantities cannot be silently converted into one another; ratio of machine to own former self is not that kind of quantity, minting `Wu` would invent unit to describe *a lack of* one. Also the one dimensionless figure in mod, worth stating once so nobody fixes it later.

Reads as **condition rather than wear** — how much machine left, not how much gone — because that figure multiplies spec sheet directly. Hammer at 72% delivers 72% of force stamped on it, no mental inversion between reading and decision. Note number bottoms out above zero, since spent machine still a machine: rescaling display so spent read 0% looks tidier and destroys only property making figure useful.

Snapshots not remembered for player, decision not omission — calipers report what is true when you click, nothing keeps it. Remembered reading is a *device*, should be sold as one if ever worth having; short of that player writes figure in book and quill, same mechanic with better handwriting.

Corollary matters as much: reading taken by act is **snapshot**, snapshots go stale. That is where drift and recalibration live, honest reason machine's condition shouldn't simply appear on HUD forever — player knows what hammer measured last time stopped, not what it is now.

And here gap says something rather than merely being realistic. Wear accrues only on misuse — two exceptions are consumables by nature, grinding wheel's grit and hand tool's durability, spent by correct use like fuel, parts not machines — so **correctly built line's reading never goes stale at all** — measure once, figure stays true indefinitely. Badly built one goes out of date quickly. How fast player's knowledge rots proportional to how wrong factory is, same argument wear mechanic already makes, made second time by measuring it.

### Precision is not a unit — it is a set of apparatus properties

Keeps previous point from being slogan. Precision not one number, must not collapse into one. For any variable, six properties describe what equipment can do with it:

- **Range** — can you physically reach this value?
- **Resolution** — can you distinguish values this close?
- **Accuracy** — when instrument says X, how close to reality?
- **Control** — can you deliberately hold X where you want?
- **Response** — how quickly can you change X?
- **Stability** — how far does X drift when you stop intervening?

Crude thermometer reads 0–1000 Tu, ±25 Tu accurate, 25 Tu resolution, updating every 20 t. Late one reads 0–3000 Tu, ±0.5 Tu, 0.1 Tu resolution, every tick. Neither "tier 1" or "tier 5." Instruments with different capabilities, and **these six properties probably describe most of progression of entire mod.**

Critically, **none are new units.** Attributes expressed in units that already exist (§17), stopping measurement vocabulary breeding. Same true of other properties deciding whether factory any good — reliability, redundancy, automation level, labor requirement. Not quantities player reads off gauge.

**Wear is exception, originally listed here in error.** Reliability is statistical description of fleet over time, nothing to point instrument at; wear is one machine's own present state, and a *dimension* — mushroomed hammer head measurably wider than new. So belongs with quantities, measured with calipers already measuring deformation, measured by act above not passively. They are things turning process from *possible with somebody standing there* into *cheap industrial production*, making them §7's three questions wearing different clothes.

### Timing is a crude form of control

Timers are cheap fallback, must be **genuinely beatable rather than merely risky**.

Process times should not be perfectly deterministic. Drift with real, *learnable* conditions: fuel quality, machine wear, ambient temperature, batch load, preheat state, thermal mass, contamination. Variation player can come to understand and predict — **not arbitrary random noise**, which teaches nothing and merely punishes.

Timer is bet placed on current conditions. Sensors are how you stop betting. Timer can make process economical long before real instrumentation, and precision control should eventually and decisively outperform it.

### Quality should fall off gracefully, not switch off

Success generally not binary — in range 100%, out of range 0%. Should behave like **distribution centered on ideal conditions**, falling off smoothly: excellent at 1500 Tu, excellent at 1499, mediocre at 1495, poor at 1480, junk at 1400. Width and shape per-process, so one material forgiving, another brutal optimum.

Makes timers degrade honestly instead of being banned. Suppose player measured furnace takes about 47 seconds, sets timer. Works. Then fuel quality varies, machine wears, load changes, ambient shifts — process drifts steadily out of good region. **Factory keeps appearing to work while quietly accumulating bad batches, wear, waste.**

Far better than forbidding timers. Player not stopped from being clever; consequences of insufficient information simply emerged, entire mod in miniature.

### Rates matter as much as amounts

Process needing sustained condition not satisfied by large burst. 10000 Tu for one second is not 1000 Tu for ten seconds. Most thermal processes should care about **state of material over time**, making heating rate property of interaction between heat source, material, vessel — not "furnace speed" stat.

### Documentation is a free sense

Same rule governing instruments governs mod's own explanations of itself, settles what in-game documentation may say.

> **Documentation is a free sense. It reports in adjectives.**

Format follows mod's subject. Almost nothing here is static fact — hammer that doesn't stop, plate degrading into foil, vessel overshooting target are all **behaviours over time**, page of text wrong instrument. Right one: **Ponder-style animated scene**: player watches plate become foil become scrap, arrives at *nobody is going to turn this off* alone. Entire pitch, shown not asserted.

Also only honest format. Mod named Feedback, thesis *observe, then reason*, shouldn't explain itself with wall of prose. Documentation should have same epistemics as thing documented.

**But format unusually dangerous here, danger precise.** Worked scene is somebody's design, rendered in three dimensions, with arrows — and §5 says automation problem with single correct answer is not a problem. Scene demonstrating two hammers interleaved deletes discovery it depicts. Scene naming steel's temperature band hands over figure §7 intends paid for in failed batches. Waste *is* the gate, documentation quite capable of tunnelling straight through.

So rule has hard boundary:

> **Show verbs. Never values, never solutions.**

- **Yes** — crank attaches to shaft like this. Hammer strikes when driven. This sensor reads and does not act. Wire runs from here to there.
- **No** — steel wants 1400 Tu. Here is working closed-loop furnace. Put two hammers side by side.

Mechanism fair game; process not. Test writing any scene: **if changing one number in balance pass would invalidate it, wrong scene.** Mechanism stable, process numbers not, scene built on stable half won't rot.

Real onboarding work inside boundary, not to be underrated. *Machine with no recipe list* unfamiliar enough players assume mod broken. Saying **yes, really — put anything in and drive it** is a verb, single most necessary thing mod must communicate, no amount of watching machine teaches it.

Corollary makes this load-bearing not decorative. **Naming designated explanation channel prevents tooltip creep.** Alternative to considered documentation policy is not silence; it is somebody patching confusing machine at 2am with tooltip giving answer away.

---

## 9. Machines Remember — and So Do Workpieces

Not all machine state transient. Machines carry persistent or semi-persistent state between runs, none readable without matching sensor.

**State not confined to machines.** Heated workpiece is *hot*, stays hot carried, stored, or dropped — cooling toward ambient entire time. Material carries own history between machines, so gap between two machines is part of process, not free teleport.

Consequences large for something so small. Machine adjacency starts mattering physically not aesthetically. Process can be lost in transit. Player acquires genuine reason to care where they put things, which no throughput tuning would produce.

Should compose messily. Glowing ingot is glowing ingot wherever — in hand, wooden chest, next to something flammable. Consequences following obviously from item being hot should be permitted to follow, not suppressed because item "supposed" to be in machine.

*Built 2026-10-14 (`feedback_mechanics.md` §7):* hot piece in hand and hot block under one now burn, by named bands, answers are physical things -- gloves, and tongs whose limit is their grip's and arms' own materials. Consequence never lock: use goes through, hand pays. Not yet built: hot piece igniting what it lies beside.

- **Thermal charge.** Cold machine runs first process slower while heating; preheated runs faster. Creates genuine standing decision: idle machine hot and waste energy, or let cool and pay startup cost. Related: large thermal mass slow to heat but easy to stabilize, small one responds fast but hard to hold steady.
- **Contamination / residue (fouling).** Running different materials through shared vessel leaves residue affecting later runs unless cleaned, or machine dedicated to one recipe family. Direct consequence of §11 — no identity checks, nothing to "filter out." One cheap multipurpose machine fouls; N dedicated machines never contaminate but cost more space and capital. Real factory-layout tradeoff, not lore excuse. Also explains reactor behavior cleanly: dedicated reactor only sees one intermediate, nothing lingers to interfere with next batch.
- **Wear.** Accumulates with use, shows up as **drift in machine's sensor baseline** — vibration sensor's "normal" reading shifts as bearings age.

Last point matters more than looks. Wear not parallel durability minigame; degrades player's *information*. Maintenance and measurement same subject.

GregTech 6's boiler calcification — mineral buildup degrading efficiency unless fed distilled water, cleanable only once cooled and depressurized — direct precedent that this class of mechanic works.

---

## 10. Energy Is Plural

Feedback has multiple genuinely independent energy systems. Which drives machine is real decision, not flavor.

| Energy | Native sources | Transport | Typical uses |
| :--- | :--- | :--- | :--- |
| **Mechanical** | Water wheels, windmills, treadmills, steam pistons | Physical linkage only — shafts, gears, belts. Lossy to friction; needs lubrication upkeep | Mixers, grinders, presses, saws, pumps, drills |
| **Thermal** | Fire, lava, Blaze Rod (heat battery) / Blaze Powder (extreme fast-burning fuel), Nether/geothermal taps, thermal mass | Conducts through connected blocks; steam is primary *vehicle* | Furnaces, boilers, evaporators, stills |
| **Chemical** | Combustion fuels, reactive intermediates | Moves as physical substance, not field | Reactors: alloying, explosives, synthesis |
| **Electrical** | Lightning capture; conversion from other three | Wires, lossy over distance without upgrades | Sensors, logic, precision control, electrolysis, electromagnets |
| **Spatial** | Ender Pearls, Chorus Fruit (→ Liquid Teleportant) | Does not transmit — consumed per discrete jump | Signal relay, small-scale item/fluid transport |

First four follow generate → store → transmit-with-loss → consume. **Spatial does not**, asymmetry deliberate.

### The departure from GregTech

GregTech treats EU as one true currency, heat, steam, chemistry as on-ramps toward it. Actual chain HU → steam → KU/RU → EU → MU/LU/QU, each machine natively accepting exactly one input type.

Feedback keeps Mechanical, Thermal, Chemical as **genuinely independent, natively consumable currencies that never *have* to become electricity.** Electrical one option among several, not mandatory final form.

### Electricity: dominant transport, not universal currency

Two true statements that look contradictory:

1. Late-game factories move energy predominantly as electricity: compact, controllable, convenient over distance.
2. Electricity is not universal currency everything secretly converts into.

Both hold, because **transport and consumption differ.** Long-haul energy electrical; final conversion happens locally at machine, through fittings and blocks — adapter motor driving shaft, adapter coil warming vessel. Player puts right form of energy next to process needing it instead of rebuilding factory around every conversion. Player never has to route mechanical process through electricity to make it usable.

**Implementation rule belonging here as principle:** *do not build eight electricities.* If energy types differ only in name, texture, unit, whole section is decoration. Each type must be behaviorally distinct in ways player feels — how transported, whether storable, how degrades, and above all **how it fails and how it stops** (§8, and per-type breakdown below).

### Each type stops differently

Sensing and stopping separate problems, both physically specific to energy involved. Character, not just cosmetics:

- **Electrical** stops nearly instantly — but switching hardware arcs and wears out under repeated load-cutting.
- **Mechanical** coasts. Momentum keeps machine turning after clutch disengages.
- **Thermal** dissipates slowly. Thermal mass gives built-in soft stop, wanted or not.
- **Chemical** stops the *feed*, not reaction. Whatever already in vessel keeps reacting to completion.

Precise sensor and actuator pairings per energy type belong in document 2.

### Notes on individual systems

**Mechanical** is industrial workhorse — most material transformation ultimately a shaft turning something. Rotary and reciprocating motion **not** separate energy types; earlier question of splitting Mechanical into GregTech-style KU/RU sub-currencies **resolved as no**. Rotational direction matters where mechanism genuinely cares, particularly reciprocating mechanisms.

But motion has **shape** as well as magnitude, and many machines want back-and-forth not round-and-round. Hammer strikes. Bellows squeezes. Saw draws. None want spinning shaft, all fed by one.

Conversion is **linkage** — crank or cam — component player places, not detail hidden inside machine (§4). Matters more than sounds, because of what linkage controls (§17): crank's throw sets how much force each stroke delivers, so **`St` is not a property of the machine at all. It is a property of how player chose to drive it.**

**Thermal** heat not internally "steam" — steam is vehicle, as electricity is real regardless of which metal carries it. Two physically opposite conversions exist. Turbine or engine lets heat flow downhill and skims work off flow, efficient across *large* gradients and destroys gradient. Heat pump forces heat uphill, efficient only across *small* gradients — useless for smelting heat, excellent for cheaply maintaining modest preheat, therefore tied directly to thermal-charge mechanic in §9. Also enables real heat integration: pump waste heat out of something that must stay cool into something wanting to stay warm.

Heat pump left unattended overruns **in both directions at once** — over-cooled source and over-heated sink from one neglected machine. Core loop generalizing cleanly to device with two outputs is good sign for loop.

**Cut: caging a living Blaze as renewable thermal source.** Earlier pass had flame-permeable cage holding living Blaze captive, escaping and turning hostile on mismanaged containment — hazard-tier outcome with real teeth, and GT6's community successor has "Infernal Boiler" on similar ground, making shape look field-tested. Dead anyway: creature held captive in cage, escaping on failure, is Create's Blaze Burner specifically, not just "anomalous thermal source" generally, and rotation network already draws enough from Create that converging on this too would undo differentiation. What stays: **Blaze Rod** genuinely good heat battery — holds heat unusually well, anomalous property worth "recognize before understanding" treatment (§2) alone. **Blaze Powder** extreme-temperature, fast-burning fuel — very hot, very short burn. Neither needs cage or hazard mechanic; anomaly is in material, not escape-failure state.

**Chemical** splits in two. Combustion fuels are chemical energy deliberately converted to heat — feed Thermal, need no transmission network of own. **Reactive intermediates** stay chemical: unstable synthesized compounds moving as physical substances with **shelf life**, decaying to inert waste if sitting too long or traveling too far before next reactor consumes them. Mirrors real process chemistry, where many intermediates cannot be stored or shipped and must be consumed on site — and gives Chemical failure mode no other system has. It **expires**, where Thermal dissipates and Electrical merely resists.

**Electrical**'s wild source is lightning capture via rod-and-capacitor array — industrializing something world already demonstrates works, since Channeling trident redirects lightning reliably. Bursty and storm-dependent, needs capacitor banks to smooth into usable, pleasing parallel to real electrical infrastructure being mostly buffering rather than generation. Electrical also default backbone for **control signals**: cheap, wired, distance-limited like redstone, needing repeaters. **Magnetism folds into Electrical** as application (electromagnets), primary gameplay home in magnetic separator.

**Spatial** structurally unlike rest: consumed per discrete jump, no generate/store/transmit staging. Mid-game, **Ender Relay** binds transmitter/receiver pair using Ender Pearl consumed in binding — one-time capital cost, like lodestone binding compass — after which pair passes **data** instantly at any distance, across dimensions. Bound link then needs continuous trickle of **Liquid Teleportant** to stay stable, not to power jumps but to suppress premature transition events. Let it run dry and link **destabilizes** rather than cleanly switching off: dropped and corrupted signals at data tier, genuine hazard at matter-transport tier, where whatever mid-transit has nowhere clean to go. Late-game, same bound pair scales up to carry small, hard-capped trickle of items or fluid — enough to keep remote outpost supplied or return samples to lab, deliberately not enough to replace belts and pipes. Spatial thus gets same two-layer cost structure as everything else (capital plus operating cost) and same per-type cutoff logic: stop fluid feed and link goes dormant safely rather than dangerously.

Because data transmission reuses energy transmission rules, expected pattern emerges without being mandated: small local controllers wired to own machines' sensors for fast reflexes, Ender Relay reserved for coarse, occasional cross-site coordination.

---

## 11. No Identity Checks

**Nothing in this mod may perform an identity check.** No block, fitting, pipe, or logic node may act on "if item == iron ingot." No generic filter.

**Rule is about player's experience, not source code** (user ruling, 2026-10-20). Where cleanest implementation compares id or registry object, and nothing player can do or see is keyed on that identity, comparison is not identity check in this sense. What §11 forbids is a *mechanic* treating iron ingots as special. Tags and properties remain default, so modded equivalents work free.

All sorting and quality control must exploit real physical properties:

- **Density / gravity** — heavier material sinks faster in fluid column or centrifuge. Real jigging and gravity separation.
- **Magnetism** — electromagnetic drum pulls ferrous material; everything else passes. (Magnetism's gameplay home, per §10.)
- **Size / screening** — mesh passes only particles under given size. Directly relevant because deliberate overrun-grinding produces *size distribution*, not one clean output (§6).
- **Optical / reflectivity** — light sensor distinguishes visually distinct materials, and is **genuinely fooled by visually similar ones**. Exploitable weakness to design around, not bug to patch.

Makes logistics substantially larger puzzle than existing tech mods: sorting becomes finding property difference to exploit rather than reading item ID. Also makes fouling (§9) real consequence not arbitrary penalty — no identity filter, nothing could have caught the contamination.

**[OPEN]** Mapping actual roster of vanilla and modded materials onto these four separator types not done. Design collaboratively; lands in document 3.

---

## 12. Chemistry as a Useful Abstraction

**Real periodic table** is default framework for material properties — conductivity, reactivity families, density — matching GregTech's approach where it improves gameplay.

Minecraft-exotic materials not mapping to real elements (redstone, Blaze material, Ender Pearl material) handled as compendium handles Redstone: **deliberately flagged exceptions** with own defined properties, sitting *alongside* real table. Not forced into invented chemistry, not left as unexplained magic.

Chemistry should allow **multiple reaction paths** without becoming chemistry simulator. Tags can express broad reagent classes — *any strong acid*, *any chloride salt* — so player's available materials, not single blessed recipe, determine route.

**Byproducts must remain meaningful.** Different paths to same product produce different byproducts, and those matter. Keeps route choice real decision not cosmetic flexibility, main thing separating abstraction worth having from lookup table.

---

## 13. Control Logic

### What a controller is

Controller attaches to machines on whatever face carries data provider player chose. Reads **everything connected to it**, performs basic operations — `+ - * /`, `> <`, `AND`, `OR` — and produces exactly one kind of output:

> **It can start or stop a supply. That is all it can do.**

Flip breaker. Cut clutch. Close damper, shut valve. Every actuator in mod is tap, controller's entire vocabulary *open* and *closed*.

Sounds impoverished, is opposite. **Controller is only block in game that sees whole picture.** Every machine otherwise blind man feeling elephant — furnace knowing own temperature and nothing else, hammer knowing nothing at all. Controller is where separate observations become situation, therefore heart of automation though it can't do anything more sophisticated than switch things off.

### The controller is a switch, never a dial

Deserves own statement, decides character of every control problem in mod.

No proportional control. No output saying *heat at 40%*. Controller turns supply on and off, so all control in Feedback fundamentally **bang-bang control**, which is why deadbands and hysteresis matter so much.

Thresholds not a *setting* controller offers — something player **builds** out of comparisons. Deadband is two comparisons and memory of what output currently doing, assembled deliberately. Same principle one level down: even controller not handed wisdom.

**Decided (2026-10-13): actuator state is another readable source.** Hysteresis loop must know whether currently on, so controller reads output back like anything else: Input card linked to clutch, valve or damper reads that actuator's on/off state as boolean. Memory lives in world, in switch's own position, not program, so controller stays stateless. Deadband then `(below low) OR (already on AND below high)`, which is why `OR` card arrived with it. Without this deadband couldn't be expressed at all, and relay flapping wouldn't be player's fault.

Consequence is the point:

> **You cannot buy a smarter algorithm. You smooth control with physical mass.**

Oscillating temperature fixed with larger vessel, better insulation, smaller bellows, faster sensor — each a physical change to build (§4). Sophistication lives in machine player assembled, never cleverness of program. Same principle as §5's "infinite ways to skin the cat," applied to control instead of process.

**One exception, and its boundary: vehicles.** Vehicle controls analog, its controller programs included (`docs/feedback_vehicles_spec.md` §5, *Controls*). Consistent version -- throttle open or shut -- weighed and rejected because miserable to drive, and car is not process held in band: human closing loop by hand, which this section already calls first controller. Boundary not where part sits (user ruling, 2026-10-21): engine analog wherever it is, on stand driving shaft as much as in car, because throttle is a supply and controller may meter supply with number. Only parts that are analog take one; clutch, valve or breaker is switch wherever it sits. What exception does *not* change is the object: in vehicle as in factory, controller meters a supply -- throttle is fuel and air to engine -- and never runs machine itself. Playtesting (2026-10-15) found "on/off only, except cars" reads as two rules; "it connects to supplies, which in a factory are switches and in a car may take a number" is one. As built (2026-10-20): every controller has arithmetic cards, and number moves only a part that is *analog* -- throttle, car's steering, its brakes -- and only vehicle parts are. Boundary is which blocks exist, not a check controller makes.

Sufficiently determined player will notice switching supply on and off *rapidly* approximates proportional output, and they're right — how real controllers do it. Mod should let them, and already charges for it: rapid switching under load is exactly what destroys breakers. Clever solution exists, works, has bill attached. Correct shape for clever solution.

### Before any of this, the player is the controller

At very beginning no control hardware at all. Player watches, decides, acts. **They are the controller**, every later tier an attempt to get that job done without them standing there.

First hardware taking any of it over is not controller either. Sensor fitting wired directly to actuator is a **thermostat**: bimetallic strip trips at temperature, bellows stops. One condition, one response, no logic block anywhere.

Only then controller arrives, for a reason — the moment one condition stops being enough.

### Controllers end up marking the eras, and that is allowed

§14 insists progression is profile not rank, and one axis will look conspicuously like tier ladder anyway: **new controllers will read as definitive mark of era change.**

Acceptable, because nobody decided it. Consequence of making the thing that comes in tiers also be nervous system of factory. When component that sees everything gets better, everything it sees gets better with it — of course feels like age turning over.

Distinction to hold: this was *earned* not imposed. Voltage tier announces era because designer said so. Controller tier announces one because player can suddenly coordinate things they couldn't yesterday.

### What tiers, and what does not

**Capability does not tier.** Punched tape can express everything late-game integrated controller can. Operations same, logic same, player who can describe what they want can always write it down.

Only two things improve.

**1. The cost of changing your mind.** Early, rewriting means cutting new physical program and walking it to reader — earliest tier may mean crafting whole new controller. Later swapping disk. Later editing in GUI. Program is item before it is setting (§4), so can be copied, stockpiled, labelled, handed to another player, and **you cannot tweak a running machine.**

Medium follows energy progression rather than assigned arbitrarily:

- **Punched tape or card** — early, entirely mechanical, read by pins and levers exactly as Jacquard loom or player piano reads one. Needs no electricity, which matters because early game has none (§10).
- **Magnetic tape** — arrives with electricity. Rewritable, denser.
- **Integrated controller** — late. Authoring and execution collapse into one block.

**2. How much you can look at.** Early controller handles booleans from two or three sources. Late reads **tables** from something on order of a hundred. Axis actually gating what player can build, compounds with sensor tiers: cheap sensor emits boolean against fixed built-in threshold, better one emits real continuous value and lets controller do own arithmetic.

Note meaning. **Controller progression is pure soft gate** (§7). Player never unable to express program. Unable to *see enough to write useful one*, and unable to afford iterating on it. Same gate thermometer imposes, one level up — why difficulty in Feedback lives in acquiring and routing data rather than learning to program:

> **The logic is trivial. Getting something worth reasoning about in front of it is the entire problem.**

### How data gets to the controller

Controller reads any data-offering block **within range** — on order of sixteen blocks, since eight doesn't cover even moderately sized build — entire rule. No cable block, no conduit network, nothing to route.

Cable is **drawn** between the two, client-side, when client can see they're linked. Making connection may cost cable item. Nothing in world is wire: no entity, no block, no path.

**Crucially, link not checked for clear path.** Passes through walls, floors, machinery, deliberate abstraction not oversight. Routing cable around obstacle not interesting decision — chore wearing decision's clothes. Range limit stays because it *is* interesting decision: makes controller placement matter, produces control rooms as natural building pattern rather than prescribed one.

Range and capacity orthogonal constraints. Range decides **where** controller can see; tier decides **how many** of those things it can hold in mind at once.

Leaves Ender Relay's job intact (§10). Sixteen blocks is workshop. Relay is any distance, across dimensions, costs continuous trickle of Liquid Teleportant to stay stable — different problem entirely, not better version of same one.

### The third axis is qualitative: what the controller runs on

Early controllers take **rotation**. Late ones take **electricity, natively.**

Not ordinary conversion story where late machine takes power because adapter handles it (§10). Literal: early controller is analogue machine physically turning tape past reader, so wants shaft. Late controller is computer, computers run on electricity.

Two consequences worth having on purpose.

**Early control shares failure domain with machines it controls.** Same water wheel drives hammer and tape reader, so stalled mechanical network takes down process *and* thing supervising it simultaneously. Later, electrically-powered controller stays awake while mechanical network down and can still act on what it sees. Real reliability improvement rather than convenience, arriving without anyone designing "reliability upgrade."

**Rotation speed is polling speed.** Tape physically turned is read at rate proportional to `RPM`, so controller's **Response** property (§8) is direct function of how fast player spins it. Control loop genuinely gets sluggish when river runs low. Speed up and loop tightens — until breaker wear from over-frequent switching bites (§13, below).

Editor's visual style takes after *Steve's Factory Manager* — flowchart of wired nodes rather than text language. (Successor *Super Factory Manager* added optional scripting later while keeping visual mode.)

### Bad logic has physical consequences

Controller built without deadband, reading value hovering near threshold, flaps relay on and off rapidly — and breakers already wear from repeated cutting under load (§10). **Sloppy control logic doesn't merely work badly; actively destroys downstream hardware.**

Ties control quality directly into mechanical stakes energy system already established, rather than inventing separate "code quality" mechanic to punish player.

---

## 14. Progression Is a History of Understanding

Progression not voltage ladder, not sequence of technological ages. Order in which player comes to understand what this world actually does.

Progression step valuable when player **understands something new about world, gains new way to observe or control it, or learns to apply old phenomenon at fundamentally greater level of control.**

### The order of understanding

**Capability bands, not tiers.** Overlap, not gates, player should never experience as locks. "Tier" remains useful shorthand for broad era; should never appear in player's face.

1. **Materials have physical behavior.** Fire, water, stone, wood, metal, weight, motion, hardness, melting — consistent and observable. Science purely phenomenological: *this thing does that.* Minecraft's rules already present; nothing postponed to magic endgame.
2. **Processes are continuous.** Heating, grinding, mixing, reacting, pressing, pumping don't stop when desired result first appears. Core loop becomes player's model of world: matter has state, continued processing moves state onward.
3. **Processes can be scaled.** Once process runs continuously, throughput becomes problem. Bigger vessels, larger work surfaces, stronger drives, better bearings, pumps, boilers, shafts.
4. **The world contains multiple useful energy phenomena.** Water, wind, fire, steam, fuels, Blaze heat, lightning, redstone behavior. Player not yet required to unify into one currency, several never need unifying.
5. **Energy can be measured and managed.** Player stops watching machines and starts instrumenting them. Temperature, torque, vibration, pressure, flow, electrical and chemical state become observable through covers. Machine didn't get smarter — **player gained a sense.**
6. **Materials have hidden, exploitable properties.** Better instruments reveal differences invisible to eye, useful for separation, control, conversion, process design. Minecraft-exotic materials start showing behavior plainly impossible by real-world expectations — measured and characterized rather than labeled magic.
7. **Mixtures can be separated and purified.** Density, magnetism, size, phase behavior, solubility, optics. Screening, gravity separation, centrifugation, filtration, distillation, crystallization, extraction become backbone of material logistics.
8. **Matter can be deliberately transformed.** Beyond combining and heating: synthesis, decomposition, redox, precipitation, neutralization, polymerization, alloying. Chemistry becomes discipline only *after* enough measurement and process control exist to make it meaningful.
9. **Processes have measurable rates and states.** Pressure, temperature, concentration, residence time, mixing, atmosphere, catalysts, electrical potential — deliberately held in useful ranges. Multi-stage process chains become natural rather than arbitrary.
10. **Extreme conditions reveal new phenomena.** With ordinary variables under control, player can deliberately explore high temperature and pressure, strong fields, unusual atmospheres, near-vacuum, intense stress. New processes arise from what conditions expose.
11. **Minecraft-exotic phenomena become subjects of systematic science.** Blaze material, redstone, Ender and Chorus phenomena, Nether materials, lightning. Player doesn't "enter magic era" — discovers existing framework was incomplete.
12. **Natural anomalies can be engineered.** From using naturally occurring impossible materials, to characterizing mechanism, isolating property, reproducing under controlled conditions, finally manufacturing superior analogues. Natural Blaze Rod was never end of thermal technology.

### Progression is a profile, not a number

Two different things hide under word "tier": player's **scientific capability** and factory's **industrial capability**. Don't advance together. Crude machine with excellent sensor outperforms sophisticated machine operated blind, and enormous throughput perfectly compatible with knowing almost nothing about what happens inside.

So progression better modelled as several axes occasionally converging into recognizable eras:

| Axis | Early | Middle | Late |
| :--- | :--- | :--- | :--- |
| Physical manipulation | intermittent, manual | continuous, large-scale | extreme conditions |
| Throughput | one item or batch | continuous flow | industrial scale |
| Observation | senses and timers | dedicated indicators | continuous instrumentation |
| Control | human intervention | threshold automation | feedback control |
| Materials | structural | engineered properties | exotic |
| Separation | crude, manual | property-based | highly selective |
| Transformation | simple processes | controlled chemistry | exotic synthesis |
| Energy | direct mechanical/thermal | multiple competing forms | distributed electrical, specialized forms |
| Logistics | physical handling | pipelines and conveyors | integrated process network |
| Exotic science | incidental | characterized | deliberately engineered |

Player's position is therefore **technology profile**, not rank:

> Mechanical capability: high · Thermal control: moderate · Instrumentation: low · Chemical processing: moderate · Separation: high · Exotic characterization: none

Far more interesting than **Voltage: EV**. Deliberate lopsidedness legitimate and playable — player deep into separation with almost no instrumentation is real build, not mistake.

Yields single most useful question for any proposed technology:

> **Which axis does this advance?**

Bigger boiler advances scale. Thermocouple advances observation. Deadband controller advances control. Centrifuge advances separation. Pressure vessel advances process conditions. Blaze Rod heat battery advances energy exploitation *and* anomalous characterization. None need be "tier 4 machines." Pieces of ecosystem, interesting moments where two or three axes cross and something new becomes possible neither allowed alone.

### Difficulty stays flat. Novelty goes up.

Processes do get more demanding as mod goes on. Tolerances narrow, conditions couple, materials stop forgiving mistakes. But player's tools improve alongside — so anyone keeping up with instrumentation given finds **nothing gets harder. Things get new.**

Intended shape of whole game, worth stating as target not leaving to emerge:

> **Difficulty is a ratio — required precision over achievable precision — and the design goal is to hold that ratio roughly constant while the numerator and denominator both grow.**

Three consequences to design for.

**New capability must not make old work harder.** Advancing player should find earlier processes becoming trivial, not merely differently annoying. This is *Return* in four-part test below, why §4 insists better saw cuts copper better rather than simply cutting new things.

**Falling behind converts novelty back into difficulty, and that is the pacing mechanism.** Player who skipped thermometer meets tightening bands as genuine pain, exactly the pressure sending them to build one. Nothing needs to enforce build order; material cost of ignorance (§7) does it, self-correcting rather than punitive.

**What actually changes over the game is the *kind* of hard.** Difficulty dimensions in §7 are real progression axis. Early process hard because *narrow* relative to crude tools. Late one hard because *coupled*, or *unstable*, or *path-dependent* — kinds of difficulty better thermometer doesn't address at all, requiring different sort of thinking rather than tighter number. Difference between game that escalates and game that develops.

### The test for a major progression event

Significant step should ideally satisfy all four:

- **Sense** — introduces genuinely new measurement or way to observe.
- **Make** — enables something previously impractical or impossible, not merely faster.
- **Need** — introduces problem specifically rewarding new capability.
- **Return** — by end of era, earlier investments visibly pay off, outline of next unsolved problem becomes visible.

### Detect before you can act

Particularly strong pattern: give player ability to **detect** phenomenon well before they can exploit it. Radiation sensor can teach player certain materials emit something strange long before any nuclear machinery exists.

General pattern, not one-off: electricity may be observable (static, conductivity, twitching needle) long before industrial power network; Redstone may show strange electrical behavior before deliberately exploited; Amethyst may show resonance before precision component; Blaze Rod may be unusual heat source before understood; Ender Pearl's spatial anomaly may be unmistakable long before spatial engineering exists; Sculk may show signal/experience-related behavior long before engineered information device; Uranium may be discoverable long before measured, let alone reactored. Observation, measurement, control are three different unlocks, material allowed to sit at any one for long time.

Makes world feel like continuous field of discoveries rather than sequence of unlock screens, gives player questions to carry forward.

### Old technology keeps its niche

Progression should not read as straight ladder of replacements. Newer energy system or machine earns place *alongside* old, not instead — mechanical power stays useful once electricity exists (§10), Crude Blast Furnace never obsolete once crucible exists precisely because never competing on crucible's axis (§15). Same meant to hold for systems not yet built: steam keeps niche once combustion engines exist, mechanical/punch-card control keeps niche once electronic control exists, belts/pipes keep niche once spatial transport exists (§10's Liquid Teleportant link deliberately capped below belt throughput for this reason). Test for new system: does it earn *different* niche — cost, reliability, scale, attention — never whether strictly better.

### Materials have histories

Material not identical to ore/block form forever. Moves through processing history — something like `ore → crushed ore → concentrated ore → chemically treated material → refined metal → alloy → finished stock → precision component` — and purity, processing history, geometry, thermal/mechanical state may all matter where doing so creates decision (§3's anti-simulator test still applies: track step only if skipping or doing badly is real, playable mistake).

### Geology should matter

**[OPEN]** Ore generation should eventually move toward large, irregular, geologically coherent deposits — associated minerals, low-grade peripheral material, richer cores, depth-dependent composition — rather than isolated decorative ore blocks. Inspired by GregTech-style large-vein generation, but mineral roster, deposit geometry, generation algorithm all undecided; see §18.

### Progression should be visible in the factory

Energy systems produce visible industrial arc without recoloured machine tiers.

- **Early** — essentially no electricity. Factories mechanically and thermally legible: shafts, gears, belts, water wheels, windmills, pistons, flames, boilers, hot vessels, hands-on material handling. First workshop should look like machine shop.
- **Middle** — electricity appears because certain jobs genuinely need it: sensing, precision control, electrochemistry, electromagnets. Still generated from thermal and mechanical systems. Steam and cogs don't disappear; share floor with wiring, instrumentation, relays.
- **Late** — electricity dominates energy *transport*, local conversion at point of use (§10). Mechanical and thermal processes continue everywhere; what changed is how energy arrives.

Intended arc **mechanical/thermal → hybrid → predominantly electrical transport with local conversion**, underlying physical processes staying recognizably continuous with what came before.

### Visual progression is a design goal, not a side effect

Most tech mods pick one aesthetic — steampunk, industrial, sci-fi — keep same machine *forms* throughout, changing only materials and textures. Feedback should make advancement visible through **changing infrastructure, process conditions, energy transport.**

Progression not *wooden machine → copper machine → steel machine → titanium machine*, one block doing one job forever. It is:

> **primitive process → scaled process → instrumented process → controlled process → exotic process**

with genuinely different machine architectures becoming appropriate along the way. Materials still govern construction and limits, but must not be only thing communicating progress. **Late-game factory should not be early-game factory with faster machines and different textures.**

### Machines grow out of process families

Machine roster should emerge from physical process families rather than recipe categories: size reduction (grinding, crushing, milling, cutting); mixing and homogenization (blending, suspension, emulsification, gas incorporation, foaming); separation (screening, gravity, magnetic, centrifugation, filtration, sedimentation, flotation, distillation, crystallization); phase change (melting, freezing, evaporation, condensation, sublimation, deposition); chemical reaction (combination, decomposition, displacement, redox, precipitation, neutralization, polymerization); extraction and purification; electrochemistry (electrolysis, electroplating, electrolytic refining, electrodeposition); thermal treatment (annealing, hardening, tempering, calcination, sintering, roasting, carbonization, thermal decomposition); and Minecraft-native transformations involving Blaze, redstone, Ender, Chorus, lightning, Nether phenomena, all expressed through measurable internal rules.

First machine families should feel like **physical inventions making these processes continuous and reliable** — not list of one-block recipes.

### Empowerment is a parallel axis

Player empowerment should not track industrial progression one-to-one. Industrial capability, instrumentation, automation, personal capability partially independent axes.

Player should gradually become more capable in world, ending with genuinely high-end personal equipment — but endpoint should read as **technology being industrialized onto the player**, not abrupt leap from ordinary armor to inexplicably immortal sci-fi Halo Doom Slayer 9000 Mecha-body. **[OPEN]** Empowerment curve designed after industrial framework settled.

### Loot is tier-skipping, never a source of the uncraftable

*(Author's ruling, 2026-10-21.)* Nothing found in world is something player *cannot* make; something they cannot make **yet**. Relic in chest is piece of technology or magic from further up progression, found early, so on paper loot is **tier skip** and nothing else. In practice preview of what understanding will buy: as player learns, they become capable of making wondrous things once only found, becoming the ancient builder, or ancient wizard, who hid them. Design consequences: every loot-only item needs a *craftable route that exists* (even if far off and hard); loot item never only source of a capability; relic's figures come from same material/part system crafted one would, so found version is hand-me-down from better maker, not different kind of object. (Why written down: "loot-only" was first phrasing, and wrong; would make loot unreachable gate, which §7 forbids.)

---

## 15. Vanilla Is Not Exempt

If heat is real continuous quantity and materials accumulate temperature, vanilla furnace cannot remain instant raw-item-to-finished-item black box in same world. Vanilla mechanics reinterpreted through same physical rules wherever visible contradiction would otherwise exist.

**Decision: three vanilla smelting operations preserved, vanilla blocks themselves superseded.** First attempt reworked in place via Mixin, patching `AbstractFurnaceBlockEntity` and two subclasses; fought vanilla internals one crash at a time, never reached player. Feedback now ships own `feedback:furnace`/`smoker`/`blast_furnace` — single `ThermalVesselBlockEntity` parameterized by `VesselKind` — same recipe and role vanilla's had, but genuine thermal behavior. Become crudest real apparatus in game, bottom of same ladder as everything player builds later.

Original vanilla blocks stay registered (existing worlds, mod compatibility) but uncraftable — recipe gone, not block. Still generate in structures for now; replacing those occurrences future work, not yet built.

### Three vessels, three personalities

Each gets distinct physical character rather than speed multiplier, described by properties in §9 and §8:

- **Furnace** — generalist. Open, crude, mediocre at everything. Wide usable band, modest peak, little thermal mass, no stability worth name.
- **Smoker** — **low ceiling.** Low thermal mass, heats fast and cheaply, twitchy and hard to hold steady.
- **Crude Blast Furnace** *(renamed)* — **high floor.** High peak temperature, high thermal mass, slow to come up, **genuinely stable** for primitive device. Not great. Good enough to be tempting.

**Vanilla specializations must be emergent, not enforced.** Recipe-type whitelists come off. Nothing declares smoker food machine or blast furnace ore machine; three numbers do it.

Mechanism: each device has **restricted operating band**, and §8's *Range* property has bottom end as well as top:

- Smoker's **ceiling** below metalworking temperature. Ore placed in one simply never arrives — material unchanged, correct failure from §6 rather than refusal.
- Blast furnace's **floor** above food window. Sealed refractory box burning coke either runs hot or isn't running; you can't hold one at cooking temperature, equally true of real thing. Food put in one destroyed on way past.
- Furnace spans both bands, good at neither.

Note floor does necessary work, ceiling alone not enough. High thermal mass makes blast furnace *slow*, and slow vessel is **more** forgiving of low, wide window, not less — would drift up through cooking temperature and linger comfortably. Without floor, blast furnace is excellent smoker. Restricting band at both ends makes specialization real.

**Built as one wall, not two.** Smoker's ceiling is genuine clamp (`FTuning.SMOKER_CEILING_TU`) — nothing about leak or mass produces it alone, since fire dominates equilibrium calculation regardless of leak at values this mod uses. Blast furnace's floor *not* mirrored as second clamp; it is high mass alone, arithmetic checked not assumed: at typical fuel temperatures vessel's own climb rate highest near ambient and falls near equilibrium, so time it can spend below food ceiling is short relative to food recipe's cooking time, and food destroyed on way past exactly as this section says. True bistable "roaring or out" floor considered and set aside — see `core/FTuning.java`'s `BLAST_FURNACE_MASS` for why, and anti-simulator test in §3 for standard it failed.

Result: both blocks end up better at exactly what they were always better at, **and no rule anywhere says so.** Emergence mod is built on, demonstrated in first hour on blocks player already knows. Also means modded recipes lose device restrictions — accepted and deliberate trade, in mod already close to total-overhaul scope: conditions decide, types do not (§5).

### Vanilla vessels are appliances. Feedback's are components.

All three vanilla blocks share one property, everything else follows: **they are self-contained.**

Fuel goes *inside*. Not implementation convenience — all three visibly have compartment for coal beneath receptacle, always have. Finished objects: fire, vessel, container in one box, nothing to attach, nothing to separate.

Feedback's own equipment opposite. Crucible is not furnace; vessel sitting over **separate** firebox, taking **separate** instrument, driven by **separate** actuator. Arrives in pieces because player meant to arrange pieces.

Distinction is whole progression in physical form:

> **The player begins by operating appliances and ends by assembling components.**

Also explains Crude Blast Furnace's refusal of covers as one property rather than two arbitrary rules. Sealed. Fuel goes in because sealed; nothing attaches because sealed; stable because sealed. **Same property making it good makes it opaque.**

Hard ceiling on **observability** rather than capability, inverse of every tier gate in genre — gives purpose-built crucible something real to be better at. Crucible wins not by holding more heat but by having been **designed to be measured**: vessel with thermowell, physical upgrade in §4 sense rather than stat.

### Attention is a currency, and the blast furnace is where you spend it

Crude Blast Furnace therefore **not predecessor to crucible.** Sits alongside, available same time or later. Never obsolete, because what it offers was never capability.

Two vessels orthogonal:

| | Crucible | Crude Blast Furnace |
| :--- | :--- | :--- |
| Speed | slow | fast |
| Batch | large | small |
| Instrumentable | yes — that is its purpose | never |
| Costs the player | capital and infrastructure | **standing there** |

Produces genuine standing choice not upgrade: *a minute of AFK, or thirty seconds of full attention?* Both correct, which one correct changes with what else player doing.

Matters well beyond one block. §7 promises manual production stays viable for surprising share of game, until now that promise rested on player's stubbornness. Blast furnace makes it **structural** — manual route gets dedicated equipment genuinely, permanently competitive, Amish% player using purpose-built tools rather than refusing good ones.

Also names something mod has been trading in without acknowledging: **player attention is a real resource**, mod should be willing to sell speed for it. Every manual process in §7 already an attention purchase. First piece of equipment designed around that fact.

### The fallback

Feedback cannot hand-specify process for every smelting recipe in every mod player has installed, and unspecified recipes must not break.

**This section originally specified Work-based mechanism, and it was wrong — recorded because error is reusable part.** First version costed progress as `X` Work per tick, `X` being how far above ambient vessel sat: hold vessel `X` Tu above ambient and it costs `X` Work per tick, item needs some total quantity of Work, progress accrues at vessel's own temperature. Reads as physically motivated, exactly the trap `docs/tpu_spec_doc.md` names: `Tu × ticks` is not Work, because temperature is state and never amount of anything (§17). Also meant three vanilla recipe types collapsed into "whichever is shortest," permanently — iron ore's blast-furnace time won in every vessel, including plain Furnace that can never actually reach blasting conditions, hidden recipe rule wearing §15's own "specialisation is emergent" claim as disguise.

**Mechanism now TPu**, per `docs/tpu_spec_doc.md` and `feedback_mechanics.md` §3: process-local progress quantity, not energy. Recipe's declared cooking time becomes baseline requirement in same ticks-equivalent units process would need at perfect conditions; vanilla recipe type (`smelting`/`blasting`/`smoking`) becomes thermal-suitability hint rather than fixed multiplier, evaluated against vessel's *current* temperature every tick rather than settled once. Ore therefore finishes faster in genuinely hot vessel and slower in mediocre one, neither block ever checking what kind of vessel it is — same emergent specialisation this section already promised for three vessels, now applied to fallback feeding them.

Still not physically honest, not meant to be. **Fallback is compatibility shim, not model of world.** Its one job: guarantee no item in any installed mod becomes uncraftable — not to make that item's process interesting, balanced, or true. Interesting belongs to hand-authored processes, imposing real windows and real rate limits (steel will not tolerate being blasted). Shim exists so nine hundred recipes nobody will hand-author keep working.

Two properties make looseness acceptable. **Fallback still imposes no ceiling on how fast item may absorb progress** — vessel held exactly at recipe's optimum finishes in exactly its baseline tick count, however heat delivered — so nothing free; speed still paid for in heating capacity and aim player must actually own. And **fallback data-driven by construction**, the real goal: writing compatibility for new mod should be table, not essay.

Hand-authored processes override fallback. Everything else keeps working untouched. Makes "world and machines obey same rules" affordable rather than infinite content obligation.

---

## 16. Relationship to Create

Create's genuinely powerful idea is not kinetic machinery. It is:

> **A machine is a component. The player is the system designer.**

Feedback takes one step further. In Create, *automation* compositional. In Feedback, **manufacturing process itself compositional** — mod supplies heat source, furnace, thermometer, pump, valve, controller, timer, press, never supplies refinery using them. Create says *here are components, build a machine.* Feedback says *here are physical constraints, build whatever you think satisfies them.*

Create therefore **philosophical predecessor**: compositional sandbox in §5 is what Feedback wants to push further toward process engineering, and Create's vocabulary — `Su` for mechanical stress, `RPM` for rotational speed — worth reusing so players read Feedback's numbers without glossary.

Mechanically, Feedback implements **own** rotational system rather than depending on Create. Create being open source makes it reference to study rather than library to import.

**[OPEN]** Whether Feedback additionally ships **compatibility** with Create's rotational network — shafts that mesh, stress models that reconcile — undecided. Two live options: *entirely separate* or *separate with some compatibility*. Nothing in design so far depends on answer, deferrable until Feedback's own mechanical system exists.

---

## 17. Vocabulary

Units follow one rule, three clauses:

> **Use a fictional unit for a game-important abstract quantity. Use the familiar unit where the Minecraft ecosystem already has a standard. Do not model a quantity at all unless modelling it creates an interesting decision.**

Third clause does most work, §3 applied to vocabulary.

| Quantity | Unit | Notes |
| :--- | :--- | :--- |
| Temperature | `Tu` | A **state**, not amount of heat. See below. |
| Pressure | `Pu` | First real use is Pressure Vessel (`TODO.md` §5e as of `50b93c6`): amount of stored air over fixed volume, same shape `ThermalMass` already gives `Tu` — see `core/unit/Pu.java`. |
| Cumulative mechanical work | `Fu` | Total mechanical application a process demands. |
| Mechanical application strength | `St` | How strong each individual application is. What that strength *accomplishes* is not machine stat — see below. |
| Mechanical stress / load | `Su` | Create's meaning, deliberately. **Never** speed. |
| Rotational speed | `RPM` | Don't invent fictional speed unit. |
| Volume | `mB` | Millibuckets — fluids, and air (bellows, firebox, Pressure Vessel). Air carried through `St` one-to-one until typing exposed it; volume is what bellows moves, and long throw moving more air than short one is why it could not be force. |
| Fluid flow | `mB/t` | |
| Mass | `Mu` | Needed for density, therefore gravity separation (§11). Density is `Mu/mB`. |
| Amount of substance | `Qu` | Stand-in for mole carrying **no implication about atoms or particles**. Concentration is `Qu/mB`. First real use is electrolytic cell (`feedback_mechanics.md` §6.14): ingot of any metal is same amount. |
| Electrical | `Eu` | Voltage *and* current. The one non-scalar unit. See below. |
| Untyped energy | `Work` | Spelled out, no abbreviation. See below. |
| Time | `t` / seconds | Ticks internally; JEI should show both — `600 t (30 s)`. |
| Resistance | *(none)* | **Deliberately not modelled.** Add only if gameplay gives reason. |

Rates derive rather than invented: `Tu/t`, `Eu/t`, `mB/t`. No reason to coin unit for every derivative.

### Temperature is a state, not an amount of heat

`Tu` measures how hot a thing *is*. Does not measure how much heat transferred to make it so. Furnace sitting at some temperature is in a state; heating it happens at some `Tu/t`.

Separation deliberate, makes §8's "rates matter as much as amounts" mean anything. If temperature and quantity-of-heat same number, process needing *sustained* condition indistinguishable from one needing large burst — and entire thermal-mass, preheat, insulation layer in §9 collapses into single efficiency stat.

### Fu and St: force is the machine's, work is the material's

`Fu` is total work, `St` is force of one application, and tempting mistake — made once already and corrected — is treating them as two independent numbers machine publishes.

Not independent, because **nothing is strong enough to dent steel yet fails to slam copper.** Machine states only force it can deliver. What force *accomplishes* is property of material hit:

> **Fu per application = St ÷ hardness**

**Hardness is one number doing both jobs.** Floor below which nothing happens at all — §7's hard gate, genuine impossibility not slow version of process — and divisor for how much of larger blow actually lands. Copper's hardness low, so heavy blow dumps great deal of work into it; steel's high, so same blow does nothing whatsoever.

> 30 Fu total, hardness 1 — anything can finish this eventually, given enough taps.
>
> 60 Fu total, hardness 15 — no number of weak taps will ever substitute.

Second form is hard gate. First has subtler consequence turning out to matter more.

**Hard blow on soft material overshoots.** If force over hardness large, single application can carry workpiece past state wanted into next one — surplus work carries through rather than stopping politely at finish line. Strongest available setup therefore not best, because §6's overrun arrives sooner and less time to intervene.

Whole mod in one line of arithmetic, arrived by itself: not designed, fell out of refusing to let force and work be two separate numbers.

**Machine defines a pair; linkage selects from it.** Crank converts rotation into reciprocation (§10). Its **throw** is how far crankpin sits off axis of rotation, sets two things at once: stroke is twice throw, force available at ram is torque divided by throw. Short throw concentrates torque into greater force over shorter stroke, long throw spreads into gentler, longer one. One stroke per revolution either way — throw doesn't change how often hammer falls, only how hard.

So machine's `St` written as two numbers not one:

```
MECHANICAL HAMMER     12 / 3 St     (short throw / long throw)
```

Both outcomes on card, which player gets depends on crank installed. `RPM` still sets how often strokes happen; `Su` still says whether network can drive thing at all.

Machine therefore has no single strength. Satisfying *hardness 15* requirement means **re-gearing the drive**, not buying stronger machine — same hammer is delicate planisher or forging press depending only on how driven. Not metaphor: light blows for finishing and heavy blows for drawing out is how smithing actually works.

**Overshoot can skip a state entirely, best version of the lesson.** Copper soft enough that short throw carries it past *plate* into *foil* in single blow — intermediate not merely quick to pass, unreachable at that setting. Two throws therefore don't make same product at different speeds. They make **different products**, selected by how machine geared and not any recipe chooser. Short-throw hammer is foil machine.

Law hiding in that, worth stating because it constrains every future process:

> **The reaction window is the strength ratio.** For a hard setting to skip a state, that state must cost less than one hard blow — which caps the gentle setting's slack at the ratio between the two, measured in blows.

`12 / 3 St` machine can never give more than four blows of grace on state its strong setting skips. Widening gap widens window. So "how forgiving is this material" and "how different are the two cranks" not two design questions; one number seen from either end.

**Neither throw may dominate other.** Got wrong once, failure worth recording because easy to repeat. When throw changed `St` alone, short throw strictly stronger at identical speed, so nobody would ever fit long one — dominated option worse than no option, costs player a decision and gives nothing back. Fix not inventing compensating bonus but noticing work per blow was never machine stat in first place. With work derived from hardness, short throw is **power** and long throw is **precision**: strong setting reaches goal in fewer blows and blows past it in fewer too.

**Gearing changes `St` too, and machine has ceiling.** Gearing down trades speed for force — oldest trade in machinery, mod shouldn't pretend otherwise — so gear train multiplies force each blow lands and divides how often blows fall. Work per second unchanged; what changes is whether any single blow hard enough to matter at all, for material with hardness floor the difference between working and doing nothing.

But force cannot be geared without limit, because **machine itself must survive transmitting it**:

> **Every machine states the most `St` it can deliver, whatever is driving it.** A paper blade at a million RPM still will not cut steel.

Ceiling is property of construction, not drive, keeps better machine worth buying once gearing exists. Gearing gets you to machine's ceiling; only better machine raises it. Without cap, one long gear train makes every hammer in game equivalent and reduces entire force axis to how many cogs you were willing to place.

Also gives force problem **second answer**, which §5 asks for. Re-gear drive, or fit shorter throw: both buy force, cost differently. Shorter throw free but spends precision — same hard blow reaching threshold also overshoots past state wanted. Gear train keeps gentler blow's control and spends capital and space instead. Neither dominates, player understanding only one can still get there.

> **[OPEN]** Whether gearing down needs *ongoing* cost or capital plus ceiling enough. Gearing preserves work per second, so on paper gear train buys force free once built. May be correct — real gearbox does exactly that, and cogs, space, machine's own ceiling genuine costs. But if gearing turns out strictly better than every other route to force, needs one.
>
> Building narrowed this without closing it. Friction and mass referred through **square of gear ratio**, standard mechanical result, not installed for balance — so geared-*up* branch charges network continuously for spinning faster, geared-*down* one genuinely costs less to turn. What remains open is only half that matters: machine's working draw is flat figure stamped on block, so gearing down to reach hardness floor is, today, free apart from cogs. Referring working draw too would be more honest and make gear train way of **fitting more machines on one wheel**, balance decision not only physics one.

Note throw deliberately **not** scalar (§3). Two named options make this decision; numeric throw would make optimization problem with right answer.

Same component reads differently on machine whose product is *displacement* rather than work. Bellows moves air in proportion to how far plates travel, so there long throw is useful one and figures run other way. One lever, two figures of merit, no rule needed for either — §5's composition test passing on single part.

Close to ideal shape for mod mechanic, none of it had to be invented. Trade real machinery already makes, expressed entirely in quantities mod had already defined, converts number printed on block into decision player makes.

### Su, mB, and borrowing on purpose

`Su` follows Create's Stress Units for mechanical load, `RPM` follows Create for rotational speed. Familiarity and ecosystem compatibility matter more here than owning vocabulary, so convention kept rather than replaced. Also why `Su` must **never** drift into meaning speed.

`mB` same judgement. Fictional fluid-volume unit considered and rejected: Minecraft modders already read millibuckets fluently, fluid systems already supply connectivity and direction, new abstraction would have bought nothing.

Small, genuinely useful accident here. Bucket is 1000 mB and fills one block — one cubic metre, 1000 litres. **So 1 mB is exactly 1 litre.** Minecraft's fluid unit already SI, so `Mu` can be anchored same way: define 1 Mu as 1 kg and water has density exactly `1 Mu/mB`. Every other material's density then reads as real-world value, free, no conversion table, no arbitrary scale to invent. Worth taking.

### Eu is electrical, and it is not a scalar

`Eu` carries **voltage and current**. Two `Eu` values with same product not interchangeable, making it only quantity in mod that isn't single number.

Deliberate exception to scalar rule in §3, not vector physics — two-component quantity of exactly the kind real electrical engineering runs on, large part of what gives Electrical character of its own alongside distinctive stopping behavior (§10).

Name was, for a while, biggest open question in whole vocabulary. Read as "Energy Units," `Eu` makes Feedback sound like IC2 or GregTech, where electricity quietly becomes universal abstraction every other system exists to feed. Precisely outcome §10 forbids.

Resolution: make `Eu` **specifically electrical** rather than generically energetic. Not mod's energy unit. Electricity's unit, sitting alongside `Fu`, `Tu`, rest as one system's currency among several.

### Work is deliberately unabbreviated

Generic, untyped energy called **`Work`** — spelled out, no abbreviation, no symbol. What a process asks for when it needs *some* energy and genuinely doesn't care which system supplies it.

Unabbreviated is the point. Short symbol looking like all others invites players to read it as another currency to convert into, and it is not one.

### What is deliberately absent

**Resistance not modelled.** Cleanest illustration of clause three: real, relevant to every electrical system ever built, earns place in Feedback only if it produces decision player finds interesting. Until it does, doesn't exist. Apply same test to anything proposed for this table.

**[OPEN]** Exact ranges, unit arithmetic, how voltage and current actually behave across wire belong in document 2.

---

## 18. Open Questions

Carried forward, not to be treated as settled:

- Mapping four separation mechanisms onto actual vanilla and modded material roster (§11).
- Exact machine roster and physical upgrade path for each process family (§14).
- How far vanilla reinterpretation extends beyond smelting — three vessels settled (§15), rest of vanilla's processing not.
- Whether Feedback ships Create compatibility, and what depth (§16).
- Mechanism and progression of local conversion covers — electrical → mechanical, electrical → thermal, any others (§10).
- Visual architecture of each factory era (§14).
- Where each Minecraft-exotic phenomenon first appears, what measurable about it, how natural form becomes engineered one (§2, §14).
- Full era arc beyond opening: how many broad eras exist, where "detect before you can act" bridges land, which discoveries pay off later (§14).
- Personal empowerment curve (§14).
- Ore deposit geometry and generation algorithm, and prospecting progression revealing it (§14). Candidate content lives in `docs/feedback_progression_roadmap.md`.

## 18a. Parked

Deliberately not being designed, recorded so not lost or accidentally started:

- ~~Food as a real thermal process.~~ **Lifted 2026-10-24** by the author; design in `docs/feedback_food_spec.md`. It was parked as scope creep against a slice about metal; the vessels, item heat and minigames it needed now exist.

---

## 19. Explicit Non-Conclusions

Distinct from open questions above: things that may *look* decided from reading design, and are not.

- Exact tier or era names.
- First machines, first materials.
- **McGuffnium is not a material.** Appeared in earlier discussion purely as metaphor.
- Exact numerical values, anywhere.
- Chemistry implementation.
- Separator-to-material mapping.
- Energy generation recipes.
- Progression boundaries.
- Control block's implementation.

Design strong enough to **constrain** these decisions. Not strong enough to have **made** them.

---

## 20. Where This Goes Next

Identity settled enough that next useful work is concrete content — but derived from progression structure above, not invented as item list.

Early game has clear shape as progression of *capability*, each step a sentence player could say:

> **Manual action** — "I can do this myself."
> **Primitive apparatus** — "I can make something do it repeatedly."
> **Continuous operation** — "It won't stop unless I stop it."
> **Timer automation** — "I can make it stop approximately when I expect."
> **Measurement** — "I can actually observe the process."
> **Control** — "I can make another component respond to what I observe."
> **Precision** — "I can build a system that holds a specific condition."

Gives first stage single coherent identity: **player moves from performing processes to building apparatus that performs processes.**

One plausible shape for slice, illustrative only: begin with forgiving process such as copper — broad temperature tolerance, low requirements, heating rate barely relevant — so vanilla-grade equipment suffices. Introduce mechanical material working, by hand at first. Primitive machine automates repetition, doesn't recognize completion. Player controls manually, then with timer. Then more demanding thermal process arrives needing narrow, sustained temperature range: technically attemptable with primitive gear, but unreliable and wasteful. Player acquires measurement, can finally see process, eventually acquires control components and closes loop.

By end of slice player should understand, without being told any of it:

machines perform physical operations; machines do not know when finished; continuous operation alters finished products; manual production possible but slow; timers are crude automation; measurement separate from control; sensors control nothing and actuators know nothing; precision belongs to whole system; process conditions matter; rates matter as much as quantities; machine state persists; physical upgrades change behavior; better machines improve old work as well as enabling new; several physical solutions can satisfy one process; strange Minecraft phenomena useful before understood.

They should leave thinking less:

> "What machine do I need for this recipe?"

and more:

> "What physical process is this, what does it require, and how do I build something that makes it happen reliably?"

### The method for filling in content

For each early-game capability, answer in order:

1. What actual problem does player encounter?
2. What physical process solves it?
3. What does that process require?
4. What can player already do manually?
5. What primitive apparatus automates it?
6. What makes that apparatus imperfect?
7. What measurement capability eventually improves it?
8. What control capability eventually improves it?
9. Which existing processes get better as consequence?
10. What becomes economical without being artificially unlocked?

### Start from the problem, and keep the slice small

Work backward from problem, never forward from machine:

> **problem → required process → required capability → machine → material → recipe → progression consequence**

Opening question for whole content effort is not *what is first machine?* or *what is tier 1?* or *what is first ore?* It is:

> **What is the first interesting problem Feedback asks the player to solve?**

Slice must be small enough to actually build. Not four hundred materials and complete chemistry engine — closer to **ten to twenty items, five to ten machines, one energy system, one or two process variables.** Make that work, then play it.

Matters more here than most mods. Feedback's entire philosophy rests on emergent behavior, and emergent behavior cannot be validated on paper. Prototype only way to find out whether manually controlling a temperature is genuinely fun or unbearable after five minutes, or whether process specification reading beautifully is incomprehensible in play.

First content pass fills in **one complete playable vertical slice** — not whole mod.