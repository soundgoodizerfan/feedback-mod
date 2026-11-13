# Light Meter

A photocell behind a pinhole. It reads the light at the face of a block, block light and sky light apart.

## Crafting

```
G
W
N
```

G is a Glass Pane, W is [Copper Wire](Wire_Drawer.md) and N is a Wrought Iron Nugget.

## Using it

**Right-click** a block to read the light at the face you click, as *Block* and *sky* levels on the card, in steps of **2 levels**. Carried in your inventory it counts as an [instrument](Instruments.md).

Clicking a block that gives light (a lamp, a torch) sets the meter against that known light and reads the lamp's own level; this puts a drifted meter back on true.

## Drift

Read a face brighter than level **13** and the cell saturates and drifts. A hard fall knocks it off too. See [Drift](Instruments.md#drift).
