# feedback_mechanics.md — Document 2: Mechanics

Document 2 in three-document spectrum `CLAUDE.md` describes: not identity (`feedback_philosophy.md`), not roster (document 3, still unwritten). This is *how each decided principle implemented* — unit arithmetic, sensor/actuator pairings, thermal model, energy networks, control node set. `feedback_philosophy.md` defers to this document by name in dozen places; those are debts, tracked in `TODO.md`'s *Document 2* list.

**Status: started, not comprehensive.** §1 (XP) and §2 (Blaze) are still speculative and unbuilt; §3-§9 are built and say so in place. Per `TODO.md` item 1, working method so far slice-first — build, then write doc 2 from what build forced you to decide. This section is exception: phenomenon worked through speculatively *before* code exists, because mechanism must be right before single unit or block entity named after it. Treat as provisional until something built contradicts it, then fix this file in same change, per `CLAUDE.md`'s living-document rule.

---

## 1. Experience (XP)

### 1.1 What it is, mechanically

`feedback_philosophy.md` §2 already commits to general move this section makes concrete: phenomenon that demonstrably happens in Minecraft is real in setting, can be industrialized once someone notices measurable variable in it. XP qualifies immediately — already stored, transportable, spendable quantity in vanilla. Only work: give it same generate/capture/store/transmit/apply staging every other energy phenomenon in mod gets (§14 of philosophy: water, wind, fire, steam, Blaze heat, lightning, redstone).

Mechanism: XP is physical quantity produced by certain living or once-living systems (players, mobs, and per §1.6 below, engineered biological processes), capturable in appropriate storage substrate. **Not** memory, thought, or information — nothing semantic. Byproduct, same way heat is byproduct of combustion. Distinction matters mechanically, not just narratively: XP-consuming machines never need know *what* they do to workpiece, only that quantity was spent. Keeps XP consistent with "machines define physical operations, not recipes."

### 1.2 Quantity and level — genuinely two-dimensional, like PneumaticCraft's air

First pass treated level as pure function of quantity (reusing Tu/Work split, where any two reservoirs holding same amount read same state). Wrong: throws away variable PneumaticCraft's pressure/air-amount model gets right, and mod already has working pattern for. **Level depends on quantity *and* reservoir capacity — two independent numbers, not one.**

Pattern to copy is `ThermalMass`, not `ItemHeat`. Temperature isn't heat; it's heat divided by mass, so bigger thermal body needs more `Work` for same `Tu`. Level isn't quantity; it's quantity divided by reservoir capacity, so bigger reservoir needs more `XPu` for same level. Same shape as ideal-gas pressure vessel (pressure depends on amount *and* volume), exactly PneumaticCraft's model — read as prior art before building, per standing rule (§1.7).

- **Quantity** — how much XP stored. Extensive, additive. Unit: `XPu`, int, following `Fu`'s precedent (XP points integers in vanilla; no argument for fractional XP that isn't also argument for fractional `Fu`).
- **Capacity** — property of specific reservoir block, not of stock it holds. Fixed per block (bigger reservoir is bigger block, exactly like bigger crucible), varies by tier/size — the "larger vessel" upgrade hard design rules already name as canonical physical upgrade. Giving XP storage identical upgrade shape as everything else is point; shouldn't need bespoke rule.
- **Level** — intensity of what's stored. Derived, **not stored as field**, computed on read from quantity and capacity together, same on-demand discipline as `ItemHeat` and `Heat.tick`. Simplest form plain ratio, `quantity / capacity`. To keep vanilla's real, non-arbitrary curve as source of nonlinearity instead of inventing one, evaluate vanilla's level-for-points function at quantity scaled by reference capacity: `level = vanillaLevelForPoints(quantity × referenceCapacity / capacity)`. Reservoir built at reference capacity reads exactly vanilla's own level for its stock; bigger reads lower for same stock, smaller reads higher — same shape as `ThermalMass`, same as pressure vessel.

  Reference capacity is player's own XP bar — only reservoir vanilla already defines curve for. Every engineered reservoir's capacity set relative to that, so calibration constant this whole unit hangs off is, structurally, size of human brain. Nobody designed it that way; vanilla's curve was simply never anything else.

**[OPEN]** Whether capacity fixed per block only, or itself fittable/upgradeable property ("wider bore" fitting on existing reservoir) — same open question thermal system already has for insulation and vessel size, so resolve once, there, not twice independently.

**[OPEN]** Whether "level" for engineering should be vanilla's stepped integer level, or continuous float on same curve (reservoir at 14.6-equivalent, not just 14). Continuous gives instruments something finer to disagree about (per §8's noise-floor rule — integer level has no room for reading error); stepped is legible at glance. Leaning continuous. Decide when first XP instrument built.

### 1.3 Storage and transmission substrate

