# Dark Cell

A mob farm that is a sealed dark room. Hostile mobs gather in it as a count and die by the kill methods set in its wall. Their drops come out beside the controller.

## Building it

Enclose a room, put a Dark Cell controller in its wall, and keep the inside dark: no block light, no daylight. A room with an opening, or any light inside, does nothing. Right-click the controller with an empty hand to read the room's volume, how many mobs are in it, its shield and how many kill methods it has.

```
P P P
P C P      P wrought iron plate, C coal block
P P P
```

Hostile mobs gather up to a limit set by the room's volume. The overworld, the Nether and the End each draw from their own kinds.

## Kill methods

Set kill-method blocks in the wall so they touch the inside. They work together; stack more than one.

| Block | Harms | Does nothing to |
| --- | --- | --- |
| Fall Plate | anything that takes fall damage; empty hand steps the height | mobs immune to falls |
| Fire Plate | anything that burns, as hot as the heat pointed at it | mobs immune to fire |
| Crush Head | everything, with the strength of a seated [Hammer](Hammer.md) turned by a shaft | |
| Fume Port | mobs that breathe, with the toxicity of the gas piped in | undead and non-breathers |
| Shock Plate | anything, with the power fed to it | a few mobs, such as creepers |
| Radiation Source | everything living, from a radioactive item loaded by hand | |

Heat cooks food drops; food left in the output chars. A Shock Plate scorches what large bodies leave. A Radiation Source makes the drops contaminated and gives off radiation itself. A Fume Port with a full tank and nothing to do seeps into the room outside.

```
Fall Plate   P P P / S . S       Fire Plate    P P P / C C C
Crush Head   P X P / P B P       Fume Port     P G P / P T P
Shock Plate  C C C / P P P       Radiation     P P P / P B P / P P P
P wrought iron plate, S stick, C copper plate, X shaft, B bearing (iron block for Radiation), G glass pane, T tube
```

## The shield

Mobs the methods do not kill crowd the room. Past half full they wear down the shield, which is the total hardness of the walls: a steel wall holds longer than stone. Mend it by right-clicking the controller with building blocks; harder blocks mend more. At nothing left, the mobs spill into the room as real creatures, a limited number of them.

## Drops and XP

Drops come from each mob's loot table as when nobody kills it, so only drops that need no player appear. A hopper, belt or chest beside the controller takes them. A full output stops the killing. Kills give no XP of their own. A Catalyst Tap (see [Experience](Experience.md)) in reach takes part of the XP of zombies, skeletons and their kin, less than a player's kill would give.

## See also

- [Hazards](Hazards.md), [Radiation](Radiation.md), [Hammer](Hammer.md)
