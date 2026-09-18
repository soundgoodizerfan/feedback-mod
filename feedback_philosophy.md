# Feedback — Design Philosophy

> **Document 1 of 3.** This is the *why* document. It states what Feedback is, what it believes, and which rules any proposal has to satisfy. It deliberately stops short of mechanism.
>
> - **1 — Philosophy** *(this document)*: identity, principles, constraints. Worked examples appear only where they prove a principle is real.
> - **2 — Mechanics**: how each principle is actually implemented. Units and their arithmetic, the sensor/actuator matrix, the thermal model, the energy networks, overrun band tuning, the control node set.
> - **3 — Content**: the roster. Every item, material, machine, fitting and process, and what each one does.
>
> Keeping these separate is load-bearing, not tidiness. Mixed together, you end up revising the philosophy every time a hypothetical copper furnace turns out not to make sense.
>
> This document supersedes `feedback_notes_i.md` and `feedback_notes_ii.md`, which were write-ups of earlier design conversations. Where those two disagreed, the disagreements are resolved here.
>
> **Status:** the principles below are settled enough to constrain decisions. The numbers, names, and rosters are not, and mostly do not exist yet. Questions still genuinely open are marked **[OPEN]** inline and collected in the final sections. Adding an `[OPEN]` is a legitimate outcome of design work; quietly resolving one is not.
>
> **This document is revised as the mod is built, and that is the intended behaviour.** A model that survived a year of playtesting unchanged would mean either that it was imagined perfectly before anything existed, or that nobody was paying attention to what the thing actually felt like. Building reveals errors that no amount of reasoning finds — §17's treatment of force and work was wrong until a hammer existed to be wrong about — so when implementation contradicts this document, the document changes. What does not change casually is the **identity** in §1 and §2: those are what the mod *is*, and revising them means making a different mod.
>
> Corrections are recorded rather than tidied away. A section that says *this was got wrong once, and here is why it was tempting* is more useful than one that was always right, because the error is usually the reusable part.

---

## 1. What Feedback Is

Feedback is a Minecraft technology mod about **building and controlling physical processes** rather than unlocking recipe-specific machines.

Its central mechanic is a single sentence:

> **Machines do not recognize when a recipe is complete.**

A machine keeps performing its physical action — heating, spinning, agitating, reacting — for as long as it has input and power, regardless of whether the output is already finished. Nothing inside the machine knows the difference between "working" and "done."

So the player has two separate problems, and must solve both:

1. **Make the process happen.**
2. **Notice that the desired state has been reached, and stop or redirect the process.**

Every other system in the mod exists to serve one of those two problems.

### What is actually novel here

Be precise about the claim, because a nearby claim is already taken. GregTech 6 has machines that idle and burn fuel indefinitely unless manually covered or unplugged. "Machines that don't switch themselves off" is not unclaimed territory.

The novel part is what happens next:

> **Continued operation past completion acts on the already-finished output**, not merely on the energy bill.

The output is *perishable to continued processing*. Overrunning doesn't waste a process — it consumes the thing the process just made. No existing tech mod does this, and it is the differentiator. Keep this framing when describing the mod to anyone.

### What the mod is not

It is not a tech tree where each tier unlocks a stronger machine that does the same job. Progression in Feedback means an increasing ability to **observe, manipulate, control, scale, and reproduce** physical processes. A new tier that only makes the same operation faster has not advanced anything.

---

## 2. Scientific, Not Realistic

Minecraft's rules are treated as the actual laws of nature.

If something strange demonstrably happens in Minecraft, it is a **real phenomenon in the setting** and can potentially be industrialized. Blazes, Ender Pearls, lightning that answers a Channeling trident, Creepers, Chorus Fruit — these are not fantasy exceptions bolted onto a physical world. They *are* the physical world, and nobody has industrialized them yet.

This is a deliberate departure from TFC and GregTech, which mostly strip out or route around Minecraft's fantastical elements in favor of real-world physics. Feedback does the opposite. It asks:

> **How would a scientifically-inclined mind industrialize this?**

The goal is that new technology feels like a **diegetic discovery** — something this world always permitted and nobody had built — rather than a tech layer dropped on top of a fantasy game.

### Real science is the method, not the target

The mod borrows scientific *habits*: measurement, controlled experiment, repeatability, process conditions, characterization, instrumentation, scale-up, engineering. It does not borrow science's completeness. A variable belongs in Feedback because it creates an interesting engineering decision, never because reality has that variable. (See §3.)

### Magic is baked in from the beginning

Minecraft-exotic phenomena should surface wherever the player has enough understanding to notice, measure, or exploit them — not be quarantined into an early-game gimmick or a final "magic tier."

The player does **not** need to understand a phenomenon before exploiting it. **Recognizing a phenomenon and understanding it are different events, and the first is enough to act on.** A scientist handed a Blaze Rod does not say *I lack the theoretical framework, so I will ignore this.* They say *this thing is incredibly hot — what can I use that for?*

The full pattern:

> **Observe → exploit → characterize → control → reproduce → engineer**

A Blaze Rod is the model case. The first Blaze technology can be genuinely primitive — Blaze Rod plus a crude heat exchanger gives a dramatically hotter working fluid, and the player has no idea why. That is enough. Later they discover it holds heat unusually well when insulated; later still they measure its emission rate, find a relationship between its state and its temperature, stumble onto some bizarre interaction with another material, and eventually understand enough to manufacture the phenomenon deliberately.

Iron does not need to walk that whole sequence, because humans already understand iron. Blaze material, redstone, Ender phenomena, Sculk and Nether materials all can. This is also where the compendium earns its place — not as a pile of late-game explanations, but as **a reservoir of hypotheses about what an engineer might notice** when handling Minecraft's stranger materials. A natural Blaze Rod is not a disposable early fuel — it is an existence proof that motivates an entire branch of thermal engineering.

The guiding question for any exotic material is:

> **What does this impossible thing actually do, and what can we build once we understand it?**

### The compendium's role

The user's own *A Speculative Physics and Biochemistry Compendium of Minecraft (Vanilla and Modded)* (`speculative_physics_inspo_doc.md`) is a **mindset reference and a parts bin, not canon.**

Feedback is not obligated to adhere to its lore. But its reasoning style — given that this demonstrably happens, what is the smallest departure from known reality required to make it work? — is exactly the reasoning style this mod should apply to its own content. And because it is the user's own work, its ideas may be lifted wholesale. Several already have been: the Liquid Teleportant processing chain, and the treatment of Redstone as a deliberately flagged exception sitting *alongside* the real periodic table rather than being forced into invented chemistry or dismissed as magic.

---

## 3. Priorities, and the Anti-Simulator Constraint

**Mechanical depth > playfulness > realism.** In that order, when they conflict.

Feedback abstracts aggressively wherever additional realism does not produce an interesting decision. The test for including any mechanic, variable, or unit:

> **Does modeling this create a meaningful engineering choice for the player?**

If not, abstract it away. "Real engineering has this variable" is not an argument. Neither is "it would be more accurate." Stated as a scope constraint:

> **Feedback models a phenomenon when doing so creates an engineering decision — not because the phenomenon exists in reality.**

Mass earns its place because density creates real separation problems. Pressure earns it because it creates apparatus tradeoffs. Heating *rate* earns it because thermal history matters. **Entropy does not**, because nobody makes a decision based on entropy — and neither, so far, do enthalpy, specific heat, viscosity, field strengths, or reaction constants. Electrical resistance is the standing example of this rule being applied rather than discussed (§17).

The system is built from the gameplay outward, with reality as inspiration and consistency check rather than specification.

This constraint has teeth, because the mod's premise makes it very easy to accidentally build a process-engineering simulator that nobody wants to play. Some specific guardrails:

- **No vector physics.** The mechanical model is scalar: how much, how fast, how strong, and — where the mechanism genuinely cares — which rotational direction. Not 3D force resolution.
- **No molecular chemistry.** Chemistry is a useful abstraction over reaction paths and byproducts, not a general-purpose reaction simulator. (See §12.)
- **No derivative-unit sprawl.** Derive rather than invent: `Tu/t` and `mB/t` are sufficient. Do not coin a bespoke unit for every rate.
- **No physics homework.** The player should never need to do arithmetic on paper to operate a machine.
- **No chores wearing a decision's clothes.** A mechanic that is real, well-precedented, and simply **not fun** gets cut anyway.
- **Discrete options, not continuous knobs.** Where a setting would otherwise be a free scalar, make it a small set of named choices instead. See below.

That last one needs its own statement, because it is the clause most likely to be argued around:

> **Some mechanics suck. Being realistic does not save them, and another mod having shipped one does not either.**

**Electrical resistance is the standing example.** It is real, it is fundamental, several mods model it — and reasoning about it is tedious every single time. It does not exist here (§17). Path-checking data links is the same call: routing a cable around a wall is busywork, so links pass through walls (§13), while the range limit stays because *placement* is a genuine choice.

Be careful not to over-apply this. It is a test about **decisions, not about categories.** Shafts and pipes are still real blocks the player places, because laying out a mechanical network is a genuine spatial and budgetary puzzle (§10). What gets cut is not the physical infrastructure — it is the unfun *behaviour* that could be simulated inside it. Keep the shaft. Skip the fluid dynamics.

### Options, not micro-optimization

A related trap, and one this mod is unusually exposed to: a mechanic can create a genuine decision and still be ruined by being *continuous*.

> **Prefer a small set of named options to a free scalar.**

A crank throw that is **short or long** is a decision. A crank throw that is any number from 1 to 64 is a spreadsheet. The difference is not depth — both offer the player control over the same physical quantity — it is that the scalar version has a *correct answer* sitting somewhere in it, and the discrete version does not.

That correct answer is the problem, because it leaves only two ways to play. Grind out the optimum and feel the time was wasted on a tenth of a second, or skip it and play on **knowing** the factory is needlessly suboptimal. Both are worse than simply not having the knob.

This mod already works this way almost everywhere — thermal mass is small or large, insulation is present or absent, instruments come in tiers, controllers come in tiers — and naming the rule makes that deliberate rather than accidental. Depth should come from **stacking choices** (§5), never from tuning one of them finely.

Put in plain words, so it can be applied without re-deriving it:

> **A good choice is a design decision — where to put it, how hard to run it, what to spend. A bad choice is sitting and tuning a number.**

And the best ones are not presented at all. A choice the game *offers* is a menu; a choice that is a **consequence of the system** is something the player worked out. Friction rising with speed is not a difficulty setting and no screen mentions it — but it quietly means a long shaft run cannot turn as fast as a short one, so *slow and wide* and *fast and narrow* become two real answers to the same problem. Nobody was asked to pick. The choice was there to be noticed.

That is also the difference between this and a **hard limit**, which is the same idea done badly. A per-component cap on throughput produces the absurdity of a generator being *too good* for the parts downstream of it, and turns an engineering question into a shopping question. A cost that scales produces the same caution without ever refusing anything.

The goal is not to simulate reality. It is to make Minecraft's physical processes **coherent enough that players can reason about them and construct their own solutions.**

---

## 4. Machines Are Physical Operations

A machine defines the **physical process it performs**, not a list of recipes it knows.

A machine should never say:

> "I know how to make Copper Plate."

It says, in effect:

> "I apply this operation to whatever is placed in me."

Heating, cooling, grinding, pressing, mixing, pumping, separating, filtering, reacting, phase-changing, cutting. The machine is a way to repeatedly apply an operation under defined conditions. It has no opinion about what you are making, and — per §1 — no idea whether you are finished.

### Upgrades are physical components

An upgrade must be something a person could point at on the machine: an enlarged vessel, thicker insulation, a larger flywheel, better bearings, a finer screen, a heat-resistant lining, a better seal, a precision valve, a pressure-rated vessel, a more capable agitator, an improved heating element.

Components must make sense for that machine and respect its material limits. **"+50% throughput" is not a design concept.** The question is always: *what physical change causes this machine to handle more throughput?*

The two real development axes are **throughput** and **precision/controllability**. Not recipe unlocks, and not voltage gates.

### Better machines improve old work

A better saw cuts copper more efficiently. It does not exist because copper became a tier-3 material.

New capabilities should emerge because improved equipment makes a previously impractical process *economical* — not because a machine was handed a longer recipe list. An advanced machine may process larger batches, hold tighter conditions, react faster, survive more severe conditions, or offer a better physical basis for instrumentation. It should not be the same machine in a more expensive metal.

### Observation is a separate layer

Machines do not get smarter as they get better. Sensing is a distinct investment (§8), delivered through covers that attach to a block face without consuming block space and compete for a machine's limited attachment slots alongside power and redstone I/O.

The consequence is deliberate: **a sophisticated machine can be difficult to control if it is poorly instrumented, and a crude machine can become remarkably reliable once the player learns to measure it.**

