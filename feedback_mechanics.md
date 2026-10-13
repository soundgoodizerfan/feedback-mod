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

### 5.4 Generated items

Which forms are generated is declared, not inferred: `data/feedback/form_type/<form>.json` names the `kinds` a form is generated for (every one today is `["metal"]`), and `block: true` makes it a placed storage block rather than an item. A material gets every form type whose kinds include its own, unless it lists an explicit item for it (`forms`) or excludes it (`exclude`). Absence is the stated thing, so a new form type reaches every metal without editing any of them; the cost is that a metal with a nonsense form (hardened steel plate, cast iron wire) has to say so.

**Any other form name is an association.** A material's `forms` may name anything made of it under any name no form type declares -- iron's `horse_armor`, sculk's `sensor`, sixteen `white`...`black` for wool. Those generate nothing; they exist so `Material.find` can say what an item is made of (the Material Probe uses it). One item per name, and one material per item.

`FormItems` reads the jar's own `material/` and `form_type/` files at registration because items exist before any datapack does, and registers `<ns>:<material>_<form>` as a `FormItem` (or a `FormBlock` and its item), named by pattern (`form.feedback.plate` = `%s Plate`, `form.feedback.block` = `Block of %s`) so a new item costs only its material's name. A form with no registered item is skipped at compile, never an error. Item models come from `FormItemModels`; block states, block loot, the pickaxe tag and the nine-to-one nugget/ingot/block recipes from `FormData` (`runData`). Lang stays hand-authored because a generated `en_us.json` would shadow it at the same path.

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

**Netherite is magnetic, and keeps it hot.** Vanilla makes a lodestone out of it, and §2 says what vanilla does is evidence. A Curie point of 1500 Tu against iron's 770 is the exploitable difference: a hot drum separates them.

- **[OPEN]** `viscosity` is recorded although philosophy §3 names it as not yet earning its place. It costs nothing unread; it earns a reader only if a misrun is a decision.
- **[OPEN]** Specific heat was **not** added. TFC's per-material heat capacity was declined once (`TODO.md`), and it would need a new unit (Work per Tu per Mu).
- **[OPEN]** Elastic modulus and fatigue were considered and left out: modulus fails §3's test, and fatigue is wear from uptime, which the wear rule forbids.
- Water and lava have no material file: `hardness` is required and a liquid has none. Lava is a heat source already; if a liquid needs figures, make hardness optional first.
- Every metal, vanilla and Feedback's own, now grows the full form set; nothing gives most of them a purpose yet (the §5.4 trap, at scale). Art is one vanilla stand-in per form, untinted, so every metal's plate looks like iron's.

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

**[OPEN] Physical anchors.** What a chemical cell reaches, what a steam-driven dynamo reaches, and what the roster has to occupy at the top. For the user to set; the band names carry no figure until then. **[OPEN]** Cells in series add voltage one-to-one, which is a free scalar again (1 to N cells). Either the stack is capped to named bands or cells come as pre-built packs per band. Not decided.

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

**Decided (2026-10-12): destruction exists for conductors and for nothing else.** `CLAUDE.md`'s wear rule says a machine "degrades and never breaks, because a machine refusing to run is a hard gate where no physical impossibility exists." Step 3 is a wire breaking, and the user has ruled that it stands: the rule is scoped to **machines** and stays exactly as written for them (§6.5: a machine is never destroyed, whatever you feed it), and **conductors are the one place destruction exists**. The reasoning, in the user's words: *destruction is a consequence of not fixing degradation.* A conductor is reached only through misuse (over-rating), never uptime, and never instantly; the rule's reason (do not turn a graded consequence into a cliff) is kept because the cliff has two readable steps in front of it. `CLAUDE.md` is updated to say so.

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
| `Eu` to Mechanical | Adapter motor (an `Adapter`, `fitting/`) | out; later slice |
| `Eu` to Thermal | Adapter coil | out; later slice |
| `Eu` to field | Electromagnet | out |
| `Eu` to Chemical | Electrolytic cell | out; later slice |

**[OPEN]** How a boundary block's figures are set without a constant. A dynamo's output voltage and current have to come from its own construction and the shaft it is on (the speed and torque it sees), and a motor's torque from the current it is delivered, in each case as that block's physics and not a table that says "N `Su` per `Eu`". This is the thing to resist: a fixed exchange constant between `Eu` and anything is the universal currency arriving by the side door.

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

**Deferred:** transformers, the capacitor and storage in general beyond the cell, the insulation layer and stripping, multi-conductor bundles, fuses and breakers, any generator fed by steam or a shaft (Slice 6 onward), motor and coil adapters, the electrolytic cell and electroplating (Slice 5), overclocking of any kind (not taken at all), and circuits and data electronics.

### 6.12 Open questions and debts

