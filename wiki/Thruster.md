# Thruster

A rocket engine for a [vehicle](Vehicles.md). It burns liquid fuel and pushes the whole body it is part of along the way it faces, at the block. Several thrusters mean more push, and one off-center turns the craft.

## Designing and building

A thruster is a modular build. Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Thruster* in the type row, under Technology); drawing it takes Aerospace Treatise II: Thrusters ([Treatise](Treatise.md)). It has one part, the **Thruster Nozzle**, shaped from plate of a metal with a measured yield strength at the [Smithing Table](Smithing_Table.md) with the blueprint in the blueprint slot. The thruster comes out with the design, and the blueprint is kept.

- **Bore** is a stepper from 8 to 40 mm in steps of 2, 20 mm by default. A wider bore pushes harder and burns more fuel; both follow the bore's area.
- **Shape** trades push against fuel. A **Bell** is the baseline. A **Cone** pushes 30% harder and burns 50% more fuel. A **Throat** pushes 30% less and burns half the fuel.
- **Regen jacket** is an optional addon, a plate wrapped round the nozzle that the fuel runs through first. The fuel arrives warm, so 15% less of it burns for the same push.

The bench sheet and the thruster's tooltip show how many times a reference thruster (20 mm bell) it pushes. A specimen to take apart for the treatise is in [Treatise](Treatise.md#specimens).

It does nothing on blocks that stand in the world. Like everything on a [Lift](Vehicles.md#the-lift), it works once the vehicle is lifted into a moving body.

## Using it

- **Place it** so it faces the way you want to be pushed: set against a face, it pushes the body away from that face. Flame comes out of the back.
- **Fuel.** A bucket fills it, or a [Pipe](Pipe.md). It mixes whatever arrives **by mass** and never refuses a fluid, like an [Engine](Engine.md)'s tank; a fluid that does not burn dilutes the flame. See [Fuels](Fuels.md).
- **Right-click** to turn it on or off. The message tells you how much fuel is left.

## What it pushes with

- **Thrust follows the flame.** A hotter fuel pushes harder and goes further per mB. A reference thruster (a 20 mm bell) burns about 1 mB a tick for a standard fuel; a wider bore or a cone burns more, a throat or a regen jacket less.
- **It needs oxygen.** A thruster burns air, which it has only in an **atmosphere**: not in the End, and not with the nozzle under water. It can instead use fuel that brings oxygen of its own (soul oil does), and then it burns anywhere, but only as hard as the fuel's own oxygen allows. With neither, it makes no thrust at all.

## Air drag and top speed

Everything that moves through air is slowed, in proportion to the **square of its speed** and the **air density** of the dimension. So a thruster-driven craft has a top speed, where its push equals the drag: about **20 blocks a second** for a body with one reference thruster, and higher with more (speed goes as the square root of the push).

- Overworld air is the reference density; the Nether's is a little thicker, and the **End is very thin** (about a twentieth of the Overworld's): wings and rotors barely bite and drag is small, and it has no oxygen, so a thruster there burns only fuel that brings its own.
- A body with a thruster is also held to a speed ceiling of about **40 blocks a second**, so it cannot fly through walls.
- In the Overworld the air also thins with height (see [air density](Vehicles.md#air-density)), so drag falls as a craft climbs.
