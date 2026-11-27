# Reactor

A steel pressure vessel that holds a mixture hot, under pressure, over a catalyst, and lets whatever reaction fits happen. It knows no recipes: it looks at what is inside and runs the reaction (a JEI *reaction* page) that matches. (The nuclear machine is the [Fission Reactor](Fission_Reactor.md).) Heavy oil from a [Fractionating Column](Fractionating_Column.md) cracks into diesel and naphtha here; ethylene polymerizes into plastic; soul sand cooks into [soul oil](Oil.md#soul-oil).

**A reactor holds its gas under pressure, and it can burst.** Read [Pressure](#pressure) before you pipe gas into one.

## Crafting

```
P P P
P P P
P   P
```

P is a [Steel Plate](Mechanical_Hammer.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 2 [fasteners](Fasteners.md), holding Nuclear Treatise I: Reactors and Fuel ([Treatise](Treatise.md)).

Its casing is riveted steel plate.

## Using it

1. **Heat it.** Set it on a [Firebox](Firebox.md), a [Fluid Burner](Fluid_Burner.md), a [Heating Coil](Heating_Coil.md) or lava, like a [Crucible](Crucible.md). It climbs toward the fire's temperature.
2. **Feed it fluid.** A bucket or a [Pipe](Pipe.md) fills it. It has **three input tanks** of 2000 mB, and it reaches for any fluid that some reaction lists as an input from a tank or pipe touching any side, whenever it has room. It does not take fluids no reaction wants.
3. **Feed it solids.** Right-click with an item and it goes into the **hopper**, three slots; a reaction that wants items takes one of each per batch.
4. **Pack a bed** if the reaction needs a catalyst: right-click with the catalyst (see [Catalysts](#catalysts)). It takes one item, and only if a reaction names it.
5. **Take the product.** The **two output tanks** (2000 mB each) push themselves out of any side into whatever accepts them; a reaction with two fluid products fills one tank with each: a pipe, a column, a tank. A solid product pops out on top.

**Right-click with an empty hand** to read it on the reading card: its temperature, its pressure and the bed's activity. **Sneak-right-click with an empty hand** to take back everything in the hopper and the bed.

## How a reaction runs

A reaction runs when every input is there (the fluids at their full amount, the items in the hopper, the catalyst bed if it needs one). Each tick it adds progress:

- **Temperature.** Every reaction has a window with a best temperature in the middle. Progress is fastest at the best and falls to nothing at either edge, the way a crucible batch does ([Steel](Guide_Making_Steel.md)). A slip out of the window only slows it, and the progress it has made decays instead of vanishing.
- **Pressure.** A gas reaction wants a minimum pressure and is fastest above its best. Under the minimum nothing happens.
- **The bed.** A worn bed slows it; below 20% activity it stops.
- **Heating too fast.** A reactor whose temperature is swinging faster than the reaction can follow makes no progress either. Heat it steadily.

When the progress is full the batch happens: the inputs are used up and the products appear in the output tank.

**Solvent hops things.** Unstable Spatial Solvent and its vapor throw nearby items and creatures a few blocks when a product has nowhere to go or the reactor holds the solvent at or above 160 Tu ([Liquid Teleportant](Liquid_Teleportant.md#unstable-solvent)).

**Too hot makes a worse product, never a lock.** Past a reaction's top temperature, a reaction that has a side product makes that instead. Cracking past 520 Tu hands back the heavy oil unchanged and cokes the bed; the inputs are gone either way.

Some reactions give off heat, which warms the reactor.

## The reactions

| Reaction | Needs | Makes | Window |
| --- | --- | --- | --- |
| **Cracking** | 100 mB heavy oil, a catalyst bed | 60 mB diesel and 30 mB naphtha | 380 to 520 Tu, best 440 |
| **Steam cracking** | 100 mB naphtha vapor | 40 mB [ethylene](Chemicals.md#ethylene) and 20 mB methane; it absorbs heat | 700 to 850 Tu, best 780 |
| **Reforming** | 100 mB naphtha vapor, a platinum or palladium ingot bed | 30 mB [xylene](Chemicals.md#polyester-chain) and 20 mB hydrogen | 450 to 520 Tu, best 490 |
| **Oxidize xylene** | 100 mB xylene, 150 mB air, a cobalt or manganese ingot bed | 80 mB [terephthalic acid](Chemicals.md#polyester-chain); held past 240 Tu, 80 mB carbon dioxide and 40 mB water instead | 180 to 240 Tu, best 210 |
| **Epoxidize ethylene** | 100 mB ethylene, 50 mB oxygen, a silver ingot bed | 80 mB [ethylene oxide](Chemicals.md#polyester-chain) | 220 to 280 Tu, best 250 |
| **Hydrate ethylene oxide** | 100 mB ethylene oxide, 100 mB water | 100 mB [ethylene glycol](Chemicals.md#polyester-chain) | 100 to 180 Tu, best 140 |
| **Polycondensation** | 50 mB terephthalic acid, 50 mB ethylene glycol | 20 mB polyester melt and 30 mB water; held past 300 Tu, 10 mB melt and 10 mB [acetaldehyde](Chemicals.md#polyester-chain) instead | 260 to 300 Tu, best 280 |
| **Polymerization** | 100 mB ethylene, a catalyst bed, **1 Pu** at least (best from 2 Pu) | 10 mB polyethylene melt, and some heat | 120 to 220 Tu, best 160 |
| **Soul polymerization** | 100 mB soul oil, a catalyst bed, **1 Pu** at least (best from 2 Pu) | 20 mB soul resin melt | 180 to 300 Tu, best 220 |
| **Soul retort** | one Soul Sand or Soul Soil in the hopper | 60 mB soul oil, 20 mB wisp gas and a **Husk Sand** | 300 to 600 Tu, best 420 |
| **Magma settling** | 1000 mB [Magma Melt](Magma_Drill.md#settling-the-melt) | 500 mB molten iron, 60 mB molten nickel, 20 mB molten cobalt and 300 mB molten slag | 1550 Tu or more, best 1700 |
| **Dissolve salt** | one Salt, 250 mB water | 250 mB [brine](Chemicals.md#brine) | 20 to 100 Tu, best 60 |
| **Bayer (bauxite)** | any bauxite item, 250 mB [lye](Chemicals.md#lye) | 2 [alumina](Guide_Making_Aluminum.md) and 200 mB lye back | 150 to 250 Tu, best 200 |
| **Bayer (lapis)** | one Lapis Lazuli, 250 mB lye | 1 alumina, one Silicate Slag and 200 mB lye back; the lapis' stored experience is lost | 150 to 250 Tu, best 200 |
| **Roast pyrite** | any pyrite item | 4 cast iron nuggets and 200 mB [sulfur dioxide](Chemicals.md#sulfur-dioxide) | 500 to 900 Tu, best 700 |
| **Sulfuric acid** | 100 mB sulfur dioxide, 50 mB oxygen, 100 mB water, a catalyst bed, **1 Pu** at least (best from 2 Pu) | 100 mB [sulfuric acid](Chemicals.md#acids), and some heat | 400 to 550 Tu, best 450 |
| **Hydrochloric acid** | 100 mB hydrogen, 100 mB chlorine | 100 mB [hydrochloric acid](Chemicals.md#acids), and some heat | 20 to 200 Tu, best 60 |
| **Hydrofluoric acid** | one Cryolite Ore, 100 mB [sulfuric acid](Chemicals.md#acids) | 200 mB [hydrofluoric acid](Chemicals.md#fluorine-and-ptfe), and some heat | 100 to 250 Tu, best 150 |
| **Methane** | one Coal, 200 mB hydrogen, a cracking catalyst bed, **1 Pu** at least (best from 2 Pu) | 100 mB [methane](Chemicals.md#fluorine-and-ptfe), and some heat | 500 to 750 Tu, best 600 |
| **Chloroform** | 100 mB methane, 300 mB chlorine | 100 mB [chloroform](Chemicals.md#fluorine-and-ptfe) and 300 mB hydrochloric acid, and some heat | 250 to 420 Tu, best 320 |
| **Chlorodifluoromethane** | 100 mB chloroform, 200 mB hydrofluoric acid, **1 Pu** at least (best from 2 Pu) | 100 mB [chlorodifluoromethane](Chemicals.md#fluorine-and-ptfe) and 200 mB hydrochloric acid, and some heat | 60 to 180 Tu, best 100 |
| **Tetrafluoroethylene** | 200 mB chlorodifluoromethane | 100 mB [tetrafluoroethylene](Chemicals.md#fluorine-and-ptfe) and 100 mB hydrochloric acid, and some heat | 600 to 850 Tu, best 700 |
| **PTFE powder** | 100 mB tetrafluoroethylene, 100 mB water, **2 Pu** at least (best from 4 Pu) | 4 **PTFE Powder**, and some heat | 40 to 110 Tu, best 70 |
| **Hypochlorite** | 100 mB chlorine, 100 mB lye | 100 mB [Hypochlorite Bleach](Chemicals.md#hypochlorite-bleach) | 20 to 60 Tu, best 40 |
| **Nitrogen separation** | 100 mB air, a molecular sieve bed (charcoal) | 78 mB [nitrogen](Chemicals.md#nitrogen) | 20 to 100 Tu, best 40 |
| **Liquefy air** | 160 mB air, **2 Pu** at least (best from 4 Pu), a [Cryocooler](Cryocooler.md) on its deep stage | 32 mB liquid air | -200 to -188 Tu, best -194 |
| **Rectify air** | 32 mB liquid air, a Cryocooler on its middle stage | 125 mB nitrogen and 7 mB oxygen-rich liquid; too warm, the liquid boils back to air | -196 to -186 Tu, best -190.5 |
| **Liquefy wisp gas** | 160 mB wisp gas, **2 Pu** at least (best from 4 Pu), a [Cryocooler](Cryocooler.md) | 32 mB [liquid wisp gas](Oil.md#soul-oil) | -120 to -100 Tu, best -110 |
| **Helium cut** | 100 mB nitrogen | 4 mB [helium](Chemicals.md#helium) and 96 mB nitrogen; too warm, only the nitrogen | -196 to -189 Tu, best -193 |
| **Argon cut** | 7 mB oxygen-rich liquid, a Cryocooler on its shallow stage | 2 mB [argon](Chemicals.md#argon) and 33 mB oxygen; too warm, only oxygen | -188 to -182.5 Tu, best -185 |
| **Leach osmiridium** | one Raw Osmiridium, 200 mB sulfuric acid, 100 mB oxygen | 100 mB [osmium tetroxide](Chemicals.md#osmium-tetroxide), one **Iridium Residue**, and some heat | 60 to 125 Tu, best 90 |
| **Reduce osmium tetroxide** | 100 mB osmium tetroxide, 400 mB hydrogen | one **Osmium Sponge**, and some heat | 280 to 420 Tu, best 330 |
| **Reduce iridium residue** | one Iridium Residue, 200 mB hydrogen | one **Iridium Sponge**, and some heat | 450 to 800 Tu, best 550 |
| **Amalgamate gold** | one gold concentrate, one mercury ingot | one **Gold Amalgam**; above 90 Tu the batch spoils to scrap metal | 10 to 60 Tu, best 30 |
| **Roast cinnabar** | one cinnabar concentrate, 100 mB oxygen | one mercury ingot and 100 mB [sulfur dioxide](Chemicals.md#sulfur-dioxide); above 750 Tu the mercury is lost and the batch spoils to scrap metal | 400 to 620 Tu, best 520 |
| **Precipitate stibnite** | one stibnite concentrate, three pieces of iron (nugget, scrap or swarf) | one antimony ingot and one Bloomery Slag; above 1100 Tu the batch is lost to slag | 600 to 950 Tu, best 750 |
| **Reduce vanadinite** | one vanadinite concentrate, two pieces of aluminum (nugget, scrap or swarf) | one vanadium ingot and one Bloomery Slag; above 1600 Tu the batch is lost to slag | 900 to 1400 Tu, best 1100 |
| **Roast molybdenite** | one molybdenite concentrate, 100 mB oxygen | one **Molybdenum Calcine**, one **Rhenium Oxide** and 100 mB [sulfur dioxide](Chemicals.md#sulfur-dioxide); above 800 Tu the batch spoils | 450 to 750 Tu, best 600 |
| **Reduce rhenium oxide** | one Rhenium Oxide, 300 mB hydrogen | one **Rhenium Sponge**, and some heat | 300 to 600 Tu, best 400 |
| **Ammonia** | 100 mB nitrogen, 300 mB hydrogen, an iron bed | 200 mB [ammonia](Chemicals.md#ammonia), and some heat | 400 to 500 Tu, best 450 |
| **Nitric acid** | 100 mB ammonia, 200 mB oxygen | 100 mB [nitric acid](Chemicals.md#nitric-acid), and some heat | 200 to 320 Tu, best 260 |
| **Nitrate cellulose** | one Paper, 100 mB nitric acid, 100 mB sulfuric acid | one **Nitrocellulose Dust** and 60 mB sulfuric acid returned | 0 to 40 Tu, best 20 |
| **Creeper oil** | one Creeper Charge, 100 mB naphtha | 100 mB [creeper oil](Chemicals.md#creeper-oil) | 30 to 90 Tu, best 60 |
| **Fertilizer** | 200 mB ammonia, 100 mB sulfuric or hydrochloric acid | 2 [Fertilizer](Fertilizer.md) | 20 to 100 Tu, best 50 |
| **Raw Spatial Slurry** | one Chorus Fruit, 100 mB water | 100 mB Raw Spatial Slurry | 0 to 60 Tu, best 25 |
| **Damped Spatial Solvent** | 100 mB Unstable Spatial Solvent, 5 mB [Heavy Water](Heavy_Water.md), one Lapis Lazuli dust | 100 mB Damped Spatial Solvent; above 70 Tu, solvent vapor | 5 to 70 Tu, best 30 |
| **Liquid Teleportant** | 100 mB Damped Spatial Solvent, one Redstone dust, an [Arc Head](Arc_Furnace.md#the-arc-head) against the reactor | 100 mB [Liquid Teleportant](Liquid_Teleportant.md); above 2200 Tu, solvent vapor | 300 to 2200 Tu, best 900 |
| **End stone** | one End Stone | 250 mB hydrogen, 100 mB [helium](Chemicals.md#helium) and one Porous Aggregate | 700 to 1300 Tu, best 900 |
| **Alkaline Electrolyte** | 500 mB water, 10 mB [lye](Chemicals.md#lye) | 500 mB Alkaline Electrolyte | 0 to 80 Tu, best 20 |
| **Roast a sulfide concentrate** | one concentrate of chalcopyrite, sphalerite, pentlandite, cobaltite or argentite, 100 mB oxygen | one Roasted concentrate, 100 mB sulfur dioxide, sulfur (and cast iron nuggets for some) | 500 to 900 Tu, best 700 |
| **Diamond (high pressure)** | one Graphite and one Diamond Seed in the hopper, 160 mB [argon](Chemicals.md#argon), an iron, nickel or cobalt ingot bed, **3.8 Pu** at least (best at 4.6 Pu) | 1 Diamond, the argon returned; over 1700 Tu, a Graphite instead | 1400 to 1700 Tu, best 1550 |
| **Diamond (vapor)** | one Diamond Seed, 8 mB [methane](Chemicals.md#fluorine-and-ptfe), 72 mB hydrogen, **1.5 Pu** at least (best at 2 Pu) | 2 Diamonds and 64 mB hydrogen; over 950 Tu, a Graphite instead | 780 to 950 Tu, best 880 |
| **Purify redstone** | one Redstone dust, 50 mB sulfuric or hydrochloric acid | one [Purified Redstone](Materials.md#superconductor) | 40 to 120 Tu, best 60 |
| **Superconductor anneal** | one fine wire of Redstone Filament with texture 1, 2 or 3, 160 mB oxygen, **3.8 Pu** at least (best at 4.6 Pu) | a poor, fair or best Redstone Superconductor Wire (by texture), the oxygen returned; too hot, redstone dust | 840 to 920 Tu, best 880 |
| **Synthesize cryolite** | one alumina, 300 mB hydrofluoric acid, 150 mB [lye](Chemicals.md#lye) | 2 Cryolite Dust, and some heat | 80 to 160 Tu, best 110 |
| **Molten cryolite** | one Cryolite Ore | 144 mB [cryolite bath](Chemicals.md#cryolite-bath) | melts at 1010 Tu |
| **Molten cryolite dust** | one Cryolite Dust | 72 mB cryolite bath | melts at 1010 Tu |

Porous Aggregate, left by the End stone row, is a very light rock that barely conducts heat. End Stone is also a tungsten ore; its lye leach is in [Ore Processing](Ore_Processing.md#leaching-and-winning).

**Leach rows.** Every ore concentrate has a leach row that dissolves its metal into a liquor, with a reagent (sulfuric acid, lye, or brine and hypochlorite) and a window of its own, and a row that precipitates a dissolved metal out with hydrogen or sulfur dioxide. They are laid out ore by ore in [Ore Processing](Ore_Processing.md#leaching-and-winning) and listed in JEI.

The cold rows need no catalyst bed. Their temperatures are set by the Cryocooler against the reactor, not by a fire. A toxic gas the output tanks have no room for escapes as a [fume cloud](Hazards.md#fume-clouds). JEI shows every row, including the ones a pack adds. Polymerization, soul polymerization and polycondensation make the melts that become [Polymers](Materials.md#polymers); soul oil and wisp gas are fuels ([Soul Oil](Oil.md#soul-oil), [Fuels](Fuels.md)); the retort needs no bed.

## Catalysts

A catalyst speeds a reaction up without being used up. The bed is an item whose **material** says it is a catalyst, so any item of that material works, and a reaction names a *kind* of catalyst, not an item. Two are known for cracking and sulfuric acid; nitrogen separation takes a **molecular sieve** bed, which is charcoal; ammonia is made over an iron ingot bed, and the high-pressure diamond row over an iron, nickel or cobalt ingot bed.

| Bed | Speed | Sulfur tolerance | Burn it clean at | Sinters at |
| --- | --- | --- | --- | --- |
| **Brick** | 1× | high (2×) | 450 Tu | 700 Tu |
| **Blaze Powder** | 2× | low (0.5×) | 450 Tu | 520 Tu |
| **Charcoal** (molecular sieve) | 0.6× | 1× | 200 Tu | 500 Tu |

The bed's activity is shown as a percentage. It falls three ways:

- **Poison.** Sulfur in the feed (a sulfurous crude's cuts carry it) fouls the bed in proportion to how much you feed it. A high tolerance fouls slower.
- **Coke.** Every batch made above a reaction's top temperature coats the bed (5% a batch).
- **Sinter.** A bed held **over its sinter temperature** loses activity for good, faster the further over. Blaze powder sinters 180 Tu sooner than brick. The reactor's own temperature tells you before it happens.

**Regeneration.** With no reaction running (the feed stopped) and the bed at least 450 Tu hot:

- An **oxidizer flowing in** (air) burns the coke off, and gives some heat back. Feed it too hot and a blaze-powder bed sinters before it is clean: the window is 450 to 520 Tu.
- A **reducing gas flowing in** sweeps poison off, slowly.

Both read the fluid's properties, never its name.

## Shell wear

A reactor starts with a sound steel shell. While any of its tanks holds a corrosive fluid (an acid such as [sulfuric acid](Chemicals.md#acids), or a base such as [lye](Chemicals.md#lye)), the shell wears a little every second, never all at once. It wears down to about a third of its condition and no further, and stops wearing when the tanks hold nothing corrosive. Condition shows in Jade as *Wearing*.

A worn shell gives way at a lower pressure: the risk line and the burst line in [Pressure](#pressure) both come down as condition falls. A [Pipe](Pipe.md) carrying acid is eaten through instead ([Corrosion](Pipe.md#corrosion)).

## Pressure

The reactor's pressure is the **gas** in its input tanks: **1 Pu for every 40 mB**. It reads like any [pressure vessel](Pressure_Vessel.md); the figures below are for an unworn shell:

| Pressure | What happens |
| --- | --- |
| up to 5 Pu (200 mB of gas) | nothing |
| above 5 Pu | each tick there is a chance it **bursts**, climbing the further over |
| 7 Pu (280 mB of gas) | it **bursts** |

A burst empties every tank and blows like a [Boiler](Boiler.md#pressure). A reactor takes gas from any pipe touching it for as long as it has room, so a gas line that is not limited ([Valve](Valve.md)) fills it far past the danger line. Feed gas in a few hundred mB at a time, and watch the reading.
