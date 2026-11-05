# Grapple

A hook on a line. Throw it at a block or a creature and the line catches. You **swing** from a block, or from a creature when the hook has no barb; with a barb a lighter creature is reeled to you instead.

## Crafting

```
P R _
_ R S
_ _ S
```

P is a Wrought Iron Plate, R a Wrought Iron Rod and S String. Recipe appears once you have wrought iron plate. It has 240 durability.

### Fittings

Put the grapple in the crafting grid with the parts, in any arrangement. Each fitting goes on once, and the tooltip lists the ones fitted.

| Fitting | Parts | Effect |
| --- | --- | --- |
| Extra coil | 4 String | The line is 28 blocks long instead of 16 |
| Winch | 1 Small Pulley | The reel runs twice as fast |
| Barbed head | 2 Wrought Iron Rods | A lighter creature is reeled to you |

## Using it

Use it to throw. The hook flies fast on a short arc and bites the first block or living thing in its path. The bare line is **16 blocks** long, with a slow reel.

- **Hold use** to winch yourself toward the anchor.
- **Sneak** to let go. You keep your speed, with a small boost. Using the grapple again does not let go.
- **A block:** the line is fixed at the bite. Its length is how far you were from the anchor when it caught, up to the line's length. Fall, or run past the line's length, and you arc around the anchor, keeping your sideways speed. The swing is a smooth pendulum; a hook straight overhead does not bounce.
- **A creature, without a barb:** the creature is a swing anchor, whatever its size.
- **A creature, with a barb:** a creature lighter than you is dragged to you. A heavier one is still a swing anchor. Mass is judged by body size, the same stand-in as the [parry](Combat.md#parry)'s.

The line also lets go if it parts, if it has been out for ten seconds, or if either end is gone.

A [Tether](Combat.md#tethers) holds the body on the line, and a creature held that way takes extra damage as with a harpoon.

## The line has a rating

The line's material (string, wool's strength) sets the load it can take. The load is the mass of the body on the line. A creature being hauled in counts as far heavier on any tick its reel stalls (caught on a corner, held by the ground). While you swing, your own weight is the load, so a heavy player on a weak line can part it. Over the rating the line gains strain every tick and **parts at full strain**; a heavier overload parts it sooner. The grapple takes 20 wear when the line parts. A load under the rating never strains it, and wear is for misuse: a pull or swing under the rating costs nothing.
