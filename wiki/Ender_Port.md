# Ender Port

One end of a bonded pair. An item put into one port comes out of its partner, wherever that is, as long as both ends are loaded.

## Crafting

Put one Ender Pearl or [Synthetic Pearl](Synthetic_Pearl.md) and two metal plates (any metals, any arrangement) in the crafting grid. The result is **two** ports that share one bond. Ports from different crafts are not bonded to each other.

## Using it

- Put items into a port with a hopper, a belt or a [tube station](Tube_Station.md); the partner pushes them into any inventory beside it.
- **Right-click** a port to read its tank of liquid teleportant, whether its chunk is anchored and the state of its partner.
- With [Jade](https://modrinth.com/mod/jade) installed, looking at a port shows the same without a click: whether it is paired, how many blocks away its partner is, its teleportant level in mB and whether its chunk is anchored.
- A pair moves only so much mass per minute. A stack heavier than the whole allowance is handed back.
- Each crossing costs [Liquid Teleportant](Liquid_Teleportant.md) from the port's tank, which a pipe fills (the page says how to make it): more for heavier items and for longer distances.
- Shulker boxes, items that hold fluid and [pods](Carrier.md#pod) never cross.
- A shulker box or [Shulker Shell](Shulker.md) beside a port blocks pairing.

## Running dry

A pair that has carried something recently and then runs out of teleportant destabilizes: the stack waiting in the port is lost, and creatures and items near the port may hop a short random distance, as with chorus fruit. A pair that has never been fed just holds its items.

## Finding the other end

A [Tuned Eye](Tuned_Eye.md) tuned to a port reads the distance, direction and height of its partner.

## Chunk Anchor

Use a Chunk Anchor on a port to keep its chunk loaded, so the pair works while nobody is near. Without one, the pair pauses whenever an end is in an unloaded chunk.

```
S E S
S   S
S S S
```

S is a Steel Plate and E is an Eye of Ender.
