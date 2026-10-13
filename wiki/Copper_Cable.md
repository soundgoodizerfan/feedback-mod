# Copper Cable

The wire that connects [electrical](Electricity.md) blocks together.

## Crafting

Three [Copper Wire](Wire_Drawer.md) across the top row, three Paper across the middle. Makes three cables. The paper is the insulation.

```
W W W
P P P
```

## Placing

Cable is a thin core, not a full block, and you can walk straight through it. It connects to anything electrical touching any of its faces: other cable, sources, machines. There's nothing to configure.

You can bolt a [Current Sensor](Current_Sensor.md) onto any face. If you break the cable, the sensor drops too.

## How much it carries

Each cable can carry a few [Electromagnets](Electromagnet.md)' worth of current before it starts warming up. Below that it stays at room temperature forever and never wears out, no matter how long it runs.

Every block of cable also costs the line a little voltage, so long runs deliver less to the far end. See [Electricity](Electricity.md#how-a-line-behaves).

## Overloading

Ask too much of a cable and it heats up. Look at it with Jade (with a [Thermometer](Thermometer.md) in your inventory, if you want a number) to watch it climb. Overloaded cable doesn't heat forever; it settles at a temperature that depends on how far over you are. What happens next depends on how hot that is:

1. **The insulation burns off.** The cable still works exactly as before, but now it's **bare**, and stepping into bare cable while current is flowing will hurt you. It doesn't kill you outright, but don't stand in it.
2. **The copper itself starts to suffer.** Past a certain heat the cable degrades: it carries less before overheating, and it loses more voltage along the line. A damaged cable makes itself worse under the same load, so this gets away from you.
3. **It burns out.** A puff of smoke, a hiss, and the cable is gone. You don't get it back.

Nothing on this list happens suddenly. You'll see it coming if you look.

Taking the load off lets the cable cool back down, but **damage doesn't heal**: a scorched cable stays bare, and a degraded one stays degraded. There's no way to repair cable at the moment. Break it and lay new cable.

## Tips

- If a cable is getting warm, split the load across a second cable run, or move some machines onto their own source.
- A cable that's already bare is a warning that it was overloaded at some point. Find out why before you replace it.
