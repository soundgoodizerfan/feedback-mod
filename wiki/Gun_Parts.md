# Gun Parts

A [gun](Guns.md) isn't crafted from ingots. It's **assembled from machined parts**, each made on the workshop machines and each made of whatever metal you chose for it. The gun's figures come from the parts you actually put in, not from the blueprint. A sloppy barrel makes a worse gun, never a refused one.

Parts can also be shaped from stock at the bench for the stock's material (metal at the [Smithing Table](Smithing_Table.md) or [Anvil](Anvil.md) with a [hammer](Hammer.md), wood, bone and leather at the [Woodworking Table](Woodworking_Table.md), cold stock at the [Stonecutter](Stonecutter.md)), at the standard dimensions of their variant (the barrel at its variant's bore, length and wall); a machine then cuts them to any size: the drill grows the bore, the lathe thins the wall and the cutoff wheel shortens the length.

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
| **Sight** | Plate | Rolling Mill | 4 + hardness | 19 |
| **Belt Feed** | Plate | Rolling Mill | 4 + hardness | 19 |
| **Box Feed** | Plate | Rolling Mill | 4 + hardness | 19 |
| **Cylinder** | Ground Ingot | Drill Press | 8 + 2 × hardness | 38 |
| **Magazine Tube** | Rod | Drill Press | 6 + 1.5 × hardness | 28.5 |
| **Suppressor** | Cylinder | Lathe | 4 + hardness | 19 |
| **Muzzle Brake** | Magazine Tube | Drill Press | 4 + hardness | 19 |
| **Eddy Sleeve** | A conducting tube round the muzzle drags the bullet once it has left the barrel, and the lost energy heats the sleeve. Drains **50%** of a **magnetic** bullet's energy at copper's conductivity (steel barely drags; a non-magnetic bullet only a tenth as much). Recoil is unchanged, so what it buys is a slower, quieter bullet: slow it under the speed of sound and the crack goes. Costs muzzle energy and **1.5×** the drained share in extra heat. |
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

A plastic or rubber ingot turns into a grip as well, with the friction of its material (synthetic rubber 1.2, polyethylene 0.3).

The turned grip is a blank. A gun takes it as a **Gun Grip**, shaped at a bench (a grind) in one of four shapes, each multiplying the friction the hand gets (so less kick, see the table under Grip friction):

| Grip | Cells | Friction |
| --- | --- | --- |
| **Straight** | 1 | x1 |
| **Pistol grip** | 2 | x1.1 |
| **Finger-groove** | 2 | x1.2 |
| **Palm-swell** | 4 | x1.4 |

## The stock

