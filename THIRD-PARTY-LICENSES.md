# Third-Party Licenses

## Feedback is GPL-3.0 (code) and All Rights Reserved (assets)

See `LICENSING.md` for why. The consequence for this file is that **the answer changed**:
the earlier version of this table said no copyleft source could be used at all, because the
project was All Rights Reserved by default and ARR cannot satisfy any copyleft licence's
terms. That was true, and it was the wrong position to be in by accident.

Under GPL-3.0 all three reference mods are legally available to adapt. What has not changed
is the working rule, which is stricter than the law on purpose: **prefer reading the design
and writing the code.** A borrowed implementation carries borrowed assumptions, and this mod
departs from all three of these deliberately.

## The table

| Mod | Licence | May we adapt its **code**? | Route, and what it obliges |
| :--- | :--- | :--- | :--- |
| **Create** | MIT | **Yes** | Permissive. Preserve the notice, state the origin |
| **GregTech CEu Modern** | LGPL-3.0 | **Yes** | LGPL-3.0 §2 permits conveying under GPL-3.0. Note the one-way street: they could not take ours |
| **TerraFirmaCraft** | EUPL-1.2 | **Yes** | Via EUPL Article 5 — its Appendix lists GPL v3 as a Compatible Licence, so a derivative may be distributed under GPL-3.0. **Record the election** wherever it is used |
| **MrCrayfish's Furniture Mod: Refurbished** | MIT | **Yes** | Permissive, same as Create. Preserve the notice, state the origin |
| **PneumaticCraft: Repressurized** | GPL-3.0 | **Yes** | Same licence as this project, code and assets both — the most direct route read all session. Assets still never used, per this project's own universal-no rule, not because the licence would forbid it |
| **RetroSFM** | MIT | **Yes** | Permissive, same as Create. Preserve the notice, state the origin |
| **Super Factory Manager** | MPL-2.0 | **Yes** | No per-file "Incompatible With Secondary Licenses" notice found, so combinable under GPL-3.0 per MPL §3.3/§10.4's default. Read, nothing taken — see below |
| **Silent Gear** | MIT | **Yes** | Permissive. Preserve the notice, state the origin. Read for modular items; design taken. **Textures copied as placeholders (2026-10-21, user ruling)** -- the repo's MIT licence covers them; see *Silent Gear textures* below |
| **Tinkers' Construct** | MIT | **Yes** | Permissive. Only a 1.20.1 source exists, so any port is to 1.21 data components. Read for modular items; design taken, nothing copied |
| **Backported Spears** (Unknowneth) | CC0 1.0 (the repository's `LICENSE`; CurseForge says Public Domain, Modrinth says MIT: all permissive and GPL-compatible, CC0 is the file in the source) | **Yes** | No obligation; attribution kindly. Cloned to `../Backported-Spears-reference` on its `1.21.1` branch (NeoForge 21.1.215, **yarn mappings and Mixins**, so nothing pastes into this Mojang/Parchment project as is). Read for the spear's charge behaviour (spec 5.4); **design only, no code taken** (the spear's charge is written fresh with Mojang names); record each site here if code is adapted later |
| **Spartan Weaponry** (ObliviousSpartan) | Apache-2.0 (code; read at its `1.20.1` branch, no 1.21.1 one exists) | **Yes** | Permissive and GPL-3.0-compatible one way. Preserve the notice, state the origin. Read for thrown modular weapons; design taken, nothing copied. Assets never |
| **TacZ** (Timeless and Classics: Zero) | GPL-3.0 (code; read at its `1.20.1` branch, commit `b482eff`; assets have their own terms and are never used) | **Yes** | Same licence as this project. Read for gun feel (spec 7.1, 2026-10-12) and for attachments and ammunition (spec 13, 2026-10-21); design taken, nothing copied |
| **Create Big Cannons** | MIT (code, `LICENSE-MIT.md`), CC BY-NC-SA 4.0 (handmade textures and models: never used) | **Yes** | Permissive. Preserve the notice, state the origin. Read at `create-v6-1.21.1` (commit `46a5d2a`) for launchers, charge failure and explosive shells (2026-10-23); design taken, nothing copied. Assets never -- see *Create Big Cannons* below |
| **Project: War Dance** (Jackiecrazy) | Unclear: `gradle.properties` says MIT, `LICENSE.txt` is the Forge MDK's LGPL 2.1, the CurseForge page says CC BY-NC (author's report 2026-10-24) | **No** | Treated as CC BY-NC, which cannot sit in a GPL-3.0 work. Read at `rework` (commit `3f44e2c`, 1.20.1) for skill content; design only, nothing copied -- see *Project: War Dance* below |
| **Tetra** | All Rights Reserved (no public source) | **No** | Design read from its public wiki only; nothing adaptable, nothing copied |
| **Create Simulated (Aeronautics)** | MIT (code), All Rights Reserved (assets) | **Yes** | Permissive. Preserve the notice, state the origin. Read for vehicles; one design taken (the steering servo's gains), nothing copied |
| **Cuisine Delight** (Minecraft-LightLand) | LGPL-2.1 (`LICENSE`, `mod_license`; "or any later version"). Read at `1.21`, commit `c49e2d7` | **Yes** | LGPL-2.1 §3 allows converting a copy to the ordinary GPL. Design only, nothing copied, assets never. Hard-depends on Farmer's Delight. See *Food* |
| **ModularDelight** (Wintooo) | MIT. Read at `master`, commit `fc27e93`; **Fabric 1.20.1, not 1.21.1** | **Yes** | Permissive. Design only, nothing copied (it could not be ported anyway), assets never. See *Food* |
| **Firmalife** (eerussianguy) | MIT. Read at `1.21.x`, commit `c1b2edf9`. A few spots are marked "copied from TFC" (EUPL): never taken | **Yes** | Permissive for its own code. Design only, nothing copied, assets never. Its decay engine is TerraFirmaCraft's, not its own. See *Food* |

**Assets are a separate question and the answer there is still no, universally.** Create's are
All Rights Reserved. GregTech's are third-party resource-pack imports (Gregtech: Refreshed,
ZedTech, TecTech) carrying their own terms. No texture, model or sound from any of them may
be used here, ever, whatever their code licence says.

Nothing in this repository is currently adapted from any of them. Every entry below records a
design debt, not copied code — which is worth keeping true, and worth noticing if it stops
being true.

## Create

Parts of Feedback's rotation network are derived from **Create**, specifically the
algorithm in `RotationPropagator` and `KineticNetwork` — source ownership, network
overpowering, conflict handling and the flicker counter. The code is rewritten rather
than copied, but the design is theirs and the debt is real.

Create's **code** is MIT licensed. Create's **assets** (everything under
`src/main/resources/assets/`) are *All Rights Reserved* — **no texture, model or sound
from Create may be used in this project, ever.**

Reference checkout lives outside this repository at `../Create-reference`
(branch `mc1.21.1/dev`). It is reference material, not a dependency.

```
MIT License

Copyright (c) The Create Team / The Creators of Create

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## TerraFirmaCraft

**EUPL-1.2 — adaptable, but only through Article 5, and the election must be recorded.**

EUPL-1.2 is strong copyleft: adapting TFC source obliges the derivative to be released under
the EUPL *or* under one of the Compatible Licences its Appendix lists. GPL v3 is on that list
— see `LICENSING.md`.

So the route exists, and it has a condition attached: relicensing a EUPL derivative to
GPL-3.0 is an **election this project makes**, and it should be traceable to the file that
prompted it rather than assumed globally. Nothing here has needed it yet.

Everything below was taken before that route was open, and is therefore what was never
copyrightable in the first place: the **ideas** — that an item's heat is best stored as a temperature plus
a timestamp and computed lazily, that forge colours make the right adjective scale, that
fuel is data rather than code. Those are read, understood, and then written from scratch
against this mod's own model, which is a materially different thing from adapting source.

Concretely, what TFC informed, with the reasoning recorded at each site:

- `ItemHeat` — the stamp-and-timestamp approach to workpiece heat. Arrived at
  independently here and then **confirmed** by TFC's `HeatComponent`, which stores
  `(capacity, lastTemperature, lastTick)` for the same reason. Convergence on an
  obvious-in-hindsight solution, not derivation.
- `Heat.cooled` — cooling switched from exponential to **linear** after reading TFC's
  `HeatCapability.adjustTemp`. See that method's own note for why; it is a game-design
  argument, not a physics one.
- `Readout.temperature` — the band structure and the idea of a **maximum visible
  temperature** come from TFC's `Heat` enum. The bands themselves are re-derived from
  blacksmithing colour references rather than copied, and the count and boundaries differ.
- `Fuel` / the former `fuel/` datapack table — the shape `(ingredient, duration, temperature)`
  was TFC's `Fuel` record. Retired 2026-10-20: the figures (duration, temperature) now sit
  in each material's `combustion` block and the ingredient key is gone (mechanics 5.7).
- Melting and casting, as a two-step split (item heats to a fluid in a vessel's tank; the
  fluid cools in a mold item back to a solid) — read from TFC's `CrucibleBlockEntity` /
  `HeatingRecipe` / `CastingRecipe` / `IMold`. Idea only, not code: `ThermalProcess`
  gained an optional fluid result rather than copying `HeatingRecipe`, because melting
  turned out to already be the same question `ThermalProcess` answers (see
  `ThermalProcessCategory`'s own doc); the mold reuses this mod's existing `ItemHeat`
  rather than TFC's separate `IHeat`/`IMold` capability pair. `Casting` stays its own
  table rather than folding in, for reasons recorded at its own class doc.
- **Alloying** (`process/AlloyRange`, `AlloyRecipe`, `AlloyTable`, `AlloyMix`; bronze and
  brass) — the ratio-window shape, where an alloy is defined by an acceptable composition
  range per metal rather than a fixed recipe, is TFC's `AlloyRange`/`AlloyRecipe`/
  `FluidAlloy`. Idea only, not code: Feedback's version is written fresh against its own
  table conventions (a plain `Codec`, no `StreamCodec`, matching `Casting`'s own
  "no JEI card yet, server-only" reasoning rather than TFC's networked, JEI/EMI-integrated
  version). What was *not* taken: TFC lets an unmatched mix sit as a generic "unknown
  alloy" fluid with its own stats; Feedback's `AlloyMix#resolve` reports empty instead,
  because an unnamed fluid with a fabricated stat block would mean inventing a fifth
  material the roster doesn't have, where "nothing pours out yet" needs nothing new at all.

TFC's **assets** are, as with Create, not ours to use. The molten-metal fluids' own
placeholder textures are vanilla's lava textures, tinted — never TFC's or Create's.

Reference checkout lives outside this repository at `../TFC-reference`. It is reference
material, not a dependency, and it must never become one without the licence question
being settled first.


### Catching a block up across an unloaded gap (2026-10-14)

Read on the `1.21.x` branch (the one matching the target version), per the standing rule:
`AbstractFirepitBlockEntity#onCalendarUpdate` and `HeatCapability#consumeFuelForTicks`.

**Taken as design, written from scratch:** the shape of burning fuel across a gap -- spend the piece alight first, then the stack in order, and hand back a remainder of unaccounted ticks, so a fuel that outlasts the gap leaves its piece partly burned and one that does not leaves the fire out. `FireboxBlockEntity#burnAcross` and the vessel's are that, over our own fuel figures (once a `fuel/` table, now material `combustion`).

**Not taken, and why it matters:**

- **The calendar.** TFC drives catch-up from a global calendar that players' clocks advance. Feedback has no calendar and does not want one: a block records the game time it was saved at and asks how long it was away on load. One `long` in its tag, no registry of tickables.
- **Cooling instantly.** When TFC's fuel runs out across a gap it extinguishes the fire and calls `coolInstantly()`, setting the contents to ambient outright. That is a cliff where the thermal model has a curve, and it throws away how long the fire was actually lit. Here the body relaxes towards the flame for the lit part of the gap and towards the room for the rest, in closed form (`Heat.relax`), so a crucible that was lit for most of the gap is warm and one that was lit for none is cold, and nothing has to be invented to say which.
- **Doing nothing while fuel remains.** TFC leaves temperature untouched when the fuel outlasts the gap. Ours relaxes it towards the flame, so a vessel left over a fire for ten minutes is hot, not frozen at the temperature it had when the chunk went.

## GregTech CEu Modern

**Read for design. LGPL-3.0, which is weak copyleft — and "weak" is about linking, not
about copying.**

This is the distinction that catches people out, so it is worth stating plainly. The LGPL
exists so that a closed or differently-licensed program may **link against** a library
without itself becoming LGPL. That permission is about *use across an interface*. It is not
permission to paste the library's source into your own tree: copied or adapted code is a
"modified version of the Library" under §2, and it carries the LGPL with it.

So the practical answer is the same as TerraFirmaCraft's — **write every line from scratch**
— but the legal route differs, and one genuinely different option exists that does not for
TFC: Feedback *could* legitimately take GTCEu as a jar dependency without relicensing. We do
not want that, for the same reason we do not depend on Create: this mod's point of departure
from GregTech is architectural, and depending on GregTech to describe it would be absurd.

That option is only on the table at all because Feedback is now GPL-3.0. While it was All
Rights Reserved, even the linking permission was unusable -- ARR and LGPL cannot be combined
in a distributed work. The relicence opened a door we have chosen not to walk through.

### What is worth studying, and why it is worth studying early

The cover system, the design behind `fitting/`. GTCEu
splits it three ways, and the split is the lesson rather than any individual class:

- `api/cover/CoverDefinition` — the registry entry. What kinds of cover exist.
- `api/cover/CoverBehavior` — one attached instance, which knows its **holder** and its
  **attached side**, and nothing else about the world.
- `ICoverable` — the host. What it means for a block to accept covers at all.
- `IIOCover`, `IUICover` — optional capability interfaces a cover opts into, rather than a
  base class that every cover pays for.

That last point is the one that maps straight onto this project. It is the same shape as
`Instrument`: declare a small number of things, and every existing display already knows what
to do with you. The reason to build it **before** a second cover needs it is the same reason
`Instrument` was built before the thermometer existed — a high fixed cost paid once beats no
fixed cost and a medium variable cost paid per feature, and the crossover is earlier than it
feels.

**Not called "cover."** GTCEu's cover is one shape: a sided I/O filter. Feedback needed three
that don't share a shape — a sided read-only `Sensor`, an unsided `Upgrade`, and a sided
addon-shaped `Adapter` that grants a second energy type — so the umbrella got its own name,
`Fitting`, rather than stretching GTCEu's word over a wider thing than it names there. See
`CLAUDE.md`'s `fitting/` row for the naming.

### Taken: ore as host stone plus tinted overlay

Ore look (2026-10-22, read at branch `1.21`, design only): GTCEu builds each ore model from a base-stone child and a greyscale overlay tinted by material colour. Feedback's `ore_layered` model does the same as a static two-element JSON, tinted by `FormTint`. **Not taken:** runtime-generated models in a dynamic resource pack, Registrate, emissive layer, icon-set enum, second tint layer (add when an ore needs two tones). No GregTech texture used. Overlays are cut from Mojang's own ore textures (user ruling, 2026-10-22) or generated by script.

### Taken: a hand tool that survives the craft

`item/HandToolItem`, `item/HandHammerItem`, `item/HandToolCrafting`,
`process/HandDeformationRecipe`.

GregTech's crafting-table tools are not a recipe type and not a special ingredient. A tool is
an ordinary ingredient matched by tag, and the entire mechanism is that its **crafting
remainder is itself, one point more damaged** -- `IGTTool#definition$getCraftingRemainingItem`
copies the stack, damages it, and returns it, so vanilla's own `Recipe#getRemainingItems`
does the rest. Feedback's Hand Hammer works exactly that way, on NeoForge's
`Item#getCraftingRemainingItem(ItemStack)`.

Design read, code written here. Nothing was copied; the file is nine lines of logic and the
shape is the borrowed part.

**Partly taken:** that hand tools are a *kind* of thing rather than a list of unrelated items.
GregTech's `GTToolType` is a real abstraction and it is right to have one. Feedback's version
is `HandToolItem`, a superclass declaring a strength, a sound and the durability rule, and the
recipe matches `instanceof HandToolItem`.

**Deliberately not taken:** the indexing apparatus around it. GTCEu addresses its nine tools by
a character symbol in a recipe pattern (`'h'` is a hard hammer) and matches them through
`craftingTags`, which is the right answer when tools appear as ingredients in dozens of
authored recipes and pure overhead when there is one dynamic recipe that accepts any of them.
Nor is there a registry of which tools perform which operations: a tool that does not strike
declares `0` St and is refused by the material's own hardness floor, so the strength is the
whole answer.

**Also taken, and then moved:** GregTech plays its tool's sound from inside
`definition$getCraftingRemainingItem`. Feedback plays it from `ItemCraftedEvent` instead --
same idea, but a side effect hidden in a getter that both logical sides call is a worse place
for it than an event that fires once when a craft actually happens.

**Deliberately not taken from TerraFirmaCraft either**, which solves the same problem with an
anvil block and a forging minigame. That is a much larger and much better mechanic than this
slice wants -- hand work here is meant to be *tedious*, not skilful, because philosophy 7
says automation makes a process practical rather than unlocking it. A hand route that was fun
would compete with the machine instead of motivating it.

The one thing Feedback does that neither does: the craft is **not** the finished product. A
blow adds `Fu` to the workpiece and hands the same workpiece back, so five crafts make a
plate and a sixth makes foil. GregTech's tool recipes are ordinary atomic crafts, which is
why theirs cannot overshoot and ours must.

### What was looked at and rejected

**Machine maintenance.** GregTech charges a flat chance of a fault per hour a machine runs,
repaired with a wrench and a duct tape. Considered as the model for Feedback's hammer wear
and declined, because it is a tax on *uptime* and carries no information: a perfectly built
line accrues it at exactly the same rate as a badly built one, so the only thing the player
learns from a maintenance fault is that time has passed.

Feedback's wear is the inverse and is deliberately unprecedented as far as either of us
knows. It accrues **only** on force that could not go into the work — a workpiece too cold to
move, a blow under the material's hardness floor, a drive geared past what the machine's
construction can take. A correctly built line therefore never wears at all, ever, and a worn
hammer is not a bill but *evidence*, pointing at a specific mistake the player can go and
find. That is the same thing this mod does everywhere else: consequence follows from the
physics being wrong rather than from a clock.

### Materials and form steps: the shape, not the code

Read: `api/data/chemical/material/` (`Material`, `PropertyKey`, `MaterialProperties`,
`MaterialFlag`) and `data/recipe/generated/WireRecipeHandler` on the `ref-1.21.1` branch.

**Taken as design:** a material is a bag of optional typed properties, null-checked by whichever
system needs one, so adding a figure costs nothing until something reads it. An item is a
*(form, material)* pair, and a recipe for a form is generated from a per-form rule applied to one
material scalar (`duration = mass * k` in theirs; `base + slope * hardness` in
`process/FormStep`). Written fresh as `process/Material`, `FormStep`, `FormTable`.

**Not taken:** the flag layer (`GENERATE_PLATE` and friends) -- a form exists for a material iff
the material lists an item for it. Their per-form rule is a Java handler; ours is data, so a pack
can add a form without code. Voltage/EU scaling, and recipes owned by a machine type.

### Ore worldgen (read 2026-10-22, ref-1.21.1 commit 23d7ee00)

Read: the ore definition (`GTOreDefinition`) and the vein generators. Nothing was copied; the
feature, record and table were written from the design.

**Taken:** the datapack-defined vein record with trimmed fields (density, cluster size,
height range, dimension filter, biomes, and a host rule for what it may replace; a per-chunk
`count` stands where theirs has a weight), and the density concept (a blob's chance per candidate
block to become ore, so a vein is lean or rich).

**Also taken (stance, 2026-10-22):** removing the vanilla ores from world generation wholesale and
generating the mod's own ore blocks in their place, as GTCEu does. Nothing copied; the vanilla
placed features are removed by a NeoForge biome modifier and the deposits are Feedback's own data.

**Not taken, and why:**
- The `ChunkGenerator.applyBiomeDecoration` mixin. Vanilla's placed feature and a NeoForge biome
  modifier reach the same step with no mixin to break on a Minecraft update.
- The grid of veins with cached blobs and a single weighted pick per layer. It lets a vein exceed
  a chunk; a graphite blob does not need that. Revisit if a deposit ever does.
- About fifteen per-stone ore prefix blocks. Feedback has stone and deepslate, two blocks.
- Bedrock-ore and fluid veins with depletion: depletion is a flat-rate wear, which Feedback rejects.
- EU-powered prospecting: a prospecting instrument here is a read-only reading, not powered by `Eu`.
- `OreProperty`-style tiering: a hard gate is a hardness comparison, never a number on the ore.
- The Veined, Geode, Dike and Cuboid generators: one blob shape in v1.
- Surface indicators: deferred (mechanics §9.5).

### The boiler and turbine

Read for `machine/boiler/` and `machine/steamengine/`, the mod's first bridge between two energy
types. The default checkout tracks the `7.5.3` tag, which targets 1.20.1; this reading was done
against the `1.21` branch instead (confirmed against `1.21.1` in its own `gradle/libs.versions.toml`
before reading, not assumed from the branch name), since Feedback targets 1.21.1 and the two
branches diverge in ways that matter for a multiblock/capability system. See `FTuning`'s
`--- the boiler ---` section for the full design account; the licence-relevant summary:

- **Taken:** `LargeBoilerMachine` and `LargeTurbineMachine`/`RotorHolderPartMachine` are already
  separate multiblocks joined only by a Steam fluid crossing hatches. Design read, not code —
  Feedback's boiler and engine are two ordinary block entities, not multiblocks, and the fluid
  crosses through NeoForge's `Capabilities.FluidHandler`, not a hatch. What carried over is the
  shape of the split itself: a boiler that only knows its own temperature and a consumer that
  only knows Steam, with nothing else in the middle.
- **Taken:** `LargeBoilerMachine#updateCurrentTemperature` gates steam production on the boiler's
  own temperature clearing a floor before draining water — the same shape as
  `FTuning.BOILER_WORKING_TU`.
- **Taken:** `RotorHolderPartMachine`'s asymmetric speed ramp (`SPEED_INCREMENT` up,
  `SPEED_DECREMENT` down, both flat, never snapping) needed nothing new on Feedback's side — it
  is the same momentum `RotationNetwork`'s inertia already provides for every other source, so
  the read confirmed a shape already built rather than adding one.
