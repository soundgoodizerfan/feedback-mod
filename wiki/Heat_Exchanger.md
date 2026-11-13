# Heat Exchanger

A wall of tubes that lets heat flow between the two things on either side of it, **from hot to cold**, all the time. It is built from a blueprint.

## Designing and building

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Heat Exchanger* in the type row); drawing it takes Chemical Treatise II ([Treatise](Treatise.md)). It has one part, the **Exchanger Core**, with three choices:

| Choice | What it sets |
| --- | --- |
| **Tubes** | Plain, finned, coiled, or plate and frame. See [Tube types](#tube-types). |
| **Tube area** | From 20 to 100 percent of the wall, in steps of 5; the default is 50. More area carries more heat. |
| **Metal** | The plate the tubes are made from. A metal that conducts heat well carries more. It must be a metal with a stated conductivity. |

How fast it carries heat is the tube metal's conductivity times the tube area. Copper tubes at the default area carry what a plain copper exchanger does.

Shape the core from plate of the metal you chose at the [Smithing Table](Smithing_Table.md), then assemble it at the [Engineering Table](Engineering_Table.md): the core in the grid, two [fasteners](Fasteners.md) in the fastener slots, and the Heat Exchanger Blueprint in the blueprint slot to judge the fit against your drawing. The Heat Exchanger comes out with the design of the core, and the blueprint is kept. Its tooltip shows how fast it carries heat ("Carries heat at"). Breaking the block gives the same exchanger back.

## Tube types

Each type is cut from a different number of stock cells, and the ones that carry more need more of them.

| Tubes | Heat carried compared with plain |
| --- | --- |
| Plain | the same |
| Coiled | 1.3 times as much |
| Finned | 1.6 times as much |
| Plate and frame | 2.0 times as much |

## Addons

An addon is an optional extra part made from raw stock, added when you assemble the exchanger or afterward on the Engineering Table's [Fit tab](Engineering_Table.md#fit-tab). The exchanger works without it.

- **Fin stack** (plate of a second metal): its conductivity adds 60% of its value to the tubes', so the exchanger carries more heat.

## Using it

It connects along one axis: the two faces at either end of the direction you placed it. Put a hot thing on one end and a cooler thing on the other. Heat flows from the hotter to the cooler, faster the bigger the difference, until they're about level.

It works with anything that has a temperature: [crucibles](Crucible.md), [furnaces](Furnaces.md), a [Boiler](Boiler.md), even overloaded [cable](Copper_Cable.md). It is how a [Fission Reactor](Fission_Reactor.md) core hands its heat to a boiler.

There's no power, no switch and no direction to set. Heat always runs downhill. The decisions are **where you put it** and what you build it from.

## Ideas

- **Waste-heat boiler**: put a boiler on the far side of a crucible that's running hot anyway.
- **Gentle cooling**: bleed heat out of a vessel into a cooler neighbor instead of opening a [Damper](Damper.md).
- Cooling through an exchanger counts as cooling for processes like tempering.
- **Cooling a [Condenser](Condenser.md)**: a condenser only works below boiling. An exchanger between it and the boiler hands the steam's heat back to the boiler.

## Attunements

A heat exchanger whose two sides stand far apart in temperature cracks its wall: thermal shock wears the core and fins. **Ward** on either slows it and **Mend** closes it, paid in experience from the nearest player within 6 blocks. See the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