---

## 5. Recipes Are Process Specifications

JEI is assumed essential, and a recipe entry is a **specification of what must physically happen**, not a machine-specific unlock.

A process entry should communicate whatever is relevant among: inputs, outputs, the required operation, temperature, pressure, mechanical requirements, duration, flow or concentration, tolerances, significant rates, byproducts, and any other decisive physical condition.

A recipe answers:

> **"What must physically happen to this material?"**

not:

> "Which machine do I put this in?"

Because the specification is physical rather than machine-bound, **a process may have several valid implementations.** The world does not care what the player built. It cares whether the conditions were met.

### The requirement vocabulary is small

Nearly every process condition falls into one of a handful of requirement types:

**State** (1497–1501 Tu) · **Threshold** (≥ 200 Pu) · **Rate** (heating ≤ 20 Tu/t) · **Duration** (maintain 600 t) · **Total quantity** (apply 500 Work) · **Throughput** (≥ 10 mB/t) · **Sequence** (heat → hold → cool) · **Composition** (reagent ≥ some Qu/mB) · **Direction** (force along the machine axis)

Note what is absent from that list: **"requires tier 5."** There is no such condition, and there is no place to put one. A tier is not a thing a process can require — it is a description of what the player happens to be able to satisfy.

### The recipe screen should be diagnostic

Because both sides are expressed in the same units, JEI can show the player *why* something isn't working — process requirements next to the apparatus they actually have:

```
REFINED [MATERIAL]                  YOUR APPARATUS
Heat to 1499-1501 Tu                Max temperature   1350 Tu
Maintain 600 t                      Control stability ±40 Tu
Max heating rate 25 Tu/t            Measurement       ±25 Tu
Atmosphere: reducing
```

This is enormously more useful than **Requires Tier 5 Furnace**, because the player can read off exactly which capability is short — and in this example it is not the one they would have guessed. The furnace cannot reach the temperature *and* the instrument could not confirm it if it could.

### Infinite ways to skin the cat

A major design goal is a Create-like compositional sandbox, pushed further toward process engineering. Give the player physical constraints, components, process specifications, energy systems, sensors, actuators, and control logic — then let them build solutions.

There should usually be many ways to accomplish a process. **Some of them should be objectively bad and still perfectly valid.** The game should not prescribe one intended chain wherever the underlying physical problem admits alternatives.

This also supplies a test for whether a new mechanic earns its place, and it is a different test from §3's. §3 asks whether a variable creates a decision. This asks what that decision **composes with**:

> **The more choices each system offers, the more they stack into things nobody designed. That is the difference between a tech mod and a science mod.**

A tech mod hands the player a sequence of better machines. A science mod hands them parts whose interactions it never fully enumerated, and lets them find the combinations. Every mechanic should therefore be judged partly on how many other mechanics it can be stacked with — a system that composes with three others is worth more than one twice as deep that composes with none.

### A problem with one answer is not a problem

> **An automation problem with a single correct answer is not a problem. It is a request that the player build somebody else's design.**

This is the sharpest form of everything above. A puzzle with one solution is a construction manual with the steps hidden, and solving it teaches the player nothing except what the designer was thinking. The interesting question is never *what is the answer* — it is **what do you value more.**

Feedback should therefore keep producing situations where two approaches are both correct and cost different things: capital against attention, throughput against reliability, capability against the ability to walk away. Not because balance demands it, but because that is the only kind of question worth asking someone who is building a factory.

### Emergent trades are the design's own test

The best of those situations are the ones nobody placed.

The crucible and the Crude Blast Furnace trade capital against attention because that trade was deliberately designed (§15). Two hammers versus a bigger water wheel make **the same trade**, and nobody designed it at all — it falls out of strokes taking time, workpieces cooling, and items being movable, three unrelated rules meeting somewhere nobody was looking.

That gives the project a check it can actually run:

> **When a trade the mod already believes in shows up somewhere it was never installed, the systems are right. When every interesting choice has to be placed by hand, they are not yet.**

It is worth watching for deliberately as more systems are added. A mechanic that produces one of these is worth more than its own depth suggests, and a mechanic that never does — however elaborate — is probably sitting alone.

Three players making the same material:

- **A** builds an elaborate closed-loop furnace with precise sensors and automatic fuel control. Best throughput.
- **B** builds a deliberately oversized furnace with a crude thermostat and an enormous thermal mass, letting sheer inertia hold the temperature. Cheap, and surprisingly robust.
- **C** babysits a weird little experimental kiln by hand. Almost no infrastructure, and half their life spent standing next to it.

**All three work.** Feedback should never ship a *[Material] Refinery™* — a single block that is the answer. It ships components and constraints, and the player is the one who designs the system. Crucially, this costs nothing to support: because requirements are independent of machine identity, the mod never has to enumerate the valid combinations. It only has to check conditions.

---

## 6. The Core Loop: Idle, Running, In-Window, Overrun

Every machine moves through four conceptual states:

- **Idle** — loaded, powered, waiting.
- **Running** — consuming energy, performing its action, progress accumulating.
- **In-window** — the desired result technically exists. The machine has not been told to stop.
- **Overrun** — continued action is now acting on the finished product.

### The severity ladder

Overrun is not a single "ruined" outcome, and it is not always a friendly sidegrade. Both extremes must exist:

| Tier | Outcome |
| :--- | :--- |
| **Sidegrade** | A different, still-useful output. Usually a narrow band; hitting it deliberately requires understanding the machine. |
| **Degrade** | The same output, worse yield or worse stats. The "I was a little late" result. |
| **Spoil** | Total loss. No byproduct, no partial credit. |
| **Hazard** | The overrun damages the machine, or produces something actively dangerous. |

**This ladder is not specific to overrun.** It is the outcome model for *getting a process wrong in any direction.* Underheat a recipe and the same four outcomes are on the table: nothing happens at all, the material degrades, it is ruined outright, or something dangerous occurs. Overrun is simply the most interesting case, because it is the one where the player already had the thing they wanted and processed it into something else.

**Band width is a tuning knob, and it is the mod's main difficulty curve.** It varies per machine — a forgiving machine has a long degrade band and no hazard band, while an exothermic reactor or a pressurized vessel might jump almost directly from in-window to hazard — but it varies far more importantly **per material, and it tightens as the game goes on.**

Copper is forgiving. There are very few ways to actively destroy copper, and a player who mishandles it mostly wastes time. A late-game material can be ruined by breathing on it wrong — and that is fair, **because by the time a player is handling it, they should know better.** Forgiveness is the thing that gets scarce.

This is a far better difficulty curve than gated recipes, because it never tells the player no. It just quietly raises the standard of care, and the player's growing instrumentation is what keeps pace with it — which, as §14 argues, means the game does not actually get harder. It is also §7's economics in another form: tightening bands are what turn wasted raw material from an early annoyance into a serious cost.

GregTech 6 supplies precedent for the top of the ladder being real: its boilers genuinely explode with a real blast radius when steam flow is over-restricted.

### Overrun is sometimes just another process

A grinder taken past completion produces **coarse → fine → powder**. That is not damage; it is a legitimate technique for obtaining finer material, paid for in yield.

This is the strongest possible confirmation of §4. If a machine performed discrete recipes, "keep grinding" would be meaningless. Because it performs a physical operation, continued operation naturally walks the material further along a real state axis — and whether that counts as overrun depends entirely on what the player wanted.

Early sketches to build from:

- **Furnace** — past completion it keeps absorbing heat. A narrow band work-hardens the product into a harder, more brittle variant (sidegrade); beyond it, slag (spoil).
- **Mixer** — overmixing shear-thins the mixture, separates it back into components, or whips in air to yield a foam that feeds a different chain (sidegrade).

---

## 7. Possible, Reliable, Economical

This is the most important progression rule in the mod.

A **process has requirements.** An **apparatus has capabilities.** A recipe is not locked merely because the player lacks the right apparatus. Instead, distinguish three separate questions:

- **Possible** — can the player physically cause this to happen at all?
- **Reliable** — can they cause it consistently enough to depend on?
- **Economical** — can they cause it at a sane rate, cost, and maintenance burden?

A demanding process may be technically possible with crude equipment, manual work, guesswork, and an enormous amount of attention. **The player should be allowed to do surprising things early if they are willing to suffer for it.**

> **The recipe is not locked. The process is difficult.**

### Hard gates and soft gates

> **A hard gate is physical impossibility. A soft gate is inability to reliably control.**

**Hard gates** — insufficient `St`, insufficient total `Work`, insufficient maximum temperature, insufficient pressure rating, insufficient flow capacity, the wrong material entirely, vessel and material limits. The apparatus simply cannot reach the required condition, and no amount of patience changes that.

**Soft gates** — inadequate measurement accuracy, coarse resolution, slow response, poor control granularity, an unstable process, high sensitivity to disturbance. The apparatus can reach the condition. The player cannot reliably *keep* it there, or cannot confirm they did.

This distinction matters enormously, because it is what stops "tier 5" from creeping back in through the side door. And it resolves what looks like a contradiction: **precision genuinely gates progression, and precision is never a lock.** It gates by making things unreliable and uneconomical, which is a soft gate by definition.

The sharpest form of the idea:

> **An apparatus does not need to lack physical capability to be gated. It can lack sufficient knowledge or control.**

Worked example. A process needs 1499–1501 Tu. Your furnace can physically sit at 1500 Tu all day. But your thermometer's resolution is 10 Tu, so you cannot tell whether you are at 1499, 1500, or 1505. The heat is not the problem. **Knowing** is the problem — and the fix is an instrument, not a bigger fire.

Note carefully what a soft gate does *not* do. It does not make the process fail. If the material really is at 1500 Tu, **it works** — the world does not consult your thermometer before deciding (§8). What you have lost is not success. It is **reproducibility.** You can absolutely stumble into a demanding process by luck, and hand-hump a startling amount of this mod that way. Doing it a second time on purpose takes either a great deal more luck, or a better instrument.

A soft gate is therefore a **reproducibility gate**, and that is exactly why it belongs in the *reliable* column rather than the *possible* one.

### The gate is almost never reaching the condition

This is worth stating bluntly, because it inverts how tech mods normally work: **the difficulty is rarely getting the material to where it needs to be. It is knowing how to get it there, and being able to do it again.**

Even a miserable furnace, given the right fuel, can physically get hot enough to bake almost anything. The heat was never the obstacle. Without a thermometer, the player simply has no way to know whether they hit the window — so they find out the only way left available to them, which is by throwing things at the wall and looking at what comes back out.

Which means the honest description of an early brute-force attempt at a demanding material is:

> **You can absolutely make the hardest ingot in the mod. You will start with six stacks of raw material and end up with two ingots — unless you put real work into the *how*.**

Nothing was locked. The recipe was available the entire time. The player simply paid for their ignorance in raw material, which is the correct currency for it.

Two things follow, and both are design requirements rather than observations.

**Failed batches must consume their inputs.** If failure were cheap, brute force would be the optimal strategy and instrumentation would be a hobby. The waste *is* the gate. It is the only thing standing between "you may attempt anything" and "you may as well attempt everything."

**Raw material abundance is therefore a per-material tuning knob** — and a far better one than a tech lock. A process on an abundant input stays brute-forceable for a long time; the same process on something scarce punishes guessing immediately. The designer sets how accessible a process is by deciding what it eats, never by deciding who is allowed to attempt it.

The cleanest way to read the whole system: **instrumentation is a yield technology.** A thermometer is not a key and unlocks nothing. It is bought because it turns six stacks into two ingots into six stacks into most of six stacks, and it pays for itself in conversion ratio like any real capital investment.

### The control envelope

Every process occupies a region in a multidimensional space: temperature, pressure, composition, flow, time, atmosphere, force, and whatever else is relevant. Some regions are enormous. Some are pinholes.

> Copper: 190–230 Tu, any ordinary heating rate, almost any holding time.
>
> Something demanding: 1497–1501 Tu, specific atmosphere, specific heating history, 600 ± 20 t.

The player's equipment determines the size and shape of the region they can actually command — from *I can probably keep this vaguely hot*, to *I can hold approximately 1500 Tu*, to *I can hold 1499 ± 1 Tu for as long as you like.*

**The recipes never get higher-tier. The player's reachable precision grows.** Progression is the expansion of the control envelope, and "what tier am I?" is better asked as:

> **What level of physical reality can I reliably command?**

A useful consequence: a process has a **feasibility profile** rather than a locked/unlocked bit. The same process might be *technically possible* by hand, *extremely difficult* with a crude machine, *feasible* once instrumented, and *efficient* once automated — which cleanly separates **access** from **viability**. And if a player assembles some unreasonable laboratory that genuinely satisfies the requirements far ahead of schedule, **the mod should let them.** That is a real high-skill route through progression, not an exploit.

