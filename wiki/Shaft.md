# Shaft

Carries [rotation](Rotation.md) in a straight line along its length. It's the most basic block in any machine setup.

## Crafting

Three ingots of any metal with a yield strength of **7.5 St** or more, such as bronze, iron or steel, in a vertical line make **four** shafts.

```
C
C
C
```

## Placing

A shaft points along the face you click. To make a long line quickly, **right-click the end of an existing shaft while holding a shaft**. The new one goes on at the far end of the line instead of where you clicked. Clicking the side of a shaft extends the line away from you.

## Friction

Every shaft costs the network **0.1 Su per RPM**: at a steam engine's 16 RPM, 1.6 Su a shaft. On long lines, swap in [Bearings](Bearing.md) (0.04) to cut the loss. A [Levitation Sleeve](Levitation.md#levitation-sleeve) fitted to a shaft removes most of its friction while a powered [Electromagnet](Electromagnet.md) touches it.

### Unsupported spans

A shaft needs something to turn in. A straight run of more than **4** plain shafts in a row (a [Flywheel](Flywheel.md) counts as one) with no support costs **twice** the friction per shaft. A [Bearing](Bearing.md) or any other block in the line, such as a machine or a cog, ends the run and supports the shafts beside it. A bearing every 4 blocks keeps a long line at normal friction.

## See also

- [Cogs and Gearbox](Cogs.md): changing speed and direction
- [Pulleys](Pulleys.md): sending power a long way without a shaft
