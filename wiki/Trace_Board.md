# Trace Board

A flat ceramic or polymer sheet with traces on it, the raw stuff of a circuit. The [Component Tester](Instruments.md#component-tester) reads it, and mounting parts on it makes a [Basic Electronic Circuit](Basic_Electronic_Circuit.md), which reads how well it was made.

## Making one

1. **Substrate**: grind a brick (or any ceramic brick) flat on a [Grinding Wheel](Grinding_Wheel.md); a stone wheel is enough. The result is a Brick Substrate, the base a board is built on. Insulation and how hot it can get come from the ceramic itself. A polymer can be the substrate too (an Aramid Substrate, say): beaten flat from the polymer's ingot with a hammer (it takes a lighter blow than a metal plate), with the same two ways of laying traces; see [how hot a board stands](#how-hot-a-board-stands).
2. **Lay traces**, in a crafting grid, one of two ways:
   - **Etched**: a substrate and **two fine wires of one metal** (from the [Wire Drawer](Wire_Drawer.md)). Fine, and it can carry power. There is no acid in the game, so the etching is the crafting.
   - **Printed**: a substrate and a **dust that sets as a trace** (redstone dust). The cheapest route, and the one for a first circuit: no wire machines. Lossy and for signals only, and the paste scorches at a lower heat than a wire.
3. **Mount parts** (optional): board plus one part in the grid, before pasting. See [Basic Electronic Circuit](Basic_Electronic_Circuit.md).
4. **Paste**: a board and a **solder ingot** in the grid; a plain Tin Ingot will do. The solder is spent now whatever happens later.

## Soldering it

Heat the pasted board past the solder's melting point, hold it there a moment, and no hotter than the alloy likes. Two ways: the [Soldering Iron](Soldering_Iron.md) by hand, or the [Reflow Oven](Reflow_Oven.md) in batches. See [Solder](Materials.md#solders) for what a good joint takes.

A board with parts on it comes out as a circuit. A joint comes out **unsoldered**, **cold (intermittent)** or **good**. If the traces get too hot they **lift**: you get the bare substrate back and lose the wire and the solder. A cold joint is not wasted: run it again.

## How hot a board stands

The traces lift at the lowest heat that either the substrate or the trace metal stands; a printed board lifts at 260 Tu at most. Among polymers, aramid stands 250 Tu and polytetrafluoroethylene 260 Tu, enough for leaded solder (melts at 183 Tu) and lead-free solder (217 Tu). Bakelite stands 150 Tu and polyester 115 Tu, so a board on either lifts its traces under both solders, as do other polymers that stand less heat than the solder needs. A board baked again in the [Reflow Oven](Reflow_Oven.md) lifts at the same limit.

## The way to a first circuit

The [Electronic circuit page in JEI](Basic_Electronic_Circuit.md#the-cheapest-way-to-a-first-circuit) lays the whole chain out with every item clickable. A short advancement trail under *Electronics* hints at the next step as you go.
