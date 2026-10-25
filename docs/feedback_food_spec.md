# Food

> Status: design spec, 2026-10-24. Lifts philosophy §18a ("Food as a real thermal process", parked). Phase 1 of §11 is built (2026-10-24, unplayed): hunger and saturation are read off the cooked form's vanilla item, not stored in the `food` block; `cook` progress added to `FoodState`; no mass term yet. Step 2 (skillet) is built, unplayed: the pan is the fire's temperature at once (no mass), the grade is the resulting `FoodState` (no separate evenness figure), one piece at a time. Step 3 (stockpot and bowls) is built without the stir-and-skim minigame, unplayed: the pot keeps summed totals (energy, toxin, pathogen, cook, volume) rather than item stacks, boiling pins its temperature at `POT_BOIL_TU` while water remains (an addition: overrun is then a boiled-dry pot, not a hot one), servings are `round(volume / POT_SERVING_MB)`, a stew carries a 1.25 bonus, and the dish name is broth, stew or scorched only. Step 4 is built, unplayed: `FoodState` gained `rot`, `stamp`, `envTu`; rot advances on read at a rate by the mean of then and now surroundings and by moisture, raises pathogen then toxin; `dried` now needs only low moisture, so raw food can be dried; heat kills pathogen as a per-tick fraction, not as a function of cook; the ice box is a chest that spends ice (any solid with melting point at or under 0 Tu) and settles its contents once a second; the rack dries up to the edge of cooking; smoke and salt are built (rulings in section 8): `FoodState` gained `smoke` (a coat the Smoker's `VesselKind` figure lays on food cooking above the cooking point and under char, shielding rot and wearing off on read) and `salt` (set by the `feedback:food_salting` crafting recipe from one food and one `saline` material, shielding rot without wearing off, drawing moisture, and dividing hazard concentrations at eating, in the pot and in `DishBake`); salt itself is what a Stockpot in an ocean or beach biome leaves when boiled dry, taken with an empty hand. Step 5 buffs are built, unplayed: compounds are a datapack table (`compound/*.json`: effect, level, ticks per unit strength, optional crash) and a food names them with a strength; `fat` and `fibre` delay a dose by 600 ticks per unit and stretch it by their sum, a fast dose (fat plus fibre under 0.3) leaves its crash, dry food adds delay; the pot and `Meal` carry compounds as concentrations. Canning is built, unplayed (ruled 2026-10-24, section 8): a `feedback:jar` filled at the crafting table with one food or serving, four jars in a Pressure Vessel with no GUI, sterilised by heat above plain boiling that only pressure reaches. The pot minigame is built, unplayed (the `[OPEN]` on held versus tapped is settled as held: circle the mouse, pace is angular speed): a wooden rod opens a top-down screen, the pot stays live on the server and takes the pace and skim clicks as packets a few times a second; stirring cancels bottom scorch over a fire above 250 Tu and lets unskimmed scum be folded in for good; scum rises only while a raw pathogen load is cooking off and counts as pathogen in a serving. Refrigeration is built, unplayed (ruled 2026-10-24, section 8): the Refrigeration Unit, an electric sink that holds an adjacent Ice Box cold with no ice and runs hot. Dough and the knead are built, unplayed (rulings in section 6, delegated by the author 2026-10-24): `feedback:dough` and its `dough` material (trait `kneadable`), baked into the new `feedback:loaf`; the Knead button on the Prep Table's Prepare tab, `modular/Kneading` (pressure and rest), `client/PrepKneadScreen`, `FOOD_KNEAD`; flatbread (`flatbread` material, trait `wrapper`) is the container of the wrap dish. Nothing else is built. Every figure is an `[OPEN]` stand-in until played. Rulings by the author, same day, are marked **(ruled)**.

## 1. The problem

Vanilla food is a number on an item. Cooking is a smelt, spoilage does not exist, a stockpile is free, and nothing about the world changes what a meal is. Meanwhile the mod already has the machinery food needs: items that carry heat (`ItemHeat`), vessels with a temperature band, a Smoker whose ceiling is a food window (`FTuning.FOOD_MAX_TU`), ambient by dimension (`AmbientTable`), phase changes for ice, a Pressure Vessel, and a minigame vocabulary (forging is rhythm, soldering is a drag).

Food is the lowest-stakes teaching material in the game (§18a said so) and the mod's core pitch lands on it cleanly: **keep cooking past done and you spoil what you made.** What was missing is that the output stay perishable *after* it is done too.

## 2. Rulings

1. **Work stays bookkeeping (ruled).** Food does not become a Work ledger, and no new currency is minted. The results of this spec's math feed back into vanilla **hunger and saturation**. A food's authored figures are the vanilla ones (cooked beef: 8 hunger, 12.8 saturation at its done state); state scales them (§4.5). The existing hand-swing exhaustion (`FTuning.HAND_SWING_EXHAUSTION_PER_ST`) is untouched.
2. **Meals carry buffs (ruled).** See §7.
3. **Minigames are wanted, and wanted often (ruled).** Cooking by hand is a minigame; automation (vessel plus time) stays possible and worse. See §6.
4. **The cook-a-batch pot is in scope (ruled).** See §5.
5. **Scalars are allowed (ruled).** The digest's "discrete options, not continuous knobs" guards against two things only: (a) a figure the player takes apart piece by piece to find a "meta" to use forever, and (b) skipping effort where adding depth would give the player a better experience. A scalar that is not (a) or (b) is fine. Consequences for food:
   - Moisture, hazard concentration and rot progress are scalars, because they are *state the world acts on*, not settings.
   - There must be **no best food**. Every figure trades against another (energy against keeping, keeping against effect, effect against onset), and the right choice depends on the trip: an expedition, a forge shift and a fight want different food. Check this at every figure added: if one ingredient or ratio beats all others everywhere, the figure is wrong.
   - Do not quantise a minigame input to satisfy the digest (see `scalars-fine-inside-minigames`). Outcomes stay classed.
6. **Breaking changes are fine.** Vanilla food items change behaviour; no migration. Each changed vanilla food gets a wiki article (original behaviour and new).

## 3. Properties, never identity

A food is a `material` (`data/feedback/material/*.json`) with a new `food` block, reached through the existing `forms` map (`"raw": "minecraft:beef"`, `"cooked": "minecraft:cooked_beef"`). Modded foods join by writing a material, or by tag. Nothing keys on an item id.

The `food` block (all `[OPEN]` figures):

| Field | Meaning |
| :--- | :--- |
| `hunger`, `saturation` | the vanilla figures at the **done** state |
| `moisture` | fraction of the item that is water; starts high for raw meat and fruit, low for grain |
| `pathogen` | live-organism load a raw item carries (0 for most plants, high for raw poultry and pork) |
| `toxin` | stable-toxin concentration (`Qu/mB`-style: dilution matters) the item carries |
| `compounds` | named actives for buffs (§7), with a strength each |
| `fat`, `fibre` | slow absorption: lengthen and delay a buff, raise saturation |
| `char_tu` | where it burns; reuses the material's `oxidation_temperature` where one exists |

The mass of the item (existing `Mu`, `ThermalBody`) sets how fast it heats through: a thick cut cooks slowly, so low and slow is a real answer and a hot fast pan chars the outside of a thick thing. No surface-versus-core simulation: one temperature, mass sets the rate (digest: don't build a simulator).

## 4. The state, and what happens to it

### 4.1 `FoodState` component

A data component on the stack, like a heated workpiece's temperature: `moisture`, `pathogen`, `toxin`, `rot`, a `stamp` tick, and the environment temperature at the stamp. Items without the component are *fresh and raw* by their material. Stacks with different state do not merge.

### 4.2 Lazy clock

No per-tick work on items, ever. Everything that changes with time is evaluated **on read** (tooltip, eating, container open, being handed to a vessel) from the stamp: `rot` and drying advance across the gap by the `Catchup` pattern (`Heat.relax` is `tick` solved for n ticks), integrating coarsely over the item's cooling toward its surroundings. Cuisine Delight and Firmalife both do this and neither scans inventories; that is the cost model to copy. `[OPEN]` how coarse the integration may be before a hot item left in a chest is visibly wrong.

### 4.3 The cooking ladder

State classes, derived from `moisture`, temperature history and `char_tu`:

| Class | Meaning | Hunger/saturation |
| :--- | :--- | :--- |
| **raw** | as found; carries its `pathogen` | base, with the hazard attached |
| **done** | pathogen killed, moisture partly driven off | the authored figure |
| **dried** | overrun on purpose: much of the water gone | less, keeps long (a sidegrade, §6 of philosophy) |
| **charred** | past `char_tu` | spoil: near nothing, a little hazard from the char |

A vessel applies heat to whatever is inside; it does not know what is being cooked. Moisture leaves when the item is above the evaporation point, faster the hotter. Done is reached when the pathogen is dead and the item has not yet lost too much water. The Smoker's low ceiling keeps food in the window by physics; a blast furnace takes it past char on the way up (philosophy §15, unchanged).

### 4.4 Hazards: two classes, two cures

- **Pathogen** (live organisms): killed by heat held long enough. Raw chicken, raw pork, rotten-but-not-yet-toxic food. A thick item takes longer to hold the temperature all through. Fixed by cooking.
- **Toxin** (a stable chemical): **heat does not touch it.** Rotten flesh, poisonous potato, spider eye, pufferfish. Cures: remove it (soak or leach in water: the toxin partitions into the water, which then carries it) or **dilute** it, since toxin is a concentration and a serving only matters when its concentration is over a tolerance. Two answers that cost different things (a vessel and water against volume of clean food).

Minecraft lens, kept in the design. Heat-killed pathogen against heat-stable toxin is real food safety (P-0, L0, extrapolation). It lines up with the compendium: the Hunger effect is a pathogen (`speculative_physics_inspo_doc.md`, pathogen exposure); and a completed zombie is not food (cadaverine and putrescine, §1.7), which is *why* rotten flesh cannot be cooked into safety, matching vanilla. The boundary condition for why this is not everywhere: it is only the animal and rotting foods that carry the problem; plants mostly carry neither. Nether lens: `AmbientTable` air is warm, so meat in a pack there cooks and then overruns without a vessel; cold dimensions and biomes preserve. Both fall out of `ItemHeat`, no new rule. `[OPEN]` whether the Nether's ambient is warm enough to matter, or only lava-adjacent air.

Gates stay soft: bad food hurts through the vanilla Hunger and Poison effects scaled by concentration, never a refusal to eat. A failed batch consumes its inputs (digest: the waste is the gate).

### 4.5 Eating: feeding the math back

Eating computes, from `FoodState`, the vanilla `FoodProperties`: hunger and saturation are the authored figures scaled by state (moisture and class for the main scale; char and rot reduce; fat and fibre raise saturation). Hazards then apply vanilla effects (`HUNGER`, `POISON`, `NAUSEA`) scaled by concentration above tolerance. Nothing new in the player's meters. `[OPEN]` the scaling curves; they exist to make each class a sidegrade, not a ladder up.

## 5. The pot (cook a batch)

A **Stockpot** vessel (a `ThermalVesselBlockEntity` kind; see `machine/vessel/VesselKind`): it holds a fluid (water, `mB`) and a handful of ingredients and heats them together. It applies the same physical operation as a pan: heat the contents.

- **Blend, not recipe.** The pot's contents are one mixture whose figures are the mass-weighted blend of its parts: moisture, pathogen, toxin (a concentration, so water dilutes it), compounds, fat, fibre. It never matches a dish name. A dish *name* shown in the tooltip is derived from the blend's classes (a stew is wet and has fibre; a broth is almost all water).
- **Batch.** The finished mixture is a stock of N servings (N follows the volume). A held item in the `feedback:ladle_containers` item tag (the bowl; a modded container joins by the tag) takes one serving; a pot whose serving would round to under 1 hunger with no saturation (plain water) refuses with "There is nothing in it worth ladling." instead of handing out food: a **meal item** holding the baked figures (modular-items spec §2: baked once from the parts, stored as a component).
- **Pot pathogen is the exception to the per-tick kill.** Items and vessels kill pathogen as a per-tick fraction at cooking speed (`Cooking.tick`); the pot alone keeps `pathogenMass` and serves it as `pathogenMass x (1 - cook)`.
- **Overrun.** Left on the heat the pot boils down: moisture falls, concentration rises (a toxin you diluted comes back up past tolerance), the bottom chars. It does not ruin all at once; a serving ladled early is fine.
- **Sense / stop.** Sense: the pot's temperature and the surface (steam, simmer, scorch) it shows; stop: pulling it off the fire, and it coasts (thermal mass).

Cuisine Delight's held skillet and ModularDelight's stockpot both inform this; neither is a template (their heat is a boolean and their cook time a constant).

## 6. Minigames (ruled: the player cooks, a machine can too)

Vocabulary is already taken: forging is rhythm; soldering is a drag. Food gets *different* kinds, and each hand-cooking one is worse in throughput than automation and better in quality, so hand work stays theoretically sufficient (digest: manual production stays possible).

- **Skillet: heat control against a lagging core (rebuilt 2026-10-24; the first build was position on a pan gradient, dropped because it gave no mass term and the mouse starts mid-pan).** The player sets how much of the fire the pan gets (a lever, analog); the food's own heat lags the pan's (first-order, `FTuning.SKILLET_LAG_TICKS`), and cooking cannot be undone, so the skill is backing off before the coast carries it past done. The enchanting game's genre (control, not timing). A thick piece should lag longer: the lag is one constant until food has a mass. The server recomputes the outcome from the submitted path and the pan's real Tu, exactly as soldering does; the screen never decides. Grades: the item's class and how evenly it cooked.
- **Pot input is a session (built).** A wooden rod opens the screen and records a session for that pot; `PotInputPayload` is accepted only with a live session for that position, the rod still in the main hand, at most one packet per 3 ticks, and the session lapses after 15 s idle or on logout. The skillet's session now also pins the held item, re-reads the fire at finish (the lesser of the two counts) and expires.
- **Pot: stir and skim.** Stirring pace stops a scorch on the bottom and mixes the blend; skimming at the right moment lifts scum (a pathogen-tied surface load) off before it is stirred back in. `[OPEN]` whether the pace is held or tapped.
- **Dough: knead and rest (built 2026-10-24, unplayed; rulings delegated by the author, final).** A hand game at the Prep Table's Prepare tab, **pressure and rest**: hold to press the dough (analog: the pointer's height over the dough is the pressure), release to rest. Pressing builds `gluten` and also `tension`; only relaxed dough takes work (gluten gain falls as tension rises), so the player chooses when to push and when to wait. Tension past a tear limit piles up overwork. It is not rhythm, drag, control, stir, placement or pour.
  - *Material drives the feel* (`modular/Kneading`, the pure model shared by the screen's live picture and the server): `moisture` is stickiness (gluten gain falls by `KNEAD_STICKY * moisture`, the dough lags the hand and oozes back, and the tear limit is `KNEAD_TEAR` lerped from 0.6x dry to 1.4x wet, so a drier dough tears sooner); `fibre` raises tension gain (x(1 + fibre)) and slows rest (x(1 - 0.8 fibre)); `fat` tenderises (gluten x(1 - 0.5 fat), tension x(1 - 0.5 fat)). Hardness is unused (0.05 is trivial). Pressure builds tension by its square and gluten by itself, so a light steady press is safe but slow against a session cap (`KNEAD_MAX_SAMPLES`, 30 s): a real speed against risk trade.
  - *Classes* (outcome discrete, input analog): `tough` if overwork reaches `KNEAD_TOUGH_OVER`; else `crumbly` under `KNEAD_GOOD_AT` gluten; else `glossy` at `KNEAD_GLOSSY_AT` gluten with the dough rested back under `KNEAD_RELAXED`; else `good`.
  - *Server authority*, as cut and pour: `PrepareBegin` kind 2 notes the dough, `PrepareFinish` kind 2 carries the press samples (0..100, one per 2 ticks, capped, `Kneading.valid`) in the existing `marks` list; the menu requires the same dough in the work slot and refuses samples that outnumber the elapsed ticks (with 20 percent allowance for a lagging server), then regrades from the samples with `Kneading.grade`. The client draws the same model live and never names a class. A cheating client can send the best honest input and no better.
  - *Result.* The `FOOD_KNEAD` component (a class) goes on the dough and rides the stack through baking (`Cooking.tick` transmutes a copy). `Cooking.apply` scales the loaf's hunger and saturation by class: glossy x1.15, good x1.0, crumbly x0.8, tough x0.7. Dough with no component bakes at x0.9, the unskilled floor (so automation is never refused and never as good as a good knead). Kneading costs nothing but time; a bad class is the price.
  - *Smallest choices.* One dough at a time (the work slot holds one); no knife; no idle-timeout auto-finish (the session cap ends it); Esc before Done discards; the class does not carry through the flatbread crafting step or into a dish's baked hunger (a dish part reads its item's authored figures); a thin line under the dough shows time left, not a dough meter.
  - *Items.* `feedback:dough` from 3 wheat and a water bucket (the bucket returns as vanilla's craft remainder) gives 3. Its `dough` material lists `{raw: dough, cooked: loaf}` and leaves vanilla bread's own `bread` material alone (bread stays a raw-form food; `Material.find` and `Cooking.index` map one item to one material, so dough cooking into `minecraft:bread` would have indexed bread twice). `feedback:loaf` is a new simple food (5 hunger, 6.0 saturation, vanilla bread's figures). Plain bread crafting is unchanged.
  - *Flatbread.* One dough laid in the Prep Table's Parts tab makes `feedback:flatbread_dough` (a food shape, `ModularType.shapes`: the stock reshaped one food per piece, its `FoodState` and knead carried, never a fresh item; it was a crafting-grid recipe until 2026-10-24, which threw the dough's state away); the `flatbread` material (raw and cooked forms, trait `wrapper`, no `starchy`) bakes it into `feedback:flatbread` through the same ladder. It is the container of the wrap dish (dish spec 2.1).

**The reveal is a prediction (ruled).** The cut, pour and knead screens run the same pure function as the server on the player's own input, so the grade word and the music can play at once; the server then confirms and applies from the submitted samples. The client never decides what is stored. If the server refuses a finish (no session, wrong item, too fast), it sends the HUD line "The work did not take." (`prep.feedback.refused`) so the prediction is not left standing. Applies to the Prep Table games (cut, pour, knead); the skillet and pot show only the state the server sends or the shared pure model.

Automation: a vessel left alone on a controlled fire does the same thing with no skill and a wider spread, at most the base class. Hand skill buys evenness and a higher done, never access to a food.

## 7. Buffs

A meal's buffs come from `compounds`, each with a strength: a quick sugar (a short sharp lift), a stimulant, a mineral. They map to vanilla `MobEffect`s by property, not by item id (modded effects join by data). The physical cause for timing is pharmacokinetics, already established for brewing in the compendium (§3.2): **fat and fibre slow absorption**, so they delay the onset and stretch the duration; a bare sugar comes on fast and ends fast, and the end is a **crash** (a weak slowing effect), the overrun of a buff. Dry food has no water to carry its compound, so its onset is later.

Decisions this creates: a fast bar before a fight; a stew before a shift; both lose to the other's job. No compound should dominate. `[OPEN]` the compound list and effect mapping. `[OPEN]` whether any buff may touch stamina regeneration (one meter, digest); it may not add a meter.

## 8. Spoilage and preservation

- **Rot** advances on read (§4.2) at a rate set by temperature and moisture. A rotting item's `pathogen` rises, and past a threshold it turns toxin-class (rotten flesh): the same ladder, extended, so a forgotten stew is first bad (pathogen, curable by heat) and then poisoned (toxin). A floor keeps this from being a nag: the first stage is cheap to survive.
- **Cold.** The **Ice Box** is a small thermal volume: ice (and later blue ice) inside is consumed as it melts (existing `phase_change`), its air drifts toward the room as the ice goes, and items inside take its temperature. Overrun: the ice is gone and the contents rot at room rate. Firmalife's shelf is a binary switch; this is a drifting mass instead.
- **Dry.** A drying rack (a heat below the cooking window plus airflow and rain interrupt) lowers `moisture` slowly. Sidegrade of §4.3 *dried*.
- **Smoke (ruled, 2026-10-24: a distinct figure, not surface moisture).** The Smoker deposits a `smoke` coat, 0 to 1, on food cooking in it. The vessel decides: `VesselKind` carries a `smoke` figure (0 for the Furnace, Crude Blast Furnace, Kiln and the rest; positive for the Smoker), and `Cooking.tick` lays it down per tick while the food is above the cooking point and under its char point (past char it is only soot). The coat multiplies the rot rate by `1 - SMOKE_ROT_SHIELD * smoke` and wears off: smoke falls by a constant per tick of the gap, settled on read like rot, so it buys days and does not replace drying. It does nothing to hunger, saturation or buffs; the tooltip says Smoked. A dish carries no spoilage, so smoke does nothing there.
- **Salt (ruled, 2026-10-24).** Three parts.
  - *Source.* Salt is made by boiling a Stockpot of water dry in an ocean or beach biome (`#minecraft:is_ocean`, `#minecraft:is_beach` at the pot's position): the water boiled away since the last dry leaves one `feedback:salt` per `SALT_MB_PER_ITEM`, kept in the pot as a fraction and taken whole with an empty hand on the dry pot (the status read comes back when there is none). Elsewhere a pot boiled dry leaves nothing. There is no ore vein and no world feature: sea water is the brine. Closes the old `[OPEN]` on salt source.
  - *Material.* `material/salt.json`: kind mineral, forms `{raw: feedback:salt}`, traits `seasoning` (so the dish finish takes it by trait, as honey) and a new `saline` (what the salting recipe takes, so no item is named). No `food` block: salt is not eaten and is not a `Cooking` entry. As a finish it feeds nothing and raises the dish's hazard tolerance as salted food does (`DishBake`); with no `molten` block `PrepWork.flow` reads it as water, so the pour floods fast.
  - *Salting.* Osmotic, by the crafting recipe `feedback:food_salting`: one food and one saline item, any arrangement, give that food with `salt` 1 and moisture lowered by `SALT_DRAW`. The spoilage up to that moment is settled first (the server's overworld clock; a client with no server previews an empty result), so salting never shields rot already earned. Salt multiplies the rot rate by `1 - SALT_ROT_SHIELD * salt` and, unlike smoke, never wears off: it stays until soaked, and leaching is not built. It also divides the pathogen and toxin concentrations by `1 + SALT_TOLERANCE * salt` wherever hazards are built (`Cooking.apply`, the ladled meal, `DishBake`). A salted food in the pot passes its salt into the mixture as a concentration (`saltMass / volume`) used the same way; a pinch cannot be added to the pot directly.
- **Sugar and honey.** Keep, because they are dry and dense; honey is already a material.
- **Canning (ruled, 2026-10-24).** Seal a jar in the Pressure Vessel: heat held above the pathogen's kill point *under pressure* sterilises, and the seal stops growth until opened. Overrun: overcooked into mush (a class drop). Late tech; also a use for the Pressure Vessel that is not a hazard. Closes the old `[OPEN]` on the Pressure Vessel holding no items.
  - *Physics.* Pressure raises the boiling point, so jars in a pressure vessel get hotter than 100 Tu, which plain boiling cannot. The vessel holds one temperature for its contents, relaxing (`POT_RELAX_PER_TICK`) toward `min(fire below, POT_BOIL_TU + CANNING_BOIL_PER_PU * Pu)`, the fire being `HeatSource.below` like the other pot-likes. Sterilising heat is `CANNING_STERILISE_TU` (116 Tu); with 6 Tu per Pu it is reached at about 2.7 Pu, so the working window is 2.7 to 5 Pu, under `PRESSURE_VESSEL_DANGER_PU`. Past the danger line the burst ladder is unchanged. Jars add nothing to pressure and do not explode; the ladder is untouched by an inventory.
  - *Items.* `feedback:jar` (a glass jar: one iron plate over two `c:glass_panes`). One jar and one food (a `Cooking` entry) or one pot serving (`MealItem`), any arrangement in the crafting grid (`feedback:jar_fill`), give `feedback:jarred_food`: a `canned` component holding a count-1 copy of the content, `sterile` 0 to 1, `sealed`, `mush` 0 to 1. The name is "Jar of" plus the content's name. The jar copies the content's `food` component (scaled by mush) onto itself so it is edible, and eating it returns the empty jar (`usingConvertsTo`). The hazards, Hunger from pathogen, Poison from toxin, and the buff doses, are the content's own, read through `Cooking.apply` and `BuffEvents`; canning does not remove toxin (heat-stable), only pathogen.
  - *Process.* The vessel has `CANNING_SLOTS` (4) jar slots, no GUI: right click with a jarred item to put one in (action bar "n of 4 jars"), empty hand while sneaking to take the last out, an empty hand not sneaking still reads pressure; breaking it drops the jars. Once a second, while the temperature is at or above sterilising heat, each unsealed jar's `sterile` rises by 20 ticks over `CANNING_STERILE_TICKS` (30 s). At 1 the jar seals: its content's spoilage is settled to now first, then pathogen is set to 0 (a serving loses its baked Hunger effect) and the clock is frozen, since a sealed jar is never read through the lazy clock. A jar kept at that heat after sealing gains `mush` over `CANNING_MUSH_TICKS` (2 min), which scales hunger and saturation by `1 - mush * (1 - FOOD_CHARRED_SCALE)` (overcooked into mush, a class drop like charring). Progress is written to the stack once a second; nothing is ticked outside the vessel. An unsealed jar is just a food in glass: it spoils as the food does, settled when eaten.
  - *Smallest choices.* Mush starts at the moment of sealing and ramps linearly, rather than after a grace period of `CANNING_MUSH_TICKS`: the 30 s sterilising time is the only grace. Temperature is a plain float on the vessel; jars out of the vessel do not cool or cook. An empty vessel looks at its heat once a second and takes the missed relaxation in one step. Heating below sterilising heat does nothing to the food (no partial pathogen kill). Unsealed jars rot at the food's own rate even inside a hot vessel. A meal in a jar is not given spoilage (a serving carries no `FoodState`).
- **Refrigeration (ruled, 2026-10-24).** The late answer to the ice that melts: the **Refrigeration Unit**, a heat pump on a wire beside an Ice Box. Closes the old `[OPEN]`.
  - *Block.* `feedback:refrigeration_unit`, a Condenser between two copper coils, ringed by iron plates. An electrical **sink** (`ElectricNode`, LV band like the Electromagnet) asking `REFRIGERATION_AMPS` (2). It touches the box on any of six faces and finds it by the `Refrigerable` interface (`core/thermal`), not by block id.
  - *Holding cold.* While the unit is fed (`ElectricRules.rate` above 0) it calls `holdCold()` on each adjacent `Refrigerable` every 10 ticks. The Ice Box keeps a `heldUntil` game time (`REFRIGERATION_LAPSE_TICKS`, 30), and its own once-a-second tick treats a held box as having the cold reserve: the temperature relaxes toward `ICEBOX_COLD_TU` as with ice, no ice is taken from the slots and none of the melting reserve is burned. Ice still works when the unit is dead. A dead unit stops holding within a second and a half, and the box then drifts to the room by the existing `ICEBOX_DRIFT_PER_SECOND` code; nothing is duplicated.
  - *Heat rejection.* A pump moves heat, it does not delete it. The unit is a `ThermalBody`: running drives it with `Heat.tick` toward `REFRIGERATION_HOT_TU` (130 Tu; it settles near 115 Tu, over the scald line, in about half a minute), off lets it fall toward the room (a few minutes). `HotContact` burns a bare hand on it with no further code. The room is not heated: a `HeatSource` is read from the block below and the unit is not under a pot, so the cost is the Eu drawn plus the hot spot. Smallest choice: it heats whenever it is fed, not only while the box is above cold.
  - *Sensing vs stopping.* No thermostat and no switch: it asks for current whenever it exists, and the player wires it through the breaker, relay and controller blocks. A box already at cold still costs the full draw.
  - *No wear, no danger* beyond the hot surface: wear is only from misuse and there is none here. The LV band is not enforced on it (no over-voltage wear), as with the Charger. No Jade line of its own (the generic electric readout applies).
  - Not built: room heating, a refrigerated Ice Box visual, a temperature readout on the unit, a Jade line for the box's held state.

Each of these changes `moisture`, temperature or a protective figure; none is a flat "x0.5 rot" label (Firmalife's traits are exactly that, and cannot show that half-dried is half-kept).

## 9. Hard-rule check

- Identity: food by material and tag; the player sees properties. ✓
- A machine that knows a recipe: pan, pot, rack, ice box apply heat/cold to a mixture. The tooltip name is derived. ✓
- Continuous knob: none are settings; minigame analog input allowed (ruling 5). ✓
- Dial controllers, wear from uptime: none. A burnt pot is the result of one act. ✓
- Precision hard gate: none; bad food is an outcome. ✓
- Electricity as currency, tier requirements: none. Refrigeration is one optional electric sink beside a box that still works on ice; heat and pressure were not required. ✓
- Units: nothing minted. Moisture is a fraction (like condition); concentrations reuse `Qu/mB`; temperatures `Tu`. Work is not touched. ✓

Axes advanced (§14): thermal process control, logistics (keeping and carrying), chemistry (partitioning and dilution), and the manual-to-automated line.

## 10. Prior art

Read 2026-10-24, design only, nothing copied (`THIRD-PARTY-LICENSES.md`, *Food*): Cuisine Delight (LGPL-2.1, `1.21`), ModularDelight (MIT, Fabric 1.20.1), Firmalife (MIT, `1.21.x`). Taken: item state with a lazy clock; apply-and-strip place preservation (reworked into a drifting thermal volume); oven as a crucible with a temperature window; meal as baked parts; stage-to-visible-item swap. Rejected: score and variety bonus; item-keyed machine recipes; flat trait multipliers; fuel whitelists; boolean heat; nutrition halved-and-averaged. Hazard classes and moisture-as-axis have no precedent in any of the three.

## 11. Build order

1. `FoodState` component, the `food` block on materials, and the ladder in the **existing** vessels; eating math; hazards. Replaces `VanillaFallback`'s food branch (the `smoking` recipe type is no longer the signal). Wiki articles for every changed vanilla food. Unplayed until you play it.
2. Skillet minigame (reuse `SolderScreen`'s structure and the server-recompute pattern).
3. Stockpot and serving items; the pot minigame.
4. Rot on read, the Ice Box, the drying rack, smoking coat, salt.
5. Canning in the Pressure Vessel (built 2026-10-24); buffs (can come with step 1 for single foods).
6. Refrigeration (built 2026-10-24), dough (built 2026-10-24: section 6), anything `[OPEN]`.
7. Modular dishes (ruled wanted, 2026-10-24): `docs/feedback_modular_dishes_spec.md`. A tool type on the modular-items core; needs steps 1 and 3 first.

## 12. `[OPEN]` summary

Scaling curves for §4.5; how coarse the lazy integration may be; whether toxin dilution is exploitable (a diluted poison potato in a huge stew is a known real behaviour; if it is a meta, tolerance needs a floor); the compound list; stamina regeneration from food; pot minigame input (settled: held); whether the knead class should carry through the flatbread step and into a dish part; whether a dough's `hardness` should ever drive the knead; whether salt tolerance makes toxin-dilution a meta (a salted rotten stew); leaching to un-salt; whether `dried` and `charred` are components of the vanilla item or new items; how `VanillaFallback.isFood` retires without breaking modded `smoking` recipes; whether repeated eating of one food should tire the palate (rejected for now as unphysical; revisit only if the no-best-food check fails in play).


## Kept on purpose: the warming floor (author ruling, 2026-10-24)

A dish on a container with the `warming` perk (blaze rod) is held at `FTuning.WARMING_HOLD_TU` (70 Tu) wherever it is, and a warm meal comforts (a little Regeneration, frost shaken off). The floor does NOT decay and needs no heat source: the blaze rod is a heat source by nature, and the cost is the blaze rod itself plus the Nether trip to get it. Deliberate; a design review flagged it as a free heal, and the author ruled it stays. Hot-holding does not stop rot in this build (`Cooking.current` reads ambient and the floor is an item-heat figure only).
