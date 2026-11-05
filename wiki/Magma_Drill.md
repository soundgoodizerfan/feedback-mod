# Magma Drill

A derrick bored down through the Nether to its magma. The top of the hole becomes a **Magma Head** that, with a fire against it and a spinning shaft, pushes **Magma Melt** into a tank or pipe. Settled in a [Reactor](Reactor.md#the-reactions), the melt gives molten iron, nickel, cobalt and slag.

## Building it

Build a derrick (base, casings and a bit) in the Nether where no [oil basin](Oil.md#soul-oil-basins) lies below it, and drive it. It bores down to **Y 28**, where the magma is, then stops. The base turns into a **Magma Head**, and the casings come back as items. The bit stays in the hole, inside the head.

Over a soul oil basin the same derrick bores to the basin instead and becomes a wellhead.

Fit the head with:

- **A shaft** on its east or west face, to spin it ([Rotation](Rotation.md)).
- **A fire against it**: a [Fluid Burner](Fluid_Burner.md), a [Heating Coil](Heating_Coil.md) or an [Arc Head](Arc_Furnace.md#the-arc-head).
- **A tank or pipe** touching it, to take the melt.

## Using it

- **Right-click with an empty hand** to read it: the fire's temperature, whether the melt is flowing, and the bit's condition as a percentage. It says when there is no bit, when the fire is too cold, when the shaft is still, and when nothing takes the melt.
- **Sneak and right-click with an empty hand** to pull the bit out and read the head.

## How it works

The fire decides whether anything comes up at all. Below **1600 Tu** nothing flows. The flow climbs with the heat and is free at **2200 Tu**. The shaft decides how fast: the rate is the shaft speed times the heat times the bit's condition. A still shaft, a cold fire or a missing bit stops it.

The melt is pushed into any tank or pipe that touches the head.

### The bit

A bit in a hot hole wears only when the fire is **hotter than the bit material's service temperature**, faster the further over. A bit of a material that stands the heat does not wear. A worn bit pumps less, so read its condition.

### Settling the melt

Magma Melt is not a metal yet. Feed it into a [Reactor](Reactor.md#the-reactions) at **1550 Tu or more** (best 1700 Tu). Each **1000 mB** gives:

| Product | Amount |
| --- | --- |
| Molten iron | 500 mB |
| Molten nickel | 60 mB |
| Molten cobalt | 20 mB |
| Molten slag | 300 mB |

Cast the metals as any other melt ([Casting and Alloys](Casting.md)). Molten slag freezes to silicate slag, and silicate slag melts back at 1300 Tu.

## Hazards

The head works against a fire of 1600 Tu or more, and the melt is hotter still. See [Hot things](Hazards.md#hot-things).
