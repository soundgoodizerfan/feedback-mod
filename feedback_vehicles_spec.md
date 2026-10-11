# Feedback — Vehicles

**Working document, not one of the three.** Scopes merging two of the author's own mods into
Feedback as first-class content — not compat:

- **SummerCraft** (`../SummerCraft-reference`, NeoForge 1.21.1, ~5.4k lines) — My Summer
  Car-style car: fit parts into slots, bolt them down, fill fluids, crank it, drive a manual.
- **SummerWorks** (`../SummerWorks-reference`, two commits) — vehicles as **Sable** physics
  bodies. Only a Vehicle Core and a Thruster exist so far.

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
3. **Vehicles are not built out of world blocks** the way Create: Aeronautics does it. The
   chassis is constructed by a system modelled on *Main Assembly* (§4). Sable is there
   because Minecraft is bad at arbitrary physics bodies, not to define the build method.
4. **Vehicles wear with use.** This is the hand-hammer precedent, not an exception to the
   misuse-only wear rule: that rule is about *machines*. A vehicle is a **tool**, and tools
   wear in vanilla. Tyres from distance, plugs from running time, clutch from slipping and
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
| torque, Nm | `Su` | Feedback's `Su` is Create's stress — a load at speed. Torque on a shaft is the same idea. **[OPEN]** whether the engine's output is a torque curve in `Su`, or whether it is expressed as `Su` *capacity* the way a Create engine is, with `Drag` (`Su/RPM`) as the load the car puts back on it. The second is what lets a vehicle engine drive a stationary shaft network for free. |
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
(tyre width 155–265, offset, camber, toe, ride height, spring rate) — every one a free scalar
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
| Tyre compound | already discrete (`TyreType`) | No |
| Tyre width | narrow / standard / wide | No |
| Camber, toe | neutral / sporty / aggressive | No |
| Ride height, spring | soft / stock / stiff | Probably not |

---

## 4. The chassis: in-world vertex assembly

Settled by the author, 2026-10-10.

- **Built in the world, from nodes.** The player places **vertices** and connects them.
  Connected vertices form edges; closed loops of edges form **plates**. The shape is
  arbitrary — the car looks like whatever was built.
- **Vertices snap to a 1/16-block grid** in 3D. Connections between vertices are
  arbitrary: any vertex may join any other, so plates need not be axis-aligned.
- **Any part on any compatible mount point.** SummerCraft's fixed `PartSlot` positions do
  not survive. A functional part (engine, wheel, tank, seat) declares what it needs to mount
  on; the frame offers mount points; compatibility is by tag and property, never item id
  (§5).
- **Aerodynamics are computed once, at build time**, and stored as numbers the driving model
  reads. The finished shape is never re-simulated while driving. This is the
  anti-simulator rule (§3 of the philosophy) applied correctly: the shape is a real
  engineering choice, so it is modelled — but as a figure, not a live fluid sim.
- When assembled, the structure becomes one Sable body. Mass, centre of mass and inertia come
  from the parts (`Mu` per part, plates by area), not from constants — which retires
  `CarPhysics.MASS = 900`.

**Aerodynamic drag is `Vdrag`** (V for vehicle) — settled. `Drag` in `core/unit/` already
means `Su/RPM`, a mechanical load, and the two must never be swappable.

**The build-time figure is projected area, angle-weighted** — settled. Each plate contributes
its area times how squarely its normal faces the airflow; summed per direction of travel.
Cheap, computed once, and good enough to make "slab-fronted brick" and "raked wedge" two
genuinely different cars. A voxel raycast was the next candidate and is not needed.

**The connecting tool is the Welder** — settled. Click vertex, click vertex, the same
interaction shape as the data connector's links.

**Plates can be any material; mass and strength follow the material** — settled. These are
properties the game already implies (an iron block is obviously heavier and tougher than a
wool one) made concrete. Mass is density (`Mu/mB`) × plate area × thickness; strength comes
from the material's existing hardness. Both are read from **tags and properties**, never a
per-item table keyed on id (§5). **[OPEN]** whether plate thickness is a choice (thin /
thick, per §3) or fixed.

