# Electric Gun

The electric gun, or **pulse gun**, fires a pulse of electrical energy from a battery's charge instead of a bullet. The pulse is instant: it holds its full damage out to a range, then nothing. It is not a [gun](Guns.md): it burns no powder and takes no ammunition, a shot costs battery charge and makes heat. It is assembled at the [Engineering Table](Engineering_Table.md) from its parts. A **Blueprint** (draw one at the [Blueprint Table](Blueprint_Table.md), under Weapons) is optional; drawing it and assembling it both take Weapons Treatise III: Electric Guns, which covers the gun, every seat and every fitting below.

## Parts

Shaped and assembled like a [gun](Gun_Parts.md), out of the same receiver, trigger, grip, stock and sight parts, plus the pulse barrel and a battery seat.

| Part | Takes | Does |
| --- | --- | --- |
| **Receiver** | any material | Its **type** is the same three as a gun's, and each sets how big a battery it takes: a **handgun** frame up to 2 cells, a **long gun** 4, a **machine gun** 8 (the bigger frames also keep their heat better). Its material's insulation sets the band it holds off. |
| **Pulse Barrel** | any material | Axes: barrel length and bore. A longer barrel holds the pulse's damage out further and shoots truer; a wide bore hits a little harder in a wider cone. A barrel that **conducts** takes energy out of the pulse and turns it to heat, so it hits weaker and heats the gun more; a poor conductor (glass, ceramic, bone, wood) gives the strongest pulse. |
| **Pulse Seat** | a conductor, packed in a dielectric | Required. A frame of contact bars and a bank of capacitors. Its shape is the gun's **heat cycle** (below); its conductor is the ceiling on how much energy one pulse can hold; its dielectric sets what a capacitor bank or burst bank holds; its **power tier** takes Light, Standard or Heavy of that ceiling. |
| **Heat Sink** | any material | A block of metal clamped to the barrel; a thermopile seat requires it and the table refuses the seat without one. Its mass times its metal's specific heat sets how long it takes to fill (below). |
| **Trigger group** | as a gun's | Required. |
| **Grip** | as a gun's | The same four shapes. |
| **Stock** | as a gun's | Fixed, skeleton or folding: steadier aim, by its core and pad materials. |
| **Sight** | as a gun's | A reflex sight or a scope (a frame and a lens you choose, with a zoom axis from 1.5 to 6), and a lit reticle (a speck of luminous mineral in a ring of iron) that cuts spread in the dark. |

### Power tier

The seat's tier is **Light**, **Standard** or **Heavy**. Each step up raises the pulse's energy and the heat per shot, the recharge, the battery it drains, the recoil and the strain on your hand together. A pulse goes through one more body per step (Light one extra, Standard two, Heavy three) and holds its damage out about a seventh further. A better conductor in the seat lifts all three tiers.

### Seats

The seat's shape is how the gun fires and cools. The **V** key cycles the fire mode among the modes the seat allows.

