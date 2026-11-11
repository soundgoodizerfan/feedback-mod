# Pressure Vessel

A sealed tank, built from a blueprint, that you pump air into with a [Powered Bellows](Powered_Bellows.md). **Pump too much in and it explodes.**

## Designing and building

The vessel is a modular build. Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Pressure Vessel* in the type row) and print it on paper; drawing it takes a Chemical [Treatise](Treatise.md). It has one part, the **Vessel Shell**, with three choices:

| Choice | What it sets |
| --- | --- |
| **Shape** | Box, cylinder or sphere. A sphere carries the most pressure and leaves the least room inside; a box carries the least and leaves the most. A cylinder sits between. |
| **Wall** | Thickness from 1 to 20 cm. A thicker wall holds more pressure and leaves less room. |
| **Metal** | The plate it is made from. A stronger metal holds more. It must be a metal with a measured yield strength. |

The sheet shows three figures: **Room inside** in mB, **Risks letting go over** in Pu, and **Gives way at** in Pu.

At the [Smithing Table](Smithing_Table.md), put the Pressure Vessel Blueprint in the blueprint slot and shape the shell from plate of the metal you chose. The Pressure Vessel comes out with the shape, wall and metal you drew, and the blueprint is kept. Its tooltip shows the room and the danger line.

## Using it

Put a bellows against it and run the bellows. Air builds up inside: **1 Pu is the vessel's room in mB of air**, so a roomy vessel fills slowly and a snug one fast. It leaks back out by itself at 1 mB a tick, so an idle vessel slowly empties.

| Pressure | What happens |
| --- | --- |
| up to the danger line | nothing, however long it sits there |
| above the danger line | each tick there's a chance it **explodes**, climbing the further over it is |
| at the critical pressure | it **explodes** |

The explosion damages blocks around it. Breaking the vessel gives back the same vessel, with its design.

The [Boiler](Boiler.md) follows the same ladder.

## Addons

An addon is an optional extra part made from raw stock, added when you assemble the vessel or by reworking a finished one. The vessel works without either; each only adds.

| Addon | Stock | What it does |
| --- | --- | --- |
| **Hoop bands** | rod | Bands round the shell. A band metal stronger than the shell's raises the danger line, by up to 60% at best. |
| **Relief valve** | nugget | Opens above 90% of the danger line and vents air, so a pump that outruns the leak cannot burst the vessel. |

## Canning

Set the vessel on a [Firebox](Firebox.md) (or lava) and it can sterilize jars of food (see [Food](Food.md)). Right click with a jarred food to put one in (up to four); sneak with an empty hand to take the last one out; breaking the vessel drops them. Pressure raises the boiling point, so with a fire under it the inside climbs past the 100 Tu a pot tops out at, up to 6 Tu for each Pu. Sterilising needs 116 Tu, which about 2.7 Pu and a fire that hot give. Keep that under the vessel's danger line: pump to the working window, not to the limit. A jar of food is not a hazard, and jars do not change when the vessel bursts.

Nothing draws the stored air back out, and no instrument reads its pressure. It's mostly a demonstration that equipment can be pushed past its limits, and a reason to put a [Valve](Valve.md) or [Clutch](Clutch.md) on a bellows line.

With Jade, the vessel shows its inside temperature (read as a thermometer would), how many jars it holds, how many are sealed or gone mushy, and how far the slowest jar is from sealing (in words; the exact percentage only at the truth tier). A jar not yet sealed says *Partly sterilized* in its tooltip once heating has begun.
