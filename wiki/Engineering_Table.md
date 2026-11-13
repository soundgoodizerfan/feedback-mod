# Engineering Table

The bench where parts are put together. It builds every [modular](Tools_and_Weapons.md) item from its parts, crafts machines with hand tools and [fasteners](Fasteners.md), fits extras onto a finished item, repairs and takes things apart, and writes [treatises](Treatise.md).

## Crafting

```
C C C
S T S
S S S
```

C is Gray Concrete, T is a Crafting Table and S is a Steel Plate. Mine it with a pickaxe: without one it drops nothing.

## Using it

The screen has four tabs: **Crafting**, **Fit**, **Repair** and **Treatise**. A **popout** beside the table holds four **hand tool** slots and three **fixings** slots, and stays on every tab. A hand tool is a [Hammer](Hammer.md), a [Sawblade](Sawblade.md), a [File](Hand_Tools.md#file) or a [Screwdriver](Hand_Tools.md#screwdriver). Each act that calls for a tool wears it a point; a broken tool stays in its slot and blocks the act until it is mended.

The popout's fixings slots take screws, machine bolts and rivets (see [Fasteners](Fasteners.md)).

The Crafting tab holds:

- a **3 by 3 grid** and an output slot,
- a **blueprint** slot (a blueprint or a [Blueprint Book](Blueprint_Book.md)),
- a **binder** slot for a [Treatise Binder](Treatise_Binder_and_Shelf.md).

A line under the grid says whether the craft is ready, or why not: a treatise is missing (it names the volume), no tool for it, a tool is broken, or no fasteners. Empty tool, fastener, blueprint and binder slots show a tooltip saying what goes in.

## What the Crafting tab makes

In this order, the first that matches:

1. **Vanilla crafting recipes.** Anything the plain [Crafting Table](Crafting_Table.md) makes.
2. **Engineering recipes.** Machines and parts that ask for a tool, fasteners and sometimes a treatise (below).
3. **A modular item from its parts.** Lay the parts in the grid in any positions, one item to a slot, and the output shows the item. Every [tool](Tools_and_Weapons.md#putting-one-together), weapon, piece of [armor](Armor.md), [gun](Guns.md) and machine built from parts is assembled here. Optional extras (a tip, a pommel, a spring, an addon) are fitted afterward on the [Fit tab](#fit-tab).

The plain Crafting Table assembles the **simple** types the same way. The Engineering Table also assembles the **complex** ones, the types drawn with a [blueprint](Blueprint_Table.md): guns, the crossbow, engines, boilers, flywheels and the rest, and every type that needs a [treatise](Treatise.md) (spear, maul, cleaver, demolition charge): the volume must be in your inventory, the binder slot or a shelf touching the table. A complex type asks for fasteners.

If the type belongs to a school, the matching [treatise](Treatise.md) volume has to be held, as it does to draw the type: loose in your inventory, in a binder in your inventory or in the binder slot, or in a [Treatise Shelf](Treatise_Binder_and_Shelf.md) touching the table.

### Engineering recipes

An engineering recipe is a shaped or shapeless grid like a crafting recipe, plus what the hands need: a **hand tool** (it wears a point), a number of **fasteners** and sometimes a treatise volume. The fastener slots take any [fastener](Fasteners.md); the count is what the craft spends.

With a **hammer**: [Annealing Furnace](Furnaces.md), [Arc Hearth](Arc_Furnace.md#the-arc-hearth), Derrick Base, [Improved Furnace](Furnaces.md), [Lathe](Lathe.md), [Lift](Vehicles.md#the-lift), [Mechanical Hammer](Mechanical_Hammer.md), [Mechanical Press](Mechanical_Press.md), [Scratch File Set](Instruments.md#scratch-file-set) and the [Magazines](Magazine.md) (these also need Weapons Treatise II).

With a **screwdriver**: [Breaker](Breaker.md), [Charging Gantry](Charging_Gantry.md), [Control Rod Assembly](Control_Rod.md), [Cryocooler](Cryocooler.md), [Differential](Differential.md), [Drill Press](Drill_Press.md), [Electrolyser](Electrolyser.md), [Electrolytic Cell](Electrolytic_Cell.md), [Electrostatic Generator](Electrostatic_Generator.md), [Feedbacker](Feedbacker.md), [Gas Centrifuge](Gas_Centrifuge.md), [Gearbox](Cogs.md#gearbox), [Grapple Winch](Grapple.md), [Mixer](Mixer.md), [Pivot Mast](Pivot_Mast.md), [Reflow Oven](Reflow_Oven.md), [Refrigeration Unit](Refrigeration_Unit.md), [Reloading Press](Reloading_Press.md), [Rolling Mill](Rolling_Mill.md), [Sorting Station](Tube_Station.md#sorting-station), [Steam Engine](Steam_Engine.md), [Steam Turbine](Steam_Turbine.md), [Stoker](Stoker.md), [Vacuum Dryer](Potion_Salts.md#vacuum-dryer), [Vacuum Pump](Pneumatic_Tube.md#vacuum-pump) and [Wire Drawer](Wire_Drawer.md).

With a **file** (no fasteners): [Calipers](Instruments.md#calipers), [Lathe Tool Bit](Lathe.md), [Saw Head](Power_Cutter.md) and the [Bullet Mold](Ammunition.md#bullets) (which also needs Weapons Treatise II).

Each page lists its own grid. The [Auto Engineer](Auto_Engineer.md) is crafted from an Engineering Table, a [Dynamotor](Dynamotor.md) and steel plates in the plain Crafting Table.

## Hand work

Taking an assembled item can start a hand-work game. The item is only made when you finish it, and no result refuses the craft: a poor play makes a worse start, never nothing.

- **Torque.** Played for a type that spends fasteners, once per fastener. Hold the mouse to wind the gauge (about two seconds to the top) and let go inside the **green band**. Letting go short leaves the fastener loose; holding past the band strips the thread. A better fastener widens the band.
- **Rivet.** Played when the work takes rivets only, once per rivet. Hold the mouse to raise the hammer and let go to strike. A strike in the **green band** sets a tight domed head; lighter leaves the head loose, harder flattens it, and harder still splits it. Soft metal (lead, copper) forgives a wide band at a light blow; hard metal (steel, titanium) wants a harder blow in a narrower band. A better fastener widens the band.
- **Fitting.** Played for swords, spears, shields, bows, dynamotors and armor. The bench plays a sequence of tool strokes (file, tap, turn, press), each lit on its tool with a sound; repeat it by clicking the tools or pressing 1 to 4. Harder stock makes a longer sequence, tougher stock slows the strokes down and forgives a slip. A wrong stroke ends the fitting where you got to; how far you got sets how well the assembly starts. It never stops you finishing.

Press Esc to back out; the craft stays offered. A **blueprint** in the blueprint slot skips the game and counts as a small bonus.

## Fit and head start

Two figures are set when an item is assembled here, and both only fall short of what its parts allow:

- **Fit** is how well the joints are made: a looser fit weakens the joints, and for a gun widens the spread.
- **Head start** is the durability the item starts with.

Three things move them. The **fasteners** (a complex type spends them): quality above the type's floor helps, below it hurts. The **hand work** (the better the torque, rivet or fitting game, the better the start). And the **fit to the drawing**.

A [blueprint](Blueprint_Table.md#what-a-blueprint-is-for) is a drawn fit. Parts of a material inside the drawn properties fit clean; any other material still builds, and the item is marked **off the drawing**. Each part also has **axes** (a bore, a length). Hand-fitted parts may be off their mate by 20% of the axis range before the joint takes a fit flaw; parts built to a blueprint may be off the drawing by 5%. Past that the item takes a flaw: looser joints and, for a gun, a wider spread. Nothing is refused.

## Fit tab

Put a finished item in the **Item** slot and up to sixteen extras in the **Attachments** grid, one attachment to a slot: a tip, a pommel, a lining, a spring, a magazine, an addon. **Apply** fits them all, in grid order, each onto the item the last made, so extras that clash are refused as they are one at a time. It is all or nothing, and a hammer in the tool row loses a point. A type with a school needs its volume held first.

## Repair tab

Put a modular item in the **Item** slot and stock in the **Part stock** slots. A hammer in the tool row loses a point for each:

- **Repair** mends the wearing part with stock of its own material, as many pieces as it takes.
- **Swap part** replaces a part with stock of a different material or shape. The old part comes back to you, the wearing part short by its wear. The replacement wearing part is fresh.
- **Take apart** returns every part. A worn wearing part comes back short by its wear (the Salvage skill lessens it, see [Skills](Skills.md)); a held item comes back whole.

On an [Engine](Engine.md), a swapped **spark plug** is a fresh one and clears the plug's wear; reworking any other part leaves the plug's wear as it is.

The plain [Crafting Table](Crafting_Table.md) swaps a part too: lay one finished item of a type that needs no blueprint or treatise in the grid with replacement parts or stock, in any positions. The part of the same role is replaced (a role the item has empty takes the stock as an addition), the old part comes back in the grid (the wearing part short by its wear), and the item wears one point.

Repairing and a like-for-like swap need no treatise. A dish is not taken apart.

## Treatise tab

Put a **Blank Treatise** or blank blueprint paper in the blank slot and a **specimen** in the specimen slot, then press **Take apart**; it costs the hammer in the tool row a point. The specimen is taken apart and you get its parts.

- A Blank Treatise becomes the volume the specimen's type belongs to. See [Treatise](Treatise.md#obtaining) for the specimens.
- Blank paper becomes a blueprint of the design the specimen's parts make. This works on any complex type and needs no treatise.

## Recording for the Auto Engineer

Hold a [Recipe Data Stick](Data_Stick.md) in your **off hand** while you take a craft from the output slot, and the stick records that assembly. An [Auto Engineer](Auto_Engineer.md) repeats it.

## See also

- [Fasteners](Fasteners.md): screws, machine bolts and rivets, and the quality each metal gives.
- [Hand Tools](Hand_Tools.md): the hammer, file, screwdriver and sawblade the tool slots take.
