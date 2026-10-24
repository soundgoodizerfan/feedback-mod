# Condenser

Turns steam back into water. It's a [Boiler](Boiler.md) in reverse: the heat the boiler put into the steam comes back out, into the condenser.

## Crafting

```
C T C
T   T
C T C
```

C is a Copper Ingot and T is Copper Tubing (two [Copper Plates](Mechanical_Hammer.md) stacked make two Tubing).

## Using it

It **pulls steam from anything touching it**, the way a [Steam Engine](Steam_Engine.md) does. It takes steam of any temperature, live or an engine's exhaust. It offers its **water** to anything that draws from it: a [Pump](Pump.md), a bucket, another mod's pipe.

Both tanks hold 1000 mB.

## It has to stay cool

Condensing gives heat back. Every mB of steam it condenses warms the condenser, and it **only condenses while it's below boiling (120 Tu)**. At boiling point it condenses nothing at all.

- At full speed it condenses **10 mB a tick**, one engine's exhaust.
- Left on its own it can only lose heat to the air, and that's slow: it settles near boiling and trickles about **a quarter of an mB a tick**.

So **getting the heat out is your job.** Anything that takes heat out of a block works. A [Heat Exchanger](Heat_Exchanger.md) to something cold does, and so does a Heat Exchanger to the **boiler itself**, which hands the heat back to where it came from.

## Closing the loop

Boiler → Steam Engine → Condenser → Pump → Boiler.

The engine's exhaust goes to the condenser, the pump sends the water back to the boiler, and a heat exchanger between the condenser and the boiler recovers the heat. Built that way, the boiler never needs a water bucket again.

Condensing live steam straight from a boiler works too. It's just wasteful.
