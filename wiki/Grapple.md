# Grapple

A hook on a line. Throw it at a block or a creature and the **lighter of you and the target is reeled toward the heavier**. A block is infinitely heavy, so you are pulled to it; a pig flies to you; an iron golem pulls *you*.

## Crafting

```
P R _
_ R S
_ _ S
```

P is an Iron Plate, R an Iron Rod and S String. Recipe appears once you have iron plate. It has 240 durability.

## Using it

Use it to throw. The hook flies fast on a short arc and bites the first block or living thing in its path. The line is up to **24 blocks** long.

- **A block:** you are reeled to it and released within about 2 blocks of it.
- **A creature lighter than you:** it is dragged to you.
- **A creature heavier than you:** you are dragged to it. Mass is judged by body size, the same stand-in as the [parry](Combat.md#parry)'s.
- **Release:** sneak, or use the grapple again. The line also lets go if it parts, if it has been out for ten seconds, or if either end is gone.

A [Tether](Combat.md#tethers) holds a creature on the line, and a held creature takes extra damage as with a harpoon. Grapples can't swing: the rope length doesn't constrain you sideways.

## The line has a rating

The line's material (string, wool's strength) sets the load it takes: strength × 40 in Mu. The load is the mass of the body being pulled, **tripled on any tick the reel stalls** (caught on a corner, held by the ground). Over the rating the line gains strain every tick and **parts at full strain**, which takes a few seconds of stall; the grapple takes 20 wear when it does. A clean pull never strains it. As everywhere in Feedback, wear is for misuse: a pull that never stalls costs nothing.

[Skills](Skills.md#melee-and-tether) in the tether branch reel up to 50% faster.
