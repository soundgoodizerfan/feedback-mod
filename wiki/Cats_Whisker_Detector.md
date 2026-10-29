# Cat's Whisker Detector

A wire touching a crystal. Somewhere on the crystal is a spot that works as a detector; finding it is the job. When it hears a nearby **spark** it closes the line for half a second, then opens again by itself. Where the [Redstone Coherer](Redstone_Coherer.md) remembers, this one doesn't.

## Crafting

```
W
C
P
```

W is [Copper Wire](Wire_Drawer.md), C is Ceramic and P is a Wrought Iron Plate.

## Using it

Place it in a line like a [cable](Copper_Cable.md).

- **Right-click with a crystal** to seat it. Any crystal with a rectifying spot works: **Galena** (best), **Pyrite**, and **Quartz** (poor). Other mods' crystals work if their material says so.
- **Right-click with an empty hand** to move the whisker to the next of 8 spots. It tells you what it hears: *dead, faint, clear* or *sharp*. The report is noisy: step round, and come back to the one you liked.
- **Right-click with a [Basic Electronic Circuit](Basic_Electronic_Circuit.md)** to fit it on the contact. Optional: it quiets the report, extends reach and shortens the hold.
- **Sneak-right-click with an empty hand** to take the crystal out, then the circuit.

A better spot hears sparks from further (up to 16 blocks) and so also hears every stray arc further. Sparks are described on the [Redstone Coherer](Redstone_Coherer.md#what-a-spark-is) page.

## Overrun

More than **0.5 A** through the closed contact burns the spot the whisker is on: *"Whisker: burnt spot"*. It hears nothing from then on and the line opens. The other spots are fine. Taking the crystal out and seating it again gives fresh spots.

Galena and Pyrite grow in the ground: see [Ore Deposits](Ore_Deposits.md#crystals-and-clay).
