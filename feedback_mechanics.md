# feedback_mechanics.md — Document 2: Mechanics

This is document 2 in the three-document spectrum `CLAUDE.md` describes: not identity (`feedback_philosophy.md`), not roster (document 3, still unwritten). This is *how each decided principle is implemented* — unit arithmetic, sensor/actuator pairings, the thermal model, energy networks, the control node set. `feedback_philosophy.md` defers to this document by name in a dozen places; those are debts, tracked in `TODO.md`'s *Document 2* list.

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

Already well-specified by the source brainstorm and consistent with the existing "world resolves on truth" rule (§8 of the philosophy): an XP instrument reveals a raw signal, never a semantic verdict. It never says "reaction 87% done." It reports a number — local level, a fluctuation amplitude, a purity signature — and the player decides what the number means. This needs no new rule; it's the existing `Instrument` contract (read-only, no way to act) applied to a new measurable. The six apparatus properties `TODO.md` already owes for every instrument — range, resolution, accuracy, control, response, stability — apply here unchanged. An XP sensor's spec sheet is not a new kind of spec sheet.

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
- **Rejected from the source material:** the "experience is not memory" framing and the neuropolariton terminology are narrative, not mechanics, and stay out of this document. If they're wanted anywhere, they belong in the compendium (`docs/speculative_physics_inspo_doc.md`) or eventually document 3, not here.

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

**[OPEN]** `ItemHeat`'s cooling rate is currently one global constant (the hot-ingot 1.5 Tu/t). This needs to become per-material data before Blaze Rod can use a different one — the same promotion fuel already made for its flame temperature (now a material's `combustion`, §5.7) and `Quench` made for `minTemperature`. Third instance of the same lesson: a figure that started as a single constant keeps turning out to need to vary by material. Worth naming as a pattern in its own right rather than re-discovering it a fourth time.

**Spent Blaze Rod is rechargeable** by reheating — a `ThermalProcess` table row (sufficient `Tu`, sufficient duration), no new system, just a data row once the underlying item exists.

### 2.3 Powder: an ordinary fuel, just an extreme one

Grinding a **still-charged** rod (existing size-reduction/grinding pipeline) yields `Blaze Powder`, whose material's `combustion` block (§5.7) has a very high `flame_temperature`, very short `burn_ticks`, and — this is where the existing `spread` field earns its keep — a wide `spread`, so the burn is genuinely inconsistent rather than just hot. That inconsistency is what "almost explosive" means mechanically: a fuel that can spike far past its own nominal rating, without inventing a second mechanic to say so.

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

Built, not speculative — `docs/tpu_spec_doc.md` (the brainstorm this section implements) is the fuller account; this is the built shape. Replaces `holdTicks` in `ThermalProcess`, `ThermalVesselBlockEntity` and `CrucibleBlockEntity`, and the Work-based `VanillaFallback` mechanism `feedback_philosophy.md` §15 originally specified (now corrected there in the same change, per the living-document rule).

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

`VanillaFallback.find` used to return one recipe — whichever of `smelting`/`blasting`/`smoking` had the shortest declared cooking time, decided once. That made an ore's blast-furnace time win in every vessel permanently, including a plain Furnace that can never reach blasting conditions — exactly the hidden-recipe-rule `docs/tpu_spec_doc.md`'s "Do Not Let the Shortest Vanilla Recipe Win" section calls out, and exactly what §15's "specialisation is emergent, not enforced" already promised not to do.

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

---

## 5. Materials and Form Steps

