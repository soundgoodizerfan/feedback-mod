# Timer

A wind-up timer. It switches the blocks it touches **on** when you start it, and **off** when it runs out. It's the first automation in the game.

## Crafting

```
P C P
P R P
```

P is a [Copper Plate](Mechanical_Hammer.md), C is a [Small Cog](Cogs.md) and R is Redstone Dust.

## Using it

- **Sneak-right-click** to choose how long a wind lasts: **2, 5, 10 or 20 seconds**.
- **Right-click** to wind it and start it. Everything switchable **touching** it turns **on**.
- When it runs out it ticks, and everything touching it turns **off**.
- Right-click while it's running to stop it early.

It works on any switchable block it's touching: a [Clutch](Clutch.md), [Valve](Valve.md), [Damper](Damper.md) and so on. There's no wiring; whatever it's touching is what it controls.

## The catch

A timer doesn't know whether the job is done. It only knows how long you told it to wait. If your [Water Wheel](Water_Wheel.md) is a bit faster today, or the line takes a bit longer to spin up, the same wind gives a different number of hammer blows. Usually you get plates. Sometimes you get foil.

It's still a big step up from doing it by hand. When you want the machine to stop because the **job** is done rather than because time ran out, you need sensors and a [Controller](Controller.md).

## Typical setup

Water wheel → shaft → **Clutch with a Timer next to it** → Crank Linkage → Mechanical Hammer. Put an ingot on the anvil, wind the timer, and come back to a plate (usually).
