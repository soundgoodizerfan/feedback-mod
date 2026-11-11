# Vehicles

> Build it out of micro-blocks, put joints where it should bend, lift it: it moves.

A vehicle is a build of [micro cells](Micro_Core.md) and full-size blocks that a [Lift](#the-lift) turns into one moving physics body. Joints let parts of it turn, swing or slide, wheels carry it over the ground, wings carry it through the air, and every part has the weight of what it is made of.

## The parts

| Item | Use |
| --- | --- |
| **[Lift](#the-lift)** | The stand you build on. Turns what is on it into a single moving object. |
| **[Micro Core](Micro_Core.md)** | Builds in voxels, a 64th of a block each, with line, box and mirror tools. |
| **[Micro Wedge](Micro_Wedge.md)** | A four-step ramp voxel. |
| **[Joints](#joints)** | [Rotary Bearing](Rotary_Bearing.md), [Ring Gear](Ring_Gear.md), [Ball Joint](Ball_Joint.md), [Locked Joint](Locked_Joint.md), [Slider Joint](Slider_Joint.md) and [Piston Joint](Piston_Joint.md), used as micro-blocks. |
| **[Engine](Engine.md)** | Power. Drawn at the Blueprint Table, built at the [Smithing Table](Smithing_Table.md). |
| **[Transmission](Transmission.md)** | Gears and a clutch. Drawn at the Blueprint Table, built at the [Smithing Table](Smithing_Table.md). |
| **[Clutch](Clutch.md)** | Can be a micro voxel that a card switches. |
| **[Thruster](Thruster.md)** | Pushes the whole body along its facing; burns liquid fuel. |
| **[Propeller](Propeller.md)** | Pulls the whole body along its facing from a turning shaft. |
| **[Gas Cell](Gas_Cell.md)** | Holds a lift gas; an [Airship](Airship.md) floats on a block of them. |
| **[Flight Sensor](Instruments.md#flight-sensor)** | Reads altitude, airspeed, pitch or roll for a program. |
| **[Driver's Seat](#the-drivers-seat)** | Sit in it; your keyboard becomes the car's controls. |

## The Lift

```
PPP
R R
R R
```

P is a Wrought Iron Plate and R is a Wrought Iron Rod.

1. Place a **Lift** and build on top of it. Everything that touches the lift, block to block, becomes the vehicle: micro cells, bearings and other joints, adapters, thrusters, seats. **It mustn't touch the ground or anything else**, or the lift complains *"Too big -- is it touching something besides the lift?"*
2. **Right-click** the lift. Everything on it becomes one object, held still: *"Lifted."*
   The message also shows the vehicle's **total mass** in Mu and its **center of mass**, as an offset from the middle of the lift's top face; it shows again each time you raise or lower the lift. If thrusters with fuel are fitted, it also gives how many times its own weight their upward push would lift.
   A body that a [joint](#joints) has turned out (a wheel on a bearing, an arm on a ball joint) rides along with the lift and keeps its angle. If the joints form closed loops, the message adds *"N closed joint loops: they will be stiff and may jitter."* The lift never refuses a loop.
3. **Right-click** again to raise it a step, and **sneak-right-click** to lower it. It goes up to 10 steps.
4. **Sneak-right-click at the bottom** (step 0) to put the vehicle back into the world as ordinary blocks, where it stands: *"Back in the world as blocks."* Every hanging body is put back first, the outermost first, each at the nearest quarter turn, and then the micro cells and blocks return. If anything is in the way you get *"Something is in the way..."* and nothing moves.
5. **Break the lift** to drop the vehicle into the world, where gravity takes over. A [Wrench](Wrench.md) takes a lift apart quickly.

A lift that is part of a vehicle itself cannot lift anything. A [Controller](Controller.md) card that targets a block of the vehicle follows that block when the vehicle is lifted or lowered.

### Carrying a creation

Right-click any creation with a **Lift item** and the whole thing goes *into the item*, with every block and everything in every chest: *"Picked up (N blocks)."* Hanging bodies are put back first, as when lowering. Clicking any part of a vehicle, a swinging arm included, takes the whole vehicle. The tooltip shows how big it is. Right-click the ground to set it back down on a new lift, already held. If anything is in the way you get *"No room to put it down here,"* and nothing is placed.

## Building with micro-blocks

Use a [Micro Core](Micro_Core.md) on a block and a **micro cell** appears, holding voxels a quarter of a block on a side: up to 64 to a cell. A voxel costs a 64th of a block. Hold a block and right-click to fill a voxel; mine a voxel to take it out. A second Micro Core in the other hand changes how the block in your main hand is placed: a line, a filled box, or mirrored across up to three planes. A cell weighs its voxels by what they are made of. Bodies collide voxel by voxel, so several spinning arms fit in one block as long as their voxels never meet.

Micro shafts, cogs and bearings join full-size rotation through a [Rotation Adapter](Rotation_Adapter.md). A micro pulley and separate outputs from a rotation adapter are not available.

## Joints

A joint is a block you place as a micro-block, with its **front** toward the part that should move. Use it with an **empty hand** to assemble what is joined to its front face, block to block, into a second body; use it again to put that body back. Only the voxels joined to the front face move; the rest of the cell stays. Putting back snaps a body to the nearest of the 24 ways a cube can be turned; a hinge's body lands at the nearest quarter turn.

| Joint | The body |
| --- | --- |
| [Rotary Bearing](Rotary_Bearing.md) | Turns about its axis. Its role sets whether the shaft turns it, a card steers it, or a card sets it as a roll, pitch or yaw surface. |
| [Ring Gear](Ring_Gear.md) | Turns about its axis, driven from the side by a cog. |
| [Ball Joint](Ball_Joint.md) | Swings inside a cone. |
| [Locked Joint](Locked_Joint.md) | Holds two bodies as one rigid piece; can anchor to a world block. |
| [Slider Joint](Slider_Joint.md) | Slides along its front axis against a spring: suspension. |
| [Piston Joint](Piston_Joint.md) | Slides to a level a card sets. |

A joint placed as a full-size block does nothing: *"It only works as a micro-block: put it in a micro cell."* A joint can sit on a body that another joint turned, so an arm can carry a wheel. Put the outermost body back first.

A [Data Connector](Data_Connector.md) picks a single joint voxel, so a [Controller](Controller.md) card can brake or steer a bearing, set a piston's level or switch a micro [Clutch](Clutch.md#micro-clutch).

## Wheels

A [Rotary Bearing](Rotary_Bearing.md) with role 0 whose turned body contains **tire stock** is a wheel. Black wool is tire stock. The voxels of the tire have no collision; the wheel finds the ground with a ray from the axle.

- **Radius.** The wheel's radius is the distance from the axle to the farthest tire voxel.
- **Springing.** The tire is a spring and a damper: the body squats on it and rebounds.- **Grip.** The tire pushes the vehicle as hard as the tire's friction against the ground's allows. A wheel pushed past its grip slips.
- **Brake and rolling resistance.** A card brakes the wheel through the bearing's **Brake** channel. A soft tire rolling on soft ground costs speed.
- **Drive.** The road's torque on the wheel loads the shaft, or helps it turn when the vehicle rolls downhill. All driven wheels on one shaft run at the same speed; there is no differential.
- **Walls.** Rays around the rim keep the wheel out of walls, steps and other vehicles.

Brake pads do not wear.

## Wings

Any voxel with nothing on either side along one axis and neighbors in its plane belongs to a **sheet** one voxel thick, and a sheet is a wing. Moving through air, it pushes the vehicle sideways to its own face (lift) and slows it (drag), both stronger the faster you go and the thinner the air. Lift grows as the sheet tilts into the wind, up to a stall angle; tilt past that and lift collapses while drag climbs, so a nose pitched too high mushes and sinks. Because the force acts at the sheet, a wing behind the center of mass steadies the nose and one off to a side rolls the vehicle. Wings work in air only. The sheets of a vehicle are found again about every 2 seconds, so a wing finished mid-flight starts to work after a short wait.

A sheet on a hinged part turns with it, so a rotary bearing set as a roll, pitch or yaw surface changes the lift on its own sheet. See [Control Surfaces](Control_Surfaces.md).

## Air drag

Every moving body is slowed by the air, in proportion to its speed **squared** and the dimension's **air density**: twice as fast is four times the drag. A craft pushed by [Thrusters](Thruster.md) tops out where its push equals the drag, about 20 blocks a second for one standard thruster and higher with more. The Nether's air is a little thicker than the Overworld's and the **End's is very thin** and holds no oxygen for a thruster to burn. A body with a thruster is held to about 40 blocks a second so it cannot fly through walls.

### Air density

Air thins with height in the Overworld, and wings, [propellers](Propeller.md) and the lift of [gas cells](Gas_Cell.md) all weaken with it:

| Height | Density |
| --- | --- |
| y 62 | 1.0 |
| y 128 | 0.75 |
| y 192 | 0.5 |
| y 256 | 0.3 |
| y 320 | 0.15 |

## The Driver's Seat

```
W
S
```

W is any Wool and S is any Wooden Slab.

Place it on the vehicle and right-click it to sit. **While you're seated, the keyboard is the seat's**: every key you press goes to the seat instead of the game (sneak, Escape, chat and the F-keys still work). Each key held **ramps from 0 to 1 over a second**, and falls back the same way when you let go: tap for a little, hold for full.

The seat drives nothing on its own. Link it to a [Controller](Controller.md):

1. Right-click the seat with a [Data Connector](Data_Connector.md).
2. On the [Programmer](Programming.md), link an *Input* card to it. Its second row says which key it reads (*"key: W"*); click it and press a key to change it.
3. Wire that to a *Switch* card linked to an [Engine](Engine.md). A number sets the engine's **throttle**.

Use *Math* cards in between to scale or combine keys (two keys and a subtract make a steering level: `D - A`).

A card that points at a part of a vehicle (an engine, a seat, a joint voxel) follows that block when the vehicle is lifted or lowered, so the same program works wherever the vehicle stands.

### Steering and surfaces

Link a *Switch* card to a [Rotary Bearing](Rotary_Bearing.md#roles) voxel and a number sets its angle: a steering bearing turns the front wheels, a control surface deflects for roll, pitch or yaw. A piston takes a level and a wheel's bearing takes the brake.

## Shove and carry

With the Brute skill [Shove](Skills.md#brute) slotted to a cast key, you put your hands on a vehicle body and keep the key down. What your hands do depends on the **mass ratio**: the body's mass over yours plus your worn armor's.

- **At or under your carry ratio** (1 to start, 1.5 with Heavy lift, 2 with Deadlift): you hold the body up like a prop, at a point in front of the camera. Holding up more weight costs more stamina.
- **Over it**: you shove the body along the ground in the direction you look while you walk or sprint forward, or drag it toward you while you step back. A shove delivers your mass times your speed at the spot you touched, and the body pushes back on you equally. Nothing in your hands decides whether it rolls: the wheels' grip and brakes resist, so a car with its brakes set stays put and a car on slick ground slides. Stamina is spent for the work you actually deliver, so leaning on a braked car costs almost nothing and moves nothing.

You let go when you release the key, stand on the body, run out of stamina, or are pulled too far from the spot you took hold of. Bodies lighter than a scrap, and blocks tagged `feedback:unshovable`, cannot be taken hold of. Pushing a body earns Brute points, fewer the more you repeat it on the same body.
