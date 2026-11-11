# Thermal Store

A tank of salt in a ceramic wall that holds heat. A fire, [Heating Coil](Heating_Coil.md) or arc head under it charges it, and a [Boiler](Boiler.md) on top draws it down into steam for a [Steam Turbine](Steam_Turbine.md). It is built from a blueprint.

## Designing and building

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Thermal Store* in the type row); it needs no treatise. It has one part, the **Store Core**, with two choices:

| Choice | What it sets |
| --- | --- |
| **Wall** | Thickness from 10 to 40 cm, in steps of 2; the default is 20. A thicker wall has more heat mass, so it charges slowly, and it leaks less. |
| **Material** | The brick the wall is made from. Its conductivity sets the leak: ceramic keeps heat and metal gives it away. It must have a stated conductivity. |

Shape the core from brick at the [Stonecutter](Stonecutter.md) with the Thermal Store Blueprint in the blueprint slot. The Thermal Store comes out with the design, and the blueprint is kept. Its tooltip shows its heat mass and how much it loses. Breaking the block gives the same store back.

## Addons

An addon is an optional extra part made from raw stock, added when you assemble the store or by reworking a finished one. The store works without either; each only adds.

| Addon | Stock | What it does |
| --- | --- | --- |
| **Lagging** | carpet | Wrapped round the outside. The poorer its conductivity, the less heat leaks, down to a fifth of the leak at best. |
| **Salt fill** | raw salt | Adds heat mass. |

## Using it

Set it over a heat source, and set a Boiler on top of it. The store warms toward the source and the Boiler heats from the store. A [Firebox](Firebox.md), a coil or an [arc head](Arc_Furnace.md#the-arc-head) all charge it.

## How it works

- It holds heat, not electricity.
- It cannot get hotter than what charges it.
- It cools slowly to the air while it holds heat; a thick wall of a poor conductor cools slowest.
- Only part of the heat comes back out, so the round trip from fuel or power to steam is poor.

A coil charging a store from spare electricity and a Boiler drawing it down at night is the use. See [Heat](Heat.md) for how heat moves between touching blocks.
