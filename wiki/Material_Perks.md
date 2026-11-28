# Material Perks

Stats are the quantitative half of what a material does to a weapon; **perks** are the qualitative half. Some materials do something to whatever they hit, and **any part made of one gives that perk to the whole weapon**: a silver blade, a prismarine head, a honey-wrapped grip. The level is the highest any one part gives, so two silver parts are not two levels.

Perks apply to melee blows, to [off-hand blows](Combat.md#off-hand-weapons) (by the off-hand weapon's own perks) and to thrown weapons such as a hatchet or harpoon.

A weapon's tooltip lists its perks in gold. All numbers are per level.

| Material | Perk | What it does |
| --- | --- | --- |
| Silver | **Smite** | +2.5 damage against the undead |
| Prismarine | **Impaling** | +2.5 damage against water-based mobs (guardians, squid, fish, dolphins and the like): prismarine's guardian-born resonance disturbs aquatic nervous tissue. Modded water mobs count when a pack adds them to the `minecraft:sensitive_to_impaling` entity-type tag |
| Blaze rod, blaze powder | **Fire aspect** | sets the target alight for 4 seconds. **Costs you:** the part is a flame in your hand, so it [burns](Hazards.md#holding-a-hot-piece) you at the blaze rod's flame temperature unless your gloves or tongs are rated for that heat |
| Blaze powder | **Warming** | stops the cold of powder snow from building up for a while; gives nothing against fire |
| Echo shard, amethyst | **Echo** | the target glows for 3 seconds |
| Lead, mercury, cadmium, antimony, stibnite, cinnabar, galena, vanadinite, chloroform, beryllium | **Toxic** | poisons the target for 3 seconds |
| Slime, natural rubber, synthetic rubber | **Bounce** | pops the target upward |
| Bell metal | **Ring** | a bell sounds; the target is slowed for 2 seconds and sculk hears the blow. Hostile mobs within 12 blocks that have no target turn on you |
| Honey, bitumen, soul resin | **Cling** | slows the target for 3 seconds |
| Ice, blue ice, snow, liquid air, liquid oxygen | **Chill** | builds freezing on the target (about 3.5 seconds of powder-snow chill) if it can freeze |
| Sculk | **Siphon** | a melee kill drops 3 extra experience |
| Ender pearl | **Blink** | a melee kill within 6 blocks steps you into the victim's place |
| Uranium, thorium, plutonium, uraninite, monazite, uranium glass, fuel rod, enriched fuel rod, breeder rod, thorium fuel rod, plutonium fuel rod, spent fuel rod | **Radiant** | a struck target withers for 3 seconds and glows for 5 (and the part is [radioactive](Radiation.md)) |
| Breeze rod, feather down, phantom membrane, aluminum foam, silica aerogel | **Featherlight** | the swing feels **40% less** of that part's mass (never below a fifth). The real mass is unchanged, so a [balance scale](Instruments.md#balance-scale) still reads it in full; the weapon swings quicker |

Not every material suits every part role.

## Aspect alloy perks

Each of the thirty-seven [aspect alloys](Mod_Compats.md#aspect-alloy-names) gives one perk, always at level 1. A part of the alloy gives it when the part is on a held modular tool or weapon, on worn modular armor, or as the head of a [Pivot Mast](Pivot_Mast.md). The item's tooltip lists it. Perks marked with a cooldown work once per wielder in that time. Blow perks also apply when a Pivot Mast head of that alloy strikes a creature. Thrown weapons and guns read only the perks in the table above.

A worn plate also hits back at whoever strikes the wearer with Fire aspect, Chill, Cling and Toxic.

| Alloy | Perk | What it does |
| --- | --- | --- |
| Aerialite | **Featherlight** | the swing feels lighter, as in the table above |
| Chthonium | **Anchor** | worn: no knockback |
| Cindersteel | **Fire aspect** | sets the target alight; burns a bare hand as in the table above |
| Thalassium | **Drench** | the target's fire goes out; water-shy mobs such as blazes and endermen are hurt |
| Eunomium | **Exact** | a swing that is not fully charged lands as a full one |
| Lysium | **Dispel** | strips one good effect from the target |
| Xenium | **Blink** | a kill steps you into its place |
| Azoth | **Etch** | the blow ignores 40% of the target's armor |
| Halcyon | **Placate** | the struck mob forgets you for about 5 seconds |
| Fangsteel | **Dread** | the struck mob flees for about 4 seconds |
| Ferinium | **Rend** | the target bleeds: 4 pulses of 1 damage |
| Noetium | **Insight** | the blow shows, in words, how hurt and how armored the target is |
| Tantalus' Metal | **Covet** | a kill rolls the victim's loot table once more |
| Wiedergang | **Relentless** | a kill leaves the next swing fully charged |
| Daedalium | **Salvage** | a block broken with it drops itself, whole, if it has no block entity |
| Rimeiron | **Chill** | builds freezing on the target |
| Briarsteel | **Verdant** | a kill acts like bone meal where the victim fell |
| Anthropium | **Rapport** | held or worn: villagers sell cheaper, as if your standing were 40 higher; prices only |
| Organon | **Versatile** | the tool digs every block a pickaxe, axe, shovel or hoe would, at its best speed; hardness still decides what drops |
| Dawnsteel | **Dazzle** | blinds the target for 3 seconds |
| Rotium | **Ratchet** | blows in quick succession on one target hit up to 40% harder |
| Adamantium | **Yank** | drags the target toward you, harder the more armor it wears |
| Necrium | **Wither** | the Wither effect for 3 seconds |
| Kinesis | **Swift** | a landed blow gives Speed II for 2.5 seconds; 3 second cooldown |
| Allagium | **Exchange** | swaps places with the target; 5 second cooldown |
| Stormsteel | **Arc** | the blow jumps to up to 2 creatures within 4 blocks for 40% of its damage; 2 second cooldown |
| Goetium | **Hex** | Weakness for 7 seconds |
| Aegis | **Brace** | a landed blow, or worn: a hit taken, gives 3 seconds of Resistance; 4 second cooldown |
| Aesthium | **Echo** | outlines the target; sculk hears |
| Eidolon | **Phase** | the blow ignores a raised shield |
| Skotium | **Shroud** | worn: mobs see you at 40% range in light level 7 or below |
| Kenium | **Stifle** | the target cannot shoot or cast and its projectiles are cancelled, for 5 seconds |
| Quickblood | **Leech** | heals you for 25% of the blow; 2 second cooldown |
| Desmos | **Cling** | slows the target |
| Miasma | **Contagion** | a struck target's harmful effects spread to creatures within 3 blocks |
| Hyalium | **Shard** | a kill bursts into glass that cuts creatures within 2.5 blocks for 25% of the victim's max health |
| Pteron | **Glide** | worn: sneak in the air to fall at a slow drift |

## Why fire aspect burns you

A blaze rod is a fire that does not go out. The mod's hot-contact rules already say what a hot thing does to a bare hand ([Hazards](Hazards.md)), so a flaming weapon uses them: a consequence of holding it, never a lock. Rated [gloves or tongs](Hazards.md#protection) make it safe.

## Silver

Silver is a metal as well as a perk: a soft, bright, very reflective, very conductive one. Its figures are in [Materials](Materials.md#the-metals).

## See also

[Tools and Weapons](Tools_and_Weapons.md) · [Combat](Combat.md) · [Materials](Materials.md)
