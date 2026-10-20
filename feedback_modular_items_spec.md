# Feedback — Modular Items

**Working document, not one of the three.** Tools, melee weapons, ranged weapons and armour as
*baked designs*: parts, each a material plus a few discrete geometry choices, baked once into
figures stored on the item. The gun's pattern (`feedback_guns_spec.md`, `weapon/GunDesign`)
widened into a shared core. Built in part: §12 says exactly what.

Prior art read 2026-10-15 (`THIRD-PARTY-LICENSES.md` gets its rows when code lands):
**Silent Gear** (MIT, 1.21.1), **Tinkers' Construct** (MIT, 1.20.1 only — no public 1.21 branch),
**Tetra** (no public source, All Rights Reserved — design read from its wiki only, nothing
adaptable). What was taken and rejected is in §9.

---

## 1. What the author asked for (rulings, 2026-10-15)

- **One core for tools, melee weapons and armour**, and guns move onto it once it holds (§8).
- **Every vanilla weapon is represented** — sword, axe, mace, trident, bow, crossbow, shield — not
  "most of them". Maces and tridents become modular even though it makes them less special.
- **Custom types** beyond the vanilla set (§5).
- **Two assembly routes.** Simple tools: the parts *are* the design, put together on the bench
  (Tinkers' shape). Complicated items (crossbows, compound tools): designed on a stand, printed
  as a blueprint, parts made to it (the gun's shape).
- **A blueprint predicts; it does not promise.** It gives the figures you *will* make if every
  part comes out exactly to drawing, which early on it won't. What you actually *have* is
  measured with instruments (§6).
- **Vanilla enchanting stays, fully.** (Footnote: a later rework may enchant *parts* rather than
  whole items — not this spec.)
- **Vanilla tools and armour go the way of the furnace**, and loot, mob gear and villager trades
  are converted, not dropped: a stone shovel from a chest is still a "Stone Shovel", but it is a
  modular item underneath (§7).
- **Depth like Silent Gear**: many figures that interact, so a bad build is possible, legal and
  learnable. Nothing stops you making a terrible sword; you find out by testing it.

## 2. The model

A **design** is a layout (what kind of item) plus an ordered list of **parts**. A part is:

- a **role** in the layout (head, edge, handle, binding, limb, plate, ...);
- a **material** (`material/*.json`, by id — data, not an identity check on an item);
- a few **discrete geometry choices** for that role (§3);
- its **as-built state**: what the forge actually produced (§4), and its heat-treatment state.

**Baking** is a pure function of design and the material table, run once when the item is
assembled or changed, and stored on the stack as a component — exactly `GunDesign.bake` into
`GunFigures`. Nothing reads a stat table per material; every figure is derived.

### 2.1 Figures, all derived

| Figure | From |
| --- | --- |
| mass (`Mu`) | each part's volume (from its geometry) × `density` |
| balance | where the mass sits: head mass × handle length over total — *head-heavy* or *light in the hand* |
| swing speed | falls with the moment of inertia about the hand (mass and how far out it sits) |
| impact | head mass × swing speed, the work a blow carries |
| bite | edge `hardness` against the target's — under it the edge skids, over it it cuts |
| chip / bend | `toughness` and `yield_strength`: a hard, brittle edge chips; a soft one rolls and dulls |
| reach | total length |
| harvest | edge hardness against the block's hardness — a hard gate by arithmetic, the hammer's `St ÷ hardness` rule again |
| durability | volume of the wearing part × `toughness`, scaled by heat treatment |
| enchantability | a new `enchantability` material figure (vanilla already treats it as a material property: gold enchants well) |
| protection (armour) | plate `hardness` and thickness against a blow's `Fu`; coverage by piece |

The depth argument: in Silent Gear most stats are weighted averages and the interaction lives
only in traits. Here every figure pulls on others. A heavier head hits harder and swings slower;
a longer handle reaches further and loads the binding; a harder edge bites more and chips; a
softer spine absorbs and dulls. None of that is authored, so it cannot be balanced away into one
right answer.

### 2.2 Integrity is a joint, not a number

Tetra's integrity is a capacity budget that parts spend and handles provide. Here it has a
cause: **the handle and binding carry the head's leverage** (head mass × handle length). A
binding's capacity comes from its material (`yield_strength`, `friction`) and its choice
(wrapped, pinned, socketed). A design over its joint is *legal* — it builds — and its joint
works loose with use, which is wear from a specific mistake. Readable, never instant.

### 2.3 Wear

- **Durability is the accepted consumable exception** (CLAUDE.md, Hand Hammer ruling,
  2026-10-09). A pick is spent by digging.
- **Misuse costs extra, and says which misuse.** An edge on something harder than itself chips;
  an axe into stone dulls; an over-leveraged joint loosens. Same rule as the Mechanical Hammer:
  force that cannot go into the work goes into the tool.
- **Broken is not destroyed** (Tinkers' `tic_broken`). At zero the item stays in hand and acts as
  an empty hand until repaired. Figures degrade in steps on the way down (Silent Gear), so a worn
  tool is visibly worse before it stops.
- Repair puts material back into the worn part. **Decided 2026-10-16 (overnight, delegated):**
  by hand on the Assembly Bench -- the worn item and the stuff its wearing part is made of, each
  item putting back its share of the durability (`1 / the part's cells`). Re-forging stays
  possible and is the same recipe: the item and a *different* material, in as many cells as one
  of its parts has, swaps that part, and a swapped wearing part is a fresh one.

## 3. Discrete choices

Per the house rule (philosophy §3): named options, never sliders. Each is a real trade, and where
it lives was decided by asking where the player would naturally *do* it (2026-10-16):

- **Edge:** keen / standard / blunt -- bite against chipping. Ground on a finished tool, so it is
  set by sneak-scroll with the tool in hand (the Hand Hammer's swing and the Welder's mode are the
  precedent) and rebakes the item.
- **Length:** short / long. The *pattern* is the choice: a handle is a number of cells, one per
  stick, and a longer pattern is a longer handle. Reach and leverage against swing speed and
  joint load. Nothing is dialled.
- **Binding:** wrapped / pinned / socketed. Read off what is put in: a fibre (leather, string)
  wraps, a metal wire pins, and no binding is a socket -- the handle simply fits the head.
  Joint capacity against cost.
- **Section** (flat / ribbed, mass against stiffness) and **heat treatment** are **not choices
  here.** Heat treatment is the *material*: hardened steel is its own file (mechanics 5.3), so a
  quenched blade is a hardened-steel blade and tempering is a material change, not a flag on the
  part. Section is **[OPEN]**: it needs a part that can carry a shape, and a part is an ordinary
  form item.

Tetra's *mutually exclusive improvement groups* are this list: one choice per axis, swapping
replaces.

## 4. As built vs as drawn

Each part was to carry its **target** geometry and its **built** geometry, as the gun barrel does
(`BarrelGeometry`), with the bake reading the built one. **Decided 2026-10-16 (overnight,
delegated):** kept minimal. Parts are ordinary form items (an ingot, a plank, a rod), so there is
no part to carry a geometry, and a head comes out at its target size. Built = target for now.
**[OPEN]** Which processes would introduce how much deviation (hand forging most, the Press
least, grinding corrects an edge) -- it needs part forms that can be hammered to a size, which is
the same thing section waits on (§3).

## 5. Types

Defined as data, one file per type (Tinkers' tool definitions), listing roles, actions, the
block tags it is effective on, and which choices apply. A new type is a JSON file and a lang
line, not a class. A type has a parent (Silent Gear's tree), so a cleaver inherits from axe.

**Vanilla, all of it:**

| Item | Parts | Physics |
| --- | --- | --- |
| pickaxe, shovel, axe, hoe | head, handle, binding | §2.1 straight |
| sword | blade, guard, grip, pommel | pommel is counterweight: it moves balance, the swing-speed choice |
| mace | head, haft | **damage from the fall**: impact is head mass × the wielder's fall speed. The heavy core is a material (§5.1) |
| trident | tines, shaft | thrown: mass and length set flight and the hit; Riptide/Loyalty stay enchantments |
| bow | limbs, string, grip | draw weight from limb `yield_strength` × section; heavier draw, faster arrow, slower draw |
| crossbow | prod, stock, lock | blueprint route; the lock is a machined part like a gun's action |
| shield | face, rim, grip | blocks a blow's `Fu`; face material takes heat (the approved thermal shield idea) |
| helmet, chestplate, leggings, boots | plate, lining | §5.2 |

**Decided 2026-10-16 (overnight, delegated):** shears are modular (two blades and a pivot), built
after the vanilla weapons. **Fishing rod, flint and steel and brush stay vanilla:** none is a tool
whose figures a material changes in a way a player would act on -- a rod's cast, a spark's
strike and a brush's sweep do not get better by being iron rather than copper in any way this
model can say. If one ever does, it is a type file and an item.

**Custom, decided 2026-10-16 (overnight, delegated): mattock** (dig and chop, each at half),
**sickle** (area harvest), **cleaver** (heavy, short, parent axe), **pike** (long, slow) and
**maul** (a head on a handle that stuns what it hits), built after every vanilla type. **Pry bar**
(lifts a block whole) and **prospector's pick** (strikes rock and reports an adjective about what
is in it -- an instrument, so it reads and never acts) stay **[OPEN]**, future.

### 5.1 Heavy core

A material file like any other. **Minecraft lens:** the heavy core is found only in trial
chambers, sealed, and it is the one item vanilla makes *dense on purpose* (the mace's whole
mechanic is its mass). Smallest departure: a metal far denser than anything smeltable — about
three times iron — that cannot be melted in any vessel the game has (so it is found, never
made), very hard and brittle. As a mace head its density is the point; anywhere else its mass
is a cost. **Decided 2026-10-16 (overnight, delegated):** `material/heavy_core.json`, kind metal,
density 23.6 (three times iron), hardness 9, toughness 20 (brittle against its hardness),
`yield_strength` high, `melting_point` 4000 (above any flame the game has, so it is found and
never made), and its form `heavy_core` is `minecraft:heavy_core`. **Not workable**: no form
step reaches it, so it is used whole, as a mace head.

### 5.2 Armour

Plate, mail and padding as layers. Plate stops a blow by hardness and thickness; mail spreads it;
padding insulates (`ThermalBody` heat from a furnace or lava) and absorbs. Mass (`Mu`) slows the
wearer — protection against mobility is a trade from physics, not a tier. Netherite stays
property-driven (`netherite.json`), never id-driven.

## 6. Reading what you made

Philosophy §8: what you need is free, what you have costs. **The tooltip gives adjectives**
("head-heavy", "keen", "brittle", "light in the hand"). **Exact figures need an instrument.**
The blueprint gives the *target's* figures for free, because it is paper.

The calipers are carrying too much already (dimensions, condition, work done). New physical
instruments, each reading one thing:

- **Balance scale** — mass (`Mu`).
- **Scratch file set** — hardness, by which file bites (an adjective ladder, then a figure).
- **Impact pendulum** (a block, Charpy-style) — toughness: strike a sample, read the energy it took.
- **Strike post** (a block) — what a weapon delivers per blow, swing after swing.
- **Calipers** — back to dimensions only.

## 7. Replacing vanilla

The furnace precedent (`TODO.md`, memory *vanilla-furnace-total-conversion*):

- Vanilla tool, weapon and armour **recipes disabled**.
- Every vanilla item has a **canonical design**: `iron_pickaxe` is an iron head, wood handle,
  standard everything. A converted item **keeps its vanilla name** ("Stone Shovel"), derived from
  the design, so nothing reads differently to a player who never opens a bench.
- **Converted on arrival**: chest and mob loot (a global loot modifier), mob equipment (on spawn),
  villager trades (trade event), and any vanilla stack that still turns up in an inventory.
- Everything that asked for a vanilla item by id moves to tags (`ecosystem-compat-priority`).

## 8. Guns on the same core

The author's suggestion: one core that guns, swords and armour all draw from. Agreed as the
destination, **not the first step.** `GunDesign` is a working 900-line record with gun-specific
fields; the shared core should be built for tools first, then guns migrated onto it once it has
held up — a rewrite of something that works is the expensive way to discover the core was
wrong. What is plainly shared: parts with materials, target vs built geometry, the bake into
cached figures, condition and heat on the item, the adjectives-vs-instruments tooltip, and the
assembly bench.

## 9. Prior art — taken and rejected

**Taken:**
- Parts on the stack and figures baked and cached on change — Silent Gear
  `GearConstructionData`/`GearData.recalculateGearData`, Tinkers `ToolStack.rebuildStats`; the
  gun already does it.
- Types as data with a parent chain — Tinkers tool definitions, Silent Gear `GearType`.
- Harvest by block tag — Silent Gear `GearDiggerTool`.
- Broken-not-destroyed — Tinkers.
- Stepwise degradation as it wears — Silent Gear `GearHelper`.
- Integrity budget (made physical, §2.2) and exclusive choice groups (§3) — Tetra, design only.
- One sprite per part tinted by material (technique, not assets) — Tinkers `ToolModel`.
- **Screen code to adapt** (both MIT): Tinkers `tables/client/inventory/module/InfoPanelScreen`
  (scrolling stat panel; depends on Mantle — port the panel, licence of Mantle unchecked) and
  `library/tools/helper/TooltipBuilder`; Silent Gear `client/tooltip/GearComponentTooltips`
  (a stat shown with *why*), `TooltipHandler`, the material book screens. Never their textures.

**Rejected, and why:**
- Hand-authored stats per material per part (both) — figures are derived here.
- Silent Gear's random **grades** and `rarity` stat — luck is not a property.
- Silent Gear's hidden 0.1–2.0 **synergy** scalar — a continuous knob nobody can see.
- Tinkers' **modifier catalogue** (sharpness bought with quartz, diamond reinforcement) — flat
  bonuses with no physical cause. Its hook architecture is fine; its contents are not.
- Tinkers' `percent` handle bonuses and continuous per-part `scale`.
- Tinkers' Part Builder (pattern + "material value") — parts come from Feedback's own forming,
  casting and machining.
- Tinkers' repair keyed on (tool id, material id) — identity.
- Per-use wear with no misuse concept (both) — kept only as the consumable exception.

## 10. Build order

1. Core: design, part, bake, figures component, tooltip adjectives. Pickaxe first, end to end.
2. The other four vanilla tools; vanilla conversion (§7) — vanilla tools cannot be deleted
   until replacements exist.
3. Sword, mace (with heavy core), trident, shield.
4. Bow; crossbow on the blueprint route.
5. Armour.
6. Instruments (§6).
7. Custom types.
8. Guns migrated onto the core. **Waits** (decided 2026-10-16): `GunDesign` is untouched until the
   core has been played; migrating a working 900-line record is the expensive way to find out the
   core was wrong.

## 11. Decided, and still open

**Decided 2026-10-16 (overnight, delegated):**
- Repair: by hand on the bench, and re-forging by the same recipe (§2.3).
- Forming deviation: minimal, built = target (§4).
- Shears modular; fishing rod, flint and steel, brush vanilla (§5).
- Heavy core: figures above (§5.1).
- Custom types: mattock, sickle, cleaver, pike, maul (§5).
- Enchantability: **one material figure**, `enchantability`, read from the wearing part -- the
  head, the blade, the plate. Vanilla scale (gold 22, iron 14, stone 5); absent reads as poor.
- Assembly is by pattern, as vanilla lays its recipes out (§12).

**Open:**
- **[OPEN]** Section (flat / ribbed) and a part that carries an as-built shape (§3, §4).
- **[OPEN]** Hand-hammered parts do not yet record work done against work required as built geometry (overrun thinner and lighter, short heavy and blunt); built = target (§4).
- **[OPEN]** Pry bar and prospector's pick (§5).
- **[OPEN]** Thrown trident damage, a crossbow's draw time and a lining's heat: the vanilla
  classes read fixed constants for these (§12 says which).

## 12. Build status

Package `modular/` (the system the code is named for, beside `weapon/` and `vehicle/`); data in
`data/feedback/modular_type/`; registry in `registry/FModular`.

**Built (2026-10-16, steps 1 and 2 of §10).**

- **Assembly is by pattern.** Each `modular_type` file lists roles (a letter, what forms of material
  fit, the volume of one cell) and patterns of letters, laid out as vanilla lays its recipes. The
  cells a letter has are the part's size: three ingots are a bigger head than one, two sticks a
  longer handle than one. A single custom crafting recipe (`feedback:modular`) matches any grid, so
  the crafting table and the 4x4 Assembly Bench both work. Parts are found by what the item is
  *made of* (`Material.find`), never by id, so wooden, stone, gold, glass and a datapack's own
  materials all work with no edit. Types are told apart by shape; where two share a shape, the
  forms their roles accept tell them apart.
- **The same recipe repairs and re-forges** (§2.3), and keeps whatever the item carried
  (enchantments, name).
- **The bake** (`ModularBake`, constants in `ModularTuning`): mass from volume and density;
  inertia and the moment the arm holds out set swing speed; a blow carries the striking mass at
  the tip's speed; bite and durability both come from the edge's competence,
  `sqrt(hardness x strength)`; the joint carries the head's leverage on handle and binding. Calibrated
  so that canonical vanilla designs land near vanilla: an iron pickaxe digs about 6 and lasts about
  250, a diamond one about 7 and 1600. **Stone is the anomaly:** its table hardness (30) is the
  hammer's, not an edge's, so a stone head passes the iron class and digs gold and diamond ore. It
  chips doing it (toughness 6), which is the physical answer; vanilla's stone tools do not.
- **Vanilla's own components are written from the figures**: `TOOL` (the tags it digs, at its
  speed, with the three tier tags denied by edge hardness: 3, 6 and 40) and the attribute
  modifiers (damage, swing speed, reach), so combat and mining need no further code. Durability is
  `MAX_DAMAGE`, enchantability a per-stack override.
- **Misuse costs extra and says why** (`ModularStacks.misuse`): an edge on a block that strains it
  past twice its toughness chips (keen chips sooner), and a head heavier than its joint carries
  works loose a little each block. A broken item stops one point short and acts as an empty hand;
  figures drop at 50, 75 and 90 percent worn.
- **Tooltip is adjectives** (head-heavy, keen, brittle, loose-jointed, worn). Exact figures wait on
  the instruments (§6).
- **Vanilla replaced** for pickaxe, axe, shovel and hoe: recipes off (`neoforge:false`, the
  furnace's pattern) and `ModularConvert` makes any vanilla stack modular -- a global loot modifier,
  a mob's gear as it joins the level, villager and wanderer offers, and a player's pockets once a
  second. The canonical design is read off the material table (iron's `pickaxe` form is
  `minecraft:iron_pickaxe`): an iron head on the type's default handle. The converted item keeps its
  name, enchantments and how worn it was.

**Built (steps 3-5).**
- **Sword** (blade, optional guard, grip, optional pommel, which is a counterweight: it lowers the moment the arm holds out), **mace** (head `heavy_core` or a metal block over a haft; the smash is vanilla's fall damage scaled by the item's mass over a heavy core on a full haft, so an iron-block mace is a weak one, and a heavy head on a wood haft works its joint loose), **trident** (tines and shaft; thrown damage is still vanilla's 8, **[OPEN]**), **shield** (face, rim, optional grip; stops a blow up to `6 + the face's competence` and only that much of a harder one, via `LivingShieldBlockEvent`; the thermal face of the approved shield idea is not built).
- **Heavy core** (`material/heavy_core.json`, §5.1) with every metal form type excluded, so nothing can work it, and `breeze_rod` (the mace's canonical haft, strong and light).
- **Bow and crossbow**, by pattern like everything else. Limbs (or prod) stiffer than the string holds make the arrow faster and the draw slower; a string weaker than half the limbs' strength is the limit and is strained (misuse, a chance of a point of wear per shot). Wool's `yield_strength` was added for this. The crossbow is assembled at the bench from a prod, string, stock and lock, **with a crossbow blueprint in the grid** (see *Blueprint Table* below). **[OPEN]** a charge time: vanilla's is a constant that cannot be overridden without a mixin.
- **Shears** (two blades, optional pivot). Fishing rod, flint and steel and brush stay vanilla.
- **Armour** (plate, optional fibrous lining), one item per slot. Points are the piece's maximum times `s / (s + 2.87)` for the plate's competence `s` (iron near vanilla's 6 for a chestplate, soft gold well under); armour toughness from the last fifth of that share; knockback resistance from density above 8 (mass is hard to push); the wearer slows by the mass over 4 kg. Mail (iron `chain`) covers three quarters as much as plate. The one worn-armour sprite sheet is a dyeable grey tinted by the plate. **[OPEN]** a lining keeping heat from the wearer: it adds a little protection only.
- Vanilla recipes off for every type built, including netherite smithing; conversion covers all of them.

**Built (step 6): the instruments.** A **balance scale** (`Quantity.MASS`, to 0.1 Mu) and a **scratch file set** (`Quantity.HARDNESS`), both carried like the calipers, which are left to dimensions and condition. With one in the pack, a modular item's tooltip gains the figure under its adjectives, signed by the instrument. The files are `ModularTuning.SCRATCH_FILES` (0.5 to 80), and the reading is the rung the edge falls between -- "scratched by file 4, not by file 3" -- because a file set cannot stand behind a finer number. **Not built:** the impact pendulum (toughness) and the strike post (what a weapon delivers per blow), which are blocks and wait.

**Built (step 7): the custom types**, each a `modular_type` file and a lang line: **mattock** (pickaxe and axe tags, each at half), **cleaver** (parent `axe`: four heads on one grip, twice the axe's strike), **pike** (one head over three handles, so it reaches further), **maul** (six heads on a handle; splits wood, and a blow slows its target for as long as the impact is worth) and **sickle** (cutting a crop or leaf takes its neighbours on the level too, one point of wear per four taken). The cleaver proves the parent chain; the pike proves a longer handle is only a longer pattern.

**Built (2026-10-20): the Blueprint Table** (`modular/BlueprintTableBlock`, `BlueprintTableMenu`, `ModularBlueprintItem`; screen `client/BlueprintTableScreen`; `net/ModularBlueprintPayload`). The Gun Stand's shape for every modular type, under the author's ruling: *a blueprint is a plan, so it shows the numbers for a tool before you make it, and is required to craft only what is complicated.*
- **What is drawn**: a type, one of its patterns (which optional parts it has and how long its handle is -- §3's length choice is the pattern), a material for each role the pattern has, and the edge. The screen lists roles off the type's file, so a role added to a type appears with no change; each role offers every material with a form the role takes (`Role.accepts`, by property, never id).
- **What it shows** is `ModularBake.bake` run on that drawing, new: the design's figures, exact, because requirements and specs are free and a blueprint is paper (philosophy 8, §6). It is never a reading of an item. The item built is baked from the parts actually put in, and what it *has* still costs an instrument.
- **Printing** turns paper (`blueprint_paper` tag) into a Blueprint carrying the design and its figures; the server rebakes on its own tables and refuses a design the type cannot be put together as (a pattern it does not have, a form a role does not take). A blueprint put back on the table opens for drawing again. The table keeps nothing; the paper comes back when it is closed.
- **Which types need one**: a type file's `blueprint: true`, and only the crossbow says it. The crossbow is the one type with a *mechanism* -- a lock that holds a drawn prod and lets it go -- whose parts have to be made to fit each other (§5's table: "the lock is a machined part like a gun's action"). Everything else is a material on a stick, plate or string, where the parts are the design (§1's Tinkers route). The shield, bow and armour were weighed and left out for the same reason: nothing in them works against anything else. A blueprint needed is any blueprint of that type; the parts need not match it -- the plan is the gate, the parts are the item. The craft keeps the blueprint, as the gun's does; repair and re-forge never need one.

**Built (2026-10-20): the tip.** A role with `"set": true` is a hard piece set into the wearing part, not a part of its own: `T` in nine types (pickaxe, axe, shovel, hoe, mattock, cleaver, sickle, pike, sword), laid beside the head or blade in a pattern of its own (`HT / S / S`), one gem, shard or nugget. `ModularBake.zone` bakes the wearing zone: **hardness is the tip's** (it gates what the edge can dig), **bite is `sqrt(tip hardness x the body's strength)`** -- the tip cuts, the body holds it -- and **toughness and wear are 70% body, 30% tip** (`ModularTuning.TIP_SHARE`). A tip no harder than its body does nothing (a gold tip on iron is a pad). Mass is small and real. So a steel pick with a diamond tip bites at 38 where steel alone is 15 and diamond whole is 45, chips like steel (toughness 43, not 5) and lasts about 1.6x as long as steel: the strength of a hard material without its brittleness and without much of it (diamond saw blades, carbide inserts). **Which types take one, and which do not:** edged tools and blades do, because an edge is where hardness pays; **mace, maul, trident, bow, crossbow, shield and shears do not** -- a mace's point is mass, a maul's is a blunt blow, a shield's face is struck whole, and none of them is an edge a tip would change. **Armour takes none** (see studs, below): an armour piece is a plate, and nothing sits at its "edge". Re-forging one gem into a tipped tool swaps the tip; a tool made without a tip cannot be given one (the bench has no way to cut the seat).

**Whole brittle material is bad, not forbidden.** Diamond is not a metal and cannot be hammered into a blade, but a diamond head can still be made (the gem is stock like any other, `ModularBake.fracture`): its durability reads `competence x min(1, toughness / (hardness x 0.2))^2`, so a material tougher than a fifth of its hardness is unaffected (stone, iron, cast iron, hardened steel all keep every point) and diamond keeps one sixteenth. It bites superbly, shatters in use and is tagged `brittle`. Precision and brittleness, never a hard gate (philosophy 7). The square: a crack runs, so a little brittleness costs little and a lot costs nearly everything. A tip does not pay it, because a tip is not whole -- it is supported.

**Not built:** guns on the core (step 8, waits), the impact pendulum and strike post, and the other `[OPEN]`s of §11.
