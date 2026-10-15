# Electrolytic Cell

A glass tank of water with two electrodes. Run current through it and it **dissolves one metal and plates another back out**. It refines alloys and scrap into pure metal, and it coats metal pieces in another metal.

## Crafting

```
W   W
G   G
G G G
```

W is [Copper Wire](Wire_Drawer.md) and G is Glass.

## Setting it up

1. **Fill it with water.** Right-click it with a Water Bucket (or pipe water in). It holds 1000 mB and stops working below 100 mB.
2. **Hang an anode:** right-click the **top** with a piece of metal, any form. This is what gets dissolved.
3. **Connect power** with [cable](Copper_Cable.md). It asks for **8 A**.

| To | Do this |
| --- | --- |
| Take metal off the cathode | Right-click with an empty hand |
| Take the anode back | Sneak-right-click with an empty hand |
| Hang a piece to be plated | Right-click a **side** with a metal piece |

## Refining

The anode dissolves into the water as the metals it's made of. **Brass** dissolves as copper and zinc, **bronze** as copper and tin. The current then plates metal back out onto the cathode.

### Current is the speed

Each amp moves metal at a fixed rate. One ingot of any metal takes:

| Supply | Current | One ingot |
| --- | --- | --- |
| Full demand | 8 A | 30 s |
| Quarter demand | 2 A | 2 min |
| [Galvanic Cell](Galvanic_Cell.md) | 1 A | 4 min |

### Voltage picks the metal

This is the important part. The **noblest** metal in the water plates first, at just **0.1 V**. A less noble metal needs 0.1 V **plus the gap** between the two metals' electrode potentials:

| Metal | Electrode potential |
| --- | --- |
| Gold | 1.50 V |
| Copper | 0.34 V |
| Lead | −0.13 V |
| Tin | −0.14 V |
| Iron | −0.44 V |
| Zinc | −0.76 V |

So with brass in the water, copper plates at 0.1 V and zinc needs 0.1 + 1.1 = **1.2 V**:

- A zinc–copper **Galvanic Cell (1.1 V)** plates pure copper and leaves the zinc in the water. Slow, but clean.
- A coarse **Dynamo at 1.5 V** plates both. Fast, but the copper comes out mixed.

Below a metal's figure, no amount of current plates it. That's a real limit, not a slow one.

### Taking the copper off

Right-click with an empty hand to strip every whole ingot's worth off the cathode. If at least **95%** of the deposit is one metal, you get **ingots** of it. Below that, you get that metal's **scrap**. Anything less than a full ingot stays on the cathode.

Scrap can be hung as an anode again, so a bad batch costs time and current, not metal.

### When the anode runs out

Once the anode is gone, the only thing left to do is split the water, which needs **1.23 V**.

- **Below 1.23 V** the cell simply stops. A low-voltage refinery is safe to forget about.
- **Above 1.23 V** it keeps going, stripping whatever is left in the water onto your finished cathode, zinc included. Leave it too long and a cathode of pure copper becomes copper scrap.

The faster supply is the one that punishes you for coming back late.

### The water remembers

Metal that didn't plate stays dissolved. The next anode you hang goes into the same water. Draining the tank takes the dissolved metal with it.

## Electroplating

Hang a metal piece on a **side** of the cell. While it hangs, whatever plates goes onto the piece instead of the cell's own cathode, by exactly the same rules: voltage picks the metal, current sets the speed. Right-click with an empty hand to take it back off. The tooltip says what it's plated with and how well.

How it comes out depends on how much metal went onto it (an ingot is 10 Qu):

| Coat | Result |
| --- | --- |
| Less than 0.5 Qu | **Patchy**: the coat hasn't closed |
| 0.5 to 2 Qu | **Plated**: a good coat |
| More than 2 Qu | **Spalled**: it flakes off as it comes out. You get the bare piece back and the coat as nuggets |
| Two metals in it | **Discoloured**: the voltage was high enough to plate a second metal |
| Too much current | **Burnt dark** |

At 1 A, a piece is in the good window from about **12 s to 48 s**. At 2 A, half that.

**Burning** depends on the size of the piece. An ingot-sized piece takes up to **2.5 A** before its coat burns; a nugget takes about a quarter of that, a block about four times. So a Galvanic Cell or a fine Dynamo plates an ingot cleanly, while a coarse Dynamo's 8 A burns an ingot and plates a whole block cleanly.

A plated piece hung again keeps its coat and grows it further. Plated iron is still iron to everything else in the game.

## Wear

It's rated for low voltage (1.5 V). More than that heats it and wears it, faster the further over, down to half condition. A fine [Dynamo](Dynamo.md) at full speed (6 V) is four times its rating. It never breaks and never refuses to run.
