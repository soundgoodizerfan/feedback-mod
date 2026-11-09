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
| Winch | 1 Grapple Winch | The reel runs twice as fast |
| Barbed head | 2 Wrought Iron Rods | A lighter creature is reeled to you |
| Second hook | 3 Wrought Iron Plates | A second line, thrown by using the grapple again with a line out |
| Metal line | 4 wires of one metal (Iron Wire, Copper Wire, any metal's wire) | The line is made of that metal; the tooltip reads "Line: <material>" |

A metal line can be fitted again with a different metal's wire. See [The line has a rating](#the-line-has-a-rating) for what the material changes.

The **Grapple Winch** is its own item:

```
R _ _
P S P
```

R is a Wrought Iron Rod, P is a Wrought Iron Plate and S is a [Shaft](Shaft.md).

## Using it

Use it to throw. The hook flies fast on a short arc and bites the first block or living thing in its path. The bare line is **16 blocks** long, with a slow reel.

- **Hold use** to winch yourself toward the anchor. With two lines out, both reel.
- **Sneak** to let go of every line. You keep your speed, with a small boost. Using the grapple again does not let go.
- **A block:** the line is fixed at the bite. Its length is how far you were from the anchor when it caught, up to the line's length. Fall, or run past the line's length, and you arc around the anchor, keeping your sideways speed. The swing is a smooth pendulum; a hook straight overhead does not bounce.
- **A creature, without a barb:** the creature is a swing anchor, whatever its size.
- **A creature, with a barb:** a creature lighter than you is dragged to you. A heavier one is still a swing anchor. Mass is judged by body size, the same stand-in as the [parry](Combat.md#parry)'s.

With a second hook fitted, using the grapple while a line is out throws the second hook, and both lines hold you as you swing. The two lines hold together: you cannot slip one by leaning on the other.

### Swinging

- **The line catches on edges.** A line that runs over a block corner bends round it and the swing hangs from the corner, with the line that is left. Up to eight bends. Each edge the line is dragged over saws at it: a bent line takes more load than a straight one (see below), so a line a straight swing would not part may part round corners. A [sheave](#sheave) is a bend that does not saw.
- **Winch.** The drum comes up to speed over half a second of holding use. Near the anchor the line drags your speed down so you stop at the anchor instead of hitting it.
- **Pay out.** Hold **X** (*Grapple: pay out line*, rebindable) to let line out, up to the grapple's longest line (16 blocks, 28 with the extra coil).
- **Pump.** In the air on a taut line, hold a direction to add speed along it, up to a limit. Air drag is lighter on a line and for a second after letting go, so a swing keeps the speed it was given.
- **Jump** to let go with an upward kick (in the air) and the swing's momentum.
- **The line shows its slack.** A loose line hangs; a taut one is straight, and the stretches over edges are straight.
- **The line reports its load.** While you hold the grapple with a line out, the action bar reads *Line: slack* (under 30% of the line's [rating](#the-line-has-a-rating)), *taut* (under 70%), *straining* (up to the rating) or *about to part* (over it, so strain is building and the line will part). A reader at the exact tier of the [Instruments](Instruments.md) sees the load as a percent of the rating and the strain as a percent instead.

The line also lets go if it parts, if it has been out for ten seconds, or if either end is gone.

A [Tether](Combat.md#tethers) holds the body on the line, and a creature held that way takes extra damage as with a harpoon.

## What the hook bites into

A swing's hook holds by the block it bit: its hardness sets the load it takes. Stone, logs and metal hold a player easily; dirt, netherrack, snow, leaves and wool let go under a player's weight, sand and gravel hold half as well as their hardness says, and ice a third. A block that cannot be broken holds anything. Over the figure the hook works loose, gaining strain each tick, and **rips free at full strain** (no wear on the grapple). If the block is broken out from under the hook, the line lets go.

## Fire and blades

Fire or lava touching the line burns it through in about a second if the line's material oxidises below the flame's temperature (string does). A sword swung across the line, or an arrow flying through it (not your own), strains it by the attacker's damage over the line's rating: an iron sword parts string in a swing or two, a bare fist takes many.

## The line has a rating

The line's material sets the load it can take: string rates as wool, and a metal line rates at that metal's yield strength. The load is the mass of the body on the line, plus the weight of the line itself (the material's density times the line's length), so a steel chain is stronger than string but heavier, and a longer line weighs more. A creature being hauled in counts as far heavier on any tick its reel stalls (caught on a corner, held by the ground). While you swing, your own weight is the load, plus a quarter of it for every edge the line is bent over, so a heavy player on a weak line can part it. Over the rating the line gains strain every tick and **parts at full strain**; a heavier overload parts it sooner. The grapple takes 20 wear when the line parts. A load under the rating never strains it, and wear is for misuse: a pull or swing under the rating costs nothing.

A string or wool line is weaker when wet: in water or rain it holds 75% of its rating.

## Lightning

A metal line is a path for lightning. Out in a thunderstorm under open sky, a metal line has a small chance each second (3%) to draw a lightning strike on the one holding the grapple, and the line parts.

## Sheave

A pulley wheel you set in the air, with no collision. A line that runs through it turns on it, like a bend at its center, and goes on from there. A sheave adds no load to the line, where a block edge saws at it. It lets you hang a line from a beam or post.

```
_ P _
P I P
_ P _
```

P is an Iron Plate and I an Iron Ingot.
