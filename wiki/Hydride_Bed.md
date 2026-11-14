# Hydride Bed

A box of hydrogen-absorbing metal. It soaks up hydrogen from a [Pipe](Pipe.md) at low pressure, a great deal for its size, and gives it back when heated.

## Crafting

```
 S  G  S
 G  C  G
 S  G  S
```

S is a steel plate, G is iron bars, C is a ceramic.

## Using it

- **Right-click** with a palladium, magnesium or titanium ingot to load one. A bed holds up to 9 ingots, mixed as you like.
- **Sneak and right-click** with an empty hand to take the last ingot back. It is refused while the gas the bed holds would not fit in what is left.
- Breaking the bed gives the ingots back.
- Connect a pipe to fill it. Jade names its state: Nothing loaded, Dry, Soaking, Nearly full, Giving gas back or Cracked.

## How it works

Each metal has an **uptake**, in volumes of gas per volume of metal, and a **release temperature**:

| Metal | Uptake | Releases above |
| --- | --- | --- |
| Palladium | 900 | 150 Tu |
| Magnesium | 1300 | 300 Tu |
| Titanium | 400 | 450 Tu |

An ingot holds its uptake times 144 mB. A mixed bed releases at the average of its metals' release temperatures, weighted by what each can hold.

- **Filling.** A pipe fills the bed with [hydrogen](Chemicals.md#hydrogen), at most 50 mB a tick, only while the bed is cooler than its release temperature. A bed at or above it takes nothing.
- **Holding.** A cool bed keeps its gas.
- **Giving back.** Any [heat source](Heat.md) the bed touches warms it. Past the release temperature it gives gas back to a pipe, 25 mB a tick for every 100 Tu over, so a hotter bed gives faster. Let it cool and it fills again.

## Misuse and hazards

Taken more than 100 Tu past its release temperature with gas in it, the metal cracks as the gas leaves too fast. Its condition falls, and its capacity with it, to half of what it was. It never fails outright, and it cracks only while it holds gas and stays that hot. See [Hazards](Hazards.md).

Hydrogen burns: a bed full of it beside a flame is a flammable-gas risk (see [Hazards](Hazards.md)).
