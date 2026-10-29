# Stonecutter

A vanilla block. Feedback keeps what it does and makes it a part-making bench.

## In vanilla

It cuts one stone block into stairs, slabs, walls and the like: one input, pick the result.

## In Feedback

The stonecutter is a [Woodworking Table](Woodworking_Table.md) with a **copper circular sawblade**. Crafted with a copper plate over a Woodworking Table. It needs **no hand tool**. Mine it with a pickaxe: without one it drops nothing.

Every bench has the same screen: a bench top with a centre slot and a column of slots on the right (the hand tool, a stock slot, a plan slot and the result). Put something in the centre and the bench answers with a **fan** of floating nodes around it; drag to pan, scroll to zoom. What is in the centre decides what the fan shows.

- **Stock** in the centre: the fan shows every shape that stock can become, as icons. Shapes the pile is too small for are faint. Hover one for a ghost of the part by the result slot and a few bars (hardness, toughness, weight); click it to make it. A part takes only the stock it needs and the rest comes back as smaller items: a block that makes a one-ingot part gives the part and eight more ingots (cobble, for stone). **Break down** turns a big piece (a block) into ingot-sized pieces without making anything.
- A **part** in the centre: the fan shows what is already fitted to it (click to take it off), then what you are carrying that attaches, then every part that could attach and would be shaped from the stock slot. Click a node to fit it. Fitting the last part makes the tool, which becomes the centre.
- A **tool** in the centre: the fan shows its parts. Click one to see what you carry that fits that place (swap it, add to an empty place, or mend a worn part with the same stuff) or to take it off; what is left is the part it was built from, with the rest fitted to it.

That vanilla cut stays: a second folder tab, **Cut**, shows the same recipes and results for the block in the centre, one input consumed per result; the **Parts** tab shows the shapes. Shapes are made from stone, ceramic, glass and crystal stock and nothing here is forged. A stone block is nine ingots' worth of stock: **Break down** gives nine cobble, and a part made straight from a block hands the spare back as cobble.

So the benches scale with the blade: complex wooden tools need only an axe, stone ones need the copper blade, and metal ones a smithing table and a hammer.

Metal parts are made at the [Smithing Table](Smithing_Table.md); organic ones at the [Woodworking Table](Woodworking_Table.md). The screen is cobblestone.
