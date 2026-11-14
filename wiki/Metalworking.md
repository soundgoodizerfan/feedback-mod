# Metalworking

This is the idea the whole mod is built around, so it's worth reading before you build your first machine.

## Machines don't know when to stop

A Feedback machine doesn't run a recipe. It does **one physical thing**, like hitting, pulling or rolling, to whatever you give it, and it **keeps doing it for as long as it has power and something to work on**. It has no idea what you wanted to make.

So when a [Mechanical Hammer](Mechanical_Hammer.md) turns your copper ingot into a copper plate, **it carries on hitting the plate**. Leave it and the plate becomes **foil**. Leave it longer and the foil becomes **scrap**. The machine never failed. You just weren't there to take the plate out.

Getting the item out **at the right moment** is the game. Instruments, gearing, timers and [controllers](Controller.md) are all ways of getting better at that.

## Work, strength and hardness

Three things decide what a blow does:

- **Strength (St)**: how hard one blow lands. It's printed on every machine and depends on how you've geared it. A [Hammer](Hammer.md) swings 2.2, 6.6 or 11 St (15.4 with Strength), as you choose; a [Mechanical Hammer](Mechanical_Hammer.md) lands an eighth of its head's ceiling on a long throw and half on a short one.
- **Hardness**: a property of the metal. Copper is 3.92, iron 7.85, steel 14.7. JEI shows it on every metalworking page, and so does [Materials](Materials.md).
- **Work (Fu)**: how much shaping the piece has taken so far. Each step (ingot → plate, plate → foil…) needs a set amount: a fixed figure plus a bit more per point of hardness. A copper plate is about 19 Fu, copper foil about 12 more; a steel plate is about 42.

The rules:

1. **A blow weaker than the metal's hardness does nothing at all.** It's not slow, it's impossible. A hammer swung by hand (15.4 St at the very most) will never shape cast iron (19.6), however long you swing.
2. **Each blow does strength ÷ hardness in work**, rounded, never less than 1. A 6.6 St swing on copper (3.92) is 2 Fu; a 29.4 St blow on steel (14.7) is 2.
3. **Extra work carries over.** If a blow does more than the step needs, the leftover goes straight into the *next* step. A hard enough blow on a soft enough metal can skip past the thing you wanted entirely.

That last rule is why strength is a choice and not just "more is better". Lead, with an 11.6 Fu plate and 7.6 Fu of foil after it:

| Blow | Per blow | Plate after | Foil after |
| --- | --- | --- | --- |
| Tap, 2.2 St | 4 Fu | blow 3 | blow 5 |
| Blow, 6.6 St | 13 Fu | blow 1 | blow 2 |
| Heavy blow, 11 St | 22 Fu | blow 1 | blow 1: the same blow |

On a **heavy blow** the blow that makes the plate carries 10 Fu over, and the foil is already done: you never see a plate. With a **tap** you get a plate on the third blow and a blow or two to grab it.

## Watching progress

You don't need an instrument to avoid ruining things. Look at the workpiece with Jade, or hover over it, and it tells you how far along it is: *Barely marked*, *Taking shape*, *Visibly worked*, *Nearly there*.

Carry [Calipers](Instruments.md#calipers) and that turns into an exact figure, like *"9 / 19 Fu"*. That's what you want when you're aiming for something that only lasts a blow or two, like foil.

## Hot metal

Some metals, **steel** for one, can only be worked **inside a temperature window**: steel's is **900–1100 Tu**. Too cold and the blow does nothing; too hot and it does nothing either. The metal is never damaged by a blow at the wrong heat, you just have to reheat it.

Hot items cool down wherever they are, at a rate set by their material (steel loses about 28 Tu a second): in your hand, in a chest, in a hopper. Steel at the top of its window has about **7 seconds** of working before it's too cold. See [Heat](Heat.md). The trip from the fire to the hammer is a race, and a hot piece burns bare hands: see [Hazards](Hazards.md).

## Work hardening

Drawing and rolling at room temperature strains a metal that is ductile enough to be cold worked (a ductility of 3 or more, shown in JEI). Each draw or roll stage a piece finishes adds one **strain**. A metal takes as many strain steps as its ductility; at that limit the next draw or roll does not move it: the blow does not land, nothing is lost, and the machine's die wears as for any blow that cannot land (see below). **Annealing** clears the strain: heat the piece to its anneal temperature (the *Anneals at* figure JEI shows), in a [Crucible](Crucible.md) (take the piece out while the crucible is at or over that temperature) or a [Kiln zone](Kiln_Zone.md). The [Annealing Furnace](Furnaces.md) tops out at 380 Tu, so it cannot reach the anneal of metals that need more. A piece at or above its anneal temperature is worked hot, without strain.

A strained piece's tooltip reads *Cold-worked: n of m steps*, in red at the limit. Hammer blows never strain a piece. Hand drawing and hand rolling strain a piece the same way.

## Wear: hitting the wrong thing

When a blow can't go into the metal, it goes into the **machine** instead. That happens when:

- the metal is too hard for the blow,
- the metal is the wrong temperature,
- the piece is cold-worked to its limit ([Work hardening](#work-hardening)),
- the blow is harder than the machine is built to take (over-geared), or
- there's nothing left to make (scrap stays scrap).

The machine slowly wears from this: *Head is marked* (below 98% condition), *battered* (below 85%), *spent* (below 65%). A worn machine hits softer and can take less. It bottoms out at **50%** and never breaks completely or stops working. About 600 wasted short-throw blows (ten minutes on a water wheel) takes a hammer from new to its floor.

**Running a machine correctly never wears it**, however long it runs. A hammer pounding empty air doesn't wear either. If one of your machines is worn, something specific went wrong, so go and find it. [Calipers](Instruments.md#calipers) can measure exactly how worn a machine is (switch it off first).

## The other shaping machines

The hammer is one of several. Each does a different action, and each makes a different chain of products:

| Machine | Action | Chain |
| --- | --- | --- |
| [Hammer](Hammer.md), [Mechanical Hammer](Mechanical_Hammer.md), [Mechanical Press](Mechanical_Press.md) | hammering | ingot → plate → foil → scrap; nugget → bullet |
| [Wire Drawer](Wire_Drawer.md) | drawing | rod → wire → fine wire; plate → case |
| [Rolling Mill](Rolling_Mill.md) | rolling | ingot → rod; plate → pipe |
| [Drill Press](Drill_Press.md) | drilling | plate → drilled plate → swarf; gun parts |
| [Lathe](Lathe.md) | turning | rod → turned rod → swarf; plate → coin; grips; gun parts |
| [Grinding Wheel](Grinding_Wheel.md) | grinding | ingot → ground ingot → swarf; gun parts |

The drill, lathe and grinder **cut** rather than hit: the tool has to be **harder than the metal** (drill 78, lathe 150, grinding wheel 196), and then each cut removes strength ÷ hardness in work. They also make and shape [gun parts](Gun_Parts.md).

Progress doesn't carry from one action to another. A half-hammered ingot put in a rolling mill starts from zero.

JEI lists every step for every metal, with how much work it takes and how hard the metal is.

## See also

- [Materials](Materials.md): which metals there are and what forms they come in
