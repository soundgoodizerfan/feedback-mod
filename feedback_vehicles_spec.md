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
| mass, kg (`MASS = 900`) | `Mu` | `Mu` is anchored to the kilogram. |
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
  box select. Not yet: a visible platform (the raised vehicle hangs over a bare block), and
  putting a dropped vehicle back on a Lift.

- **A pile of blocks is a body; free-form parts hang off a body as boxes.** A free-form group
  with no block on it (a wishbone, a shaft) stays a cheap box cluster (§5). Anything that needs
  a block needs a base of its own, so it is a body.
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
  vehicle is lifted, Scrap Mechanic's way. A weld on a point set as a bearing is that bearing,
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
- When assembled, the block base becomes one Sable body and the plates and tubes become boxes
  joined to it (*Bridging to Sable*, below; §5 for pivots). Mass, centre of mass and inertia come
  from the parts (`Mu` per part, plates by area), not from constants — which retires
  `CarPhysics.MASS = 900`. **The vehicle keeps its own total** (author, 2026-10-11): Sable
  weighs the blocks, the boxes carry their own mass, and the vehicle sums both for driving, the
  HUD and the dyno -- 300 kg of engine plus 200 kg of steel panels drives like 500.

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
wool one) made concrete. Mass is density (`Mu/mB`) × plate area × thickness; strength comes
from the material's existing hardness. Both are read from **tags and properties**, never a
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

### Bridging to Sable — prototype: one free box per plate

**Ruling (author, 2026-10-11): chassis geometry is not blocks.** A plate is part of the Sable
object, not a block placed in the world. Sable offers one thing below blocks: `ArbitraryPhysicsObject`,
of which `BoxPhysicsObject` (an oriented box with a pose, half-extents and mass) is the only
shape. There is no mesh, convex or compound shape, and `VoxelColliderData.addBox` is axis-aligned
within a block -- which is why the plate cells below were 1/16 staircases.

**Built:** the frame block is the whole Sable body (`FrameAssembly` lifts that one block and
nothing else). `FrameBodies` makes one `BoxPhysicsObject` per plate and per tube; each group's
boxes are welded to a root box, and a group meets the body only at a welded point (above),
contacts off. A plate is its triangle's bounding rectangle in its
own plane, one `PLATE_THICKNESS` deep, along its longest edge; a tube is a box along its edge
at its diameter. Mass is the material's `sable:mass` × volume, carried by the box.

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

**Superseded -- plate cells at 1/16.** The previous answer put a hidden `ChassisCellBlock` in
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
(`Frame.pivots`); once assembled, the frame is cut at each pivot *in how its boxes are held*:
everything reachable from a side without crossing a pivot is a group, and each group is a rigid
cluster of boxes -- welded to one root box, with no block and no body of its own -- held to its
neighbours by the bearings and to the body by its welded points (§4).
**One chassis is one frame block, however many bearings it has** (author, 2026-10-11; an
earlier pass gave each group its own block and body, which was wrong and is gone). A group gets
a body only by carrying blocks -- a base of its own (§4). Ball is Sable's `Generic` joint with linear axes locked, so
a ball bearing with nothing holding it goes slack and swings down under gravity; hinge is
`Rotary`; locked is `Fixed`, unlike a weld because it can be switched back. The constraint is
made between any two physics bodies -- the sublevel for the main group, a root box for the
rest -- with its anchor in each one's own space. Setting a pivot on a vehicle that is already a
body takes effect at once; the groups it changes are re-made at rest relative to the body, so
whatever was swinging snaps back as it is re-cut. Boxes are not saved (Sable drops them with
their chunk), so a reload also re-makes every group at rest. On a joint that already exists
(two frames, welded or bearing) the same tool cycles the joint, so a weld between frames can
be made a bearing and back. A point whose edges all sit on one side of the cut has nothing to
pivot against and does nothing. The frame block is where the frame's data lives and ticks, and
the boxes are not saved, so it is the anchor; with the block base (§4) it is lifted with the
other blocks of the car and stops being a spare block.

This is **not** the shaft Bearing block. That one is a support in the mechanical network; a
vehicle bearing is a joint between rigid bodies, and the two had been sharing an item and a tag
(`feedback:bearings`) on the strength of the word. Divorced: the tag is gone, the Welder no
longer reads the off hand for a bearing, and the pivot has no item to spend. **Rejected:** a
consumable bearing in the off hand -- it made a *setting* cost an item, and the hand it
occupied was the one that holds the plate material. Shape taken from Scrap Mechanic's bearing
(closed source -- design only, nothing read); its hinge-only bearing is widened here by the
ball and locked settings.

**Drive is recorded but does nothing yet**: it needs the drivetrain (§8 step 6), where a driving
bearing becomes a shaft link and a free one does not. **Not yet:** entity collision with the
plate and tube boxes -- see below.

**Tubing and drive shafts are one part** (author, 2026-10-11) — built. A tube is an *edge given a
material*, the way a plate is a triangle given one: Welder on an edge with a block in the off hand.
It splits with its edge, goes when the edge is cut, carries mass (length × a 2/16-square section ×
the material's mass) as its box. A **drive shaft is a frame made of tubes on
driving bearings**: a hinge's axis is now any direction, and a bearing on a vertex with exactly one
tube takes the tube's direction, so a shaft turns about itself at any angle. The shaft's rotation
reaching anything waits on the drivetrain, like the drive flag.

**Tube sizes** (author, 2026-10-11) — built. Sizes are a banded choice: thin, medium, thick (2, 4, 6
sixteenths across); the Welder with the tube's own material again steps it up, thick wrapping to
thin. A tube's box is its diameter square, along its edge. (Its collision was once a 1/16-voxel
outline per block state; see *Superseded* above.)

Not yet: springs/shocks, ray tires. Breakage waits for the deformation pass.

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
same number that already gates blows, so one figure per material, not a second one; **no prior
art** to study (*Automation — The Car Company Tycoon Game* stays a design reference only, closed
source). Step size stays **[OPEN]** until the editor exists to try it.

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
   would not open a window in the session that built it. ✅ Assembles into a Sable body with one
   free box per plate and tube (§4, *Bridging to Sable*); game-tested, not yet driven or seen, and entities do not yet collide with it.
4. **Block base** (§4, 2026-10-11). ✅ Lift: lifts what rests on it into one held body, raises,
   lowers, drops on break; core and frame merged by the Lift taking assembly. Game-tested, not
   yet seen in game. ✅ Welder welds a vertex to a base block -- the only thing holding a frame
   to its vehicle; game-tested. ❌ A second base is a second body (trailers). Then the Extruder (thicken, pull) and entity collision against the boxes.
5. **SummerCraft parts as base blocks**, converting as §2 and §7 say: units first, then
   identity checks, then the engine datapack table, then the thermal model.
6. **Driving** on the Sable body: ray tires (§5), SummerCraft's controls, clutch, gearbox, grip.
7. ✅ (early) **Build-time aero figure** — `Frame.vdrag` exists; nothing reads it until driving
   does.
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
- §6 — editor step size for bore, stroke and ratios, until the editor exists to try it.
- §8 — whether the engine lives under `machine/` if it drives shafts.
