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
| **Suit Module** | a finished module built from casing, wiring and core | any piece | One upgrade for that piece; see [Upgrade modules](#upgrade-modules). |

A servo's drive is the conductor's current rating times the wire cells in the winding times 11 A. A thicker winding drives harder. The waste share is 12% at the least, plus what the conductor loses to its resistance.

## The battery

Open the **Suit diagnostic** screen (key `J`) and use **Fit battery** or **Take battery** to seat or remove a [Battery](Battery.md) from your inventory. The diagnostic shows each piece, the charge, the draw, the suit's mass, each servo's drive and loss, and the condition. Without a battery, or with it flat, the suit is dead weight and the plate is plain armor; it never locks you.

Charge the battery by taking the suit's battery out and seating it in a [Charger](Charger.md), or by standing in a [Charging Gantry](Charging_Gantry.md) with the suit on.

## Controls

- `V` (hold): the ability wheel, a ring of ten segments. Release over a segment to switch that ability on or off, or to fire it if it is a pulse.
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

A stronger servo gives a larger effect and draws harder for it.

A segment whose fitting is missing shows hatched and does nothing. The three pulses fire once when you release over them, then need cooling before they fire again.

### Thermal view

Floor items, hot blocks and machines show at their real [Tu](Units.md), mobs by a body-heat class (warm-blooded, cold-blooded and so on), and lava. Walls block it. The reticle names the temperature of what is nearest the crosshair: a word, or a figure if the suit has a gauge. More fibre in the sensor head reaches farther.

Mob body heat is a datapack folder; entity-type tags such as `feedback:warm_blooded` map to a temperature.

## Upgrade modules

Each of the four pieces takes one **Suit Module**, a finished item built from three parts, shaped and assembled at a bench like any other [tool](Tools_and_Weapons.md#putting-one-together):

- **Module Casing**: plate, 2 cells. Its material sets the module's weight.
- **Module Wiring**: wire, 1, 2 or 4 cells. The conductor sets how much of what the module draws is wasted as heat; heavier wiring wastes less.
- **Module Core**: an ingot, plate or rod, cut in a shape of 2 to 8 cells. The shape names the upgrade and the armor piece it is for; the material sets how strongly it works. Heat upgrades follow the material's thermal conductivity, electric ones its wire current rating, sturdy ones its hardness and strength, and optic ones its reflectivity.

To fit it, put the armor piece in the center of the bench and click the module you carry in the fan; it goes into the piece's upgrade place, one per piece. Taking the piece apart gives the module back whole. The Suit diagnostic screen (`J`) shows the upgrade each piece carries.

Upgrades work whenever the suit is powered. The Heat Sink and Coolant Loop always work. Some upgrades draw charge as they act, as noted. The Cloak Field, Jet Pack, Shock Ram, Scan Ping and EMP Coil give the wheel a segment of their own (see [Abilities](#abilities)).

| Piece | Upgrade | What it does |
| --- | --- | --- |
| Helmet | Rebreather | Breath lasts longer underwater. |
| Helmet | Rangefinder | Longer reach for blocks and attacks. |
| Helmet | Respirator | Clears poison, nausea and hunger. |
| Helmet | Glare Filter | Clears blindness and darkness. |
| Helmet | Long Lens | Thermal view reaches farther. |
| Helmet | Crash Liner | Less damage from projectiles. |
| Helmet | Reflex Link | Faster attack speed. |
| Helmet | Scan Ping | Pulse that makes nearby living things glow. |
| Chestplate | Heat Sink | The suit sheds heat faster. |
| Chestplate | Coolant Loop | The servos cut out at a higher temperature. |
| Chestplate | Efficiency Bus | Everything the suit does draws less charge. |
| Chestplate | Autodoc | Slowly heals you; heavy draw. |
| Chestplate | Reactive Plating | Less knockback. |
| Chestplate | Blast Shell | Less explosion damage. |
| Chestplate | Thorn Array | Shocks melee attackers; draws charge per hit. |
| Chestplate | Thermal Lining | Immune to freezing. |
| Chestplate | Magnet Pickup | Draws loose items to you. |
| Chestplate | Fire Curtain | Less fire damage. |
| Chestplate | Cloak Field | Hides the wearer while switched on; draws constantly. |
| Chestplate | Jet Pack | Lifts you while `G` is held; burns charge fast. |
| Leggings | Strider Pistons | Faster walking; draws charge while you move. |
| Leggings | Step Assist | Steps up blocks without jumping. |
| Leggings | Swim Fins | Better movement in water. |
| Leggings | Shock Absorbers | Less fall damage. |
| Leggings | Grip Pads | Soft ground, webs and water slow you less. |
| Leggings | Load Frame | A [Backpack](Backpack.md) carries more before it slows you. |
| Leggings | Stabiliser | Faster sneaking and a little less knockback. |
| Leggings | EMP Coil | Pulse that slows, weakens and blinds nearby creatures. |
| Boots | Magnetic Anchor | Far less knockback. |
| Boots | Heat Shield | Fire and lava do not burn you while the battery lasts; heavy draw. |
| Boots | Jump Coil | A higher jump. |
| Boots | Landing Gear | You fall farther without damage. |
| Boots | Rocket Spur | A harder, faster dash. |
| Boots | Stomp Actuator | Landing from 3 or more blocks damages everything around you; draws charge per landing. |
| Boots | Shock Ram | Pulse: a shockwave that knocks back and hurts everything near. |

## Weight and heat

The suit's mass slows you like heavy armor; strength cancels the slowdown.

The servos waste a share of what they draw as heat, and the suit warms. It sheds heat to the air, faster in water, rain and cold biomes, slower in the Nether. Above **110 Tu** the servos cut out until the suit falls under **80 Tu**; see [Heat](Heat.md).

## HUD

A card at the bottom left shows five lamps (ST, JP, PL, NV, TH) for the abilities, one more lamp (CL, JT, SK, SC, EM) for each fitted module ability, a charge bar and a heat bar. **PACK OVERLOAD** shows when the [Backpack](Backpack.md) is heavier than you can carry.

## Misuse and hazards

The suit wears only by misuse. See [Hazards](Hazards.md).

- Holding the dash while the servos are cut out from heat wears the chestplate.
- Dashing into a wall wears the leggings.
- Charging from a line over the battery's case band wears the battery.
