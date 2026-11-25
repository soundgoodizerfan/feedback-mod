# Smithing Table

A vanilla block. Feedback keeps the block and changes what it is for. The [Anvil](Anvil.md) is its upgrade.

## What vanilla does

It upgrades diamond gear to netherite and applies armor trims, using smithing templates.

Feedback's Smithing Table keeps the trim half. Gear is not upgraded to netherite; netherite comes from [melting and soul quenching](Netherite.md).

## Crafting

```
B B
P P
P P
```

B is a Bronze Plate and P is any plank.

## Filing

A made part with dimensions (a gun barrel's bore, a head's size) can be filed down at any part bench: put the part in the middle slot, carry a [file](Hand_Tools.md#file), and click a dimension. Each stroke takes one grain off that dimension (a bastard-cut [file](Hand_Tools.md#file) takes two, a smooth cut half a grain) and sheds the metal that came off as **swarf** of the same metal, into your inventory; the file wears a point every fifth stroke. A part cannot be filed up: a part below its smallest size is melted down instead. Swarf melts back in a vessel at nine tenths of its volume and a whole part at nineteen twentieths.

A part shaped by hand at a part bench remembers what its maker set each measurement to. Its tooltip says so for every axis they set, for example *"Length cut to 15 cm"*, with no instrument. That is the aim, not the result: the cut still comes out within the bench's noise, and [calipers](Instruments.md#calipers) read the true size. A part made by a machine carries no such line until it is measured.

## What Feedback does

The Smithing Table is where **metal parts** are shaped, with a [Hammer](Hammer.md) in the hammer slot. Stock must be metal: a material of kind metal, or any material with a hot-working window. Metal with a hot-working window (iron, wrought iron, steel) is shaped only while the stock is inside that window, so it has to arrive hot from a heater; the bench does not reheat it, and the stock cools while it sits. Metal with no window (copper, bronze) is shaped cold. Any hammer in the slot will do: it is the required tool, not the force behind the shaping. Forged variants (the ones played as a rhythm) are made at the [Anvil](Anvil.md); a [Hearth](Hearth.md) beside the Anvil keeps the stock hot. The block breaks fastest with an axe.

Every bench has the same screen: a bench top with a center slot and a column of slots on the right (the hand tool, a stock slot and the result). Put stock in the center and the bench answers with a **fan** of floating nodes around it; drag to pan, scroll to zoom. The client setting **reduceMotion** (in the mod's client config) stills the fan's drifting nodes, the shake and the idle animations; sounds stay.

- **Stock** in the center: heated pieces of one item put into the stock slot, by click or shift-click, are blended to their count-weighted mean heat (heat is shared, not made), so ingots taken from a heater one at a time stack into one pile; outside the bench, [right-click](Heat.md#merging-heated-stacks) does the same. The pile is judged on its coldest piece. The fan shows every shape that stock can become and that your [treatises](Treatise.md#using-it) allow, as icons: a shape is listed when some type that uses it needs no treatise or its volume is held. Parts of a gun or other treatise-gated type stay out of the fan until you hold the volume, while shapes shared with ungated tools (plates, rods) remain. Shapes the pile is too small for are faint. Hover one for a ghost of the part by the result slot and a few bars (hardness, toughness, weight); click it to make it. A part takes only the stock it needs and the rest comes back as smaller items: a block that makes a one-ingot part gives the part and eight more ingots (ingots, for stone). **Break down** turns a big piece (a block) into ingot-sized pieces without making anything.

A refused click says why in red: the stock is too cold or too hot to work, there is not enough stock for the shape, the shape does not take this stock, or the station cannot forge.

Two folder tabs stand above the panel. **Forge** is the bench described above. **Trim** is exactly the vanilla smithing table: **Template**, **Base** and **Addition** slots in a row and the result on the right, so armor trims work. No hand tool is needed there.

**Filter:** with stock in the center, type any letter, digit, space or minus and a small **Filter** box opens in the fan's top-right corner, no click needed. The fan (shapes and cuts) narrows as you type; every word you type must match the shape's name, part, type or variant. Backspace deletes, Esc clears. Typing does nothing while a minigame plays.

The bench top behind the fan is lit by a fire that follows the stock's heat: low embers for cold stock, taller and whiter as it heats; the same firelight runs along the panel's bottom edge.

Inside the work area (not over slots or buttons) the pointer is the bench's hand tool: the hammer, axe or saw, tipping forward while you hold the button. Every forging blow shakes the whole panel, heavier blows more, and a clean strike more than a dead one.

Every part of every modular type is made at the bench for its stock: metal here, [wood, bone and leather](Woodworking_Table.md) at the Woodworking Table, and cold stock (stone, ceramic, glass, crystal) at the [Stonecutter](Stonecutter.md). That includes the parts of a [gun](Gun_Parts.md), once you hold its treatise volume. Parts are put together at the [Crafting Table](Crafting_Table.md) or the [Engineering Table](Engineering_Table.md).