| Seat | How it fires | Modes |
| --- | --- | --- |
| **Cylinder** | Six small capacitors in a drum, each trickle-charging from the battery. A shot spends one and the drum turns a place; the wait for the next shot is a capacitor filling, faster with a stronger battery and a better conductor in the seat, slower at higher tiers. | semi |
| **Flux Cycler** | Every shot draws from the battery with no capacitor to wait for, and the heat goes into the barrel, which limits how long you can hold fire. | auto, semi |
| **Capacitor Bank** | Every cell discharges at once into one very large pulse that goes through **every** body in line, then recharges for a long time (about 8, 12 or 16 seconds by tier with a two-cell battery, longer with more cells). The insulation of the plates sets how much the bank holds. | semi |
| **Core Breech** | One very large shot of slow heat: pellets that fly a few blocks (one big pellet, or a cone of them with a beam splitter), light what they hit and fall away at close range. The cores are then too hot to fire, so after every shot the gun vents itself: your support hand slams the barrels open, the cores vent in a puff of steam, and the hand throws them shut. The gun is ready again once the whole 0.9 s motion has finished. A Feedbacker punch sends your own pellets back fast and hard. | semi |
| **Burst Bank** | A bank spent as three shots, then recharged. | burst, semi |
| **Flywheel** | A wheel that spins up while you hold the trigger, so the first shot waits, and spins down quickly after you let go. A denser wheel holds more and spins up slower. The wheel turns on the gun and the report rises in pitch with its spin. | auto, semi |
| **Recoil Dynamo** | A coil that returns a tenth of each shot's energy to the battery. | auto, semi, burst |
| **Thermopile Seat** | A ring of two joined metals between the barrel and the clamped heat sink, turning the barrel's heat back into charge: each shot returns a share of its energy to the battery, more the hotter the barrel, up to a cap set by the power tier. A cold gun is a plain gun. The seat's conductor and its dielectric slot are the two metals: the further apart their Seebeck figures, the more each shot returns. A material's file gives its Seebeck figure where it has one; otherwise it is estimated from its electrode potential. The sink fills as it soaks: it takes about 8 returning shots for a sink with the heat capacity of 1 kg of copper, and a full sink returns nothing until it has emptied, about 20 seconds. | auto, semi |
| **Tether Seat** | The gun holds no battery and draws from a battery in a [backpack](Backpack.md#fittings)'s battery fitting, or from the battery bay of a worn [exosuit](Exosuit.md). | auto, semi, burst |

The capacitor bank, burst bank, core breech, flywheel and thermopile need a **long gun or machine gun** frame; the table refuses them on a handgun frame.

### Heat governor

Barrel heat is the barrel's real temperature, in Tu, and it warms and cools as a [gun's](Guns.md#heat) does: the fittings (a shroud, a thermal sleeve) set how fast it sheds heat, and the limit is the barrel material's service temperature. With no governor fitted, a shot fired at the heat limit **burns you and wears the barrel**. A **Heat Governor** is a bimetal strip or a thermistor bead pressed against the barrel, in a housing with a heating element and contacts, working a relay in the firing circuit. One of four fits, and only one:

- **Fire-Rate Throttle:** the rate of fire falls smoothly as the barrel heats, to about 40% of normal near the limit, so the gun never reaches the limit that burns.
- **Forced Vent:** at the limit the gun locks out, for between about 1.5 and 4 seconds depending on how far over it ran, and lets go a cloud of **steam** in front of the muzzle. The steam lives about 3 seconds and scalds any living thing that stays in it for about a second, you and your friends included.
- **Dead-Man Cutoff:** stops the fire on barrel heat alone and lets it resume once the barrel has cooled. Its shape is chosen at the part bench.
- **Surge Shunt:** a hand-closed shunt that bypasses the relay. Press the reload key and for a window of about 3 seconds the rate of fire doubles, each pulse is 1.25 times stronger and the barrel heats faster; then the gun locks until the barrel is back under 60% of its limit. The contacts' metal sets the window: a metal that welds or softens at a lower temperature than copper shortens it, a higher one lengthens it, between 0.4 and 2 times. A shot at the heat limit inside the window burns you, as with no governor.

The governor's **element** is how fast it follows the barrel: a housing that conducts heat well reads quickly and a poor conductor reads low, so a throttle slows late and a dead-man cutoff can let a shot through past the limit, which burns you.

### Fittings

The same fittings as a gun's, made of metal and behaving as described under [Gun Parts](Gun_Parts.md#fittings), where they make sense: a **suppressor** (a quieter report, more heat), a **muzzle brake** (louder), a **bayonet**, a **bipod** (steadier), a **counterweight**, a **shroud** and a **thermal sleeve** (the gun sheds heat faster, so it recovers faster), and a **resonance weight**. The pulse gun's own:

The **reload key works exactly one fitting** per gun: a pump charge, a hold-charge trigger, a surge shunt or a shock lead. The [Blueprint Table](Blueprint_Table.md) refuses two.

- **Hold-Charge Trigger** (a long-travel lever on a leaf spring; seats that fire one shot a pull: cylinder, capacitor bank, burst bank, core breech): hold the reload key to build a stronger shot that goes through more bodies, and release to fire; releasing early gives an ordinary shot. The spring is a material choice: its yield strength sets the power a hold builds, and a flexing lever widens the release.
- **Pump Charge** (a pump lever on a ratchet turning a hand magneto winding; the same seats): press the reload key to pump: on a long gun the forend slides back and runs home (your support hand rides it), and the click sounds as it arrives; a hot core breech cannot be pumped until it has vented. Each of the first two pumps widens the spread and raises the power and heat of the next shot; with a beam splitter fitted each also adds beams, as buckshot adds pellets. The third pump sounds a tone: the next pull fires no pulse but a blast around the gun that deals 1.5 times the total damage of the three-pump shot (a splitter's beams added) to everything within 4 blocks, and the same to you, capped at three quarters of your health. The blast spends the barrel: it adds no steam or burn from the governor, and the barrel is left cool. Further pumps do nothing, and any shot resets the pumps. The winding is a material choice: its ampacity sets the energy of each pump, and a weak lever bends on the third pump and wears the gun.
- **Beam Lens:** a housing round cut-glass facets or a pair of spark-gap electrodes. A **beam splitter** turns one pulse into three to seven weaker beams in a spread, hardest up close; you choose the beam count, in steps of one, on the table. An **arc fork** (the electrodes) carries the shock of a hit on to the nearest conductive body within reach, then to any body. A **coherence lens** bounces the pulse off what it strikes, and a **malefic lens** blows it at the first thing it meets (both below). A gun has one lens: splitter, fork, coherence or malefic.
- **Coherence Lens:** the pulse bounces off what it strikes and keeps the share that surface reflects. A block's reflectivity is its material's own where it is one material: metal blocks and polished stone keep a lot, ordinary stone, wood and ice keep some, tinted glass a fair share, and dirt, sand, gravel and wool almost nothing, so a wall of dirt ends the beam. The lens lining's own reflectivity sets how many bounces the pulse survives, and a held hold-charge trigger buys up to 2 more. A beam that crosses a projectile in flight (an arrow, a fireball, a skull) sets it off where it is, with a small blast. The bounce gives no aim assist.
- **Malefic Lens:** the pulse goes through nothing: it stops at the first body or block and blows there. The blast's radius follows the energy put in, from 1.5 to 6 blocks, and it deals 1.6 times the pulse's damage, less with distance; a wet or metal-armored body takes the shock bonus. Blocks break by the game's rules. Stand inside the blast and you take it too, capped at three quarters of your health.
- **Shock Lead:** a reel and a plug on a line, with a **line length** axis from 3 to 16 m. Press the reload key to throw the plug at the creature you aim at within the line's reach; the battery then drains down the line into it and the charge builds the longer the plug stays in, faster in a wet or metal-armored body, up to what the line's ampacity can carry. Press the key again to discharge; it also discharges when the charge is full, when the cell goes flat and after 5 seconds. The shock arcs on from the body to wet or conductive bodies near it, as an arc fork's does. The line breaks if it is stretched past its length or a block comes between you and the plug. The lead bites creatures only, not blocks.
- **Zoom Charge:** a solenoid, a winding of wire round an iron core. Zoom builds while you aim and tightens the shot and lengthens its reach; it starts over after each shot. The core must be magnetic for the full field. A winding of a better conductor builds more of both; a poor one builds half.
- **Heat Gauge:** a thermistor on the barrel feeding a moving-coil meter: a dial on the gun whose needle swings with the barrel's heat, and a finer heat bar with a scale on the HUD. The gauge coil's material sets how finely the HUD reading steps. A fitted heat governor sits on the gun's right flank, clear of the gauge.

The gun is drawn from its parts like a powder gun: the frame by receiver type, a **cell bay** on the left with a glass window and four lamps that fill with the battery's charge, and the seat on the right with its dials and lamps. The drum turns a place a shot with a lamp for each charged chamber, the flywheel spins, a bank shows its charge. An empty bay is a dark slot.

### Naming

A built gun is named after its seat and frame. A seat word replaces the class on any frame: a cylinder is a **Rotary Slinger**, a capacitor bank a **Railcannon**, a burst bank a **Triplet**, a core breech a **Core Breech**, a flywheel a **Spinner**, a thermopile a **Hotshot**, a flux cycler a **Flux Cycler**. A recoil dynamo or tether gun is a **Pulse** plus its class by frame and size: Pulse Pistol, Pulse Hand Cannon, Pulse Carbine, Pulse Rifle, Pulse Marksman Rifle, Pulse Sniper Rifle, Pulse Cannon, Pulse Machine Gun. A **Light** or **Heavy** tier adds that word before the name; Standard adds none. A name gives no bonus. What gives each is under [Electric guns](Designations.md#electric-guns).

## Beam color

At the Blueprint Table a pulse gun's drawing has a **beam color**: type a hex color (`#RRGGBB`) beside the swatch at the top right of the canvas. It is blue until you change it. It is saved on the blueprint and carried onto a gun built to that drawing, and it tints the beam, the sparks and glow at the muzzle and where it hits, and the lamp at the muzzle. A gun made with `/feedback build` keeps the drawn color.

## Using it

**Load a battery** (one that fits the receiver) by clicking the gun in your inventory with a [battery](Battery.md) on your cursor; click the gun with an empty cursor to take it out. The battery keeps its charge, so it goes back to a [Charger](Charger.md) as it is.

The gun is held and fired like a [gun](Guns.md#controls): attack (left mouse) fires, and holding it repeats where the seat allows; use (right mouse), held, aims down the sights. The view kicks and the hand strains on each shot as with a powder gun (see [Handling](Guns.md#handling)). The gun's hand rides each recoil out and back over a share of the gun's cycle: a fast gun twitches, a slow one heaves. A weak battery gives a weaker shot, never a refusal.

### What a shot does

- A pulse is instant and holds its full damage out to a cutoff range, then nothing. A barrel of 0.4 m reaches about 70 blocks at Light; a longer barrel reaches further, and so do higher tiers and a zoom charge.
- It goes through glass and glass panes, but not tinted glass, and stops at any other block.
- It hurts a limited number of bodies in a line (set by the tier and a hold charge); a capacitor bank's pulse hurts every one.
- Armor resists it as it resists a bullet, except **metal armor**, which carries the shock: a body in metal armor, or a **wet** one, takes extra damage, and is staggered for a moment by the shot's energy.
- A beam is drawn from the muzzle in the gun's color with sparks where it lands, and a hole where it strikes a block that is small for a weak shot and larger for a strong one, and the muzzle glows brighter as the barrel heats. The build sets its look:
  - **Laser** (any other build): a thin hard beam, sparks and a scorch where it lands.
  - **Pulse** (a capacitor bank, a burst bank or a wide bore): a thick jittery beam that crackles.
  - **Arc** (an arc fork lens): a jagged branching bolt that chains between bodies and arcs to wet targets.
  - **Plasma** (a core breech): a slow glowing blob that splashes where it lands.

A coin flipped with [Coin flip](Skills.md#observation) redirects a pulse without costing it any damage, the pulse holds in the struck coin for a blink with a ringing note before it goes on, and a **cylinder** pulse hits harder when redirected.

Skills and casts that need a loaded gun (quick draw, walking fire, Overwatch, Snap Volley and the like) work with it, counting its charge as shots ready.

### HUD

Holding the gun shows a card with the battery's charge in percent, what the seat holds (six pips for a cylinder, a bar for a bank or a flywheel's spin), a heat bar and the fire mode. **NO CELL** shows when no battery is loaded, **NO TETHER** when a tether seat has nothing to draw from, and **CELL FLAT** when the battery has no charge left.

## Misuse and hazards

Ordinary shots never wear the barrel; the tooltip shows misuse wear as **Bore wear**. Fittings wear the gun with use: a muzzle brake's ports and a relief vent's seat, more in a soft metal, and a pump charge's weak lever. A battery whose case band is over the receiver's band shocks you and wears the gun, more the further over it is. Firing in water or rain arcs to you unless you are insulated (insulating gloves stop it; see [Hazards](Hazards.md#shock)); an arc fork fired in water shocks everything wet within reach, you included. A shot at the heat limit with no governor, or inside a surge shunt's window, burns you and wears the gun. A malefic lens blasts the wielder too when you stand in the blast. A shock lead trailed through your own water **shorts to you** and the line goes dead, unless a glove rated for the cell's band insulates your hand; the part tooltips show these as *Blasts the wielder too* and *Shorts in water*. The steam of a forced vent scalds you as it does anyone else. A worn gun delivers weaker pulses and spreads wider, as a worn [powder gun](Guns.md#wear) does; **Ward** slows the wear, **Mend** repairs it and **Tithe** adds wear on kills (see [the gun table](Enchanting_Table.md#on-a-gun)).

## Wither skeletons

A wither skeleton sometimes carries a weak pulse pistol (a cylinder seat at the Light tier) and fires it from a distance with a clear line. Plain skeletons keep their powder guns.

## Attunements

The seat takes Charge, which holds a bigger pulse; see [the gun table](Enchanting_Table.md#on-a-gun).
