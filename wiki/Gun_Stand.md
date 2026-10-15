# Gun Stand

The drafting table where you design a [gun](Guns.md). The screen is a blueprint: the gun is drawn on a grid and redraws itself as you change it. You set it up part by part, choose a metal for each part, and print the design onto paper as a **Gun Blueprint**.

There's no crafting recipe for the Gun Stand yet. It's in the creative menu.

## The screen

**The type** comes first, at the top: **Pistol**, **Marksman rifle**, **Shotgun** or **LMG**. It decides which parts are drawn and which actions and feeds are offered. See [Guns](Guns.md#types-actions-and-feeds).

**Click a part** on the drawing to select it. Its settings appear below, changed with the **< >** buttons:

| Part | Settings |
| --- | --- |
| **Barrel** | **Bore** 5–20 mm · **Barrel** short (0.1 m), medium (0.2 m) or long (0.4 m) · **Wall** thin (1.5 mm), standard (3 mm) or thick (5 mm) · **Muzzle**: open, suppressor or muzzle brake |
| **Receiver** (a pistol's frame) | **Sight**: iron, reflex or scope · **Relief vent** · **Shroud** |
| **Trigger** | **Action** · **Feed** (with how many rounds it holds) · **Launcher**: chamber or coil |
| **Slide** or **Bolt**, **Grip** or **Stock** | only a metal |

A break action has no slide or bolt. Fittings are explained on [Gun Parts](Gun_Parts.md#fittings).

**The sample slot** sets the metal of the **selected part**. Put in any piece of the metal you want (an ingot, plate, rod or nugget). **The sample isn't used up.** The grip also takes anything a grip can be turned from: a stick, a bone or bamboo. New designs start as steel throughout.

**The name box** names the design. The name goes onto the blueprint and every gun built from it.

**The paper slot** takes Paper to print on. Put a Gun Blueprint here instead to load its design (and its name), change it, and print over it. Press **Print**.

The figures at the bottom update as you go: chamber, muzzle, cycle and mass, the same spec sheet as the gun's tooltip. See [Guns](Guns.md#reading-the-tooltip).

## The chamber

How much pressure a chamber holds is the barrel metal's **strength** times the **wall**, divided by **half the bore**:

> rating (Pu) = strength × wall (mm) ÷ (bore (mm) ÷ 2)

A standard round of gunpowder makes **3 Pu**. So a 9 mm chamber with a standard 3 mm wall holds:

| Barrel metal | Strength | 9 mm, standard wall | |
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

Halving the bore doubles the rating; so does going from a 1.5 mm wall to 3 mm. Watch the **Chamber** line: if it's red, the gun can't take a standard round safely. See [Overpressure](Guns.md#overpressure).

The other parts don't affect the chamber. They add weight (heavier guns kick less and heat up more slowly) and their own metal to the parts list.

The stand only accepts metals that are strong enough to build with and come as ingots. Only the metal of the barrel, and the parts you can actually machine, matter in the end: see [Gun Parts](Gun_Parts.md) for which metals the workshop can cut.

## Building the gun

The blueprint lists the parts it needs. Machine them ([Gun Parts](Gun_Parts.md)) and put them in a crafting grid with the blueprint, or an [Assembly Bench](Assembly_Bench.md) for designs with more than eight parts. You get the gun and the blueprint comes back.

The blueprint's bore, barrel and wall are **targets**. The gun is baked from the barrel you actually made.
