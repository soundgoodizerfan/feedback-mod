# Mixer

A drum that blends materials, powders and molten metals among them, into a mixture. Set over a fire it is also a heated vessel for alloying. It turns on a vertical [Shaft](Shaft.md) and has a screen. Powder blends feed the [Reloading Press](Reloading_Press.md), and the sintered metal powders feed the [Mechanical Press](Mechanical_Press.md).

## Crafting

```
P   P
P B P
P S P
```

P is a Wrought Iron Plate, S is a [Shaft](Shaft.md) and B is a [Bearing](Bearing.md). Craft it at the [Engineering Table](Engineering_Table.md) with a [screwdriver](Hand_Tools.md#screwdriver) in the tool row and 3 [fasteners](Fasteners.md).

## Using it

- **Right click with any item that is a material** pours one in, up to a full load of **40 parts**.
- **Right click empty-handed** opens the screen.
- **A pipe** fills the drum with any fluid that has a material (molten metals among them). **144 mB** counts as one part, and fluid shares the 8-part limit with items. Buckets cannot fill it.

The screen has three input slots, an output slot, a **Draw** button, a tank gauge, and readings. Items in the input slots pour in one at a time while there is room. Share bars show what each part makes up of the drum, in quarters. A word reading gives how well it is mixed, and a banded heat gauge shows the drum's temperature (hover for the reading in Tu).

**Draw** keeps the mixed share of each part (see Turning), then the mixture table decides the result:

- A row that makes an item puts it in the output slot, one per part of the mixture.
- A row that makes a fluid holds it at the fluid port until a pipe drains it. Meanwhile the drum refuses more fluid and Draw is blocked.
- With no row, the output is a **Mixture** item. Its tooltip gives rough shares (*mostly* one part, *some* of another, *a little* of a third) and lists any fluid parts. Powder Mixtures go in a charge slot of the press; the press refuses a Mixture with fluid parts.
- Black powder comes out as gunpowder items.
- Drawn hotter than a row's top temperature, the result is a **Mixture** item instead.
- Molten metal in a drum colder than its melting point is set solid. It stays a part, but Draw refuses until the drum is heated.

Hoppers and pipes can feed the inputs and take only what was drawn, never the drum's contents.

With [Jade](https://modrinth.com/mod/jade) you can see what is in the drum, how mixed it is, and its heat before you draw it. The drum is open, so no instrument is needed for the load and the mix; the heat reads as any temperature does.

## Washing ore

A drum holding water (a full bucket, 1000 mB, from a bucket on the block or a pipe at the fluid port) washes ore instead of pouring it. Put crushed ore or dust in the input slots and turn the shaft: about every half second one unit is washed, and the water carries the lighter rock away from the heavier ore. A washed unit spills over the rim as one **Concentrate** and one **Tailings**; a unit the water cannot tell from its rock stays in the slot and goes round again, so nothing is lost. How well a wash works depends on how much denser the ore is than its host rock, which JEI's Separation card shows. See [Ore Processing](Ore_Processing.md#separating).

## Alloying with heat

Put the drum over a fire or any other heat source below it, as with a [Crucible](Crucible.md). The drum warms toward the fire's temperature and cools back when the fire goes out. With no fire it stays at room temperature, and cold powder mixing works as usual.

Some alloys form here from molten metal and a solid item, one part each (144 mB of metal to one item). The drum's temperature must sit in the window, and the metal must be molten. The alloy comes out at the fluid port for a pipe.

The drum also alloys molten metals on their own, as a [Crucible](Crucible.md) does. Pipe the metals in as fluid parts at the alloy's proportions (144 mB of each counts as one part) and Draw. JEI's *Mixing* card lists each alloy's shares. Nickel and iron mixed this way draw as Molten Nickel-Iron, which casts like any melt.

| Metal | Item | Drum temperature | Makes |
| --- | --- | --- | --- |
| Molten titanium | Graphite | 1700 to 2000 Tu | Molten titanium carbide |
| Molten tungsten | Graphite | 2400 to 2700 Tu | Molten tungsten carbide |
| Molten chromium | Redstone dust or Redstone Block | above chromium's melting point | Molten redstone-chromium |

## Mixing powders for pressing

Cermet and tungsten carbide start as powders in the drum. Pour in the parts with the fire out, then Draw with the drum at 600 Tu or under. Any form of the material counts as a part, ground ingots included.

- **Cermet powder**: about 6 parts [titanium carbide](Materials.md#the-metals), 2 iron, 1 nickel and 1 cobalt. Draw gives one Cermet Powder for each part of the load.
- **Tungsten carbide powder**: about 9 parts tungsten carbide to 1 cobalt. Draw gives one Tungsten Carbide Powder for each part.

- **Osmium powder**: 7 parts [osmium](Heavy_Core.md#making-it) sponge to 1 nickel. Draw gives one Osmium Powder for each part.

The ratio must be close. Without the cobalt, or with the wrong ratio, Draw gives a plain Mixture that nothing presses. - **Netherite powder**: 55 to 65% cermet, 20 to 30% tungsten and 10 to 20% cobalt, drawn at 600 Tu or under. It is the start of [synthesized netherite](Netherite.md#synthesis).

Pack the powder with the [Mechanical Press](Mechanical_Press.md#pressing-powder), then [sinter](Furnaces.md#sintering) the compact.

## Turning

The drum counts turns: **0.00625 for every RPM each tick**, so a 16 RPM [Steam Engine](Steam_Engine.md) gives 0.1 a tick. Nothing counts below a crawl.

- **Mixing.** The share of the load that is mixed through rises evenly to full at **40 turns** (20 seconds at 16 RPM). Draw it then and everything is usable. Draw it early and you get only the mixed share of each part, rounded; the rest is lost, so a failed batch still eats its parts.
- **Adding parts** mid-turn spreads the turns already done over the bigger load, so topping up sets the mix back.
- **Grinding.** Past 40 turns the mix starts to come apart again, falling to half over the next **60 turns** and staying there. Draw anywhere in between and you get that share. The sweet spot is 40 turns and no more.
- **Friction heat.** Only past 40 turns, each turn adds **0.5 Tu** to the drum on top of the fire's heat. Friction heat is not shed while stopped, so a blend left in a drum that was over-run keeps it. When the drum's temperature, fire included, reaches the **lowest ignition temperature of any powder in the load**, the whole load goes up with the force of the powder in it, taking the drum with it. A powder load over a fire can ignite from the fire's heat alone. Only a drum of nothing but powders can ignite; any other part in the load removes the risk.

The drum does not know when it is done. Watch the mix and the heat in Jade, and draw it at the line.
