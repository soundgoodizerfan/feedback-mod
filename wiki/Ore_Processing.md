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

Melting raw or crushed chalcopyrite gives molten copper, and its iron leaves as **Bloomery Slag**, which you can crush and melt for iron. Its concentrate and tailings leave no slag.

Native copper, gold, tin, zinc and lead ores take the same ladder: they are separated from their rock rather than ground.

## Stained soil as ore

The stained soils that mark a vein bear a little of its metal and take the same ladder as any ore: crush a block, wash it, then melt or leach it. The yield is small.

| Block | Metal (share) |
| --- | --- |
| Rust-Stained Soil | Cast iron (12%) |
| Gossan Rubble | Gold (2%) |
| Bleached Soil | Uranium (3%); faintly [radioactive](Radiation.md) |
| Tarnished Soil | Silver (3%) |
| Black Sand | Tin (8%); [magnetic](Electromagnet.md#separating-ore) |

Sooty Soil bears no metal.

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

Leaching dissolves the metal out of a concentrate into a **liquor**, a fluid named for the salt it carries, in the [Reactor](Reactor.md). Each liquor then gives up its metal one of two ways: **electrowinning** in the [Electrolyser](Electrolyser.md#plated-metal), which plates it on the cathode as nuggets, or **precipitation** in the Reactor with a reducing gas. Iron ore also reduces straight to Pure Iron with hydrogen, without a liquor ([Iron](Iron.md#getting-it)).

| Ore, as fed | Reagent | Liquor | Also leaves | Then |
| --- | --- | --- | --- | --- |
| Chalcopyrite (roasted) | sulfuric acid | Copper Sulfate | Silicate Slag | electrowin copper |
| Malachite, Cuprite, native Copper | sulfuric acid | Copper Sulfate | Silicate Slag (sand from native copper); malachite also vents [carbon dioxide](Chemicals.md#carbon-dioxide) | electrowin copper |
| Sphalerite (roasted), native Zinc | sulfuric acid | Zinc Sulfate | Silicate Slag; sphalerite also a cadmium nugget (10%) and an indium nugget (5%) | electrowin zinc |
| Pentlandite (roasted) | sulfuric acid | Nickel Sulfate | 2 cobalt nuggets; rarely an Osmiridium lump, a platinum nugget (3%) or a palladium nugget (5%) | electrowin nickel |
| Cobaltite (roasted) | sulfuric acid | Cobalt Sulfate | Silicate Slag | electrowin cobalt |
| Gold | brine and hypochlorite | Gold Chloride | 2 silver nuggets | precipitate gold with sulfur dioxide |
| Argentite (roasted) | brine and hypochlorite | Silver Chloride | a lead nugget | precipitate silver with hydrogen |
| Cast iron nuggets, swarf, dust | sulfuric acid | Iron Sulfate | hydrogen, graphite dust | electrowin iron |
| Ilmenite | sulfuric acid | Titanyl Sulfate | 3 iron nuggets (cast iron) | precipitate titanium with hydrogen |
| Wolframite | lye | Sodium Tungstate | 2 manganese nuggets | precipitate tungsten with hydrogen |
| End Stone | lye | Sodium Tungstate | nothing | precipitate tungsten with hydrogen |
| Uraninite | sulfuric acid and oxygen | Uranyl Sulfate | 2 lead nuggets | precipitate uranium with hydrogen |
| Monazite | lye | Thorium Hydroxide | Silicate Slag, Rare Earth Residue | precipitate thorium with hydrogen |
| Bauxite | lye | Sodium Aluminate | Red Mud; a gallium nugget (10%) | precipitate alumina (see [Guide: Making Aluminum](Guide_Making_Aluminum.md)) |

**End Stone** is a low-grade tungsten ore. Hammer a block of it to Crushed End Stone ([Mechanical Hammer](Mechanical_Hammer.md#crushing-ore) or by hand on an [Anvil](Anvil.md)), then wash it in the [Mixer](Mixer.md#washing-ore), draw it with an [Electromagnet](Electromagnet.md#separating-ore) or sort it with a [Belt Optical Reader](Belt_Modules.md#belt-optical-reader) for End Stone Concentrate and Tailings. In the [Reactor](Reactor.md) at **150 to 250 Tu**, 200 mB of lye leaches the concentrate into **33 mB** of sodium tungstate and the tailings into **10 mB**. Heating End Stone itself gives hydrogen and helium ([Reactor](Reactor.md#the-reactions)).

Tailings of each of these leach too, at a lower yield. Rows and windows are on the Reactor's JEI cards; the Reactor page has the layout of a reaction.

**Ores that stop at the concentrate** (apart from the refractory ones above) are smelted from it: galena roasts to molten lead with oxygen, leaving sulfur, a silver nugget and, one time in ten, a bismuth nugget; tin and pyrolusite are reduced with charcoal to molten tin and manganese; chromite with charcoal to molten chromium and iron nuggets. Cinnabar concentrate roasts in the [Reactor](Reactor.md#the-reactions) with oxygen at **400 to 620 Tu** (best near 520) to a mercury ingot and sulfur dioxide; above 750 Tu the mercury is lost and the batch spoils to scrap metal. Stibnite concentrate is precipitated with iron: one concentrate and three pieces of iron (nugget, scrap or swarf) in the Reactor at **600 to 950 Tu** (best near 750) give an antimony ingot and a Bloomery Slag, and above 1100 Tu the batch is lost to slag. Stibnite also melts directly. Vanadinite concentrate is reduced with aluminum: one concentrate and two pieces of aluminum (nugget, scrap or swarf) in the Reactor at **900 to 1400 Tu** (best near 1100) give a vanadium ingot and a Bloomery Slag, and above 1600 Tu the batch is lost to slag. Vanadinite also melts directly.

## Reducing refractory ores

**Ilmenite, dolomite and beryl are refractory**: a crucible or furnace gives crushed ore, concentrate and tailings of them but never melts them to metal. Their metal comes only from a reducing process, run as a heat process in a vessel (bands and holds are on the JEI cards).

| Metal | Feed | Heat | Gives |
| --- | --- | --- | --- |
| Titanium (Kroll) | 2 magnesium ingots and 144 mB titanium tetrachloride | 800 to 1050 Tu, best near 900 | 9 titanium nuggets |
| Titanium (sulfate) | ilmenite concentrate leached with sulfuric acid, then precipitated with hydrogen ([above](#leaching-and-winning)) | Reactor | titanium |
| Magnesium (Pidgeon) | 4 dolomite concentrate and 1 crude silicon ingot | 1100 to 1300 Tu, best near 1200 | 8 magnesium nuggets |
| Beryllium | 6 beryl concentrate, 1 magnesium ingot and 288 mB hydrofluoric acid | 1200 to 1400 Tu, best near 1300 | 4 beryllium nuggets |

Ilmenite concentrate with charcoal and 600 mB chlorine at 800 to 1200 Tu, best near 1000, gives 3 iron nuggets and 210 mB of titanium tetrachloride, the Kroll feed. Dolomite gives the magnesium that reduces both titanium tetrachloride and beryl. The silicon for the Pidgeon process is [Crude Silicon](Materials.md#silicon).

**Red Mud** is the bauxite waste. It melts to a little iron.

## Gold by amalgam

Mercury takes gold out of its concentrate without acid. In the [Reactor](Reactor.md#the-reactions), a gold concentrate and a mercury ingot kept at **10 to 60 Tu** (best near 30) make one **Gold Amalgam**; above 90 Tu the batch spoils to scrap metal. Gold Amalgam is a toxic dust ([Materials](Materials.md#dust)).

Heat the amalgam in a vessel at **360 to 600 Tu** (best near 450) and it gives **12 gold nuggets** and the mercury ingot back, ready for the next batch. Above 700 Tu the batch spoils to scrap metal. Finishing the batch in a vessel open to the air lets a toxic [fume cloud](Hazards.md#fume-clouds) go; a sealed vessel lets nothing out.

## Hazards

- **Sulfur dioxide, osmium tetroxide** and the other gases leave as a fume cloud if the Reactor's output tank has no room ([Hazards](Hazards.md#fume-clouds)).
- **Radioactive ores** (uraninite, monazite) stay radioactive as crushed ore, dust, concentrate and tailings.
- **Gold Amalgam** retorted in an open vessel releases a fume cloud of mercury.
- Dust of a toxic or combustible material is a hazard of its own; see [Dust](Materials.md#dust).

See also [Ore Deposits](Ore_Deposits.md) and [Materials](Materials.md#ore-minerals).
