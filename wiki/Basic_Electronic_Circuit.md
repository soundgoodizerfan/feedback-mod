# Basic Electronic Circuit

A soldered board with parts on it. It is not a tier or a key: it carries **properties**, and a machine that can use them works better with a good one. Nothing needs it. A detector with no circuit works, just with more noise.

## What it carries

- **Switching elements**: how many parts on the board switch (crystals, [Relays](Relay.md), [Coherers](Redstone_Coherer.md)). A circuit with none is a **dead part**.
- **Speed**: the fastest element. A rectifying crystal is fast (galena most), a relay's contacts slow, a coherer's grains slowest.
- **Noise**: the elements' own, the traces' (printed is grainier than etched), and a cold joint's. A ringing quartz plate on the board steadies it. Noise never reaches zero.
- **Joints**: good or cold, as graded by the [Solder](Solder.md) rules.

Read them with a [Component Tester](Component_Tester.md): hold it with the circuit in the other hand.

## Making one

1. Make a [Trace Board](Trace_Board.md), **before pasting it**.
2. In a crafting grid, put the board and **one part** to mount it. Up to 4 per board. A part is a **rectifying crystal** (galena, pyrite, quartz), a Relay, a Coherer, or a **quartz or amethyst plate** (the clock: it switches nothing, it steadies the rest).
3. Paste the board with solder, then solder it with the [Soldering Iron](Soldering_Iron.md) or the [Reflow Oven](Reflow_Oven.md).

A good joint gives a good circuit. A **cold joint** gives a noisy one. If the traces **lift** the circuit comes out dead, and the parts with it. Nothing refuses to run.

## Using one

Right-click a [Cat's Whisker Detector](Cats_Whisker_Detector.md) with it to fit it on the contact: the hunt report gets quieter, it hears further, and it lets go sooner. Sneak-right-click with an empty hand takes it out (crystal first).

## Overrun

- **Heat**: past what its traces stand, the traces lift and it goes dead. Baking a finished circuit in the Reflow Oven does it too. Watch the oven with a [Thermometer](Thermometer.md); take it out to stop.
- **Current**: a printed circuit on a whisker carries only 0.1 A. More than that and an element fuses now and then; the last one makes it dead. Etched traces carry 2 A, more than the whisker's own fuse ever lets through.
