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
| **Super Factory Manager** | MPL-2.0 | **Yes** | No per-file "Incompatible With Secondary Licenses" notice found, so combinable under GPL-3.0 per MPL §3.3/§10.4's default. Read, nothing taken — see below |

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
- `Fuel` / the `fuel/` datapack table — the shape `(ingredient, duration, temperature)`
  is TFC's `Fuel` record. It is also the obvious shape, and it is the one §15 already
  wanted for compat authored as a table.
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
`TODO.md` §4d for the full naming discussion.

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

Reference checkout lives outside this repository at `../GregTech-Modern-7.5.3`. Reference
material, never a dependency.

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

## Super Factory Manager

Cloned (`../SuperFactoryManager`, branch `1.21.1` — verified against `gradle.properties` before
relying on it) and browsed while scoping the controller design, then set aside: it's a genuine
domain-specific *language* for logistics automation, with its own grammar, parser, and a VS Code
language-extension package — solving item/fluid routing, not "read a quantity and switch a
supply," and built at the complexity of a real compiler where `feedback_controller_spec.md` §1
deliberately draws a ceiling. Nothing taken; see that document's §6 for the full reasoning.

MPL-2.0 (`LICENSE.txt`), no per-file "Incompatible With Secondary Licenses" notice found in the
source read, so under MPL §10.4 the code remains available under GPL-3.0 as a Secondary
Licence — a real route, just not one walked, same as GregTech's linking-only route was never
walked either.

## PneumaticCraft: Repressurized

Read for the controller/program-graph design in `feedback_controller_spec.md` — the drone
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
than drawn from a palette. See `feedback_controller_spec.md` §7 for the full account.

**Read a second time, for `PressureVesselBlockEntity` (`TODO.md` §5e) — user-directed rather than
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
