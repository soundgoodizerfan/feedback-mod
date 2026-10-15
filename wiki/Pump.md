# Pump

Moves fluid from its back to its front while its shaft turns. Any fluid: water, steam, molten metal, another mod's oil.

## Crafting

```
S S S
T H T
S S S
```

S is a [Steel Ingot](Steel.md), T is Copper Tubing (two [Copper Plates](Mechanical_Hammer.md) stacked make two) and H is a [Shaft](Shaft.md).

## Placing

Place it **against the block it should draw from**. Its front faces away from that block and pushes into whatever is there.

The shaft can't share the fluid's direction, so it goes in **across** it: from above or below on a pump lying sideways, and from the east or west side on one pumping straight up or down.

## How much it moves

**0.5 mB per tick for every RPM.** It's a flat load of **8 Su**, however much it's moving.

| Shaft | Moves |
| --- | --- |
| [Water Wheel](Water_Wheel.md), one side of flow (4 RPM) | 2 mB/t |
| [Steam Engine](Steam_Engine.md) (16 RPM) | 8 mB/t |
| 20 RPM | 10 mB/t, enough to keep a [Boiler](Boiler.md) fed for one engine |
| [Hand Crank](Hand_Crank.md) (32 RPM) | 16 mB/t |

## Where it draws from

- **A tank** behind it: a [Condenser](Condenser.md), a boiler, a crucible, another mod's tank.
- **A source block** behind it, if it's a fluid that **renews itself**. Water does, so a pump in a pool of water (two or more sources) draws water forever without draining it. Lava doesn't renew, so a pump won't make lava from nothing.

If there's nowhere for the fluid to go in front, it doesn't pump.

## Notes

- It doesn't care what's above it. Lifting fluid ten blocks up costs the same as moving it sideways.
- It has no switch. Put a [Clutch](Clutch.md) on its shaft.
