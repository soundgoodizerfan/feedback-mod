# Armor

Helmets, chestplates, leggings and boots are shaped from plate at a bench, and the plate is the piece: it comes out ready to wear.

## Making the piece

Shape the plate at the bench for its stock: the [Smithing Table](Smithing_Table.md) (metal, with a [Hammer](Hammer.md)), the [Woodworking Table](Woodworking_Table.md) (wood, bone, leather, fiber, with an axe) or the [Stonecutter](Stonecutter.md) (stone, ceramic, glass, crystal, with a [Sawblade](Sawblade.md)). Wooden plates are sawn and shaped at the Woodworking Table. The tool loses a point. Put stock in as many cells as the shape has. What comes out is the finished piece, plain, and it can be worn at once.

| Piece | Plate shape (cells; the shape is only a picture) |
| --- | --- |
| **Helmet** | `XXX / X_X` |
| **Chestplate** | `X_X / XXX / XXX` |
| **Leggings** | `XXX / X_X / X_X` |
| **Boots** | `X_X / X_X` |

Stock is ingots, plates, leather, gems or chain, all the same material. A metal with a hot-working window (steel) has to be hot to shape, as under any hammer.

## Lining and studs

Put the piece in the item slot of the Engineering Table's [Fit tab](Engineering_Table.md#fit-tab) and the **lining** (anything fibrous: leather, wool) for a little extra protection, and/or **studs** (a pair) in the attachment slots, one stack of stock to a slot, and press **Apply**:

| On the piece | Gives |
| --- | --- |
| nothing | plain |
| lining | lined |
| studs | studded |
| lining, studs | lined and studded |

To take a lining or studs off, take the piece apart on the Repair tab and fit the rest again; a plain piece is worn as it is.

### Studs

**Studs** are gems, shards or nuggets set into the plate (on any plate; diamond studs in steel plate). Each stud covers a tenth of the piece, up to half, and that share takes the stud's hardness, toughness and wear. Four diamond studs in a steel chestplate lift it from steel's 6.7 points to about 7.2 and keep steel's toughness, where a whole diamond plate is 7.5 points and glass. Studs add a little mass and nothing else. Armor takes no tip: a plate has no edge.

## Axes, aiming and names

Each plate has **axes**: continuous measures you aim at the bench before you make it. A chestplate has **width** and **torso length**, a helmet **face opening** and **crown height**, leggings **length** and **width**, boots **shaft height** and **sole thickness**. The axes also shape the piece you wear: a taller crown, a wider chest, longer legs stretch it, a closed face opening shows a visor, and a boot with a high shaft shows a cuff. Pick the plate's shape in the fan, then use the strip that appears (minus and plus, or drag the plate preview) to set each axis, and click the shape again to make it. More material means more cover and more weight, and more stock. How well the work goes sets how close the made part is to what you aimed at: over or under by chance, never exactly, and a poor forging misses by more.

A word for the range an axis sits in (narrow, wide, short, long, closed, open, thin, thick...) is a **band**. Some bands carry a trait: a long torso is cramped to crouch in, a tall crown is a lever for blows to the head, long leggings tire a sprint and drag in water, a high boot shaft keeps water and powder snow out, a thick sole cuts fall damage, a closed face is a closed visor and an open one sees well.

### Names

A finished piece is named by rule from its plate shape, the bands its axes sit in and the fittings on it. A name gives no bonus: the piece does what its parts, bands and shapes do. The rules, and the figure each band covers, are on [Designations](Designations.md#armor).

## What it gives

- **Protection depends on how hard and strong the plate is.** Iron gives what it does in vanilla: six points in a chestplate. Diamond and netherite give more; **gold and leather give much less**, because they're soft. A plate that is better than iron gives less and less extra: there's a ceiling.
- **Chain covers less than plate** (three quarters as much).
- **Armor toughness** only comes from the very best plate (diamond, netherite).
- **Dense metal resists being pushed.** Heavy metals like gold and netherite give knockback resistance.
- **Weight slows you.** A chestplate of dense metal (gold is nearly three times iron's weight) makes you a little slower. Iron is light enough not to.

The piece is drawn on the vanilla armor shape, **tinted by the plate**, so you can see at a glance what someone is wearing. Each plate variant adds its own pieces to the silhouette:

| Piece | Shape | Added shape |
| --- | --- | --- |
| Helmet | conical, ridged | crest |
| Helmet | pointed-faced | visor and brim |
| Helmet | tailed | neck guard and brim |
| Chestplate | brigandine | collar |
| Chestplate | breastplate, segmented | ridge and pauldrons |
| Chestplate | hauberk, scale, lamellar | collar and skirt |
| Leggings | articulated | knee guards |
| Leggings | mail chausses | thigh plates |
| Boots | sabatons | toe caps |

[Studs](#studs) on a helmet or chestplate show on the piece in the color of their own material.

## Powered suit

A set of armor with servos, a sensor head and a battery bay fitted is an [Exosuit](Exosuit.md).

## Wearing out

Armor wears like any [tool](Tools_and_Weapons.md#when-it-breaks-mending-and-swapping-parts): it stops one point short of gone and gives nothing until it's repaired on the Repair tab of the [Engineering Table](Engineering_Table.md#repair-tab) with stock of the plate's material. Old vanilla armor you find is converted; diamond armor becomes [steel with four diamond studs](Tools_and_Weapons.md#old-vanilla-tools).

Armor can be [plated](Tools_and_Weapons.md#plating) in an [Electrolytic Cell](Electrolytic_Cell.md#electroplating), by [hot dipping](Casting.md#hot-dipping), or with a clad plate from the [Rolling Mill](Rolling_Mill.md#cladding). A sound coat of a harder metal is a skin over the whole plate and adds about a tenth of its hardness.

[Guns](Guns.md#hardness-glass-glancing-armor) read the plate's hardness to decide what a bullet goes through.