Sculk is capture/store/transmit medium, not source (matches source brainstorm's §4 exactly, worth keeping: sculk-as-conduit is separable claim from sculk-as-anything-else, so sculk resonance — vibration sensing, unrelated to XP — can become own domain later without this section changing). Sculk-based reservoir block holds `XPu`; sculk-based conductor moves it. Gives XP same three-tier shape (source, network, sink) as rotation network and thermal model, arguing for eventual own package (`core/experience/`, mirroring `core/thermal/` and `core/rotation/`) — but per source layout rule, package not created until second thing to put in it. Right now notes, not code.

### 1.4 Energy niche — not a fifth currency

Load-bearing decision, resolves real tension source brainstorm didn't quite settle. `feedback_philosophy.md`'s hard rules commit to exactly four domains natively consumable and actuation-capable: Mechanical, Thermal, Chemical, Electrical. Adding XP as fifth interchangeable currency — "engine" converting `XPu` into `Work` at some ratio — would break enumeration and quietly re-create GTCEu's everything-funnels-to-one-currency problem with different currency.

**Decision: XP is not power source, has no engine.** Nothing converts `XPu` into generic `Work`. Never enters actuator. Only role: *conditioning input* to process already running on one of four real domains — changes how process happens, never whether it has enough energy to happen at all (source brainstorm's §7 point, correct, just needed stating as hard constraint rather than stylistic preference). Concretely: material-conditioning stage still needs heat and electrical bias from real domains; XP is additional variable deciding what state material lands in, not what powers stage.

Also why XP exempt from "problem with one answer is not a problem" tension it might otherwise create — doesn't compete with four real domains for "how do I power this" answer, so can't collapse process to one obviously-correct energy source.

### 1.5 Sensing

Already well-specified by source brainstorm, consistent with existing "world resolves on truth" rule (§8 of philosophy): XP instrument reveals raw signal, never semantic verdict. Never says "reaction 87% done." Reports number — local level, fluctuation amplitude, purity signature — player decides meaning. Needs no new rule; existing `Instrument` contract (read-only, no way to act) applied to new measurable. Six apparatus properties `TODO.md` already owes every instrument — range, resolution, accuracy, control, response, stability — apply unchanged. XP sensor's spec sheet not new kind of spec sheet.

### 1.6 Applications

Three application shapes, all consistent with existing hard rules:

- **Repair.** XP restores structure still physically existing in damaged form — cracks, dislocations, residual stress — never matter fully lost. Part missing material outright still needs feedstock. Existing distinction "conventional repair replaces lost material"; gives Mending industrial reading without turning XP into duplication mechanic.
- **Catalysis.** Catalyst conditioned with `XPu` lowers reaction's requirements (temperature, pressure) without supplying reaction's energy. Degrades with use, needs reconditioning — consumable capital cost, not permanent unlock. Real complete loop (manufacture, condition, run, degrade, recondition), gives XP ongoing consumption sink instead of one-time enchant-and-forget.
- **Material conditioning.** Final-process-stage input alongside real heat/electrical conditions, per §1.4. Never gates completion — probability/quality variable feeding noise-floor model already established for instrumentation, not hard requirement.

**Explicitly rejected:** XP as bulk power source for machines (violates §1.4); XP sensors as recipe-completion meters (violates §8's "world resolves on truth, instruments never enter the calculation").

### 1.7 Open questions and debts

- **[OPEN]** Unit name. `XPu` used provisionally; confirm against `Units` checklist before real.
- **[OPEN]** Continuous vs. stepped level — see §1.2.
- **Prior art not yet read.** Per `CLAUDE.md`'s standing rule, XP-as-industrial-resource has real precedent — EnderIO (XP fluid + vat), Botania (mana pool, overflow behavior under load), Actually Additions (XP crystal storage). **PneumaticCraft now closest match for §1.2 specifically** — air pressure/volume model is the two-dimensional quantity/capacity relationship this section adopts. None cloned or read yet. Do before writing any code, same way Create/TFC/GTCEu read before rotation network, thermal model, fitting system.
- **Not yet decided:** whether XP belongs in `feedback_philosophy.md`'s phenomena list (§14: "the world contains multiple useful energy phenomena"). Not added now because nothing built — per `TODO.md`'s slice-first method, entry should wait until XP reservoir actually exists to describe, not be asserted from this document.
- **Rejected from source material:** "experience is not memory" framing and neuropolariton terminology are narrative, not mechanics, stay out. If wanted anywhere, belong in compendium (`docs/speculative_physics_inspo_doc.md`) or eventually document 3, not here.

---

## 2. Blaze Material

`feedback_philosophy.md` names Blaze material as own headline case (§80/82) — "Blaze Rod plus a crude heat exchanger gives a dramatically hotter working fluid" — sat unworked longest of anything philosophy gestures at by name. Question here: *what Blaze does beyond being hotter fuel item*, since "hotter fuel item" already free.

**First pass proposed live-containment `HeatSource` block (cage holding trapped Blaze essence, killed by water contact). Superseded below — much cheaper design does more with what's already built, needs no new block-entity machinery.** Tempting part of old version was treating Blaze as *fire that never depletes*; better read: rod isn't fire at all. Kept here rather than deleted because water-collision-with-`Quench` idea (real system interaction, not Blaze-specific rule) worth remembering if some *other* phenomenon needs genuine containment/kill-condition shape later.

### 2.1 The split: rod stores, powder burns

Vanilla gives Blaze Rod and Blaze Powder as two items already. Feedback's mechanism gives them two *roles* instead of interchangeable brewing/fuel ingredients:

- **Blaze Rod does not go in firebox.** Storage item, not fuel.
- **Blaze Powder is fuel** — extreme one, because breaking rod into powder releases what it held.

### 2.2 Rod: reuses `ItemHeat`, just with different numbers

`Blaze Rod` is ordinary `ItemHeat`-bearing item — same stamp-plus-timestamp, computed-on-demand pattern as any hot workpiece, no new plumbing. Different: constants. Settles at small elevated baseline over ambient (hand-warmer warmth, not blazing) via cooling rate small enough that reaching true ambient takes very long. When it finally does, becomes second, discrete item — **`Spent Blaze Rod`** — not continuous "0% charged" reading, per hard rule against continuous knobs (§3): player-facing state is "still good" or "gone cold," not percentage ticking down.

**[OPEN]** `ItemHeat`'s cooling rate currently one global constant (hot-ingot 1.5 Tu/t). Must become per-material data before Blaze Rod can use different one — same promotion fuel already made for flame temperature (now material's `combustion`, §5.7) and `Quench` made for `minTemperature`. Third instance of same lesson: figure starting as single constant keeps needing to vary by material. Name as pattern in own right rather than rediscover fourth time.

**Spent Blaze Rod rechargeable** by reheating — `ThermalProcess` table row (sufficient `Tu`, sufficient duration), no new system, just data row once underlying item exists.

### 2.3 Powder: an ordinary fuel, just an extreme one

Grinding **still-charged** rod (existing size-reduction/grinding pipeline) yields `Blaze Powder`, whose material's `combustion` block (§5.7) has very high `flame_temperature`, very short `burn_ticks`, and — where existing `spread` field earns keep — wide `spread`, so burn genuinely inconsistent, not just hot. Inconsistency is what "almost explosive" means mechanically: fuel that can spike far past own nominal rating, without inventing second mechanic.

Grinding **`Spent Blaze Rod`** yields inert dust, no fuel value — real cost of letting charged rod go cold before use, genuine timing decision rather than flavor text.

### 2.4 What happens when it's fed to something

Overheating Feedback vessel with Blaze Powder needs no special case: existing overshoot rule (force that can't go into work goes into machine; machine degrades, never breaks) applied to fuel whose `spread` wide enough to overshoot often. No new consequence to design.

Vanilla furnace briefly considered as deliberate exception to that promise — "degrades, never breaks" is commitment about Feedback's own apparatus, not blocks mod doesn't own. Moot: vanilla's furnace/smoker/blast furnace uncraftable and functionally replaced by Feedback's own `ThermalVesselBlockEntity` (`feedback_philosophy.md` §15), structure-spawned copies slated for eventual replacement too (`TODO.md`). No survival player has one left to feed joke to.

### 2.5 Open hook: pulsed processes

Idea that Blaze's `spread`-driven variance could be *feature* for process wanting cyclic rather than steady heat (tempering-adjacent, scheduled annealing) stays marker, not mechanism — no such process exists yet. Revisit when document 2's process section covers thermal cycling, if ever.

### 2.6 Prior art and debts

- **Prior art not yet read.** Real hand warmers and dust explosions both genuine physical phenomena (surface-area-to-volume ratio governing reaction/release rate), so section on solid "scientific, not realistic" ground without outside mod precedent for mechanism. Still worth checking whether TFC or GTCEu — both already reference mods — have rod/dust dual-item precedent worth reading before building.
- **[OPEN]** `ItemHeat` cooling rate must move from global constant to per-material data — see §2.2.
- **Rejected:** live-containment `HeatSource` block and water-kill condition — see note at top of section.

---

## 3. The Thermal Process Model: TPu

Built, not speculative — `docs/tpu_spec_doc.md` (brainstorm this section implements) is fuller account; this is built shape. Replaces `holdTicks` in `ThermalProcess`, `ThermalVesselBlockEntity` and `CrucibleBlockEntity`, and Work-based `VanillaFallback` mechanism `feedback_philosophy.md` §15 originally specified (now corrected there in same change, per living-document rule).

### 3.1 What TPu is, and isn't

TPu (Thermal Process Units) is process-local progress, not physical quantity — does not join §17's unit table, no codec-through-`Unit` machinery, never displayed. `ThermalProcess`'s `required_tpu` field is plain float, same "no type" treatment `Work` already gets, same reason: wrapper would imply TPu joins physical quantities it explicitly is not.

**Never player-facing**, per source doc: no tooltip, measurement tool, HUD figure. What *is* shown on process's JEI card is `required_tpu` itself, printed as "ticks (seconds) at optimum" — a **requirement**, and philosophy 8's table says requirements free, exact, published regardless of instrumentation. Number player reads never "how much TPu accumulated" (a reading, would cost instrument, doesn't exist), only "how many ticks this needs held exactly at optimum" (a spec, same status as steel's temperature band).

### 3.2 Accumulation: a triangular suitability curve

Every `ThermalProcess` now carries `optimal_temperature` alongside `min`/`max`. `ThermalProcess.suitability(tu)` returns 0-to-1 fraction: 0 at or outside band edges, 1.0 at optimum, ramping linearly between. Each tick in band, `currentTpu += suitability`; process completes when `currentTpu >= requiredTpu`. Held exactly at optimum, reproduces old flat-`holdTicks` behaviour exactly (`requiredTpu` ticks, no more); drift off and same ticks stop being enough, entire point — better vessel or better-aimed player finishes measurably faster, which flat countdown could never express.

Philosophy 8's "quality should fall off gracefully, not switch off" applied to completion itself rather than yield. Three points (min/optimal/max) rather than source doc's illustrative six-point table — doc calls exact curve shape tuning decision, three is minimum with a peak at all.

### 3.3 Decay, not reset

Outside band, or changing faster than `max_rate` tolerates, `currentTpu` decays at `FTuning.TPU_DECAY_PER_TICK` (one tick-equivalent per tick, matching fastest possible gain) rather than resetting to zero. Real behaviour fix, not only doc-follow: `CrucibleBlockEntity` already stalled progress out-of-band ("nothing lost") while `ThermalVesselBlockEntity` reset it outright for identical case — two block entities disagreeing about same rule. Decay is correct middle ground source doc asks for, now both share it.

**`max_heating` → `max_rate`, and why "annealing" was never missing mechanic.** Field was heating-only in name and in `CrucibleBlockEntity`'s check (`lastDelta > max`); `ThermalVesselBlockEntity`'s equivalent check already used `Math.abs(lastDelta)`, bounding both directions — same species of two-block-entities-disagreeing bug this section already documents once, found second time. Fix widens field rather than forking new one: `maxRateTuPerTick` now bounds rate of change either way, both block entities agree, and Slice 2's "annealing"/"controlled cooling" need no new mechanic — they're `require_cooling: true` plus tight `max_rate`, same two knobs tempering already has, both turned at once. User's own framing settled this: *"thermal processes should all be `ThermalProcess`; if cooling, annealing, and otherwise can't fit within the schema, the schema isn't loose enough."* No annealing datapack entry exists yet — mechanism only, same "infrastructure before concrete use" pattern as `Instrument` before any instrument existed.

Tempering's `require_cooling` flag keeps own, different rule: flat or rising tick **pauses** (no accumulation, no decay) rather than advancing or decaying, because philosophy 13's actuator-is-a-switch constraint means player has no rate to hold steady with, only on/off timed by hand or Damper — punishing wait as harshly as out-of-band drift would make mechanic itself the trap.

Melt (`required_tpu: 0`) bypasses suitability entirely, completes instant it is in band, exactly as `hold_ticks: 0` did.

### 3.4 The vanilla fallback: thermal hints, not a fixed pick

`VanillaFallback.find` used to return one recipe — whichever of `smelting`/`blasting`/`smoking` had shortest declared cooking time, decided once. Made ore's blast-furnace time win in every vessel permanently, including plain Furnace that can never reach blasting conditions — exactly hidden-recipe-rule `docs/tpu_spec_doc.md`'s "Do Not Let the Shortest Vanilla Recipe Win" section calls out, and exactly what §15's "specialisation is emergent, not enforced" already promised not to do.

`find` now returns `Match(recipe, suitability)`, recomputed every tick against vessel's *current* temperature: `smoking` maps to food-like low/stable profile (`FTuning.SMOKING_OPTIMAL_TU`, ceiling at `FOOD_MAX_TU`), `blasting` to hot metallurgical profile (`BLASTING_OPTIMAL_TU`, no ceiling), plain `smelting` to generic middle profile (`SMELTING_OPTIMAL_TU`) — three curves through same `ThermalProcess.suitability` math hand-authored process uses, never per-vessel identity check. Ore now finishes faster in vessel actually hot, slower in one that isn't, without either block knowing what other is.

Recipe's own declared cooking time is `required_tpu` baseline directly (no separate Work calibration constant), per source doc's "convert vanilla cooking time into baseline TPu" — also retires `FTuning.FALLBACK_WORK_PER_200_TICKS` and `Tu × ticks`-as-Work arithmetic it fed, the exact "fake Work" pattern source doc opens by rejecting.

---

## 4. Alloying

Built, not speculative. Prior art: TerraFirmaCraft's `AlloyRange`/`AlloyRecipe`/`FluidAlloy` (EUPL-1.2, read per standing rule); see `THIRD-PARTY-LICENSES.md`'s TFC section for exactly what taken (ratio-window shape) and not (an "unknown alloy" fallback fluid — see §4.3).

### 4.1 The crucible's tank was never the source of truth to begin with

Before, `CrucibleBlockEntity` held one `FluidTank`, filled directly from completed melt. Pouring second, different molten metal into occupied tank simply failed — vanilla `FluidTank` refuses fluid not matching what it holds — accidental block on mixing, not designed one.

`AlloyMix` (`process/AlloyMix.java`) replaces that: plain `Map<Fluid, Integer>` ledger of every metal actually poured in, held by crucible alongside tank. Tank becomes *view* computed by `AlloyMix#resolve` — matched alloy's fluid if ratio resolves to one (`AlloyTable#find`), single metal itself if only one ever poured (unalloyed melt behaves exactly as before), or empty.

### 4.2 The recipe: a composition window, not a fixed ratio

`AlloyRecipe` (`process/AlloyRecipe.java`, loaded from `data/feedback/alloy/*.json` by `AlloyTable`) is list of `AlloyRange`s — one per metal, each a `[min, max]` fraction of whole melt. Bronze is copper 88–92% + tin 8–12%; brass is copper 60–70% + zinc 30–40% (`alloy/bronze.json`, `alloy/brass.json`). Mix matches only if it holds *exactly* metals recipe names — no unlisted metal tolerated — and every share falls in range.

Same shape as §7's control envelope describes for temperature (band with optimum), applied to composition — not new kind of continuous knob, because player never dials ratio directly. They pour known-sized ladles of known metals, ratio falls out of how much of each used, same way crucible's temperature falls out of fire and mass rather than dial.

### 4.3 What happens when the ratio is wrong

`AlloyMix#resolve` returns empty. Metal not lost — still tracked in ledger — but isn't anything with name, so nothing can be poured out until ratio corrected. TFC's answer is generic "unknown alloy" fluid with interpolated stats; Feedback doesn't, because giving unnamed mix real stats would mean inventing fifth material roster doesn't have, where reporting nothing needs nothing new.

**[OPEN]** Currently no way to *correct* bad mix short of building new crucible — no partial extraction, no adding more of one metal to dilute other back into range without also adding more total volume. Widen once real process wants it; not solved speculatively here.

### 4.4 Extraction has to stay in step

Mold draining crucible's tank (`MoldItem#onItemUseFirst`) mutates tank directly, would leave `AlloyMix` reporting stale, too-high total on next melt. `MoltenVessel` gained `default void onDrained(FluidStack)` hook for exactly this; `CrucibleBlockEntity` overrides it to call `AlloyMix#remove`, shrinking every metal present by same proportion — no such thing as draining "just the tin" — keeping ratio, hence what tank shows next, correct.

### 4.5 Cast iron: not an alloy, but the other half of this pass

Steelmaking's already-verified recipe (`thermal_process/steel_ingot.json`, `minecraft:iron_ingot` + `minecraft:charcoal`) untouched. Alongside it, `thermal_process/cast_iron.json` (`iron_ingot` + `coal` → `feedback:cast_iron`, wider and lower band) and `thermal_process/steel_from_cast_iron.json` (`cast_iron` + `charcoal` → `steel_ingot`, same band as original, lower `required_tpu`) add second route: one extra step up front, less time in second. Two ways to same material costing different things — §5's kind of trade, placed by hand here rather than found emergent, which philosophy explicitly allows.

---

## 5. Materials and Form Steps

Built. Replaces per-recipe `deformation/` and `removal/` tables; prior art in
`THIRD-PARTY-LICENSES.md` (GregTech's material/prefix model, shape only).

### 5.1 What is stored

- **Material** (`data/feedback/material/*.json`): `hardness`, optional `min_temperature`/`max_temperature` (hot-working window, deformation only), `density`, and `forms`, map from form name to item that is this material in that form. Item id appears here and nowhere else. Everything else optional, unread so far, listed in §5.6.
- **Form step** (`data/feedback/form/*.json`): `[from form, to form, operation]` plus `base` and `slope`. Cost in Fu for material is `base + slope * hardness`, rounded, at least 1.

`Deformation` or `Removal` is *compiled* from every material having both forms of a step (`FormTable`). Machines, sync, JEI read compiled lists through `DeformationTable`/`RemovalTable`, don't know recipes not authored. Removal sheds material's `swarf` form, if it has one.

### 5.2 Why affine, not `k * hardness`

Blow lands `St / hardness` Fu, so blows needed go as `work * hardness / St`. With `work = k * hardness` that is hardness squared: steel at 15 would need 225 times copper's blows for step it should need about four times work for. `base` absorbs part of step's cost that doesn't grow with hardness. Existing figures fit closely: steel/copper cost was 4.3x for plate, 4.0x for rod, 3.75x for wire.

### 5.3 State changes are material changes

Hardened steel is 25 hardness against steel's 15, removal reads hardness from material. Form cannot override it; hardened steel is own material and quench maps one material's item to other's. If something later needs hardness to vary within material, sign material is really two.

**Tempered steel has forms; hardened steel does not.** Hardened steel glass-brittle (toughness 8, under own hardness): anything that could move it cracks it, so comes as ingot and ground ingot and nothing else. Tempering buys toughness back (40), and what tempered steel is *for* -- springs, blades, tools, moving parts of gun -- is shapes. So takes every cold-working and machined form part needs: plate (spring strip, blade stock), rod and wire (spring wire drawn and coiled after tempering in reality too), and drilled, turned, ground forms and gun parts following from those. No hot-working window, because heating to forging heat would undo temper; at hardness 20 it is cold-worked, taking more force than short-throw crank delivers -- gear train or better machine, not lock. Left out: foil, fine wire and scrap (two cold steps of ductility doesn't reach them), nugget and block (storage forms are for remelting, remelting undoes temper), case and bullet (brass and lead jobs), relief vent (made from nugget), grip (grip is for insulating).

**Iron, wrought iron, cast iron, steel.** `material/iron.json` is **wrought iron** in everything but id, and display name says so (2026-10-20); id stays `iron`, vanilla's items keep names. Vanilla's iron is bloomery product -- smelted below iron's melting point, hammered, never poured -- which is wrought iron: soft (hardness 6), tough, low carbon, cold-workable, generic structural metal. *Cast iron* (`material/cast_iron.json`) is high-carbon one, iron fully melted with coal: pours well, hard and stiff under compression, brittle under blow, so not forged. Today only job is shorter road to steel (§4.5). Real niche still to come: **cookware and castings** -- pots, pans, machine beds and frames, anything poured once to shape and loaded in compression rather than struck. *Steel* sits between: carbon enough to harden on quench (§5.3), little enough to forge hot. Trap worth naming: calling soft metal plain "iron" invites player to expect cast iron's hardness from it, or wrought iron's toughness from cast pot.

### 5.4 Generated items

Which forms generated is declared, not inferred: `data/feedback/form_type/<form>.json` names `kinds` a form is generated for (every one today `["metal"]`), and `block: true` makes it placed storage block rather than item. Material gets every form type whose kinds include its own, unless it lists explicit item for it (`forms`) or excludes it (`exclude`). Absence is the stated thing, so new form type reaches every metal without editing any; cost: metal with nonsense form (hardened steel plate, cast iron wire) has to say so.

**Any other form name is an association.** Material's `forms` may name anything made of it under any name no form type declares -- iron's `horse_armor`, sculk's `sensor`, sixteen `white`...`black` for wool. Those generate nothing; exist so `Material.find` can say what item is made of (Material Probe uses it). One item per name, one material per item.

`FormItems` reads jar's own `material/` and `form_type/` files at registration because items exist before any datapack does, registers `<ns>:<material>_<form>` as `FormItem` (or `FormBlock` and its item), named by pattern (`form.feedback.plate` = `%s Plate`, `form.feedback.block` = `Block of %s`) so new item costs only its material's name. Form with no registered item skipped at compile, never error. Item models come from `FormItemModels` (every one points at its form's grey sprite); block states, block loot, pickaxe tag and nine-to-one nugget/ingot/block recipes from `FormData` (`runData`). Lang stays hand-authored because generated `en_us.json` would shadow it at same path.

**First non-metal form type** is `grip` (2026-10-14): `["metal", "organic"]`, seven organics excluding it. Seven excludes against three real grips is cost of "absence is the stated thing" when kind mostly nonsense for form; if second organic form arrives with same ratio, opt-in list on form type is cheaper statement.

**Trap:** generation makes dead ends. Copper swarf exists, no remelt in `thermal_process/`; steel foil exists, nothing uses it. Generating free, giving each purpose is not -- document 3's job.

### 5.5 Debts

- **[OPEN]** Single-material steps (`wire_to_fine_wire`, `plate_to_foil`, `foil_to_scrap`, every removal) have `base`/`slope` reproducing copper's or steel's old number, not a fit. Free to move; any second material gaining the forms is first real test of pair.
- Explicit per-recipe overrides deliberately not kept. If pair genuinely can't be expressed as function of hardness, restore table rather than bend function.

### 5.6 The property table, ahead of its readers

Every vanilla substance and Feedback's own metals (tin, lead, zinc, bronze, brass, cast iron) have material file now -- metals, crystals, stones, ceramics, granular stuff, organics, and §12's exotics -- each with figures below wherever it has one. **Nothing reads them yet** except `density` (engine, plate and tube mass) and `yield_strength` (engine rev limit). Entered in one pass so next system needing figure finds it already there for sixty materials, rather than adding one field to three files and leaving rest blank. The `Instrument` bet again: high fixed cost once, cheap per feature after.

Numbers are placeholders (philosophy §19). Method: real-world values where reality has one, rounded, Tu read as degrees Celsius -- tin, lead, zinc already melt at real figures, lava at 1200 Tu is stone's melting point, so lava *is* molten stone. Where existing table disagrees with reality (copper melts at 1000 Tu, gold at 950, iron at 1600), table wins and material agrees with it. Absent means unmeasured, never zero.

**Qualitative:** `kind` -- one of metal, mineral, crystal, ceramic, glass, granular, organic, exotic -- is adjective player has before owning instrument (philosophy §8). `traits` are yes/no observations person could make by handling: alloy, flammable, fireproof, transparent, luminous, fibrous, porous, sticky. Neither is identity (§11): two materials can share both.

| Group | Field | Unit | Waiting for |
| --- | --- | --- | --- |
| `thermal` | `melting_point` | Tu | one figure in place of each melt recipe restating it |
| | `boiling_point` | Tu | distillation (zinc out of brass) |
| | `cooling_rate` | Tu/t | per-material `ItemHeat` cooling -- §2.2's open item; blaze rod is 0.01 |
| | `conductivity` | Conductance (one-block wall; copper = the heat exchanger's 0.1) | vessel walls, exchangers, engine cooling |
| | `oxidation_temperature` | Tu | scale loss on hot metal, burning for organics -- an overrun hook |
| | `anneal_temperature` | Tu | resetting work hardening |
| | `service_temperature` | Tu | machine body of it softening and wearing when run too hot |
| `mechanical` | `toughness` | St | one blow above it cracks piece -- hardened steel's 8 sits between crank's 3 and 12 |
| | `ductility` | cold steps | over-drawn wire cracking: overrun on finished product |
| | `work_hardening` | St per cold step | work hot, or work cold and anneal -- two answers |
| | `yield_strength` | St | frames, shafts, engine limits |
| | `friction` | coefficient | brakes, clutches, bearings, tires; ice and blue ice already differ |
| | `abrasiveness` | 0-1 | grinding wheel grit wear |
| | `hardenable` | bool | whether quench hardens it (steel, and glass -- tempered glass) |
| `molten` | `viscosity` | × water | mold misruns |
| | `density` | Mu/mB | slag floating off melt |
| `separation` | `magnetic` | bool | magnetic drum |
| | `curie_temperature` | Tu | hot iron passes drum |
| | `reflectivity` | 0-1 | optical sorter, fooled by lookalikes on purpose |
| top level | `ampacity` | × copper | wire melting under load -- ratio because resistance not modelled |
| | `corrosion_resistance` | 0-1 | corrosion from water or acid -- misuse only, never time |
| | `color` | `"#rrggbb"` | tint of its parts, forms and cables -- art, but pack changes it in material's file. Absent: client averages texture of its ingot or block item (`textures/item/<path>.png`, else `textures/block/`), else kind's grey. Server never draws it |
| | `composition` | material to share | read: what alloy dissolves into in electrolytic cell (§6.14) |

**Netherite is magnetic, keeps it hot.** Vanilla makes lodestone out of it, and §2 says what vanilla does is evidence. Curie point of 1500 Tu against iron's 770 is exploitable difference: hot drum separates them.

- **[OPEN]** `viscosity` recorded although philosophy §3 names it as not yet earning place. Costs nothing unread; earns reader only if misrun is decision.
- **[OPEN]** Specific heat **not** added. TFC's per-material heat capacity declined once (`TODO.md`), would need new unit (Work per Tu per Mu).
- **[OPEN]** Elastic modulus and fatigue considered and left out: modulus fails §3's test, fatigue is wear from uptime, which wear rule forbids.
- **`parent`.** Material file may name another (`"parent": "steel"`) and write only what differs. Objects merge key by key down chain; number or list replaces parent's whole value (so `exclude` restated, not appended); `null` removes inherited key (tempered steel drops steel's hot-working window and `hardenable` that way). `MaterialTable.resolve`.
- Water and lava have no material file: `hardness` required, liquid has none. Lava already heat source; if liquid needs figures, make hardness optional first. *Bucket* of lava is form of stone (§5.7), way powder snow bucket is form of snow.
- Every metal, vanilla and Feedback's own, now grows full form set; nothing gives most purpose yet (§5.4 trap, at scale). Art is one greyscale sprite per form (`textures/item/form/<form>.png`, `textures/block/form/block.png`) tinted per material by `client/FormTint` from `MaterialColors` -- generated forms, storage blocks and feedback-namespace items material's `forms` names. Vanilla's own items (iron, copper, gold ingots) keep vanilla's sprite. New form type needs sprite and entry in `FormTint.SPRITE_FORMS`; new material needs only `MaterialColors` entry (unlisted take kind's colour).

### 5.7 Combustion: one block, two fires

**Built 2026-10-20.** Fuel was own datapack table, `fuel/*.json`, rows of `{ingredient, duration, temperature, spread}`; gun propellant was `propellant` block in material file. Both gone into one `combustion` block in `material/*.json`. Item burns because its material does: item, to its material by form (`Material.find`, existing route), to that material's combustion. Nothing names an item.

**Why the split existed.** Fuel table came first (slice 1), before material table, and ingredient row was obvious shape -- TFC's, and the one every other table already had. Propellant came after materials, so went where materials were. Nobody decided they were different things; written in different months. Cost was real: ingredient row is identity lookup wearing a datapack (philosophy 11), modded coal had to be registered twice to burn in firebox and in case, and "what is coal" had two answers.

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

Material with `flame_temperature` and `burn_ticks` is fuel; one with `chamber` is propellant; one can be both. Firebox and self-fired vessel read open half (`process/Fuel`, now resolved figure rather than table row); gun reads confined half (`Material.propellant()`, view gun code already read, so nothing in `weapon/` changed). Ignition point in neither half: it is `thermal.oxidation_temperature`, which table already stated as *above this, held in air, it burns*. Cook-off and fire catching are same temperature.

**A piece, and how much stuff a form is.** `burn_ticks` is per piece of base form; `Fuel.pieces` scales rest by crafting yields: block is nine (coal block still 21600 t), plank a quarter of log, stick half a plank, ore is mostly rock and does not burn as itself. Before, log made four planks and twice its own burn, and anything made of wood would have burned as whole log.

**Lava is not combustion.** Burns nothing, runs out of nothing: body at fixed temperature giving up latent heat as it freezes. So lava bucket is stone's `molten_bucket` form, and stone's `molten.freeze_ticks` (20000) says how long bucket holds stone's `melting_point` (1200 Tu -- §5.6 already says lava is molten stone) in fire, no spread, because phase change not noisy. Giving it flame temperature would have said lava is fuel that happens to be orange, and let pack give it oxygen balance. Lava in world unchanged (`HeatSource`, `FTuning.LAVA_TU`).

**Redstone is not combustion either.** Coil gun's charge is field pulling slug, so own `coil` block (`field_pressure`, `quench_temperature`). `Material.propellant()` presents both as one view gun reads, only place they meet.

**Numbers that changed.** Coal, coal block, charcoal, blaze rod, logs and lava bucket exactly as before, so is every gun figure.
- Planks: 300 t at 700 Tu, now 150 t at 760 Tu (quarter log, wood has one flame).
- Blaze powder's oxidation temperature: 200, now 220 Tu, so cook-off stays old ignition figure. Gunpowder's two already agreed (300); nothing else read blaze powder's.
- Newly burn as wood, at crafting share: stripped logs, wood and stripped wood blocks (now wood's forms; before, burned through `logs` tag), sticks, wooden tools, bows, crossbows, shields.
- No longer burn: crimson and warped stems and planks (nether wood not flammable, as in vanilla), bamboo planks, modded logs and planks only `logs`/`planks` tags reached.

**[OPEN]**
- **Compat regression.** Modded log burned through `#minecraft:logs`; now burns only if material lists it. Fix is general: let material's forms name a tag, so `Material.find` -- not only fuel -- reaches modded items. Not done here.
- `burn_ticks` and chamber's `burn_rate` are same property (how fast it burns) in two confinements. Deriving one from other needs constant tying firebox's ticks to barrel's metres, and no material has both figures today, so nothing to fit. When one does (blaze powder below), try it.
- §2.3 wants blaze *powder* to be extreme fuel and rod heat store never seeing firebox. Table has always burned rod (1400 Tu, 2400 t), migration keeps it. Moving that `combustion` from `blaze_rod.json` to `blaze_powder.json`, short burn and wide spread, now data edit -- and makes powder fuel and propellant from one block.
- `oxygen_balance` read by case only. Firebox could read it: fuel short of oxygen needing more bellows is same fact.
- Bamboo has no combustion: stalk is base piece and planks not a quarter of one, so needs own yields.

### 5.8 Asking about stuff in a recipe: the material ingredient

**Built 2026-10-20.** Recipe system can name item, tag or component, none can say "any wire carrying at least this much". `feedback:material` (`process/MaterialIngredient`, NeoForge `ICustomIngredient`) is that question:

```
{ "type": "feedback:material", "form": "wire", "property": "ampacity", "min": 0.5 }
```

`form` (item is that form of some material) or `tag` (each item of tag must be some material's) names stuff; `property` with `min` and/or `max` bounds figure from material's file. `MaterialProperty` is closed list of figures recipe may ask about (ampacity, hardness, density, melting point, toughness, yield strength): each is accessor machines already use, absent means unmeasured, so material without figure matches no range. `getItems()` enumerates matching stacks from table so recipe book and JEI cycle through them; `describe()` is sentence JEI slot tooltip says. Reads server's table where one loaded, client's copy otherwise. Client's copy in place before its recipes because `PlayerList` posts `OnDatapackSyncEvent`, which sends material table, just ahead of recipe packet, on login and `/reload`; `Ingredient` caches `getItems()`, so other order would leave client's ingredients empty for good.

**Trap:** recipe cannot copy ingredient's material into output. Anything that must (the cable) is `CustomRecipe` using ingredient's `test` and `Material.find`, as `TongsRecipe` does.

---

## 6. Electricity (`Eu`)

**Built in part (slice 4, 2026-10-12): see §6.13 for exactly what.** Written speculatively, ahead of code, like §1; where §6.13 and paragraph above disagree, §6.13 describes code. Everything stated flatly is **user ruling of 2026-10-12**, recorded as decided. Everything marked `[OPEN]` is not. `feedback_philosophy.md` §10 (energy is plural) and §17 (`Eu` is electrical, not scalar) are authority; this section is how implemented. Prior art is GregTech CEu Modern's electricity (LGPL-3.0, read for design only; see §6.9 and `THIRD-PARTY-LICENSES.md`).

### 6.1 What `Eu` is

`Eu` is **voltage and current**, two components, no arithmetic on type (`core/unit/` rule: plumbing typed, physics unwraps inline). Two `Eu` with same product not interchangeable: cell and stepped-up line can carry same power and be useless to each other's loads.

- **DC only. Frequency not modelled.** Tentative. If AC ever wanted (piezo resonator, oscillator crystal in roadmap Slice 8) frequency is **third component**. Type widens by one record field; datapack form stays compatible if missing frequency reads as "DC", way missing optional field already reads as absent elsewhere (§5). Nothing here should assume exactly two components outside `Eu` record itself.
- **Resistance still not modelled** (§17, "what is deliberately absent"). §6.4's per-block loss gets decision resistance would give without the variable, which is how it passes "does this create a meaningful choice" test.
- **Cost in a process is threshold plus rate, never bare `EU/t`.** Process needing electricity says *"at least this voltage; this much current is how fast it goes"* (electrolysis: decomposition potential, then current sets throughput). No `Eu`-denominated recipe cost. See §6.8.

**Decided (2026-10-12): `Eu` record's components are `voltage` and `current`.** `voltage` is **band** (§6.3), `current` is **integer count of amps**, like `Fu`. §6.4's drop-per-block arithmetic works on integer amps and band's own reference voltage, so type needs no float. Lang symbols, as built: unit `Eu` (`3 Eu at LV`), plain band names; `raw()` is current and display goes through `Eu.figure()`. **[OPEN]** whether those final.

### 6.2 Per-packet: any voltage may be on a wire, each sink checks

**Decided.** Wire does not have "a voltage". Sources put `(V, I)` on it; each sink looks at what arrives and checks against own rating. Model making transformer a real object (step between two different voltages on two runs, trading V for I at constant product) and making "I wired 12-volt thing to the line" a mistake player can actually make.

Alternative, one voltage per connected run with rotation-style ledger, was simpler and consistent with `Su`, and **not chosen**. Remains fallback if open items below prove unsolvable without it.

GTCEu study's objection to per-packet routing was real and is what this section has to answer: in GTCEu far sink starved by nearer ones is **invisible and unconfigurable**, no answer to "which source fed this", no brownout, only full voltage or nothing. Routing and resolution decided below ("Routing and resolution"). Constraint on them, which they were chosen to meet: **every failure of supply must show on the thing that failed.**

#### How failure stays visible

- **Every sink carries a reading.** Electrical consumer is a `DataNode` reporting voltage seen, current delivered, current wanted. Starved sink is sink whose delivered current is below wanted, galvanometer (§6.7) or any tooltip reads exactly that. Nothing hidden in ordering player cannot see.
- **Under-supply is slower, not off** (§6.5), so starved machine is *visibly* slow. Symptom on machine, cause on wire.
- **Brownout is distance.** Delivered voltage falls by conductor's drop per block per amp (§6.4), so long, thin or overloaded run reads as gradual sag at far end, never cliff. The brownout GTCEu's all-or-nothing delivery lacks.
- **Over-voltage is evidence.** Condition loss and heat on machine (§6.5) are symptom, in same currency every other overrun uses.
- **A source shows what it is being asked for.** Its node reports offered and drawn current. Idling source shows idling; source asked for more than rating shows that too (source sag, below).

#### Routing and resolution

**Decided (2026-10-12), on performance grounds.** Per-packet routing evaluated per tick per wire block per sink would be expensive way, so work moved off tick:

- **Run-level resolver**, in `RotationPropagator` pattern: when topology changes (wire placed or broken, source or sink added or removed), rebuild connected run once by flood fill and **cache sink list per run**, with each sink's path drop (§6.4). Cache invalidated only by topology change; per tick resolver reads cache and does arithmetic, no search.
- **Sinks take proportional share** of what run's sources supply. Each sink's share is its wanted current over total wanted current; when supply covers demand every sink gets what it wants, when not, every shortfall shared and readable on every sink (no nearest-first starvation).
- **Per-packet voltage model stays.** Sharing divides *current*; each sink still checks voltage *it sees* (offered voltage less path drop) against own rating (§6.5). Transformer still real object.
- *Fallback, kept:* one voltage per connected run with rotation-style ledger, if sub-questions below prove unsolvable. Not needed so far.

**Mixed-voltage runs.** Best resolution: **sources of different voltage on one run do not blend.** Sink sees highest source voltage present on its run (less path drop); lower-voltage source on that run is *blocked* (supplies nothing to that sink, node reports "blocked", so mistake shows on thing that is wrong). Nothing back-fed into lower source. Keeps "I put a cell on the line" visible mistake, matches how per-packet model already treats mismatch as consequence, not lock.

**Source sag under over-draw.** Best resolution: source drawn past rated current **sags in voltage** (non-destructive overload character, as real cell does through internal resistance) and reports it on own node; downstream sinks then see lower voltage and slow (§6.5). Source never destroyed by over-draw.

**[OPEN]** Shape of sag curve (how fast voltage falls per amp over rating) and whether source's current cap is hard or only the sag itself. **[OPEN]** Whether blocked lower-voltage source should instead be pulled to shared voltage when no higher source actually delivering to that sink. **[OPEN]** How many amps one sink may take from one wire at once.

### 6.3 Voltage bands

**Decided (2026-10-12): four named bands, `LV`, `MV`, `HV`, `UHV`, closed list.** Small, geometrically spaced set, never free scalar (§3). Making list data-extensible deferred, not refused. **Voltage belongs to the source**, and **bands gate nothing by themselves**: band is rating source and conductor's insulation have, not machine "tier" doubling as progression gate (§6.9). "Needs band N" is exactly hard-gate-by-tier shape `CLAUDE.md` rules out; mismatch is consequence (§6.5), not lock.

**Considered and replaced:** physical-place naming proposal (*Cell*, *Bench*, *Line*, *Arc*) anchored to galvanic cell, shaft dynamo, stepped-up transmission, spark gap. Tried to keep names from drifting into tier labels and make point that voltage and current are separate axes (electrostatic generator is top band by voltage and nearly nothing by current, whole reason `Eu` has two components). User chose plain band names instead; second point still stands.

**Physical anchors, partly fallen out (2026-10-13).** Chemical cell is LV (zinc against copper is 1.1 V). Dynamo has no band of its own: reaches whatever shaft speed times winding makes, windings placed so steam engine at working speed gives LV on coarse and MV on fine. Hand crank is faster than engine, so cranked fine dynamo reaches past MV: speed, not source type, sets voltage. **[OPEN]** what roster has to occupy at HV and UHV. **[OPEN]** Cells in series add voltage one-to-one, free scalar again (1 to N cells). Either stack capped to named bands or cells come as pre-built packs per band. Not decided.

### 6.4 Conductors

Conductor is **material times form**, like everything else in §5. Three figures per conductor, from `material/*.json` (§5.6's table already carries `ampacity`; figures below extend it):

- **Current rating**: amps conductor carries before it heats. Today's `ampacity` field, ratio to copper, is existing form.
- **Voltage rating**: how much insulation layer withstands, per band.
- **Loss: voltage drop per block per amp. Decided: material property in `material/*.json`.** Path's drop is sum over its blocks of `drop × current`. Long run at low voltage delivers little or nothing; same power at higher voltage carries smaller current and loses less. Whole reason voltage lever exists, obtained with no resistance variable: nobody can tell it is not `I²R`, and it plays right. *Shape* is GTCEu's. Path whose summed drop reaches offered voltage delivers zero, legible failure ("wire too long for this voltage"), not hidden one.

**Wire is a `ThermalBody`.** Current over rating is heat input; existing `Heat`/`Conductance` model is whole mechanism, no new simulator. Wire inside rating never heats, so wire never wears from uptime. Rule machine shop already runs on, applied to cable.

**Failure is staged and every stage readable:**

1. Insulation fails first. Separate physical layer (component you can point at) burning off above its limit. Bare conductor still conducts; also now **shorts to neighbours and shocks**, the new consequence.
2. Conductor degrades under continued overload: condition falls, rating and drop worsen with it.
3. **Left long enough, conductor is destroyed.** User's ruling: *destruction is the consequence of not fixing degradation.* Never instant, never surprise; end of visible path player was walking.

**Decided (2026-10-12): destruction exists for conductors and nothing else.** `CLAUDE.md`'s wear rule says machine "degrades and never breaks, because a machine refusing to run is a hard gate where no physical impossibility exists." Step 3 is wire breaking, user ruled it stands: rule scoped to **machines**, stays exactly as written for them (§6.5: machine never destroyed, whatever you feed it), and **conductors are the one place destruction exists**. Reasoning, in user's words: *destruction is a consequence of not fixing degradation.* Conductor reached only through misuse (over-rating), never uptime, never instantly; rule's reason (don't turn graded consequence into cliff) kept because cliff has two readable steps in front of it. `CLAUDE.md` updated to say so. **Widened 2026-10-14 (user ruling):** pressure holders destructible too -- see §7.3. Rule's reason survives: nothing destroyed by small mistake or by uptime, and boiler weeps scalding steam before it lets go.

**Built (2026-10-14): both protective parts** (`machine/breaker/`). Open one is member that *does not conduct*: `ElectricMember.conducting()`, read by propagator's `connects`, so open breaker or blown fuse sits in run of its own and line through it is two runs; change calls `ElectricPropagator.reconnect`, rebuilding it and neighbours.
- **Breaker**: wire while closed, `Switchable`, so hand or controller opens it. Opening while current flows costs condition per amp broken (the arc), pitted contacts lose more volts per amp, so controller flapping it under load with no deadband shows on far side as sag (philosophy 13). Trips itself above copper cable's rating, which is opening under load and charged like one. Never broken; keeps switching at its floor.
- **Fuse**: holder for one fine wire of any metal, rewired by hand. Both its figures are wire's metal: rating is its `ampacity` times `FUSE_AMPS_PER_AMPACITY`, over it wire warms as cable does (ten times as fast, thin wire) until it passes its metal's `melting_point` and is gone. Tin blows low and fast, copper high and slow. Holder never wears; wire is spent.

**Protective parts.** **Fuse** allowed as consumable: sacrificial element opening on over-current and spent doing so. Reusable **breaker** also exists and **arcs and wears under switching load** (philosophy §10: electrical stops nearly instantly, hardware wears cutting load). Breaker's wear is wear from specific act, already fits wear rule. **Fuse is third accepted wear-with-correct-use exception (user ruling 2026-10-12)**, alongside Grinding Wheel's grit and Hand Hammer's durability: fuse is consumable by nature, sacrificed to protect what it guards. Reusable breaker is **not** exception: wears from specific act, switching under load, like any misuse. Exception is **narrow**, same shape as other two, does not extend to machine bodies. Recorded in `CLAUDE.md`.

### 6.5 Sinks: under-voltage, over-voltage

**Under-voltage slows; does not stop.** For general machine, delivered voltage below rating reduces rate, so long run or weak source makes machine slow rather than dead. "Recipe not locked, process difficult" applied to electricity. **Exception is genuine physical threshold**: electrolytic cell below decomposition potential does nothing, because no current at too low potential decomposes electrolyte; arc will not strike below breakdown; magnet saturates. Those are hard gates in §7 sense, only those.

**Decided (2026-10-12): rate floor is zero.** Under-voltage can slow machine to standstill, never wear it; slow is whole consequence. **[OPEN]** Shape of slow-down itself: linear in delivered voltage, in current, or in their product.

**Over-voltage goes into machine.** Voltage in excess of sink's rating is force that cannot go into work, wear rule says where such force goes: **condition loss, plus heat into machine's own `ThermalBody`, scaled by excess and by duration.** Machine **never destroyed, never explodes**. Worn machine is evidence of specific wiring mistake, exactly what wear rule is for.

**Decided (2026-10-12): machine never allowed to refuse a line.** Always runs, always wears. Refusal would be hard gate where no physical impossibility exists, which wear rule written to avoid; consequence of wrong wiring is wear, no lock. **[OPEN]** Per-unit scale (condition per volt-over per second, heat per same).

### 6.6 Storage

**Decided: for now only Leyden-jar-style capacitor and chemical cell.** **No generic energy-bank block**: unbounded bank is universal jar, "energy in a box with a number on it", the smell §6.9 describes.

- **Capacitor** (Leyden jar): holds charge at voltage and gives it back in *burst*; leaks. Character is smoothing and bursting, not holding. Also storage making pulse source (electrostatic machine, lightning capture) usable at all, pleasing parallel philosophy §10 already draws between real electrical infrastructure and buffering.
- **Chemical cell**: stores chemically, leaks chemically. Also continuous source in Slice 4; same block is both, as real cell is.

Machines hold **no** hidden stored-`Eu` buffer. Sink needing smoothing is sink with capacitor *in front of it, as block player placed*.

### 6.7 Sensing

**Decided.** Wire carries `DataNode` reading `(V, I)` at that block. **Galvanometer is fitting on wire** (`fitting/`: `Sensor`, sided, read-only), not separate network; follows `TemperatureSensorFitting` precedent exactly. Same reading is what sink's status (§6.2) reports. Electroscope reads potential only and coarsely (no current), as hand-carried `Instrument`: tells you wire is live and roughly how hard, not how much flowing. Per §8, none enter calculation; bad reading costs reproducibility, never success.

### 6.8 Where `Eu` enters and leaves

**Decided: one physical block per boundary.** Electricity crosses into or out of another energy system at one block you could point at, crosses at *local* physical relationship, never through global exchange rate.

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

How boundary block's figures set without constant. Dynamo's output voltage and current must come from own construction and shaft it is on (speed and torque it sees), motor's torque from current delivered, in each case as that block's physics, not table saying "N `Su` per `Eu`". Thing to resist: fixed exchange constant between `Eu` and anything is universal currency arriving by side door.

**Decided (2026-10-13) for dynamo: speed makes volts, current costs torque, by one winding figure.** Real generator's back-EMF constant and torque constant are same number (`k_e = k_t`), so dynamo has one figure per winding, `voltsPerRpm`: voltage is input shaft's live RPM times it, every amp drawn costs that shaft same figure in `Su`. Conservation stated once inside one block, not exchange rate: dynamo nobody draws from costs shaft nothing, slow river makes low voltage, gearing up is how to raise it (paying gearing's friction). Winding is discrete choice (§3): **coarse** (few volts per RPM, many amps) or **fine** (reverse), same power, so neither dominates. Trap worth recording: obvious first version is "dynamo makes X `Eu` per `Su`", reads as harmless bookkeeping and is exactly side-door currency above. Motor (`Eu` out to shaft) is same winding figure run backwards (§6.13).

**Decided (2026-10-14): voltage converted by motor-generator set, not transformer.** `Eu` is DC (§6.1) and real transformer needs AC, so block trading volts for amps at constant product would be invented physics. DC era's own answer: motor driving shaft driving dynamo on other winding, here no new physics at all: motor (owed, dynamo reversed), shaft, and dynamo that exists, losses being shaft's friction and gearing's that already exist, winding the discrete choice it already is. Lets high-current, low-voltage load (§6.14) sit far from source: send at MV, step down beside load. AC and static transformer stay possible later (frequency as third `Eu` component); motor-generator set would keep niche (needs no AC). §6.9's "transformer as discrete step" superseded by this for DC.

### 6.9 Prior art: GregTech CEu Modern

Read for design; LGPL-3.0, cleared; study recorded in `THIRD-PARTY-LICENSES.md`'s GregTech section. Written from scratch.

**Taken as design:** voltage as small closed set of named, geometrically spaced steps; energy as `(voltage, current)` pair rather than scalar; loss as per-block voltage drop per amp, making distance a cost and voltage a lever without resistance variable; conductor with own temperature and excess-load heat input; insulation as separate layer failing before conductor; transformer as discrete step with V and I trading at constant product (later slice, not free: `ThermalBody` that wears under overcurrent); caching route until topology changes.

**Not taken, and why it matters:**

- **Tier as progression ladder.** Fifteen tiers, each machine and recipe keyed to one, makes tier count the content roadmap and "needs tier N" everywhere. `CLAUDE.md`: "If a proposal needs 'requires tier N,' it's wrong." Band here is rating, about four of them.
- **Electricity as universal currency.** In GTCEu every recipe denominated in `EU/t`, steam is EU payment method at fixed mB per EU, generators output bare EU from fuel, overclocking keyed to tier, converter bridges rest of ecosystem. Feedback refuses all five. Philosophy §10 is reason: Mechanical, Thermal, Chemical are natively consumable currencies that never *have* to become electricity, electricity is dominant *transport*, not universal *consumption*. Concretely: no `Eu` capability on machines, no mB-per-`Eu` constant, no generator emitting bare `Eu` from fuel, no overclock-by-tier. Also what makes problems here have more than one answer (§5): process can be powered from energy that suits it.
- **Explosion on over-voltage** (§6.10).
- **Buffer in every machine.** See §6.10.
- **Push routing in nearest-first order** as the *model*. Per-packet voltage kept; nearest-first starvation and its invisibility not (§6.2).
- **Item batteries as charging currency for tools.** Feedback's tools are mechanical and thermal.

### 6.10 What was tempting, and wrong

- **Explosion on over-voltage.** Unmistakable and teaches "match your voltage", why tempting. Wrong because instant, total, carries no information except "you plugged it in wrong", destroys machine and neighbours for plumbing mistake, has one correct answer (use transformer). Also breaks "degrades, never breaks" for machine. Replacement is wear whose size says *how* wrong and *for how long*, same lesson with gradient.
- **Hidden buffer in every machine.** Decouples tick order and removes need to think about smoothing, why GTCEu does it. Wrong because each buffer is stored-energy number player cannot see or place, sum of them is universal currency in jar. Smoothing is block (§6.6) or does not exist.
- **`EU`-denominated recipes.** Recipe says how much `EU/t` it costs, machine is function of tier. Tempting because makes every process comparable and balanceable on one axis. Wrong because that comparability is the currency; process here asks for *condition* (voltage over threshold) and *rate* (current), what powers it is local fact (§6.8).
- **Treating "insufficient voltage" as always hard gate.** Gate only where physics has threshold (§6.5). Everywhere else slow machine, keeping precision soft gate.
- **One voltage per run, for simplicity.** Not wrong, kept as fallback (§6.2); recorded because it is choice that would have made routing trivial and transformer meaningless.

### 6.11 Minimal scope, and what is deferred

**Slice 4** (roadmap, "Electricity Exists") is novelty-scale: player runs electrical experiments without grid. Needs:

- `Eu` record in `core/unit/`, with codecs and lang key per `Units` checklist, no arithmetic;
- one **cell source** (voltage from material pair, current capped small) and one **electrostatic pulse source** (high voltage, bursts);
- one **wire block**: material-derived current rating, voltage rating and drop per block, from `material/*.json`; no insulation layer yet if roster has only bare copper;
- one **resolver**, rotation-propagator style with cached sink list per run and proportional shares (§6.2);
- **galvanometer** (fitting on wire) and **electroscope** (hand-carried);
- **electromagnet**, as first consumer.

Single failure at this stage: over-rating warming wire's `ThermalBody`.

**Deferred:** transformers, capacitor and storage in general beyond cell, insulation layer and stripping, multi-conductor bundles, any generator fed by steam or shaft (Slice 6 onward), electroplating (Slice 5; electrolytic cell built, §6.14), overclocking of any kind (not taken at all), circuits and data electronics.

### 6.12 Open questions and debts

- **Resolved 2026-10-12:** component names (`voltage` band, `current` integer amps, §6.1); routing as run-level resolver with proportional shares (§6.2); four closed bands `LV`/`MV`/`HV`/`UHV` (§6.3); rate floor zero (§6.5); machine never refuses line (§6.5); wire destruction scoped to conductors and fuse as third exception (§6.4, in `CLAUDE.md`).
- **[OPEN]** Whether built lang symbols (`Eu`, plain band names) final. §6.1.
- **[OPEN]** Sag curve shape and source current cap; blocked-source handling; amps per sink per wire. §6.2.
- **[OPEN]** Band anchors at HV and UHV (cell and dynamo settled 2026-10-13); series-stack capping. §6.3.
- **[OPEN]** Shape of under-voltage slow-down. §6.5.
- **[OPEN]** Over-voltage per-unit scale. §6.5.
- **Resolved 2026-10-13** for dynamo: one winding figure, volts per RPM and Su per amp (§6.8). Motor built 2026-10-14 as that figure reversed (§6.13); coil 2026-10-14 by Joule's law (§6.16).

### 6.13 Build status

**Built: conductor and resolver.** `core/electric/` follows `core/rotation/`: `ElectricPropagator` floods run when node placed, broken or loaded, hands `ElectricRun` member list and adjacency list; `ElectricRun` builds routing tree from supplying sources and caches it, each tick is arithmetic over arrays. Tree rebuilt on topology change and also when *pool* of supplying sources changes under standing topology (pulse starting, source outbid), detected by comparing which sources pooled.

What tick does, as built: highest nominal voltage among sources offering anything sets run's voltage and those sources form pool; lower-voltage sources blocked and report it (§6.2 as written). Reachable sinks ask for current; if pool, up to `SOURCE_OVERDRAW_LIMIT` times its rating, cannot cover it every sink gets same fraction. Pool sags as drawn past rating, linearly to floor and never to zero (`ElectricRules.sagFactor`). Current accumulates from sinks back up tree and each wire block takes `dropPerAmp x current` off voltage through it; path dropping the lot delivers zero volts, never negative. **Current routed along each sink's shortest path to pool**, approximating how parallel supplies share wire, chosen for being deterministic and loop-free.

**Built: the wire.** `machine/cable/`. `ThermalBody`: nothing put in at or below rating, so wire inside rating never leaves ambient; above it, `FTuning.WIRE_OVERLOAD_WORK_PER_AMP` per amp over goes in through `Heat.warm`, leak is `Heat.tick`'s own. Settles where input equals leak, cannot run away. Insulation burns off above `WIRE_INSULATION_LIMIT_TU`, then conductor degrades above its material's `service_temperature` (rating falls, drop rises), then destroyed at zero condition. Bare conductor under load shocks what touches it. Figures come from material: rating from `ampacity` x `WIRE_AMPS_PER_AMPACITY`, drop from new `electrical.drop`. **One `cable` block serves every metal** (2026-10-20): material rides on item as `MadeOf` and lives in block entity, so new conductor is material file with `ampacity`, not registration line. It is `cable`, not `wire`, because `wire` is a form: `Material.find` reads item carrying `MadeOf` as form named by its id, and placed cable read as wire would be accepted by recipe making cable. `CableRecipe` (three wires of one conductor over three paper) is code, not JSON, because recipe cannot say "output is made of whatever went in"; tint, name and drop all follow stack's material.

**Divergences from text above.**
- Sink's current is `float` inside resolver; `Eu` type (band and whole amps) is typed seam for display, not what physics unwraps. Sink sees continuous voltage after drop, which no band names.
- Sources' nominal voltage is plain volts, band derived (`ElectricRules.bandOf`), because galvanic cell's voltage is difference of two electrode potentials, not a band.
- No voltage rating on wire yet (LV only), no insulation *item*: insulation is flag on cable, minimum letting stage 1 of §6.4 exist.

**Built: two sources.** *Galvanic cell* (`machine/cell/`) is given anode and cathode material and holds magnitude of difference of their `electrical.electrode_potential` (zinc against copper is 1.1 V), at `GALVANIC_CELL_AMPS`; never runs down (`[OPEN]`, `TODO.md`). *Electrostatic generator* (`machine/electrostatic/`) is pulse: using it is one turn of handle, putting `ELECTROSTATIC_VOLTS` at `ELECTROSTATIC_AMPS` on line for `ELECTROSTATIC_PULSE_TICKS` then nothing. Holds no charge. While pulsing it outbids lower-voltage cell on same line, blocked for exactly that long. Neither is conversion: nothing in mechanical or thermal networks feeds either.

**Built: sensing.** Every `ElectricNode` is `DataNode` and holds own reading: voltage at block, current through or delivered, current wanted (sinks), blocked (sources). Reading server-side and not synced to clients -- instrument composes its figure where it is true. **Galvanometer** exists twice, as §6.7 says: carried `GalvanometerItem` (current, to `GALVANOMETER_RESOLUTION_AMPS`, and on sink what was wanted beside what delivered) and `CurrentSensorFitting` bolted to cable face, sided `SensorFitting` exactly as `TemperatureSensorFitting` is, which controller card can be linked to. **Electroscope** carried, reads potential only, coarsely. Jade card says *live*, *dead*, *starved* or *outbid* for nothing and gives figures only with matching instrument carried. `Quantity` gained `VOLTAGE` and `CURRENT`, appended.

**Built: electromagnet** (`machine/electromagnet/`), first sink. Wants `ELECTROMAGNET_AMPS` (one cell's worth) at LV. Field is `ElectricRules.rate(volts seen, amps delivered, amps wanted, LV)` times its condition: product of fraction of rated voltage it sees and fraction of wanted current it gets, so long run and starved supply both slow it and neither refuses it. Zero is floor it can sit on indefinitely without wear. Voltage over rating is wear and heat, scaled by excess: condition falls to `ELECTROMAGNET_CONDITION_FLOOR` and `Heat.warm` puts Work into its `ThermalBody`, settling under same difference-driven leak as everything else. Never destroyed. No band above LV built, so over-voltage reached only by test calling `onResolved` with high voltage. Magnet pulls dropped item entities whose material has `separation.magnetic` (iron, steel, cast iron, netherite) toward itself at range growing with field strength.

**§6.5 slow-down is the product, provisionally.** Voltage fraction times current fraction chosen because it makes both visible failures of §6.2 slow machine; `ElectricRules.rate` is one method holding the choice.

**Built (2026-10-13): dynamo** (`machine/dynamo/`), mechanical-to-`Eu` boundary of §6.8. Source `ElectricNode` with input face toward shaft; volts are that shaft's live RPM times winding's `voltsPerRpm`, rated amps are winding's, and current delivered last tick times same figure charged to shaft's run through `RotationNetwork.loadByBelt`, flat and unreferred like every machine's working draw. Not `RotationNode` (block entity can extend one base), so couples to shaft the way Pulley couples to partner's run, doesn't spin on screen yet. Sneak-use with empty hand switches coarse/fine.

**Built (2026-10-13): Leyden jar** (`machine/capacitor/`), §6.6's capacitor. Keeps charge in amp-ticks (internal, no type, like `Work`) and holds charge over `LEYDEN_JAR_CAPACITANCE` volts. `ElectricMember` allows one role, so jar changes role: **sink** charging at cell's current while its run holds higher voltage, **source** offering own voltage at burst current once nothing holds it up, flipping when supply goes or when outbid, waiting as sink when empty. Flip rebuilds its run, because role decides which neighbours conduct. Voltage falls as it empties, so sink on it slows as it goes: burst, not battery. Leaks fixed fraction a tick. Makes electrostatic generator's pulse usable.

**Built (2026-10-14): Charger** (`machine/charger/`), sink charging *jar rounds* for coil gun (`docs/feedback_guns_spec.md` §11 step 4). §6.9 refuses item batteries for tools; answer here keeps that: charge rides in ammunition, one round one shot, leaking linearly wherever it is (stamp and tick, like workpiece's heat). No rating, never wears -- terminal, and over-voltage reaches coil's winding as field pressure when round fired, consequence, never refusal.

**Built (2026-10-14): electrolytic cell** (`machine/electrolytic/`), §6.14.

**Built (2026-10-14): motor** (`machine/dynamo/MotorBlock`), §6.8's `Eu`-to-Mechanical boundary, dynamo reversed. Sink with output face toward shaft and dynamo's two windings. Voltage it sees over winding's `voltsPerRpm` is its **no-load speed**; current delivered times same figure is its **torque**, handed to shaft run through `RotationNetwork.driveByBelt` every tick, as belt drives run (into run's frame through coupling node's ratio). Current it asks for is winding's rated amps times `1 - rpm / no-load rpm`: real DC motor's back-EMF line, rated amps standing in for stall current because resistance not modelled. So idle motor draws next to nothing, loaded one slows until its current carries load, under-voltage one runs slow, never stopped (§6.5) -- none written as rule. Fine motor on fine dynamo's line, or fine motor turning coarse dynamo, passes power through at shaft's friction and nothing else, the motor-generator set: MV in, LV out, no constant.

`driveByBelt` widened in same change: capacities from several drivers now add and fastest target wins, so two motors on one shaft share its load (belt and motor too). Before, last caller in tick won.

**Divergence:** §6.8's table named *adapter* motor, fitting on machine. It is block, because motor-generator set needs shaft between two, and fitting has none. Adapter form (motor bolted onto machine taking shaft) can follow, same arithmetic. **[OPEN]** Over-voltage: motor over its band only spins faster, nothing wears it; overspeed trip already answers shaft. **[OPEN]** One direction only.

**Not built:** everything else under §6.11's *Deferred* stays deferred; `[OPEN]`s raised while building are in `TODO.md` under `Eu`.

### 6.14 The electrolytic cell: electrorefining

**Built 2026-10-14, user rulings same day:** electrorefining first (not water electrolysis), impure cathode comes off as scrap, plain water as electrolyte for now. Roadmap Slice 5's first machine, first reader of `Qu`.

**What it is.** Sink with water tank, anode slot and cathode. Any metal, or alloy of metals, in any form, hangs as anode. Dissolves into water as its constituents -- pure metal as itself, alloy by its material file's new `composition` (brass is copper 0.65 and zinc 0.35). Current plates metal back out of water onto cathode. One ingot of any metal is `FTuning.QU_PER_INGOT`; nugget a ninth, block nine.

**Voltage is threshold, current is rate** (§6.1). Every amp delivered moves `QU_PER_AMP_TICK` Qu a tick, times cell's condition; cell does not use `ElectricRules.rate`. Voltage decides only *which* metal plates: noblest metal in water (highest `electrode_potential`) plates at `ELECTROLYSIS_OVERPOTENTIAL_VOLTS`, less noble one needs that plus gap. Below metal's figure no current plates it -- one of §6.5's real thresholds, so hard gate.

**Voltage is selectivity, and it is property of supply.** Zinc sits 1.1 V behind copper, so zinc-copper galvanic cell (1.1 V) refines copper out of brass and leaves zinc in water, and coarse dynamo at engine's working speed (1.5 V) plates both. Tin sits 0.48 V behind copper: zinc-copper cell does not refine bronze, lead-copper cell (0.47 V) does. So slow, selective route is galvanic cell of right pair, fast route is dynamo held under threshold by slower shaft or longer run. Nothing names a metal; six potentials already in material table do it.

**Overrun spoils finished deposit.** While anode lasts it keeps noble metal coming. Once gone, only thing left for anode to do is split water, taking `ELECTROLYSIS_WATER_VOLTS` (1.23 V, real). Below that cell stops, so low-voltage refinery safe to forget. Above it, current strips what is left in water -- noble metal, then whatever waited behind it -- onto cathode player hasn't taken yet. Purity judged on whole deposit when it comes off (`ELECTROLYTIC_PURE_FRACTION`), so overrun turns copper cathode into copper scrap. Scrap hangs as anode again, so loss is time and current, not metal. Fast supply punishes late hand, and that trade was not placed by hand: falls out of water's potential sitting between copper's and zinc's.

**The water remembers** (philosophy §9). Unplated metal stays dissolved; next anode in that water inherits it. Draining tank takes dissolved metal with it in proportion.

**Over-voltage** is electromagnet's rule: rated LV, excess is condition and heat, never refusal, never destruction.

**What was tempting, and wrong.** *Water electrolysis first.* Easier to state, but nothing finished sits in cell to be spoiled, so no overrun. *Thresholds as absolute per-metal figures* (copper at 0.34 V etc.): selectivity comes from *gap* between metals present, so rule relative to noblest in water.

**[OPEN]**
- Real refining needs conductive electrolyte, acid or salt in water. Plain water accepted for now; conductivity as composition belongs to acids pass.
- Concentration (`Qu/mB`) not readable by any instrument; what is in water invisible until something comes off cathode. Concentration instrument is obvious next sense (philosophy §14's *Sense*).
- Current density: real deposits burn (powdery, dark) when current outruns what solution can supply. Built for plated piece (§6.15); cell's own cathode still never burns, cell size as physical upgrade not built.
- Gas: above 1.23 V charge plating nothing evolves hydrogen and oxygen, lost. Capturing waits on gases as fluids.
- Less noble anode in nobler solution (zinc into copper water) plates spontaneously, which is galvanic cell. Cell stays sink and needs supply anyway.
- Degrade tier: impure is spoil only, as removal overruns are; named intermediate items are document 3's job.

### 6.15 Electroplating

**Built 2026-10-14, user rulings same day:** no new block -- electrolytic cell's own chemistry onto item hung as cathode; overrun is coat thickening past what was wanted, too much current burns it. No prior art worth reading (author's word).

**What it is.** Hang metal piece on a *side* of cell (top takes anode). While it hangs, what plates goes onto it rather than cell's own deposit, by §6.14's rules unchanged: voltage picks metal (noblest first), current is rate. Only metal conducts, so only metal is cathode. Taking it off (empty hand) gives piece back with `Plating` data component: dominant metal, thickness in Qu, and quality. Piece is still what it was -- `Material.find` still calls plated iron iron (philosophy 11).

**The ladder** (philosophy 6), all judged when piece comes out:
- **Porous** (degrade): under `PLATING_POROUS_BELOW_QU`, coat hasn't closed.
- **Sound**: in window.
- **Mixed** (degrade): under `ELECTROLYTIC_PURE_FRACTION` of one metal -- voltage reached second metal in water.
- **Burnt** (degrade): over `PLATING_BURNT_SHARE` of coat grew at more current than piece takes. Limit is current density: `PLATING_BURN_AMPS_PER_INGOT` times piece's size (read off its form, as cell reads anode) to two-thirds, since surface goes as size to two-thirds. So galvanic cell's amp or fine dynamo's two plates ingot clean, coarse dynamo's eight burns it -- and plates block clean. Which supply suits is property of piece, not recipe.
- **Spalled** (spoil, the overrun): over `PLATING_SPALLS_ABOVE_QU` coat too thick to hold and flakes off as piece comes out: piece comes back bare and metal as nuggets, which hang as anode again. Loss is time and current, as scrap cathode's. Finished coat keeps growing as long as nobody takes it out -- core loop on cell's second job.

Plated piece hung again brings coat back in to grow further, burnt included.

**What was tempting, and wrong.** *Widening the `Dip`* (`process/Dip`, built naming electroplating as reason, `TODO.md` §5f as of `50b93c6`). Dip is table of item-in, item-out conditions, would have made plating recipe per piece and per metal; cell already had chemistry and selectivity, piece hung in it needs none restated. `Dip` keeps glazing.

**[OPEN]**
- Nothing reads coat yet: §5.4 trap. Corrosion (`corrosion_resistance`, unread) is obvious reader, optical sorter fooled by gold coat the most Feedback-like one; neither exists.
- Thickness is figure no instrument measures; tooltip gives metal and adjective only, thickness at truth tier alone (`Quantity.FIGURE`, philosophy 8). Calipers reading it would be natural instrument.
- One piece at a time.

### 6.16 The heating coil

**Built 2026-10-14.** §6.8's `Eu`-to-Thermal boundary: block set under vessel, boiler or anything else reading `HeatSource.below`, as firebox is.

**Element is body, not flame.** Fire has temperature and no mass (`HeatSource`); element has small mass, so is `ThermalBody`. Each tick current puts `volts x amps x COIL_WORK_PER_WATT_TICK` of Work into it -- Joule's law in this mod's units, the one physical constant, as `QU_PER_AMP_TICK` is Faraday's -- and it gives body above `FIRE_CONDUCTANCE x (element - load)`, same figure that body's own `Heat.tick` takes from its fire, so what vessel gains element loses. Settles a little above its load. Not exchange rate in §6.8's sense: nothing says "N Work per Eu" as price; it is energy current dissipates, stated once, and vessel still heats on difference as everything does.

**Electrical heat's character** (philosophy 10) falls out of mass: cut supply and element has almost nothing to coast on. Fire's character is its fuel; coil's is its supply -- dynamo's shaft speed sets volts, so slower river is cooler coil.

**Element's limit is its metal's.** Above element's `service_temperature` (from its material file) coil loses condition by excess, to floor; condition multiplies heat. Over-voltage needs no rule of its own (§6.5): more volts is more heat, and that is where it shows. Copper wears above 300 Tu and iron holds to 500, so element is physical upgrade -- better element is hotter coil you could point at, one class and one block per metal. Later alloy element is registration line.

**Numbers, simulated.** Fine dynamo at steam engine's speed (6 V, 2 A) holds small crucible near 290 Tu, just under copper's limit; hand-cranked one near 560. Game test runs two coils at 9 V: crucible passes 300 Tu, copper wears, iron does not. So coil is precise low heater -- tempering, preheat, boiler -- not yet smelter.

**[OPEN]**
- No coil reaches smelting heat on today's sources (`TODO.md`).
- Built since: breaker in its line is its switch (§6.4), controller can drive it.
- One coil heats only block above it.

---

## 7. Contact hazards: hot, wet, live

**Built 2026-10-14, user rulings same day.** Philosophy 9 says glowing ingot is glowing ingot wherever it is, "in a hand, in a wooden chest, next to something flammable", and consequences following obviously should be allowed to. Until now none did: 900 Tu ingot could be carried in bare hands forever. This section is the missing part of that sentence.

### 7.1 One shape, three physics

State machine already holds (temperature, pressure, voltage) is above threshold, and whoever is in contact pays. Nothing is uptime, nothing is machine's own wear, nothing asks what block *is*: block is hot if it is `ThermalBody`, live if it is `ElectricNode` with `touchVolts() > 0`, pressurised if it holds `Pu`. Code is `core/hazard/`.

**Named bands, not dial** (philosophy 3). `HotContact` has three, anchored on figures mod already has: **scald** from `CONTACT_PAIN_TU` (100, water boils), **burn** from `CONTACT_BURN_TU` (300, wood chars) which also sets fire, **sear** from `CONTACT_SEAR_TU` (700, first glow). Player learns "that is a sear", not slope. `Shock` takes figure from voltage's `VoltageBand`, LV keeping bare wire's old one so nothing added beside it.

Damage types are data (`data/feedback/damage_type/`): `hot_contact` and `scald` in `minecraft:is_fire`, so **fire resistance is real, bought answer** to handling red-hot work, and `shock` in `bypasses_armor` because chestplate is not insulation.

### 7.2 Hot contact

- **Hot piece in either hand** hurts twice a second (`CONTACT_INTERVAL_TICKS`) while its own stamped temperature (`ItemHeat`, nothing ticked per item) is over what player can handle. Only hands: hot ingot in pocket is player's own business, keeping this from being chore.
- **Bare hand on hot block** (right-click on any `ThermalBody` over its limit) hurts once per click. Use is *not* cancelled. Consequence, never lock: reaching into hot crucible bare-handed possible, once, teaches why gloves exist.
- Creative and spectator players left alone.

**Protection is physical thing, rated by its material.** `Insulator` is asked of stack; nothing says which item it is. `Grip` reads gripping material's own file: `service_temperature`, else `oxidation_temperature`, and only if it **insulates** -- `conductivity` under `GRIP_MAX_CONDUCTIVITY`, or organic where nobody measured. Thing may be handled while `ambient + exposure x (T - ambient) <= what the grip stands`, so *exposure* is the physical change: glove sits on palm (`GLOVE_EXPOSURE` 0.25), tongs put forearm of arm and grip between jaws and hand (`TONGS_EXPOSURE` 0.1).

- **Gloves** (`GloveItem`, Curios `hands` curio): made of material, rated by it. Leather holds warm things (about 340 Tu) not red ones. Also stop bare conductor up to `GLOVE_SHOCK_PROOF_VOLTS` where material insulates.
- **Tongs** (`TongsItem`, in a hand): two rods of one metal and one grip, in any arrangement (`TongsRecipe`), component carrying both materials. Limit is lower of grip's (at tong exposure) and arms' melting point. Wooden grip and iron arms hold yellow heat; bone grip holds whatever iron will; copper arms give out at copper's melt, well before steel's window. **Steel work wants bone-gripped tongs, a thing player works out.** Do nothing against wire.

Two answers costing different things (philosophy 5): glove always on, covers warm range; tongs cost a hand, cover glowing one. Neither wears (the three accepted wear-with-use exceptions not a list that grows).

**What was tempting, and wrong.** *Per-item "protects to N Tu".* Stat on item, wouldn't move when grip material did, exactly the "+50%" non-design philosophy names. *Hot items burning anywhere in inventory.* Real in a sense and chore in practice: removes decision (carry it in hand, or don't), replaces it with rule about pockets. *Cancelling interaction.* Lock where there was consequence.

### 7.3 Steam, and what lets go

`PressureBurst` is the one ladder, shared by Pressure Vessel and Boiler: nothing under danger line however long it sits; between danger and critical per-tick chance climbing linearly; at critical certain. Shape is PneumaticCraft: Repressurized's (GPL-3.0; recorded in `THIRD-PARTY-LICENSES.md`).

**Boiler gains a pressure** (`BoilerBlockEntity#getPressure`), which it never had, because it couldn't be over-restricted -- open item `FTuning` boiler note records as "Not taken: the exploding boiler". Pressure is held steam over `BOILER_STEAM_MB_PER_PU` (full 4000 mB tank is 4 Pu, under danger line of 5) **plus, only while tank has no room for another tick's production, `BOILER_PU_PER_TU_OVER` per Tu over working temperature**. Boiler making steam into open line spends heat on work of boiling and sits near working temperature however big the fire. Shut one has no outlet, so fire keeps pushing and surplus paid as pressure. Only mistake killing a boiler is restricted line under real fire, the GregTech case and nothing else: boiler merely sitting full is safe.

**Warning comes first.** Over `BOILER_SEEP_PU` seams weep: steam lost (partial relief) and whoever stands beside is scalded, with hiss and cloud. Loud, local, survivable, and boiler a little over line may settle on it. One under real fire does not, and lets go: explosion (vessel's own kind) plus scald out to `STEAM_BURST_REACH`. Steam gets in under glove, so scalding asks no protection: only fire resistance answers.

**Wear rule, clarified (user ruling 2026-10-14).** "Machine degrades and never breaks" is about *factories*: run something slightly suboptimally for long time and it gets worse slowly and surely, almost or entirely unusable only at end of readable path, never instantly. Not promise that no machine can be destroyed. Pressure holders are Hazard tier of philosophy 6 and may be destroyed by pressure player let build; conductors may be destroyed by degradation left unfixed (§6.4). Both reached only by misuse. `CLAUDE.md` says so.

**Relief valve (built).** Boiler is `Fittable` for Upgrades, and `ReliefValveFitting` (item `relief_valve`, one per boiler, no wear) makes it vent steam over `BOILER_RELIEF_PU` (3.0 Pu, under seep line) at `BOILER_RELIEF_MB_PER_PU` per Pu over, with same hiss and scald as seams. Venting un-traps full tank, dropping over-temperature term, so valved boiler holds under seep and danger lines; seams remain fallback if valve cannot keep up. Relief only: never vents below its line, never regulates (ruling 2026-10-13). Price is steam and scald.

**Not built.** Instrument reading boiler pressure (Pressure Vessel has none either); sidedness of scald; steam as world fluid (steam has no block, nothing to stand in).

### 7.4 Shock

`ElectricNode#touchVolts()` is volts bare hand would meet: `0` where casing or insulation covers it, almost everywhere. **Bare, loaded conductor** (`CableBlockEntity`: insulation gone, current flowing) says its potential, never under LV's reference; **charged Leyden jar** says its held voltage once at `SHOCK_MIN_VOLTS` (MV) or more. Touching is walk-in (`entityInside`, any entity) or right-click (player). Damage follows `VoltageBand` (`SHOCK_MV_DAMAGE` and up); insulating glove stops it below `GLOVE_SHOCK_PROOF_VOLTS`; armour does not.

Resistance still not modelled (philosophy 17): shock is property of conductor being *exposed* at voltage, which player can see and fix, not of current through particular hand.

**[OPEN]**
- Galvanometer or electroscope used on bare, live wire is right-click on it and shocks. Physically honest; whether *instrument* should carry insulation (probe, not hand) is call for later.
- Dynamos and electrostatic generators have no exposed terminals today (`touchVolts` is 0); only cable and jar are bare.
- Hot piece does not ignite flammable block it lands on or sits beside. Philosophy 9 allows it; nothing builds it.

---

## 8. Offline catch-up

**Built 2026-10-14, user ruling same day:** cooling plus fuel burn across gap, process progress not advanced.

### 8.1 The gap

Workpiece cools in unloaded chunk for free (`ItemHeat`: stamp and tick, computed when asked). *Block* holds live temperature and updates it by ticking, and unloaded chunk does not tick. Crucible you walked away from froze at whatever it was and resumed from there, hot or cold, as though world paused around it. Recorded as known gap in `TODO.md` ("Not taken: catch-up for unloaded devices"); this closes part that mattered. TerraFirmaCraft keeps calendar and burns fuel across time skips; read is in `THIRD-PARTY-LICENSES.md`, what taken is idea, not calendar.

### 8.2 How

Block records game time it was saved at (`Catchup`, one `SavedAt` long in its tag). On first tick after load it asks how long it was away; if `CATCHUP_MIN_TICKS` or more, advanced across gap **in one step**:

- `Heat.relax` is `Heat.tick` solved for *n* ticks. One tick moves body by `-k (T - Teq)` with `k` total conductance over mass and `Teq` existing `Heat.equilibrium`, geometric series, so `(1 - k)^n` is whole of it. Gap of million ticks costs one `pow`, game test pins it to loop.
- `HeatSource` answers `catchUp(gap)` with `FireSpan`: how many of gap's ticks it burned and at what mean temperature. **Fuel burns until spent and then fire is out**, lit first and dark after, so one span is whole shape. Firebox and self-fired vessel burn piece alight and then their stack, unblown (nobody worked bellows) and at each fuel's nominal temperature (quality roll is noise floor for fire someone watches).
- Body relaxes towards fire for lit part and towards room for rest (`Catchup.settle`). Crucible or boiler asks block under it (`HeatSource.spanBelow`), and lava is lit for whole gap, as always.
- Electrical nodes that are `ThermalBody`s settle at first resolve after load, from one place in `ElectricNode`, so cable, magnet, fuse, cell and coil need no edit of their own.

### 8.3 What it does not do

- **Process progress not advanced.** Ore in crucible not cooked across gap: that is second simulation (suitability integrated along curve) for stretch nobody saw, and player who walked away hasn't earned completion. Resumes where left off, at temperature it now has.
- **No air.** Bellows' banked air is gone.
- **Firebox answers for itself.** If vessel's chunk was unloaded and firebox under it was not, firebox was ticking throughout and is asked if lit; span is then whole gap at its current flame, fair statement and not exact one.
- **Machines that move** (rotation run, electric run) not caught up: nothing keeps their state across gap and run rebuilds itself on load.

**Trap worth recording.** First draft cooled by *rate* per elapsed tick, which is item's law (linear, `Heat.cooled`) and wrong for vessel: vessels cool by difference-driven leak (§ thermal model), so gap would have fallen off cliff to ambient. Vessel's law solved in closed form is same mathematics with no new variable.

---

## 9. Deposits: ore as geography

> **Build status: graphite, and both batches of the vanilla replacement (coal, iron, copper, gold, lapis, redstone, diamond, emerald, nether gold, nether quartz; §9.6).** `docs/feedback_progression_roadmap.md` §4 holds wider geology direction (tin, zinc, lead, kaolinite wait on it); this section is mechanism deposits built on, so others add data rather than code. Ancient debris stays vanilla (ruling), so do vanilla's noise-carved veins (below).

### 9.1 Why graphite goes first

`thermal_process/graphite.json` asks 2200-2600 Tu. No flame in game gets there (blown blaze rod caps at 2100), so graphite cannot be made by player with only slice 1-2 heat. Real graphite also mined: mineral, found in metamorphosed carbon-rich rock, and 2200+ Tu process is *synthetic* route (Slice 10's arc furnace). So natural deposit is early source, refractory for crucible lining and kiln ceilings, and process stays as late, scalable one. Nothing keyed on id: ore is one more source for graphite *material*, and pack adding second graphite source adds deposit file, not code.

### 9.2 A deposit is data, in its own table

`data/<ns>/ore_vein/*.json`, one file per deposit, plain `SimpleJsonResourceReloadListener` over record with `Codec`, same convention as `thermal_process`, `quench` and `casting`. **Not** field of material file: deposit is geography (where, how big, how dense), and one material can sit in several deposits of different shapes (hematite band, magnetite plug) while one deposit can carry several materials.

Fields (trimmed from GregTech CEu Modern's `GTOreDefinition`; read is in `THIRD-PARTY-LICENSES.md`):

- `ore` and `deepslate_ore`: block each host class places. Two blocks, not one per stone, because graphite vein only needs to look right in stone and deepslate; single block with host property is revisit once three or four ores exist.
- `host`: block tag ore may replace (default `minecraft:stone_ore_replaceables` / `minecraft:deepslate_ore_replaceables`, vanilla's own pattern).
- `cluster_size`: blocks in blob, int range.
- `density`: 0 to 1, chance candidate block in blob becomes ore. Knob making vein lean or rich, property of geology, not machine.
- `count`: blobs attempted per chunk on average (fraction is chance of one more). Replaced placement `weight` once every deposit needed own rate: weight picks one deposit per attempt, cannot say "thirty small coal blobs and half a graphite one".
- `min_y`, `max_y` and `triangle`: Y window, drawn uniform or (with `triangle`) most likely mid-window, as vanilla's `trapezoid` is.
- `air_discard`: chance block touching air is left as stone, vanilla's `discard_chance_on_air_exposure`, so buried deposit shows less in cave walls.
- `dimension_filter` and `biomes`: where it can occur; biome tag.

### 9.3 Placement: vanilla machinery, not a mixin

Every deposit placed by one custom `Feature` through one `PlacedFeature` (no placement modifiers) and NeoForge biome modifier at underground-ores step. Feature reads loaded deposit table and, per chunk, makes each deposit's `count` attempts at own Y window, so table is single source of truth and placed feature JSON carries no height or count. (Earlier version rolled one weighted deposit per attempt and kept copy of height window in placed feature; two files that had to agree.) GregTech instead hooks `ChunkGenerator.applyBiomeDecoration` with mixin and grid of vein centres whose blobs are cached and sliced per chunk, so veins can be larger than chunk and identical whichever chunk generates first. **Not taken:** mixin, grid, per-layer single weighted pick. Graphite blob small enough to sit inside one chunk's decoration reach, and vanilla's own `ore` features already handle that limit; grid is thing to revisit if deposit ever needs to be bigger than chunk. Weighting is per biome, so geography does work tier ladder would otherwise do.

### 9.4 Mining it

Ore block drops what its material's `forms.lump` says, through ordinary loot table; pickaxe tag and hardness from material file decide whether tool can mine it, exactly as for generated forms. No ore tier: hard gate here is hardness comparison (philosophy §7), never number on ore.

### 9.5 Not in v1

- **Surface indicators** (loose flakes at vein centre; GregTech's `indicators`). Sensing, optional, free to skip, and graphite is dark seam you can see in cave wall already. Add with second deposit, when finding one starts to matter.
- **Prospecting instrument.** When it comes it is read-only `Quantity` reading with noise floor (roadmap §4.3), never unlock, and not powered by `Eu`.
- **Bedrock-ore and fluid veins** with depletion (flat-rate wear Feedback rejects).
- **Deposit shapes other than blob** (dike, cuboid, noise-carved veins).

### 9.6 Replacing vanilla's ores (batch 1: coal, iron, copper, gold)

**Ruling (2026-10-22):** as GregTech CEu Modern does, vanilla's ores leave world generation and Feedback's own blocks, placed by this table, take their place. Vanilla blocks still exist (structures, creative, old worlds); simply stop generating. Breaking changes fine: no migration.

**What changed.** `feedback:<metal>_ore` and `feedback:deepslate_<metal>_ore` for the four, with vanilla's hardness, blast resistance, sound, tool tier (`needs_stone_tool` for iron and copper, `needs_iron_tool` for gold, none for coal) and loot (coal, `raw_iron`, `raw_copper` 2-5, `raw_gold`; Fortune by `ore_drops`; Silk Touch gives block; coal also 0-2 experience). Join `minecraft:<metal>_ores` (block and item) and so NeoForge's `c:ores`, `c:ores/<metal>`; and `c:ores_in_ground/stone` and `/deepslate`. Material files' `forms.ore` / `forms.deepslate_ore` point at them, and `molten_*` processes and coal's smelting recipes accept them. Deposit files start from vanilla's figures: `coal_upper`, `coal_lower`, `iron_upper`, `iron_middle`, `iron_small`, `copper`, `copper_large` (dripstone caves), `gold`, `gold_lower`, `gold_extra` (badlands). `neoforge/biome_modifier/remove_vanilla_ores.json` removes vanilla's matching placed features from overworld biomes. All numbers Invented; density 0.9 is stand-in for vanilla blobs not wasting candidates on non-host blocks.

**Hand-registered, not generated form.** Form system (§5.4) generates block per *material*, by kind, with one loot table (`dropSelf`); ore needs per-ore host pair, drop that is different item with own count rule, and tool tier, none of which form type states. Graphite's hand-registered pattern is right one until ore forms carry that data.

**Not removable by biome modifier: vanilla's noise-carved ore veins** (large copper and iron veins with raw blocks, made by overworld noise settings, `ore_veins_enabled`). Flag exists in `worldgen/noise_settings/overworld.json`, but turning it off means shipping copy of that 109 kB file, which any other worldgen mod, amplified and large-biomes presets, or vanilla update would silently overwrite or contradict. Not shipped: those two veins keep generating vanilla copper and iron ore (and granite, tuff, raw blocks) until decision made.

**Batch 2 (2026-10-22): lapis, redstone, diamond, emerald, nether gold, nether quartz.** Same pattern; what differed:
- **Redstone ore** is vanilla's `RedStoneOreBlock` with our properties (`LIT`, light 9 while lit, emissive while lit, random ticks), so lights when touched or mined and drops own experience with no restated behaviour. Blockstate lists `lit=false` and `lit=true` against one model: vanilla draws lit ore same and lights it by emission.
- **Experience** is vanilla's per ore through `DropExperienceBlock` (lapis 2-5, diamond and emerald 3-7, nether gold 0-1, nether quartz 2-5); redstone's comes from its class.
- **Tool tiers:** lapis stone tool; redstone, diamond and emerald iron one; nether pair any pickaxe. Drops: lapis 4-9 (ore-drops Fortune), redstone 4-5 (uniform-bonus Fortune), nether gold 2-6 nuggets.
- **Diamond is four bands** (`diamond_small`, `_medium`, `_large`, `_buried`), as `ore_diamond`, `_medium`, `_large`, `_buried`; lapis two, redstone two. Air discard is each band's own.
- **Emerald** follows vanilla's actual list of biomes carrying `ore_emerald`, ten, not `#minecraft:is_mountain` (six; leaves out windswept hills): `feedback:has_emerald_ore`.
- **Nether needs no new schema.** Deposit table already filters by dimension, so one placed feature added to `#minecraft:is_nether` by second biome modifier (`add_ore_veins_nether.json`), and vanilla's `ore_gold_nether` and `ore_quartz_nether` removed by `remove_vanilla_ores_nether.json`. Nether ore is one block: `ore` and `deepslate_ore` name same block, `host` is `feedback:netherrack_ore_replaceables` (netherrack alone, as vanilla's own match; `base_stone_nether` would put ore in basalt and blackstone), deepslate host never matches in Nether. Nether gold joins `minecraft:gold_ores`, making piglins guard it.
- **Trap, found in batch 1's files:** `OreVein`'s tag fields use `TagKey.codec`, taking plain id. `"#minecraft:is_badlands"` and `"#feedback:dripstone_caves"` failed to parse, so `gold_extra` and `copper_large` silently never loaded (logged "Bad ore vein"). Fixed; tag in deposit file has no hash.
- Smelting: lapis, redstone, diamond, emerald and nether quartz each get smelting and blasting recipe for new blocks beside vanilla's (vanilla's still work on vanilla blocks); nether gold smelts to nothing, as all gold ores since batch 1 (vanilla's recipe disabled), and melts in crucible.

**Ore look: strata plus overlay (2026-10-22).** An ore block is two cube elements: host stone's own vanilla texture, and an overlay slightly larger, cutout (`models/block/ore_layered.json`; per-ore model file is parent, strata texture, overlay). Vanilla ores' overlays are **extracted from Mojang's own textures** (user ruling, 2026-10-22: Mojang's assets may be reused in this mod): ore pixels are those whose luminance falls outside the host stone's range or whose chroma exceeds the stone's, saved as `textures/block/ore/<name>.png`. Plain subtraction failed: Mojang repainted the stone part of each ore texture. Ores without a vanilla texture use `ore_layered_tinted`: a grey overlay (`overlay_chunk.png`, tint index 0) coloured by the material's `color` (`MaterialColors.of` via `FormTint.oreBlocks`, which finds ore blocks from material files' `forms.*ore`); graphite is the first. Tin, zinc and lead ores take that route with a new grey shape. New host stone = new ore blocks plus a model naming its texture. Taken from GregTech CEu Modern: base-plus-tinted-overlay split. Not taken: its runtime-generated models, Registrate, emissive layer, icon-set enum.

**Batch 3 (2026-10-22): tin, zinc, lead.** First ores with no vanilla item, so they bring their own: `raw_tin`, `raw_zinc`, `raw_lead` (hand-registered, one grey `item/form/raw` sprite tinted by the material colour; `forms.raw`, in `SPRITE_FORMS`) and `feedback:<metal>_ore` / `deepslate_<metal>_ore`, stone-tool tier, vanilla-copper strength, drops raw metal with Fortune. Overlays are the grey shapes `overlay_cluster` (tin), `overlay_speck` (zinc), `overlay_lump` (lead), made from the copper, iron and coal overlays and tinted through `ore_layered_tinted`. `molten_<metal>` accepts the ores and the raw item alongside the ingot. One `ore_vein/<metal>.json` each, **Invented** figures: tin y 0 to 64 (8 blobs of 8), zinc y -32 to 48 (7 of 9), lead y -64 to 16 (8 of 9), overworld, triangle. No vanilla placed feature to remove. Mineral names (cassiterite, sphalerite, galena) stay unused: nothing here separates ore by mineral yet.

**Adding the next ore** (batch 2 followed these steps):
1. `FBlocks`: `ore("<name>_ore", false, <drops experience>)` and deepslate pair; `FItems` block items; `FCreativeTabs` entries.
2. Assets: blockstate, block model and item model for each block; `block.feedback.<name>` lang keys.
3. Data: loot table per block under `data/feedback/loot_table/blocks/` (copy vanilla's from Minecraft jar); `FormData.BlockTagsGen` (pickaxe and `needs_*_tool` tag), then `runData`; add blocks to `minecraft:<name>_ores` block and item tags and `c:ores_in_ground` tags.
4. `data/feedback/ore_vein/<name>*.json`, one per height band, from vanilla's placed and configured feature (`count`, `size`, height range, `discard_chance_on_air_exposure`); re-point material's `forms.ore` / `forms.deepslate_ore`; add ore items to material's smelting or `thermal_process` inputs.
5. List vanilla placed features in `remove_vanilla_ores.json` (nether ores need own modifier with nether biome tag and nether host tag).