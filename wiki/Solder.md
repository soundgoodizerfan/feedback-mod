# Solder

Plain **tin** is a solder: a Tin Ingot pastes a board as it is, no alloying needed, which makes it the solder for a first circuit. Two better ones are made by melting tin with lead or with a little copper in a [crucible](Crucible.md) or [mixer](Casting.md) and casting ingots like any metal.

| Solder | Mix | Melts at | Good window | Hazard |
| --- | --- | --- | --- | --- |
| Tin | pure tin | 232 Tu | 20 Tu above | none |
| Leaded Solder | tin and lead | 183 Tu | 60 Tu above | poisons while hot |
| Lead-Free Solder | tin and a little copper | 227 Tu | 30 Tu above | none |

The **window** is how far above its melting point a joint is still good. A thinner melt wets better, so lead solder forgives more; lead-free is cleaner but wants a steadier hand or a better oven. Pure tin skins over with oxide and gives the narrowest window of the three: it works, but the iron has to be held close to the melting point. A printed board's traces lift at 260 Tu, so on one the tin window leaves little room above it.

## What makes a good joint

- Reach the melting point and stay on the pad long enough (hand iron: drag at a steady pace; each pad is judged on its own, and a pad you sweep over stays cold).
- Stay inside the window. Above it the flux burns and the joint goes **cold**.
- Stay under what the traces and substrate stand (a printed trace lifts first). Past that the traces **lift**.

Working leaded solder while it is hot poisons whoever is within 3 blocks. Poison never kills.

Any other mod's alloy works if its material file marks it as a solder.
