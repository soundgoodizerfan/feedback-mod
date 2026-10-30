 # Skills

Combat has a **skill tree**, opened with **K**. A skill never locks something you could otherwise do: it either nudges a figure (a wider [parry](Combat.md#parry) window) or opens a **technique** the body could always attempt badly. Nothing is gated on what you're holding by name.


## Points and pools

There are seven trees, each with its own **pool of points**, earned by doing that tree's thing. Spend points on a node with **Learn** in the skill screen. A node needs the nodes it requires to be learned first. Points and learned nodes **survive death**.

### Earning points

| Pool | You earn it by | Points |
| --- | --- | --- |
| Anchor | landing a [parry](Combat.md#parry), per attacker | 10 |
| Veil | a [roll](Combat.md#roll) that dodged a blow struck by something (fire and other sourceless damage don't count), per attacker | 8 |
| Brute | a main-hand hit with a blade, mace or spear (by its design, not its name) | 3 |
| Onslaught | any blow with an empty main hand (a punch, a kick) | 3 |
| Observation | a gun hit (a bullet) from 6 blocks or more away; nearer hits pay nothing | 2 |
| Proficiency | surviving a blow struck by something, per attacker (fire, falls and other sourceless damage don't count, and a blow that kills you pays nothing); also a small trickle for taking a fume, radiation or risky-food dose, per kind | 4 |
| Chaos | a new kind of disorder left on a target: burning, armour worn, weapon stripped, knocked into another body, blinded, tripped, pinned, powder-burned, panicked. Each kind pays once per target and repeats pay less | 8 |

**Repeats pay less.** Each award is 60% of the previous one from the same target (or attacker), and one repeat is forgotten every 10 seconds. A dummy stops paying; a fresh enemy pays in full. Off-hand strikes earn nothing yet.

## Techniques, keys and stamina

Many nodes unlock a **cast**: a technique you slot into one of five **cast slots** (keys default to **Z**, **Y** and **G** for the first three; the radial on **C** also fires them) on the Equip tab of the skill screen, then fire with that key. Nodes that are not casts are **riders** (they change what a blow, parry or roll does) or plain **figures** (a number nudged).

- **Stamina** is the one meter. It runs to 100; each cast spends some (its cost is in its description). After about a second without spending it refills, and refilling costs a little hunger.
- **Rearm.** A cast that fires is *spent* until its rearm happens: a timer, a landed blow, a landed parry or staggering something. Each node lists its own; a node with none shares the clock of the node it requires.
- Some nodes sit in an **exclusive group**: you can own only one of them at a time (respec to swap). Some need a **trait** on the weapon and do nothing without it.

## The trees

Each tree is a web grown from one root node; the table lists nodes from the root outward.

### Anchor

Anchor: hold ground. Parries, bracing, taking blows without moving, and what you answer with; the Steady limb carries that into work: footing against a pull, a hand crank under load, a braced soldering hand. Pool `anchor`.

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Wider window | 20 |  | Parry window +2 ticks |
| Quick recovery | 30 | Wider window | Parry recovery -4 ticks (shorter) |
| Settle | 30 | Wider window | Parry recovery -2 ticks (shorter) |
| Tight redirect | 30 | Wider window | A turned projectile flies straighter (-1.5 degrees of spread) |
| Challenge | 40 | Quick recovery | Cast: ring your weapon on your guard; every hostile within ten blocks turns on you. The weapon pays a point of wear for the noise. Costs 15 stamina. |
| Hard stagger | 40 | Tight redirect | Parried attackers stagger +25% longer |
| Roll with it | 50 | Quick recovery | A parry on the last tick takes the blow on the weapon's flat: excess force becomes extra stagger on the attacker instead of weapon wear. Price: the late tick. |
| Brace | 60 | Quick recovery | Cast: plant yourself for four seconds. Knockback cannot move you and you can barely walk, and every blow taken is held by your mass (armour counts) against the attacker's: the held share is not taken, it goes into the attacker as stagger, and the held weapon wears for it. Costs 20 stamina. *(once used, rearms on a timer.)* |
| Return fire | 60 | Tight redirect | A weapon parry in its window turns a bullet back at the shooter carrying the round's own energy, not a multiple. Price: the parry timing, and the blow wears the weapon. |
| Braced | 40 | Brace | Recoil is momentum. Opens the recoil techniques. |
| Human shield | 60 | Brace | Cast: the body you hold takes every blow aimed at you for as long as you hold it. If it dies, so does the cover. Needs a hold (Grab). Costs 20 stamina. *(once used, rearms on a timer.)* |
| Rebuke | 60 | Hard stagger | Cast: for three seconds every melee blow you take is struck back at the attacker for six tenths of what it dealt. You still take all of it. Costs 25 stamina. *(once used, rearms on a timer.)* |
| Shield wall | 70 | Brace | Cast, with a shield or blocking item in hand: for three seconds anything that comes at you inside the cone in front is turned back and staggered once, by its mass and speed. Costs 25 stamina. *(once used, rearms on a timer.)* |
| Riposte | 80 | Hard stagger, Tight redirect | After a successful parry, your next melee hit within 1.5 s (30 ticks) deals 1.5x damage. *(once used, rearms on a landed parry.)* |
| Braced fire | 50 | Braced | Shooting braced (crouched against a block, or in the air against a wall) sheds most of the heat bloom of the spread. Price: it needs terrain. |
| Throwback | 50 | Riposte | A parry made with the off hand empty returns the attacker's stagger doubled; the riposte window is your free counter. Price: a free hand, so no shield or dual wield. *(excludes Answer, Slip; respec to swap.)* |
| Answer | 60 | Riposte | The first blow after a clean parry is blocked outright. Price: the guard rearms only when you land a blow, so waiting buys nothing. *(once used, rearms on a landed blow; excludes Throwback, Slip; respec to swap.)* |
| Slip | 60 | Riposte | A parry on the window's very last tick turns into a step-through: the blow whiffs and you end up behind the attacker. Price: the last tick is the narrow one, and a missed slip is normal recovery. *(excludes Throwback, Answer; respec to swap.)* |
| Plant feet | 30 | Settle | Crouched, a tether (a grapple line) pulls you 30% less; worn armour mass adds to the hold. Holding against a pull pays a small trickle of Anchor points. |
| Dig in | 40 | Plant feet | Crouched, a tether pulls you a further 20% less. |
| Crank beat | 30 | Plant feet | Each click on a hand crank whose network carries a load keeps it turning 8 ticks longer, so a slow cadence holds the beat. Cranking a loaded crank pays a small trickle of Anchor points. |
| Bench stance | 30 | Plant feet | Lift the soldering iron crouched and the tip may stray 0.2 of a pad pitch further before a joint bridges. |
| Advance | 70 | Shield wall | Cast: for three seconds walk forward behind a held guard; everything in front is shoved back by the ratio of your mass to its, and the lighter ones are staggered. Needs a shield-type item. Costs 25 stamina. |
| Reckoning | 70 | Riposte | Cast: the damage your parries took in the last ten seconds is banked, and this lets it go as one blow at what you face, staggering it by twice that. The harder you were hit, the harder it lands. Rearms when you stagger something. Costs 30 stamina. *(once used, rearms on staggering something.)* |
| Reprisal | 70 | Riposte | Cast: for five seconds every parry you land is answered at once with a blow at whoever struck. Costs 25 stamina. *(once used, rearms on a timer.)* |

### Veil

Veil: act unseen. Rolls, shadow steps, smoke, wall kicks; the Quiet hand limb shrinks how far your steps and hands are heard by sculk and wardens, and steadies a soldering drag. Pool `veil`.

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Shadow step | 50 |  | Cast: vanish and step in behind the nearest thing in front of you, within nine blocks, on the side it is not facing. Needs room to stand there. Costs 25 stamina. |
| Quiet roll | 25 | Shadow step | Roll goes 1 block further |
| Ambush | 50 | Shadow step | The first blow within three seconds of a Shadow step staggers its target, by the blow over its mass, tripled, whatever it was facing. The step is the opening; a dagger's backstab is a different thing (the weapon's own). |
| Pocket sand | 50 | Shadow step | Cast: throw a fistful of the ground at the eyes of a cone: loose sand blinds six seconds, dirt three, bare stone gives you nothing to throw. Targets lose you and flinch. Reads the block underfoot. Costs 15 stamina. |
| Smoke step | 60 | Shadow step | Cast: roll out of a cloud left where you stood; for three seconds anything inside it loses you as a target, unless it is already at your elbow. Costs 25 stamina. |
| Hushed step | 30 | Shadow step | Your footsteps carry a quarter nearer to sculk sensors and wardens. Each listener that would have heard you and does not pays a small trickle of Veil points. |
| Muffled boots | 40 | Hushed step | Your footsteps carry a further 20% nearer. |
| Soft hands | 30 | Hushed step | Breaking, placing, opening a container and pressing a button carry 30% nearer to sculk sensors and wardens. |
| Quiet drag | 30 | Soft hands | While soldering, the tip may stray 0.15 of a pad pitch further before a joint bridges. |
| Long roll | 20 | Quiet roll | Roll goes 1 block further |
| Slip past | 35 | Quiet roll | Roll invulnerability +2 ticks |
| Dodge charge | 40 | Quiet roll | Rolling through an attack clears the gun's cycle wait, so the next shot is ready at once. Price: the roll has to cross a blow, and it rearms on the clock. *(once used, rearms on a timer.)* |
| Grit | 40 | Pocket sand | Rider: Pocket sand blinds two seconds longer, and crushed stone underfoot will do. |
| Thick smoke | 40 | Smoke step | Rider: Smoke step's cloud is half again as wide and lasts five seconds. |
| Vanish | 70 | Smoke step | Cast: invisible for about five seconds, less the more armour you wear (mass clanks); landing a blow gives you away. Reads worn armour mass. Costs 20 stamina. *(once used, rearms on a timer.)* |
| Far roll | 25 | Long roll | Roll goes 1 block further |
| Long shadow | 40 | Vanish | Rider: Vanish lasts three seconds longer. |
| Spent-case draw | 40 | Dodge charge | Reloading during a roll takes only as long as the roll has left. Price: the reload still needs the roll to be long enough. |
| Wall kick | 70 | Long roll | Tap jump in the air, facing a wall, within 1.2 s of sprinting or rolling on the ground, to kick off it, up and back. Costs 10 stamina; resets fall distance. *(once used, rearms on a timer.)* |
| Wall rebound | 60 | Wall kick | A wall kick keeps its momentum for one more kick off a second wall before you touch ground. Price: it needs a second wall. *(excludes Wall smash; respec to swap.)* |
| Wall smash | 60 | Wall kick | A kick that sends a foe into a wall adds the weight of the armour they wear to the stagger. Price: it needs a wall at their back. *(excludes Wall rebound; respec to swap.)* |
| Wall run | 70 | Wall kick | Cast: with a wall beside you, run along it for most of a second, the wall taking your weight; look steers, crouch drops you. Reads whether a wall is at your side and your sprint. Costs 20 stamina. |

### Brute

Brute: overpower. Weight: the bigger hit, the heavier body, the harder push. Pistol-whipping, grabs and throws, maces and spears, stomps. Pool `brute`.

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Grab | 20 |  | Take hold of what is in front of you, bare-handed. A worn glove improves it: a longer hold, and heavier targets. Throw and the grip nodes build on it. |
| Shove | 50 | Grab | Cast, held: hands on a vehicle body. A body no heavier than you and your armour is held up in front of the camera; a heavier one is shoved or dragged along the ground at your mass times your speed, with an equal push back on you. See [Shove and carry](Vehicles.md#shove-and-carry). Costs stamina for work delivered. |
| Heavy lift | 40 | Shove | You can carry a body up to 1.5 times your weight before it must be shoved. |
| Deadlift | 70 | Heavy lift | You can carry a body up to 2 times your weight. |
| Pistol whip | 15 | Grab | Hold the melee-stance key to swing a held gun as a bludgeon. Damage and recovery follow the gun's mass. *(once used, rearms on a landed blow.)* |
| Firm grip | 25 | Grab | [Grab](Combat.md#grab) holds 1 second longer |
| Mace quake | 60 | Grab | A mace blow of 5+ damage shakes the ground: bodies within 2.5 blocks of the target are knocked back and staggered. |
| Quake leap | 60 | Grab | Cast: vault forward and up, and land hard. The landing shoves everything within reach and staggers it by your mass (armour counts) and how far you fell, over each target's own mass. A heavy armoured fighter shakes the ground. Costs 25 stamina. |
| Heavy butt | 25 | Pistol whip | Parried attackers stagger +15% longer |
| Iron grip | 45 | Firm grip | [Grab](Combat.md#grab) holds 2 seconds longer |
| Chamber | 50 | Pistol whip | A pistol-whip heavy enough to stagger the target fires a free shot at it. Needs a loaded gun; one stagger, one shot. *(once used, rearms on a timer.)* |
| Stomp | 50 | Quake leap | Cast: your whole weight on the nearest staggered thing in reach, as damage and a fresh stagger. Nothing standing steady will take it. Rearms on a stagger. Costs 20 stamina. *(once used, rearms on staggering something.)* |
| Dual draw | 60 | Pistol whip | The off-hand gun can pistol-whip too, on its own cooldown. |
| Throw | 60 | Firm grip | Release a staggered body you have pulled in (a grab, or a grapple that reached you) past you along your look; mass decides how far, and whatever stands there does the rest. Price: you are left in reach, recovering. Spent until you stagger something. *(once used, rearms on staggering something.)* |
| Whirl | 60 | Mace quake | Cast: a full turn with the held weapon, striking everything in reach at four-fifths of its damage and staggering it by that damage over its mass. Rearms when you stagger something, so it chains off a parry or a kick. Costs 30 stamina. *(once used, rearms on staggering something.)* |
| Cleave | 70 | Mace quake | Cast: raise a heavy weapon (damage 5 or more) and bring it down; the target takes the blow and a crack runs seven blocks on, staggering by the weapon's damage and fading with distance. Costs 30 stamina. *(once used, rearms on a timer.)* |
| Point blank | 70 | Pistol whip | Cast: fire into a body you hold or one already staggered within reach; the round's whole impulse is stagger and, past ten joules to the point, damage. Costs a round and 25 stamina. *(once used, rearms on a timer.)* |
| Vice | 35 | Iron grip | [Grab](Combat.md#grab) holds 1 second longer |
| Brand | 40 | Heavy butt | A whip with the barrel above the burn line sears the target by the barrel's temperature, and the barrel gives up some heat. The more you fired first, the harder it brands. Price: you shot to get it hot, and a hot barrel burns your own hand. *(excludes Muzzle stagger, Bayonet drill; respec to swap.)* |
| Crush | 40 | Stomp | Rider: Stomp hurts half again as much. |
| Muzzle stagger | 50 | Heavy butt | A point-blank shot just after a whip lands its impulse as stagger on the target, over the target's mass: a heavy bore shoves hard, a small one barely. Price: you have to be on top of them; spent until you land a blow. *(excludes Bayonet drill, Brand; respec to swap.)* |
| Bayonet drill | 60 | Heavy butt | With a bayonet lug fitted the swing is a thrust that rides the gun's own cycle, hitting by the bayonet metal's hardness. Price: the lug is a part you must build and carry. *(excludes Muzzle stagger, Brand; respec to swap.)* |
| Bowling | 60 | Throw | Throw upgrade: the thrown body flies as a ball for 25 ticks; everything it strikes is staggered by its mass (body plus armour), and it takes the knocks too. Heavier bodies stagger harder. Price: the body is hurt by each strike. |
| Spear vault | 60 | Whirl | A spear thrust landed while sprinting on the ground vaults you up and forward. 2 s cooldown. |
| Punish | 70 | Chamber | Cast: a heavy blow, over twice the weapon's damage, on the nearest thing in reach that is already staggered; nothing standing steady will take it. Rearms on a stagger. Costs 25 stamina. *(once used, rearms on staggering something.)* |
| Spin | 90 | Chamber, Dual draw | Sneak while pistol-whipping to spin: hits every body within 2 blocks, but recovery is 1.5x longer. |
| Hamstring | 60 | Spear vault | Cast: a low cut at what you face, for seven tenths of the weapon's damage, that cripples its legs: slowed hard for four seconds. A fighter that cannot close or flee is a fighter you choose the range against. Costs 25 stamina. |

### Onslaught

Onslaught: relentless rhythm. Fist and kick chains, dual-wield tempo, running gunfire. Pool `onslaught`.

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Jab | 30 |  | Standing on the ground, an unarmed blow reaches a little further. Price: only standing; it is the start of the chain. A move landing soon after the one before it in the chain hits 30% harder. |
| Kick | 20 | Jab | Drive a kick into whatever is in front: a stagger from your mass (armour counts) against the target's. Needs the skill; gear can grant it. Wall Smash and Breach Kick build on it. |
| Lunge | 50 | Jab | A sprinting unarmed blow staggers half again as hard and guards you for a few ticks. Price: you have to be at a run, and it comes after Jab. A move landing soon after the one before it in the chain hits 30% harder. |
| Rising tempo | 50 | Jab | Cast: with empty hands, start a chain: each blow you land within a second and a half of the last is fifteen percent harder than the one before, to eight deep. A pause breaks it. Rearms on a landed blow. Costs 20 stamina. *(once used, rearms on a landed blow.)* |
| Sweep | 50 | Jab | A standing unarmed blow also strikes everything across a wide arc in front of you at part damage. Price: standing still, and it comes after Jab. A move landing soon after the one before it in the chain hits 30% harder. |
| Walking fire | 50 | Jab | Cast: three rounds from the held gun, each laid on the next target in front of you, so one press sweeps a room. Every round heats the barrel and spends ammo as usual. Costs 30 stamina. |
| Blade flourish | 60 | Jab | A landed blade hit takes 10 ticks off the roll cooldown (parry and kick wait on it too). |
| Cadence | 40 | Rising tempo | At the Smithing Table, every three clean blows in a row raise your tempo one step (a pip lights under the beat), and each blow struck at a step lands eight percent more Work, filling what lighter blows left short. A miss drops the tempo to nothing. Clean streaks also earn Onslaught points, fewer for each repeat of the same shape. |
| Deep cadence | 40 | Cadence | Your tempo climbs two steps higher. |
| Steady cadence | 50 | Deep cadence | A loose blow holds your tempo instead of dropping it, once per run. |
| Running bolt | 30 | Walking fire | An open-bolt gun kept firing has 20% less spread and kick |
| Twin reload | 30 | Walking fire | The extra reload time of a gun in each hand is 50% shorter |
| Wall force | 40 | Kick | A kick that pins a foe to a wall staggers +50% more |
| Feint | 50 | Blade flourish | With a one-handed weapon in each hand, an off-hand strike pressed while your main hand is mid-swing cuts in at once at full strength and the target flinches. Price: spent until you land a blow. *(once used, rearms on a landed blow.)* |
| Breach kick | 60 | Kick | A kick on a target already staggered knocks it down; on a steady one it only shoves. Kick what is already broken. |
| Chop | 60 | Lunge | A falling unarmed blow drives the target down. Price: you have to be falling; it follows a Lunge. A move landing soon after the one before it in the chain hits 30% harder. |
| Dropkick | 60 | Kick | Cast, in the air: dive forward and down; the landing is a kick on whatever is under you, by your mass (armour counts) over its. Costs 25 stamina. |
| Pummel | 60 | Lunge | Cast, empty-handed: six quick blows on whoever you face, the last one a stagger. Costs 30 stamina. |
| Snap volley | 60 | Walking fire | Cast: up to three rounds, four ticks apart, each at the next nearest living thing in the forward cone. The gun's own cycle may refuse a follow-up; each round spends ammo and adds heat. Costs 30 stamina. *(once used, rearms on a timer.)* |
| Swing | 60 | Rising tempo | Cast: fix a line to the block you look at, four to twenty-four blocks away, and pendulum across the gap on it; crouch to let go. Reads the rope and your momentum. Costs 20 stamina. *(once used, rearms on a timer.)* |
| Frenzy | 70 | Rising tempo | Cast: with empty hands, swing eighty percent faster for five seconds; afterwards three seconds of swinging a third slower, winded. Costs 30 stamina. *(once used, rearms on a timer.)* |
| Catch breath | 30 | Pummel | Every timer rearm takes 1 seconds less |
| Hard kick | 30 | Breach kick | A kick that pins a foe to a wall staggers +25% more |
| Spring heel | 30 | Dropkick | Every timer rearm takes 0.5 seconds less |
| Draw switch | 40 | Feint | Swapping to a weapon of a different build family gives you the swing back at once instead of restarting the recharge. Price: you carry two kinds of weapon, and it rearms on the clock. *(once used, rearms on a timer.)* |
| Axe kick | 60 | Breach kick | Cast: an overhead heel on the nearest thing in front; a raised guard is broken and its bearer staggered by your mass. Costs 25 stamina. *(once used, rearms on a timer.)* |
| Haymaker | 60 | Chop | Cast: wind up for half a second, then one punch for nine damage that shoves a body back by your mass over its. A miss leaves you staggered. Costs 30 stamina. *(once used, rearms on a timer.)* |
| Uppercut | 70 | Chop | A rising unarmed blow launches the target. Price: you have to be rising; it follows a Chop. A move landing soon after the one before it in the chain hits 30% harder. |
| Wall crush | 30 | Hard kick | A kick that pins a foe to a wall staggers +25% more |

### Observation

Observation: see and reach. Aimed shots, coins, weapon reach, the grapple. Pool `observation`.

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Called shot | 70 |  | Cast: the round is aimed not at where the thing under your crosshair is but at where its motion puts it by the time the round arrives. Needs line of sight, forty blocks at most. Costs a round, its heat and 25 stamina. *(once used, rearms on a timer.)* |
| Close work | 30 | Called shot | Blows landing inside half your reach recharge the swing faster. Price: at the tip you get nothing. Excludes Far end. *(excludes Far end; respec to swap.)* |
| Far end | 30 | Called shot | Blows landing out at the tip of your reach hit harder, more the more head-heavy the weapon. Price: up close you get nothing. Excludes Close work. *(excludes Close work; respec to swap.)* |
| Coin flip | 60 | Called shot | Cast: toss a coin and shoot it. A harder coin than the round rebounds it to the nearest thing within 24 blocks that has a clear line from the coin, seen or not; a softer one is punched through. Heavy coins slow the round. Price: a stamped coin, 15 stamina, and it rearms when you land a blow, or after 30 s out of a fight. *(once used, rearms on a landed blow.)* |
| Recoil vault | 60 | Called shot | A shot fired airborne or against a surface pushes you by the gun's impulse over your mass: a rifle launches you, a pocket pistol nudges. Price: a hot or worn gun wears faster when used as a thruster. |
| Dead eye | 70 | Called shot | Cast: for five seconds, your next shot leaves exact, with no spread, and its round counts as twice as hard against armour. Rearms when you land a blow. Costs 25 stamina. *(once used, rearms on a landed blow.)* |
| Light foot | 30 | Recoil vault | +8% speed with nothing worn, falling to nothing by forty Mu of armour: a bare or light-clad scout is quickest. |
| Running start | 40 | Light foot | A straight sprint builds speed, up to a quarter faster after three seconds. A hard turn or a stop lets it go. |
| Fleet | 60 | Running start | Cast: a burst of speed for two and a half seconds with a trail of dust. Bare it is sixty percent faster; armour worn takes from the gain, down to a fifth of it in full plate. Costs 20 stamina. *(once used, rearms on a landed blow.)* |
| Steady walk | 35 | Light foot | Walking adds half the spread to a shot that it otherwise would. |
| Quick draw | 30 | Steady walk | Every reload beat takes a fifth less time. |
| Fine eye | 40 | Quarry mark | Every instrument you carry reads one step finer: its step halves, so a thermometer that read to 25 Tu reads to 12.5. The instrument signs the reading as before. |
| Sharper eye | 60 | Fine eye | A second halving of every carried instrument's step. A reading never goes finer than the exact figure the world holds. |
| Patient hand | 30 | Dead eye | Every timer rearm takes 1 seconds less |
| Keen eye | 35 | Far end | Parry window +1 ticks |
| Second mark | 60 | Coin flip | The coin cast may be used twice: a second coin goes up while the first is still in the air, with no rearm of its own. A rebound goes into the other coin first, so you can shoot a coin into a coin and on to a target. Price: a second stamped coin and a second 15 stamina, and the shot has to be fast. *(excludes Round the corner; respec to swap.)* |
| Round the corner | 70 | Coin flip | The rebound looks three times as far for a target. Price: it still needs a clear line from the coin, and it gives up the second press. *(excludes Second mark; respec to swap.)* |
| Mark and fire | 90 | Dead eye | Held cast, with a hammer-and-cylinder gun (a revolver): hold the key and your sights on things, and each beat marks one more of the rounds chambered, a thing can take several. Let go, fill the cylinder or run dry of stamina, and the hammer lets go: you swing round to each mark in turn and put an exact shot in it, two ticks apart. You walk slowly while laying it and have six seconds. Every round still heats the barrel and spends ammo. Costs 10 stamina, then 4 a second while held. *(does nothing without a weapon with the `fan` trait.)* |
| Fast reel | 25 | Keen eye | [Grapple](Grapple.md) reels +20% faster |
| Winch | 45 | Fast reel | [Grapple](Grapple.md) reels +30% faster |
| Reel in | 50 | Fast reel | Cast: haul the nearest thing within twelve blocks to you on a line; it arrives staggered by your mass (faster reel nodes hit harder). A body over two and a half times your mass will not come. Costs 20 stamina. *(once used, rearms on a timer.)* |
| Vault | 60 | Fast reel | Cast: spring off the shoulders of the nearest thing in front and land behind it; it is staggered by your mass pushed off it. Costs 25 stamina. |

### Proficiency

Proficiency: survive. Healing, breath, heat, getting out of trouble. Pool `proficiency`.

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Field dressing | 25 |  | Held cast: stand still and hold the key to bind your wounds. Health comes back while you hold it, and stamina drains 25 a second, so a full bar buys about four hearts. Moving, taking damage, full health or an empty bar ends it: it is for between fights. Rearms on the clock. *(once used, rearms on a timer.)* |
| Blood up | 35 | Field dressing | Three empty-hand hits, each within three seconds of the last, with no damage taken between, heal one heart. Taking a hit breaks the run. Rearms on the clock. *(once used, rearms on a timer.)* |
| Second breath | 40 | Field dressing | Cast: a deep breath that refills stamina to sixty and pauses its regen, as a rest does. Worn armour presses on the chest: heavy plate breathes to about forty. Costs no stamina; rearms when you land a blow. *(once used, rearms on a landed blow.)* |
| Hot clear | 50 | Field dressing | Sneak-reload with the barrel near cook-off ejects the chambered round unfired. Price: it only helps where cook-off is a danger. |
| Last stand | 60 | Field dressing | Taking a hit that leaves you at 3 hearts or less makes the next roll that swallows a blow a free parry: the attacker is staggered and your riposte opens. Price: it takes a hit to arm, and it rearms only by the clock. *(once used, rearms on a timer.)* |
| Quick hands | 25 | Second breath | Every timer rearm takes 0.75 seconds less |
| Callused hands | 30 | Hot clear | Bare hands take the scald band: anything under 300 Tu can be held or touched unhurt. Burning and searing still hurt; gloves and tongs still do better. |
| Shake off | 40 | Last stand | Cast: a hard full-body shake. If you are burning it beats the fire out; otherwise it clears the harmful effect with the most time left. One thing per cast. Rearms when you stagger something. *(once used, rearms on staggering something.)* |
| Breather | 45 | Blood up | A kill with a modular weapon in your main hand gives back stamina by the weapon's mass: 10 a Mu, between 10 and 40. A heavy weapon earns a deep breath. |
| Grit teeth | 50 | Last stand | Cast: set your body for four seconds. Every blow lands lighter by a share read off the armour you wear: 15% bare, up to 55% in full plate. You are rooted, three tenths slower, while it lasts. Rearms on the clock. *(once used, rearms on a timer.)* |
| Extra i-frames | 30 | Grit teeth | Roll invulnerability +2 ticks |
| Tend | 30 | Field dressing | Held cast: stand still within two blocks of a hurt ally and hold the key to bind their wounds. They mend as fast as Field dressing mends you, and stamina drains from you, 25 a second. Either of you moving, either taking damage, the ally at full health or an empty bar ends it. *(once used, rearms on a timer.)* |
| Haul out | 40 | Tend | Cast: put a line on an ally within twelve blocks and haul them to your feet, setting them down steady. A body over two and a half times your mass will not come. Costs 20 stamina. *(once used, rearms on a timer.)* |
| Hand off | 35 | Tend | Cast: pass stamina to an ally within three blocks. You spend 30; 60% of it reaches them, the rest is lost in the handing over. *(once used, rearms on a timer.)* |
| Fume ward | 35 | Hot clear | A fume dose (soldering, the reflow oven) gives 30% less poison. |
| Slow dose | 40 | Fume ward | The radiation dose you carry clears half again as fast, so the illness steps (hunger, weakness and mining fatigue, wither) end sooner. |
| Iron stomach | 45 | Fume ward | Eating or biting risky food (live organism, toxin, char) gives 40% less of its Hunger, Poison and Nausea. A bite still tells you which class the food is in. |
| Cut loose | 45 | Break free | Cast: cut an ally within five blocks free of every rope someone else has on them (a grab, a hooked weapon) and break the grab on them, staggering the holder by their mass. Costs 20 stamina. *(once used, rearms on a timer.)* |
| Adrenaline | 40 | Quick hands | Stamina regenerates +100% faster while under 30 |
| Break free | 40 | Shake off | Shake off first throws off any rope someone else has on you (a grab, a hooked weapon) and staggers whoever held it, by their mass. Your own grapple is left alone. |
| Steady lungs | 40 | Quick hands | Every timer rearm takes 2 seconds less |
| Cauterize | 60 | Callused hands | Cast: press a hot piece from either hand to the wound. A piece hot enough to burn heals three hearts, one hot enough to sear heals six, and you take that band's burn and catch fire. The piece comes away 200 Tu cooler. Needs something at least burning hot in hand. *(once used, rearms on a timer.)* |
| Sear shut | 50 | Cauterize | A Cauterize with a piece hot enough to sear also clears the harmful effect with the most time left. Price: the sear's own burn and fire. |
| Wind back | 50 | Adrenaline | A parry landed while your stamina is under 30 gives 15 back, without pausing regen. Price: you have to be low and still parry; rearms on the clock. *(once used, rearms on a timer.)* |

### Chaos

Chaos: leave targets with what you did to them, and let it spread. Worn armour stays worn until repaired, a stripped weapon stays stripped, fire passes to the next body, a shoved body knocks into another. Limbs: breakage (Rend, Rending heel, Pry open, Sunder), fire (Searing strike, Forge slam, Kindle, Scorch), scatter (Slide, Barge, Stampede), and carrier rounds from the gun, further down the tree (Muzzle flash, Pin down, Powder burn, Disarm shot, Barbed shaft). Pool `chaos`.

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Rend | 60 |  | Cast: a short blow that grinds the target's worn armour. Each piece loses durability by your weapon's hardness against that piece's, and your weapon is worn in turn by the hardest piece struck. Nothing worn, nothing happens. Costs 25 stamina. |
| Rending heel | 45 | Rend | Rider: Axe kick also wears the target's armour, a heel's hardness against each piece's. |
| Pry open | 60 | Rend | A blow landing on a target whose weapon was just beaten (staggered, or guard up) binds that hand; a raised shield is disabled instead. Softer, more worn steel binds longer. Price: only on an opening, and spent until you parry. |
| Careful hands | 40 | Pry open | Taking a worn tool apart (or swapping or removing a part) returns its wearing part as if a quarter less worn, so more of it comes back. Dismantling a worn tool pays a small trickle of Chaos points, once per kind of tool. |
| Clean strip | 50 | Careful hands | A further quarter off the wear a part holds when a tool is taken apart. |
| Tamped charge | 45 | Careful hands | A demolition charge you light blasts at seven tenths of its power: a tighter radius, as the pack and casing allow. A charge going off pays a small trickle of Chaos points, once per place. |
| Sunder | 70 | Pry open | Cast: a heavy blow into the shield or weapon the target holds. A raised shield is split and disabled; a held weapon is worn, and struck from the hand to lie on the ground if yours is half again as hard. Your weapon is worn by theirs. Rearms when you stagger something. Costs 30 stamina. |
| Searing strike | 60 | Rend | Any held item above the burn line sears what a landed blow hits: a blade fresh from the fire, never keyed on what it is. The item cools. Price: a hot hand burns you, and the heat is spent. |
| Forge slam | 60 | Searing strike | Cast: slam a held item above the burn line into the ground. A ring of searing goes out by its temperature, and the item gives up heat to it, more the more it burns. A forge-fresh blade is a bomb for one swing. Costs 30 stamina. |
| Kindle | 55 | Searing strike | Cast: touch a held item above the burn line to the ground ahead. Every flammable block and living thing within two blocks catches fire, burning longer the hotter the item, and the fire passes to bodies beside them. The item cools by what it lit and scalds your hand. Rearms on a landed blow. Costs 20 stamina. |
| Scorch | 45 | Kindle | Rider: Kindle and Searing strike leave their targets burning twice as long. |
| Steady barrage | 30 | Scorch | Every timer rearm takes 1.5 seconds less |
| Slide | 50 | Rend | Cast: from a sprint, skid low along the ground; every body in the way is tripped once, staggered by your mass (armour counts). Costs 15 stamina. |
| Barge | 55 | Slide | Cast: a shoulder into the body ahead. It is staggered by your mass (armour counts) and sent along your line, faster the more you outweigh it; whatever it meets is staggered by its own mass at that speed. A body too heavy to move is not shoved. Costs 25 stamina. |
| Stampede | 55 | Barge | Cast: a roar that sends every burning creature within nine blocks bolting away from you at a gallop, carrying their fire into whatever they pass. Rearms when you stagger something. Costs 20 stamina. |
| Muzzle flash | 60 | Barge | Cast: fire the gun as a blast. The round's baked impulse is the stagger of everything in the cone in front, and the flash blinds it. A heavy bore staggers a lot, a light one barely. Costs 30 stamina. |
| Dazzle | 40 | Muzzle flash | Rider: Muzzle flash blinds for six seconds. |
| Concussive | 50 | Muzzle flash | Rider: Muzzle flash staggers half again as hard. |
| Pin down | 60 | Muzzle flash | Always on, with a gun: any round you fire that passes close to a living thing slows it hard for two seconds, hit or not. The crack of a near miss is what pins them; area denial from a gun that is not trying to hit. |
| Powder burn | 50 | Muzzle flash | A close shot leaves a powder burn on the target for ten seconds, up to five stacked; your next melee blow spends them as damage. Price: you have to get close, and the burn fades. |
| Disarm shot | 60 | Powder burn | Cast: a round aimed at the weapon hand of whoever you face within twenty blocks; if it leaves the muzzle, what they held falls at their feet and they flinch by the round's impulse. Costs a round and 25 stamina. |
| Barbed shaft | 45 | Powder burn | A close bow or crossbow hit leaves a barb in the target for ten seconds; your next melee blow spends the barbs as damage, up to five. Twin of Marked rounds. Price: you have to get close, and the barbs work loose. |

## Respeccing

An **Unlearning Token** (paper, an ink sac and an iron nugget, shapeless) un-buys every node and gives the points back. It isn't used up if you hadn't learned anything.

## Skills from gear

A worn curio can **grant** a technique directly, as if you'd bought it. The [Feedbacker](Feedbacker.md) grants its projectile punch this way.

## Datapacks

Trees are datapack files (`data/<namespace>/skill_tree/*.json`): a tree name and a list of nodes, each with an id, a cost, the nodes it requires and either a `figure` and `amount` or an `unlock` id; optional `rearm`, `exclusive`, `branch` and `needs_trait`. A pack can add a tree.

## Commands

`/feedback skill grant <tree> <points>` (operators) gives points to test with. Trees: `anchor`, `veil`, `brute`, `onslaught`, `observation`, `proficiency`, `chaos`.
