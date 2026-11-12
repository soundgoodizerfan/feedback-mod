# Differential

Splits one turning shaft into two axles and shares the speed between them by how hard each axle is to turn. It lets the two wheels of a vehicle go round a corner at different speeds.

## Crafting

```
C G C
  S
```

C is a Small Cog, G is a [Gearbox](Cogs.md#gearbox) and S is a [Shaft](Shaft.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md).

## Using it

Place it against the shaft that drives it: the input is the face you placed it against. The two axles come out on either side, across the way you look. It works as a full-size block and as a voxel of a [micro build](Micro_Core.md).

**Sneak-click** it to change the mode. A strip under the crosshair lists open, limited slip and locked with the current one lit.

| Mode | The axles |
| --- | --- |
| Open | The axle that resists less turns faster. A wheel in the air spins while the one on the ground stands. |
| Limited slip | The speeds may differ by only a little: each axle's share of the input stays within **30%** of an even split. |
| Locked | Both axles turn together as one solid axle. |

## How it works

Only the input shaft is joined to the differential. Each axle is a separate run of rotation, so a stalled or overloaded axle does not hold the other one still. See [Rotation](Rotation.md) and [Wheels](Vehicles.md#wheels).

## See also

- [Rotation Adapter](Rotation_Adapter.md): carries full-size rotation into a micro build.
- [Vehicles](Vehicles.md): wheels and drive.
