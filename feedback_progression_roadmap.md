# Feedback — Progression & Content Roadmap

> **Status: DRAFT / DISCUSSION SPEC — working document, not one of the three canonical documents.**
>
> Replaces `feedback_post_tpu_spec.md` (deleted; recoverable from git history). Its philosophy-level
> content (the world is one science, discovery precedes utility, old tech keeps its niche, materials
> have histories, geology should matter) was merged into `feedback_philosophy.md` §14. Everything
> here is the rest: candidate roster/progression content that feeds a future document 3, plus the
> energy-progression and geology detail that feeds document 2.
>
> **Nothing in this document is Established** in the sense of `feedback_philosophy.md` §19. Treat
> every slice, material, machine, and number below as a **Proposal** at best — see §0.1 for the
> category definitions. Do not implement any of it without asking the user first.

## 0. How to use this document

### 0.1 Categories

- **Established** — explicitly confirmed by the user. Nothing in this file starts here.
- **Strong direction** — consistent with confirmed design (`feedback_philosophy.md`), still adjustable.
- **Proposal** — creative content suggested by this document. Most of what follows is this.
- **Unknown** — not yet decided; see §7.

Only implement Established decisions and explicitly approved proposals. When a proposal is approved,
move the relevant content out of this document and into document 2 or document 3 (once it exists),
and mark it Established there — don't leave it dangling here.

**Build status lines.** Each slice in §2 opens with a `Build status` line from a code audit
(2026-10-12: `src/main/java`, `data/feedback/`, `src/generated/resources`, checked against
`TODO.md`). **Built** = registered and reachable by its process; **partial** = exists but diverges
from the roster entry or has no survival route to it; **not built** = nothing in code. Anything
built is de facto approved and is documented where `TODO.md` / `feedback_mechanics.md` /
`feedback_machine_shop_spec.md` say — the roster line here is only the list, not the record.
"Verified" follows `TODO.md`'s in-game verification, not build-passes. The status goes stale fast;
re-audit before trusting it.

