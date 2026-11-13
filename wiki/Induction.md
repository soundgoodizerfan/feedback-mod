# Induction

Heat made by a changing field inside the metal itself, with no flame and no smoke. Three [MV](Electricity.md) blocks use it: the Induction Coil melts metal in a [Crucible](Crucible.md), the Induction Stove heats magnetic cookware, and the Forging Preheat Coil brings a workpiece to forging heat.

A field only heats what couples to it. The coil and the preheat coil need metal; the stove needs a magnetic material. Everything else sitting in the field stays cold, and a block with nothing to heat draws no current.

## Induction Coil

A coil set against the [Crucible](Crucible.md) it surrounds. It heats the vessel only while the vessel holds metal: a metal item, scrap, or molten metal. An empty crucible, or one holding only food, water or stone, gets nothing.

```
W S W
W C W
W S W
```

W is Copper Fine Wire, S is a Steel Plate and C is Ceramic.

- It faces away from the face you click when placing it, like the [Heating Coil](Heating_Coil.md). The block it faces gets its full heat; see [Heat](Heat.md#heat-sources-have-a-facing).
- It draws **8 A** in the MV band while there is metal to heat.
- Its field follows its power with some inertia and tops out at **1,800 Tu**, hot enough to melt metals the [Heating Coil](Heating_Coil.md) cannot. The [Arc Furnace](Arc_Furnace.md) reaches higher still.

## Induction Stove

A hob for cookware. A [Skillet](Skillet.md) or [Stockpot](Stockpot.md) set on top heats when its material is magnetic: iron, cast iron and steel yes; copper and aluminum pans get nothing.
```
C W C
S S S
```

C is Ceramic, W is Copper Fine Wire and S is a Steel Plate.

- It heats only the block above it, up to **600 Tu**.
- It draws **4 A** in the MV band, and only while magnetic cookware sits on it.

## Forging Preheat Coil

A coil with one slot for a metal workpiece.

```
W W W
C   C
S S S
```

W is Copper Fine Wire, C is Ceramic and S is a Steel Plate.

- **Right-click** with a piece to lay it in the coil; **right-click** empty-handed to take it back.
- **Right-click** empty-handed on an empty coil, or **sneak and right-click** with a piece in it, to step the target temperature: off, then 600, 800, 1,000, 1,200, 1,400, 1,600 and 1,800 Tu. The target shows on the reading card and on the Jade readout.
- Powered, it heats the piece to the target and holds it there while the power lasts. It draws **8 A** in the MV band while the piece is below that heat. It does not know the piece's forging window; choose a target that suits the metal.
- Unpowered, the piece cools in the slot as it would anywhere.
- Only metals heat.

See [Heat Treatment](Heat_Treatment.md) and [Smithing Table](Smithing_Table.md) for the forging windows.

## Automation

None of the three has a switch. Put a [Breaker](Breaker.md) in the supply line to cut them by hand or from a [Controller](Controller.md).
