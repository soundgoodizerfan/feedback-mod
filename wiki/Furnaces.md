# Furnaces

## What vanilla does

The Furnace, Smoker and Blast Furnace each cook one item at a time from an input slot with a fuel slot beside it. Cooking takes a fixed time per recipe, a fuel's burn time is just a count of items it can smelt, and each block only accepts its own kind of recipe: the Smoker food, the Blast Furnace ores and metal, the Furnace anything.

## What Feedback does

Feedback **replaces** the vanilla Furnace, Smoker and Blast Furnace with its own. It has no vanilla furnace recipe; there is a family of seven self-fired furnaces. They all work the same way, and what makes each one different is **how heavy it is, how well it holds heat, and how hot it can physically get**.

Like the vanilla blocks they replace, they are mined with a pickaxe and drop nothing without one.

None of them is restricted to certain items. Any of them will *try* to cook anything. Each one just turns out to be good at certain jobs because of how it heats.

## The screen

Every furnace has **nine slots**, all the same, plus a **fuel slot**, and a **heat gauge** that steps through the glow colors. There's no "input" or "output" slot. Things change where they sit, and the finished item replaces the raw one in place.

Each item in the nine slots has [its own temperature](Heat.md#warming-in-a-vessel), shown on its tooltip. A slot holding glowing stock, such as hot metal, shines with a faint bloom in the glow color of that item's heat, and the chamber flickers like fire. A slot shines once its item reaches the faint red band. Sparks lift off glowing stock, more of them the hotter the item, and food slots give off pale puffs of steam once the furnace is above 100 Tu. With reduce motion on, the sparks and steam do not appear.

The Kiln, Annealing Furnace and Improved Furnace screens each have their own look, with panels tiled from the block's own texture: terracotta for the Kiln, brooding brick for the Annealing Furnace, sturdy brick with brass fittings for the Improved Furnace. The Smoker, Crude Blast Furnace and Puddling Furnace panels are lit by firelight along the bottom edge, brighter and flickering as the vessel heats.

Anything you take out (by hand or by hopper) comes out **at the temperature it has reached** and starts cooling.

Fuel works like in a [Firebox](Firebox.md): each fuel has its own flame temperature. A [Powered Bellows](Powered_Bellows.md) touching a furnace blows straight into its fire.

## Vanilla recipes

Every vanilla (and modded) smelting, smoking and blasting recipe still works, in every furnace, except ore into metal: iron ore, raw iron and vanilla's [iron ore](Vanilla_Changes.md#iron-ore) do not smelt to an ingot in any vessel ([Guide: Making Wrought Iron](Guide_Making_Wrought_Iron.md)). **How fast** depends on how hot the furnace is:

- **Metal and ore** recipes don't progress at all below **800 Tu**, and go fastest around 1000 (blasting recipes around 1400). A furnace burning only logs (760 Tu) **can't smelt ore**; coal or charcoal can.
- A smelting, blasting or other cooking recipe (not smoking) whose result is a form of a metal with a molten fluid pours that molten metal instead of an item, at the metal's melting point: 2 ingots give 288 mB.
- Each smelting, smoking or blasting recipe also earns experience, as in vanilla, but it is banked in the furnace rather than paid out. A [Smelter Tap](Experience.md#taps) beside the furnace moves it to an XP run; a broken furnace spills what it holds as orbs.
- **Food** cooks fastest around **250 Tu**. **Food in a furnace above 400 Tu burns away to nothing.** An unwatched furnace running on coal will do this.
- Every stack has its own timer and they all run in parallel, so a stack of eight beef takes the same time as one.

## Charcoal

Vanilla smelted a log into charcoal in any furnace. Here charcoal is wood heated **without air**, and a furnace with a fire blowing on the logs just cannot do it.

Only the **Kiln** and the **Annealing Furnace** are air-tight enough. Put logs in one, keep it between **300 and 500 Tu** (fastest near 400), and each log slowly becomes one charcoal (about a minute at the best temperature). Above **700 Tu** the logs are ruined into [Charred Remains](Heat.md#failed-batches). Blowing a [Powered Bellows](Powered_Bellows.md) into the furnace lets air in and stalls the batch. In any other furnace, or a [Crucible](Crucible.md), logs just sit there.

Before you have bricks, bury a [Campfire](Campfire.md): cover it on every side, put logs on it, and the smouldering fire (420 Tu) chars them the same way.

The Annealing Furnace tops out at 380 Tu, so it can't overshoot; the Kiln needs watching. JEI shows the condition as *Atmosphere: Sealed: no air*.

## The seven furnaces

| Furnace | Recipe | Character |
| --- | --- | --- |
| **Furnace** | Ring of Cobblestone | Light and quick. General purpose. Gets hot enough to burn food if you leave it. |
| **Smoker** | Furnace with four Logs around it | **Physically can't get above 170 Tu**, so it can't burn food. Great for food, useless for metal. |
| **Crude Blast Furnace** | Furnace, Wrought Iron Plates, Copper Tubing and Bricks (Engineering Table) | Very heavy and very well sealed. Slow to heat, but gets hot and stays steady. Good for ore. |
| **Improved Furnace** | Furnace, Steel Plates and Copper Tubing (Engineering Table) | Between the two. The only furnace with an opening, so a [Temperature Sensor](Instruments.md#temperature-sensor) can be fitted and a controller can watch it. |
| **Kiln** | Bricks: a row of three over two columns (`###` / `#_#` / `#_#`) | For ceramics and sintered carbides. Tops out at **1330 Tu**: enough for **Firebrick** (1200–1500), never enough for steel (1350). |
| **Annealing Furnace** | [Firebox](Firebox.md), Steel Plate and [Insulation](Insulation.md) (Engineering Table) | Tops out at **380 Tu**, inside tempering's 300–400, so it **can't overheat** a tempering batch. Heavy, so it cools slowly. |
| **[Puddling Furnace](Puddling_Furnace.md)** | Bricks, a Furnace, a Shaft, a Wrought Iron Rod and Plate (Engineering Table) | A shaft into its back stirs the charge. Turns cast iron into a clean iron bloom. |

Recipes shown as grids:

```
Furnace          Smoker           Crude Blast      Improved         Kiln             Annealing
# # #              L              P P P            S S S            B B B            N N N
#   #            L F L            T F P            S F S            B   B            N S N
# # #              L              B B B            S T S            B   B            N X N
```

`#` Cobblestone, `L` Log, `F` Furnace, `P` Wrought Iron Plate (Crude Blast Furnace), `T` Copper Tubing, `S` Steel Plate (Improved Furnace and Annealing Furnace), `B` Brick, `N` Insulation, `X` Firebox. The Crude Blast Furnace, Improved Furnace and Annealing Furnace grids are laid out at the [Engineering Table](Engineering_Table.md); the Kiln, Furnace and Smoker are crafted in the plain Crafting Table.

The three Engineering Table furnaces are built with a [hammer](Hammer.md) in the tool row and rivets ([Fasteners](Fasteners.md)): 3 for the Crude Blast Furnace and the Improved Furnace, 2 for the Annealing Furnace. Each needs Materials Treatise II: Metallurgy and Heat Treatment ([Treatise](Treatise.md)).

## Sealed vs. open

All of them except the Improved Furnace are **sealed**. Jade can't tell you how hot they are (*"Sealed - cannot be measured"*), sensors won't fit, and a [Bimetallic Strip](Bimetallic_Strip.md) can't feel them. You can still right-click one with a [Thermometer](Instruments.md#thermometer) for a one-off reading.

Sealed furnaces are excellent vessels, but you can't automate them against temperature. For that you need a [Crucible](Crucible.md) or the Improved Furnace.

## Sintering

Cermet and tungsten carbide are pressed into a green compact first, then baked in a **Kiln**. It is the only furnace that is both sealed and hot enough. The Kiln's fluid port takes **nitrogen** (from air separation, see [Chemicals](Chemicals.md#nitrogen)): at least 50 mB for each compact, and only a sealed vessel accepts it.

- **Cermet compact**: hold the Kiln between **1100 and 1290 Tu**, best at 1200.
- **Tungsten carbide compact**: hold it between **1150 and 1300 Tu**, best at 1250.

When the bake finishes, the compact becomes an ingot and the nitrogen is spent. If there is no nitrogen, or too little, at that moment, the batch leaves **Porous Sinter** instead. So does heating past 1315 Tu for cermet or 1320 Tu for tungsten carbide. Cooler than the window, the batch simply stalls.

## Feedback's own processes

Besides vanilla recipes, the furnaces run the same processes a crucible does (steel, ceramics, firebrick, tempering), with the same bands, holds and spoil lines shown in JEI. See [Heat](Heat.md#processes).

## Melting, boiling and cooking

Furnaces and both [Crucibles](Crucible.md) are one family of vessels, and each does everything the others do:

- **Melting.** Metal melts into one shared tank, and two metals in the right proportions alloy there: copper and tin make bronze in a furnace too ([Casting and Alloys](Casting.md#alloys)). A melt will not pour into a tank holding crude oil or a gas.
- **Boiling.** Water, or any liquid that boils, boils in the tank once it reaches its boiling point. The vapor goes to the furnace's vapor tank and on to a neighboring [Condenser](Condenser.md). Crude oil distilled by hand in a furnace leaves the crude heavier, as the light cuts leave.
- **Cooking.** Food goes through raw, done, dried, charred and spoiled; see [Food](Food.md). Furnace-type vessels stack a food that finishes into the same food already held when their states are close; see [Crucible](Crucible.md#cooking).

## Kiln Zone

A tunnel kiln: ware goes in at the back, rides a car through the heat and comes out the front.

```
B B B
P X P      B bricks, P wrought iron plate, X shaft
B B B
```

Point a [Firebox](Firebox.md) or other heat source at it and put a vertical shaft into the top or bottom. A hopper or belt feeds the back; a hopper, belt or chest at the front takes what comes out. The car has four positions: a cool start, two hot, and a cool finish. The zone runs every item the way a furnace would, by the heat in its position: food goes up its cooking ladder, and anything else runs the one-ingredient process it matches, with the same holds and the same ruin. It does not melt metal. A [Stoker](Firebox.md#stoker) keeps a firebox fed.

The shaft sets how long ware stays. A slow shaft gives a clean result and few items; a fast one leaves ware raw. A stopped car is the overrun: ware sits in the fire and goes past done to char, crack or spoil. The same line through a hot zone cooks ore-bearing stock and through a cool one dries food; nothing in the kiln says which.

It starts and stops from a [Controller](Controller.md) or redstone. Stop the car and the stoker together.
