# Warehouse Dock

The end of a [Warehouse](Warehouse.md) aisle: goods go in here, retrieved goods come out here, and its screen lists everything on the racks.

## Crafting

```
P H P
G C R
P C P
```

P is a Wrought Iron Plate, H is a [Hopper](Hopper.md), G is a [Small Cog](Cogs.md), C is any wooden chest and R is a Wrought Iron Rod. Craft it at the [Engineering Table](Engineering_Table.md) with a [Screwdriver](Hand_Tools.md#screwdriver), 4 screws and Mechanical Treatise I ([Treatise](Treatise.md)).

## Intake and output

- **Intake:** feed the dock from any side with a [belt](Conveyor_Belt.md), a [Hopper](Hopper.md) or anything else that hands items to a container.
- **Output:** retrieved goods are pushed out of the front onto a belt or into a container. They can also be pulled out from any side.

The intake and the output each hold **128 Mu**; an empty one always takes at least one item, however heavy. A single item over **256 Mu**, such as a heavily loaded [Pallet Rack](Pallet_Rack.md), is refused.

## Storing

The carriage takes waiting goods to a bay that already holds the same stack and has room. With no such bay, it takes them to the nearest bay with room. A small delivery waits about 2 seconds for more to gather before the carriage sets out.

Goods that no bay takes stay in the intake. **Sneak and right-click the dock with an empty hand** to take back everything waiting in the intake.

## The screen

**Right-click** the dock to open it.

- Click the title and type to name the dock (up to 32 characters). The name is kept when the dock is broken, and a [Catalog Desk](Catalog_Desk.md) lists the dock by it.
- The list on the left shows each distinct stack on the aisle with its count. Type in **Search** to narrow it by name.
- Click a line, set the **Amount** (64 to begin with; **All** fills in the whole count), then press **Fetch** or Enter.
- **Queue**, on the right, lists the retrievals waiting. Click one to drop it. The queue holds up to 32 lines: asking again for a stack adds to its line, and each [Catalog Desk](Catalog_Desk.md) order takes a line of its own.
- The top corner shows the number of racks on the aisle and, while the shaft turns, the time of a round trip to the farthest one.

Retrievals go before storing. While the output is full, retrievals wait and storing carries on.

| Status line | Meaning |
| --- | --- |
| Carriage ready | Nothing holds the aisle up. |
| No shuttle head behind the dock | There is no [Shuttle Head](Warehouse.md#shuttle-head) behind the dock facing away from it. |
| Shuttle shaft is still | The head's shaft is not turning. |
| No bay takes the intake | Something waiting has nowhere to go. |
| Output full: collect it | The next retrieval has no room in the output. |

[Jade](Mod_Compats.md#jade) shows the same status when you look at the dock.

## Catalog Desk

A [Catalog Desk](Catalog_Desk.md) lists and orders the stock of several docks at once. Link a dock to it with the [Data Connector](Data_Connector.md).

## Breaking it

A broken dock drops what is in its intake, its output and on the carriage.
