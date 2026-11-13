# Hand Crank

The simplest source of [rotation](Rotation.md): it turns as long as you keep turning it.

## Building it

Shape a **Crank Arm** from a rod or a stick at a bench, then put the arm in a crafting grid; the crank comes out with that arm. It needs no blueprint and no treatise.

Its **arm length** is a stepper from 4 to 8 on the drawing: a longer arm gives more Su and less speed, a shorter one the reverse. The offset arm is a shape of its own. The straight arm is the reference:

| Arm | Su | Speed |
| --- | --- | --- |
| Straight, arm length 6 | standard | standard |
| Offset | 1.3 times | 0.9 times |

**Addon.** A **flywheel** (plate) is an optional disc that triples the crank's inertia: it carries the turn through the dead spot and is slower to start.

## Using it

**Hold right-click** on it. Each click keeps it turning for a moment, and holding the button keeps it going. Let go and it stops (and the line coasts down). A shaft can attach to either end.

## How much it can drive

**12 Su** at **32 RPM** with the straight arm. A hand crank is **too weak to drive a [Mechanical Hammer](Mechanical_Hammer.md)** (80 Su), or any of the other workshop machines (30 Su and up). Hook one up and the network just strains and won't turn. Gearing doesn't help: it changes how hard each turn pushes, not how much load the crank can carry.

It's useful for small loads, like a [Controller](Controller.md) (2 Su) or a [Pump](Pump.md) (8 Su), and for learning how shafts behave. It's also the fastest source early on, so it's the way to spin a [Dynamotor](Dynamotor.md) up to a high band. For real work you want a [Water Wheel](Water_Wheel.md).
