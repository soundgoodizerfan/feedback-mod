# Fluid Caster

The fluid caster is a hand-held gun that throws a stream of whatever its tank holds. What the stream does comes from the fluid itself: water douses, hot fluid scalds, a fluid that burns sets things alight.

## Parts

Shaped at the bench that suits the stock ([Smithing Table](Smithing_Table.md) for metal, [Woodworking Table](Woodworking_Table.md) for wood) and assembled at the [Engineering Table](Engineering_Table.md). A **Blueprint** (draw one at the [Blueprint Table](Blueprint_Table.md), under Weapons) is optional; drawing it and assembling it take Weapons Treatise II, see [Treatise](Treatise.md). The parts:

| Part | Takes | Does |
| --- | --- | --- |
| **Caster Tank** | a metal plate | Holds the fluid and the pressure. Its **wall** (1 to 8 mm) and its metal's strength set the pressure it holds before it weeps; its **length** (10 to 40 cm) sets how much it holds. |
| **Caster Nozzle** | a metal rod, turned rod or ingot | Its **bore** (2 to 12 mm) trades reach against flow: a narrow bore throws a thin stream far, a wide one passes more fluid and spreads it. Its variant sets the fan. |
| **Caster Receiver** | any lump, sheet or board | The body the tank, valve and nozzle are fixed to, and the hand holds. Its variant is the frame. |
| **Caster Valve** | a metal rod or ingot | The valve the trigger opens. |
| **Caster Pump** (optional) | a metal rod or plate | A pump drive that holds the stream at a steady head. Its variant sets how it is worked. |
| **Caster Pilot** (optional) | a metal rod or turned rod | A small flame fitted at the nozzle that lights a stream of a fluid that burns. Its **orifice** (1 to 4 mm) sets how much fluid it burns. Its variant sets how far out the flame stands. |

The nozzle comes in four variants:

| Nozzle | Stream |
| --- | --- |
| **Jet** | A tight stream. |
| **Fan** | A narrow cone. |
| **Mist** | A wide cone. |
| **Atomiser** | A cone of fine mist that hangs where it lands as a vapor cloud. |

The receiver comes in three frames:

| Frame | Build | Effect |
| --- | --- | --- |
| **Squirt Frame** | A short body with the tank on top and a grip under it. The lightest. | The baseline for reach, cone and capacity. |
| **Lance Frame** | A long gun frame with a stock and the tank slung under a long, heat-shrouded pipe. Heavier. | A longer reach, a narrower cone and a bigger tank. |
| **Backpack Frame** | A short wand on a hose from a tank on a back plate. The heaviest. | Twice the capacity, but the hose loses some of the pressure on the way to the nozzle and the short wand reaches less. |

The pump comes in two variants:

| Pump | Worked by | Notes |
| --- | --- | --- |
| **Hand Pump** | The reload key. Its **stroke** (1 to 6 cm) sets how long one stroke keeps the stream going. | Needs no power. |
| **Electric Pump** | A motor that runs while left click is held. Its **motor** size (1 to 8) times its metal's strength sets its head. | Draws charge from a seated cell. |

The pilot comes in two variants:

| Pilot | Flame stands | Notes |
| --- | --- | --- |
| **Pilot Flame** | 0.4 blocks out from the muzzle | A small part. |
| **Torch** | 1.2 blocks out from the muzzle | More metal, and more fluid burned. |

The blueprint and the tooltips show these figures:

