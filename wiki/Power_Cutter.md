# Power Cutter

A machine that cuts stock fed to it, two cuts a second, repeating a cut recorded on a [Recipe Data Stick](Data_Stick.md).

## Crafting

```
S S S
W C W
S D S
```

S is a Steel Plate, W is [Copper Wire](Wire_Drawer.md), C is Ceramic and D is a [Basic Electronic Circuit](Basic_Electronic_Circuit.md), which the finished cutter keeps. Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row, 3 machine bolts or welding rods ([Fasteners](Fasteners.md)) and Electrical Treatise III: Circuits ([Treatise](Treatise.md)).

### Heads

The cutter does nothing without a head. The head's material sets the hardness of stock it can take; its tooltip reads "Takes stock up to hardness (n), condition (n)%".

- **Saw Head** cuts cold stock: stone, wood, ceramic, glass, crystal and bone.

```
T T T
T S T
```

T is a Tungsten Carbide Plate and S is a Steel Plate. Craft it at the [Engineering Table](Engineering_Table.md) with a [file](Hand_Tools.md#file) in the tool row. It needs Materials Treatise I: Machine-Shop Consumables ([Treatise](Treatise.md)).

- **Plasma Head** cuts conducting metal plate only, since the arc needs something to conduct.

```
  T
T C T
```

T is a Tungsten Plate and C is Ceramic. Craft it at the [Engineering Table](Engineering_Table.md) with a [file](Hand_Tools.md#file) in the tool row. It needs Materials Treatise I: Machine-Shop Consumables ([Treatise](Treatise.md)).

- **Waterjet Head** cuts cold stock as a saw head does and any metal plate as a plasma head does, with no current needed. It cuts only with grit loaded in it.

```
N
P
```

N is a Tungsten Carbide Drilled Plate and P is a Steel Pipe. Craft it at the [Engineering Table](Engineering_Table.md) with a [file](Hand_Tools.md#file) in the tool row. It needs Materials Treatise I: Machine-Shop Consumables ([Treatise](Treatise.md)).

Right-click the cutter with a gem or dust of a hard, abrasive material (abrasiveness 0.5 or more; garnet is the natural one, ruby and diamond also work) to load the Waterjet Head. Each piece loads eight cuts' worth, up to sixty-four. Each cut spends one grit. A head with no grit holds the cycle and keeps the input. The nozzle wears by how abrasive the grit is.

## Using it

- It is an **EHV** sink: about **2.6 A**. See [Electricity](Electricity.md).
- **Right-click** with a head or a data stick to seat it. **Right-click** empty-handed to take out the output, then the stick. **Sneak** and right-click to take the head.
- It has one head slot and one stick slot. A hopper feeds it and empties it: stock goes in, cut pieces come out.
- It idles with no head, a Waterjet Head with no grit, no stock or no power.
- **Right-click empty-handed** on a jammed cutter clears the jam; the next empty-handed click takes the output.

## Circuit

The [circuit](Basic_Electronic_Circuit.md) the cutter was crafted with is built in and cannot be taken out or swapped. Without one the cutter does not run. A placed cutter keeps its circuit, and so does the item it drops when broken. The [Engineering Table's Repair tab](Engineering_Table.md#repair-tab) takes the cutter apart and returns the circuit.

- **Faster.** A fast circuit makes the cutter play its cuts faster, up to about twice as fast. A slow one leaves the pace near the one given above.
- **More accurate.** A quiet circuit lowers the odds of a jam, blade wear or scrap on stock the stick does not cover, and sometimes makes the cutter hold off from guessing at such stock at all.
- **Longer links.** A circuit with gain lengthens a [Data Connector](Data_Connector.md#linking-nodes) link that ends at the cutter.
- **Heater.** A circuit with a [tube](Vacuum_Tube.md) takes its heater's current from the cutter's line on top of the cutter's own, and runs slow and noisy until warm ([Warming up](Basic_Electronic_Circuit.md#warming-up)).
- **Readable.** An Input card on a [Controller](Controller.md) can read the cutter's **Progress** (how far through the cut), **Condition** (the head's) and **Output fill**. Link the machine to the Controller with the [Data Connector](Data_Connector.md) and click the card's channel row to pick one (see [Programming](Programming.md#input-and-switch-cards)).

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

## Attunements

A cutting head takes **Ward** and **Mend** against its misuse wear, **Edge** to cut faster and **Spread** to cut slower, and **Bane**, **Kin** or **Mark** on the creatures around the saw. See the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
