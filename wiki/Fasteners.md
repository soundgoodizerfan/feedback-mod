# Fasteners

Screws, machine bolts and rivets: the small metal pieces the [Engineering Table](Engineering_Table.md) spends to hold an assembly together. A better metal makes a better fastener, and a better fastener makes a better-fitted item.

## Obtaining

Each form exists in every metal, named for it ("Steel Screw", "Iron Rivet"). Only the metals in the [quality list](#quality) fit a fastener slot.

- **Rivet**: hammered from a **nugget** of the metal, by hand with a [Hammer](Hammer.md) in a crafting grid like any hand-hammered piece. A metal the hammer cannot move cannot be hammered into a rivet by hand.
- **Screw** and **Machine Bolt**: turned from a **rod** of the metal on a [Lathe](Lathe.md).

Gun bolts are a different part; see [Gun Parts](Gun_Parts.md).

## Quality

Quality is a figure from 1 to 5 read from the metal; a screw, a bolt and a rivet of one metal share it.

| Quality | Metals |
| --- | --- |
| 1 | Cast iron, copper, gold, lead, silver, tin, zinc |
| 2 | Aluminum, brass, bronze, iron, wrought iron |
| 3 | Chromium, manganese steel, nickel, nickel iron, steel |
| 4 | Cobalt, osmium, tungsten |
| 5 | Netherite, stainless steel, titanium |

## Using them

Put fasteners in the three fastener slots of the Engineering Table. An assembly spends as many as it asks for (two for a complex item), taking the best stacks first. The count is the only gate: a craft with too few is not offered.

Most machine recipes name the forms they take, and a fastener of another form does not count for them. Rivets suit permanent, hot or pressurised work (furnaces, engines, turbines, cooling plant). Machine bolts suit frames and moving gear that carries load (gearboxes, presses, mills, lathes, lifts). Screws suit housings, electrics and anything meant to be taken apart again. Where two forms fit, the recipe lists both, and the recipe's JEI card shows which. A recipe that names no form takes any.

Every type and recipe has a **floor**, the quality it is built for. Fasteners above the floor give the finished item a better start, fasteners below it a worse one, and the item is made either way ([Fit and head start](Engineering_Table.md#fit-and-head-start)). A fastener also widens the green band of the torque game ([Hand work](Engineering_Table.md#hand-work)). A machine recipe makes a plain item with no fit to improve, so it only counts the fasteners.

An [Auto Engineer](Auto_Engineer.md) takes fasteners from its own stock and builds no better than the floor start, and fasteners below the floor lower it.