### Difficulty is multidimensional

"Hard" is not one thing. A process can be hard because it is:

- **Narrow** — a very small acceptable range.
- **Fast-changing** — conditions must be adjusted quickly.
- **Long-duration** — conditions must be held for a long time.
- **Coupled** — changing one variable moves another.
- **Poorly observable** — the state that matters isn't directly visible.
- **Out of reach** — you can measure it fine; the apparatus just cannot get there. (The only kind that is a hard gate.)
- **Unstable** — it runs away unless actively controlled.
- **History-sensitive** — the result depends on *how you got there*, not merely where you ended up.

That last one deserves emphasis, because it is what makes instrumentation matter beyond hitting a number. A state-based process says *get this to molten temperature.* A path-dependent one says *heat at roughly this rate, reach this temperature, hold it, then cool no faster than this.* The second cannot be satisfied by luck, and it ties process requirements directly to the thermal memory in §9 — without ever labelling anything an "advanced recipe."

### Manual production stays possible

Most processes should remain theoretically reproducible by hand given enough knowledge, tools, workspace, and attention. Manual rates should be tiny — one item at a time, or an occasional success — so that automation becomes necessary for *practical scale* rather than because the recipe is metaphysically machine-only.

A "100% Feedback, no automation" run — call it Amish% — should be funny, miserable, and technically viable for a surprising share of the game. The costs are exactly what they should be: negligible throughput, constant attention, poor repeatability, no scaling, and high waste.

Automation is therefore **a way of making physical processes practical, not a key that unlocks recipes.**

---

## 8. Measurement, Control, and Actuation Are Separate

This is arguably the central architecture of the mod. Four distinct things exist, and the player assembles the connections between them:

- **Process physics** — the machine changes state according to its inputs and losses.
- **Measurement** — a sensor observes some aspect of that state.
- **Control** — a controller interprets the measurement and decides what should happen.
- **Actuation** — an actuator changes the physical system.

Worked example, which is the whole philosophy in one line:

> **Thermometer → Controller → Heat Pump → Furnace**

The furnace has no target temperature. The heat pump has no target temperature. The thermometer controls nothing. The controller does not magically know what temperature the process wants. **The player builds the feedback loop**, and the resulting behavior — a temperature oscillating inside a chosen band — exists nowhere in the components and only in their assembly.

This generalizes past the loop, and it is close to the thesis of the entire mod:

> **No part holds all the wisdom — not even the controller. A machine is far more than the sum of its parts, because none of the parts work alone.**

The vessel does not know the recipe. The sensor does not know what matters. The actuator does not know why it was asked. The controller does not know the target. Every component is deliberately, permanently ignorant of the thing it is participating in, and the competence belongs to the arrangement. **The player is the only thing in the system that understands what is happening**, and that is not a limitation of the design — it is the design.

Without the loop, the same job is done by hand: heat, observe, adjust, wait, observe, adjust. Entirely possible. Entirely tedious. That contrast is the mod.

### Accuracy vs. precision

Keep this distinction explicit, because players conflate them and the mod depends on the difference.

- **Accuracy** is a property of the *instrument*: how close the reported measurement is to the real state.
- **Precision** is a property of the *whole assembled system*: whether it can obtain good enough information and respond fast enough to hold the desired state.

Therefore both of these are true:

> If you cannot sense something, you cannot precisely control it.
>
> If you can sense it accurately but cannot react quickly enough, you still cannot precisely control it.

**Precision is emergent.** It is never a stat printed on a machine, and never a recipe requirement.

### The world resolves on truth, not on readings

**A process succeeds or fails on its actual physical conditions. The player's instruments never enter that calculation.** A sensor is a window, not a participant. Nothing in the mod should ever compute an outcome from a displayed value, and no process should care how well it was being watched.

Two consequences worth stating plainly, because everything else in this section depends on them.

**Luck is real and legitimate.** If the vessel genuinely sat in the window, the batch works — whether the player planned it, guessed it, or had no idea it happened. A player with crude equipment and enormous patience can hand-hump their way through a surprising amount of this mod on accident. That is a feature, and it is §7's *possible* column doing its job.

**Variation is real, and some of it is genuinely random.** Much of what the player experiences as noise is their own missing information — fuel quality, wear, ambient conditions, batch load and preheat are real hidden variables (§9) that an instrument can turn into facts. But not all of it, and the difference is deliberate.

There must be an **irreducible noise floor**. No apparatus, however good, should ever reach zero variance.

This is the most important tuning constraint in the mod, and it exists to protect the second gimmick. If a sufficiently well-built factory can become genuinely deterministic, then the correct endgame strategy is to measure everything once, write down the numbers, and run the entire base on timers forever. Sensing becomes a scaffold the player discards. **The whole measurement-and-control layer would be a phase rather than a system.**

Noise is what makes instrumentation *permanent*. A single reading goes stale, so the player cannot measure once and schedule forever — they have to keep watching, which is the behavior the mod is actually about.

It is also honest. Real processes are not meaningfully predictable in every part; cleanrooms that approach it exist, and they are extraordinary and expensive achievements rather than the default state of a workshop.

And it is cheap, which matters. Simulating a dozen hidden variables to produce a wobble the player cannot distinguish from `/dev/urandom` is a large amount of computation buying nothing. **§3's test applies to randomness exactly as it applies to everything else:** model a variable when the player can act on it, and let the rest be noise. Preheat is worth simulating because the player can preheat. The thousand small things that make a real furnace wander are not, because there is no decision on the other end of them.

What the noise must **not** be is arbitrary. The requirement is that variation be *learnable in aggregate* even where it is unpredictable per-instance: it has a knowable center, its spread responds to conditions the player can influence, and better equipment **narrows the distribution without ever collapsing it.** A player should be able to say "this runs hot when the fuel is poor" and be right, while still never being able to say exactly how hot this particular batch will run.

> **Better equipment buys a tighter distribution, never a guarantee.**

### Senses give adjectives. Numbers cost an instrument.

One rule covers every measurable quantity in the mod:

> **The player's own senses report qualitatively and for free. Numbers must be bought.**

With no instrument, a vessel reads *cold · room temperature · hot · really hot!!!* — genuinely useful, genuinely imprecise, and exactly what a person standing next to a furnace actually knows. A part-worked ingot looks visibly flattened. Nothing is hidden and nothing is numeric.

Instruments convert adjectives into figures, and each tier buys **significant figures**:

```
no instrument   "really hot!!!"
thermometer I    1000 Tu
thermometer II   1250 Tu
thermometer III  1256 Tu
thermometer IV   1256.3 Tu
```

Above those sits a different kind of instrument again: one that reports **change over time** rather than state — a temperature climbing at 18 Tu/t. This is not a luxury tier. §5's requirement vocabulary contains *rate* requirements, and a player with no rate readout cannot see whether they are violating one. **The delta instrument is what makes an entire class of process requirement playable.**

The rule generalizes past temperature without any new machinery. Mechanical work is qualitative for free — the ingot looks flattened — and numeric only with **calipers**, which turn *kinda flattened* into `16 / 20 Fu`. Pressure, concentration and the rest follow the same shape.

### What you need is free. What you have is a cost.

The adjectives rule needs a boundary, or it collapses into hiding things for their own sake. This is the boundary:

> **Knowing what a process requires is free. Knowing what your equipment is currently doing is not.**

A recipe is published data. Steel wants 1350–1410 Tu held for 600 ticks, heated no faster than 25 Tu/t — that is a figure from a handbook, and every real metallurgist has it. There is nothing to discover and nothing to gate. A recipe browser should state it in **exact units**, with no instrument owned and nothing unlocked.

What costs something is the other half of the sentence: *is my crucible in that band right now?* Requirements are public; **state is measured**, and measurement is what instruments are for.

This is the cleanest statement of §7 the document has. The recipe was never locked. The player can read precisely what steel needs on their first day, walk to a furnace, and still fail thirty times — because knowing the target and knowing where you are are different problems, and only the second one is for sale.

It also settles the whole information layer, which otherwise gets argued case by case:

| | Free | Costs |
| --- | --- | --- |
| **Mechanism** — how a thing works | Ponder scenes, §8 above | — |
| **Requirement** — what a process needs | Recipe browser, exact figures | — |
| **Equipment spec** — what a machine can deliver | Printed on the block (§17) | — |
| **State** — what is happening right now | Adjectives only | Figures, per instrument tier |

Everything a player could have learned from a manual is free. Everything they could only learn by *looking at their own factory* has to be instrumented. A tooltip may therefore say a workpiece needs 14 Fu; only calipers may say it has 9.

### Instruments never lie

**An instrument may never display more figures than it can stand behind.** A device accurate to ±25 Tu reads `1250 Tu`, never `1256.3 Tu`. Display resolution is bounded by accuracy, always.

> **A vague but entirely true answer beats a confident, smiling lie.**

This is not only a fairness rule, it is what keeps cheap instruments *worth owning*. A crude thermometer that reports honestly at low resolution is a real tool the player will keep using for the rest of the game. A crude thermometer that reports confident nonsense is not a worse tool — it is a worthless one, and worse than that, it teaches the player to distrust the entire instrument layer that the mod is built on.

**Miscalibration is not a lie, and remains available.** An instrument that has drifted through wear (§9) is a different thing entirely: it is not claiming false precision, it is a real physical object that has aged, and it is honest about how finely it can resolve. It is simply *offset*. That remains legitimate, and it is interesting exactly where fake precision is not, because the player can detect it, reason about it, and fix it — by checking the instrument against a known physical reference, which is a real and satisfying thing to have to do. The line is:

> **An instrument may become wrong. It may never pretend to be more certain than it is.**

### Some quantities are visible; some are not

Instruments do not all do the same job, because the quantities they measure are not equally observable to begin with.

A part-worked ingot is **visibly** part-worked. Anybody can see it is flattening, roughly how far along it is, and whether it has gone too far. Mechanical deformation announces itself.

Temperature does not. Looking at a furnace tells the player that there is a fire somewhere. That is the entire content of the observation, and it is nearly useless.

So the two instruments are different in kind:

- Where a quantity is already visible, the instrument buys **precision** — calipers turn *kinda flattened* into `16 / 20 Fu`. Useful for *aiming*, not for avoiding disaster, since the player could already see disaster coming.
- Where a quantity is invisible, the instrument buys **existence** — the thermometer is the difference between a number and nothing at all.

This should drive how urgently each instrument is needed, and in what order the player wants them. **An instrument that reveals is a bigger event than one that refines**, and the design should not treat them as the same beat.

### Some readings are free; some cost an action

A second split, cutting across the first, and it is about the *object being measured* rather than the quantity.

An **item** should never require an action to read. It is in the player's hand or in a chest, holding the instrument is enough, and demanding a right-click on top of that is a keystroke with nothing inside it — §3's test, applied to a mechanic that is real and well-precedented and simply not fun. Calipers work by being owned.

A **machine** is different, and the difference is not flavour. Measuring a machine means getting down and putting the instrument on it, and — this is the part that makes it a mechanic rather than a mood — **you cannot measure a machine that is running.** So taking the reading costs throughput. The player stops the line, learns something, and starts it again, which is §5's trade in a place nobody installed it: attention and downtime bought information, and a player who would rather keep producing may simply decline to know.

That gives the rule, and it is about price and not about what kind of object it is:

> **A reading is passive when taking it is free, and an action when taking it costs something.**

Item readings are free, so they are passive. Machine readings cost a stopped machine, so they are an act. Nothing has to be decided per instrument, and the same calipers do both — which is the point, because it is *one measurement* pointed at two things. A worn hammer head has mushroomed: it is wider and shorter than it was, and reading how far a piece of metal has deformed is exactly what calipers already do to a workpiece. The tool is not being given a second use. It is being pointed somewhere else.

**The precedent is the Thaumometer**, and it is worth naming because it got this exact split right years ago. You point it at things in the world to scan them, and with the right addons you stop having to scan items — anything you pick up is scanned automatically. Nobody experienced that as losing content, because pointing a scanner at something in the world is *fun* and remembering to swap your hotbar before opening your inventory is not. The rule above is that observation with a price on it, and the price is what tells the two cases apart.

**Wear has no unit, deliberately.** It is a plain percentage — 100% is a machine as built, and it falls. §17's units exist so that quantities cannot be silently converted into one another; a ratio of a machine to its own former self is not that kind of quantity, and minting `Wu` for it would be inventing a unit to describe *a lack of* one. It is also the one figure in the mod that is dimensionless, which is a fact worth stating once so nobody fixes it later.

It reads as **condition rather than wear** — how much machine is left, not how much is gone — because that figure multiplies the spec sheet directly. A hammer at 72% delivers 72% of the force stamped on it, and no mental inversion stands between the reading and the decision. Note that the number bottoms out above zero, since a spent machine is still a machine: rescaling the display so that spent read 0% would look tidier and would destroy the only property that makes the figure useful.

