# Fluid Burner

A burner for liquid and gas fuel, fed by a pipe. It heats whatever sits on top of it (it always radiates up) exactly as a [Firebox](Firebox.md) does: set a [Crucible](Crucible.md), [Boiler](Boiler.md) or [Reactor](Reactor.md) on it and it climbs toward the flame's temperature ([Heat](Heat.md)). The firebox burns items; the burner burns fluid.

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Fluid Burner* in the type row); the bore is set there, along with the nozzle shape and addon. It needs a blueprint but no treatise. Shape a **Burner Nozzle** from plate at a bench (the [Smithing Table](Smithing_Table.md) for metal), then put it together at the bench with the Fluid Burner Blueprint in the blueprint slot; the burner comes out with the design, and the blueprint is kept.

The nozzle has one measure, the **bore**, from 4 to 20 mm (10 by default). A wider bore burns more fuel. Its **shape** trades burn against flame temperature:

| Nozzle | Burn | Flame |
| --- | --- | --- |
| Ring | standard | standard |
| Jet | 1.5 times | 8% hotter |
| Wick | 0.6 times | 8% cooler |

**Addon.** A **flame holder** (nugget) is an optional piece that steadies the flame, 3% hotter.

## Using it

- **Fuel.** A bucket fills it, or a [Pipe](Pipe.md) or tank touching any side. It holds **2000 mB** and never filters: a fluid that does not burn just sits there and the burner stays dark.
- **Burn.** It lights as soon as it holds a fuel and burns **0.5 mB a tick** (10 mB a second) with a standard 10 mm nozzle, for as long as it has any, whether or not anything is on top. It shows lit while it burns.
- **Flame.** The flame temperature is the fuel's own ([Fuels](Fuels.md)). A fuel that brings oxygen of its own (soul oil) burns hotter, as if blown by a [Powered Bellows](Powered_Bellows.md), up to 1.5×. An ordinary fuel burns in the air around it. There is no bellows to add.

Unlike a firebox it needs no lighting and no stoking, and its heat is as steady as the supply: the flame stops the tick the fuel does.

A lit burner is an open flame, like a lit firebox: a cloud of fuel vapor drifting onto it goes off. Keep leaking fuel lines away from it.
