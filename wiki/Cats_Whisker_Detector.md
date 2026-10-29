# Cat's Whisker Detector

A detector that hears sparks without being wired to them. A **spark** is the flash made when a [Breaker](Breaker.md) or [Relay](Relay.md) opens with current flowing, or when an [Electrostatic Generator](Electrostatic_Generator.md) is turned (see [what a spark is](Redstone_Coherer.md#what-a-spark-is)). The whisker sits in a line like a cable and normally breaks it. When a spark goes off within earshot it closes the line for half a second, then opens again by itself. So it turns "something nearby just switched under load" into a short pulse on its own line, which can run an alarm, a relay or a [Current Sensor](Current_Sensor.md).

It is the [Redstone Coherer](Redstone_Coherer.md)'s fussier cousin. The coherer welds shut and stays shut until you tap it. The whisker lets go by itself, so you never reset it, but it needs a crystal found in the ground and a patient hand: a wire laid on a crystal only works on a lucky spot, and finding that spot is the job. A good spot hears sparks from further away.

**First thing to do with it:** seat a crystal, step the whisker to a spot that reports *clear* or *sharp*, put it in a line with a [Galvanic Cell](Galvanic_Cell.md) and an [Electromagnet](Electromagnet.md), and turn an Electrostatic Generator a few blocks off. Each turn gives one click of the magnet.

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
