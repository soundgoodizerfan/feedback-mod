# Blueprint Table

A drafting board for drawing a tool, weapon, shield, bow, piece of armor, [backpack](Backpack.md), [bed](Bed.md) or gun on paper and **reading its numbers before you make it**. Guns are drawn here too, and so are machines and their parts (engines, gearboxes, propellers, thrusters, jets, flywheels, pressure vessels, boilers, heat exchangers, gas tanks, gas compressors, thermal stores, Leyden jars, pumps, electromagnets, galvanic cells, gas centrifuges, column trays, lamps, motors, cells, belts and suit modules). Some types can be drawn only by someone who holds the right [treatise](Treatise.md); see [Schools](#schools).

## Crafting

```
P P
T T
```

P is Paper and T is a Crafting Table.

## Using it

Right-click it to open the board. The item is drawn as a **diagram**: each part is a box placed where it sits in the finished piece. Drag to pan and use the wheel to zoom.

- **Pick a type** (pickaxe, chestplate, bow...) from the row of icons across the top. The wheel scrolls the row, and the search button opens the full list under headings (Tools, Weapons, Wearables, Technology; the demolition charge sits under Technology) with a search box.
- **Click a part** to select it; click bare canvas to let go of it. Until a part is selected the part panel shows only a hint and the figure bars are hidden. The selected part's panel has two pages, picked by the **Shape** and **Material** tabs in its title row; the chosen material's name sits beside the part's name.
  - The **Shape** page opens with a **Moves** line naming the figures that differ between the part's variants. Below it come tabs for the head kinds (on hafted heads) and a scrollable list with one row per variant (a rapier blade, a hatchet head), each showing a turning render of the part in the chosen material and the variant's name. The variant drawn is open: its description is indented under the name (ending in an ellipsis when it is too long for the list) and, under that, come the part's steppers (count, the edge, and axes such as edge length or head size). Hover any row to read its full description in a tooltip and to see the figures it would give on the bars; click a row to draw that variant. Right-click in the list to shut the open row so every row shows only its name; right-click again, or click a variant, to open one again.
  - The **Material** page holds a **search box** (it matches a material's name, its kind and its traits) and a scrolling grid of **material swatches** for the materials that fit the part, filling the panel. Each swatch is a cell showing the part's own item in that material, turning under the pointer. Hover one for a card with the material's name, its kind and up to six figures the whole design would gain (green up arrow) or lose (red down arrow) with it, the biggest change first. A material the part cannot take says so in red on its card. Hovering a part's role in the diagram prints **Takes:** (the shapes) and **Needs:** (the properties) for its slot, and holding Shift shows the thresholds; see [What a Part Slot Takes](What_a_Part_Slot_Takes.md). A part made of several materials shows a slot picker first. With only one material that fits, the swatches and the search box are hidden.

  A material the part cannot be made from (a [Battery](Battery.md)'s plates need a metal whose plates take a charge, such as lead) is not listed.
- **Required parts.** A type cannot be printed until every part it cannot be made without is drawn (a gun needs a receiver, a barrel, a trigger and a grip; a hafted tool needs a head, a handle and a binding, except a hammer head, which a machine takes on its own). The list beside the figures names what is missing.
- **Paint a part** by carrying a material item on the cursor and clicking the part. A **sample** item in the sample slot sets the selected part's material once.
- **Dashed + slots** are optional parts the type could take (a tip, a guard...). Click one to add the part, and use the **x** on a part's box in the diagram to take it off again. Which parts a type can take, and how long its handle is, follow from what you have drawn.
- **Swap buttons.** When another part could stand in for the selected one and the design stays buildable, the part's title row shows a **To <part>** button for each, left of the Shape and Material tabs (a gun's slide shows **To bolt**, a bolt shows **To slide**). Click it to take the part off and fit the other in its place; the chosen material carries over if the new part accepts it. Parts with no material page, and parts every build of the type has, show none.
- A part that has a measure you can choose (an engine's bore and stroke, a gearbox's ratios) shows a **< >** stepper for it on the Shape page. A part with more than three such measures shows three at a time and scrolls the rest with the wheel. A gun barrel's bore steps 1 mm at a time from 5 to 20 mm.
- On a gun, **attachments hang on the part they fit**. A part that can still take one wears a **+**; click it and the available attachments fan out round it while everything else dims. Attachments you have fitted stay drawn beside their part. An attachment that cannot go on with what is already fitted is grayed out. Attachments can hang on other attachments: a reticle on a sight. Click empty canvas to put the fan away. A tip or pommel sits where the finished piece carries it.
- **Rig** toggles a looping demonstration of the drawing in use, for pick-like tools, a sword, a bow, a shield and a battery.

### Links

Parts that touch are joined by a link, colored by how tight that joint is: thick and cool with margin, amber near the limit, and **red and dashed** past it. Hover a link to read the percent of its limit it carries. A drawing that cannot be made shows the failing link in red with the reason beside it. A gun shows **every** failing link at once, and names the problems the same way: an action and feed that do not work together, a material that cannot be built into a gun, two devices on one mounting.

A gun's name (pistol, rifle, shotgun...), read from its parts, is shown beside the type name.

### Figures

The sheet shows the design's figures as **one list of bars**, and only the figures that matter for the type drawn (a pickaxe does not list armor figures). The list scrolls, so no figure is cut off. The selected part's own figures come first. Machine numbers are bars like the rest, each with a short label (an engine's cylinders, displacement, torque and redline, a gearbox's gears, a propeller's disc, a dynamotor's rpm, a tank's room, a belt's grip). Fitted attachments and chosen variants add their own figures too: a stock or bipod adds brace figures, an armor servo adds drive and loss. Bars show, for a battery: cells, the band it is rated and its capacity in Eu; for a backpack: slots, carry rating, mounting points left and store slots; for a [bed](Bed.md): comfort and warmth; for a belt: its grip on cargo and the hottest cargo it carries. Figures that are words, ranks or yes/no (a pulse gun's voltage band rating, a divergent nozzle) stay text rows at the end of the list; they sort by rank like the bars. Qualities and adjectives the design earns sit along the bottom edge.

Machine parts give more figures:

- **Boiler:** steam made a tick and water held.
- **Gas tank:** the safe fill in mB, short of the danger line, and how much it vents a tick near that line. A **pressure vessel** gives its safe air and valve relief the same way.
- **Heating coil:** its heating power and how many Tu a second it warms unloaded. A **thermal store** gives the time it takes to lose half its heat.
- **Leyden jar:** how long it keeps half its charge, and how long it takes to fill.
- **Gas centrifuge:** how many times its starting speed it can spin before it tears apart.
- **Jet:** the hottest flame it runs, tip speed at full power and fuel burned a tick. A **thruster** gives its push per mB burned.
- **Galvanic cell:** the band it supplies, the amps, and how many minutes the anode lasts at full draw.
- **Engine:** the ignition window of the fuels it runs, fuel burned a tick, and how long the spark plug lasts and how fast it fouls.
- **Transmission:** its list of ratios.
- **Propeller:** the RPM it tears apart at, its pitch (fine, medium or coarse) and whether it has pitch control.
- **Conveyor belt:** the least cargo friction at which it still climbs, dry and wet.
- **Dynamotor:** the share of its power lost as heat at rated load and how much heat it sheds.
- **Battery:** discharge amps, the share of a charge it keeps and the capacity it holds when freezing.
- **Machine heads:** how much the head wears per 1000 St of overload (per Wu ground, for a wheel); see [Machine Heads](Machine_Heads.md#wear).
- **Lined or surfaced blocks:** how long a corrosive liquid takes to eat through; the lining's corrosion resistance sets it.

- The figures follow the part: a sword lists no dig speed, and a file, drill, knife or screwdriver head lists no damage, impact or bite. Each hafted head lists its own.
- **Part sizes** (length, width, head size, limb length, face size, bore and the like) show as plain lines. They are set by the steppers, not by the material, so they cannot be sorted by.
- Further lines name what a type does with its numbers:
  - A hand hammer shows the share of a hand-forging blow it carries.
  - A digging tool shows what it harvests: soft blocks only, or stone-, iron- or diamond-class blocks.
  - A spear or throwing axe shows its thrown speed in blocks a tick.
  - A gun shows the rounds it holds itself, the heat each shot adds to the barrel and the heat the barrel sheds a second (both in Tu), and how quickly its sight comes up (in ms).
  - A demolition charge shows its fragment damage and its fuse (or "on impact").
  - A sword, spear, maul or axe build shows its **parry window**, **recovery**, **cone** and **rating** (see [Combat](Combat.md#parry)).
  - A hafted head shows **Cuts clean to**: the hardest block its edge cuts without chipping. A brittle edge cuts clean only to softer blocks.
  - A hafted file, screwdriver bit or knife lists its bench job in words: a file cuts coarse, medium or fine and leaves marks or a polish; a bit's tip suits screws, bolts or rivets, and it ratchets or not; a knife's blade slides or grains.
  - A bow or crossbow shows its draw time, speed, damage, spread, pierce and the slow of drawing it, with the limb or prod length, a recurve, a rest, a stabilizer and a magazine already counted in.
  - A shield shows how much it slows you while raised, the heat it holds full strength to (or "never softens"), its **wear rate** and the **chip damage**: the blocked damage a face takes without chipping.
  - A gun shows its **bore life**: how many shots it takes to wear the bore to its floor.
  - Armor shows the share of slash turned, and of blunt on mail or of shock on a rigid breastplate.
  - A spoon shows its stirring and skimming against the standard spoon, and the heat it can be held over a pot at.
  - A launcher shows the speed a shell leaves at.

Every row behaves the same way:

- A mark on each row shows where the **first drawing** stood. An arrow shows which way the row has moved since.
- **Hold Shift** to read the exact figure with its unit. **Hover a row** to read its full line. Hover the **mass** bar for what it counts: the tooltip is written for the kind of thing drawn (a gun, a belt, a backpack, a bed, a shield, a battery cell, a charge, a vessel, a machine part).
- **Hover a swatch, a variant row, or a part while carrying a material** to preview how the bars would change; hovering a swatch also draws a ghost mark on each row where that material would put it.
- **Hover a row** to light the parts that moved it.
- **Click a row** to sort the material swatches by that figure, using the figure the whole design would have with each material. The first click puts the highest first, a second click the lowest first, a third click turns the sort off. The sorted figure's label turns the accent color. Rows the selected part does not move are dimmed and do not sort.
- **Pin** keeps a draft: each pinned draft puts its own colored mark on every row to compare against, up to three. Click a pinned draft to open it, Shift-click to drop it.

A blueprint's tooltip lists the same figures. Blueprints stack to sixteen. The table offers exactly what a [crafting grid](Tools_and_Weapons.md#putting-one-together) can assemble, because it asks the same rules: if a drawing is on the sheet, those parts put together at the [Engineering Table](Engineering_Table.md) make that tool. Heat and forging flaws are not part of a drawing: a blueprint is paper, and flaws are what forging adds. Requirements and blueprints are free; what a finished piece *has* still costs an instrument ([Balance Scale](Instruments.md#balance-scale), [Scratch Files](Instruments.md#scratch-file-set)).

**Print**, at the bottom right, turns a sheet of paper in the paper slot into a **Blueprint** carrying the design and its figures. The pinned drafts print onto the paper too, and come back as pins when you put the Blueprint back in the slot to open it for drawing again. The table keeps nothing; the paper, the source blueprint, the samples and the test round come back to you when you close it.

### Round test

The **test round slot** on the bottom row, between the sample slot and the copy slot, takes a cartridge. With a gun drawn on the board, a **Round test** plaque shows the drawing worked out against that round on paper: the parts exactly to drawing, in ideal conditions. Nothing is fired and nothing is measured, so the figures are exact and need no instrument. The real gun's figures still come off the item with an [instrument](Instruments.md).

The plaque lists:

- **Chamber:** the charge against what the walls hold, in red when the charge asks more than the chamber holds, with the share it asks.
- **Muzzle:** speed in m/s and the Work the shot carries.
- **Cycle:** time between shots and spread.
- **Mass:** the gun and the projectile.
- **Kick** in degrees.
- **Heat:** what each shot adds to the barrel and how much the barrel sheds a second, in Tu.
- **Damage:** the most one hit can be worth, the muzzle Work at 10 Work per damage with every bit landing. The round's shape and the target take their share of it.

A round the drawn gun cannot fire says why in red instead of the figures: a shot charge does not fire from a rifled bore, a bore that is not the round's does not chamber it, or the round cannot be fired from this gun. The round is never spent.

### Copying

The **source slot** takes a blueprint. Put blank paper in the paper slot and press **Copy**: the paper becomes a copy of the source blueprint, drawing, figures and pinned drafts. The original stays in the source slot. Copying needs no treatise.

A [Computer](Electric_Controller.md#apps) can run the same board as the **Blueprint App Disk**, in the desktop's colors. It cannot print onto paper and has no sample slot.

The [Prep Table](Prep_Table.md#recipe-tab) has a Recipe tab that reads a dish the same way; the Blueprint Table itself does not draw dishes.

### Suit modules

A module drawing lists:

- **Wiring feeds N of 4**: how many attachments the wiring can carry current to. Heavier wiring (more turns, a better conductor) feeds more.
- **One line per attachment**, with its strength in percent and its headline: lens reach (blocks), spring fall cut, actuator speed, tread ground grip, filter breath, heat plate fire cut, thruster push, plate blast cut, magnet pull radius, dispenser dose (mB). Hover it for the other end of that attachment's trade-off (a short lens against sight with a long one, a stiff spring's jump, a wide tread's water speed, the plate's shot cut and knockback resistance) and, where it draws charge, its cost in Eu.
- **Draws up to N Eu/t, N Mu**: the charge the attachments draw while they work, added up, and the module's whole mass.
- **Wheel**: the abilities the held circuit, motor and cell add to the suit's wheel (see [Abilities](Exosuit.md#abilities)).

A drawing with a motor, controller or cell predicts power as if a middling one were fitted.

### Robots, respirators, backpacks and beds

- **Robot**: speed in blocks a second, the hardest block its tool head breaks, rotor lift left (in Mu, rotor robots) or the height of ledge it steps over (wheels and legs), and, with an inertial sensor, the share of its position drift it keeps.
- **Respirator**: how long the filter lasts and how long the cylinder's air lasts, each as a time. A cylinder mask shows no thinnest-air figure.
- **Backpack**: free carry in Mu (the carry rating less the pack's own mass) and, with a fire patch fitted, the chance fire reaches the shell.
- **Bed**: the share of your health a night heals, and the share in snow country, and the nights it lasts. The heal is half the comfort.

## Schools

Some **parts** and **shapes** belong to a school too (the servos of a powered suit need Robotics Treatise I): they are offered only while you hold the volume. A type that belongs to a **school** is drawn here only while you hold the right numbered volume of that school's [treatise](Treatise.md); each volume is a subject of its own. Press **Print** without it and the table says which volume the type takes. A volume's types are left out of the type row and list while you do not hold it, and the treatise's tooltip lists what it opens. A treatise counts as held when it lies loose in your inventory, sits inside a [Treatise Binder](Treatise.md#treatise-binder-and-shelf) in your inventory, or sits in a [Treatise Shelf](Treatise.md#treatise-binder-and-shelf) touching the table. It is kept, never used up.

| Volume | Types |
| --- | --- |
| Weapons I | maul, spear, cleaver |
| Weapons II | gun, launcher, crossbow |
| Weapons III | electric gun |
| Mechanical I | engine, flywheel, dynamotor |
| Mechanical II | transmission |
| Aerospace I | propeller |
| Aerospace II | thruster |
| Aerospace III | jet |
| Aerospace IV | none |
| Robotics I | suit module |
| Robotics II | robot |
| Chemical I | pressure vessel, boiler, gas tank |
| Chemical II | heat exchanger, gas compressor |
| Chemical III | demolition charge |
| Electrical I | battery, galvanic cell, Leyden jar |
| Electrical II | heating coil, linear motor, electromagnet |
| Electrical III | vacuum tube, resonator, lattice |
| Materials I | none |
| Materials II | none |
| Nuclear I | gas centrifuge |

A pulse gun's drawing also lists the ways the build hurts its wielder, as red chips beside the usual ones: **Burns the hand** (no heat governor), **Shocks the wielder** (a cell above the receiver's band), **Arcs when wet**, and **Locks hot** (a core breech). See [Electric Gun](Electric_Gun.md#misuse-and-hazards).

Every other type (tools, armor, backpack, bed, thermal store, pump, lamp, belt and the rest) needs no treatise.

## What a blueprint is for

A blueprint is a **drawn fit**: a specification that the [Engineering Table](Engineering_Table.md) judges an assembly against. It is never required to build anything.

- Put it in the Engineering Table's blueprint slot (a [Blueprint Book](#blueprint-book) works too) with the parts in the grid. A part whose material lies inside the properties the drawing names (strength, hardness, conductivity, insulation, density) fits clean. Another material still builds, and the item is marked **off the drawing**.
- Each axis (a bore, a wall, a length) has a target. Parts built to a blueprint may be off by 5% of the axis range; past that the item takes a **fit flaw** (looser joints, and for a gun a wider spread). Without a blueprint the parts are judged against each other at 20%.
- A blueprint skips the hand-work game and counts as a small bonus to the start. See [Fit and head start](Engineering_Table.md#fit-and-head-start).
- The dimension of a machine (bore, turns, size, gauge, wall, area) is set here on the drawing. The [Thermal Store](Thermal_Store.md), [Leyden Jar](Leyden_Jar.md), [Pump](Pump.md), [Electromagnet](Electromagnet.md), [Galvanic Cell](Galvanic_Cell.md), [Gas Centrifuge](Gas_Centrifuge.md), [Column Tray](Fractionating_Column.md#building-a-tray), [Glowstone Lamp](Lamp.md), [Boiler](Boiler.md), [Flywheel](Flywheel.md), [Pressure Vessel](Pressure_Vessel.md), [Heat Exchanger](Heat_Exchanger.md), [Gas Tank](Gas_Tank.md), [Gas Compressor](Gas_Compressor.md), [Thruster](Thruster.md), [engine](Engine.md), [transmission](Transmission.md), [propeller](Propeller.md), [jet](Jet.md), [suit module](Exosuit.md#learning-a-module) and [crossbow](Tools_and_Weapons.md#crossbow) are complex types: only the Engineering Table assembles them. Some need a treatise to draw and to assemble. The [Hand Crank](Hand_Crank.md) needs neither.

A blueprint comes from the Print button, a copy, a villager or a chest, or from taking a complex item apart on the Treatise tab of the Engineering Table with blank paper in the blank slot, which writes a blueprint of its design.

### Fine blueprints

A drawing of a machine that a [finespace](Finespace.md#machines-in-finespace) cell can hold (a boiler, lamp, electromagnet, pump, flywheel, heat exchanger, thermal store and the like) has a **Finespace** toggle at the top right of the canvas. Turned on, the parts list and the part icons show each part's count divided by 4, rounded up, at least 1, and the printed sheet is marked **Fine**. Its tooltip reads "Fine: a quarter of the parts, for finespace". The [Engineering Table](Engineering_Table.md#fine-builds) builds a Fine machine from those counts.

Operators can make a blueprint of the item in their hand with `/feedback blueprint`; it works on any modular item that is not itself a blueprint.

## Naming, credit and the checklist

The table has a **title** field: what you type goes on the printed blueprint, with a credit, **Designed by** your name (the table writes it, you cannot sign someone else's). Both show on the blueprint's tooltip. A copy keeps them. A thing made to the drawing is named by the title, whoever makes it, and carries the credit in its tooltip; if it departs from the drawing, the credit goes. Keys typed in the field stay in the field: the inventory key does not close the table while you type.

A blueprint is never required and gates nothing; it gives the credit and a **recipe card**. Right-click a blueprint to open the card (it grows with the window, lists the parts in columns and scrolls when there are many) listing its parts, materials, shapes and axis values. It lists a missing material and never refuses to work without it.

A part with **axes** (a chestplate's width, a sword blade's length) shows steppers on the drawing for the target value of each. A part that is made of several materials (a blade with a harder edge, a scope's lens) also shows a **material stepper** for each slot you choose; the drawing pins those materials. See [Composed Parts](Composed_Parts.md).

An [electric gun](Electric_Gun.md) drawing also has a **beam color**: a swatch and a hex entry (`#RRGGBB`) at the top right of the canvas. The color is saved on the printed blueprint and carried onto a gun built to it. A drawing that cannot be built, such as a capacitor bank on a handgun frame, shows the reason beside the parts at fault.

The pulse gun's fittings add bars for the figures they take from their metals, as percentages: how much of a charge the spring holds, what the pump winding gives, the zoom field, how finely the gauge reads, how closely the governor follows heat, how much the lens lining keeps, the heat sink's capacity against copper, the shock lead's carry against copper, and the barrel's loss a shot. A pulse gun also lists the voltage band its receiver insulates to as a text row.

A pulse gun's drawing also shows how it fires, in three rows (hover a row for the full line):

- **Charges a shot in N t (S s):** the wait for a shot, in ticks and seconds. A cylinder is the time one chamber takes to refill from the receiver's largest battery; a bank is the time it takes to recharge; the other seats are ready again after their own cycle.
- **N shots from a full battery:** how many pulses a full stock battery of the most cells the receiver takes gives, counting what a tether lead loses in the line.
- **Bounces off stone: N:** with a coherence lens fitted, how many bounces the pulse makes off stone. Each bounce keeps only the share the surface reflects, and a pulse that has fallen very low stops, so the lining's own bounce count is cut short; stone keeps little, so a pulse survives at most one stone bounce.

The real-world name of the build you are drawing shows beside the type's name: a gun is named by its action and barrel, and armor, blades, shields, bows and electric guns by their shape and the ranges their axes sit in (a cuirass, a rapier, a tower shield, a Railcannon). A build with no recognised name shows none here; the item itself still gets a name from its size, shape and material.

## Blueprint Book

Carries up to sixteen blueprints, the way a bundle carries items. The [Engineering Table](Engineering_Table.md) reads every sheet inside the book from its blueprint slot.

Craft it from Leather + 3 Paper + String (shapeless).

Click a blueprint onto the book to put it in. Click the book on an empty slot to take a sheet out. Only blueprints go in. The sheets in a book count exactly as if each lay in the blueprint slot on its own; see [Learning a module](Exosuit.md#learning-a-module).
