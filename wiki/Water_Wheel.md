# Water Wheel

Free, continuous [rotation](Rotation.md) from flowing water. It's the first source that can actually run machines.

## Designing and building

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Water Wheel* in the type row); drawing it takes a Mechanical [Treatise](Treatise.md). It has one part, the **Wheel Rim**, with three choices:

| Choice | What it sets |
| --- | --- |
| **Diameter** | From 100 to 400 cm, in steps of 20; the default is 200. A bigger wheel catches more water and makes more Su, but turns slower for the same flow. |
| **Buckets** | From 4 to 16; the default is 8. More buckets take more of the flow's push and make more Su. |
| **Wood** | The planks the rim is made from. A heavier wood gives the rim more momentum. |

A 200 cm wheel with 8 buckets makes 256 Su and turns 4 RPM for each flowing side.

At the [Woodworking Table](Woodworking_Table.md), put the Water Wheel Blueprint in the blueprint slot and shape the rim from planks. The Water Wheel comes out with the design, and the blueprint is kept. Its tooltip shows the Su it makes, the RPM per flowing side and its momentum. Breaking the block gives the same wheel back.

## Using it

Place it with **flowing water** against it. Its axle runs along the face you place it on, and shafts connect at either end of the axle.

**Speed depends on how many sides have moving water** against them: the RPM per flowing side on the tooltip, times up to four sides. The two axle ends don't count. It needs to be **flowing** water: still source blocks don't push it.

Where you put it matters. A wheel set into a running stream with water on several faces is worth much more than one dipped in a trickle.

## How much it can drive

A water wheel is strong: the Su on its tooltip. A 200 cm, 8-bucket wheel can carry **three [Mechanical Hammers](Mechanical_Hammer.md)** (80 each), but not four. A bigger wheel with more buckets carries more; so do more wheels on the same line.

It's heavy, so a network with a water wheel on it is slow to spin up but steady once it's going, and it coasts a long way when disconnected. A bigger wheel of a heavier wood has more momentum.

## Notes

- Its speed wanders with the water, which is why a [Timer](Timer.md) run off a water wheel doesn't always give the same result twice.