- **Not taken: EU as the output.** GTCEu's turbine produces electricity because everything in
  GTCEu eventually funnels there. Feedback's engine outputs `Su`/`Rpm` on a plain `RotationNode`
  — mechanical, full stop — because philosophy §10 says Mechanical never has to become Electrical.
- **Not taken (yet): the rotor as a physical, damageable item** (`TurbineRotorBehaviour`).
  Philosophy's "upgrades are physical components" independently wants exactly this, and it is
  deferred rather than declined — `FTuning.STEAM_ENGINE_CAPACITY_SU` is a flat number standing in
  for a rotor that does not exist yet, the same way `HAND_CRANK_CAPACITY_SU` once stood in for
  gearing before the cog existed to make it real.
- **Not taken (yet): the exploding boiler.** `feedback_philosophy.md` §14 already names this as
  good precedent for a genuine hard-gate consequence. Left out because nothing in Feedback's
  boiler can currently be over-restricted — there is no failure state to explode into, and
  building the punishment before the mistake it punishes is possible would be backwards.

### Electricity: design only, nothing built

Read for `feedback_mechanics.md` §6 (study of `GTValues`, `EnergyNet*`, `Insulation`,
`WireProperties`, `CableBlockEntity`, `TransformerMachine`, `NotifiableEnergyContainer` and the
recipe `EURecipeCapability` on the `ref-1.21.1` branch). No code adapted.

