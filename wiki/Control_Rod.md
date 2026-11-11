# Control Rod Assembly

A bundle of absorber that slides between five positions in the lattice of a [Fission Reactor](Fission_Reactor.md), soaking up neutrons to slow the chain. Fully in, it shuts the reaction down; fully out, it lets the reaction run.

## Crafting

```
P P P
L R L
P P P
```

P is a Steel Plate, L is a Lead Plate and R is a Steel Rod. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 [fasteners](Fasteners.md).

## Using it

The assembly is empty until you load it. It does nothing, in or out, without absorber.

- **Right-click with absorber in hand** to load it: a cobalt, tungsten or lead item. It takes up to **8** items of the **same kind**.
- **Right-click with an empty hand** to step its **target** one place: 0 (out) to 4 (in), then back to 0. A line shows the rods' current position, out of 4, and the target. The assembly moves toward the target one position about each second.
- **Sneak and right-click with an empty hand** to take the absorber back out. Breaking the block drops it.
- A [Controller](Controller.md) switch sets the target: on is all the way in, off is all the way out.
- On a [SCRAM](Scram_Trip.md) the core drops every assembly all the way in at once.

## How it works

How much an assembly soaks up depends on what it holds and how far in it is:

| Absorber | Absorption |
| --- | --- |
| Boron | 1.1 |
| Cobalt | 0.5 |
| Tungsten | 0.4 |
| Lead | 0.15 |

The figure is for a full bundle of 8 items; a part-full bundle absorbs in proportion. The assembly's reading in Jade shows its absorber strength as a percentage. The deeper the position, the more it takes off the core's multiplication ([Fission Reactor](Fission_Reactor.md#how-it-works)). Fuel cells right beside the assembly feel it far more than the rest of the lattice, so spread assemblies among the fuel.

Any item whose material absorbs neutrons works ([Materials](Materials.md#neutrons)).

A [Controller](Controller.md) actuator card aimed at an assembly offers three channels: the plain switch (all the way in or out), **step in** and **step out**. A step channel moves the rod one position on the change of its input to true, and does nothing while the input holds, so a program walks a rod by pulsing it.
