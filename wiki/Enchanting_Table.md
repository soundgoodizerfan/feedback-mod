# Enchanting Table

## What vanilla does

You put an item and some lapis lazuli in the table, spent experience levels, and picked one of three offered enchantments. The offers were rolled: which enchantments and how strong came from the level you spent and a hidden roll, and bookshelves around the table nudged the roll up. The enchantment sat on the whole item.

## What Feedback does

Feedback **replaces** the Enchanting Table with its own. It is crafted the same way (a Book, two Diamonds and four Obsidian) and looks the same, with the same low shape (three quarters of a block high). The vanilla table has no recipe. Any vanilla table that appears in the world, in a village or a generated structure or placed by command, turns into Feedback's the next tick.

Nothing is rolled and there are no bookshelves. An enchantment is called an **attunement**: a coupling laid onto a single **part**, a pickaxe head, a handle, a chestplate's plate, by writing its name into the part with a pen of light. What it does depends on **which part it sits in** and **what the part is made of**. Some pairings are bad on purpose: Heat on a sword blade sets what you hit alight, but on a handle it sets *you* alight.

## The screen

Put one part in the slot: a shaped part, or raw stock that some tool takes as a part (a rod, a grip, a bone). Any part can be attuned, hot or cold. A part you have just heated in a [furnace](Furnaces.md) or the [Hearth](Hearth.md) and carried over costs less experience, but it cools while you write and the window closes; a cold part costs full price and takes as long as it needs. The table doesn't heat anything.

Pick an effect from the list on the left. Each row shows what the effect does on the part's role in its tooltip; a dimmed row does nothing on this part, and a gold pip marks an effect the part already has. A row of pips under the part's name shows how many attunements it can hold, filled for those it has. Then press **Begin**. Attuning costs **30 experience points** for a cold part (about three levels from the start), and down to 12 for a part at its attuning point or hotter, spent whatever the result. A Creative XP source touching the table pays instead, and Creative players pay nothing. The part stays on the table's pad when you close the screen, cooling on its own while it sits, and drops when the table is broken. Take it out of the slot to carry it away.

Some effects name a potion as a reagent (their tooltip says which: Heat takes a potion of fire, Ward a potion of resistance). A bottle of that potion in your inventory is burned with the session, whatever the result, and the empty bottle returns. A fresh potion saves up to half of the experience cost; a faded one saves less.

