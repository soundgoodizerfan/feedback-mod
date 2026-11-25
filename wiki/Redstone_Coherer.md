# Redstone Coherer

A glass tube of loose magnetite grains. It starts **open**. A nearby **spark** welds the grains together and it turns into a piece of wire, and it **stays** one until you tap it. It listens for [sparks](#what-a-spark-is), and it is the first detector that doesn't need a wire running to what it watches. It is the first part to mount on a [Basic Electronic Circuit](Basic_Electronic_Circuit.md).

## Crafting

```
G G G
W M W
```

G is Glass, W is [Copper Wire](Wire_Drawer.md) and M is Magnetite Dust. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, holding Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)); it spends no fasteners.

## Using it

Place it in a line like a [cable](Copper_Cable.md). Open, it splits the line in two; welded, it joins them. Whatever is on the far side (a [Relay](Relay.md)'s electromagnet, a load, a [Current Sensor](Instruments.md#current-sensor) linked to a [Controller](Controller.md)) is what the detection turns on.

- **Right-click with an empty hand** to tap it: *"Coherer tapped: grains loose, open"*.
- **Right-click with a conducting dust** (redstone, say) to shake in fresh grains: one dust gives full [condition](#wear-and-fusing), and the coherer is loose and open again.
- A [Coherer Clapper](#coherer-clapper) seated on it taps it for you.
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

## Coherer Clapper

A small electromagnet and iron rod that taps the grains loose by itself. Without one, only a hand or a [Controller](Controller.md) taps the coherer.

```
W R
P  
```

W is [Copper Wire](Wire_Drawer.md), R is a Wrought Iron Rod and P is a Wrought Iron Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 1 screw ([Fasteners](Fasteners.md)) and Electrical Treatise I: Cells and Static Charge ([Treatise](Treatise.md)).

Right-click a coherer with the clapper to seat it. After a spark welds the coherer, the clapper taps it loose about half a second later, and taps again while the grains hold. A coherer with a clapper therefore closes for a moment on each spark and then opens again.

## Left welded

A welded coherer passes everything, like any wire. Without a clapper, forgetting to tap it is the usual failure. A [fused](#wear-and-fusing) one needs a refill, not a tap.

## Wear and fusing

Each weld wears the grains a little, so the coherer's condition falls with use and a tap frees it less often. Wear from welding alone never takes it below 20%, where one tap in five works. A [Component Tester](Instruments.md#component-tester) reads the condition.

A spark with more than **6 A** behind it fuses the grains into one solid lump. A fused coherer stays welded, hears nothing and shrugs off every tap (*"Coherer: grains fused into one lump, a tap does nothing"*; the tester reads *Fused*). Fresh grains from a conducting dust are the only cure.

## Overrun

More than **2 A** through a welded coherer sinters the grains. Its condition falls (to 20%), and a tap then frees it only some of the time: at 20% condition, one tap in five. It never stops working, you just tap more. Tapping costs nothing.
