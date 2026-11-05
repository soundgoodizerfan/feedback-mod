# Scram Trip

A passive safety block that drops every control rod of a [Fission Reactor](Fission_Reactor.md) the moment the core runs too hot. It needs no power and no signal.

## Crafting

```
P P P
P B P
P P P
```

P is a Steel Plate and B is a [Bimetallic Strip](Bimetallic_Strip.md).

## Using it

Build it into the shell of the reactor in place of one wall block. It counts as shell. One is enough; more do no harm.

When the core passes **850 Tu**, every [Control Rod Assembly](Control_Rod.md) drops all the way in at once. The core lets the rods out again to their targets once it has fallen to **650 Tu**.

## What it does not do

A SCRAM ends the chain, not the heat. The fuel keeps heating the core as decay heat afterward ([Fission Reactor](Fission_Reactor.md#stopping-it)), so the cooling loop must keep working. A core without a scram trip is stopped only by hand or by a [Controller](Controller.md).
