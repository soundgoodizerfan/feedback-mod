# Basic Electronic Circuit

A soldered board with parts on it. It is not a tier or a key: it carries **properties**, and a machine that can use them works better with a good one. Nothing needs it. A detector with no circuit works, just with more noise.

## What it carries

- **Switching elements**: how many parts on the board switch (crystals, [Relays](Relay.md), [Coherers](Redstone_Coherer.md)). A circuit with none is a **dead part**.
- **Speed**: the fastest element. A rectifying crystal is fast (galena most), a relay's contacts slow, a coherer's grains slowest.
- **Noise**: the elements' own, the traces' (printed is grainier than etched), and a cold joint's. A ringing quartz plate on the board steadies it. Noise never reaches zero.
- **Joints**: good or cold, as graded by the [Solder](Solder.md) rules.

Read them with a [Component Tester](Component_Tester.md): hold it with the circuit in the other hand.

## The cheapest way to a first circuit

No ore trip beyond tin and redstone, and no wire machine:

1. Grind a brick on a [Grinding Wheel](Grinding_Wheel.md): a Brick Substrate.
2. Substrate plus redstone dust in a crafting grid: a printed [Trace Board](Trace_Board.md).
3. Board plus a [Redstone Coherer](Redstone_Coherer.md) in a crafting grid: the part is mounted.
4. Board plus a Tin Ingot in a crafting grid: pasted (see [Solder](Solder.md); leaded and lead-free solder are the more forgiving tiers).
5. Heat a [Soldering Iron](Soldering_Iron.md) at a [Firebox](Firebox.md), hold the board in the other hand and drag the tip along the pads.

The JEI page *Electronic circuit* shows these five steps on one card with each item clickable, and an advancement trail under *Electronics* hints at each next step. Hovering a substrate, a solder, a board or a circuit says what it is for.

## Making one

1. Make a [Trace Board](Trace_Board.md), **before pasting it**.
2. In a crafting grid, put the board and **one part** to mount it. Up to 4 per board. A part is a **rectifying crystal** (galena, pyrite, quartz), a Relay, a Coherer, or a **quartz or amethyst plate** (the clock: it switches nothing, it steadies the rest).
3. Paste the board with solder, then solder it with the [Soldering Iron](Soldering_Iron.md) or the [Reflow Oven](Reflow_Oven.md).

A good joint gives a good circuit. A **cold joint** gives a noisy one. If the traces **lift** the circuit comes out dead, and the parts with it. Nothing refuses to run.

## Using one

Right-click a [Cat's Whisker Detector](Cats_Whisker_Detector.md) (it hears [sparks](Redstone_Coherer.md#what-a-spark-is) without a wire to them) with it to fit it on the contact: the hunt report gets quieter, it hears further, and it lets go sooner. Sneak-right-click with an empty hand takes it out (crystal first).

## Overrun

- **Heat**: past what its traces stand, the traces lift and it goes dead. Baking a finished circuit in the Reflow Oven does it too. Watch the oven with a [Thermometer](Thermometer.md); take it out to stop.
- **Current**: a printed circuit on a whisker carries only 0.1 A. More than that and an element fuses now and then; the last one makes it dead. Etched traces carry 2 A, more than the whisker's own fuse ever lets through.

## Sharpening an instrument

A circuit crafted with a [Thermometer](Thermometer.md), [Pressure Gauge](Pressure_Gauge.md), [Galvanometer](Galvanometer.md) or [Electroscope](Electroscope.md) is fitted to it and the instrument reads in finer steps everywhere (hand readings, tooltips, Jade, the lot). The step is multiplied by `1 - 0.6 x (1 - noise)`: a quiet etched circuit with a clock (noise near 0.05) gives about 43% of the bare step, a noisy circuit (noise 0.6) about 76%, and a dead one nothing. The step never reaches zero: a fitted instrument is still not exact. Check the circuit's noise with a [Component Tester](Component_Tester.md) before fitting. Nothing is gated; a bare instrument reads, just coarser.
