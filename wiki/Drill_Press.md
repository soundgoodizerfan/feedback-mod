# Drill Press

A spinning bit over a workpiece. The shaft keeps the bit turning, but **nothing gets drilled until you push it**. It's hand-fed, one push per click.

The first part cut on a drill press enters the [Machine Age](Ages.md) for you and any player nearby.

## Crafting

```
  S
  B
I I I
```

I is a Wrought Iron Plate, B is a [Bearing](Bearing.md) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 [fasteners](Fasteners.md).

## Using it

1. Put it on a shaft line (any side) and get it turning. It's a **35 Su** load.
2. **Right-click with a workpiece** to put it under the bit.
3. **Right-click with an empty hand** to push the bit down once. Each push cuts a little.
4. **Right-click with an item** on an occupied press to take the workpiece back.

**Sneak-right-click** with an empty hand switches between **slow feed** and **fast feed**.

The bit has to be spinning. Pushing a stopped bit does nothing.

## What it makes

**Plate → Drilled Plate → Swarf**

Keep pushing after the holes are done and you drill the plate away into swarf. Bits of swarf also fly off the top of the press now and then while you cut (15% of cuts).

It also drills most [gun parts](Gun_Parts.md): a turned rod into a **Gun Barrel** (and wider), an ingot into a **Receiver**, a ground ingot into a **Cylinder**, a rod into a **Magazine Tube**, a magazine tube into a **Muzzle Brake**, a nugget into a **Relief Vent**.

An ingot of any metal or polymer drills into a [Pipe](Pipe.md) of that material.

Two Wrought Iron Drilled Plates make a **Bullet Mold** (see [Ammunition](Ammunition.md#bullets)).

## Cutting is different from hammering

Hammering is about **force**. Cutting is about the **tool being harder than the metal**. The drill bit is **78**: hard enough for copper, iron, cast iron, steel (14.7) and hardened steel (68.7), but **not** netherite (128) or tungsten carbide (157). Those need a harder cutter such as the [Grinding Wheel](Grinding_Wheel.md). JEI shows each metal's hardness under *"Tool must beat"*.

## Slow vs. fast feed

- **Slow** (22 St) cuts less per push and never jams.
- **Fast** (48.4 St) cuts more than twice as much per push, but **one push in twenty** the bit **binds**: you hear a clank and that push is wasted. Nothing is damaged.

Each push removes strength ÷ hardness in work. A drilled steel plate (about 15 Fu) is about 5 fast pushes or 10 slow ones.

## Wear

Pushing when the bit can't cut (metal too hard, or nothing left) wears the press.
