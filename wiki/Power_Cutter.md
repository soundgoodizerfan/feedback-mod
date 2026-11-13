# Power Cutter

A machine that cuts stock fed to it, two cuts a second, repeating a cut recorded on a [Recipe Data Stick](Data_Stick.md).

## Crafting

```
S S S
W C W
S   S
```

S is a Steel Plate, W is Copper Fine Wire and C is Ceramic.

### Heads

The cutter does nothing without a head. The head's material sets the hardness of stock it can take; its tooltip reads "Takes stock up to hardness (n), condition (n)%".

- **Saw Head** cuts cold stock: stone, wood, ceramic, glass, crystal and bone.

```
T T T
T S T
```

T is a Tungsten Carbide Plate and S is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [file](Hand_Tools.md#file) in the tool row.

- **Plasma Head** cuts conducting metal plate only, since the arc needs something to conduct.

```
  T
T C T
```

T is a Tungsten Plate and C is Ceramic.

## Using it

- It is an **EHV** sink: about **2.6 A**. See [Electricity](Electricity.md).
- **Right-click** with a head or a data stick to seat it. **Right-click** empty-handed to take out the output, then the stick. **Sneak** and right-click to take the head.
- It has one head slot and one stick slot. A hopper feeds it and empties it: stock goes in, cut pieces come out.
- It idles with no head, no stock or no power.
- **Right-click empty-handed** on a jammed cutter clears the jam; the next empty-handed click takes the output.

## How it cuts

Each piece of stock is cut in turn, two a second.

- **Stock the stick recorded** (same kind of material and same form) gets the recorded cut.
- **Any other stock** (not covered by the stick) rolls an outcome for each piece:
  - **the seeded cut**: a cut the head could make on it, chosen so that the same stock gives the same result every time in a world, and a different one from world to world;
  - **a jam**: the cutter stops until an empty hand on the block clears it, and Jade shows *Jammed*;
  - **blade wear**: the piece is spent and the head wears;
  - **scrap**: the piece turns into scrap of its material, if the material has a scrap form; otherwise it gets the seeded cut.
- **Stock the head cannot cut at all**, or harder than the head's material allows, is voided.

A stick that covers the stock keeps the cutter clear of all but the recorded cut, so record what you mean to cut.

## Head wear

A correct cut wears nothing. A head wears from misuse only:

- stock harder than the head,
- a full output (the cut waits and wears the head each time it retries),
- a supply over the EHV rating.

A worn head cuts slower and never stops. See [Hazards](Hazards.md).
