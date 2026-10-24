# Redstone Coherer

A glass tube of loose redstone grains. It starts **open**. A nearby **spark** welds the grains together and it turns into a piece of wire, and it **stays** one until you tap it. It is the first detector that doesn't need a wire running to what it listens for.

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

An arc: a [Breaker](Breaker.md) or [Relay](Relay.md) opening with at least **1 A** flowing, or a turn of an [Electrostatic Generator](Electrostatic_Generator.md). It carries **16 blocks**. Your own breakers count, so a coherer beside one that flaps under load will weld itself.

## Left welded

A welded coherer passes everything, like any wire. Forgetting to tap it is the usual failure.

## Overrun

More than **2 A** through a welded coherer sinters the grains. Its condition falls (to 20%), and a tap then frees it only some of the time: at 20% condition, one tap in five. It never stops working, you just tap more. Welding and tapping cost nothing; only the overload wears it.
