# Thruster

A rocket engine for a [vehicle](Vehicles.md). It burns liquid fuel and pushes the whole body it is part of along the way it faces, at the block. Several thrusters mean more push, and one off-centre turns the craft.

It does nothing on blocks that stand in the world. Like everything on a [Lift](Vehicles.md#the-lift), it works once the vehicle is lifted into a moving body.

## Using it

- **Place it** so it faces the way you want to be pushed: set against a face, it pushes the body away from that face. Flame comes out of the back.
- **Fuel.** A bucket fills it, or a [Pipe](Pipe.md). It mixes whatever arrives **by mass** and never refuses a fluid, like an [Engine](Engine.md)'s tank; a fluid that does not burn dilutes the flame. See [Fuels](Fuels.md).
- **Right-click** to turn it on or off. The message tells you how much fuel is left.

## What it pushes with

- **Thrust follows the flame.** A hotter fuel pushes harder and goes further per mB. The standard thruster burns about 1 mB a tick for a standard fuel.
- **It needs oxygen.** A thruster burns air, which it has only in an **atmosphere**: not in the End, and not with the nozzle under water. It can instead use fuel that brings oxygen of its own (soul oil does), and then it burns anywhere, but only as hard as the fuel's own oxygen allows. With neither, it makes no thrust at all.

## Air drag and top speed

Everything that moves through air is slowed, in proportion to the **square of its speed** and the **air density** of the dimension. So a thruster-driven craft has a top speed, where its push equals the drag: about **20 blocks a second** for a body with one standard thruster, and higher with more (speed goes as the square root of the push).

- Overworld air is the reference density; the Nether's is a little thicker, and the **End is very thin** (about a twentieth of the Overworld's): wings and rotors barely bite and drag is small, and it has no oxygen, so a thruster there burns only fuel that brings its own.
- A body with a thruster is also held to a speed ceiling of about **40 blocks a second**, so it cannot fly through walls.
- A car built on a drive line drags by its own silhouette instead ([Vehicles](Vehicles.md#connecting-to-the-drive)).
