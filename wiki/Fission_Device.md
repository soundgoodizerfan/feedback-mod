# Fission Device

A plutonium core inside a shell of conventional explosive, set off by a redstone signal. It is a weapon: one huge blast and a field of radioactive slag.

## Crafting

```
S T S
P P P
S T S
```

S is a Steel Plate, T is TNT and P is a Plutonium Ingot ([Nuclear Fuel](Nuclear_Fuel.md#reprocessing)). Craft it at the [Engineering Table](Engineering_Table.md) with a driver in the tool row and 4 machine bolts ([Fasteners](Fasteners.md)), holding Nuclear Treatise I: Reactors and Fuel ([Treatise](Treatise.md)).

## Using it

Place it and give it a redstone signal. It goes off the moment it is powered. Breaking it by hand drops it unharmed.

## Where it is allowed

A server switch, **nuclearArms** in the nuclear config file (`feedback-nuclear.toml`), allows it. The switch is off by default. Where it is off, the device sits inert: right-click it and it says **The device is inert here.**, and a redstone signal does nothing.

## What it does

The device is removed and a very large blast follows, which breaks blocks only where the world's game rules allow blast damage, like any other explosion. The ground around the crater is left as **Core Slag**: it drops nothing and gives off radiation for hours of play ([Radiation](Radiation.md#the-reactor)).
