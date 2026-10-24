# Grinding Wheel

An abrasive stone wheel. It's the hardest cutter in the game, and the **only** machine that can shape **hardened steel**.

## Crafting

```
C C C
C B C
  S
```

C is Cobblestone, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md).

## Using it

It sits **directly on a shaft line**; connect it from any side. Faster turning grinds faster. It's a **45 Su** load.

The wheel's hardness is **30**, above every metal but netherite (40). It cannot cut quartz or amethyst (40); for those, see the Diamond Grinding Wheel below.

- **Right-click with an item** to put a workpiece in (or take the current one back).
- **Right-click with an empty hand** to take it out.
- **Sneak-right-click with an empty hand** switches between **coarse** and **fine** grit.

## What it makes

**Ingot → Ground Ingot → Swarf**

It doesn't stop when the ingot is ground. Leave it and the ingot is ground down to swarf.

It also grinds [gun parts](Gun_Parts.md): a plate into a **Slide**, a rod into a **Bolt**, a drilled plate into a **Trigger Group**. And it shortens a **Gun Barrel**, 5 mm per Fu: grind it under 5 cm and it's swarf.

## Grinding makes heat

The wheel **heats whatever it's grinding**, 0.4 Tu for every Fu it grinds. Enough grinding on hardened steel can push it back into tempering range. A piece that has been on the wheel a while comes off warm or hot, and it cools down like any other [hot item](Heat.md#hot-items-stay-hot-and-cool-down-anywhere).

## The wheel wears out, and that's normal

Every other machine wears only when misused. A grinding wheel is different: **it wears every time it cuts**, because the grit is being used up.

- **Coarse** (16 St) cuts fast and wears the wheel fast: a wheel lasts **250 Fu** of grinding.
- **Fine** (6 St) cuts slower and makes the wheel last longer: about **670 Fu**.

A fully worn (bald) wheel **can't cut anything at all**. Breaking and replacing it doesn't help, because the wear goes with the item. Craft a new one.

## Diamond Grinding Wheel

The same wheel with diamond grit. Its hardness is **the diamond material's own (100)**, so it cuts everything the stone wheel does and the crystals too: **quartz and amethyst** (hardness 40), which is how you get the plates for a [Mass Probe](Mass_Probe.md).

```
D C D
C B C
  S
```

D is a Diamond, C is Cobblestone, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md).

It works exactly like the stone wheel: it sits on a shaft line, 45 Su, coarse and fine grit by sneak-right-click, heats what it grinds. **Grit is still the consumable**: it wears at the same rate per Fu as the stone wheel and a bald one cuts nothing. A harder grit does not last longer yet; that's untested.
