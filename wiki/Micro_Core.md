# Micro Core

Starts a micro build: blocks a quarter of a block on a side, packed into a single block space. Used for small mechanisms, thin arms, fine models and the bodies of [vehicles](Vehicles.md).

## Crafting

```
RP
PR
```

R is a Wrought Iron Rod and P is a Wrought Iron Plate.

## Using it

Use the core on a block and a **micro cell** appears where a block would go, holding one voxel of stone where you pointed. A cell holds up to 64 voxels (4 by 4 by 4). Used on a cell, the core adds a stone voxel in front of the face you hit.

- **Hold a block and right-click** a cell: the voxel in front of the face you hit becomes that block, in the next cell over if you hit an edge. Only plain blocks fit (anything without a menu or machinery), and the rotation parts and joints below.
- **Mine a voxel** with the right tool for its block to take it out. Mining a cell takes one voxel at a time, by the one you look at.
- Each voxel is drawn as its block's own model at a quarter of the size, and you walk on and bump into the exact voxels. A cell weighs its voxels by what they are made of. A micro build lifts on a [Lift](Vehicles.md#the-lift) like any other.
- A glowing block as a voxel lights the space around it.
- A face on the edge of a cell is hidden when the voxel in the next cell covers it.

Blocks place the way they do full size: a log lies along its axis, a stair takes the half you clicked.

### Cost

A voxel costs a 64th of a block. A block in your hand pays for 64 voxels. Removed voxels refill that credit, and every 64 give a whole block back. The action bar shows *"N voxels left of the last block."* Creative costs nothing.

## Building tools

Hold a second Micro Core in your **other hand** and it sets how the block in your main hand is placed. **Sneak-use** the core, on a block or in the air, to cycle the mode; the message names it.

| Mode | What a click does |
| --- | --- |
| Single | Places one voxel. |
| Line | Click a start, then an end: a line of voxels joins them. |
| Box | Click one corner, then the other: the box between them fills. At most 4096 voxels in one go. |
| Mirror | Use the core on a face of the build to set a mirror plane through it; use it on the same plane again to remove it. Up to 3 planes. |

With mirror planes set, every voxel you place is copied across each plane, with its facing flipped.

## Micro rotation

A [Shaft](Shaft.md), [Bearing](Bearing.md), [Cog](Cogs.md), Gearbox, [Clutch](Clutch.md#micro-clutch), [Rotary Bearing](Rotary_Bearing.md) or [Ring Gear](Ring_Gear.md) can be a voxel too. A micro shaft lies along the face you click and joins the voxels it touches end to end. A micro cog takes the axis of the micro shaft you click it onto. Micro cogs mesh by the same rules as full-size [Cogs](Cogs.md), at a quarter of the size. The [Flywheel](Flywheel.md) stays full size.

Only a [Rotation Adapter](Rotation_Adapter.md) joins micro rotation to full-size rotation.

## Joints and ramps

The [Ball Joint](Ball_Joint.md), [Locked Joint](Locked_Joint.md), [Slider Joint](Slider_Joint.md), [Piston Joint](Piston_Joint.md), Rotary Bearing and Ring Gear are used with an empty hand once placed as voxels; see [Vehicles](Vehicles.md#joints). The [Micro Wedge](Micro_Wedge.md) is a four-step ramp voxel.

## See also

- [Vehicles](Vehicles.md): lifting a micro build, wheels and wings.
