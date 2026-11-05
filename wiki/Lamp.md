# Glowstone Lamp

A panel of glowstone that lights by how well a power line feeds it. It replaces the vanilla [Redstone Lamp](Redstone_Lamp.md).

## Crafting

```
P P P
F D F
S S S
```

P is a Glass Pane, F is Copper Foil, D is Glowstone Dust and S is a Steel Plate.

## Using it

Place it touching an [electrical](Electricity.md) line, such as a [cable](Copper_Cable.md) or a source. It is the end of the line, a load of **0.5 A** rated for **6 V** (MV), and it has nothing to configure.

It glows in four steps by how well it is fed: dark, **dim**, **bright** and **full**, giving light level 0, 5, 10 and 15. Jade names the step. A step changes only once the supply has moved well past the boundary, so a line hovering near one does not flicker the light.

## Under and over

- **Too little** voltage or current only dims it. A starved line leaves it dark.
- **Mild overvoltage** wears the panel: it loses brightness for good, the further over the faster, and never drops below about a third of full.
- **Twice its rated voltage or more** burns the panel out at once. A burnt lamp is dark, draws nothing and stays dark; break it and place another by hand.

See [Electricity](Electricity.md#under-and-over) for how machines take too much or too little.
