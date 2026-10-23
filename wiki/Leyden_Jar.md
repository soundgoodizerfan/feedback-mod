# Leyden Jar

A glass jar lined with copper foil that **stores a charge and gives it back as a burst**. It's what makes the [Electrostatic Generator](Electrostatic_Generator.md)'s one-second pulse useful.

## Crafting

```
  W
F G F
F G F
```

W is [Copper Wire](Wire_Drawer.md), F is [Copper Foil](Mechanical_Hammer.md) and G is Glass.

## How it works

Put it on a line with a source. It switches between two jobs on its own:

- **Charging.** While a higher voltage is on the line, the jar draws **1 A** and fills up.
- **Discharging.** When the supply goes (the pulse ends, the cell is cut off), the jar becomes a source itself and gives back up to **8 A** at whatever voltage it holds.

It holds **10 amp-ticks per volt**. One Electrostatic Generator pulse (1 A for 20 ticks) fills it to the generator's 1.5 V with a little to spare.

## A burst, not a battery

Its voltage **falls as it empties**, so anything running off it slows down as it goes: a strong start that tails off. It also **leaks** about 0.2% of its charge every tick, which halves it in roughly 17 seconds, whether or not anything is connected.

There's no general-purpose battery in Feedback. A jar smooths a pulse and carries you over a gap; it won't store a day's power.

## Careful

A jar charged to **6 V (MV) or more** shocks whoever touches it, walking into it or right-clicking it. See [Hazards](Hazards.md#shock). One charged from a Galvanic Cell or an Electrostatic Generator is never that high.

See also: [Jar Baton](Jar_Baton.md), which draws a jar's charge and spends it on a melee hit.