Snapshots are not remembered for the player, and that is a decision rather than an omission — the calipers report what is true when you click and nothing keeps it. A remembered reading is a *device*, and should be sold as one if it is ever worth having; short of that the player writes the figure in a book and quill, which is the same mechanic with better handwriting.

The corollary matters as much: a reading taken by an act is a **snapshot**, and a snapshot goes stale. That is where drift and recalibration live, and it is the honest reason a machine's condition should not simply appear on a HUD forever — the player knows what the hammer measured at the last time they stopped it, which is not the same as knowing what it is now.

And in this mod that gap says something, rather than merely being realistic. Wear accrues only on misuse, so a **correctly built line's reading never goes stale at all** — measure it once and the figure stays true indefinitely. A badly built one goes out of date quickly. How fast the player's knowledge rots is therefore proportional to how wrong the factory is, which is the same argument the wear mechanic already makes, made a second time by the measuring of it.

### Precision is not a unit — it is a set of apparatus properties

This is what keeps the previous point from being merely a slogan. Precision is not one number, and it must not collapse into one. For any variable, six properties describe what a piece of equipment can do with it:

- **Range** — can you physically reach this value?
- **Resolution** — can you distinguish values this close together?
- **Accuracy** — when the instrument says X, how close is it to reality?
- **Control** — can you deliberately hold X where you want it?
- **Response** — how quickly can you change X?
- **Stability** — how far does X drift when you stop intervening?

A crude thermometer reads 0–1000 Tu, ±25 Tu accurate, 25 Tu resolution, updating every 20 t. A late one reads 0–3000 Tu, ±0.5 Tu, 0.1 Tu resolution, every tick. Neither is "tier 1" or "tier 5." They are instruments with different capabilities, and **these six properties probably describe most of the progression of the entire mod.**

Critically, **none of them are new units.** They are attributes expressed in the units that already exist (§17), which is what stops the measurement vocabulary from breeding. The same is true of the other properties that decide whether a factory is any good — reliability, redundancy, automation level, labor requirement. They are not quantities the player reads off a gauge.

**Wear is the exception, and it was originally listed here in error.** Reliability is a statistical description of a fleet over time and there is nothing to point an instrument at; wear is one machine's own present state, and it is a *dimension* — a mushroomed hammer head is measurably wider than a new one. So it belongs with the quantities, it is measured with the calipers that were already measuring deformation, and it is measured by the act above rather than passively. They are the things that turn a process from *possible with somebody standing there* into *cheap industrial production*, which makes them §7's three questions wearing different clothes.

### Timing is a crude form of control

Timers are the cheap fallback, and they must be **genuinely beatable rather than merely risky**.

Process times should not be perfectly deterministic. They should drift with real, *learnable* conditions: fuel quality, machine wear, ambient temperature, batch load, preheat state, thermal mass, contamination. This is variation the player can come to understand and predict — **not arbitrary random noise**, which would teach nothing and merely punish.

A timer is a bet placed on current conditions. Sensors are how you stop betting. A timer can make a process economical long before the player has real instrumentation, and precision control should eventually and decisively outperform it.

### Quality should fall off gracefully, not switch off

Success should generally not be binary — in range 100%, out of range 0%. It should behave more like **a distribution centered on ideal conditions**, falling off smoothly as the process departs from them: excellent at 1500 Tu, excellent at 1499, mediocre at 1495, poor at 1480, junk at 1400. Width and shape are per-process, so one material is forgiving and another has a brutal optimum.

This is what makes timers degrade honestly instead of being banned. Suppose the player has measured that the furnace takes about 47 seconds, and sets a timer accordingly. It works. Then fuel quality varies, the machine wears, the load changes, the ambient temperature shifts — and the process drifts steadily out of the good region. **The factory keeps appearing to work while quietly accumulating bad batches, wear, and waste.**

That is a far better outcome than forbidding timers. The player was not stopped from being clever; the consequences of insufficient information simply emerged on their own, which is the entire mod in miniature.

### Rates matter as much as amounts

A process that needs a sustained condition is not satisfied by a large burst. 10000 Tu for one second is not 1000 Tu for ten seconds. Most thermal processes should care about the **state of the material over time**, which makes heating rate a property of the interaction between heat source, material, and vessel — not a "furnace speed" stat.

### Documentation is a free sense

The same rule that governs instruments governs the mod's own explanations of itself, and it settles what the in-game documentation is allowed to say.

> **Documentation is a free sense. It reports in adjectives.**

The format follows from the mod's subject. Almost nothing here is a static fact — a hammer that does not stop, a plate degrading into foil, a vessel overshooting its target are all **behaviours over time**, and a page of text is the wrong instrument for them. The right one is a **Ponder-style animated scene**: the player watches the plate become foil become scrap and arrives at *nobody is going to turn this off* on their own. That is the entire pitch, shown rather than asserted.

It is also the only honest format. A mod named Feedback, whose thesis is *observe, then reason*, should not explain itself with a wall of prose. The documentation should have the same epistemics as the thing it documents.

**But this format is unusually dangerous here, and the danger is precise.** A worked scene is somebody's design, rendered in three dimensions, with arrows — and §5 says an automation problem with a single correct answer is not a problem. A scene demonstrating two hammers interleaved would delete the discovery it depicts. A scene naming steel's temperature band would hand over a figure that §7 intends to be paid for in failed batches. The waste *is* the gate, and documentation is quite capable of tunnelling straight through it.

So the rule has a hard boundary:

> **Show verbs. Never values, never solutions.**

- **Yes** — a crank attaches to a shaft like this. A hammer strikes when driven. This sensor reads and does not act. The wire runs from here to there.
- **No** — steel wants 1400 Tu. Here is a working closed-loop furnace. Put two hammers side by side.

Mechanism is fair game; process is not. The test when writing any scene: **if changing one number in a balance pass would invalidate it, it is the wrong scene.** Mechanism is stable, process numbers are not, and a scene built on the stable half will not rot.

There is real onboarding work inside that boundary, and it should not be underrated. *A machine with no recipe list* is unfamiliar enough that players will assume the mod is broken. Saying **yes, really — put anything in and drive it** is a verb, it is the single most necessary thing the mod has to communicate, and no amount of watching a machine will teach it.

The corollary is what makes this load-bearing rather than decorative. **Naming a designated explanation channel is what prevents tooltip creep.** The alternative to a considered documentation policy is not silence; it is somebody patching a confusing machine at 2am by writing a tooltip that gives the answer away.

---

## 9. Machines Remember — and So Do Workpieces

Not all machine state is transient. Machines carry persistent or semi-persistent state between runs, none of it readable without the matching sensor.

**State is not confined to machines.** A workpiece that has been heated is *hot*, and stays hot as it is carried, stored, or dropped — cooling toward ambient the entire time. Material carries its own history between machines, which means the gap between two machines is part of the process rather than a free teleport.

The consequences are large for something so small. Machine adjacency starts to matter physically rather than aesthetically. A process can be lost in transit. And the player acquires a genuine reason to care where they put things, which no amount of throughput tuning would have produced.

It should also be allowed to compose messily. A glowing ingot is a glowing ingot wherever it happens to be — in a hand, in a wooden chest, next to something flammable. Consequences that follow obviously from an item being hot should be permitted to follow, rather than being suppressed because the item is "supposed" to be in a machine.

- **Thermal charge.** A cold machine runs its first process slower while it heats; a preheated one runs faster. This creates a genuine standing decision: idle the machine hot and waste energy, or let it cool and pay the startup cost. Related: a large thermal mass is slow to heat but easy to stabilize, while a small one responds fast but is hard to hold steady.
- **Contamination / residue (fouling).** Running different materials through a shared vessel leaves residue that affects later runs unless cleaned, or unless the machine is dedicated to one recipe family. This is the direct consequence of §11 — with no identity checks, there is nothing to "filter out." One cheap multipurpose machine fouls; N dedicated machines never contaminate but cost more space and capital. A real factory-layout tradeoff, not a lore excuse. It also explains reactor behavior cleanly: a dedicated reactor only ever sees one intermediate, so nothing lingers to interfere with the next batch.
- **Wear.** Accumulates with use, and shows up as **drift in the machine's sensor baseline** — a vibration sensor's "normal" reading shifts as bearings age.

That last point matters more than it looks. Wear is not a parallel durability minigame; it degrades the player's *information*. Maintenance and measurement are the same subject.

GregTech 6's boiler calcification — mineral buildup that degrades efficiency unless you feed distilled water, cleanable only once cooled and depressurized — is direct precedent that this class of mechanic works.

---

## 10. Energy Is Plural

Feedback has multiple genuinely independent energy systems. Which one drives a machine is a real decision, not flavor.

| Energy | Native sources | Transport | Typical uses |
| :--- | :--- | :--- | :--- |
| **Mechanical** | Water wheels, windmills, treadmills, steam pistons | Physical linkage only — shafts, gears, belts. Lossy to friction; needs lubrication upkeep | Mixers, grinders, presses, saws, pumps, drills |
| **Thermal** | Fire, lava, Blaze Rod (heat battery) / Blaze Powder (extreme fast-burning fuel), Nether/geothermal taps, thermal mass | Conducts through connected blocks; steam is the primary *vehicle* | Furnaces, boilers, evaporators, stills |
| **Chemical** | Combustion fuels, and reactive intermediates | Moves as physical substance, not as a field | Reactors: alloying, explosives, synthesis |
| **Electrical** | Lightning capture; conversion from the other three | Wires, lossy over distance without upgrades | Sensors, logic, precision control, electrolysis, electromagnets |
| **Spatial** | Ender Pearls, Chorus Fruit (→ Liquid Teleportant) | Does not transmit — consumed per discrete jump | Signal relay, small-scale item/fluid transport |

The first four follow generate → store → transmit-with-loss → consume. **Spatial does not**, and that asymmetry is deliberate.

### The departure from GregTech

GregTech treats EU as the one true currency, with heat, steam, and chemistry as on-ramps toward it. Its actual chain is HU → steam → KU/RU → EU → MU/LU/QU, with each machine natively accepting exactly one input type.

Feedback keeps Mechanical, Thermal, and Chemical as **genuinely independent, natively consumable currencies that never *have* to become electricity.** Electrical is one option among several, not a mandatory final form.

### Electricity: dominant transport, not universal currency

Two true statements that look contradictory:

1. Late-game factories move energy predominantly as electricity, because it is compact, controllable, and convenient over distance.
2. Electricity is not the universal currency that everything secretly converts into.

Both hold, because **transport and consumption are different things.** Long-haul energy is electrical; the final conversion happens locally, at the machine, through fittings and blocks — an adapter motor driving a shaft, an adapter coil warming a vessel. The player puts the right form of energy next to the process that needs it instead of rebuilding the factory around every conversion. What the player never has to do is route a mechanical process through electricity to make it usable.

**And an implementation rule that belongs here as a principle:** *do not build eight electricities.* If the energy types differ only in name, texture, and unit, this whole section is decoration. Each type must be behaviorally distinct in ways the player feels — how it is transported, whether it can be stored, how it degrades, and above all **how it fails and how it stops** (§8, and the per-type breakdown below).

### Each type stops differently

Sensing and stopping are separate problems, and both are physically specific to the energy involved. Character, not just cosmetics:

- **Electrical** stops nearly instantly — but the switching hardware arcs and wears out under repeated load-cutting.
- **Mechanical** coasts. Momentum keeps the machine turning after the clutch disengages.
- **Thermal** dissipates slowly. Thermal mass gives a built-in soft stop, whether you want one or not.
- **Chemical** stops the *feed*, not the reaction. Whatever is already in the vessel keeps reacting to completion.

Precise pairings of sensor and actuator per energy type belong in document 2.

### Notes on individual systems

**Mechanical** is the industrial workhorse — most material transformation is ultimately a shaft turning something. Rotary and reciprocating motion are **not** separate energy types; the earlier question of splitting Mechanical into GregTech-style KU/RU sub-currencies is **resolved as no**. Rotational direction does matter where the mechanism genuinely cares, particularly for reciprocating mechanisms.

But motion has a **shape** as well as a magnitude, and a great many machines want back-and-forth rather than round-and-round. A hammer strikes. A bellows squeezes. A saw draws. None of them want a spinning shaft, and all of them are fed by one.

The conversion is a **linkage** — a crank or cam — and it is a component the player places, not a detail hidden inside the machine (§4). That matters more than it sounds, because of what the linkage turns out to control (§17): a crank's throw sets how much force each stroke delivers, so **`St` is not a property of the machine at all. It is a property of how the player chose to drive it.**

