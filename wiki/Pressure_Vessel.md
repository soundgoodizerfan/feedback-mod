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

## Canning

Set the vessel on a [Firebox](Firebox.md) (or lava) and it can sterilize jars of food (see [Food](Food.md)). Right click with a jarred food to put one in (up to four); sneak with an empty hand to take the last one out; breaking the vessel drops them. Pressure raises the boiling point, so with a fire under it the inside climbs past the 100 Tu a pot tops out at, up to 6 Tu for each Pu. Sterilising needs 116 Tu, which about 2.7 Pu and a fire that hot give. That is well under the 5 Pu danger line: pump to the working window, not to the limit. A jar of food is not a hazard, and jars do not change when the vessel bursts.

Nothing draws the stored air back out, and no instrument reads its pressure. It's mostly a demonstration that equipment can be pushed past its limits, and a reason to put a [Valve](Valve.md) or [Clutch](Clutch.md) on a bellows line.

With Jade, the vessel shows its inside temperature (read as a thermometer would), how many jars it holds, how many are sealed or gone mushy, and how far the slowest jar is from sealing (in words; the exact percentage only at the truth tier). A jar not yet sealed says *Partly sterilized* in its tooltip once heating has begun.
