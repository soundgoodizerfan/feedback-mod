# Vehicles

> Build it in finespace, put joints where it should bend, lift it: it moves.

A vehicle is a build of [finespace](Finespace.md) and full-size blocks that a [Lift](#the-lift) turns into one moving physics body. Joints let parts of it turn, swing or slide, wheels carry it over the ground, wings carry it through the air, and every part has the weight of what it is made of.

## The parts

| Item | Use |
| --- | --- |
| **[Lift](#the-lift)** | The stand you build on. Turns what is on it into a single moving object. |
| **[Mote](Finespace.md#motes)** | A 64th of a block, cut from a whole block; what finespace is built from. |
| **[Fine Gauge](Finespace.md#fine-gauge)** | Sets single, line, box, mirror or wedge placement of motes. |
| **[Fine Wedge](Finespace.md#fine-wedge)** | A four-step ramp voxel. |
| **[Joints](#joints)** | [Rotary Bearing](Rotary_Bearing.md), [Ring Gear](Ring_Gear.md), [Large Ring Gear](Ring_Gear.md#large-ring-gear), [Ball Joint](Joints.md#ball-joint), [Locked Joint](Joints.md#locked-joint), [Slider Joint](Joints.md#slider-joint) and [Piston Joint](Joints.md#piston-joint), each placed against a block or as a voxel of finespace. |
| **[Engine](Engine.md)** | Power. Drawn at the Blueprint Table, built at the [Engineering Table](Engineering_Table.md). |
| **[Transmission](Transmission.md)** | Gears and a clutch. Drawn at the Blueprint Table, built at the [Engineering Table](Engineering_Table.md). |
| **[Clutch](Clutch.md)** | Can be a voxel of finespace that a card switches. |
| **[Thruster](Thruster.md)** | Pushes the whole body along its facing; burns liquid fuel. |
| **[Propeller](Propeller.md)** | Pulls the whole body along its facing from a turning shaft. |
| **[Gas Cell](Gas_Cell.md)** | Holds a lift gas; an [Airship](Airship.md) floats on a block of them. |
| **[Flight Sensor](Instruments.md#flight-sensor)** | Reads altitude, airspeed, pitch, roll, vertical speed or air density for a program; right-click opens the [Contraption Diagram](Instruments.md#contraption-diagram) of every force on the vehicle. |
| **[Bearing Sensor](Bearing_Sensor.md)** | In the [Gap](The_Gap.md), reads the turn, tip or distance to the End for a program. |
| **[Driver's Seat](#the-drivers-seat)** | Sit in it; your keyboard becomes the car's controls. |

An engine, transmission, propeller, jet or thruster weighs on a vehicle what the **mass** bar of its drawing on the Blueprint Table shows, and the total mass the lift reports includes it.

## The Lift

```
P P P
B T B
P S G
```

P is a Wrought Iron Plate, B is a Brass Ingot, T is Copper Tubing, S is a [Shaft](Shaft.md) and G is a Pressure Gauge. Craft it at the [Engineering Table](Engineering_Table.md) with a [hammer](Hammer.md) in the tool row, 4 machine bolts ([Fasteners](Fasteners.md)) and Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)).

It is a broad light-gray housing with a mounting block at each corner, a verdigris-teal cylinder, and a platform with brass brackets under its edges. A front console with a dark display of small blue and green indicator lights faces the player who placed it.

1. Place a **Lift** and build on top of it. Everything that touches the lift, block to block, becomes the vehicle: finespace, bearings and other joints, adapters, thrusters, seats. **It mustn't touch the ground or anything else**, or the lift complains *"Too big -- is it touching something besides the lift?"*
2. **Right-click** the lift. Everything on it becomes one object, held still: *"Lifted."*
   The message also shows the vehicle's **total mass** in Mu and its **center of mass**, as an offset from the middle of the lift's top face; it shows again each time you raise or lower the lift. If thrusters with fuel are fitted, it also gives how many times its own weight their upward push would lift.
   A body that a [joint](#joints) has turned out (a wheel on a bearing, an arm on a ball joint) rides along with the lift and keeps its angle. If the joints form closed loops, the message adds *"N closed joint loops: they will be stiff and may jitter."* The lift never refuses a loop.
3. **Right-click** again to raise it a step, and **sneak-right-click** to lower it. It goes up to 10 steps.
4. **Sneak-right-click at the bottom** (step 0) to put the vehicle back into the world as ordinary blocks, where it stands: *"Back in the world as blocks."* Every hanging body is put back first, the outermost first, each at the nearest quarter turn, and then the finespace and blocks return. If anything is in the way you get *"Something is in the way..."* and nothing moves.
5. **Break the lift** to drop the vehicle into the world, where gravity takes over. A [Wrench](Wrench.md) takes a lift apart quickly.

While the vehicle is raised, the platform and its brackets rise with the vehicle, joined to the cylinder by a bronze ram. A lift that is part of a vehicle itself cannot lift anything. A [Controller](Controller.md) card that targets a block of the vehicle follows that block when the vehicle is lifted or lowered.

### Carrying a creation

Right-click any creation with a **Lift item** and the whole thing goes *into the item*, with every block and everything in every chest: *"Picked up (N blocks)."* Hanging bodies are put back first, as when lowering. Clicking any part of a vehicle, a swinging arm included, takes the whole vehicle. The tooltip shows how big it is. Right-click the ground to set it back down on a new lift, already held. If anything is in the way you get *"No room to put it down here,"* and nothing is placed.

## Building in finespace

Cut a block into [motes](Finespace.md#motes) and place them with right-click: each mote is a voxel a quarter of a block on a side, up to 64 to a block of [Finespace](Finespace.md). Mine a voxel to take it out and get its mote back. A [Fine Gauge](Finespace.md#fine-gauge) in either hand changes how motes go in: a line, a filled box, a ramp, or mirrored across up to three planes. A block of Finespace weighs its voxels by what they are made of, and a hosted machine by its design. Bodies collide voxel by voxel, so several spinning arms fit in one block as long as their voxels never meet.

Shafts, cogs and bearings in finespace join full-size rotation through a [Rotation Adapter](Rotation_Adapter.md). [Pulleys](Pulleys.md#pulleys-in-finespace) in finespace join each other by belt. A [Differential](Differential.md) shares a drive shaft between two axles.

A modular machine such as a thruster, dynamotor or gas tank can take one voxel of existing finespace and work there at a 64th of its size; a thruster in a voxel pushes the body, and a dynamotor in a voxel couples to finespace shafts and the cable it touches. See [Machines in finespace](Finespace.md#machines-in-finespace).

## Joints

A joint is a block you place against a block or in finespace, with its **front** toward the part that should move. Use it with an **empty hand** to assemble what is joined to its front face, block to block, into a second body; use it again to put that body back. Only the voxels joined to the front face move; the rest of the block stays. Putting back snaps a body to the nearest of the 24 ways a cube can be turned; a hinge's body lands at the nearest quarter turn.

| Joint | The body |
| --- | --- |
| [Rotary Bearing](Rotary_Bearing.md) | Turns about its axis. Its role sets whether the shaft turns it, a card steers it, or a card sets it as a roll, pitch or yaw surface. |
| [Ring Gear](Ring_Gear.md) | Turns about its axis, driven from the side by a cog. |
| [Large Ring Gear](Ring_Gear.md#large-ring-gear) | Turns about its axis, driven from the side by a cog with a large cog's teeth. |
| [Ball Joint](Joints.md#ball-joint) | Swings inside a cone. |
| [Locked Joint](Joints.md#locked-joint) | Holds two bodies as one rigid piece; can anchor to a world block or dock a craft to another body; a controller card can release it to drop a stage. |
| [Slider Joint](Joints.md#slider-joint) | Slides along its front axis against a spring: suspension. |
| [Piston Joint](Joints.md#piston-joint) | Slides to a level a card sets. |

The [Ball](Joints.md#ball-joint), [Locked](Joints.md#locked-joint), [Slider](Joints.md#slider-joint) and [Piston](Joints.md#piston-joint) Joints work the same as full-size blocks and as voxels. A joint can sit on a body that another joint turned, so an arm can carry a wheel. Put the outermost body back first.

Breaking an assembled bearing, as a voxel of finespace or as a full-size block, puts its body back into the world as voxels or blocks, as a click would. The body is let go only if something blocks the put-back or it carries a body of its own. A [Joint Link](Joints.md#joint-link) holds a lifted body to a joint voxel without making a new body; a Lift refuses a vehicle that has a link until it is let go.

Full-size blocks touching an arm in finespace join it when its bearing is inside a body. Putting such an arm back lands them only at turns that put them on the grid; otherwise the message says to turn it back first.

A [Data Connector](Data_Connector.md) picks a single joint voxel, so a [Controller](Controller.md) card can brake or steer a bearing, set a piston's level or switch a [Clutch](Clutch.md#fine-clutch) in finespace.

## Floating on a magnet

A [Levitation Plate](Levitation.md#levitation-plate) in a vehicle floats above a powered [Electromagnet](Electromagnet.md).

## Wheels

A [Rotary Bearing](Rotary_Bearing.md) with role 0 whose turned body holds a [Tire Hub](Tire.md#tire-hub) is a wheel. The hub is placed on the axle, as a block or as a voxel, and a [tire](Tire.md) is fitted to it. The tire has no collision; the wheel finds the ground with a ray from the axle, and a ring of rays round the rim keeps it out of walls.

- **Radius.** The hub's size sets the wheel's radius, from the axle to the tread.
- **Springing.** The hub has no suspension, so the wheel is wherever it is mounted. Build a [Slider Joint](Joints.md#slider-joint) into the mount to make the ride smoother.
- **Grip.** The tire pushes the vehicle as hard as the tire's friction against the ground's allows. A wheel pushed past its grip slips.
- **Tire wear.** A tire wears by distance rolled on the ground, and faster while it skids past its grip. A softer tire material wears faster. Grip falls with the wear, to a floor of 30% of a new tire's. Sneak and right-click the wheel's hinge to read the tire's condition, with the joint and brake pad. Use a [tire](Tire.md) on the hinge to replace the tire, which resets the wear.
- **Brake and rolling resistance.** A card brakes the wheel through the bearing's **Brake** channel. Rolling costs speed: a soft tire and loose or sinking ground roll harder, and ice wastes none.
- **Drive.** The road's torque on the wheel loads the shaft, or helps it turn when the vehicle rolls downhill. All driven wheels on one shaft run at the same speed; a [Differential](Differential.md) lets two wheels turn at different speeds.

## Wear

A ball joint at the edge of its cone, a slider bottomed out or a piston that cannot reach its level wears its joint when it is forced against that stop for 5 seconds or more. A drive that pushes against a held brake wears the brake pad, which lowers the brake's grip. A worn joint loses condition down to a floor of 30%.

**Sneak-click** an assembled joint voxel to read *"Joint sound, brake pad marked"*: each is said in words, **sound**, **marked**, **battered** or **spent**, worse as it wears. Clicking it with a full-size [Bearing](Bearing.md) renews both.

## Attuned machines on board

An attuned machine on a moving vehicle body works as it does on the ground. It draws from a Crystal Holder, Crystal Bank or XP wire riding on the same vehicle, and effects that act on the world act where the vehicle is. See [Paying for attunements](Enchanting_Table.md#paying-for-attunements).

## Wings

Any voxel with nothing on either side along one axis and neighbors in its plane belongs to a **sheet**, and a sheet is a wing. A slab 1 to 4 voxel layers thick is a wing; a thicker slab stalls later and drags more. A column over 4 voxels thick is not a wing. Moving through air, a wing pushes the vehicle sideways to its own face (lift) and slows it (drag), both stronger the faster you go and the thinner the air. Lift grows as the sheet tilts into the wind, up to a stall angle; tilt past that and lift collapses while drag climbs, so a nose pitched too high mushes and sinks. Because the force acts at the sheet, a wing behind the center of mass steadies the nose and one off to a side rolls the vehicle. Wings work in air only. The sheets of a vehicle are found again about every 2 seconds, so a wing finished mid-flight starts to work after a short wait.

A sheet on a hinged part turns with it, so a rotary bearing set as a roll, pitch or yaw surface changes the lift on its own sheet. See [Control surfaces](#control-surfaces).

### Control surfaces

Hinged wing sheets roll, pitch or yaw a flying vehicle. A deflected surface changes the lift on its own sheet, and the force on that sheet turns the craft.

Build a wing sheet in front of a [Rotary Bearing](Rotary_Bearing.md) voxel and set the bearing's **role** before assembling: sneak-use it with an empty hand until the message names the role you want.

- **Roll surface** (aileron)
- **Pitch surface** (elevator)
- **Yaw surface** (rudder)

Then use the bearing with an empty hand to assemble the sheet as the turning part. A [Controller](Controller.md) card sets the bearing's **Angle**: link the bearing voxel to the Controller with a [Data Connector](Data_Connector.md), add a *Switch* card for it, and feed the card from the [Driver's Seat](#the-drivers-seat) keys, a sensor or any program.

## Air drag

Every moving body is slowed by the air, in proportion to its speed **squared** and the dimension's **air density**: twice as fast is four times the drag. A craft pushed by [Thrusters](Thruster.md) tops out where its push equals the drag, about 20 blocks a second for one standard thruster and higher with more. The Nether's air is a little thicker than the Overworld's and the **End's is very thin** and holds no oxygen for a thruster to burn. The [acid-cloud world's](The_Gap.md#the-acid-cloud-world-by-rocket) is far thicker than the Overworld's and holds no oxygen either. A body with a thruster is held to about 40 blocks a second so it cannot fly through walls.

### Air density

Air thins with height in the Overworld, and wings, [propellers](Propeller.md) and the lift of [gas cells](Gas_Cell.md) all weaken with it, and fade to nothing by y 312, where a vehicle leaves for the [Gap](The_Gap.md). Oxygen is a share of the air: a person breathes the density times that share.


| Height | Density |
| --- | --- |
| y 62 | 1.0 |
| y 128 | 0.75 |
| y 192 | 0.5 |
| y 256 | 0.3 |
| y 288 | 0.1 |
| y 312 | 0 |

## The Driver's Seat

```
R S R
  W
  L
```

R is a Wrought Iron Rod, S is a Shaft, W is any Wool and L is any Wooden Slab.

The seat is a plank base with a wool cushion at the back and a bronze column at the front carrying a steering wheel. It faces the way you look when you place it, so the wheel is ahead of the sitter.

Place it on the vehicle and right-click it to sit. **While you're seated, the keyboard is the seat's**: every key you press goes to the seat instead of the game (sneak, Escape, chat and the F-keys still work). Each key held **ramps from 0 to 1 over a second**, and falls back the same way when you let go: tap for a little, hold for full.

The seat drives nothing on its own. Link it to a [Controller](Controller.md):

1. Link the seat to the Controller with a [Data Connector](Data_Connector.md).
2. On the [Programmer](Programming.md), add an *Input* card with signal *Control key* and kind *Driver seat*. Its key row says which key it reads (*"key: W"*); click it and press a key to change it.
3. Wire that to a *Switch* card of kind *Engine*. A number sets the engine's **throttle**.

Use *Math* cards in between to scale or combine keys (two keys and a subtract make a steering level: `D - A`).

A card that points at a part of a vehicle (an engine, a seat, a joint voxel) follows that block when the vehicle is lifted or lowered, so the same program works wherever the vehicle stands.

### Steering and surfaces

Link a *Switch* card to a [Rotary Bearing](Rotary_Bearing.md#roles) voxel and a number sets its angle: a steering bearing turns the front wheels, a control surface deflects for roll, pitch or yaw. A piston takes a level and a wheel's bearing takes the brake.

## Autopilot

An autopilot is a program you write, running on an [Electric Controller](Electric_Controller.md) or a [Computer](Electric_Controller.md#computer) that sits in the vehicle. Link the [Flight Sensor](Instruments.md#flight-sensor), the engines, bearings and surfaces to the Controller, as for the Driver's Seat, and ask for them with Input and Switch cards; write it in cards or in the Computer's [text view](Programming.md#text-form). Nothing is stabilized for you: holding a heading or a height is a program.

- **Program size.** A Controller riding in a vehicle runs a program of **6 cards** with no circuit, and **5 more for each element on its [circuit](Basic_Electronic_Circuit.md)**, up to 40. Card Racks do not count on a vehicle; a Controller on the ground keeps them.
- **The circuit does the rest.** Its speed sets how often the program steps and its noise how much readings wobble; a [Resonator](Resonator.md) steadies both. A circuit with a [tube](Vacuum_Tube.md) draws its heater from the vehicle's own line.
- **Waypoints.** A [Lattice](Lattice.md) adds variables. Write the Flight Sensor's *position east* and *position south* readings into two variables when a trigger turns yes, and compare the readings against them to know where you have been.

## Shove and carry

With the Brute skill [Shove](Skills.md#brute) slotted to a cast key, you put your hands on a vehicle body and keep the key down. What your hands do depends on the **mass ratio**: the body's mass over yours plus your worn armor's.

- **At or under your carry ratio** (1 to start, 1.5 with Heavy lift, 2 with Deadlift): you hold the body up like a prop, at a point in front of the camera. Holding up more weight costs more stamina.
- **Over it**: you shove the body along the ground in the direction you look while you walk or sprint forward, or drag it toward you while you step back. A shove delivers your mass times your speed at the spot you touched, and the body pushes back on you equally. Nothing in your hands decides whether it rolls: the wheels' grip and brakes resist, so a car with its brakes set stays put and a car on slick ground slides. Stamina is spent for the work you actually deliver, so leaning on a braked car costs almost nothing and moves nothing.

You let go when you release the key, stand on the body, run out of stamina, or are pulled too far from the spot you took hold of. Bodies lighter than a scrap, and blocks tagged `feedback:unshovable`, cannot be taken hold of. When the key finds nothing to take hold of, the action bar says why: nothing in reach, not a loose body (part of the ground), too light, or you are standing on it. Pushing a body earns Brute points, fewer the more you repeat it on the same body.