**Thermal** heat is not internally "steam" — steam is the vehicle, the way electricity is real regardless of which metal carries it. Two physically opposite conversions exist. A turbine or engine lets heat flow downhill and skims work off the flow, which is efficient across *large* gradients and destroys the gradient. A heat pump forces heat uphill, which is efficient only across *small* gradients — useless for smelting heat, excellent for cheaply maintaining a modest preheat, and therefore tied directly to the thermal-charge mechanic in §9. It also enables real heat integration: pump waste heat out of something that must stay cool and into something that wants to stay warm.

A heat pump left unattended overruns **in both directions at once** — an over-cooled source and an over-heated sink from one neglected machine. That the core loop generalizes cleanly to a device with two outputs is a good sign for the loop.

**Cut: caging a living Blaze as a renewable thermal source.** An earlier pass here had a flame-permeable cage holding a living Blaze captive, escaping and turning hostile on mismanaged containment — a hazard-tier outcome with real teeth, and GT6's community successor has an "Infernal Boiler" on similar ground, which made the shape look field-tested. It's dead anyway: a creature held captive in a cage, escaping on failure, is Create's Blaze Burner specifically, not just "an anomalous thermal source" generally, and the rotation network already draws enough from Create that converging on this one too would undo the differentiation. What stays instead: **Blaze Rod** is a genuinely good heat battery — it holds heat unusually well, an anomalous property worth the "recognize before understanding" treatment (§2) on its own. **Blaze Powder** is an extreme-temperature, fast-burning fuel — very hot, very short burn. Neither needs a cage or a hazard mechanic; the anomaly is in the material, not in an escape-failure state.

**Chemical** splits in two. Combustion fuels are chemical energy deliberately converted to heat — they feed Thermal and need no transmission network of their own. **Reactive intermediates** stay chemical: unstable synthesized compounds that move as physical substances and have a **shelf life**, decaying to inert waste if they sit too long or travel too far before the next reactor consumes them. This mirrors real process chemistry, where many intermediates cannot be stored or shipped and must be consumed on site — and it gives Chemical a failure mode no other system has. It **expires**, where Thermal dissipates and Electrical merely resists.

**Electrical**'s wild source is lightning capture via rod-and-capacitor array — industrializing something the world already demonstrates works, since a Channeling trident redirects lightning reliably. It is bursty and storm-dependent and needs capacitor banks to smooth into something usable, which is a pleasing parallel to real electrical infrastructure being mostly buffering rather than generation. Electrical is also the default backbone for **control signals**: cheap, wired, and distance-limited like redstone, needing repeaters. **Magnetism folds into Electrical** as an application (electromagnets), with its primary gameplay home in the magnetic separator.

**Spatial** is structurally unlike the rest: consumed per discrete jump, with no generate/store/transmit staging. Mid-game, an **Ender Relay** binds a transmitter/receiver pair using an Ender Pearl consumed in the binding — a one-time capital cost, like a lodestone binding a compass — after which the pair passes **data** instantly at any distance, across dimensions. The bound link then needs a continuous trickle of **Liquid Teleportant** to stay stable, not to power jumps but to suppress premature transition events. Let it run dry and the link **destabilizes** rather than cleanly switching off: dropped and corrupted signals at the data tier, and a genuine hazard at the matter-transport tier, where whatever is mid-transit has nowhere clean to go. Late-game, the same bound pair scales up to carry a small, hard-capped trickle of items or fluid — enough to keep a remote outpost supplied or return samples to a lab, deliberately not enough to replace belts and pipes. Spatial thus gets the same two-layer cost structure as everything else (capital plus operating cost) and the same per-type cutoff logic: stop the fluid feed and the link goes dormant safely rather than dangerously.

Because data transmission reuses the energy transmission rules, the expected pattern emerges without being mandated: small local controllers wired to their own machines' sensors for fast reflexes, with Ender Relay reserved for coarse, occasional cross-site coordination.

---

## 11. No Identity Checks

**Nothing in this mod may perform an identity check.** No block, fitting, pipe, or logic node may act on "if item == iron ingot." There is no generic filter.

All sorting and quality control must exploit real physical properties:

- **Density / gravity** — heavier material sinks faster in a fluid column or centrifuge. Real jigging and gravity separation.
- **Magnetism** — an electromagnetic drum pulls ferrous material; everything else passes. (Magnetism's gameplay home, per §10.)
- **Size / screening** — a mesh passes only particles under a given size. Directly relevant because deliberate overrun-grinding produces a *size distribution*, not one clean output (§6).
- **Optical / reflectivity** — a light sensor distinguishes visually distinct materials, and is **genuinely fooled by visually similar ones**. That is an exploitable weakness to design around, not a bug to patch.

This makes logistics a substantially larger puzzle than in existing tech mods: sorting becomes a matter of finding a property difference to exploit rather than a matter of reading an item ID. It is also what makes fouling (§9) a real consequence rather than an arbitrary penalty — with no identity filter, there is nothing that could have caught the contamination.

**[OPEN]** Mapping the actual roster of vanilla and modded materials onto these four separator types has not been done. To be designed collaboratively; it lands in document 3.

---

## 12. Chemistry as a Useful Abstraction

The **real periodic table** is the default framework for material properties — conductivity, reactivity families, density — matching GregTech's approach where it improves gameplay.

Minecraft-exotic materials that do not map to real elements (redstone, Blaze material, Ender Pearl material) are handled the way the compendium handles Redstone: as **deliberately flagged exceptions** with their own defined properties, sitting *alongside* the real table. Not forced into invented chemistry, and not left as unexplained magic.

Chemistry should allow **multiple reaction paths** without becoming a chemistry simulator. Tags can express broad reagent classes — *any strong acid*, *any chloride salt* — so that the player's available materials, not a single blessed recipe, determine the route.

**Byproducts must remain meaningful.** Different paths to the same product should produce different byproducts, and those byproducts should matter. That is what keeps route choice a real decision rather than cosmetic flexibility, and it is the main thing separating an abstraction worth having from a lookup table.

---

## 13. Control Logic

### What a controller is

A controller attaches to machines on whatever face carries the data provider the player chose. It can read **everything connected to it**, perform basic operations — `+ - * /`, `> <`, `AND`, `OR` — and produce exactly one kind of output:

> **It can start or stop a supply. That is all it can do.**

Flip a breaker. Cut a clutch. Close a damper, shut a valve. Every actuator in the mod is a tap, and the controller's entire vocabulary is *open* and *closed*.

That sounds impoverished, and it is the opposite. **The controller is the only block in the game that sees the whole picture.** Every machine is otherwise a blind man feeling an elephant — a furnace that knows its own temperature and nothing else, a hammer that knows nothing at all. The controller is where separate observations become a situation, and it is therefore the heart of automation even though it cannot do anything more sophisticated than switch things off.

### The controller is a switch, never a dial

This deserves stating on its own, because it decides the character of every control problem in the mod.

There is no proportional control. There is no output that says *heat at 40%*. A controller turns a supply on and it turns a supply off, so all control in Feedback is fundamentally **bang-bang control**, which is why deadbands and hysteresis matter so much.

Thresholds are not a *setting* the controller offers — they are something the player **builds** out of comparisons. A deadband is two comparisons and a memory of what the output is currently doing, assembled deliberately. This is the same principle one level down: even the controller is not handed the wisdom.

**[OPEN]** A hysteresis loop has to know whether it is currently on, which means the controller needs its output state back as an input. Cleanest answer is that actuator state is simply another readable data source — the controller reads everything connected to it, and a breaker is connected to it. Worth confirming, because without it a deadband cannot be expressed at all and relay flapping stops being the player's fault.

The consequence is the point:

> **You cannot buy a smarter algorithm. You smooth control with physical mass.**

An oscillating temperature is fixed with a larger vessel, better insulation, a smaller bellows, a faster sensor — every one of them a physical change to the build (§4). Sophistication lives in the machine the player assembled, never in the cleverness of the program. This is the same principle as §5's "infinite ways to skin the cat," applied to control instead of process.

A sufficiently determined player will notice that switching a supply on and off *rapidly* approximates a proportional output, and they are right — that is how real controllers do it. The mod should let them, and it already charges for it: rapid switching under load is exactly what destroys breakers. The clever solution exists, works, and has a bill attached. That is the correct shape for a clever solution.

### Before any of this, the player is the controller

At the very beginning there is no control hardware at all. The player watches, decides, and acts. **They are the controller**, and every later tier is an attempt to get that job done without them standing there.

The first piece of hardware to take any of it over is not a controller either. A sensor fitting wired directly to an actuator is a **thermostat**: a bimetallic strip trips at a temperature, the bellows stops. One condition, one response, no logic block anywhere.

Only then does a controller arrive, and it arrives for a reason — the moment one condition stops being enough.

### Controllers end up marking the eras, and that is allowed

§14 insists progression is a profile rather than a rank, and one axis is going to look conspicuously like a tier ladder anyway: **new controllers will read as the definitive mark of an era change.**

That is acceptable, because nobody decided it. It is a consequence of making the thing that comes in tiers also be the nervous system of the factory. When the component that sees everything gets better, everything it sees gets better with it — of course that feels like an age turning over.

The distinction worth holding onto is that this was *earned* rather than imposed. A voltage tier announces an era because the designer said so. A controller tier announces one because the player can suddenly coordinate things they could not coordinate yesterday.

### What tiers, and what does not

**Capability does not tier.** A punched tape can express everything a late-game integrated controller can. The operations are the same, the logic is the same, and a player who can describe what they want can always write it down.

What improves is only two things.

**1. The cost of changing your mind.** Early on, rewriting means cutting a new physical program and walking it to the reader — and the earliest tier of all may mean crafting a whole new controller. Later it is swapping a disk. Later still it is editing in a GUI. The program is an item before it is a setting (§4), which means it can be copied, stockpiled, labelled and handed to another player, and it means **you cannot tweak a running machine.**

The medium follows the energy progression rather than being assigned arbitrarily:

- **Punched tape or card** — early, entirely mechanical, read by pins and levers exactly as a Jacquard loom or player piano reads one. Needs no electricity, which matters because the early game has none (§10).
- **Magnetic tape** — arrives with electricity. Rewritable and denser.
- **Integrated controller** — late. Authoring and execution collapse into one block.

**2. How much you can look at.** The early controller handles booleans from two or three sources. The late one reads **tables** from something on the order of a hundred. This is the axis that actually gates what the player can build, and it compounds with sensor tiers: a cheap sensor emits a boolean against a fixed built-in threshold, a better one emits the real continuous value and lets the controller do its own arithmetic.

Notice what this means. **The controller progression is a pure soft gate** (§7). The player was never unable to express the program. They were unable to *see enough to write a useful one*, and unable to afford to iterate on it. That is the same gate the thermometer imposes, one level up — and it is why the difficulty in Feedback lives in acquiring and routing data rather than in learning to program:

> **The logic is trivial. Getting something worth reasoning about in front of it is the entire problem.**

### How data gets to the controller

A controller reads any data-offering block **within range** — on the order of sixteen blocks, since eight does not cover even a moderately sized build — and that is the entire rule. There is no cable block, no conduit network, and nothing to route.

A cable is **drawn** between the two, client-side, when the client can see they are linked. Making a connection may cost a cable item. But nothing in the world is a wire: no entity, no block, no path.

**Crucially, the link is not checked for a clear path.** It passes through walls, floors and machinery, and that is a deliberate abstraction rather than an oversight. Routing a cable around an obstacle is not an interesting decision — it is a chore wearing a decision's clothes. The range limit stays because it *is* an interesting decision: it makes controller placement matter, and it produces control rooms as a natural building pattern rather than a prescribed one.

Range and capacity are orthogonal constraints. Range decides **where** a controller can see; its tier decides **how many** of those things it can hold in mind at once.

This also leaves the Ender Relay's job intact (§10). Sixteen blocks is a workshop. The Relay is any distance, across dimensions, and costs a continuous trickle of Liquid Teleportant to stay stable — a different problem entirely, not a better version of the same one.

### The third axis is qualitative: what the controller runs on

Early controllers take **rotation**. Late ones take **electricity, natively.**

This is not the ordinary conversion story where a late machine takes power because an adapter handles it (§10). It is literal: an early controller is an analogue machine that physically turns a tape past a reader, so it wants a shaft. A late controller is a computer, and computers run on electricity.

Two consequences worth having on purpose.

**Early control shares a failure domain with the machines it controls.** The same water wheel drives the hammer and the tape reader, so a stalled mechanical network takes down the process *and* the thing supervising it simultaneously. Later, an electrically-powered controller stays awake while the mechanical network is down and can still act on what it sees. That is a real improvement in reliability rather than convenience, and it arrives without anyone designing a "reliability upgrade."

**Rotation speed is polling speed.** A tape being physically turned is read at a rate proportional to `RPM`, which means the controller's **Response** property (§8) is a direct function of how fast the player is spinning it. A control loop genuinely gets sluggish when the river runs low. Speed it up and the loop tightens — until the breaker wear from over-frequent switching starts to bite (§13, below).

The visual style of the editor takes after *Steve's Factory Manager* — a flowchart of wired nodes rather than a text language. (Its successor, *Super Factory Manager*, added optional scripting later while keeping the visual mode.)

### Bad logic has physical consequences

A controller built without a deadband, reading a value that hovers near its threshold, will flap a relay on and off rapidly — and breakers already wear from repeated cutting under load (§10). **Sloppy control logic does not merely work badly; it actively destroys downstream hardware.**

This ties control quality directly into the mechanical stakes the energy system already established, rather than inventing a separate "code quality" mechanic to punish the player with.

---

## 14. Progression Is a History of Understanding

Progression is not a voltage ladder and not a sequence of technological ages. It is the order in which the player comes to understand what this world actually does.

A progression step is valuable when the player **understands something new about the world, gains a new way to observe or control it, or learns to apply an old phenomenon at a fundamentally greater level of control.**

### The order of understanding

These are **capability bands, not tiers.** They overlap, they are not gates, and the player should never experience them as locks. "Tier" remains useful shorthand for a broad era; it should never appear in the player's face.

1. **Materials have physical behavior.** Fire, water, stone, wood, metal, weight, motion, hardness, melting — consistent and observable. The science here is purely phenomenological: *this thing does that.* Minecraft's rules are already present; nothing is postponed to a magic endgame.
2. **Processes are continuous.** Heating, grinding, mixing, reacting, pressing and pumping do not stop when the desired result first appears. This is where the core loop becomes the player's model of the world: matter has state, and continued processing moves that state onward.
3. **Processes can be scaled.** Once a process runs continuously, throughput becomes the problem. Bigger vessels, larger work surfaces, stronger drives, better bearings, pumps, boilers, shafts.
4. **The world contains multiple useful energy phenomena.** Water, wind, fire, steam, fuels, Blaze heat, lightning, redstone behavior. The player is not yet required to unify them into one currency, and several never need unifying at all.
5. **Energy can be measured and managed.** The player stops watching machines and starts instrumenting them. Temperature, torque, vibration, pressure, flow, electrical and chemical state become observable through covers. The machine did not get smarter — **the player gained a sense.**
6. **Materials have hidden, exploitable properties.** Better instruments reveal differences invisible to the eye, useful for separation, control, conversion, and process design. Minecraft-exotic materials start showing behavior that is plainly impossible by real-world expectations — and it gets measured and characterized rather than labeled magic.
7. **Mixtures can be separated and purified.** Density, magnetism, size, phase behavior, solubility, optics. Screening, gravity separation, centrifugation, filtration, distillation, crystallization, extraction become the backbone of material logistics.
8. **Matter can be deliberately transformed.** Beyond combining and heating: synthesis, decomposition, redox, precipitation, neutralization, polymerization, alloying. Chemistry becomes a discipline only *after* enough measurement and process control exist to make it meaningful.
9. **Processes have measurable rates and states.** Pressure, temperature, concentration, residence time, mixing, atmosphere, catalysts, electrical potential — deliberately held in useful ranges. Multi-stage process chains become natural rather than arbitrary.
10. **Extreme conditions reveal new phenomena.** With ordinary variables under control, the player can deliberately explore high temperature and pressure, strong fields, unusual atmospheres, near-vacuum, intense stress. New processes arise from what those conditions expose.
11. **Minecraft-exotic phenomena become subjects of systematic science.** Blaze material, redstone, Ender and Chorus phenomena, Nether materials, lightning. The player does not "enter the magic era" — they discover their existing framework was incomplete.
12. **Natural anomalies can be engineered.** From using naturally occurring impossible materials, to characterizing the mechanism, isolating the property, reproducing it under controlled conditions, and finally manufacturing superior analogues. The natural Blaze Rod was never the end of thermal technology.

### Progression is a profile, not a number

Two different things hide under the word "tier": the player's **scientific capability** and their factory's **industrial capability**. They do not advance together. A crude machine with an excellent sensor outperforms a sophisticated machine operated blind, and enormous throughput is perfectly compatible with knowing almost nothing about what is happening inside it.

So progression is better modelled as several axes that occasionally converge into recognizable eras:

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

A player's position is therefore a **technology profile**, not a rank:

> Mechanical capability: high · Thermal control: moderate · Instrumentation: low · Chemical processing: moderate · Separation: high · Exotic characterization: none

Which is a far more interesting thing to be than **Voltage: EV**. Deliberate lopsidedness is legitimate and should be playable — a player deep into separation with almost no instrumentation is a real build, not a mistake.

This yields the single most useful question to ask of any proposed piece of technology:

> **Which axis does this advance?**

A bigger boiler advances scale. A thermocouple advances observation. A deadband controller advances control. A centrifuge advances separation. A pressure vessel advances process conditions. A Blaze Rod heat battery advances energy exploitation *and* anomalous characterization. None of them need to be "tier 4 machines." They are pieces of an ecosystem, and the interesting moments are where two or three axes cross and something new becomes possible that neither would have allowed alone.

### Difficulty stays flat. Novelty goes up.

Processes do get more demanding as the mod goes on. Tolerances narrow, conditions couple, materials stop forgiving mistakes. But the player's tools improve alongside them — so for anyone who has kept up with the instrumentation they were given, **nothing gets harder. Things get new.**

This is the intended shape of the whole game, and it is worth stating as a target rather than leaving it to emerge:

> **Difficulty is a ratio — required precision over achievable precision — and the design goal is to hold that ratio roughly constant while the numerator and denominator both grow.**

Three consequences worth designing to.

**New capability must not make old work harder.** A player who advances should find their earlier processes becoming trivial, not merely differently annoying. This is the *Return* in the four-part test below, and it is why §4 insists a better saw cuts copper better rather than simply cutting new things.

**Falling behind converts novelty back into difficulty, and that is the pacing mechanism.** A player who skipped the thermometer meets tightening bands as genuine pain, which is exactly the pressure that sends them to build one. Nothing needs to enforce a build order; the material cost of ignorance (§7) does it, and it is self-correcting rather than punitive.

**What actually changes over the game is the *kind* of hard.** The difficulty dimensions in §7 are the real progression axis. An early process is hard because it is *narrow* relative to crude tools. A late one is hard because it is *coupled*, or *unstable*, or *path-dependent* — kinds of difficulty that a better thermometer does not address at all, and that require a different sort of thinking rather than a tighter number. That is the difference between a game that escalates and a game that develops.

### The test for a major progression event

A significant step should ideally satisfy all four:

- **Sense** — it introduces a genuinely new measurement or way to observe.
- **Make** — it enables something previously impractical or impossible, not merely faster.
- **Need** — it introduces a problem that specifically rewards the new capability.
- **Return** — by the end of the era, earlier investments visibly pay off, and the outline of the next unsolved problem becomes visible.

### Detect before you can act

A particularly strong pattern: give the player the ability to **detect** a phenomenon well before they can exploit it. A radiation sensor can teach a player that certain materials emit something strange long before any nuclear machinery exists.

This is a general pattern, not a one-off: electricity may be observable (static, conductivity, a twitching needle) long before it is an industrial power network; Redstone may show strange electrical behavior before it is deliberately exploited; Amethyst may show resonance before it is a precision component; a Blaze Rod may be an unusual heat source before it is understood; an Ender Pearl's spatial anomaly may be unmistakable long before spatial engineering exists; Sculk may show signal/experience-related behavior long before an engineered information device does; Uranium may be discoverable long before it can be measured, let alone reactored. Observation, measurement, and control are three different unlocks, and a material is allowed to sit at any one of them for a long time.

This makes the world feel like a continuous field of discoveries rather than a sequence of unlock screens, and it gives the player questions to carry forward.

### Old technology keeps its niche

Progression should not read as a straight ladder of replacements. A newer energy system or machine earns a place *alongside* the old one, not instead of it — mechanical power stays useful once electricity exists (§10), and the Crude Blast Furnace never becomes obsolete once the crucible exists precisely because it was never competing on the crucible's axis (§15). The same is meant to hold for systems not yet built: steam should keep a niche once combustion engines exist, mechanical/punch-card control should keep a niche once electronic control exists, and belts/pipes should keep a niche once spatial transport exists (§10's Liquid Teleportant link is deliberately capped below belt throughput for this reason). The test for a new system is whether it earns a *different* niche — cost, reliability, scale, attention — never whether it is strictly better.

