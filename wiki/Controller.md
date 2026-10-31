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

1. Write a program onto a Punch Card at the [Programmer](Programming.md).
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

## It controls energy, not machines

A controller never runs a machine. It can only connect to the things that let energy through (a clutch passes rotation, a valve passes air or steam, a breaker passes current) and open or close them. The hammer, the crucible and the bellows don't know it exists.

In a factory those are all **on/off switches**, so the controller's whole answer is on or off. There's no "half open". If your crucible swings back and forth around the target, that isn't a programming problem. Fix it physically: a heavier vessel, insulation, less air per stroke. See [Bimetallic Strip](Bimetallic_Strip.md#it-overshoots-and-thats-expected).

**Car parts** are the other thing it can connect to, and the rule is the same there. An engine's throttle is how much fuel and air the engine gets, so it's a flow of energy too. It just takes a number instead of on/off, which is why a *Math* card's number can set it (and, later, a car's steering). A number sent to a clutch or valve does nothing.

## Holding a band

A single comparison ("bellows on below 1380") flips the switch every time the reading crosses the line, which can be many times a second. That's hard on anything that wears when switched under load, like a [Breaker](Breaker.md).

The fix is a **band** (a deadband): turn on below the bottom, turn off above the top, and in between **leave it as it is**. The switch itself remembers, so read it back:

> **on** if (temperature < 1360) **OR** (temperature < 1400 **AND** the bellows clutch is already on)

Link an *Input* card to the clutch itself and it reads yes while the clutch is engaged. Inside the band the clutch keeps doing whatever it was doing.

## Memory and upgrades

A bare Controller runs a program of up to **16 cards** with **4 variables** (named numbers or yes/no values a program can set and read back), and offers Delay cards the lengths 1 s, 5 s, 30 s, 2 min and 10 min. Three parts, fitted by **right-clicking the Controller with them**, stretch that:

| Part | Gives | Up to |
| --- | --- | --- |
| **Memory Drum** | 4 more variables | 3 |
| **Card Rack** | 8 more cards | 3 |
| **Escapement** | Delay cards may use any length from 1 s | 1 |

A card whose program needs more than the fitted parts give is refused when you try to put it in. A [Crowbar](Crowbar.md) will not pry off a part the card in the Controller needs. Variable values survive a reload; Delay history does not, and a Delay restarts from whatever its input reads. A stopped shaft freezes variables and delays along with everything else.

When a new card goes in, variables it declares with the same name and kind keep their values; the rest are cleared.

### Crafting the parts

```
Memory Drum        Card Rack        Escapement
P B P              I A I            S
P R P              P A P            C
P P P              I A I            R
```

- Memory Drum: P is a [Copper Plate](Mechanical_Hammer.md), B a Redstone Block, R a copper rod.
- Card Rack: P is any planks, I a wrought iron rod, A paper.
- Escapement: S is a steel plate, C a small cog, R a wrought iron rod.
