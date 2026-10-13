# Temperature Sensor

A thermometer you bolt onto a vessel so it reads all the time, and so a [Controller](Controller.md) can use the reading.

> There's no crafting recipe for it yet. It's in the creative menu.

## Attaching

Right-click a face of a [Crucible](Crucible.md) or an **Improved Furnace** with it. It attaches to that side; one sensor per face.

It **won't** attach to the sealed [furnaces](Furnaces.md) (Furnace, Smoker, Crude Blast Furnace, Kiln, Annealing Furnace). They have no opening for it. Being unwatchable is the price of a sealed furnace.

## Using it

Link it to a program with a [Data Connector](Data_Connector.md) and a *Read Sensor* card on the [Programmer](Programmer.md), then compare the reading with *Greater Than* / *Less Than*.

Its readings are **finer** than a hand-held [Thermometer](Thermometer.md)'s, fine enough to hold steel's narrow window.

## Ideas

- **Bellows thermostat**: *Read Sensor → Less Than (window bottom) → Flip Clutch* on the bellows' shaft.
- **Two-sided control**: one comparison runs the bellows, another opens a [Damper](Damper.md) when it's too hot.
