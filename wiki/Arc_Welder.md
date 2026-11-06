# Arc Welder

A hand tool that mends a worn part with filler metal and a battery's charge. A repaired part is permanently a little weaker than it was.

## Crafting

```
P C
  I
  S
```

P is a Wrought Iron Plate, C is a Copper Rod, I is a Wrought Iron Rod and S is a Stick.

## Using it

It needs a charged [Battery](Battery.md) and an ingot of the part's own wear material in your inventory.

- **A tool, weapon, armor piece, gun or hammer:** hold the welder in your main hand and the worn item in your off hand, then hold right-click for about 2 seconds.
- **A worn machine:** right-click it with the welder. This works on the shop machines, the [Mechanical Hammer](Mechanical_Hammer.md), the [Breaker](Breaker.md), the [Dynamotor](Dynamotor.md) and the [Battery](Battery.md). It takes iron if the machine has no material of its own.

A weld costs **2,000 amp-ticks** of charge and one ingot. The status line reports "Nothing to weld.", "No charged battery in your pack." or which ingot is missing.

## Weak spots

Every part has a best condition it can reach. A weld restores the part to that best and then lowers the best by **0.1**, down to **0.5**. A part welded again and again is never as good as new, and cannot be mended past half.

The welder's own electrode wears a little with each weld and keeps working; its tooltip reads sound, marked, battered or spent.

See [Guns](Guns.md#wear) for gun wear.
