# Dynamo

Turns [rotation](Rotation.md) into [electricity](Electricity.md). A coil of copper wire spun by a shaft: the faster the shaft turns, the higher the voltage.

## Crafting

```
S W S
W I W
S H S
```

S is a [Steel Ingot](Steel.md), W is [Copper Wire](Wire_Drawer.md), I is a Wrought Iron Ingot and H is a [Shaft](Shaft.md).

A Dynamo and a [Motor](Motor.md) are the same machine. Put either one in a crafting grid on its own to get the other.

## Placing

Place it **against the end of a shaft**. That face is its input, and it reads that shaft's speed. Any other face connects to [cable](Copper_Cable.md) and machines.

It doesn't spin on screen, but it is turning.

## Windings

**Sneak-right-click with an empty hand** to cycle the windings: coarse, fine, dense, and round to coarse again. The [Motor](Motor.md) has only coarse and fine.

| Winding | Volts per RPM | Holds its voltage up to | At a [Steam Engine](Steam_Engine.md)'s 16 RPM |
| --- | --- | --- | --- |
| **Coarse** | 0.094 V | 8 A | 1.5 V (LV) |
| **Fine** | 0.375 V | 2 A | 6 V (MV) |
| **Dense** | 1.5 V | 42 A | 24 V (HV) |

Coarse and fine carry the same power at the same speed. Coarse gives you lots of current at a low voltage; fine gives you a high voltage and little current. Dense has many more turns: a high voltage and a heavy current together. Pick the one your load wants.

Voltage is **shaft speed times the winding's figure**, and nothing else:

| Shaft | Coarse | Fine | Dense |
| --- | --- | --- | --- |
| [Water Wheel](Water_Wheel.md), one side of flow (4 RPM) | 0.4 V | 1.5 V | 6 V |
| Water Wheel, four sides (16 RPM) | 1.5 V | 6 V | 24 V |
| Steam Engine (16 RPM) | 1.5 V | 6 V | 24 V |
| [Hand Crank](Hand_Crank.md) (32 RPM) | 3 V | 12 V | 48 V |
| [Gas Turbine](Gas_Turbine.md) (240 RPM) | 22.5 V | 90 V | 360 V (EHV) |

To get a higher voltage, **gear the shaft up** with [cogs](Cogs.md) or [pulleys](Pulleys.md). A slow river gives a low voltage, and a shaft that's slowing down gives a falling one.

## What it costs the shaft

A dynamo nobody draws from costs its shaft nothing. Every amp drawn costs the shaft **the winding's volts-per-RPM figure in Su**: about 0.09 Su per amp on coarse, 0.38 on fine, 1.5 on dense. Fully loaded, coarse and fine cost **0.75 Su**, which is very little next to a [Mechanical Hammer](Mechanical_Hammer.md)'s 80. A fully loaded dense winding costs **63 Su** (42 A at 1.5 Su per amp), so only a turbine-class shaft such as a Gas Turbine can drive it.

Drawing more than the winding's amps makes the voltage **sag**, down to a quarter at twice the rated current. It never draws more than twice its rating, and it's never damaged by it.

## The dense winding

At a Gas Turbine's speed the dense winding gives about 360 V, in the [EHV](Electricity.md#volts-and-amps) band, at up to 42 A. That needs [cable](Copper_Cable.md) built for the current and insulated to EHV (quartz or PTFE); anything less burns off its coat and shocks anyone who touches it ([Hazards](Hazards.md#shock)). It is the supply for an [Arc Furnace](Arc_Furnace.md).

## Uses

- A steady supply that never runs out, from any shaft you already have.
- The fine winding at a steam engine's speed is the reference voltage for a [Round Charger](Round_Charger.md): it charges coil rounds to exactly their nominal 6 V.
- With a [Motor](Motor.md) on the other winding, a dynamo is how you **convert voltage**. See [Electricity](Electricity.md#changing-voltage).
