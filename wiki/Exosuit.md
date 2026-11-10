# Exosuit

A powered suit built from ordinary modular [armor](Armor.md) with fittings added at the bench. A [Battery](Battery.md) in the chestplate runs five abilities (strength, jump and dash, plating, night sight and thermal view), and upgrade modules fitted to the pieces add more.

## Building the suit

Make the pieces as described on [Armor](Armor.md), then add parts the usual way: put the piece in the center of the [Smithing Table](Smithing_Table.md), the [Woodworking Table](Woodworking_Table.md) or the [Stonecutter](Stonecutter.md) and add the part for the stock's bench. The fittings:

| Fitting | Made from | Goes on | What it does |
| --- | --- | --- | --- |
| **Servo** | a winding shaped from wire | any piece | Drives the abilities. The wire sets how hard it pulls and how much of the power it wastes as heat. |
| **Gauge** | a lens | any piece | Without any gauge the suit reports in words; with one it reports in figures, finer with clearer glass. |
| **Sensor head** | fibre, 2 cells | helmet | Night sight and thermal view. |
| **Battery bay** | plates, 2 cells | chestplate | Holds the battery the whole suit runs on. |
| **Suit Module** | a finished module built from casing, wiring and attachments | any piece | One upgrade for that piece; see [Upgrade modules](#upgrade-modules). |

A servo's drive is the conductor's current rating times the wire cells in the winding times 11 A. A thicker winding drives harder. The waste share is 12% at the least, plus what the conductor loses to its resistance.

## The battery

Open the **Suit diagnostic** screen (key `J`) and use **Fit battery** or **Take battery** to seat or remove a [Battery](Battery.md) from your inventory. The diagnostic shows each piece, the charge, the draw, the suit's mass, each servo's drive and loss, and the condition. Without a battery, or with it flat, the suit is dead weight and the plate is plain armor; it never locks you.

Charge the battery by taking the suit's battery out and seating it in a [Charger](Charger.md), or by standing in a [Charging Gantry](Charging_Gantry.md) with the suit on.

## Controls

- `V` (hold): the ability wheel, a ring of the five built-in segments plus one for each ability a fitted module gives. Release over a segment to switch that ability on or off, or to fire it if it is a pulse.
- `B` (hold): dash thrust.
- `G` (hold): jet thrust, once the Jet Pack is switched on.
- `J`: the Suit diagnostic screen.

## Abilities

| Ability | Needs | What it does |
| --- | --- | --- |
| **Strength** | servo in the chestplate | Cancels the suit's weight slowdown, raises what a [Backpack](Backpack.md) can carry before it slows you, adds attack damage and block-breaking speed. |
| **Jump / dash** | servo in the leggings or boots | A higher jump and softer landings. Holding dash thrusts you along, mostly sideways, with a short hop off the ground. Thin air (the End) gives less thrust. Draws heavy charge. |
| **Plating** | servo in the chestplate | Raises armor and toughness while on. Each hit it absorbs costs charge. |
| **Night sight** | sensor head | Night vision with a phosphor look. |
| **Thermal** | sensor head | A false-color view of true temperatures. |
| **Cloak** | Cloak Field module | A switch you leave on: hides the wearer, and draws charge all the while. |
| **Jet** | Jet Pack module | Switch it on, then hold `G` to lift. Thin air (the End) gives less lift. Burns charge fast. |
| **Shock** | Shock Ram module | A pulse: a shockwave that knocks back and hurts everything near. |
| **Scan** | Scan Ping module | A pulse: every living thing in a wide ring glows for a while. |
| **EMP** | EMP Coil module | A pulse: nearby creatures are slowed, weakened and blinded. |
| **Magnet** | Magnet Pickup module | A switch: draws loose items to you while on. |
| **Stride** | Strider Pistons module | A switch: faster walking while on. |
| **Step** | Step Assist module | A switch: steps up a full block while on. |
| **Range** | Rangefinder module | A switch: longer reach for blocks and attacks while on. |
| **Lens** | Long Lens module | A switch: thermal view reaches farther while on. |
| **Heal** | Autodoc module | A pulse: heals you at once. Long cooldown. |
| **Vent** | Coolant Loop module | A pulse: vents most of the suit's heat at once, which can end a heat cut-out. |
| **Burst** | Rocket Spur module | A pulse: a single burst of thrust along the direction you look. |
| **Thorns** | Thorn Array module | A pulse: hurts everything within arm's reach. |

A stronger servo gives a larger effect and draws harder for it.

A segment whose fitting is missing shows hatched and does nothing. A pulse fires once when you release over it, then needs cooling before it fires again. A switch module works only while its segment is on.

### Thermal view

Floor items, hot blocks and machines show at their real [Tu](Units.md), mobs by a body-heat class (warm-blooded, cold-blooded and so on), and lava. Walls block it. The reticle names the temperature of what is nearest the crosshair: a word, or a figure if the suit has a gauge. More fibre in the sensor head reaches farther.

Mob body heat is a datapack folder; entity-type tags such as `feedback:warm_blooded` map to a temperature.

## Upgrade modules

Each of the four pieces takes one **Suit Module**, a finished item built from a casing, wiring, attachments and support parts, shaped and assembled at a bench like any other [tool](Tools_and_Weapons.md#putting-one-together). A module has at most six parts at the bench: the casing, the wiring and four others. Modules must be learned first; see [Learning a module](#learning-a-module).

- **Module Casing**: plate, 2 cells. Its material sets the module's weight.
- **Module Wiring**: wire, 1, 2 or 4 cells. The conductor sets how much of what the module draws is wasted as heat, and how many attachments it can feed. Heavier wiring feeds more attachments, up to four, and wastes less.
- **Attachments**: one to four, each is a part of one of seven kinds (Module Optic, Module Filter, Module Thermal, Module Plating, Module Actuator, Module Thruster, Module Coil), cut from an ingot, plate or rod. The shape it is cut in is the upgrade it gives, so two attachments of one kind do very different things. The material sets how strongly the upgrade works. Heat upgrades follow the material's thermal conductivity, electric ones its wire current rating, sturdy ones its hardness and strength, and optic ones its reflectivity.
- **Support parts**: some upgrades need a **controller** (a circuit), a **power cell** (a [Battery](Battery.md)) or a **motor** (a [Dynamotor](Dynamotor.md)) set beside the attachments, as the table below lists. A better circuit, a higher-capacity battery or a motor rated for more current makes the upgrade stronger. An upgrade whose support part is missing does not work.

A module fits any armor piece. To fit it, put the armor piece in the center of the bench and click the module you carry in the fan; it goes into the piece's upgrade place, one per piece. Taking the piece apart gives the module back whole. The Suit diagnostic screen (`J`) shows the upgrade each piece carries.

An Actuator or Thruster attachment has a **Bore** (in mm, up to 40) that you set on the drawing and cut with a drill. Each upgrade wants a bore of about its own size:

| Upgrade | Bore (mm) |
| --- | --- |
| Rocket Spur | 8 |
| Grip Pads, Swim Fins | 10 |
| Jump Coil | 12 |
| Strider Pistons | 14 |
| Jet Pack, Shock Absorbers | 16 |
| Step Assist | 18 |
| Landing Gear | 20 |
| Shock Ram | 22 |
| Stomp Actuator | 24 |

The closer the cut is to that size, the stronger the upgrade; far off, it works at about half strength. A part left uncut still works.

Upgrades work whenever the suit is powered. The Heat Sink always works. Some upgrades draw charge as they act, as noted. Fourteen upgrades give the wheel a segment of their own: Cloak Field, Jet Pack, Shock Ram, Scan Ping, EMP Coil, Magnet Pickup, Strider Pistons, Step Assist, Rangefinder, Long Lens, Autodoc, Coolant Loop, Rocket Spur and Thorn Array (see [Abilities](#abilities)). Their effects work only through the wheel, except the Coolant Loop and Rocket Spur, which also work without it.

| Attachment | Upgrade | Needs | What it does |
| --- | --- | --- | --- |
| Optic | Rangefinder | none | Longer reach for blocks and attacks while switched on. |
| Optic | Long Lens | none | Thermal view reaches farther while switched on. |
| Optic | Glare Filter | none | Clears blindness and darkness. |
| Optic | Scan Ping | controller | Pulse that makes nearby living things glow. |
| Filter | Rebreather | none | Breath lasts longer underwater. |
| Filter | Respirator | none | Clears poison, nausea and hunger. |
| Thermal | Heat Sink | none | The suit sheds heat faster. |
| Thermal | Coolant Loop | motor | The servos cut out at a higher temperature. Pulse: vents most of the suit's heat. |
| Thermal | Thermal Lining | none | Immune to freezing. |
| Thermal | Heat Shield | none | Fire and lava do not burn you while the battery lasts; heavy draw. |
| Thermal | Fire Curtain | none | Less fire damage. |
| Thermal | Cloak Field | controller | Hides the wearer while switched on; draws constantly. |
| Plating | Reactive Plating | none | Less knockback. |
| Plating | Blast Shell | none | Less explosion damage. |
| Plating | Crash Liner | none | Less damage from projectiles. |
| Plating | Thorn Array | none | Pulse: hurts everything in arm's reach. |
| Plating | Magnetic Anchor | none | Far less knockback. |
| Plating | Stabiliser | none | Faster sneaking and a little less knockback. |
| Plating | Load Frame | none | A [Backpack](Backpack.md) carries more before it slows you. |
| Actuator | Strider Pistons | motor | Faster walking while switched on; draws charge while you move. |
| Actuator | Step Assist | motor | Steps up a full block without jumping while switched on. |
| Actuator | Jump Coil | cell | A higher jump. |
| Actuator | Landing Gear | none | You fall farther without damage. |
| Actuator | Shock Absorbers | none | Less fall damage. |
| Actuator | Stomp Actuator | motor | Landing from 3 or more blocks damages everything around you; draws charge per landing. |
| Actuator | Grip Pads | none | Soft ground, webs and water slow you less. |
| Actuator | Swim Fins | none | Better movement in water. |
| Actuator | Shock Ram | motor | Pulse: a shockwave that knocks back and hurts everything near. |
| Thruster | Rocket Spur | none | A harder, faster dash. Pulse: a single burst of thrust along your look. |
| Thruster | Jet Pack | cell and motor | Lifts you while `G` is held; burns charge fast. |
| Coil | Efficiency Bus | controller | Everything the suit does draws less charge. |
| Coil | Autodoc | controller and cell | Pulse: heals you at once; heavy draw. |
| Coil | Reflex Link | controller | Faster attack speed. |
| Coil | Magnet Pickup | motor | Draws loose items to you while switched on. |
| Coil | EMP Coil | cell and controller | Pulse that slows, weakens and blinds nearby creatures. |

### Learning a module

A module is drawn at the [Blueprint Table](Blueprint_Table.md), and only while you hold a Robotics [Treatise](Treatise.md); the bench builds one only with its blueprint in the bench's blueprint slot. A [Blueprint Book](Blueprint_Book.md) in the blueprint slot offers every sheet inside it.

Blueprints come from:

- Rare finds in chests.
- Drawing it yourself at the Blueprint Table and printing it.
- Measuring: take a module apart at the bench with blank paper in the blueprint slot, and the paper becomes a blueprint of that design.

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
