# Heat Exchanger

A copper-and-brick wall that lets heat flow between the two things on either side of it, **from hot to cold**, all the time.

## Crafting

```
C B C
B   B
C B C
```

C is a Copper Ingot and B is a Brick.

## Using it

It connects along one axis: the two faces at either end of the direction you placed it. Put a hot thing on one end and a cooler thing on the other. Heat flows from the hotter to the cooler, faster the bigger the difference, until they're about level.

It works with anything that has a temperature: [crucibles](Crucible.md), [furnaces](Furnaces.md), a [Boiler](Boiler.md), even overloaded [cable](Copper_Cable.md).

There's no power, no switch and no direction to set. Heat always runs downhill. The only decision is **where you put it**.

## Ideas

- **Waste-heat boiler**: put a boiler on the far side of a crucible that's running hot anyway.
- **Gentle cooling**: bleed heat out of a vessel into a cooler neighbor instead of opening a [Damper](Damper.md).
- Cooling through an exchanger counts as cooling for processes like tempering.
- **Cooling a [Condenser](Condenser.md)**: a condenser only works below boiling. An exchanger between it and the boiler hands the steam's heat back to the boiler.
