# Feedback — Vehicles

**Working document, not one of the three.** Scopes merging two of the author's own mods into
Feedback as first-class content — not compat:

- **SummerCraft** (`../SummerCraft-reference`, NeoForge 1.21.1, ~5.4k lines) — My Summer
  Car-style car: fit parts into slots, bolt them down, fill fluids, crank it, drive a manual.
- **SummerWorks** (`../SummerWorks-reference`, two commits) — vehicles as **Sable** physics
  bodies. Only a Vehicle Core and a Thruster existed there.

Both are the author's own code, so nothing here is a licensing debt; `THIRD-PARTY-LICENSES.md`
does not apply. Sable is a dependency, not a source — see `LICENSING.md`'s section 7
exception. Where this document and `feedback_philosophy.md` disagree, the philosophy wins.

**Status (2026-10-16): cars drive.** Built and played in a client: the welded frame, the Lift
(lifting, lowering to blocks, carrying), collision cells, bearings, ray tires with guard rays,
drive lines from any turning block (several per frame, straight axles, universal joints), and the
build tools' corner card. Built, not yet played: the Driver's Seat and analog controller cards.
Not built: steering, brakes, springs and shocks, a gearbox, a differential.

---

## 1. Rulings already made

Settled by the author, 2026-10-10. Not reopened here.

1. **Sable is a hard dependency.** Feedback is played standalone already; nobody installs it
   piecemeal. The licence exception is in `LICENSING.md`.
2. **The two mods merge into one vehicle system now**, rather than SummerWorks maturing
   separately first. SummerCraft supplies the content — engine, parts, bolts, fluids,
   tuning, driving, HUD, dyno. SummerWorks supplies the body: Sable instead of
   SummerCraft's hand-rolled `CarEntity` + `CarPhysics`.
3. **A vehicle is a base of ordinary blocks with free-form parts built onto it** (author,
   2026-10-11, reversing the 2026-10-10 ruling that vehicles are *not* built from world blocks).
   The functional core -- engine, seat, power sources, control blocks -- is placed as normal
   blocks, Aeronautics-style, and lifted into one Sable body. Suspension, drivetrain and body
   panels are built in the free-form vertex system (§4), modelled on *Main Assembly*. The old
   ruling was tempting because it kept the car's look entirely in the player's hands; it broke
   on the parts that *are* blocks by nature -- an engine wants a block entity, a block world and
   a place to tick, and gluing one to a tilted plate has no good answer. Free-form stays for the
   parts whose shape is the engineering choice.
4. **Vehicles wear with use.** This is the hand-hammer precedent, not an exception to the
   misuse-only wear rule: that rule is about *machines*. A vehicle is a **tool**, and tools
   wear in vanilla. Tires from distance, plugs from running time, clutch from slipping and
   brakes from braking all stay.

   Worth stating the boundary so it does not creep: usage wear is for the vehicle and its
   parts. A *stationary* machine driven by a vehicle engine (an engine on a stand turning a
   shaft) is a machine again, and wears only from misuse.
5. **Tuning stays a knob, but banded** — named coarse positions (low / mid / high) with
   adjustment *inside* the band. See §3, which argues the inside-the-band part.
6. **Fine information comes from an instrument.** Bolt sizes, exact mixture, exact timing
   are read off a carried item, consistent with §8's *senses give adjectives, numbers cost an
   instrument*. The world gives adjectives for free: black smoke, knock, a hot gauge.
7. **SummerCraft's own units map onto Feedback's** (§2). No parallel unit system.

---

## 2. Unit mapping

SummerCraft works in SI. Most of it lands on an existing §17 unit with no fudge, because
Feedback's units were anchored to SI in the first place.

| SummerCraft | Feedback | Note |
| :--- | :--- | :--- |
| RPM | `Rpm` | Identical. |
| torque, Nm | `Su` | Feedback's `Su` is Create's stress — a load at speed. Torque on a shaft is the same idea. **Settled (author, 2026-10-11): a torque curve** -- weak at idle, peak mid-range, falling at redline. Read at the current RPM, the curve *is* the engine's `Su` capacity, so an engine on a stand drives a stationary shaft network with no translator. |
| horsepower, kW | — | Derived, not stored. Power is `Su × RPM`; the dyno graph shows that product. |
| °C (coolant, engine temp) | `Tu` | The engine is a `ThermalBody`. Combustion is a `HeatSource`; the radiator is a `Conductance` to ambient. No coolant means a leak that is too small, and the engine heads for the flame temperature — which is §1's thermal model working unmodified. |
| fuel, litres | `mB` | 1 mB is exactly 1 litre. No conversion. |
| mass, kg (`MASS = 900`) | `Mu` | `Mu` is anchored to the kilogram. **Into Sable: 1 kpg = 1000 Mu** (author, 2026-10-11) -- Sable weighs a normal block at 1.0, and a block of water is 1000 Mu; stone at Sable's 2.0 reads 2000 Mu, near real. One conversion, `Units.kpg`, for every Mu handed to Sable. |
| battery charge | `Eu` | First live use of electricity outside its doc. Battery, starter and plugs are an electrical loop; §10 allows it as long as nothing has to *convert* to `Eu`. |
| part durability | condition % | Same convention as machine condition. |
| oil level, fuel level | `mB` | Fluids. |

**Not mapped, deliberately:** grip coefficients, cornering stiffness, yaw inertia, spring
rates. They are vehicle-internal constants of the driving model, never cross a system seam,
and so per CLAUDE.md's *type the plumbing, not the arithmetic* they stay floats inside the
physics.

---

## 3. Banded knobs

Applies to carburettor mixture, ignition timing, and SummerCraft's whole `CarSetup` screen
(tire width 155–265, offset, camber, toe, ride height, spring rate) — every one a free scalar
today.

The ruling is bands with adjustment inside them. The bands are uncontroversial: *rich /
stoichiometric / lean* and *retarded / stock / advanced* are exactly §3's "short or long
crank throw."

**The inside-the-band adjustment needs one more thing to be legal under §3**, and this is the
argument for it. §3's objection to a scalar is that it *has a correct answer sitting in it*.
A fine adjustment inside a band still has one — unless **the correct answer moves**. So:

> **The in-band optimum must depend on conditions the player does not control.**

Mixture's best point shifts with boost, air temperature (biome), altitude, and engine
temperature. Timing's best point shifts with boost and fuel. Then there is no number to
grind out once and forget: fine-tuning becomes a *control* problem — tune for this car, this
route, this fuel — and a player who leaves it in the middle of the right band is not playing
knowingly suboptimal, they have chosen robustness over peak, which is a §5 trade.

If a given knob's optimum *cannot* be made to move like that (toe, probably), it should be
bands only, no fine adjustment. **[OPEN]** knob by knob; proposed first cut:

| Knob | Bands | Fine adjustment? |
| :--- | :--- | :--- |
| Mixture | rich / stoich / lean | Yes — moves with boost, temperature, altitude |
| Timing | retarded / stock / advanced | Yes — moves with boost, fuel |
| Tire compound | already discrete (`TyreType`) | No |
| Tire width | narrow / standard / wide | No |
| Camber, toe | neutral / sporty / aggressive | No |
| Ride height, spring | soft / stock / stiff | Probably not |

---

## 4. The chassis: block base plus free-form parts

Settled by the author, 2026-10-10; base reshaped 2026-10-11 (§1.3).

**The block base** (2026-10-11). Engine, seat, power sources and control blocks are ordinary
blocks, built on a **Lift** and lifted from it into one Sable body. Rules:

