# Bimetallic Strip

Two metals bonded together that bend when they get hot. Clip it to a [Crucible](Crucible.md) and it becomes a **thermostat**: it switches things touching it **on** while the crucible is below its trip temperature and **off** once it's above.

## Crafting

```
F F
N N
```

F is [Copper Foil](Mechanical_Hammer.md) and N is an Iron Nugget.

## Setting it up

Place it **touching the crucible** and **touching the thing you want switched**, usually the [Clutch](Clutch.md) on your [Powered Bellows](Powered_Bellows.md)' shaft, or a [Valve](Valve.md) in its air line.

- Crucible **below** the trip point: *"Closed - calling for heat"*. Everything it touches is switched **on**.
- Crucible **above** it: *"Tripped - no longer calling"*. Everything it touches is switched **off**.

Jade shows its state and its **trip temperature**, which is printed on the strip.

It only works on a vessel it can reach, which means a crucible. It can't sense through the sealed walls of the [furnaces](Furnaces.md).

## No settings

A strip trips at one temperature, fixed by its metals: the copper–iron strip trips at **1440 Tu**. You can't adjust it. Compare its trip point with what you're making in JEI before you rely on it. That's above steel's band (1350–1410), so on its own this strip keeps a steel batch safe but not progressing.

## It overshoots, and that's expected

The strip checks once a second, and a crucible keeps climbing for a bit after the air is cut. So a strip-controlled crucible **swings past** the trip point and back. A large crucible on full air swings about 20 Tu either side. A small one swings well over 100 Tu, past 1470, where a steel batch burns.

Ways to calm it down, none of which involve a better strip:

- a **heavier** vessel: a Large Crucible, or a [Refractory Lining](Refractory_Lining.md)
- **[Insulation](Insulation.md)**
- **less air per stroke** (a short throw on the bellows), or **fewer strokes** (gear the bellows down)

When you need to watch more than one thing, or a different trip point, move up to a [Controller](Controller.md).
