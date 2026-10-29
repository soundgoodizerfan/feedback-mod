# Enchanting Table

## What vanilla did

You put an item and some lapis lazuli in the table, spent experience levels, and picked one of three offered enchantments. The offers were rolled: which enchantments and how strong came from the level you spent and a hidden roll, and bookshelves around the table nudged the roll up. The enchantment sat on the whole item.

## What Feedback does

Feedback **replaces** the Enchanting Table with its own. It is crafted the same way (a Book, two Diamonds and four Obsidian) and looks the same, but you can't craft the vanilla one any more. Any vanilla table that appears in the world, in a village or a generated structure or placed by command, turns into Feedback's the next tick.

Nothing is rolled and there are no bookshelves. An enchantment is called an **attunement**: a coupling laid onto a single **part**, a pickaxe head, a handle, a chestplate's plate, by playing a short game with its heat. What it does depends on **which part it sits in** and **what the part is made of**. Some pairings are bad on purpose: Heat on a sword blade sets what you hit alight, but on a handle it sets *you* alight.

## The screen

Put one part in the slot: a shaped part, or raw stock that some tool takes as a part (a rod, a grip, a bone). A metal part has to be **hot**: heat it in a [furnace](Furnaces.md) or the [Hearth](Hearth.md) and carry it over, exactly as you do forging stock. The table doesn't heat it, and a part that has already cooled below its lock is refused. Parts of anything that isn't a metal (wood, leather, bone, stone and the like) are worked cold and need no heat at all.

Press the effect button to open a searchable list of effects (each shows what it does on the part's role), choose one, then **Begin**. Attuning costs **30 experience points** (about three levels from the start), spent whatever the result. A Creative XP source touching the table pays instead, and Creative players pay nothing. The part goes back to your inventory when you close the screen.

On the right of the screen are two gauges:

- **Field:** a vertical track. The shaded band is the target you want to land in. If the effect has a mirror there is a second band at the same height below the middle. The bright line is where the part's polarization is now. Hold the left mouse button over the gauge: how far the pointer sits above the middle of the track is how strong a field you apply (below the middle reverses it). Let go and the field is zero, and the polarization **stays where it is**.
- **Heat:** the part cools while you play, at the usual 1.5 Tu a tick. The gold stretch is its **attuning window**. The field only works while the part is inside it. Above it nothing happens yet; below it (the lock) the part locks and the result is judged as it stands. Press **Commit** to stop early. A session is never longer than 20 seconds.

### How the part answers

A field has to be past the material's **coercive field** (around 0.3 of the track) before the polarization moves at all; under that nothing happens. Over it, the polarization drifts toward a value set by how hard you push, with a lag of several ticks, so a strong field gets there sooner but you have less time to stop at the right place. There is always a little jitter, and more of it while the part is hotter. Each material has its own attuning point, lock, coercive field, loop width and lag, so iron and gold play differently: read the gauge before you commit to a plan.

### Starting hot or cold

A part that starts well above its window has a long wait but a forgiving clock, and its signal is noisy. A part that starts already inside the window is cleaner but you have less time. Either works.

## The result

Each effect has a **target** (where the polarization must land, from 0.05 to 1) and a **band** (how far off still counts; narrower is stronger).

| Where it lands | Result |
| --- | --- |
| Within half a band of the target | the part's top level, no flaw |
| Within the band | one level lower |
| Past the band, within one and a half bands | two levels lower, **one flaw** |
| Past that, within two bands | two levels lower, **two flaws** |
| Further, overshooting, or the field never got past the coercive field | **nothing**. The part is never destroyed, but the experience is spent. |

Flaws make a part weaker, as with [forging](Hand_Hammer.md). Some effects also add a flaw per level, or take one away, as part of what they do.

Landing **below zero** is judged against the mirror target and gives the effect's **mirror**: the same coupling reversed (Bane's mirror is Kin, attraction to the undead). An effect with no mirror takes nothing there.

### How strong and how many

Both come from the material's **enchantability** (shown on the part's figures):

- **Top level:** 1 + enchantability ÷ 8, up to level 3.
- **Slots:** one effect per 10 enchantability, rounded up, at least one.

| Material | Enchantability | Top level | Effects it holds |
| --- | --- | --- | --- |
| Gold | 22 | 3 | 3 |
| Silver, blaze rod, breeze rod | 20 | 3 | 2 |
| Leather, wood, wool, netherite | 15 | 2 | 2 |
| Wrought iron | 14 | 2 | 2 |
| Iron, bamboo, emerald | 12 | 2 | 2 |
| Diamond, steel, quartz | 10 | 2 | 1 |
| Copper | 8 | 2 | 1 |
| Stone, cast iron | 5 | 1 | 1 |
| Lead | 3 | 1 | 1 |

