# Valve

An on/off cutoff that you put in the middle of a line. It works on **air** (from a [Powered Bellows](Powered_Bellows.md)) and on **fluids**, including **steam** from a [Boiler](Boiler.md).

## Crafting

An Iron Ingot, a [Copper Plate](Mechanical_Hammer.md) and another Iron Ingot in a row.

```
I C I
```

## Placing

A valve has a direction. **Whatever is behind it feeds in, and whatever is in front of it gets fed.** When you place it, the front points away from the block you clicked. The four other sides don't connect to anything.

Typical uses:

- **Bellows → Valve → Firebox**: cut the air to a fire without breaking anything.
- **Boiler → Valve → [Steam Engine](Steam_Engine.md)**: shut off steam so the engine stops pulling it.

## Using it

**Right-click** to open or close it. You'll hear a trapdoor-like clunk. New valves are placed **open**.

- **Open**: everything passes straight through.
- **Closed**: air blown into it is simply lost, and anything trying to draw fluid through it gets nothing.

The valve doesn't hold anything itself, so there's nothing to drain and nothing to lose when you break it.

## Automating it

A [Controller](Controller.md) can open and close a valve. Point a *Flip Clutch* card in your [Programmer](Programmer.md) program at it, the same way you would a [Clutch](Clutch.md). Pair that with a [Temperature Sensor](Temperature_Sensor.md) and you have a thermostat that cuts the air to a fire when it gets too hot.

Keep in mind that closing a valve doesn't put out a fire or empty a boiler. It only stops *more* from arriving, and what's already there carries on until it runs down.
