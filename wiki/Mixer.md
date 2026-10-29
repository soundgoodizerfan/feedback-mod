# Mixer

A drum that blends loose powders for the [Reloading Press](Reloading_Press.md). It turns on a vertical [Shaft](Shaft.md); no screen.

## Crafting

```
P   P
P B P
P S P
```

P is a Wrought Iron Plate, B is a [Bearing](Bearing.md) and S is a Shaft.

## Using it

- **Right click with a powder** (gunpowder, blaze powder, redstone dust) pours one in, up to a full charge of 8. A coil's redstone drive cannot be mixed with a gas powder.
- **Right click empty-handed** draws the batch out as a **Powder Blend**. Put it in a charge slot of the press.

With [Jade](https://modrinth.com/mod/jade) you can see what is in the drum, how mixed it is, and its friction heat before you draw it. The drum is open, so no instrument is needed for the load and the mix; the heat reads as any temperature does.

## Turning

The drum counts turns: **0.00625 for every RPM each tick**, so a 16 RPM [Steam Engine](Steam_Engine.md) gives 0.1 a tick. Nothing counts below a crawl.

- **Mixing.** The share of the load that is mixed through rises evenly to full at **40 turns** (20 seconds at 16 RPM). Draw it then and everything is usable. Draw it early and you get only the mixed share of each powder, rounded; the rest is lost, so a failed batch still eats its powder.
- **Adding powder** mid-turn spreads the turns already done over the bigger load, so topping up sets the mix back.
- **Grinding.** Past 40 turns the mix starts to come apart again, falling to half over the next **60 turns** and staying there. Draw anywhere in between and you get that share. The sweet spot is 40 turns and no more.
- **Friction heat.** Only past 40 turns, each turn adds **0.5 Tu** to the drum. The drum doesn't shed it while stopped, so a blend left in a drum that was over-run keeps its heat. When room temperature plus that heat reaches the **lowest ignition temperature of any powder in the load**, the whole load goes up with the force of the powder in it, taking the drum with it.

The drum does not know when it is done. Watch the mix and the heat in Jade, and draw it at the line.
