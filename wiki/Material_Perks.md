# Material Perks

Stats are the quantitative half of what a material does to a weapon; **perks** are the qualitative half. Some materials do something to whatever they hit, and **any part made of one gives that perk to the whole weapon**: a silver blade, a prismarine head, a honey-wrapped grip. The level is the highest any one part gives, so two silver parts are not two levels.

Perks apply to melee blows, to [off-hand blows](Combat.md#off-hand-weapons) (by the off-hand weapon's own perks) and to thrown weapons such as a hatchet or harpoon.

A weapon's tooltip lists its perks in gold. All numbers are per level.

| Material | Perk | What it does |
| --- | --- | --- |
| Silver | **Smite** | +2.5 damage against the undead |
| Prismarine | **Impaling** | +2.5 damage against water-based mobs (guardians, squid, fish, dolphins and the like): prismarine's guardian-born resonance disturbs aquatic nervous tissue. Modded water mobs count when a pack adds them to the `minecraft:sensitive_to_impaling` entity-type tag |
| Blaze rod | **Fire aspect** | sets the target alight for 4 seconds. **Costs you:** the part is a flame in your hand, so it [burns](Hazards.md#holding-a-hot-piece) you at the blaze rod's flame temperature unless your gloves or tongs are rated for that heat |
| Echo shard | **Echo** | the target glows for 3 seconds |
| Lead | **Toxic** | poisons the target for 3 seconds |
| Slime | **Bounce** | pops the target upward |
| Honey | **Cling** | slows the target for 3 seconds |
| Ice, blue ice | **Chill** | builds freezing on the target (about 3.5 seconds of powder-snow chill) if it can freeze |
| Sculk | **Siphon** | a melee kill drops 3 extra experience |
| Ender pearl | **Blink** | a melee kill within 6 blocks steps you into the victim's place |
| Uranium | **Radiant** | a struck target withers for 3 seconds and glows for 5 (and the part is [radioactive](Radiation.md)) |
| Breeze rod | **Featherlight** | the swing feels **40% less** of that part's mass (never below a fifth). The real mass is unchanged, so a [balance scale](Balance_Scale.md) still reads it in full; the weapon swings quicker |

Not every material suits every part role.

## Why fire aspect burns you

A blaze rod is a fire that does not go out. The mod's hot-contact rules already say what a hot thing does to a bare hand ([Hazards](Hazards.md)), so a flaming weapon uses them: a consequence of holding it, never a lock. Rated [gloves or tongs](Hazards.md#protection) make it safe.

## Silver

Silver is a metal as well as a perk: a soft, bright, very reflective, very conductive one. Its figures are in [Materials](Materials.md#the-metals).

## See also

[Tools and Weapons](Tools_and_Weapons.md) · [Combat](Combat.md) · [Materials](Materials.md)
