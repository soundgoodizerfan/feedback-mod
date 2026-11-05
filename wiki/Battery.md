# Battery

A lead-acid battery: cells in series that charge from any line above their voltage and give the charge back when the line drops. It stores real power, holding its voltage nearly flat until almost empty.

## Crafting

Build it at the Tool Table, as for any [modular tool](Tools_and_Weapons.md), from two parts:

- **Battery Plates**, shaped from lead plates (or another metal whose plates take a charge). Two plates make a cell. The shape sets the cell count:

| Plates | Cells | Volts when full |
| --- | --- | --- |
| 2 | 1 | about 2.1 V |
| 6 | 3 | about 6.3 V |
| 12 | 6 | about 12.6 V |
| 24 | 12 | about 25 V |

- **A case**: a pane, shard or sheet of an insulating material such as glass, ceramic or a polymer. The highest voltage the case holds off is the highest voltage the battery takes without wearing.

The plate metal sets the volts of a cell, how much charge a plate holds and how it fares in the cold. Cells in series add their volts, not their capacity.

## Setting up

Place the battery and fill it from a bucket or a [pipe](Pipe.md):

- **Sulfuric acid**: 250 mB per cell. Without acid the battery is dry and does nothing.
- **Water**: a 1000 mB tank, topped up the same way. See [Water](Water.md).

Wire it to a line like any other electrical block; see [Electricity](Electricity.md). Breaking it keeps its charge, water, acid and condition in the item.

## How it works

The battery switches between three states on its own, shown in Jade:

- **Charging**: the line is above the battery's voltage. It draws current and fills, from empty to full in about 10 minutes at its limit.
- **Supplying**: the line has dropped below it. It becomes a source, and a full battery can run flat in about 5 minutes at its limit.
- **Holding**: the line is at its own voltage; it neither takes nor gives.

Each plate holds 24000 amp-ticks, so more plates hold more. Voltage stays flat while charge lasts and falls off a cliff near empty. About 85% of the energy put in comes back; the rest heats the case. A battery loses about 1% of its charge a day when idle.

Voltage band: a battery's band follows its full voltage, so 2 V is LV, 6 V and 12 V are MV and 24 V is HV.

### Reading the charge

Jade shows the charge in words: **Low**, **Part charged**, **Nearly full**, **Full**. A [Hydrometer](Instruments.md#hydrometer) reads it to 5%.

## Misuse and hazards

See [Hazards](Hazards.md) for the general rules.

- **Overcharge.** A line over 1.1 times the battery's full voltage, or charging past full, boils water off as hydrogen. Jade shows *Weeping gas*. A neighboring [Gas Tank](Gas_Tank.md) takes the hydrogen; otherwise a flame beside more than 40 mB of it blasts.
- **Low water.** Jade shows *Low water*. Exposed plates cut capacity; top the water up.
- **Overdraw.** Drawing more than the limit heats the case and costs condition.
- **Heat.** A case over 50 Tu costs condition. See [Heat](Heat.md).
- **Overvoltage.** A line above the case's insulation band costs condition.
- **Cold.** Capacity falls as the body cools below 20 Tu, to half at 0 Tu, as in an [Ice Box](Ice_Box.md).

An [Arc Welder](Arc_Welder.md) draws its charge from a Battery in your inventory.
