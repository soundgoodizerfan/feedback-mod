# Cogs and Gearbox

Cogs change **speed** (and with it, force). The gearbox changes **direction**, by turning power round a corner.

## Small Cog

```
  P
P S P
  P
```

P is any Planks and S is a [Shaft](Shaft.md).

## Large Cog

```
  P
P C P
  P
```

P is any Planks and C is a Small Cog.

## How cogs work

A cog sits on a shaft line and turns with it. A Large Cog is drawn wider than its block, but it still takes only one block. Two cogs mesh where their teeth reach, as in Create:

| Driving | Driven | Where | Result |
| --- | --- | --- | --- |
| Small | Small | Side by side, flat in the same plane | Same speed, reversed |
| Large | Small | **Diagonal** in the same plane, the small one on the large one's rim | **Twice** as fast, reversed, half the force |
| Small | Large | Diagonal in the same plane | **Half** as fast, reversed, twice the force |
| Large | Large | **At right angles**: one block along each cog's axis, diagonally | Same speed, turned through 90° |

Two Large Cogs side by side in one plane don't mesh, and neither do a Small and a Large Cog side by side. Two cogs stacked end to end on the same shaft don't mesh either; they just turn together. Gearing that loops back on itself and disagrees jams the line.

When placed, a cog takes the axis of whatever you placed it against. **Sneak** while placing to point it along the face you clicked instead.

### Why gear down?

Slower and stronger is how you get a [hammer](Mechanical_Hammer.md) to hit hard enough for hard metals without switching to a short throw. Every metal has a hardness, and a blow below it does nothing. See [Metalworking](Metalworking.md).

Gearing **up** makes things faster, but the fast branch costs more power to friction. Gear up far enough and you can trip an [Overspeed Trip](Overspeed_Trip.md).

## Gearbox

```
  C
C S C
  C
```

C is a Small Cog and S is a Shaft.

A gearbox connects shafts on **any of its six faces** at the same speed, so you can take power round corners and split it. Its friction is **0.2 Su per RPM**, twice a plain shaft's 0.1.

It has no settings. Which way each output turns depends on which faces you use: shafts on **opposite** faces turn opposite ways. If something's spinning the wrong way, add a pair of cogs to flip it.
