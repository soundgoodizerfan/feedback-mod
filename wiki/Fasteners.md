# Fasteners

Screws, machine bolts, rivets, welding rods, solder wire and glue: the fixings the [Engineering Table](Engineering_Table.md) spends to hold an assembly together. A better material makes a better fixing, and a better fixing makes a better-fitted item.

## Obtaining

Each form exists in every metal, named for it ("Steel Screw", "Iron Rivet"). Only the metals in the [quality list](#quality) fit a fastener slot.

- **Rivet**: hammered from a **nugget** of the metal, by hand: hold a [Hammer](Hammer.md) in one hand and the nugget in the other, as for any hand-hammered piece. A metal the hammer cannot move cannot be hammered into a rivet by hand.
- **Screw** and **Machine Bolt**: turned from a **rod** of the metal on a [Lathe](Lathe.md), or cut from it by hand with a [File](Hand_Tools.md#filing-a-rod). Hand filing takes about twice the work of the Lathe.
- **Welding Rod**: a **rod** of any metal drawn thinner in the [Wire Drawer](Wire_Drawer.md#what-it-makes).
- **Solder Wire**: an **ingot** of any [solder](Materials.md#solders) drawn in the [Wire Drawer](Wire_Drawer.md#what-it-makes).
- **Glue**: a form of an **adhesive**, a material that holds two others together by sticking to both. **Collagen Glue** is boiled from leather and bone with 250 mB of water at about 100 Tu in a vessel over heat; the batch gives 4 glue ([Materials](Materials.md#collagen)). Anything sticky that has a quality, such as a slime ball, counts as glue without being made into one.

A screw or machine bolt can also be turned from a **thread rod**, rolled or cut. See [Thread forms](#thread-forms).

Gun bolts are a different part; see [Gun Parts](Gun_Parts.md).

## Quality

Quality is a figure from 1 to 5 read from the material; a screw, a bolt and a rivet of one metal share it (a rolled screw or bolt is one level above). A welding rod, solder wire and glue move it by their form: a welding rod gives 1 more than the metal, solder wire 1 less and glue 2 less, never below 1 or above 5. A weld holds best, solder less, glue least.

| Quality | Metals |
| --- | --- |
| 1 | Cast iron, copper, gold, lead, silver, tin, zinc |
| 2 | Aluminum, brass, bronze, iron, wrought iron |
| 3 | Chromium, manganese steel, nickel, steel |
| 4 | Cobalt, osmium, tungsten |
| 5 | Netherite, stainless steel, titanium |

The table lists metals. Among the sticky materials, collagen is 2 and slime is 1.

## Thread forms

A thread rod from the [Rolling Mill](Rolling_Mill.md) (**Thread Rolled**) or the [Lathe](Lathe.md) (**Thread Cut**) is turned on the lathe to a **Rolled** or **Cut** Screw or Machine Bolt, named for its metal and thread ("Steel Screw, Thread Cut").

- **Rolled**: the thread is worked, not cut away, so the fastener is one quality level stronger than a plain one of the metal. It has play.
- **Cut**: the metal is as it was and the fit is tight; its quality is the plain one.

Either counts as a screw or a machine bolt wherever a recipe, a driver or a hafted tool asks for one. A recipe that cannot take play refuses the rolled form: the [Lathe](Lathe.md) and the [Drill Press](Drill_Press.md) take cut machine bolts and plain ones, not rolled.

## Using them

Put fixings in the three fixings slots of the Engineering Table. An assembly spends as many as it asks for (two for a complex item), taking the best stacks first. The count is the only gate: a craft with too few is not offered.

Most machine recipes name the forms they take, and a fastener of another form does not count for them. A recipe can also refuse forms, so a pressure vessel can turn glue away. Rivets suit permanent, hot or pressurised work (furnaces, engines, turbines, cooling plant). Machine bolts suit frames and moving gear that carries load (gearboxes, presses, mills, lathes, lifts). Screws suit housings, electrics and anything meant to be taken apart again. A welding rod suits a frame that is welded rather than bolted. Solder wire and glue hold light, low-load work. Where two forms fit, the recipe lists both (the [Power Cutter](Power_Cutter.md) takes machine bolts or welding rods), and the recipe's JEI card shows which. A recipe that names no form takes any.

Every type and recipe has a **floor**, the quality it is built for. Fasteners above the floor give the finished item a better start, fasteners below it a worse one, and the item is made either way ([Fit and head start](Engineering_Table.md#fit-and-head-start)). A fastener also widens the green band of the torque game ([Hand work](Engineering_Table.md#hand-work)). A machine recipe makes a plain item with no fit to improve, so it only counts the fasteners.

An [Auto Engineer](Auto_Engineer.md) takes fasteners from its own stock and builds no better than the floor start, and fasteners below the floor lower it.
