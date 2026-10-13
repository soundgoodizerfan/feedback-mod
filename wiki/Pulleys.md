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

The belt item isn't used up. Linking a pulley to a new partner replaces its old belt. If they're too far apart you'll get *"Could not fit a belt there."*

## Speed

A large pulley driving a small one speeds things up, and a small one driving a large one slows them down, by less than a pair of cogs would.

## Slipping

A belt can only carry so much. It's less than a full [Mechanical Hammer](Mechanical_Hammer.md)'s worth. Ask more of it and the belt **slips**: the driving side keeps turning normally, and the driven side runs **slower** instead of stalling. Nothing is gained for free: a slipping belt delivers less speed.

Slipping is also the only thing that **wears a belt**. A worn belt carries less and slips sooner. Keep the load under what it can take and it lasts forever.

## Limitations

- The driven side should have **no source of its own**. If both lines already have their own power, the belt does nothing.
- A belt doesn't merge the two lines. Each side can be overloaded or slowed independently.
