# Engine

A piston engine that you design yourself on the **Engine Stand** (also called the **Car Design Stand**). Once built, it's a rotation source. Put it on a shaft and it drives your [mechanical network](Rotation.md), just like a [Steam Engine](Steam_Engine.md). It's also meant to be the heart of a [vehicle](Vehicles.md).

> The Engine Stand (the Car Design Stand) is two copper plates over two crafting tables. An engine burns liquid or gas fuel from its own tank.

## Designing one: the Engine Stand

Open the stand and use the **< >** buttons to set:

| Setting | Range | Effect |
| --- | --- | --- |
| **Cylinders** | 1–8 | More cylinders mean more torque and more weight, and cost more. |
| **Bore** | 40–120 mm | Wider cylinders mean more torque. |
| **Stroke** | 40–120 mm | Longer strokes add torque, but the engine **revs lower**. Short-stroke engines rev high. |
| **Compression** | low, medium, high | Which fuels it can burn. See [Compression, fuel and knock](#compression-fuel-and-knock). |
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

## Fuel

An engine runs on fuel in its own tank, 2000 mB. Fill it with a bucket or a [Pipe](Pipe.md) from a tank. It takes **any fluid** and never refuses: what you put in mixes **by mass** into one blend ([Fuels](Fuels.md#mixtures)), and it only burns what it holds, so you cannot pump it back out.

- **Power follows the flame.** A hotter-burning fuel gives more power, and lasts longer per mB, than a cooler one. Diesel (1350 Tu) is a little above the reference; heavy oil (1200 Tu) a little below. A fluid that does not burn gives nothing.
- **Fuel is used at the throttle.** The engine burns in proportion to its displacement and how far the throttle is open, so a big engine at full throttle drinks the most. A 1.6 L engine at full throttle uses about 0.13 mB a second, so a full tank lasts hours. A switched-off engine uses none.
- **No fuel, no power.** An engine out of fuel (or with nothing but a fluid that does not burn) makes no power; it coasts like a switched-off one.

### Compression, fuel and knock

Compression is a **part of the design**, chosen on the Engine Stand with the stepper under the figure (*Compression: low / medium / high*) and printed on the blueprint. It decides which fuels the engine can burn, judged by the blend's **ignition point** ([Fuels](Fuels.md)):

| Compression | Misfires on a fuel igniting above | Knocks on a fuel igniting below |
| --- | --- | --- |
| Low | 300 Tu | never |
| Medium | 340 Tu | 240 Tu |
| High | never | 280 Tu |

- **Misfire.** The fuel will not light. The engine makes **no power** and the fuel still goes through. Diesel (320 Tu) and heavy oil (330 Tu) misfire at low compression; ethylene and wisp gas misfire at medium.
- **Knock.** The fuel lights before the spark. The engine still runs, but it **wears**: its condition falls (a clank sounds), down to a floor of 20% of its original power, and never lower. A worn engine is weaker in proportion. Naphtha (250 Tu) and soul oil (260 Tu) knock at high compression.
- **Medium** burns every liquid cut without trouble. Choose low or high only for a fuel the middle does not suit.

A blend of two fuels has an ignition point between them, so a blend can sit inside a window neither fuel does.

## Running it

Place the Engine on a shaft line. Its crankshaft runs straight through, so **both ends** connect.

- Engines are placed **switched off**. **Right-click** to start or stop one.
- A [Controller](Controller.md) can start and stop it too, with a *Switch* card.
- A Controller can also set its **throttle**, by sending a number (0 to 1) instead of yes/no, from a [Driver's Seat](Vehicles.md#the-drivers-seat) key or a *Math* card. With the throttle shut a running engine **idles** at 15% of its torque and speed, so it doesn't stall at a standstill. Engines start with the throttle wide open.

An engine is **weak at low speed** and strongest around its peak. When you start it under load it has to work its way up. If the load is too heavy for it, it bogs down and stalls instead of reaching speed. Gear it so the load is light at first, or give it a bigger engine.

When switched off, the line coasts down on its own momentum.

Breaking an engine keeps its design (compression included). Place it again and it's the same engine.

## Tips

- Want speed? Short stroke, light strong metal. Want pulling power? Big bore, lots of cylinders.
- Engine speed is easily fast enough to trip an [Overspeed Trip](Overspeed_Trip.md) if you gear it up, so put one inline to protect anything delicate.
