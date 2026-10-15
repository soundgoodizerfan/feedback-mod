# Powered Bellows

Blows air into whatever it's touching. Air makes a fire burn **hotter**. It's the only way past a fuel's normal temperature, and the only way to reach steel.

## Crafting

```
L P L
L S L
```

L is Leather, P is any Planks and S is a [Shaft](Shaft.md).

## Setting it up

The bellows is driven by strokes from a [Crank Linkage](Crank_Linkage.md), the same way a hammer is. Put it **in front of the linkage**, and touching what you want to blow into:

- a [Firebox](Firebox.md) (under a crucible or boiler),
- a self-fired [furnace](Furnaces.md),
- a [Valve](Valve.md), to cut the air on and off,
- a [Pressure Vessel](Pressure_Vessel.md).

It blows into everything it touches, sharing each stroke between them.

## Throw: long is better here

On the bellows, the linkage's **long throw** moves **30 mB** of air per stroke and the short throw only **10 mB**. That's the opposite of the hammer, where short is the strong one.

A fully blown fire burns **4 mB of air a tick**. A long throw keeps up with that at about **21 RPM**. A short throw would need 64 RPM.

Gearing the linkage down doesn't squeeze any more air out of a stroke. It only changes how often the bellows strokes. Faster strokes mean more air per second.

## What air does

A fire needs a steady supply of air to stay hot. Keep the bellows pumping and the fire climbs to as much as **1.5×** what the fuel does on its own (charcoal: 1220 Tu to 1830 Tu). Stop, and the fire falls back to normal within a second or so.

Air only makes the fire hotter. **The fuel doesn't burn any faster.**

## Controlling it

The bellows has **no on/off switch**. It runs whenever its shaft turns. To control it, put a [Clutch](Clutch.md) on its shaft and switch that, by hand or with a [Bimetallic Strip](Bimetallic_Strip.md), [Timer](Timer.md) or [Controller](Controller.md). Or put a [Valve](Valve.md) between it and the fire.

It's a load of **40 Su** on the shaft, half a hammer.

Remember that the bellows can only add heat. To take heat away, use a [Damper](Damper.md).
