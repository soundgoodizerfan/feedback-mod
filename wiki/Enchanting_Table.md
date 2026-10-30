# Enchanting Table

## What vanilla did

You put an item and some lapis lazuli in the table, spent experience levels, and picked one of three offered enchantments. The offers were rolled: which enchantments and how strong came from the level you spent and a hidden roll, and bookshelves around the table nudged the roll up. The enchantment sat on the whole item.

## What Feedback does

Feedback **replaces** the Enchanting Table with its own. It is crafted the same way (a Book, two Diamonds and four Obsidian) and looks the same, but you can't craft the vanilla one any more. Any vanilla table that appears in the world, in a village or a generated structure or placed by command, turns into Feedback's the next tick.

Nothing is rolled and there are no bookshelves. An enchantment is called an **attunement**: a coupling laid onto a single **part**, a pickaxe head, a handle, a chestplate's plate, by writing its name into the part with a pen of light. What it does depends on **which part it sits in** and **what the part is made of**. Some pairings are bad on purpose: Heat on a sword blade sets what you hit alight, but on a handle it sets *you* alight.

## The screen

Put one part in the slot: a shaped part, or raw stock that some tool takes as a part (a rod, a grip, a bone). Any part can be attuned, hot or cold. A part you have just heated in a [furnace](Furnaces.md) or the [Hearth](Hearth.md) and carried over costs less experience, but it cools while you write and the window closes; a cold part costs full price and takes as long as it needs. The table doesn't heat anything.

Pick an effect from the list on the left. Each row shows what the effect does on the part's role in its tooltip; a dimmed row does nothing on this part, and a gold pip marks an effect the part already has. A row of pips under the part's name shows how many attunements it can hold, filled for those it has. Then press **Begin**. Attuning costs **30 experience points** for a cold part (about three levels from the start), and down to 12 for a part at its attuning point or hotter, spent whatever the result. A Creative XP source touching the table pays instead, and Creative players pay nothing. The part goes back to your inventory when you close the screen.

Some effects name a potion as a reagent (their tooltip says which: Heat takes a potion of fire, Ward a potion of resistance). A bottle of that potion in your inventory is burned with the session, whatever the result, and the empty bottle returns. A fresh potion saves up to half of the experience cost; a faded one saves less.

Four lamps under the pad say what stands between you and Begin: a part seated, whether the part is hot (lit: cheaper but timed; dark: full price, no rush), an effect picked with room for it, and experience to pay. Hover a lamp to read what it means.

On the right of the screen is the **pad**, with the part's heat beside it.

- **The pad:** the effect's name is written in the table's script, the same alphabet vanilla's enchanting table floats over its book. One letter shows at a time as a constellation of glowing stars. Hold the left mouse button over the pad and drag the pen through the stars to light them; lift the pen for a moment and the letter is finished, shrinks into the margin (gold when it was written well) and the next letter appears. The pen follows the mouse rather than sitting on it, and the part's material decides how: a stiff stock drags and rounds corners, a hard stock only marks under a firm, fast hand, and a forgiving stock lets a stroke stray and still count. A bar of **ink** at the pad's edge runs down as the pen marks, and with none left the pen marks nothing, so a wandering hand runs dry before the word is done. Effects with a mirror show a **Flip** box: tick it to write the word reversed. The result lands as a stamp, and the written word stays until the next Begin.
- **Heat:** a hot part cools while you write, at the usual 1.5 Tu a tick. The gold stretch is its **attuning window**. The pen only marks while the part is inside it. Above it nothing happens yet; below it (the lock) the part locks and the result is judged as it stands. Press **Commit** to stop early. A session is never longer than 20 seconds.

### How the pen answers

Each material has its own attuning point, lock, coercive field, loop width and lag, so iron and gold write differently. The lag is the pen's inertia. The coercive field is how fast the pen must move to leave a mark. The loop width is how far from a star a mark still counts. There is always a little tremor in the pen, and more of it while the part is hotter. A letter is graded on how many of its stars the marks reach and how much of the ink went near a star, so scribbling over the whole pad lights every star but spoils the letter.

### Hot or cold

A cold part is held: the window never closes, the pen is steady, and it costs the full 30. A hot part is cheaper, but it cools as you write; the hotter it starts, the cheaper it is, and the more the pen shakes at the hot end. When it cools to its lock the session ends and the word is judged as far as you got. Reheating an attuned part past its attuning point erases what you wrote.

## The result

The written word is graded on how well it matches the effect's name, letter by letter. Each effect has a **band** (how far from a perfect word still counts; narrower is stronger).

| How the word reads | Result |
| --- | --- |
| Within half a band of perfect | the part's top level, no flaw |
| Within the band | one level lower |
| Past the band, within one and a half bands | two levels lower, **one flaw** |
| Past that, within two bands | two levels lower, **two flaws** |
| Further, or the pen never marked | **nothing**. The part is never destroyed, but the experience is spent. |

Flaws make a part weaker, as with [forging](Hand_Hammer.md). Some effects also add a flaw per level, or take one away, as part of what they do.

A word written **reversed** (Flip) that fits better than the normal one is judged as the mirror and gives the effect's **mirror**: the same coupling reversed (Bane's mirror is Kin, attraction to the undead). An effect with no mirror takes nothing there.

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