Four lamps under the pad say what stands between you and Begin: a part seated, whether the part is hot (lit: cheaper but timed; dark: full price, no rush), an effect picked with room for it, and experience to pay (from an [XP run](Experience.md#drawing-from-a-run) touching the table if it stands high enough, else from you). Hover a lamp to read what it means.

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

Flaws make a part weaker, as with [forging](Hammer.md). Some effects also add a flaw per level, or take one away, as part of what they do.

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
| **Edge** | head or blade: cuts deeper (brittle). On the head of a [Mechanical Hammer](Mechanical_Hammer.md) or [Mechanical Press](Mechanical_Press.md): a harder blow | **Spread**: head or blade turns damage into stagger; plate deflects projectiles |
| **Ward** | plate resists fracture; elsewhere, and on hammer and press heads and the parts of a motor, dynamo, battery or engine, turns away some of each use's wear | none |
| **Bane** | cuts deeper into undead and arthropods; on armor blunts their blows | **Kin**: undead struck lose interest, but hit harder; armor hides you from them |
| **Hold** | handle clamps to the hand, so it can't be dropped | **Free**: lining makes crouching less slow; handle may slip but gives under force |
| **Impulse** | head throws targets back; handle recoils on you; plate returns part of each blow | **Pull**: head draws targets in; plate soaks explosions and falls |
| **Momentum** | hits harder the longer you ran first; legs run faster the longer you run | **Stance**: hits harder the longer you stood still; armor takes less damage |
| **Pack** | hits harder, and armor turns away more, for each ally nearby | **Lone**: the same when no ally is near |
| **Mark** | marks the target; the next blow from anyone does extra | **Decoy**: armor draws hostile mobs onto you (brittle) |
| **Mend** | repairs itself, paid for in your experience | **Tithe**: pays out experience on kills, wearing a little per kill |
| **Yield** | more from what it breaks or kills; armor adds to experience orbs | **Intact**: breaks blocks out whole |
| **Hunger** | restores food on a kill, costs hunger per blow; armor cuts hunger burn | **Famine**: drains food from a player hit, weakens a mob |
| **Echo** | repeats each blow a beat later at a share of damage | **Lag**: each swing lands a few seconds after it is struck; armor delays part of each hit |
| **Charge** | blows build charge, released on a sneak-use blow; too much arcs through you unless the handle insulates | **Drain**: handle bleeds charge, never arcs you |
| **Grudge** | damage you take is stored as charge | **Calm**: it bleeds charge away |
| **Reservoir** | holds experience points: 60 per level of the effect, at a level set by the part's material (sneak-use in the air moves up to 20 points from whoever stands at the higher level to whoever stands at the lower; a Creative XP source fills it up to the source's level; seated in a Crystal Holder it joins the [XP network](Experience.md)) | none |
| **Bloom** | a swing at the ground grows crops and grass; boots do it as you walk | **Reap**: harvests and replants ripe crops |
| **Tide** | head puts out fire and hurts blazes; boots cool lava to obsidian | **Slip**: lining seals out water; plate cuts wading drag; head works under water |
| **Conduct** | head draws lightning in a storm and calls a bolt on what it hits | **Ground**: plate sheds lightning's damage and fire |
| **Lode** | head sends a kill's drops flying to you; armor pulls loose items to you (dead above the Curie point) | **Scatter**: head flings a kill's drops away; armor turns aside metal-tipped arrows |
| **Piezo** | strikes charge a Leyden jar by hand, up to a fifth of what it holds; plate arcs a hit onto the attacker, at a band that rises with the hit | **Gauge**: tells you how hard each blow landed; plate turns away tool wear |
| **Spring** | limbs and prod store more draw energy; boots launch you on a jump | **Snap**: the string lets go sooner |
| **Recall** | a magnetic head pulls the weapon back to the thrower; armor snaps you back a few seconds | **Anchor**: a thrown weapon plants the target |
| **Cling** | a blow sticks to the target and slows it; boots stop a slide | **Glaze**: a blow knocks further; boots let you skate faster |
| **Gleam** | carries a light that moves with you | **Murk**: distant hostile mobs don't target you in low light |
| **Ring** | a blow rings, so sculk and hostile mobs notice it | **Hush**: no vibration for sculk to feel |
| **Bait** | draws passive animals toward you | **Spook**: animals and weak hostiles keep away |
| **Wager** | flips a coin on each blow: double damage, or it heals the target; armor flips on each hit you take | none |
| **Dowse** | a needle orbits the crosshair and points toward the nearest magnetic, dense, conductive or flammable block (four separate effects), in eighths relative to where you face; *"Right over it"* when you stand directly over or under it | **Shun**: it leans away |

Some effects need the material to be right: **Heat** on a handle is only as hot as the material burns, **Lode** and the magnetic **Dowse** die when the part is heated past its Curie point, and **Impulse** on a crossbow's stock recoils the shooter. **Impulse** on a gun's grip locks it in the hand against hard recoil and throws the shooter back instead (see [Guns](Guns.md#recoil)).

## On a machine

A machine's parts are attuned at the table like a tool's, and a built machine's tooltip lists them. Each part has a place in the machine: a **working face** (what does the work: a head, rotor, impeller, winding), a **housing** (shells, cases, linings) or a **mount** (frames, bearings, shafts). The table lists an effect on a part only where the machine does something with it.

### Wear and repair

- **Ward** slows the wear that misuse causes, on every part named on the machine's page. About 1 in (levels + 1) of the wear gets through: one level halves it, two levels leave a third. It turns away the wear of a [Pressure Vessel](Pressure_Vessel.md), [Boiler](Boiler.md) or [Gas Tank](Gas_Tank.md) held past its danger line, of a [Pump](Pump.md) running dry or against a closed line, of a [Water Wheel](Water_Wheel.md) held still in flowing water, of a [Flywheel](Flywheel.md) past its rim speed, of a [Transmission](Transmission.md) shifted at speed, of a [Propeller](Propeller.md) past its blade-tip limit, of a [Conveyor Belt](Conveyor_Belt.md) carrying hot cargo, of a [Mechanical Hammer](Mechanical_Hammer.md) or [Mechanical Press](Mechanical_Press.md) stroke that achieves nothing, and the overload, knock, surge and thermal-shock wear of a [Dynamotor](Dynamotor.md), [Battery](Battery.md), [Engine](Engine.md), [Gas Compressor](Gas_Compressor.md), [Heat Exchanger](Heat_Exchanger.md), [Electromagnet](Electromagnet.md) and [Lamp](Lamp.md).
- **Mend** closes a machine's wear on any part Ward takes. Each step costs 1 experience point, about every 2 seconds, paid first from a reserve housing on the machine (see Reservoir below), then from the nearest player within 6 blocks who has experience. With nobody near and no reserve, the machine stays worn.
- **Tithe** (Mend's mirror) on a vessel, boiler or tank shell or its reinforcing bands pays out experience orbs for the wear the shell takes.
- A worn pressure vessel, boiler or gas tank shell bursts at a lower pressure: a shell worn to its floor lets go at 60% of its rated pressure. Shell wear only accrues above the danger line.
- **Slip** (Hold's mirror) on a frame turns some misuse wear into slipping, at the price of torque.

### Working parts

| Effect | In a machine | Parts |
| --- | --- | --- |
| **Edge** | The face moves more per turn and takes more to turn; a saw or plasma head cuts faster; a hammer or press head hits 25% harder per level | Pump Impeller, Compressor Rotor, Cutting Head, hammer and press heads |
| **Spread** | The same reversed: less per turn, less to turn, a gentler stroke, a slower cut | the same parts |
| **Heat** | The face runs hotter and loses more of its power as heat | Compressor Rotor, Core, Machine Winding |
| **Cold** | The face runs colder and loses less as heat; a lagging or housing sheds less of its heat | the same, plus Lagging and Housing |
| **Charge** | A battery, jar or galvanic cell holds more charge | Plates, Case, Foil, Jar Glass, Outer Sleeve, Cell Anode |
| **Conduct** | A winding loses less to resistance; a jar's foil carries a harder discharge | Machine Winding, Foil |
| **Impulse** | A nozzle or water wheel pushes harder: more thrust, more push from the flow | Thruster Nozzle, Wheel Rim |
| **Pull** | A pump lifts higher; on a vessel, boiler or tank shell a burst blows less hard | Pump Impeller; Vessel Shell, Boiler Shell, Tank Shell, Reinforcing Bands |
| **Momentum** | A flywheel or water wheel carries more momentum | Flywheel Disc, Crank Flywheel, Wheel Rim |
| **Stance** | A flywheel's rim holds to a higher speed before it lets go | Flywheel Disc |
| **Hold** | A flywheel bearing holds its rim to a higher speed; a frame carries more torque | Flywheel Bearing, Frame |
| **Cling** | A belt's cleat grips cargo harder | Cleat |
| **Glaze** | A flywheel drags less in the air and on its bearings | Flywheel Disc |
| **Tide** | A compressor rotor runs cooler | Compressor Rotor |
| **Slip** | A pump drives through liquid with less load; a jar leaks less charge | Pump Impeller; Jar Glass, Outer Sleeve |
| **Gleam** | A lamp filament gives its light on less current | Lamp Filament |
| **Murk** | The filament runs cooler and burns out later | Lamp Filament |
| **Lode** | An electromagnet's core reaches further | Magnet Core |
| **Scatter** | The core throws the magnetic items it would draw, and reaches further | Magnet Core |
| **Spring** | A thermal store's wall holds more heat | Store Core |
| **Snap** | The wall gives its heat up sooner | Store Core |
| **Echo** | Each stroke of a hammer or press repeats a beat later at a share of its strength | hammer and press heads |
| **Lag** | Each stroke lands one stroke late; a stroke held too long is lost | the same heads |
| **Wager** | Each stroke is double or does nothing | the same heads |
| **Pack** / **Lone** | A stroke hits harder for each other attuned machine within a few blocks (Pack) or when none stands near (Lone) | the same heads |
| **Piezo** | Each stroke of a seated head, and each knock beat of an engine, charges a Leyden jar or battery beside the machine a little | the same heads, Pistons |
| **Gauge** | Tells whoever stands near, in words, how hard each stroke landed | the same heads, Pistons |
| **Yield** | A pivot mast head with Fortune doubles the loot of the blocks it breaks, one with Intact breaks them out whole | [Pivot Mast](Pivot_Mast.md) head |
| **Recall** | A seated head goes to whoever breaks the machine, or the nearest player when it is blown up, instead of falling loose | hammer, press and mast heads |
| **Hunger** | A working machine feeds the saturation of players within a few blocks | seated heads |
| **Famine** | A working machine takes strength from players within a few blocks and weakens hostile mobs there | seated heads |

### Around the machine

These act on the world near a running machine, every 2 seconds or so. A machine that wears acts only while it runs; Mend, Reservoir and Grudge work either way.

| Effect | In a machine | Parts |
| --- | --- | --- |
| **Bait** / **Spook** | Animals nearby walk toward the machine (Bait) or keep away (Spook) | Frame, Bearing, Crankshaft, Flywheel Bearing, Crank Arm, Rim Ring |
| **Bloom** / **Reap** | Crops beside the machine take a bone-meal step (Bloom), or ripe ones are harvested and replanted (Reap) | Pump Impeller, Wheel Rim |
| **Bane** | Undead and arthropods within a few blocks are cut | Cutting Head, Flywheel Disc, Pistons, Wheel Rim |
| **Kin** | Undead within a few blocks lose interest | the same parts |
| **Mark** | Hostile mobs within a few blocks are marked | the same parts |
| **Ring** | A vibration every few seconds that sculk and wandering hostile mobs notice | Flywheel Disc, Pistons, Pump Impeller, Compressor Rotor |
| **Hush** | Silences the vibrations made near the machine | the same parts |
| **Dowse** | The machine's Jade tooltip carries the needle, in compass words, toward (or away from, with Shun) the nearest magnetic, dense, conductive or flammable block | Frame, Bearing, Crankshaft, Flywheel Bearing, Crank Arm, Rim Ring |
| **Ground** | A lightning strike within a few blocks of the machine is grounded: no harm, no fire | Housing, Case, Gearbox Case, Outer Sleeve, Yoke |
| **Spring** | Whatever falls onto the machine takes no fall damage and is thrown back up, higher the further it fell | Housing, Case, Volute, Gearbox Case |
| **Reservoir** | Holds experience points: orbs drifting near are drawn in, Tithe fills it first, and Mend draws on it before any player | Housing, Case, Volute, Engine Block, vessel, boiler and tank shells |
| **Grudge** | A blast that would break the housing is taken, with a chance per level, and the damage kept, then given back to the nearest hostile creature within a few blocks | Housing, Case, Volute, Engine Block, the three shells, Gearbox Case, Outer Sleeve |
| **Calm** | The same blast is taken and shed | the same parts |

Heads named here are the heads seated in a Mechanical Hammer, Mechanical Press, [Power Cutter](Power_Cutter.md) (a cutting head) or Pivot Mast. Which effects a given part takes is shown on its row in the effect list at the table.

## On a gun

Every bullet that hits carries the attunements of the gun's parts, in the part's role:

| Part | Effects |
|---|---|
| **Barrel**, second barrel | **Edge** (harder hits; mirror **Spread**), **Bane** (mirror **Kin**), **Heat** (sets the target alight; mirror **Cold**), **Impulse** (knocks the target back; mirror **Pull**), **Yield** (mirror **Intact**), **Hunger** (mirror **Famine**), **Cling** (mirror **Glaze**), **Echo** (mirror **Lag**) |
| **Sight**, lit reticle | **Mark** (the next hit on the target does more), **Pack** (mirror **Lone**) |
| **Trigger** | **Wager**: each shot is double damage or heals the target |
| **Muzzle brake**, suppressor | **Ring** (the shot is heard; mirror **Hush**) |
| **Grip**, stock | the handle effects above (**Impulse**, **Hold**, **Reservoir**, **Heat** burning you) |
| **Any part** | **Ward** turns away part of the bore's wear (each level leaves one share in levels + 1); **Mend** drinks experience to close the bore's wear; its mirror **Tithe** pays experience per kill and wears the bore |

## The effect is on the part

Attunements live on the **part**, not the tool. Set an attuned part into a tool and it carries across. **Reheating a part past its attuning point erases its attunements**, so keep the part out of a furnace, crucible or Hearth once it is attuned. A finished tool heated past a part's attuning point loses that part's attunements the same way, judged against each part's own material.

Vanilla gear converted into modular gear keeps the enchantments it already had.

## Knowing an effect

You start knowing five effects: **Edge, Ward, Mend, Yield and Hold**. Every other effect shows in the list as **???**, with no description, until you learn it. Three ways to learn one:

- **Read a book.** Right-click a vanilla enchanted book (a cleric's, or loot). For each enchantment in it that an effect names, you learn that effect and the book is used up. A book that teaches nothing new is kept and says so. Which enchantment teaches which effect:

| Enchantment | Effect |
|---|---|
| Sharpness | Edge |
| Smite, Bane of Arthropods | Bane |
| Fortune, Looting | Yield |
| Protection, Projectile Protection, Blast Protection, Fire Protection, Feather Falling | Ward |
| Unbreaking | Hold |
| Mending | Mend |
| Fire Aspect, Flame | Heat |
| Knockback, Punch | Impulse |
| Loyalty | Recall |
| Lure, Luck of the Sea | Bait |
| Depth Strider, Riptide | Tide |
| Sweeping Edge, Efficiency | Momentum |
| Power, Impaling | Charge |
| Thorns | Grudge |
| Infinity | Reservoir |
| Channeling | Conduct |
| Silk Touch | Lode |

- **Attempt it blind.** You may attempt an effect you do not know, at full price. The pad shows no guide: you must find the word's shape yourself. A clean result teaches you the effect; a flawed or failed one teaches nothing and the experience is spent.
- **Study a part.** Seat a part that already holds the effect and press **Study** (it replaces Begin for an effect the part holds and you do not know). You learn it for nothing.

Knowledge is yours alone and is kept when you die.

## What is left alone

Loot books are unchanged. Clerics sell one enchanted book a day at the [counter](Villagers.md#goods), always one that teaches an effect.
