# Optical Terminal

One end of a fiber data link. Two terminals joined by fiber carry data across a long distance with no powered relay between them.

## Crafting

```
P T P
P W P
```

P is a Brass Plate, T is Glass Tubing and W is Copper Fine Wire. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 4 screws. It needs electrical school 2.

## Using it

- **Power it.** A terminal is a sink on an [LV](Electricity.md#bands-and-amps) line and draws a small standing current while a crystal is seated. With no power or no crystal, the fiber it ends is dark. A line of a higher band burns the crystal out; the terminal itself is unharmed.
- **Seat a crystal.** Right-click with a crystal: quartz, cuprite, galena or pyrite (any item whose material rectifies). The crystal is the light source and the detector at this end.
- **Seat a circuit.** Right-click with a [circuit](Basic_Electronic_Circuit.md). Sneak and right-click with an empty hand takes the circuit out, then the crystal.
- **Right-click** empty-handed to read it: *no crystal*, *lit* or *dark*.
- **Link nodes to it** with ordinary wire, with the [Data Connector](Data_Connector.md#linking-nodes), as to any node. Join two terminals with [fiber](Data_Connector.md#fiber), made from glass rods at the [Wire Drawer](Wire_Drawer.md#what-it-makes).

Jade shows the state of a terminal and how clean its fiber reads: exact, past its clean reach and rounded to two figures, far past and rounded to one figure, or dark.

## How far it reaches

The two crystals set a **loss budget** of 20 dB, scaled by the weaker end's crystal: a crystal that rectifies better gives a larger share. A circuit with gain at either end lengthens the clean reach as at a [Data Port](Data_Port.md#circuit); the better end counts. A tube gives gain only while it is warm, and a terminal has no heater.

The **clean reach** is the loss budget divided by the fiber's loss per meter, up to 512 blocks. Soda glass reaches about 20 blocks; silica glass and quartz glass reach the cap. Beyond the clean reach a reading rounds to two figures, then one, then the link is lost ([Data Connector](Data_Connector.md#fiber)). The fiber figures are in [Materials](Materials.md#glass-fiber).

## Circuit

Right-click a terminal with a circuit to seat it. The terminal works without one. Only its gain matters here.
