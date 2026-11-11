# Blueprint Table

A drafting board for drawing a tool, weapon, shield, bow, piece of armor, [backpack](Backpack.md), [bedroll](Bedroll.md) or gun on paper and **reading its numbers before you make it**. Guns are drawn here too, and so are machines and their parts (engines, gearboxes, propellers, thrusters, flywheels, pressure vessels, boilers, heat exchangers, gas tanks, gas compressors, water wheels, thermal stores, Leyden jars, pumps, fluid burners, electromagnets, galvanic cells, lamps, motors, cells, belts and suit modules). Some types can be drawn only by someone who holds the right [treatise](Treatise.md); see [Schools](#schools).

## Crafting

```
P P
T T
```

P is Paper and T is a Crafting Table.

## Using it

Right-click it to open the board. The item is drawn as a **diagram**: each part is a box placed where it sits in the finished piece. Drag to pan and use the wheel to zoom.

- **Pick a type** (pickaxe, chestplate, bow...) from the row of icons across the top. The wheel scrolls the row, and the search button opens the full list under headings (Tools, Weapons, Wearables, Technology; the demolition charge sits under Technology) with a search box.
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

Blueprints stack to sixteen. The table offers exactly what a [crafting grid](Tools_and_Weapons.md#putting-one-together) can assemble, because it asks the same rules: if a drawing is on the sheet, those parts put together at the [Engineering Table](Engineering_Table.md) make that tool. Heat and forging flaws are not part of a drawing: a blueprint is paper, and flaws are what forging adds. Requirements and blueprints are free; what a finished piece *has* still costs an instrument ([Balance Scale](Instruments.md#balance-scale), [Scratch Files](Instruments.md#scratch-file-set)).

**Print**, at the bottom right, turns a sheet of paper in the paper slot into a **Blueprint** carrying the design and its figures. The pinned drafts print onto the paper too, and come back as pins when you put the Blueprint back in the slot to open it for drawing again. The table keeps nothing; the paper, the source blueprint and the samples come back to you when you close it.

### Copying

The **source slot** takes a blueprint. Put blank paper in the paper slot and press **Copy**: the paper becomes a copy of the source blueprint, drawing, figures and pinned drafts. The original stays in the source slot. Copying needs no treatise.

A [Computer](Electric_Controller.md#apps) can run the same board as the **Blueprint App Disk**, in the desktop's colors. It cannot print onto paper and has no sample slot.

The [Prep Table](Prep_Table.md#blueprint-tab) has a Blueprint tab with a board for dishes only; the Blueprint Table itself does not draw dishes.

## Schools

A type that belongs to a **school** is drawn here only while you hold the right numbered volume of that school's [treatise](Treatise.md); each volume is a subject of its own. Press **Print** without it and the table says which volume the type takes. A volume's types are left out of the type row and list while you do not hold it, and the treatise's tooltip lists what it opens. A treatise counts as held when it lies loose in your inventory, sits inside a [Treatise Binder](Treatise_Binder_and_Shelf.md) in your inventory, or sits in a [Treatise Shelf](Treatise_Binder_and_Shelf.md) touching the table. It is kept, never used up.

| Volume | Types |
| --- | --- |
| Weapons I | maul, spear, cleaver |
| Weapons II | gun, spring gun, crossbow |
| Weapons III | electric gun |
| Mechanical I | engine, flywheel |
| Mechanical II | transmission |
| Aerospace I | propeller |
| Aerospace II | thruster |
| Robotics I | suit module |
| Chemical I | pressure vessel, boiler, large boiler, gas tank |
| Chemical II | heat exchanger, gas compressor |
| Chemical III | demolition charge |
| Materials I | none |

Every other type (tools, armor, backpack, bedroll, water wheel, thermal store, Leyden jar, pump, fluid burner, electromagnet, galvanic cell, lamp, dynamotor, battery, belt and the rest) needs no treatise.

## What a blueprint is for

A blueprint is a **drawn fit**: a specification that the [Engineering Table](Engineering_Table.md) judges an assembly against. It is never required to build anything.

- Put it in the Engineering Table's blueprint slot (a [Blueprint Book](Blueprint_Book.md) works too) with the parts in the grid. A part whose material lies inside the properties the drawing names (strength, hardness, conductivity, insulation, density) fits clean. Another material still builds, and the item is marked **off the drawing**.
- Each axis (a bore, a wall, a length) has a target. Parts built to a blueprint may be off by 5% of the axis range; past that the item takes a **fit flaw** (looser joints, and for a gun a wider spread). Without a blueprint the parts are judged against each other at 20%.
- A blueprint skips the hand-work game and counts as a small bonus to the start. See [Fit and head start](Engineering_Table.md#fit-and-head-start).
- The dimension of a machine (diameter, bore, turns, size, gauge, wall, area, buckets) is set here on the drawing. The [Water Wheel](Water_Wheel.md), [Thermal Store](Thermal_Store.md), [Leyden Jar](Leyden_Jar.md), [Pump](Pump.md), [Fluid Burner](Fluid_Burner.md), [Electromagnet](Electromagnet.md), [Galvanic Cell](Galvanic_Cell.md), [Glowstone Lamp](Lamp.md), [Boiler](Boiler.md) and Large Boiler, [Flywheel](Flywheel.md), [Pressure Vessel](Pressure_Vessel.md), [Heat Exchanger](Heat_Exchanger.md), [Gas Tank](Gas_Tank.md), [Gas Compressor](Gas_Compressor.md), [Thruster](Thruster.md), [engine](Engine.md), [transmission](Transmission.md), [propeller](Propeller.md), [suit module](Exosuit.md#learning-a-module) and [crossbow](Tools_and_Weapons.md#crossbow) are complex types: only the Engineering Table assembles them. Some need a treatise to draw and to assemble. The [Hand Crank](Hand_Crank.md) needs neither.

A blueprint comes from the Print button, a copy, a villager or a chest, or from taking a complex item apart on the Treatise tab of the Engineering Table with blank paper in the blank slot, which writes a blueprint of its design.

Operators can make a blueprint of the item in their hand with `/feedback blueprint`; it works on any modular item that is not itself a blueprint.

## Naming, credit and the checklist

The table has a **title** field: what you type goes on the printed blueprint, with a credit, **Designed by** your name (the table writes it, you cannot sign someone else's). Both show on the blueprint's tooltip. A copy keeps them. A thing made to the drawing carries the title and the credit in its tooltip, whoever makes it; if it departs from the drawing, the credit goes. The item's name is still whatever it is by its parts.

A blueprint is never required and gates nothing; it gives the credit and a **recipe card**. Right-click a blueprint to open the card listing its parts, materials, shapes and axis values. It lists a missing material and never refuses to work without it.

A part with **axes** (a chestplate's width, a sword blade's length) shows steppers on the drawing for the target value of each.

The real-world name of the build you are drawing shows beside the type's name: a gun is named by its action and barrel, and armour, blades, shields, bows and electric guns by their shape and the ranges their axes sit in (a cuirass, a rapier, a tower shield, a railgun). A build with no recognised name shows none here; the item itself still gets a name from its size, shape and material.
