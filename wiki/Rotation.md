# Rotation

Most machines in Feedback run on **rotation**: spinning shafts carrying power from a source to the machines that use it.

## The basics

- A **source** makes rotation: [Water Wheel](Water_Wheel.md), [Steam Engine](Steam_Engine.md), [Engine](Engine.md), [Hand Crank](Hand_Crank.md), [Motor](Motor.md).
- **Shafts**, [cogs](Cogs.md), [gearboxes](Cogs.md#gearbox) and [pulleys](Pulleys.md) carry it.
- **Machines** use it: [hammers](Mechanical_Hammer.md) (through a [Crank Linkage](Crank_Linkage.md)), [presses](Mechanical_Press.md), [mills](Rolling_Mill.md), [lathes](Lathe.md) and so on.

Everything touching along a shaft line is one **network**, and the whole network turns together.

## Power: Su

Every source can carry a certain load, and every machine adds load. Both are measured in **Su**. A machine's load is printed on it.

| Source | Su | Speed |
| --- | --- | --- |
| [Hand Crank](Hand_Crank.md) | 12 | 32 RPM |
| [Steam Engine](Steam_Engine.md) | 64 | 16 RPM |
| [Water Wheel](Water_Wheel.md) | 256 | 4 RPM per side of flowing water, up to 16 |
| [Engine](Engine.md) | by design | by design |

| Machine | Su |
| --- | --- |
| [Controller](Controller.md) | 2 |
| [Pump](Pump.md) | 8 |
| [Wire Drawer](Wire_Drawer.md) | 30 |
| [Drill Press](Drill_Press.md) | 35 |
| [Powered Bellows](Powered_Bellows.md) | 40 |
| [Lathe](Lathe.md) | 40 |
| [Grinding Wheel](Grinding_Wheel.md) | 45 |
| [Rolling Mill](Rolling_Mill.md) | 50 |
| [Mechanical Hammer](Mechanical_Hammer.md) | 80 |
| [Mechanical Press](Mechanical_Press.md) | 90 |

A water wheel carries three hammers (240 Su) but not four.

Sources on the same network add their capacity together.

## When it's too much

A network carrying more than its sources can give is **overloaded**. It isn't driven any more, and it **winds down** to a stop under its own load. Nothing breaks.

You'll know because the **sources smoke and creak**: every so often when the network is close to its limit, and more insistently when it's overloaded. Jade shows *Straining* (above 90% of capacity) or *Overloaded* on any part of the network. A straining network takes much longer to come up to speed, because only the spare 10% is left to accelerate it. Take some load off, or add another source.

## Speed and momentum

A network doesn't jump to speed. It **spins up** using whatever spare power it has, so a lightly loaded network gets up to speed fast and a heavily loaded one takes a while. Heavy things on the line (water wheels, steam engines, big cogs) make it slower to change speed in both directions.

Cut the power and a network **coasts** down instead of stopping dead. Machines on it keep working, a little, until it stops. That matters when you're trying to stop a hammer at exactly the right moment.

Jade describes speed in words: *Stopped*, *Turning slowly* (under 8 RPM), *Turning*, *Spinning fast* (24 RPM and up).

## Friction

Every shaft, cog and gearbox costs a little power, and more the faster it spins:

| Part | Friction per RPM |
| --- | --- |
| [Bearing](Bearing.md) | 0.04 Su |
| [Shaft](Shaft.md), Small [Cog](Cogs.md), [Pulley](Pulleys.md) | 0.1 Su |
| Large Cog | 0.15 Su |
| [Gearbox](Cogs.md#gearbox), [Flywheel](Flywheel.md) | 0.2 Su |

So twenty shafts at 16 RPM cost 32 Su, half a steam engine. A network speeds up until its spare power runs out, so a long run doesn't stall; it just turns slower. A stopped shaft costs nothing. Gearing a branch *up* to a higher speed makes its friction costlier still. Distance itself is free: every block in the network costs the same wherever the machine is.

## Gearing

Gearing trades speed for force. Slow a line down with [cogs](Cogs.md) and each turn pushes harder; speed it up and each turn pushes softer. For the hammer that's the difference between a blow that shapes steel and one that does nothing. See [Crank Linkage](Crank_Linkage.md#gearing).

## Stopping things

- A [Clutch](Clutch.md) disconnects everything past it.
- An [Overspeed Trip](Overspeed_Trip.md) disconnects automatically above 48 RPM.
- A [Flywheel](Flywheel.md) smooths a line out and makes it coast longer.
- A [Timer](Timer.md) or [Controller](Controller.md) can work a clutch for you.

Remember that disconnecting doesn't brake anything. The far side coasts down.
