# Galvanic Cell

A zinc–copper battery. It's the steady [electricity](Electricity.md) source: low voltage, not much current, and it never runs out.

## Crafting

A Zinc Ingot, a Paper and a Copper Ingot, side by side in one row.

```
Z P C
```

## Using it

Place it touching [Copper Cable](Copper_Cable.md) or a machine. It supplies power straight away and never stops. There's no fuel, no GUI and no off switch. To turn it off, break it or cut the cable.

It's a weak source. One cell runs one [Electromagnet](Electromagnet.md), but not at full strength: the magnet's reach is shorter on a cell than on an [Electrostatic Generator](Electrostatic_Generator.md) pulse. Hang a second magnet on the same cell and the cell's voltage sags badly, so both magnets get much weaker.

An [Electroscope](Electroscope.md) shows the cell sitting at about one volt.

## Using several

Cells touching the same line pool together and share the load, so two cells feed two magnets about as well as one cell feeds one. They don't *raise* the voltage, though. Placing more cells won't make a single magnet any stronger than one cell can.

If a turning Electrostatic Generator is on the same line, its higher voltage overrules the cells for as long as the pulse lasts. Jade shows the cells as *"Outbid by a higher voltage"* during that time. That's normal.

## Notes

- The voltage comes from the two metals inside it. Only the zinc–copper cell exists right now.
- Cells don't wear out or get used up yet. That may change.
- Zinc doesn't generate in the world yet, so for now the cell is creative-only in practice.
