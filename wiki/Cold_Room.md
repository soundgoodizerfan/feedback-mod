# Cold Room

A walled room held cold by a [Refrigeration Unit](Refrigeration_Unit.md): a walk-in fridge or freezer. Every [Ice Box](Ice_Box.md) and every loose dropped item inside settles at the room's temperature.

## Building one

1. Enclose a space with walls of full blocks. Closed doors, trapdoors and fence gates count as wall.
2. Place a Refrigeration Unit against the inside of the room and connect it to a line. Several units can serve one room; their pull adds.
3. Optionally put a [Condenser](Condenser.md) against a unit as a compressor stage. The room then holds **Frozen** (-18 Tu) instead of **Chilled** (2 Tu), and food in it [freezes](Food.md#freezing).

The room reaches up to 7 blocks each way from the unit. Past that distance the edge of the box is an opening.

## How it works

The room drifts toward the temperature the running units can hold, slower the bigger the room. Losing power lets it drift back to the surroundings.

Walls leak by their material's thermal conductivity: wool leaks little, stone more. An opening leaks far more. Openings are a gap in the wall, an open door, open sky, or the edge of the 7-block box. An opening never stops a room counting as a room; it only raises the leak.

Each unit draws more current the more the room leaks, from a quarter of the base draw up to four times it. A tight, well-insulated room is cheap to run; a leaky one costs more current and holds a warmer temperature.

## Finding leaks

Right-click a unit with an empty hand. Particles puff at each leaking spot, and the action bar shows the room size and the number of openings. With Jade, the unit also shows the room's blocks of air, openings, temperature, leak and whether it has a compressor stage.