A stock is made at a bench from a plate (any metal, polymer or organic material that comes as one) or from planks, in one of three shapes: **fixed**, **skeleton** or **folding**. It takes the kick into your shoulder while you are crouched and still, so the gun kicks less and groups tighter ([Guns](Guns.md#frames)). It is optional, and fits any frame.

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

**Without [Calipers](Instruments.md#calipers)** a barrel just says *"A bored barrel. Calipers would say how wide."* With calipers in your inventory you get its bore, wall and length to 0.1 mm.

### Hitting the target

The blueprint names a target bore (5 to 20 mm), outside diameter and barrel length. Any barrel assembles; one whose size is off the drawing past the tolerance ([Fit and head start](Engineering_Table.md#fit-and-head-start)) gives the gun a fit flaw, a wider spread. What happens in the barrel is about how the cartridge fits:

- A round will only **chamber** if its bore is within **1 mm** of the barrel's real bore.
- **Too loose:** gas leaks past. For every mm loose the round loses 60% of its energy (down to a floor of 30%) and spreads **3° wider**.
- **Too tight:** the round is squeezed. For every mm tight, peak pressure rises by **150%**, which can easily take a gun into [overpressure](Guns.md#overpressure).

Get the bore to within a few tenths of a millimetre of your cartridges.

## Assembling

Lay the gun's parts in the grid of the [Engineering Table](Engineering_Table.md), in any slots, with two [fasteners](Fasteners.md) in the fastener slots: the parts decide the gun. Assembling a gun takes Weapons Treatise II ([Treatise](Treatise.md)). A blueprint in the blueprint slot is optional; it judges the fit of the parts against the drawing, skips the torque game and is kept. The parts a gun is made of:

| Part | Role |
| --- | --- |
| Gun Barrel | always; its variant is smoothbore or rifled ([Guns](Guns.md#barrels)); a **second** barrel only on a break action |
| Receiver | always; its variant is the frame: handgun, long gun or machine gun (both need a stock) |
| Slide **or** Bolt | the action; its variant says which: self-loading or automatic (slide); bolt, lever, pump, break, self-loading or automatic (bolt) |
| Trigger Group, Gun Grip | always; a grip is wood unless you choose another material; its variant is straight, pistol grip, finger-groove or palm-swell |
| Cylinder, Magazine Tube, Belt Feed or Box Feed | optional: the feed (none means a box magazine on a slide; a bolt or lever with none loads one round by hand, and a Box Feed gives it a box magazine; a break action holds its barrels) |
| Sight | optional: a reflex sight (see-through glass, with a lit dot if illuminated) or a scope (aimed, the zoom shows inside its round lens with a reticle; the view outside the lens stays normal) (none is iron sights) |
| A fitting part | each fitting on the gun, one to a slot, added on the Engineering Table's [Fit tab](Engineering_Table.md#fit-tab) |

**Each part's material is on the part.** A receiver can be a metal or a polymer (a plastic frame): it holds the action, and on a gun without a slide it also holds the breech, so it limits the chamber (see [The chamber](#the-chamber)). The barrel's metal and wall decide the chamber's strength. The rest add their own weight. A double's second barrel has its own metal and bore; two rounds in fires the first barrel, one round fires the second.

### Grip friction

Your hand holds the grip, so the grip's material sets how much friction it gets. Kick goes down as friction goes up:

| Grip surface | Friction |
| --- | --- |
| Iron, steel | 0.6 |
| Bone, copper | 0.5 |
| Wood, bamboo, brass | 0.4 |
| Bronze | 0.3 |

Kick scales with 0.6 ÷ friction, so a bronze grip kicks twice as hard as a steel one. The grip's shape multiplies the friction on top.

## Fittings

Optional parts change what a gun does. They are tip-like parts, machined like any other and added to a finished gun on the Engineering Table's [Fit tab](Engineering_Table.md#fit-tab). Each weighs what its metal weighs. A fitting may only change figures, only add a trait, or both.

| Fitting | Effect |
| --- | --- |
| **Suppressor** | Report down to 35%, and sculk only hears it close by. Traps the gas's heat: the gun heats **1.6×** as fast per shot. |
| **Muzzle Brake** | Cancels 70% of the gas's share of recoil (the gas adds a quarter on top of the bullet's). **1.6×** louder. |
| **Relief Vent** | Opens at **90%** of the chamber's rating, so the chamber never sees more than that: no overpressure damage, no blast in your face. Leaks **10%** of every shot's energy. |
| **Barrel Shroud** | Fins: the barrel sheds heat **1.8×** as fast. |

A suppressor, a brake and an eddy sleeve all go on the muzzle, so a gun has one of them.

### Fittings made of the stuff itself

These four are shaped at a bench from raw stock, and the material decides what they do. All four act on the shot as you aim it, so the HUD and the spec sheet show the effect.

| Fitting | Made of | Effect |
| --- | --- | --- |
| **Obturator** | Leather, or any raw stock such as phantom membrane | A ring that seals a loose chamber (a Flat Ring, a Cup Seal that cuts the most kick, or a Double Ring that tightens the group most). The softer the stuff, the more it cuts: up to **25%** less kick and **15%** tighter group. Once the barrel is hotter than the stuff's oxidation temperature it has burned away and leaks: **10%** more kick and **20%** wider group, until the barrel cools. |
| **Hydraulic Buffer** | Something sticky: honey, slime | A sealed chamber of fluid the recoil pushes through. Honey takes kick to **50%** and slows the cycle by **30%**; thinner fluids do less. The fluid thins as the barrel heats and is no use at the round's ignition temperature. |
| **Lit Reticle** | Something luminous, as dust or a block: glowstone, redstone | Glowing mineral in the sight, needing no power. A **Dot** shows at light level 7 or less and tightens the group by up to **20%**; a **Ring** shows in dim light (11 or less) and tightens it less; a **Cross** shows only in deep dark (4 or less) and tightens it most. In daylight it does nothing. |
| **Magnetic Sear** | A magnetic metal: iron, steel, nickel | A magnet holds the sear: up to **15%** tighter group and a **10%** quicker cycle. The pull fades as the barrel warms and is gone at the metal's Curie temperature. A non-magnetic metal does nothing. |

### Changing a fitting

Add a fitting to a finished gun on the [Fit tab](Engineering_Table.md#fit-tab) of the Engineering Table. A gun takes one fitting to a slot, so to swap a suppressor for a brake, take the gun apart or swap the part in place with another of its own kind on the Repair tab.

## The chamber

How much pressure a chamber holds is the barrel metal's **strength** times the barrel's **wall**, divided by **half the bore**:

> rating (Pu) = strength × wall (mm) ÷ (bore (mm) ÷ 2)

A standard round of gunpowder makes **3 Pu**. So a 9 mm chamber with a 3 mm wall holds:

| Barrel metal | Strength | 9 mm, 3 mm wall | |
| --- | --- | --- | --- |
| Netherite | 50 | 33 Pu | |
| Hardened Steel | 35 | 23 Pu | |
| Steel | 15 | 10 Pu | |
| Iron | 8 | 5.3 Pu | |
| Bronze, Cast Iron | 6 | 4 Pu | |
| Brass | 5 | 3.3 Pu | barely |
| Copper | 4 | 2.7 Pu | damaged by a standard round |
| Zinc | 3 | 2 Pu | damaged |
| Gold | 2 | 1.3 Pu | badly damaged |
| Tin | 1 | 0.7 Pu | wrecked almost at once |
| Lead | 0.5 | 0.3 Pu | wrecked on the first shot. Lead is for bullets |

Halving the bore doubles the rating; so does going from a 1.5 mm wall to 3 mm. The other parts don't affect the chamber: they add weight and their own metal. The action then scales it: a bolt's locking lugs rate the same steel 25% higher, a pump or self-loading action holds it as is, a lever rates it 25% lower and a break action 40% lower, so a break gun takes shot and low-pressure rounds while a bolt rifle takes the hot ones.

**The receiver.** On a bolt, lever, pump, break or self-loading gun (any gun without a slide), the receiver holds the breech shut, so it caps the rating at **half its metal's strength**, whatever the barrel could take. Steel and other strong metals never reach the cap; a polymer or soft-metal receiver (lead, tin, polyethylene) holds only weak loads, and the chamber figure turns red when a round is over it. A slide pistol is exempt: the slide carries the breech, and the receiver takes none of the pressure.

**A softening receiver.** Every receiver material has a service temperature, and the barrel's heat reaches the receiver. Polyethylene softens at **80 Tu**. Each shot fired with the barrel above the receiver's service temperature wears the gun faster, more the further over it runs, so a plastic frame wants a cool barrel.

## Test commands

Operators can skip the shop for testing: `/feedback build parts` gives every part the held blueprint lists, and `/feedback build` gives the finished gun. `/feedback barrel` gives a barrel of any shape. These are for testing, not for survival.
