# Reactor

A steel pressure vessel that holds a mixture hot, under pressure, over a catalyst, and lets whatever reaction fits happen. It knows no recipes: it looks at what is inside and runs the reaction (a JEI *reaction* page) that matches. Heavy oil from a [Fractionating Column](Fractionating_Column.md) cracks into diesel and naphtha here; ethylene polymerises into plastic; soul sand cooks into [soul oil](Soul_Oil.md).

**A reactor holds its gas under pressure, and it can burst.** Read [Pressure](#pressure) before you pipe gas into one.

## Crafting

```
P P P
P   P
P P P
```

P is a [Steel Plate](Mechanical_Hammer.md).

## Using it

1. **Heat it.** Set it on a [Firebox](Firebox.md), a [Fluid Burner](Fluid_Burner.md), a [Heating Coil](Heating_Coil.md) or lava, like a [Crucible](Crucible.md). It climbs toward the fire's temperature.
2. **Feed it fluid.** A bucket or a [Pipe](Pipe.md) fills it. It has **three input tanks** of 2000 mB, and it reaches for any fluid that some reaction lists as an input from a tank or pipe touching any side, whenever it has room. It does not take fluids no reaction wants.
3. **Feed it solids.** Right-click with an item and it goes into the **hopper**, three slots; a reaction that wants items takes one of each per batch.
4. **Pack a bed** if the reaction needs a catalyst: right-click with the catalyst (see [Catalysts](#catalysts)). It takes one item, and only if a reaction names it.
5. **Take the product.** The **output tank** (2000 mB) pushes itself out of any side into whatever accepts it: a pipe, a column, a tank. A solid product pops out on top.

**Right-click with an empty hand** to read it: its temperature, its pressure and the bed's activity. **Sneak-right-click with an empty hand** to take back everything in the hopper and the bed.

## How a reaction runs

A reaction runs when every input is there (the fluids at their full amount, the items in the hopper, the catalyst bed if it needs one). Each tick it adds progress:

- **Temperature.** Every reaction has a window with a best temperature in the middle. Progress is fastest at the best and falls to nothing at either edge, the way a crucible batch does ([Steel](Steel.md)). A slip out of the window only slows it, and the progress it has made decays instead of vanishing.
- **Pressure.** A gas reaction wants a minimum pressure and is fastest above its best. Under the minimum nothing happens.
- **The bed.** A worn bed slows it; below 20% activity it stops.
- **Heating too fast.** A reactor whose temperature is swinging faster than the reaction can follow makes no progress either. Heat it steadily.

When the progress is full the batch happens: the inputs are used up and the products appear in the output tank.

**Too hot makes a worse product, never a lock.** Past a reaction's top temperature, a reaction that has a side product makes that instead. Cracking past 520 Tu hands back the heavy oil unchanged and cokes the bed; the inputs are gone either way.

Some reactions give off heat, which warms the reactor.

## The reactions

| Reaction | Needs | Makes | Window |
| --- | --- | --- | --- |
| **Cracking** | 100 mB heavy oil, a catalyst bed | 60 mB diesel and 30 mB naphtha | 380 to 520 Tu, best 440 |
| **Polymerisation** | 100 mB ethylene, a catalyst bed, **1 Pu** at least (best from 2 Pu) | 10 mB polyethylene melt, and some heat | 120 to 220 Tu, best 160 |
| **Soul retort** | one Soul Sand or Soul Soil in the hopper | 60 mB soul oil, 20 mB wisp gas and a **Husk Sand** | 300 to 600 Tu, best 420 |

JEI shows every row, including the ones a pack adds. Polymerisation makes the melt that becomes [Polymers](Polymers.md); soul oil and wisp gas are fuels ([Soul Oil](Soul_Oil.md), [Fuels](Fuels.md)); the retort needs no bed.

## Catalysts

A catalyst speeds a reaction up without being used up. The bed is an item whose **material** says it is a catalyst, so any item of that material works, and a reaction names a *kind* of catalyst, not an item. Two are known:

| Bed | Speed | Sulfur tolerance | Burn it clean at | Sinters at |
| --- | --- | --- | --- | --- |
| **Brick** | 1× | high (2×) | 450 Tu | 700 Tu |
| **Blaze Powder** | 2× | low (0.5×) | 450 Tu | 520 Tu |

The bed's activity is shown as a percentage. It falls three ways:

- **Poison.** Sulfur in the feed (a sulfurous crude's cuts carry it) fouls the bed in proportion to how much you feed it. A high tolerance fouls slower.
- **Coke.** Every batch made above a reaction's top temperature coats the bed (5% a batch).
- **Sinter.** A bed held **over its sinter temperature** loses activity for good, faster the further over. Blaze powder sinters 180 Tu sooner than brick. The reactor's own temperature tells you before it happens.

**Regeneration.** With no reaction running (the feed stopped) and the bed at least 450 Tu hot:

- An **oxidiser flowing in** (air) burns the coke off, and gives some heat back. Feed it too hot and a blaze-powder bed sinters before it is clean: the window is 450 to 520 Tu.
- A **reducing gas flowing in** sweeps poison off, slowly.

Both read the fluid's properties, never its name.

## Pressure

The reactor's pressure is the **gas** in its input tanks: **1 Pu for every 40 mB**. It reads like any [pressure vessel](Pressure_Vessel.md):

| Pressure | What happens |
| --- | --- |
| up to 5 Pu (200 mB of gas) | nothing |
| above 5 Pu | each tick there is a chance it **bursts**, climbing the further over |
| 7 Pu (280 mB of gas) | it **bursts** |

A burst empties every tank and blows like a [Boiler](Boiler.md#pressure). A reactor takes gas from any pipe touching it for as long as it has room, so a gas line that is not limited ([Valve](Valve.md)) fills it far past the danger line. Feed gas in a few hundred mB at a time, and watch the reading.
