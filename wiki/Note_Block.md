# Note Block

A note block plays a note when it gets a redstone signal or is punched. The sound comes from a **noisemaker** fitted into it.

## Crafting

```
P P P
P C P
P P P
```

P is any plank, C is a Copper Plate.

Each noisemaker is one material with a stick (shapeless):

| Noisemaker | Material |
| --- | --- |
| Harp | dirt |
| Basedrum | stone |
| Snare | sand |
| Hat | glass |
| Bass | any plank |
| Flute | clay |
| Bell | gold block |
| Guitar | any wool |
| Chime | packed ice |
| Xylophone | bone block |
| Iron Xylophone | iron block |
| Cow Bell | soul sand |
| Didgeridoo | pumpkin |
| Bit | emerald block |
| Banjo | hay bale |
| Pling | glowstone |

## What vanilla does

A note block plays the instrument that matches the block under it, and using it steps the pitch through 25 notes.

## What Feedback does

A note block with no noisemaker is silent. The noisemaker sets the instrument, and the pitch is the note, 0 to 24.

- **Use it with a noisemaker** to fit it. A noisemaker already in the block comes back out.
- **Sneak and use with an empty hand** to take the noisemaker out.
- **Use with an empty hand** to step the pitch and hear it.
- A **rising redstone signal** or a **punch** plays it. The block above it must be air.

Vanilla note blocks found in the world turn into Feedback note blocks and keep their pitch; fit a noisemaker to make them sound.