- **Taken as design:** voltage as a few named, geometrically spaced steps; energy as a
  `(voltage, current)` pair; cable loss as a flat voltage drop per block per amp (distance becomes
  a cost and voltage a lever with no resistance variable); a cable carrying its own temperature
  with excess load as the heat input; insulation as a separate layer that fails before the
  conductor; a transformer as a discrete step with voltage and current trading at constant
  product; caching routes until topology changes.
- **Not taken:** tier as the progression ladder (15 of them, every recipe keyed to one); EU as the
  denomination of every recipe, steam and fuel as EU payment, overclock by tier and the FE
  converter (the universal-currency chain, philosophy §10); explosion on over-voltage; a stored-EU
  buffer in every machine; nearest-first push routing as the model (its invisible starvation);
  item batteries as a tool-charging currency.

Reference checkout lives outside this repository at `../GregTech-Modern-7.5.3`. Reference
material, never a dependency.

**Slice 8 phase 1 (basic electronics), read 2026-10-23 on branch `1.21` for early-circuit design.**
**Taken:** component slots as property tags (a probe asks for a crystal by `piezo_coupling`, not
by id) and the yield-ladder idea (an instrument improves yield and unlocks nothing). **Rejected:**
circuit tier = voltage tier, the wafer/boule chain, SMD multipliers, the cleanroom gate, the
flat-fluid solder tax. No code copied.

## MrCrayfish's Furniture Mod: Refurbished

Read for the data-link system's node/connection graph and select-then-connect tool
interaction (`control/data/`) — a link between two data-link endpoints is invisible, has no
block or entity in the world, and is made by right-clicking one endpoint and then another
with a tool. That whole shape is CFM-Refurbished's `electricity` package: `IElectricityNode`
(the capability a block entity implements), `Connection` (a resolved-on-demand pair of
positions, not a cached live reference), and `LinkManager` (the per-player pending-selection
state machine behind its Wrench item).

**Taken:** the three-part split above, and the state-machine shape in `LinkManager`
(`DataLinkManager` here) — first interaction remembers a node, second interaction attempts a
link, no in-world representation at any point.

**Widened, not copied:** CFM keys a node by `BlockPos` alone — one node per block entity.
Feedback's `DataNodeRef` adds an optional `Direction`, because a `Fittable` holder can carry
several sensor fittings, one per side, each needing its own node identity; CFM's furniture
never has more than one electrical connection point per block, so it never needed this.

**Deliberately not taken:**
- The electricity/power semantics — `ISourceNode`/`IModuleNode`, overload, powerable-zone
  radius. A data link never carries current; it just lets a reading be asked for. There is no
  "powered" state to propagate and nothing to overload, so none of that machinery has an
  analogue here.
- Sub-block node raycasting (`NodeHitResult`, the custom `traverseBlocks` walk in
  `WrenchItem`). CFM needs it because several of its electricity nodes can share one block.
  Feedback's sided fittings map exactly onto vanilla's own block-face hit result, so the
  connector tool uses plain `UseOnContext#getClickedFace()` instead — simpler because the
  read changed the shape of the problem, not just the names in it.

**On the renderer, a correction mid-session.** The repository's default branch (`26.1.2`)
draws nodes and links through walls with a separate GPU frame-graph texture target
(`ElectricityRenderer`, `FrameGraphBuilder`/`RenderPipelines`) that targets a newer Minecraft
than Feedback's 1.21.1 and has no equivalent on this toolchain — that was checked, correctly,
and correctly rejected as unportable. What was wrong was stopping there: the mod also has a
`1.21.1` branch, not fetched on the first pass, whose renderer is a completely different and
much older implementation — plain `BlockEntityRenderer`s (`ElectricBlockEntityRenderer`)
queuing draws into a small deferred list (`DeferredElectricRenderer`) that gets flushed once a
frame, via ordinary `Tesselator`/`BufferBuilder`/`RenderSystem` calls, into
`mc.levelRenderer.entityTarget()` — the same render target vanilla already uses for a glowing
entity's outline. That trick is exactly what makes it visible through a wall, and it needs
nothing this toolchain lacks. **Taken**, once the right branch was actually read:

- `DeferredDataRenderer` — the queue-then-flush shape of `DeferredElectricRenderer`, and the
  `entityTarget()` render-through-walls trick itself
- `DataNodeRenderer` — the shape of `ElectricBlockEntityRenderer`: a small box per node, a
  thin rotated box per connection, both only drawn while the connector tool is held
- `DataLinkFrame` — the `RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES` hook that flushes
  the deferred queue once a frame, camera-translated, straight from `NeoForgeClientEvents`

**Not taken:** the hover/selection highlight colours (`LinkHandler`'s success/error/default
link colour, the "you have this node selected" render state) and the powerable-zone shape
overlay. Both are real polish on top of a working link graph, not load-bearing for the tech
demo's actual claim — that a link is genuinely invisible except with the tool in hand — so
building them now would have been reaching past what this pass needed to prove.

```
MIT License

Copyright (c) 2024 MrCrayfish

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

Reference checkout lives outside this repository at
`../MrCrayfishFurnitureMod-Refurbished`, on the `1.21.1` branch (matching this mod's own
toolchain — the repository defaults to `26.1.2`, which is a different Minecraft version with a
materially different renderer; check out the branch that matches before reading, not after).
Reference material, never a dependency.

## RetroSFM

Cloned (`../RetroSFM-reference`, branch `1.21.1` — the repo's default is `main`, and a `26.1.2`
branch exists too) and read for the Programmer's node canvas. RetroSFM is a blueprint-style
node editor over Super Factory Manager that generates SFML text: the Steve's Factory Manager
look on a modern codebase, which the 1.7.10 original was not worth reading for.

**Taken, as design, written fresh in `control/programmer/ProgrammerScreen`:** the camera
(pan, zoom about the cursor, frame-all), a per-node pin layout hit-tested pin by pin, drag-from-pin
wiring with compatible-pin highlighting, a right-click palette that — when a wire is dropped on
empty canvas — is filtered to cards that can attach and wires the new one in, click/shift/box
selection with group drag, alt-click to cut a pin or wire, horizontal-tangent bezier wires, and
undo/copy/paste over a client-side copy committed in one go.

**Not taken:** the inspector dock (a Feedback card has at most one setting, edited in place on
the card), the SFML preview and text import (a `ProgramGraph` has no text form), snap-to-grid,
UUID node ids (`ProgramGraph` already numbers its nodes), and its static, cross-screen clipboard
(here the clipboard dies with the screen, and copy is off on a printed card — see
`docs/feedback_controller_spec.md`'s Programmer GUI note).

MIT (`LICENSE`, © 2026 Saereth). No code copied, so nothing to preserve yet; if any is ever
adapted, this entry is where the notice goes.

## Super Factory Manager

Cloned (`../SuperFactoryManager`, branch `1.21.1` — verified against `gradle.properties` before
relying on it) and browsed while scoping the controller design, then set aside: it's a genuine
domain-specific *language* for logistics automation, with its own grammar, parser, and a VS Code
language-extension package — solving item/fluid routing, not "read a quantity and switch a
supply," and built at the complexity of a real compiler where `docs/feedback_controller_spec.md` §1
deliberately draws a ceiling. Nothing taken; see that document's §6 for the full reasoning.

MPL-2.0 (`LICENSE.txt`), no per-file "Incompatible With Secondary Licenses" notice found in the
source read, so under MPL §10.4 the code remains available under GPL-3.0 as a Secondary
Licence — a real route, just not one walked, same as GregTech's linking-only route was never
walked either.

## PneumaticCraft: Repressurized

Read for the controller/program-graph design in `docs/feedback_controller_spec.md` — the drone
programming system's `IProgWidget`/`ProgWidget` hierarchy: a card as a typed node with declared
inputs and outputs, a numeric condition reading a live value through a `getCount()`-shaped
accessor, and validating a program before it's usable (`addErrors()`) rather than failing at
runtime.

**Taken:** the node-as-typed-contract shape, and the live-value-accessor and
validate-before-use ideas named above. **Deliberately not taken:** PNC's jump-based control flow
(`ProgWidgetConditionBase.getOutputWidget` calling `ProgWidgetJump.jumpToLabel`) — real branching
power, and exactly what Feedback's controller spec rules out on purpose (philosophy §13: the
controller is a switch, never a dial); the drone/`Goal`/AI domain entirely, since nothing here
moves; and PNC's puzzle pieces being individually-crafted physical items per placement, rather
than drawn from a palette. See `docs/feedback_controller_spec.md` §7 for the full account.

**Read a second time, for `PressureVesselBlockEntity` (`TODO.md` §5e as of `50b93c6`) — user-directed rather than
surveyed, since `Pu` had no prior implementation to weigh options against.** `IAirHandler`'s
`pressure = air / volume` (an amount over a fixed volume, computed on read rather than stored) and
`PressureTier`'s danger/critical thresholds with risk climbing linearly between them
(`MachineAirHandler#addAir`) are both taken as design, rewritten fresh against this mod's own
`Blown`/`Pu` types. **Not taken:** volume upgrades, the safety-valve venting state machine, and
the cross-chunk leak/render sync machinery — Feedback's vessel has one fixed volume and a single
constant bleed instead, since nothing yet gives the player a reason to want a bigger one or to
vent it deliberately.

GPL-3.0, code and assets both, per its own `LICENSE` file — this project's own licence exactly,
the most direct legal route of anything read this session. The house rule still applies (read
the design, write the code), and the universal no-assets-from-anyone rule still applies
regardless of what this licence would permit.

