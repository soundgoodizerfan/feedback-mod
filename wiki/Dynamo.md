# Dynamo

Turns [rotation](Rotation.md) into [electricity](Electricity.md). A coil of copper wire spun by a shaft: the faster the shaft turns, the higher the voltage.

## Crafting

```
S W S
W I W
S H S
```

S is a [Steel Ingot](Steel.md), W is [Copper Wire](Wire_Drawer.md), I is an Iron Ingot and H is a [Shaft](Shaft.md).

A Dynamo and a [Motor](Motor.md) are the same machine. Put either one in a crafting grid on its own to get the other.

## Placing

Place it **against the end of a shaft**. That face is its input, and it reads that shaft's speed. Any other face connects to [cable](Copper_Cable.md) and machines.

It doesn't spin on screen yet, but it is turning.

## Windings

**Sneak-right-click with an empty hand** to switch between the two windings:

| Winding | Volts per RPM | Holds its voltage up to | At a [Steam Engine](Steam_Engine.md)'s 16 RPM |
| --- | --- | --- | --- |
| **Coarse** | 0.094 V | 8 A | 1.5 V (LV) |
| **Fine** | 0.375 V | 2 A | 6 V (MV) |

Both windings carry the same power at the same speed. Coarse gives you lots of current at a low voltage; fine gives you a high voltage and little current. Pick the one your load wants.

Voltage is **shaft speed times the winding's figure**, and nothing else:

| Shaft | Coarse | Fine |
| --- | --- | --- |
| [Water Wheel](Water_Wheel.md), one side of flow (4 RPM) | 0.4 V | 1.5 V |
| Water Wheel, four sides (16 RPM) | 1.5 V | 6 V |
| Steam Engine (16 RPM) | 1.5 V | 6 V |
| [Hand Crank](Hand_Crank.md) (32 RPM) | 3 V | 12 V |

To get a higher voltage, **gear the shaft up** with [cogs](Cogs.md) or [pulleys](Pulleys.md). A slow river gives a low voltage, and a shaft that's slowing down gives a falling one.

## What it costs the shaft

A dynamo nobody draws from costs its shaft nothing. Every amp drawn costs the shaft **the winding's volts-per-RPM figure in Su**: about 0.09 Su per amp on coarse, 0.38 on fine. Fully loaded, either winding costs **0.75 Su**, which is very little next to a [Mechanical Hammer](Mechanical_Hammer.md)'s 80.

Drawing more than the winding's amps makes the voltage **sag**, down to a quarter at twice the rated current. It never draws more than twice its rating, and it's never damaged by it.

## Uses

- A steady supply that never runs out, from any shaft you already have.
- The fine winding at a steam engine's speed is the reference voltage for a [Round Charger](Round_Charger.md): it charges coil rounds to exactly their nominal 6 V.
- With a [Motor](Motor.md) on the other winding, a dynamo is how you **convert voltage**. See [Electricity](Electricity.md#changing-voltage).
