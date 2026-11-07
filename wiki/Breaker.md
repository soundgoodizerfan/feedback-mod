# Breaker

A switch for [electricity](Electricity.md). Closed, it's a piece of wire. Open, nothing passes and the line is split in two.

## Crafting

```
C L C
W I W
C C C
```

C is Ceramic, L is a Lever, W is [Copper Wire](Wire_Drawer.md) and I is a Wrought Iron Plate. This makes a standard breaker.

Put two [cables](Copper_Cable.md) of one gauge where the wires go and the breaker takes that gauge: thin, standard, heavy or bus bar. Two cables of different gauges make nothing.

## Using it

Place it in a line like a [cable](Copper_Cable.md).

- **Right-click** to open or close it: *"Breaker open"* / *"Breaker closed"*.
- A [Controller](Controller.md) can switch it with a *Switch* card, the same as any other switchable block. A Controller with [Current Sensor](Instruments.md#current-sensor) inputs can open the Breakers on low-priority branches when supply sags: see [Substation](Substation.md).

## It trips itself

If more than its rating flows through it, it opens on its own. The rating is that of the cable it was built from, for copper **1 A** thin, **4 A** standard, **16 A** heavy and **64 A** bus bar (see [Copper Cable](Copper_Cable.md#how-much-it-carries)). Close it again by hand once you've dealt with the cause.

## Arcing

Opening a breaker while current is flowing draws an **arc** that pits the contacts. Each opening under load costs condition, **1% per amp** broken. A tripped breaker counts too.

Pitted contacts lose voltage: **0.02 V per amp** in a standard breaker, less in a thicker gauge (a quarter as much per step up) and more in a thinner one, and more as the condition drops. A worn breaker shows up as a sag on the far side of it. It bottoms out at 25% and keeps switching forever after that.

**Opening it with no current flowing costs nothing.** Switch the load off first, or open the breaker on a line that's already idle, and it lasts forever. A controller flapping a breaker on and off under load with no gap between "on" and "off" will wear it out. See [Controller](Controller.md#holding-a-band).

## Arc flash

At about **EHV (384 V)** or more, a breaker close to its trip current hums and sparks as a warning. Opening one under heavy current at that voltage throws an **arc flash**: every living thing within **3 blocks** is burned (hot damage) and set alight. A player whose gear insulates for that voltage is spared. Open the load first, or keep a trunk breaker out of reach. See [Hazards](Hazards.md#arc-flash).

## Breaker or fuse?

A [Fuse](Fuse.md) blows at a rating you choose by its wire and is used up when it does. A breaker trips at its gauge's rating and can be closed again, but wears every time it opens under load.
