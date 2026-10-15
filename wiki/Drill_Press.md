# Drill Press

A spinning bit over a workpiece. The shaft keeps the bit turning, but **nothing gets drilled until you push it**. It's hand-fed, one push per click.

## Crafting

```
  S
  B
I I I
```

S is a [Shaft](Shaft.md), B is a [Bearing](Bearing.md) and I is an Iron Ingot.

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

Two Iron Drilled Plates make a **Bullet Mold** (see [Ammunition](Ammunition.md#bullets)).

## Cutting is different from hammering

Hammering is about **force**. Cutting is about the **tool being harder than the metal**. The drill bit is **18**: hard enough for copper, iron, cast iron and steel (15), but **not** hardened steel (25) or netherite (40). Those need a [Grinding Wheel](Grinding_Wheel.md). JEI shows each metal's hardness under *"Tool must beat"*.

## Slow vs. fast feed

- **Slow** (10 St) cuts less per push and never jams.
- **Fast** (22 St) cuts more than twice as much per push, but **one push in twenty** the bit **binds**: you hear a clank and that push is wasted. Nothing is damaged.

Each push removes strength ÷ hardness in work. A drilled steel plate (20 Fu) is about 14 fast pushes or 30 slow ones.

## Wear

Pushing when the bit can't cut (metal too hard, or nothing left) wears the press.