### Materials have histories

A material is not identical to its ore/block form forever. It moves through a processing history — something like `ore → crushed ore → concentrated ore → chemically treated material → refined metal → alloy → finished stock → precision component` — and purity, processing history, geometry, and thermal/mechanical state may all matter where doing so creates a decision (§3's anti-simulator test still applies: track a step only if skipping it or doing it badly is a real, playable mistake).

### Geology should matter

**[OPEN]** Ore generation should eventually move toward large, irregular, geologically coherent deposits — associated minerals, low-grade peripheral material, richer cores, depth-dependent composition — rather than isolated decorative ore blocks. This is inspired by GregTech-style large-vein generation, but the mineral roster, deposit geometry, and generation algorithm are all undecided; see §18.

### Progression should be visible in the factory

Energy systems produce a visible industrial arc without recoloured machine tiers.

- **Early** — essentially no electricity. Factories are mechanically and thermally legible: shafts, gears, belts, water wheels, windmills, pistons, flames, boilers, hot vessels, hands-on material handling. A first workshop should look like a machine shop.
- **Middle** — electricity appears because certain jobs genuinely need it: sensing, precision control, electrochemistry, electromagnets. It is still generated from thermal and mechanical systems. Steam and cogs do not disappear; they share the floor with wiring, instrumentation and relays.
- **Late** — electricity dominates energy *transport*, with local conversion at the point of use (§10). Mechanical and thermal processes continue everywhere; what changed is how energy arrives.

The intended arc is **mechanical/thermal → hybrid → predominantly electrical transport with local conversion**, while the underlying physical processes stay recognizably continuous with what came before.

### Visual progression is a design goal, not a side effect

Most tech mods pick one aesthetic — steampunk, industrial, sci-fi — and keep the same machine *forms* throughout, changing only materials and textures. Feedback should make advancement visible through **changing infrastructure, process conditions, and energy transport.**

The progression is not *wooden machine → copper machine → steel machine → titanium machine*, one block doing one job forever. It is:

> **primitive process → scaled process → instrumented process → controlled process → exotic process**

with genuinely different machine architectures becoming appropriate along the way. Materials still govern construction and limits, but they must not be the only thing communicating progress. **A late-game factory should not be an early-game factory with faster machines and different textures.**

### Machines grow out of process families

The machine roster should emerge from physical process families rather than recipe categories: size reduction (grinding, crushing, milling, cutting); mixing and homogenization (blending, suspension, emulsification, gas incorporation, foaming); separation (screening, gravity, magnetic, centrifugation, filtration, sedimentation, flotation, distillation, crystallization); phase change (melting, freezing, evaporation, condensation, sublimation, deposition); chemical reaction (combination, decomposition, displacement, redox, precipitation, neutralization, polymerization); extraction and purification; electrochemistry (electrolysis, electroplating, electrolytic refining, electrodeposition); thermal treatment (annealing, hardening, tempering, calcination, sintering, roasting, carbonization, thermal decomposition); and Minecraft-native transformations involving Blaze, redstone, Ender, Chorus, lightning and Nether phenomena, all expressed through measurable internal rules.

The first machine families should feel like **physical inventions that make these processes continuous and reliable** — not like a list of one-block recipes.

### Empowerment is a parallel axis

Player empowerment should not track industrial progression one-to-one. Industrial capability, instrumentation, automation, and personal capability are partially independent axes.

The player should gradually become more capable in the world, ending with genuinely high-end personal equipment — but the endpoint should read as **technology being industrialized onto the player**, not an abrupt leap from ordinary armor to an inexplicably immortal sci-fi Halo Doom Slayer 9000 Mecha-body. **[OPEN]** The empowerment curve gets designed after the industrial framework is settled.

---

## 15. Vanilla Is Not Exempt

If heat is a real continuous quantity and materials accumulate temperature, then the vanilla furnace cannot remain an instant raw-item-to-finished-item black box sitting in the same world. Vanilla mechanics get reinterpreted through the same physical rules wherever a visible contradiction would otherwise exist.

**The decision: the three vanilla smelting operations are preserved, but the vanilla blocks themselves are superseded.** First attempt reworked them in place via Mixin, patching `AbstractFurnaceBlockEntity` and its two subclasses; that fought vanilla internals one crash at a time and never reached a player. Feedback now ships its own `feedback:furnace`/`smoker`/`blast_furnace` — a single `ThermalVesselBlockEntity` parameterized by `VesselKind` — with the same recipe and role vanilla's versions had, but genuine thermal behavior. They become the crudest real apparatus in the game, sitting at the bottom of the same ladder as everything the player builds later.

The original vanilla blocks stay registered (existing worlds, mod compatibility) but are uncraftable — the recipe is gone, not the block. They still generate in structures for now; replacing those occurrences too is future work, not yet built.

### Three vessels, three personalities

Each gets a distinct physical character rather than a speed multiplier, and each is described by the properties in §9 and §8:

- **Furnace** — the generalist. Open, crude, mediocre at everything. Wide usable band, modest peak, little thermal mass, no stability worth the name.
- **Smoker** — **low ceiling.** Low thermal mass, heats fast and cheaply, twitchy and hard to hold steady.
- **Crude Blast Furnace** *(renamed)* — **high floor.** High peak temperature, high thermal mass, slow to come up, and **genuinely stable** for a primitive device. Not great. Good enough to be tempting.

**Their vanilla specializations must be emergent, not enforced.** The recipe-type whitelists come off. Nothing declares the smoker a food machine or the blast furnace an ore machine; three numbers do it.

The mechanism is that each device has a **restricted operating band**, and §8's *Range* property has a bottom end as well as a top:

- The smoker's **ceiling** is below metalworking temperature. Ore placed in one simply never arrives — the material is unchanged, which is the correct failure from §6 rather than a refusal.
- The blast furnace's **floor** is above the food window. A sealed refractory box burning coke either runs hot or is not running; you cannot hold one at cooking temperature, which is equally true of the real thing. Food put in one is destroyed on the way past.
- The furnace spans both bands and is good at neither.

Note that the floor is doing necessary work, and that a ceiling alone would not have been enough. High thermal mass makes the blast furnace *slow*, and a slow vessel is **more** forgiving of a low, wide window, not less — it would drift up through cooking temperature and linger there comfortably. Without a floor, the blast furnace is an excellent smoker. Restricting the band at both ends is what makes the specialization real.

**Built as one wall, not two.** The smoker's ceiling is a genuine clamp (`FTuning.SMOKER_CEILING_TU`) — nothing about leak or mass produces it on its own, since the fire dominates the equilibrium calculation regardless of leak at the values this mod uses. The blast furnace's floor is *not* mirrored as a second clamp; it is high mass alone, and the arithmetic was checked rather than assumed: at typical fuel temperatures the vessel's own climb rate is highest near ambient and falls as it nears equilibrium, so the time it can spend below the food ceiling is short relative to a food recipe's cooking time, and food is destroyed on the way past exactly as this section says. A true bistable "roaring or out" floor was considered and set aside — see `core/FTuning.java`'s `BLAST_FURNACE_MASS` for why, and the anti-simulator test in §3 for the standard it failed.

The result is that both blocks end up better at exactly what they were always better at, **and no rule anywhere says so.** This is the emergence the mod is built on, demonstrated in the first hour on blocks the player already knows. It also means modded recipes lose their device restrictions — an accepted and deliberate trade, in a mod already close to total-overhaul scope: conditions decide, types do not (§5).

### Vanilla vessels are appliances. Feedback's are components.

All three vanilla blocks share one property, and everything else about them follows from it: **they are self-contained.**

The fuel goes *inside*. This is not an implementation convenience — all three visibly have a compartment for coal beneath the receptacle, and always have. They are finished objects: fire, vessel, and container in one box, with nothing to attach and nothing to separate.

Feedback's own equipment is the opposite. A crucible is not a furnace; it is a vessel that sits over a **separate** firebox, takes a **separate** instrument, and is driven by a **separate** actuator. It arrives in pieces because the player is meant to arrange the pieces.

That distinction is the whole progression in physical form:

> **The player begins by operating appliances and ends by assembling components.**

It also explains the Crude Blast Furnace's refusal of covers as one property rather than two arbitrary rules. It is sealed. Fuel goes in it because it is sealed; nothing attaches to it because it is sealed; it is stable because it is sealed. **The same property that makes it good makes it opaque.**

This is a hard ceiling on **observability** rather than on capability, which is the inverse of every tier gate in the genre — and it is what gives the purpose-built crucible something real to be better at. The crucible wins not by holding more heat but by having been **designed to be measured**: a vessel with a thermowell, which is a physical upgrade in the §4 sense rather than a stat.

### Attention is a currency, and the blast furnace is where you spend it

The Crude Blast Furnace is therefore **not a predecessor to the crucible.** It sits alongside it, and it is available at the same time or later. It never becomes obsolete, because what it offers was never capability in the first place.

The two vessels are orthogonal:

| | Crucible | Crude Blast Furnace |
| :--- | :--- | :--- |
| Speed | slow | fast |
| Batch | large | small |
| Instrumentable | yes — that is its purpose | never |
| Costs the player | capital and infrastructure | **standing there** |

Which produces a genuine standing choice rather than an upgrade: *a minute of AFK, or thirty seconds of your full attention?* Both answers are correct, and which one is correct changes with what else the player is doing at the time.

This matters well beyond one block. §7 promises that manual production stays viable for a surprising share of the game, and until now that promise rested on the player's stubbornness. The blast furnace makes it **structural** — the manual route gets dedicated equipment that is genuinely, permanently competitive, and the Amish% player is using purpose-built tools rather than refusing to use the good ones.

It also names something the mod has been trading in without acknowledging: **player attention is a real resource**, and the mod should be willing to sell speed for it. Every manual process in §7 is already an attention purchase. This is the first piece of equipment designed around that fact.

### The fallback

Feedback cannot hand-specify a process for every smelting recipe in every mod a player has installed, and unspecified recipes must not break.

**This section originally specified a Work-based mechanism, and it was wrong — recorded here because the error is the reusable part.** The first version costed progress as `X` Work per tick, `X` being how far above ambient the vessel sat: hold a vessel `X` Tu above ambient and it costs `X` Work per tick, an item needs some total quantity of Work, and progress accrues at the vessel's own temperature. That reads as physically motivated, and it is exactly the trap `tpu_spec_doc.md` names: `Tu × ticks` is not Work, because temperature is a state and never an amount of anything (§17). It also meant three vanilla recipe types collapsed into "whichever is shortest," permanently — an iron ore's blast-furnace time won in every vessel, including a plain Furnace that can never actually reach blasting conditions, which is a hidden recipe rule wearing §15's own "specialisation is emergent" claim as a disguise.

**The mechanism is now TPu**, per `tpu_spec_doc.md` and `feedback_mechanics.md` §3: a process-local progress quantity, not energy. A recipe's declared cooking time becomes a baseline requirement in the same ticks-equivalent units the process would need at perfect conditions; the vanilla recipe type (`smelting`/`blasting`/`smoking`) becomes a thermal-suitability hint rather than a fixed multiplier, evaluated against the vessel's *current* temperature every tick rather than settled once. An ore therefore finishes faster in a genuinely hot vessel and slower in a mediocre one, without either block ever checking what kind of vessel it is — the same emergent specialisation this section already promised for the three vessels themselves, now applied to the fallback that feeds them.

That is still not physically honest, and it is not meant to be. **The fallback is a compatibility shim, not a model of the world.** Its one job is to guarantee that no item in any installed mod becomes uncraftable — not to make that item's process interesting, balanced, or true. Interesting belongs to hand-authored processes, which impose real windows and real rate limits (steel will not tolerate being blasted). The shim exists so that the nine hundred recipes nobody will ever hand-author keep working.

Two properties make the looseness acceptable. **The fallback still imposes no ceiling on how fast an item may absorb progress** — a vessel held exactly at a recipe's optimum finishes in exactly its baseline tick count, however that heat was delivered — so nothing is free; speed still has to be paid for in heating capacity and aim the player must actually own. And **the fallback is data-driven by construction**, which is the real goal: writing compatibility for a new mod should be a table, not an essay.

Hand-authored processes override the fallback. Everything else keeps working untouched. This is what makes "the world and the machines obey the same rules" affordable rather than an infinite content obligation.

---

## 16. Relationship to Create

Create's genuinely powerful idea is not kinetic machinery. It is:

> **A machine is a component. The player is the system designer.**

Feedback takes that one step further. In Create, the *automation* is compositional. In Feedback, **the manufacturing process itself is compositional** — the mod supplies a heat source, a furnace, a thermometer, a pump, a valve, a controller, a timer, a press, and never supplies the refinery that uses them. Create says *here are the components, build a machine.* Feedback says *here are the physical constraints, build whatever you think satisfies them.*

Create is therefore a **philosophical predecessor**: the compositional sandbox in §5 is the thing Feedback wants to push further toward process engineering, and Create's vocabulary — `Su` for mechanical stress, `RPM` for rotational speed — is worth reusing so that players read Feedback's numbers without a glossary.

Mechanically, Feedback implements its **own** rotational system rather than depending on Create. Create being open source makes it a reference to study rather than a library to import.

**[OPEN]** Whether Feedback additionally ships **compatibility** with Create's rotational network — shafts that mesh, stress models that reconcile — is undecided. The two live options are *entirely separate* or *separate with some compatibility*. Nothing in the design so far depends on the answer, so it can be deferred until Feedback's own mechanical system exists.

---

## 17. Vocabulary

Units follow one rule, in three clauses:

> **Use a fictional unit for a game-important abstract quantity. Use the familiar unit where the Minecraft ecosystem already has a standard. Do not model a quantity at all unless modelling it creates an interesting decision.**

The third clause is the one that does the most work, and it is §3 applied to vocabulary.

| Quantity | Unit | Notes |
| :--- | :--- | :--- |
| Temperature | `Tu` | A **state**, not an amount of heat. See below. |
| Pressure | `Pu` | First real use is the Pressure Vessel (`TODO.md` §5e): an amount of stored air over a fixed volume, the same shape `ThermalMass` already gives `Tu` — see `core/unit/Pu.java`. |
| Cumulative mechanical work | `Fu` | Total mechanical application a process demands. |
| Mechanical application strength | `St` | How strong each individual application is. What that strength *accomplishes* is not a machine stat — see below. |
| Mechanical stress / load | `Su` | Create's meaning, deliberately. **Never** speed. |
| Rotational speed | `RPM` | Don't invent a fictional speed unit. |
| Fluid volume | `mB` | Millibuckets. |
| Fluid flow | `mB/t` | |
| Mass | `Mu` | Needed for density, and therefore for gravity separation (§11). Density is `Mu/mB`. |
| Amount of substance | `Qu` | A stand-in for the mole that carries **no implication about atoms or particles**. Concentration is `Qu/mB`. |
| Electrical | `Eu` | Voltage *and* current. The one non-scalar unit. See below. |
| Untyped energy | `Work` | Spelled out, no abbreviation. See below. |
| Time | `t` / seconds | Ticks internally; JEI should show both — `600 t (30 s)`. |
| Resistance | *(none)* | **Deliberately not modelled.** Add only if gameplay gives a reason. |

Rates derive rather than being invented: `Tu/t`, `Eu/t`, `mB/t`. There is no reason to coin a unit for every derivative.

### Temperature is a state, not an amount of heat

`Tu` measures how hot a thing *is*. It does not measure how much heat was transferred to make it that way. A furnace sitting at some temperature is in a state; heating it happens at some `Tu/t`.

This separation is deliberate and it is what makes §8's "rates matter as much as amounts" mean anything. If temperature and quantity-of-heat were the same number, a process that needs a *sustained* condition would be indistinguishable from one that needs a large burst — and the entire thermal-mass, preheat, and insulation layer in §9 would collapse into a single efficiency stat.

### Fu and St: force is the machine's, work is the material's

`Fu` is total work and `St` is the force of one application, and the tempting mistake — made once already and corrected — is to treat them as two independent numbers a machine publishes.

They are not independent, because **nothing is strong enough to dent steel yet fails to slam copper.** A machine states only the force it can deliver. What that force *accomplishes* is a property of the material being hit:

> **Fu per application = St ÷ hardness**

**Hardness is one number doing both jobs.** It is the floor below which nothing happens at all — §7's hard gate, a genuine impossibility rather than a slow version of the process — and it is the divisor for how much of a larger blow actually lands. Copper's hardness is low, so a heavy blow dumps a great deal of work into it; steel's is high, so the same blow does nothing whatsoever.

> 30 Fu total, hardness 1 — anything can finish this eventually, given enough taps.
>
> 60 Fu total, hardness 15 — no number of weak taps will ever substitute.

The second form is the hard gate. The first has a subtler consequence that turns out to matter more.

**A hard blow on a soft material overshoots.** If force over hardness is large, a single application can carry the workpiece past the state you wanted and into the next one — and surplus work carries through rather than stopping politely at the finish line. The strongest available setup is therefore not the best one, because §6's overrun arrives sooner and there is less time to intervene.

That is the whole mod in one line of arithmetic, and it arrived by itself: it was not designed, it fell out of refusing to let force and work be two separate numbers.

**The machine defines a pair; the linkage selects from it.** A crank converts rotation into reciprocation (§10). Its **throw** is how far the crankpin sits off the axis of rotation, and it sets two things at once: the stroke is twice the throw, and the force available at the ram is the torque divided by the throw. A short throw therefore concentrates torque into greater force over a shorter stroke, and a long throw spreads it into a gentler, longer one. One stroke per revolution either way — throw does not change how often the hammer falls, only how hard.

So a machine's `St` is written as two numbers rather than one:

```
MECHANICAL HAMMER     12 / 3 St     (short throw / long throw)
```

Both outcomes are on the card, and which one the player gets depends on the crank they installed. `RPM` still sets how often strokes happen; `Su` still says whether the network can drive the thing at all.

A machine therefore has no single strength. Satisfying a *hardness 15* requirement means **re-gearing the drive**, not buying a stronger machine — and the same hammer is a delicate planisher or a forging press depending only on how it is driven. That is not a metaphor: light blows for finishing and heavy blows for drawing out is how smithing actually works.

**Overshoot can skip a state entirely, and that is the best version of the lesson.** Copper is soft enough that a short throw carries it past *plate* and into *foil* in a single blow — the intermediate is not merely quick to pass, it is unreachable at that setting. The two throws therefore do not make the same product at different speeds. They make **different products**, selected by how the machine is geared and not by any recipe chooser. A short-throw hammer is a foil machine.

There is a law hiding in that, and it is worth stating because it constrains every future process:

> **The reaction window is the strength ratio.** For a hard setting to skip a state, that state must cost less than one hard blow — which caps the gentle setting's slack at the ratio between the two, measured in blows.

A `12 / 3 St` machine can never give more than four blows of grace on a state its strong setting skips. Widening the gap widens the window. So "how forgiving is this material" and "how different are the two cranks" are not two design questions; they are one number seen from either end.

**Neither throw may dominate the other.** This was got wrong once, and the failure is worth recording because it is easy to repeat. When throw changed `St` alone, the short throw was strictly stronger at identical speed, so nobody would ever have fitted the long one — and a dominated option is worse than no option, because it costs the player a decision and gives nothing back. The fix was not to invent a compensating bonus but to notice that work per blow was never a machine stat in the first place. With work derived from hardness, the short throw is **power** and the long throw is **precision**: the strong setting reaches the goal in fewer blows and blows past it in fewer too.

**Gearing changes `St` too, and a machine has a ceiling.** Gearing down trades speed for force — that is the oldest trade in machinery and the mod should not pretend otherwise — so a gear train multiplies the force each blow lands and divides how often blows fall. Work per second is unchanged; what changes is whether any single blow is hard enough to matter at all, which for a material with a hardness floor is the difference between working and doing nothing.

But force cannot be geared without limit, because **the machine itself has to survive transmitting it**:

> **Every machine states the most `St` it can deliver, whatever is driving it.** A paper blade at a million RPM still will not cut steel.

That ceiling is a property of construction, not of the drive, and it is what keeps a better machine worth buying once gearing exists. Gearing gets you to a machine's ceiling; only a better machine raises it. Without the cap, one long gear train would make every hammer in the game equivalent and reduce the entire force axis to how many cogs you were willing to place.

It also gives the force problem a **second answer**, which §5 asks for. Re-gear the drive, or fit a shorter throw: both buy force, and they cost differently. A shorter throw is free but spends precision — the same hard blow that reaches the threshold also overshoots past the state you wanted. A gear train keeps the gentler blow's control and spends capital and space instead. Neither dominates, and a player who understands only one of them can still get there.

> **[OPEN]** Whether gearing down needs an *ongoing* cost or whether capital plus the ceiling is enough. Gearing preserves work per second, so on paper a gear train buys force for free once built. That may be correct — a real gearbox does exactly that, and the cogs, the space and the machine's own ceiling are genuine costs. But if gearing turns out to be strictly better than every other route to force, it needs one.
>
> Building narrowed this without closing it. Friction and mass are referred through the **square of the gear ratio**, which is the standard mechanical result and was not installed for balance — so a geared-*up* branch charges the network continuously for spinning faster, and a geared-*down* one genuinely costs less to turn. What remains open is only the half that matters: a machine's working draw is a flat figure stamped on the block, so gearing down to reach a hardness floor is, today, free apart from the cogs. Referring the working draw too would be more honest and would make a gear train a way of **fitting more machines on one wheel**, which is a balance decision and not only a physics one.

Note that throw is deliberately **not** a scalar (§3). Two named options make this a decision; a numeric throw would make it an optimization problem with a right answer.

The same component reads differently on a machine whose product is *displacement* rather than work. A bellows moves air in proportion to how far its plates travel, so there the long throw is the useful one and the figures run the other way. One lever, two figures of merit, and no rule needed for either — which is §5's composition test passing on a single part.

This is close to the ideal shape for a mod mechanic, because none of it had to be invented. It is the trade real machinery already makes, it is expressed entirely in quantities the mod had already defined, and it converts a number printed on a block into a decision the player makes.

### Su, mB, and borrowing on purpose

`Su` follows Create's Stress Units for mechanical load, and `RPM` follows Create for rotational speed. Familiarity and ecosystem compatibility matter more here than owning the vocabulary, so the convention is kept rather than replaced. This is also why `Su` must **never** drift into meaning speed.

`mB` is the same judgement. A fictional fluid-volume unit was considered and rejected: Minecraft modders already read millibuckets fluently, and fluid systems already supply connectivity and direction, so a new abstraction would have bought nothing.

There is also a small, genuinely useful accident here. A bucket is 1000 mB and fills one block — one cubic metre, which is 1000 litres. **So 1 mB is exactly 1 litre.** Minecraft's fluid unit is already SI, which means `Mu` can be anchored the same way: define 1 Mu as 1 kg and water has a density of exactly `1 Mu/mB`. Every other material's density then reads as its real-world value, free, with no conversion table and no arbitrary scale to invent. Worth taking.

### Eu is electrical, and it is not a scalar

`Eu` carries a **voltage and a current**. Two `Eu` values with the same product are not interchangeable, which makes it the only quantity in the mod that isn't a single number.

That is a deliberate exception to the scalar rule in §3, and it is not vector physics — it is a two-component quantity of exactly the kind real electrical engineering runs on, and it is a large part of what gives Electrical a character of its own alongside its distinctive stopping behavior (§10).

The name was, for a while, the biggest open question in the whole vocabulary. Read as "Energy Units," `Eu` makes Feedback sound like IC2 or GregTech, where electricity quietly becomes the universal abstraction that every other system exists to feed. That is precisely the outcome §10 forbids.

The resolution is to make `Eu` **specifically electrical** rather than generically energetic. It is not the mod's energy unit. It is electricity's unit, sitting alongside `Fu`, `Tu`, and the rest as one system's currency among several.

### Work is deliberately unabbreviated

Generic, untyped energy is called **`Work`** — spelled out, no abbreviation, no symbol. It is what a process asks for when it needs *some* energy and genuinely does not care which system supplies it.

Keeping it unabbreviated is the point. A short symbol that looks like all the others invites players to read it as another currency to convert into, and it is not one.

### What is deliberately absent

**Resistance is not modelled.** It is the cleanest illustration of clause three: it is real, it is relevant to every electrical system ever built, and it earns its place in Feedback only if it produces a decision the player finds interesting. Until it does, it does not exist. Apply the same test to anything proposed for this table.

**[OPEN]** Exact ranges, unit arithmetic, and how voltage and current actually behave across a wire belong in document 2.

---

## 18. Open Questions

Carried forward, and not to be treated as settled:

- Mapping the four separation mechanisms onto the actual vanilla and modded material roster (§11).
- The exact machine roster and the physical upgrade path for each process family (§14).
- How far vanilla reinterpretation extends beyond smelting — the three vessels are settled (§15), the rest of vanilla's processing is not.
- Whether Feedback ships Create compatibility, and of what depth (§16).
- The mechanism and progression of local conversion covers — electrical → mechanical, electrical → thermal, and any others (§10).
- The visual architecture of each factory era (§14).
- Where each Minecraft-exotic phenomenon first appears, what is measurable about it, and how its natural form becomes an engineered one (§2, §14).
- The full era arc beyond the opening: how many broad eras exist, where the "detect before you can act" bridges land, and which discoveries pay off later (§14).
- The personal empowerment curve (§14).
- Ore deposit geometry and generation algorithm, and the prospecting progression that reveals it (§14). Candidate content lives in `feedback_progression_roadmap.md`.

## 18a. Parked

Deliberately not being designed, recorded so it is not lost or accidentally started:

- **Food as a real thermal process.** Cooking meat to different degrees yielding different hunger/saturation profiles. It fits the model almost too well — §6's severity ladder is already the difference between rare, done, and charcoal, and food is the lowest-stakes teaching material in the game. It is also unmistakable scope creep against a slice that is about metal, and at worst it is an excellent addon rather than a loss. **Not now.**

---

## 19. Explicit Non-Conclusions

Distinct from the open questions above: these are things that may *look* decided from reading the design, and are not.

- Exact tier or era names.
- The first machines, and the first materials.
- **McGuffnium is not a material.** It appeared in earlier discussion purely as a metaphor.
- Exact numerical values, anywhere.
- The chemistry implementation.
- The separator-to-material mapping.
- Energy generation recipes.
- Progression boundaries.
- The control block's implementation.

The design is strong enough to **constrain** these decisions. It is not strong enough to have **made** them.

---

## 20. Where This Goes Next

The identity is settled enough that the next useful work is concrete content — but derived from the progression structure above, not invented as an item list.

The early game has a clear shape as a progression of *capability*, each step a sentence the player could say:

> **Manual action** — "I can do this myself."
> **Primitive apparatus** — "I can make something do it repeatedly."
> **Continuous operation** — "It won't stop unless I stop it."
> **Timer automation** — "I can make it stop approximately when I expect."
> **Measurement** — "I can actually observe the process."
> **Control** — "I can make another component respond to what I observe."
> **Precision** — "I can build a system that holds a specific condition."

Which gives the first stage a single coherent identity: **the player moves from performing processes to building apparatus that performs processes.**

One plausible shape for that slice, illustrative only: begin with a forgiving process such as copper — broad temperature tolerance, low requirements, heating rate barely relevant — so vanilla-grade equipment suffices. Introduce mechanical material working, done by hand at first. A primitive machine automates the repetition, and does not recognize completion. The player controls it manually, then with a timer. Then a more demanding thermal process arrives needing a narrow, sustained temperature range: technically attemptable with primitive gear, but unreliable and wasteful. The player acquires measurement, can finally see the process, and eventually acquires control components and closes the loop.

By the end of that slice the player should understand, without having been told any of it:

machines perform physical operations; machines do not know when they are finished; continuous operation alters finished products; manual production is possible but slow; timers are crude automation; measurement is separate from control; sensors control nothing and actuators know nothing; precision belongs to the whole system; process conditions matter; rates matter as much as quantities; machine state persists; physical upgrades change behavior; better machines improve old work as well as enabling new; several physical solutions can satisfy one process; and strange Minecraft phenomena are useful before they are understood.

They should leave it thinking less:

> "What machine do I need for this recipe?"

and more:

> "What physical process is this, what does it require, and how do I build something that makes it happen reliably?"

### The method for filling in content

For each capability in the early game, answer in order:

1. What actual problem does the player encounter?
2. What physical process solves it?
3. What does that process require?
4. What can the player already do manually?
5. What primitive apparatus automates it?
6. What makes that apparatus imperfect?
7. What measurement capability eventually improves it?
8. What control capability eventually improves it?
9. Which existing processes get better as a consequence?
10. What becomes economical without being artificially unlocked?

### Start from the problem, and keep the slice small

Work backward from a problem, never forward from a machine:

> **problem → required process → required capability → machine → material → recipe → progression consequence**

So the opening question for the whole content effort is not *what is the first machine?* or *what is tier 1?* or *what is the first ore?* It is:

> **What is the first interesting problem Feedback asks the player to solve?**

And the slice has to be small enough to actually build. Not four hundred materials and a complete chemistry engine — something closer to **ten to twenty items, five to ten machines, one energy system, one or two process variables.** Make that work, then play it.

This matters more here than in most mods. Feedback's entire philosophy rests on emergent behavior, and emergent behavior cannot be validated on paper. A prototype is the only way to find out whether manually controlling a temperature is genuinely fun or unbearable after five minutes, or whether a process specification that reads beautifully is incomprehensible in play.

The first content pass fills in **one complete playable vertical slice** — not the whole mod.
