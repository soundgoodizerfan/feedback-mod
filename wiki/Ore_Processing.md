# Ore Processing

A path from rock to metal in stages. Each stage turns an ore into a finer form of itself and gets more metal out of it: melting the raw rock gives the least, crushing, separating and leaching give more. JEI's **Ore Processing** card shows, for every ore, which stages it has and how much of its metal each one pays.

## The forms

An ore comes in forms, named after the ore (hematite, chalcopyrite, malachite and so on):

| Form | What it is |
| --- | --- |
| Raw, Ore | The rock as mined. |
| Crushed | The rock broken into grains. Some grains are still locked in their host rock. |
| Dust | The rock ground fine. Every grain is free. |
| Concentrate | Crushed or ground ore with most of its host rock washed, sorted or drawn away. |
| Tailings | What a separation leaves behind: mostly rock, a little metal. |
| Roasted (calcine) | A sulfide concentrate with its sulfur burned off. |

Tailings keep the ore's nature: tailings of a radioactive ore are [radioactive](Radiation.md).

## How much metal each stage gives

The share of the metal in the rock that ends up as metal:

| Stage | Share |
| --- | --- |
| Melt the raw rock | 33% |
| Melt crushed ore or dust | 45% |
| Melt a concentrate | 67% |
| Melt tailings | 10% |
| Leach a concentrate (or roasted concentrate), then win the metal | 95% |
| Leach tailings | 35% |

The full route is about 2.85 times what melting the raw rock gives. The [Electrolytic Cell](Electrolytic_Cell.md#refining) pays the same ladder when an ore is hung as its anode.

Melting raw rock or crushed ore also leaves that ore's own **Tailings** beside the metal: the metal the melt missed, which you can leach or melt again. An ore with no tailings form leaves a lump of **Silicate Slag** instead. Silicate slag holds a little iron: crush it and melt the grains for a little pig iron. Slag itself melts to molten slag.

Native copper, gold, tin, zinc and lead ores take the same ladder: they are separated from their rock rather than ground.

## Crushing and grinding

1. **Crush** raw rock to Crushed with the [Mechanical Hammer](Mechanical_Hammer.md#crushing-ore) or by hand on an [Anvil](Anvil.md).
2. **Grind** crushed ore to Dust in the [Rolling Mill](Rolling_Mill.md#grinding-to-dust). Only brittle ores grind; a native metal is separated from its rock instead.

## Separating

A separation sorts crushed ore or dust a unit at a time. A unit either reports to the concentrate (you get one Concentrate and one Tailings) or stays where it was to go round again, so a weak separator costs passes and loses nothing. How often a unit reports depends on how different the ore is from its host rock in the property the separator reads, and crushed grains report less often than dust because some are still locked in rock. JEI's **Separation** card lists, per ore, the host rock, which separator suits it and its share a pass for crushed ore and for dust.

| Separator | Reads | Setup |
| --- | --- | --- |
| [Mixer](Mixer.md#washing-ore) | Density | Water in the drum, shaft turning, ore in the input slots. |
| [Electromagnet](Electromagnet.md#separating-ore) | Magnetism | Magnetic ore lying close to a powered magnet. |
| [Belt Optical Reader](Belt_Modules.md#belt-optical-reader) | Sheen (reflectivity) | A conveyor run with the reader fitted. |

Chaining separators raises the share that gets sorted: a second pass over what the first left behind catches more. Three optical readers in a row on one belt, a magnet pulling again or a drum run on all count. Each stage a unit meets gets its own chance.

## Roasting sulfides

Sulfide ores (chalcopyrite, sphalerite, pentlandite, cobaltite, argentite) are **roasted** before they leach. Heat a concentrate to 500 to 900 Tu with a little [oxygen](Chemicals.md#oxygen) in the [Reactor](Reactor.md) and it turns to a Roasted concentrate (calcine); [sulfur dioxide](Chemicals.md#sulfur-dioxide) comes off and sulfur, and for some ores iron nuggets, drop beside it. [Pyrite](Materials.md#ore-minerals) roasts in any form to iron nuggets and sulfur dioxide without a concentrate step.

Finer forms speed any reaction that accepts more than one form: roasting pyrite and carburizing cast iron to steel run quicker on dust than on a lump or an ingot. The hold JEI shows for such a reaction follows the form on display.

## Leaching and winning

Leaching dissolves the metal out of a concentrate into a **liquor**, a fluid named for the salt it carries, in the [Reactor](Reactor.md). Each liquor then gives up its metal one of two ways: **electrowinning** in the [Electrolyser](Electrolyser.md#plated-metal), which plates it on the cathode as nuggets, or **precipitation** in the Reactor with a reducing gas.

| Ore, as fed | Reagent | Liquor | Also leaves | Then |
| --- | --- | --- | --- | --- |
| Chalcopyrite (roasted) | sulfuric acid | Copper Sulfate | Silicate Slag | electrowin copper |
| Malachite, Cuprite, native Copper | sulfuric acid | Copper Sulfate | Silicate Slag (sand from native copper); malachite also vents [carbon dioxide](Chemicals.md#carbon-dioxide) | electrowin copper |
| Sphalerite (roasted), native Zinc | sulfuric acid | Zinc Sulfate | Silicate Slag | electrowin zinc |
| Pentlandite (roasted) | sulfuric acid | Nickel Sulfate | 2 cobalt nuggets; rarely an Osmiridium lump | electrowin nickel |
| Cobaltite (roasted) | sulfuric acid | Cobalt Sulfate | Silicate Slag | electrowin cobalt |
| Gold | brine and hypochlorite | Gold Chloride | 2 silver nuggets | precipitate gold with sulfur dioxide |
| Argentite (roasted) | brine and hypochlorite | Silver Chloride | a lead nugget | precipitate silver with hydrogen |
| Ilmenite | sulfuric acid | Titanyl Sulfate | 3 iron nuggets | precipitate titanium with hydrogen |
| Wolframite | lye | Sodium Tungstate | 2 manganese nuggets | precipitate tungsten with hydrogen |
| End Stone | lye | Sodium Tungstate | nothing | precipitate tungsten with hydrogen |
| Uraninite | sulfuric acid and oxygen | Uranyl Sulfate | 2 lead nuggets | precipitate uranium with hydrogen |
| Monazite | lye | Thorium Hydroxide | Silicate Slag, Rare Earth Residue | precipitate thorium with hydrogen |
| Bauxite | lye | Sodium Aluminate | Red Mud | precipitate alumina (see [Making Aluminum](Making_Aluminum.md)) |

Tailings of each of these leach too, at a lower yield. Rows and windows are on the Reactor's JEI cards; the Reactor page has the layout of a reaction.

**Ores that stop at the concentrate** are smelted from it: galena roasts to molten lead with oxygen, leaving sulfur and a silver nugget; tin and pyrolusite are reduced with charcoal to molten tin and manganese; chromite with charcoal to molten chromium and iron nuggets.

**Red Mud** is the bauxite waste. It melts to a little iron.

## Hazards

- **Sulfur dioxide, osmium tetroxide** and the other gases leave as a fume cloud if the Reactor's output tank has no room ([Hazards](Hazards.md#fume-clouds)).
- **Radioactive ores** (uraninite, monazite) stay radioactive as crushed ore, dust, concentrate and tailings.
- Dust of a toxic or combustible material is a hazard of its own; see [Dust](Materials.md#dust).

See also [Ore Deposits](Ore_Deposits.md) and [Materials](Materials.md#ore-minerals).
