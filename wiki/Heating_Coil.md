# Heating Coil

Turns [electricity](Electricity.md) into [heat](Heat.md). Set it under a [Crucible](Crucible.md), [Boiler](Boiler.md) or anything else you'd put on a [Firebox](Firebox.md), and it heats it.

## Crafting

```
C W C
W   W
C W C
```

C is Ceramic and W is Fine Wire of the element metal:

- **Copper Heating Coil**: [Copper Fine Wire](Wire_Drawer.md)
- **Iron Heating Coil**: Wrought Iron Fine Wire

## Using it

Put the vessel on top and run [cable](Copper_Cable.md) to the coil. It asks for **2 A**. It has no switch; put a [Breaker](Breaker.md) in its line to turn it on and off by hand or with a [Controller](Controller.md).

The power it turns into heat is **volts × amps**. So the coil is as hot as its supply:

| Supply | Small crucible settles near |
| --- | --- |
| [Dynamo](Dynamo.md), fine winding, at a [Steam Engine](Steam_Engine.md)'s speed (6 V, 2 A) | 290 Tu |
| The same dynamo on a [Hand Crank](Hand_Crank.md) (12 V) | 560 Tu |

The element runs a little hotter than what it's heating, and heats its vessel the same way a fire does: fast while there's a big difference, slower as they get close.

## Not like a fire

- **It stops almost instantly.** The element is light, so cut the power and it has almost nothing left to give. A fire's character is its fuel; a coil's is its supply.
- **It's steady and controllable.** No fuel to vary, no air to manage.
- **It isn't hot enough to smelt.** Nothing in the game today pushes a coil to metalworking heat. It's for low, precise jobs: tempering, preheating, raising steam.

## The element's limit

Each element metal has a temperature it can run at forever:

| Coil | Runs safely up to |
| --- | --- |
| Copper | 300 Tu |
| Iron | 500 Tu |

Above that, the element **wears**, faster the further over, down to half condition. A worn element puts out less heat. It never breaks.

So the iron coil is the upgrade: the same machine with a better element you can point at.
