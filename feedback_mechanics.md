# feedback_mechanics.md — Document 2: Mechanics

This is document 2 in the three-document spectrum `CLAUDE.md` describes: not identity (`feedback_philosophy.md`), not roster (document 3, still unwritten). This is *how each decided principle is implemented* — unit arithmetic, sensor/actuator pairings, the thermal model, energy networks, the control node set. `feedback_philosophy.md` defers to this document by name in a dozen places; those are debts, tracked in `TODO.md` §3.

**Status: started, not comprehensive.** Nothing below is built. Per `TODO.md` item 1, the working method so far has been slice-first — build, then write doc 2 from what the build forced you to decide. This section is the exception: a phenomenon speculatively worked through *before* code exists, because the mechanism needs to be right before a single unit or block entity gets named after it. Treat it as provisional until something built contradicts it, at which point this file gets fixed in the same change, per `CLAUDE.md`'s living-document rule.

---

## 1. Experience (XP)

### 1.1 What it is, mechanically

`feedback_philosophy.md` §2 already commits to the general move this section makes concrete: a phenomenon that demonstrably happens in Minecraft is real in the setting and can be industrialized once someone notices a measurable variable in it. XP qualifies immediately — it is already a stored, transportable, spendable quantity in vanilla. The only work here is giving it the same generate/capture/store/transmit/apply staging every other energy phenomenon in the mod gets (§14 of the philosophy: water, wind, fire, steam, Blaze heat, lightning, redstone).

The mechanism: XP is a physical quantity produced by certain living or once-living systems (players, mobs, and per §1.6 below, engineered biological processes) and capturable in an appropriate storage substrate. It is **not** memory, thought, or information — nothing about it is semantic. It is a byproduct, the same way heat is a byproduct of combustion. This distinction matters mechanically, not just narratively: it means XP-consuming machines never need to know *what* they're doing to a workpiece, only that a quantity was spent. That keeps XP consistent with "machines define physical operations, not recipes."

### 1.2 Quantity and level — genuinely two-dimensional, like PneumaticCraft's air

First pass at this treated level as a pure function of quantity (reusing the Tu/Work split, where any two reservoirs holding the same amount necessarily read the same state). That's wrong: it throws away a variable PneumaticCraft's pressure/air-amount model gets right, and that the mod already has a working pattern for. **Level depends on quantity *and* the reservoir's capacity — two independent numbers, not one.**

The existing pattern to copy is `ThermalMass`, not `ItemHeat`. Temperature isn't heat; it's heat divided by mass, so a bigger thermal body needs more `Work` to reach the same `Tu`. Level isn't quantity; it's quantity divided by the reservoir's capacity, so a bigger reservoir needs more `XPu` to reach the same level. Same shape as an ideal-gas pressure vessel (pressure depends on amount *and* volume), which is exactly PneumaticCraft's model — worth reading as prior art before building, per the standing rule (§1.7).

- **Quantity** — how much XP is stored. Extensive, additive. Unit: `XPu`, an int, following `Fu`'s precedent (XP points are integers in vanilla; no argument for fractional XP that isn't also an argument for fractional `Fu`).
- **Capacity** — a property of the specific reservoir block, not of the stock it holds. Fixed per block (a bigger reservoir is a bigger block, exactly like a bigger crucible), varying by tier/size — this is the "larger vessel" upgrade the hard design rules already name as the canonical example of a physical upgrade. Giving XP storage the identical upgrade shape as everything else is the point; it shouldn't need a bespoke rule.
- **Level** — the intensity of what's stored. Derived, **not stored as a field**, computed on read from quantity and capacity together, the same on-demand discipline as `ItemHeat` and `Heat.tick`. Simplest form is a plain ratio, `quantity / capacity`. To keep vanilla's real, non-arbitrary curve as the source of nonlinearity rather than inventing one, evaluate vanilla's level-for-points function at quantity scaled by a reference capacity: `level = vanillaLevelForPoints(quantity × referenceCapacity / capacity)`. A reservoir built at the reference capacity reads exactly vanilla's own level for its stock; a bigger one reads lower for the same stock, a smaller one reads higher — same shape as `ThermalMass`, same shape as a pressure vessel.

  The reference capacity is a player's own XP bar — the only reservoir vanilla already defines a curve for. Every engineered reservoir's capacity is set relative to that, which means the calibration constant this whole unit hangs off is, structurally, the size of a human brain. Nobody designed it that way; vanilla's curve was simply never anything else.

