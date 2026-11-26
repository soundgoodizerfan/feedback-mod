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

The carriage picks a bay with room for the waiting goods, in this order:

1. A bay that already holds the same stack.
2. A bay on the nearest [filtered rack](Pallet_Rack.md#filter) that accepts the goods.
3. The nearest bay on an unfiltered rack.

A filtered rack that does not accept the goods is passed over, even if it holds the same stack. A small delivery waits about 2 seconds for more to gather before the carriage sets out.

Goods that no bay takes stay in the intake. **Sneak and right-click the dock with an empty hand**, on a face without a reader head, to take back everything waiting in the intake.

## Reader heads

[Belt modules](Belt_Modules.md#sorting) fitted to the dock read goods on their way to the racks. **Use the module on either side or the top** of the dock to fit it, one to a face.

| Head | Records |
| --- | --- |
| Belt Magnet, Belt Electromagnet | Magnetism |
| Belt Optical Reader | Sheen |
| Belt Weigher | Density |
| Belt Scratch Reader | Hardness |
| Belt Drag Reader | Grip |
| Belt Spark Reader | Kind |
| Belt Heat Reader | Heat |
| Belt Screen | Size, by the weight of one piece: small, mid or large |

- What the heads read is **recorded with the goods** and kept in their bay. A later delivery of the same stack to that bay replaces the record for each property read. An item with no known material gets no record.
- Heads have the [weaknesses they have on a belt](Belt_Modules.md#sorting) and can record the wrong band near a band edge. The weigher judges a single piece here, so a stack does not blur it. A second head on the same property lowers the chance of a mistake.
- The Electromagnet, Optical Reader, Weigher and Heat Reader need an **LV** wire against the dock and draw **1 A** each. Without current they record nothing.
- An Optical Reader or Screen fouls after **64** loads. A dirty optical reader reads everything duller; a fouled screen records nothing. **Right-click the head with an empty hand** to wipe it. [Jade](Mod_Compats.md#jade) shows when a head wants wiping.
- **Sneak and right-click a head with an empty hand** to take it off.

## The screen

**Right-click** the dock to open it.

- Click the title and type to name the dock (up to 32 characters). The name is kept when the dock is broken, and a [Catalog Desk](Catalog_Desk.md) lists the dock by it.
- The list on the left shows each distinct stack on the aisle with its count. Type in **Search** to narrow it by name, or use the [Filter sheet](#filter-sheet).
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

## Filter sheet

Press **Filter** to open a sheet over the stock list and queue, and press it again to return. It narrows the stock list by what the [reader heads](#reader-heads) recorded.

- Click a band to pick it, and click it again to drop it. The rows are Magnet, Sheen, Density, Hardness, Grip, Kind, Heat and Size.
- Several bands picked in one row match **any** of them. Every row with a pick has to match.
- **Count** and **Mu** set the least number of items and the least weight a line has to total.
- Goods stored without a head for a property are not listed while you filter on that property.

The button shows how many filters are on.

## Watching stock

The four **watch slots** at the bottom right of the screen each hold one kind of goods. Pick a line in the stock list, then **click a slot** to watch it; **right-click** a slot to clear it.

## Control

The dock is a data node: link it with the [Data Connector](Data_Connector.md) to a [Controller](Controller.md) (see [Sensors](Sensors.md)). It offers five channels.

| Channel | Sensor card reads | Actuator card does |
| --- | --- | --- |
| 0, the switch | Nothing | Off holds retrievals while storing goes on; on lets them run |
| 1 to 4 | The count of the goods in that watch slot that the aisle holds, or 0 when the slot is empty | A rising edge (off to on) queues a retrieval of one stack of the watched item |

- The queued stack is the item's full stack size. It is added to a line already waiting for that item, and it is limited to what the aisle holds beyond what the queue already asks for.
- The goods come out like any other retrieval, at the output.
- Holding a channel on asks once; it has to turn off and on again to ask again.

## Catalog Desk

A [Catalog Desk](Catalog_Desk.md) lists and orders the stock of several docks at once. Link a dock to it with the [Data Connector](Data_Connector.md).

## Breaking it

A broken dock drops its reader heads and what is in its intake, its output and on the carriage.
