# Micro Core

Starts a micro build: blocks a quarter of a block on a side, packed into a single block space. Used for small mechanisms, thin arms and fine models.

## Crafting

```
RP
PR
```

R is a Wrought Iron Rod and P is a Wrought Iron Plate.

## Using it

Use the core on a block and a **micro cell** appears where a block would go, holding one voxel of stone where you pointed. A cell holds up to 64 voxels (4 by 4 by 4). Used on a cell, the core adds a stone voxel in front of the face you hit.

- **Hold a block and right-click** a cell: the voxel in front of the face you hit becomes that block, in the next cell over if you hit an edge. It costs one block a voxel, or nothing in creative. Only plain blocks fit (anything without a menu or machinery), and rotation parts below.
- **Mine a voxel** with the right tool for its block to take it out and get its block back. Mining a cell takes one voxel at a time, by the one you look at.
- Each voxel is drawn as its block's own model at a quarter of the size, and you walk on and bump into the exact voxels. A cell weighs its voxels by what they are made of. A micro build lifts on a [Lift](Vehicles.md#the-lift) like any other.

Blocks place the way they do full size: a log lies along its axis, a stair takes the half you clicked.

## Micro rotation

A [Shaft](Shaft.md), [Bearing](Bearing.md), [Flywheel](Flywheel.md), [Cog](Cogs.md), Gearbox, [Rotary Bearing](Rotary_Bearing.md) or [Ring Gear](Ring_Gear.md) can be a voxel too. A micro shaft lies along the face you click and joins the voxels it touches end to end. A micro cog takes the axis of the micro shaft you click it onto. Micro cogs mesh by the same rules as full-size [Cogs](Cogs.md), at a quarter of the size.

Only a [Rotation Adapter](Rotation_Adapter.md) joins micro rotation to full-size rotation.

## See also

- [Vehicles](Vehicles.md#micro-blocks): lifting a micro build.
