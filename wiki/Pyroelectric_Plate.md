# Pyroelectric Plate

A thin crystal plate that answers only to change in temperature. Set against a hot or cold block, it gives a [Controller](Controller.md) reading for as long as that block warms or cools.

## Crafting

Shapeless: one **Plate, Cut** of a pyroelectric crystal + two metal wires, anywhere in the grid. The plate carries its crystal's name (a *Tourmaline Pyroelectric Plate*). [Tourmaline](Materials.md#gems) is the pyroelectric crystal.

## Using it

Place it against any block that has a temperature: a [Crucible](Crucible.md), [Boiler](Boiler.md), [Heat Exchanger](Heat_Exchanger.md), a kiln or a furnace. It is a flat 4/16 plate and can sit against several such blocks at once.

About twice a second it compares each touching block's temperature with the last reading and keeps the fastest change, warming or cooling, in Tu per second. A block holding steady at any temperature gives nothing.

- **Controller**: a signed *heating rate* reading in Tu per second, in steps of **0.5 Tu/s** for tourmaline. Warming is positive, cooling negative. See [Sensors](Sensors.md) and [Programming](Programming.md).
- **Sneak and right-click**: reads the rate to you as an [instrument](Instruments.md) would.
- **Jade** names the state: Warming fast, Warming, Still, Cooling, Cooling fast.

## How it works

The plate makes charge from change of temperature, not from temperature itself. A weaker pyroelectric material gives less charge for the same change, so it resolves the rate in coarser steps.

It gives no power and controls nothing; it only reports. Use it where a thermometer reading would not tell you the thing you need, such as a furnace that has just been lit, a vessel that has just been fed cold stock, or a [Heating Coil](Heating_Coil.md) that has been switched off.