**Naming note:** the "Slice N" numbering below (18 candidate content eras) is unrelated to
`feedback_slice_01.md`'s "slice" — that document is a single **built, playable vertical increment**
(copper then steel, confirmed in-game per `TODO.md`), while a "slice" here is a **candidate content
package on the timeline**, most of it unbuilt. The word collision is inherited from this document's
own framing (`CLAUDE.md`'s table already calls these "18 speculative content slices") and isn't worth
a rename, but don't read "Slice 1 — Primitive Industry" below as a synonym for `feedback_slice_01.md`
— the built slice's roster (copper, steel, crucible, thermometer, bellows) is a proper subset of this
document's Slice 1, not the same object.

**Slices are soft-ordered, not hard-gated.** Confirmed by the user: most slices aren't hard
prerequisites of the ones after them. As a rule of thumb, skipping roughly one tier ahead is fine and
expected — you can get into brass and bronze before steel — but skip further and a real (soft) gate
starts to bite: you definitely need steel by the time you're making copper wire. Slice numbering below
is a rough sequence, not a dependency chain; don't read "Slice 9 comes after Slice 8" as "Slice 9
requires Slice 8" unless a slice says so explicitly.

### 0.2 Ask before implementing

Before implementing any item, recipe, machine, progression gate, world-generation rule, or
energy-system detail described here, ask the user rather than silently picking the reasonable-looking
answer. This applies especially to: exact recipes and numbers, process temperatures and rates, machine
power requirements, energy conversion efficiencies, ore vein sizes and world-gen algorithms, material
properties, whether a Minecraft-native material has a given fictional property, progression gates,
whether a proposed machine/item should exist at all, how fictional phenomena interact with ordinary
physics, and terminology.

This is `CLAUDE.md`'s "ask about prior art first" rule applied to content instead of implementation
technique — the same reasoning applies: asking costs one question, guessing costs a rewrite.

### 0.3 Also apply, without repeating here

Everything in `CLAUDE.md`'s **Hard design rules** applies to every slice below without restatement —
no identity checks, machines define operations not recipes, discrete options not continuous knobs, a
problem should have more than one correct approach, upgrades are physical. A slice proposal that needs
`if (item == SOME_ORE && machine == BLAST_FURNACE) speed *= 2;` is wrong regardless of what section it
came from; let the physical systems produce the specialization the way §15 already does for the three
vanilla vessels.

---

## 1. Progression model

**Category: Strong direction.** Not itself new — it's §14's already-Established "capability bands,
not tiers" and "detect before you can act" claims, restated as a per-phenomenon sequence rather than
a single global list. Nothing below asks the user to accept a new principle, only to apply an existing
one uniformly.

Overlapping technological horizons, not rigid mutually exclusive tiers — this is the same claim
`feedback_philosophy.md` §14 makes about capability bands, applied per-phenomenon rather than
globally:

- A material can appear early while its useful applications appear much later.
- A phenomenon can be observed before it can be measured, measured before it can be controlled,
  controlled before it can be industrialized.
- A fictional phenomenon can accelerate an otherwise-ordinary technology, and ordinary science can
  make a fictional phenomenon exploitable.

A useful per-phenomenon sequence, complementary to §14's global order-of-understanding list:

```
unknown phenomenon
→ observation
→ measurement
→ repeatable experiment
→ controlled manipulation
→ material processing
→ reliable application
→ industrial application
```

---

## 2. Candidate progression / slice plan

**All Proposal-level.** A coding agent should not implement any of this simply because it appears
below — see §0.2.

### Slice 1 — Primitive Industry

**Why this roster is bigger than §20's 10-20 item guidance for a real slice:** it was written as
"essentially the already-implemented content." **That overstated it** — the 2026-10-12 audit below
finds most of it built (much of it only after this roster prompted `TODO.md` §5b-§5d to close the
gap), but not all: no forge, anvil, flywheel block, copper sheet or limestone, and none of the
Minecraft-native observations. Expect the other 17 slices to end up just as populated once each is
actually built.

> **Build status (2026-10-12): 31 built · 8 partial · 8 not built.**
> - **Built, verified in game:** copper, iron, coal, charcoal, clay (→ ceramic), quartz (= nether
>   quartz; vanilla has one item, the roster lists it twice), gold, blaze rod (a 1400 Tu `fuel/`
>   entry — the hottest fuel, which is also the "blaze produces extreme heat" observation); copper ingot/plate/foil, iron ingot, cast iron, steel (two routes,
>   `feedback_mechanics.md` §4.5), glass (as a cast `glass_ingot`, not glass blocks — `TODO.md` §5c),
>   ceramic; furnace (`feedback:furnace`, vanilla's replaced — `TODO.md` §4f), crucible (small/large),
>   hammer (Hand Hammer + Mechanical Hammer), shaft, clutch, bellows; smelting, casting, hammering,
>   hot working, steelmaking, glassmaking, ceramic firing. Milestone met (steel machines unlock on
>   first steel ingot).
> - **Partial:** redstone, amethyst, obsidian (a `material/` property file only — nothing reads it,
>   `feedback_mechanics.md` §5.6); firebrick (fires from `kaolinite`, which has no survival source);
>   brass, bronze (alloying works, but zinc and tin ingots have no survival source — no ore, no
>   recipe); flywheel (no block — `Inertia` on cogs and the Steam Engine stands in); casting molds
>   (one shape, `ingot_mold`).
> - **Not built:** limestone (no material, no item); copper sheet (plate and foil only); forge (the
>   Firebox + vessel pair fills the role under other names); anvil (hand work is the crafting-grid
>   Hand Hammer recipe, `feedback:hand_deformation`); the redstone, amethyst, nether-quartz and ender
>   pearl observations.

- **Core materials:** copper, iron, coal, charcoal, clay, limestone, quartz, gold, redstone, nether
  quartz, amethyst, blaze rod, obsidian.
- **Manufactured materials:** copper ingot/plate/sheet/foil, iron ingot, cast iron, steel, glass,
  ceramic, firebrick, brass, bronze.
- **Machines/tooling:** forge, furnace, crucible, anvil, hammer, basic shaft, flywheel, clutch,
  bellows, casting molds.
- **Processes:** smelting, casting, hammering, hot working, steelmaking, glassmaking, ceramic firing.
- **Minecraft-native observations (not yet useful):** redstone behaves strangely; **amethyst has
  unusual resonant behavior — first of only two touches this material gets in the roadmap now (the
  other is Slice 8's real piezoelectric payoff; see the note under Slice 8 for why the other five
  mentions were cut)**; blaze material produces extreme heat; nether quartz appears unusually pure /
  thermally useful; ender pearls are anomalous but poorly understood.
- **Milestone:** steel becomes the first major machine-construction material.

**Grounding note on "amethyst resonance":** this isn't decorative pseudoscience. Amethyst is
structurally α-quartz (SiO₂ with trace Fe³⁺ substitution for color), and α-quartz is a real
piezoelectric crystal — the same physical fact that makes quartz crystal oscillators, phonograph
cartridges, and push-button piezo igniters work. Treating amethyst as an anomalous-but-real resonant
material is "scientific, not realistic" (`feedback_philosophy.md` §2) rather than crystal-healing
flavor text, and it's the same real-physics anchor `speculative_physics_inspo_doc.md` already uses for
piezoelectric ignition and enchanting (§1.4, §5.2 there). What's still fictional and Proposal-level is
*how far* the resonance goes past real quartz's Curie-point/voltage limits — that's the anomaly worth
characterizing per §14's "detect before you can act," not the underlying mechanism.

### Slice 2 — Heat Engineering

> **Build status (2026-10-12): 16 built · 10 partial · 2 not built.** Records: `TODO.md` §5a-§5f.
> - **Built, verified in game:** cast iron, copper tubing (crafted from plate; nothing consumes it
>   yet); blaze rods as fuel; all nine machines — Improved Furnace, Kiln, Annealing Furnace, Blast
>   Furnace, Smoker, Heat Exchanger, thermometer, Pressure Vessel, Boiler; tempering, controlled
>   cooling (Damper + `max_rate`), brass/bronze alloying, ceramic firing and glazing (`dip/`).
> - **Partial:** firebrick and kaolinite (kaolinite has no survival source, and nothing consumes
>   firebrick — it is not yet a refractory *of* anything); graphite (item and a 2200–2600 Tu process
>   exist, but nothing built reaches 2200 Tu — re-homed as an ore + Slice 10 arc-furnace product,
>   `TODO.md` pick-up list); brass, bronze, lead, zinc (ingots and melts exist, but tin/zinc/lead have
>   no ore or recipe — creative-only inputs); high-temperature glass (quartz glass, melted from nether
>   quartz and cast — no tubing); nether quartz (the quartz-glass melt only, no other experiments);
>   annealing (the Annealing Furnace and the `require_cooling` + `max_rate` mechanism exist, but no
>   annealing process entry — the furnace holds tempering's band, `feedback_mechanics.md` §3.3).
> - **Not built:** a distinct refractory ceramic; glass tubing (needs `CastingTable` keyed on mold
>   shape, `TODO.md` §5d).
> - **Divergences:** the Heat Exchanger is the passive half only (`TODO.md` §5e); the Pressure Vessel
>   holds bellows *air*, not steam, and no process needs pressure yet. Blaze rod burns as an ordinary
>   fuel in code, which contradicts `feedback_mechanics.md` §2.1's "Blaze Rod does not go in a
>   firebox" (that section is speculative, not built) — unreconciled, not resolved here.

- **Materials:** firebrick, high-purity clay/kaolinite-like material, graphite, cast iron, brass,
  bronze, lead, zinc, refractory ceramic, high-temperature glass/tubing, copper tubing.
- **Minecraft-native:** blaze rods as a high-temperature experimental fuel/source; nether quartz enters
  refractory/optical/high-purity-silica experiments. (Amethyst removed — see Slice 8's note; this was a
  7th mention the earlier consistency pass missed when it counted six.)
- **Machines:** improved furnace, kiln, annealing furnace, blast furnace, smoker, heat exchanger,
  thermometer, pressure vessel, boiler.
- **Processes:** tempering, annealing, controlled cooling, brass/bronze alloying, glass tubing, ceramic
  firing/glazing.
- **Milestone:** the thermal environment itself becomes an engineering target.

### Slice 3 — Machine Shop

> **Build status (2026-10-12): 21 built · 4 partial · 7 not built — all built, none verified in
> game** (`TODO.md` §5g). Scoped and recorded by `feedback_machine_shop_spec.md`; forms and costs
> come from `material/` + `form/` (`feedback_mechanics.md` §5), and every metal gets every form.
> - **Built:** steel, cast iron, copper; gears (small/large cog, gearbox), shafts, bearings, valves,
>   pulleys (+ belt), copper rod/wire/fine wire, steel cable; Lathe, Drill Press, Mechanical Press,
>   Rolling Mill, Wire Drawer, Grinding Wheel; turning, drilling, wire drawing, rolling, grinding.
>   Milestone met on paper (fine copper wire off the Wire Drawer).
> - **Partial:** brass, bronze, lead, tin (forms generated, but no survival source of the metal —
>   see Slice 2).
> - **Not built:** springs, pistons (built and removed — a Crank Linkage + Clutch already does it,
>   `TODO.md` §5g), flywheels (no block), threaded rod (spec §6 `[OPEN]`); saw, gear cutter (spec §6,
>   deliberately unscoped); precision boring.
> - **Divergences:** the roster's "products" are shaped-crafted blocks, not shop output — only steel
>   cable consumes a machined part; turned rods, drilled plates and ground steel are dead ends (the
>   `feedback_mechanics.md` §5.4 trap). The Valve gates a bellows *air* line, not steam or fluid.

- **Materials:** steel, cast iron, brass, bronze, lead, copper, tin.
- **Products:** gears, shafts, bearings, springs, valves, pistons, flywheels, pulleys, threaded rod,
  copper rod/wire/fine wire, steel cable.
- **Machines:** lathe, drill press, mechanical press, rolling mill, wire drawer, grinding wheel, saw,
  gear cutter.
- **Processes:** turning, drilling, wire drawing, rolling, grinding, precision boring.
- **Milestone:** fine copper wire becomes possible.

### Slice 4 — Electricity Exists

> **Build status (2026-10-13): 6 built** (the machines/items list below).
> - **Built, verified in game (2026-10-14):** galvanic cell, electrostatic generator, galvanometer (carried and as a
>   fitting on the cable), electroscope, electromagnet -- plus the conductor and run-level resolver they
>   need (`core/electric/`, `machine/cable/`; `feedback_mechanics.md` §6.13).
> - **Built, verified in game (2026-10-14):** Leyden jar (`machine/capacitor/`, mechanics 6.6), which
>   keeps an electrostatic pulse and gives it back as a falling burst.
> - **Divergence:** nothing above LV is built, because the band anchors are `[OPEN]` (`TODO.md` §3). Zinc, lead, tin and glass exist only as Slice 2 left them.

Electricity becomes observable and measurable before it becomes a practical factory-wide energy
system — and it's deliberately allowed to sit there a while.

**Confirmed: this is a pre-viability novelty, and that's intentional, not a sequencing mistake.**
Electricity exists here, observably, long before it's useful for anything — mirroring real history,
where things tend to be novelties before anyone finds out what's useful about them. A galvanic cell is
NOT an industrial current source; it's a curiosity that proves electricity is real. The actual
industrial-scale electrochemistry that needs real current moved to Slice 5, now placed after Slice 6
(Steam) — see the note there for why.

- **Materials:** copper, gold, zinc, lead, tin, glass, ceramic, redstone.
- **Phenomena:** static electricity, conductivity, potential difference, electromagnetic behavior,
  redstone anomalies.
- **Machines/items:** electroscope, electrostatic generator, Leyden-jar-like capacitor, galvanometer,
  electromagnet apparatus, primitive galvanic cell. (The electroplating bath moved to Slice 5 — a
  single-item novelty demo here and an "industrial" process there was the same object twice; Slice 5
  is where electroplating actually belongs now that it needs real current.)
- **Key principle:** the player can run electrical experiments without a convenient grid yet.
- **Milestone:** a galvanic cell provides continuous electrical potential — at novelty scale, not
  industrial scale.

### Slice 6 — Steam Industry

> **Build status (2026-10-13): 7 built · 1 partial · 1 not built** (machines plus the power chain).
> - **Built, verified in game:** Boiler, Steam Engine, and the `fuel → boiler → steam → engine →
>   shaft` chain itself (`TODO.md` §5a). The Boiler takes heat from any `ThermalBody`, not only fuel.
> - **Built, verified in game (2026-10-14):** centrifugal overspeed trip (`machine/overspeedtrip/`, a latching
>   self-opening clutch; `TODO.md` §5a).
> - **Partial:** flywheel — no block, but the engine carries its own `FTuning.STEAM_ENGINE_INERTIA`.
> - **Built, verified in game (2026-10-14):** steam valve -- not a second block: the existing Valve now also
>   gates a fluid line (`TODO.md` §5a).
> - **Built, verified in game (2026-10-14):** condenser, pump, and the engine's
>   spent-steam exhaust, closing `boiler → engine → condenser → pump → boiler` (`TODO.md` §5a).
> - **Not built, by ruling:** pressure regulator -- steam has no `Pu`; relief only, later.
> - **Divergence:** the engine's output is a flat threshold (`STEAM_ENGINE_CAPACITY_SU`/`_RPM`), not
>   a curve — `TODO.md` §5a's `[OPEN]`.
> - **Added, built and game-tested 2026-10-13:** the dynamo (`machine/dynamo/`, mechanics §6.8), not on
>   this roster but the reason Slice 5 sits after this one. An engine at working speed gives LV on a
>   coarse winding, MV on a fine one.

Steam becomes the first serious industrial prime mover.

- **Materials:** steel, copper, brass, bronze, graphite, refractory ceramics.
- **Machines:** boiler, steam engine, condenser, pump, pressure regulator, centrifugal overspeed trip,
  steam valve, flywheel.
- **Power chain:** `fuel → boiler → steam → engine → shaft`.
- **Mechanical specialization:** steam is a large, robust, relatively slow power source; a mechanical
  network can drive crushers, rollers, pumps, hammers, generators simultaneously subject to load.
- **Speed behavior (confirmed):** moved to `TODO.md` §5a, since the Steam Engine is built — no
  governor, damping from its own `Inertia`, a centrifugal overspeed trip (built), speed control
  left to the player's loop.
- **Minecraft-native:** blaze material as an unusually hot thermal source, ahead of its deeper uses.
- **Milestone:** industrial-scale mechanical power becomes practical.

### Slice 5 — Electrochemistry and Industrial Refining

> **Build status (2026-10-12): nothing built.** The industrial current source it waits on now exists
> (the dynamo, 2026-10-13, Slice 6's note). Only infrastructure ahead of it: `process/Dip` +
> `machine/dip/Dipping` were built with electroplating named as the reason (`TODO.md` §5f), but
> nothing electrical exists for it to use; its one entry is ceramic glazing.

**Sequenced after Slice 6 despite the lower number** — a deliberate exception to the usual rough
ordering, not an error. Slice 4's galvanic cell is a novelty-scale source, not an industrial current
source; real electrolysis at scale needs current Slice 4 can't provide. Steam-driven generation (Slice
6) is the first point a viable industrial current source exists, so that's when electrochemistry
actually becomes an *industrial* process rather than a lab curiosity. The slice keeps its "5" label for
continuity with the rest of the roster, but reads as coming after Slice 6.

Chemistry has existed since the beginning; this slice makes it deliberate, measurable, and useful at
scale — the electrochemistry lab becomes the electrochemistry plant once there's power to run it at
scale.

- **Materials:** copper, zinc, lead, tin, sulfur, salt, limestone, charcoal, quartz, redstone.
- **Fluids:** brine, acidic/alkaline solutions, electrolytes.
- **Machines:** electrolytic cell, electroplating bath (moved here from Slice 4 — see that slice's
  note), chemical bath, distillation apparatus, reaction vessel, filter press, condenser.
- **Processes:** electrolysis, electroplating, acid leaching, precipitation, distillation, metal
  purification.
- **Outputs:** high-purity copper/zinc/lead, sulfur compounds.
- **Minecraft-native:** redstone, glowstone, blaze powder, nether quartz gain deliberately investigated
  chemical/electrical properties.
- **Milestone:** pure, controlled feedstock becomes available for advanced metallurgy and electronics.

### Slice 7 — Petrochemistry

> **Build status (2026-10-12): nothing built.** Adjacent, not this slice: the vehicle Engine block
> (`vehicle/engine/`, `feedback_vehicles_spec.md`) is a combustion-shaped rotation source, but it
> burns no fuel yet.

Petroleum as a major system, not decorative fuel.

**[OPEN] — oil worldgen vs. ore-vein geology are not reconciled.** §4's ore-family geology is solid,
zoned deposits (surface indicators → peripheral → primary → richer core). Oil reservoirs are
fundamentally different — fluid-filled sedimentary basins, not solid zoned bodies. This hasn't actually
been thought through as one geology system with two deposit shapes; confirmed genuinely open, not
quietly solved. Don't assume the ore-vein generator can also place oil without a real answer here.

- **Geology:** hydrocarbons in large sedimentary basins/reservoirs; possible associated resources
  (crude oil, natural gas, sulfur, salt, limestone, shale); surface seeps through deep reservoirs.
- **Products:** crude oil, naphtha, kerosene, diesel, heavy oil, lubricants, asphalt, solvents, polymer
  feedstocks.
- **Machines:** refinery column, fractionator, cracker, chemical reactor, condenser, gas compressor,
  storage tanks.
- **Polymers:** polyethylene, polypropylene, synthetic rubber/fibers, electrical insulation polymers.
- **Power generation:** `diesel → combustion → shaft`, `natural gas → turbine → shaft` — distinct
  engineering niches, not just bigger steam engines.
- **Milestone:** dense liquid/gaseous fuels and industrial polymers become available.

### Slice 8 — Basic Electronics

> **Build status (2026-10-12): nothing built.** Redstone, glowstone and amethyst have `material/`
> property files only (`feedback_mechanics.md` §5.6) — nothing reads them.

Target point for the first genuinely recognizable electronic circuit.

- **Prerequisites:** fine copper wire, insulation, glass, ceramic substrates, refined metals,
  batteries, electromagnets, precision machine tools, polymer insulation, basic electrical
  measurement.
- **Components:** wire, insulated wire, electrode, resistor, capacitor, relay, diode-like component,
  **point-contact semiconductor device** (a cat's-whisker-style primitive contact device — historically
  real, and it predates a doped transistor; this is NOT the doped Slice 10/15 transistor, since silicon
  purification/doping doesn't exist yet at this point in the roster — that was a real physical-dependency
  contradiction in an earlier pass, now resolved by being explicit about which device this is), circuit
  substrate, solder alloy.
- **Machine:** circuit assembly station, plus wire bonder, soldering station, component tester, signal
  generator, oscilloscope-like instrument.
- **Major item — Basic Electronic Circuit:** ceramic/polymer substrate + copper traces + passive
  components + switching component + soldered connections.
- **Minecraft-native:** redstone evolves from strange material to deliberately exploitable electrical
  material; **amethyst's real payoff — the second and last of its two confirmed appearances in this
  roster (the first is Slice 1's initial observation).** Amethyst/quartz-family piezoelectricity is real
  (grounded against real α-quartz piezoelectricity, cross-referenced in `speculative_physics_inspo_doc.md`
  §1.4/§5.2) and gives a genuine piezoelectric resonator/oscillator component here — a real payoff, not
  another vague mention. **Conditionally open to a third appearance as an instrument**, but only if a
  resonant-frequency-based *measurement* use is identified where the resonance actually tracks a real
  physical quantity — the kind of thing a real quartz crystal microbalance does (resonant frequency
  shifts under mass loading, used to measure deposited mass/thickness). "Detecting hollowness" was
  considered and explicitly rejected as not a meaningful observation. Absent a real quantity like that,
  amethyst stays at two appearances; glowstone in optical/electrical experiments.
- **Milestone:** controlled electronics becomes manufacturable.

**Why amethyst needed cutting from 7 mentions (slices 1, 2, 3, 4, 5, 10, 14 plus this one — Slice 15 also
had a stray mention, also removed) down to these two:** it had become a default "something
Minecraft-native goes here" filler reached for
repeatedly across slices without each appearance earning a distinct payoff — and the document even
flagged its own uncertainty about what amethyst actually does, at the end, after listing it eight times.
That combination (spammed, then hedged) is the tell that it was overused rather than under-specified.

### Slice 9 — Industrial Chemical Engineering

> **Build status (2026-10-12): nothing built.** (Heat exchanger and pressure vessel exist from Slice 2.)

**Ordering note:** real heavy industrial chemistry (contact process, Haber-Bosch) historically predates
electronics by decades and doesn't need it. Slice 9 doesn't hard-depend on Slice 8 — the general
one-tier-skip principle in §0.1 covers this; the numbering here is organizational, not a prerequisite
chain.

- **Materials:** sulfur, sodium compounds, chlorine, hydrogen, oxygen, nitrogen, silicon-bearing
  feedstock, carbon, phosphates, nitrates.
- **Machines:** large chemical reactor, distillation tower, absorption tower, electrolyzer, compressor,
  gas separator, heat exchanger, pressure vessel, industrial pumps.
- **Products:** sulfuric/hydrochloric/nitric acid, caustic soda, hydrogen, oxygen, chlorine, ammonia,
  fertilizers, catalysts, industrial solvents.
- **Minecraft-native:** blaze, glowstone, redstone, nether quartz as unusual materials, catalysts,
  reagents, or energy-related substances.
- **Milestone:** chemistry becomes a factory-scale material-processing discipline.

### Slice 10 — Advanced Materials

> **Build status (2026-10-12): nothing built.** Graphite's 2200+ Tu process exists but is unreachable;
> `TODO.md`'s pick-up list reassigns it here as the arc furnace's synthetic graphite.

- **Materials:** aluminum, nickel, chromium, silicon, graphite, titanium, tungsten, cobalt, manganese.
  (Rare-earth materials cut — no ore family, no application, nothing cashing them in later; dropped
  rather than left as an unanchored placeholder. Re-add only with a stated purpose, e.g. magnets,
  phosphors, catalysts.)
- **Products:** aluminum alloys, stainless steel, nichrome, high-speed steel, tungsten carbide
  (cutting-tool inserts), titanium alloys, graphite electrodes, silicon wafers.
- **Machines:** vacuum furnace, arc furnace, induction furnace, crystal-growth system,
  powder-metallurgy press, sintering furnace, vacuum pump.
- **Processes:** vacuum treatment, high-purity refining, crystal growth, advanced alloying, powder
  metallurgy, sintering, surface treatment.
- **Minecraft-native:** nether quartz as silica/silicon feedstock; redstone gains specialized
  material-science applications. (Amethyst dropped from this slice — see Slice 8's note; it's down to
  two confirmed appearances in the whole roster.)
- **Milestone:** high-purity materials and engineered material properties become practical.

### Slice 11 — Radiation Is Discovered

> **Build status (2026-10-12): nothing built.**

Uranium and related materials appear *before* nuclear power is useful.

- **Materials:** uranium ore/uraninite, thorium-bearing ore, lead, boron-bearing material,
  radiation-resistant glass/ceramics.
- **Instruments:** radiation detector, Geiger-counter-like instrument, dosimeter, scintillator/
  detection crystal, neutron detector.
- **Products:** refined uranium compounds, sealed radioactive source, radiation shielding.
- **Core principle:** the player discovers some materials are intrinsically unstable/radioactive —
  another instance of measurable-before-useful.
- **Hazard character (confirmed, distinct from other failure shapes):** radiation exposure/contamination
  *lingers* — it persists in materials or an area rather than triggering one discrete on/off event.
  Explicitly NOT modeled on a containment-failure template (a creature that escapes a cage on
  mismanagement) — that mechanic is cut from `feedback_philosophy.md` entirely (it was Create's Blaze
  Burner shape specifically), and the mod isn't converging on that shape a second time for radiation
  either.
- **Minecraft-native (speculative):** whether Ender-related materials respond unusually to
  high-energy/radioactive conditions. Not locked.
- **Milestone:** radiation becomes a measurable physical phenomenon.

### Slice 12 — Nuclear Fission

> **Build status (2026-10-12): nothing built.**

- **Materials:** uranium, graphite/moderator material, heavy-water-like coolant/moderator (if
  desired), boron, zirconium-like cladding, steel, high-temperature ceramics, lead.
- **Machines:** fuel fabrication press, nuclear reactor, control rods, reactor vessel, coolant loop,
  steam generator, turbine, radiation shielding.
- **Power chain:** `fission → heat → steam/working fluid → turbine → mechanical power → generator →
  electricity`. The reactor produces heat, never directly electricity.
- **Design principle:** nuclear power is another energy-conversion chain, so it participates in Work
  conservation (`feedback_philosophy.md` §10).
- **Control depth (confirmed):** control-rod positions are discrete player inputs (withdrawn / inserted
  / SCRAM), correctly matching the switch-only controller rule — the same discrete-vs-continuous split
  Slice 6's steam engine already makes for speed control. But the reactor's underlying reaction
  rate/thermal state is a real, continuously-simulated value underneath those discrete inputs — it can
  actually climb toward a meltdown between player actions if mismanaged. This is not a teleport between
  three fixed bands with nothing happening in between; genuine near-misses are possible.
- **Milestone:** extremely high energy-density thermal power becomes practical.

### Slice 13 — Nuclear / Ender Coupling (aspect, not a standalone tier)

> **Build status (2026-10-12): nothing built.**

**Reframed:** the user is attached to the IDEA here — letting Ender phenomena interact with nuclear
physics, as a way to bridge Minecraft-native materials into the mod's science model — but not to it
being its own dedicated tier. This is now an *aspect* that should fold into a broader "nuclear-science"
tier (alongside Slice 11/12's radiation and fission content) rather than a standalone Slice 13. Kept
under its own heading here for now since the broader tier isn't drawn up yet, but treat it structurally
as part of that tier, not a separate one.

The first deliberately high-concept fusion of "ordinary" and Minecraft-native physics.

- **Hypothesis to explore:** under sufficiently energetic conditions, Ender-related materials may
  exhibit behaviors ordinary material physics does not predict — anomalous particle behavior, spatial
  distortion, unusual inertia/radiation response, matter displacement, resonance with high-energy
  systems.
- **Candidate materials:** ender pearl derivatives, end stone derivatives, chorus-derived materials,
  exotic crystals, ender-reactive ceramics.
- **Candidate equipment:** ender-material analyzer, high-energy resonance chamber, spatial detector,
  anomaly containment vessel.
- **IMPORTANT — Unknown, not Proposal.** This is intentionally speculative fiction. Ask the user to
  decide the actual mechanics, properties, and terminology before implementing anything here.
- **Milestone:** the player discovers some Minecraft-native phenomena couple to high-energy physical
  systems.

### Slice 14 — Sculk / Experience Engineering

> **Build status (2026-10-12): nothing built.** Sculk and echo shard have `material/` property
> files only; XP (`feedback_mechanics.md` §1) is unbuilt.

Sculk as a scientific subject, not a final-tier magic resource.

**Partly superseded by `feedback_mechanics.md` §1, which is further along than "Unknown."** That
section (speculative, not built, but a real working model — see `TODO.md` §1) already settles: XP is
a physical byproduct with a PneumaticCraft-style two-dimensional quantity(`XPu`)/capacity relationship
(§1.2), sculk is the **capture/store/transmit conduit, not the source** (§1.3 — deliberately
separable from "sculk resonance," i.e. vibration sensing, which stays its own domain), and XP is
explicitly **not a fifth power currency** — no engine converts `XPu` to `Work`; it's only a
conditioning input (repair, catalysis, material conditioning) on top of one of the four real energy
domains (§1.4-§1.6). Anything in this slice should build on that framework rather than re-opening it.
What mechanics.md §1 does *not* cover, and this slice still owns: the vibration-sensing/signal-
propagation side of Sculk (a genuinely separate phenomenon per §1.3's own note), Echo Shards, and
memory-like behavior.

- **Materials:** sculk, echo shards, redstone, quartz, experience-derived materials if desired.
  (Amethyst removed — see Slice 8's note; down to two confirmed appearances in the whole roster.)
- **Phenomena to investigate:** vibration sensing, signal propagation, energy absorption, resonance,
  memory-like behavior. (XP quantity/level/capacity itself is `feedback_mechanics.md` §1's job, not
  this slice's — see above.)
- **Confirmed:** vibration/signal sensing is the *lore justification* for sculk enabling better sensors
  — it is explicitly NOT a separate parallel sensing pathway. Those sensors feed into the existing
  `control/data` (`DataNode`/`DataLinkManager`) infrastructure as a new sensor type, same as any other
  instrument.
- **Candidate machines:** sculk analyzer, resonance transducer, XP reservoir (the
  `feedback_mechanics.md` §1.2 reservoir, not a new concept), sculk sensor package, echo memory device.
- **Candidate outputs:** vibration sensor (a `DataNode` source), resonant transducer, experience-storage
  component, echo-memory component.
- **IMPORTANT — Unknown.** Sculk's vibration/signal/memory behavior and Echo Shards' physical
  interpretation are not finalized. Ask before implementation. (XP's own model is drafted in
  `feedback_mechanics.md` §1, still with its own `[OPEN]` items there — don't re-derive it here.)
- **Milestone:** sculk becomes an engineered information/energy material.

### Slice 15 — Advanced Electronics

> **Build status (2026-10-12): nothing built** of the electronics roster. The controller line it
> defers to has Tier 1 (Punch Card) built — see below.

- **Materials:** high-purity silicon, germanium/other semiconductor materials if desired, copper,
  gold, aluminum, ceramics, polymers, redstone, rare-earth materials. (Amethyst removed — see Slice 8's
  note; down to two confirmed appearances in the whole roster.)
- **Processes:** semiconductor purification, doping, oxidation, lithography-like patterning,
  deposition, etching, wafer processing.
- **Products:** diode, transistor, integrated circuit, memory chip, sensor chip, power transistor,
  oscillator, microcontroller.
- **Machines:** semiconductor fabrication station, doping apparatus, lithography apparatus, wafer
  processing tools, advanced electronic tester.
- **Controller progression — superseded by the built spec, not a competing proposal.** This slice
  used to name its own four stages (`mechanical/punch card → relay logic → electronic logic →
  programmable controller`), but `feedback_controller_spec.md` §4 already settled and partly built
  this: **Punch Card (Su-driven, hard-rewrite) → Circuit Board (electrical, hard-rewrite) → Floppy
  Disk (electrical, soft-rewrite) → USB/ender-networked (electrical, soft-rewrite, program lives on
  the controller itself)**, with Punch Card built (`control/controller/`, `control/program/`,
  `control/programmer/`) and the rest still just the table. Slice 15's electronics roster (diode,
  transistor, integrated circuit) is the material precondition for the Circuit Board/Floppy Disk
  media, not a separate tier scheme — defer to `feedback_controller_spec.md` for names and stages.
  Same principle either way, and it is Established there, not just Strong direction: capability never
  tiers (a punch card can express anything a late controller can, per `feedback_philosophy.md` §13),
  only iteration cost falls and how much the controller can hold in mind at once grows, and older
  media keep their niche (mechanical control has no electricity dependency, so it survives an outage
  that takes an electronic controller down with it).
- **Milestone:** compact programmable electronics become manufacturable.

### Slice 16 — Ender Engineering

> **Build status (2026-10-12): nothing built.** Ender pearl and end stone have `material/` property
> files only.

The Ender phenomenon becomes industrial technology only after long observation and instrumentation.

- **Materials:** ender pearls, end stone, chorus-derived materials, resonant crystals,
  ender-infused ceramics, exotic conductors.
- **Machines:** ender analyzer, spatial stabilizer, ender relay, spatial transmitter/receiver,
  dimensional anchor, ender pump/transport apparatus.
- **Application progression:** information transmission → small-item transport → fluid transport →
  larger logistics. **Confirmed: "larger" means bigger than the tiny starting trickle, never means
  out-scaling conventional logistics.** `feedback_philosophy.md` §10's hard cap on Spatial transport
  stands as written — "a small, hard-capped trickle of items or fluid... deliberately not enough to
  replace belts and pipes." Normal belts/pipes stay useful for cheap, high-throughput transport (§1.3);
  spatial transport needs a real niche — distance, precision, immediacy — not blanket superiority or a
  path to eventually replacing belts. This is the same shape as the existing spatial energy type in
  `feedback_philosophy.md` §10.
- **Milestone:** distance itself becomes an engineering variable.

### Slice 17 — Exotic Materials / Netherite Engineering

> **Build status (2026-10-12): nothing built.** Netherite and ancient debris have `material/` files,
> and netherite is a `metal`, so form generation gives it plate/rod/wire/foil items the shop works
> out of a vanilla netherite ingot — generated, not engineered. Vanilla's smithing upgrade is untouched.

**Rewritten per user decision, canon-first reasoning:** netherite itself is NOT hard to make — piglins
made it, canonically, so it can't be some absurdly demanding synthesis. The gate is specifically on
making it *without* someone else's ancient debris/scrap, not on making it at all.

- **Foraging path (free, vanilla-adjacent):** finding and smelting ancient debris is "using the world's
  existing netherite-precursor material" — stays as the cheap, no-infrastructure route, same standing as
  finding any other ore.
- **Synthesis path — easy inside the Nether:** the Nether's own ambient conditions are what make
  netherite synthesis easy there, canonically (piglins do it casually). Actually producing new netherite
  from raw materials (not found scrap) should be easy *if done in the Nether*.
- **Synthesis path — hard outside the Nether:** two options, both genuinely harder: (a) physically set
  up a production line in the Nether — a real cost specifically for this mod, since Feedback is
  infrastructure-heavy and dragging a whole line into the Nether is expensive; or (b) engineer a way to
  recreate the Nether's advantageous conditions elsewhere — a real, harder synthesis chain.
- **Composition/mechanism:** draw from `speculative_physics_inspo_doc.md` rather than inventing new
  chemistry here — that document is the parts bin for exactly this kind of Minecraft-exotic material
  question.
- **[OPEN] — vanilla's smithing-table upgrade:** vanilla still lets you smith diamond gear into
  netherite for free at a smithing table, same category of problem the furnace recipes solved via
  removal (`CLAUDE.md`'s "Vanilla Is Not Exempt", §15). The *making* question above is answered, but
  whether vanilla's free smithing-table upgrade needs removing, restricting, or is fine left alone was
  not reached in this round — genuinely open, not decided.
- **Milestone:** minecraft-native materials become deliberately engineered rather than merely
  harvested.

### Slice 18 — Fusion / Ultimate Energy Systems

> **Build status (2026-10-12): nothing built.**

Only after a mature industrial, electrical, thermal, and materials base exists.

- **Materials:** tungsten, titanium, niobium-like superconducting material, advanced ceramics,
  specialized conductors, superconducting materials, exotic ender materials.
- **Machines:** vacuum vessel, magnetic confinement system, superconducting magnets, plasma injector,
  fusion reactor, fusion control system, advanced heat exchanger.
- **Power chain:** `fusion → thermal energy → working fluid → turbine → mechanical power → generator →
  electricity`.
- **Ender interaction (Unknown):** Ender materials providing anomalous confinement/stabilization
  conventional materials can't. Proposal only — confirm the fictional physics before implementing.
- **What actually distinguishes fusion from a maxed-out fission reactor (confirmed, three answers, not
  just "bigger number" — though that's also true and fine):**
  1. **Genuinely higher power ceiling** than any achievable fission reactor. True, and allowed to just be
     bigger — that's not a cop-out on its own, just not the *only* answer.
  2. **No waste byproduct**, in explicit contrast to fission's waste-handling mechanic — a real point of
     engineering difference, not flavor.
  3. **Meaningfully simpler/smoother control-loop requirements than fission.** Never literally
     "set-and-forget," but noticeably less finicky — an earned reward for reaching the last tier, in
     contrast to fission's real continuous-reaction-rate risk (Slice 12).
  - **Rejected idea, worth recording why:** fusion producing new/heavier elements — breeding into the
    island of stability, feeding later tiers with new materials — was considered and genuinely liked,
    but rejected specifically because fusion is currently the terminal/last tier: there's no later tier
    for such materials to feed into. If a tier is ever added after fusion, revisit this.
- **Milestone:** the player engineers an extreme energy-density system combining ordinary and
  minecraft-native physics.

---

## 3. Energy / power progression

**Category split:** the *principle* that sources keep separate niches instead of forming a strict
replacement ladder is Established (`feedback_philosophy.md` §10, §14 "old technology keeps its
niche"). The *specific ordering* below — which source shows up before which — is Proposal, same as
everything else in §2.

Not a straight "better generator" ladder — different sources keep different engineering niches
(`feedback_philosophy.md` §1.3/§14). **Redrawn as overlapping bands, not a single-arrow chain** — the
old version was one strict top-to-bottom sequence sitting right next to prose insisting it wasn't a
ladder, which visually contradicted its own caption. Bands run in parallel; a lower band doesn't end
where a higher one begins:

```
Mechanical band:    manual power → water/wind → mechanical transmission ───────────────────────▶
                                                                                (stays useful throughout)

Thermal/chemical:            steam ─────────────▶ combustion engines ──────────▶ nuclear heat ──▶ fusion
                                     \                                  \
Electrical band:                     galvanic cell (novelty, Slice 4)   generators → electricity
                                      ...................................│................▶ gas turbines /
                                      (idle until Slice 5's industrial     │                 advanced engines
                                       electrochemistry, after Steam)      │
                                                                            ▼
                                                              high-efficiency electrical generation

Exotic band:                                                          exotic nuclear/ender coupling ▶
                                                                       (aspect of nuclear-science tier,
                                                                        see Slice 13)
```

Read top-to-bottom within a band as rough sequence; read left-to-right across bands as "roughly the
same era, running in parallel," not "must happen in this order." The galvanic cell's dotted line marks
it as a novelty sitting idle until Slice 6 (Steam) makes the electrical band industrially viable — see
Slice 4/5's notes for why.

Work conservation across this whole chain is already Established — see `feedback_philosophy.md` §10.
No new principle here, only the candidate slice order above. This is still a Proposal-tier
illustration — it doesn't claim more precision than the slices it summarizes.

### 3.1 Fuel

Fuel should eventually represent finite chemical energy rather than "burns for N ticks at T
temperature." The current fallback (`feedback_philosophy.md` §15, "The fallback") is an accepted,
explicitly non-physical compatibility shim, not a target state. Do not redesign fuel accounting without
asking the user first about the intended energy quantities and conservation rules — this is exactly
the kind of Work/fuel accounting question §7 below flags as unresolved.

---

## 4. Geology and ore generation

> **Build status (2026-10-12): nothing built — no worldgen exists at all.** This is why tin, zinc,
> lead and kaolinite have no survival source (Slices 1-3 above).

Direction: large, irregular, geologically meaningful deposits instead of isolated ore-block scatter
(`feedback_philosophy.md` §14, "Geology should matter", **[OPEN]**).

### 4.1 Candidate ore families (Proposal — mineral names, associations, and sizes all unconfirmed)

**Confirmed:** where a metal lists two mineral variants (e.g. copper's chalcopyrite vs. malachite),
they're meant to genuinely diverge in processing route and byproducts, per §12's rule that different
routes must have different byproducts — not just be a worldgen reskin of the same recipe. Exact routes
and byproducts are still unconfirmed (see §7), but the *intent* that they diverge for real is settled.

| Metal | Candidate ore mineral(s) |
| --- | --- |
| Copper | Chalcopyrite, malachite/oxide copper minerals |
| Zinc | Sphalerite |
| Tin | Cassiterite |
| Lead | Galena |
| Nickel | Pentlandite |
| Chromium | Chromite |
| Aluminum | Bauxite |
| Iron | Hematite, magnetite |
| Sulfur | Pyrite and sulfur-bearing deposits |
| Silica | Quartz-rich deposits |
| Uranium | Uraninite |
| Thorium/rare earths | Monazite/thorium-bearing analogues, if desired |
| Hydrocarbons | Crude oil/natural gas reservoirs, shale, tar/bitumen |

### 4.2 Deposit design goal

A deposit should be more than a random cluster of ore blocks:

```
surface indicators
→ low-grade peripheral material
→ primary ore zone
→ richer central zone
→ deeper associated minerals
```

Different deposit types may have different geometries and associated resources.

### 4.3 Prospecting progression (Proposal — simplified per user feedback)

**The previous 7-step "model deposit" ladder over-engineered this.** User's own words: "I don't really
want this to be significantly more complex than GregTech's ore system." Simplified to roughly
GTCEu-style scale: a vein/cluster of ore generates as one geological unit; the player uses a
scanning/prospecting tool near a candidate deposit to reveal what's there (metal, rough size) without
digging it all out blind; better tools narrow the estimate, per the mod's own noise-floor rule (§8 of
`feedback_philosophy.md`) rather than adding more abstract pipeline stages. This is a plain description
at the scale the user asked for, not a citation-accurate account of GTCEu's actual prospecting
tool/UI — that hasn't been verified firsthand against the real mod, and shouldn't be assumed precise
without checking (`../GregTech-Modern-7.5.3` per `CLAUDE.md`'s prior-art rule).

```
find deposit (surface indicator or scan)
→ scan/prospect it (tool-based, one action)
→ tool reports rough metal + size, noisier on cheaper tools
```

---

## 5. Concrete cross-domain milestones

Candidate "big moments," Proposal-level pacing markers rather than locked content. **Added: control-
system milestones**, missing from an earlier pass despite `feedback_philosophy.md` §13 calling
controllers "the heart of automation" and saying they mark eras as much as materials do:

first steel · first controlled high-temperature process · first fine copper wire · first electrical
experiment · first galvanic cell · **first automated on/off control loop** · first steam engine · **first
multi-sensor program-graph** (Punch Card tier) · first large ore-processing plant · first refinery ·
first basic electronic circuit · **first deadband loop holding a process in band unattended** · first
radiation measurement · first nuclear reactor · first successful ender-material experiment · first
integrated exotic system (an ordinary physical system and a minecraft-native system deliberately
combined into one engineered process).

> **Build status (2026-10-12):** reached — first steel, first controlled high-temperature process,
> first automated on/off control loop (bimetallic strip / Controller), first steam engine, first
> multi-sensor program-graph (Punch Card, built; the rebuilt Programmer canvas is unplaytested,
> `TODO.md` top); first fine copper wire is built but unverified in game (`TODO.md` §5g). The
> deadband loop is now *expressible* (2026-10-13: a Read Sensor card reads an actuator's own state,
> plus an OR card; `feedback_controller_spec.md` §5) and tested in the evaluator, not yet held in
> band in game. **Not reached:** everything electrical or later.

---

## 6. Cross-domain interaction testing

Every new subsystem here should be tested against existing systems, not only in isolation:
thermal↔mechanical, thermal↔electrical, chemical↔thermal, chemical↔electrical, electrical↔mechanical,
nuclear↔thermal, nuclear↔ender, sculk↔electronics, redstone↔electrical, spatial transport↔control/data.

---

## 7. Things intentionally left unresolved

Ask the user rather than deciding silently:

1. The exact Work/fuel accounting model (§3.1).
2. Exact energy units and conversion efficiencies for future energy systems (§3).
3. Exact material properties for alloys and advanced materials (slices 2, 10, 17).
4. Exact geology/world-generation rules, mineral roster, and deposit sizes (§4).
5. Exact petroleum formation and reservoir generation (slice 7).
6. Exact radiation model (slice 11).
7. Exact nuclear fuel-cycle complexity (slice 12).
8. Exact relationship between nuclear phenomena and Ender phenomena (slice 13).
9. Exact physical interpretation of Redstone and Sculk/XP's useful properties (slices 8, 14, 15).
   Amethyst is narrower now — confirmed down to two appearances (Slice 1 observation, Slice 8
   piezoelectric payoff), with only a conditional third (a real resonant-frequency measurement use)
   still open; see Slice 8's note.
10. Exact Netherite metallurgy — narrowed to the specific synthesis chain inside vs. outside the Nether
    (slice 17); whether vanilla's smithing-table diamond→netherite upgrade needs addressing is a
    separate, still-fully-open sub-question, also flagged at Slice 17.
11. Exact fusion implementation (slice 18) — the three distinguishers (power ceiling, no waste, simpler
    control) are now confirmed; exact numbers/mechanics under them are still open.
12. ~~Whether any proposed slice 9+ system should exist in the final mod at all.~~ **Answered:** all of
    it — every idea in slices 9-18 stays in some form. What's still open is *shape*, not existence: many
    won't be standalone tiers, may shrink, grow, or fold into aspects of a broader tier (see Slice 13's
    reframe as the first concrete instance of this). Don't read a slice's current size or standalone
    status as fixed.

(Mechanical-network behavior at splits/joins with momentum/inertia, and TPu's rate equation/decay/
monotonicity, are tracked in `feedback_mechanics.md` and `tpu_spec_doc.md` respectively — not
duplicated here.)
