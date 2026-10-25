# Modular foods

> Status: design spec, 2026-10-25. Nothing built. Companion to `docs/feedback_food_spec.md` (state, ladder) and `docs/feedback_modular_dishes_spec.md` (dishes, cut). Every figure `[OPEN]` until played. Rulings by the author are marked **(ruled)**.

## 1. What was asked

- **(ruled)** Modular dishes exist; modular *foods* do not. A food's identity is read off what was done to it: slice bread and cook it on and it is croutons; dice potato a lot and bowl it, mashed potatoes; well-cut tomato with other things in a bowl, salsa.
- **(ruled)** Names are derived on the fly from parts and state, the way `GunDesignation` calls a handgun with a cylinder a revolver. Nothing authored per result.
- **(ruled)** More crops, so more foods. Sprites from Croptopia (MIT, user's reading of the licence; the repo `LICENSE` is MIT).
- **(ruled)** A named result shows its own sprite where one exists; otherwise it keeps the sprite it already had.
- **(ruled, scope)** Everything: naming, Croptopia ingredient items, and growable crop blocks.

## 2. The model

A name is a **rule over facts**, not a recipe. Facts of a single food stack: the food material's traits (`starchy`, `vegetal`, ...), `FoodCut` level and grade, `Cooking.doneness`, moisture class, smoke, salt, and rot. Facts of a dish add the container kind, the roles filled and their parts' facts.

Rules are data, `data/feedback/food_name/*.json`, one file per name, tried in file-id order, first match wins (the gun-designation precedent):

```json
{ "name": "croutons", "traits": ["starchy"], "without": ["wet"],
  "cut": 1, "doneness": ["dried", "charred"], "model": 1 }
```

`name` is a lang key tail (`food.feedback.name.<name>`); `model` is a `custom_model_data` int, absent or 0 for no sprite of its own. A dish rule adds `container` and `roles` (traits required of the main/base/side, count of parts, minimum cut).

Nothing in play depends on a name: hunger, saturation, hazards and buffs already come from the state. The name is how the player and the wiki call the thing, like a gun designation. It is not a gate, so it cannot become a recipe list by the back door (digest: no identity checks).

### 2.1 Single foods

`Cooking.apply` is the one place a food's state becomes a stack (cook, rot, drying all pass through it) and the Prep Table's cut finish is the other. Both call `FoodNames.stamp(stack, entry, state)`, which matches the rules and writes `DataComponents.ITEM_NAME` and `CUSTOM_MODEL_DATA` (or removes them when nothing matches, so a rule that stops holding restores the vanilla name and sprite). Server side only; the components sync like any.

Sprites: a base item that has a named variant gets an `overrides` entry on its model keyed on `custom_model_data`. Variant textures are Croptopia's where one exists (`salsa`, `mashed_potatoes`, ...) and a recolour of the base sprite otherwise. No rule means no component means the old sprite.

### 2.2 Dishes

`DishItem.getName` already builds "X Bowl". It first asks the dish rules; a hit replaces the generated name ("Mashed Potatoes", "Salsa") and `model` picks the sprite via the same component. The structural name is the fallback, unchanged.

Facts for the rules come from the baked parts (`Part.dims`: cook, moisture, `cut`, `evenness`), so a well-cut tomato part (even, minced) beside vegetal side parts in a bowl is salsa, and a ragged cut is "Chunky" something or no name at all.

### 2.3 Crops

Croptopia's crops become Feedback crops, **data first**: a `crop` registry/table (`data/feedback/crop/*.json`: id, seed item, product, stages 4, growth, soil/biome preference as properties) read by one `FeedbackCropBlock` class generated per entry, instead of a Java class per crop. Products are food items whose `material/*.json` carry traits and a `food` block, so they join every system for free. Textures: `item/<crop>`, `item/<crop>_seed`, `block/<crop>_crop_stage0..3`.

Seeds come from grass and loot like wheat seeds, by tag, not id.

**Growth reads the world (ruled 2026-10-25), for vanilla crops too.** One growth rule serves every crop: each crop (ours and vanilla's) has its optimum temperature band and light need as properties, and a random tick grows it only as often as the place suits it. Ambient comes from `AmbientTable` (by dimension and biome) and the crop's heat from `Heat` where a block is warmed. A cold biome slows wheat, a hot dimension scorches it, a frost halts it (ruled: halts, never kills, so it recovers when it warms); nothing is a gate, it is a rate (digest: precision is never a hard gate), so a crop outside its band still grows, slowly.

Vanilla crops are overridden by NeoForge's `BlockEvent.CropGrowEvent.Pre` (result `GROW` / `DO_NOT_GROW` / default), not by mixins or replacing blocks, so modded crops that fire the event follow the same rule for free. Vanilla crop tolerances are a datapack table keyed on the crop **block tag** (`feedback:crop_climate/*.json`: tag, band, light, rate), never a block id, so a modded crop joins by tagging. Bonemeal is untouched. Wiki gets a Crops article saying what each vanilla crop now wants (changed vanilla block rule).

## 3. Licence

Croptopia is MIT. Textures copied from `shared/src/main/resources/assets/croptopia/textures/` go into `assets/feedback/textures/` with a table in `THIRD-PARTY-LICENSES.md` (Silent Gear precedent), copyright "Thonk, Rainbowcraft2" per its `LICENSE`. Code is not taken, only the sprite files. Checked out at `../Croptopia-reference`, branch `development` (no 1.21 branch exists upstream; the sprites do not change between versions).

## 4. Hard-rule check

- No identity checks: rules key on traits and state. Pass.
- Not a recipe list: names are labels; the figures are unchanged. Pass.
- No scalar meta to mine: a name gives nothing. Pass.
- Perishable: a named food still rots; stamping on every `apply` means a rotten salsa is still salsa with its rot line. `[OPEN]` a "spoiled" prefix.

## 5. Build order

1. `FoodNames` + `food_name` table + lang + stamp in `Cooking.apply` and the cut finish. A handful of rules: croutons, mashed potatoes, salsa, fries, chips.
2. Dish rules in `DishItem.getName`.
3. Sprites and overrides for the base items that gain names.
4. Croptopia ingredient items and food materials (no growing yet), sprites copied.
5. Crop climate table, `CropGrowEvent.Pre` handler over every crop (vanilla included), then the crop table and block, seeds from loot, worldgen is out.
6. Wiki: a Foods article listing what each rule means, and every vanilla item whose name now changes.

## 6. `[OPEN]` summary

Crop climate figures; a spoiled prefix; whether `custom_model_data` int per variant or a string-keyed component; meat shapes (patty, meatball) from the dish spec still open; stack-merge: two stacks of the same base with different names must not merge (state already differs, `ItemStack.isSameItemSameComponents` covers it).

## 7. Generated names (2026-10-25)

A rule's `name` is a lang pattern; `args` lists the roles (`main`, `base`, `side`) whose material names fill its `%s` slots, in order. A rule matches only when every arg role is filled. `cuisine` (a trait: `italian`, `east_asian`, `mexican`) matches when the dish's parts vote that cuisine, one vote per part carrying the trait, a tie lets every leading cuisine's rules try, in file order (tomato is both italian and mexican). Files sort by id, so a specific rule goes before a general one. Soup naming is open: a soup is a pot serving with no ingredient list, so the pot must record its ingredients' traits first (the borscht rule was removed for that). Also built: prefixes spoiled/stale/burnt/chunky; a `wet` flag on crop climate lines for paddies. Built: "Pasta e %s", "Pasta al %s", "Stir-fried %s Noodles", "Arroz con %s", "%s Rice Bowl", "%s Burrito", "Noodles with %s".

Conventions a cuisine file can hold, each a pattern *shape* (word order, linker, which slot leads), not a vocabulary list:

1. Linker + ingredient (Italian): "Pasta e Fagioli", "Spaghetti alle Vongole".
2. Method + main + "with" side (Chinese): "Stir-fried Beef with Broccoli", cut word first ("Shredded Pork").
3. Place or cook as the name (French): "Beef Bourguignon", "Chicken Kiev"; needs a cuisine's style tag picked by the vote.
4. Sauce leads (French): "Chicken in Cream Sauce".
5. Method as the noun, ingredient as adjective (Japanese): "Chicken Katsu", "Teriyaki Chicken".
6. Bare ingredient list: "Chicken and Rice", "Arroz con Pollo".
7. Texture or shape word first: Crispy, Creamy, Chunky, Silky, Mashed.
8. Fused compound (German, Finnish): one word built from the parts.
9. Mood or possessive (diner): "Grandma's Stew", "Chef's Salad", "Bowl of Fire" from the finish.
10. Origin prefix: Szechuan, Cajun, Tuscan, chosen by the vote.

Further reach, `[OPEN]`, none built: a grammar of pieces (head noun, state prefixes, "with" tail); method words (needs a method fact on the food); grade words from the cut, pour and knead classes; a cosmetic dishes-tried codex (never a gate, or it becomes a recipe list); a dish sprite layered from the bowl, a base mound tinted by the base's colour and a topping tinted by the main's; one shared namer over fact bags for food, brews and alloys (`GunDesignation` is the precedent).

## 8. Food perks (2026-10-25)

**Built mechanism.** A food has compounds (stats everything has: sugar, protein, vitamin, mineral, stimulant) and now also perks: traits usually unique to one food, the way silver has `smite`. A perk is a file `data/feedback/food_perk/<id>.json` with a `kind` and that kind's fields; a food names it in its `food` block as `"perks": {"<id>": potency}`, potency 0 to 1 per unit of mass, the same meaning as a compound strength. `Material.Food.actives()` returns compounds plus perks keyed `perk:<id>`, and the stockpot, the dish bake and the single-food dose all blend that one map by mass, so a perk's strength in a meal is its concentration: a potency of 0.9 at a fifth of a pot blends to about 0.18. A potent food reaches full strength in a small share, a mild one needs most of the dish. Perks ride the serving dims as `c:perk:<id>`, so a soup keeps them.

In `BuffEvents.dose` a `perk:` key looks up `FoodPerk` instead of `CompoundTable`, is skipped under `FOOD_BUFF_MIN_STRENGTH`, and is scheduled in the same persistent dose list with `"kind":"cleanse"` (an entry with no kind is the old effect dose) and the same absorb delay, so fat and fibre slow it. `FoodPerk.apply` switches on `kind`; an unknown kind is logged and ignored. Kinds in `FoodPerk.Def` (data: `effects`, `max_amplifier`, `target`, `factor`, `ticks`, `grant`, `drawbacks`, `traits`): `ward`, `resist` and `tune` arm a timed trait list on the player (`FoodPerk.trait`, `warded`, `tick`; ticked twice a second only while one is live; cleared on death), `purge`/`tolerance` divide the hazards in `Cooking.tolerance(salt, actives, hazard)`, `tune` also reshapes `BuffEvents.dose` (traits `absorb`, `duration`, `warm_regen`). `grant` and `drawbacks` are vanilla effects given at dose time, ticks x strength. Cleanse: for each listed effect the player has at or under `max_amplifier`, its remaining duration is cut by the strength (1 removes it, 0.4 removes 40% of what is left). A debuff above the cap, or not listed, is untouched. First perk: `settling` (nausea, hunger, cap II), on ginger.

### Kinds (all BUILT 2026-10-25, unplayed)

- **cleanse**: shortens listed effects up to a level cap.
- **ward**: for a while, listed effects cannot be applied (temporary immunity), capped by amplifier too.
- **purge**: reduces live pathogen or toxin on the player's next meal or in the gut; needs the food-state hooks.
- **resist**: damage type or environment resistance, a vanilla effect or a modifier for a while.
- **tune**: lengthens or shortens, or raises or lowers the level of, another effect while it lasts.
- **tolerance**: moves the salt or pathogen tolerance of what is eaten with it.

### Perk list (all BUILT 2026-10-25, unplayed; notes name any deviation)

Ids name what the perk does, never a food, so a modded herb can share one. Each: kind, effect, cap or limit, trade-off, Minecraft lens.

1. **warming** (resist, cold): stops powder-snow freezing building and .... Examples: chile_pepper, ginger, pepper. Limit: no help against fire. Trade-off: in a hot dimension it adds nothing and the chile makes you sweat (hunger drains a little faster). Lens: freezing is a vanilla meter nobody can eat against. **Status: BUILT. resist, trait freeze_relief sheds freeze ticks each pass; drawback hunger. Chile, ginger, pepper.**
2. **cooling** (resist, heat): shortens burning and eases Nether heat. Examples: cucumber-like future crops, mint, melon. Limit: standing in lava still kills. Trade-off: in cold biomes it slows you. Lens: fire resistance without the potion. **Status: BUILT, no food carries it (mint, melon, cucumber materials missing). resist, trait cool_burn sheds fire ticks; DEVIATION: no Nether-heat easing (no such mechanic exists); drawback slowness.**
3. **antitoxin** (purge): lowers the toxin of what is eaten in the same meal. Examples: garlic, onion, brown_mushroom. Limit: a fraction only; a pufferfish stays deadly. Trade-off: strong smell, hostile mobs notice you from further (future). Lens: poison in food is a number the player can now fight. **Status: BUILT. purge, toxin divisor 1 + 2 x strength. Garlic, onion, brown mushroom. DEVIATION: smell trade-off is future.**
4. **antibiotic** (purge): lowers live pathogen in the same meal. Examples: garlic, onion, honey. Limit: spoiled and rotten food is past help. Trade-off: costs a little hunger bar. Lens: the raw chicken problem gets a second answer besides the fire. **Status: BUILT. purge, pathogen divisor 1 + 2 x strength (rot is not capped separately); drawback hunger. Garlic, onion, honey.**
5. **settling** (cleanse, built): nausea and hunger, cap II. Example: ginger, mint. Trade-off: none beyond needing a large share of the dish. **Status: BUILT (cleanse). Ginger; mint missing.**
6. **antivenin** (cleanse): poison, cap I. Examples: honey, milk, kelp. Limit: poison II and wither are beyond it. Trade-off: milk spoils fast. Lens: vanilla milk cures everything; this gives the cure to a cheap everyday food, weaker. **Status: BUILT. cleanse poison cap I. Honey, kelp; milk missing, no spoil trade-off.**
7. **sobering** (cleanse): darkness, blindness, slowness, cap I. Examples: carrot, golden_carrot, cabbage. Limit: not mining fatigue, not weakness. Trade-off: low hunger. Lens: carrots for eyes, lightly. **Status: BUILT. cleanse darkness, blindness, slowness cap I. Carrot, golden carrot, cabbage.**
8. **night_sight** (tune/ward): short night vision, and blindness cannot land. Examples: carrot, golden_carrot. Limit: short. Trade-off: on the surface in daylight the sharp light dazzles, a brief slowness. **Status: BUILT. ward blindness (any level) + night vision grant + short slowness. DEVIATION: the slowness is always, not only in daylight on the surface.**
9. **fortifying** (tune): lengthens beneficial food effects from other compounds in the meal. Examples: salt, rice, bread. Limit: caps at double. Trade-off: raises salt load, thirst-like hunger drain. Lens: salt as preservative and enhancer. **Status: BUILT. tune trait duration, caps at double; drawback hunger. Rice, bread; salt has no `food` block so none on it.**
10. **slow_release** (tune): stretches duration of the meal's buffs but delays them more. Examples: potato, rice, noodles. Limit: nothing new, only reshaped. Trade-off: the buff arrives late, bad when you need it now. Lens: complex carbs. **Status: BUILT. tune trait absorb +1: later and longer, no crash. Potato, rice, noodles.**
11. **quickening** (tune): meal buffs arrive sooner and end sooner. Examples: honey, sweet_berries, tomato. Limit: crash follows, as sugar does. Trade-off: shorter. Lens: sugar rush. **Status: BUILT. tune trait absorb -0.9: sooner and shorter, and the existing crash rule fires. Honey, sweet berries, tomato.**
12. **bracing** (resist, damage): small absorption for a while. Examples: beef, mutton, pork. Limit: small, one hit worth. Trade-off: heavy meat slows the dose. Lens: a hearty meal takes a hit. **Status: BUILT. resist, absorption grant (one hit). Beef, mutton, pork. DEVIATION: slow dose comes only from the meat's own fat.**
13. **mending** (tune): speeds natural regeneration for a while. Examples: soybean, cod, salmon. Limit: only while fed. Trade-off: no saturation in it. Lens: protein heals slowly, not instantly. **Status: BUILT. tune timed trait natural_regen heals while food level is 18+. Soybean, cod, salmon. No-saturation trade-off not built.**
14. **steadying** (ward): knockback cannot land for a short while. Examples: potato, turnip, beetroot. Limit: explosions still push. Trade-off: slowness I while it lasts. Lens: rooted vegetables, rooted player. **Status: BUILT. ward trait knockback_ward scales the knockback strength down; explosions untouched; slowness I drawback. Potato, turnip, beetroot.**
15. **lightfoot** (tune): short speed, and stamina refills sooner. Examples: sweet_berries, corn. Limit: stamina only, not carry weight. Trade-off: hungrier. Lens: berries on the run; stamina is the one meter. **Status: BUILT. tune: speed grant, timed trait stamina_regen multiplies `Stamina` regeneration; drawback hunger. Sweet berries, corn.**
16. **bitter_tonic** (cleanse): weakness and mining fatigue, cap I. Examples: beetroot, tea, kelp. Limit: nothing against hunger or poison. Trade-off: leaves a short bitter slowness. Lens: a tonic that hurts first. **Status: BUILT. cleanse weakness, mining fatigue cap I; slowness drawback. Beetroot, kelp; tea missing.**
17. **brine_tolerance** (tolerance): the same dish tolerates more salt before it turns. Examples: kelp, cod, salt. Limit: not toxin. Trade-off: the dish is salty, thirstier. Lens: sea food for sea salt. **Status: BUILT. tolerance, multiplies the salt tolerance for pathogen only; drawback hunger. Kelp, cod; salt has no `food` block.**
18. **sleep_ward** (ward): a short delay before phantoms mark you, and a bed-less rest. Examples: milk, chamomile-like herb, honey. Limit: only for one night. Trade-off: drowsy, slowness I. Lens: phantoms count nights awake. **Status: BUILT. ward trait rest cuts the time-since-rest stat (phantom clock) by a share; slowness drawback. Honey; milk missing. DEVIATION: set back, not a bed-less rest.**
19. **breathing** (resist, drowning): lengthens air under water. Examples: kelp, cod, salmon. Limit: the air bar only, no damage cut. Trade-off: heavy, slower swimming at depth. Lens: kelp as a diver's food. **Status: BUILT. resist trait air refills air supply while underwater (0.6 = 60% slower loss); slowness drawback. Kelp, cod, salmon.**
20. **digestive** (tune): the meal's fat and fibre slow absorption less, so buffs come sooner. Examples: ginger, mint, tea. Limit: no stronger. Trade-off: less saturation from the same bowl. Lens: spices that speed digestion. **Status: BUILT. tune trait absorb -0.5. Ginger; mint, tea missing; less-saturation trade-off not built.**
21. **hearth** (resist): after a warm meal, regeneration lasts longer. Examples: potato, bread, milk. Limit: only warm food, cold meals get none. Trade-off: slow to arrive. Lens: soup in winter; ties to Feedback's warm-meal rule. **Status: BUILT. tune trait warm_regen lengthens the warm-meal regeneration (only in stews and dishes, which carry the warm-meal rule). Potato, bread; milk missing.**
22. **keen_edge** (tune): the next few strikes land harder while it lasts. Examples: chile_pepper, pepper, garlic. Limit: short, no stacking with strength potions. Trade-off: lowers resistance briefly (hurt more). Lens: spicy, reckless. **Status: BUILT. tune timed traits strike (outgoing damage, skipped under Strength) and frail (incoming). DEVIATION: timed, not counted strikes. Chile, pepper, garlic.**

## 9. Pies (2026-10-25)

A pie is formed, cooked as a whole, then eaten (author: "pies are something you form and then cook and then eat"). Type `modular_type/pie.json` (item `feedback:pie`), dish family with `bakes: true`: container C and lid T take the new `pastry` trait (material `pastry`: raw `pastry_dough`, cooked `pastry_shell`; dough + egg makes two), main M a `vegetal` filling, optional side S a `protein`, finish F seasoning (sugar), seal 1. A role may also list `any_traits` (a part needs at least one, besides all of `traits`): main and side both take vegetal or protein, so a pie is vegetal, meat or both; pastry carries neither, so it cannot fill them, and a food cannot be the shell. Name rule `15_pie.json` fills "%s Pie" from the main.

Built, unplayed: a `bakes` dish is assembled with the cook of its rawest part (`DishBake`, not the served cook 1), so a fresh pie feeds raw figures and carries its parts' pathogen. `Cooking.findCookable` (vessel only, not the skillet) lets a `bakes` dish be a vessel input; `Cooking.tick` heats the dish's own `FoodState`; when its cook reaches 1 `ModularStacks.bakeThrough` sets every food part cook 1 with the pathogen and water the heat left, clears the state and bakes again, so the cooked figures, meal and food component are the cooked pie's. Chosen over a lazily interpolated dish cook because the bake already interpolates per part. A baked pie left in the heat keeps drying and charring on the dish state. Pumpkin now has a cooked form (`roasted_pumpkin`); `DishBake` interpolates a part whose raw item is no food (pumpkin, egg) from zero to its cooked item by the part's cook, instead of skipping it.
