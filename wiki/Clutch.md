# Clutch

A shaft coupling you can disconnect. Everything past it stops being driven.

## Crafting

```
P P
S S
```

P is a [Copper Plate](Mechanical_Hammer.md) and S is a [Shaft](Shaft.md).

## Placing

Put it inline on a shaft. The side you place it **against** is the **driving** side; the far side is the output.

## Using it

**Right-click** to disengage or re-engage it. New clutches are placed **engaged**.

A [Timer](Timer.md) touching it, or a [Controller](Controller.md) with a *Switch* card, can switch it for you.

## What disengaging actually does

A clutch **doesn't brake**. It stops *driving* the far side. Whatever is downstream keeps turning on its own momentum and coasts down, quickly if it's loaded, slowly if it's light or has heavy things on it. A [hammer](Mechanical_Hammer.md) behind a clutch can land a blow or two after you throw it, so allow for that.

The disengaged clutch stays part of the driving side, so the power source keeps running normally.

## Uses

- Stopping one machine without stopping the whole line.
- Automating stops with a [Timer](Timer.md) or [Controller](Controller.md).
- An automatic version that trips on overspeed is the [Overspeed Trip](Overspeed_Trip.md).
