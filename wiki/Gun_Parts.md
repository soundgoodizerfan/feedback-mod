# Gun Parts

A [gun](Guns.md) isn't crafted from ingots. It's **assembled from machined parts**, each made on the workshop machines and each made of whatever metal you chose for it. The gun's figures come from the parts you actually put in, not from the blueprint. A sloppy barrel makes a worse gun, never a refused one.

## The parts

Every metal has these part forms. Each is made by one step on a machine, like any other [form](Materials.md#forms). A part is one item (a *Gun Barrel*, say) and the metal it's made of rides on it, shown on its tooltip:

| Part | Made from | By | Work (Fu) | Steel |
| --- | --- | --- | --- | --- |
| **Gun Barrel** | Turned Rod | [Drill Press](Drill_Press.md) | 6 + 1.5 × hardness | 28.5 |
| **Receiver** | Ingot | Drill Press | 8 + 2 × hardness | 38 |
| **Slide** | Plate | [Grinding Wheel](Grinding_Wheel.md) | 6 + 1.5 × hardness | 28.5 |
| **Bolt** | Rod | Grinding Wheel | 6 + 1.5 × hardness | 28.5 |
| **Trigger Group** | Drilled Plate | Grinding Wheel | 4 + hardness | 19 |
| **Grip** | Ingot | [Lathe](Lathe.md) | 4 + hardness | 19 |
| **Cylinder** | Ground Ingot | Drill Press | 8 + 2 × hardness | 38 |
| **Magazine Tube** | Rod | Drill Press | 6 + 1.5 × hardness | 28.5 |
| **Suppressor** | Cylinder | Lathe | 4 + hardness | 19 |
| **Muzzle Brake** | Magazine Tube | Drill Press | 4 + hardness | 19 |
| **Relief Vent** | Nugget | Drill Press | 4 + hardness | 19 |
| **Barrel Shroud** | Drilled Plate | [Rolling Mill](Rolling_Mill.md) | 4 + hardness | 19 |

The cutting machines also have to be **harder than the metal**: the drill bit is 18, the lathe bit 20, the grinding wheel 30. So hardened steel (25) parts need a grinding wheel for every step, which rules out anything drilled or turned. **Netherite (40) is harder than every cutting tool**, so netherite parts can't be machined yet.

### Grips from bone and wood

A grip carries no load, so it doesn't have to be a strong metal. The lathe also turns grips from:

| From | Grip | Work (Fu) | Grip friction |
| --- | --- | --- | --- |
| Stick | Wood | 2.5 | 0.4 |
| Bamboo | Bamboo | 3 | 0.4 |
| Bone | Bone | 5 | 0.5 |

Leather, wool, paper and honey can't be turned into a grip, but they can **wrap** one (see [Assembling](#assembling)).

## The barrel

The drill press makes a Gun Barrel by piercing a turned rod. A fresh barrel is a **3 mm** pilot bore through a rod **30 mm** across and **0.6 m** long. It's nowhere near a gun yet. You cut it to shape, and **each machine changes one dimension** for as long as you keep it working:

| Machine | Changes | Per Fu of work |
| --- | --- | --- |
| Drill Press | widens the **bore** | +0.1 mm |
| Lathe | thins the **outside** | −0.2 mm |
| Grinding Wheel | shortens the **length** | −5 mm |

The **wall** is what's left between the bore and the outside: (outside − bore) ÷ 2.

The machines don't know what you're aiming for. Drill until the wall is under **0.5 mm**, or grind it shorter than **5 cm**, and the barrel is **swarf**.

Work per cut is the machine's strength (St) divided by the metal's hardness. For example, a fast drill feed (22 St) on steel (15) is about 1.5 Fu a push, so each push widens the bore by about 0.15 mm.

**Without [Calipers](Calipers.md)** a barrel just says *"A bored barrel. Calipers would say how wide."* With calipers in your inventory you get its bore, wall and length to 0.1 mm.

### Hitting the target

The blueprint names a target bore, wall and barrel length (short 0.1 m, medium 0.2 m, long 0.4 m), but there's no tolerance check. Any barrel assembles. What happens is about how the cartridge fits:

- A round will only **chamber** if its bore is within **1 mm** of the barrel's real bore.
- **Too loose:** gas leaks past. For every mm loose the round loses 60% of its energy (down to a floor of 30%) and spreads **3° wider**.
- **Too tight:** the round is squeezed. For every mm tight, peak pressure rises by **150%**, which can easily take a gun into [overpressure](Guns.md#overpressure).

Get the bore to within a few tenths of a millimetre of your cartridges.

## Assembling

Put the **Gun Blueprint** and the parts it lists into a crafting grid, in any arrangement. A big gun won't fit in a crafting table's nine slots; use an [Assembly Bench](Assembly_Bench.md). You get the gun and **keep the blueprint**.

The blueprint's tooltip lists the parts (*"Parts: barrel, receiver, slide, trigger group, grip"*). Which ones depends on the design:

| Part | When |
| --- | --- |
| Gun Barrel | always; **two** for a double-barrelled shotgun |
| Receiver, Trigger Group, Grip | always |
| Slide | a self-loading or automatic **pistol** |
| Bolt | any other action except a break action |
| Cylinder | a revolver (cylinder feed) |
| Magazine Tube | a tube feed |
| Plates | 1 for a reflex sight, 2 for a scope |
| A fitting part | each fitting on the design |
| Blocks of Redstone | a coilgun: 1, 2 or 3 for a short, medium or long coil |

**Each part's metal is read off the item you put in.** The barrel's metal decides the chamber's strength. The rest add their own weight. A double's second barrel is a part of its own, with its own metal and its own bore; two rounds in fires the first barrel, one round fires the second, so a mismatched pair shoots two different shots.

### Wrapping the grip

Add any **fibrous or sticky** organic to the assembly (leather, wool, paper, honey) and it **wraps the grip**. Your hand holds the wrap, so the wrap's grip friction replaces the grip's. Kick goes down as friction goes up:

| Grip surface | Friction |
| --- | --- |
| Honey | 1.5 |
| Leather, iron, steel | 0.6 |
| Bone, copper, wool | 0.5 |
| Wood, bamboo, brass | 0.4 |
| Bronze | 0.3 |

Kick scales with 0.6 ÷ friction, so a bronze grip kicks twice as hard as a leather-wrapped one, and a honey wrap kicks at 40%.

## Fittings

Four optional parts change what a gun does. You add them on the [Gun Stand](Gun_Stand.md), machine them like any other part, and include them in the assembly. Each weighs what its metal weighs.

| Fitting | Effect |
| --- | --- |
| **Suppressor** | Report down to 35%, and sculk only hears it close by. Traps the gas's heat: the gun heats **1.6×** as fast per shot. |
| **Muzzle Brake** | Cancels 70% of the gas's share of recoil (the gas adds a quarter on top of the bullet's). **1.6×** louder. |
| **Relief Vent** | Opens at **90%** of the chamber's rating, so the chamber never sees more than that: no overpressure damage, no blast in your face. Leaks **10%** of every shot's energy. |
| **Barrel Shroud** | Fins: the barrel sheds heat **1.8×** as fast. |

A suppressor and a brake both go on the muzzle, so a gun has one or the other.

### Refitting

Put a **built gun** and **one fitting part** in a grid to fit it. Whatever it displaces comes back in the grid where the new part was: fit a brake to a suppressed gun and you get the suppressor back. **Plates** refit the sight the same way: one plate for a reflex sight, two for a scope.

The gun keeps its rounds, heat and wear.

## Test commands

Operators can skip the shop for testing: `/feedback build parts` gives every part the held blueprint lists, with the barrel cut exactly to target, and `/feedback build` gives the finished gun. `/feedback barrel` gives a barrel of any shape. These are for testing, not for survival.
