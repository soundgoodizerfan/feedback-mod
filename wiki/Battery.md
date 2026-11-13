# Battery

A rechargeable battery, lead-acid or nickel-iron: cells in series that charge from any line in a band above their own and give the charge back when the line drops. It stores real power, holding its band nearly flat until almost empty.

## Crafting

Shape the parts at a bench and put them together in a crafting grid, as for any [modular tool](Tools_and_Weapons.md). It has two parts:

- **Battery Plates**, shaped from plates of a metal that takes a charge: lead or [nickel-iron](Materials.md#nickel-iron). Two steppers set the size: **cell size** (1 to 4), how much each cell holds, and **cell count** (1 to 12), how many cells sit in series.

- **A case**: a pane, shard or sheet of an insulating material such as glass, ceramic or a polymer. The highest band the case holds off is the highest band the battery takes without wearing.

The plate metal sets the strength of a cell, how much charge a plate holds, how it fares in the cold, how much of the charge comes back and which liquid fills it. Cells in series add their strength, not their capacity, and the stack's band is the band that total reaches when full. A lead stack of 1 or 2 cells is LV, of 3 to 10 cells MV and of 11 or 12 cells HV. A nickel-iron stack of up to 4 cells is LV and of 5 or more MV.

| Plate metal | Strength per cell | Charge per plate | Charge returned | Cold | Gassing | Fill with |
| --- | --- | --- | --- | --- | --- | --- |
| Lead | higher | standard | about 85% | half the capacity at 0 Tu | yes | sulfuric acid |
| Nickel-iron | lower | half | about 65% | most of it kept at 0 Tu | none | lye |

## Setting up

Place the battery and fill it from a bucket or a [pipe](Pipe.md):

- **Sulfuric acid** for lead plates, or [**lye**](Chemicals.md#lye) for nickel-iron: 250 mB per cell. Without it the battery is dry and does nothing. Each battery takes only its own liquid.
- **Water**: a 1000 mB tank, topped up the same way. See [Water](Water.md).

Wire it to a line like any other electrical block; see [Electricity](Electricity.md). Breaking it keeps its charge, water, electrolyte and condition in the item.

## How it works

The battery switches between three states on its own, shown in Jade:

- **Charging**: the line is in a band above the battery's own. It draws current and fills, from empty to full in about 2.5 minutes at its limit, or about 1 minute beside a powered [Rectifier](Rectifier.md).
- **Supplying**: the line has dropped below it. It becomes a source, and a full battery can run flat in about 5 minutes at its limit.
- **Holding**: the line is in the battery's own band; it neither takes nor gives.

The band stays flat while charge lasts and falls off a cliff near empty. The share of the energy put in that comes back is in the table above; the rest heats the case. A battery loses about 1% of its charge a day when idle.

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

An [Arc Welder](Arc_Welder.md) draws its charge from a Battery in your inventory. A [Charger](Charger.md) takes one Battery item, and the chestplate of an [Exosuit](Exosuit.md) holds the Battery that runs the suit; a [Charging Gantry](Charging_Gantry.md) charges it while worn.
