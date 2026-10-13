# Lathe

Spins a rod and cuts it continuously with a tool bit, for as long as it's turning. Rods become turned rods, and if you leave them they become swarf.

## Crafting

**Lathe:**

```
I I I
B S B
I I I
```

I is an Iron Ingot, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md).

**Lathe Tool Bit:** two Iron Ingots stacked vertically.

## Setting it up

The lathe sits **directly on a shaft line**; connect it from any side. The faster it turns, the faster it cuts.

It has two slots:

- **Right-click with a Lathe Tool Bit** to fit (or swap) the bit.
- **Right-click with anything else** to chuck a workpiece (or swap it).
- **Sneak-right-click with an empty hand** switches between a **light** and a **heavy** cut.

There's no cutting without a bit.

## What it makes

**Rod → Turned Rod → Swarf**

Like every machine, it doesn't stop when the rod is finished. Leave it and it cuts the rod away to swarf. Swarf also pops out of the top while it works.

The bit can cut anything up to and including **steel**. Hardened steel needs the [Grinding Wheel](Grinding_Wheel.md).

## Light vs. heavy cut

**Heavy** cuts much faster. **Light** is slower, so there's more time to catch the turned rod before it's ruined.

## The lathe is also a sensor

The lathe reports **how much it has cut off the piece in it**. Link it with a [Data Connector](Data_Connector.md) and read it in a [Controller](Controller.md) program, then compare it and flip a [Clutch](Clutch.md) to stop the lathe at exactly the right point. It's one of the cleanest automatic-stop setups in the game.

[Calipers](Calipers.md) in your inventory show the same progress in Jade.

## Wear

Here **the bit** wears, not the lathe. It wears when it's asked to cut something too hard for it, or keeps cutting once there's nothing left. A worn bit cuts less each pass, but it never stops cutting what it could cut before. Swap in a fresh bit when it gets slow.
