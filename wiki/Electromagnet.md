# Electromagnet

The first machine that runs on [electricity](Electricity.md). While it's powered, it pulls nearby dropped items toward itself, but only items made of a magnetic metal.

## Building it

Draw it at the [Blueprint Table](Blueprint_Table.md) (pick *Electromagnet* in the type row); the turns are set there, along with the core shape and addon. It needs no treatise. Shape a **Magnet Core** from a metal ingot at the [Smithing Table](Smithing_Table.md), then assemble it at the [Engineering Table](Engineering_Table.md) with two [fasteners](Fasteners.md); the Electromagnet Blueprint in the blueprint slot judges the fit against your drawing. The magnet comes out with the design of the core, and the blueprint is kept.

The core has one measure, the **turns**, from 50 to 150 percent (100 by default). More turns pull harder and draw more current. Its **shape** trades pull against reach:

| Core | Pull | Reach |
| --- | --- | --- |
| Bar | standard | standard |
| Horseshoe | 1.4 times | 0.7 times |
| Pot | 1.8 times | 0.5 times |

**Addon.** A **yoke** (plate) is an optional piece that closes the magnetic path, adding 20% to the pull.

## What it pulls

Anything on the ground made of **iron, steel, cast iron, hardened steel or netherite**, in any form. That means ingots, nuggets, plates, rods, scrap, swarf, dust, raw iron. (Assembled [tools and armor](Tools_and_Weapons.md) are not pulled yet.)

It ignores everything else: copper, gold, zinc, tin, brass, bronze, lead, stone, wood and so on. A magnet over a mixed pile will drag the iron and steel out and leave the rest behind. That's what it's for: **sorting by what something is made of**, not by which item it is.

It only moves items that are lying on the ground. It doesn't pick anything up into an inventory, and it doesn't touch mobs or players. Put a hopper or a chest under where the items end up.

## Separating ore

Crushed ore or dust of a **magnetic** ore (such as magnetite) lying within about a block and a half of a powered magnet sheds its host rock. Each unit the magnet can tell from its rock splits into a **Concentrate**, which clings to the magnet, and a **Tailings**, which falls away and is never pulled; a unit it cannot tell stays to be pulled again. It works on a handful of units at a time. See [Ore Processing](Ore_Processing.md#separating).

## Power

Connect it to a [Galvanic Cell](Galvanic_Cell.md) or an [Electrostatic Generator](Electrostatic_Generator.md) with [Copper Cable](Copper_Cable.md), or just place it touching one.

How hard and how far it pulls depends on how much power actually reaches it:

A standard core (100 percent turns) is rated for the **LV** band at **1 A**; the rating's current follows the turns. Its strength is the share of an LV packet it receives **times** the share of that current it gets, and at full strength it reaches **4 blocks**.

- **On a single Galvanic Cell** (LV, 1 A) it pulls at full strength, with a short run of cable.
- **On an Electrostatic Generator pulse** (HV) it pulls at full strength too, but the pulse is a band over its rating and wears it (see [Wear](#wear)).
- **At the end of a long cable run**, which shrinks the packet, or **sharing a source with more magnets than the source can feed**, it gets weaker.

It never refuses to run. A badly powered magnet is just a weak magnet, and running it underpowered does it no harm at all. If one seems sluggish, look at it with Jade: *"Starved"* means it isn't getting enough current. A [Galvanometer](Instruments.md#galvanometer) shows exactly how much it's getting compared with how much it wants (1 A).

A magnet doesn't pass power through to whatever is on its other side. Give each magnet its own cable.

## Wear

Running a magnet normally never wears it out, however long it runs. The only thing that hurts a magnet is feeding it **a band above LV**. That heats it up and permanently weakens it, faster the further over (each band over is four times the packet), down to half strength. It never breaks completely. A fine-wound [Dynamotor](Dynamotor.md) at a steam engine's speed makes MV, so keep magnets on LV.

You can check a magnet's condition with [Calipers](Instruments.md#calipers), but only while it's switched off. You can't measure a magnet while it's pulling.

## Tips

- Put the magnet a block or two above a hopper line, and dropped iron will gather over the hopper.
- A magnet doesn't care whether something is hot, so it'll happily pull a glowing steel ingot too.

## Attunements

**Lode** on the magnet core reaches further, **Scatter** throws the magnetic items it would draw; **Ward** and **Mend** handle its overload wear, and the yoke takes **Ground**. See the [Enchanting Table](Enchanting_Table.md#on-a-machine) page.