**[OPEN]** Whether capacity is fixed per block only, or itself a fittable/upgradeable property (a "wider bore" fitting on an existing reservoir) — this is the same open question the thermal system already has for insulation and vessel size, so resolve it once, there, rather than twice independently.

**[OPEN]** Whether "level" for engineering purposes should be vanilla's stepped integer level, or a continuous float evaluated at the same curve (a reservoir sitting at 14.6-equivalent, not just 14). A continuous read gives instruments something finer to disagree about (per §8's noise-floor rule — an integer level has no room for reading error); a stepped level is legible at a glance. Leaning continuous. Decide when the first XP instrument gets built.

### 1.3 Storage and transmission substrate

Sculk is the capture/store/transmit medium, not the source (matches the source brainstorm's §4 exactly, and is worth keeping: sculk-as-conduit is a separable claim from sculk-as-anything-else, so sculk resonance — vibration sensing, unrelated to XP — can become its own domain later without this section needing to change). A sculk-based reservoir block holds `XPu`; a sculk-based conductor moves it. This gives XP the same three-tier shape (source, network, sink) as the rotation network and the thermal model, which argues for it eventually living in its own package (`core/experience/`, mirroring `core/thermal/` and `core/rotation/`) — but per the source layout rule, that package doesn't get created until there's a second thing to put in it. Right now this is notes, not code.

### 1.4 Energy niche — not a fifth currency

This is the load-bearing decision, and it resolves a real tension the source brainstorm didn't quite settle. `feedback_philosophy.md`'s hard rules commit to exactly four domains that are natively consumable and actuation-capable: Mechanical, Thermal, Chemical, Electrical. Adding XP as a fifth interchangeable currency — something with an "engine" that converts `XPu` into `Work` at some ratio — would break that enumeration and quietly re-create GTCEu's everything-funnels-to-one-currency problem with a different currency.

**Decision: XP is not a power source and has no engine.** Nothing converts `XPu` into generic `Work`. It never enters an actuator. Its only role is as a *conditioning input* to a process that's already running on one of the four real domains — it changes how a process happens, never whether it has enough energy to happen at all (this is the source brainstorm's §7 point, and it's correct, it just needed to be stated as a hard constraint rather than a stylistic preference). Concretely: a material-conditioning stage still needs its heat and its electrical bias from real domains; XP is the additional variable that decides what state the material lands in, not what powers the stage.

This is also why XP is exempt from the "problem with one answer is not a problem" tension it might otherwise create — it doesn't compete with the four real domains for the "how do I power this" answer, so it can't collapse a process down to one obviously-correct energy source.

### 1.5 Sensing

Already well-specified by the source brainstorm and consistent with the existing "world resolves on truth" rule (§8 of the philosophy): an XP instrument reveals a raw signal, never a semantic verdict. It never says "reaction 87% done." It reports a number — local level, a fluctuation amplitude, a purity signature — and the player decides what the number means. This needs no new rule; it's the existing `Instrument` contract (read-only, no way to act) applied to a new measurable. The six apparatus properties `TODO.md` §3 already owes for every instrument — range, resolution, accuracy, control, response, stability — apply here unchanged. An XP sensor's spec sheet is not a new kind of spec sheet.

### 1.6 Applications

Three application shapes, all consistent with existing hard rules:

- **Repair.** XP restores structure that still physically exists in damaged form — cracks, dislocations, residual stress — never matter that's been fully lost. A part missing material outright still needs feedstock. This is the existing distinction between "conventional repair replaces lost material" and gives Mending an industrial reading without turning XP into a duplication mechanic.
- **Catalysis.** A catalyst conditioned with `XPu` lowers a reaction's requirements (temperature, pressure) without supplying the reaction's energy. It degrades with use and needs reconditioning — a consumable capital cost, not a permanent unlock. This is a real complete loop (manufacture, condition, run, degrade, recondition) and gives XP an ongoing consumption sink instead of a one-time enchant-and-forget.
- **Material conditioning.** A final-process-stage input alongside real heat/electrical conditions, per §1.4. Never gates completion — it's a probability/quality variable feeding the noise-floor model already established for instrumentation, not a hard requirement.

**Explicitly rejected:** XP as a bulk power source for machines (violates §1.4); XP sensors as recipe-completion meters (violates §8's "world resolves on truth, instruments never enter the calculation").

### 1.7 Open questions and debts

- **[OPEN]** Unit name. `XPu` used provisionally above; confirm against the `Units` checklist before it's real.
- **[OPEN]** Continuous vs. stepped level — see §1.2.
- **Prior art not yet read.** Per `CLAUDE.md`'s standing rule, XP-as-industrial-resource has real precedent — EnderIO (XP fluid + vat), Botania (mana pool, overflow behavior under load), Actually Additions (XP crystal storage). **PneumaticCraft is now the closest match for §1.2 specifically** — its air pressure/volume model is the two-dimensional quantity/capacity relationship this section adopts. None have been cloned or read yet. Do this before writing any code, the same way Create/TFC/GTCEu were read before the rotation network, thermal model, and fitting system.
- **Not yet decided:** whether XP belongs in `feedback_philosophy.md`'s phenomena list (§14: "the world contains multiple useful energy phenomena"). It isn't added there now because nothing here is built — per `TODO.md`'s slice-first method, that entry should wait until an XP reservoir actually exists to be described, not be asserted from this document.
- **Rejected from the source material:** the "experience is not memory" framing and the neuropolariton terminology are narrative, not mechanics, and stay out of this document. If they're wanted anywhere, they belong in the compendium (`speculative_physics_inspo_doc.md`) or eventually document 3, not here.

---

## 2. Blaze Material

`feedback_philosophy.md` names Blaze material as its own headline case (§80/82) — "Blaze Rod plus a crude heat exchanger gives a dramatically hotter working fluid" — and it has sat unworked the longest of anything the philosophy gestures at by name. The question this section answers is specifically *what Blaze does beyond being a hotter fuel item*, since "hotter fuel item" turns out to already be free.

**First pass at this section proposed a live-containment `HeatSource` block (a cage holding trapped Blaze essence, killed by water contact). Superseded below — a much cheaper design does more with what's already built, and doesn't need any new block-entity machinery at all.** The tempting part of the old version was treating Blaze as a *fire that never depletes*; the better read, below, is that the rod isn't a fire at all. Kept here rather than deleted because the water-collision-with-`Quench` idea (a real system interaction, not a Blaze-specific rule) is worth remembering if some *other* phenomenon turns out to need a genuine containment/kill-condition shape later.

### 2.1 The split: rod stores, powder burns

Vanilla gives Blaze Rod and Blaze Powder as two items already. Feedback's mechanism gives them two *roles* instead of treating them as interchangeable brewing/fuel ingredients:

- **Blaze Rod does not go in a firebox.** It's a storage item, not a fuel.
- **Blaze Powder is the fuel** — and it's an extreme one, because breaking a rod into powder is what releases what it was holding.

### 2.2 Rod: reuses `ItemHeat`, just with different numbers

A `Blaze Rod` is an ordinary `ItemHeat`-bearing item — same stamp-plus-timestamp, computed-on-demand pattern as any hot workpiece, no new plumbing. What's different is the constants: it settles at a small elevated baseline over ambient (hand-warmer warmth, not blazing) via a cooling rate small enough that reaching true ambient takes a very long time. When it finally does, it becomes a second, discrete item — **`Spent Blaze Rod`** — rather than a continuous "0% charged" reading, per the hard rule against continuous knobs (§3): the player-facing state is "still good" or "gone cold," not a percentage to watch tick down.

**[OPEN]** `ItemHeat`'s cooling rate is currently one global constant (the hot-ingot 1.5 Tu/t). This needs to become per-material data before Blaze Rod can use a different one — the same promotion `Fuel` already made for `temperature` and `Quench` made for `minTemperature`. Third instance of the same lesson: a figure that started as a single constant keeps turning out to need to vary by material. Worth naming as a pattern in its own right rather than re-discovering it a fourth time.

**Spent Blaze Rod is rechargeable** by reheating — a `ThermalProcess` table row (sufficient `Tu`, sufficient duration), no new system, just a data row once the underlying item exists.

### 2.3 Powder: an ordinary `Fuel` entry, just an extreme one

Grinding a **still-charged** rod (existing size-reduction/grinding pipeline) yields `Blaze Powder`, a `process/Fuel.java` entry with a very high `temperature`, very short `duration`, and — this is where the existing `spread` field earns its keep — a wide `spread`, so the burn is genuinely inconsistent rather than just hot. That inconsistency is what "almost explosive" means mechanically: a fuel that can spike far past its own nominal rating, without inventing a second mechanic to say so.

Grinding a **`Spent Blaze Rod`** instead yields an inert dust with no fuel value — the real cost of letting a charged rod go cold before using it, and a genuine timing decision rather than flavor text.

### 2.4 What happens when it's fed to something

Overheating a Feedback vessel with Blaze Powder needs no special case at all: it's the existing overshoot rule (force that can't go into the work goes into the machine; the machine degrades, never breaks) applied to a fuel whose `spread` happens to be wide enough to overshoot often. No new consequence to design.

A vanilla furnace was briefly considered here as the deliberate exception to that promise — "degrades, never breaks" is a commitment about Feedback's own apparatus, not blocks the mod doesn't own. Moot: vanilla's furnace/smoker/blast furnace are uncraftable and functionally replaced by Feedback's own `ThermalVesselBlockEntity` (`feedback_philosophy.md` §15), with structure-spawned copies slated for eventual replacement too (`TODO.md`). No survival player has one left to feed the joke to.

### 2.5 Open hook: pulsed processes

The idea that Blaze's `spread`-driven variance could be a *feature* for a process that wants cyclic rather than steady heat (tempering-adjacent, scheduled annealing) stays a marker, not a mechanism — no such process exists yet in the mod to make it real. Revisit when document 2's process section covers thermal cycling, if it ever does.

### 2.6 Prior art and debts

- **Prior art not yet read.** Real hand warmers and dust explosions are both genuine physical phenomena (surface-area-to-volume ratio governing reaction/release rate), so this section is on solid "scientific, not realistic" ground without needing outside mod precedent for the mechanism itself. Still worth checking whether TFC or GTCEu — both already reference mods here — have a rod/dust dual-item precedent worth reading before building.
- **[OPEN]** `ItemHeat` cooling rate needs to move from a global constant to per-material data — see §2.2.
- **Rejected:** the live-containment `HeatSource` block and its water-kill condition — see the note at the top of this section.

---

## 3. The Thermal Process Model: TPu

Built, not speculative — `tpu_spec_doc.md` (the brainstorm this section implements) is the fuller account; this is the built shape. Replaces `holdTicks` in `ThermalProcess`, `ThermalVesselBlockEntity` and `CrucibleBlockEntity`, and the Work-based `VanillaFallback` mechanism `feedback_philosophy.md` §15 originally specified (now corrected there in the same change, per the living-document rule).

### 3.1 What TPu is, and isn't

TPu (Thermal Process Units) is process-local progress, not a physical quantity — it does not join §17's unit table, has no codec-through-`Unit` machinery, and is never displayed. A `ThermalProcess`'s `required_tpu` field is a plain float, the same "no type" treatment `Work` already gets, for the same reason: a wrapper here would imply TPu joins the physical quantities it is explicitly not one of.

**Never player-facing**, per the source doc: no tooltip, no measurement tool, no HUD figure. What *is* shown on a process's JEI card is `required_tpu` itself, printed as "ticks (seconds) at optimum" — that is a **requirement**, and philosophy 8's table says requirements are free, exact, and published regardless of instrumentation. The number a player reads is never "how much TPu has accumulated" (a reading, which would cost an instrument and doesn't exist), only "how many ticks this needs if held exactly at the optimum" (a spec, same status as steel's temperature band).

### 3.2 Accumulation: a triangular suitability curve

Every `ThermalProcess` now carries `optimal_temperature` alongside `min`/`max`. `ThermalProcess.suitability(tu)` returns a 0-to-1 fraction: 0 at or outside the band edges, 1.0 at the optimum, ramping linearly between. Each tick in band, `currentTpu += suitability`; the process completes when `currentTpu >= requiredTpu`. Held exactly at the optimum, this reproduces the old flat-`holdTicks` behaviour exactly (`requiredTpu` ticks, no more); drift off it and the same number of ticks stops being enough, which is the entire point — a better vessel or a better-aimed player finishes measurably faster, something a flat countdown could never express.

This is philosophy 8's "quality should fall off gracefully, not switch off" applied to completion itself rather than to yield. Three points (min/optimal/max) rather than the source doc's illustrative six-point table — the doc calls the exact curve shape a tuning decision, and three is the minimum that has a peak at all.

### 3.3 Decay, not reset

Outside the band, or changing faster than `max_rate` tolerates, `currentTpu` decays at `FTuning.TPU_DECAY_PER_TICK` (one tick-equivalent per tick, matching the fastest possible gain) rather than resetting to zero. This is a real behaviour fix, not only a doc-follow: `CrucibleBlockEntity` already stalled progress out-of-band ("nothing lost") while `ThermalVesselBlockEntity` reset it outright for the identical case — two block entities disagreeing about the same rule. Decay is the correct middle ground the source doc asks for, and now both share it.

**`max_heating` → `max_rate`, and why "annealing" was never a missing mechanic.** The field was heating-only in name and in `CrucibleBlockEntity`'s check (`lastDelta > max`); `ThermalVesselBlockEntity`'s equivalent check already used `Math.abs(lastDelta)`, bounding both directions — the same species of two-block-entities-disagreeing bug this section already documents once, found a second time. The fix widens the field rather than forking a new one: `maxRateTuPerTick` now bounds the rate of change either way, both block entities agree, and Slice 2's "annealing"/"controlled cooling" turn out to need no new mechanic at all — they're `require_cooling: true` plus a tight `max_rate`, exactly the same two knobs tempering already has, just both turned at once. The user's own framing settled this: *"thermal processes should all be `ThermalProcess`; if cooling, annealing, and otherwise can't fit within the schema, the schema isn't loose enough."* No annealing datapack entry exists yet — this is the mechanism only, same "infrastructure before the concrete use" pattern as `Instrument` before any instrument existed.

Tempering's `require_cooling` flag keeps its own, different rule: a flat or rising tick **pauses** (no accumulation, no decay) rather than either advancing or decaying, because philosophy 13's actuator-is-a-switch constraint means the player has no rate to hold steady with, only on/off timed by hand or a Damper — punishing the wait as harshly as an out-of-band drift would make the mechanic itself the trap.

A melt (`required_tpu: 0`) bypasses suitability entirely and completes the instant it is in band, exactly as `hold_ticks: 0` did.

### 3.4 The vanilla fallback: thermal hints, not a fixed pick

`VanillaFallback.find` used to return one recipe — whichever of `smelting`/`blasting`/`smoking` had the shortest declared cooking time, decided once. That made an ore's blast-furnace time win in every vessel permanently, including a plain Furnace that can never reach blasting conditions — exactly the hidden-recipe-rule `tpu_spec_doc.md`'s "Do Not Let the Shortest Vanilla Recipe Win" section calls out, and exactly what §15's "specialisation is emergent, not enforced" already promised not to do.

`find` now returns a `Match(recipe, suitability)`, recomputed every tick against the vessel's *current* temperature: `smoking` maps to a food-like low/stable profile (`FTuning.SMOKING_OPTIMAL_TU`, ceiling at `FOOD_MAX_TU`), `blasting` to a hot metallurgical profile (`BLASTING_OPTIMAL_TU`, no ceiling), and plain `smelting` to a generic middle profile (`SMELTING_OPTIMAL_TU`) — three curves through the same `ThermalProcess.suitability` math a hand-authored process uses, never a per-vessel identity check. An ore now finishes faster in a vessel that is actually hot, and slower in one that isn't, without either block knowing what the other is.

The recipe's own declared cooking time is the `required_tpu` baseline directly (no separate Work calibration constant), per the source doc's "convert vanilla cooking time into baseline TPu" — this also retires `FTuning.FALLBACK_WORK_PER_200_TICKS` and the `Tu × ticks`-as-Work arithmetic it fed, which is the exact "fake Work" pattern the source doc opens by rejecting.

---

## 4. Alloying

Built, not speculative. Prior art: TerraFirmaCraft's `AlloyRange`/`AlloyRecipe`/`FluidAlloy` (EUPL-1.2, read per the standing rule); see `THIRD-PARTY-LICENSES.md`'s TFC section for exactly what was taken (the ratio-window shape) and what wasn't (an "unknown alloy" fallback fluid — see §4.3).

### 4.1 The crucible's tank was never the source of truth to begin with

Before this, `CrucibleBlockEntity` held one `FluidTank`, filled directly from a completed melt. Pouring a second, different molten metal into an already-occupied tank simply failed — a vanilla `FluidTank` refuses a fluid that doesn't match what it already holds — which was an accidental block on mixing, not a designed one.

`AlloyMix` (`process/AlloyMix.java`) replaces that: a plain `Map<Fluid, Integer>` ledger of every metal actually poured in, held by the crucible alongside its tank. The tank becomes a *view* computed by `AlloyMix#resolve` — the matched alloy's fluid if the ratio resolves to one (`AlloyTable#find`), the single metal itself if only one has ever been poured (so an unalloyed melt behaves exactly as before this existed), or empty.

### 4.2 The recipe: a composition window, not a fixed ratio

`AlloyRecipe` (`process/AlloyRecipe.java`, loaded from `data/feedback/alloy/*.json` by `AlloyTable`) is a list of `AlloyRange`s — one per metal, each a `[min, max]` fraction of the whole melt. Bronze is copper 88–92% + tin 8–12%; brass is copper 60–70% + zinc 30–40% (`alloy/bronze.json`, `alloy/brass.json`). A mix matches only if it holds *exactly* the metals a recipe names — no unlisted metal tolerated — and every one's share falls in its range.

This is the same shape §7's control envelope already describes for temperature (a band with an optimum), applied to composition instead — not a new kind of continuous knob, because the player never dials a ratio directly. They pour known-sized ladles of known metals and the ratio falls out of how much of each they used, the same way a crucible's temperature falls out of its fire and mass rather than being set on a dial.

### 4.3 What happens when the ratio is wrong

`AlloyMix#resolve` returns empty. The metal isn't lost — it's still tracked in the ledger — but it isn't anything with a name either, so nothing can be poured out of the crucible until the ratio is corrected. TFC's own answer here is a generic "unknown alloy" fluid with interpolated stats; Feedback doesn't do that, because giving an unnamed mix real stats would mean inventing a fifth material the roster doesn't have, where reporting nothing needs nothing new.

**[OPEN]** There is currently no way to *correct* a bad mix short of building a new crucible — no partial extraction, no way to add more of one metal to dilute the other back into range without also adding more total volume. Worth widening once a real process wants it; not solved speculatively here.

### 4.4 Extraction has to stay in step

A mold draining the crucible's tank (`MoldItem#onItemUseFirst`) mutates the tank directly, which would leave `AlloyMix` reporting a stale, too-high total on the next melt. `MoltenVessel` gained a `default void onDrained(FluidStack)` hook for exactly this; `CrucibleBlockEntity` overrides it to call `AlloyMix#remove`, which shrinks every metal present by the same proportion — there's no such thing as draining "just the tin" — keeping the ratio, and therefore what the tank shows next, correct.

### 4.5 Cast iron: not an alloy, but the other half of this pass

Steelmaking's already-verified recipe (`thermal_process/steel_ingot.json`, `minecraft:iron_ingot` + `minecraft:charcoal`) is untouched. Alongside it, `thermal_process/cast_iron.json` (`iron_ingot` + `coal` → `feedback:cast_iron`, a wider and lower band) and `thermal_process/steel_from_cast_iron.json` (`cast_iron` + `charcoal` → `steel_ingot`, same band as the original, lower `required_tpu`) add a second route: one extra step up front, less time in the second. Two ways to the same material that cost different things — §5's kind of trade, placed by hand here rather than found emergent, which the philosophy explicitly allows.
