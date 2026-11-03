# Mixer

A drum that blends materials, powders and molten metals among them, into a mixture. It turns on a vertical [Shaft](Shaft.md) and has a screen. Powder blends feed the [Reloading Press](Reloading_Press.md).

## Crafting

```
P   P
P B P
P S P
```

P is a Wrought Iron Plate, B is a [Bearing](Bearing.md) and S is a Shaft.

## Using it

- **Right click with any item that is a material** pours one in, up to a full load of **8 parts**. A coil's redstone drive cannot be mixed with a gas powder when the drum holds only powders.
- **Right click empty-handed** opens the screen.
- **A pipe** fills the drum with any fluid that has a material (molten metals among them). **144 mB** counts as one part, and fluid shares the 8-part limit with items. Buckets cannot fill it.

The screen has three input slots, an output slot, a **Draw** button, a tank gauge, and readings. Items in the input slots pour in one at a time while there is room. Share bars show what each part makes up of the drum, in quarters. A word reading gives how well it is mixed, and the friction heat reads as a temperature.

**Draw** keeps the mixed share of each part (see Turning), then the mixture table decides the result:

- A row that makes an item puts it in the output slot, one per part of the mixture.
- A row that makes a fluid holds it at the fluid port until a pipe drains it. Meanwhile the drum refuses new fluid and Draw is blocked.
- With no row, the output is a **Mixture** item. Its tooltip gives rough shares (*mostly* one part, *some* of another, *a little* of a third) and lists any fluid parts. Powder Mixtures go in a charge slot of the press; the press refuses a Mixture with fluid parts.
- Black powder comes out as gunpowder items.

Hoppers and pipes can feed the inputs and take only what was drawn, never the drum's contents.

With [Jade](https://modrinth.com/mod/jade) you can see what is in the drum, how mixed it is, and its friction heat before you draw it. The drum is open, so no instrument is needed for the load and the mix; the heat reads as any temperature does.

## Turning

The drum counts turns: **0.00625 for every RPM each tick**, so a 16 RPM [Steam Engine](Steam_Engine.md) gives 0.1 a tick. Nothing counts below a crawl.

- **Mixing.** The share of the load that is mixed through rises evenly to full at **40 turns** (20 seconds at 16 RPM). Draw it then and everything is usable. Draw it early and you get only the mixed share of each part, rounded; the rest is lost, so a failed batch still eats its parts.
- **Adding parts** mid-turn spreads the turns already done over the bigger load, so topping up sets the mix back.
- **Grinding.** Past 40 turns the mix starts to come apart again, falling to half over the next **60 turns** and staying there. Draw anywhere in between and you get that share. The sweet spot is 40 turns and no more.
- **Friction heat.** Only past 40 turns, each turn adds **0.5 Tu** to the drum. The drum doesn't shed it while stopped, so a blend left in a drum that was over-run keeps its heat. When room temperature plus that heat reaches the **lowest ignition temperature of any powder in the load**, the whole load goes up with the force of the powder in it, taking the drum with it. Only a drum of nothing but powders can ignite; any other part in the load removes the risk.

The drum does not know when it is done. Watch the mix and the heat in Jade, and draw it at the line.
