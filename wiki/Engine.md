# Engine

A piston engine that you design yourself on the **Engine Stand** (also called the **Car Design Stand**). Once built, it's a rotation source. Put it on a shaft and it drives your [mechanical network](Rotation.md), just like a [Steam Engine](Steam_Engine.md). It's also meant to be the heart of a [vehicle](Vehicles.md).

> The Engine Stand (the Car Design Stand) is two copper plates over two crafting tables. Engines use no fuel: a running engine is free power.

## Designing one: the Engine Stand

Open the stand and use the **< >** buttons to set:

| Setting | Range | Effect |
| --- | --- | --- |
| **Cylinders** | 1–8 | More cylinders mean more torque and more weight, and cost more. |
| **Bore** | 40–120 mm | Wider cylinders mean more torque. |
| **Stroke** | 40–120 mm | Longer strokes add torque, but the engine **revs lower**. Short-stroke engines rev high. |
| **Material** | from the sample slot | See below. |

Put any piece of a metal in the **sample slot** to choose the material (it isn't used up). Then put **Paper** in the paper slot and press **Print** to get an **Engine Blueprint**. To edit an old design, put its blueprint back in the paper slot, change it, and print again.

### The spec sheet

The screen, the blueprint and the finished engine all show:

- **Displacement**, in litres.
- **Peak torque**, in Su (100 Su per litre: a 1.6 L engine makes about 160), and the RPM where it peaks (60% of redline).
- **Redline**: the fastest it can safely spin. A steel 4 × 80 × 80 mm engine redlines around 7500 RPM, a copper one around 3600.
- **Mass**: what it weighs.

### Choosing a metal

The metal decides how fast the engine can rev. **Light, strong metals rev highest.** Heavy or soft ones rev lower and weigh more. Hardened steel revs highest of all, with netherite close behind (but much heavier). Steel is a good all-rounder. Copper, gold and lead make lazy, low-revving engines. The metal has to be strong enough to build with and come as ingots, or the stand won't take it.

## Building it

Put the blueprint in a crafting table with **one ingot of the chosen metal per cylinder**, in any arrangement. You get the Engine and you keep the blueprint.

## Running it

Place the Engine on a shaft line. Its crankshaft runs straight through, so **both ends** connect.

- Engines are placed **switched off**. **Right-click** to start or stop one.
- A [Controller](Controller.md) can start and stop it too, with a *Switch* card.
- A Controller can also set its **throttle**, by sending a number (0 to 1) instead of yes/no, from a [Driver's Seat](Vehicles.md#the-drivers-seat) key or a *Math* card. With the throttle shut a running engine **idles** at 15% of its torque and speed, so it doesn't stall at a standstill. Engines start with the throttle wide open.

An engine is **weak at low speed** and strongest around its peak. When you start it under load it has to work its way up. If the load is too heavy for it, it bogs down and stalls instead of reaching speed. Gear it so the load is light at first, or give it a bigger engine.

When switched off, the line coasts down on its own momentum.

Breaking an engine keeps its design. Place it again and it's the same engine.

## Tips

- Want speed? Short stroke, light strong metal. Want pulling power? Big bore, lots of cylinders.
- Engine speed is easily fast enough to trip an [Overspeed Trip](Overspeed_Trip.md) if you gear it up, so put one inline to protect anything delicate.
