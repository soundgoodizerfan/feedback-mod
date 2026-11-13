# Leyden Jar

A glass jar lined with copper foil that **stores a charge and gives it back as a burst**. It's what makes the [Electrostatic Generator](Electrostatic_Generator.md)'s one-second pulse useful.

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Leyden Jar* in the type row); the plate area and glass wall are set there, along with the jar type and addons. It needs a blueprint but no treatise. It has one part, the **Jar Glass**, with four choices:

| Choice | What it sets |
| --- | --- |
| **Jar type** | Flat plate, lined or rolled. See [Jar types](#jar-types). |
| **Plate area** | From 20 to 100 percent, in steps of 5; the default is 50. More area stores more charge and bursts at more amps. |
| **Glass wall** | From 2 to 10 mm; the default is 4. Thinner glass stores more but leaks as the square: half the thickness leaks four times as fast. |
| **Glass** | The material of the wall. It must insulate, and how well it insulates is the band the jar is rated. |

Shape the glass from a glass ingot at the [Stonecutter](Stonecutter.md), then assemble it at the [Engineering Table](Engineering_Table.md) with two [fasteners](Fasteners.md); the Leyden Jar Blueprint in the blueprint slot judges the fit against your drawing. The Leyden Jar comes out with the design of the glass, and the blueprint is kept. Its tooltip shows the band it is rated, what it stores in Eu, how fast it leaks and the amps it bursts at. Breaking the block gives the same jar back.

## How it works

Put it on a line with a source. It switches between two jobs on its own:

- **Charging.** While the line is in the jar's band or a higher one, the jar draws **1 A** and fills up. A higher band simply fills it; a lower band never charges it.
- **Discharging.** When the supply goes (the pulse ends, the cell is cut off), the jar becomes a source itself and gives back packets of its own band, up to its burst amps (**8 A** at the default area), until its Eu run out.

**Beside a generator.** On a line with a generator of the jar's own band, the jar is a **reserve**: the generator carries the load, the jar supplies only what the generator cannot, and it charges from any surplus.

The jar's charge is counted in Eu: how many packets of its band it holds, set by plate area over wall thickness. An [Electrostatic Generator](Electrostatic_Generator.md) pulse is a higher band than an LV jar's, so it fills such a jar; a jar rated for the pulse's band takes it too.

## Jar types

Each type is cut from a different number of stock cells. The lined jar is the standard the figures on this page describe.

| Type | Plate area | Leak | Stock |
| --- | --- | --- | --- |
| Flat plate | 70% | half | least |
| Lined | 100% | standard | between |
| Rolled | 150% | 150% | most |

## Addons

An addon is an optional extra part made from raw stock, added when you assemble the jar or afterward on the Engineering Table's [Fit tab](Engineering_Table.md#fit-tab). The jar works without either; each only adds.

| Addon | Stock | What it does |
| --- | --- | --- |
| **Foil** | foil | Burst current scales with the metal's ampacity. |
| **Outer sleeve** | glass tubing | A second insulator over the jar; halves the leak. |

## A burst, not a battery

It always gives its own band, and its last packet tapers as the Eu run out: a strong start that ends in a short tail. It also **leaks** charge every tick, whether or not anything is connected: at the default wall about 0.2% a tick, which halves it in roughly 17 seconds.

A jar smooths a pulse and carries you over a gap. For power that lasts, use a [Battery](Battery.md).

## Careful

**A band above its rating cracks the jar.** Fed a band higher than the glass is rated for, the jar shatters and drops nothing. It discharges at once as a shock at its own band to every living thing within 3 blocks, whatever the charge it held. [Gloves](Hazards.md#shock) rated for that band protect.

A jar rated **MV** or more and holding charge shocks whoever touches it, walking into it or right-clicking it. See [Hazards](Hazards.md#shock).

See also: [Jar Baton](Jar_Baton.md), which draws a jar's charge and spends it on a melee hit.
