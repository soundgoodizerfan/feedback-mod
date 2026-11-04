# Materials

In Feedback, **what something is made of matters more than what item it is**. Machines, magnets and guns don't check for "an iron ingot". They look at the **material**: how hard it is, how dense, whether it's magnetic, how strong. Any item made of that material behaves the same way.

## Amounts: Qu

Amounts of metal and other stuff are counted in **Qu**. See [Units](Units.md#amount-qu).

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
| Iron | 4.5 | 7.87 | 5 | 1538 Tu | **Magnetic.** Pure iron: what hematite, limonite and magnetite smelt to. Softer and weaker than wrought iron. |
| Wrought Iron | 6 | 7.9 | 8 | 1600 Tu | **Magnetic.** Vanilla's iron: soft, tough, worked cold. |
| Cast Iron | 12 | 7.2 | 6 | 1200 Tu | **Magnetic.** Too brittle for plates, rods or wire; good for steelmaking. |
| Steel | 15 | 7.9 | 15 | 1450 Tu | **Magnetic.** Only moves between **900 and 1100 Tu**. See [Making Steel](Steel.md). |
| Hardened Steel | 25 | 7.9 | 35 | 1450 Tu | **Magnetic.** Only the [Grinding Wheel](Grinding_Wheel.md) can shape it. |
| Tempered Steel | 20 | 7.9 | 28 | 1450 Tu | **Magnetic.** [Tempered](Heat_Treatment.md#tempering) hardened steel: tough again. Worked cold, never hot. Springs, blades, gun parts. |
| Chromium | 9 | 7.19 | 12 | 1907 Tu | Hard and stainless. For stainless steel and the redstone alloy. |
| Nickel | 4 | 8.91 | 6 | 1455 Tu | **Magnetic.** For stainless steel and cermet. |
| Cobalt | 5 | 8.9 | 8 | 1495 Tu | **Magnetic.** The binder in cermet. |
| Manganese | 6 | 7.21 | 6 | 1246 Tu | Brittle alone. For manganese steel. |
| Aluminum | 1.5 | 2.7 | 2 | 660 Tu | Light and soft; conducts well for its weight. Made from bauxite by [electrolysis](Making_Aluminum.md). |
| Titanium | 6 | 4.51 | 12 | 1668 Tu | Light, strong, stands the heat. For cermet. |
| Tungsten | 7.5 | 19.25 | 20 | 3422 Tu | The highest melting point of any metal. |
| Thorium | 2.5 | 11.7 | 3 | 1750 Tu | **Radioactive**, weaker than uranium. See [Thorium](#thorium). |
| Stainless Steel | 18 | 7.9 | 18 | 1450 Tu | Steel with chromium and nickel. Barely corrodes. |
| Manganese Steel | 22 | 7.8 | 22 | 1400 Tu | Steel with manganese. Hardens as it is worked. |
| Tungsten Carbide | 40 | 15.6 | 40 | 2870 Tu | Hard and brittle. Sintered from powder with cobalt ([Furnaces](Furnaces.md#sintering)). |
| Titanium Carbide | 36 | 4.9 | 36 | 3160 Tu | Titanium baked with graphite. Hard and brittle. The grain in cermet. |
| Thorium-Tungsten | 8 | 19 | 22 | 3400 Tu | Tungsten with a little thorium. Stands 2800 Tu. |
| Cermet | 38 | 6 | 35 | 2900 Tu | **Magnetic.** Titanium carbide grains in an iron, nickel and cobalt binder, sintered from powder ([Furnaces](Furnaces.md#sintering)). |
| Redstone-Chromium | 5 | 4.5 | 6 | 1300 Tu | Redstone with a little chromium. A drawn alloy of redstone. |
| Uranium | 6 | 19.1 | 7 | 1132 Tu | **Radioactive.** The densest metal. See [Uranium](#uranium). |
| Netherite | 40 | 13.5 | 50 | 2600 Tu | **Magnetic.** Harder than every cutting tool. |

What the columns mean in play:

- **Hardness** decides what can shape a metal. A blow weaker than the hardness, or a cutting tool not harder than the metal, does nothing at all. Above that, each blow does **strength of the blow ÷ hardness** in work. See [Metalworking](Metalworking.md). It also decides what a bullet goes through: see [Guns](Guns.md#hardness-glass-glancing-armor).
- **Density** (kg per liter, water is 1) decides how heavy things are: bullets, guns, engines and vehicle plates. Dense bullets keep their energy.
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
| Diamond | 100 | | | armor: nothing pierces it |
| Heavy Core | 9 | | | found, not made: see [Heavy Core](Heavy_Core.md) |
| Silicon, glasses, graphite, polymers | | | | sections below |

Every material also decides how well it **takes an enchantment** when it's the main part of a [tool or piece of armor](Tools_and_Weapons.md): gold is best, then leather, wood, iron and netherite, with stone worst.

"Stands" is the temperature a grip or glove can bear against your hand. See [Hazards](Hazards.md).

## Graphite

A soft, dark, fireproof mineral. Coal baked at 2200 to 2600 Tu turns to graphite; above 2900 Tu the batch is ruined into Silicate Slag. Graphite is also mined as an ore. It is the carbon mixed into molten titanium and tungsten to make [titanium carbide and tungsten carbide](Mixer.md#alloying-with-heat).

Graphite ore sits in veins in stone and deepslate, rarer than coal, in small seams of dark flecks; the two look alike apart from the rock around them. It is soft, softer than coal ore, but needs a pickaxe. Mining it gives graphite lumps; Silk Touch gives the ore block. A loose rock on the surface marks a vein ([Ore Deposits](Ore_Deposits.md)). Graphite stands heat that would ruin most things, which suits the parts of a build next to the hottest fires ([Refractory Lining](Refractory_Lining.md), [Heat](Heat.md)).

## Saltpetre

A pale mineral mined from cave deposits (Y -16 to 64; [Ore Deposits](Ore_Deposits.md)). It supplies oxygen to a burn: a creeper in a [Creeper Pen](Creeper_Pen.md) eats it as oxidiser. Crafted with sulfur and charcoal it makes gunpowder.

```
3 saltpetre + 1 sulfur + 1 charcoal (shapeless) -> 4 gunpowder
```

## Sulfur

A yellow, flammable mineral from Nether deposits. It melts at 115 Tu and burns as fuel: a creeper in a [Creeper Pen](Creeper_Pen.md) eats it as fuel, and it goes into gunpowder with [saltpetre](#saltpetre).

## Uranium

A dense, silvery-green metal that gives off radiation, found as the ore **uraninite**.

Uraninite grows in rare, deep veins (Y -64 to 16), rimmed with galena (the lead that uranium decays into) and flecked with pyrite. A [Survey Meter](Instruments.md#survey-meter) finds the ore; a [Geiger Counter](Instruments.md#geiger-counter) finds it by ear. Uraninite ore, its lumps and every uranium form are [radioactive](Radiation.md), the ore weaker than the metal.

Uraninite melts into uranium (1132 Tu) like any ore, and molten uranium casts into ingots. Uranium makes every metal form: plates, rods, wire, blocks and the rest. A block of uranium is nine ingots of radiation in one place.

- It is the densest metal in the game (19.1), twice lead's weight per block. Heavy parts make heavy hammers, counterweights and shot.
- **Radiant**: a weapon with a uranium part withers what it hits and makes it glow for a few seconds. Whoever carries the weapon carries the part, and takes the dose.
- It is **luminous**.

See [Radiation](Radiation.md) before carrying any of it.

## Thorium

A heavy, soft, silvery metal that gives off radiation, found as the ore **monazite**.

Monazite grows in deep veins (Y -48 to 16), with black sand over some of them. The ore and every thorium form are [radioactive](Radiation.md): the ore at 0.1, the metal at 0.25, a quarter of uranium's. A [Survey Meter](Instruments.md#survey-meter) finds the ore; a [Geiger Counter](Instruments.md#geiger-counter) finds it by ear.

Monazite melts into thorium (1750 Tu) like any ore, and molten thorium casts into ingots. It makes every metal form.

Thorium stands heat. Melted into tungsten in a [crucible](Crucible.md) (about 1 part thorium to 9 of tungsten) it makes **thorium-tungsten**, which stands 2800 Tu in service. Carry it with care: see [Radiation](Radiation.md).

## Silicon

A dark, brittle metalloid. It casts like a metal but breaks like glass, and it is for glass and optics. Hardness 10, melts at 1414 Tu.

- **Crude Silicon**: sand with charcoal at 1600 to 1800 Tu. Above 1950 Tu the batch is ruined into Porous Sinter.
- **Silicon**: nether quartz with charcoal at 1600 to 1800 Tu. Purer. Above 1950 Tu the batch is ruined into Silicate Slag.

Both cast into ingots. Plates and rods are worked from the ingot like a metal's, with a hard enough hammer.

## Silica Glass and Optical Glass

Melt sand with a silicon ingot in a crucible at 1500 Tu or more and it makes glass. Crude silicon makes **Silica Glass**; pure silicon makes **Optical Glass**, the clearest there is. Both have hardness 30, insulate to HV, cast into ingots, and plates and rods are worked from them. See [Optics](Optics.md).

## Solders

Plain **tin** is a solder: a Tin Ingot pastes a board as it is, no alloying needed, which makes it the solder for a first circuit. Two better ones are made by melting tin with lead or with a little copper in a [crucible](Crucible.md) or [mixer](Casting.md) and casting ingots like any metal.

| Solder | Mix | Melts at | Good window | Hazard |
| --- | --- | --- | --- | --- |
| Tin | pure tin | 232 Tu | 20 Tu above | none |
| Leaded Solder | tin and lead | 183 Tu | 60 Tu above | poisons while hot |
| Lead-Free Solder | tin and a little copper | 227 Tu | 30 Tu above | none |

The **window** is how far above its melting point a joint is still good. A thinner melt wets better, so lead solder forgives more; lead-free is cleaner but wants a steadier hand or a better oven. Pure tin skins over with oxide and gives the narrowest window of the three: the iron has to be held close to the melting point. A printed board's traces lift at 260 Tu, so on one the tin window leaves little room above it.

What makes a good joint:

- Reach the melting point and stay on the pad long enough (hand iron: drag at a steady pace; each pad is judged on its own, and a pad you sweep over stays cold).
- Stay inside the window. Above it the flux burns and the joint goes **cold**.
- Stay under what the traces and substrate stand (a printed trace lifts first). Past that the traces **lift**.

Working leaded solder while it is hot poisons whoever is within 3 blocks. Poison never kills. Any other mod's alloy works if its material file marks it as a solder. The [Soldering Iron](Soldering_Iron.md) and [Reflow Oven](Reflow_Oven.md) apply it to a [Trace Board](Trace_Board.md).

## Polymers

Plastics and rubbers. They are light, soft, poor conductors and cheap to shape, and they come from oil. Like a metal, a polymer is a material: any item made of one behaves the same, and it comes in a family of **forms**.

| Polymer | Hardness | Density | Melts at | Stands | Grip friction | Insulates |
| --- | --- | --- | --- | --- | --- | --- |
| Polyethylene | 2 | 0.95 | 130 Tu | 80 Tu | 0.3 | HV |
| Polypropylene | 2 | 0.9 | 165 Tu | 100 Tu | 0.3 | MV |
| Synthetic Rubber | 1 | 1.1 | 180 Tu | 90 Tu | 1.2 | HV |
| PVC | 3 | 1.4 | 160 Tu | 60 Tu | 0.4 | MV |
| Soul Resin | 2 | 1.1 | 200 Tu | 160 Tu | 0.4 | MV |
| Bitumen | 1 | 1.05 | 100 Tu | 50 Tu | 0.5 | LV |

"Stands" is the temperature the polymer can bear before it softens, the same figure grips and gloves use ([Hazards](Hazards.md)). Soul resin stands twice what polyethylene does.

### Making polymers

A polymer starts as a **melt**, a hot fluid.

- **Polyethylene Melt** comes from the [Reactor](Reactor.md): ethylene gas, polymerized over a catalyst at 120 to 220 Tu and a little pressure.
- **Soul Resin Melt** is the Nether's plastic, a stranger and tougher cousin of polyethylene. It comes from the Reactor too: [soul oil](Oil.md#soul-oil) polymerized over a catalyst at 180 to 300 Tu and a little pressure (100 mB gives 20 mB).

Pour a melt into an **Ingot Mold** from a [crucible or ladle](Casting.md) and it sets into an **ingot** (polyethylene at 100 Tu, soul resin at 150). From the ingot the other forms follow:

| Form | Made by |
| --- | --- |
| Ingot | casting a melt |
| Block | nine ingots, crafted (and back) |
| Insulation | drawing an ingot at the [Wire Drawer](Wire_Drawer.md) |
| Pipe | drilling an ingot at the [Drill Press](Drill_Press.md) |

Drilling gives a [Pipe](Pipe.md) of that polymer; a metal ingot drills into one the same way.

### Polymer insulation

Every polymer has an **insulation band**, shown in the table: how high a voltage it holds back. Put three of any polymer item (the *Insulation* form is the neat one) in a row under three [Copper Wire](Wire_Drawer.md) and the [cable](Copper_Cable.md) comes out insulated to that band. A cable that sees more voltage than its band loses its coat. The bands run LV, MV, HV, UHV, EHV: polyethylene and rubber hold HV, which quartz glass and quartz beat.

### Polymer tires and grips

A **Block** of a polymer is tire stock. Hold it in the off-hand and right-click a hinge with the Welder or Pivot Setter as with any block ([tires](Vehicles.md#tires)). **Synthetic rubber** grips at 1.2, against 0.4 for planks and 0.6 for iron; only slime and honey grip more. It is soft, so a rubber tire flexes as it rolls and drags like any soft tire.

A polymer grip on a tool or gun raises how securely it is held, by its friction. Rubber's is highest of the polymers.

## Ore minerals

Ore minerals are not metal. Each melts into the metal it carries the most of. Find them in [Ore Deposits](Ore_Deposits.md).

| Mineral | Smelts to |
| --- | --- |
| Hematite, Limonite, Magnetite | Iron |
| Chalcopyrite, Malachite, Cuprite | Copper (chalcopyrite carries iron too) |
| Sphalerite | Zinc |
| Argentite | Silver |
| Chromite | Chromium |
| Pentlandite | Nickel |
| Cobaltite | Cobalt |
| Pyrolusite | Manganese |
| Ilmenite | Titanium |
| Wolframite | Tungsten |
| Uraninite | Uranium |
| Monazite | Thorium |

**Galena** and **Pyrite** are crystals that rectify, which is why a [Cat's Whisker Detector](Cats_Whisker_Detector.md) wants one. Galena melts down to lead.

## Forms

Every metal comes in a family of **forms**, each made by a particular machine:

| Form | Made by |
| --- | --- |
| Ingot, Nugget, Block | crafting (nine nuggets ↔ ingot ↔ block) |
| Plate | hammering an ingot |
| Foil | hammering a plate (keep going) |
| Scrap | hammering foil (too far) |
| Rod | [Rolling Mill](Rolling_Mill.md) |
| Pipe | [Drill Press](Drill_Press.md) (an ingot) or [Rolling Mill](Rolling_Mill.md) (a plate) |
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

Copper, gold and wrought iron come from vanilla's ores (iron ore and raw iron melt to wrought iron). The iron minerals hematite, limonite and magnetite ([Ore Deposits](Ore_Deposits.md)) smelt to pure **iron** instead: an ore melts into the metal it carries the most of. Brass and bronze are [alloyed](Casting.md#alloys). Steel and cast iron are made in a [crucible](Steel.md). Pure copper can also be refined out of brass or scrap in the [Electrolytic Cell](Electrolytic_Cell.md).

Tin, zinc and lead are ores in the ground. **Silver** comes from **argentite**, which fills its own silver lode ([Ore Deposits](Ore_Deposits.md)) and shows up in a thin scatter through galena veins. Argentite smelts to silver (see [ore minerals](#ore-minerals)). Silver comes as ingots, nuggets and blocks.

JEI's **Material Stats** page shows every figure a material has (kind, XP top level, XP capacity, XP conductance, XP leak, oxygen share, hardness, density, heat, strength, electrical, and for foods moisture, fat, fiber, germs and toxin in Qu, with its compounds and perks). Figures that are zero are left out, and hovering a row says what it means. Hardness, density, melting point, service temperature, thermal conductivity, toughness, yield, friction and flame temperature carry a word beside the figure (low, middling or high, against the range across all materials); holding Shift while hovering gives the edges. Holding Shift over an item shows the same word beside its hardness, melting point and burning temperature.

Some materials also carry a **[perk](Material_Perks.md)**: what they do to whatever a weapon made of them hits.
