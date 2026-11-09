# Data Connector

The tool that tells a [Programmer](Programming.md) **which** sensor or block a card refers to.

> **Recipe:** a copper wire on top of two iron rods, in a column.

## Using it

1. **Right-click** a sensor or switchable block: a [Temperature Sensor](Instruments.md#temperature-sensor) or [Current Sensor](Instruments.md#current-sensor) face, a [Lathe](Lathe.md), a [Clutch](Clutch.md), [Valve](Valve.md), [Damper](Damper.md), [Breaker](Breaker.md), [Engine](Engine.md) or a [Driver's Seat](Vehicles.md#the-drivers-seat). You'll see *"Node selected."*
2. Open the Programmer and click the **link** button on the card that should use it.

For sensors attached to a face, click **the face the sensor is on**.

## Linking nodes

An [Electric Controller](Electric_Controller.md) or [Computer](Electric_Controller.md#computer) reads a sensor only through a path of linked nodes. Right-click the first node, then right-click the second: the two are linked. A chain of links works as a path, so a sensor can reach a Computer through other linked nodes.

While you hold the connector in your main hand after selecting the first node, a wire stretches from that node to wherever you aim. It is blue while it hangs in the air, turns green when it snaps to a node under your crosshair, and turns red when the distance is longer than the longest wire reaches.

Each link costs fine wire from your inventory, one piece per block of distance. Creative players pay nothing. The wire is taken from the stack you carry when the link is made, and the link remembers its grade. If you carry too little, the bar message says how much wire the link needs; if the distance is beyond every wire, it says no wire reaches that far.

| Wire | Reach | Reading it carries |
| --- | --- | --- |
| Copper Fine Wire (poor grade) | up to 8 blocks | rounded to one significant figure |
| Redstone-Chromium Fine Wire (good grade) | up to 32 blocks | exact |

A link uses copper wire when it reaches and you have enough, otherwise redstone-chromium. A path is as good as its worst wire.

Redstone-chromium wire quenches: if either end of a good line carries more than 3 links, the line goes dark for 10 seconds and then recovers undamaged. A reader with no working path reads 0.

The mechanical [Controller](Controller.md) and the blocks it switches ([Valve](Valve.md), [Clutch](Clutch.md) and the like) are not linked this way and need no wire.

While you hold the Data Connector, connected sensors and their links are drawn in the world, outlined through walls, so you can see what's wired to what.
