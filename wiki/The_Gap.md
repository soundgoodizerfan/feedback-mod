# The Gap

The Gap is the dimension above the Overworld's sky: a void with no gravity, no air, great cold and the sky's radiation. A craft reaches it by climbing out of the air.

## Getting there and back

Air in the Overworld thins with height and is gone by y 312 (see [air density](Vehicles.md#air-density)). A [vehicle](Vehicles.md) that stays in air of zero density for about a second leaves for the Gap, and the screen clears from black. Below the Gap's floor the vehicle returns to the Overworld, and the screen clears from white-orange. Below the floor, a player standing on no vehicle falls out into the Overworld too: same x and z, at y 300 in thin air, with the same speed and fall distance, and on fire for 10 s. A player on a vehicle goes with it. Wings, [propellers](Propeller.md) and air-breathing [thrusters](Thruster.md) fade as the air thins and give nothing at the top.

## Size

A craft that enters the Gap shrinks to a hundredth of its size, with everything alive aboard: your size, reach, step, jump, walking speed and fall distance shrink with it. The craft and everyone aboard grow back on the return. Speeds and distances shrink too, so a long trip is short in blocks and takes the same time.

## The End by rocket

The End is a point in the Gap along a fixed bearing, the gas giant in the sky (see below), about 120 blocks from the Gap's origin point (x 0, z 0). A craft that comes within 15 blocks of it while heading toward it arrives in the End at full size, over the main island, coming in fast and descending. A craft that misses has no rim to stop it and coasts on. A [Bearing Sensor](Bearing_Sensor.md) reads the turn and the distance to the End.

The End's air is very thin and holds no oxygen, and is gone above y 320. A [thruster](Thruster.md) that carries its own oxidizer works there; an air-breathing one does not. A [Vacuum Dryer](Potion_Salts.md#vacuum-dryer) set in the End draws its chamber down with no shaft turning. Leaving upward out of the End's air, as in the Overworld climb, returns the craft to the Gap just short of the End, shrunk again.

## The acid-cloud world by rocket

The acid-cloud world is a second point in the Gap along its own fixed bearing, the ochre planet in the sky, farther from the origin than the End. A craft that gets within its capture sphere while heading toward it arrives in the acid-cloud world at full size, high up, coming in fast and descending. A craft that misses coasts on. The planet grows in the sky as the craft closes on it.

The world has a yellow-green sky and fog, acid-green water and rain, and no mobs. The air is thick: density 3.0 at the ground, thinning with height to zero near the top of the build height (y 316). It holds no oxygen, so a [jet](Jet.md) cannot burn it and a person needs a [Respirator](Respirator.md) or a sealed cabin (see [thin air](Hazards.md#thin-air)). The ambient temperature is 90 Tu. Terrain is shaped like the Overworld's. Flying up out of the thick air returns the craft to the Gap just short of the planet, shrunk again.

Its crust is rich in osmiridium, from y 20 to 90 ([Ores](Ores.md)), the platinum-group ore.

The rain eats a hull. Every few seconds, exposed blocks of a metal with a **corrosion resistance under 0.9** (a block with air on at least one face) can be eaten away, the less resistant the metal the likelier. Steel, iron, bronze and lead go over minutes; tin, stainless steel, titanium and the noble metals are untouched. An eaten block leaves a gap in the hull, which you can patch with a block of a resistant metal. Resistances are on each [metal's page](Metals.md).

## The frozen moon by rocket

The frozen moon is a point in the Gap along its own fixed bearing, the pale blue-white disc in the sky. A craft that gets within its capture sphere while heading toward it arrives on the moon at full size, high up, coming in fast and descending. A craft that misses coasts on. The moon grows in the sky as the craft closes on it.

The world has no weather, and its water freezes. The air is very thin: density 0.15 at the ground, thinning with height to zero at y 220. It holds no oxygen, so a [jet](Jet.md) cannot burn it and a person needs a [Respirator](Respirator.md) or a sealed cabin (see [thin air](Hazards.md#thin-air)). The ambient temperature is -150 Tu. Flying up out of the air returns the craft to the Gap just short of the moon, shrunk again.

The ground carries **Nitrogen Ice**, mined with a pickaxe. Melted in a vessel held cold, about -205 Tu, a block gives 1000 mB of [nitrogen](Chemicals.md#nitrogen). The moon has no liquid seas.

## The crystal world by rocket

The crystal world is a point in the Gap along its own fixed bearing, the violet disc in the sky. A craft that gets within its capture sphere while heading toward it arrives in the crystal world at full size, high up, coming in fast and descending. A craft that misses coasts on. The planet grows in the sky as the craft closes on it.

The world lies under a violet haze, with frequent amethyst geodes in the ground. Amethyst is the crystal of [experience](Experience.md). The air is thin: density 0.8 at the ground, thinning with height to zero at y 300. It holds some oxygen, a share of 0.4, but too little to breathe unaided, so a person needs a [Respirator](Respirator.md) or a sealed cabin (see [thin air](Hazards.md#thin-air)). The ambient temperature is 15 Tu. Flying up out of the air returns the craft to the Gap just short of the planet, shrunk again.

## Salvage

Sparse **asteroids** drift in the Gap: tuff with veins of iron, pentlandite (nickel), sperrylite and osmiridium (platinum-group) and, rarely, beryl ore. Rare **derelict wrecks** with iron hulls hold a chest of iron, aluminum, copper and lead ingots, coal and redstone. Like everything else in the Gap they are scaled bodies, the right size beside a craft. They appear around players and not near the Gap's origin point, about 160 blocks across; each place is rolled once.

## Vacuum refining

A Thermal Vessel standing where the air has no oxygen (the Gap, the End) runs airless. Fine metal dust melts with no oxide loss, and airless processes such as charcoal making and sintering run without a sealed chamber. In free fall, [lead-aluminum alloy](Casting.md#free-fall-casting) resolves from a melt.

## Coming back

A body moving fast through air heats on its leading face. Blocks on the windward side burn off a voxel at a time when the heat passes what their material can take: burnable ones oxidize and metals melt. A block counts by the material it is a form of, so an iron block holds until iron melts and a wool block burns away at wool's oxidation temperature. A block that is no material's form does not burn. Thick burnable layers last longer, and refractory plates hold. Blocks that are only touching are not broken. A hard landing hurts everyone aboard like a fall of the speed change. A landing worse than a fall of about 10 blocks also shatters brittle blocks aboard, those with a hardness of 0.6 or under (glass and glass panes): each one breaks with a chance that rises from nothing at that fall to certain at twice it, and which ones go is not tied to where the craft touched.

## Living in it

- **Weight.** You have no weight, and you keep your speed.
- **Air.** There is none. Breathe from a [Respirator](Respirator.md) with an air cylinder, or from a sealed cabin with an [Air Valve](Air_Valve.md) (see [thin air](Hazards.md#thin-air)).
- **Heat.** It is cold, and a vessel keeps all the heat it has. A [Radiator Panel](Radiator_Panel.md) sheds it.
- **Radiation.** Your dose rate gains the sky's own. See below.
- **Map.** Below the craft the explored Overworld shows as a flat map at the chunks' real places, one pixel for each chunk, in the colors of the map. The higher the craft, the smaller and coarser the map.
- **Pushing off.** Touching a wall and pressing jump pushes you off it, the only way to start or change a drift.

## The sky

The Gap has its own sky. A hard sun lights the scene, and the planets are lit from its side. About six thousand stars fill the dark, with a milky band across them. The Overworld's air shows as a blue glow along the limb, at and below the horizon; the explored map (see Living in it) draws below that.

Four planets hang at fixed bearings, so each one is a landmark to steer toward:

- **Acid-cloud world.** Ochre and hazy.
- **Frozen moon.** Pale blue-white.
- **Crystal world.** Violet and teal.
- **The End's gas giant.** Banded, low on the horizon and small.

Each planet grows as you close on it: the gas giant as you close on the End, the others as you close on their own worlds.

## Radiation

In the Gap the sky's radiation adds to the [dose](Radiation.md) you carry, once a second. Armor shields it by density, as for any source, and so do the dense plates of a hull around you: more open sides to the sky means more dose.

## Commands

Operators can move a craft by hand: `/feedback space send`, `/feedback space back` and `/feedback space scale <k>` act on the craft you stand on or look at (send, back, and set its scale).
