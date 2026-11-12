# Cryocooler

A cold head on a wire that chills the [Reactor](Reactor.md) it is set against far below room temperature. It is what makes liquid air, and so nitrogen, oxygen and argon, by cold. See [Chemicals](Chemicals.md#air-separation).

## Crafting

```
S S S
N R N
S S S
```

S is a Steel Plate, N is a [Condenser](Condenser.md) and R is a [Refrigeration Unit](Refrigeration_Unit.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 4 [fasteners](Fasteners.md).

## Using it

1. Place it against a Reactor. It chills the block it was placed against.
2. Run a medium-voltage line to it. It draws **6 A**; with no power it holds nothing and the reactor warms back up.
3. **Right-click with an empty hand** to cycle its stage. The card names the stage and its temperature.

| Stage | The reactor settles near | Does |
| --- | --- | --- |
| Deep | -194 Tu | liquefies compressed air |
| Middle | -190 Tu | boils nitrogen off liquid air |
| Shallow | -185 Tu | boils argon off the oxygen-rich liquid |

The reactor settles a little short of the head's temperature. A stage warmer than a reaction's window does not separate: the cold liquid boils back to air. The three reactions are in the [Reactor](Reactor.md#the-reactions) table.
