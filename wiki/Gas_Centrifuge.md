# Gas Centrifuge

A shaft-driven machine that separates natural uranium into **Enriched Uranium** and **Depleted Uranium**. The enriched ingots make [Enriched Fuel Rods](Nuclear_Fuel.md#enriched-fuel-rod); the depleted ones make [Breeder Rods](Nuclear_Fuel.md#breeder-rod).

## Crafting

```
S T S
S B S
S T S
```

S is a Stainless Steel Plate, T is Copper Tubing and B is a Bearing.

## Using it

Put the centrifuge on a [shaft](Shaft.md) (the shaft crosses the block). Right-click with Uranium Ingots to load them (up to four batches), and with a bucket of hydrofluoric acid, or a pipe, to fill its tank. Right-click with an empty hand to take what it has made, or to read its feed, acid and progress.

## How it works

A batch takes **4 ingots and 50 mB of hydrofluoric acid** and makes **1 Enriched Uranium and 3 Depleted Uranium ingot**. Nothing happens below **24 RPM**. Above it the batch advances with the square of the speed, so a shaft at twice the speed finishes four times as fast. It draws a steady load from the shaft. The machine keeps its feed, acid and finished ingots until you take them.
