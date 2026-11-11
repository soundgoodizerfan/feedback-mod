# Rotary Bearing

Turns whatever you build in front of it, as one piece, at the speed of the shaft behind it. It is how an arm, a rotor, a wheel or a sail spins on a line of [rotation](Rotation.md), and as a micro-block it is a hinge that a card can steer.

## Crafting

```
P
S
```

P is a Smooth Stone Slab and S is a [Shaft](Shaft.md).

## Using it

Place it against the end of a [Shaft](Shaft.md), the face of a [Gearbox](Cogs.md#gearbox) or a [Cog](Cogs.md); it points away from the face you click. Its back joins that block the way a shaft end does. Build in front of it with ordinary blocks and [micro cells](Micro_Core.md).

- **Right-click it with an empty hand** to take everything built in front of it, joined block to block, as one piece. It turns with the shaft: *"Assembled: it turns with the shaft."*
- **Right-click it again** to put the piece back as blocks where it started, unturned. A piece that carries another bearing needs that bearing's piece put back first: *"Put back what its bearings carry first."*

The piece can hold up to 1024 blocks. A bigger build, or one touching the ground or something that is not part of it, is refused: *"Too big, or it touches the ground."* With nothing in front you see *"Nothing in front of it to turn."* If a block stands where one of the piece's blocks would go back, putting it back is refused: *"Something stands where a block would go back."*

## How it works

The piece speeds up and slows down with the network: a heavy piece makes the whole line slower to change speed, and the bearing is what holds it to the shaft's turn. The bearing costs the network **0.2 Su per RPM** of friction.

A bearing also works on something that moves. Put one on a car or on the arm of another bearing and the part it turns pushes back on what carries it. Put back the outermost arm first.

Breaking the bearing lets its piece go free.

## Micro bearings

A Rotary Bearing can be a voxel of a micro build, used with an empty hand. It turns the voxels joined to its front face; the rest of their cell stays. Putting back lands the voxels at the nearest quarter turn. A piece can hold up to 16384 voxels. Several spinning arms fit in one block as long as their voxels never meet.

### Roles

Before assembling, **sneak-use** the bearing with an empty hand to cycle its role; the message names it.

| Role | What the piece does |
| --- | --- |
| 0 | Turns at the shaft's speed. A card can brake it through the **Brake** channel. |
| 1 | Steers: a card sets its angle through the **Angle** channel. |
| 2, 3, 4 | Roll, pitch and yaw [control surface](Control_Surfaces.md): a card sets its angle. |

A bearing that is stopped and undriven holds its angle and does not sag.

A hinge drive that sits at its torque cap while the arm does not turn is stalled, and a stalled drive loses condition down to a floor of 30%, which cuts its torque. Time spent running costs nothing. A drive pushing against a held brake wears the brake pad and lowers the brake's grip. See [Wear](Vehicles.md#wear).

A role 0 bearing whose piece holds tire stock is a wheel; see [Wheels](Vehicles.md#wheels).

## See also

- [Ring Gear](Ring_Gear.md): the same, driven from the side by a cog instead of from behind.
- [Bearing](Bearing.md): the low-friction shaft, a different block.
- [Vehicles](Vehicles.md#joints): the other joints a micro-block build uses.
