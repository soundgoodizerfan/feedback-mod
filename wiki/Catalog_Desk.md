# Catalog Desk

One searchable list over several [Warehouses](Warehouse.md). Link it to their [Warehouse Docks](Warehouse_Dock.md), then search and order from all of them at one block.

## Crafting

```
P G P
D E D
P W P
```

P is a Wrought Iron Plate, G is a glass pane, D is any wooden chest, E is a [Basic Electronic Circuit](Basic_Electronic_Circuit.md), which the finished desk keeps, and W is [Copper Wire](Wire_Drawer.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver), 4 screws ([Fasteners](Fasteners.md)) and Electrical Treatise II: Coils and Magnets ([Treatise](Treatise.md)).

## Setting it up

1. Place the desk. Its front faces you as you place it.
2. Run an **LV** wire to a side of it ([Electricity](Electricity.md)). The desk draws **0.5 A**.
3. With the [Data Connector](Data_Connector.md#linking-nodes), right-click the desk, then a Warehouse Dock. Link each dock to the desk itself: the desk reads only the docks wired directly to it.

The circuit's **speed** sets how many docks the desk reads, from **4** with the slowest to **32** with the fastest. With more docks linked than that, it reads the nearest ones.

With no working circuit or no current the desk is dark: it lists nothing and takes no orders. The docks carry on working by themselves, including retrievals already queued.

## The screen

**Right-click** the desk to open it.

- The list on the left shows each distinct stack with its total across the docks. Type in **Search** to narrow it by name. Hover a line to see how many each dock holds.
- **Filter** opens the same [Filter sheet](Warehouse_Dock.md#filter-sheet) as a dock's. A stack is listed if the stock of at least one dock matches the picked bands; **Count** and **Mu** apply to the total across docks.
- Click a line, set the **Amount** (64 to begin with; **All** fills in the whole count), then press **Fetch** or Enter.
- **Queue**, on the right, lists the retrievals waiting at the docks. Hover one to see its dock and, for an order from a desk, the order number. Click it to drop it from that dock's queue.
- The top corner shows the number of docks. Hover the status line to see how each dock is doing, in the words of the [dock's status line](Warehouse_Dock.md#the-screen).

A dock goes by the name typed on [its screen](Warehouse_Dock.md). Docks with no name are called **Dock 1**, **Dock 2** and so on, nearest first, each with its distance in blocks. A dock in an unloaded chunk shows as **Away**, and its stock is left out of the list.

| Status line | Meaning |
| --- | --- |
| Docks ready: (number) | Every dock read is ready. |
| Docks held up: (number) | That many docks are away or held up. |
| No docks linked | No dock is wired to the desk. |
| No working circuit | The desk's circuit is missing or dead. |
| No current | No power reaches the desk. |

[Jade](Mod_Compats.md#jade) shows why a dark desk is dark.

## Orders

An amount you ask for is shared among the docks holding that stack, starting with the one able to deliver soonest. Docks with a shorter queue, a nearer bay and a faster shaft come first; a dock that cannot deliver at the moment comes last. Each share joins its dock's queue and comes out of that dock's [output](Warehouse_Dock.md#intake-and-output), so one order can arrive in several places.

A dock with a full queue cannot take a share. If that leaves the order short, the message "The dock queue is full" appears.

## Circuit

The circuit is built in, and the desk keeps it when broken. A dead one is swapped, or the circuit recovered, at the [Engineering Table's Repair tab](Engineering_Table.md#repair-tab).
