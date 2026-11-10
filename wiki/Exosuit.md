# Exosuit

A powered suit built from ordinary modular [armor](Armor.md) with fittings added at the bench. A [Battery](Battery.md) in the chestplate runs five abilities: strength, jump and dash, plating, night sight and thermal view.

## Building the suit

Make the pieces as described on [Armor](Armor.md), then add parts the usual way: put the piece in the center of the [Smithing Table](Smithing_Table.md), the [Woodworking Table](Woodworking_Table.md) or the [Stonecutter](Stonecutter.md) and add the part for the stock's bench. The fittings:

| Fitting | Made from | Goes on | What it does |
| --- | --- | --- | --- |
| **Servo** | a winding shaped from wire | any piece | Drives the abilities. The wire sets how hard it pulls and how much of the power it wastes as heat. |
| **Gauge** | a lens | any piece | Without any gauge the suit reports in words; with one it reports in figures, finer with clearer glass. |
| **Sensor head** | fibre, 2 cells | helmet | Night sight and thermal view. |
| **Battery bay** | plates, 2 cells | chestplate | Holds the battery the whole suit runs on. |

A servo's drive is the conductor's current rating times the wire cells in the winding times 11 A. A thicker winding drives harder. The waste share is 12% at the least, plus what the conductor loses to its resistance.

## The battery

Open the **Suit diagnostic** screen (key `J`) and use **Fit battery** or **Take battery** to seat or remove a [Battery](Battery.md) from your inventory. The diagnostic shows each piece, the charge, the draw, the suit's mass, each servo's drive and loss, and the condition. Without a battery, or with it flat, the suit is dead weight and the plate is plain armor; it never locks you.

Charge the battery by taking the suit's battery out and seating it in a [Charger](Charger.md), or by standing in a [Charging Gantry](Charging_Gantry.md) with the suit on.

## Controls

- `V` (hold): the ability wheel, a hexagon. Release over a segment to switch that ability on or off.
- `B` (hold): dash thrust.
- `J`: the Suit diagnostic screen.

## Abilities

| Ability | Needs | What it does |
| --- | --- | --- |
| **Strength** | servo in the chestplate | Cancels the suit's weight slowdown, raises what a [Backpack](Backpack.md) can carry before it slows you, adds attack damage and block-breaking speed. |
| **Jump / dash** | servo in the leggings or boots | A higher jump and softer landings. Holding dash thrusts you along, mostly sideways, with a short hop off the ground. Thin air (the End) gives less thrust. Draws heavy charge. |
| **Plating** | servo in the chestplate | Raises armor and toughness while on. Each hit it absorbs costs charge. |
| **Night sight** | sensor head | Night vision with a phosphor look. |
| **Thermal** | sensor head | A false-color view of true temperatures. |

A stronger servo gives a larger effect and draws harder for it.

### Thermal view

Floor items, hot blocks and machines show at their real [Tu](Units.md), mobs by a body-heat class (warm-blooded, cold-blooded and so on), and lava. Walls block it. The reticle names the temperature of what is nearest the crosshair: a word, or a figure if the suit has a gauge. More fibre in the sensor head reaches farther.

Mob body heat is a datapack folder; entity-type tags such as `feedback:warm_blooded` map to a temperature.

## Weight and heat

The suit's mass slows you like heavy armor; strength cancels the slowdown.

The servos waste a share of what they draw as heat, and the suit warms. It sheds heat to the air, faster in water, rain and cold biomes, slower in the Nether. Above **110 Tu** the servos cut out until the suit falls under **80 Tu**; see [Heat](Heat.md).

## HUD

A card at the bottom left shows five lamps (ST, JP, PL, NV, TH) for the abilities, a charge bar and a heat bar. **PACK OVERLOAD** shows when the [Backpack](Backpack.md) is heavier than you can carry.

## Misuse and hazards

The suit wears only by misuse. See [Hazards](Hazards.md).

- Holding the dash while the servos are cut out from heat wears the chestplate.
- Dashing into a wall wears the leggings.
- Charging from a line over the battery's case band wears the battery.