- **Tank**: how much fluid it holds.
- **Wall rating** (Pu): the pressure the walls hold before they weep.
- **Reach** and **Flow**: of a stream of water at the pressure the walls hold (or at the pump's head).
- **Head** (Pu): the steady pressure the stream is made at, a pump's head or what the walls hold with no pump. Under **4 Pu** nothing leaves the nozzle.
- **Corrosion resistance** (0 to 1): the weakest among the tank, valve and nozzle.
- **Tank service** (Tu): the hottest fluid the tank walls hold before they soften.
- **Pump over walls**: what the pump would drive over what the walls hold. Past **0.8** the walls hold the pump down to 0.8.

The caster's name takes an adjective for each fault, and Shift explains it:

| Adjective | Meaning |
| --- | --- |
| **feeble** | The head is under 4 Pu, so the stream does not leave the nozzle. |
| **short** | The reach is under 3 blocks. |
| **pump held back** | Pump over walls is above 0.8. |
| **undriven** | No pump is fitted, so it runs on the tank's own air. |
| **corrodible** | A part's corrosion resistance is under 0.3. |

The Blueprint Table marks a missing required part with "Required part missing." It also faults a tank whose metal states no strength: "This tank metal states no strength, so its walls hold no pressure."

## Filling

Right-click works as a fill or pour only while the crosshair is on a fluid source or a block that holds fluid; anywhere else right-click aims as usual.

- **Use it on a fluid source block** in the world: the tank draws the fluid in, as a bucket does.
- **Use it on a block that holds fluid** (a tank, a vessel): the caster is a hose and takes the fluid. If the block is under pressure, the caster takes that pressure with it, up to its own gauge reading.
- **Sneak and use it** on such a block to pour the tank back in.
- **In an inventory**, drop a fluid item onto the caster as you would an item into a bundle: pick the item up on the cursor and right-click the caster in its slot, or pick the caster up and right-click the fluid item. A full item (a bucket, for example) fills the tank and comes back empty. An empty item takes fluid from the tank, but only if it accepts that amount: a bucket needs 1000 mB. Only a single item works, not a stack. A potion bottle pours its 250 mB in and leaves a glass bottle; a glass bottle takes 250 mB of potion out as a potion bottle.
- A [pipe](Pipe.md) or a placed tank fills and drains the caster as it would any fluid container.
- **Press the unload key** (default `U`) while holding the caster to let the whole tank out at your feet. The tank pressure drops to zero, with a splash and the bucket-empty sound. The fluid lands like a stream that stopped there: a container block under you takes it in, a cauldron fills a level (water), farmland under you is watered (water); otherwise it is gone.

The tooltip names what the tank holds and how many mB, and says when it is under pressure. A potion in the tank is named by its effect and how fresh it is. The fluid keeps its temperature in the tank.

## Using it

Hold **left click** to open the valve; release to close it. A stream with nothing to throw, or with no pressure behind it, gives a dry click. The only fire mode is held-open.

The push comes in one of two ways, set by whether a pump is fitted.

- **No pump: a pressurized tank.** The tank's gauge pressure throws the stream. The pressure falls as the tank empties, because the gas has more room, so the stream weakens and shortens. The **reload key** works the tank's own air pump and raises the pressure; it climbs faster the fuller the tank is.
- **With a hand pump: a steady head.** The **reload key** is a pump stroke. Each stroke primes the stream for a while at a steady pressure, whatever the gauge says; strokes add up to a limit. A pump never drives harder than the walls hold.
- **With an electric pump: a motor.** Holding left click spins the motor up in about half a second, then it holds the stream at a steady head for as long as you hold. The reload key does nothing. A bigger motor in a stronger metal holds a higher head, still kept under what the walls hold.

### Seating a cell

An electric pump runs on charge from a battery or Leyden jar seated in the caster. In an inventory, pick the cell up on the cursor and right-click the caster in its slot to seat it; right-click the caster with an empty cursor to take the cell out. The tooltip reads **No cell seated.** or the cell's charge as a percent. The draw grows with the fluid moved and the head, so a wide bore or a high head drains the cell faster. An empty or dead cell gives a dry click.

A denser fluid leaves slower at the same pressure. A wide bore empties the tank faster. A stream pushes back on the wielder, harder with more flow and speed.

## What the stream does

Every effect is read from the fluid's own figures (temperature, flash point, thickness, toxicity, corrosivity, radioactivity), not its name. A fluid can do several things at once.

- **Hot** fluid (over **55 Tu**) scalds what it hits; the hotter and heavier the flow, the more. At **250 Tu** or over (lava, molten metal) it also sets the target alight.
- **Cold** fluid (**0 Tu** or under) chills what it hits, building frost faster the colder it is. Well under 0 Tu it freezes flesh outright.
- A fluid that **burns** (it has a flash point it is at or over) lights from an open flame in the stream's path: fire, lava, a burning body, or you alight. A lit stream sets bodies alight for as long as it burns, from 3 to 15 seconds, longer and harder the hotter its flame, and starts fires where it lands.
- An **oxidiser** (oxygen, air, liquid oxygen, liquid air) does not burn itself and leaves no vapor a flame sets off. A flame in its path (the pilot flame, a fire, a burning wielder) makes the stream flare: bodies in it take fire damage and are set alight for longer, and bodies already alight flare too. The more oxygen the fluid carries, the stronger the flare; argon or other inert filler in it dilutes the flare.
- **Water** puts out burning bodies and fires, douses lit blocks, waters farmland and fills a cauldron a level at a time. Creatures that hate water are hurt.
- A **thick** fluid (viscosity 5 or more) clings to what it hits and slows it; the thicker, the slower.
- An **acid or a base** burns flesh as chemical burn and bites the armor of what it hits. How hard depends on the fluid's **corrosivity**, its pH distance from neutral (see [Materials](Materials.md#heat-and-other-figures)). Hydrofluoric acid is a weaker acid than most but very toxic.
- A **toxic** fluid (toxicity 0.8 or more) poisons what it hits, longer the more toxic.
- A **radioactive** fluid (uranyl sulfate, thorium hydroxide) withers what it hits and outlines it with a glow.
- A **spatially unstable** fluid hops whatever stands near it from place to place.
- A heavy, fast flow **shoves** what it hits, like a fire hose. A fast, dense stream also **cuts**.
- A stream of a fluid with no color of its own takes the average color of the fluid's texture.
- A **potion** gives its effect to each body in the stream, and acts where it lands; see [Sprayed from a fluid caster](Thrown_and_Imbued_Potions.md#sprayed-from-a-fluid-caster).
- A **container** the stream lands on (a tank or other fluid block) takes the fluid in.
- An **atomiser** leaves a vapor cloud where the stream lands or runs out; a fluid that burns makes a cloud a flame can set off. See [Hazards](Hazards.md).

### Gases

A fluid hotter than its boiling point leaves the nozzle as a gas. It makes no ribbon and does not shove. A toxic gas leaves a lingering poison cloud where it goes; a gas that burns leaves a vapor that a flame sets off. See [Fume clouds](Hazards.md#fume-clouds).

Nothing places a fluid block in the world.

### Pilot

Tap the **Gun option** key (**V** by default) with a piloted caster in hand to turn the pilot flame off or on; the action bar reads *Pilot flame: lit* or *Pilot flame: out*. With the pilot out there is no flame at the nozzle and no trickle drawn from the tank.

The pilot is lit only while the valve is open, the tank's fluid burns or is an oxidiser, and the muzzle is not under water; closing the valve puts it out. While lit it shows as a small flame at the nozzle and draws a trickle from the tank, more through a wider orifice. It counts as an open flame in the stream's path, so a burning stream, a body set alight, fires and a vapor cloud going off all follow as above.

Its tip also lights a campfire or a candle, starts a fire beside a flammable block where the game's fire spread allows it, and sets off a flammable block the tip is inside, about four times a second.

## How the stream looks

The stream is drawn from the fluid and the nozzle, so you can read what is in the tank and how hard it is driven from the spray.

- **Liquid** leaves as droplets in a cone and falls in an arc. A weakly driven gun sags and drops its stream short. A wider spread makes smaller droplets; a thicker fluid makes bigger ones. A hard-driven jet keeps a bright core near the nozzle.
- **Gas and mist** leave as soft puffs that slow as they go.
- **Burning fuel** is a billowing flame that runs white-yellow, then orange, then red, with a rim of soot, and glows where it lands. The flame is drawn with a flickering shader; the **shaderFlame** setting in the mod's client config (on by default) turns it off, and a shader that fails to load leaves the plain flame. A cooler fuel leaves more soot; a hotter one burns whiter.
- **Cold fluid** (0 Tu or under) is pale, trails fog and leaves frost motes where it lands.
- **A potion** sparkles.
- **An oxidiser** with a flame in its path whitens as the drops fly.

A stream 48 blocks or more away is drawn as a bare strip. A lower **Particles** setting in the video options thins the droplets.

## Misuse and hazards

The caster is safe when the fluid and the pressure suit its parts. See [Hazards](Hazards.md).

- **Overpressure.** Pressure over the wall rating makes the walls **weep**: a hiss, lost pressure and fluid, and wear. Well over it the tank **bursts**: the fluid is gone, the gun is badly worn and you are hurt (scalded as well if the fluid was hot). Pumping the air stroke past the rating is the usual way.
- **Corrosive fluid** eats the tank, valve and nozzle, the weakest of them first, both in the tank and as it flows.
- **Hot fluid.** A fluid hotter than the tank's metal can take wears the walls; one at the metal's melting point lets the tank go.
- **Dry pump.** A pump stroked with an empty tank heats and wears. An electric pump held on with an empty tank wears the same way and spends no charge.
- **Flashback.** A burning stream of a fluid that gives off vapor, through a bore of **6 mm** or more, with the flame near the muzzle (a lit pilot is such a flame, and runs back about once in 25 ticks of flow), runs back into the tank: the tank loses half its fluid and its pressure, the gun wears, and you take fire damage and are set alight. A narrower bore stays safe.
- **Hot fluid and the pilot.** A fluid hotter than the pilot's metal can take slowly wears the gun.

A worn caster loses some of its push.
