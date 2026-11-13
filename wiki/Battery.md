# Battery

A rechargeable battery, lead-acid or nickel-iron: cells in series, rated a band, that store charge in Eu. It charges from a line in its band or a higher one and gives its own band back when the line is lower, at full current until almost empty.

## Crafting

Shape the parts at a bench and put them together in a crafting grid, as for any [modular tool](Tools_and_Weapons.md). It has two parts:

- **Battery Plates**, shaped from plates of a metal that takes a charge: lead or [nickel-iron](Materials.md#nickel-iron). Two steppers set the size: **cell size** (1 to 4), how much each cell holds, and **cell count** (1 to 12), how many cells sit in series.

- **A case**: a pane, shard or sheet of an insulating material such as glass, ceramic or a polymer. The highest band the case holds off is the band the battery is **rated**: it charges from a line of that band or higher, and a line above it wears the battery.

The plate metal sets how much charge a plate holds, how it fares in the cold, how much of the charge comes back and which liquid fills it. Capacity is in Eu: the cells in series add up what the battery holds, but not the current it can give, which is one cell's. The blueprint sheet shows the rated band and the capacity.

| Plate metal | Charge per plate | Charge returned | Cold | Gassing | Fill with |
| --- | --- | --- | --- | --- | --- |
| Lead | standard | about 85% | half the capacity at 0 Tu | yes | sulfuric acid |
| Nickel-iron | half | about 65% | most of it kept at 0 Tu | none | lye |

## Setting up

Place the battery and fill it from a bucket or a [pipe](Pipe.md):

- **Sulfuric acid** for lead plates, or [**lye**](Chemicals.md#lye) for nickel-iron: 250 mB per cell. Without it the battery is dry and does nothing. Each battery takes only its own liquid.
- **Water**: a 1000 mB tank, topped up the same way. See [Water](Water.md).

Wire it to a line like any other electrical block; see [Electricity](Electricity.md). Breaking it keeps its charge, water, electrolyte and condition in the item.

## How it works

The battery switches between three states on its own, shown in Jade:

- **Charging**: the line is in the battery's band or a higher one. It draws current and fills, from empty to full in about 2.5 minutes at its limit, or about 1 minute beside a powered [Rectifier](Rectifier.md).
- **Supplying**: the line is in a lower band. It becomes a source of its own band, and a full battery can run flat in about 5 minutes at its limit.
- **Holding**: it neither takes nor gives.

A line in a lower band never charges it. The current it gives stays full while charge lasts and falls away below 10% charge. The share of the energy put in that comes back is in the table above; the rest heats the case. A battery loses about 1% of its charge a day when idle.

### Reading the charge

Jade shows the charge in words: **Low**, **Part charged**, **Nearly full**, **Full**. A [Hydrometer](Instruments.md#hydrometer) reads it to 5%.

## Misuse and hazards

See [Hazards](Hazards.md) for the general rules.

- **Overcharge.** On a lead battery, a line well above its full strength, or charging past full, boils water off as hydrogen. Nickel-iron cells do not gas. Jade shows *Weeping gas*. A neighboring [Gas Tank](Gas_Tank.md) takes the hydrogen; otherwise a flame beside more than 40 mB of it blasts.
- **Low water.** Jade shows *Low water*. Exposed plates cut capacity; top the water up.
- **Overdraw.** Drawing more than the limit heats the case and costs condition.
- **Heat.** A case over 50 Tu costs condition. See [Heat](Heat.md).
- **Too high a band.** A line above the case's insulation band costs condition.
- **Cold.** Capacity falls as the body cools below 20 Tu, to half at 0 Tu for lead and much less for nickel-iron, as in an [Ice Box](Ice_Box.md).

A charged battery with a case rated MV or more shocks whoever touches it; see [Hazards](Hazards.md#shock).

An [Arc Welder](Arc_Welder.md) draws its charge from a Battery in your inventory. A [Charger](Charger.md) takes one Battery item, and the chestplate of an [Exosuit](Exosuit.md) holds the Battery that runs the suit; a [Charging Gantry](Charging_Gantry.md) charges it while worn.
