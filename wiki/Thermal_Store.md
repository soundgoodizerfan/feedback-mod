# Thermal Store

A brick tank of salt that holds heat. A fire, [Heating Coil](Heating_Coil.md) or arc head under it charges it, and a [Boiler](Boiler.md) on top draws it down into steam for a [Steam Turbine](Steam_Turbine.md).

## Crafting

```
B B B
B S B
B B B
```

B is a Bricks block and S is Salt.

## Using it

Set it over a heat source, and set a Boiler on top of it. The store warms toward the source and the Boiler heats from the store. A [Firebox](Firebox.md), a coil or an [arc head](Arc_Furnace.md#the-arc-head) all charge it.

## How it works

- It holds heat, not electricity.
- It cannot get hotter than what charges it.
- It cools slowly to the air while it holds heat.
- Only part of the heat comes back out, so the round trip from fuel or power to steam is poor.

A coil charging a store from spare electricity and a Boiler drawing it down at night is the use. See [Heat](Heat.md) for how heat moves between touching blocks.
