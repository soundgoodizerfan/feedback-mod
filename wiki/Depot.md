# Depot

One end of a trade road. A linked pair of depots swaps goods by convoy each market day.

## Crafting

```
P E P
P B P      P any plank, E emerald, B barrel
P P P
```

The **Depot Link Card** is paper + emerald.

## Using it

1. Use a link card on one depot. The card records it.
2. Use the card on a second depot to link the two. A message confirms the link. If the recorded depot is gone or too far, the card keeps what it recorded.
3. Right-click a depot to open it. The **outgoing** shelf takes goods; the **incoming** shelf is take-only. The screen says whether the depot is linked and how many parcels are on the road.

Hoppers fill the outgoing shelf and empty the incoming one.

## How it works

On each [market day](Market_Cross.md), a convoy carries the outgoing goods to the linked depot, provided a road of at least 12 tiles leaves the depot and the other end is in the same dimension. Each linked depot sends its own. Road blocks are the same as for a [Counter](Counter.md#reach).

The trip takes about 12 ticks per block of distance, and at least a minute. A donkey courier is seen leaving when a player is within 64 blocks.

### Raiders

Each stack on the road is lost to raiders with a 20% chance. Each bell within 12 blocks of either depot lowers the chance by 15%; up to 3 bells count, and the chance never falls below 3%.

Villages pay differently for the same goods (see [Regional prices](Counter.md#regional-prices)): carry what the other village wants.
