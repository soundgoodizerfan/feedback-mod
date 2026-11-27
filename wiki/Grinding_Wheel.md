# Grinding Wheel

Spins an [Abrasive Wheel](Machine_Heads.md) against a workpiece. With a hard grit it cuts what a drill bit or a lathe bit cannot, such as tungsten carbide and cermet.

The first part cut on a grinding wheel enters the [Machine Age](Ages.md) for you and any player nearby.

## Crafting

```
C C C
C B C
  S
```

C is Cobblestone, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md).

The wheel is a [machine head](Machine_Heads.md): draw it at the Blueprint Table and put it together in a crafting grid. Its edge material is the grit; a wheel with diamond grit cuts the hardest stock. Diamond Grit is the lump form of diamond, so it is stock for the wheel's edge at the part bench ([Smithing Table](Smithing_Table.md)).

## Using it

It sits **directly on a shaft line**; connect it from any side. Faster turning grinds faster. It's a **45 Su** load.

The wheel's **grit hardness** decides what it cuts: everything softer than its grit, quartz and amethyst (108) and cermet (177) included. Titanium carbide and boron (294) need harder grit, such as diamond. The wheel's grit axis and shape set how hard the machine may drive it.

- **Right-click with an Abrasive Wheel** to seat it; the wheel it replaces comes back.
- **Right-click with an item** to put a workpiece in (or take the current one back).
- **Right-click with an empty hand** to take the workpiece out, then the wheel.

With no wheel the machine does nothing.

## What it makes

**Ingot → Ground Ingot → Swarf**

It doesn't stop when the ingot is ground. Leave it and the ingot is ground down to swarf.

It also grinds [gun parts](Gun_Parts.md): a plate into a **Slide**, a rod into a **Bolt**, a drilled plate into a **Trigger Group**. And it shortens a **Gun Barrel**, 5 mm per Wu: grind it under 5 cm and it's swarf.

A pass that takes more off than the stage asks leaves the piece overworked; see [Worked past its mark](Metalworking.md#worked-past-its-mark).

## Grinding makes heat

The wheel **heats whatever it's grinding**, 0.4 Tu for every Wu it grinds. Enough grinding on hardened steel can push it back into tempering range. A piece that has been on the wheel a while comes off warm or hot, and it cools down like any other [hot item](Heat.md#hot-items-stay-hot-and-cool-down-anywhere).

## The wheel wears out, and that's normal

Every other head wears only when misused. An abrasive wheel is different: **it wears every time it cuts**, because the grit is being used up. A finer grit cuts gently and sheds slowly.

A fully worn (bald) wheel **can't cut anything at all**. The wear goes with the wheel, so taking it out and putting it back does not help. Make another.
