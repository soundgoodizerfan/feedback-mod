# Metalworking

This is the idea the whole mod is built around, so it's worth reading before you build your first machine.

## Machines don't know when to stop

A Feedback machine doesn't run a recipe. It does **one physical thing**, like hitting, pulling or rolling, to whatever you give it, and it **keeps doing it for as long as it has power and something to work on**. It has no idea what you wanted to make.

So when a [Mechanical Hammer](Mechanical_Hammer.md) turns your copper ingot into a copper plate, **it carries on hitting the plate**. Leave it and the plate becomes **foil**. Leave it longer and the foil becomes **scrap**. The machine never failed. You just weren't there to take the plate out.

Getting the item out **at the right moment** is the game. Instruments, gearing, timers and [controllers](Controller.md) are all ways of getting better at that.

## Work, strength and hardness

Three things decide what a blow does:

- **Strength (St)**: how hard one blow lands. It's printed on every machine and depends on how you've geared it. A [Hand Hammer](Hand_Hammer.md) swings 1, 3 or 5 St (7 with Strength), as you choose; a [Mechanical Hammer](Mechanical_Hammer.md) lands 3 on a long throw and 12 on a short one.
- **Hardness**: a property of the metal. Copper is 1, iron 6, steel 15. JEI shows it on every metalworking page, and so does [Materials](Materials.md).
- **Work (Fu)**: how much shaping the piece has taken so far. Each step (ingot → plate, plate → foil…) needs a set amount: a fixed figure plus a bit more per point of hardness. A copper plate is 14 Fu, copper foil 9 more; a steel plate is 60.

The rules:

1. **A blow weaker than the metal's hardness does nothing at all.** It's not slow, it's impossible. A hand hammer (7 St at the very most) will never shape steel (15), however long you swing.
2. **Each blow does strength ÷ hardness in work**, rounded, never less than 1. A 3 St swing on copper is 3 Fu; a 24 St blow on steel is 2.
3. **Extra work carries over.** If a blow does more than the step needs, the leftover goes straight into the *next* step. A hard enough blow on a soft enough metal can skip past the thing you wanted entirely.

That last rule is why strength is a choice and not just "more is better". Copper, with a 14 Fu plate and 9 Fu of foil after it:

| Blow | Per blow | Plate after | Foil after |
| --- | --- | --- | --- |
| Long throw, 3 St | 3 Fu | blow 5 | blow 8 |
| Short throw, 12 St | 12 Fu | blow 2 | blow 2: the same blow |

On a **short throw** the blow that makes the plate carries 10 Fu over, and the 9 Fu of foil is already done: you never see a plate. On a **long throw** you get a plate on the fifth blow and three blows to grab it.

## Watching progress

You don't need an instrument to avoid ruining things. Look at the workpiece with Jade, or hover over it, and it tells you how far along it is: *Barely marked*, *Taking shape*, *Visibly worked*, *Nearly there*.

Carry [Calipers](Calipers.md) and that turns into an exact figure, like *"9 / 14 Fu"*. That's what you want when you're aiming for something that only lasts a blow or two, like foil.

## Hot metal

Some metals, **steel** for one, can only be worked **inside a temperature window**: steel's is **900–1100 Tu**. Too cold and the blow does nothing; too hot and it does nothing either. The metal is never damaged by a blow at the wrong heat, you just have to reheat it.

Hot items cool down wherever they are, at 30 Tu a second: in your hand, in a chest, in a hopper. Steel at the top of its window has about **7 seconds** of working before it's too cold. See [Heat](Heat.md). The trip from the fire to the hammer is a race, and a hot piece burns bare hands: see [Hazards](Hazards.md).

## Wear: hitting the wrong thing

When a blow can't go into the metal, it goes into the **machine** instead. That happens when:

- the metal is too hard for the blow,
- the metal is the wrong temperature,
- the blow is harder than the machine is built to take (over-geared), or
- there's nothing left to make (scrap stays scrap).

The machine slowly wears from this: *Head is marked* (below 98% condition), *battered* (below 85%), *spent* (below 65%). A worn machine hits softer and can take less. It bottoms out at **50%** and never breaks completely or stops working. About 600 wasted short-throw blows (ten minutes on a water wheel) takes a hammer from new to its floor.

**Running a machine correctly never wears it**, however long it runs. A hammer pounding empty air doesn't wear either. If one of your machines is worn, something specific went wrong, so go and find it. [Calipers](Calipers.md) can measure exactly how worn a machine is (switch it off first).

## The other shaping machines

The hammer is one of several. Each does a different action, and each makes a different chain of products:

| Machine | Action | Chain |
| --- | --- | --- |
| [Hand Hammer](Hand_Hammer.md), [Mechanical Hammer](Mechanical_Hammer.md), [Mechanical Press](Mechanical_Press.md) | hammering | ingot → plate → foil → scrap; nugget → bullet |
| [Wire Drawer](Wire_Drawer.md) | drawing | rod → wire → fine wire; plate → case |
| [Rolling Mill](Rolling_Mill.md) | rolling | ingot → rod |
| [Drill Press](Drill_Press.md) | drilling | plate → drilled plate → swarf; gun parts |
| [Lathe](Lathe.md) | turning | rod → turned rod → swarf; plate → coin; grips; gun parts |
| [Grinding Wheel](Grinding_Wheel.md) | grinding | ingot → ground ingot → swarf; gun parts |

The drill, lathe and grinder **cut** rather than hit: the tool has to be **harder than the metal** (drill 18, lathe 20, grinding wheel 30), and then each cut removes strength ÷ hardness in work. They also make and shape [gun parts](Gun_Parts.md).

Progress doesn't carry from one action to another. A half-hammered ingot put in a rolling mill starts from zero.

JEI lists every step for every metal, with how much work it takes and how hard the metal is.

## See also

- [Materials](Materials.md): which metals there are and what forms they come in
