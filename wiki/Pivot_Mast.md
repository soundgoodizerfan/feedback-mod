# Pivot Mast

A vertical shaft whose arm sweeps a circle. The tip on the arm works every block the arm crosses, along its whole length, so one mast tends a round field.

## Crafting

```
P S P
P G P      P wrought iron plate, S shaft, G gearbox, B bearing
P B P
```

| Part | Recipe |
| --- | --- |
| Pivot Arm (Short), 3 blocks | three wrought iron rods in a row |
| Pivot Arm (Medium), 5 blocks | three rods in a row, one beneath the left |
| Pivot Arm (Long), 8 blocks | two rows of three rods |
| Any tip | a wrought iron plate, a wrought iron rod and a marker, shapeless |

Tip markers: string for Cutting, flint for Breaking, wheat seeds for Planting, wheat for Feeding, an iron nugget for Striking, a hopper for Collecting.

## Using it

Run a shaft into the top or bottom. Right-click the mast with an arm, then a tip. The Cutting, Breaking and Striking tips also need a [Hammer](Hammer.md) seated as the head, the same way. Seed, feed or bone meal for the Planting and Feeding tips goes in as any other item, by hand or from a hopper; what the Collecting tip sweeps up is in the other slots, which a hopper can empty. Sneak with an empty hand to take parts back, one at a time. An empty hand reads the arm, the tip and the head's condition.

The arm turns slowly. A longer arm and a heavier head ask more of the drive, and a drive too weak for them stalls.

| Tip | What it does to each block it crosses |
| --- | --- |
| Cutting | cuts plants, leaves, cane, kelp and the wool of sheep |
| Breaking | breaks blocks the head is strong enough for; machines and chests are left standing |
| Planting | places the block item in its slot where it can stand |
| Feeding | uses the item in its slot on whatever is there: feed on an animal, bone meal on a crop, shears on a sheep |
| Striking | strikes creatures with the weight of the head |
| Collecting | sweeps dropped items into the mast |

## Misuse

A tip knows nothing about what it works on. A Cutting tip cuts green crop as readily as ripe, so switch the drive with a [Ripeness Probe](Sensors.md): seat it as the sensing head, link the mast to a Controller, and stop the arm while the crop is unripe. A Breaking tip on a block too hard for the head does not break it and wears the head by the difference. A Striking tip hurts anything living, a player included.

## See also

- [Sensors](Sensors.md), [Controller](Controller.md), [Rotation](Rotation.md)
