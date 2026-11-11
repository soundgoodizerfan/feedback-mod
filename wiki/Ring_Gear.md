# Ring Gear

A hollow gear that turns whatever is built in front of it, driven by a cog beside it. It works like a [Rotary Bearing](Rotary_Bearing.md), with an open center for a shaft or gearbox to pass through.

## Crafting

```
  C
CBC
  C
```

C is a Small Cog and B is a [Rotary Bearing](Rotary_Bearing.md). The corners are empty.

## Using it

Place it against the face of a [Shaft](Shaft.md) end, [Gearbox](Cogs.md#gearbox) or [Cog](Cogs.md), and build in front of it. **Right-click it with an empty hand** to take what is built there (blocks and [micro cells](Micro_Core.md)) as one turning piece; right-click again to put it back as blocks. The messages, the size limit and the rules for putting back are the Rotary Bearing's.

## How it works

Nothing behind it drives it: a shaft or gearbox face behind turns freely inside the ring. Its rim meshes with cogs the way a Small Cog's does, so it is turned from the side by:

- a Small Cog beside it in its plane, at the same speed and reversed, or
- a Large Cog diagonally in its plane, on its rim: the ring turns twice as fast as the cog, reversed, with half the force.

See [Cogs](Cogs.md#how-cogs-work) for the full meshing rules. It costs the network **0.2 Su per RPM** of friction.

A Ring Gear can also be a voxel of a micro build, used with an empty hand: it turns the voxels joined to its front face, and putting back lands them at the nearest quarter turn. See [Vehicles](Vehicles.md#joints).

## See also

- [Rotary Bearing](Rotary_Bearing.md): the shaft-driven version.
