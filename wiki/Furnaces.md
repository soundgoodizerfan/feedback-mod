# Furnaces

Feedback **replaces** the vanilla Furnace, Smoker and Blast Furnace with its own. You can't craft the vanilla ones any more. Instead there's a family of six self-fired furnaces. They all work the same way, and what makes each one different is **how heavy it is, how well it holds heat, and how hot it can physically get**.

None of them is restricted to certain items. Any of them will *try* to cook anything. Each one just turns out to be good at certain jobs because of how it heats.

## The screen

Every furnace has **nine slots**, all the same, plus a **fuel slot**, and a **heat gauge** that steps through the glow colours. There's no "input" or "output" slot. Things change where they sit, and the finished item replaces the raw one in place.

Anything you take out (by hand or by hopper) comes out **at the furnace's temperature** and starts cooling.

Fuel works like in a [Firebox](Firebox.md): each fuel has its own flame temperature. A [Powered Bellows](Powered_Bellows.md) touching a furnace blows straight into its fire.

## Vanilla recipes

Every vanilla (and modded) smelting, smoking and blasting recipe still works, in every furnace. **How fast** depends on how hot the furnace is:

- **Metal and ore** recipes don't progress at all below a fairly high temperature. A furnace burning only wood **can't smelt ore**; coal or charcoal can.
- **Food** cooks best low and steady. **Food left in a furnace that gets too hot burns away to nothing.** An unwatched furnace running on coal will do this.
- A whole stack cooks together: eight beef take eight times as long as one.

## The six furnaces

| Furnace | Recipe | Character |
| --- | --- | --- |
| **Furnace** | Ring of Cobblestone | Light and quick. General purpose. Gets hot enough to burn food if you leave it. |
| **Smoker** | Furnace with four Logs around it | **Physically can't get hot enough to burn food.** Great for food, useless for metal. |
| **Crude Blast Furnace** | Furnace, Iron Ingots around the top, Smooth Stone below | Very heavy and very well sealed. Slow to heat, but gets hot and stays steady. Good for ore. |
| **Improved Furnace** | Furnace surrounded by Steel Ingots | Between the two. The only furnace with an opening, so a [Temperature Sensor](Temperature_Sensor.md) can be fitted and a controller can watch it. |
| **Kiln** | Ring of Brick | For ceramics. Tops out just high enough for **Firebrick**, and no higher. |
| **Annealing Furnace** | Steel Ingot surrounded by [Insulation](Insulation.md) | Tops out at **tempering** temperature, so it **can't overheat** a tempering batch. Heavy, so it cools slowly. |

Recipes shown as grids:

```
Furnace          Smoker           Crude Blast      Improved         Kiln             Annealing
# # #              L              I I I            S S S            B B B            N N N
#   #            L F L            I F I            S F S            B   B            N S N
# # #              L              s s s            S S S            B B B            N N N
```

`#` Cobblestone, `L` Log, `F` Furnace, `I` Iron Ingot, `s` Smooth Stone, `S` Steel Ingot, `B` Brick, `N` Insulation.

## Sealed vs. open

All of them except the Improved Furnace are **sealed**. Jade can't tell you how hot they are (*"Sealed - cannot be measured"*), sensors won't fit, and a [Bimetallic Strip](Bimetallic_Strip.md) can't feel them. You can still right-click one with a [Thermometer](Thermometer.md) for a one-off reading.

Sealed furnaces are excellent vessels, but you can't automate them against temperature. For that you need a [Crucible](Crucible.md) or the Improved Furnace.

## Feedback's own processes

Besides vanilla recipes, the furnaces run the same processes a crucible does (steel, ceramics, firebrick, tempering), with the same bands, holds and spoil lines shown in JEI. See [Heat](Heat.md#processes).

They can melt metal too, but a furnace can only hold **one** molten metal at a time. Alloys need a [Crucible](Crucible.md).
