# Fuse Holder

Holds one thin wire in an [electrical](Electricity.md) line. If too much current flows, the wire melts and the line opens. The wire is used up; the holder isn't.

## Crafting

```
C C C
W   W
C C C
```

C is Ceramic and W is [Copper Wire](Wire_Drawer.md). The holder is placed **empty**.

## Fitting a wire

**Right-click the holder with a Fine Wire** of any metal: *"Fuse wire fitted."* An empty or blown holder conducts nothing. Fine wire comes from the [Wire Drawer](Wire_Drawer.md).

## The wire's metal is the fuse's rating

Two figures, both from the metal:

- **Rating:** the current it carries without warming at all.
- **Melting point:** how much overload it stands before it goes.

Over its rating the wire heats up, and a thin wire heats fast. It settles at a temperature that depends on how far over it is. If that's above its melting point, it blows, usually within a second or two.

| Wire | Carries forever | Blows above about |
| --- | --- | --- |
| Lead | 1.3 A | 1.9 A |
| Steel | 1.6 A | 4.5 A |
| Tin | 2.4 A | 2.8 A |
| Iron | 2.7 A | 5.9 A |
| Zinc | 4.5 A | 5.3 A |
| Brass | 4.5 A | 6.2 A |
| Gold | 11.2 A | 13.0 A |
| Copper | 16 A | 18 A |

The second column is for a room at 20 Tu. In the Nether, where the room is hotter, each fuse blows a little sooner.

Low-melting metals (tin, lead, zinc) blow sharply, just past their rating. High-melting ones carry a long way over theirs first.

## Picking one

A fuse should blow **before** the thing it protects is harmed. A [Copper Cable](Copper_Cable.md) carries 4 A, so:

- **Tin** (blows near 2.8 A) or **zinc** (5.3 A, a little over) protects copper cable.
- **Copper** wire carries 16 A and won't protect copper cable at all.

## Notes

- Blowing is the fuse doing its job. It's one of the few things in Feedback that's *meant* to be used up.
- The holder itself never wears out.
- Want something you can reset? See the [Breaker](Breaker.md).
