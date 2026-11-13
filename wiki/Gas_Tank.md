# Gas Tank

A big pressure shell for gas, built from a blueprint. It is where a compressed line goes to rest.

## Designing and building

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Gas Tank* in the type row); drawing it takes Chemical Treatise I ([Treatise](Treatise.md)). It has one part, the **Tank Shell**, with the same three choices as the [Pressure Vessel](Pressure_Vessel.md#designing-and-building): shape (box, cylinder, capsule or sphere, with the same trade of strength against room as the [Pressure Vessel's shapes](Pressure_Vessel.md#shapes)), wall thickness from 1 to 20 cm, and the metal, which must have a measured yield strength. The shell is the vessel's, many times the size: it holds 16 cubes of gas at the pressure one such cube would, so the same wall carries the same line.

Shape the shell from plate of the metal you chose at the [Smithing Table](Smithing_Table.md), then assemble it at the [Engineering Table](Engineering_Table.md): the shell in the grid, two [fasteners](Fasteners.md) in the fastener slots, and the Gas Tank Blueprint in the blueprint slot to judge the fit against your drawing. The Gas Tank comes out with the design of the shell, and the blueprint is kept. Its tooltip shows what it holds, the mB it takes for each Pu, the pressure it risks letting go over and the pressure it gives way at. Breaking the block gives the same tank back.

## Using it

- A [Pipe](Pipe.md) or any ordinary flow fills it only to four fifths of its danger line. Plain flow never over-fills it.
- A [Gas Compressor](Gas_Compressor.md) fills it past that.
- **Right-click** with an empty hand to read what it holds and its pressure. A [Pressure Gauge](Instruments.md#pressure-gauge) reads the pressure too.
- Its gas cools toward the air around it.

## Addons

An addon is an optional extra part made from raw stock, added when you assemble the tank or afterward on the Engineering Table's [Fit tab](Engineering_Table.md#fit-tab). The tank works without either; each only adds.

| Addon | Stock | What it does |
| --- | --- | --- |
| **Hoop bands** | rod | Bands round the shell. A band metal stronger than the shell's raises the danger line, by up to 60% at best. |
| **Relief valve** | nugget | Opens above 90% of the danger line and vents gas, so a compressor that outruns what you draw off cannot burst the tank. |

## It can burst

Over the danger line on its tooltip the tank can burst, the chance climbing as the pressure does. At the critical pressure it is certain. The gas is lost with it, in an explosion. The only way to reach that line is to compress more in than you draw off. Burning the gas off or using it keeps the pressure down.

## Attunements

Wear only builds while the pressure is past the danger line, faster the nearer the critical line. A worn shell bursts at a lower pressure: one worn to its floor lets go at 60% of its rated pressure. **Ward** on the tank shell or its bands slows the wear; **Mend** closes it, paid in experience from a reserve housing or the nearest player within 6 blocks; **Tithe** pays out experience orbs for the wear instead. **Pull** on the shell or bands blunts a burst; **Reservoir**, **Grudge** and **Calm** on the shell hold experience or take a blast. Details and the rest of the effects are on the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
