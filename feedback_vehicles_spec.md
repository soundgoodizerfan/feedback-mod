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

### Bridging to Sable — **[OPEN], needs the author**

Found while building: **Sable takes mass and collision per `BlockState`, not per block
entity.** `BlockSubLevelCollisionShape` returns a `VoxelShape` from `(BlockGetter, BlockState)`
with no position, and mass comes from Sable's `physics_block_properties` datapack table, keyed
by block/tag and state properties. A welded chassis is one block entity holding arbitrary
triangles, so neither can be handed to Sable directly. Three ways through:

| | How | Cost |
| :--- | :--- | :--- |
| **A. Rasterise on assembly** *(proposed)* | When the Vehicle Core assembles, every block cell a plate passes through gets a hidden *chassis cell* block. Its collision is the cell (coarse, 1-block). Its mass is a `load` state property, 0–15, mapped to mass through Sable's own `physics_block_properties` overrides — Sable's documented mechanism, no mixins. | Collision is blocky; mass quantised to 16 steps per cell. |
| B. Mixin into Sable | Add the frame's exact mass to `MassTracker.build`; exact collision via a custom collider. | Breaks on Sable updates; the mixin surface is internal. |
| C. Ask upstream | Request a per-block-entity mass/collision hook from Sable's author. | Waiting. |

A is the one that ships without touching Sable internals; B or C later only if A's blocky
collision turns out to matter in play. Plate mass under A is the material's own `sable:mass`
(per full block) × plate volume — a property the game already gives every block, made
concrete, per the ruling above.

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

## 6. Build order

The author's scope call: **first, SummerCraft's functionality moves in, ending with cars that
can be built in the vertex system. Then the "crazy stuff"** — thrusters, aircraft, anything
SummerWorks was heading toward.

1. ✅ **Sable in the build.** Sable 2.0.6; Flywheel raised to 1.0.7 (Sable refuses below 1.0.6).
2. ✅ **Vehicle Core** under `vehicle/`, assembling connected blocks into a Sable body. Game test
   passes.
3. **Vertex assembly.** ✅ Chassis Frame block, Welder, frame model (vertices, edges, triangle
   plates, material), `Vdrag`, renderer, game tests. ❌ Not yet seen in game — the dev client
   would not open a window in the session that built it. ❌ Not yet a Sable body — blocked on
   *Bridging to Sable* (§4).
4. **Mount points + SummerCraft parts**, converting as §2 and §5 say: units first, then
   identity checks, then the engine datapack table, then the thermal model.
5. **Driving** on the Sable body: SummerCraft's controls, clutch, gearbox, grip.
6. ✅ (early) **Build-time aero figure** — `Frame.vdrag` exists; nothing reads it until driving
   does.
7. **Banded tuning** (§3), last, once there is a car to tune.

Package is `vehicle/` (chassis under `vehicle/frame/`), registers in `registry/FVehicles`.
**[OPEN]** whether the engine lives under `machine/` if it is allowed to drive shafts.
