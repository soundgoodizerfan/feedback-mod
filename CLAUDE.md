# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repository is

This is the repository for **Feedback**, a Minecraft technology mod. It is a design-first project: the mod was specified before a line of it was written, and the specification documents live here alongside the source.

The design documents remain the spine of the project; the source is now being written against them.

**`feedback_philosophy.md` and `feedback_mechanics.md` are required reading before any implementation edit** — not background, not optional context. A `PreToolUse` hook (`.claude/hooks/require-design-docs-read.sh`, wired in `.claude/settings.json`) enforces this mechanically: it blocks `Edit`/`Write`/`MultiEdit` under `src/main/` until this session has `Read` both files. If an edit gets blocked, read them, then retry — don't route around it.

**Platform: NeoForge on Minecraft 1.21.1**, built with **ModDevGradle**. 1.21.1 was chosen over a newer version deliberately — the newer backend's data-driven item work suits a mod built on "properties and tags matter more than the id," but 1.21.1 is where the players are.

**No git credentials in this environment.** `git push` will fail (`could not read Username for 'https://github.com'`). Don't worry about the remote — commit locally and stop there. Never attempt to push.

## Build and run

```bash
./gradlew build          # jar -> build/libs/feedback-<version>.jar
./gradlew runClient      # dev client
./gradlew runData        # datagen -> src/generated/resources
./gradlew runGameTestServer
./build-and-deploy.sh    # build, then deploy to the Feedback PrismLauncher instance
```

`build-and-deploy.sh` clears its own stale `feedback-*.jar` from the instance `mods/` folder before copying, because two jars with one mod id is a launch crash. It touches nothing else in that folder and has no packwiz step — Feedback is a standalone mod, not a modpack coremod.

Java 21 is the machine default and the correct toolchain; no `JAVA_HOME` override, unlike the sibling ForgeGradle repos.

Versions live in `gradle.properties` — NeoForge `21.1.250`, ModDevGradle `2.0.147`, Parchment `2024.11.17`, Gradle `9.2.1` via the wrapper.

## Source layout

Root package `io.github.soundgoodizerfan.feedback`. The package tree follows the mod's **separation of systems**, not Minecraft's registry categories — measurement, control and actuation are separate systems the player wires together, and the code says so:

