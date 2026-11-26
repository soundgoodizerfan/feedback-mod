# Pallet Rack

A shelf unit of eight bays that holds the stock of a [Warehouse](Warehouse.md) aisle. It is built from a frame and shelves, and their materials set what it bears.

## Designing and building

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Pallet Rack* in the type row); it needs no treatise. It has two parts:

| Part | Stock | What its material sets |
| --- | --- | --- |
| **Rack Frame** (the uprights) | rod | Its yield strength limits what a bay bears. A frame of a material that burns makes a rack that burns. |
| **Rack Shelf** (the decks) | plate | Its yield strength limits what a bay bears. Its service temperature, or failing that the temperature it fails at, sets how hot a load may be. |

Both materials need a stated yield strength. Shape metal parts at the [Smithing Table](Smithing_Table.md), then assemble them at the [Engineering Table](Engineering_Table.md) with two [fasteners](Fasteners.md); a Pallet Rack blueprint in the blueprint slot judges the fit against your drawing. The tooltip shows the **Bay strength** and the heat the shelves stand.

## Bay strength

The weaker of frame and shelf decides. A yield strength of 250 gives a bay strength of ×1, and the figure scales with the yield strength from ×0.25 to ×2.

| Bay | Holds | Limit at ×1 |
| --- | --- | --- |
| Pallet bay (no fitting) | one kind of stack, up to 4096 items | 1024 [Mu](Units.md#mass-mu) |
| [Bulk Hopper](#bulk-hopper) | one loose stuff, any count | 1024 Mu |
| [Parts Bin](#parts-bin) | up to 8 kinds of small pieces | 128 Mu |

The mass limit is multiplied by the bay strength.

## Using it

A rack has two bays across, two up and two deep. The front and back faces each reach the four bays on that half: click the quarter of the face you mean.

- **Right-click** a bay with an empty hand to read its fitting and contents.
- **Right-click** an empty pallet bay with a fitting to install it.
- **Sneak and right-click** an empty fitted bay with an empty hand to take the fitting out.

The [Warehouse Dock](Warehouse_Dock.md) puts stock in and takes it out. [Jade](Mod_Compats.md#jade) shows how many bays are in use and whether the shelves are worn.

## Fittings

Both are [Crafting Table](Crafting_Table.md) recipes.

### Bulk Hopper

```
P . P
P R P
```

P is a Wrought Iron Plate and R is a Wrought Iron Rod. This makes the **Bulk Hopper Fitting**. The bay holds a single loose stuff by weight alone: a grain or granular material, or any material as dust, crushed rock or ore, concentrate, calcine, tailings, swarf, shavings, loose rock or ore. An item carrying details of its own, such as heat or damage, does not pour into it.

### Parts Bin

```
P R P
P P P
```

P is a Wrought Iron Plate and R is a Wrought Iron Rod. This makes the **Parts Bin Fitting**. The bay holds up to **8** kinds of pieces that each weigh **0.2 Mu** or less.

## Filter

A filtered rack takes only goods recorded in one band of one property by the dock's [reader heads](Warehouse_Dock.md#reader-heads). The filter covers all eight bays.

- **Use a reader module on the rack** to filter by the property it reads. Each use steps to the next band of that property. The module stays in your hand.
- **Sneak and use a reader module** to clear the filter.

Any module that [fits a dock as a head](Warehouse_Dock.md#reader-heads) works, and needs no power for this. Goods with no record for the property are refused. Stock already on the shelves stays there. [Jade](Mod_Compats.md#jade) shows the band the rack takes.

## Hot loads

A load set down hotter than the shelf stands wears the shelves, more the further it is over. Wear lowers the bay strength, down to a quarter of it at worst. The tooltip shows the rack's condition. Stock already on the shelves stays there.

## Fire

A rack whose frame burns catches fire and spreads it like wood. When it catches, its stock spills out as loose items, exposed to the fire.

## Breaking and moving it

A broken rack keeps its stock, fittings and filter. An item holding stock is marked **Loaded: its stock comes with it**. The stock adds to the item's weight. If the item itself is destroyed, the stock spills where it lay. A rack with stock, fittings or a filter on it cannot be worked on or taken apart at the Engineering Table: empty the bays, take the fittings out and clear the filter first.
