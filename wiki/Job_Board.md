# Job Board

A board of sixteen slots that a fleet of [robots](Robots.md) shares over radio. Each slot is free, claimed or done and holds a figure, so robots can split work between them without a [Controller](Controller.md) in the loop.

## Crafting

```
   W  
 P E P
 P P P
```

W is [Copper Wire](Wire_Drawer.md), E is any [circuit](Basic_Electronic_Circuit.md) and P is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a driver in the tool row and 2 [screws](Fasteners.md), holding Robotics Treatise II: Robots ([Treatise](Treatise.md)). The circuit stays in the board.

## Using it

The board is a sink on an [electrical](Electricity.md) line: feed it and it listens. Unpowered, it hears nothing. A robot needs a **Robot Radio** part ([Robots](Robots.md#the-radio)) to use it. Nothing is paired: a robot the board hears is on it.

Hover with Jade to see how many robots it hears out of how many are in reach, and which robot holds each claimed slot.

## How it works

Each slot is **free**, **claimed** by one robot, or **done**, and carries a figure.

- A robot with the board's slot chosen reads that slot's state and figure, and steps to the next slot with *Next slot*.
- A robot holds a claim by keeping *Claim* on. The claim lapses about 30 seconds after the robot stops, so a lost or dead robot holds nothing.
- *Done* marks the robot's own claimed slot done.
- *Post* puts the chosen slot up as free, with the figure set to the number of cargo stacks the robot carries.

The channels are listed under [Fleets](Robots.md#fleets).

### Range, noise and capacity

The board reaches as far as the current it is fed, as a [Radio Mast](Radio_Mast.md#range-and-noise) does, and noise drowns a robot the same way; lead-lined chests around the board shield its end as they do a mast's.

It hears only as many robots as the current pays for: two per amp, nearest first. A faster circuit in the board hears more on the same current. A robot it does not hear reads zero on every board channel and carries on with its own program.

## Tips and automation

A base Controller reads and posts slots without a robot:

- A **Sensor** card on channels 0 to 15 reads each slot's state (0 free, 1 claimed, 2 done); channels 16 to 31 read each slot's figure.
- An **Actuator** card on channel *n* posts slot *n* while it is on and clears it when it goes off. A claimed slot is never overwritten.

See also [Robots](Robots.md#fleets) for programs that work a board.
