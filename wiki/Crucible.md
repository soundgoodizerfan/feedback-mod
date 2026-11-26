# Crucible

An open pot that sits over a [Firebox](Firebox.md) (or lava), or beside a [Heating Coil](Heating_Coil.md), [Induction Coil](Induction.md#induction-coil) or [Arc Head](Arc_Furnace.md#the-arc-head) or [Star Burner](Star_Burner.md). It's where you make steel, melt metal and mix alloys, and it's the one hot vessel you can **measure and automate**.

## Building it

Shape a **Crucible Shell** from **brick** or from **plate** of a metal at the [Stonecutter](Stonecutter.md) (brick) or [Smithing Table](Smithing_Table.md) (plate), choosing its slots and wall there. Put the shell alone in the [crafting grid](Crafting_Table.md) and the crucible comes out with the shell's slots, wall and wall metal. It needs no blueprint, no treatise and no fasteners. Breaking the block gives the same crucible back, with its wear.

The shell has two measures:

| Measure | Range | What it sets |
| --- | --- | --- |
| **Charge slots** | 2 to 8 | How many items it holds, and how big a vessel it is. Each slot adds **648 mB** to the tank (4.5 ingots). |
| **Wall** | 10 to 60 mm, steps of 5 | How thick the wall is. A thicker wall has more to heat. |

The default is a **two-slot, 30 mm brick** crucible: it holds two items and a **1296 mB** tank, nine ingots' worth.

## Size, wall and heat

How much heat a crucible takes to warm is set by its size, its wall thickness and **what the wall is made of**: a metal wall takes up and gives off heat quickly, brick holds it and warms slowly. More slots make a bigger, heavier vessel that is slower to warm and slower to cool; a thicker wall is slower still. A light crucible closes most of the gap to its fire in a couple of seconds, overshoots easily and is hard to hold steady; a heavy one takes many times longer but holds a temperature almost on its own. All of them settle at the same temperature on the same fire. Some processes (steel especially) have a *max rate*, and a light crucible on a full fire may simply climb too fast to satisfy it. See [Heat](Heat.md#heavy-vs-light-vessels).

A [Refractory Lining](Refractory_Lining.md) makes a light crucible behave more like a heavy one.

### Wall wear

Every wall material holds up to a temperature. Run the crucible **hotter than its wall material holds** and the wall wears; a worn wall leaks more heat, and a spent wall gives way. Brick holds less than a good metal; choose the wall for the temperatures you mean to run.

### Thermal shock

A wall of brick, glass, ceramic or another material with a **shock limit** (see [Thermal shock](Heat.md#thermal-shock)) wears each time molten metal is poured into the crucible. The jump counts between the metal's melting point and the crucible's own temperature: it costs wear for every time the jump overshoots the wall's limit, with the sound of breaking glass, and a wall worn through gives way and the crucible breaks. A jump within the limit costs nothing. A warm crucible takes a smaller jump than a cold one, so warm the pot before you pour a hot melt into it. A metal wall and the [Arc Hearth](Arc_Furnace.md#the-arc-hearth) take no shock.

## Wall heat

A [Heat attunement](Enchanting_Table.md#paying-for-attunements) on the crucible shell makes the wall a small heat source. While the crucible holds anything and an XP run touching it stands at level 15 or higher, the wall draws experience and warms the pot a modest amount above its surroundings for each level: a slow trickle, not a furnace. A fire or coil hotter than that takes over. Running hotter than the wall material holds wears it as usual. Cut the supply and the heating stops.

## How it looks

A built crucible is drawn as a plain square box with straight vertical walls, a flat rim, a square opening and a full-width floor. A bigger tank makes a wider, taller box, and a heavier wall for its size makes thicker walls, so a glance tells a roomy light crucible from a small heavy one. Molten metal shows as a surface in the box, in the fluid's own color and texture, at the height the tank is filled. Each charge slot that holds something shows a lump on the floor. A hot crucible warms toward the glow color of its heat band (see [Heat](Heat.md)). Whatever its size, the outline when you look at it is the same.

## Using it

- **Right-click with an item** to drop one in. Shift shows the same. A bucket or other fluid container is poured into the crucible's tank as far as it has room, and the empty bucket comes back (water here is what a dirt dip turns into clay, and what sets concrete powder into concrete). It holds as many items as it has charge slots.
- **Right-click with an empty hand** to open its screen. Its item slots are in rows of three, beside a heat gauge and a tank gauge. Inputs and finished products share the slots: a product lands in a free slot.
- The **heat gauge** is banded in the same ten bands as the glow of a Thermal Vessel; hover it to read the temperature in Tu.
- Each charge has [its own temperature](Heat.md#warming-in-a-vessel), shown on its tooltip. Slots holding glowing stock shine with a faint bloom in the glow color of that item's heat, and sparks lift off them, more of them the hotter the item. The chamber gives off a soft, flickering halo as it heats. Once the crucible is above 480 Tu, bubbles rise in the melt in the tank. With reduce motion on, the sparks and bubbles do not appear.
- The **tank gauge** is filled in the melt's color, which shines toward the glow color as the crucible heats; hover it to see the fluid and its amount in ingots. Molds fill from the tank, not from the screen.
- Items taken out of a slot come out **at the temperature they have reached** and start cooling at once. Ingots taken a click apart differ by a few Tu and do not stack on their own. **Right-click** one held on the cursor onto the other in any inventory or container to merge them at their [average heat](Heat.md#merging-heated-stacks).

Whatever is inside warms toward the crucible's temperature. The crucible doesn't know what you're making. It just gets hot, and the items do whatever they do at the crucible's temperature (see JEI's *Thermal process* pages).

### Making things

Put in the ingredients and hold the temperature inside the band. For example, an **Iron Ingot** + **Charcoal** becomes a **Steel Ingot**. See [Guide: Making Steel](Guide_Making_Steel.md). Let it get too hot and the batch is **ruined** into a single item: *Burnt Iron* for steel and cast iron, otherwise what the first ingredient leaves (see [Failed batches](Heat.md#failed-batches)).

### Melting

Ores, raw metal and ingots melt into the crucible's **tank** once they're hot enough. Fill an [Ingot Mold or Bullet Mold](Casting.md) by right-clicking the crucible with it, or set the crucible above a [Casting Basin](Casting.md#casting-basin) to pour into molds there. Melt two metals in the right proportions and you get an alloy. See [Casting and Alloys](Casting.md).

Scrap, swarf and a whole made part melt back too. Swarf pours nine tenths of its volume (a part of metal filed down at a bench sheds it, and so do the [Lathe](Lathe.md) and other cutting machines); a whole made part pours nineteen twentieths of the metal it holds, a filed part less.

Melting takes the metal's **melting heat** out of the crucible: the pot cools a little for every piece that melts, more for a metal with a high melting heat. The tank holds 648 mB for each charge slot, 1296 mB on the default build (144 mB an ingot). If it's full, extra metal that melts is lost. Molten metal held past its boiling point boils off with a poisonous fume ([Arc Furnace](Arc_Furnace.md#hazards)). A melt will not pour into a tank holding crude oil or a gas.

## Building around it

The crucible notices what's built against it:

- A **heat source** against the crucible heats it through the face the source points at; see [Heat](Heat.md#heat-sources-have-a-facing). An [Arc Head](Arc_Furnace.md#the-arc-head) set on top lets it melt what no fire can, though the [Arc Hearth](Arc_Furnace.md#the-arc-hearth) holds more. An [Induction Coil](Induction.md#induction-coil) set against it melts metal cleanly, but only while the crucible holds metal.
- **[Insulation](Insulation.md)** blocks against its sides help it hold heat.
- **[Dampers](Damper.md)** against its sides let you dump heat on demand.
- A **[Bimetallic Strip](Bimetallic_Strip.md)** against it makes a simple thermostat.
- A **[Temperature Sensor](Instruments.md#temperature-sensor)** on any face lets a [Controller](Controller.md) read it.
- A **[Powered Bellows](Powered_Bellows.md)** goes against the **firebox**, not the crucible.

## Reading it

Jade shows how hot it is in words, or as a figure with a [Thermometer](Instruments.md#thermometer) in your inventory. It also shows how many sides are insulated and what's inside.

Because it's open, a crucible can always be watched. The sealed furnaces can't; see [Furnaces](Furnaces.md).

## Boiling

Water (or any other liquid that boils) in a crucible that reaches its boiling point turns to vapor and leaves through the open top. Each part that boils takes its boiling heat from the crucible, so the pot cools while it boils.

## Cooking

Food in a crucible goes through raw, done, dried, charred and spoiled like food in a [furnace](Furnaces.md#melting-boiling-and-cooking), and any vanilla or modded smelting, blasting or smoking recipe works in it. A food that comes out of a dip (wet wheat into dough) or finishes cooking (dough into bread) stacks with a held stack of the same food when their states are close (cook, water, live load, toxin, char, rot, smoke and salt each within a hair); the merged stack keeps the lower of each. Dishes, and foods that differ in anything else, stay apart.
