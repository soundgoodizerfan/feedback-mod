# Basic Electronic Circuit

A soldered board with parts on it. It is not a tier or a key: it carries **properties**, and a machine that can use them works better with a good one. A detector with no circuit works, just with more noise. The circuit remembers the board it was made from, so a [Soldering Iron](Soldering_Iron.md#reworking-a-circuit) and a wick can lift its parts back out.

Any item in the circuits tag counts as a circuit wherever a recipe asks for one: the [Recipe Data Stick](Data_Stick.md), [USB Drive](Programming.md#media), [Electric Programmer](Electric_Controller.md#electric-programmer), [Programmed Junction](Pneumatic_Tube.md#tube-junction), [Analog Adapter](Redstone_Bridges.md#analog-adapter), [Dark Cell](Dark_Cell.md), the [Temperature Sensor, Current Sensor](Instruments.md) and [Fill Gauge and other sensors](Sensors.md#crafting), the Oscilloscope and the [Sorting Station](Tube_Station.md#sorting-station). The Basic Electronic Circuit is in that tag.

## What it carries

- **Switching elements**: how many parts on the board switch (crystals, [Relays](Relay.md), [Coherers](Redstone_Coherer.md), [Vacuum Tubes](Vacuum_Tube.md)). A circuit with none is a **dead part**.
- **Speed**: the fastest element. A rectifying crystal is fast (galena most), a relay's contacts slow, a coherer's grains slowest.
- **Noise**: the elements' own, the traces' (printed is grainier than etched), and a cold joint's. A ringing quartz plate on the board steadies it. Noise never reaches zero.
- **Resonator and lattice figures**: with a [Resonator](Resonator.md) on the board, **stability**, **channels** and **bandwidth**; with a [Lattice](Lattice.md), **memory**, **adaptation** and **retention**. Both elements add a little noise of their own.
- **Joints**: good or cold, as graded by the [Solder](Materials.md#solders) rules.
- **Gain** and a **heater**: only with a [Vacuum Tube](Vacuum_Tube.md) on the board. Gain is what the tubes add as amplifiers; the heater is the standing power they take to run.

Read them with a [Component Tester](Instruments.md#component-tester): hold it with the circuit in the other hand. A circuit with a resonator or a lattice says so on its tooltip.

## The cheapest way to a first circuit

No wire machine:

1. Grind a brick on a [Grinding Wheel](Grinding_Wheel.md): a Brick Substrate.
2. Substrate plus redstone dust in a crafting grid: a printed [Trace Board](Trace_Board.md).
3. Board plus a [Redstone Coherer](Redstone_Coherer.md) in a crafting grid: the part is mounted.
4. Board plus a Tin Ingot in a crafting grid: pasted (see [Solder](Materials.md#solders); leaded and lead-free solder are the more forgiving tiers).
5. Heat a [Soldering Iron](Soldering_Iron.md) at a [Firebox](Firebox.md), hold the board in the other hand and drag the tip along the pads.

The JEI page *Electronic circuit* shows these five steps on one card with each item clickable, and an advancement trail under *Electronics* hints at each next step. Hovering a substrate, a solder, a board or a circuit says what it is for.

## Making one

1. Make a [Trace Board](Trace_Board.md), **before pasting it**.
2. In a crafting grid, put the board and **one part** to mount it. Up to 4 per board. A part is a **rectifying crystal** (galena, pyrite, quartz), a Relay, a Coherer, a **sealed [Vacuum Tube](Vacuum_Tube.md)**, a **quartz or amethyst plate** (the clock: it switches nothing, it steadies the rest), a [Resonator](Resonator.md) or a [Lattice](Lattice.md). A tube still open to the air mounts as nothing.
3. Paste the board with solder, then solder it with the [Soldering Iron](Soldering_Iron.md) or the [Reflow Oven](Reflow_Oven.md).

A good joint gives a good circuit. A **cold joint** gives a noisy one. If the traces **lift** the circuit comes out dead, and the parts with it. Nothing refuses to run.

## Warming up

A circuit with a tube starts **cold**. Cold, it is slow and noisy; as its bulb warms it comes up to the figures it was built for. It is never refused: a cold circuit works, badly. Its tooltip says where it stands:

- **Its tube is warm: it works at its best.**
- **Its tube is warming: still slow and noisy.**
- **Its tube is cold: slow and noisy until a heater warms it.**

Hold Shift on the tooltip to see what each word means in how far the bulb has come toward its working heat. A [Component Tester](Instruments.md#component-tester) on a tubed circuit also reads its **gain**. Gain brings faint readings within reach of a [Galvanometer](Instruments.md#galvanometer) and lengthens [data links](Data_Connector.md#linking-nodes) that end at a host holding the circuit; a cold tube gives none.

A host that has power warms the circuit it holds with the tube's heater and pays for it: an [Electric Controller](Electric_Controller.md#circuit-slot), a [Computer](Electric_Controller.md#circuit-slot), a [Power Cutter](Power_Cutter.md#circuit) and an [Auto Engineer](Auto_Engineer.md#circuit) (circuits built in at the Engineering Table) each draw the heater's current from their line on top of their own. Heat from a fire warms the circuit too. A host with no power, such as the mechanical [Controller](Controller.md#circuit-slot) or a Cat's Whisker Detector, leaves the circuit as warm as its surroundings make it. A heater never takes a board past what its traces stand, so a tube on a board with poor traces runs short of its best.

[Instruments](Instruments.md#fitting-a-circuit) and a [robot's brain](Robots.md#the-brain) read a tubed circuit at the warmth it has: a cold one narrows an instrument's step less and gives a robot less dexterity.

## Using one

Right-click a [Cat's Whisker Detector](Cats_Whisker_Detector.md) (it hears [sparks](Redstone_Coherer.md#what-a-spark-is) without a wire to them) with it to fit it on the contact: the hunt report gets quieter, it hears further, and it lets go sooner. Sneak-right-click with an empty hand takes it out (crystal first).

A [Controller](Controller.md#circuit-slot), [Data Port](Data_Port.md#circuit), [Electrolytic Cell](Electrolytic_Cell.md#circuit), [Wire Drawer](Wire_Drawer.md#circuit) and [Drill Press](Drill_Press.md#circuit) each take one in a circuit slot. A [Power Cutter](Power_Cutter.md#circuit), [Auto Engineer](Auto_Engineer.md#circuit), [Electric Controller](Electric_Controller.md#circuit-slot), [Computer](Electric_Controller.md#circuit-slot), [Converter](Converter.md#the-circuit), [Ripeness Probe and Fill Gauge](Sensors.md#crafting) and [Catalog Desk](Catalog_Desk.md) are crafted with one built in. A [gun](Gun_Parts.md#fire-control), [launcher](Launcher.md#fuse-control) or [suit module](Exosuit.md#the-circuit-in-a-module) clips one on, and a [Robot](Robots.md#the-brain) or [Robot Arm](Robots.md#robot-arm) uses one as its brain.

## Overrun

- **Heat**: past what its traces stand, the traces lift and it goes dead. Baking a finished circuit in the Reflow Oven does it too. Watch the oven with a [Thermometer](Instruments.md#thermometer); take it out to stop.
- **Current**: a printed circuit on a whisker carries only 0.1 A. More than that and an element fuses now and then; the last one makes it dead. Etched traces carry 2 A, more than the whisker's own fuse ever lets through.

A dead circuit built into a machine can be swapped for a live one at the [Engineering Table](Engineering_Table.md#repair-tab) without losing the machine.

## Sharpening an instrument

A circuit crafted with a [Thermometer](Instruments.md#thermometer), [Pressure Gauge](Instruments.md#pressure-gauge), [Galvanometer](Instruments.md#galvanometer) or [Electroscope](Instruments.md#electroscope) is fitted to it and the instrument reads in finer steps everywhere (hand readings, tooltips, Jade, the lot). The step is multiplied by `1 - 0.6 x (1 - noise)`: a quiet etched circuit with a clock (noise near 0.05) gives about 43% of the bare step, a noisy circuit (noise 0.6) about 76%, and a dead one nothing. The step never reaches zero: a fitted instrument is still not exact. Check the circuit's noise with a [Component Tester](Instruments.md#component-tester) before fitting. Nothing is gated; a bare instrument reads, just coarser.
