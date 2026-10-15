# Breaker

A switch for [electricity](Electricity.md). Closed, it's a piece of wire. Open, nothing passes and the line is split in two.

## Crafting

```
C L C
W I W
C C C
```

C is Ceramic, L is a Lever, W is [Copper Wire](Wire_Drawer.md) and I is an Iron Ingot.

## Using it

Place it in a line like a [cable](Copper_Cable.md).

- **Right-click** to open or close it: *"Breaker open"* / *"Breaker closed"*.
- A [Controller](Controller.md) can switch it with a *Switch* card, the same as any other switchable block.

## It trips itself

If more than **4 A** flows through it (a [Copper Cable](Copper_Cable.md)'s rating), it opens on its own. Close it again by hand once you've dealt with the cause.

## Arcing

Opening a breaker while current is flowing draws an **arc** that pits the contacts. Each opening under load costs condition, **1% per amp** broken. A tripped breaker counts too.

Pitted contacts lose voltage: **0.02 V per amp**, more as the condition drops. A worn breaker shows up as a sag on the far side of it. It bottoms out at 25% and keeps switching forever after that.

**Opening it with no current flowing costs nothing.** Switch the load off first, or open the breaker on a line that's already idle, and it lasts forever. A controller flapping a breaker on and off under load with no gap between "on" and "off" will wear it out. See [Controller](Controller.md#holding-a-band).

## Breaker or fuse?

A [Fuse](Fuse.md) blows at a rating you choose by its wire and is used up when it does. A breaker trips at a fixed 4 A and can be closed again, but wears every time it opens under load.
