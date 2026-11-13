# Creeper Pen

A cage that holds a creeper and feeds it. Fed the right things, the creeper grows a reserve of charge that is sheared off as a **Creeper Sac** and cut into **Creeper Charge**, a powder stronger than gunpowder.

## Crafting

```
B B B
B   B
P P P
```

B is Iron Bars and P is Wrought Iron Plate.

## Using it

- **Lead a creeper in**: use a lead on a creeper to leash it (vanilla refuses to leash a creeper; here it takes a lead like an animal, and a leashed creeper still chases and can blow), walk it to the pen, and use the lead on the pen. The lead is used up and the creeper is kept inside. The pen shows an empty cage, a creeper, or a swollen creeper.
- **Feed it**: right-click with an item. The creeper eats any item whose material burns and has a combustion balance: items with spare oxygen such as [saltpetre](Materials.md#saltpetre) fill the **oxidiser** half of its stomach, items that want oxygen such as [sulfur](Materials.md#sulfur), coal and charcoal fill the **fuel** half. Gunpowder fills both halves at once, 1.5 units each per item. Anything else is refused. Each half holds 96 units.
- **Shear it**: any shears take a Creeper Sac holding the whole reserve. The creeper is left empty and cannot blow up until it has grown a reserve again.
- **Break the pen**: the creeper comes out, charged if it was charged.

## How it works

While both halves of the stomach have food, the reserve grows toward a cap of 64 units of charge. The best case takes 20 minutes. A pen in an unloaded chunk catches up when you return. A creeper only grows while fed.

### Charged creeper

Lightning striking within 3 blocks of a pen with a creeper makes it charged for good. A charged creeper holds twice the reserve, and its blast also shocks everything in reach at EHV ([Shock](Hazards.md#shock)).

## Misuse and hazards

- **A full reserve blows.** At the cap the pen swells and hisses for 30 seconds; sculk sensors hear it. Shearing during that time empties the pen and cancels the blast. After 30 seconds it explodes with a power that grows with the reserve: 8 at full, 12 at most. A pen whose warning ran out while its chunk was unloaded blows when the chunk loads.
- **Sparks set it off.** An electrical spark within 16 blocks detonates a pen holding at least 1 unit.
- **Shearing can go wrong.** Shearing a loaded pen can set the reserve off, more often the fuller it is. Sneaking halves the chance, so do padded hands (any hand protection above bare hands) and the careful-hands skill ([Skills](Skills.md)).

## Creeper Sac

A sac holds the reserve shears took. Its tooltip says how much: an empty, small, heavy or swollen sac. A sac goes off if shocked, burnt or sparked.

To cut it, hold the sac and a **Hand Knife** in the other hand and right-click. The sac becomes **Creeper Charge**, one per 4 units; the remainder is lost and the knife loses a point. ([Hand Knife](Prep_Table.md))

A dropped sac or charge in fire or lava goes off. A player carrying them who takes fire, blast, lightning or fall damage sets all of them off at once, with a power that grows with the total carried.

## Creeper Charge

A powder that carries its own oxygen, with a peak pressure of 12 Pu against gunpowder's 3, and it burns fast. It works wherever a powder does: the [Reloading Press](Reloading_Press.md), the [Mixer](Mixer.md), and demolition charges. A gun chamber rated below its pressure fails ([Overpressure](Guns.md#overpressure)). See [Ammunition](Ammunition.md#creeper-charge).

Wild creepers still drop gunpowder, like half-digested feed. A penned creeper eats gunpowder and gives nothing back.
