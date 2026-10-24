# Steam Engine

Turns steam into [rotation](Rotation.md).

## Crafting

```
P P
S S
```

P is a [Steel Plate](Mechanical_Hammer.md) and S is a [Shaft](Shaft.md).

## Using it

Place it **touching a [Boiler](Boiler.md)** (or anything else that offers steam, including other mods' steam). It pulls steam in by itself. A shaft connects at either end of its axle.

When it has steam it runs at a flat **16 RPM** with **64 Su** to give, drawing **10 mB of steam a tick**: one boiler's full output. When the steam runs out it stops and the line coasts down. It's heavy, like a flywheel, so it smooths out the speed of the line. Its small store (200 mB) carries it over a stutter of about a second.

## Exhaust

What it burns comes out as cooler steam (40 Tu lower), in an exhaust tank of its own (200 mB). The engine only runs on steam at least 110 Tu hot. A [Condenser](Condenser.md) touching it pulls the exhaust and turns it back into water. A second engine can run on the exhaust only while it is still 110 Tu or hotter.

If nothing takes the exhaust, it **vents to the air** once the tank is full. The engine keeps running; you just lose the water. Close the loop with a condenser and a [Pump](Pump.md) and the boiler never needs refilling.

## How much it can drive

64 Su is **most of a [Mechanical Hammer](Mechanical_Hammer.md)** (80 Su). One engine on its own can't quite carry a hammer. Put two engines on the same line, each with its own boiler, or share the line with a [Water Wheel](Water_Wheel.md) (256 Su).

## Controlling it

Put a [Valve](Valve.md) between the boiler and the engine to shut the steam off. The engine stops when its own store runs dry. A [Controller](Controller.md) can work the valve.

**Shutting the steam off traps it in the boiler.** On a hot fire a shut boiler builds pressure and can burst. Turn the fire down (or let it run out) when you shut an engine off for long. See [Boiler](Boiler.md#pressure).

At 16 RPM a steam engine never trips an [Overspeed Trip](Overspeed_Trip.md) (48 RPM) unless you gear it up threefold. It's also the speed the [Dynamo](Dynamo.md)'s windings are set against: 1.5 V coarse, 6 V fine.