A later effect is an independent session and doesn't disturb the earlier ones. Attuning an effect the part already has replaces it; a part with no slots left refuses a new one.

## Effects

Each effect does something different on a different **role** (the part's place in the tool), and a role an effect has no entry for does nothing. The tooltip on a finished tool names what each effect does in its role. The **+** side is the first landing; the **mirror** is the same coupling reversed.

| Effect | + side | Mirror |
| --- | --- | --- |
| **Heat** | head or blade: sets what it strikes alight. Handle: burns *you*, as hot as the material burns. Plate: sets whoever hits you alight | **Cold**: head or blade stacks freezing on the target; lining turns fire damage away |
| **Edge** | head or blade: cuts deeper (brittle) | **Spread**: head or blade turns damage into stagger; plate deflects projectiles |
| **Ward** | plate resists fracture; elsewhere turns away some of each use's wear | none |
| **Bane** | cuts deeper into undead and arthropods; on armour blunts their blows | **Kin**: undead struck lose interest, but hit harder; armour hides you from them |
| **Hold** | handle clamps to the hand, so it can't be dropped | **Free**: lining makes crouching less slow; handle may slip but gives under force |
| **Impulse** | head throws targets back; handle recoils on you; plate returns part of each blow | **Pull**: head draws targets in; plate soaks explosions and falls |
| **Momentum** | hits harder the longer you ran first; legs run faster the longer you run | **Stance**: hits harder the longer you stood still; armour takes less damage |
| **Pack** | hits harder, and armour turns away more, for each ally nearby | **Lone**: the same when no ally is near |
| **Mark** | marks the target; the next blow from anyone does extra | **Decoy**: armour draws hostile mobs onto you (brittle) |
| **Mend** | repairs itself, paid for in your experience | **Tithe**: pays out experience on kills, wearing a little per kill |
| **Yield** | more from what it breaks or kills; armour adds to experience orbs | **Intact**: breaks blocks out whole |
| **Hunger** | restores food on a kill, costs hunger per blow; armour cuts hunger burn | **Famine**: drains food from a player hit, weakens a mob |
| **Echo** | repeats each blow a beat later at a share of damage | **Lag**: each swing lands a few seconds after it is struck; armour delays part of each hit |
| **Charge** | blows build charge, released on a sneak-use blow; too much arcs through you unless the handle insulates | **Drain**: handle bleeds charge, never arcs you |
| **Grudge** | damage you take is stored as charge | **Calm**: it bleeds charge away |
| **Reservoir** | holds experience points (sneak-use to store or fill) | none |
| **Bloom** | a swing at the ground grows crops and grass; boots do it as you walk | **Reap**: harvests and replants ripe crops |
| **Tide** | head puts out fire and hurts blazes; boots cool lava to obsidian | **Slip**: lining seals out water; plate cuts wading drag; head works under water |
| **Conduct** | head draws lightning in a storm and calls a bolt on what it hits | **Ground**: plate sheds lightning's damage and fire |
| **Lode** | head draws lightning; armour pulls loose items to you (dead above the Curie point) | **Scatter**: head flings a kill's drops away; armour turns aside metal-tipped arrows |
| **Piezo** | strikes charge a Leyden jar by hand; plate arcs a hit onto the attacker | **Gauge**: tells you how hard each blow landed; plate turns away tool wear |
| **Spring** | limbs and prod store more draw energy; boots launch you on a jump | **Snap**: the string lets go sooner |
| **Recall** | a magnetic head pulls the weapon back to the thrower; armour snaps you back a few seconds | **Anchor**: a thrown weapon plants the target |
| **Cling** | a blow sticks to the target and slows it; boots stop a slide | **Glaze**: a blow knocks further; boots let you skate faster |
| **Gleam** | carries a light that moves with you | **Murk**: distant hostile mobs don't target you in low light |
| **Ring** | a blow rings, so sculk and hostile mobs notice it | **Hush**: no vibration for sculk to feel |
| **Bait** | draws passive animals toward you | **Spook**: animals and weak hostiles keep away |
| **Wager** | flips a coin on each blow: double damage, or it heals the target; armour flips on each hit you take | none |
| **Dowse** | a needle in the action bar leans toward the nearest magnetic, dense, conductive or flammable block (four separate effects) | **Shun**: it leans away |

Some effects need the material to be right: **Heat** on a handle is only as hot as the material burns, **Lode** and the magnetic **Dowse** die when the part is heated past its Curie point, and **Impulse** on a stock recoils the shooter of a gun.

## The effect is on the part

Attunements live on the **part**, not the tool. Set an attuned part into a tool and it carries across. **Reheating a part past its attuning point erases its attunements**, so keep the part out of a furnace, crucible or Hearth once it is attuned. A part already built into a tool is not reached.

Vanilla gear converted into modular gear keeps the enchantments it already had.

## What is left alone

Villager enchanted books and loot books are unchanged.
