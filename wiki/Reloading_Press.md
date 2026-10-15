# Reloading Press

Where you assemble [ammunition](Ammunition.md). Put in a case, a charge and a projectile, choose the size, and pull the lever.

## Crafting

```
P S P
  I
I I I
```

P is an Iron Plate, S is a [Shaft](Shaft.md) and I is an Iron Ingot.

## The screen

| Slot | Takes |
| --- | --- |
| **Case** | a metal case |
| **Charge** | loose powder: gunpowder, blaze powder, or redstone dust. Leave empty for a jar round |
| **+ Charge** | a second powder, to make a blend |
| **Bullet** | a metal bullet, or a **nugget** to make shot |
| **Payload** | optional: glowstone dust, redstone dust, gunpowder or blaze powder |
| **Round** | where the finished cartridge comes out |

Set the **bore** with **< >** at the top. This is the die set. Every round you seat comes out at that size, and only guns of that bore (within 1 mm, on a built gun; see [Gun Parts](Gun_Parts.md#hitting-the-target)) will take it.

Press **Seat** to make one round.

## What comes out

| Charge | Projectile | Round |
| --- | --- | --- |
| powder | bullet | an ordinary cartridge |
| powder | nugget | a **shot shell**, for shotguns |
| redstone | iron bullet | a coilgun round |
| nothing | iron bullet | a **jar round**: *"Seated dry: a jar round. Charge it on a wire."* See [Round Charger](Round_Charger.md) |

## How much charge goes in

**The press seats everything in the charge slots.** Put in a stack of three and you get a three-powder round. The press doesn't decide how much is "right"; that's up to you. It refuses more than a case can physically hold (8 in total).

One case, one projectile and one payload are used per round. The charge slots are emptied.

## The preview line

With a case and charge in, a line at the bottom shows the pressure the charge will make **against what that case can hold**, e.g. *"Charge 4: 3.0 Pu against case 3.8 Pu"*. It turns **red** if the charge is more than the case can take.

That line is about the **case**. Whether your **gun** can take it is a separate question: compare it with the *Chamber* line on the gun's tooltip.

## Messages

- *"Needs a case and a bullet."*: something is missing, or something is in the wrong slot.
- *"A case takes at most 8 charge."*: take some powder out.
- *"That charge will not seat: a coil's drive cannot be blended with powder."*: you've mixed redstone with gunpowder or blaze powder.
- *"Take the round out first."*: empty the output slot.
