# Reloading Press

Where you assemble [ammunition](Ammunition.md). Put in a case, a charge and a projectile, choose the size, and pull the lever.

## Crafting

```
P S P
  I
I I I
```

I is a Wrought Iron Plate, P is a Wrought Iron Plate and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md).

## The screen

| Slot | Takes |
| --- | --- |
| **Case** | a metal case, or a [paper case](#paper-cases) |
| **Charge** | loose powder (gunpowder, blaze powder) or a [Mixture](Mixer.md#using-it) from the Mixer |
| **+ Charge** | a second powder or Mixture, to make a blend |
| **Bullet** | a metal or polymer bullet of any [shape](Ammunition.md#bullet-shapes), or a **nugget** or other [shot](Ammunition.md#shot-shells) to make a shell |
| **core** (beside the bullet) | optional: a nugget of a second metal, set inside the bullet's jacket: see [Jacket and core](Ammunition.md#jacket-and-core) |
| **Payload** | optional: glowstone dust, redstone dust, gunpowder, blaze powder, a dust with a perk that works on what the round hits, a brewed potion, a seed, a strand of string or a lump of slime (see [Payloads](Ammunition.md#payloads)) |
| **Round** | where the finished cartridge comes out |

The top of the panel is a rail of sockets with the round being built drawn underneath: its case, powder, bullet, core and payload in the colours of what you seated, and the bullet in its real shape. A lead runs from each socket to its part. Under it the **Bore** stepper runs from 5 to 20 mm in 1 mm steps. Every round you seat comes out at that size, and only guns of that bore (within 1 mm, on a built gun; see [Gun Parts](Gun_Parts.md#hitting-the-target)) will take it. The shape is the bullet you seat; see [Bullet shapes](Ammunition.md#bullet-shapes).

Press **Seat** to make one round.

## What comes out

| Charge | Projectile | Round |
| --- | --- | --- |
| powder | bullet | an ordinary cartridge |
| powder | nugget | a **shot shell**, for smoothbore barrels |

## Paper cases

A sheet of paper put through a [Rolling Mill](Rolling_Mill.md) becomes a **Rolled Paper Case**, and the press takes it in the case slot. Paper holds almost no pressure. In a [flintlock](Guns.md#flintlock) the paper is torn open in the barrel and the round works. In a gun that chambers it, the case bursts.

## How much charge goes in

**The press seats everything in the charge slots.** Put in a stack of three and you get a three-powder round. The press doesn't decide how much is "right"; that's up to you. It refuses more than the case's bore can hold: 8 in total at 9 mm, more in a wider case (see [Charges](Ammunition.md#charges)).

A Mixture seats as the loose powders it holds. A Mixture with fluid parts is refused. The [JEI](https://modrinth.com/mod/jei) *Mixing* card lists the recipes.

One case, one projectile and one payload are used per round. The charge slots are emptied.

## The preview line

The sheet along the bottom names the bullet's shape, with its drag, pierce and hurt multipliers. With a case and charge in, it also shows the pressure the charge will make **against what that case can hold**, e.g. *"Charge 4: 3.0 Pu against case 3.8 Pu"*. It turns **red** if the charge is more than the case can take.

That line is about the **case**. Whether your **gun** can take it is a separate question: compare it with the *Chamber* line on the gun's tooltip.

## Messages

- *"Needs a case and a bullet."*: something is missing, or something is in the wrong slot.
- *"A case takes at most N charge."* (N is the most for that bore): take some powder out.
- *"Take the round out first."*: empty the output slot.
