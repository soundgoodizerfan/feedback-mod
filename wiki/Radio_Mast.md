# Radio Mast

The base end of a radio link to a [robot](Robots.md). Paired with a robot that carries a Robot Radio, it lets an ordinary [Controller](Controller.md) read the robot's senses and hold its actions on or off from a distance, with no wire and no tether.

## Crafting

```
   W  
 W P W
 P P P
```

W is [Copper Wire](Wire_Drawer.md) and P is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Robotics Treatise II: Robots ([Treatise](Treatise.md)).

## Using it

The mast is a sink on an [electrical](Electricity.md) line: feed it and it listens. Unpowered, it hears nothing.

1. Build a robot with a **Robot Radio** part (see [Robots](Robots.md#the-radio)).
2. Right-click the mast with the [Data Connector](Data_Connector.md) to select it.
3. Use the Data Connector on the robot. The bar says *"Mast paired with the robot"*. A robot with no radio says *"This robot has no radio"*.
4. Select the mast with the Data Connector again and use it on the Controller, as for any other node. Cards then ask for kind *Mast*.

A mast pairs with one robot at a time.

## What a Controller does with it

- An **Input** card of kind *Mast* reads one of the robot's senses. Click the card's channel row to pick which: *Blocked ahead*, *Breakable ahead*, *Items ahead*, *Cargo full*, *Battery charge*, *Distance ahead*, *Hardness ahead*, *Heading*, *Beacon near*, *Dock ahead*, *Position east*, *Position south*, *Height above start* and *Laser ahead: beam reach*. They mean what they do on the robot itself ([Robots](Robots.md#programming-a-robot)); a sense the robot's parts do not give reads zero.
- A **Switch** card of kind *Mast* holds one of the robot's actions on while the card is on and lets it go while the card is off. Click the card's channel row to pick the action: roll forward, turn left, turn right, break, place, pick up, drop, put into or take from a container, use the held item, switch a machine, climb and descend.

The robot works its actions as a hand-programmed robot does, so one base Controller can drive a free robot.

## Range and noise

The link reaches farther the more current the mast is fed, up to a limit that rises with the line's band ([Electricity](Electricity.md#bands-and-amps)). A starved mast reaches less.

Out of range, or in noise, the mast hears nothing: every reading is zero and every action the Controller held is let go. Nothing is refused; the robot just stops hearing.

**Sparks drown the link.** An [Arc Head](Arc_Furnace.md#the-arc-head) within a few blocks of the robot or of the mast is noise.

A mast boxed in on four or more sides by lead-lined chests does not hear noise at its own end. The robot's end stays open, so a robot working beside an arc is still deaf.

## Tips and automation

Read *Battery charge* and have the card for *Roll forward* go off below a threshold, so the robot stops in range of a [Robot Dock](Robots.md#robot-dock) before its battery runs flat. Read *Distance ahead* to keep it from running into a wall.

See [Controller](Controller.md#reading-another-controllers-variables) for how a second Controller reads the first one's variables, so several robots share state.
