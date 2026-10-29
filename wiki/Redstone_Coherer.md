# Redstone Coherer

A glass tube of loose redstone grains. It starts **open**. A nearby **spark** welds the grains together and it turns into a piece of wire, and it **stays** one until you tap it. It listens for [sparks](#what-a-spark-is), and it is the first detector that doesn't need a wire running to what it watches. Its glass, copper wire and redstone need no mine trip, which makes it the first part to mount on a [Basic Electronic Circuit](Basic_Electronic_Circuit.md).

## Crafting

```
G G G
W R W
```

G is Glass, W is [Copper Wire](Wire_Drawer.md) and R is Redstone.

## Using it

Place it in a line like a [cable](Copper_Cable.md). Open, it splits the line in two; welded, it joins them. Whatever is on the far side (a [Relay](Relay.md)'s electromagnet, a load, a [Current Sensor](Current_Sensor.md) linked to a [Controller](Controller.md)) is what the detection turns on.

- **Right-click with an empty hand** to tap it: *"Coherer tapped: grains loose, open"*.
- A [Controller](Controller.md) can tap it with a *Switch* card by switching it off. It can't switch it on: only a spark does.

## What a spark is

A spark is the flash you get when current is cut off while it is flowing: the line is broken and the current jumps the gap for an instant. In this game a spark is made three ways, and only these:

- a [Breaker](Breaker.md) opening with at least **1 A** flowing through it,
- a [Relay](Relay.md) opening its contacts with at least **1 A** flowing,
- one turn of an [Electrostatic Generator](Electrostatic_Generator.md), which needs no power at all: right-click it and it sparks.

A spark is not carried along the wire. It is a flash in the air, and anything that listens for it within **16 blocks** hears it, wired to the source or not. That is what makes the coherer a detector with no wire running to the thing it watches. A switch opened with nothing flowing makes no spark, so only a switch that was doing work is heard. Your own breakers count: a coherer beside one that opens under load will weld itself.

## Your first test

1. Lay one line with [Copper Cable](Copper_Cable.md): a [Galvanic Cell](Galvanic_Cell.md), the coherer, and an [Electromagnet](Electromagnet.md). Nothing happens, because the coherer is open and splits the line.
2. A few blocks away, with no cable to that line, put down an [Electrostatic Generator](Electrostatic_Generator.md) and right-click it.
3. The coherer hears the spark and welds, the line closes, and the electromagnet pulls.
4. Right-click the coherer with an empty hand to tap it open again.

The same trick listens to a real machine: put the coherer within 16 blocks of a breaker on a working line, and the day that breaker is opened under load, your separate alarm line switches on.

## Left welded

A welded coherer passes everything, like any wire. Forgetting to tap it is the usual failure.

## Overrun

More than **2 A** through a welded coherer sinters the grains. Its condition falls (to 20%), and a tap then frees it only some of the time: at 20% condition, one tap in five. It never stops working, you just tap more. Welding and tapping cost nothing; only the overload wears it.
