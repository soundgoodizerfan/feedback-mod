# Mechanical Hammer

A powered hammer and anvil. Driven by a [Crank Linkage](Crank_Linkage.md), it hits whatever is on its anvil **over and over, for as long as it's turning**. It's the first real machine in the mod, and the clearest example of how every Feedback machine behaves.

## Crafting

```
P P P
  S
B B B
```

P is a [Copper Plate](#what-it-makes), S is a [Shaft](Shaft.md) and B is Cobblestone.

## Setting it up

1. Run a shaft from a power source to a **Crank Linkage**. A [Water Wheel](Water_Wheel.md) is the usual first choice. A [Hand Crank](Hand_Crank.md) is too weak, and one [Steam Engine](Steam_Engine.md) on its own isn't quite enough either.
2. Put the hammer **directly in front of** the linkage. The linkage turns rotation into hammer blows.

The linkage adds the hammer's **80 Su** load to your shaft network, so make sure your source can carry it. A water wheel (256 Su) carries three.

## Using it

- **Right-click with an item** to put it on the anvil. One item at a time.
- **Right-click** again (with anything, even a full hand) to take it back off.
- Hoppers can take items off the anvil too. But a hopper doesn't know when the piece is "done" either; it just grabs whatever is there at that moment.

While the linkage turns, the hammer strikes. Each blow puts work into the piece. When the piece has taken enough, it becomes the next thing (ingot → plate), and **the hammer keeps right on hitting that**. Take it off at the right time. See [Metalworking](Metalworking.md).

## What it makes

| From | Becomes | Then | Then |
| --- | --- | --- | --- |
| Ingot | Plate | Foil | Scrap |
| Nugget | Bullet | | |

This works on every metal in the game, provided the blow is strong enough for it.

Foil isn't just a mistake. It's a useful material in its own right (the [Electroscope](Electroscope.md) and [Thermometer](Thermometer.md) use it), and on copper it sits in a narrow window just past plate.

## Reading it

Look at it with Jade:

- **Strength** is printed as *short throw / long throw* St, along with the hammer's **ceiling** (the hardest blow it can take) and its **load** in Su. These are always exact and free.
- **What's on the anvil**, and how far along it is (*Taking shape*, *Nearly there*…). With [Calipers](Calipers.md) in your inventory you get the exact work figure instead.
- **Condition**, if it's been abused: *Head is marked* (below 98%), *battered* (below 85%), *spent* (below 65%).

## Strength and gearing

How hard the hammer hits depends on the linkage's **throw** and on any gearing between the source and the linkage:

| Setting | Strength |
| --- | --- |
| Long throw | 3 St |
| Short throw | 12 St |
| Ceiling (most it can land) | 24 St |

- **Long throw**: gentle. Use it for soft metals like copper, where you want to stop at plate (fifth blow).
- **Short throw**: four times harder. It's too much for copper (it skips plate entirely and goes straight to foil), but you need it for harder metals: iron (6) and cast iron (12).
- **Gearing down** (slower shaft, more force) multiplies the blow. A large cog driven by a small one doubles it. That's how you reach **steel** (hardness 15): a short throw geared down 2:1 lands 24 St, 2 Fu a blow, so a steel plate (60 Fu) takes 30 blows. Steel also has to be between **900 and 1100 Tu** while you hit it.

Gearing up past the hammer's **24 St ceiling** doesn't make it hit harder. The extra force goes into the hammer and wears it.

## Wear

The hammer wears **only** when a blow can't go into the metal:

- the metal is too hard for the blow,
- steel that has cooled down (or got too hot),
- scrap, which can't become anything else, or
- gearing past the ceiling.

A worn head hits softer and takes less before it's over its ceiling. It bottoms out at half condition and never breaks or stops entirely. Pounding an **empty** anvil does no harm.

Breaking and replacing a worn hammer doesn't reset it; the wear stays with the item. [Calipers](Calipers.md) give you an exact condition reading, but only while the hammer is **stopped**.

## Tips

- A [Clutch](Clutch.md) between the source and the linkage lets you stop the hammer without stopping everything else.
- A [Timer](Timer.md) or a [Controller](Controller.md) can stop it for you. That's the first real automation problem in the mod.
- A hot ingot is cooling from the moment it leaves the fire, so keep the hammer close to the crucible.
