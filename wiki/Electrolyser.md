# Electrolyser

Runs electric current through a liquid and collects what comes apart at each electrode. It splits water into [hydrogen](Chemicals.md#hydrogen) and [oxygen](Chemicals.md#oxygen), [brine](Chemicals.md#brine) into hydrogen, [chlorine](Chemicals.md#chlorine) and [lye](Chemicals.md#lye), the [leach liquors](Chemicals.md#leach-liquors) of copper, zinc, nickel and cobalt into metal plated on the cathode and oxygen, and a molten [cryolite bath](Chemicals.md#cryolite-bath) into [aluminum](Making_Aluminum.md) and oxygen. It is a different machine from the [Electrolytic Cell](Electrolytic_Cell.md), which dissolves and plates metal.

## Crafting

```
W   W
G G G
P P P
```

W is [Copper Wire](Wire_Drawer.md), G is Glass and P is a wrought iron plate.

## Using it

1. **Fill it with the liquid** from any side face: right-click with a bucket or run a [Pipe](Pipe.md) in. It holds 2000 mB, and takes only a liquid that can be split.
2. **Connect power** with [cable](Copper_Cable.md). It asks for **8 A** and needs the liquid's splitting voltage; below that it does nothing. It is rated for **6 V** (medium voltage).
3. **Take the products off.** What collects at the **anode** leaves the **front** face and what collects at the **cathode** the **back** face. Each has its own 2000 mB tank. Put a [Gas Tank](Gas_Tank.md) or pipe against a face, or a [Culture Dish](Sculk_Culture.md) against the front.

## Seeing what it can split

JEI's *Thermal process* card lists every liquid it can split, the voltage it needs and what leaves at each electrode.

## What it splits

| Liquid | Voltage | Cathode (back) | Anode (front) | Also |
| --- | --- | --- | --- | --- |
| Water | 1.23 V | 2 mB hydrogen per mB | 1 mB oxygen per mB | |
| Alkaline Electrolyte | 1.8 V | 2 mB hydrogen per mB | 1 mB oxygen per mB | about 1 mB [Enriched Water](Heavy_Water.md) stays per 25 mB |
| Enriched Water | 1.8 V | 2 mB hydrogen per mB | 1 mB oxygen per mB | about 1 mB [Heavy Water](Heavy_Water.md) stays per 20 mB |
| Brine | 2.2 V | 1 mB hydrogen per mB | 1 mB chlorine per mB | 1 mB lye per mB stays in the cell |
| Copper Sulfate Liquor | 2.0 V | 1 Copper Nugget plated per 16 mB | 1 mB oxygen per mB | |
| Zinc Sulfate Liquor | 3.0 V | 1 Zinc Nugget plated per 16 mB | 1 mB oxygen per mB | |
| Nickel Sulfate Liquor | 2.5 V | 1 Nickel Nugget plated per 16 mB | 1 mB oxygen per mB | |
| Cobalt Sulfate Liquor | 2.5 V | 1 Cobalt Nugget plated per 16 mB | 1 mB oxygen per mB | |
| Cryolite bath with alumina | 4.0 V | 1 mB molten aluminum per unit | 1 mB oxygen per unit | needs the bath at **950 Tu** or hotter |

### Plated metal

A sulfate liquor gives up its metal as **nuggets plated on the cathode**, not as a fluid. They collect in the cell: take them with an empty hand, or let a hopper draw them from the **back** (cathode) face. When the plate stack is full the cell stops until they are taken.

### Brine

The lye the brine leaves behind stays in the cell. Draw it off from a side face with a bucket or [Pipe](Pipe.md).

### Aluminum bath

The cryolite bath is the solvent and is not used up; the cell needs a good measure of it (500 mB) to run. Feed **alumina** into the cell by hand or from a hopper; each alumina gives **72 mB** of molten aluminum at the cathode and oxygen at the anode.

The cell warms from its own current (volts times amps) while it is powered and cools toward room temperature when idle. A cold bath takes current to warm before it splits anything, and the cell holds its heat while it works.

**Anode effect.** A bath with no alumina left breaks down: the bath drains slowly, fumes poison anyone nearby and the cell loses condition. Keep alumina going in, or cut the power.

## Rate

It splits 0.05 mB of feed per tick for every amp, so a full 8 A uses 0.4 mB a tick. Less current splits proportionally less. With the feed out, or an electrode tank full, it asks for no current.

## Wear and hazards

More than the voltage it needs costs condition, down to a floor of 20%. It never stops running because of it.

Its glass-and-plate lining resists the acids and bases it makes. A toxic gas such as chlorine in a full electrode tank with nowhere to go seeps out and poisons anyone nearby; keep the electrode tanks drained ([Hazards](Hazards.md)).
