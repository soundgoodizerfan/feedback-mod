# Warehouse

An aisle of racks served by a carriage on a rail. You feed goods in at one end, the carriage files them away, and you ask for them back by name. It is made of a [Warehouse Dock](Warehouse_Dock.md), a Shuttle Head, Warehouse Rail and [Pallet Racks](Pallet_Rack.md).

## Layout

1. Place the **Warehouse Dock**. Its front faces you as you place it.
2. Behind the dock, place the **Shuttle Head** while looking away from the dock.
3. Lay **Warehouse Rail** onward from the head in one straight line, up to **64** rails long, looking along the line as you place it.
4. Stand **Pallet Racks** against either side of the rail, on the same level, stacked up to **8** high. A column of racks counts from the bottom up to its first gap.
5. Turn the Shuttle Head with a shaft from either side.

A rail turned across the line, or a gap in the line, ends the aisle there. Racks in unloaded chunks are left out until they load: their stock is not listed, and nothing is stored in them or fetched from them.

## Shuttle Head

The drive of the aisle. The carriage's speed follows the speed of its shaft ([Rotation](Rotation.md)), in either direction: about 1 block a second for each 8 RPM. A still shaft stops the carriage where it is.

```
P C P
K S K
P P P
```

P is a Wrought Iron Plate, C is a [Small Cog](Cogs.md), K is a chain and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [Screwdriver](Hand_Tools.md#screwdriver), 4 screws and Mechanical Treatise I ([Treatise](Treatise.md)).

The empty carriage loads the shaft with 8 Su. Each [Mu](Units.md#mass-mu) it carries adds 0.02 Su for every RPM, so a heavy load drags harder the faster the shaft turns, and a weak power source slows under it.

## Warehouse Rail

The track the carriage runs on.

```
R . R
P P P
R . R
```

R is a Wrought Iron Rod and P is a Wrought Iron Plate. A [Crafting Table](Crafting_Table.md) recipe, making 4.

## Trips

The carriage makes one round trip at a time, out to one bay and back. Each block of rail run is one block of travel, and each rack level climbed counts as two. A trip carries up to **256 Mu**, within what the [dock's buffers](Warehouse_Dock.md#intake-and-output) hold. A faster shaft and a nearer bay both shorten the trip.

If the bay is gone or full when the carriage reaches it, the load rides home to the dock.