- **Resolved 2026-10-12:** component names (`voltage` a band, `current` integer amps, §6.1); routing as run-level resolver with proportional shares (§6.2); four closed bands `LV`/`MV`/`HV`/`UHV` (§6.3); rate floor zero (§6.5); a machine never refuses a line (§6.5); wire destruction scoped to conductors and the fuse as third exception (§6.4, in `CLAUDE.md`).
- **[OPEN]** Whether the built lang symbols (`Eu`, plain band names) are final. §6.1.
- **[OPEN]** Sag curve shape and source current cap; blocked-source handling; amps per sink per wire. §6.2.
- **[OPEN]** Band physical anchors (cell, steam dynamo); series-stack capping. §6.3.
- **[OPEN]** Shape of the under-voltage slow-down. §6.5.
- **[OPEN]** Over-voltage per-unit scale. §6.5.
- **[OPEN]** How a boundary block's figures are set without an exchange constant; to be worked out with the user. §6.8.

### 6.13 Build status

**Built: the conductor and the resolver.** `core/electric/` follows `core/rotation/`: `ElectricPropagator` floods a run when a node is placed, broken or loaded, and hands `ElectricRun` a member list and an adjacency list; `ElectricRun` builds a routing tree from the supplying sources and caches it, and each tick is arithmetic over arrays. The tree is rebuilt on a topology change and also when the *pool* of supplying sources changes under a standing topology (a pulse starting, a source being outbid), detected by comparing which sources are pooled.

What a tick does, as built: the highest nominal voltage among sources offering anything sets the run's voltage and those sources form the pool; lower-voltage sources are blocked and report it (§6.2 as written). Reachable sinks ask for current; if the pool, up to `SOURCE_OVERDRAW_LIMIT` times its rating, cannot cover it every sink gets the same fraction. The pool sags as it is drawn past its rating, linearly to a floor and never to zero (`ElectricRules.sagFactor`). Current accumulates from the sinks back up the tree and each wire block takes `dropPerAmp x current` off the voltage through it; a path that drops the lot delivers zero volts, never a negative one. **Current is routed along each sink's shortest path to the pool**, which approximates how parallel supplies share a wire and is chosen for being deterministic and loop-free.

**Built: the wire.** `machine/cable/`. A `ThermalBody`: nothing is put in at or below the rating, so a wire inside its rating never leaves ambient; above it, `FTuning.WIRE_OVERLOAD_WORK_PER_AMP` per amp over goes in through `Heat.warm` and the leak is `Heat.tick`'s own. It settles where input equals leak and cannot run away. Insulation burns off above `WIRE_INSULATION_LIMIT_TU`, then the conductor degrades above its material's `service_temperature` (rating falls, drop rises), then it is destroyed at zero condition. A bare conductor under load shocks what touches it. Figures come from the material: rating from `ampacity` x `WIRE_AMPS_PER_AMPACITY`, drop from the new `electrical.drop`, so a second metal is a registration line.

**Divergences from the text above.**
- A sink's current is `float` inside the resolver; the `Eu` type (band and whole amps) is the typed seam for display, not what the physics unwraps. A sink sees a continuous voltage after drop, which no band names.
- The sources' nominal voltage is plain volts, with a band derived from it (`ElectricRules.bandOf`), because a galvanic cell's voltage is the difference of two electrode potentials, which is not a band.
- No voltage rating on the wire yet (LV only), and no insulation *item*: insulation is a flag on the cable, which is the minimum that lets stage 1 of §6.4 exist.

**Built: two sources.** The *galvanic cell* (`machine/cell/`) is given an anode and a cathode material and holds the magnitude of the difference of their `electrical.electrode_potential` (zinc against copper is 1.1 V), at `GALVANIC_CELL_AMPS`; it never runs down (`[OPEN]`, `TODO.md`). The *electrostatic generator* (`machine/electrostatic/`) is a pulse: using it is one turn of the handle, which puts `ELECTROSTATIC_VOLTS` at `ELECTROSTATIC_AMPS` on the line for `ELECTROSTATIC_PULSE_TICKS` and then nothing. It holds no charge. While it pulses it outbids a lower-voltage cell on the same line, which is blocked for exactly that long. Neither is a conversion: nothing in the mechanical or thermal networks feeds either.

**Built: sensing.** Every `ElectricNode` is a `DataNode` and holds its own reading: voltage at the block, current through or delivered, current wanted (sinks), blocked (sources). The reading is server-side and is not synced to clients -- an instrument composes its figure where it is true. The **galvanometer** exists twice, as §6.7 says: a carried `GalvanometerItem` (current, to `GALVANOMETER_RESOLUTION_AMPS`, and on a sink what was wanted beside what was delivered) and a `CurrentSensorFitting` bolted to a face of a cable, a sided `SensorFitting` exactly as `TemperatureSensorFitting` is, which a controller card can be linked to. The **electroscope** is carried and reads potential only, coarsely. The Jade card says *live*, *dead*, *starved* or *outbid* for nothing and gives figures only with the matching instrument carried. `Quantity` gained `VOLTAGE` and `CURRENT`, appended.

**Not built:** the electromagnet is the following commit of the same slice and are recorded in `TODO.md`; everything under §6.11's *Deferred* stays deferred.
