# Smithing Table

A vanilla block. Feedback keeps the block and changes what it is for.

## In vanilla

It upgrades diamond gear to netherite and applies armor trims, using smithing templates.

## In Feedback

The Smithing Table is where **metal parts** are shaped and forged, played as the forging rhythm game, with a [Hand Hammer](Hand_Hammer.md) in the hammer slot. Stock must be metal: a material of kind metal, or any material with a hot-working window. A [Hearth](Hearth.md) set beside it keeps the stock hot. The block breaks fastest with an axe.

Every bench has the same screen: a bench top with a centre slot and a column of slots on the right (the hand tool, a stock slot, a plan slot and the result). Put something in the centre and the bench answers with a **fan** of floating nodes around it; drag to pan, scroll to zoom. What is in the centre decides what the fan shows. The client setting **reduceMotion** (in the mod's client config) stills the fan's drifting nodes, the shake and the idle animations; sounds stay.

- **Stock** in the centre: heated pieces of one item put into the stock slot, by click or shift-click, are blended to their count-weighted mean heat (heat is shared, not made), so ingots taken from a heater one at a time stack into one pile; outside the bench, [right-click](Heat.md#merging-heated-stacks) does the same. The pile is judged on its coldest piece.  the fan shows every shape that stock can become, as icons. Shapes the pile is too small for are faint. Hover one for a ghost of the part by the result slot and a few bars (hardness, toughness, weight); click it to make it. A part takes only the stock it needs and the rest comes back as smaller items: a block that makes a one-ingot part gives the part and eight more ingots (ingots, for stone). **Break down** turns a big piece (a block) into ingot-sized pieces without making anything.
- A **part** in the centre: the fan shows what is already fitted to it (click to take it off), then what you are carrying that attaches, then every part that could attach and would be shaped from the stock slot. Click a node to fit it. Fitting the last part makes the tool, which becomes the centre.
- A **tool** in the centre: the fan shows its parts. Click one to see what you carry that fits that place (swap it, add to an empty place, or mend a worn part with the same stuff) or to take it off; what is left is the part it was built from, with the rest fitted to it. A piece of armour is its plate: shaped here it is the finished piece, and its lining and studs are fitted and taken off as parts of it.

Two folder tabs stand above the panel. **Forge** is the bench described above. **Trim** is exactly the vanilla smithing table: **Template**, **Base** and **Addition** slots in a row and the result on the right, so netherite upgrades and armour trims still work. No hand tool is needed there.

The bench top behind the fan is lit by a fire that follows the stock's heat: low embers for cold stock, taller and whiter as it heats.

## Forging

A forged variant is played in the table's screen. **Tap** on the left lane (left click or Z) and **heavy** on the right (right click or X) as notes fall to the ring. A metronome clicks the tempo and counts four beats in. Each blow flashes Clean, Loose or Miss; a harder metal plays slower. The stock cools while you play: notes struck out of its window are dead. A sloppy run leaves **flaws** that take a share of the edge's toughness and durability. Players who learned Cadence (Onslaught) see tempo pips under the beat: clean blows in a row light them, and each lit pip makes the next blows land a little more Work. The screen is dark iron and brass. The pattern is played around the stock on the bench; nothing covers the screen.

A tool is put together here too, by fitting parts to a part (see above). Parts for organic stock are made at the [Woodworking Table](Woodworking_Table.md); everything else at the [Stonecutter](Stonecutter.md).
