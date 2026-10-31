# Sculk Culture

A warden leaves a **Culture Seed**: the starter for a living sculk culture you keep in a dish. The culture is wetware, fed **XP** and **air**. Fed, it grows and holds a small, high-level store of XP that grows with it. Neglected, it goes quiet, then wastes, and a culture left ripe for long enough calls a warden.

## The warden's drops

A warden drops one **Culture Seed** and one **Relay Organ**, besides the sculk catalyst vanilla gives.

## Culture Dish

Made from glass, a sculk block and wrought iron plates. Use a Culture Seed on an empty dish to start a culture. A dish sits on the [XP network](Experience.md): wire, holders and other nodes that touch it join its run, and the culture eats XP out of its own store, which the run keeps filling.

- **Upkeep.** A living culture eats a little XP all the time, more the bigger it is.
- **Growth.** Below full mass it eats more, as fast as the run can supply, up to about ten minutes for a whole culture. Its store grows with it: a seed's store is a sliver, a full culture's is a sculk container's worth.
- **Air.** The dish breathes air from any neighbouring block that holds it (a [Gas Tank](Gas_Tank.md)). It draws a trickle a second; without it the culture goes pale. The dish's look shows empty, growing, ripe, or ailing (any starved, pale, clotted or shrieking stage).
- **Ripe.** A full culture is ripe. Use a hoe on a ripe dish to cut it: two sculk blocks and two sculk veins, and the culture falls back to a third of its size. A well-fed culture cut before it clots may also bud a spare Culture Seed. XP held above the smaller store spills out as orbs.

## Neglect

| What is missing | What happens |
|---|---|
| XP | After ten minutes growth stops (dormant). After thirty the culture starts to waste. |
| Air | Pale at once. After a minute the culture starts to die; from full it is gone in about eight minutes. Air returning stops the loss. |
| Harvest | Left ripe for ten minutes, the culture clots and bleeds its store away as orbs. Left twenty minutes more, it buds a shrieker beside the dish, set to summon a warden. |

An atrophied or suffocated culture that dies leaves an empty dish. The dish pulses as it changes stage: faintly when it is ripe, starving or pale, loudly once it clots. Sculk sensors and wardens feel the pulse.

## Catalyst Tap

Made from a sculk catalyst. A node on the XP network that takes the XP of anything dying within 8 blocks into the run it sits on, as the catalyst takes it in vanilla, without spreading any sculk. What the run cannot hold drops as orbs as usual. Players' deaths are not taken.

## Sculk Relay

Made from a Relay Organ. Every relay within 24 blocks of another joins its run, with no wire between and no line of sight needed, in the same dimension.

## Data Sensor

Made from a sculk sensor. Listens like one (8 blocks, the same vibration frequencies) and holds the last frequency for two seconds. Link it to a [Controller](Controller.md) with a Data Connector and read it with a sensor card: channel 0 is the frequency over 15, channels 1 to 15 are on while that exact frequency was just heard. It has no wool dampening and no sneak filter.

## Where to see it

Jade names the dish's stage. With an [XP Meter](XP_Meter.md) carried it also shows mass, air and the store.
