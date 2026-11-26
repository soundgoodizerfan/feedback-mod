# Mod Compats

The mods Feedback works with, and what each one gives you. Some are required, some are optional, and Thaumaturge changes nothing without it.

| Mod | Status | What it does for you |
|---|---|---|
| [Sable](#sable-dimensional-sable-and-sable-scaling) | Required | Moving physics bodies for [Vehicles](Vehicles.md) |
| [Dimensional Sable](#sable-dimensional-sable-and-sable-scaling) | Required | Carries a craft between dimensions |
| [Sable Scaling](#sable-dimensional-sable-and-sable-scaling) | Required | Scales a craft |
| [Curios](#curios) | Required | Worn slots for gloves and combat gear |
| [Better Combat](#better-combat) | Required | Swing animations and attack arcs |
| [Flywheel](#flywheel) | Required | Rendering on the client |
| [JEI](#jei) | Optional | Recipe, process and info pages |
| [Jade](#jade) | Optional | Readouts on blocks and villagers |
| [Thaumaturge](#thaumaturge) | Optional | Aspects on Feedback items, flux and essentia machines, essentia turned to gas, a vis-fed current source, thaumium alloys, wand caps and rods |
| [Iris](#iris) | Optional | Shader packs: the fluid caster's flame follows the pack |
| [Serene Seasons](#serene-seasons) | Optional | The season shifts the Overworld's room temperature |
| [Legendary Survival Overhaul](#legendary-survival-overhaul) | Optional | Body temperature and thirst follow Feedback's rooms, machines, armor, food and fluids |
| [EMI](#emi) | Optional | Shows Feedback's recipe cards through its JEI bridge |
| [Other mods' foods](#other-mods-foods) | Optional | Any food from another mod keeps and spoils like Feedback food |
| [Inventory Sorter](#inventory-sorter) | Optional | Sorting skips screens whose slots are roles |
| [Simple Voice Chat](#simple-voice-chat) | Optional | Carries the [Radio Set](Radio_Set.md)'s voice |
| [Xaero's Minimap](#xaeros-minimap) | Optional | Survey Meter readings pin to the map |
| [Applied Energistics 2](#applied-energistics-2) | Optional | Certus Quartz and Fluix crystals and dusts count as materials |
| [Artifacts](#artifacts) | Optional | Engineering Table recipes for its items |
| [Cataclysm](#cataclysm) | Optional | Black Steel, Ancient Metal, Witherite, Ignitium and Cursium count as materials |
| [Born in Chaos](#born-in-chaos) | Optional | Dark Metal counts as a material |
| [Thaumic Bases](#thaumic-bases) | Optional | Thauminite counts as a material |
| [Deeper and Darker](#deeper-and-darker) | Optional | Resonarium and Soul Crystal count as materials |
| [Other mods](#other-mods) | Optional | Fluid tags, shared biome and stone tags, and Feedback blocks accepted in place of vanilla ones |

## Sable, Dimensional Sable and Sable Scaling

**Required.** Sable turns a build into one moving physics body, which is what a [Lift](Vehicles.md#the-lift) does to a vehicle. Dimensional Sable lets such a body cross between dimensions, as a craft does when it climbs to [The Gap](The_Gap.md) and returns. Sable Scaling resizes a body. All three must be installed.

## Curios

**Required.** [Leather Gloves](Hazards.md#gloves) sit in Curios' hands slot, and the three pieces of [Combat Gear](Combat_Gear.md) each take a slot that the mod adds to the Curios screen.

## Better Combat

**Required.** Weapon swings play Better Combat's attack animations and arcs, which the [Combat](Combat.md) page builds on. Better Combat needs Cloth Config installed alongside it.

## Flywheel

**Required** on the client. It draws moving machine parts and bodies. A dedicated server does not need it, and it adds no items or screens of its own.

## JEI

**Optional.** With Just Enough Items installed, the recipe browser covers Feedback's tables as well as its crafting recipes:

- Cards for thermal processes (melting, smelting, reactions, boiling, quenching, dips, electrolysis, freezing), deformation and removal, alloy mixes, fuels, ore veins and fluid deposits, villager goods, brewing ingredients, soldering, guns and tool and part benches. [Heat](Heat.md) describes the thermal cards.
- A **Material Stats** card for every material, and a **Separation** card per material that a separator can tell apart. See [Materials](Materials.md). The item box of a stats card lists every item that is the material, from other mods too.
- Info pages on machines, stations and items that have no recipe card, saying what they do. They never give away a puzzle's answer.
- Crafting cards for recipes that take any material, cycling through every material that fits.
- Vanilla smelting, blasting and smoking cards, vanilla ores and the vanilla beds are hidden, because Feedback replaces them.

The [Engineering Table](Engineering_Table.md) screen and the [Computer](Electric_Controller.md) screen give JEI room: a Computer window in fullscreen hides JEI's lists.

## Jade

**Optional.** With Jade installed, looking at a block shows a readout of its state: rotation on [shafts and machines](Rotation.md), temperature on [heat machines](Heat.md), the band on [electric lines](Electricity.md), food equipment, [experience](Experience.md) stores, the [Fission Reactor](Fission_Reactor.md), a worn or workpiece-holding block, a [Pallet Rack](Pallet_Rack.md) and a [Warehouse Dock](Warehouse_Dock.md), and a villager's standing with you (see [Villages](Villages.md)).

Readouts follow the instrument rule: a requirement printed on a part shows exactly, but a state such as a temperature, a speed or a band shows as a word until you carry an [instrument](Instruments.md) that reads it. Then it shows a figure.

## Thaumaturge

**Optional.** With the **Thaumaturge** mod installed (a Thaumcraft port with aspects and a Thaumometer for scanning), nearly every Feedback item carries Thaumaturge aspects, so scanning them works as it does for any other item. Amounts differ from item to item. Without Thaumaturge, nothing changes.

### Materials

Every form of a material (ingot, dust, block, plate, nugget and the rest) carries aspects worked out from what the material is and how it behaves:

| Property of the material | Aspect |
|---|---|
| Metal | Metallum (metal) |
| Mineral | Terra (earth) |
| Glass and gems | Vitreus (crystal) |
| Fluids | Aqua (water) or Aer (air) |
| Plants | Herba (plant) |
| Animals | Bestia (beast) and Victus (life) |
| Light animal fiber, such as [feather down](Materials.md#feather-down-and-rabbit-hide) | Volatus (flight) |
| Electrical conductivity | Potentia (energy) |
| High reflectivity | Lux (light) |
| Flammable or burning | Ignis (fire) |
| Toxicity | Mortuus (death) |
| Hardness and alloys | Ordo (order) |
| Magnetism | Motus (motion) |
| Powders | Perditio (entropy) |
| Plastics, rubbers and composites | Fabrico (craft) |
| Insulating plastics and rubbers | Praemunio (ward) |
| Elastic rubber, such as tire rubber | Motus (motion) |
| Bonded composites | Ordo (order) |

Amounts follow the size of the form: a nugget carries less than an ingot, a block more. They also follow the material's figures, so a denser metal carries more Metallum and a better conductor more Potentia. See [Materials](Materials.md) for the properties themselves.

### Thaumaturge items as materials

Thaumaturge items that match a Feedback material are read as that material in every process, as any item tagged as that metal is. Using one opens the material's stats card, whose item box lists it.

- Quicksilver and its nuggets are mercury.
- Clusters (iron, gold, copper, tin, silver, lead, cinnabar and quartz) are a cluster form of those metals and minerals.
- Amber and cinnabar ores, blocks and raw items are amber and cinnabar.
- Rare earth is rare earth residue.
- Each essentia crystal is a crystal material of its aspect, from Aer Essentia to Vitium Essentia (see [Essentia and thaumium](#essentia-and-thaumium)). The air, fire, water, earth, order, entropy and flux world crystals are also the essentia of their aspect.
- Thaumium is iron with a little Praecantatio and Terra essentia. Thaumium and void metal exist only with Thaumaturge installed. Their ingot, nugget, plate and block are Thaumaturge's own items, and they melt, cast and work like any other metal's forms.
- Void metal is dense, very hard, brittle and dark (88% osmium, 8% ender pearl, 2% Metallum essentia, 2% Vitium essentia). It remembers a shape: its transition is **80 Tu**, so a coil spring of it opens a [Thermal Valve](Valve.md#thermal-valve) at 80 Tu.
- Thaumaturge's thaumium and void swords, pickaxes, axes, shovels, hoes, helmets, chestplates, leggings and boots are converted on the spot into the modular item of that kind, headed or plated in that metal, like vanilla tools (see [Old vanilla tools](Tools_and_Weapons.md#old-vanilla-tools)). Robes and shields are not converted. Other mods' recipes and JEI pages that ask for one of these items accept the converted item too.
- Greatwood and silverwood logs are wood materials. Greatwood is a dense hardwood that burns long; silverwood is a lighter, pale wood that burns shorter.
- Cinderpearl is a pyrophoric herb: it burns at a low temperature, short and hot, as fuel.
- Fabric is cloth, mirrored glass is mirrored glass and tallow is tallow. These three have no items of their own, and anything tagged tallow or animal fat from any mod counts as tallow.
- Flux Goo, Thaumaturge's fluid, is a corrosive acidic sludge with pH 3.

### Vanilla blocks Feedback replaces

Feedback's version of a [replaced vanilla block](#vanilla-blocks-in-other-mods-recipes) counts wherever Thaumaturge asks for the vanilla one.

- Research that asks for the item accepts Feedback's, and the research page shows it. The Vis Powered Generator's piston is one example.
- Golem components and filters accept it.
- The golem press multiblock takes Feedback's [Anvil](Anvil.md) and a Feedback [Piston](Piston.md) facing up.

### Wand caps and rods

Feedback adds wand parts made of its own materials. They work in Thaumaturge's wand assembly, which offers recipes that combine them with Thaumaturge's own caps and rods.

- **Wand caps**: Steel Wand Cap, Bronze Wand Cap, Tin Wand Cap, Aluminum Wand Cap, Titanium Wand Cap and Platinum Wand Cap.
- **Wand rods**: Bone Wand Rod, Bamboo Wand Rod, Amber Wand Rod and Quartz Glass Wand Rod.

A cap's vis cost is lower the better its metal conducts electricity, with silver as the reference. Its craft cost rises with how high the metal melts. A rod holds more vis the better its material insulates and the denser it is. See [Materials](Materials.md) for these properties.

### Essentia and thaumium

With Thaumaturge installed, each of its 37 aspects is a separate crystal material named for the aspect, such as **Ignis Essentia**, with its own figures. Thaumaturge's essentia crystal of that aspect is its item. A compound aspect's figures sit between those of the two aspects that make it, with a few traits of its own.

An essentia crystal melts in a [Crucible](Crucible.md) at its aspect's melting point, from 320 Tu (Gelum) to 1750 Tu (Terra), into one ingot's volume of molten essentia.

Every essentia crystal solders onto a [circuit board](Basic_Electronic_Circuit.md) as a switching part, and seats in a [Cat's Whisker Detector](Cats_Whisker_Detector.md) or an [Optical Terminal](Optical_Terminal.md). A higher rectifying figure makes a better crystal.

An essentia crystal is also **gem stock**. A [part slot](What_a_Part_Slot_Takes.md) that takes the gem shape takes it, if the essentia has the properties the slot asks for. Shape it at a part bench such as the [Stonecutter](Stonecutter.md).

One crystal makes a **tool tip**: shape it into a Tip, then put it on the tool on the Engineering Table's [Fit tab](Engineering_Table.md#fit-tab). The tool takes the tip's hardness, so a tip harder than the tool's body raises what it digs (see [Tips](Tools_and_Weapons.md#tips)). The hardest tips are **Instrumentum** (180), **Vitreus** (170), **Aversio** (160) and **Terra** (150). Vitreus and Aversio are very brittle.

Each essentia crystal also grinds on a [Grinding Wheel](Grinding_Wheel.md) into that essentia's **crystal plate**, as a gem does. The plate:

- makes a [Resonator](Resonator.md) crystal;
- mounts on a circuit board as a clock for Aer, Alienis, Alkimia, Aqua, Auram, Bestia, Exanimis, Fabrico, Instrumentum, Machina, Metallum, Motus, Ordo, Permutatio, Potentia, Praecantatio, Sensus, Vinculum and Volatus, and as a switching part for the other essentias;
- makes a [Mass Probe](Instruments.md#mass-probe) from any of the essentias that mount as a clock.

The six primal essentias:

| Essentia | Melts at (Tu) | Density | Hardness | Toughness | Enchantability | Rectifying | Stands out for |
|---|---|---|---|---|---|---|---|
| Aer | 900 | 0.9 | 50 | 0.6 | 24 | 0.1 | Very light, faint glow |
| Terra | 1750 | 5.2 | 150 | 4 | 14 | 0.2 | Dense, hard, very thick melt |
| Ignis | 650 | 2.4 | 80 | 0.8 | 20 | 0.3 | Glows brightly |
| Aqua | 550 | 1.4 | 40 | 1.2 | 18 | 0.1 | Takes much heat to warm and melt, very runny |
| Ordo | 1500 | 3.1 | 130 | 2 | 30 | 0.05 | Highest enchantability of the six |
| Perditio | 1100 | 2.0 | 35 | 0.4 | 8 | 0.95 | Brittle, toxic, strong rectifier |

#### Thaumium and aspect alloys

Molten essentia mixed into molten iron in a Crucible makes an iron alloy. The mix has to land inside the window, and JEI's *Mixing* card lists each one.

| Alloy | Iron | Essentia |
|---|---|---|
| **Thaumium** | 90 to 95% | Praecantatio and Terra, 2.5 to 5% each |
| Each [aspect alloy](#aspect-alloy-names) | 90 to 95% | its aspect, 5 to 10% |

Each alloy has its own figures. Thaumium uses Thaumaturge's own items; the aspect alloys make the usual tinted form items. All are magnetic except Xenium, Lysium, Allagium and Aesthium.

#### Thaumaturge's crucible

Each aspect alloy ingot can also be made in Thaumaturge's own crucible: drop an iron ingot into a crucible holding **10** of the alloy's aspect, and one ingot of the alloy comes out. It unlocks at the same Metallurgy research stage as thaumium and has no Thaumonomicon page of its own. A crucible holding enough for more than one recipe yields only one of them, so dissolve only the aspect you want. In that crucible, thaumium is made by Thaumaturge's own recipe.

#### Aspect alloy names

Each aspect has its own alloy. The six primals come first, then the compounds in alphabetical order:

| Aspect | Alloy |
|---|---|
| Aer | Aerialite |
| Terra | Chthonium |
| Ignis | Cindersteel |
| Aqua | Thalassium |
| Ordo | Eunomium |
| Perditio | Lysium |
| Alienis | Xenium |
| Alkimia | Azoth |
| Auram | Halcyon |
| Aversio | Fangsteel |
| Bestia | Ferinium |
| Cognitio | Noetium |
| Desiderium | Tantalus' Metal |
| Exanimis | Wiedergang |
| Fabrico | Daedalium |
| Gelum | Rimeiron |
| Herba | Briarsteel |
| Humanus | Anthropium |
| Instrumentum | Organon |
| Lux | Dawnsteel |
| Machina | Rotium |
| Metallum | Adamantium |
| Mortuus | Necrium |
| Motus | Kinesis |
| Permutatio | Allagium |
| Potentia | Stormsteel |
| Praecantatio | Goetium |
| Praemunio | Aegis |
| Sensus | Aesthium |
| Spiritus | Eidolon |
| Tenebrae | Skotium |
| Vacuos | Kenium |
| Victus | Quickblood |
| Vinculum | Desmos |
| Vitium | Miasma |
| Vitreus | Hyalium |
| Volatus | Pteron |

#### Alloy figures

Thaumium and some of the aspect alloys, the primals first:

| Alloy | Hardness | Toughness | Enchantability | Stands out for |
|---|---|---|---|---|
| Thaumium | 14 | 60 | 22 | The alloy of Thaumaturge's own items |
| Aerialite | 7 | 22 | 24 | Lighter than most |
| Chthonium | 18 | 83 | 14 | Highest melting point and service temperature |
| Cindersteel | 10 | 27 | 20 | Glows |
| Thalassium | 5 | 36 | 18 | Soft, low friction |
| Eunomium | 16 | 51 | 30 | High enchantability |
| Lysium | 5 | 17 | 8 | Soft, brittle, corrodes easily |
| Organon | 21 | 93 | 24 | Hardest |
| Adamantium | 17 | 135 | 22 | Best electrical conductor, very tough |
| Ferinium | 12 | 140 | 26 | Joint toughest |
| Aegis | 14 | 140 | 14 | Joint toughest, high yield strength |
| Rotium | 16 | 68 | 25 | Highest yield strength, low friction |
| Goetium | 10 | 32 | 36 | Highest enchantability |
| Kinesis | 11 | 38 | 27 | Lowest friction |
| Desmos | 8 | 97 | 18 | Highest friction |
| Dawnsteel | 8 | 25 | 22 | Brightest glow |
| Rimeiron | 8 | 22 | 14 | Lowest melting point and service temperature |
| Necrium | 8 | 79 | 6 | Lowest enchantability |
| Miasma | 7 | 25 | 22 | Most toxic, corrodes fastest |

### Foods

Every [food](Food.md) carries Victus (life) and Herba (plant), plus an aspect for what it does to the eater:

| Effect on the eater | Aspect |
|---|---|
| Warming and hearth foods | Ignis (fire) |
| Cooling foods | Gelum (ice) |
| Night sight | Lux (light) and Sensus (senses) |
| Sobering | Cognitio (mind) |
| Quickening | Motus (motion) |
| Antitoxin | Alkimia (alchemy) |
| Mending | Victus (life) |

### Machines and hardware

Machines, devices and carried hardware carry aspects that match their function:

- Mechanisms, [belts](Belt_Modules.md), moving parts and [vehicle](Vehicles.md) hardware such as engines: Machina (mechanism) and Motus.
- Tools and workshop machines: Instrumentum (tool) and Fabrico (craft). Tool heads and hand tools carry the same pair.
- [Instruments](Instruments.md): Sensus (senses) and Instrumentum.
- [Guns](Guns.md): Aversio (conflict).
- [Armor](Armor.md) and [Combat Gear](Combat_Gear.md): Praemunio (ward).
- Heat machines: Ignis. Cold machines: Gelum (ice). See [Heat](Heat.md).
- Electrical parts: Potentia, and with electronics Cognitio (mind). See [Electricity](Electricity.md).
- Flight hardware: Volatus (flight). This covers [thrusters](Thruster.md) with their nozzle and the suit thruster module of the [Exosuit](Exosuit.md), [jets](Jet.md) with their nozzle and compressor, [propellers](Propeller.md), the Levitation Plate and the Levitation Sleeve (see [Levitation](Levitation.md)). Light things that lift, such as feather down and the vanilla Phantom Membrane, carry it too. The gas cells of an [Airship](Airship.md) carry Aer (air) and Volatus.
- Sensors: Sensus (senses).
- Controllers and computers: Cognitio (mind). See [Controller](Controller.md).
- Treatises and other books: Cognitio.
- Experience, ender and sculk gear: Praecantatio (magic). See [Experience](Experience.md) and [Sculk](Sculk.md).
- Reactor parts: their own aspects, set by what they do. See [Fission Reactor](Fission_Reactor.md).
- [Robot](Robots.md) parts (chassis, drive, head, loader, radio, sensor, tether): Machina, Cognitio and Motus.

Part items that carry their material on the stack have no aspects of their own, and Thaumaturge works theirs out from their recipes. So do engineered items that have no aspects set by hand: they take them from their recipe ingredients. Creative and debug items have none.

Scanning Feedback's entities with the Thaumometer also yields aspects: the robot, the steam cloud, the grenade, the thrown potion bottle, the grapple hook and the thrown weapon.

### Flux, scrubbing and rust

Thaumaturge tracks **flux** in each chunk. Five Feedback blocks and one item deal with it.

- The [Scrubbing Bed](Scrubbing_Bed.md) takes flux out of the chunk it stands in. It fills up, and a full bed leaks flux back.
- The [Aura Sensor](Aura_Sensor.md) reads the chunk's vis and flux for a [Controller](Controller.md).
- The [Essentia Vent](Essentia_Vent.md) bleeds essentia from a full essentia store into the chunk as flux.
- The [Essentia Adapter](Essentia_Adapter.md) turns essentia from a store into gas. A full tank spills the rest into the chunk as flux.
- The [Vis Tap](Vis_Tap.md) turns vis from a vis relay network into current. With no load, the vis it cannot hold backs up into the chunk as flux.
- **Flux rust.** In a chunk whose flux stands well above its base, bare metal machines slowly corrode: [shafts](Shaft.md), [pulleys](Pulleys.md), [dynamotors](Dynamotor.md), [converters](Converter.md), [coils](Heating_Coil.md), [relays](Relay.md), [electromagnets](Electromagnet.md) and [batteries](Battery.md) lose condition, faster the more flux there is. It is the place doing it, not the machine's use. It happens in chunks near a player. A machine whose block wears a [Taint Glaze](Taint_Glaze.md) does not corrode, and neither does anything once a scrubbing bed brings the flux back down.
- **Flux goo into brine.** 200 mB of Thaumaturge's Flux Goo and 100 mB of [lye](Chemicals.md#lye) in a [Reactor](Reactor.md) make 300 mB of [brine](Chemicals.md#brine). The Reactor wants it cool, 10 to 120 Tu, best near 40.

### Taint and portable holes

Feedback machines are immune to Thaumaturge's taint: it does not convert them. A portable hole does not take them either.

Core Slag, Crude Pools, Tar Seeps and Soul Seeps count as physical flux for Thaumaturge: they feed a chunk's flux floor and taint outbreaks, where Thaumaturge's options for that are on. Crude Pools, Tar Seeps and Soul Seeps can also be cleared by Thaumaturge's scrubbers. Without Thaumaturge, nothing changes.

### Spectrometer and aspects

With Thaumaturge installed, the [Spectrometer](Instruments.md#spectrometer) adds a line to an item's tooltip listing its aspects and amounts, but only the aspects you have already discovered. Undiscovered aspects stay hidden.

### Survey Meter and warp

With Thaumaturge installed, the [Survey Meter](Instruments.md#survey-meter) adds a line, **Field anomaly, no ore**, with a bearing towards the chunk within 16 blocks whose vis is the highest share of its base aura, from 1.2 times base up. A chunk near that mark shows only some of the time.

Crafting some Feedback items gives Thaumaturge warp, a few points each: the fuel rods ([Fuel Rod, Enriched Fuel Rod, Spent Fuel Rod, Thorium Fuel Rod](Nuclear_Fuel.md) and Breeder Rod), the [XP Container](Experience.md), [Culture Dish](Sculk.md), [Catalyst Tap](Sculk.md#catalyst-tap), [Sculk Relay](Sculk.md#sculk-relay) and [Chunk Anchor](Ender_Port.md#chunk-anchor). Spent Fuel Rod carries the most.

### The Outer Lands and arrival pads

Thaumaturge's Outer Lands dimension is cold and has the thin, oxygen-free air of the End, so [Heat](Heat.md) rules for cold and the sealed-suit rules of [Hazards](Hazards.md) apply there.

Hot surfaces, plants that hurt on contact, Core Slag, Crude Pools, Tar Seeps and Soul Seeps count as unsafe landings for Thaumaturge's arrival pads.

## Legendary Survival Overhaul

**Optional.** Legendary Survival Overhaul (LSO) gives the player a body temperature, thirst and a temperature icon on the HUD. With both mods installed, Feedback's world drives them.

### Body temperature

- **Rooms.** Feedback's [room temperature](Heat.md#room-temperature) for the dimension (with the season shift from [Serene Seasons](#serene-seasons)) replaces LSO's own dimension and season effects. LSO's biome, altitude, night, weather, wetness and sprint effects stay. The body scale is in Tu: **20 Tu** is normal body temperature and **40 Tu** is the Nether's heat.
- **Machines.** A hot or cold Feedback machine within **5 blocks** warms or chills you, less with distance. Fires count by their flame temperature, and vessels and other thermal bodies by their temperature. One machine adds a limited amount and all machines together a limited amount, so a crowded forge room is hot but bounded.
- **Armor.** Worn modular [armor](Armor.md) insulates by what its parts are made of: poor heat conductors (leather, cloth, wood) insulate and metal plate does not. The chest piece counts most, then legs, then head and feet. LSO treats the result as the armor's thermal resistance.

### Food and drink

Finishing a Feedback food or dish hydrates by its nutrition times its water share: a thin soup hydrates well and dry bread barely. A meal far from room temperature warms or chills you for a while, stronger and longer the further it is from room temperature. A wet meal counts as a drink for LSO's drink effects. See [Food](Food.md).

### Fluids

Drinking a placed Feedback fluid has a cost: **Brine** dehydrates and sickens, and **Lye**, **Hydrochloric Acid**, **Sulfuric Acid** and **Hypochlorite Bleach** poison.

### Reading the body

LSO's icon and its word states work as they do without Feedback. While you carry a Feedback temperature instrument (the [Thermometer](Instruments.md#thermometer)), a line **Body: N Tu** shows above the left end of the hotbar. It reads to a tenth of the instrument's step, which is **2.5 Tu** for the Thermometer, and never finer. It is hidden while LSO's own thermometer is worn, which shows its own number.

## EMI

**Optional.** EMI shows Feedback's JEI cards (see [JEI](#jei)) through EMI's own JEI bridge, so the recipe, process and info pages appear in EMI's browser. Feedback has no EMI plugin of its own, and none for REI or WTHIT; Jade is the block readout it supports.

## Iris

**Optional.** While Iris is running a shader pack, the [Fluid Caster](Fluid_Caster.md) draws the plain flame in place of its flickering shader flame.

## Serene Seasons

**Optional.** With Serene Seasons installed, the season shifts the Overworld's [room temperature](Heat.md#room-temperature) for every vessel and workpiece there: winter is **15 Tu** colder, autumn **4 Tu** colder, spring unchanged and summer **4 Tu** warmer. The room never goes above 60 Tu or below 0 Tu. A kiln or crucible loses heat faster in a winter room and a little slower in a summer one. The Nether and the other dimensions keep their own figures.

## Other mods' foods

Any food from another mod (an item that is edible and is not from Minecraft or Feedback) keeps, spoils and feeds through the [Food](Food.md) state model. Its keeping figures come from its nutrition: a heartier food is drier, and a sweeter food holds more fat. It has no compounds and no perks, and cooking it yields nothing. A material file for the item overrides these figures.

## Inventory Sorter

**Optional.** With Inventory Sorter installed, sorting and scroll-moving do nothing on screens whose slots are roles, where an input, a fuel and a result each sit in a place that matters: the [Engineering Table](Engineering_Table.md), Reloading Press, Blueprint Table, Research Table, Material Probe, Enchanting screen, Casting Basin, Mixer, Brewing Stand, Station, Programmer, Prep Table, Thermal Vessel, Arc Hearth and Crucible. Packs and depots stay sortable. Food that differs in cook or moisture state does not merge in a sort.

## Simple Voice Chat

**Optional.** With Simple Voice Chat installed on the server and for players, holding the talk key on a [Radio Set](Radio_Set.md) sends your voice to everyone holding a set on the same band, and replaces your proximity voice while you talk. Without it a set still tunes, and carries no voice.

## Xaero's Minimap

**Optional.** With Xaero's Minimap installed, every [Survey Meter](Instruments.md#survey-meter) reading pins to the map. The pins sit in a waypoint set named **Feedback survey**, one yellow waypoint for each line the meter read: each kind of ore, oil, and the Thaumaturge field anomaly. A pin marks where most of that kind lies, rounded to 8 blocks, not the exact block. The next reading clears the set and fills it again, and a reading that finds nothing leaves it empty.

## Applied Energistics 2

**Optional.** With Applied Energistics 2 installed, two materials exist, and AE2's crystals and dusts are those materials in every recipe and process that reads a material by its properties:

| Material | What it is | Insulation | Piezo coupling |
|---|---|---|---|
| **Certus Quartz** | A quartz-like piezo crystal | ehv | 1.0 |
| **Fluix** | A weaker mixed crystal | hv | 0.7 |

Certus Quartz and Fluix crystals and dusts count as their material. Their blocks, buds and ores do not. Using one opens the material's stats card in JEI, whose item box lists it. Without Applied Energistics 2, neither material exists.

AE2's own fluid cells already hold Feedback's fluids and gases. Feedback adds nothing for AE2 network storage: it is not part of Feedback's own logistics.

## Artifacts

**Optional.** With Artifacts installed, each of its 48 items (everything but the Mimic Spawn Egg) has an [Engineering Table](Engineering_Table.md#engineering-recipes) recipe. Crafting is a second route: the items also turn up as loot, as Artifacts places them. JEI shows each recipe with its hand tool, [fasteners](Fasteners.md) and treatise. A recipe that spends fasteners takes screws.

Every recipe needs one [treatise](Treatise.md) volume held:

| Treatise volume | Items |
|---|---|
| Mechanical I | Angler's Hat, Bunny Hoppers, Cowboy Hat, Feral Claws, Flippers, Kitty Slippers, Lucky Scarf, Novelty Drinking Hat, Plastic Drinking Hat, Pocket Piston, Rooted Boots, Running Shoes, Snorkel, Snowshoes, Steadfast Spikes, Superstitious Hat, Thorn Pendant, Umbrella, Villager Hat, Whoopee Cushion |
| Mechanical II | Aqua-Dashers, Digging Claws, Fire Gauntlet, Power Glove |
| Chemical I | Antidote Vessel, Cloud in a Bottle, Flame Pendant, Onion Ring, Panic Necklace |
| Materials I | Charm of Sinking, Cross Necklace, Obsidian Skull, Strider Shoes |
| Electrical II | Pickaxe Heater, Shock Pendant |
| Electrical III | Helium Flamingo, Night Vision Goggles, Universal Attractor |
| Experience I | Golden Hook |
| Experience II | Charm of Shrinking, Chorus Totem, Crystal Heart, Eternal Steak, Scarf of Invisibility, Vampiric Glove, Withered Bracelet |
| Experience III | Everlasting Beef, Warp Drive |

Night Vision Goggles, the Universal Attractor, the Helium Flamingo and the Warp Drive each also take a [circuit](Basic_Electronic_Circuit.md). The Eternal Steak is built from an Everlasting Beef.

## Materials from other mods

[Cataclysm](#cataclysm), [Born in Chaos](#born-in-chaos), [Thaumic Bases](#thaumic-bases) and [Deeper and Darker](#deeper-and-darker) each bring materials that exist only with that mod installed.

- The mod's own ingots, nuggets, blocks, plates, gems and dusts of a material, where it has them, count as that material in every process. Using one opens the material's stats card in JEI, whose item box lists it.
- Feedback adds other forms of the material. Each metal gets the [forms](Materials.md#forms) the mod lacks, such as wire, rods and machined parts.
- Black Steel, Thauminite and Resonarium gear from these mods (swords, pickaxes, axes, shovels, hoes, helmets, chestplates, leggings and boots) is converted on the spot into the modular item of that kind in that material, like vanilla tools (see [Old vanilla tools](Tools_and_Weapons.md#old-vanilla-tools)). Other mods' recipes that ask for one of these items accept the converted item.
- A [perk](Material_Perks.md) listed for a material comes with any weapon part made of it.
- **Quality** is the metal's [fastener quality](Fasteners.md#quality).

## Cataclysm

**Optional.** With Cataclysm installed, five metals exist:

| Metal | Hardness | Toughness | Yield strength | Density | Melts at (Tu) | Enchantability | Quality | Perk |
|---|---|---|---|---|---|---|---|---|
| **Black Steel** | 22 | 80 | 18 | 7.9 | 1450 | 10 | 3 | none |
| **Ancient Metal** | 18 | 90 | 12 | 8.6 | 1100 | 20 | 3 | none |
| **Cursium** | 70 | 30 | 35 | 3.0 | 2000 | 28 | 4 | Featherlight, Chill |
| **Witherite** | 90 | 40 | 40 | 8.0 | 2400 | 12 | 4 | Radiant |
| **Ignitium** | 110 | 35 | 60 | 6.5 | 3000 | 15 | 5 | Fire aspect |

- Black Steel is **magnetic**.
- Ancient Metal, Cursium and Ignitium resist corrosion completely.
- Witherite is the best electrical conductor of the five. Its Radiant perk comes without [radioactivity](Radiation.md).
- Ignitium's Fire aspect burns a bare hand holding an Ignitium part, with a flame of **1400 Tu** ([why](Material_Perks.md#why-fire-aspect-burns-you)).
- The Black Steel sword and tools convert to modular items. The Black Steel Targe counts as Black Steel and stays a targe.
- Cataclysm's Cursium and Ignitium gear, the Ignitium elytra chestplate among it, stays Cataclysm's own items with their own abilities.

## Born in Chaos

**Optional.** With Born in Chaos installed, one metal exists:

| Metal | Hardness | Toughness | Yield strength | Density | Melts at (Tu) | Enchantability | Quality | Perk |
|---|---|---|---|---|---|---|---|---|
| **Dark Metal** | 40 | 25 | 22 | 9.0 | 1800 | 14 | 3 | Smite |

The Dark Metal armor and the Sharpened Dark Metal Sword stay Born in Chaos's own items with their own abilities.

## Thaumic Bases

**Optional.** With Thaumic Bases installed, one metal exists:

| Metal | Hardness | Toughness | Yield strength | Density | Melts at (Tu) | Enchantability | Quality | Perk |
|---|---|---|---|---|---|---|---|---|
| **Thauminite** | 20 | 55 | 13 | 6.8 | 1550 | 26 | 3 | none |

Thauminite is **magnetic**. It is [thaumium](#thaumium-and-aspect-alloys) with a little Praecantatio and Terra essentia. Its tools and armor convert to modular items; its shears count as Thauminite and stay shears.

## Deeper and Darker

**Optional.** With Deeper and Darker installed, two materials exist:

| Material | What it is | Hardness | Toughness | Density | Melts at (Tu) | Enchantability | Perk |
|---|---|---|---|---|---|---|---|
| **Resonarium** | A piezo composite | 25 | 20 | 3.5 | 1300 | 16 | Ring |
| **Soul Crystal** | A brittle gem that holds experience | 70 | 1.5 | 2.8 | 1500 | 18 | none |

Both insulate to **HV**.

- The Resonarium Plate is Resonarium's plate, and Feedback adds its ingot, rod and block. It has a piezo coupling of **1.4**. Resonarium tools and armor convert to modular items.
- The Soul Crystal and Soul Dust items are Soul Crystal's gem and dust, and Feedback adds its crystal plate. It is an [Experience](Experience.md) material: an XP Container vial of it has a top level of **40**, higher than echo shard's, and holds more points. Three of either item in a row make three XP Wire, which moves experience faster than echo shard wire and leaks none.

## Energy Fitting

The [Energy Fitting](Energy_Fitting.md) is the one place Forge Energy (FE) leaves a Feedback electrical line. Placed against another mod's machine that takes FE, it hands that machine FE made from the line's current, at a loss. Nothing else in Feedback gives or takes FE.

## Other mods

Every Feedback fluid and gas is published under the common fluid tags, so other mods' pumps and tanks accept them. Gases lighter than air are tagged `c:gaseous`, and each fluid has a tag of its own named for it, such as `c:molten_copper`.

### Vanilla blocks in other mods' recipes

Other mods' recipes that ask for one of these vanilla blocks accept Feedback's block in its place, and JEI lists it among the ingredients.

| Vanilla block | Feedback block |
|---|---|
| Furnace, Smoker | Furnace, Smoker |
| Blast Furnace | Crude Blast Furnace |
| Campfire, Soul Campfire | Campfire, Soul Campfire |
| Anvil (any wear) | Anvil |
| Smithing Table, Stonecutter | Smithing Table, Stonecutter |
| Brewing Stand, Enchanting Table | Brewing Stand, Enchanting Table |
| Piston, Sticky Piston | Piston |
| Hopper, Dropper, Dispenser | Hopper, Dropper, Dispenser |
| Note Block | Note Block |
| Observer | Observer |
| Comparator | Fill Gauge |
| Redstone Lamp | Glowstone Lamp |

The biome lists for oil, evaporite and emerald deposits also accept other mods' biomes through the common biome tags (`c:is_desert`, `c:is_plains`, `c:is_mountain`). Ore that generates in Nether stone also generates in other mods' stone tagged `c:netherrack`.
