# Belt Modules

Twenty-one fittings that give a [Conveyor Belt](Conveyor_Belt.md) cell a job: loading, unloading, sorting, dropping, counting and conditioning cargo. A cell holds one module. This page lists them in the order a line needs them: moving cargo on and off, sorting, then conditioning. The sorting modules other than the air jet also serve as [reader heads](Warehouse_Dock.md#reader-heads) on a Warehouse Dock, and set the filter of a [Pallet Rack](Pallet_Rack.md#filter). The output funnel, chute, diverter, splitter and gate also fit on the [Belt Head Roller](Conveyor_Belt.md#belt-head-roller) at the end of a run.

## Fitting and using

- **Use the module item on a cell** to fit it. A module that acts to one side acts on the side face you clicked.
- **Sneak with an empty hand** to pull the module back out.
- **Right-click the top** of a cell with an empty hand to take a unit of cargo off (the jam clear).
- **Right-click a side** with an empty hand to wipe a fouled screen or a dirty optical reader, to cycle a reader's band or a screen's mesh, or to engage or release a gate, diverter, electromagnet or air jet.
- A module that draws current ([Belt Electromagnet](#belt-electromagnet), [Belt Optical Reader](#belt-optical-reader), [Belt Weigher](#belt-weigher), [Belt Heat Reader](#belt-heat-reader), [Belt Fan](#belt-fan), [Linear Motor Coil](#linear-motor-coil)) needs a wire against its cell and does nothing without it. Each draws 1 A at LV. See [Electricity](Electricity.md).

## Crafting

The plain [Crafting Table](Crafting_Table.md) makes the funnels, chute, diverter, magnet, screen, air jet, scratch reader, drag reader, spark reader, hood and cooler. The gate, splitter, counter, fan, linear coil, electromagnet, optical reader, weigher and heat reader are made at the [Engineering Table](Engineering_Table.md): each line below names the screws and the [treatise](Treatise.md) volume held, and every one wears the [screwdriver](Hand_Tools.md#screwdriver) a point.

## Moving cargo

### Belt Funnel (Input)

Pulls items out of the container on the side it was placed against, one unit at a time, as fast as the belt has room. It reads nothing about what it pulls.

Crafting: 2 Wrought Iron Plates + Hopper.

### Belt Funnel (Output)

Pushes passing cargo into the container on its side. At the end of a run, a full container leaves the cargo queued on the belt.

Crafting: Hopper + 2 Wrought Iron Plates.

### Belt Chute

Drops cargo off the belt, down the column below its side, into whatever is there: a container, a lower belt or the floor. Brittle cargo (low toughness for its hardness) breaks on a long drop, three blocks or more.

Crafting: 3 Wrought Iron Plates.

### Belt Diverter

A plough that sweeps every unit reaching it to its side. Engage or release it with an empty hand or a [Controller](Controller.md).

Crafting: 3 Wrought Iron Plates + Wrought Iron Rod.

### Belt Splitter

Sends passing cargo to alternate sides of the belt. If one side is full the other takes it.

Crafting: 2 Wrought Iron Plates + Wrought Iron Rod + Escapement. Engineering Table, screwdriver, 2 screws, Mechanical Treatise I.

### Belt Gate

Holds cargo at its cell while engaged. Release it with an empty hand or a controller.

Crafting: 2 Wrought Iron Plates + Wrought Iron Rod + [Ball Joint](Joints.md#ball-joint). Engineering Table, screwdriver, 2 screws, Mechanical Treatise I.

## Sorting

A sorting module reads a property of the cargo, not its name, and sweeps cargo that matches to its side. Each reads in **bands** (for example dull, mid or bright), and each has a weakness, so a line mixes more than one kind.

| Module | Reads | Weakness |
| --- | --- | --- |
| Belt Magnet | Pulled or ignored by a magnet | Misses cargo that is hot or made of a non-magnetic metal. Iron junk is pulled as readily as iron goods. |
| Belt Electromagnet | Pulled or ignored | Same as the magnet but stronger, and can be released by hand or controller. |
| Belt Optical Reader | Sheen: dull, mid, bright | Glowing-hot cargo reads bright, and a dirty head reads everything duller. |
| Belt Weigher | Density: light, mid or heavy | A stack blurs the reading because it weighs the pile. |
| Belt Scratch Reader | Hardness: soft, mid or hard | Its point blunts on the hardest stuff and reads it poorly. |
| Belt Drag Reader | Grip: slick, mid or grippy | A sticky piece reads grippier than it is. |
| Belt Spark Reader | Kind of material | Alloys, coats and soft metals muddy the test. |
| Belt Heat Reader | Heat: cold, warm or hot | A bright surface reads colder than it is. |
| Belt Air Jet | Light cargo, blown off the belt | Misses damp or sticky cargo. |
| Belt Screen | Weight, by a fine, medium or coarse mesh | Fines clog the mesh. |

Near a band's edge a reading can be wrong; far from the edge it is right, apart from the [scratch reader](#belt-scratch-reader) on very hard stuff and the [spark reader](#belt-spark-reader). JEI shows the sheen, density, hardness and kind bands of each material under **Sorts as**.

Cargo a reader cannot place at all (an item with no known material) is handled by the module's kind: the magnet, air jet, scratch reader, drag reader and spark reader let it pass on, and the electromagnet, optical reader, weigher and heat reader send it to the side opposite the match.

### Belt Magnet

A permanent magnet over the belt. It sweeps magnetic cargo to its side.

Crafting: 2 Magnetite Lumps + 3 Wrought Iron Plates.

### Belt Electromagnet

A switchable magnet that draws current. Engage or release it with an empty hand or a controller.

Crafting: [Electromagnet](Electromagnet.md) + 3 Wrought Iron Plates + 2 Copper Wire. Engineering Table, screwdriver, 2 screws, Electrical Treatise II.

### Belt Optical Reader

Reads the sheen of passing cargo and sweeps one band (dull, mid or bright) to its side. Click to choose the band. A plated piece reads as its coat's metal, in proportion to how much of the coat is on it: a gold-plated iron ingot reads as gold. A dirty head is wiped by hand; it fouls with use. Draws current.

Crushed ore and dust it meets are **separated** instead: it tells the ore from its rock by their sheen. The concentrate is swept off its side, the tailings drop below the cell, and a unit it cannot tell goes on to be tried again. A dirty head separates nothing. Chain readers along a belt to raise the share that gets sorted ([Ore Processing](Ore_Processing.md#separating)).

Crafting: Optical Glass Lens + [Photocell](Sensors.md) + 2 Steel Plates + a [Basic Electronic Circuit](Basic_Electronic_Circuit.md). Engineering Table, screwdriver, 3 screws, Electrical Treatise III.

### Belt Weigher

Weighs passing cargo and sweeps one density band (light, mid or heavy) to its side. Click to choose the band. Draws current.

Crafting: Balance Scale + 2 Steel Plates + a [Basic Electronic Circuit](Basic_Electronic_Circuit.md). Engineering Table, screwdriver, 3 screws, Electrical Treatise III.

### Belt Air Jet

A nozzle that blows light, dry cargo off the belt. It needs a gas pipe with pressure beside it and takes a little air with each puff; the pressure sets how dense a unit it can lift. Engage or release it with an empty hand or a controller.

Crafting: Copper Tubing + [Powered Bellows](Powered_Bellows.md) + Wrought Iron Plate.

### Belt Screen

A mesh the cargo passes over. Units lighter than the aperture fall through. Click to choose fine, medium or coarse. Fines clog the mesh and blind it until it is wiped by hand.

Crafting: 4 Wrought Iron Plates + 5 Wrought Iron Rods.

### Belt Scratch Reader

Drags a hard point over passing cargo and sweeps one hardness band (soft, mid or hard) to its side. Click to choose the band. The point blunts on very hard cargo, which it can misread even far from a band edge.

Crafting: 2 Wrought Iron Plates + Wrought Iron Rod + Diamond.

### Belt Drag Reader

Drags a weighted shoe over passing cargo and sweeps one grip band (slick, mid or grippy) to its side. Click to choose the band. A sticky piece reads grippier than it is.

Crafting: 2 Wrought Iron Rods + String + Wrought Iron Plate.

### Belt Spark Reader

Tests passing cargo by spark and flame and sweeps one kind to its side: metallic, stony, granular, organic, polymer or other. Click to choose the kind. The test works from a sample, so even a plain piece is now and then mistaken. Alloys, plated pieces, soft metals and [look-alikes](Tube_Station.md#sorting-by-kind) fool it more often.

Crafting: 2 Wrought Iron Plates + Flint + Wrought Iron Rod.

### Belt Heat Reader

An optical thermometer that sweeps one heat band to its side: **cold** (up to 60 Tu), **warm** (above 60 Tu) or **hot** (200 Tu and over). Click to choose the band. A bright surface reads colder than it is. Draws current.

Crafting: Optical Glass Lens + [Photocell](Sensors.md) + 2 Steel Plates + 2 Copper Wire + a [Basic Electronic Circuit](Basic_Electronic_Circuit.md). Engineering Table, screwdriver, 3 screws, Electrical Treatise III.

## Counting and conditioning

### Belt Counter

Counts what passes and reports to a linked [Controller](Controller.md) (see [Data Connector](Data_Connector.md)). Link it to the Controller and read it with an Input card: key **C** reads the count, any other key the rate. It controls nothing and reads nothing about the cargo.

Crafting: Pivot Arm (Short) + [Small Cog](Cogs.md) + Escapement + 2 Wrought Iron Plates. Engineering Table, screwdriver, 2 screws, Mechanical Treatise I.

### Belt Hood

A cover over a belt cell. Rain and fluid above do not wet the band there, so a rise keeps its grip. Cargo that holds fluid still wets it.

Crafting: 3 Wrought Iron Plates + 2 Leather.

### Linear Motor Coil

A coil in place of a roller. While it has current it drives the whole run at a fixed speed, with or without a head roller or a shaft. Draws current.

Crafting: 2 Copper Wire + Wrought Iron Rod + 3 Wrought Iron Plates. Engineering Table, screwdriver, 2 screws, Electrical Treatise II.

### Belt Cooler

A cold plate that cools cargo passing over it toward a window, as a hot ingot must be cooled before it meets a leather band.

Crafting: Heatsink + Copper Plate + Wrought Iron Plate.

### Belt Fan

Blows air over passing cargo and cools it. Draws current.

Crafting: 4 Wrought Iron Plates + [Dynamotor](Dynamotor.md). Engineering Table, screwdriver, 4 screws, Mechanical Treatise I.
