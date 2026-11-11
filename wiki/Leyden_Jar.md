# Leyden Jar

A glass jar lined with copper foil that **stores a charge and gives it back as a burst**. It's what makes the [Electrostatic Generator](Electrostatic_Generator.md)'s one-second pulse useful.

## Designing and building

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Leyden Jar* in the type row); it needs no treatise. It has one part, the **Jar Glass**, with four choices:

| Choice | What it sets |
| --- | --- |
| **Jar type** | Flat plate, lined or rolled. See [Jar types](#jar-types). |
| **Plate area** | From 20 to 100 percent, in steps of 5; the default is 50. More area stores more charge and bursts at more amps. |
| **Glass wall** | From 2 to 10 mm; the default is 4. Thinner glass stores more but leaks as the square: half the thickness leaks four times as fast. |
| **Glass** | The material of the wall. It must insulate. |

Shape the glass from a glass ingot at the [Stonecutter](Stonecutter.md) with the Leyden Jar Blueprint in the blueprint slot. The Leyden Jar comes out with the design, and the blueprint is kept. Its tooltip shows what it stores, how fast it leaks and the amps it bursts at. Breaking the block gives the same jar back.

## How it works

Put it on a line with a source. It switches between two jobs on its own:

- **Charging.** While a higher voltage is on the line, the jar draws **1 A** and fills up.
- **Discharging.** When the supply goes (the pulse ends, the cell is cut off), the jar becomes a source itself and gives back up to its burst amps (**8 A** at the default area) at whatever voltage it holds.

A jar at the default area and wall holds **10 amp-ticks per volt**. One Electrostatic Generator pulse (1 A for 20 ticks) fills it to the generator's 1.5 V with a little to spare.

## Jar types

Each type is cut from a different number of stock cells. The lined jar is the standard the figures on this page describe.

| Type | Plate area | Leak | Stock |
| --- | --- | --- | --- |
| Flat plate | 70% | half | least |
| Lined | 100% | standard | between |
| Rolled | 150% | 150% | most |

## Addons

An addon is an optional extra part made from raw stock, added when you assemble the jar or by reworking a finished one. The jar works without either; each only adds.

| Addon | Stock | What it does |
| --- | --- | --- |
| **Foil** | foil | Burst current scales with the metal's ampacity. |
| **Outer sleeve** | glass tubing | A second insulator over the jar; halves the leak. |

## A burst, not a battery

Its voltage **falls as it empties**, so anything running off it slows down as it goes: a strong start that tails off. It also **leaks** charge every tick, whether or not anything is connected: at the default wall about 0.2% a tick, which halves it in roughly 17 seconds.

A jar smooths a pulse and carries you over a gap. For power that lasts, use a [Battery](Battery.md).

## Careful

A jar charged to **6 V (MV) or more** shocks whoever touches it, walking into it or right-clicking it. See [Hazards](Hazards.md#shock). One charged from a Galvanic Cell or an Electrostatic Generator is never that high.

See also: [Jar Baton](Jar_Baton.md), which draws a jar's charge and spends it on a melee hit.
