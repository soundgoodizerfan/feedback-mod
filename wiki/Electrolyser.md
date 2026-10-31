# Electrolyser

Runs electric current through a liquid and collects what comes apart at each electrode. It splits water into [hydrogen](Chemicals.md#hydrogen) and [oxygen](Chemicals.md#oxygen). It is a different machine from the [Electrolytic Cell](Electrolytic_Cell.md), which dissolves and plates metal.

## Crafting

```
W   W
G G G
P P P
```

W is [Copper Wire](Wire_Drawer.md), G is Glass and P is a wrought iron plate.

## Using it

1. **Fill it with the liquid** from any side face: right-click with a bucket or run a [Pipe](Pipe.md) in. It holds 2000 mB, and takes only a liquid that can be split.
2. **Connect power** with [cable](Copper_Cable.md). It asks for **8 A** and needs the liquid's splitting voltage (**1.23 V** for water); below that it does nothing.
3. **Take the products off.** What collects at the **anode** leaves the **front** face and what collects at the **cathode** the **back** face. Each has its own 2000 mB tank. Put a [Gas Tank](Gas_Tank.md) or pipe against a face, or a [Culture Dish](Sculk_Culture.md) against the front.

## Water

Each mB of water gives **2 mB of hydrogen** at the cathode (back) and **1 mB of oxygen** at the anode (front).

## Rate

It splits 0.05 mB of feed per tick for every amp, so a full 8 A uses 0.4 mB a tick. Less current splits proportionally less. With the feed out, or an electrode tank full, it asks for no current.

## Wear

More than the voltage it needs costs condition, down to a floor of 20%. It never stops running because of it.
