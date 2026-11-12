# Electric Gun

The electric gun throws a projectile with a battery's charge instead of burning powder, so it is not a [gun](Guns.md). It is assembled at the [Engineering Table](Engineering_Table.md) from its parts. A **Blueprint** (draw one at the [Blueprint Table](Blueprint_Table.md), under Weapons) is optional; drawing it and assembling it both take Weapons Treatise III: Electric Guns.

## Parts

Shaped and assembled like a [gun](Gun_Parts.md), out of the same parts, minus everything that needs powder: no action, bolt, slide, cylinder or magazine.

| Part | Takes | Does |
| --- | --- | --- |
| **Receiver** | any material | Its **type** is the same three as a gun's, and each sets how big a battery it takes: a **handgun** frame up to 2 cells, a **long gun** 4, a **machine gun** 8 (the bigger frames also keep their heat better). Its material's insulation sets the voltage band it holds off. |
| **Barrel** | a conductor (a metal that carries current) | One of three kinds, below. Its conductor sets the current it carries before it wears. Axes: barrel length, bore, and for a coil, stages. |
| **Trigger group** | as a gun's | Required. |
| **Grip** | as a gun's | The same four shapes. |
| **Stock** | as a gun's | Fixed, skeleton or folding: steadier aim. |
| **Sight** | as a gun's | A reflex sight or a scope, and a lit reticle that cuts spread in the dark. |

### Barrels

- **Coil barrel:** windings round the bore, drawn as a ring for each stage. Each **stage** adds energy, but only as many as the barrel is long enough to fit work well; the rest are mostly wasted as switching losses. The slug must be a **magnetic** metal (iron, for one).
- **Rail barrel:** two rails the slug bridges. It takes what its conductor is rated for, and any conductor slug works. A longer pair keeps the push.
- **Plasma barrel:** a glass arc channel. A shot vaporises a conductor nugget into a short bolt that burns what it hits and falls off with distance.

### Names

A built gun is named by rule from its barrel kind, receiver and fittings, and for a coil its stages: a pistol, carbine, rifle, marksman or sniper rifle, cannon or machine gun of the barrel kind (Coil Pistol, Rail Carbine, Plasma Cannon), with the receiver, bands and fittings deciding which. A name gives no bonus: the perks come only from the parts. What gives each is under [Electric guns](Designations.md#electric-guns).

### Attachments

The same fittings as a gun's, where they make sense: a **suppressor** (a quieter report, more heat), a **muzzle brake** (louder), a **bayonet**, a **bipod** (steadier), a **counterweight**, a **shroud** and a **thermal sleeve** (the gun sheds heat faster, so it recovers faster), an **eddy sleeve** (drains a conductor slug's energy), and a **resonance weight**.

The gun is drawn from its parts like a powder gun: the frame by receiver type, the barrel's coil, rails or channel, and a **cell bay** on the left with a glass window and four lamps that fill with the battery's charge, green, then amber, then red as it runs down. An empty bay is a dark slot.

## Using it

**Load a battery** (one that fits the receiver) by clicking the gun in your inventory with a [battery](Battery.md) on your cursor; click the gun with an empty cursor to take it out. The battery keeps its charge, so it goes back to a [Charger](Charger.md) as it is.

The gun is held and fired like a [gun](Guns.md#controls): attack (left mouse) fires, and holding it repeats; **V** cycles the fire mode between automatic, semi-automatic and burst; use (right mouse), held, aims down the sights. The view kicks and the hand strains on each shot as with a powder gun (see [Handling](Guns.md#handling)). It has no reload; a battery is changed by hand in the inventory.

Each shot takes one metal nugget from your inventory and the shot's energy from the battery: a weak battery gives a weaker shot, never a refusal. A nugget fills the chamber with several slugs, shown on the HUD, and the next shots use those before another nugget is taken. Coil and rail slugs are real bullets: they hit, meet armor and meet blocks by the same rules as a firearm's bullets ([What bullets do](Guns.md#what-bullets-do)). A plasma bolt is a fast bolt that sets what it hits alight, loses speed quickly and hits weaker with distance; it reaches about 14 blocks.

Each shot warms the gun, and a warm gun recovers more slowly, so the cycle lengthens as it heats. A rail gives a crack, a coil a thump, and an arc of sparks leaves the muzzle.

Gun skills and casts that need firearm rounds (such as Overwatch and Snap Volley) do not fire it.

### HUD

Holding the gun shows a card with the battery's charge in percent, the slugs chambered, the barrel's heat and the fire mode. **NO CELL** shows when no battery is loaded, and **CELL FLAT** when the battery has no charge left.

## Misuse

Nothing wears from correct use. A battery whose voltage is over the receiver's band shocks you and wears the receiver. Firing in water or rain arcs to you. A current over what the barrel's conductor carries wears the barrel, and far over it the conductor fuses and wrecks the gun.
