# Galvanic Cell

A zinc–copper battery. It's the steady [electricity](Electricity.md) source: **1 A in the LV band**, for as long as its anode lasts. Five cells make a pack that gives MV.

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Galvanic Cell* in the type row); the size is set there, along with the anode shape and addon. It needs no treatise. Shape a **Cell Anode** from an ingot at the [Smithing Table](Smithing_Table.md), then assemble it at the [Engineering Table](Engineering_Table.md) with two [fasteners](Fasteners.md); the Galvanic Cell Blueprint in the blueprint slot judges the fit against your drawing. The cell comes out with the design of the anode, and the blueprint is kept.

The anode has one measure, its **size**, from 20 to 100 percent (50 by default). A bigger anode gives more current and lasts longer. Its **shape** trades current against life:

| Anode | Current | Life |
| --- | --- | --- |
| Rod | standard | standard |
| Plate | 1.6 times | 0.7 times |
| Block | 0.8 times | 1.9 times |

**Addon.** A **copper cathode** (plate) is an optional piece that adds 25% to the current.

## Using it

Place it touching [Copper Cable](Copper_Cable.md) or a machine. It supplies power straight away, and keeps supplying it until the anode is spent. There's no GUI and no off switch. To turn it off, break it or cut the cable.

It's a weak source. One cell runs one [Electromagnet](Electromagnet.md) at full strength. A cell can be drawn up to twice its 1 A, so a second magnet on the same cell still runs; hang a third and all three get two thirds of what they want.

The band comes from how far apart the two electrode metals sit in nobility: a close pair is LV, a wide pair MV. Zinc and copper are a close pair, so a single cell is LV. A cell is also a fine supply for the [Electrolytic Cell](Electrolytic_Cell.md): in water it plates pure copper out of brass and leaves the zinc dissolved.

## Using several

Cells touching the same line pool together and share the load, so two cells feed two magnets about as well as one cell feeds one. They don't *raise* the band, though. Placing more cells side by side won't make a single magnet any stronger than one cell can.

### Cell Pack

Cells in series raise the band: the first cell sets it by its pair, and every 4 cells past the first raise it one band. A **Galvanic Cell Pack** is 5 cells in series, so it is one band up: MV for zinc and copper. Craft it from five Galvanic Cells:

```
CCC
C C
```

C is a Galvanic Cell. The pack gives the same 1 A, at its higher band. A higher band carries more energy in each packet, so the pack spends its anode faster for the same current.

If a turning Electrostatic Generator is on the same line, its higher band overrules the cells for as long as the pulse lasts. Jade shows the cells as *"Outbid by a higher band"* during that time. That's normal.

## Notes

- The anode is used up by the energy the cell gives: a standard LV cell at its full 1 A spends a full anode in about 20 minutes, and a spent cell supplies nothing. Right-click it with a Zinc Ingot to restore half the anode. The remaining anode stays with the cell when you break it.
