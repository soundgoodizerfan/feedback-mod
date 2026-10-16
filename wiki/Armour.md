# Armour

Helmets, chestplates, leggings and boots are assembled from plate the way [tools](Tools_and_Weapons.md) are. The recipes for vanilla's armour are gone.

## Layout

| Piece | Layout |
| --- | --- |
| **Helmet** | `PPP / P_P` |
| **Chestplate** | `P_P / PPP / PPP` |
| **Leggings** | `PPP / P_P / P_P` |
| **Boots** | `P_P / P_P` |

P is **plate**: ingots, plates, leather, gems, or chain. Every plate in a piece must be the same material.

A **lining** (anything fibrous: leather, wool) goes in the gap: `PPP / PLP`, `PLP / PPP / PPP`, `PPP / PLP / P_P`, `P_P / PLP`. It adds a little protection.

## What it gives

- **Protection depends on how hard and strong the plate is.** Iron gives about what it used to: six points in a chestplate. Diamond and netherite give more; **gold and leather give much less**, because they're soft. A plate that is better than iron gives less and less extra: there's a ceiling.
- **Chain covers less than plate** (three quarters as much).
- **Armour toughness** only comes from the very best plate (diamond, netherite).
- **Dense metal resists being pushed.** Heavy metals like gold and netherite give knockback resistance.
- **Weight slows you.** A chestplate of dense metal (gold is nearly three times iron's weight) makes you a little slower. Iron is light enough not to.

The piece is drawn in a plain grey sprite **tinted by the plate**, so you can see at a glance what someone is wearing.

## Wearing out

Armour wears like any [tool](Tools_and_Weapons.md#when-it-breaks-mending-and-swapping-parts): it stops one point short of gone and gives nothing until it's repaired with the plate's material in the crafting grid. Old vanilla armour you find is converted.

[Guns](Guns.md#hardness-glass-glancing-armour) read the plate's hardness to decide what a bullet goes through.
