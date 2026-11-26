# Joints

A joint is a block that lets a part of a build turn, swing or slide against the rest. The [Ball Joint](#ball-joint), [Locked Joint](#locked-joint), [Slider Joint](#slider-joint) and [Piston Joint](#piston-joint) are placed against a block or as a voxel of [finespace](Finespace.md), with their front toward the part that should move; the [Joint Link](#joint-link) closes chains of them into loops. Use a placed joint with an **empty hand** to assemble what is joined to its front face into a second body, and use it again to put the body back. The rules every joint shares, and the joints that turn ([Rotary Bearing](Rotary_Bearing.md), [Ring Gear](Ring_Gear.md)), are on [Vehicles](Vehicles.md#joints).

All four joints are crafted at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and need Mechanical Treatise I: Engines, Flywheels and Dynamotors ([Treatise](Treatise.md)). A body put back lands at the nearest of the 24 ways a cube can be turned. Put back the outermost body first when joints are chained.

## Ball Joint

Lets one body swing in every direction inside a cone. It is the joint for a suspension arm, a turret or a leg.

```
I I
 B 
I I
```

I is a Wrought Iron Plate and B is a [Rotary Bearing](Rotary_Bearing.md). It takes 4 screws ([Fasteners](Fasteners.md)).

1. Place it against a block, or put it in finespace, with its front toward the part that should swing.
2. **Sneak-use** it with an empty hand to choose the cone: **narrow** (20 degrees), **stock** (45 degrees) or **wide** (90 degrees). The message reads *"Choice N of 3."* Choose before assembling.
3. **Use it with an empty hand** to assemble everything joined to its front face into a second body that swings about the joint.
4. **Use it again** to put the body back.

The body swings freely inside the cone and is stopped at its edge. A [Lift](Vehicles.md#the-lift) carries the swung body with the vehicle and keeps its angle. A body forced against the edge of the cone for 5 seconds or more wears the joint ([Wear](Vehicles.md#wear)).

## Locked Joint

Holds two bodies together as one rigid piece. It splits a build into parts that can be put back separately, or anchors a build to a block in the world.

```
I B I
R
```

I is a Wrought Iron Plate, B is a [Rotary Bearing](Rotary_Bearing.md) and R is a Wrought Iron Rod. It takes 2 screws ([Fasteners](Fasteners.md)).

1. Place it against a block, or put it in finespace, with its front toward the part to split off.
2. **Use it with an empty hand** to assemble what is joined to its front face into a second body, held rigidly to the first.
3. **Use it again** to put the body back.

Placed against a world block, it anchors the build to that block.

### Docking

With nothing in front of its face, but another body touching the cell in front of it, **use it with an empty hand** to dock: the joint holds that body as it stands, rigid, at the pose it has. The message reads *"Docked: it holds the body in front of it"*. A craft made fast to a station or to a second craft this way flies as one with it. The docked body is never turned back into blocks.

**Use the joint again** to let go; the message reads *"Let go"* and a small spring kick parts the two bodies. A Release channel set to one half or more ([Staging](#staging)) lets go the same way. A docked joint is kept when the world is saved and holds again on reload.

### Staging

Link a Locked Joint to a Controller with a [Data Connector](Data_Connector.md) and it has a **Release** channel. When a card sets the channel to one half or more, the joint lets go: a spring pushes the two bodies apart along the joint's front, an equal push on each, and the held body flies free as a body of its own. The joint is empty again and can be assembled anew. Put one between a [Thruster](Thruster.md) stage and the rest of a rocket to drop the spent stage in flight.

## Slider Joint

Lets a body slide along its front axis against a spring. It is a suspension: a wheel on a slider rides over bumps and rebounds, since a [Tire Hub](Tire.md#tire-hub) has no suspension of its own.

```
I
B
S
```

I is a Wrought Iron Plate, B is a [Rotary Bearing](Rotary_Bearing.md) and S is a [Shaft](Shaft.md). It takes 2 screws ([Fasteners](Fasteners.md)).

1. Place it against a block, or put it in finespace, with its front along the line the body should slide on.
2. **Sneak-use** it with an empty hand to choose the spring: **soft**, **stock** or **stiff**. The message reads *"Choice N of 3."* Choose before assembling.
3. **Use it with an empty hand** to assemble everything joined to its front face into a second body that slides.
4. **Use it again** to put the body back.

The body rests where the spring holds it. A load pushes it back along the axis and the spring pushes it out again. A soft spring squats further and bounces slowly; a stiff one holds the body higher and tighter. A slider held bottomed out for 5 seconds or more wears the joint ([Wear](Vehicles.md#wear)).

### Spring from a metal

A Slider Joint plus four wires of one metal, in any arrangement, makes a slider with that metal as its spring. The tooltip shows the spring. When the slider is placed, the metal's yield strength sets the band: under 4 is soft, 12 and over is stiff, anything between is stock. Sneak-use still cycles the band.

## Piston Joint

Slides a body along its front axis to a level a [Controller](Controller.md) card sets. It is a linear actuator: a lift, a ram, a retracting leg.

```
S
B
I
```

S is a [Shaft](Shaft.md), B is a [Rotary Bearing](Rotary_Bearing.md) and I is a Wrought Iron Plate. It takes 2 screws ([Fasteners](Fasteners.md)).

1. Place it against a block, or put it in finespace, with its front along the line the body should slide on.
2. **Use it with an empty hand** to assemble everything joined to its front face into a second body that slides.
3. **Use it again** to put the body back.
4. Link the piston to a Controller with a [Data Connector](Data_Connector.md) and add a *Switch* card on its **Piston level** channel. A number from -1 to 1 sets the level; the travel is 0.5 block in all.

The piston holds the level the card sets against a load. It wears the joint if a load keeps it from reaching the level for 5 seconds or more ([Wear](Vehicles.md#wear)). A [Slider Joint](#slider-joint) slides by a spring instead.

## Joint Link

A reusable tool that ties a joint voxel to a body that is already lifted, without making a second body. It closes chains of joints into loops.

```
R
B
R
```

R is a Wrought Iron Rod and B is a [Rotary Bearing](Rotary_Bearing.md). It takes 2 screws ([Fasteners](Fasteners.md)) and the same Treatise as the joints.

1. Click a joint voxel that has nothing in front of its front face. A chip left of the crosshair says *"Linking from"* and the joint's name until the link is made, fails or is cancelled.
2. Click a voxel of another lifted body within 1.5 blocks of that face.

That body is held to the joint, as a body assembled from the blocks in front of it would be. Nothing is made and nothing is used up. **Right-click the joint voxel with an empty hand** to let the body go.

A chain of joints closed this way is a loop. The Lift reports loops when it lifts the vehicle, and refuses a vehicle that has a link until the link is let go.
