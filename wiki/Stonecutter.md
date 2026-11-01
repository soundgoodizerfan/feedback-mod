# Stonecutter

A vanilla block. Feedback keeps what it does and makes it a part-making bench.

## What vanilla does

It cuts one stone block into stairs, slabs, walls and the like: one input, pick the result.

## Crafting

```
s W s
p p p
```

s is a Stone Slab, W is a [Woodworking Table](Woodworking_Table.md) and p is any wooden slab. The tooltip reads "Also cuts wood!".

## What Feedback does

The stonecutter takes a [Hand Saw](Hand_Saw.md) in its own slot. Every act here (shaping a part, breaking stock down, a cut) wears the saw, and without a saw in the slot nothing can be done: the status line reads "Put a hand saw in the saw slot." Mine it with a pickaxe: without one it drops nothing.

The screen is the [Smithing Table](Smithing_Table.md)'s: a center slot, a **fan** of nodes around whatever is in it, and a column of slots on the right. Putting a part or a tool in the center works as described there. Stock in the center works like this:

- **Stock** in the center: the fan shows every shape that stock can become, as icons. Shapes the pile is too small for are faint. Hover one for a ghost of the part by the result slot and a few bars (hardness, toughness, weight); click it to make it. A part takes only the stock it needs and the rest comes back as smaller items: a block that makes a one-ingot part gives the part and eight more ingots (ingots, for stone). **Break down** turns a big piece (a block) into ingot-sized pieces without making anything.

A second folder tab, **Cut**, shows the cuts for what is in the center, one input consumed per result; the **Parts** tab shows the shapes. For a stone block the Cut tab lists the vanilla stonecutting results. Shapes are made from stone, ceramic, glass and crystal stock and nothing here is forged. A stone block is nine ingots' worth of stock: **Break down** gives nine stone ingots, and a part made straight from a block hands the spare back as stone ingots.

### Wood

Put a plank or a log in the center and the Cut tab lists the same wood cuts as the [Woodworking Table](Woodworking_Table.md#what-it-does)'s Saw tab: stairs, slabs, doors, trapdoors, fences, signs, boats, chests, bowls, sticks, a log sawn into planks, a log stripped. A log counts as four planks. The saw does the work here, not an axe, and each cut wears it.

The benches each take their own tool: an axe for wood, a saw for stone, a hammer for metal. The stonecutter's saw also cuts wood.

Metal parts are made at the [Smithing Table](Smithing_Table.md); organic ones at the [Woodworking Table](Woodworking_Table.md). The screen is cobblestone.