Reference checkout lives outside this repository at `../pnc-repressurized`, on the `1.21`
branch (which is Minecraft 1.21.1 exactly — verified via `gradle.properties` before relying on
it, per the correction recorded above for MrCrayfish's Furniture Mod). Reference material,
never a dependency.

## Create Big Cannons (launchers and explosives, 2026-10-23)

Read at `../CreateBigCannons-reference`, branch `create-v6-1.21.1`. CBC has no non-powder launcher, no
placeable demolition charge and no grenade, so the spring and steam launchers took nothing from it.

**Taken (design only):** failure explosion scaled by the charge actually loaded (the demolition charge's
power goes as its packed filler, `DemolitionChargeBlockEntity`); an arming delay before a fuse can act
(`FTuning.DEMO_ROUND_ARM_TICKS`: an explosive round is a dud point blank); a failure mode that is a named
choice and not a number. **Rejected:** dice-roll failure chances (Feedback fails by computed margin), flat
per-shell explosion power (power is derived from the filler's blend, `Cartridge.Blend`), fuse sliders and
config toggles, the per-block armour table (the blast uses vanilla blast resistance).

## Silent Gear, Tinkers' Construct and Tetra (modular items, 2026-10-15/16)

Read for `docs/feedback_modular_items_spec.md` (the `modular/` package), after the author named the
shape: parts with materials, baked figures, a bench. **No code or asset was copied from any of
the three**; each idea below is rewritten against this mod's own material table, and the site
that uses it says so in its comment.

**Silent Gear (MIT, 1.21.1 branch).** *Taken:* parts held on the stack with the stats baked and
cached on change (`GearConstructionData`, `GearData.recalculateGearData` -- the gun already did
the same); gear types as data with a parent chain (`GearType`); harvest by block tag
(`GearDiggerTool`, writing vanilla's `TOOL` component from the stats); stepwise degradation as
an item wears (`GearHelper`); the tooltip showing a stat with the reason behind it
(`GearComponentTooltips`). *Rejected:* hand-authored stats per material per part (every figure
here is derived from `material/*.json`); random **grades** and the `rarity` stat (luck is not a
property); the hidden 0.1-2.0 **synergy** scalar (a continuous knob nobody can see).

**Tinkers' Construct (MIT, 1.20.1 only).** *Taken:* tool definitions as data listing parts and
actions; **broken, not destroyed** (`tic_broken` -- an item stops one point short and acts as an
empty hand); one sprite per part tinted by material (`ToolModel`, the technique and not its
textures, which are ours drawn from scratch); the idea that a tool station takes parts in and
gives a tool out. *Rejected:* the modifier catalogue (sharpness bought with quartz is a flat
bonus with no physical cause); `percent` handle bonuses and continuous per-part `scale`; the Part
Builder's pattern-plus-"material value" (parts come from Feedback's own forming, casting and
machining); and **repair keyed on (tool id, material id)**, which is identity -- repair here is
by property, the stuff the wearing part is made of. Not ported: `InfoPanelScreen` and
`TooltipBuilder` (the stat panel depends on Mantle, whose licence was not checked); tooltips here
are adjectives, so there was nothing to port.

**Tetra (no public source).** Design read from its wiki: the **integrity** budget (a capacity
parts spend and handles provide) is taken as an idea and given a cause -- the joint carries the
head's leverage, and one over its capacity works loose -- and **mutually exclusive improvement
groups** are the discrete choices (one per axis). Nothing else.

## Project: War Dance (skill content, 2026-10-24)

**Read at `origin/rework` (commit `3f44e2c`, Minecraft 1.20.1, cloned to `../WarDance-reference`); no 1.21.1 branch exists.** Licence treated as **CC BY-NC** (the author's report of the CurseForge page; the repo's own files disagree, see the table). NC is incompatible with GPL-3.0 code, so **no code, text, skill names, numbers or assets were taken.** Read for the *content* of a skill tree (`docs/feedback_skill_content_spec.md`).

*Taken, as mechanic shapes restated from scratch:* a trigger with swappable payoff variants; a rearm that shortens with the play the skill asks for; two-stage marks that are built and then cashed in; payoffs that read a state (target stagger, an empty off hand, airborne, sprinting) and not a number; a price carried by the risky variant; a stance chain where each move's condition is the last move's result. *Rejected:* spirit, might and adrenaline as a universal currency (Feedback's costs are physical acts), percent-per-health-lost scaling and effectiveness dials (free scalars), magic skills with no physical cause, identity checks on a skill, and the colour cap on equipped styles.

## TacZ (guns, 2026-10-12 and 2026-10-21)

**GPL-3.0, read at its `1.20.1` branch (commit `b482eff`); no 1.21.1 branch was checked.** Read for how a gun *feels* (spec 7.1, earlier) and, 2026-10-21, for how it models attachments and ammunition (spec 13). **No code was copied.** The earlier feel read was not recorded here when it was made; this entry covers both.

*Taken:* the idea of folding installed parts into one cached figure set when something changes and reading it at the shot (already how the baked gun works); capacity in discrete steps; a melee block on a bayonet-style part; a blow split into a normal part and a piercing part by a fraction (to be the round's own figure). *Rejected, and why it matters:* attachments that rewrite the round's damage, speed, pierce, armour ignore, ignite and explosion (a grip can set a bullet alight, so the owner of a property is nobody), bullet figures stored in the gun's data with the ammunition item only an id (so there are no ammunition variants), an extended magazine standing in for a slug load, free-scalar and scripted modifiers, and compatibility by id lists. Feedback's rule is the opposite on each (spec 13.1). *Not read:* Point Blank (no public source).

## Spartan Weaponry (thrown modular weapons, 2026-10-21)

**Apache-2.0, read at its `1.20.1` branch (commit `e45342c`); no 1.21.1 source from the author.**
Read only for how a thrown weapon behaves as an entity (`ThrowingWeaponEntity`,
`ThrowingWeaponRenderer`, the throw path of `ThrowingWeaponItem`), for `modular/ThrownWeapon`,
`modular/Throwing` and `client/ThrownWeaponRenderer`. **No code was copied; each idea below is
written again** against this mod's baked figures, and the sites say so.

*Taken:* the thrown weapon is a vanilla `AbstractArrow` (flight sweep, drag, sticking in a block,
shaking, pickup by touch inherited); a blow that fails to hurt (blocked, immune) bounces the weapon
off and it keeps flying, and only a landed blow spends it; the steered return (damped velocity plus
a pull to the thrower's eye, so it arcs) with a sound; launch speed scaled by how long the wind-up
was held; the renderer's pose (the model laid along the velocity, its diagonal turned onto the
flight axis). *Rejected:* every enchantment-keyed behaviour (Loyalty, Hydrodynamic, Propel,
Razor's Edge, Incendiary: identity and not a baked figure), the ammo-stack UUID merging, the random
crit on top of a figure, a flat 1 durability per hit, and per-item damage and velocity constants
(every figure here is derived from the item's baked design). *Added here, not theirs:* tumbling
(the hatchet's blow depends on how far it has turned), the rope and reel (the harpoon), wear from
striking a block harder than the edge takes.

## Truly Modular / Modular-Item-API (modular items, 2026-10-20)

**PolyForm Shield 1.0.0 -- source-available, not open source, not GPL-compatible.** Read for
`docs/feedback_modular_items_spec.md` as a **design-only** read (author's ruling): its docs and
structure, no code kept, no code ported, no key names, lang strings or assets. Nothing adaptable,
ever. *Taken as ideas:* a before/after figure comparison when swapping a part; a stat shown with
its cause. *Rejected:* authored stat expressions, the synergy rule engine, an unbounded module
tree, free cosmetic knobs, item-id converters, foreign-tier import. Detail in spec §9.

## Create Simulated (vehicle steering, 2026-10-20)

`Creators-of-Aeronautics/Simulated-Project`, `main`, cloned to `../Simulated-reference`; code
MIT, assets All Rights Reserved. First read for the vehicles spec (section 5, *Prior art read*).

**Taken, design only:** its swivel bearing drives a `Rotary` joint to an angle with Sable's
`setMotor`, and scales the servo's stiffness and damping by the turning body's inertia, so a
heavy part settles as fast as a light one. Feedback's steered hinge does the same
(`FrameBlockEntity#steer`, `FTuning.STEER_SERVO_RATE`), measured about the kingpin.

**Not taken:** its config-set stiffness with no force limit. Feedback's servo is capped at the
driver's arm (`FTuning.STEER_ARM_SU`), and that cap is the whole point: steering is heavy at
rest because the tire's scrub beats the arm, not because a rule says so.

**Second read, vehicles beyond cars (2026-10-20), `main`, MC 1.21.1, commit 8d482a5.** Nothing
copied. *Taken as ideas:* the flood-filled airtight envelope with a fill lag and a visible leak
(lift from gas density difference here, not a strength constant); sail count setting a propeller's
blades and radius; a passive pendulum hinge keeping a thrust axis upright; friction read from the
block under a tire, and a tire carried as item data; a named assembly failure on the lever;
settings copied by clipboard; altitude, velocity and gimbal sensors; a spring that snaps when
overextended. *Rejected:* levitite (permanent lift with no input), scroll values and 0-15 signals
as dials for gas and suspension, a fuel-free burner, docking that passes energy and fluid as a
universal currency, the primary-assembler lock (a gate with no physical cause).

## SummerCraft (engine sounds, 2026-10-20)

The author's own mod (`../summercraft`). Its twelve engine recordings (inline-4, V6, V8, four
loops each, synthesised by that repo's `tools/engine_synth.py`) are copied unmodified to
`assets/feedback/sounds/engine/`, and `client/BlockLoops` follows `CarSounds`' design: equal-power
crossfade between the two loops either side of the current revs, each pitched by RPM. Same
owner, so no licence is owed.

## Food (Cuisine Delight, ModularDelight, Firmalife; 2026-10-24)

Read by `prior-art-reader` agents for `docs/feedback_food_spec.md`; clones at `../Cuisine-Delight-reference`, `../ModularDelight-reference`, `../firmalife-reference`. **No code, text, numbers or assets were taken.**

*Taken, restated from scratch:* state carried on the item with a lazy `gameTime` clock evaluated on read and no per-tick scan (Cuisine Delight, Firmalife); stage-to-visible-item swap (Cuisine Delight); a meal as a list of parts baked once into a component (ModularDelight); preservation applied by the place and stripped on extraction (Firmalife), reworked into a drifting thermal volume; an oven as a crucible with a temperature window and a burn-to-nothing overrun (Firmalife); a timer that resets on interruption (Firmalife).

*Rejected:* a 0-100 score, variety bonus and food-category enum (free scalar plus identity, Cuisine Delight); a plate or dish recipe table and item-keyed machine recipes (Firmalife, Cuisine Delight); named traits as flat decay multipliers (Firmalife); fuel whitelists to produce a bad result (Firmalife); a boolean heat source with a fixed cook time (Cuisine Delight, ModularDelight); nutrition halved and averaged from vanilla (ModularDelight); tag lists of item ids that assign one property (ModularDelight); enchantments as cook speed (Cuisine Delight); an in-memory per-player effect map (ModularDelight). Hazard classes (heat-killed pathogen against heat-stable toxin) and moisture as the single axis have no precedent in any of the three.

## Placeholder texture repositories

The three reference mods above are closed to us for *assets*. These are a different case:
standalone texture repositories whose authors published them for reuse, cloned outside the
repo at `../Texture-reference` (reference material, never a dependency). Textures are copied
in **only when a model references them**, one row per file below.

| Repository | Licence | Status |
| --- | --- | --- |
| [malcolmriley/unused-textures](https://github.com/malcolmriley/unused-textures) | **CC BY 4.0** | **In use** — every texture below. Copied unmodified, `.mcmeta` included. Credit: Malcolm Riley |
| [Auseawesome/Minecraft-Textures](https://github.com/Auseawesome/Minecraft-Textures) | CC0 | Cloned, nothing used yet. No conditions |
| [cleannrooster/forg-cleannrooster-assets](https://github.com/cleannrooster/forg-cleannrooster-assets) | MIT | Cloned, nothing used yet. Notice must be preserved if used; covers only that repo's own assets |
| [SilentChaos512/Silent-Gear](https://github.com/SilentChaos512/Silent-Gear) (`1.21.1` branch, `../SilentGear-reference`) | **MIT** (Copyright (c) 2018 SilentChaos512) | **In use** -- modular tool layers, listed below. Copied unmodified. Notice preserved here |
| [Foreck1/foreck-textures](https://github.com/Foreck1/foreck-textures) | CC BY 4.0 | Cloned, nothing used yet. Credit: Foreck |
| [Futureazoo/TextureRepository](https://github.com/Futureazoo/TextureRepository) | **CC BY-NC-SA 4.0** | **Rejected, not cloned into the reference dir.** NonCommercial blocks any monetised use; ShareAlike would force every adapted texture under CC BY-NC-SA, which cannot coexist with this project's All Rights Reserved assets. "Free to use" in its README is not the same as usable here |

**Why these are acceptable when the mods' assets are not:** each was published by its author
under a licence that grants reuse, and none of the permissive ones (CC0, MIT, CC BY) restrict
the project's own asset licensing — attribution is the only debt. CC BY's attribution is met
by this table and the `credits` line in `neoforge.mods.toml`. The licences apply to *those
files only*; they do not relicense Feedback's own art, which stays All Rights Reserved.

**These stay placeholder.** They exist so the roster reads as distinct objects in game; they
are not the mod's art direction, and each is expected to be replaced. Removing a row here
when its file is replaced is part of that change.

Texture to source (all under `malcolmriley/unused-textures`, CC BY 4.0, Malcolm Riley):

| Feedback file | Source path |
| --- | --- |
| `textures/block/block_metal_fiery.png` | `blocks/block_metal_fiery.png` |
| `textures/block/block_metal_hepatizon.png` | `blocks/block_metal_hepatizon.png` |
| `textures/block/brick_refractory.png` | `blocks/brick_refractory.png` |
| `textures/block/brooding_brick.png` | `blocks/brooding_brick.png` |
| `textures/block/bundle_logs_top.png` | `blocks/bundle_logs_top.png` |
| `textures/block/cloth_dry.png` | `blocks/cloth_dry.png` |
| `textures/block/concrete_slabs.png` | `blocks/concrete_slabs.png` |
| `textures/block/copper_coil_side.png` | `blocks/copper_coil_side.png` |
| `textures/block/device_brooding_forge_side_off.png` | `blocks/device_brooding_forge_side_off.png` |
| `textures/block/device_brooding_forge_side_on.png` | `blocks/device_brooding_forge_side_on.png` |
| `textures/block/device_brooding_forge_up_off.png` | `blocks/device_brooding_forge_up_off.png` |
| `textures/block/device_brooding_forge_up_on.png` | `blocks/device_brooding_forge_up_on.png` |
| `textures/block/device_gearbox_clutch_off.png` | `blocks/device_gearbox_clutch_off.png` |
| `textures/block/device_gearbox_clutch_on.png` | `blocks/device_gearbox_clutch_on.png` |
| `textures/block/device_gearbox_sides.png` | `blocks/device_gearbox_sides.png` |
| `textures/block/device_kiln.png` | `blocks/device_kiln.png` |
| `textures/block/device_kiln_2.png` | `blocks/device_kiln_2.png` |
| `textures/block/device_kiln_active_2.png` | `blocks/device_kiln_active_2.png` |
| `textures/block/device_kiln_front_active.png` | `blocks/device_kiln_front_active.png` |
| `textures/block/device_kiln_top.png` | `blocks/device_kiln_top.png` |
| `textures/block/device_kiln_top_2.png` | `blocks/device_kiln_top_2.png` |
| `textures/block/industrial_metal_duct_flat_1.png` | `blocks/industrial_metal_duct_flat_1.png` |
| `textures/block/industrial_metal_duct_intake.png` | `blocks/industrial_metal_duct_intake.png` |
| `textures/block/industrial_metal_duct_vent.png` | `blocks/industrial_metal_duct_vent.png` |
| `textures/block/log_cronetree_inner.png` | `blocks/log_cronetree_inner.png` |
| `textures/block/log_fig_inner.png` | `blocks/log_fig_inner.png` |
| `textures/block/log_mulberry_inner.png` | `blocks/log_mulberry_inner.png` |
| `textures/block/metal_pipes_copper.png` | `blocks/metal_pipes_copper.png` |
| `textures/block/metal_steel_block.png` | `blocks/metal_steel_block.png` |
| `textures/block/plank_plywood.png` | `blocks/plank_plywood.png` |
| `textures/block/space-plating-vent.png` | `blocks/space-plating-vent.png` |
| `textures/block/stone_tiles_small.png` | `blocks/stone_tiles_small.png` |
| `textures/block/sturdy_bricks.png` | `blocks/sturdy_bricks.png` |
| `textures/block/terracotta_tile_large.png` | `blocks/terracotta_tile_large.png` |
| `textures/item/armor_helmet_steel.png` | `items/armor_helmet_steel.png` |
| `textures/item/gem_lens.png` | `items/gem_lens.png` |
| `textures/item/ingot_brass.png` | `items/ingot_brass.png` |
| `textures/item/ingot_bronze.png` | `items/ingot_bronze.png` |
| `textures/item/ingot_lead.png` | `items/ingot_lead.png` |
| `textures/item/ingot_steel.png` | `items/ingot_steel.png` |
| `textures/item/ingot_wroughtiron.png` | `items/ingot_wroughtiron.png` |
| `textures/item/ingot_zinc.png` | `items/ingot_zinc.png` |
| `textures/item/material_dust_clay.png` | `items/material_dust_clay.png` |
| `textures/item/material_dust_metal_steel.png` | `items/material_dust_metal_steel.png` |
| `textures/item/material_dust_quartz.png` | `items/material_dust_quartz.png` |
| `textures/item/material_graphite.png` | `items/material_graphite.png` |
| `textures/item/material_pile_scrap_metal.png` | `items/material_pile_scrap_metal.png` |
| `textures/item/material_pile_scrap_parts.png` | `items/material_pile_scrap_parts.png` |
| `textures/item/metal_steel_ingot.png` | `items/metal_steel_ingot.png` |
| `textures/item/metal_steel_plate.png` | `items/metal_steel_plate.png` |
| `textures/item/part_brick_adobe_fired.png` | `items/part_brick_adobe_fired.png` |
| `textures/item/part_brick_refractory.png` | `items/part_brick_refractory.png` |
| `textures/item/part_brick_sturdy.png` | `items/part_brick_sturdy.png` |
| `textures/item/part_chainlinks.png` | `items/part_chainlinks.png` |
| `textures/item/part_electronic_datacard_0.png` | `items/part_electronic_datacard_0.png` |
| `textures/item/part_electronic_electrode.png` | `items/part_electronic_electrode.png` |
| `textures/item/part_electronic_insulated_wire.png` | `items/part_electronic_insulated_wire.png` |
| `textures/item/part_electronic_thermionicvalve_1.png` | `items/part_electronic_thermionicvalve_1.png` |
| `textures/item/part_leather_strap.png` | `items/part_leather_strap.png` |
| `textures/block/industrial_metal_duct_1.png` | `blocks/industrial_metal_duct_1.png` |
| `textures/block/industrial_metal_duct_2.png` | `blocks/industrial_metal_duct_2.png` |
| `textures/block/industrial_metal_duct_sides_1.png` | `blocks/industrial_metal_duct_sides_1.png` |
| `textures/block/industrial_metal_duct_sides_2.png` | `blocks/industrial_metal_duct_sides_2.png` |
| `textures/block/industrial_metal_duct_flat_2.png` | `blocks/industrial_metal_duct_flat_2.png` |
| `textures/block/metal_alloy_block.png` | `blocks/metal_alloy_block.png` |
| `textures/block/device_workbench_top.png` | `blocks/device_workbench_top.png` |
| `textures/block/metal_tank_copper_sides.png` | `blocks/metal_tank_copper_sides.png` |
| `textures/block/device_terminal_front_on.png` | `blocks/device_terminal_front_on.png` |
| `textures/block/device_smelter_side.png` | `blocks/device_smelter_side.png` |
| `textures/block/device_terminal_side.png` | `blocks/device_terminal_side.png` |
| `textures/block/device_terminal_side_2.png` | `blocks/device_terminal_side_2.png` |
| `textures/block/device_barrel_sides.png` | `blocks/device_barrel_sides.png` |
| `textures/block/space-plating-square.png` | `blocks/space-plating-square.png` |
| `textures/block/block_metal_black_bronze_block_1.png` | `blocks/block_metal_black_bronze_block_1.png` |
| `textures/block/space-plating-plain.png` | `blocks/space-plating-plain.png` |
| `textures/block/device_miningcharge_top.png` | `blocks/device_miningcharge_top.png` |
| `textures/block/device_miningcharge_bottom.png` | `blocks/device_miningcharge_bottom.png` |
| `textures/block/device_miningcharge_sides.png` | `blocks/device_miningcharge_sides.png` |
| `textures/block/ore_stone_big_galena.png` | `blocks/ore_stone_big_galena.png` |
| `textures/block/ore_stone_brimstone.png` | `blocks/ore_stone_brimstone.png` |
| `textures/block/ore_stone_big_cylindrite.png` | `blocks/ore_stone_big_cylindrite.png` |
| `textures/block/ore_stone_zinc.png` | `blocks/ore_stone_zinc.png` |
| `textures/block/ore_stone_lead_2.png` | `blocks/ore_stone_lead_2.png` |
| `textures/item/dust_charcoal_mixture.png` | `items/dust_charcoal_mixture.png` |
| `textures/item/material_crystal_pyrite.png` | `items/material_crystal_pyrite.png` |
| `textures/item/material_rock_kaolin.png` | `items/material_rock_kaolin.png` |
| `textures/item/material_rock_ore_galena.png` | `items/material_rock_ore_galena.png` |
| `textures/item/paper_note_clean.png` | `items/paper_note_clean.png` |
| `textures/item/paper_note_major.png` | `items/paper_note_major.png` |
| `textures/item/paper_note_minor.png` | `items/paper_note_minor.png` |
| `textures/item/part_bronze_mechanisms.png` | `items/part_bronze_mechanisms.png` |
| `textures/item/part_cannister.png` | `items/part_cannister.png` |
| `textures/item/part_electronic_circuit_discrete.png` | `items/part_electronic_circuit_discrete.png` |
| `textures/item/part_electronic_coil_copper_2.png` | `items/part_electronic_coil_copper_2.png` |
| `textures/item/part_electronic_laser_diode.png` | `items/part_electronic_laser_diode.png` |
| `textures/item/part_electronic_magnet_2.png` | `items/part_electronic_magnet_2.png` |
| `textures/item/part_electronic_misc_0.png` | `items/part_electronic_misc_0.png` |
| `textures/item/part_electronic_misc_1.png` | `items/part_electronic_misc_1.png` |
| `textures/item/part_electronic_misc_10.png` | `items/part_electronic_misc_10.png` |
| `textures/item/part_electronic_misc_11.png` | `items/part_electronic_misc_11.png` |
| `textures/item/part_electronic_misc_12.png` | `items/part_electronic_misc_12.png` |
| `textures/item/part_electronic_misc_2.png` | `items/part_electronic_misc_2.png` |
| `textures/item/part_electronic_misc_3.png` | `items/part_electronic_misc_3.png` |
| `textures/item/part_electronic_misc_4.png` | `items/part_electronic_misc_4.png` |
| `textures/item/part_electronic_misc_5.png` | `items/part_electronic_misc_5.png` |
| `textures/item/part_electronic_resistor_3.png` | `items/part_electronic_resistor_3.png` |
| `textures/item/part_electronic_thermionicvalve_4.png` | `items/part_electronic_thermionicvalve_4.png` |
| `textures/item/part_electronic_thermionicvalve_8.png` | `items/part_electronic_thermionicvalve_8.png` |
| `textures/item/part_leather_patch_4.png` | `items/part_leather_patch_4.png` |
| `textures/item/part_leather_strap_2.png` | `items/part_leather_strap_2.png` |
| `textures/item/part_metal_alloy_flange.png` | `items/part_metal_alloy_flange.png` |
| `textures/item/part_metal_alloy_frame.png` | `items/part_metal_alloy_frame.png` |
| `textures/item/part_metal_alloy_nugget.png` | `items/part_metal_alloy_nugget.png` |
| `textures/item/part_metal_alloy_sheet_2.png` | `items/part_metal_alloy_sheet_2.png` |
| `textures/item/part_metal_alloy_tube.png` | `items/part_metal_alloy_tube.png` |
| `textures/item/part_metal_bolts.png` | `items/part_metal_bolts.png` |
| `textures/item/part_metal_box_large.png` | `items/part_metal_box_large.png` |
| `textures/item/part_metal_box_small.png` | `items/part_metal_box_small.png` |
| `textures/item/part_metal_composite_tube.png` | `items/part_metal_composite_tube.png` |
| `textures/item/part_metal_gasket.png` | `items/part_metal_gasket.png` |
| `textures/item/part_metal_lead_ingot.png` | `items/part_metal_lead_ingot.png` |
| `textures/item/part_metal_lead_ingot_2.png` | `items/part_metal_lead_ingot_2.png` |
| `textures/item/part_metal_plate_heavy_cladding.png` | `items/part_metal_plate_heavy_cladding.png` |
| `textures/item/part_metal_spring.png` | `items/part_metal_spring.png` |
| `textures/item/part_mould.png` | `items/part_mould.png` |
| `textures/item/part_mould_sword.png` | `items/part_mould_sword.png` |
| `textures/item/part_paper_rune_bloody.png` | `items/part_paper_rune_bloody.png` |
| `textures/item/part_plate_small_laminate_green.png` | `items/part_plate_small_laminate_green.png` |
| `textures/item/part_plate_small_leatherbound.png` | `items/part_plate_small_leatherbound.png` |
| `textures/item/part_plate_thick_steel.png` | `items/part_plate_thick_steel.png` |
| `textures/item/part_rail.png` | `items/part_rail.png` |
| `textures/item/part_rope.png` | `items/part_rope.png` |
| `textures/item/part_rubber_gasket.png` | `items/part_rubber_gasket.png` |
| `textures/item/part_tube_composite.png` | `items/part_tube_composite.png` |
| `textures/item/part_tube_galvanized.png` | `items/part_tube_galvanized.png` |
| `textures/item/part_tube_iron.png` | `items/part_tube_iron.png` |
| `textures/item/part_tube_plastic.png` | `items/part_tube_plastic.png` |
| `textures/item/tool_chisel_iron.png` | `items/tool_chisel_iron.png` |
| `textures/item/tool_fancy_iron_chisel.png` | `items/tool_fancy_iron_chisel.png` |
| `textures/item/tool_soldering_steel.png` | `items/tool_soldering_steel.png` |
| `textures/item/tool_spanner_bronze.png` | `items/tool_spanner_bronze.png` |
| `textures/item/tool_spanner_lead.png` | `items/tool_spanner_lead.png` |
| `textures/item/part_metal_alloy_rod.png` | `items/part_metal_alloy_rod.png` |
| `textures/item/part_metal_steel_ingot.png` | `items/part_metal_steel_ingot.png` |
| `textures/item/part_metal_steel_rod.png` | `items/part_metal_steel_rod.png` |
| `textures/item/part_mould_ingot.png` | `items/part_mould_ingot.png` |
| `textures/item/part_plate_copper.png` | `items/part_plate_copper.png` |
| `textures/item/part_plate_rivets.png` | `items/part_plate_rivets.png` |
| `textures/item/part_plate_small_copper.png` | `items/part_plate_small_copper.png` |
| `textures/item/part_plate_steel.png` | `items/part_plate_steel.png` |
| `textures/item/part_punchcard_blank.png` | `items/part_punchcard_blank.png` |
| `textures/item/part_tube_copper.png` | `items/part_tube_copper.png` |
| `textures/item/part_wire_copper.png` | `items/part_wire_copper.png` |
| `textures/item/part_wire_iron.png` | `items/part_wire_iron.png` |
| `textures/item/tool_chisel_steel.png` | `items/tool_chisel_steel.png` |
| `textures/item/tool_hammer_stone.png` | `items/tool_hammer_stone.png` |
| `textures/item/tool_pouch_brown.png` | `items/tool_pouch_brown.png` |
| `textures/item/instrument_bag.png` | `items/tool_pouch_gray.png` |
| `textures/item/tool_soldering_iron.png` | `items/tool_soldering_iron.png` |
| `textures/item/tool_spanner_iron.png` | `items/tool_spanner_iron.png` |

Greyscale form sprites (2026-10-20): each is a recoloured copy (luminance only, brightest 5% stretched to near white, alpha kept) of a file above, tinted per material at runtime by `client/FormTint`. Modified, so CC BY 4.0's "indicate changes" applies.

| `textures/item/form/*.png` | derived from |
| --- | --- |
| `ingot`, `ground_ingot` | `items/ingot_steel.png` |
| `nugget`, `bullet`, `case` | `items/annealed_copper_nugget.png` |
| `plate`, `grip` | `items/annealed_copper_plate.png` |
| `rod`, `turned_rod` | `items/annealed_copper_rod.png` (file since deleted, source `unused-textures`) |
| `wire`, `fine_wire` | `items/part_wire_iron.png` |
| `foil` | `items/part_plate_small_copper.png` |
| `scrap` | `items/material_pile_scrap_metal.png` |
| `swarf` | `items/material_dust_metal_steel.png` |
| `drilled_plate` | `items/part_plate_rivets.png` |
| `textures/block/form/block.png` | `blocks/annealed_copper_block.png` (file since deleted, source `unused-textures`) |
| `textures/block/metal_tank_sides.png`, `metal_tank_ends.png` | `blocks/metal_tank_copper_sides.png`, `blocks/metal_tank_copper_ends.png` (same recolour; tinted copper in `client/FormTint`) |

Molten-metal sprite (2026-10-20): `textures/block/molten_metal.png` (+ `.mcmeta`) is `fluids/fluid_molten.png` from `malcolmriley/unused-textures` (CC BY 4.0, Malcolm Riley), greyscaled (luminance only, brightest 3% stretched to white) and tinted per metal in `FClientSetup.registerFluidRendering`. Modified, so CC BY 4.0's "indicate changes" applies. Replaces vanilla's lava sprites as the molten stand-in.

### Silent Gear textures

All under `src/main/resources/assets/silentgear/textures/item/` in `SilentChaos512/Silent-Gear`, MIT,
Copyright (c) 2018 SilentChaos512. Grayscale layers, tinted per part by `client/ModularClient`.
Placeholder, to be replaced like the rest.

| Feedback file (`textures/item/modular/`) | Source (`<type>/<file>`) |
| --- | --- |
| `sword_0`, `sword_1`, `sword_2`, `sword_3` | `sword/main_generic_hc`, `sword/rod_generic_hc`, `sword/guard_generic_hc`, `sword/binding_generic` |
| `pickaxe_0`, `pickaxe_1` | `pickaxe/main_generic_hc`, `pickaxe/rod_generic_hc` |
| `axe_0`, `axe_1` | `axe/main_generic_hc`, `axe/rod_generic_hc` |
| `shovel_0`, `shovel_1` | `shovel/main_generic_hc`, `shovel/rod_generic_hc` |
| `hoe_0`, `hoe_1` | `hoe/main_generic_hc`, `hoe/rod_generic_hc` |
| `sickle_0`, `sickle_1` | `sickle/main_generic_hc`, `sickle/rod_generic_hc` |
| `spear_0`, `spear_1` | `spear/main_generic_hc`, `spear/rod_generic_hc` |
| `maul_0`, `maul_1` | `hammer/main_generic_hc`, `hammer/rod_generic_hc` |
| `maul_mace_0`, `maul_mace_1` | `mace/main_generic_hc`, `mace/rod_generic_hc` |
| `spear_trident_0`, `spear_trident_1` | `trident/main_generic_hc`, `trident/rod_generic_hc` (trident and pitchfork) |
| `sword_dagger_0`, `sword_dagger_1` | `dagger/main_generic_hc`, `dagger/rod_generic_hc` |
| `sword_katana_0`, `sword_katana_1` | `katana/main_generic_hc`, `katana/rod_generic_hc` |
| `sword_machete_0`, `sword_machete_1` | `machete/main_generic_hc`, `machete/rod_generic_hc` |
| `pickaxe_prospector_0`, `pickaxe_prospector_1` | `prospector_hammer/main_generic_hc`, `prospector_hammer/rod_generic_hc` |
| `pickaxe_miner_0`, `pickaxe_miner_1` | `mattock/main_generic_hc`, `mattock/rod_generic_hc` |
| `shears_0`, `shears_1` | `shears/main_generic_hc`, `shears/rod_generic_hc` |

## Sounds

| File | Source | Licence |
| --- | --- | --- |
| `assets/feedback/sounds/combat/feedbacker_parry.ogg` | the parry sound from ULTRAKILL (Hakita, New Blood Interactive), supplied by the author from `Ultrakill_parry.mp3` (the first 285 ms of silence trimmed 2026-10-24, so the sound starts when the punch lands) | **Not licensed to us; kept on purpose** (author ruling, 2026-10-22). An explicit reference to ULTRAKILL's parry in the Feedbacker, which the author holds to be fair use, with precedent for such homages. Fair use is a defence rather than a permission, so this stays a ruling to revisit if the rights holder objects. |
| `assets/feedback/sounds/cooking/bgm_great.ogg`, `bgm_success.ogg`, `bgm_fail.ogg`, `fanfare_success.ogg`, `fanfare_fail.ogg` | the cooking-result music and jingles from The Legend of Zelda: Breath of the Wild (Nintendo), supplied by the author as `BGM_CookingGreat.wav`, `BGM_CookingSuccess.wav`, `BGM_CookingFail.wav`, `Cookset_SuccessGreat.wav` and `Cookset_Fail.wav`, converted to Ogg Vorbis unchanged otherwise (2026-10-24). Played by the Prep Table's pour minigame (`client/PrepPourScreen`, `FSounds.COOKING_*`): the music while a pour is judged, then a fanfare as the result is shown | **Not licensed to us; kept on purpose** (author ruling, 2026-10-24). A homage to Breath of the Wild's cooking: five short sounds in a free mod that is not sold and does not compete with the game. That is the author's judgement, not a permission: Nintendo has not licensed these to us, and fair use is a defence rather than a grant. This stays a ruling to revisit, and the files are to be replaced or removed if the rights holder objects. |

## Skins

| File | Source | Licence |
| --- | --- | --- |
| `assets/feedback/textures/entity/feedbacker_skin.png` | a Minecraft skin supplied by the author; only its right arm is drawn, as the Feedbacker's sleeve | Author's own supply; creator and licence not recorded. Confirm before any public release. |

## Models

| File | Source | Licence |
| --- | --- | --- |
| `assets/feedback/models/item/gun.json` | "[minecraft] pistol" by sedona1029 on Sketchfab. Converted from its glTF into a vanilla JSON element model (cube geometry and rotations kept; UVs replaced with the greyscale block sprite at one texel per unit, tint index per cube by gun role; display transforms added). Its own texture is not used. Placeholder for the gun item. | **CC BY 4.0**, credit: sedona1029 |

### cleannrooster/forg-cleannrooster-assets (skill tree icons, 2026-10-23)

Free for any use including commercial, attribution appreciated, covers only what that repo holds (its README). Copied unmodified, tree-node icons for the `K` skill screen. Credit: cleannrooster.

| Feedback file | Source path |
| --- | --- |
| `textures/gui/skill/wider_window.png` | `spell/64/prepare64.png` |
| `textures/gui/skill/quick_recovery.png` | `spell/64/time_dilate.png` |
| `textures/gui/skill/tight_redirect.png` | `spell/64/homing.png` |
| `textures/gui/skill/hard_stagger.png` | `spell/64/skullcrusher64.png` |
| `textures/gui/skill/riposte.png` | `spell/64/devastating_cut64.png` |
| `textures/gui/skill/long_roll.png` | `spell/64/blink.png` |
| `textures/gui/skill/extra_iframes.png` | `spell/64/overload.png` |
| `textures/gui/skill/wall_kick.png` | `spell/64/upheaval.png` |
| `textures/gui/skill/pistol_whip.png` | `spell/64/buckshot.png` |
| `textures/gui/skill/chamber.png` | `spell/64/amethystburst.png` |
| `textures/gui/skill/dual.png` | `spell/battlemage/replicating_missile.png` |
| `textures/gui/skill/spin.png` | `spell/soulwarden/seismic_cry.png` |
| `textures/gui/skill/blade_edge.png` | `spell/64/cleave64.png` |
| `textures/gui/skill/blade_flourish.png` | `spell/battlemage/frost_fan.png` |
| `textures/gui/skill/mace_weight.png` | `spell/64/glacialhammer.png` |
| `textures/gui/skill/mace_quake.png` | `spell/64/slam64.png` |
| `textures/gui/skill/spear_reach.png` | `spell/64/spear_throw.png` |
| `textures/gui/skill/spear_vault.png` | `spell/64/vaulting_slam64.png` |
| `textures/gui/skill/fast_reel.png` | `spell/soulwarden/tendrils.png` |
| `textures/gui/skill/winch.png` | `spell/soulwarden/bind.png` |
| `textures/gui/skill/firm_grip.png` | `spell/soulwarden/guide.png` |
| `textures/gui/skill/iron_grip.png` | `spell/soulwarden/heavy_strike.png` |

Animation clip (2026-10-23, same licence as above, copied unmodified): `assets/feedback/player_animations/slice.json` is `anim/ability/slice.json`, played by `client/SwingClips` through playerAnimator.

### cleannrooster/forg-cleannrooster-assets (skill icons, batch 2, 2026-10-24)

Same source and licence as the batch above (free for any use, attribution appreciated, credit cleannrooster). Copied unmodified, one icon per skill node that had none. Node icons for the seven trees.

| Feedback file | Source path |
| --- | --- |
| `textures/gui/skill/roll_with_it.png` | `spell/soulwarden/pilgrimage.png` |
| `textures/gui/skill/brace.png` | `spell/soulwarden/ancestral_guardian.png` |
| `textures/gui/skill/shield_wall.png` | `spell/64/glacier.png` |
| `textures/gui/skill/advance.png` | `spell/64/sonicboom.png` |
| `textures/gui/skill/settle.png` | `spell/soulwarden/ancestral_totem.png` |
| `textures/gui/skill/challenge.png` | `spell/legacy/challenge.png` |
| `textures/gui/skill/throwback.png` | `spell/64/scorching_ray.png` |
| `textures/gui/skill/answer.png` | `spell/legacy/reengage.png` |
| `textures/gui/skill/slip.png` | `spell/legacy/frost_burst.png` |
| `textures/gui/skill/reckoning.png` | `spell/64/supernova.png` |
| `textures/gui/skill/reprisal.png` | `spell/64/eldritch_blast.png` |
| `textures/gui/skill/rebuke.png` | `spell/legacy/rebuke.png` |
| `textures/gui/skill/return_fire.png` | `spell/64/arcane_launch.png` |
| `textures/gui/skill/braced.png` | `spell/legacy/bulwark.png` |
| `textures/gui/skill/braced_fire.png` | `spell/battlemage/frost_glyph.png` |
| `textures/gui/skill/human_shield.png` | `spell/legacy/protect.png` |
| `textures/gui/skill/heavy_butt.png` | `spell/legacy/slam.png` |
| `textures/gui/skill/muzzle_stagger.png` | `spell/64/sonicboom.png` |
| `textures/gui/skill/bayonet.png` | `spell/legacy/knife_throw.png` |
| `textures/gui/skill/point_blank.png` | `spell/legacy/focus_shot.png` |
| `textures/gui/skill/grab.png` | `spell/legacy/beartrap.png` |
| `textures/gui/skill/throw.png` | `spell/legacy/throwbomb.png` |
| `textures/gui/skill/bowling.png` | `spell/legacy/monkeyslam.png` |
| `textures/gui/skill/vice.png` | `spell/soulwarden/pain_attunement.png` |
| `textures/gui/skill/punish.png` | `spell/64/power_word_kill.png` |
| `textures/gui/skill/stomp.png` | `spell/legacy/collapse.png` |
| `textures/gui/skill/crush.png` | `spell/legacy/shield_shatter.png` |
| `textures/gui/skill/cleave.png` | `spell/legacy/cleave.png` |
| `textures/gui/skill/whirl.png` | `spell/legacy/whirlingblades.png` |
| `textures/gui/skill/hamstring.png` | `spell/legacy/eviscerate.png` |
| `textures/gui/skill/quake_leap.png` | `spell/64/flame_geyser.png` |
| `textures/gui/skill/brand.png` | `spell/64/combustion.png` |
| `textures/gui/skill/overheat.png` | `spell/64/greater_combust.png` |
| `textures/gui/skill/scorch.png` | `spell/64/scorching_agony.png` |
| `textures/gui/skill/pin_down.png` | `spell/64/snare.png` |
| `textures/gui/skill/muzzle_flash.png` | `spell/64/arcane_nova.png` |
| `textures/gui/skill/dazzle.png` | `spell/soulwarden/soulflare.png` |
| `textures/gui/skill/concussive.png` | `spell/battlemage/fire_burst.png` |
| `textures/gui/skill/wide_pin.png` | `spell/64/frozen_resonance.png` |
| `textures/gui/skill/held_pin.png` | `spell/64/deep_chill.png` |
| `textures/gui/skill/rending_heel.png` | `spell/legacy/lightningslash.png` |
| `textures/gui/skill/searing_strike.png` | `spell/battlemage/flame_torrent.png` |
| `textures/gui/skill/forge_slam.png` | `spell/64/greater_fireball.png` |
| `textures/gui/skill/pry_open.png` | `spell/soulwarden/spiderlightning.png` |
| `textures/gui/skill/marked_rounds.png` | `spell/64/agonizingblast.png` |
| `textures/gui/skill/disarm_shot.png` | `spell/64/magic_missile.png` |
| `textures/gui/skill/slide.png` | `spell/legacy/lightningstep.png` |
| `textures/gui/skill/steady_barrage.png` | `spell/legacy/riflebarrage.png` |
| `textures/gui/skill/called_shot.png` | `spell/battlemage/aether_ray.png` |
| `textures/gui/skill/dead_eye.png` | `spell/64/enders_gaze.png` |
| `textures/gui/skill/coin_flip.png` | `spell/battlemage/sky_shard.png` |
| `textures/gui/skill/second_mark.png` | `spell/battlemage/sigil_of_consumption.png` |
| `textures/gui/skill/round_the_corner.png` | `spell/battlemage/lightning_tendrils.png` |
| `textures/gui/skill/mark_and_fire.png` | `spell/legacy/detonate_mines.png` |
| `textures/gui/skill/recoil_vault.png` | `spell/legacy/vaulting_slam.png` |
| `textures/gui/skill/keen_eye.png` | `spell/battlemage/arcanopulse.png` |
| `textures/gui/skill/far_end.png` | `spell/64/icestorm.png` |
| `textures/gui/skill/close_work.png` | `spell/legacy/spellstrike.png` |
| `textures/gui/skill/reel_in.png` | `spell/soulwarden/spirit_bolts.png` |
| `textures/gui/skill/vault.png` | `spell/legacy/phoenix_dive.png` |
| `textures/gui/skill/patient_hand.png` | `spell/legacy/meditate.png` |
| `textures/gui/skill/jab.png` | `spell/legacy/flicker_strike.png` |
| `textures/gui/skill/lunge.png` | `spell/legacy/flame_slash.png` |
| `textures/gui/skill/sweep.png` | `spell/legacy/amethyst_slash.png` |
| `textures/gui/skill/chop.png` | `spell/legacy/finalstrike.png` |
| `textures/gui/skill/uppercut.png` | `spell/legacy/skullcrusher.png` |
| `textures/gui/skill/flurry.png` | `spell/legacy/featherstorm.png` |
| `textures/gui/skill/rising_tempo.png` | `spell/soulwarden/galvanize.png` |
| `textures/gui/skill/frenzy.png` | `spell/battlemage/tempest.png` |
| `textures/gui/skill/haymaker.png` | `spell/legacy/explosion.png` |
| `textures/gui/skill/catch_breath.png` | `spell/legacy/coldbuff.png` |
| `textures/gui/skill/spring_heel.png` | `spell/legacy/bolster.png` |
| `textures/gui/skill/kick.png` | `spell/legacy/feather_lash.png` |
| `textures/gui/skill/breach_kick.png` | `spell/legacy/shield_charge.png` |
| `textures/gui/skill/dropkick.png` | `spell/legacy/phoenix_down.png` |
| `textures/gui/skill/axe_kick.png` | `spell/legacy/divine_smite.png` |
| `textures/gui/skill/hard_kick.png` | `spell/legacy/charge.png` |
| `textures/gui/skill/wall_force.png` | `spell/legacy/siege_cascade.png` |
| `textures/gui/skill/wall_crush.png` | `spell/legacy/manastorm.png` |
| `textures/gui/skill/feint.png` | `spell/legacy/lights_out.png` |
| `textures/gui/skill/draw_switch.png` | `spell/legacy/pocket_mortar.png` |
| `textures/gui/skill/swing.png` | `spell/legacy/eldritchblast.png` |
| `textures/gui/skill/snap_volley.png` | `spell/legacy/laytrap.png` |
| `textures/gui/skill/walking_fire.png` | `spell/legacy/fire_burst.png` |
| `textures/gui/skill/quick_hands.png` | `spell/legacy/prepare.png` |
| `textures/gui/skill/steady_lungs.png` | `spell/legacy/defiance_of_destiny.png` |
| `textures/gui/skill/adrenaline.png` | `spell/legacy/righteous_fervor.png` |
| `textures/gui/skill/shake_off.png` | `spell/32/monkeyreflect.png` |
| `textures/gui/skill/cauterize.png` | `spell/64/flameray.png` |
| `textures/gui/skill/wind_back.png` | `spell/64/ice_nova.png` |
| `textures/gui/skill/grit_teeth.png` | `spell/battlemage/word_of_pain.png` |
| `textures/gui/skill/break_free.png` | `spell/64/icebarrage.png` |
| `textures/gui/skill/callused_hands.png` | `spell/64/combust.png` |
| `textures/gui/skill/sear_shut.png` | `spell/battlemage/ignition.png` |
| `textures/gui/skill/blood_up.png` | `spell/legacy/rally.png` |
| `textures/gui/skill/breather.png` | `spell/legacy/whirling_assault.png` |
| `textures/gui/skill/last_stand.png` | `spell/legacy/soul_of_vengeance.png` |
| `textures/gui/skill/hot_clear.png` | `spell/legacy/fire_spellstrike.png` |
| `textures/gui/skill/dodge_charge.png` | `spell/legacy/arcane_blink.png` |
| `textures/gui/skill/spent_case_draw.png` | `spell/legacy/arcane_burst.png` |
| `textures/gui/skill/field_dressing.png` | `spell/legacy/consecrate.png` |
| `textures/gui/skill/second_breath.png` | `spell/legacy/encore.png` |
| `textures/gui/skill/shadow_step.png` | `spell/legacy/arcane_barrage.png` |
| `textures/gui/skill/ambush.png` | `spell/legacy/deathchill.png` |
| `textures/gui/skill/quiet_roll.png` | `spell/legacy/thesis.png` |
| `textures/gui/skill/slip_past.png` | `spell/legacy/arctic_armor.png` |
| `textures/gui/skill/smoke_step.png` | `spell/64/scorchingwind.png` |
| `textures/gui/skill/thick_smoke.png` | `spell/legacy/combustion.png` |
| `textures/gui/skill/vanish.png` | `spell/legacy/icenova.png` |
| `textures/gui/skill/long_shadow.png` | `spell/legacy/frostgrasp.png` |
| `textures/gui/skill/pocket_sand.png` | `spell/legacy/frostslash.png` |
| `textures/gui/skill/grit.png` | `spell/legacy/reverberation_brand.png` |
| `textures/gui/skill/wall_smash.png` | `spell/legacy/dynamo.png` |
| `textures/gui/skill/far_roll.png` | `spell/legacy/disengage.png` |
| `textures/gui/skill/wall_run.png` | `spell/legacy/echoes1.png` |
| `textures/gui/skill/wall_rebound.png` | `spell/legacy/echoes2.png` |

### KosmX/minecraftPlayerAnimator (`1.21` branch, `../PlayerAnimator-reference`)

**MIT** (Copyright (c) 2022 KosmX). Used as a dependency, not copied: `dev.kosmx.player-anim:player-animation-lib-forge:2.0.4+1.21.1`, jarJar'd. API read from its source and README; nothing taken.

19 of the 24 emote-format clips from `anim/ability` (cleave, flourish, whirl, stabend and fist_flurry_right were tried and removed) are in `assets/feedback/player_animations/`, copied unmodified (2026-10-23). `shield_bash.json` is not emote format and was not copied.

### Better Combat (Zsolt Molnar, all rights reserved; `../BetterCombat-reference`, 1.21.1 branch)

**Nothing of it is copied, bundled or shipped; it is a required mod that players install** (`neoforge.mods.toml`, client side; pinned in the test pack by `build-and-deploy.sh`). `client/UpswingSwing` calls its `PlayerAttackAnimatable` (compile-only dependency) to play its clips with its own upswing gearing, and plays its sounds by event id; `SwingClips` and the Feedbacker parry use its punch. Its attack rules (weapon-attributes files, hitboxes, timing, the attack key) are not used: our code decides them. The README invites mods to integrate through its JSON API and to reference its bundled clips; this is a deeper, Java-level use of the same pieces, and the author's ruling on it has not been asked (author's decision 2026-10-24: proceed, with the notice below). A log warning and a once-per-session chat line say to report problems to Feedback, never to Better Combat. `LICENSING.md` carries no §7 exception for it yet.

### Croptopia (`development` branch, `../Croptopia-reference`)

**MIT** (Copyright (c) 2020 Thonk, Rainbowcraft2). Sprite files only, copied unmodified (2026-10-25) for named foods (`docs/feedback_modular_foods_spec.md`); no code taken.

| Ours (`assets/feedback/textures/item/`) | Croptopia (`assets/croptopia/textures/item/`) |
|---|---|
| `french_fries.png` | `french_fries.png` |
| `potato_chips.png` | `potato_chips.png` |
| `mashed_potatoes.png` | `mashed_potatoes.png` |
| `tomato.png` | `tomato.png` |
| `onion.png` | `onion.png` |
| `garlic.png` | `garlic.png` |
| `cabbage.png` | `cabbage.png` |
| `corn.png` | `corn.png` |
| `rice.png` | `rice.png` |
| `steamed_rice.png` | `steamed_rice.png` |
| `soybean.png` | `soybean.png` |
| `chile_pepper.png` | `chile_pepper.png` |
| `ginger.png` | `ginger.png` |
| `pepper.png` | `pepper.png` |
| `salsa.png` | `salsa.png` |
| `turnip.png` | `turnip.png` |
| `block/cabbage_crop_stage0.png` | `block/cabbage_crop_stage0.png` |
| `block/cabbage_crop_stage1.png` | `block/cabbage_crop_stage1.png` |
| `block/cabbage_crop_stage2.png` | `block/cabbage_crop_stage2.png` |
| `block/cabbage_crop_stage3.png` | `block/cabbage_crop_stage3.png` |
| `item/cabbage_seed.png` | `item/cabbage_seed.png` |
| `block/onion_crop_stage0.png` | `block/onion_crop_stage0.png` |
| `block/onion_crop_stage1.png` | `block/onion_crop_stage1.png` |
| `block/onion_crop_stage2.png` | `block/onion_crop_stage2.png` |
| `block/onion_crop_stage3.png` | `block/onion_crop_stage3.png` |
| `item/onion_seed.png` | `item/onion_seed.png` |
| `block/garlic_crop_stage0.png` | `block/garlic_crop_stage0.png` |
| `block/garlic_crop_stage1.png` | `block/garlic_crop_stage1.png` |
| `block/garlic_crop_stage2.png` | `block/garlic_crop_stage2.png` |
| `block/garlic_crop_stage3.png` | `block/garlic_crop_stage3.png` |
| `item/garlic_seed.png` | `item/garlic_seed.png` |
| `block/turnip_crop_stage0.png` | `block/turnip_crop_stage0.png` |
| `block/turnip_crop_stage1.png` | `block/turnip_crop_stage1.png` |
| `block/turnip_crop_stage2.png` | `block/turnip_crop_stage2.png` |
| `block/turnip_crop_stage3.png` | `block/turnip_crop_stage3.png` |
| `item/turnip_seed.png` | `item/turnip_seed.png` |
| `block/soybean_crop_stage0.png` | `block/soybean_crop_stage0.png` |
| `block/soybean_crop_stage1.png` | `block/soybean_crop_stage1.png` |
| `block/soybean_crop_stage2.png` | `block/soybean_crop_stage2.png` |
| `block/soybean_crop_stage3.png` | `block/soybean_crop_stage3.png` |
| `item/soybean_seed.png` | `item/soybean_seed.png` |
| `block/tomato_crop_stage0.png` | `block/tomato_crop_stage0.png` |
| `block/tomato_crop_stage1.png` | `block/tomato_crop_stage1.png` |
| `block/tomato_crop_stage2.png` | `block/tomato_crop_stage2.png` |
| `block/tomato_crop_stage3.png` | `block/tomato_crop_stage3.png` |
| `item/tomato_seed.png` | `item/tomato_seed.png` |
| `block/corn_crop_stage0.png` | `block/corn_crop_stage0.png` |
| `block/corn_crop_stage1.png` | `block/corn_crop_stage1.png` |
| `block/corn_crop_stage2.png` | `block/corn_crop_stage2.png` |
| `block/corn_crop_stage3.png` | `block/corn_crop_stage3.png` |
| `item/corn_seed.png` | `item/corn_seed.png` |
| `block/chile_pepper_crop_stage0.png` | `block/chile_pepper_crop_stage0.png` |
| `block/chile_pepper_crop_stage1.png` | `block/chile_pepper_crop_stage1.png` |
| `block/chile_pepper_crop_stage2.png` | `block/chile_pepper_crop_stage2.png` |
| `block/chile_pepper_crop_stage3.png` | `block/chile_pepper_crop_stage3.png` |
| `item/chile_pepper_seed.png` | `item/chile_pepper_seed.png` |
| `block/rice_crop_stage0.png` | `block/rice_crop_stage0.png` |
| `block/rice_crop_stage1.png` | `block/rice_crop_stage1.png` |
| `block/rice_crop_stage2.png` | `block/rice_crop_stage2.png` |
| `block/rice_crop_stage3.png` | `block/rice_crop_stage3.png` |
| `item/rice_seed.png` | `item/rice_seed.png` |
| `block/ginger_crop_stage0.png` | `block/ginger_crop_stage0.png` |
| `block/ginger_crop_stage1.png` | `block/ginger_crop_stage1.png` |
| `block/ginger_crop_stage2.png` | `block/ginger_crop_stage2.png` |
| `block/ginger_crop_stage3.png` | `block/ginger_crop_stage3.png` |
| `item/ginger_seed.png` | `item/ginger_seed.png` |
| `block/pepper_crop_stage0.png` | `block/pepper_crop_stage0.png` |
| `block/pepper_crop_stage1.png` | `block/pepper_crop_stage1.png` |
| `block/pepper_crop_stage2.png` | `block/pepper_crop_stage2.png` |
| `block/pepper_crop_stage3.png` | `block/pepper_crop_stage3.png` |
| `item/pepper_seed.png` | `item/pepper_seed.png` |
| `fried_egg.png` | `sunny_side_eggs.png` |
