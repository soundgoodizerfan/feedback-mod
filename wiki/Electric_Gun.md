# Electric Gun

The electric gun throws a projectile with a battery's charge instead of burning powder, so it is not a [gun](Guns.md). It is made at the [Gun Table](Gun_Table.md) from a **Blueprint** (draw one at the [Blueprint Table](Blueprint_Table.md), under Weapons), and needs the weapons treatise like the other weapons.

## Parts

Made at the [Gun Table](Gun_Table.md) from a **Blueprint**, out of the same parts as a [gun](Gun_Parts.md), minus everything that needs powder: no action, bolt, slide, cylinder or magazine.

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
- **Rail barrel:** two rails the slug bridges. It takes what its conductor is rated for, and any conductor slug will do. A longer pair keeps the push.
- **Plasma barrel:** a glass arc channel. A shot vaporises a conductor nugget into a short bolt that burns what it hits and falls off with distance.

### Names

A built gun is named by rule from its barrel kind and, for a coil, its stages. The first row that fits is the name:

| Name | What gives it |
| --- | --- |
| **Gauss Rifle** | a coil barrel with many stages (over 3) |
| **Coilgun** | a coil barrel with few stages (up to 3) |
| **Railgun** | a rail barrel |
| **Plasma Gun** | a plasma barrel |

A name gives no bonus: the perks come only from the parts. A name never repeats a word its rule already needs.

### Attachments

The same fittings as a gun's, where they make sense: a **suppressor** (a quieter report, more heat), a **muzzle brake** (louder), a **bayonet**, a **bipod** (steadier), a **counterweight**, a **shroud** and a **thermal sleeve** (the gun sheds heat faster, so it recovers faster), an **eddy sleeve** (drains a conductor slug's energy), and a **resonance weight**.

The gun is drawn from its parts like a powder gun: the frame by receiver type, the barrel's coil, rails or channel, and a **cell bay** on the left with a glass window and four lamps that fill with the battery's charge, green, then amber, then red as it runs down. An empty bay is a dark slot.

## Using it

**Load a battery** (one that fits the receiver) by clicking the gun in your inventory with a [battery](Battery.md) on your cursor; click the gun with an empty cursor to take it out. The battery keeps its charge, so it goes back to a [Charger](Charger.md) as it is. Use fires. Each shot takes one metal nugget from your inventory and the shot's energy from the battery: a weak battery gives a weaker shot, never a refusal. Coil and rail slugs fly like arrows; a plasma bolt is instant and short-ranged.

Each shot warms the gun, and a warm gun recovers more slowly. A rail gives a crack, a coil a thump, and an arc of sparks leaves the muzzle.

## Misuse

Nothing wears from correct use. A battery whose voltage is over the receiver's band shocks you and wears the receiver. Firing in water or rain arcs to you. A current over what the barrel's conductor carries wears the barrel, and far over it the conductor fuses and wrecks the gun.
