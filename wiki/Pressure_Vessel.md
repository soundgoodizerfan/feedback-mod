# Pressure Vessel

A sealed steel tank you pump air into with a [Powered Bellows](Powered_Bellows.md). **Pump too much in and it explodes.**

## Crafting

```
S S S
S   S
S S S
```

S is a [Steel Ingot](Steel.md).

## Using it

Put a bellows against it and run the bellows. Air builds up inside: **1000 mB of air is 1 Pu**. It leaks back out by itself at 1 mB a tick, so an idle vessel slowly empties.

| Pressure | What happens |
| --- | --- |
| up to 5 Pu | nothing, however long it sits there |
| above 5 Pu | each tick there's a chance it **explodes**, climbing the further over it is |
| 7 Pu | it **explodes** |

The explosion damages blocks around it. A long-throw bellows (30 mB a stroke) needs about 170 strokes to take one from empty to 5 Pu, plus whatever leaks out meanwhile.

The [Boiler](Boiler.md) follows the same ladder.

Nothing draws the stored air back out yet, and no instrument reads its pressure. For now it's mostly a demonstration that equipment can be pushed past its limits, and a reason to put a [Valve](Valve.md) or [Clutch](Clutch.md) on a bellows line.
