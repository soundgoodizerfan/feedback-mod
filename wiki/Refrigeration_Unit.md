# Refrigeration Unit

A heat pump that holds an [Ice Box](Ice_Box.md) or a whole walled [cold room](Cold_Room.md) cold without ice. It is an [electrical](Electricity.md) load.

## Crafting

```
P P P
K N K
P P P
```

P is a Wrought Iron Plate, K is a Copper Coil and N is a [Condenser](Condenser.md).

## Using it

- Place it against any face of an Ice Box, or against the inside of a walled room (see [Cold Room](Cold_Room.md)), and connect it to a line.
- While it is powered it draws **2 A** on an Ice Box and holds the box near freezing (**2 Tu**, Chilled). The box needs no ice, and no ice is used up while the unit is running. Ice put in still works if the unit has no power.
- When the power fails the box drifts back to the room's temperature within a couple of seconds, the same as a box with no ice.
- Put a [Condenser](Condenser.md) against the unit as a compressor stage and it holds **-18 Tu** (Frozen) instead. Food held that cold is [frozen solid](Food.md#freezing).
- Right-click the unit with an empty hand to see where a room leaks (see [Cold Room](Cold_Room.md#finding-leaks)).
- It has no thermostat. It runs whenever it is fed, so switch it with a [Breaker](Breaker.md), a [Relay](Relay.md) or a controller.

## It runs hot

A heat pump moves heat, it does not delete it. While running, the unit itself heats up past scalding (about 115 Tu) over half a minute, so a bare hand on it is burned. Switched off, it cools over a few minutes. It does not warm the blocks around it.

With Jade, the unit says whether it is pumping, besides its own temperature. Against a room it also shows the room's blocks of air, its openings, the room temperature, the leak and whether it has a compressor stage.
