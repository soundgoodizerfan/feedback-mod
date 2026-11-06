# Redstone Bridges

Two blocks join Feedback machines to vanilla redstone, and every machine with a switch answers to redstone on its own.

## Redstone on a switch

Any Feedback machine a [Controller](Controller.md) can start and stop also starts and stops from redstone. A redstone signal arriving at the machine switches it on; the signal dropping switches it off. Nothing needs to be fitted. A machine that a controller is also driving takes whichever change arrived last.

## Redstone Emitter

Turns a data signal into redstone. A controller, timer or sensor program switches it like any other machine; when it is on it puts a full redstone signal out of every face. Use it to drive a piston, lamp or door from a [sensor](Sensors.md).

```
P R P
R C R      P wrought iron plate, R redstone, C copper plate
P R P
```

## Analog Adapter

A fitting for a machine with a variable setting. Right-click a face of the machine with it; redstone power arriving at that face then sets the setting from none to full in fifteen even steps: power 15 is full, power 0 is none. The [Valve](Valve.md) has one, its opening, and scales the flow it passes. Take the adapter off with a crowbar and the machine returns to full. A machine with no variable setting ignores it.

```
P R
R P        P wrought iron plate, R redstone
```

## See also

- [Controller](Controller.md) for starting and stopping machines from sensors.
- [Sensors](Sensors.md) for what a controller can read.
