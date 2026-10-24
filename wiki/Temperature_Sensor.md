# Temperature Sensor

A thermometer you bolt onto a vessel so it reads all the time, and so a [Controller](Controller.md) can use the reading.

## Attaching

Right-click a face of a [Crucible](Crucible.md) or an **Improved Furnace** with it. It attaches to that side; one sensor per face.

It **won't** attach to the sealed [furnaces](Furnaces.md) (Furnace, Smoker, Crude Blast Furnace, Kiln, Annealing Furnace). They have no opening for it. Being unwatchable is the price of a sealed furnace.

## Using it

Link it to a program with a [Data Connector](Data_Connector.md) and an *Input* card on the [Programmer](Programming.md), then compare the reading with *Greater Than* / *Less Than*.

It reads in steps of **10 Tu**, finer than a hand-held [Thermometer](Thermometer.md)'s 25, fine enough to hold steel's 60 Tu window.

## Ideas

- **Bellows thermostat**: *Input → Less Than (window bottom) → Switch* on the bellows' shaft.
- **Two-sided control**: one comparison runs the bellows, another opens a [Damper](Damper.md) when it's too hot.
