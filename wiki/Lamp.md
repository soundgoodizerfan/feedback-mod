# Glowstone Lamp

A panel of glowstone that lights by how well a power line feeds it. It replaces the vanilla [Redstone Lamp](Redstone_Lamp.md).

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Glowstone Lamp* in the type row); the gauge is set there, along with the addon. It needs no treatise. Shape a **Lamp Filament** from wire at a bench, then assemble it at the [Engineering Table](Engineering_Table.md) with two [fasteners](Fasteners.md); the Glowstone Lamp Blueprint in the blueprint slot judges the fit against your drawing. The lamp comes out with the design of the filament, and the blueprint is kept. The wire's metal must conduct.

The filament has one measure, its **gauge**, from 20 to 100 percent (50 by default). The draw rises with the square of the gauge, and a thicker filament lasts longer when the line is over its band. The filament has a fixed standard surface that is never picked.

**Addon.** A **ballast** (nugget) is an optional piece that adds 20% to the overload the filament survives.

## Using it

Place it touching an [electrical](Electricity.md) line, such as a [cable](Copper_Cable.md) or a source. It is the end of the line, a load of **0.5 A** with a standard 50 percent filament, rated for the **MV** band, and it has nothing to configure.

It glows in four steps by how well it is fed: dark, **dim**, **bright** and **full**, giving light level 0, 5, 10 and 15. Jade names the step. A step changes only once the supply has moved well past the boundary, so a line hovering near one does not flicker the light.

## Under and over

- **Too little** packet (a long cable run, or an LV line) or current only dims it. A starved line leaves it dark.
- **A packet a little over its rating** (a long cable run on a higher band that leaves it only just over) wears the panel: it loses brightness for good, the further over the faster, and never drops below about a third of full.
- **Twice its rated packet or more** (more with a tougher filament or a ballast), which an HV line or higher delivers unless the cable takes most of it, burns the panel out at once. A burnt lamp is dark, draws nothing and stays dark; break it and place another by hand.

See [Electricity](Electricity.md#under-and-over) for how machines take too much or too little.

## Attunements

**Gleam** on the filament gives its light on less current; **Murk** runs it cooler so it burns out later. **Ward** and **Mend** handle overvoltage wear. See the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
