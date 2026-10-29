# Electroscope

Two thin leaves of copper foil in a glass jar. They spread apart when they touch something charged. It's the cheapest [electrical](Electricity.md) instrument, and it's deliberately a rough one.

## Crafting

A [Copper Foil](Mechanical_Hammer.md) above a Glass block.

```
F
G
```

## Using it

- **Right-click** any electrical block (cable, cell, generator, magnet) to get a voltage reading on your action bar, like *"Electroscope: 1.0 V"*.
- **Carry it anywhere in your inventory** and Jade's tooltip on electrical blocks shows volts as well as *Live/Dead*.

## What it can't do

It only shows voltage, and only roughly: readings come in steps of **0.5 V**, so a cell's 1.1 V reads as 1.0 V and small differences along a cable won't show up. It can tell you a line is live and roughly how strongly. It **can't** tell you how much current is flowing. For that you need a [Galvanometer](Galvanometer.md).

## Tips

- Touch it to an [Electrostatic Generator](Electrostatic_Generator.md) while someone turns the handle, and you'll see the leaves jump and fall again.
- Compare the reading at a [Galvanic Cell](Galvanic_Cell.md) with the reading at the far end of a long cable. If they differ, the cable is costing you power.

## Fitting a circuit

Put the Electroscope and a live [Basic Electronic Circuit](Basic_Electronic_Circuit.md) in a crafting grid, in any arrangement: the Electroscope comes out carrying the circuit and reads in finer steps (the usual 0.5 V). A quiet circuit takes the step down to about half; a noisy one, such as a cold joint, helps little; a dead one cannot be fitted. A fitted Electroscope still never reads the exact figure. Its tooltip says how fine it reads, and its signature on readings says it is circuit-fitted.
