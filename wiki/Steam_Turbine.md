# Steam Turbine

Turns hot steam into [rotation](Rotation.md) at factory scale: a [Steam Engine](Steam_Engine.md) with a bladed rotor, fast and strong, and hungry for steam.

## Crafting

```
P P P
B S B
```

P is a Steel Plate, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 [fasteners](Fasteners.md).

## Using it

Place it **touching a [Boiler](Boiler.md)** or a boiler at a high [scale](Boiler.md#scale), or feed it steam from a [Pipe](Pipe.md). It pulls steam in by itself. A shaft connects at either end of its axle.

When it has steam it runs at a flat **240 RPM** with up to **64 Su** to give, about 15 kW, drawing **100 mB of steam a tick**. That is ten boilers' output, or one boiler at scale 10. Its store is 1000 mB.

### Steam heat sets the output

It runs only on steam at least **150 Tu** hot. Hotter steam gives more: output steps up every 60 Tu of steam heat above 150 Tu, a quarter of the full 64 Su at the bottom and the whole of it at **330 Tu** or hotter. The speed stays 240 RPM.

## Exhaust

What it uses comes out as steam 80 Tu cooler, in an exhaust tank of its own (1000 mB). A [Condenser](Condenser.md) touching it pulls the exhaust and turns it back into water, as with the engine. If nothing takes the exhaust it vents to the air once the tank is full; the turbine keeps running.

## Controlling it

A [Valve](Valve.md) between the boiler and the turbine shuts the steam off. **Shutting the steam off traps it in the boiler**; see [Boiler](Boiler.md#pressure).

## Overspeed Trip

An [Overspeed Trip](Overspeed_Trip.md) opens above 48 RPM, so one on a turbine's shaft opens at once at 240 RPM. Leave it off the turbine's own line, or put it after gearing that brings the speed down below 48 RPM.

## Driving a generator

A large [Dynamotor](Dynamotor.md) on a 4-turn winding turns 240 RPM into the EHV band, enough for an [Arc Furnace](Arc_Furnace.md#power).
