# Copper Cable

The wire that connects [electrical](Electricity.md) blocks together.

## Crafting

Cable comes in four **gauges**: thin, standard, heavy and bus bar. They are the same block with a thicker or thinner core, and each takes a different arrangement of [Copper Wire](Wire_Drawer.md) (or another metal's):

| Gauge | Grid | Makes |
| --- | --- | --- |
| Thin | 3 fine wires in a row | 3 cables |
| Standard | 3 wires in a row | 3 cables |
| Heavy | 4 wires in a 2x2 square | 1 cable |
| Bus bar | 1 plate | 1 bus bar |

```
W W W
I I I
```

The grid shows standard cable: W is a wire and I is the optional insulating row. Thin cable takes fine wire; heavy cable takes the 2x2 square with a two-wide row under it; a bus bar takes one plate with one item under it. Each gauge carries four times the current of the one before it, for four times the metal.

The row under the conductors is optional. Without it the cables are **bare**. Put a row of an insulating material under them (all the same band, as wide as the conductors) and the cables come out **insulated** to that material's band. Leather, wool and paper insulate to LV; terracotta and brick to MV; glass to HV; quartz glass to UHV; quartz to EHV. [Polymers](Materials.md#polymer-insulation) do too: bitumen to LV, polypropylene, PVC and soul resin to MV, polyethylene and synthetic rubber to HV, PTFE to Trunk. Their *Insulation* form works as the coat, and so does any other item of the material.

A bus bar is named *Copper Bus Bar* (or the metal's name); the other gauges are all *Copper Cable*. Hover a cable to read its gauge. [Electrified Fence](Electrified_Fence.md) posts made from a cable take its gauge, and look the same at every gauge.

## Placing

Cable is a thin core, not a full block, and you can walk straight through it. It connects to anything electrical touching any of its faces: other cable, sources, machines. There's nothing to configure.

You can bolt a [Current Sensor](Instruments.md#current-sensor) onto any face. If you break the cable, the sensor drops too.

## Bare cable arcs

Bare cable (never insulated, or burnt) shocks, and it also **arcs across one block of air** to another bare cable in a straight line. The two lines become one while both are bare, with the usual results: the higher voltage wins, the lower side's machines wear from over-voltage. Sparks and a crackle show a gap that is carrying. Put a solid block in the gap, or insulate either cable, to end it.

## How much it carries

Copper figures, per gauge:

| Gauge | Rating | Voltage lost per amp, per block |
| --- | --- | --- |
| Thin | **1 A** | **0.2 V** |
| Standard | **4 A** | **0.05 V** |
| Heavy | **16 A** | **0.0125 V** |
| Bus bar | **64 A** | **0.003125 V** |

Other metals scale these by their own ampacity and drop: a metal that carries half as much as copper has half the rating at every gauge.

Up to its rating a cable stays at room temperature forever and never wears out, no matter how long it runs. A standard copper cable's 4 A is four [Electromagnets](Electromagnet.md), or two [Heating Coils](Heating_Coil.md), but only a fraction of what a coarse [Dynamotor](Dynamotor.md) can deliver; heavy cable and bus bar are for that.

Every block of cable also costs the line voltage, so long runs deliver less to the far end. See [Electricity](Electricity.md#how-a-line-behaves).

## Overloading

Ask too much of a cable and it heats up. Look at it with Jade (with a [Thermometer](Instruments.md#thermometer) in your inventory, if you want a number) to watch it climb. Overloaded cable doesn't heat forever; it settles about **50 Tu above the room for every amp over its rating**, at any gauge, within a couple of seconds. What happens next depends on how hot that is:

| Over the rating | Settles at (room 20 Tu) | What happens |
| --- | --- | --- |
| nothing | room temperature | nothing, ever |
| 1 A | 70 Tu | warm, harmless |
| over 1.6 A | over 100 Tu | **the insulation burns off** |
| over 5.6 A | over 300 Tu | **the copper starts to degrade** |
| 11.6 A | 600 Tu | degrades fast: gone in about 25 s |

1. **The insulation burns off** above 100 Tu, or the moment the cable sees a voltage above its insulation's band (LV paper on a 6 V line, say). The cable still works exactly as before, but now it's **bare**, and touching bare cable while current is flowing will hurt you. See [Hazards](Hazards.md#shock).
2. **The copper itself starts to suffer** above 300 Tu, copper's limit. The cable degrades, faster the hotter it is: it carries less before overheating, and it loses more voltage along the line. A damaged cable makes itself worse under the same load, so this gets away from you.
3. **It burns out.** A puff of smoke, a hiss, and the cable is gone. You don't get it back.

Nothing on this list happens suddenly. You'll see it coming if you look.

Taking the load off lets the cable cool back down, but **damage doesn't heal**: a scorched cable stays bare, and a degraded one stays degraded. Cable cannot be repaired. Break it and lay new cable.

## Superconducting cable

Cable made from Redstone Superconductor Wire carries up to its **critical current** with no voltage drop. At standard gauge the critical current is **4 A** for the poor grade, **10 A** for fair and **25 A** for the best grade; other gauges scale like copper. Past it the cable **quenches**: it goes resistive, with a heavy voltage drop, and heats on what it carries. It recovers once it has cooled to near room temperature and carries no more than its rating. Left heating, it degrades and burns out like any wire ([Overloading](#overloading)). All figures are placeholders.

## Protecting it

- A [Breaker](Breaker.md) built from cables of a gauge trips at exactly that gauge's rating.
- On standard copper cable, a **tin** [Fuse](Fuse.md) blows near 2.8 A; **zinc** near 5.3 A, before the insulation goes.
- If a cable is getting warm, split the load across a second cable run, or move some machines onto their own source.
- A cable that's already bare is a warning that it was overloaded at some point. Find out why before you replace it.
