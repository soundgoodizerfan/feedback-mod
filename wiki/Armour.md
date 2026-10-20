# Armour

Helmets, chestplates, leggings and boots are assembled from plate the way [tools](Tools_and_Weapons.md) are. The recipes for vanilla's armour are gone.

## Making the plate

The plate is a part of its own. Shape it in a crafting table (or [Assembly Bench](Assembly_Bench.md)) with a [Hand Hammer](Hand_Hammer.md) anywhere else in the grid; the hammer loses a point.

| Piece | Plate shape (stock in each `X`) |
| --- | --- |
| **Helmet** | `XXX / X_X` |
| **Chestplate** | `X_X / XXX / XXX` |
| **Leggings** | `XXX / X_X / X_X` |
| **Boots** | `X_X / X_X` |

Stock is ingots, plates, leather, gems or chain, all the same material. A metal with a hot-working window (steel) has to be hot to shape, as under any hammer.

## Assembling

Put the plate in the grid on its own for the bare piece. Add a **lining** (anything fibrous: leather, wool) under it for a little extra protection, and/or **studs**:

| Layout | Gives |
| --- | --- |
| `P` | plain |
| `P / L` | lined |
| `P / T`, `P / TT` | studs (one or two) |
| `P / L / T`, `P / L / TT` | lined and studded |

### Studs

**Studs** are gems, shards or nuggets set into the plate (brigandine; diamond studs in steel plate). Each stud covers a tenth of the piece, up to half, and that share takes the stud's hardness, toughness and wear. Four diamond studs in a steel chestplate lift it from steel's 6.7 points to about 7.2 and keep steel's toughness, where a whole diamond plate is 7.5 points and glass. Studs add a little mass and nothing else. Armour takes no tip: a plate has no edge.

## What it gives

- **Protection depends on how hard and strong the plate is.** Iron gives about what it used to: six points in a chestplate. Diamond and netherite give more; **gold and leather give much less**, because they're soft. A plate that is better than iron gives less and less extra: there's a ceiling.
- **Chain covers less than plate** (three quarters as much).
- **Armour toughness** only comes from the very best plate (diamond, netherite).
- **Dense metal resists being pushed.** Heavy metals like gold and netherite give knockback resistance.
- **Weight slows you.** A chestplate of dense metal (gold is nearly three times iron's weight) makes you a little slower. Iron is light enough not to.

The piece is drawn in a plain grey sprite **tinted by the plate**, so you can see at a glance what someone is wearing.

## Wearing out

Armour wears like any [tool](Tools_and_Weapons.md#when-it-breaks-mending-and-swapping-parts): it stops one point short of gone and gives nothing until it's repaired with the plate's material in the crafting grid. Old vanilla armour you find is converted; diamond armour becomes [steel with four diamond studs](Tools_and_Weapons.md#old-vanilla-tools).

Armour can be [plated](Tools_and_Weapons.md#plating). A sound coat of a harder metal is a skin over the whole plate and adds about a tenth of its hardness.

[Guns](Guns.md#hardness-glass-glancing-armour) read the plate's hardness to decide what a bullet goes through.
