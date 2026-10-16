# Electromagnet

The first machine that runs on [electricity](Electricity.md). While it's powered, it pulls nearby dropped items toward itself, but only items made of a magnetic metal.

## Crafting

Eight [Copper Wire](Wire_Drawer.md) wrapped around an Iron Ingot.

```
W W W
W I W
W W W
```

## What it pulls

Anything on the ground made of **iron, steel, cast iron, hardened steel or netherite**, in any form. That means ingots, nuggets, plates, rods, scrap, swarf, raw iron. (Assembled [tools and armour](Tools_and_Weapons.md) are not pulled yet.)

It ignores everything else: copper, gold, zinc, tin, brass, bronze, lead, stone, wood and so on. A magnet over a mixed pile will drag the iron and steel out and leave the rest behind. That's what it's for: **sorting by what something is made of**, not by which item it is.

It only moves items that are lying on the ground. It doesn't pick anything up into an inventory, and it doesn't touch mobs or players. Put a hopper or a chest under where the items end up.

## Power

Connect it to a [Galvanic Cell](Galvanic_Cell.md) or an [Electrostatic Generator](Electrostatic_Generator.md) with [Copper Cable](Copper_Cable.md), or just place it touching one.

How hard and how far it pulls depends on how much power actually reaches it:

It's rated for **1.5 V at 1 A**. Its strength is the share of that voltage it sees **times** the share of that current it gets, and at full strength it reaches **4 blocks**.

- **On an Electrostatic Generator pulse** (1.5 V) it pulls at full strength.
- **On a single Galvanic Cell** (1.1 V) it works at about three-quarters strength, with a shorter reach.
- **At the end of a long cable run**, or **sharing a source with other magnets**, it gets weaker still.

It never refuses to run. A badly powered magnet is just a weak magnet, and running it underpowered does it no harm at all. If one seems sluggish, look at it with Jade: *"Starved"* means it isn't getting enough current. A [Galvanometer](Galvanometer.md) shows exactly how much it's getting compared with how much it wants (1 A).

A magnet doesn't pass power through to whatever is on its other side. Give each magnet its own cable.

## Wear

Running a magnet normally never wears it out, however long it runs. The only thing that hurts a magnet is feeding it **more voltage than it's built for** (1.5 V). That heats it up and permanently weakens it, faster the further over, down to half strength. It never breaks completely. A fine-wound [Dynamo](Dynamo.md) at a steam engine's speed makes 6 V, four times the magnet's rating, so keep magnets on LV.

You can check a magnet's condition with [Calipers](Calipers.md), but only while it's switched off. You can't measure a magnet while it's pulling.

## Tips

- Put the magnet a block or two above a hopper line, and dropped iron will gather over the hopper.
- A magnet doesn't care whether something is hot, so it'll happily pull a glowing steel ingot too.
