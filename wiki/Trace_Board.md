# Trace Board

A flat ceramic with traces on it, the raw stuff of a circuit. The [Component Tester](Component_Tester.md) reads it, and mounting parts on it makes a [Basic Electronic Circuit](Basic_Electronic_Circuit.md), which reads how well it was made.

## Making one

1. **Substrate**: grind a brick (or any ceramic brick) flat on a [Grinding Wheel](Grinding_Wheel.md). Insulation and how hot it can get come from the ceramic itself.
2. **Lay traces**, in a crafting grid, one of two ways:
   - **Etched**: a substrate and **two fine wires of one metal** (from the [Wire Drawer](Wire_Drawer.md)). Fine, and it can carry power. There is no acid in the game, so the etching is the crafting.
   - **Printed**: a substrate and a **dust that sets as a trace** (redstone dust). Cheap, but lossy and for signals only, and the paste scorches at a lower heat than a wire.
3. **Mount parts** (optional): board plus one part in the grid, before pasting. See [Basic Electronic Circuit](Basic_Electronic_Circuit.md).
4. **Paste**: a board and a **solder ingot** in the grid. The solder is spent now whatever happens later.

## Soldering it

Heat the pasted board past the solder's melting point, hold it there a moment, and no hotter than the alloy likes. Two ways: the [Soldering Iron](Soldering_Iron.md) by hand, or the [Reflow Oven](Reflow_Oven.md) in batches. See [Solder](Solder.md) for what a good joint takes.

A board with parts on it comes out as a circuit. A joint comes out **unsoldered**, **cold (intermittent)** or **good**. If the traces get too hot they **lift**: you get the bare substrate back and lose the wire and the solder. A cold joint is not wasted: run it again.
