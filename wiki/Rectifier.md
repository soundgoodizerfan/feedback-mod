# Rectifier

A fitting that makes a [battery](Battery.md) beside it charge in about 1 minute from empty instead of about 2.5.

## Crafting

```
W X W
P P P
```

W is [Copper Wire](Wire_Drawer.md), X is a [Cat's Whisker Detector](Cats_Whisker_Detector.md) and P is a Wrought Iron Plate.

## Using it

Place it touching a battery on any face and wire it to an MV (6 V) [electrical](Electricity.md) line like any other load. While the line feeds it, the battery next to it charges at the faster rate, whenever the battery itself is charging. It picks up a changed neighbor within about a second.

It draws **0.25 A** at MV whether or not a battery is there, and does nothing on its own.

## How it works

The faster charge is a larger current into the battery: the line must be able to supply it. The extra current heats the battery more, and a lead-acid battery gasses sooner. See [Battery](Battery.md#misuse-and-hazards).
