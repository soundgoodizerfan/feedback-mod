# Feedback — Modular Items

**Working document, not one of the three.** Tools, melee weapons, ranged weapons and armour as
*baked designs*: parts, each a material plus a few discrete geometry choices, baked once into
figures stored on the item. The gun's pattern (`feedback_guns_spec.md`, `weapon/GunDesign`)
widened into a shared core. Draft; nothing built.

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
- Repair puts material back into the worn part. **[OPEN]** By hand at the bench, or only by
  re-forging the part.

## 3. Discrete choices

Per the house rule (philosophy §3): named options, never sliders. Proposed set, each a real
trade:

- **Edge:** keen / standard / blunt — bite against chipping.
- **Length:** short / long — reach and leverage against swing speed and joint load.
- **Section:** flat / ribbed (fullered) — mass against stiffness.
- **Binding:** wrapped / pinned / socketed — joint capacity against cost and repairability.
- **Heat treatment** (slice 2's quench and temper, already built): as-forged / hardened /
  tempered. **The core pitch lands here directly:** temper too long and a finished blade softens;
  quench too hot and it cracks.

Tetra's *mutually exclusive improvement groups* are this list: one choice per axis, swapping
replaces.

## 4. As built vs as drawn

Each part carries its **target** geometry (from the blueprint or the bench) and its **built**
geometry, as the gun barrel already does (`BarrelGeometry`). The forge produces the built
figure: a head hammered past its target is thinner and lighter; one stopped short is heavy and
blunt. The bake reads the *built* geometry, so the item you hold is the item you made.

A blueprint's figures are the target's. They are exact only for parts forged exactly to
drawing. **[OPEN]** Which processes introduce how much deviation (hand forging most, the Press
least, grinding corrects an edge).

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

**[OPEN]** The non-weapon tools: shears, fishing rod, flint and steel, brush. Modular or left
alone.

**Custom (proposed, author picks):** mattock (dig and chop, each at half), sickle and scythe
(area harvest), cleaver (heavy, short), pike (long, slow), maul (splits and staggers), pry bar
(lifts a block whole), **prospector's pick** (strikes rock and reports an adjective about what is
in it — an instrument, so it reads and never acts).

### 5.1 Heavy core

A material file like any other. **Minecraft lens:** the heavy core is found only in trial
chambers, sealed, and it is the one item vanilla makes *dense on purpose* (the mace's whole
mechanic is its mass). Smallest departure: a metal far denser than anything smeltable — about
three times iron — that cannot be melted in any vessel the game has (so it is found, never
made), very hard and brittle. As a mace head its density is the point; anywhere else its mass
is a cost. **[OPEN]** Its figures, and whether it is workable at all or only usable whole.

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
8. Guns migrated onto the core.

## 11. Open

- **[OPEN]** Repair: by hand, or by re-forging the part (§2.3).
- **[OPEN]** Forming deviation per process (§4).
- **[OPEN]** Shears, fishing rod, flint and steel, brush (§5).
- **[OPEN]** Heavy core's figures and workability (§5.1).
- **[OPEN]** Which custom types (§5).
- **[OPEN]** Whether enchantability is one material figure or per role.
