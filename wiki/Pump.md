# Pump

Moves fluid from its back to its front while its shaft turns. Any fluid: water, steam, molten metal, another mod's oil.

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Pump* in the type row); the diameter is set there, along with the impeller shape and addon. It needs no treatise. Shape a **Pump Impeller** from plate at a bench (the [Smithing Table](Smithing_Table.md) for metal), then assemble it at the [Engineering Table](Engineering_Table.md) with two [fasteners](Fasteners.md); the Pump Blueprint in the blueprint slot judges the fit against your drawing. The pump comes out with the design of the impeller, and the blueprint is kept.

The impeller has one measure, the **diameter**, from 10 to 60 cm (30 by default). A bigger impeller moves more fluid and loads the shaft more. Its **shape** trades flow against lift:

| Impeller | Flow | Lift |
| --- | --- | --- |
| Centrifugal | standard | standard |
| Axial | 1.6 times | half |
| Screw | 0.7 times | 1.6 times |

**Addon.** A **volute** (plate) is an optional casing that adds 15% to the flow, for a little more load.

## Placing

Place it **against the block it should draw from**. Its front faces away from that block and pushes into whatever is there.

The shaft can't share the fluid's direction, so it goes in **across** it: from above or below on a pump lying sideways, and from the east or west side on one pumping straight up or down.

## How much it moves

**0.5 mB per tick for every RPM** and a flat load of **8 Su**, however much it's moving, for a standard 30 cm impeller. The figures scale with the impeller: its tooltip shows them.

| Shaft | Moves |
| --- | --- |
| [Water Wheel](Water_Wheel.md), default wheel, one side of flow (4 RPM) | 2 mB/t |
| [Steam Engine](Steam_Engine.md) (16 RPM) | 8 mB/t |
| 20 RPM | 10 mB/t, enough to keep a [Boiler](Boiler.md) fed for one engine |
| [Hand Crank](Hand_Crank.md) (32 RPM) | 16 mB/t |

## Where it draws from

- **A tank** behind it: a [Condenser](Condenser.md), a boiler, a crucible, a [Pipe](Pipe.md) run, another mod's tank.
- **A source block** behind it, if it's a fluid that **renews itself**. Water does, so a pump in a pool of water (two or more sources) draws water forever without draining it. Lava doesn't renew, so a pump won't make lava from nothing.

If there's nowhere for the fluid to go in front, it doesn't pump.

## Notes

- Pumping **up** adds a lift load on top of the flat load: 6 Su with a standard impeller, more with an axial one, less with a screw. Level and downward cost nothing extra.
- It has no switch. Put a [Clutch](Clutch.md) on its shaft.
