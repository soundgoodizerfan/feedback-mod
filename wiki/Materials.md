# Materials

In Feedback, **what something is made of matters more than what item it is**. Machines, magnets and guns don't check for "an iron ingot". They look at the **material**: how hard it is, how dense, whether it's magnetic, how strong. Any item made of that material behaves the same way.

## The metals

| Metal | Hardness | Density | Strength | Melts at | Notes |
| --- | --- | --- | --- | --- | --- |
| Lead | 0.3 | 11.3 | 0.5 | 327 Tu | Dense: the classic bullet metal. Useless for guns. |
| Silver | 0.9 | 10.5 | 3 | 962 Tu | Soft and bright; the best conductor. Its [smite](Material_Perks.md) hurts the undead. |
| Gold | 0.5 | 19.3 | 2 | 950 Tu | |
| Tin | 0.6 | 7.3 | 1 | 232 Tu | For bronze. |
| Copper | 1 | 9.0 | 4 | 1000 Tu | The first metal you'll work. Wire, cable, plates, foil. |
| Zinc | 2 | 7.1 | 3 | 420 Tu | For brass and the [Galvanic Cell](Galvanic_Cell.md). |
| Brass | 4 | 8.5 | 5 | 900 Tu | 65% copper, 35% zinc. Good cartridge cases. |
| Bronze | 5 | 8.8 | 6 | 950 Tu | 90% copper, 10% tin. |
| Wrought Iron | 6 | 7.9 | 8 | 1600 Tu | **Magnetic.** Vanilla's iron: soft, tough, worked cold. |
| Cast Iron | 12 | 7.2 | 6 | 1200 Tu | **Magnetic.** Too brittle for plates, rods or wire; good for steelmaking. |
| Steel | 15 | 7.9 | 15 | 1450 Tu | **Magnetic.** Only moves between **900 and 1100 Tu**. See [Making Steel](Steel.md). |
| Hardened Steel | 25 | 7.9 | 35 | 1450 Tu | **Magnetic.** Only the [Grinding Wheel](Grinding_Wheel.md) can shape it. |
| Tempered Steel | 20 | 7.9 | 28 | 1450 Tu | **Magnetic.** [Tempered](Heat_Treatment.md#tempering) hardened steel: tough again. Worked cold, never hot. Springs, blades, gun parts. |
| Netherite | 40 | 13.5 | 50 | 2600 Tu | **Magnetic.** Harder than every cutting tool. |

What the columns mean in play:

- **Hardness** decides what can shape a metal. A blow weaker than the hardness, or a cutting tool not harder than the metal, does nothing at all. Above that, each blow does **strength of the blow ÷ hardness** in work. See [Metalworking](Metalworking.md). It also decides what a bullet goes through: see [Guns](Guns.md#hardness-glass-glancing-armour).
- **Density** (kg per litre, water is 1) decides how heavy things are: bullets, guns, engines and vehicle plates. Dense bullets keep their energy.
- **Strength** decides how much pressure a gun chamber or a cartridge case holds, and how fast an engine can rev. A 9 mm chamber with a 3 mm wall holds 1.33 × strength in Pu. See [Gun Parts](Gun_Parts.md#the-chamber).
- **Melting point** is where a crucible melts it, and where a [Fuse](Fuse.md) wire or a pair of [tongs](Hazards.md#tongs) gives out.
- **Magnetic** metals are pulled by the [Electromagnet](Electromagnet.md) and work as [coilgun](Coilgun.md) slugs.

Electrical figures (how much current a wire of each metal carries, its voltage loss, and its electrode potential) are on the [Fuse](Fuse.md) and [Electrolytic Cell](Electrolytic_Cell.md#voltage-picks-the-metal) pages.

## Other materials

Not everything is metal. A few others matter for parts and protection:

| Material | Hardness | Grip friction | Stands | Used for |
| --- | --- | --- | --- | --- |
| Wood | 0.5 | 0.4 | 150 Tu | grips turned from a stick |
| Bamboo | 1 | 0.4 | 150 Tu | grips turned from a stalk |
| Bone | 3 | 0.5 | 200 Tu | grips turned from a bone |
| Leather | 0.3 | 0.6 | 100 Tu | gloves; wrapping a grip |
| Wool, Paper | | 0.5 | | wrapping a grip |
| Honey | | 1.5 | | wrapping a grip: the grippiest there is |
| Diamond | 100 | | | armour: nothing pierces it |
| Heavy Core | 9 | | | found, not made: see [Heavy Core](Heavy_Core.md) |
| Graphite | | | | coal baked at 2200 to 2600 Tu; no use yet |

Every material also decides how well it **takes an enchantment** when it's the main part of a [tool or piece of armour](Tools_and_Weapons.md): gold is best, then leather, wood, iron and netherite, with stone worst.

"Stands" is the temperature a grip or glove can bear against your hand. See [Hazards](Hazards.md).

## Forms

Every metal comes in a family of **forms**, each made by a particular machine:

| Form | Made by |
| --- | --- |
| Ingot, Nugget, Block | crafting (nine nuggets ↔ ingot ↔ block) |
| Plate | hammering an ingot |
| Foil | hammering a plate (keep going) |
| Scrap | hammering foil (too far) |
| Rod | [Rolling Mill](Rolling_Mill.md) |
| Wire, Fine Wire | [Wire Drawer](Wire_Drawer.md) |
| Case | drawing a plate |
| Bullet | hammering a nugget, or [casting](Casting.md) |
| Drilled Plate | [Drill Press](Drill_Press.md) |
| Turned Rod | [Lathe](Lathe.md) |
| Ground Ingot | [Grinding Wheel](Grinding_Wheel.md) |
| Swarf | the leftovers of drilling, turning and grinding |
| Gun Barrel, Receiver, Slide, Bolt, Trigger Group, Grip, Cylinder, Magazine Tube, Suppressor, Muzzle Brake, Relief Vent, Barrel Shroud | machined: see [Gun Parts](Gun_Parts.md) |

So there's a *Bronze Plate*, *Lead Wire*, *Gold Foil* and so on, for every metal. A few don't exist: cast iron is too brittle to be made into plates, rods or wire, and neither is hardened steel. Tempered steel is: plate, rod, wire and the machined parts, but no foil, nuggets or blocks.

How much work each step takes depends on the metal: **a fixed amount plus a little more for every point of hardness**. A copper plate takes 14 Fu; a steel plate 60. JEI shows the figure for every step and every metal.

## Where to find them

Copper, iron and gold come from vanilla ores. Brass and bronze are [alloyed](Casting.md#alloys). Steel and cast iron are made in a [crucible](Steel.md). Pure copper can also be refined out of brass or scrap in the [Electrolytic Cell](Electrolytic_Cell.md).

Tin, zinc and lead are ores in the ground. **Silver** has no ore of its own: heat galena hot enough and a little silver comes out of it. Silver comes as ingots, nuggets and blocks.

Some materials also carry a **[perk](Material_Perks.md)**: what they do to whatever a weapon made of them hits.
