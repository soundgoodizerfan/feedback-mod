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

It trips at **48 RPM**. A shaft turning at the speed of its own source (a [Hand Crank](Hand_Crank.md) at 32, a [Steam Engine](Steam_Engine.md) at 16, a [Water Wheel](Water_Wheel.md) at up to 16) will **never** trip it. It only lets go when you've **geared up** past that with [cogs](Cogs.md) or [pulleys](Pulleys.md). The trip speed is fixed; you can't adjust it.

## Notes

- It doesn't slow anything down or hold a speed. It's all-or-nothing, like a fuse for rotation.
- Tripping does it no harm at all. It never wears out, however often it fires.
