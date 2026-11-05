# Belt Modules

Fifteen fittings that give a [Conveyor Belt](Conveyor_Belt.md) cell a job: loading, unloading, sorting, dropping, counting and conditioning cargo. A cell holds one module. This page lists them in the order a line needs them: moving cargo on and off, sorting, then conditioning.

## Fitting and using

- **Use the module item on a cell** to fit it. A module that acts to one side acts on the side face you clicked.
- **Sneak with an empty hand** to pull the module back out.
- **Right-click the top** of a cell with an empty hand to take a unit of cargo off (the jam clear).
- **Right-click a side** with an empty hand to wipe a fouled screen or a dirty optical reader, to cycle a reader's band or a screen's mesh, or to engage or release a gate, diverter, electromagnet or air jet.
- A module that draws current ([Belt Electromagnet](#belt-electromagnet), [Belt Optical Reader](#belt-optical-reader), [Belt Weigher](#belt-weigher), [Belt Fan](#belt-fan)) needs a wire against its cell and does nothing without it. Each draws 1 A at LV. See [Electricity](Electricity.md).

## Moving cargo

### Belt Funnel (Input)

Pulls items out of the container on the side it was placed against, one unit at a time, as fast as the belt has room. It reads nothing about what it pulls.

Crafting: 2 Wrought Iron Plates + Hopper + Stick.

### Belt Funnel (Output)

Pushes passing cargo into the container on its side. At the end of a run, a full container leaves the cargo queued on the belt.

Crafting: 2 Wrought Iron Plates + Hopper + String.

### Belt Chute

Drops cargo off the belt, down the column below its side, into whatever is there: a container, a lower belt or the floor. Brittle cargo (low toughness for its hardness) breaks on a long drop, three blocks or more.

Crafting: 3 Wrought Iron Plates + Stick.

### Belt Diverter

A plough that sweeps every unit reaching it to its side. Engage or release it with an empty hand or a [Controller](Controller.md).

Crafting: Wrought Iron Plate + 2 Sticks.

### Belt Splitter

Sends passing cargo to alternate sides of the belt. If one side is full the other takes it.

Crafting: 2 Wrought Iron Plates + 2 Sticks.

### Belt Gate

Holds cargo at its cell while engaged. Release it with an empty hand or a controller.

Crafting: 2 Wrought Iron Plates + Iron Ingot + Stick.

## Sorting

A sorting module reads a property of the cargo, not its name, and sweeps cargo that matches to its side. Each reads in **bands** (for example dull, mid or bright), and each has a weakness, so a line mixes more than one kind.

| Module | Reads | Weakness |
| --- | --- | --- |
| Belt Magnet | Pulled or ignored by a magnet | Misses cargo that is hot or made of a non-magnetic metal. Iron junk is pulled as readily as iron goods. |
| Belt Electromagnet | Pulled or ignored | Same as the magnet but stronger, and can be released by hand or controller. |
| Belt Optical Reader | Sheen: dull, mid, bright | Glowing-hot cargo reads bright, and a dirty head reads everything duller. |
| Belt Weigher | Density bands and size bands | A stack blurs the reading because it weighs the pile. |
| Belt Air Jet | Light cargo, blown off the belt | Misses damp or sticky cargo. |
| Belt Screen | Weight, by a fine, medium or coarse mesh | Fines clog the mesh. |

Near a band's edge a reading can be wrong; far from the edge it is right. JEI shows the band each material sorts as, under **Sorts as**.

Cargo a reader cannot place at all (an item with no known material) is handled by the module's kind: the magnet and air jet let it pass on, and the electromagnet, optical reader and weigher send it to the side opposite the match.

### Belt Magnet

A permanent magnet over the belt. It sweeps magnetic cargo to its side.

Crafting: 2 Iron Ingots + Stick.

### Belt Electromagnet

A switchable magnet that draws current. Engage or release it with an empty hand or a controller.

Crafting: [Electromagnet](Electromagnet.md) + Wrought Iron Plate.

### Belt Optical Reader

Reads the sheen of passing cargo and sweeps one band (dull, mid or bright) to its side. Click to choose the band. A dirty head is wiped by hand; it fouls with use. Draws current.

Crafting: Optical Glass Lens + Wrought Iron Plate + Copper Wire.

### Belt Weigher

Reads the weight of passing cargo as density bands and size bands. Click to choose the band. Draws current.

Crafting: Wrought Iron Plate + Steel Plate + Copper Wire.

### Belt Air Jet

A nozzle that blows light, dry cargo off the belt. It needs a gas pipe with pressure beside it and takes a little air with each puff; the pressure sets how dense a unit it can lift. Engage or release it with an empty hand or a controller.

Crafting: Wrought Iron Plate + [Powered Bellows](Powered_Bellows.md).

### Belt Screen

A mesh the cargo passes over. Units lighter than the aperture fall through. Click to choose fine, medium or coarse. Fines clog the mesh and blind it until it is wiped by hand.

Crafting: 2 Iron Bars + Wrought Iron Plate.

## Counting and conditioning

### Belt Counter

Counts what passes and reports the count and rate to a linked [Controller](Controller.md) (see [Data Connector](Data_Connector.md)). It controls nothing and reads nothing about the cargo.

Crafting: 2 Copper Wire + Wrought Iron Plate.

### Belt Cooler

A cold plate that cools cargo passing over it toward a window, as a hot ingot must be cooled before it meets a leather band.

Crafting: Wrought Iron Plate + 2 Copper Plates.

### Belt Fan

Blows air over passing cargo and cools it. Draws current.

Crafting: 2 Wrought Iron Plates + 2 Copper Wire.
