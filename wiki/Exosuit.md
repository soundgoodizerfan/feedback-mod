# Exosuit

A powered suit built from ordinary modular [armor](Armor.md) with fittings added at the [Engineering Table](Engineering_Table.md). A [Battery](Battery.md) in the chestplate runs five abilities (strength, jump and dash, plating, night sight and thermal view), and upgrade modules fitted to the pieces add more.

## Building the suit

Make the pieces as described on [Armor](Armor.md), then add parts on the [Fit tab](Engineering_Table.md#fit-tab) of the Engineering Table: the piece in the item slot, the part in an attachment slot (shape each at the bench for its stock first). The fittings:

| Fitting | Made from | Goes on | What it does |
| --- | --- | --- | --- |
| **Servo** | a finished motor ([Dynamotor](Dynamotor.md)) held whole | any piece | Drives the abilities. The motor's rated amps set how hard it pulls and how much of the power it wastes as heat. |
| **Gauge** | a lens | any piece | Without any gauge the suit reports in words; with one it reports in figures, finer with clearer glass. |
| **Sensor head** | fibre, 2 cells | helmet | Night sight and thermal view. |
| **Battery bay** | plates, 2 cells | chestplate | Holds the battery the whole suit runs on. |
| **Suit Module** | a finished module built from casing, wiring and attachments | any piece | One upgrade for that piece; see [Upgrade modules](#upgrade-modules). |

A servo's drive and waste come from its motor's rated amps: a motor rated higher drives harder, and a motor short of its full rating wastes more of the power as heat.

## The battery

Open the **Suit diagnostic** screen (key `J`) and use **Fit battery** or **Take battery** to seat or remove a [Battery](Battery.md) from your inventory. The diagnostic shows each piece, the charge, the draw, the suit's mass, each servo's drive and loss, and the condition. Without a battery, or with it flat, the suit is dead weight and the plate is plain armor; it never locks you.

Charge the battery by taking the suit's battery out and seating it in a [Charger](Charger.md), or by standing in a [Charging Gantry](Charging_Gantry.md) with the suit on.

## Controls

- `V` (hold): the ability wheel, a ring of the five built-in segments plus one for each ability a fitted module gives. Release over a segment to switch that ability on or off, or to fire it if it is a pulse.
- `B` (hold): dash thrust.
- `G` (hold): jet thrust, once Thrust is switched on.
- `J`: the Suit diagnostic screen.

## Abilities

| Ability | Needs | What it does |
| --- | --- | --- |
| **Strength** | servo in the chestplate | Cancels the suit's weight slowdown, raises what a [Backpack](Backpack.md) can carry before it slows you, adds attack damage and block-breaking speed. |
| **Jump / dash** | servo in the leggings or boots | A higher jump and softer landings. Holding dash thrusts you along, mostly sideways, with a short hop off the ground. Thin air (the End) gives less thrust. Draws heavy charge. |
| **Plating** | servo in the chestplate | Raises armor and toughness while on. Each hit it absorbs costs charge. |
| **Night sight** | sensor head | Night vision with a phosphor look. |
| **Thermal** | sensor head | A false-color view of true temperatures. |
| **Thermal mask** | Module Thermal with a controller | A switch you leave on: other wearers' thermal view does not show you. Draws charge all the while. |
| **Thrust** | Module Thruster | Switch it on, then hold `G` to lift on burning fuel from the pack. Thin air (the End) gives less lift. |
| **Burst** | Module Thruster | A pulse: a single burst of thrust along the direction you look. |
| **Ram** | Module Actuator | A pulse: a shockwave that knocks back and hurts everything near. |
| **Scan** | Module Optic with a controller | A pulse: every living thing in a wide ring glows for a while. |
| **EMP** | Module Coil with a cell | A pulse: nearby creatures are slowed, weakened and blinded. |
| **Electromagnet** | Module Coil | A switch: pulls loose magnetic items to you while on. |
| **Stride** | Module Actuator | A switch: a short stroke walks faster, a long stroke steps up higher, while on. |
| **Optic** | Module Optic | A switch: a short focal length gives longer reach for blocks and attacks, a long one extends the thermal view, while on. |
| **Dose** | Module Dispenser | A pulse: drinks the first potion bottle in your pack for you. |
| **Vent** | Module Thermal with a motor | A pulse: vents most of the suit's heat at once, which can end a heat cut-out. |

A stronger servo gives a larger effect and draws harder for it. A Module Optic's lens is a stock slot of its own that you choose ([Composed Parts](Composed_Parts.md)).

A segment whose fitting is missing shows hatched and does nothing. A pulse fires once when you release over it, then needs cooling before it fires again. A switch module works only while its segment is on.

### Thermal view

Floor items, hot blocks and machines show at their real [Tu](Units.md), mobs by a body-heat class (warm-blooded, cold-blooded and so on), and lava. Walls block it. The reticle names the temperature of what is nearest the crosshair: a word, or a figure if the suit has a gauge. More fibre in the sensor head reaches farther.

Mob body heat is a datapack folder; entity-type tags such as `feedback:warm_blooded` map to a temperature.

## Upgrade modules

Each of the four pieces takes one **Suit Module**, a finished item built from a casing, wiring, attachments and support parts, shaped at a bench and assembled at the [Engineering Table](Engineering_Table.md) like any other complex item. A module has at most six parts: the casing, the wiring and four others. Modules need a treatise; see [Learning a module](#learning-a-module).

- **Module Casing**: plate, 2 cells. Its material sets the module's weight.
- **Module Wiring**: wire, 1, 2 or 4 cells. The conductor sets how much of what the module draws is wasted as heat, and how many attachments it can feed. Heavier wiring feeds more attachments, up to four, and wastes less.
- **Attachments**: one to four, each is a part of one of ten kinds (Module Optic, Module Filter, Module Thermal, Module Plating, Module Actuator, Module Spring, Module Tread, Module Thruster, Module Coil, Module Dispenser), cut from an ingot, plate or rod and tuned by one setting (focal length, bed depth, thickness, stroke, span, bore, core length or dose) rather than cut in a shape. The material sets how strongly the upgrade works. Heat upgrades follow the material's thermal conductivity, sturdy ones its hardness and strength, optic ones its reflectivity, thruster ones its melting point, coils whether it is magnetic and dispensers its corrosion resistance.
- **Support parts**: some upgrades need a **controller** (a circuit), a **power cell** (a [Battery](Battery.md)) or a **motor** (a [Dynamotor](Dynamotor.md)) set beside the attachments, as the table below lists. A better circuit, a higher-capacity battery or a motor rated for more current makes the upgrade stronger. An upgrade whose support part is missing does not work.

A module fits any armor piece. To fit it, put the armor piece in the item slot of the Engineering Table's Fit tab and the module in an attachment slot; it goes into the piece's upgrade place, one per piece. Taking the piece apart on the Repair tab gives the module back whole. The Suit diagnostic screen (`J`) shows the upgrade each piece carries.

A Module Actuator has a **Bore** (in mm, up to 40) that you set on the drawing and cut with a drill. It wants a bore of about 16 mm: the closer the cut is to that size, the stronger the upgrade; far off, it works at about half strength. A part left uncut still works.

Upgrades work whenever the suit is powered. Some upgrades draw charge as they act, as noted. Wheel segments appear only when a fitted part can act: a Module Optic (Optic, and Scan with a controller), Module Actuator (Stride and Ram), Module Thermal (Vent with a motor, Thermal mask with a controller), Module Thruster (Thrust and Burst), Module Coil (Electromagnet, and EMP with a cell) and Module Dispenser (Dose) each give theirs (see [Abilities](#abilities)). Their effects work only through the wheel. Springs, treads, filter breath, heat plates and armored inserts work without charge.

| Attachment | Needs | What it does |
| --- | --- | --- |
| Module Optic | controller for the Scan pulse | One tuned part: a focal length of 20 cm or less gives longer reach for blocks and attacks, a longer one extends the thermal view, while switched on. Glass clarity sets the power; reflective glass clears blindness and darkness. With a live controller held, Scan is a pulse that makes nearby living things glow. |
| Module Filter | none | A cartridge with a porous media slot (charcoal or brick) and a bed depth. A thin bed (3 cm or less) lengthens breath; a deep bed clears poison, nausea and hunger, only while the suit is powered. |
| Module Thermal | none; motor for Vent; controller for Thermal mask | A heat exchanger plate, tuned by thickness. A thin plate (3 mm or less) sheds the suit's heat faster; a thick one cuts fire damage and, while the suit is powered, keeps frost off. The better the metal conducts heat, the harder it works. Held motor: Vent is a pulse that vents most of the suit's heat at once. Held controller: Thermal mask is a switch that hides the wearer from other wearers' thermal view, at a steady draw. |
| Module Plating | none | An armored insert, tuned by thickness. A thin plate (3 mm or less) cuts projectile damage; a thick one cuts blast damage, resists knockback and lets a [Backpack](Backpack.md) carry more before it slows you. A harder, tougher metal works better. |
| Module Actuator | motor | Stroke and bore are tuned. A short stroke (8 cm or less) speeds walking while switched on and draws charge while you move; a long one lifts you over steps. Ram is a pulse: a shockwave that knocks back and hurts everything near. |
| Module Spring | none | Stroke is tuned; the stock must have yield strength. A short stroke kicks the jump higher; a long one adds safe fall and a landing rebound that hurts nearby mobs after a fall of 3 or more blocks. |
| Module Tread | none | Span is tuned. Narrow grips soft ground and webs; wide swims better. |
| Module Thruster | none | A nozzle tuned by bore (8 to 40 mm). It burns fuel drawn from the first fluid container in your pack whose fluid burns, such as a bucket of fuel; the hotter the flame, the harder it pushes. A wide bore pushes harder and burns faster; a nozzle metal with a higher melting point works better. It needs air or a fuel that carries its own oxygen, and with no fuel in the pack nothing fires. Thrust lifts you while `G` is held; Burst is a pulse: a single lump of fuel fired along your look. |
| Module Coil | cell for EMP | An electromagnet tuned by core length. Electromagnet is a switch that pulls only magnetic loose items (iron, steel and the like) to you; an iron-like core carries the field best. A long core (over 8 cm) reaches farther, a short one pulls harder. With a power cell held, EMP is a pulse that slows, weakens and blinds nearby creatures. |
| Module Dispenser | none | A dosing pump tuned by dose (50 to 500 mB). Dose is a pulse that drinks the first potion bottle in your pack for you and returns the empty bottle; a larger dose lasts longer. A metal that resists corrosion works better. It draws a little charge. |

### Learning a module

A module is drawn at the [Blueprint Table](Blueprint_Table.md), and assembled at the Engineering Table, only while you hold Robotics Treatise I: Suit Modules ([Treatise](Treatise.md)). A blueprint is not required to assemble one; it judges the fit against the drawing, and a [Blueprint Book](Blueprint_Book.md) in the blueprint slot offers every sheet inside it.

Blueprints come from:

- Rare finds in chests.
- Drawing it yourself at the Blueprint Table and printing it.
- Measuring: take a module apart on the Treatise tab of the Engineering Table with blank paper in the blank slot, and the paper becomes a blueprint of that design.

## Weight and heat

The suit's mass slows you like heavy armor; strength cancels the slowdown.

The servos waste a share of what they draw as heat, and the suit warms. It sheds heat to the air, faster in water, rain and cold biomes, slower in the Nether. Above **110 Tu** the servos cut out until the suit falls under **80 Tu**; see [Heat](Heat.md).

## HUD

A card at the bottom left shows five lamps (ST, JP, PL, NV, TH) for the abilities, one more lamp for each fitted module ability, a charge bar and a heat bar. **PACK OVERLOAD** shows when the [Backpack](Backpack.md) is heavier than you can carry.

## Misuse and hazards

The suit wears only by misuse. See [Hazards](Hazards.md).

- Holding the dash while the servos are cut out from heat wears the chestplate.
- Dashing into a wall wears the leggings.
- Charging from a line over the battery's case band wears the battery.
