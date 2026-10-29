# Current Sensor

A small galvanometer that you bolt onto one face of a [Copper Cable](Copper_Cable.md). It reads the current through that cable all the time, so a [Controller](Controller.md) program can act on it without you standing there.

## Crafting

A [Copper Wire](Wire_Drawer.md) next to a Wrought Iron Nugget.

```
W N
```

## Attaching

Right-click the face of a cable you want it on. It attaches to that side, and you can put one on each of the six faces. Breaking the cable drops the sensor.

It only fits on cable, not directly on sources or machines.

## Linking it to a controller

It works like any other sensor:

1. Right-click the sensor with a [Data Connector](Data_Connector.md).
2. Link it into a program on the [Programmer](Programming.md) with an *Input* card.
3. Compare it with *Greater Than* / *Less Than* and act on the result.

It reads in steps of **0.5 A**, coarser than a hand-held [Galvanometer](Galvanometer.md)'s 0.25 A.

## Ideas

- Watch a cable that's close to its 4 A limit, and open a [Breaker](Breaker.md) (or flip a [Clutch](Clutch.md) on whatever drives the load) when the current climbs too high.
- Detect when a machine at the end of a line starts or stops drawing.
