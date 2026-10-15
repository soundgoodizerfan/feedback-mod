# Crank Linkage

Turns shaft rotation into back-and-forth strokes. Machines that work in strokes, like the [Mechanical Hammer](Mechanical_Hammer.md) and the [Powered Bellows](Powered_Bellows.md), go **directly in front of** it.

## Crafting

```
C P
P S
```

C is a [Small Cog](Cogs.md), P is a [Copper Plate](Mechanical_Hammer.md) and S is a [Shaft](Shaft.md).

## Placing

The shaft goes in the **back** and the machine sits at the **front**. If you place it next to a shaft, it automatically faces away from the shaft so it's the right way round. Otherwise its front faces away from the block you clicked.

The linkage puts the load of whatever it's driving onto your shaft network. A hammer behind a linkage costs as much power as it would anywhere else.

## Throw

The linkage has one setting: its **throw**, **long** or **short**. **Right-click** it to switch. New linkages start on **long**. Jade shows the current throw.

Throw doesn't change how much work gets done over time. It changes **how hard each stroke lands**:

| | Long throw | Short throw |
| --- | --- | --- |
| On a hammer | Gentle blows, 3 St | Hard blows, 12 St |
| Use for | Soft metal, stopping at plate | Hard metal, or making foil on purpose |
| On a bellows | More air per stroke, 30 mB | Less air per stroke, 10 mB |

Short and long make **different products**, not the same product faster or slower. On copper, a short throw skips plate entirely and goes straight to foil.

## Gearing

Gearing between your source and the linkage changes how hard each stroke lands too. A slower, geared-down shaft hits harder but less often; a faster shaft hits softer but more often. Gearing down 2:1 doubles the strength and halves the rate.

The linkage makes one stroke a second at **8 RPM**, four at a hand crank's 32. Use it together with the throw to reach the strength a metal needs. See [Cogs](Cogs.md).

The machine in front has its own **ceiling**. Gearing past it doesn't help, and on a hammer it wears the head.
