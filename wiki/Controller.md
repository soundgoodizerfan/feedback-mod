# Controller

A mechanical logic box. It reads sensors, compares numbers, and switches things **on or off**, following a program punched into a **Punch Card**.

It's the step up from a [Timer](Timer.md) or [Bimetallic Strip](Bimetallic_Strip.md): it can watch more than one thing, compare against any number you like, and switch several things at once.

## Crafting

```
P C P
P S P
```

P is a [Copper Plate](Mechanical_Hammer.md), C is a Redstone Comparator and S is a [Shaft](Shaft.md).

## Setting it up

1. Write a program onto a Punch Card at the [Programmer](Programmer.md).
2. **Right-click the Controller with the printed card** to put it in. Right-click with an empty hand to take it out.
3. **Connect the Controller to a turning shaft.** It's mechanical, and it only thinks while it's being turned. It connects on any side and takes **2 Su**; even a [Hand Crank](Hand_Crank.md) (12 Su) is enough while you're turning it.

If the shaft stops, the controller stops making decisions, and everything it controls **stays as it was last set**.

## What it can read

Anything you can link as a sensor:

- [Temperature Sensor](Temperature_Sensor.md) on a crucible or Improved Furnace
- [Current Sensor](Current_Sensor.md) on a cable
- a [Lathe](Lathe.md) (how much it has cut off its workpiece)
- anything it can switch (below): **whether it's on**, as a yes/no. See [Holding a band](#holding-a-band).
- a [Driver's Seat](Vehicles.md#the-drivers-seat): how far a key is pressed

## What it can switch

Anything that turns on and off:

- [Clutch](Clutch.md) and [Overspeed Trip](Overspeed_Trip.md)
- [Valve](Valve.md)
- [Damper](Damper.md)
- [Engine](Engine.md)
- [Breaker](Breaker.md)

A **true** signal switches the target **on**: clutch engaged, valve open, damper open, engine running, breaker closed. **False** switches it **off**.

## It's a switch, not a dial

A controller can only turn things fully on or fully off. There's no "half open". If your crucible swings back and forth around the target, that isn't a programming problem. Fix it physically: a heavier vessel, insulation, less air per stroke. See [Bimetallic Strip](Bimetallic_Strip.md#it-overshoots-and-thats-expected).

The one exception is **vehicles**. A *Math* card's number can set an engine's throttle or (later) a car's steering, because only vehicle parts take a number. A number sent to a clutch or valve does nothing.

## Holding a band

A single comparison ("bellows on below 1380") flips the switch every time the reading crosses the line, which can be many times a second. That's hard on anything that wears when switched under load, like a [Breaker](Breaker.md).

The fix is a **band** (a deadband): turn on below the bottom, turn off above the top, and in between **leave it as it is**. The controller has no memory, but the switch does, so read it back:

> **on** if (temperature < 1360) **OR** (temperature < 1400 **AND** the bellows clutch is already on)

Link a *Read Sensor* card to the clutch itself and it reads yes while the clutch is engaged. Inside the band the clutch keeps doing whatever it was doing.