| Package | Holds |
| --- | --- |
| `registry/` | every `DeferredRegister` — one place to look |
| `item/` | hand-carried items — instruments (`ThermometerItem`, `CalipersItem`, `TemperatureSensorItem`), `HandHammerItem` and its `HandToolItem`/`HandToolCrafting` base, `MoldItem`, `DataConnectorItem`, `DebugHelmetItem` |
| `core/unit/` | **the units of §17 as types, and the machinery for the next one.** Ten live: `Tu`, `TuRate`, `ThermalMass`, `Conductance`, `Su`, `Rpm`, `Fu` (an int), `St`, `Drag` (`Su/RPM`), `Inertia` (`Su·t/RPM`). Thin records, one component *named for the dimension* — that naming is the second guard, after the parameter type. **No arithmetic on them**: they type the plumbing and the physics unwraps inline to floats, so `Heat.tick` still reads like the equation. `Unit` is the common interface, `Units` holds codec/wire/format helpers — **the checklist for adding a unit is in `Units`**. Datapack records (`Deformation`, `ThermalProcess`, `Quench`, `Fuel`) are typed too now, via `Units.codec`/`Units.streamCodec` so the on-disk JSON stays a bare number; `Work`, condition and ticks have no type, for stated reasons |
| `core/rotation/` | the mechanical network — `RotationNode`, `RotationNetwork`, `RotationPropagator` |
| `core/thermal/` | the thermal model — `ThermalBody` (has a temperature and a mass), `HeatSource` (has a flame temperature and no mass), `Heat` (the maths), `ItemHeat` (a workpiece's own heat) |
| `machine/` | physical operations. A machine class never names a recipe |
| `process/` | operation definitions, completion, and overrun behaviour |
| `instrument/` | carried sensors. Read-only by construction — an instrument has no way to act |
| `control/` | controllers, timers, data links. `control/controller/`, `control/program/` and `control/programmer/` are the Tier 1 program-graph controller from `feedback_controller_spec.md`: a `ProgramGraph` of sensor/comparator/actuator nodes, authored on a Punch Card by the Programmer block. `control/bimetallic/` and `control/timer/` are simpler standalone controllers (bimetallic strip, timer); `Switchable` is the on/off interface all controllers and actuators implement. `control/data/` (`DataNode`, `DataNodeRef`, `DataLinkManager`) is the data-link system's tech-demo pass — read from MrCrayfish's Furniture Mod: Refurbished's electricity system (MIT), **its `1.21.1` branch specifically, not the repo's default `26.1.2`** — check out the branch matching the target Minecraft version before reading, always; see `TODO.md` §4d-1. `control/debug/` is its debug-only display sink, same standing as `DebugHelmetItem`. The node/link overlay (`client/DataNodeRenderer`, `DeferredDataRenderer`, `DataLinkFrame`) is also from that read: a `BlockEntityRenderer` queuing draws that get flushed once a frame into vanilla's entity-outline render target, visible only while the connector is held |
| `actuator/` | things that start and stop a supply |
| `fitting/` | **the addon system, and it is not called "cover."** GTCEu's cover is one sided-I/O-filter concept; Feedback needs three shapes that don't share one: `Sensor` (sided, read-only, a `DataNode` — an `Instrument` found attached instead of carried), `Upgrade` (unsided, changes the holder's own numbers), `Adapter` (sided like a Sensor, addon-shaped like an Upgrade — grants a second energy type to run on). `CrucibleBlockEntity` is the one `Fittable` holder so far; `fitting/sensor/TemperatureSensorFitting` is the one concrete fitting. `Upgrade` and `Adapter` still have no concrete implementer — see `TODO.md` §4d |
| `compat/` | soft integration with other mods — `compat/jei/` (recipe-viewer pages; see `THIRD-PARTY-LICENSES.md`), `compat/jade/` (tooltip provider) |
| `net/` | `CustomPacketPayload` records for client/server sync (`ThermalProcessSyncPayload`, `DeformationSyncPayload`) |
| `data/` | datagen |
| `client/` | renderers and screens |

Create a package when there is something to put in it; don't scaffold empty ones.

**Datagen is not written yet.** `src/generated/resources` is a resource root and is committed, but blockstates, models, loot tables, lang, crafting recipes and recipe-unlock advancements are all hand-authored under `src/main/resources` for now. Convert when the roster is big enough that hand-editing starts going wrong — not before. The recipe pass (19 shaped recipes, 3 advancements) was the closest call so far and hand-editing still won.

**All art is placeholder.** Every texture currently referenced is a vanilla one. No Create asset may ever be used here: Create's *code* is MIT, its *assets* are All Rights Reserved. `THIRD-PARTY-LICENSES.md` records what we owe and to whom.

### The thermal model, in one paragraph

Heat flows on a **difference**, never at a flat rate: `conductance × (fire − vessel)` in, `leak × (vessel − ambient)` out, divided by thermal mass. This matters because the flat-rate version — which is what gets written first — makes a vessel settle at `supply ÷ leak`, so insulating a crucible raises its final temperature without limit and insulation becomes a trap rather than an upgrade. Driving on the difference means a vessel approaches its fire's temperature and can never pass it, so the only way past a flame is a hotter flame, which is exactly what the bellows sells. It also pays for the slice's lava for free: lava is a fire fixed at 1200 Tu, so a crucible over lava sits just under 1200 Tu forever — the most stable heat source in the game and permanently too cool for steel. Nobody wrote that rule.

Typing the model turned up something the code had not been saying: **a vessel's leak and a fire's conductance are the same dimension** (`Work/t/Tu`), and `Heat.equilibrium` had been adding them together correctly all along. They are one type now, `Conductance`, in two roles — a leak *is* a conductance, and calling it a leak is a statement about whether you wanted it.

A **workpiece** is different again. It stores the temperature it was last stamped at plus the tick that happened on, and its current figure is computed on demand — so it cools in a chest, in a hopper, in an unloaded chunk and in a mod we have never heard of, because nothing has to remember to cool it. Item cooling is deliberately **linear** rather than exponential: an exponential never arrives, needs an arbitrary floor to stop it, and buries the whole working window in a flat tail. A constant rate makes *"you have fourteen seconds to reach the anvil"* literally true, which is what the player is actually budgeting against.

### The rotation network, in one paragraph

A `RotationNode` is a block entity that knows its own speed and which neighbour drives it. It cannot tell on its own whether it is overloaded — that is a whole-network question, answered by `RotationNetwork` (the Su ledger) and pushed down. When anything changes, `RotationPropagator` **rebuilds the entire connected run**: flood fill, find the strongest source, walk outward assigning speeds. Create propagates incrementally instead, and needs a "flicker score" to break blocks caught in propagation loops; a rebuild visits each node once and cannot loop, so we don't. Overstress reads as zero speed everywhere, but propagation deliberately uses *theoretical* speed — otherwise an overloaded network would tear itself down and rebuild the instant the load came off.

## The documents

The design is intended to live in **three documents on a spectrum from idea to implementation**:

| File | Role |
| --- | --- |
| `feedback_philosophy.md` | **Document 1 — authoritative.** Identity, principles, constraints. Worked examples only where they prove a principle is real. Open questions are marked `**[OPEN]**` inline and collected in §18; §19 lists things that look decided and are not. |
| `feedback_mechanics.md` | **Document 2 — mechanics. Started, not comprehensive.** How each principle is implemented: unit arithmetic, the sensor/actuator matrix, the thermal model, energy networks, overrun band tuning, the control node set. First section written speculatively, ahead of code: XP as an energy phenomenon (quantity vs. level, storage substrate, why it isn't a fifth power currency, sensing, applications). Everything else `TODO.md` §3 owes is still unwritten. |
| *(document 3 — content)* | **Does not exist yet.** The roster: every item, material, machine, fitting and process, and what each does. |
| `feedback_slice_01.md` | **Working document, not one of the three.** The first playable vertical slice, built by `feedback_philosophy.md` §20's method — two beats (mechanical repetition, then thermal control), copper then steel. Concrete but explicitly placeholder-numbered. Its rules feed document 2; its objects feed document 3. Where it and the philosophy disagree, the philosophy wins. |
| `feedback_controller_spec.md` | **Working document, not one of the three.** The controller/program-graph design slice 1 deliberately left dangling: the dataflow-graph program model, port/type shapes, the four-tier medium progression, and the `Switchable`/`DataNode` widening it needs. **Tier 1 (Punch Card) is built** — `control/controller/`, `control/program/`, `control/programmer/`. Tiers 2-4 of the medium progression are still just the document. Replaces `controller_spec.txt` (deleted). |
| `tpu_spec_doc.md` | **Working document, not one of the three.** Replaces `holdTime` with `TPu` (Thermal Process Units) as the thermal-process completion quantity — recipe duration becomes an outcome of thermal conditions rather than a recipe constant. Internal only, never player-facing; not energy, not Work. |
| `feedback_machine_shop_spec.md` | **Working document, not one of the three.** Scopes `feedback_progression_roadmap.md`'s "Slice 3 — Machine Shop" into something buildable: a new material-*removal* primitive (Drill Press, Lathe, Grinding Wheel — gated by tool hardness exceeding workpiece hardness, same shape as the existing blow/hardness gate) alongside deformation-family extensions (Wire Drawer, Rolling Mill, Mechanical Press) that need no new engine. Draft, not built; deliberately not named `feedback_slice_03.md` to avoid the numbering collision the roadmap's own §0.1 already warns about. |
| `feedback_progression_roadmap.md` | **Working document, not one of the three.** Post-TPu candidate progression: 18 speculative content slices (materials/machines/processes per era), the energy/power progression, geology/ore-generation direction, and cross-domain milestones. Feeds document 2 (energy progression, geology) and document 3 (the slice rosters) once approved piece by piece — **nothing in it is Established**, see its §0.1. Replaces `feedback_post_tpu_spec.md` (deleted; its philosophy-level content merged into `feedback_philosophy.md` §14). |
| `LICENSING.md` | **Feedback is GPL-3.0-or-later (code) + All Rights Reserved (assets).** Full copyleft on purpose, not LGPL: anything built on this code is free too, because a mod arguing that systems should be legible and open to being taken apart would be incoherent if its licence let people build closed things on it. Accepted cost: no closed-source addons. Also records who owns it and the AI-authorship caveat. |
| `THIRD-PARTY-LICENSES.md` | **What we owe and to whom.** All three reference mods are now legally adaptable; the working rule stays stricter than the law — read the design, write the code — and every debt is recorded per site. Their *assets* are off limits universally and permanently. |
| `speculative_physics_inspo_doc.md` | *A Speculative Physics and Biochemistry Compendium of Minecraft (Vanilla and Modded)* — the user's own worldbuilding document, ~2800 lines. Explicitly **not canon**. A **mindset reference** (how to reason about Minecraft phenomena scientifically) and a parts bin — the Liquid Teleportant chain and the "flagged exception" treatment of Redstone were already lifted from it. |
| *(`feedback_notes_i.md`, `feedback_notes_ii.md`)* | **Deleted in `4020934`**, recoverable from `404a0c3`. Write-ups of earlier design conversations with a different AI, fully merged into `feedback_philosophy.md` with their conflicts resolved. Don't restore them; don't cite them as current design. |

## The core pitch — state it precisely

Machines do not recognize when a recipe is complete. They keep performing their physical action for as long as they have input and power. Continued operation past completion **acts on the already-finished output**, changing, degrading, spoiling, or endangering it.

The last clause is the whole differentiator. GregTech 6 already has machines that idle and burn fuel forever; "always-on machines" is not the novel part. *The output is perishable to continued processing* is. Preserve this framing when describing the mod.

## Before building a feature, ask about prior art

**Ask the user whether there is a relevant open-source mod to study before implementing
anything non-trivial.** This is now an established pattern rather than a suggestion — the
rotation network came out of reading Create, the thermal model was materially improved by
reading TerraFirmaCraft *after* it was written, and the fitting system's infrastructure came
out of reading GregTech CEu Modern's cover system. Each time, the reading changed the design,
and each time it would have been cheaper before the code than after.

The user knows this ecosystem far better than any model does, and knows which mod solved a
given problem well. Asking costs one question. Not asking costs a rewrite — TFC's reading
reversed a cooling model that was already built and already worked.

What "study" means here is strict, and it is the second half of the rule:

1. **Ask first.** Name the feature; ask if anything out there already solves it.
2. **Check the licence before reading, not after.** See `LICENSING.md` and
   `THIRD-PARTY-LICENSES.md`. Feedback is **GPL-3.0 (code) / All Rights Reserved (assets)**,
   chosen so that Create (MIT), GregTech CEu Modern (LGPL-3.0, conveyable under GPL) and
   TerraFirmaCraft (EUPL-1.2, via its Article 5 compatibility appendix) are all adaptable. Copyleft licences
   are *not* mutually compatible in general, so a new source needs checking rather than
   assuming. **Assets are always no, from everyone.**
3. **Clone outside the repo**, next to `../Create-reference`, `../TFC-reference`,
   `../GregTech-Modern-7.5.3`. Reference material, never a dependency.
4. **Prefer taking the design and writing the code**, even now that adapting is legal. A
   borrowed implementation is a borrowed set of assumptions, and this mod departs from all
   three of these on purpose. Where a decision came from somebody else's mod, say so *at the
   site*, in the comment, along with what was taken and what was deliberately not. Where code
   is genuinely adapted, also record it in `THIRD-PARTY-LICENSES.md` with its licence.
5. **Record what was rejected too.** TFC's per-material heat capacity and calendar catch-up
   were both looked at and declined for stated reasons. That is worth as much as what was
   adopted, because the next person will otherwise re-open it.

The corollary, and the reason this is worth the fixed cost: **infrastructure should be built
slightly before it is necessary.** `Instrument` was written before any instrument existed and
the thermometer then cost one class and touched no display code. The `fitting/` package was
built the same way — high fixed cost paid once beats no fixed cost and a medium variable cost
paid per feature, and the crossover arrives earlier than it feels like it will. Its first
concrete fitting (`TemperatureSensorFitting`) proved it the same way; see `TODO.md` §4d/§4d-1
for what's next.

## Hard design rules

These are settled and constrain any proposal:

- **No identity checks.** Nothing in the mod may route or sort on item identity ("if item == iron ingot"). All sorting exploits physical properties — density, magnetism, particle size, optical/reflectivity — each with real, exploitable weaknesses.
- **Electricity is not the universal currency.** Mechanical, Thermal, and Chemical are natively consumable and never *have* to convert to Electrical. This is the explicit point of departure from GregTech, where everything funnels to EU.
- **Machines define physical operations, not recipes.** A machine says "I apply this operation to whatever is here," never "I know how to make Copper Plate."
- **Discrete options, not continuous knobs** (§3). Where a setting would be a free scalar, it is a small set of named choices instead — crank throw is short or long, never 1–64. A scalar has a correct answer in it; that leaves only grinding for it or knowingly playing suboptimally. Depth comes from stacking choices, never from tuning one finely.
- **A problem with one answer is not a problem** (§5). Any automation problem should have at least two correct approaches that cost different things — capital against attention, throughput against reliability, capability against walking away.
- **Workpieces carry state too** (§9). A heated item is hot wherever it is, cooling toward ambient in hand, in a chest, in transit. Machine adjacency is physical, not cosmetic, and consequences of an item being hot are allowed to follow anywhere.
- **Upgrades are physical components** you could point at — larger vessel, thicker insulation, flywheel, finer screen, better seal. "+50% throughput" is not a design concept; the question is what physical change causes it.
- **Measurement, control, and actuation are separate systems** the player wires together. A sensor controls nothing; an actuator knows nothing; the controller has no built-in target. Precision is emergent from the whole loop, never a machine stat.
- **Precision is never a hard gate.** Hard gates are genuine physical impossibilities (insufficient temperature, strength, work, pressure, material limits). Anything precision-limited stays *possible* — just unreliable and uneconomical. The shorthand: *the recipe is not locked, the process is difficult.*
- **Manual production stays theoretically possible** for a surprising share of the game, at tiny rates. Automation makes processes practical; it does not unlock them.
- **Force that cannot go into the work goes into the machine.** A blow on metal too cold to move, one under the material's hardness floor, or one geared past what the machine's construction can take, is not discarded — the machine absorbs it and wears. Wear is caused *only* by misuse, never by uptime, so a correctly built line never accrues any: a worn machine is evidence of a specific mistake, not a maintenance bill. (GregTech's flat chance per runtime hour was looked at and rejected for carrying no such information — see `THIRD-PARTY-LICENSES.md`.) It degrades and never breaks, because a machine refusing to run is a hard gate where no physical impossibility exists.
- **Sensing and stopping are separate problems, with distinct hardware per energy type** — and each cutoff has its own failure character (breakers arc and wear, clutches coast, thermal mass dissipates slowly, closing a reagent valve doesn't stop the reaction already underway).
- **Don't build a physics simulator.** The test for any variable: *does modeling this create a meaningful engineering choice?* If not, abstract it away. Real engineering having the variable is not a reason.

## Conventions

- **Units:** `Tu` temperature — a *state*, not an amount of heat — `Pu` pressure, `Fu` cumulative mechanical work, `St` per-application strength (the two are **not independent** — work delivered per blow is `St ÷ material hardness`, so what a blow accomplishes is a property of the material, never a machine stat), `Su` stress/load (Create's meaning — never speed), `RPM` rotational speed, `mB`/`mB/t` fluids, `Mu` mass (density is `Mu/mB`), `Qu` amount of substance (concentration is `Qu/mB`; a mole stand-in with no atomic implications), `Eu` electrical — voltage *and* current, the one non-scalar unit. Generic untyped energy is `Work`, spelled out and deliberately unabbreviated so it doesn't read as another currency; `Eu` is electricity's unit, not the mod's. Resistance is deliberately not modelled. Derive rates rather than invent them — `Tu/t`, `Eu/t`, `mB/t`. Show time as both: `600 t (30 s)`. Handy: 1 mB is exactly 1 litre (1000 mB fills a 1 m³ block), so `Mu` anchors to the kilogram and water is exactly `1 Mu/mB`.
- **Units are types, and the rule has three parts.** Every live quantity in §17 is a record in `core/unit/`. (1) *Type the plumbing, not the arithmetic* — interfaces, `FTuning` constants and cross-system seams are typed; `Heat.tick` and `RotationNetwork.tick` unwrap and work in floats, because those files' javadoc is the record of physics that was wrong once and it has to stay checkable. (2) *Unwrap inline, never into a local* — two bare floats side by side are back within swapping distance. (3) *Hold a primitive field, wrap at the accessor* — a wrapper in a field outlives its method and is the one case the JIT cannot eliminate. Adding a unit is one file plus a lang key; the checklist is in `Units`.

- **Wear is a percentage, not a unit.** Machine condition is dimensionless — 100% as built, falling to a floor — and it is reported as *condition* rather than wear so the figure multiplies the spec sheet directly. Don't mint a unit for it; §8's apparatus properties are expressed in units that already exist.
- **`[OPEN]`** marks an unresolved question inside the notes. Adding one is a legitimate outcome; silently resolving one is not.
- **Hard gate vs. soft gate** (§7) is the load-bearing distinction: a hard gate is physical impossibility, a soft gate is a *reproducibility* gate — you can still succeed by luck. Precision only ever gates softly. If a proposal needs "requires tier N," it's wrong.
- **Instrumentation is a yield technology, not a key** (§7). It unlocks nothing; it's bought because it improves the conversion ratio. Corollary that makes this work: failed batches must consume their inputs — the waste *is* the gate.
- **The world resolves on truth, not on readings** (§8). Success is computed from real conditions; instruments never enter the calculation — a bad sensor costs you reproducibility, never success.
- **There is an irreducible noise floor** (§8). No apparatus reaches zero variance, deliberately: if a factory could become fully deterministic, the optimal endgame is timers and the whole sensing layer becomes a discardable scaffold. Better equipment narrows the distribution, never collapses it. Model a variable only where the player can act on it; the rest is honest noise, not a simulated stand-in for noise.
- **Difficulty stays flat; novelty goes up** (§14). Tolerances tighten, but tools improve in step — difficulty is the *ratio* of required to achievable precision, held roughly constant. What changes is the *kind* of hard (narrow → coupled → unstable → path-dependent), not the amount. A proposal that makes old work harder is wrong.
- **Specialization should be emergent, never enforced** (§15). The vanilla Smoker and Crude Blast Furnace keep their traditional niches purely through thermal behavior — the recipe-type whitelists come off. If a proposal needs a whitelist to produce the right outcome, the physics isn't doing its job.
- **The controller is a switch, never a dial** (§13). Its only output is starting or stopping a supply — no proportional control exists. Oscillation is fixed with physical mass, not a better algorithm. Capability never tiers; only iteration cost and how many sources it can read.
- **"Which axis does this advance?"** (§14) is the question to ask of any proposed technology — progression is a profile across ten axes, not a rank.
- **`feedback_philosophy.md` §19 lists explicit non-conclusions** — tier names, first machines, first materials, numbers, chemistry implementation, separator mapping, progression boundaries, control block implementation. Treat these as deliberately open. ("McGuffnium" appears in the old notes as a metaphor only; it is not a material.)
- **Every design document here is a living document, and revising them is expected work.** That covers `feedback_philosophy.md`, `feedback_slice_01.md`, and documents 2 and 3 when they exist. Building finds errors reasoning does not — §17's force/work model was wrong until a hammer existed to be wrong about. **When implementation contradicts a document, fix the document in the same change**, rather than leaving it stale or filing a note about the discrepancy. Git holds the old version if anyone ever wants it.
- Record *why* the old version was tempting. A section that says *this was got wrong once, and here is the trap* is worth more than one that was always right — the error is usually the reusable part.
- **The exception is identity.** §1 and §2 say what the mod *is*; changing those means making a different mod, so raise it rather than edit it. Everything else is fair game with a stated reason.
- These are the user's own documents and a live design conversation. Argue for changes as you make them.

## Outstanding work

1. **Write document 2 (mechanics).** Started (`feedback_mechanics.md`) — XP section only, speculative and ahead of code. `feedback_philosophy.md` defers to it by name throughout for the rest — unit arithmetic, per-energy sensor/actuator pairings, the thermal model, overrun band tuning, the vanilla vessel thermal bands.
2. **Write document 3 (content).** Not started, and downstream of 2.
3. **Project scaffold is done** — Gradle, run configs, Parchment, deploy script, mod entrypoint. **The data-driven recipe format is now settled by precedent rather than by decree:** four datapack tables (`deformation`, `thermal_process`, `quench`, `fuel`), each a plain `SimpleJsonResourceReloadListener` over a record with a `Codec` and a `StreamCodec`, none of them a vanilla `RecipeType`. §15's "compat authored as a table" is satisfied — a pack that wants coke to burn hotter writes a line of JSON.

   **The one vanilla `RecipeType` is `feedback:hand_deformation`**, and it is the exception that states the rule. The ban exists so no *machine* owns a recipe list; a crafting table is vanilla's recipe machine and the player is the one swinging, so hand work is allowed to be a recipe. It is an adapter over `DeformationTable` and not a fifth table — every figure still comes from the datapack, and deleting the class would remove a route to a plate without removing a fact about copper.
4. **Slice 1 is built, both beats, and verified in game** — not just build-passes-and-boots. Beat 1 (mechanical repetition) and beat 2 (heat, one steel ingot made start to finish) are both confirmed by eye. Slice 2 (tempering, damper) and the controller/program-graph system are the current front; the latter now has its own spec (`feedback_controller_spec.md`) with Tier 1 built. **Status like this goes stale fast — `TODO.md` is the live record.** When something here needs updating, update `TODO.md`, not this list; this item exists to point there, not to re-narrate current state.
5. **`TODO.md` is the working checklist** — source of truth for *what's next*, where `feedback_philosophy.md` stays source of truth for *why*. Keep it current as items close.
