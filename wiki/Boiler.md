# Boiler

Turns heat into **steam**. Sits over a fire like a [Crucible](Crucible.md) does. Its steam drives a [Steam Engine](Steam_Engine.md).

**A boiler with nowhere to send its steam builds pressure, and can explode.** Read [Pressure](#pressure) before you put a valve on one.

## Crafting

```
P   P
P P P
```

P is a [Steel Plate](Mechanical_Hammer.md).

## Using it

1. Put it on top of a [Firebox](Firebox.md), over lava, or on a [Heating Coil](Heating_Coil.md).
2. **Fill it with water**: right-click it with a Water Bucket, or feed it with a [Pump](Pump.md). It holds 4000 mB of water and 4000 mB of steam.
3. Once it reaches **120 Tu**, it starts turning water into steam.

A boiler needs far less heat than metalwork. It starts boiling well below a red glow, so even a modest fire runs it.

## How much steam

At full output it makes **10 mB of steam a tick**, using 10 mB of water. That keeps **one Steam Engine** running.

Making steam **uses up the boiler's heat**: 20 Work for every mB. A weak fire holds the boiler just at boiling and makes a trickle. Full output needs a fire about **330 Tu hotter** than the boiler, so roughly **450 Tu** or more. Logs (760 Tu) are plenty. If your engine is starving, the fire is too weak, not the boiler too small.

## Getting steam out

There are no pipes. A [Steam Engine](Steam_Engine.md) placed **touching** the boiler draws steam from it directly. Put a [Valve](Valve.md) between them to cut the steam off. A [Condenser](Condenser.md) can turn the engine's exhaust back into water for the pump to return.

## Pressure

A boiler holds pressure in Pu:

- **Steam it's holding:** 1 Pu for every 1000 mB, so a full steam tank is **4 Pu**.
- **Heat with nowhere to go:** while the steam tank is **full**, the fire keeps pushing and every Tu the boiler climbs past 120 adds **0.004 Pu**.

A boiler sending its steam somewhere spends its heat boiling water and sits near 120 Tu, however big the fire. A **shut** one (valve closed, engine gone, nothing drawing) has no outlet. Its steam tank fills, and then the fire drives it toward the fire's own temperature, and the pressure with it:

| Pressure | What happens |
| --- | --- |
| up to 3.5 Pu | nothing |
| above 3.5 Pu | **Weeping**: the seams leak steam (6 mB a tick for every Pu over), hiss, and **scald** anyone within about 1.5 blocks for 1 heart |
| above 5 Pu | each tick there's a chance it **bursts**, climbing the further over it is |
| 7 Pu | it **bursts** |

A shut boiler on a fire at 870 Tu or hotter reaches 7 Pu. That's any coal or charcoal fire, or lava. Even a log fire gets it well into the danger zone.

A burst is an explosion that wrecks blocks around it and scalds everyone within 4 blocks for 4 hearts. Steam gets in under gloves; only Fire Resistance helps. See [Hazards](Hazards.md#steam).

Weeping lets a little pressure off, and a boiler only slightly over the line may settle there, hissing. One on a real fire won't. **The warning is the hiss and the cloud.** Open the line or put the fire out.

A boiler that's merely full and idle sits at 4 Pu and weeps a little. That's harmless; it's the fire under a shut boiler that kills it.

## Notes

- Remember to refill the water, or close the loop with a [Condenser](Condenser.md) and [Pump](Pump.md).
- No instrument reads boiler pressure yet. Watch for the hiss.
