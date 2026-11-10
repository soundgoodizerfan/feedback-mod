# Blueprint Table

A drafting board for drawing a tool, weapon, shield, bow, piece of armor, [backpack](Backpack.md), [bedroll](Bedroll.md) or gun on paper and **reading its numbers before you make it**. Guns are drawn here too, for the [Gun Table](Gun_Table.md), and so are machines and their parts (engines, gearboxes, propellers, motors, cells, belts and suit modules). Some types can be drawn only by someone who holds the right [treatise](Treatise.md); see [Schools](#schools).

## Crafting

```
P P
T T
```

P is Paper and T is a Crafting Table.

## Using it

Right-click it to open the board. The item is drawn as a **diagram**: each part is a box placed where it sits in the finished piece. Drag to pan and use the wheel to zoom.

- **Pick a type** (pickaxe, chestplate, bow...) from the row of icons across the top. The wheel scrolls the row, and the search button opens the full list under headings (Tools, Weapons, Wearables, Technology) with a search box.
- **Click a part** to select it. Its panel has a **search box** (it matches a material's name, its kind and its traits), **material swatches** for every material, and **shape thumbnails**: the standard shape or a variant (a rapier blade, a hatchet head). Hover a shape to read what it does. A material the part cannot be made from (a [Battery](Battery.md)'s plates need a metal whose plates take a charge, such as lead) is dimmed with an x and cannot be picked; its tooltip reads *Does not fit this part.*
- **Paint a part** by carrying a material item on the cursor and clicking the part. A **sample** item in the sample slot sets the selected part's material once.
- **Dashed + slots** are optional parts the type could take (a tip, a binding, a guard...). Click one to add the part, and use the corner mark on a part to take it off again. Which parts a type can take, and how long its handle is, follow from what you have drawn.
- Set the **edge**, on a tool that has one.
- A part that has a measure you can choose (an engine's bore and stroke, a gearbox's ratios) shows a **< >** stepper for it under the part's name. A part with more than three such measures shows three at a time and scrolls the rest with the wheel. A gun barrel's bore steps 1 mm at a time from 5 to 20 mm.
- On a gun, **attachments hang on the part they fit**. A part that can still take one wears a **+**; click it and the available attachments fan out round it while everything else dims. Attachments you have fitted stay drawn beside their part. An attachment that cannot go on with what is already fitted is grayed out. Attachments can hang on other attachments: a reticle on a sight. Click empty canvas to put the fan away. A tip or pommel sits where the finished piece carries it.
- **Rig** toggles a looping demonstration of the drawing in use, for pick-like tools, a sword, a bow, a shield and a battery.

### Links

Parts that touch are joined by a link, colored by how tight that joint is: thick and cool with margin, amber near the limit, and **red and dashed** past it. Hover a link to read the percent of its limit it carries. A drawing that cannot be made shows the failing link in red with the reason beside it. A gun shows **every** failing link at once, and names the problems the same way: an action and feed that do not work together, a material that cannot be built into a gun, two devices on one mounting.

A gun's name (pistol, rifle, shotgun...), read from its parts, is shown beside the type name.

### Figures

The sheet shows the design's figures as **bars** (for a battery: cells, full voltage, capacity and the voltage band its case holds; for a backpack: slots, carry rating, mounting points left and store slots; for a [bedroll](Bedroll.md): comfort; for a belt: its grip on cargo and the hottest cargo it carries). Qualities and adjectives the design earns sit along the bottom edge.

- A mark on each bar shows where the **first drawing** stood. An arrow shows which way the bar has moved since.
- **Hold Shift** to read the exact figure. Hover the **mass** bar for what it counts: the tooltip is written for the kind of thing drawn (a gun, a belt, a backpack, a bedroll, a shield, a battery cell, a charge, a vessel, a machine part).
- **Hover a swatch, a shape, or a part while carrying a material** to preview how the bars would change.
- **Hover a bar** to light the parts that moved it.
- **Pin** keeps a draft: each pinned draft puts its own colored mark on every bar to compare against, up to three. Click a pinned draft to open it, Shift-click to drop it.

Blueprints stack to sixteen. The table offers exactly what a [crafting grid](Tools_and_Weapons.md#putting-one-together) can assemble, because it asks the same rules: if a drawing is on the sheet, those parts fitted together at a bench make that tool. Heat and forging flaws are not part of a drawing: a blueprint is paper, and flaws are what forging adds. Requirements and blueprints are free; what a finished piece *has* still costs an instrument ([Balance Scale](Instruments.md#balance-scale), [Scratch Files](Instruments.md#scratch-file-set)).

**Print**, at the bottom right, turns a sheet of paper in the paper slot into a **Blueprint** carrying the design and its figures. The pinned drafts print onto the paper too, and come back as pins when you put the Blueprint back in the slot to open it for drawing again. The table keeps nothing; the paper, the source blueprint and the samples come back to you when you close it.

### Copying

The **source slot** takes a blueprint. Put blank paper in the paper slot and press **Copy**: the paper becomes a copy of the source blueprint, drawing, figures and pinned drafts. The original stays in the source slot. Copying needs no treatise.

A [Computer](Electric_Controller.md#apps) can run the same board as the **Blueprint App Disk**, in the desktop's colors. It cannot print onto paper and has no sample slot.

The [Prep Table](Prep_Table.md#blueprint-tab) has a Blueprint tab with a board for dishes only; the Blueprint Table itself does not draw dishes.

## Schools

A type that belongs to a **school** is drawn here only while you hold that school's [treatise](Treatise.md). Press **Print** without it and the table says which treatise the type takes. A school's types are left out of the type row and list while you do not hold its treatise, and the treatise's tooltip lists what it opens. A treatise counts as held when it lies loose in your inventory, sits inside a [Treatise Binder](Treatise_Binder_and_Shelf.md) in your inventory, or sits in a [Treatise Shelf](Treatise_Binder_and_Shelf.md) touching the table. It is kept, never used up.

| School | Types |
| --- | --- |
| Weapons | gun, crossbow |
| Mechanical | engine, transmission |
| Aerospace | propeller |
| Robotics | suit module |

Every other type (tools, armor, backpack, bedroll, dynamotor, battery, belt and the rest) needs no treatise.

## What a blueprint is for

Mostly it's a drawing to look at. Some things **need** one to be built.

- A [suit module](Exosuit.md#learning-a-module), an [engine](Engine.md), a [transmission](Transmission.md) and a [propeller](Propeller.md) are built only with their blueprint in the bench's blueprint slot. A blueprint can sit in a [Blueprint Book](Blueprint_Book.md).
- The [crossbow](Tools_and_Weapons.md#crossbow-blueprint), the only tool with a mechanism whose parts must fit each other: put a Crossbow Blueprint in the crafting grid with the parts. The blueprint must show the same parts as the ones you put in, role for role: same material, same part and same count. Ground-in qualities (measured figures, flaws) need not match. A blueprint of a different design does not work.

Repairs and swaps never need one.

A blueprint comes from the Print button, a copy, a villager or a chest, or from taking an item apart at a bench with blank paper in the blueprint slot, which writes a blueprint of its design.

Operators can make a blueprint of the item in their hand with `/feedback blueprint`; it works on any modular item that is not itself a blueprint.
