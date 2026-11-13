# Experience

Experience is a stored, transferable quantity with two numbers: the **level** it stands at and the **points** held at that level. In your own body the two are locked to the vanilla bar. In a container they are separate: a small container can hold few points at a high level, a big one many points at a low level.

## Containers

An **XP Container** is a vial of crystal, mineral or paper, made at the Stonecutter (crystal, mineral) or the Woodworking Table (paper). The **volume** axis (1 to 9) is the size: one, three and nine are a vial, a flask and a carboy, holding that many times the points of a vial. The **material** sets the top level and how many points a vial holds; the size sets only how many vials' worth. Paper tops out at a low level, quartz and amethyst in the middle, lapis a little higher, sculk higher still (a vein makes a vial), echo shard highest and roomiest. The bench offers a vial body only in materials that can hold XP; diamond, for one, is not offered.

Use a container (right-click) with points in it to pour all of them into yourself and empty it; your levels follow the vanilla curve.

Sneak-use a container in the air to move up to 20 points between it and you: the lower level gives to the higher. A [Reservoir](Enchanting_Table.md) attunement on a tool is a container too.

Hold a container and its tooltip says how high its level stands (low, moderate, high) and how full it is (empty, part full, nearly full, full); hold Shift for what the words mean. An [XP Meter](Instruments.md#xp-meter) in your inventory turns the words into figures.

## Crystal Holder

A block with one slot. Use a container on it to seat it, an empty hand to take it out. The holder puts the seated container's XP on the network; the XP stays in the container, so taking it out takes the XP with it.

```
P   P
P A P
P P P
```
P is a wrought iron plate, A is an amethyst shard.

## XP wire

Wire made of a material that conducts XP (paper, quartz, amethyst, lapis, sculk, echo shard). Holders and wires that touch form one network. Containers on it settle toward one shared level and the total points stay the same. How fast depends on the weakest wire: it moves its conductance in points a tick for every level of difference. A poor wire leaks part of what it carries as XP orbs at the wire; echo shard leaks none, sculk a little, paper the most. Two holders touching without wire settle at once.

Craft three XP Wire by placing three items of one material in a row on a crafting grid: any form (sheet, gem, vein and so on) of paper, quartz, amethyst, lapis, sculk or echo shard gives wire of that material. The wire is tinted the color of its material.

The Creative XP Source (operator tab) on a network holds the whole run at the level it is set to.

## Taps

Four blocks put the world's experience into the run they sit on. Each joins the network by touching wire or a holder, and holds nothing itself. What the run cannot hold is not taken: it drops or stays as orbs as usual.

| Block | Takes | Reach |
|---|---|---|
| **Orb Absorber** | loose XP orbs, once a second | 4 blocks |
| **Ore Tap** | the XP of ore (or any block that pays it) broken near it, by hand or by machine | 8 blocks |
| **Trade Tap** | the XP a villager's trade pays, and the XP a [Counter](Counter.md) transaction pays (3 points per point of villager experience earned; with no tap in reach a Counter pays no XP at all) | 8 blocks |
| **Smelter Tap** | the XP a furnace or other vessel beside it earned from smelting, smoking and blasting recipes, 20 points a second | touching the vessel |

A vessel with no Smelter Tap keeps what it earned, up to a cap, and spills it as orbs when it is broken. Authored processes (steel, ceramics, alloys) earn none; only vanilla and modded cooking recipes do.

```
P W P      Ore Tap          P E P      Trade Tap
W L W      L lapis block    W E W      E emerald
P W P      W copper wire    P E P      P wrought iron plate
```

```
P A P      Orb Absorber     P A P      Smelter Tap
A S A      S sculk shrieker A H A      H hopper
P A P                       P P P
```
A is an amethyst shard.

## Disenchanting Stone

Right-click it with an attuned part to grind one attunement off. Half of what an enchanting session costs comes back as XP, into a run touching the stone, or as orbs if none has room. The part keeps any flaws the attunement left. The stone takes one attunement at a time, in alphabetical order of the effect.

```
A P A
P G P
A A A
```
G is a grindstone, A an amethyst shard, P a wrought iron plate.

## Drawing from a run

An [Enchanting Table](Enchanting_Table.md) touching a Crystal Holder, XP wire or other node of a run pays a session out of the run when the run stands at level 5 or higher and holds the points. It spends the run's points before yours. A Creative XP Source pays without spending.

Sneak-use a [Reservoir](Enchanting_Table.md) tool on any node of a run: the lower level gives to the higher, up to 20 points a use. A tool below the run's level draws from it; a tool above it gives back.

## Sculk

A [Culture Dish](Sculk_Culture.md) is a node that grows its own sculk store from XP on the run; a Catalyst Tap feeds a run from deaths nearby, and Sculk Relays join runs across distance.

## Where to see it

Jade shows the seated container in words, and whether a run is moving or settled. With an XP Meter carried it shows level, points, flow and leak.
