# Cogs and Gearbox

Cogs change **speed** (and with it, force) by their tooth counts. The gearbox changes **direction**, by turning power round a corner.

## Obtaining

A cog has a **tooth count** and a metal. Cut one from a metal plate at the [Gear Cutter](Gear_Cutter.md), at 8, 12, 16, 24 or 32 teeth. A cog of 8 or 12 teeth is a **Small Cog**; one of 16, 24 or 32 teeth is a **Large Cog**. A cut cog's tooltip names its metal and tooth count.

A wooden cog is crafted from planks. A wooden Small Cog has 12 teeth and a wooden Large Cog 24.

**Wooden Small Cog:**

```
  P
P S P
  P
```

P is any Planks and S is a [Shaft](Shaft.md).

**Wooden Large Cog:**

```
  P
P C P
  P
```

P is any Planks and C is a Small Cog.

## How cogs work

A cog sits on a shaft line and turns with it. A Large Cog is drawn wider than its block, but it still takes only one block. Which positions mesh depends on whether the cogs are small or large; the **ratio** between two meshing cogs is their tooth counts. The cog with fewer teeth turns faster, with the force shared the other way.

| Driving | Driven | Where | Result |
| --- | --- | --- | --- |
| Small | Small | Side by side, flat in the same plane | Speed in the ratio of the teeth, reversed |
| Large | Small | **Diagonal** in the same plane, the small one on the large one's rim | Speed in the ratio of the teeth, reversed |
| Small | Large | Diagonal in the same plane | Speed in the ratio of the teeth, reversed |
| Large | Large | **At right angles**: one block along each cog's axis, diagonally | Speed in the ratio of the teeth, turned through 90° |

Two 12-tooth cogs turn at the same speed, reversed. A 24-tooth Large Cog driving a 12-tooth Small Cog turns it **twice** as fast with half the force; the other way round the small one drives the large at **half** speed with twice the force. A 32-tooth cog driving an 8-tooth one turns it four times as fast.

Two Large Cogs side by side in one plane don't mesh, and neither do a Small and a Large Cog side by side. Two cogs stacked end to end on the same shaft don't mesh either; they just turn together. Gearing that loops back on itself and disagrees jams the line.

When placed, a cog takes the axis of whatever you placed it against. **Sneak** while placing to point it along the face you clicked instead. A cut cog keeps its metal and tooth count when it is placed and when it is mined.

### Why gear down?

Slower and stronger is how you get a [hammer](Mechanical_Hammer.md) to hit hard enough for hard metals without switching to a short throw. Every metal has a hardness, and a blow below it does nothing. See [Metalworking](Metalworking.md).

Gearing **up** makes things faster, but the fast branch costs more power to friction. Gear up far enough and you can trip an [Overspeed Trip](Clutch.md#overspeed-trip).

## Gearbox

```
P C P
C S C
P C P
```

C is a Small Cog, S is a [Shaft](Shaft.md) and P is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 4 machine bolts ([Fasteners](Fasteners.md)). It needs Mechanical Treatise II: Transmission ([Treatise](Treatise.md)).

A gearbox connects shafts on **any of its six faces** at the same speed, so you can take power round corners and split it. Its friction is **0.2 Su per RPM**, twice a plain shaft's 0.1.

To split one shaft into two axles that share speed by load, see the [Differential](Differential.md). The gearbox has no settings. Which way each output turns depends on which faces you use: shafts on **opposite** faces turn opposite ways. If something's spinning the wrong way, add a pair of cogs to flip it.
