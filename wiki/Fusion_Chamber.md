# Fusion Chamber

A held plasma loop that burns hydrogen into helium and gives off heat to the block it faces. It needs a heat source hotter than its ignition point and a powered magnet field.

## Crafting

```
T W T
W   W
T W T
```

T is a Tungsten Carbide Plate and W is Copper Wire. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 4 machine bolts ([Fasteners](Fasteners.md)) and Nuclear Treatise I: Reactors and Fuel ([Treatise](Treatise.md)).

## Using it

The chamber faces the block it was placed against, like an [Arc Head](Arc_Furnace.md#the-arc-head), and gives its heat to that block.

- **Fuel in.** Pipe in any hydrogen gas ([Chemicals](Chemicals.md#hydrogen), [Pipe](Pipe.md)). The fuel tank holds 4000 mB.
- **Helium out.** Helium collects in a second 4000 mB tank and leaves through the same fluid port.
- **Field.** Run an [EHV](Electricity.md#bands-and-amps) line to it. The magnet field draws **12 A**. A [Breaker](Breaker.md) in the line switches it.
- **Ignition.** A heat source hotter than **3700 Tu** reaching the chamber lights it. A [Star Burner](Star_Burner.md) set beside it is enough.

## How it works

Once lit, the plasma climbs to a very high temperature on its own and the chamber gives heat to the block it faces, up to **4500 Tu** ([Heat](Heat.md)). It burns **2 mB** of hydrogen every tick and makes **0.25 mB** of helium for each mB burned. It leaves no solid waste.

When the fuel runs out the plasma goes out and the chamber cools.

## Misuse and hazards

The field must hold the plasma. If the field falls below **90 percent** of full while the chamber is lit, the wall wears every tick; a starved or broken line, a long cable run or a tripped breaker all do it. At zero condition the chamber explodes with great force. Read the condition in Jade, where it shows as *Wearing*. Running out of fuel only cools it and does not wear the wall. See [Hazards](Hazards.md).
