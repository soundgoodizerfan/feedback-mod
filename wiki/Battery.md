# Battery

A rechargeable battery, lead-acid or nickel-iron: cells in series that charge from any line above their voltage and give the charge back when the line drops. It stores real power, holding its voltage nearly flat until almost empty.

## Crafting

Build it at a bench, as for any [modular tool](Tools_and_Weapons.md), from two parts:

- **Battery Plates**, shaped from plates of a metal that takes a charge: lead or [nickel-iron](Materials.md#nickel-iron). The shape sets the cell count and how much each cell holds:

| Plates | Cells | Each cell holds |
| --- | --- | --- |
| 1 | 1 | half the standard |
| 2 | 1 | standard |
| 4 (heavy) | 1 | twice the standard |
| 6 | 3 | standard |
| 12 | 6 | standard |
| 24 | 12 | standard |

- **A case**: a pane, shard or sheet of an insulating material such as glass, ceramic or a polymer. The highest voltage the case holds off is the highest voltage the battery takes without wearing.

The plate metal sets the volts of a cell, how much charge a plate holds, how it fares in the cold, how much of the charge comes back and which liquid fills it. Cells in series add their volts, not their capacity. Lead cells give about 2.1 V full, so the volts when full run about 2.1 V (2 plates), 6.3 V (6), 12.6 V (12) and 25 V (24). Nickel-iron cells give about 1.3 V each.

| Plate metal | Volts per cell, full | Charge per plate | Charge returned | Cold | Gassing | Fill with |
| --- | --- | --- | --- | --- | --- | --- |
| Lead | about 2.1 V | standard | about 85% | half the capacity at 0 Tu | yes | sulfuric acid |
| Nickel-iron | about 1.3 V | half | about 65% | most of it kept at 0 Tu | none | lye |

## Setting up

Place the battery and fill it from a bucket or a [pipe](Pipe.md):

- **Sulfuric acid** for lead plates, or [**lye**](Chemicals.md#lye) for nickel-iron: 250 mB per cell. Without it the battery is dry and does nothing. Each battery takes only its own liquid.
- **Water**: a 1000 mB tank, topped up the same way. See [Water](Water.md).

Wire it to a line like any other electrical block; see [Electricity](Electricity.md). Breaking it keeps its charge, water, electrolyte and condition in the item.

## How it works

The battery switches between three states on its own, shown in Jade:

- **Charging**: the line is above the battery's voltage. It draws current and fills, from empty to full in about 10 minutes at its limit, or about 3 minutes beside a powered [Rectifier](Rectifier.md).
- **Supplying**: the line has dropped below it. It becomes a source, and a full battery can run flat in about 5 minutes at its limit.
- **Holding**: the line is at its own voltage; it neither takes nor gives.

Voltage stays flat while charge lasts and falls off a cliff near empty. The share of the energy put in that comes back is in the table above; the rest heats the case. A battery loses about 1% of its charge a day when idle.

Voltage band: a battery's band follows its full voltage, so 1.3 V and 2 V are LV, 6 V and 12 V are MV and 24 V is HV.

### Reading the charge

Jade shows the charge in words: **Low**, **Part charged**, **Nearly full**, **Full**. A [Hydrometer](Instruments.md#hydrometer) reads it to 5%.

## Misuse and hazards

See [Hazards](Hazards.md) for the general rules.

- **Overcharge.** On a lead battery, a line over 1.1 times its full voltage, or charging past full, boils water off as hydrogen. Nickel-iron cells do not gas. Jade shows *Weeping gas*. A neighboring [Gas Tank](Gas_Tank.md) takes the hydrogen; otherwise a flame beside more than 40 mB of it blasts.
- **Low water.** Jade shows *Low water*. Exposed plates cut capacity; top the water up.
- **Overdraw.** Drawing more than the limit heats the case and costs condition.
- **Heat.** A case over 50 Tu costs condition. See [Heat](Heat.md).
- **Overvoltage.** A line above the case's insulation band costs condition.
- **Cold.** Capacity falls as the body cools below 20 Tu, to half at 0 Tu for lead and much less for nickel-iron, as in an [Ice Box](Ice_Box.md).

An [Arc Welder](Arc_Welder.md) draws its charge from a Battery in your inventory.
