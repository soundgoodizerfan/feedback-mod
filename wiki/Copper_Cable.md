# Copper Cable

The wire that connects [electrical](Electricity.md) blocks together.

## Crafting

Three [Copper Wire](Wire_Drawer.md) in a row makes three **bare** cables. Put a row of an insulating material under them (all three the same band) and the cables come out **insulated** to that material's band. Leather, wool and paper insulate to LV; terracotta and brick to MV; glass to HV; quartz to UHV.

```
W W W
I I I
```

## Placing

Cable is a thin core, not a full block, and you can walk straight through it. It connects to anything electrical touching any of its faces: other cable, sources, machines. There's nothing to configure.

You can bolt a [Current Sensor](Current_Sensor.md) onto any face. If you break the cable, the sensor drops too.

## Bare cable arcs

Bare cable (never insulated, or burnt) shocks, and it also **arcs across one block of air** to another bare cable in a straight line. The two lines become one while both are bare, with the usual results: the higher voltage wins, the lower side's machines wear from over-voltage. Sparks and a crackle show a gap that is carrying. Put a solid block in the gap, or insulate either cable, to end it.

## How much it carries

| | |
| --- | --- |
| Rating | **4 A** |
| Voltage lost | **0.05 V per amp, per block** |

Up to 4 A the cable stays at room temperature forever and never wears out, no matter how long it runs. That's four [Electromagnets](Electromagnet.md), or two [Heating Coils](Heating_Coil.md), but only half of a coarse [Dynamo](Dynamo.md)'s 8 A.

Every block of cable also costs the line voltage, so long runs deliver less to the far end. See [Electricity](Electricity.md#how-a-line-behaves).

## Overloading

Ask too much of a cable and it heats up. Look at it with Jade (with a [Thermometer](Thermometer.md) in your inventory, if you want a number) to watch it climb. Overloaded cable doesn't heat forever; it settles about **50 Tu above the room for every amp over 4 A**, within a couple of seconds. What happens next depends on how hot that is:

| Current | Settles at (room 20 Tu) | What happens |
| --- | --- | --- |
| up to 4 A | room temperature | nothing, ever |
| 5 A | 70 Tu | warm, harmless |
| over 5.6 A | over 100 Tu | **the insulation burns off** |
| over 9.6 A | over 300 Tu | **the copper starts to degrade** |
| 15.6 A | 600 Tu | degrades fast: gone in about 25 s |

1. **The insulation burns off** above 100 Tu, or the moment the cable sees a voltage above its insulation's band (LV paper on a 6 V line, say). The cable still works exactly as before, but now it's **bare**, and touching bare cable while current is flowing will hurt you. See [Hazards](Hazards.md#shock).
2. **The copper itself starts to suffer** above 300 Tu, copper's limit. The cable degrades, faster the hotter it is: it carries less before overheating, and it loses more voltage along the line. A damaged cable makes itself worse under the same load, so this gets away from you.
3. **It burns out.** A puff of smoke, a hiss, and the cable is gone. You don't get it back.

Nothing on this list happens suddenly. You'll see it coming if you look.

Taking the load off lets the cable cool back down, but **damage doesn't heal**: a scorched cable stays bare, and a degraded one stays degraded. There's no way to repair cable at the moment. Break it and lay new cable.

## Protecting it

- A [Breaker](Breaker.md) trips at 4 A, exactly this cable's rating.
- A **tin** [Fuse](Fuse.md) blows near 2.8 A; **zinc** near 5.3 A, before the insulation goes.
- If a cable is getting warm, split the load across a second cable run, or move some machines onto their own source.
- A cable that's already bare is a warning that it was overloaded at some point. Find out why before you replace it.