- **Built on a Lift, Scrap Mechanic's way** (author, 2026-10-11) -- built. Use the Lift and
  everything resting on it becomes one body, held still: Sable's `Fixed` joint between the body
  and the world (`null` as the second body -- what Sable does for Create's Sticker). Use again
  to raise it a step, sneak-use to lower; the joint's world end moves and the body follows,
  kept upright. Break the Lift and the vehicle drops into physics. The Lift is never part of the
  vehicle: it is what bounds Sable's floodfill, which otherwise takes every touching block --
  the ground included -- so a build touching anything but the Lift reports *too big*.
  **Rejected:** welding blocks one by one (Aeronautics glues them, via Create's contraption
  rules) -- a click per block; skipping terrain by tag -- a cobblestone floor would come along;
  box select. A lift that is itself part of a vehicle refuses to lift -- its position is in that
  vehicle's plot, and the hold it made crashed Sable's anchor check (found in play). Not yet: a
  visible platform (the raised vehicle hangs over a bare block).
- **The Lift is also how a creation is carried** (author, 2026-10-11) -- built. Use a Lift item
  on *any* creation, lifted or loose, and the whole of it goes into that one item -- every
  block and everything each block holds (chest contents, fuel, a frame's plates and tubes) --
  and it is gone from the world. Use the carrying item on the ground: a lift goes down with the
  creation on it, held, if its box is clear. Kept as a vanilla structure template, so block
  entities round-trip with no per-block code. The carrying item splits off its stack and
  stacks to one, or a stack would copy the creation. Lose the item, lose the creation.
  **[OPEN]** size: the template rides on the item and syncs to clients, fine for a car,
  unproven for a 4096-block creation full of shulker boxes.

- **A pile of blocks is a body.** Under the cell route (*Bridging to Sable*, 2026-10-15) a
  free-form group becomes blocks too, so every group the frame's bearings cut it into -- a
  wishbone, a shaft -- is a body of its own, joined by the bearing. *Was:* a group with no block
  on it stayed a cheap box cluster (§5), which never swung.
- **A trailer is a second block base**, so a second body, joined to the car by free-form parts
  and a pivot -- tow-bar tubes on the trailer, a ball pivot at the car's hitch. That is a joint
  between two bodies, which frame-to-frame joints already are. This is why a box-only group
  could not be the whole answer: a trailer carries blocks.
- **The Vehicle Core and the Chassis Frame merge into one block** -- done by the Lift taking
  the core's one job (assembly): `VehicleCoreBlock` is gone, and the Chassis Frame no longer
  assembles on use. The frame block is the holder of the free-form data and a real block of
  the car, lifted with the rest. This retires "zero frame blocks is the aim" (§5). A frame
  block alone on a Lift is still a vehicle of plates and tubes.
- **Free-form parts attach to the base by the Welder, and only by it** (author, 2026-10-11) --
  built. Connect mode: select a point, use a block of the base; again to unweld. Welded points
  draw blue. Nothing else holds a frame to its vehicle -- not even the frame block it is measured
  from -- so a group with no weld, and no bearing to a group that has one, falls off when the
  vehicle is lifted, Scrap Mechanic's way. *Stiffness, resolved by cells (2026-10-15):* a heavy
  floor held by a single welded point once sat 1-3 blocks off in a third of test-server runs,
  held by a chain world → body → root box → box; a welded group is now cells of the base body,
  rigid by construction, however few points are welded. A weld on a point set as a bearing is that bearing,
  so a wishbone's inner end set to hinge and welded to a block hinges on the block. Mine the
  block and the weld holds nothing. Stored as the block's offset from the frame block, since
  the two are lifted together. **Rejected:** keeping the largest group welded automatically --
  it attached a part by being there. **Not yet:** welding to a block of a *different* vehicle;
  trailers hitch frame-to-frame for now (both need a frame).

**The free-form parts** -- suspension, drivetrain, body panels:

- **Built in the world, from nodes.** The player places **vertices** and connects them.
  Connected vertices form edges; closed loops of edges form **plates**. The shape is
  arbitrary — the body looks like whatever was built.
- **A vertex is one 1/16-block cell** (16³ to a block), drawn and located at the cell's centre —
  never on a grid line. Snapping is by cell (floor), not by nearest crossing. Connections between vertices are
  arbitrary: any vertex may join any other, so plates need not be axis-aligned.
- **Functional parts are blocks in the base**, not mounts on plates. SummerCraft's fixed
  `PartSlot` positions do not survive; what a block *is* to the vehicle (engine, tank, seat) is
  read by tag and property, never item id (§7).
- **Aerodynamics are computed once, at build time**, and stored as numbers the driving model
  reads. The finished shape is never re-simulated while driving. This is the
  anti-simulator rule (§3 of the philosophy) applied correctly: the shape is a real
  engineering choice, so it is modelled — but as a figure, not a live fluid sim.
- When assembled, the block base becomes one Sable body and the plates and tubes become cells:
  in it where welded, bodies of their own where they hang on bearings (*Bridging to Sable*,
  below; §5 for pivots). Mass, centre of mass and inertia come from the parts (`Mu` per part,
  plates by area), not from constants — which retires `CarPhysics.MASS = 900`. 300 kg of engine
  plus 200 kg of steel panels drives like 500: the cells carry the panels' mass, so Sable's own
  total is the vehicle's (the box era had to add the boxes back by hand).

**Aerodynamic drag is `Vdrag`** (V for vehicle) — settled. `Drag` in `core/unit/` already
means `Su/RPM`, a mechanical load, and the two must never be swappable.

**The build-time figure is projected area, angle-weighted** — settled. Each plate contributes
its area times how squarely its normal faces the airflow; summed per direction of travel.
Cheap, computed once, and good enough to make "slab-fronted brick" and "raked wedge" two
genuinely different cars. A voxel raycast was the next candidate and is not needed.

**The connecting tool is the Welder** — settled. Click vertex, click vertex, the same
interaction shape as the data connector's links. It has three modes, cycled by shift-scroll
(`WelderMode`): **point** (place, select, move), **connect** (edge between two points; a tube if a
block is in the off hand) and **plate** (three points lay a plate in the off-hand block). Closing
a triangle no longer plates it on its own — that was one click doing two jobs. Plates render as
slabs `PLATE_THICKNESS` thick, not as flat surfaces.

**Plates can be any material; mass and strength follow the material** — settled. These are
properties the game already implies (an iron block is obviously heavier and tougher than a
wool one) made concrete. Mass is density (`Mu/mB`) × plate area × thickness -- built, read
from the material system; strength comes from the material's existing hardness. Both are read from **tags and properties**, never a
per-item table keyed on id (§5).

**Thickness comes from the Extruder** (author, 2026-10-11), a new item with two modes:
**thicken** (drag a plate to any 1/16 depth -- still one plate, a fatter slab) and **pull**
(drag a plate's face outward into a solid: new vertices, new side plates, Blender's extrude).
This is a free amount, not §3's banded choice, on purpose: §3 is about *settings* that hide a
correct answer, and this is building -- vertex positions are already free on the 1/16 grid, and
depth is one more vertex position. Not built.

**Plates are triangles** — decided in the build. "Closed loops form plates" only pins down a
plate when the loop is three edges: four vertices in a ring need not be flat, and a non-flat
quad has no agreed area. A flat quad is two triangles on a diagonal. No material in the off
hand, no plate — the edges stay a bare frame.

**Plates are paid in plate items, by size** (author, 2026-10-11; built version still takes one
off-hand block per plate). The off hand holds the material's `plate` form -- the one the presses
already make (`form/ingot_to_plate.json`) -- not its block, because this is a tech mod and a
pressed sheet is what a body panel is. Cost scales with area × thickness, so the Extruder's
thicken mode costs more plate; fractions are tracked on the vehicle, not rounded away. Which
item counts is read from its form and material, never its id. **Rate** (author, 2026-10-11):
one plate item is one ingot, and an ingot is 1/9 of a block, so a plate item covers 1/9 block of
*volume* -- 4096/9 = 455.1 cubic sixteenths, rounded down to **455**. At the 1/16 minimum
thickness that is 455/256 ≈ 1.78 block² of panel. **[OPEN]** whether tubes switch to a pressed
form too (`rod`?) on the same reasoning.

**Vdrag is the silhouette, not a sum.** Summing every plate's `area × |cos|` counts a closed
body twice (nose and tail project onto the same patch of sky), and halving it is wrong for a
lone plate. `Frame.vdrag` rasterises the projection at 1/16 resolution, keeps the frontmost
plate per cell and weights the cell by that plate's `|cos|`. Tested: a 1×1 slab is 1.0, the same
plate raked 45° is 0.71, a closed unit box is 1.0.

### Bridging to Sable — cells, again (author, 2026-10-15)

**Ruling (author, 2026-10-15), reversing 2026-10-11's "chassis geometry is not blocks": a frame
assembles into blocks.** The frame stays the authoring tool -- vertices, edges, plates, the Main
Assembly feel of extruding and deforming lines and planes in free space -- and what it *outputs*
on assembly is cells: hidden blocks whose collision shapes trace the plates, lifted into Sable by
`SubLevelAssemblyHelper` like any other block. Rendering stays smooth: the frame draws the true
plate, the cells draw nothing.

**Why it was reversed -- the playtest, 2026-10-15.** The box prototype below was a Sable body
of one block with every plate and tube a free `BoxPhysicsObject` glued on by constraints, and
switched quiet against its neighbours by more constraints. Sable was not built for that shape,
and a long playtest found what that costs: cars that bounce and fling themselves for no visible
reason; tubes colliding with blocks inconsistently; a wooden plate bouncing on flat grass after a
short drop; unwelded parts that stay attached with no collision instead of falling off; a bearing
arm that would not swing; and a crash on every join (a bearing joined to itself; fixed, but the
kind of failure the constraint web breeds). A Scrap Mechanic veteran's verdict was "working axle
and rotation that isn't fucked". Each box is a body Sable steps separately from the vehicle and
holds on by a soft joint; blocks are what Sable's mass tracking, centre of mass, inertia, entity
collision, chunk baking and splitting are all written for.

**What the cell route gets for free** (read from the Sable 2.0.6 jar, 2026-10-15; the deepwiki
the author linked, <https://deepwiki.com/ryanhcode/sable/1-overview>, rate-limited the read twice;
the build read Sable's own sources jar instead, `sable-neoforge-1.21.1-2.0.6-sources.jar`):

- **Mass, centre of mass and inertia** from `MassTracker`, block by block, updated as blocks
  change. The boxes' separate mass, which the driving model had to add back by hand, goes away.
- **Entity collision.** `SubLevelEntityCollision` asks each block for
  `getCollisionShape(level, pos)` -- *per position*, so a cell's block entity can give players a
  shape as fine as it likes. This closes "entities walk through plates" below.
- **Loose parts fall off by themselves.** Sable floods each sub-level for connectivity
  (`SubLevelHeatMapManager`) and splits off what is no longer joined. Unwelded cells that touch
  nothing welded are a separate body the moment they are gathered or cut loose -- the
  Scrap Mechanic behaviour §4 promised and the boxes never delivered. *Corrected on reading
  the source:* the split floods face- and edge-neighbours (not corners) and runs only after a
  block is *removed*, so nothing is split at the moment of assembly. Assembly does not lean on it:
  it lifts each unwelded group into its own body itself. The split still catches a part cut
  loose later.
- **A hook on assembly**: `BlockSubLevelAssemblyListener.afterMove`, for a frame to prune unused
  vertices once lifted (playtest ask).

**The constraint that shapes everything: Sable's physics collider is per `BlockState`.**
`RapierVoxelColliderBakery` memoizes one collider per state, from
`BlockSubLevelCollisionShape.getSubLevelCollisionShape(getter, state)` or the state's own
collision shape, as axis-aligned boxes clamped to the unit cube. `BlockSubLevelDynamicCollider`
exists in the API and **nothing in 2.0.6 calls it**, so per-position physics shapes are not
available -- the other model's "re-bake the box list at runtime" claim does not hold for this
version. So a cell's physics shape must be named by its state, which is exactly the family the
old cells (below) had to enumerate. Entities can be finer than physics (per position, above);
the vehicle against the world cannot.

**What changes from the first cell pass**, which was built 2026-10-10 (`16a20e0`, `c4e9904`)
and removed a day later (`7d19df5`); git holds `ChassisCellBlock`/`ChassisTubeBlock`:

- **One body per bearing-cut group**, joined at the bearing by Sable's `Rotary`/`Generic`
  constraint between two sub-levels -- the frame-to-frame joint that already works. Not boxes on
  constraints inside one body.
- **Cells are placed only on assembly and removed on disassembly** (the Lift lowered, below), so
  the world never holds them while building. Editing an assembled vehicle re-voxelizes the cells
  it changed.
- **States, not shapes, are the budget.** The first pass named a plane per cell (49 directions x
  16 offsets, staircased) with creases as a second family, and tubes as 24,843 states.
  **Decided (2026-10-15): octants.** A cell's state names which of its eight half-block octants
  the frame passes through -- 255 shapes of at most eight boxes -- times 16 mass steps
  (`ChassisCellBlock`, 4,080 states). Players, entities and the outline get the frame traced at
  1/16 per position from the cell's block entity, so the coarse shape is only ever what the
  vehicle meets the world with. *Why:* stability first -- a half-block box is the coarsest
  contact Rapier is asked to resolve, against a 1/16 staircase of dozens of slivers per cell
  whose edges catch on each other; and one family covers plates, tubes, creases and any number
  of plates through one block by OR-ing bits, where the plane family needed a crease family, a
  tube family of 24,843 states, and still fell back to a solid cube where three planes met.
  *Why it was tempting:* the plane family already existed in git and its physics followed the
  plate to 1/16. *The cost, recorded:* physics stands up to half a block proud of the true
  plate (a floor at the bottom of a block collides as a half slab) -- roughly right, not exact.
  Round a hub it would be worse than rough, an axle's octant dragging on the road under a small
  tire, so every octant within a tire's radius of its hub is left empty: the tire is a ray and
  the space is its. **[OPEN]** whether half-block physics is felt in play (a vehicle sitting
  low, gaps that close); if so, the next step is a finer shape at the cells that touch the
  ground, not a return to the plane family.
- **The highlight follows the visible shape** (the author's one hard requirement). A cell's
  outline is per position, from the frame's true plate, never the physics staircase.
- **Plates still mine only by the Welder**: a cell is not something a player breaks.

**The Lift lowered turns a vehicle back into world blocks** (playtest ask). Sable 2.0.6 has no
disassemble call; `SubLevelAssemblyHelper.moveBlocks` with the inverse of the body's pose,
snapped to the grid, is the candidate, and cells are removed rather than moved.

**Kept from the box era:** the frame data and editor, `Vdrag`, plate mass from material density,
welding as the only attachment, and the drivetrain's tire raycasts (which never needed boxes).

**As built (2026-10-15).** `FrameBodies` and its boxes and quiet constraints are gone.
- **Assembly** (`FrameAssembly.cut`, called by the Lift for each frame it gathers): the frame is
  cut at its bearings into groups (`FrameAssembly.parts`; the whole frame is one group with no
  bearing). Each group is traced into cells -- samples every 1/32 across each plate and along
  each tube mark 1/16 voxels and the octants they fall in, and carry the plate's or tube's mass
  -- and the cells are placed where the world is air. A group with a weld on one of its own
  points (not a bearing) is rigid with the base: its cells go up in the Lift's block list. Every
  other group is lifted at once by `SubLevelAssemblyHelper.assembleBlocks`, a body of its own;
  the frame records the body and where the frame's origin falls in its plot
  (`FrameBlockEntity.GroupBody`), saved.
- **Bearings** are Sable joints between group bodies (`FrameBlockEntity.pivots`): `Rotary` for a
  hinge, `Generic` with the linear axes locked for a ball -- the joints frame-to-frame welds had
  already found work. **A locked bearing does not cut**: its two sides are one group, one body.
  **Nor does a hinge that carries rotation** (`Frame.Pivot.cuts`, playtest 2026-10-16): its spin
  is the shaft's, computed and never simulated, so as a body it is rigid. Cut there, Sable let the
  driven side spin loose about the axle, the road's push turned the tube and tire instead of the
  car, and no tube ever drove in ten hours of play -- the driving test passed only because its
  joints were locked. A **ball that carries rotation** still cuts (it swings) but its twist about
  its axis is locked: a universal joint. A weld on a bearing that does not cut holds like any weld.
  **The two sides of a bearing collide** (2026-10-16): each side's cells are cleared within half a
  block of it (`FrameAssembly.BEARING_CLEAR`) and the joint's contacts are on, so an arm swung
  into the chassis stops instead of passing through. Before, contacts were off between joined
  bodies outright, because their cells met at the bearing.
  *Found:* the driving test's shaft bends down to its hub through two locked joints, and as
  three small bodies on `Fixed` joints the chain jammed into the road and pinned the car on its
  blocks; one body is steadier than two held rigid by a joint. Switching it to a hinge takes
  effect at the next lift, like any change to the cut. Each plot is the frame as built, so a joint is
  made at rest with the pivot at the same frame point in both bodies and a hinge's axis the same
  plot axis, whenever the two bodies' turn has since gone; contacts between the pair are off. A
  weld on a bearing is that bearing between the base and the group. Two groups both welded are
  one body and need no joint. Rebuilt on any change to pivots, welds or bodies, and after a
  reload (Sable does not save joints).
- **Mass** reaches Sable through the cell's state: a `load` property in 16 steps of root two from
  0.005 kpg (5 kg) to about 0.9 kpg (`physics_block_properties/chassis_cell.json`), the nearest
  step by ratio, so each cell is within about 19% and a plate's total closer. Chosen over writing
  into `MassTracker`: the tracker is Sable's own sum over blocks, and a cell that carries its mass
  in its state moves, splits and re-cuts with it, no bookkeeping beside. The
  driving model's hand-added box mass is gone: a hub's share of the car is Sable's own figure.
- **Entity collision and the outline** are per position: the cell's block entity holds its
  4,096-bit voxel set and answers `getShape`/`getCollisionShape` with it merged into boxes
  (`dynamicShape`, so no state-cached shape stands in). The highlight is the frame traced at
  1/16 -- the plate's own voxels, never the octants. *Not* the smooth triangle: drawing the true
  plate edges in place of the vanilla outline is a client hook not built.
- **Drawing**: a group in a body of its own is drawn through that body's pose (`FrameRenderer`),
  so a swinging arm is seen swinging. The editor's look ray still meets the frame as built.
- **Editing an assembled vehicle** re-cuts cells in the body each group already has
  (`FrameAssembly.recut`). A group an edit newly cuts off (a new bearing) stays rigid until the
  vehicle is lowered and lifted again; a group an edit erases takes its body with it.
- **Drivetrain**: a hub not welded to a block pushes the body of its own group; the rays look
  past every body of their frame (a hub's own axle cells are not road).
- **Lowered** (`LiftBlockEntity.lower`, sneak-use at height 0): every non-cell block moves back
  to the world by `moveBlocks`, translated to where it now stands rounded to the grid (the lift
  holds the body upright, so no turn); group bodies and cells are destroyed; refused, moving
  nothing, if a block would land on another.
- **Game-tested:** cells and mass on assembly, re-cutting on edit, a welded floor staying and an
  unwelded one falling as its own body, a tube on a ball bearing and on a hinge swinging down from
  a held body, and lowering back to blocks. **Not yet seen in game.** **Not yet:** the Lift item
  carrying a creation takes its base body only, not its group bodies; frame-to-frame joints still
  join the frames' own bodies, not the groups that hold the joined points.

**Editor, alongside** (playtest asks, not blocked on the above): a selected vertex moves voxel
by voxel through space (gizmo arrows or arrow keys) instead of only by re-aiming; and a corner
overlay where the ammo HUD sits lists what each click does in the current Welder or Pivot Setter
mode. Placement landing inside or outside a block inconsistently stops mattering once a vertex
can be nudged.

### Bridging to Sable — prototype: one free box per plate (superseded 2026-10-15)

Kept for the record: what it was, what it got right, and the costs that ended it (above).

**Ruling (author, 2026-10-11, reversed 2026-10-15): chassis geometry is not blocks.** A plate is part of the Sable
object, not a block placed in the world. Sable offers one thing below blocks: `ArbitraryPhysicsObject`,
of which `BoxPhysicsObject` (an oriented box with a pose, half-extents and mass) is the only
shape. There is no mesh, convex or compound shape, and `VoxelColliderData.addBox` is axis-aligned
within a block -- which is why the plate cells below were 1/16 staircases.

**Built:** the frame block is the whole Sable body (`FrameAssembly` lifts that one block and
nothing else). `FrameBodies` makes one `BoxPhysicsObject` per plate and per tube; each group's
boxes are welded to a root box, and a group meets the body only at a welded point (above),
contacts off. A plate is its triangle's bounding rectangle in its
own plane, one `PLATE_THICKNESS` deep, along its longest edge; a tube is a box along its edge
at its diameter. Mass is the material's `density` × volume (a block is 1000 mB), through
`Units.kpg`, carried by the box -- the block the plate names is looked up with `Material.find`.
A block no material claims, or one with no density, falls back to Sable's per-block mass
(`sable:mass`) × volume, which was the only source before materials were weighed.

Consequences, each found by building it:

- **Boxes are not saved.** Sable drops them when their chunk unloads, so they are derived from
  the `Frame` and rebuilt by the frame's tick, like a joint's constraint handle.
- **Boxes overlap, and Rapier has no collision groups.** Every pair of touching boxes gets a
  `Free` constraint -- it holds nothing, which is exactly the point -- with contacts off. Two joined
  frames' boxes are quietened against each other the same way (`quietenAcross`). Without it the
  game tests drift by a block or more.
- **The bounding rectangle over-covers a triangle** by up to its own area at the corner away from
  the long edge, and two triangles of a quad each cover the whole quad. Harmless inside one
  vehicle (they ignore each other); a ghost corner against the world. **Left as is**
  (author, 2026-10-11): split into smaller boxes only if it is felt in play.

**Not solved -- entities walk through plates.** Sable's entity collision runs against sub-level
*block* voxels (`SubLevelEntityCollision`); nothing in it reads `ArbitraryPhysicsObject`s (the
only users of `BoxPhysicsObject` in the jar are the physics system and its ticket manager).
Read from the code, not tested in game. A player standing on a vehicle floor would fall through
it. The way forward is our own entity-collision hook: carry the entity's AABB into the body's
local space and push it out of each oriented box (SAT, minimum translation), the way Sable does
for voxels.

**Also not carried over from the cells:** mining a plate (it is gone only by the Welder now), and
the sub-level's own mass data -- Sable sums blocks, so the body reports the frame block's mass
and the plates' mass lives on the boxes. Anything that applies force to the body for driving
(§8 step 6) has to account for it.

**Editing is live, assembled or not**: the Welder carries its look ray into an assembled body's
local space, so moving, splitting, removing, re-plating and welding all work on a vehicle exactly
as on a frame in the world. An edit marks the boxes stale and the next tick brings them in line.

**Superseded at the time -- plate cells at 1/16** (and brought back, changed, 2026-10-15; see the
section above). The previous answer put a hidden `ChassisCellBlock` in
every block a plate crossed: 49 plane directions × 16 offsets, creases as a second state family,
a `load` state carrying mass through `physics_block_properties`, and `ChassisTubeBlock`'s 24,843
tube states for tubes. It gave collision Sable and entities both understood, at the price of a
placed block per cell, plates that mined and broke as blocks, and plates quantised to 1/16. It
was removed for the boxes above. Git holds it (`FrameAssembly.cells`, `ChassisCellBlock`).

**Prior art:** none directly. The nearest is Chisels & Bits, but it is voxel-based (1/16
cubes), not vertex/plate-based, so it informs the 1/16 grid and placement feel at most.
Feedback's own in-world node system (`control/data/`, `DataNode`, the connector overlay) is
the closer precedent and will be reused where it fits.

---

## 5. Mounting and suspension are built, not picked

Settled by the author, 2026-10-11:

- ~~Parts mount on plates at free 1/16 points~~ -- superseded 2026-10-11: functional parts are
  blocks in the base (§4). What hangs off the base is free-form running gear and body.
- ~~An overloaded plate wears instead of refusing the load~~ -- dropped (author, 2026-10-11):
  nothing wears a plate for what sits on it. Load, breakage and damage belong to a later
  **deformation pass** -- crashes, crumple zones, joints letting go -- designed as one system,
  not piecemeal here.
- **Dedicated parts, not edges, make the running gear:** axle, tubing, bearing (pivot),
  spring, shock. Players build suspension geometry themselves — wishbones, trailing arms,
  solid axles — out of tubing and pivots, sprung and damped by parts they chose.

**This needs joints between physics objects, and Sable has them** (corrects an earlier note here that it
offered none — that read missed `dev.ryanhcode.sable.api.physics.constraint`):
`PhysicsPipeline.addConstraint(bodyA, bodyB, config)` with `Fixed`, `Free`, `Rotary` (hinge,
`setServoCoefficients`) and `Generic` (six axes, per-axis lock and `setLimit`). Every handle has
`setMotor(axis, target, velocity, stiffness, damping, …)` and `getJointImpulses`. So:

| Part | Sable joint |
| --- | --- |
| bearing / pivot | `Rotary` between two bodies |
| axle + wheel | no collider -- a ray tire on the hub (below); the hub turns on a `Rotary` pivot |
| spring + shock | `Generic`, one linear axis free with `setLimit` travel; the push is our own per-tick force, not `setMotor` (below) |
| welded joint | `Fixed` — **not** `Generic` with every axis locked: tried first, it held bodies at whatever offset they started at and never closed a gap |

**Built (2026-10-11):** frame-to-frame joints. Frames carry a UUID (assembly moves a frame's
block entity into a body's plot, so position cannot name it); `FrameJoint` stores the vertex pair,
kind and bearing axis; the frame's ticker makes the Sable constraint once both frames are bodies,
with contacts between the two switched off. Welder: select a vertex, use a vertex on *another*
frame -- a weld; use again to cut it. Game-tested weld and bearing. **Testing caveat:** the
game-test server runs tests ~10⁷ blocks out, where Sable poses come back rounded to a sizeable
fraction of a block; joint precision must be judged near the origin, in game -- and the weld and
ball tests that read a gap there flicker between runs with no code changed.

**Bearings are set, not carried** (author, 2026-10-11, superseding the off-hand bearing item).
The **Pivot Setter** is its own tool: look at a point, use, and the point cycles plain, hinge,
ball, locked, plain; sneak-use toggles whether it carries rotation across or spins free. A hinge
takes its axis from the one tube on the point, else the axis nearest the look. **A pivot is a
setting until the chassis is a physics object.** On a frame not yet a body it is only recorded
(`Frame.pivots`); once assembled, the frame is cut at each pivot: everything reachable from a
side without crossing a pivot is a group, and each group not welded to the base by a point of
its own is cells in a body of its own, held to its neighbours by the bearings (§4, *cells,
again*). **One chassis is one frame block, however many bearings it has** (author, 2026-10-11)
-- still true: an earlier pass gave each group its own *frame block* and body, which was wrong
and is gone; the box era then gave groups no body at all, which did not swing; under cells a
group has a body and no block of its own. Ball is Sable's `Generic` joint with linear axes
locked, so a ball bearing with nothing holding it goes slack and swings down under gravity;
hinge is `Rotary`; locked is rigid, unlike a weld because it can be switched back -- and since
cells it does not cut the frame at all (*As built*). Each joint is made at rest, the pivot at the same frame point in both bodies' plots. Switching a pivot's
kind on an assembled vehicle swaps its joint at once; adding or removing a bearing changes the
cut, which takes effect when the vehicle is next lowered and lifted (§4, *As built*). Sable does
not save joints, so a reload re-makes them. On a joint that already exists (two frames, welded or
bearing) the same tool cycles the joint, so a weld between frames can be made a bearing and back.
A point whose edges all sit on one side of the cut has nothing to pivot against and does
nothing. The frame block is where the frame's data lives and ticks; with the block base (§4) it
is lifted with the other blocks of the car and stops being a spare block.

This is **not** the shaft Bearing block. That one is a support in the mechanical network; a
vehicle bearing is a joint between rigid bodies, and the two had been sharing an item and a tag
(`feedback:bearings`) on the strength of the word. Divorced: the tag is gone, the Welder no
longer reads the off hand for a bearing, and the pivot has no item to spend. **Rejected:** a
consumable bearing in the off hand -- it made a *setting* cost an item, and the hand it
occupied was the one that holds the plate material. Shape taken from Scrap Mechanic's bearing
(closed source -- design only, nothing read); its hinge-only bearing is widened here by the
ball and locked settings.

**Drive is recorded but does nothing yet**: it needs the drivetrain (§8 step 6), where a driving
bearing becomes a shaft link and a free one does not.

**Tubing and drive shafts are one part** (author, 2026-10-11) — built. A tube is an *edge given a
material*, the way a plate is a triangle given one: Welder on an edge with a block in the off hand.
It splits with its edge, goes when the edge is cut, carries mass (length × a 2/16-square section ×
the material's density) as its box. A **drive shaft is a frame made of tubes on
driving bearings**: a hinge's axis is now any direction, and a bearing on a vertex with exactly one
tube takes the tube's direction, so a shaft turns about itself at any angle. The shaft's rotation
reaching anything waits on the drivetrain, like the drive flag.

**Tube sizes** (author, 2026-10-11) — built. Sizes are a banded choice: thin, medium, thick (2, 4, 6
sixteenths across); the Welder with the tube's own material again steps it up, thick wrapping to
thin. A tube's box is its diameter square, along its edge. (Its collision was once a 1/16-voxel
outline per block state; see *Superseded* above.)

Not yet: springs/shocks. Ray tires are built (step 4, below). Breakage waits for the deformation pass.

`getJointImpulses` is the stress readout the deformation pass will read, not a guess. A raycast-suspension shortcut was considered and rejected: it fakes exactly the
geometry the author wants players to build.

**Wheels are ray tires on real arms** (author, 2026-10-11). Sable's only free shape is a box,
and a box does not roll. So only the rubber is faked: the hub casts short rays at the road and
pushes *the hub* when they hit -- and the hub hangs on the arms, pivots and springs the player
built, so a bad wishbone still drives badly. This is not the rejected raycast suspension: the
ray stands in for the tire, never for the arms. Prior art: Offroad (in `../Simulated-reference`,
code MIT) -- its Wheel Mount casts three rays down (front, middle, back of the tire), takes grip
from the struck block's Sable friction, drives from shaft speed and brakes from redstone, all as
impulses through `ForceTotal`. **Taken:** the three-ray patch and friction from the ground
block's physics property. **Not taken:** its spring, a scroll-wheel strength inside the mount --
that is exactly the number our built suspension replaces. Not built.

**Moving pieces inside one chassis are pivot groups, not frames.** An earlier ruling had each
moving piece be its own Chassis Frame with its own body; it is superseded by the pivot cut above
(one frame block per chassis). **Frames still weld to each other** (Scrap Mechanic-style): the
Welder joins two separate frames at a point on the 1/16 grid with a `Fixed` joint. Proposed for
the deformation pass: a joint overloaded past its material lets go and the piece falls off
whole — re-weld it, never rebuild it. **Spring and shock strength: material sets the range, a band picks inside it** (author,
2026-10-11) -- steel and thick is a stiff range, and soft / stock / stiff chooses within it,
the same shape as tubes (material + size) with §3's band on top.

**Springs push by our own hand** (author, 2026-10-11): every physics tick, measure the
compression and push the two ends apart through `ForceTotal`, with damping on the closing speed --
Simulated's and Offroad's shape. Chosen over a `Generic` motor because the force is then ours to
read and bend: sag, wear and soft/stock/stiff bands are all edits to one formula, where a motor
is a black box. The table's spring row is superseded.

**Prior art read (2026-10-11): Create Aeronautics' Simulated Project**
(`Creators-of-Aeronautics/Simulated-Project`, `main` only, code MIT / assets All Rights Reserved,
cloned to `../Simulated-reference`). Everything in it is *blocks* in a sub-level -- it never uses
`ArbitraryPhysicsObject` or `BlockSubLevelDynamicCollider`, and its one collision mixin is a Create
contraption tweak -- so it has nothing for non-block plate collision. Of use later: springs are
force-per-physics-tick through `ForceTotal`/`applyForcesAndReset` with hand-written damping (not
`Generic` motors), the swivel bearing is `Rotary`, and `FreeConstraintConfiguration` is used as a
pull-to-goal anchor for handles and ropes. Nothing adapted yet.

### Drivetrain: the free-form side is one member of the block network

**Ruling (author, 2026-10-11):** a tube welded onto a shaft block turns with it -- it joins the
network. The free-form drivetrain does its own sums on the Sable side and the block network is
told only the totals.

That fits the network as built, with nothing new on the block side: every `RotationNetwork`
member already reports a static load (`Su`), a `Drag` (`Su/RPM`) and an `Inertia`
(`Su·t/RPM`), each referred through the gear ratio. So the whole free-form drivetrain -- tube
shafts, driving bearings, hubs, tires -- is **one member** at the welded shaft, reporting:

- **load** -- what the tires are pushing against right now: rolling resistance, the hill, the
  acceleration being asked for, as force × tire radius;
- **drag** -- what grows with speed (air, via `Vdrag`; bearing friction);
- **inertia** -- the car's mass seen through tire radius and ratio, so a heavy car spins up
  slowly for the same engine.

Speed flows the other way: the network sets the shaft's RPM, the free-form side carries it
through its ratios to each hub, and a ray tire turns hub speed into traction (Offroad's shape,
§5 wheels). **Consequences that fall out, not designed:** a load past the engine's `Su` at its
RPM is an overstressed network, and an overstressed network reads as zero speed -- *that is a
stall*, on a hill in too tall a gear. Gearing down is the cure, which is §17's force-for-speed
trade arriving in a car unasked.

**Speed authority** when the tires do not agree with the shaft -- settled (author, 2026-10-11). The network already has
inertia and coasting (`RotationNetwork`: a target speed, a current speed that chases it, a
network with no drive winding down under load) but it assumes only sources push. A car pushes
back -- rolling in neutral, engine braking downhill. So each tire is a two-way slip
coupling. Its force is grip-limited and proportional to *hub surface speed − ground speed*,
applied to the car through Sable and, equal and opposite, to the network as a signed load.

- Wheelspin: hub faster than ground, force capped by grip, the engine sees a light load and revs.
- Locked brake: brake is a load that stops the hub; the tire skids at full grip.
- Airborne: no contact, no force, the shaft spins free.
- Neutral downhill: the car drives the wheel side, a negative load, so it keeps turning.

What it needs from the block side is a load allowed to go negative -- the one change to the
network this design asks for.

#### Implementation plan

Written 2026-10-11 against `core/rotation/` as it stood, and decided by the author the same day
(the rulings are marked where they fall). **Steps 0 to 2 are built** (5, *Build order*); the rest is
plan, and *As built* notes mark where building changed it.

**1. The member is a channel, not a node.** `RotationNetwork` members are `RotationNode`s --
block entities found by `RotationPropagator`'s flood fill through `Rotatable.hasShaftTowards`.
The free-form drivetrain was boxes, not blocks (and its cells now carry no shaft), so it cannot be a member that way. It does not
need to be: the network already has one outside contributor that is not a member, the **belt**.
A pulley writes `driveByBelt`/`loadByBelt` into a network every block-entity tick, the network
uses the figures in its own tick (`LevelTickEvent.Post`, after every block entity) and zeroes
them, so a belt that stops writing stops counting -- no rebuild, no `recalculate`, no stale
ledger. The drivetrain uses the same shape:

- **The coupling node** is the `RotationNode` that a driving tube is welded to: `Frame.anchors`
  names the block (as an offset from the frame block), and it couples only if that block
  `hasShaftTowards` the welded tube's direction along the tube's own axis. Its network and its
  `getRatio()` are what the free-form side reads. **Any shaft block** (author, 2026-10-11) -- no
  dedicated flange block. *As built:* "along the tube's axis" means the tube leaves the welded
  point square to the grid, and the block needs a shaft on the face it leaves by; a tube at an
  angle couples nothing. Only an assembled frame couples -- a frame standing in the world turns
  nothing. If several welds couple, the first found reports and the rest do not (**[OPEN]**: two
  shafts welded to one drivetrain -- which run is it a member of?).
- **The sums live on the frame.** A new `vehicle/drive/Drivetrain`, owned by
  `FrameBlockEntity`, derived from the `Frame` and rebuilt when an edit marks it stale (the cells'
  rule): the chain from the coupled tube through *driving* pivots to each hub, each hub's ratio
  and sign, its tire radius. Per tick it reports one figure set to the coupling node's network:
  `network.driveExternally(Su push, Drag drag, Inertia inertia)` (built, step 1) -- three
  per-tick fields beside the belt's, summed if several callers write, cleared after the
  network's tick. The figures are in the run's own frame: the *caller* refers them through the
  coupling node's ratio the way `recalculate` refers members (push by `1/ratio`, drag and
  inertia by `ratio²`).
- **Load** (`Su`, signed) is the tires' reaction, `Σ F_long × r` over the hubs, each through its
  hub ratio -- see 2. **Drag** (`Su/RPM`) is bearing friction per driving pivot, the shaft's own
  `SHAFT_DRAG_SU_PER_RPM` for now; air drag (`Vdrag`) acts on the *car*, so it reaches the shaft
  through the tires, not as drag. **Inertia** (`Su·t/RPM`) is the free-form parts' own spin --
  tubes, hubs -- **not** the car's mass: the car's mass already resists through the tire force,
  and adding it as inertia too would count it twice. (This corrects the bullet above that put
  the car's mass in inertia: it is right only while the tires grip perfectly, and wrong in
  wheelspin and in the air, where a heavy car must not make the shaft sluggish.)

**2. The signed load: what `RotationNetwork` must change.** Today every load opposes motion:
`liveLoadSu()` is `static + drag × |rpm|`, and slowing is `max(liveLoad, MINIMUM_BRAKING_SU)`
toward the target. A tire is not that. Its force is `grip-capped k × (hub surface speed − ground
speed)` -- it opposes the hub when the hub is faster than the ground and *drives* it when slower.
So the external load must be signed **in the network's own frame** (positive pushes toward
+RPM), not relative to the direction of motion: at rest there is no direction of motion, and a
car rolling downhill has to be able to start a stopped shaft. Changes, all in `tickSpeed`:

- Split the external torque `τ` each tick into **resisting** and **assisting**. *As built:*
  the whole push is one or the other, judged against the **drive's** direction while a source
  or belt drives the run, and against the current spin only while nothing does (at rest with
  neither, it assists). The draft judged against the spin always; that made a car rolling back
  down the hill it stalled on an *assist* to its backwards motion, and the run spun backwards
  against a running engine -- exactly the reversal case below. Judged against the drive it is
  load, the network overstresses, and the run stalls at zero. Decided once per tick, before the
  chase, so a push that brakes a run through zero is not also applied as an assist that tick.
- **Resisting joins the static load** -- in `isOverstressed`, the spin-up surplus and the
  terminal-speed clamp. That is what makes the ruling's stall real: a hill in too tall a gear is
  load past the engine's curve at its RPM, an overstressed network, zero target, the run winds
  down.
- **Assisting is a separate acceleration**, `τ / inertia × INERTIA_RESPONSE`, applied every tick
  *after* the chase-the-target step, and not bounded by the target: a car rolling downhill
  overspeeds the engine, and the existing slowing branch (`liveLoad` pulling back toward target)
  becomes engine braking without being written.
- **Three early-outs must stop eating it:** `if (currentRpm == effectiveTarget) return;`, the
  `STOPPED_RPM_THRESHOLD` snap to zero (a slow downhill creep would be zeroed every tick), and
  the `force` floor `MINIMUM_BRAKING_SU` (it would fight a gentle push). Each applies only when
  the external torque is zero.
- *As built,* external drag joins the friction figure (`liveLoad`, the terminal clamp) and
  external inertia the run's own for the tick.
- *Consequence worth knowing:* an overstressed run's target is zero, but an assist still turns
  it, against the braking of its stalled machines' load. Overstress stays true -- the machines
  stay stalled in the ledger -- so "overstress reads as zero speed everywhere" now holds only
  while nothing pushes. Left so; a car rolling downhill does turn the shaft it is welded to.
- **Assist does not offset other machines' overstress** (author, 2026-10-11) -- a car coasting
  downhill does not help a hammer on the same run. Physically it would; no (overstress compares
  capacity with machines + resisting only), because a negative term in the overstress test is a
  new way for the ledger to lie while a car bounces over a kerb.

**What could break.** *Overstress* -- the reversal case: a car stalled on a hill rolls back, the
tires push the run backwards while the engine's target is forwards; `reversing` charges that as
braking, and it must still end in the stall, not in a run spinning backwards against a running
engine. *Propagation* -- nothing, deliberately: the external channel is not a source, so it can
never trip `assignRatios`' two-source jam and destroy a block. *Coasting* -- the three
early-outs above. *Stability* -- the coupling is stiff (`k` large, shaft inertia small), and a
slip force sampled once a game tick against a network stepped once a game tick can ring; clamp
the per-tick tire torque to what would bring hub speed to ground speed in one step (Offroad's
impulses are clamped the same way). *Readouts* -- Jade and the controller's Su sensor will show
a negative load; check both print it sanely. *Speed scale* -- an engine runs at thousands of RPM
where the block network runs at tens (§6); the plan assumes hub speeds come out of the
engine's numbers.

**How to test it**, with no Sable needed: a plain shaft run with the external channel written
from `helper.onEachTick` -- (a) a negative push spins a stopped run up in the push's direction
and it coasts down when the push stops; (b) engine + a resisting load past its curve stalls to
zero; (c) engine at target + an assisting push runs above target and falls back when it stops;
(d) zero external torque reproduces today's spin-up and coast tick for tick -- step 0 below.

**3. Speed to a hub.** Down the chain the network's speed is authority. Hub RPM = network RPM ×
coupling ratio × the chain's ratio. Tubes joined at a *driving* pivot share their speed (ratio
±1, the sign from the tube's direction against the coupling axis -- a hinge on a bent shaft is a
universal joint, and turns at the same speed); a non-driving pivot ends the chain, a free hub
spins on nothing. **Ratios on the free-form side are real parts** (author, 2026-10-11) -- a
final drive or a differential as free-form parts, not only the base gearbox. The proposal here
was the gearbox only; overruled. Ratio 1 everywhere free-form until those parts exist.

**4. The ray tire.** **A Tire setting makes a vertex a hub** (author, 2026-10-11) -- set on a
driving or free hinge point (the Pivot Setter's shape, a setting not an item), radius and compound from the tire's material and a size band. Each physics tick, for each
hub: three rays from the hub centre along the hub group's down, perpendicular to its axle (front,
middle, back of the patch -- Offroad's); on a hit, normal load `N` from the ray's compression
against the spring the player built (the hub's spring force, which we compute ourselves, §5
*Springs push by our own hand*), grip `μ` from the struck block's Sable friction property;
longitudinal slip `s = ω_hub·r − v_ground` along the tire's forward; `F = clamp(k·s, ±μN)`
applied at the contact point to the body of the hub's group (the base's, for a hub on a welded
group) through `ForceTotal`/`applyForcesAndReset`; lateral grip `−k_lat·v_lateral` in the same
grip circle. The equal and opposite `−F·r` is that hub's share of the signed load in 1. Airborne:
no hit, no force, no load -- the shaft spins free to its target. The hub's `Rotary` joint is
**not** motored: the tire's spin is computed, not simulated, and drawn -- no motor for looks
(author, 2026-10-11). Physics ticks may substep, so forces are summed per physics step and the
network gets the game-tick average.

**Corrected 2026-10-16 (playtest).** `N` pushes along the *struck face's* normal, not along the
hub group's up. The first build pushed along the group's up, which reads as "the spring pushes
along its own axis" and is the trap: a car tilted on its springs then took a level-ground
component of `N` forever, and a free wheel has nothing to resist it, so a car with the engine off
crept one way, came back to that way after a push, and passed the old drive test (which asked
only "moved"). The spring sets how hard; the arms carry it; the road pushes along its own normal.
A face met edge-on bears by the cosine. Free wheels also had no **rolling resistance** at all, so
a pushed car coasted on regardless of tire. Rolling resistance (a fraction of load, against the
rolling speed) is now deformation, from figures already there (author, 2026-10-16): the tire's
and the ground's material hardness each give `ROLLING_SOFT / (1 + hardness / ROLLING_HARDNESS)`,
ground that falls in vanilla (sand, gravel) is loose and adds `ROLLING_LOOSE`, and ground that
slows a walker (soul sand, honey: vanilla's speed factor) is ground a wheel sinks in and adds
`ROLLING_SINK x (1 / speed - 1)`. Soul sand is meant to be really bad -- real quicksand, not the
film kind: nothing drowns, a weak engine does not climb out (author). A side whose material's
sliding `friction` is under `ROLLING_SLICK` loses rolling in proportion: blue ice is the
natural world's best coaster as tire or road, as slime is its best gripper (author) -- and,
gripping nothing, the worst to drive on. So slime, the grippiest
tire (canon: the sticky piston), also loses the most rolling, and iron coasts: grip against
coasting, placed by nobody. A dedicated `rolling_resistance` field was the alternative, kept as
the fallback if a material reads wrong.

**Spin is carried as a direction, not assumed to follow the walk** (`Drivetrain.chain`,
2026-10-16). Each tube on a drive line knows which way it turns. Straight on through a point: the
same spin. A bend at a joint that carries rotation is a universal joint: the spin keeps its sense
relative to the way the line goes. But two tubes leaving a driving joint in one straight line are
one axle driven from the side -- a solid rear axle off its ring gear -- and turn as one rod. The
first build walked every branch as a universal joint, so an axle driven at its middle spun its
halves mirror-image and the wheels fought; a fan of two angled tubes from one point still does,
because two real U-joints would. A hub's sense is the carried spin against its axle. A true
differential (halves at different speeds) waits for steering.

**Guard rays** (2026-10-16, author's choice of a guard over a rolling tire body): twelve rays
round each tire's rim, in its plane, outside the ground rays' patch, push the tire out of
anything inside its radius with the ground contact's spring and damper and no grip; the struck
body, if any but the hub's own, takes the push back. Walls and steps stop a wheel, and it no
longer passes through the world at speed. A box collider inside the tire was the plan and cannot
be built: Sable's physics is half-block octants baked per block state, and the largest tire is
half a block across its radius, so a guard smaller than the tire is no octant at all. A tire as
its own rolling body of 1/16 boxes on a motored hinge stays a possible later experiment.

**5. Build order, each step testable alone.**

0. **Baseline rotation tests** -- spin-up, coast-down, overstress, belt drive -- recording
   today's behaviour. No code change. *Risk:* none; it is the net for step 1. **Built
   (2026-10-11):** `gametest/RotationGameTests` -- a hand crank spins a run up at exactly
   surplus ÷ inertia a tick to its 32 RPM; a load past capacity never turns and a turning run
   it overstresses winds down rather than snapping; a cut run coasts at exactly
   max(load, `MINIMUM_BRAKING_SU`) ÷ inertia a tick and a loaded one stops first; a belt
   drives a sourceless run; cogs refer speed by their teeth. The per-tick rates are pinned
   exactly, so they are (d) as well. *Found:* a rotation run left standing at shutdown can
   hang the server in `stopServer`'s chunk-unload loop (about half of runs, never once the
   rigs are cleared). Probable cause, not proven: a chunk unload removes a node,
   `recalculate`'s `dropStale` calls `getBlockEntity` on members in chunks already unloaded,
   and that loads them again. Not fixed; the tests clear their rigs instead.
1. **Signed external channel** in `RotationNetwork` (2), tests (a)-(d). *Risk:* the early-outs
   and reversal; this is the one step that touches every factory in the game. **Built
   (2026-10-11):** `RotationNetwork.driveExternally(Su push, Drag drag, Inertia inertia)`;
   nothing calls it in play yet. Tests in `RotationGameTests` drive it directly: a push turns a
   sourceless run either way and it coasts when let go (a); a push against a crank eats its
   spin-up surplus and lowers its friction ceiling exactly, and past capacity overstresses it
   and stalls a turning run at zero, never backwards (b); a push with it runs it past the
   crank's speed and it falls back after (c); a push never rescues a drill press's overstress.
   Step 0's exact rates still hold with no push (d).
2. **Coupling:** a frame welded to a shaft on a lifted vehicle writes a fixed placeholder load;
   the engine on that shaft slows. *Risk:* tick order -- a sub-level's block entities must tick
   before `LevelTickEvent.Post` or the channel is zeroed before it is read; and a `RotationNode`
   inside a Sable plot is not yet game-tested at all. **Built (2026-10-12):**
   `vehicle/drive/Drivetrain`, owned by `FrameBlockEntity`, rebuilt from the frame when an edit
   marks it stale and ticked from the frame's block entity tick. The placeholder is
   `Drivetrain.PLACEHOLDER_LOAD`, 10 Su *against the coupling node's turning* (signed from its
   speed, so it is load whichever way the run turns and nothing at rest), referred by 1/ratio;
   drag and inertia report nothing until step 6. Test `VehicleGameTests.weldedTubeLoadsItsShaft`:
   a crank, shaft and frame lifted into one body; the crank spins the run to its 32 RPM *in the
   plot* (both risks' first half: a plot's nodes join a network and tick), then the tube's end is
   welded to the shaft and the run settles exactly at the friction ceiling the load leaves,
   (12 − 10) ÷ drag. *Found:* tick order did not bite, and with this channel's shape it cannot:
   the figures are cleared *after* the network uses them, so a write lands in the next network
   tick whichever side of `LevelTickEvent.Post` it comes from -- it is one write per tick that
   matters, not its order. (The plot's block entities do tick in the level's own block-entity
   pass, before `Post`, as far as anything observed says.) *Found:* 8 Su, the first placeholder,
   did nothing visible -- one shaft's drag is 0.1 Su/RPM, so a crank with 4 Su to spare still
   reaches 32 RPM; a resisting load only slows a run below its target through the friction
   ceiling or by overstressing it.
3. **Drivetrain graph** (3): hub speeds from network speed; a free pivot ends the chain.
   *Risk:* low -- pure graph walk over `Frame`. **Built (2026-10-14):** `Drivetrain.chain` walks
   out from each coupling weld along tubes. A *driving* bearing of any kind passes the turn on at
   any angle (a universal joint turns at the speed it is given); a plain point passes it only
   into a tube in line, since a bent rigid shaft is a crank, not a shaft; a hub ends the walk. A
   hub reached through a driving hinge is driven, with sense `sign(tube · axle)`; one whose axle
   sits within ~72° of square to the arriving tube (|cos| ≤ 0.3) is not turned by it. Every
   other hub rolls free. *As built,* a **locked** bearing set to drive is how a shaft bends down
   to a hub below the base: rigid geometry, the turn carried through, a CV joint in a casing.
4. **Tire part + ray tire** (4), force only, no reaction: engine on, a dropped car moves.
   *Risk:* the test server's position rounding ~10⁷ blocks out (§5) -- assert "moved forward by
   more than a block", never a distance. **Built (2026-10-14):** `Frame.Tire` -- the Pivot Setter
   used on a hinge with a block in the off hand makes it a hub of that material; the same material
   again steps the size small / medium / large (`TIRE_SIZES`, radius 4 / 6 / 8 sixteenths), as
   tubes do; cycling the hinge away takes the tire off. Grip is the material's
   `mechanical.friction` (refused if unmeasured -- no default borrowed) times the ground block's
   Sable friction, with Offroad's floor under a slick block. The rays run every **physics** step:
   `FrameBlockEntity` is a Sable `BlockEntitySubLevelActor` and hands `sable$physicsTick` to
   `Drivetrain.physicsTick`. Forces go to the body when the hub point is welded to a block, else
   to the body of the group holding it (a root box, in the box era), so the arms carry the car.
   Under cells the rays look past every body of their own frame: a hub's axle cells are not road.
   *As built, the tire is its own spring* until springs exist: it rings at `TIRE_SPRING_HZ` under
   its share of the car (the body's mass at the contact, split between the hubs; the cells are in it)
   at `TIRE_DAMPING_RATIO` of critical -- a frequency, not a stiffness, so every car squats about
   3 cm. The damper is capped at the closing speed per step. Grip is a slip coupling that cancels
   `TIRE_SLIP_RESPONSE` of the slip a step, capped at `friction × load`; a free hub only grips
   sideways (it rolls). *Found:* the first damper used the whole body's mass per hub and an
   uncapped explicit step, and the car left the ground at 30 blocks a second. *Found, and the
   reason the test is built where it is:* at the test server's ~10⁷ blocks Sable's poses and
   velocities are f32 noise of tens of blocks a second, so the driving test builds its car near
   the world origin, not in its own plot -- and clears bodies left there by earlier runs, since
   the test world keeps them. *Found, by the physics, not designed:* one driven wheel at the end
   of an axle, off the car's centre of mass, yaws the car round in circles -- the first test car
   did exactly that. That is why real cars have a **differential**, and it is the first free-form
   ratio part (step 3) worth building; until then a car drives straight only with its driven
   wheel on the centreline. The test car is a tricycle for that reason: 8 x 100 x 100 steel
   engine, small wool tires, about 9 blocks in its first 6 s, straight.
5. **Reaction into the network:** wheelspin (airborne shaft revs to target), stall on a slope,
   neutral downhill keeps the shaft turning. *Risk:* ringing (2, *Stability*). **Built
   (2026-10-14):** the road's torque on each driven wheel about its axle, `axle · (lever × F)`,
   goes back up the chain by its sense and through the coupling face's sense to the node, is
   averaged over the game tick's physics steps and pushed by 1/ratio -- the signed channel of
   step 1, unchanged. **Torque crosses at 1 Sable torque = 1000 Su** (`Units.SU_PER_KPG_TORQUE`):
   a kpg is a tonne and a block a metre, so Sable's torque is a kN·m, and Su already reads as N·m
   (the engine's 100 Su a litre is a real engine's figure). No fudge: it is what makes an
   ungeared 1.6 L engine lug a 2.5-tonne car at 0.06 b/s² -- the spec's "gearing down is the
   cure", arriving from the units. The push is clamped to what brings the hubs to the road's speed
   in one network step (*Stability*). Not yet game-tested: wheelspin in the air, the stall on a
   slope, neutral downhill -- the code path is the one tested, the cases are not.
6. **Drag and inertia** of the free-form parts; air drag on the body from `Vdrag`. *Risk:*
   unit constants for `Vdrag` → force are new placeholders. **Built (2026-10-14):** the chain's
   drag is `SHAFT_DRAG_SU_PER_RPM` per driving bearing it crosses and per driven hub; its inertia
   is each tube's and each driven tire's own spin (half m r², the tire a disc its radius across
   and as wide as its radius, weighed by its material), both referred by ratio². **Air drag is
   not a placeholder:** `AIR_HALF_DENSITY_KPG_PER_BLOCK3` is real air (1.225 kg/m³ over 2) in
   Sable's units, times `Vdrag` along the way the body is going (the six axis silhouettes,
   cached at rebuild, blended by the direction), times speed squared, on the body.
   **Replaced:** `PLACEHOLDER_LOAD` and its test; `VehicleGameTests.weldedTubeJoinsItsShaft` now
   checks the chain reaches its driving bearing and charges its friction, and
   `engineDrivesACarOnItsTires` drops a four-tire car and drives it.

**Still not built, and needed before a player drives:** the seat, the in-car controller and the
analog throttle (*Controls*), the steering actuator and linkage (*Steering*), brakes, and springs
and shocks to hang a hub from (the tire stands in). Tires do not turn on screen (nothing syncs
the spin). Entities still fall through plates.

### Controls: one controller block in the car

**Ruling (author, 2026-10-11):** driver controls are wired with the data-link architecture
(`control/data/`), and all of a vehicle's control runs through a **controller block placed in
the car**. The seat is a data source (the driver's inputs); throttle, brake, clutch and steering
are actuators the controller drives.

**The throttle is analog** (author, 2026-10-11). This is a deliberate exception to philosophy
§13's *the controller is a switch, never a dial*: the consistent version -- a throttle that is
open or shut -- was weighed and rejected because it is miserable to drive. Recorded because the
exception must not creep: §13's reason was that bang-bang control makes *process* control a
problem solved with physical mass. A driver is a human closing the loop by hand, which §13
already calls the first controller.

**The exception covers vehicles, and only vehicles** (author, 2026-10-11): anything in a
vehicle may carry an analog value, controller programs included; a factory controller stays a
switch. The boundary is the same one §1.4 draws for wear -- an engine on a stand driving a
shaft is a machine again, and its controller switches. Rejected: driver input only (narrower,
but a car's own logic -- traction control, cruise -- wants proportional output too); analog
everywhere (it would dissolve §13's oscillation problems, which are the factory game).

**Keyboard to pedal: ramp while held** (author, 2026-10-11). Hold the key and the pedal rises
over about a second; release and it falls back. Tap for a little, hold for full.

**Every vehicle needs a controller to drive** (author, 2026-10-11) -- the seat never links
straight to an actuator. Seat → controller → parts, always. Consequence, and the cost of the
ruling: a first car needs a program written before it moves. **No Programmer, no vehicle**
(author, 2026-10-11): there is no starter card and no default program; the program arrives with
the Programmer. This is a component requirement, the same as needing an engine -- a car with
no controller has nothing to tell its throttle anything -- not a tier lock.

### Steering: as deep as the block-world mechanics

**Ruling (author, 2026-10-11):** steering is mechanical, at the same depth as the block-world
machines -- a linkage the player builds, not a turn-angle setting. The input is a **steering
actuator** that turns a shaft to an angle from the controller, with **optional power assist**
that draws `Su` from the drivetrain network -- engine off, assist gone, steering heavy. From
the shaft on it is the player's linkage.

**The linkage is whatever the player builds** (author, 2026-10-11) -- rack, pitman arm, or
something nobody named. No shape is prescribed; tubes, pivots and the actuator are the parts.
(A rack needs rotation turned into a slide, which is a `Generic` joint with one linear axis
free -- the same joint the spring row already uses.)

**Without assist, steering is heavy at rest and light on the move** (author, 2026-10-11). Parked,
the wheels barely turn; rolling, they turn, but it takes more turns of the wheel -- sluggish.
With power steering it is far more controllable. This wants to be emergent rather than a
speed-scaled number: the driver's arm is an actuator with a small `Su` capacity, a tire
scrubbing in place resists far more than a rolling one, so at rest the load exceeds the arm and
on the move it does not; assist adds `Su` from the drivetrain. **The steering gear ratio is set
in the Programmer** (author, 2026-10-11) -- part of the program, so changing it means a new
card. **Banded** (author, 2026-10-11), per §1.5 -- a few named ratios (slow / medium / quick),
because it is a setup choice made once per card, not something steered with.

Bad geometry steers badly (bump steer, wheels that fight) the way a bad wishbone already rides badly.

---

## 6. Engines and gearboxes are designed, not picked

Settled by the author, 2026-10-10: engines and gearboxes are **authored in a GUI editor**
on the **Engine Stand**, not chosen from SummerCraft's four fixed `EngineType`s. The result is an
item carrying its design — the same *program is an item* shape as the Punch Card — which is then
placed as a block in the vehicle's base (§4).

**Engine:** number of cylinders, layout (inline / V / boxer — SummerCraft's four engines become
points in this space), bore, stroke, and materials. Materials set how much stress the engine can
take.

**Gearbox:** number of gears, each ratio, and materials. Materials set how fast it wears.

### How this sits with the rules

- **Bore, stroke and ratios are scalars, and §3 distrusts scalars.** They pass §3's own test
  for a scalar worth keeping (section 3 above): *the best value moves with what you want the
  vehicle to do.* Big bore / short stroke revs and makes top-end power; small bore / long stroke
  makes low-end torque; tall gears cruise, short gears climb. There is no single correct engine,
  only one for this car, this route, this load. **[OPEN]** step size — coarse steps (say whole
  millimetres, ratios to 0.05) so nobody grinds decimals.
- **Displacement is fluid volume, so it is `mB`.** 1 mB is a litre: a 2.0 L engine is 2 mB of
  swept volume. No new unit.
- **Materials as a stress ceiling is §17's rule already.** *Every machine states the most it can
  deliver, whatever is driving it* — an engine's material caps the cylinder pressure and RPM it
  survives; past it, the force that cannot go into the work goes into the engine. Read from the
  material's properties, never its id (§11).
- **Wear:** vehicles wear with use (§1.4), and a gearbox's material sets the rate. Over-stressing
  an engine past its material is misuse wear on top, and should read differently in a tooltip.

Settled 2026-10-11: the editor lives on the **Engine Stand** (gearboxes too, unless they earn a
bench of their own); **strength is the material's `hardness`** from the deformation table — the
same number that already gates blows, so one figure per material, not a second one
(**superseded for the rev limit**, 2026-10-11: see *What the material system gains* below); **no prior
art** to study (*Automation — The Car Company Tycoon Game* stays a design reference only, closed
source). Step size stays **[OPEN]** until the editor exists to try it.

### Engine Stand v1 -- the bare minimum, built before any car drives

**Ruling (author, 2026-10-11):** no car moves until the editor exists, and the editor is kept
to the minimum; everything past it is later work.

**Built (2026-10-11), `vehicle/engine/`:** Engine Stand (paper slot; − / + per setting; a
**sample slot** for the material; live readout; Print), Engine Blueprint, the
crafting recipe (Blueprint + the material's `ingot` form × cylinders → Engine, Blueprint kept),
and the Engine block, which keeps its design through place and break.

**The material is set by a sample, not a list** (author, 2026-10-11) -- built. Any form of a
metal (an ingot, a nugget, a plate, a storage block) dropped in the sample slot makes the engine
that metal: `Material.find` names the material, and `EngineDesign.buildable` says whether an
engine can be made of it (kind `metal`, a density, a yield strength, an ingot to pay in). The
sample is a reference and is never used up; the ingots are paid at the crafting table. Replaced
a cycle-through picker, which listed materials by id -- the slot asks the player for the metal
the way they would ask a smith. The material table is now
synced to clients for the readout. Game-tested: the design trades, print, and the recipe
refusing the wrong material or count. Not yet seen in game.

**An engine drives the block network** -- built (2026-10-11). The Engine block is a source in
the existing rotation network, like the Steam Engine:

- **Capacity is the torque curve at the engine's own current RPM** (§2's ruling, literally):
  `torqueAt(|rpm|)`, so it starts weak from rest (45% of peak at 0 RPM), climbs as the run spins
  up, and a run settles where curve, load and shaft friction meet. The network reads capacity
  only on recalculation, so the engine's tick asks for one whenever the curve has moved by 1 Su.
- **Target speed is the peak-torque RPM** -- a **placeholder** until there is a throttle. With
  real shaft friction a run never gets there; it settles lower, on the rising side of the curve.
- **Off when placed.** Use toggles it; it is `Switchable` (and a `DataNode`, the Clutch's shape),
  so the timer, the bimetallic strip and a Punch Card's actuator node all switch it. Off is no
  capacity and not a source: the run coasts down under its own load. Why off: with no fuel
  nothing else makes starting it an act, and a source live from placement spins whatever it was
  set against mid-build. On a stand its controller switches it (§13); the analog throttle is the
  vehicle's, and comes with driving.
- **The crankshaft runs through the block** along a `facing` set from the clicked face, so
  either end couples -- the Steam Engine's and Hand Crank's rule. Placeholder art: the gearbox
  side texture on both shaft ends. No rotating render.
- Game-tested: off when placed, on turns a coupled shaft with network capacity equal to the
  curve at the run's speed, off supplies nothing and the run coasts down.

**Not yet:** fuel, heat (the engine as a `ThermalBody`, §7), wear, throttle, a starter. Until
fuel exists a running engine is free power. **Scale clash, flagged:** an engine's peak RPM is in
the thousands (a 1.6 L steel four peaks near 4400) where every other source turns at tens --
Steam Engine 16 RPM. Machines read RPM for stroke rate, so an engine on a stand driving a hammer
directly is far faster than anything else; gearing down is the cure. **Accepted as is** (author,
2026-10-11): no rescaling of either side.

**An engine weighs its design** -- built. Sable weighs a block by its state only, so the Engine
block is `sable:super_light` (0.25 kpg) and, on a vehicle, carries the rest of its design's mass
on a box welded inside it, contacts off -- the plates' way. Weightless was tried first: a
vehicle of only an engine weighed zero at the moment of lifting and Sable discarded it. Cost:
an engine designed under 250 Mu weighs 250. Plates and tubes weigh by material density too
now (*Bridging to Sable*).

- **Settings:** cylinders (1-8), bore, stroke, material. Layout (inline / V / boxer) comes later
  and will change only the shape until then.
- **The engine is always one block** in v1, whatever its size.
- **A design becomes a Blueprint item** (the Punch Card shape -- copyable), and the Blueprint is
  crafted with the material's ingots into the Engine item that carries the design.
- **Every material figure comes from the material system** (author, 2026-10-11, a standing
  rule): the material is chosen from `material/*.json`, never by item id, and anything the
  engine needs that a material does not yet hold is **added to `Material`** as a new optional
  field -- not kept in an engine-side table. The material record only ever held what recipes
  needed so far; it is meant to grow.

**How the numbers come out** -- simple physics, every constant a placeholder (philosophy §19):

| Figure | From | Placeholder |
| :--- | :--- | :--- |
| displacement (`mB`) | cylinders × π/4 × bore² × stroke | -- |
| peak torque (`Su`) | proportional to displacement | 100 `Su` per litre (`mB`) |
| redline (`RPM`) | the material's piston-speed limit ÷ (2 × stroke) | limit = 14.5 m/s × √(yield strength ÷ density) |
| peak-torque RPM | a fraction of redline | 0.6 |
| torque at RPM | SummerCraft's curve: peak × clamp(1 − 0.55x², 0.3, 1), x = (rpm − peak)/peak | -- |
| mass (`Mu`) | metal volume × the material's **density** | metal volume = 20 × displacement |
| cost | the material's `ingot` form | **[OPEN]** count |

So the trades §6 promised fall out: a short stroke revs higher, a long one revs lower and, at
the same displacement, the bore shrinks; a bigger engine is stronger and heavier. A 4 × 80 × 80
in copper (yield 4, 8.96 Mu/mB) redlines about 3600 RPM, in steel (yield 15, 7.85) about 7500 --
what the engine is made of is a real choice. The square root of yield over density is the one
piece of physics kept: a piston's own inertia loads it as density × speed², so a light strong
metal revs highest and a heavy one pays for its weight twice.

**What the material system gains:** `density` (`Mu/mB`) -- the first figure added for a non-recipe
use, and now read by plates and tubes too. **The rev limit reads `mechanical.yield_strength`,
not hardness** (author, 2026-10-11). Hardness was the first choice only because it was the one
mechanical figure every material had when the engine was built -- the property table
(mechanics §5.6) did not exist yet, and reusing it looked like the "one figure per material"
economy the ruling above asked for. But hardness is resistance to being dented, which is what a
blow cares about; what a piston at speed cares about is how much load it takes before it bends,
which is yield strength, and the two disagree (gold is soft and weak, stone hard and brittle).
A material with no yield strength is not offered by the stand, and one that somehow reaches an
engine anyway redlines at zero rather than borrowing a default figure. **[OPEN]** cost in ingots: a real engine's metal is a fraction of an
ingot at Minecraft's scale (an ingot is 1/9 m³), so cost by volume is too cheap to mean anything.

---

## 7. Conflicts found in the SummerCraft code

Things the merge has to change, not just move.

- **Identity checks.** `EngineType.byBlock(Item)` and `PartSlot.accepts(Item)` route on item
  identity, which §11 forbids. A mount point should accept a part by **tag and properties**
  (bolt pattern, size class, what it attaches to), so a pack's third-party carburettor fits.
  This is the same move the memory note *ecosystem compat: tags/properties, never vanilla
  identity* records.
- **Engine families as an enum.** `EngineType` hard-codes four engines with torque, peak and
  redline numbers. Per *machines define operations, not recipes* and the datapack-table
  precedent (`deformation`, `thermal_process`, ...), engine figures belong in a datapack
  table so a pack can add a straight-six with a line of JSON.
- **The car's thermal model.** SummerCraft's engine temperature is its own sim. It becomes a
  `ThermalBody` driven by `Heat` — fewer lines, and it inherits the "cannot pass the flame"
  property for free.
- **Wear sources.** Usage wear stays (§1.4). But *piston wear from knock, heat or no oil* is
  misuse wear, and is already the shape Feedback's rule wants: it is evidence of a specific
  mistake. Keep the two visibly distinct in tooltips — "worn" vs. "damaged by knock".
- **Generated art and sounds.** `tools/gen_textures.py` and `gen_sounds.py` are the author's
  own and are fine to carry over; CLAUDE.md's "all art is placeholder" still holds.

---

## 8. Build order

The author's scope call: **first, SummerCraft's functionality moves in, ending with cars that
can be built in the vertex system. Then the "crazy stuff"** — thrusters, aircraft, anything
SummerWorks was heading toward.

1. ✅ **Sable in the build.** Sable 2.0.6; Flywheel raised to 1.0.7 (Sable refuses below 1.0.6).
2. ✅ **Vehicle Core** under `vehicle/`, assembling connected blocks into a Sable body. Game test
   passed. Since replaced by the Lift (step 4).
3. **Vertex assembly.** ✅ Chassis Frame block, Welder, frame model (vertices, edges, triangle
   plates, material), `Vdrag`, renderer, game tests. ❌ Not yet seen in game — the dev client
   would not open a window in the session that built it. ~~Assembles into a Sable body with one
   free box per plate and tube~~ -- replaced. ✅ **Redone on cells** (2026-10-15 playtest, §4
   *cells, again*, *As built*): octant state encoding, each bearing-cut group not welded to the
   base its own body, bearings as joints between bodies, 1/16 outline and entity shape per
   position, the Lift lowered at the bottom puts the vehicle back as blocks. Game-tested and
   played (2026-10-16). ✅ The click overlay (`client/BuildToolHud`), Weld and Remove modes.
   ✅ Built, not yet played (2026-10-16): multi-point selection (clicked or boxed), the move gizmo
   (click to step, drag along an axis), Extrude, alignment lines, mirror planes (`Symmetry`, the
   symmetry screen on the frame block). ❌ The rest of Main Assembly-style editing (waiting on
   the author's description).
4. **Block base** (§4, 2026-10-11). ✅ Lift: lifts what rests on it into one held body, raises,
   lowers, drops on break; core and frame merged by the Lift taking assembly. Played
   (2026-10-16); follows welds and frame joints, and lowers and carries the whole superobject. ✅ Welder welds a vertex to a base block -- the only thing holding a frame
   to its vehicle; game-tested. ❌ A second base is a second body (trailers). Then the Extruder (thicken, pull). Entity collision is the cells' (step 3).
5. **SummerCraft parts as base blocks**, converting as §2 and §7 say: units first, then
   identity checks, then the engine datapack table, then the thermal model.
6. **Driving** on the Sable body: ray tires (§5), SummerCraft's controls, clutch, gearbox, grip.
   ✅ Ray tires, grip, the reaction into the network and air drag (§5 steps 3-6). ✅ **Driven in
   a client** (2026-10-16): engines through tubes to tires, guard rays, rolling resistance,
   straight axles, several drive lines. ✅ built, not played: Driver's Seat, analog cards.
   ❌ Steering, clutch, gearbox, brakes, differential.
7. ✅ **Build-time aero figure** — `Frame.vdrag`, read as air drag every physics step.
8. **Banded tuning** (§3), last, once there is a car to tune.
9. **Deformation pass** (later): crashes, crumple zones, joints letting go, all as one system.

Package is `vehicle/` (chassis under `vehicle/frame/`), registers in `registry/FVehicles`.
**[OPEN]** whether the engine lives under `machine/` if it is allowed to drive shafts.

---

## 9. Open questions

Collected from the inline **[OPEN]** marks above; each is answered where it sits, then struck
here.

- §3 — which knobs get fine adjustment inside their band, knob by knob.
- §4 — whether tubes switch to a pressed form too.
- §4 — carried creation size: does a huge creation on an item break syncing?
- §4 — whether half-block octant physics is felt in play (*cells, again*).
- §6 — engine cost in ingots (volume-based is too cheap at Minecraft scale).
- §6 — editor step size for bore, stroke and ratios, until the editor exists to try it.
- §8 — whether the engine lives under `machine/` if it drives shafts. (It drives them now, from
  `vehicle/engine/`.)
