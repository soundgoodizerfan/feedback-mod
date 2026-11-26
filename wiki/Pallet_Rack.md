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
| [Cabinet](#cabinet) | up to 16 single pieces | 256 Mu |

The mass limit is multiplied by the bay strength.

## Using it

A rack has two bays across, two up and two deep. The front and back faces each reach the four bays on that half: click the quarter of the face you mean.

- **Right-click** a bay with an empty hand to read its fitting and contents.
- **Right-click** an empty pallet bay with a fitting to install it.
- **Sneak and right-click** an empty fitted bay with an empty hand to take the treatment out first, then the container fitting.

A bay takes one container fitting and one optional [treatment](#treatments), installed only while the bay is empty.

The [Warehouse Dock](Warehouse_Dock.md) puts stock in and takes it out. [Jade](Mod_Compats.md#jade) shows how many bays are in use and whether the shelves are worn.

## Fittings

The containers and treatments are all [Crafting Table](Crafting_Table.md) recipes.

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

### Cabinet

```
P R P
W W W
P R P
```

P is a Wrought Iron Plate, R is a Wrought Iron Rod and W is any plank. This makes the **Cabinet Fitting**. The bay holds up to **16** single pieces of any item, each keeping its own state: wear, charge and the like.

## Treatments

A treatment goes on a bay beside its container fitting.

### Cold Plate

```
C C C
R P R
```

C is a Copper Plate, P is a Wrought Iron Plate and R is a Wrought Iron Rod. This makes the **Cold Plate Fitting**. Food in the bay spoils at the cold rate while a [Refrigeration Unit](Refrigeration_Unit.md) touches or cools the rack. With no running unit the bay spoils at the room's rate and looks fine until opened.

### Sealed Liner

```
L R L
L . L
L L L
```

L is a Lead Plate and R is a Wrought Iron Rod. This makes the **Sealed Liner Fitting**. Radioactive goods in a lined bay give off only what a lead lining lets out; the goods in a bare bay radiate in full. See [Hazards](Hazards.md).

## Cooling the rack

A rack is refrigerable like the [Ice Box](Ice_Box.md): a Refrigeration Unit against it holds it cold while powered.

## Goods over time

Goods on the shelves change with time: hot cargo cools toward room temperature and food rots. An item is brought up to date when it is retrieved, and when a [Catalog Desk](Catalog_Desk.md) or dock reads the stock, about every 10 seconds. The temperature band shown in searches comes from the goods' current temperature.

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
