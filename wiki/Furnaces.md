# Furnaces

## What vanilla did

The Furnace, Smoker and Blast Furnace each cook one item at a time from an input slot with a fuel slot beside it. Cooking takes a fixed time per recipe, a fuel's burn time is just a count of items it can smelt, and each block only accepts its own kind of recipe: the Smoker food, the Blast Furnace ores and metal, the Furnace anything.

## What Feedback does

Feedback **replaces** the vanilla Furnace, Smoker and Blast Furnace with its own. You can't craft the vanilla ones any more. Instead there's a family of six self-fired furnaces. They all work the same way, and what makes each one different is **how heavy it is, how well it holds heat, and how hot it can physically get**.

Like the vanilla blocks they replace, they are mined with a pickaxe and drop nothing without one.

None of them is restricted to certain items. Any of them will *try* to cook anything. Each one just turns out to be good at certain jobs because of how it heats.

## The screen

Every furnace has **nine slots**, all the same, plus a **fuel slot**, and a **heat gauge** that steps through the glow colours. There's no "input" or "output" slot. Things change where they sit, and the finished item replaces the raw one in place.

Anything you take out (by hand or by hopper) comes out **at the furnace's temperature** and starts cooling.

Fuel works like in a [Firebox](Firebox.md): each fuel has its own flame temperature. A [Powered Bellows](Powered_Bellows.md) touching a furnace blows straight into its fire.

## Vanilla recipes

Every vanilla (and modded) smelting, smoking and blasting recipe still works, in every furnace. **How fast** depends on how hot the furnace is:

- **Metal and ore** recipes don't progress at all below **800 Tu**, and go fastest around 1000 (blasting recipes around 1400). A furnace burning only logs (760 Tu) **can't smelt ore**; coal or charcoal can.
- **Food** cooks fastest around **250 Tu**. **Food in a furnace above 400 Tu burns away to nothing.** An unwatched furnace running on coal will do this.
- A whole stack cooks together: eight beef take eight times as long as one.

## Charcoal

Vanilla smelted a log into charcoal in any furnace. Here that recipe is gone; charcoal is wood heated **without air**, and a furnace with a fire blowing on the logs just can't do it.

Only the **Kiln** and the **Annealing Furnace** are air-tight enough. Put logs in one, keep it between **300 and 500 Tu** (fastest near 400), and each log slowly becomes one charcoal (about a minute at the best temperature). Above **700 Tu** the logs burn away to nothing. Blowing a [Powered Bellows](Powered_Bellows.md) into the furnace lets air in and stalls the batch. In any other furnace, or a [Crucible](Crucible.md), logs just sit there.

Before you have bricks, bury a [Campfire](Campfire.md): cover it on every side, put logs on it, and the smouldering fire (420 Tu) chars them the same way.

The Annealing Furnace tops out at 380 Tu, so it can't overshoot; the Kiln needs watching. JEI shows the condition as *Atmosphere: Sealed: no air*.

## The six furnaces

| Furnace | Recipe | Character |
| --- | --- | --- |
| **Furnace** | Ring of Cobblestone | Light and quick. General purpose. Gets hot enough to burn food if you leave it. |
| **Smoker** | Furnace with four Logs around it | **Physically can't get above 380 Tu**, so it can't burn food. Great for food, useless for metal. |
| **Crude Blast Furnace** | Furnace, Wrought Iron Ingots around the top, Smooth Stone below | Very heavy and very well sealed. Slow to heat, but gets hot and stays steady. Good for ore. |
| **Improved Furnace** | Furnace surrounded by Steel Ingots | Between the two. The only furnace with an opening, so a [Temperature Sensor](Temperature_Sensor.md) can be fitted and a controller can watch it. |
| **Kiln** | Ring of Brick | For ceramics. Tops out at **1330 Tu**: enough for **Firebrick** (1200–1500), never enough for steel (1350). |
| **Annealing Furnace** | Steel Ingot surrounded by [Insulation](Insulation.md) | Tops out at **380 Tu**, inside tempering's 300–400, so it **can't overheat** a tempering batch. Heavy, so it cools slowly. |

Recipes shown as grids:

```
Furnace          Smoker           Crude Blast      Improved         Kiln             Annealing
# # #              L              I I I            S S S            B B B            N N N
#   #            L F L            I F I            S F S            B   B            N S N
# # #              L              s s s            S S S            B B B            N N N
```

`#` Cobblestone, `L` Log, `F` Furnace, `I` Wrought Iron Ingot, `s` Smooth Stone, `S` Steel Ingot, `B` Brick, `N` Insulation.

## Sealed vs. open

All of them except the Improved Furnace are **sealed**. Jade can't tell you how hot they are (*"Sealed - cannot be measured"*), sensors won't fit, and a [Bimetallic Strip](Bimetallic_Strip.md) can't feel them. You can still right-click one with a [Thermometer](Thermometer.md) for a one-off reading.

Sealed furnaces are excellent vessels, but you can't automate them against temperature. For that you need a [Crucible](Crucible.md) or the Improved Furnace.

## Feedback's own processes

Besides vanilla recipes, the furnaces run the same processes a crucible does (steel, ceramics, firebrick, tempering), with the same bands, holds and spoil lines shown in JEI. See [Heat](Heat.md#processes).

They can melt metal too, but a furnace can only hold **one** molten metal at a time. Alloys need a [Crucible](Crucible.md).
