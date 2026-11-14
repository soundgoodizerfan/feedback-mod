# Wire Drawer

Pulls metal through a die, again and again for as long as it's turning. Rods become wire, wire becomes fine wire, and plates become cartridge cases.

## Crafting

```
I B I
  S
```

I is a Wrought Iron Plate, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md).

## Setting it up

The drawer sits **directly on a shaft line**. Connect it from any side; you don't need a [Crank Linkage](Crank_Linkage.md). The faster the shaft turns, the faster it draws.

- **Right-click with an item** to put a workpiece in (or swap it for what's there).
- **Right-click with an empty hand** to take the workpiece out.
- **Sneak-right-click with an empty hand** to switch between **slow** and **fast** draw.

## What it makes

| From | Becomes | Then |
| --- | --- | --- |
| Rod | Wire | Fine Wire |
| Plate | Case | |

Like every machine, it doesn't stop when the wire is done. Leave copper wire in and it becomes **fine wire**. See [Metalworking](Metalworking.md).

Rods come from the [Rolling Mill](Rolling_Mill.md). Copper wire is used all over the mod: [cable](Copper_Cable.md), [electromagnets](Electromagnet.md), instruments. Cases are for [ammunition](Ammunition.md).

## Slow vs. fast

- **Slow** (13.2 St) is gentle and completely safe, and it's all copper needs. It **can't** draw steel (hardness 14.7) at all.
- **Fast** (39.6 St) pulls hard enough to draw **steel**. But every draw that actually moves the metal has a **5%** chance of **snapping the wire**, and then the workpiece is **gone**. You'll hear it go.

Steel wire is only possible on fast, so you have to accept losing the odd piece. A steel rod takes about 8 draws to become wire, so about one steel rod in three is lost along the way.

The drawer is a **30 Su** load.

## Wear

A draw that can't move the metal goes into the machine instead and wears it. That happens when the metal is too hard for the setting, when the piece is cold-worked to its limit and needs an anneal ([Work hardening](Metalworking.md#work-hardening)), or there's nothing left to draw (fine wire is as far as it goes). Correct use never wears it.

## Steel Cable

Three Steel Wire (drawn on the fast setting) stacked vertically in a crafting grid make one **Steel Cable**.
