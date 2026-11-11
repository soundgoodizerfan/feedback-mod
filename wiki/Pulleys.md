# Pulleys and Belt

Send [rotation](Rotation.md) between two shaft lines **without connecting them with a shaft**, even a good distance apart. Unlike a shaft, a belt **slips** when it's overloaded instead of stalling everything.

## Crafting

**Small Pulley**

```
P P
S S
P P
```

**Large Pulley**

```
P P P
S S S
P P P
```

P is any Planks and S is a [Shaft](Shaft.md).

**Belt**: four Leather in a square.

## Linking

1. Put a pulley on each shaft line. A pulley sits on a shaft like a [cog](Cogs.md).
2. Hold the **Belt** and right-click the first pulley: *"Pulley selected."*
3. Right-click the second pulley: *"Belt fitted."*

The belt item isn't used up. Linking a pulley to a new partner replaces its old belt. Pulleys can be up to **16 blocks** apart; further and you'll get *"Could not fit a belt there."*

## Speed

A large pulley driving a small one speeds things up, and a small one driving a large one slows them down, by less than a pair of cogs would.

## Slipping

A belt can carry **60 Su**, less than a full [Mechanical Hammer](Mechanical_Hammer.md)'s 80. Ask more of it and the belt **slips**: the driving side keeps turning normally, and the driven side runs **slower** instead of stalling. Nothing is gained for free: a slipping belt delivers less speed.

Slipping is also the only thing that **wears a belt**. A worn belt carries less and slips sooner, down to 30% condition. Keep the load under 60 Su and it lasts forever.

## Micro pulleys

Both pulley items can be placed as voxels of a [micro build](Micro_Core.md). Use the **Belt** on one pulley voxel and then on another: *"Pulley selected,"* then *"Belt fitted."* Two voxels belt together when they are up to **16 blocks** apart. The belt is drawn as two dark lines. It slips instead of stalling and wears the same way as a full-size belt. A [Lift](Vehicles.md#the-lift) carries its belts along.

A micro pulley can also be belted to a full-size pulley block: select one, then the other. The belt is drawn through the world. A micro pulley counts a quarter of the block pulley's radius, so it turns faster than the block pulley it is belted to.

## Limitations

- The driven side should have **no source of its own**. If both lines already have their own power, the belt does nothing.
- A belt doesn't merge the two lines. Each side can be overloaded or slowed independently.