Built. Replaces the per-recipe `deformation/` and `removal/` tables; prior art in
`THIRD-PARTY-LICENSES.md` (GregTech's material/prefix model, shape only).

### 5.1 What is stored

- **Material** (`data/feedback/material/*.json`): `hardness`, optional `min_temperature`/`max_temperature` (a hot-working window, deformation only), `density`, and `forms`, a map from form name to the item that is this material in that form. The item id appears here and nowhere else. Everything else is optional, unread so far, and listed in §5.6.
- **Form step** (`data/feedback/form/*.json`): `[from form, to form, operation]` plus `base` and `slope`. Cost in Fu for a material is `base + slope * hardness`, rounded, at least 1.

A `Deformation` or `Removal` is *compiled* from every material that has both forms of a step (`FormTable`). Machines, sync and JEI read the compiled lists through `DeformationTable`/`RemovalTable` and do not know recipes are not authored. A removal sheds the material's `swarf` form, if it has one.

### 5.2 Why affine, not `k * hardness`

A blow lands `St / hardness` Fu, so blows needed go as `work * hardness / St`. With `work = k * hardness` that is hardness squared: steel at 15 would need 225 times copper's blows for a step it should need about four times the work for. `base` absorbs the part of a step's cost that does not grow with hardness. The existing figures fit it closely: steel/copper cost was 4.3x for plate, 4.0x for rod, 3.75x for wire.

### 5.3 State changes are material changes

Hardened steel is 25 hardness against steel's 15, and removal reads hardness from the material. A form cannot override it; hardened steel is its own material and quench maps one material's item to the other's. If something later needs hardness to vary within a material, that is the sign the material is really two.

**Tempered steel has forms; hardened steel does not.** Hardened steel is glass-brittle (toughness 8, under its own hardness): anything that could move it cracks it, so it comes as an ingot and a ground ingot and nothing else. Tempering buys the toughness back (40), and what tempered steel is *for* -- springs, blades, tools, the moving parts of a gun -- is shapes. So it takes every cold-working and machined form a part needs: plate (spring strip, blade stock), rod and wire (spring wire is drawn and coiled after tempering in reality too), and the drilled, turned and ground forms and gun parts that follow from those. It has no hot-working window, because heating it to forging heat would undo the temper; at hardness 20 it is cold-worked, which takes more force than a short-throw crank delivers -- a gear train or a better machine, not a lock. Left out: foil, fine wire and scrap (two cold steps of ductility does not reach them), nugget and block (storage forms are for remelting, and remelting undoes the temper), case and bullet (brass and lead jobs), relief vent (made from a nugget), grip (a grip is for insulating).

**Iron, wrought iron, cast iron, steel.** `material/iron.json` is **wrought iron** in everything but its id, and its display name says so (2026-10-20); the id stays `iron` and vanilla's items keep their names. Vanilla's iron is a bloomery product -- smelted below iron's melting point, hammered, never poured -- which is wrought iron: soft (hardness 6), tough, low in carbon, cold-workable, the generic structural metal. *Cast iron* (`material/cast_iron.json`) is the high-carbon one, iron that has been fully melted with coal: it pours well and is hard and stiff under compression, and it is brittle under a blow, so it is not forged. Today its only job is the shorter road to steel (§4.5). Its real niche is still to come: **cookware and castings** -- pots, pans, machine beds and frames, anything poured once to shape and loaded in compression rather than struck. *Steel* sits between: carbon enough to harden on a quench (§5.3), little enough to forge hot. The trap worth naming: calling the soft metal plain "iron" invites a player to expect cast iron's hardness from it, or wrought iron's toughness from a cast pot.

### 5.4 Generated items

Which forms are generated is declared, not inferred: `data/feedback/form_type/<form>.json` names the `kinds` a form is generated for (every one today is `["metal"]`), and `block: true` makes it a placed storage block rather than an item. A material gets every form type whose kinds include its own, unless it lists an explicit item for it (`forms`) or excludes it (`exclude`). Absence is the stated thing, so a new form type reaches every metal without editing any of them; the cost is that a metal with a nonsense form (hardened steel plate, cast iron wire) has to say so.

**Any other form name is an association.** A material's `forms` may name anything made of it under any name no form type declares -- iron's `horse_armor`, sculk's `sensor`, sixteen `white`...`black` for wool. Those generate nothing; they exist so `Material.find` can say what an item is made of (the Material Probe uses it). One item per name, and one material per item.

`FormItems` reads the jar's own `material/` and `form_type/` files at registration because items exist before any datapack does, and registers `<ns>:<material>_<form>` as a `FormItem` (or a `FormBlock` and its item), named by pattern (`form.feedback.plate` = `%s Plate`, `form.feedback.block` = `Block of %s`) so a new item costs only its material's name. A form with no registered item is skipped at compile, never an error. Item models come from `FormItemModels` (every one points at its form's grey sprite); block states, block loot, the pickaxe tag and the nine-to-one nugget/ingot/block recipes from `FormData` (`runData`). Lang stays hand-authored because a generated `en_us.json` would shadow it at the same path.

**The first non-metal form type** is `grip` (2026-10-14): `["metal", "organic"]`, with seven organics excluding it. Seven excludes against three real grips is the cost of "absence is the stated thing" when a kind is mostly nonsense for a form; if a second organic form arrives with the same ratio, an opt-in list on the form type is the cheaper statement.

**Trap:** generation makes dead ends. Copper swarf exists and has no remelt in `thermal_process/`; steel foil exists and nothing makes use of it. Generating is free, giving each a purpose is not -- that is document 3's job.

### 5.5 Debts

- **[OPEN]** Single-material steps (`wire_to_fine_wire`, `plate_to_foil`, `foil_to_scrap`, every removal) have a `base`/`slope` that reproduces copper's or steel's old number, not a fit. They are free to move; any second material that gains the forms is the first real test of the pair.
- Explicit per-recipe overrides were deliberately not kept. If a pair genuinely cannot be expressed as a function of hardness, restore a table rather than bend the function.

### 5.6 The property table, ahead of its readers

Every vanilla substance and Feedback's own metals (tin, lead, zinc, bronze, brass, cast iron) have a material file now -- metals, crystals, stones, ceramics, granular stuff, organics, and §12's exotics -- each with the figures below wherever it has one. **Nothing reads them yet** except `density` (engine, plate and tube mass) and `yield_strength` (engine rev limit). They were entered in one pass so the next system that needs a figure finds it already there for sixty materials, rather than adding one field to three files and leaving the rest blank. That is the `Instrument` bet again: high fixed cost once, cheap per feature after.

Numbers are placeholders (philosophy §19). The method: real-world values where reality has one, rounded, with Tu read as degrees Celsius -- tin, lead and zinc already melt at their real figures, and lava at 1200 Tu is stone's melting point, so lava *is* molten stone. Where an existing table disagrees with reality (copper melts at 1000 Tu, gold at 950, iron at 1600), the table wins and the material agrees with it. Absent means unmeasured, never zero.

**Qualitative:** `kind` -- one of metal, mineral, crystal, ceramic, glass, granular, organic, exotic -- is the adjective a player has before owning an instrument (philosophy §8). `traits` are yes/no observations a person could make by handling it: alloy, flammable, fireproof, transparent, luminous, fibrous, porous, sticky. Neither is identity (§11): two materials can share both.

| Group | Field | Unit | Waiting for |
| --- | --- | --- | --- |
| `thermal` | `melting_point` | Tu | one figure in place of each melt recipe restating it |
| | `boiling_point` | Tu | distillation (zinc out of brass) |
| | `cooling_rate` | Tu/t | per-material `ItemHeat` cooling -- §2.2's open item; blaze rod is 0.01 |
| | `conductivity` | Conductance (one-block wall; copper = the heat exchanger's 0.1) | vessel walls, exchangers, engine cooling |
| | `oxidation_temperature` | Tu | scale loss on hot metal, burning for organics -- an overrun hook |
| | `anneal_temperature` | Tu | resetting work hardening |
| | `service_temperature` | Tu | a machine body of it softening and wearing when run too hot |
| `mechanical` | `toughness` | St | one blow above it cracks the piece -- hardened steel's 8 sits between a crank's 3 and 12 |
| | `ductility` | cold steps | over-drawn wire cracking: overrun on the finished product |
| | `work_hardening` | St per cold step | work hot, or work cold and anneal -- two answers |
| | `yield_strength` | St | frames, shafts, engine limits |
| | `friction` | coefficient | brakes, clutches, bearings, tires; ice and blue ice already differ |
| | `abrasiveness` | 0-1 | grinding wheel grit wear |
| | `hardenable` | bool | whether a quench hardens it (steel, and glass -- tempered glass) |
| `molten` | `viscosity` | × water | mold misruns |
| | `density` | Mu/mB | slag floating off a melt |
| `separation` | `magnetic` | bool | the magnetic drum |
| | `curie_temperature` | Tu | hot iron passes the drum |
| | `reflectivity` | 0-1 | the optical sorter, fooled by lookalikes on purpose |
| top level | `ampacity` | × copper | wire melting under load -- a ratio because resistance is not modelled |
| | `corrosion_resistance` | 0-1 | corrosion from water or acid -- misuse only, never time |
| | `color` | `"#rrggbb"` | the tint of its parts, forms and cables -- art, but a pack changes it in the material's file. Absent: the client averages the texture of its ingot or block item (`textures/item/<path>.png`, else `textures/block/`), else the kind's grey. Server never draws it |
| | `composition` | material to share | read: what an alloy dissolves into in the electrolytic cell (§6.14) |

**Netherite is magnetic, and keeps it hot.** Vanilla makes a lodestone out of it, and §2 says what vanilla does is evidence. A Curie point of 1500 Tu against iron's 770 is the exploitable difference: a hot drum separates them.

- **[OPEN]** `viscosity` is recorded although philosophy §3 names it as not yet earning its place. It costs nothing unread; it earns a reader only if a misrun is a decision.
- **[OPEN]** Specific heat was **not** added. TFC's per-material heat capacity was declined once (`TODO.md`), and it would need a new unit (Work per Tu per Mu).
- **[OPEN]** Elastic modulus and fatigue were considered and left out: modulus fails §3's test, and fatigue is wear from uptime, which the wear rule forbids.
- **`parent`.** A material file may name another (`"parent": "steel"`) and write only what differs. Objects merge key by key down the chain; a number or list replaces the parent's whole value (so `exclude` is restated, not appended); `null` removes an inherited key (tempered steel drops steel's hot-working window and `hardenable` that way). `MaterialTable.resolve`.
- Water and lava have no material file: `hardness` is required and a liquid has none. Lava is a heat source already; if a liquid needs figures, make hardness optional first. A *bucket* of lava is a form of stone (§5.7), the way a powder snow bucket is a form of snow.
- Every metal, vanilla and Feedback's own, now grows the full form set; nothing gives most of them a purpose yet (the §5.4 trap, at scale). Art is one greyscale sprite per form (`textures/item/form/<form>.png`, `textures/block/form/block.png`) tinted per material by `client/FormTint` from `MaterialColors` -- generated forms, storage blocks and the feedback-namespace items a material's `forms` names. Vanilla's own items (iron, copper and gold ingots) keep vanilla's sprite. A new form type needs a sprite and an entry in `FormTint.SPRITE_FORMS`; a new material needs only a `MaterialColors` entry (unlisted ones take their kind's colour).

### 5.7 Combustion: one block, two fires

**Built 2026-10-20.** Fuel was a datapack table of its own, `fuel/*.json`, rows of `{ingredient, duration, temperature, spread}`; gun propellant was a `propellant` block in the material file. Both are gone into one `combustion` block in `material/*.json`, and an item burns because its material does: item, to its material by form (`Material.find`, the existing route), to that material's combustion. Nothing names an item.

**Why the split existed.** The fuel table came first (slice 1), before the material table did, and an ingredient row was the obvious shape -- TFC's, and the one every other table already had. Propellant came after materials, so it went where materials were. Nobody decided they were different things; they were written in different months. The cost was real: an ingredient row is an identity lookup wearing a datapack (philosophy 11), a modded coal had to be registered twice to burn in a firebox and in a case, and "what is coal" had two answers.

**The shape.**

```
"combustion": {
  "flame_temperature": 1180,   // open fire: a hard ceiling on what it heats
  "burn_ticks": 2400,          // one piece of it in a fire
  "spread": 0.1,               // flame noise, rolled once per piece (philosophy 8)
  "oxygen_balance": 0.5,       // oxygen beyond its own fuel's needs
  "chamber": { "peak_pressure": 3, "burn_rate": 4.25 }   // confined in a case
}
```

A material with `flame_temperature` and `burn_ticks` is a fuel; one with a `chamber` is a propellant; one can be both. A firebox and a self-fired vessel read the open half (`process/Fuel`, now a resolved figure rather than a table row); a gun reads the confined half (`Material.propellant()`, a view the gun code already read, so nothing in `weapon/` changed). The ignition point is in neither half: it is `thermal.oxidation_temperature`, which the table already stated as *above this, held in air, it burns*. A cook-off and a fire catching are the same temperature.

**A piece, and how much stuff a form is.** `burn_ticks` is per piece of the base form; `Fuel.pieces` scales the rest by their crafting yields: a block is nine (a coal block is still 21600 t), a plank a quarter of a log, a stick half a plank, and an ore is mostly rock and does not burn as itself. Before this, a log made four planks and twice its own burn, and anything made of wood would have burned as a whole log.

**Lava is not combustion.** It burns nothing and runs out of nothing: it is a body at a fixed temperature giving up latent heat as it freezes. So a lava bucket is stone's `molten_bucket` form, and stone's `molten.freeze_ticks` (20000) says how long a bucket holds stone's `melting_point` (1200 Tu -- §5.6 already says lava is molten stone) in a fire, with no spread, because a phase change is not noisy. Giving it a flame temperature would have said lava is a fuel that happens to be orange, and would have let a pack give it an oxygen balance. Lava in the world is unchanged (`HeatSource`, `FTuning.LAVA_TU`).

**Redstone is not combustion either.** A coil gun's charge is a field pulling a slug, so it has its own `coil` block (`field_pressure`, `quench_temperature`). `Material.propellant()` presents both as the one view a gun reads, which is the only place they meet.

**Numbers that changed.** Coal, coal block, charcoal, blaze rod, logs and lava bucket are exactly as before, and so is every gun figure.
- Planks: 300 t at 700 Tu, now 150 t at 760 Tu (a quarter log, and wood has one flame).
- Blaze powder's oxidation temperature: 200, now 220 Tu, so its cook-off stays its old ignition figure. Gunpowder's two already agreed (300); nothing else read blaze powder's.
- Newly burn as wood, at their crafting share: stripped logs, wood and stripped wood blocks (now wood's forms; before, they burned through the `logs` tag), sticks, and wooden tools, bows, crossbows and shields.
- No longer burn: crimson and warped stems and planks (nether wood is not flammable, as in vanilla), bamboo planks, and modded logs and planks that only the `logs`/`planks` tags reached.

**[OPEN]**
- **Compat regression.** A modded log burned through `#minecraft:logs`; it now burns only if a material lists it. The fix is general: let a material's forms name a tag, so `Material.find` -- not only fuel -- reaches modded items. Not done here.
- `burn_ticks` and a chamber's `burn_rate` are the same property (how fast it burns) in two confinements. Deriving one from the other needs a constant tying a firebox's ticks to a barrel's metres, and no material has both figures today, so there was nothing to fit. When one does (blaze powder below), try it.
- §2.3 wants blaze *powder* to be the extreme fuel and the rod a heat store that never sees a firebox. The table has always burned the rod (1400 Tu, 2400 t), and this migration keeps it. Moving that `combustion` from `blaze_rod.json` to `blaze_powder.json`, with a short burn and a wide spread, is now a data edit -- and makes the powder a fuel and a propellant from one block.
- `oxygen_balance` is read by a case only. A firebox could read it: a fuel short of oxygen needing more bellows is the same fact.
- Bamboo has no combustion: its stalk is the base piece and its planks are not a quarter of one, so it needs its own yields.

### 5.8 Asking about stuff in a recipe: the material ingredient

**Built 2026-10-20.** The recipe system can name an item, a tag or a component, and none of those can say "any wire that carries at least this much". `feedback:material` (`process/MaterialIngredient`, a NeoForge `ICustomIngredient`) is that question:

```
{ "type": "feedback:material", "form": "wire", "property": "ampacity", "min": 0.5 }
```

`form` (the item is that form of some material) or `tag` (each item of the tag must be some material's) names the stuff; `property` with `min` and/or `max` bounds a figure from the material's file. `MaterialProperty` is the closed list of figures a recipe may ask about (ampacity, hardness, density, melting point, toughness, yield strength): each is the accessor the machines already use, and absent means unmeasured, so a material without the figure matches no range. `getItems()` enumerates the matching stacks from the table so the recipe book and JEI cycle through them; `describe()` is the sentence a JEI slot tooltip says. It reads the server's table where one is loaded and the client's copy otherwise. The client's copy is in place before its recipes because `PlayerList` posts `OnDatapackSyncEvent`, which sends the material table, just ahead of the recipe packet, on login and on `/reload`; `Ingredient` caches `getItems()`, so the other order would leave a client's ingredients empty for good.

**Trap:** a recipe cannot copy an ingredient's material into its output. Anything that must (the cable) is a `CustomRecipe` using the ingredient's `test` and `Material.find`, as `TongsRecipe` does.

---

## 6. Electricity (`Eu`)

**Built in part (slice 4, 2026-10-12): see §6.13 for exactly what.** Written speculatively, ahead of code, like §1; where §6.13 and a paragraph above disagree, §6.13 describes the code. Everything stated flatly is a **user ruling of 2026-10-12** and is recorded as decided. Everything marked `[OPEN]` is not. `feedback_philosophy.md` §10 (energy is plural) and §17 (`Eu` is electrical, and it is not a scalar) are the authority; this section is how those are implemented. Prior art is GregTech CEu Modern's electricity (LGPL-3.0, read for design only; see §6.9 and `THIRD-PARTY-LICENSES.md`).

### 6.1 What `Eu` is

`Eu` is a **voltage and a current**, two components, no arithmetic on the type (the `core/unit/` rule: the plumbing is typed, the physics unwraps inline). Two `Eu` with the same product are not interchangeable: a cell and a stepped-up line can carry the same power and be useless to each other's loads.

- **DC only. Frequency is not modelled.** Tentative. If AC is ever wanted (a piezo resonator, an oscillator crystal in roadmap Slice 8) frequency is a **third component**. The type widens by one record field; the datapack form stays compatible if a missing frequency reads as "DC", the way a missing optional field already reads as absent elsewhere (§5). Nothing in this section should assume exactly two components outside the `Eu` record itself.
- **Resistance is still not modelled** (§17, "what is deliberately absent"). §6.4's per-block loss gets the decision resistance would give without the variable, which is how it passes the "does this create a meaningful choice" test.
- **A cost in a process is a threshold plus a rate, never a bare `EU/t`.** A process that needs electricity says *"at least this voltage; this much current is how fast it goes"* (electrolysis: a decomposition potential, then current sets throughput). There is no `Eu`-denominated recipe cost. See §6.8.

**Decided (2026-10-12): the `Eu` record's components are `voltage` and `current`.** `voltage` is a **band** (§6.3), `current` is an **integer count of amps**, like `Fu`. §6.4's drop-per-block arithmetic works on the integer amps and on the band's own reference voltage, so the type needs no float. Lang symbols, as built: unit `Eu` (`3 Eu at LV`), plain band names; `raw()` is the current and display goes through `Eu.figure()`. **[OPEN]** whether those are final.

### 6.2 Per-packet: any voltage may be on a wire, each sink checks

**Decided.** A wire does not have "a voltage". Sources put `(V, I)` on it; each sink looks at what arrives and checks it against its own rating. This is the model that makes a transformer a real object (a step between two different voltages on two runs, trading V for I at constant product) and makes "I wired a 12-volt thing to the line" a mistake the player can actually make.

The alternative, one voltage per connected run with a rotation-style ledger, was simpler and consistent with `Su`, and it was **not** chosen. It remains the fallback if the open items below turn out to be unsolvable without it.

The GTCEu study's objection to per-packet routing was real and is the thing this section has to answer: in GTCEu a far sink starved by nearer ones is **invisible and unconfigurable**, there is no answer to "which source fed this", and there is no brownout, only full voltage or nothing. Routing and resolution are decided below ("Routing and resolution"). The constraint on them, which they were chosen to meet: **every failure of supply must show on the thing that failed.**

#### How failure stays visible

- **Every sink carries a reading.** An electrical consumer is a `DataNode` reporting voltage seen, current delivered, and current wanted. A starved sink is a sink whose delivered current is below its wanted current, and the galvanometer (§6.7) or any tooltip reads exactly that. Nothing is hidden in an ordering the player cannot see.
- **Under-supply is slower, not off** (§6.5), so a starved machine is *visibly* slow. The symptom is on the machine and the cause is on the wire.
- **Brownout is distance.** Delivered voltage falls by the conductor's drop per block per amp (§6.4), so a long, thin or overloaded run reads as a gradual sag at the far end, never a cliff. This is the brownout GTCEu's all-or-nothing delivery lacks.
- **Over-voltage is evidence.** Condition loss and heat on the machine (§6.5) are the symptom, in the same currency every other overrun uses.
- **A source shows what it is being asked for.** Its node reports offered and drawn current. A source that idles shows it is idling; a source asked for more than its rating shows that too (source sag, below).

#### Routing and resolution

**Decided (2026-10-12), on performance grounds.** Per-packet routing evaluated per tick per wire block per sink would be the expensive way to do this, so the work is moved off the tick:

- **A run-level resolver**, in the `RotationPropagator` pattern: when topology changes (a wire placed or broken, a source or sink added or removed), rebuild the connected run once by flood fill and **cache a sink list per run**, with each sink's path drop (§6.4). The cache is invalidated only by a topology change; per tick the resolver reads the cache and does arithmetic, no search.
- **Sinks take a proportional share** of what the run's sources supply. Each sink's share is its wanted current over the total wanted current; when supply covers demand every sink gets what it wants, and when it does not, every shortfall is shared and readable on every sink (no nearest-first starvation).
- **The per-packet voltage model stays.** Sharing divides *current*; each sink still checks the voltage *it sees* (offered voltage less its path drop) against its own rating (§6.5). A transformer is still a real object.
- *Fallback, kept:* one voltage per connected run with a rotation-style ledger, if the sub-questions below prove unsolvable. Not needed so far.

**Mixed-voltage runs.** Best resolution: **sources of different voltage on one run do not blend.** A sink sees the highest source voltage present on its run (less path drop); a lower-voltage source on that run is *blocked* (it supplies nothing to that sink and its node reports "blocked", so the mistake shows on the thing that is wrong). Nothing is back-fed into a lower source. This keeps "I put a cell on the line" a visible mistake and matches how the per-packet model already treats a mismatch as a consequence, not a lock.

**Source sag under over-draw.** Best resolution: a source drawn past its rated current **sags in voltage** (a non-destructive overload character, as a real cell does through internal resistance) and reports it on its own node; downstream sinks then see the lower voltage and slow (§6.5). A source is never destroyed by over-draw.

**[OPEN]** The shape of the sag curve (how fast voltage falls per amp over rating) and whether a source's current cap is hard or is only the sag itself. **[OPEN]** Whether a blocked lower-voltage source should instead be pulled to the shared voltage when no higher source is actually delivering to that sink. **[OPEN]** How many amps one sink may take from one wire at once.

### 6.3 Voltage bands

**Decided (2026-10-12): four named bands, `LV`, `MV`, `HV`, `UHV`, a closed list.** A small, geometrically spaced set, never a free scalar (§3). Making the list data-extensible is deferred, not refused. **Voltage belongs to the source**, and **bands gate nothing by themselves**: a band is a rating a source and a conductor's insulation have, not a machine "tier" that doubles as a progression gate (§6.9). "Needs band N" is exactly the hard-gate-by-tier shape `CLAUDE.md` rules out; mismatch is a consequence (§6.5), not a lock.

**Considered and replaced:** a physical-place naming proposal (*Cell*, *Bench*, *Line*, *Arc*) anchored to a galvanic cell, a shaft dynamo, stepped-up transmission, and a spark gap. It was trying to keep the names from drifting into tier labels and to make the point that voltage and current are separate axes (an electrostatic generator is the top band by voltage and nearly nothing by current, which is the whole reason `Eu` has two components). The user chose plain band names instead; the second point still stands.

**Physical anchors, partly fallen out (2026-10-13).** A chemical cell is LV (zinc against copper is 1.1 V). A dynamo has no band of its own: it reaches whatever its shaft speed times its winding makes, and the windings are placed so a steam engine at its working speed gives LV on coarse and MV on fine. A hand crank is faster than an engine, so a cranked fine dynamo reaches past MV: speed, not source type, sets the voltage. **[OPEN]** what the roster has to occupy at HV and UHV. **[OPEN]** Cells in series add voltage one-to-one, which is a free scalar again (1 to N cells). Either the stack is capped to named bands or cells come as pre-built packs per band. Not decided.

### 6.4 Conductors

A conductor is a **material times a form**, like everything else in §5. Three figures per conductor, from `material/*.json` (§5.6's table already carries `ampacity`; the figures below extend it):

- **Current rating**: how many amps the conductor carries before it heats. Today's `ampacity` field, a ratio to copper, is the existing form of this.
- **Voltage rating**: how much an insulation layer withstands, per band.
- **Loss: a voltage drop per block per amp. Decided: a material property in `material/*.json`.** A path's drop is the sum over its blocks of `drop × current`. A long run at low voltage delivers little or nothing; the same power at higher voltage carries a smaller current and loses less. That is the whole reason the voltage lever exists, and it is obtained with no resistance variable: nobody can tell it is not `I²R`, and it plays right. The *shape* is GTCEu's. A path whose summed drop reaches the offered voltage delivers zero, which is a legible failure ("the wire is too long for this voltage"), not a hidden one.

**A wire is a `ThermalBody`.** Current over its rating is a heat input; the existing `Heat`/`Conductance` model is the whole mechanism and no new simulator is added. A wire inside its rating never heats, so the wire never wears from uptime. This is the rule the machine shop already runs on, applied to cable.

**Failure is staged and every stage is readable:**

1. Insulation fails first. It is a separate physical layer (a component you can point at) that burns off above its limit. A bare conductor still conducts; it also now **shorts to its neighbours and shocks**, which is the new consequence.
2. The conductor degrades under continued overload: condition falls, rating and drop worsen with it.
3. **Left long enough, the conductor is destroyed.** This is the user's ruling: *destruction is the consequence of not fixing degradation.* It is never instant and never a surprise; it is the end of a visible path the player was walking.

**Decided (2026-10-12): destruction exists for conductors and for nothing else.** `CLAUDE.md`'s wear rule says a machine "degrades and never breaks, because a machine refusing to run is a hard gate where no physical impossibility exists." Step 3 is a wire breaking, and the user has ruled that it stands: the rule is scoped to **machines** and stays exactly as written for them (§6.5: a machine is never destroyed, whatever you feed it), and **conductors are the one place destruction exists**. The reasoning, in the user's words: *destruction is a consequence of not fixing degradation.* A conductor is reached only through misuse (over-rating), never uptime, and never instantly; the rule's reason (do not turn a graded consequence into a cliff) is kept because the cliff has two readable steps in front of it. `CLAUDE.md` is updated to say so. **Widened 2026-10-14 (user ruling):** pressure holders are destructible too -- see §7.3. The rule's reason survives: nothing is destroyed by a small mistake or by uptime, and a boiler weeps scalding steam before it lets go.

**Built (2026-10-14): both protective parts** (`machine/breaker/`). An open one is a member that *does not conduct*: `ElectricMember.conducting()`, read by the propagator's `connects`, so an open breaker or a blown fuse sits in a run of its own and the line through it is two runs; a change calls `ElectricPropagator.reconnect`, which rebuilds it and its neighbours.
- **Breaker**: a wire while closed, `Switchable`, so a hand or a controller opens it. Opening while current flows costs condition per amp broken (the arc), and pitted contacts lose more volts per amp, so a controller flapping it under load with no deadband shows on the far side as a sag (philosophy 13). It trips itself above a copper cable's rating, which is an opening under load and charged like one. Never broken; it keeps switching at its floor.
- **Fuse**: a holder for one fine wire of any metal, rewired by hand. Both its figures are the wire's metal: the rating is its `ampacity` times `FUSE_AMPS_PER_AMPACITY`, and over it the wire warms as a cable does (ten times as fast, a thin wire) until it passes its metal's `melting_point` and is gone. Tin blows low and fast, copper high and slow. The holder never wears; the wire is spent.

**Protective parts.** A **fuse** is allowed as a consumable: a sacrificial element that opens on over-current and is spent doing so. A reusable **breaker** also exists and **arcs and wears under switching load** (philosophy §10: electrical stops nearly instantly, and the hardware wears cutting load). The breaker's wear is wear from a specific act and already fits the wear rule. **The fuse is the third accepted wear-with-correct-use exception (user ruling 2026-10-12)**, alongside the Grinding Wheel's grit and the Hand Hammer's durability: a fuse is a consumable by nature, sacrificed to protect what it guards. A reusable breaker is **not** an exception: it wears from a specific act, switching under load, like any misuse. The exception is **narrow**, the same shape as the other two, and does not extend to machine bodies. It is recorded in `CLAUDE.md`.

### 6.5 Sinks: under-voltage, over-voltage

**Under-voltage slows; it does not stop.** For a general machine, delivered voltage below its rating reduces its rate, so a long run or a weak source makes a machine slow rather than dead. This is "the recipe is not locked, the process is difficult" applied to electricity. **The exception is a genuine physical threshold**: an electrolytic cell below its decomposition potential does nothing, because no amount of current at too low a potential decomposes the electrolyte; an arc will not strike below its breakdown; a magnet saturates. Those are hard gates in the §7 sense, and only those.

**Decided (2026-10-12): the rate floor is zero.** Under-voltage can slow a machine to a standstill, never wear it; slow is the whole consequence. **[OPEN]** The shape of the slow-down itself: linear in delivered voltage, in current, or in their product.

**Over-voltage goes into the machine.** Voltage in excess of a sink's rating is force that cannot go into the work, and the wear rule says where such force goes: **condition loss, plus heat into the machine's own `ThermalBody`, scaled by the excess and by the duration.** The machine is **never destroyed and never explodes**. A worn machine is evidence of a specific wiring mistake, which is exactly what the wear rule is for.

**Decided (2026-10-12): a machine is never allowed to refuse a line.** It always runs, and it always wears. Refusal would be a hard gate where no physical impossibility exists, which the wear rule is written to avoid; the consequence of the wrong wiring is wear, with no lock. **[OPEN]** The per-unit scale (condition per volt-over per second, heat per the same).

### 6.6 Storage

**Decided: for now only a Leyden-jar-style capacitor and a chemical cell.** There is **no generic energy-bank block**: an unbounded bank is the universal jar, "energy in a box with a number on it", which is the smell §6.9 describes.

- **Capacitor** (Leyden jar): holds charge at a voltage and gives it back in a *burst*; it leaks. Its character is smoothing and bursting, not holding. It is also the storage that makes a pulse source (electrostatic machine, lightning capture) usable at all, which is the pleasing parallel philosophy §10 already draws between real electrical infrastructure and buffering.
- **Chemical cell**: stores chemically and leaks chemically. It is also the continuous source in Slice 4; the same block is both, as a real cell is.

Machines hold **no** hidden stored-`Eu` buffer. A sink that needs smoothing is a sink with a capacitor *in front of it, as a block the player placed*.

### 6.7 Sensing

**Decided.** A wire carries a `DataNode` reading `(V, I)` at that block. The **galvanometer is a fitting on the wire** (`fitting/`: a `Sensor`, sided, read-only), not a separate network; it follows the `TemperatureSensorFitting` precedent exactly. The same reading is what a sink's status (§6.2) reports. An electroscope reads potential only and coarsely (no current), as a hand-carried `Instrument`: it can tell you a wire is live and roughly how hard, not how much is flowing. Per §8, none of them enter the calculation; a bad reading costs reproducibility, never success.

### 6.8 Where `Eu` enters and leaves

**Decided: one physical block per boundary.** Electricity crosses into or out of another energy system at one block you could point at, and it crosses at a *local* physical relationship, never through a global exchange rate.

| Boundary | Block | Direction |
| --- | --- | --- |
| Chemical to `Eu` | Chemical cell | in |
| Pulse source | Electrostatic generator; lightning capture | in |
| Mechanical to `Eu` | Dynamo (shaft in) | in; later slice |
| `Eu` to Mechanical | Motor (a block; an `Adapter` form may follow) | out; built (§6.13) |
| `Eu` to Thermal | Heating coil (a block under a vessel) | out; built (§6.16) |
| `Eu` to field | Electromagnet | out |
| `Eu` to field, carried | Charger (a jar round, fired by a coil gun) | out |
| `Eu` to Chemical | Electrolytic cell | out; built (§6.14) |

How a boundary block's figures are set without a constant. A dynamo's output voltage and current have to come from its own construction and the shaft it is on (the speed and torque it sees), and a motor's torque from the current it is delivered, in each case as that block's physics and not a table that says "N `Su` per `Eu`". This is the thing to resist: a fixed exchange constant between `Eu` and anything is the universal currency arriving by the side door.

**Decided (2026-10-13) for the dynamo: speed makes volts, current costs torque, by one winding figure.** A real generator's back-EMF constant and torque constant are the same number (`k_e = k_t`), so the dynamo has one figure per winding, `voltsPerRpm`: its voltage is the input shaft's live RPM times it, and every amp drawn costs that shaft the same figure in `Su`. That is conservation stated once inside one block, not an exchange rate: a dynamo nobody draws from costs the shaft nothing, a slow river makes a low voltage, and gearing up is how to raise it (paying gearing's friction). The winding is the discrete choice (§3): **coarse** (few volts per RPM, many amps) or **fine** (the reverse), at the same power, so neither dominates. The trap worth recording: the obvious first version is "a dynamo makes X `Eu` per `Su`", which reads as harmless bookkeeping and is exactly the side-door currency above. The motor (`Eu` out to a shaft) is the same winding figure run backwards (§6.13).

**Decided (2026-10-14): voltage is converted by a motor-generator set, not a transformer.** `Eu` is DC (§6.1) and a real transformer needs AC, so a block that trades volts for amps at constant product would be invented physics. The DC era's own answer was a motor driving a shaft that drives a dynamo on the other winding, and here that is no new physics at all: the motor (owed, the dynamo reversed), a shaft, and the dynamo that exists, with the losses being the shaft's friction and gearing's that already exist, and the winding the discrete choice it already is. It is what lets a high-current, low-voltage load (§6.14) sit far from its source: send at MV, step down beside the load. AC and a static transformer stay possible later (frequency as the third `Eu` component); the motor-generator set would keep its niche (needs no AC). §6.9's "a transformer as a discrete step" is superseded by this for DC.

### 6.9 Prior art: GregTech CEu Modern

Read for design; LGPL-3.0, cleared; the study is recorded in `THIRD-PARTY-LICENSES.md`'s GregTech section. Written from scratch.

**Taken as design:** voltage as a small closed set of named, geometrically spaced steps; energy as a `(voltage, current)` pair rather than a scalar; loss as a per-block voltage drop per amp, which makes distance a cost and voltage a lever without a resistance variable; a conductor with its own temperature and an excess-load heat input; insulation as a separate layer that fails before the conductor; a transformer as a discrete step with V and I trading at constant product (later slice, and not free: it is a `ThermalBody` that wears under overcurrent); caching the route until topology changes.

**Not taken, and why it matters:**

- **Tier as the progression ladder.** Fifteen tiers, each machine and recipe keyed to one, makes the tier count the content roadmap and "needs tier N" everywhere. `CLAUDE.md`: "If a proposal needs 'requires tier N,' it's wrong." A band here is a rating, and there are about four.
- **Electricity as the universal currency.** In GTCEu every recipe is denominated in `EU/t`, steam is an EU payment method at a fixed mB per EU, generators output a bare EU from fuel, overclocking is keyed to tier, and a converter bridges the rest of the ecosystem. Feedback refuses all five. Philosophy §10 is the reason: Mechanical, Thermal and Chemical are natively consumable currencies that never *have* to become electricity, and electricity is the dominant *transport*, not the universal *consumption*. Concretely: no `Eu` capability on machines, no mB-per-`Eu` constant, no generator that emits bare `Eu` from a fuel, no overclock-by-tier. This is also what makes problems here have more than one answer (§5): a process can be powered from the energy that suits it.
- **Explosion on over-voltage** (§6.10).
- **A buffer in every machine.** See §6.10.
- **Push routing in nearest-first order** as the *model*. Per-packet voltage is kept; nearest-first starvation and its invisibility are not (§6.2).
- **Item batteries as the charging currency for tools.** Feedback's tools are mechanical and thermal.

### 6.10 What was tempting, and wrong

- **Explosion on over-voltage.** It is unmistakable and it teaches "match your voltage", which is why it is tempting. It is wrong because it is instant, total and carries no information except "you plugged it in wrong", destroys a machine and its neighbours for a plumbing mistake, and has one correct answer (use a transformer). It also breaks "degrades, never breaks" for a machine. The replacement is wear whose size says *how* wrong and *for how long*, which is the same lesson with a gradient.
- **A hidden buffer in every machine.** It decouples tick order and removes the need to think about smoothing, which is why GTCEu does it. It is wrong because each buffer is a stored-energy number the player cannot see or place, and the sum of them is a universal currency in a jar. Smoothing is a block (§6.6) or it does not exist.
- **`EU`-denominated recipes.** The recipe says how much `EU/t` it costs and the machine is a function of tier. It is tempting because it makes every process comparable and balanceable on one axis. It is wrong because that comparability is the currency; a process here asks for a *condition* (a voltage over a threshold) and a *rate* (current), and what powers it is a local fact (§6.8).
- **Treating "insufficient voltage" as always a hard gate.** It is a gate only where physics has a threshold (§6.5). Everywhere else it is a slow machine, which keeps precision a soft gate.
- **One voltage per run, for simplicity.** Not wrong, and kept as the fallback (§6.2); recorded because it is the choice that would have made routing trivial and a transformer meaningless.

### 6.11 Minimal scope, and what is deferred

**Slice 4** (roadmap, "Electricity Exists") is novelty-scale: the player runs electrical experiments without a grid. It needs:

- the `Eu` record in `core/unit/`, with codecs and the lang key per the `Units` checklist, no arithmetic;
- one **cell source** (voltage from its material pair, current capped small) and one **electrostatic pulse source** (high voltage, bursts);
- one **wire block**: material-derived current rating, voltage rating and drop per block, from `material/*.json`; no insulation layer yet if the roster has only bare copper;
- one **resolver**, in the rotation-propagator style with a cached sink list per run and proportional shares (§6.2);
- the **galvanometer** (a fitting on the wire) and the **electroscope** (hand-carried);
- the **electromagnet**, as the first consumer.

The single failure at this stage is over-rating warming the wire's `ThermalBody`.

**Deferred:** transformers, the capacitor and storage in general beyond the cell, the insulation layer and stripping, multi-conductor bundles, any generator fed by steam or a shaft (Slice 6 onward), electroplating (Slice 5; the electrolytic cell is built, §6.14), overclocking of any kind (not taken at all), and circuits and data electronics.

### 6.12 Open questions and debts

- **Resolved 2026-10-12:** component names (`voltage` a band, `current` integer amps, §6.1); routing as run-level resolver with proportional shares (§6.2); four closed bands `LV`/`MV`/`HV`/`UHV` (§6.3); rate floor zero (§6.5); a machine never refuses a line (§6.5); wire destruction scoped to conductors and the fuse as third exception (§6.4, in `CLAUDE.md`).
- **[OPEN]** Whether the built lang symbols (`Eu`, plain band names) are final. §6.1.
- **[OPEN]** Sag curve shape and source current cap; blocked-source handling; amps per sink per wire. §6.2.
- **[OPEN]** Band anchors at HV and UHV (cell and dynamo settled 2026-10-13); series-stack capping. §6.3.
- **[OPEN]** Shape of the under-voltage slow-down. §6.5.
- **[OPEN]** Over-voltage per-unit scale. §6.5.
- **Resolved 2026-10-13** for the dynamo: one winding figure, volts per RPM and Su per amp (§6.8). Motor built 2026-10-14 as that figure reversed (§6.13); the coil 2026-10-14 by Joule's law (§6.16).

### 6.13 Build status

**Built: the conductor and the resolver.** `core/electric/` follows `core/rotation/`: `ElectricPropagator` floods a run when a node is placed, broken or loaded, and hands `ElectricRun` a member list and an adjacency list; `ElectricRun` builds a routing tree from the supplying sources and caches it, and each tick is arithmetic over arrays. The tree is rebuilt on a topology change and also when the *pool* of supplying sources changes under a standing topology (a pulse starting, a source being outbid), detected by comparing which sources are pooled.

What a tick does, as built: the highest nominal voltage among sources offering anything sets the run's voltage and those sources form the pool; lower-voltage sources are blocked and report it (§6.2 as written). Reachable sinks ask for current; if the pool, up to `SOURCE_OVERDRAW_LIMIT` times its rating, cannot cover it every sink gets the same fraction. The pool sags as it is drawn past its rating, linearly to a floor and never to zero (`ElectricRules.sagFactor`). Current accumulates from the sinks back up the tree and each wire block takes `dropPerAmp x current` off the voltage through it; a path that drops the lot delivers zero volts, never a negative one. **Current is routed along each sink's shortest path to the pool**, which approximates how parallel supplies share a wire and is chosen for being deterministic and loop-free.

**Built: the wire.** `machine/cable/`. A `ThermalBody`: nothing is put in at or below the rating, so a wire inside its rating never leaves ambient; above it, `FTuning.WIRE_OVERLOAD_WORK_PER_AMP` per amp over goes in through `Heat.warm` and the leak is `Heat.tick`'s own. It settles where input equals leak and cannot run away. Insulation burns off above `WIRE_INSULATION_LIMIT_TU`, then the conductor degrades above its material's `service_temperature` (rating falls, drop rises), then it is destroyed at zero condition. A bare conductor under load shocks what touches it. Figures come from the material: rating from `ampacity` x `WIRE_AMPS_PER_AMPACITY`, drop from the new `electrical.drop`. **One `cable` block serves every metal** (2026-10-20): the material rides on the item as `MadeOf` and lives in the block entity, so a new conductor is a material file with an `ampacity`, not a registration line. It is `cable`, not `wire`, because `wire` is a form: `Material.find` reads an item carrying `MadeOf` as the form named by its id, and a placed cable read as a wire would be accepted by the recipe that makes cable. `CableRecipe` (three wires of one conductor over three paper) is code, not JSON, because a recipe cannot say "the output is made of whatever went in"; the tint, name and drop all follow the stack's material.

**Divergences from the text above.**
- A sink's current is `float` inside the resolver; the `Eu` type (band and whole amps) is the typed seam for display, not what the physics unwraps. A sink sees a continuous voltage after drop, which no band names.
- The sources' nominal voltage is plain volts, with a band derived from it (`ElectricRules.bandOf`), because a galvanic cell's voltage is the difference of two electrode potentials, which is not a band.
- No voltage rating on the wire yet (LV only), and no insulation *item*: insulation is a flag on the cable, which is the minimum that lets stage 1 of §6.4 exist.

**Built: two sources.** The *galvanic cell* (`machine/cell/`) is given an anode and a cathode material and holds the magnitude of the difference of their `electrical.electrode_potential` (zinc against copper is 1.1 V), at `GALVANIC_CELL_AMPS`; it never runs down (`[OPEN]`, `TODO.md`). The *electrostatic generator* (`machine/electrostatic/`) is a pulse: using it is one turn of the handle, which puts `ELECTROSTATIC_VOLTS` at `ELECTROSTATIC_AMPS` on the line for `ELECTROSTATIC_PULSE_TICKS` and then nothing. It holds no charge. While it pulses it outbids a lower-voltage cell on the same line, which is blocked for exactly that long. Neither is a conversion: nothing in the mechanical or thermal networks feeds either.

**Built: sensing.** Every `ElectricNode` is a `DataNode` and holds its own reading: voltage at the block, current through or delivered, current wanted (sinks), blocked (sources). The reading is server-side and is not synced to clients -- an instrument composes its figure where it is true. The **galvanometer** exists twice, as §6.7 says: a carried `GalvanometerItem` (current, to `GALVANOMETER_RESOLUTION_AMPS`, and on a sink what was wanted beside what was delivered) and a `CurrentSensorFitting` bolted to a face of a cable, a sided `SensorFitting` exactly as `TemperatureSensorFitting` is, which a controller card can be linked to. The **electroscope** is carried and reads potential only, coarsely. The Jade card says *live*, *dead*, *starved* or *outbid* for nothing and gives figures only with the matching instrument carried. `Quantity` gained `VOLTAGE` and `CURRENT`, appended.

**Built: the electromagnet** (`machine/electromagnet/`), the first sink. It wants `ELECTROMAGNET_AMPS` (one cell's worth) at LV. Its field is `ElectricRules.rate(volts seen, amps delivered, amps wanted, LV)` times its condition: the product of the fraction of rated voltage it sees and the fraction of wanted current it gets, so a long run and a starved supply both slow it and neither refuses it. Zero is a floor it can sit on indefinitely without wear. Voltage over its rating is wear and heat, scaled by the excess: condition falls to `ELECTROMAGNET_CONDITION_FLOOR` and `Heat.warm` puts Work into its `ThermalBody`, which settles under the same difference-driven leak as everything else. It is never destroyed. No band above LV is built, so over-voltage is reached only by a test calling `onResolved` with a high voltage. The magnet pulls dropped item entities whose material has `separation.magnetic` (iron, steel, cast iron, netherite) toward itself at a range that grows with field strength.

**The §6.5 slow-down is the product, provisionally.** Voltage fraction times current fraction was chosen because it makes both visible failures of §6.2 slow the machine; `ElectricRules.rate` is the one method that holds the choice.

**Built (2026-10-13): the dynamo** (`machine/dynamo/`), the mechanical-to-`Eu` boundary of §6.8. A source `ElectricNode` with an input face toward a shaft; volts are that shaft's live RPM times the winding's `voltsPerRpm`, rated amps are the winding's, and the current it delivered last tick times the same figure is charged to the shaft's run through `RotationNetwork.loadByBelt`, flat and unreferred like every machine's working draw. It is not a `RotationNode` (a block entity can extend one base), so it couples to the shaft the way a Pulley couples to its partner's run, and it does not spin on screen yet. Sneak-use with an empty hand switches coarse/fine.

**Built (2026-10-13): the Leyden jar** (`machine/capacitor/`), §6.6's capacitor. It keeps charge in amp-ticks (internal, no type, like `Work`) and holds charge over `LEYDEN_JAR_CAPACITANCE` volts. `ElectricMember` allows one role, so the jar changes role: a **sink** charging at a cell's current while its run holds a higher voltage, a **source** offering its own voltage at a burst current once nothing holds it up, flipping when the supply goes or when it is outbid, and waiting as a sink when empty. A flip rebuilds its run, because role decides which neighbours conduct. Its voltage falls as it empties, so a sink on it slows as it goes: a burst, not a battery. It leaks a fixed fraction a tick. This is what makes the electrostatic generator's pulse usable.

**Built (2026-10-14): the Charger** (`machine/charger/`), a sink that charges *jar rounds* for a coil gun (`docs/feedback_guns_spec.md` §11 step 4). §6.9 refuses item batteries for tools; the answer here keeps that: the charge rides in the ammunition, one round one shot, leaking linearly wherever it is (a stamp and a tick, like a workpiece's heat). It has no rating and never wears -- it is a terminal, and over-voltage reaches the coil's winding as field pressure when the round is fired, which is a consequence, never a refusal.

**Built (2026-10-14): the electrolytic cell** (`machine/electrolytic/`), §6.14.

**Built (2026-10-14): the motor** (`machine/dynamo/MotorBlock`), §6.8's `Eu`-to-Mechanical boundary, the dynamo reversed. A sink with an output face toward a shaft and the dynamo's two windings. The voltage it sees over the winding's `voltsPerRpm` is its **no-load speed**; the current it is delivered times the same figure is its **torque**, handed to the shaft run through `RotationNetwork.driveByBelt` every tick, as a belt drives a run (into the run's frame through the coupling node's ratio). The current it asks for is the winding's rated amps times `1 - rpm / no-load rpm`: a real DC motor's back-EMF line, with the rated amps standing in for the stall current because resistance is not modelled. So an idle motor draws next to nothing, a loaded one slows until its current carries the load, and an under-voltage one runs slow, never stopped (§6.5) -- none of it written as a rule. A fine motor on a fine dynamo's line, or a fine motor turning a coarse dynamo, passes power through at the shaft's friction and nothing else, which is the motor-generator set: MV in, LV out, with no constant.

`driveByBelt` was widened in the same change: capacities from several drivers now add and the fastest target wins, so two motors on one shaft share its load (a belt and a motor too). Before, the last caller in a tick won.

**Divergence:** §6.8's table named an *adapter* motor, a fitting on a machine. It is a block, because a motor-generator set needs a shaft between the two, and a fitting has none. An adapter form (motor bolted onto a machine that takes a shaft) can follow and would be the same arithmetic. **[OPEN]** Over-voltage: a motor over its band only spins faster, nothing wears it; the overspeed trip already answers the shaft. **[OPEN]** One direction only.

**Not built:** everything else under §6.11's *Deferred* stays deferred; the `[OPEN]`s raised while building are in `TODO.md` under `Eu`.

### 6.14 The electrolytic cell: electrorefining

**Built 2026-10-14, user rulings of the same day:** electrorefining first (not water electrolysis), an impure cathode comes off as scrap, plain water as the electrolyte for now. Roadmap Slice 5's first machine, and the first reader of `Qu`.

**What it is.** A sink with a water tank, an anode slot and a cathode. Any metal, or an alloy of metals, in any form, hangs as the anode. It dissolves into the water as its constituents -- a pure metal as itself, an alloy by its material file's new `composition` (brass is copper 0.65 and zinc 0.35). The current plates metal back out of the water onto the cathode. One ingot of any metal is `FTuning.QU_PER_INGOT`; a nugget a ninth, a block nine.

**Voltage is a threshold, current is the rate** (§6.1). Every amp delivered moves `QU_PER_AMP_TICK` Qu a tick, times the cell's condition; the cell does not use `ElectricRules.rate`. Voltage decides only *which* metal plates: the noblest metal in the water (highest `electrode_potential`) plates at `ELECTROLYSIS_OVERPOTENTIAL_VOLTS`, and a less noble one needs that plus the gap. Below a metal's figure no amount of current plates it -- one of §6.5's real thresholds, so a hard gate.

**The voltage is the selectivity, and it is a property of the supply.** Zinc sits 1.1 V behind copper, so a zinc-copper galvanic cell (1.1 V) refines copper out of brass and leaves the zinc in the water, and a coarse dynamo at an engine's working speed (1.5 V) plates both. Tin sits 0.48 V behind copper: a zinc-copper cell does not refine bronze, a lead-copper cell (0.47 V) does. So the slow, selective route is a galvanic cell of the right pair, and the fast route is a dynamo held under the threshold by a slower shaft or a longer run. Nothing names a metal; the six potentials already in the material table do it.

**Overrun spoils the finished deposit.** While the anode lasts it keeps the noble metal coming. Once it is gone, the only thing left for the anode to do is split water, which takes `ELECTROLYSIS_WATER_VOLTS` (1.23 V, real). Below that the cell stops, so a low-voltage refinery is safe to forget. Above it, the current strips what is left in the water -- the noble metal, then whatever waited behind it -- onto the cathode the player has not taken yet. Purity is judged on the whole deposit when it comes off (`ELECTROLYTIC_PURE_FRACTION`), so an overrun turns a cathode of copper into copper scrap. Scrap hangs as an anode again, so the loss is time and current, not metal. The fast supply is the one that punishes a late hand, and that trade was not placed by hand: it falls out of water's potential sitting between copper's and zinc's.

**The water remembers** (philosophy §9). Unplated metal stays dissolved; the next anode in that water inherits it. Draining the tank takes dissolved metal with it in proportion.

**Over-voltage** is the electromagnet's rule: rated LV, excess is condition and heat, never refusal, never destruction.

**What was tempting, and wrong.** *Water electrolysis first.* Easier to state, but nothing finished sits in the cell to be spoiled, so it had no overrun. *Thresholds as absolute per-metal figures* (copper at 0.34 V and so on): selectivity comes from the *gap* between metals present, so the rule is relative to the noblest in the water.

**[OPEN]**
- Real refining needs a conductive electrolyte, an acid or salt in the water. Plain water is accepted for now; conductivity as composition belongs to the acids pass.
- Concentration (`Qu/mB`) is not readable by any instrument; what is in the water is invisible until something comes off the cathode. A concentration instrument is the obvious next sense (philosophy §14's *Sense*).
- Current density: real deposits burn (powdery, dark) when the current outruns what the solution can supply. Built for a plated piece (§6.15); the cell's own cathode still never burns, and cell size as a physical upgrade is not built.
- Gas: above 1.23 V the charge that plates nothing evolves hydrogen and oxygen, and that is lost. Capturing it waits on gases as fluids.
- A less noble anode in a nobler solution (zinc into copper water) plates spontaneously, which is a galvanic cell. The cell stays a sink and needs a supply anyway.
- Degrade tier: impure is spoil only, as the removal overruns are; named intermediate items are document 3's job.

### 6.15 Electroplating

**Built 2026-10-14, user rulings of the same day:** no new block -- the electrolytic cell's own chemistry onto an item hung as the cathode; overrun is the coat thickening past what was wanted, and too much current burns it. No prior art worth reading (author's word).

**What it is.** Hang a metal piece on a *side* of the cell (the top takes the anode). While it hangs, what plates goes onto it rather than onto the cell's own deposit, by §6.14's rules unchanged: voltage picks the metal (noblest first), current is the rate. Only a metal conducts, so only a metal is a cathode. Taking it off (empty hand) gives the piece back with a `Plating` data component: the dominant metal, the thickness in Qu, and a quality. The piece is still what it was -- `Material.find` still calls plated iron iron (philosophy 11).

**The ladder** (philosophy 6), all judged when the piece comes out:
- **Porous** (degrade): under `PLATING_POROUS_BELOW_QU`, the coat has not closed.
- **Sound**: in the window.
- **Mixed** (degrade): under `ELECTROLYTIC_PURE_FRACTION` of one metal -- the voltage reached a second metal in the water.
- **Burnt** (degrade): over `PLATING_BURNT_SHARE` of the coat grew at more current than the piece takes. That limit is current density: `PLATING_BURN_AMPS_PER_INGOT` times the piece's size (read off its form, as the cell reads an anode) to the two-thirds, since surface goes as size to the two-thirds. So a galvanic cell's amp or a fine dynamo's two plates an ingot clean, a coarse dynamo's eight burns it -- and plates a block clean. Which supply suits is a property of the piece, not of a recipe.
- **Spalled** (spoil, the overrun): over `PLATING_SPALLS_ABOVE_QU` the coat is too thick to hold and flakes off as the piece comes out: the piece comes back bare and the metal as nuggets, which hang as an anode again. The loss is time and current, as a scrap cathode's is. The finished coat keeps growing for as long as nobody takes it out -- the core loop on the cell's second job.

A plated piece hung again brings its coat back in to grow further, burnt included.

**What was tempting, and wrong.** *Widening the `Dip`* (`process/Dip`, built naming electroplating as its reason, `TODO.md` §5f as of `50b93c6`). A dip is a table of item-in, item-out conditions, which would have made plating a recipe per piece and per metal; the cell already had the chemistry and the selectivity, and a piece hung in it needs none of that restated. The `Dip` keeps glazing.

**[OPEN]**
- Nothing reads a coat yet: the §5.4 trap. Corrosion (`corrosion_resistance`, unread) is the obvious reader, the optical sorter fooled by a gold coat the most Feedback-like one; neither exists.
- Thickness is a figure no instrument measures; the tooltip gives the metal and the adjective only, and the thickness at the truth tier alone (`Quantity.FIGURE`, philosophy 8). Calipers reading it would be the natural instrument.
- One piece at a time.

### 6.16 The heating coil

**Built 2026-10-14.** §6.8's `Eu`-to-Thermal boundary: a block set under a vessel, boiler or anything else that reads `HeatSource.below`, as a firebox is.

**The element is a body, not a flame.** A fire has a temperature and no mass (`HeatSource`); an element has a small mass, so it is a `ThermalBody`. Each tick the current puts `volts x amps x COIL_WORK_PER_WATT_TICK` of Work into it -- Joule's law in this mod's units, the one physical constant, as `QU_PER_AMP_TICK` is Faraday's -- and it gives the body above `FIRE_CONDUCTANCE x (element - load)`, the same figure that body's own `Heat.tick` takes from its fire, so what the vessel gains the element loses. It settles a little above its load. That is not an exchange rate in §6.8's sense: nothing says "N Work per Eu" as a price; it is the energy the current dissipates, stated once, and the vessel still heats on a difference as everything does.

**Electrical heat's character** (philosophy 10) falls out of the mass: cut the supply and the element has almost nothing to coast on. A fire's character is its fuel; a coil's is its supply -- a dynamo's shaft speed sets the volts, so a slower river is a cooler coil.

**The element's limit is its metal's.** Above the element's `service_temperature` (from its material file) the coil loses condition by the excess, to a floor; condition multiplies the heat. Over-voltage needs no rule of its own (§6.5): more volts is more heat, and that is where it shows. Copper wears above 300 Tu and iron holds to 500, so the element is the physical upgrade -- a better element is a hotter coil you could point at, one class and one block per metal. A later alloy element is a registration line.

**Numbers, simulated.** A fine dynamo at a steam engine's speed (6 V, 2 A) holds a small crucible near 290 Tu, just under copper's limit; a hand-cranked one near 560. The game test runs two coils at 9 V: the crucible passes 300 Tu, copper wears, iron does not. So a coil is a precise low heater -- tempering, preheat, a boiler -- and not yet a smelter.

**[OPEN]**
- No coil reaches smelting heat on today's sources (`TODO.md`).
- Built since: a breaker in its line is its switch (§6.4), and a controller can drive it.
- One coil heats only the block above it.

---

## 7. Contact hazards: hot, wet, live

**Built 2026-10-14, user rulings of the same day.** Philosophy 9 says a glowing ingot is a glowing ingot wherever it is, "in a hand, in a wooden chest, next to something flammable", and that consequences which follow obviously should be allowed to. Until now none did: a 900 Tu ingot could be carried in bare hands forever. This section is the part of that sentence that was missing.

### 7.1 One shape, three physics

A state the machine already holds (a temperature, a pressure, a voltage) is above a threshold, and whoever is in contact pays. Nothing is uptime, nothing is a machine's own wear, and nothing asks what a block *is*: a block is hot if it is a `ThermalBody`, live if it is an `ElectricNode` with `touchVolts() > 0`, pressurised if it holds a `Pu`. The code is `core/hazard/`.

**Named bands, not a dial** (philosophy 3). `HotContact` has three, anchored on figures the mod already has: **scald** from `CONTACT_PAIN_TU` (100, water boils), **burn** from `CONTACT_BURN_TU` (300, wood chars) which also sets fire, **sear** from `CONTACT_SEAR_TU` (700, the first glow). A player learns "that is a sear", not a slope. `Shock` takes its figure from the voltage's `VoltageBand`, with LV keeping the bare wire's old one so nothing was added beside it.

Damage types are data (`data/feedback/damage_type/`): `hot_contact` and `scald` are in `minecraft:is_fire`, so **fire resistance is a real, bought answer** to handling red-hot work, and `shock` is in `bypasses_armor` because a chestplate is not insulation.

### 7.2 Hot contact

- **A hot piece in either hand** hurts twice a second (`CONTACT_INTERVAL_TICKS`) while its own stamped temperature (`ItemHeat`, nothing ticked per item) is over what the player can handle. Only the hands: a hot ingot in a pocket is the player's own business, which is what keeps this from being a chore.
- **A bare hand on a hot block** (right-click on any `ThermalBody` over its limit) hurts once per click. The use is *not* cancelled. A consequence, never a lock: reaching into a hot crucible bare-handed is possible, once, and teaches why gloves exist.
- Creative and spectator players are left alone.

**Protection is a physical thing, rated by its material.** `Insulator` is asked of a stack; nothing says which item it is. `Grip` reads the gripping material's own file: `service_temperature`, else `oxidation_temperature`, and only if it **insulates** -- `conductivity` under `GRIP_MAX_CONDUCTIVITY`, or an organic where nobody measured. A thing may be handled while `ambient + exposure x (T - ambient) <= what the grip stands`, so the *exposure* is the physical change: a glove sits on the palm (`GLOVE_EXPOSURE` 0.25), tongs put a forearm of arm and a grip between jaws and hand (`TONGS_EXPOSURE` 0.1).

- **Gloves** (`GloveItem`, a Curios `hands` curio): made of a material, rated by it. Leather holds warm things (about 340 Tu) and not red ones. They also stop a bare conductor up to `GLOVE_SHOCK_PROOF_VOLTS` where the material insulates.
- **Tongs** (`TongsItem`, in a hand): two rods of one metal and one grip, in any arrangement (`TongsRecipe`), the component carrying both materials. The limit is the lower of the grip's (at tong exposure) and the arms' melting point. A wooden grip and iron arms hold yellow heat; a bone grip holds whatever the iron will; copper arms give out at copper's melt, well before steel's window. **Steel work wants bone-gripped tongs, which is a thing a player works out.** They do nothing against a wire.

Two answers that cost different things (philosophy 5): a glove is always on and covers the warm range; tongs cost a hand and cover the glowing one. Neither wears (the three accepted wear-with-use exceptions are not a list that grows).

**What was tempting, and wrong.** *A per-item "protects to N Tu".* It would be a stat on the item, would not move when the grip material did, and is exactly the "+50%" non-design the philosophy names. *Hot items burning anywhere in the inventory.* Real in a sense and a chore in practice: it removes the decision (carry it in your hand, or don't) and replaces it with a rule about pockets. *Cancelling the interaction.* A lock where there was a consequence.

### 7.3 Steam, and what lets go

`PressureBurst` is the one ladder, shared by the Pressure Vessel and the Boiler: nothing under the danger line however long it sits; between danger and critical a per-tick chance that climbs linearly; at critical certain. The shape is PneumaticCraft: Repressurized's (GPL-3.0; recorded in `THIRD-PARTY-LICENSES.md`).

**The boiler gains a pressure** (`BoilerBlockEntity#getPressure`), which it never had, because it could not be over-restricted -- the open item the `FTuning` boiler note records as "Not taken: the exploding boiler". Pressure is the held steam over `BOILER_STEAM_MB_PER_PU` (a full 4000 mB tank is 4 Pu, under the danger line of 5) **plus, only while the tank has no room for another tick's production, `BOILER_PU_PER_TU_OVER` per Tu over working temperature**. A boiler making steam into an open line spends its heat on the work of boiling and sits near working temperature however big the fire. A shut one has no outlet, so the fire keeps pushing and the surplus is paid as pressure. The only mistake that kills a boiler is a restricted line under a real fire, which is the GregTech case and nothing else: a boiler that merely sits full is safe.

**The warning comes first.** Over `BOILER_SEEP_PU` the seams weep: steam is lost (a partial relief) and whoever stands beside it is scalded, with a hiss and a cloud. It is loud, local and survivable, and a boiler a little over the line may settle on it. One under a real fire does not, and lets go: an explosion (the vessel's own kind) plus a scald out to `STEAM_BURST_REACH`. Steam gets in under a glove, so scalding asks no protection: only fire resistance answers.

**The wear rule, clarified (user ruling 2026-10-14).** "A machine degrades and never breaks" is about *factories*: run something slightly suboptimally for a long time and it gets worse slowly and surely, almost or entirely unusable only at the end of a readable path, never instantly. It is not a promise that no machine can be destroyed. Pressure holders are the Hazard tier of philosophy 6 and may be destroyed by a pressure the player let build; conductors may be destroyed by degradation left unfixed (§6.4). Both are reached only by misuse. `CLAUDE.md` says so.

**Not built.** A relief valve as a physical part (the obvious answer, and a fitting: a boiler with one would weep at a set pressure and never reach the danger line, at the cost of steam and a scald); an instrument that reads boiler pressure (the Pressure Vessel has none either); sidedness of the scald; steam as a world fluid (steam has no block, so there is nothing to stand in).

### 7.4 Shock

`ElectricNode#touchVolts()` is the volts a bare hand would meet: `0` where a casing or insulation covers it, which is almost everywhere. A **bare, loaded conductor** (`CableBlockEntity`: insulation gone, current flowing) says its potential, never under LV's reference; a **charged Leyden jar** says its held voltage once it is at `SHOCK_MIN_VOLTS` (MV) or more. Touching is a walk-in (`entityInside`, any entity) or a right-click (a player). Damage follows the `VoltageBand` (`SHOCK_MV_DAMAGE` and up); an insulating glove stops it below `GLOVE_SHOCK_PROOF_VOLTS`; armour does not.

Resistance is still not modelled (philosophy 17): a shock is a property of a conductor being *exposed* at a voltage, which a player can see and fix, not of the current through a particular hand.

**[OPEN]**
- A galvanometer or electroscope used on a bare, live wire is a right-click on it and shocks. That is physically honest; whether the *instrument* should carry the insulation (a probe, not a hand) is a call for later.
- Dynamos and electrostatic generators have no exposed terminals today (`touchVolts` is 0); only the cable and the jar are bare.
- A hot piece does not ignite a flammable block it lands on or sits beside. Philosophy 9 allows it; nothing builds it.

---

## 8. Offline catch-up

**Built 2026-10-14, user ruling of the same day:** cooling plus fuel burn across the gap, process progress not advanced.

### 8.1 The gap

A workpiece cools in an unloaded chunk for free (`ItemHeat`: a stamp and a tick, computed when asked). A *block* holds a live temperature and updates it by ticking, and a chunk that is not loaded does not tick. A crucible you walked away from froze at whatever it was and resumed from there, hot or cold, as though the world had paused around it. It was recorded as a known gap in `TODO.md` ("Not taken: catch-up for unloaded devices"); this closes the part of it that mattered. TerraFirmaCraft keeps a calendar and burns fuel across time skips; the read is in `THIRD-PARTY-LICENSES.md`, and what is taken is the idea, not a calendar.

### 8.2 How

A block records the game time it was saved at (`Catchup`, one `SavedAt` long in its tag). On its first tick after a load it asks how long it was away; if that was `CATCHUP_MIN_TICKS` or more it is advanced across the gap **in one step**:

- `Heat.relax` is `Heat.tick` solved for *n* ticks. One tick moves a body by `-k (T - Teq)` with `k` the total conductance over the mass and `Teq` the existing `Heat.equilibrium`, a geometric series, so `(1 - k)^n` is the whole of it. A gap of a million ticks costs one `pow`, and a game test pins it to the loop.
- A `HeatSource` answers `catchUp(gap)` with a `FireSpan`: how many of the gap's ticks it burned and at what mean temperature. **A fuel burns until spent and then the fire is out**, lit first and dark after, so one span is the whole shape. The firebox and the self-fired vessel burn the piece alight and then their stack, unblown (nobody was working the bellows) and at each fuel's nominal temperature (the quality roll is a noise floor for a fire someone watches).
- A body relaxes towards the fire for the lit part and towards the room for the rest (`Catchup.settle`). A crucible or boiler asks the block under it (`HeatSource.spanBelow`), and lava is lit for the whole gap, as it always is.
- Electrical nodes that are `ThermalBody`s settle at their first resolve after the load, from the one place in `ElectricNode`, so cable, magnet, fuse, cell and coil need no edit of their own.

### 8.3 What it does not do

- **Process progress is not advanced.** An ore in a crucible is not cooked across a gap: that is a second simulation (suitability integrated along a curve) for a stretch nobody saw, and a player who walked away has not earned the completion. It resumes where it left off, at the temperature it now has.
- **No air.** A bellows' banked air is gone.
- **The firebox answers for itself.** If a vessel's chunk was unloaded and the firebox under it was not, the firebox was ticking throughout and is asked if it is lit; the span is then the whole gap at its current flame, which is a fair statement and not an exact one.
- **Machines that move** (a rotation run, an electric run) are not caught up: nothing keeps their state across a gap and a run rebuilds itself on load.

**Trap worth recording.** The first draft cooled by a *rate* per elapsed tick, which is the item's law (linear, `Heat.cooled`) and wrong for a vessel: vessels cool by a difference-driven leak (§ thermal model), so the gap would have fallen off a cliff to ambient. The vessel's law solved in closed form is the same mathematics with no new variable.