**Plates are triangles** — decided in the build. "Closed loops form plates" only pins down a
plate when the loop is three edges: four vertices in a ring need not be flat, and a non-flat
quad has no agreed area. A flat quad is two triangles on a diagonal. Each plate costs one of the
off-hand block (**[OPEN]**: a placeholder; a real cost would scale with area). No block in the
off hand, no plate — the edges stay a bare frame.

**Vdrag is the silhouette, not a sum.** Summing every plate's `area × |cos|` counts a closed
body twice (nose and tail project onto the same patch of sky), and halving it is wrong for a
lone plate. `Frame.vdrag` rasterises the projection at 1/16 resolution, keeps the frontmost
plate per cell and weights the cell by that plate's `|cos|`. Tested: a 1×1 slab is 1.0, the same
plate raked 45° is 0.71, a closed unit box is 1.0.

### Bridging to Sable — settled: plate cells at 1/16

Sable takes collision and mass per `BlockState`, never per block entity (its
`RapierVoxelColliderBakery` reads each state's `VoxelShape` and bakes its boxes into Rapier),
and its native bridge has no convex or mesh shape. An entity chassis does not escape that:
Sable's own physics boxes exist, but entities walk through them, and vanilla entity collision
is unrotated boxes. Patching Sable or waiting for its unused `BlockSubLevelDynamicCollider`
were the other routes; both rejected for now.

**The frame renders the true flat plate; collision is a hidden block per cell, at 1/16.**
`FrameAssembly` samples every plate, and each block it crosses gets a `ChassisCellBlock` whose
state names the plane: one of 49 directions (every primitive integer vector in −2..2) and one
of 16 offsets along it. Each pair's shape is a 1/16 staircase, greedy-merged into few boxes
(a flat floor is one box; a 45° plate is a short staircase). A cell crossed by two plates at
different angles has no single plane and is solid. Mass rides on a `load` state (8 steps)
through Sable's own `physics_block_properties` table; plate mass is the material's `sable:mass`
× plate volume. Using the Chassis Frame with an empty hand assembles frame + cells — an
explicit list, not a floodfill, so a chassis parked on the ground leaves the ground behind.

**Editing is live, assembled or not** (author's call: changes on the fly must be quick). The
Welder carries its look ray into an assembled body's local space, so moving, splitting,
removing, re-plating and welding all work on a vehicle exactly as on a frame in the world; every
edit re-cuts that frame's cells in place (`FrameAssembly.recut`) and Sable picks the block
changes up itself. A new cell that would land on another block is skipped rather than refusing
the edit — a gap in collision, not a blocked repair.

**[OPEN]** Mass step size (`FrameAssembly.MASS_PER_LOAD`) is a guess. There is no disassembly yet.

**A cell is its plate's material** (author's call). It mines with that block's break time and
tool requirement, sounds like it and resists blasts like it — the hardest, where plates meet.
Breaking a cell removes every plate through that block from the frame, drops their material if
the tool could have harvested it, and re-cuts; an explosion removes them with no drops. A cell is
never removed on its own, since the plate would still render over a hole.

Mining one cell takes the **whole triangle**, however many blocks it spans; finer damage is had
by splitting edges into smaller plates. Cutting just the mined block's piece out of a plate
(automatic re-triangulation) was offered and **deferred by the author** — keep it simple for now. Editing inside a moving body is not yet tested
with a real player — game tests cover the re-cut, not the transformed aim. If Sable ships per-block colliders, the cells
go and nothing else changes.

**Prior art:** none directly. The nearest is Chisels & Bits, but it is voxel-based (1/16
cubes), not vertex/plate-based, so it informs the 1/16 grid and placement feel at most.
Feedback's own in-world node system (`control/data/`, `DataNode`, the connector overlay) is
the closer precedent and will be reused where it fits.

---

## 5. Conflicts found in the SummerCraft code

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

## 4a. Mounting and suspension are built, not picked

Settled by the author, 2026-10-11:

- **Parts mount on plates** at free 1/16 points, kind by tag (`feedback:mount/*`), never id.
- **An overloaded plate wears instead of refusing the part, and says so** — a stress
  notification, same rule as §17's ceiling: the mount holds, the plate pays.
- **Dedicated parts, not edges, make the running gear:** axle, tubing, bearing (pivot),
  spring, shock. Players build suspension geometry themselves — wishbones, trailing arms,
  solid axles — out of tubing and pivots, sprung and damped by parts they chose.

**This needs more than one body, and Sable has joints** (corrects an earlier note here that it
offered none — that read missed `dev.ryanhcode.sable.api.physics.constraint`):
`PhysicsPipeline.addConstraint(bodyA, bodyB, config)` with `Fixed`, `Free`, `Rotary` (hinge,
`setServoCoefficients`) and `Generic` (six axes, per-axis lock and `setLimit`). Every handle has
`setMotor(axis, target, velocity, stiffness, damping, …)` and `getJointImpulses`. So:

| Part | Sable joint |
| --- | --- |
| bearing / pivot | `Rotary` between two bodies |
| axle + wheel | `Rotary` on the wheel's own body, motor driven by the drivetrain |
| spring + shock | `Generic`, one linear axis free with `setLimit` travel; `setMotor` stiffness is the spring, damping the shock |
| welded joint | `Fixed` — **not** `Generic` with every axis locked: tried first, it held bodies at whatever offset they started at and never closed a gap |

**Built (2026-10-11):** frame-to-frame joints. Frames carry a UUID (assembly moves a frame's
block entity into a body's plot, so position cannot name it); `FrameJoint` stores the vertex pair,
kind and bearing axis; the frame's ticker makes the Sable constraint once both frames are bodies,
with contacts between the two switched off. Welder: select a vertex, use a vertex on *another*
frame — a weld, or a bearing about the axis nearest the look if the off hand holds anything tagged
`feedback:bearings` (the shaft Bearing is the first member — the same part, two jobs). Use again
to cut it. Game-tested weld and bearing. **Testing caveat:** the game-test server runs tests ~10⁷
blocks out, where Sable poses come back rounded to a sizeable fraction of a block; joint precision
must be judged near the origin, in game. Not yet: tubing, springs/shocks, axle/wheel, joint
breakage and the stress notification.

`getJointImpulses` is the stress readout — the plate-wear and notification figure comes from it,
not a guess. A raycast-suspension shortcut was considered and rejected: it fakes exactly the
geometry the author wants players to build. **One frame is one body** — settled 2026-10-11. Each moving piece is
its own Chassis Frame; bearings and springs join frames. **Frames weld to each other**
(Scrap Mechanic-style): the Welder joins two frames at a point on the 1/16 grid with a `Fixed`
joint. A joint overloaded past its material lets go and the piece falls off whole — re-weld
it, never rebuild it. **[OPEN]** spring/shock steps (banded,
§3); Aeronautics as prior art — author knows of none.

## 5a. Engines and gearboxes are designed, not picked

Settled by the author, 2026-10-10: engines and gearboxes are **authored in a GUI editor**
on the **Engine Stand**, not chosen from SummerCraft's four fixed `EngineType`s. The result is an
item carrying its design — the same *program is an item* shape as the Punch Card — which then
mounts on the frame like any part.

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

## 6. Build order

The author's scope call: **first, SummerCraft's functionality moves in, ending with cars that
can be built in the vertex system. Then the "crazy stuff"** — thrusters, aircraft, anything
SummerWorks was heading toward.

1. ✅ **Sable in the build.** Sable 2.0.6; Flywheel raised to 1.0.7 (Sable refuses below 1.0.6).
2. ✅ **Vehicle Core** under `vehicle/`, assembling connected blocks into a Sable body. Game test
   passes.
3. **Vertex assembly.** ✅ Chassis Frame block, Welder, frame model (vertices, edges, triangle
   plates, material), `Vdrag`, renderer, game tests. ❌ Not yet seen in game — the dev client
   would not open a window in the session that built it. ✅ Assembles into a Sable body through 1/16
   plate cells (§4, *Bridging to Sable*); game-tested, not yet driven or seen.
4. **Mount points + SummerCraft parts**, converting as §2 and §5 say: units first, then
   identity checks, then the engine datapack table, then the thermal model.
5. **Driving** on the Sable body: SummerCraft's controls, clutch, gearbox, grip.
6. ✅ (early) **Build-time aero figure** — `Frame.vdrag` exists; nothing reads it until driving
   does.
7. **Banded tuning** (§3), last, once there is a car to tune.

Package is `vehicle/` (chassis under `vehicle/frame/`), registers in `registry/FVehicles`.
**[OPEN]** whether the engine lives under `machine/` if it is allowed to drive shafts.
