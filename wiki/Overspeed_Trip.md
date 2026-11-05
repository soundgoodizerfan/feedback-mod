# Overspeed Trip

A [Clutch](Clutch.md) with spinning weights inside that **throws itself open** if its shaft turns too fast. It's a safety cutoff for gear trains that might run away.

## Crafting

Two [Steel Ingots](Steel.md) above a Clutch.

```
S S
 C
```

## How it works

Place it inline on a shaft exactly like a clutch. The side you place it against is the driving side. Normally it just passes rotation through.

If the driving shaft spins **faster than the trip is built for**, the weights fly out and it **disengages**, the same as if you'd right-clicked a clutch. Everything downstream is cut off from the power and coasts down on its own momentum.

It **stays open** until someone resets it:

- **Right-click** it to close it again, or
- have a [Controller](Controller.md) flip it with a *Switch* card.

If the shaft is still spinning too fast when you reset it, it trips again immediately. That's your hint that whatever caused it is still going on.

## When it trips

It is built with a **light spring** that trips it at **48 RPM**. **Sneak and right-click** it to swap the spring: light (48 RPM), medium (120 RPM) or heavy (300 RPM), then back to light. A message shows the speed it now trips at. Three springs only; there is no finer setting.

A shaft turning at the speed of its own source (a [Hand Crank](Hand_Crank.md) at 32, a [Steam Engine](Steam_Engine.md) at 16, a [Water Wheel](Water_Wheel.md) at up to 16) never trips the light spring. A [Steam Turbine](Steam_Turbine.md) at 240 RPM trips the light and medium springs at once and needs the heavy one. It lets go when you've **geared up** past the fitted spring with [cogs](Cogs.md) or [pulleys](Pulleys.md).

## Notes

- It doesn't slow anything down or hold a speed. It's all-or-nothing, like a fuse for rotation.
- Tripping does it no harm at all. It never wears out, however often it fires.
