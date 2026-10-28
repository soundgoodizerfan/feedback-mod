# Tools and Weapons

Pickaxes, axes, swords, bows, shields and the rest are no longer a fixed list of wooden, stone, iron, golden, diamond and netherite. Every one is **assembled from parts**, and what it does comes from what those parts are made of. The recipes for vanilla's tools are gone (a wooden or stone pickaxe, axe and shovel still craft by hand, so you can build the first table, and are converted like any old tool, below); make the parts at a bench (the [Smithing Table](Smithing_Table.md), [Woodworking Table](Woodworking_Table.md) or [Stonecutter](Stonecutter.md)) and put them together in a crafting grid.

Armour works the same way: see [Armour](Armour.md).

## Making the parts

A pickaxe is a **head** on a **handle**, and the head is a part of its own. Shape it first at the bench for its stock: the [Smithing Table](Smithing_Table.md) (metal, with a [Hand Hammer](Hand_Hammer.md)), the [Woodworking Table](Woodworking_Table.md) (wood, bone, leather, fibre, with an axe) or the [Stonecutter](Stonecutter.md) (stone, ceramic, glass, crystal, no tool). Put stock in the cells first; the list then shows only the parts that stock can make. Fill the cells (the **cells** column below); the hammer or axe loses a point. What comes out is one `Pickaxe Head`, whatever it's made of; the material rides on the item.

| Part | Cells (the shape is only a picture) |
| --- | --- |
| **Pickaxe head** | `XXX` |
| **Axe head** | `XX / X_` |
| **Shovel head** | `X_X / _X_` |
| **Hoe head** | `XX` |
| **Sword blade** | `X / X` |
| **Sword blade, claymore** | `XX / X_ / X_` | four ingots: a long, heavy two-hander that hits hard |
| **Sword blade, dagger** | `X_ / _X` | two ingots on a diagonal: short and quick, hits light |
| **Pickaxe head, heavy** | `XXX / _X_` | four ingots: a heavier head that mines a three-by-three |
| **Axe head, bearded** | `XXX / X__` | four ingots: a heavier head that hits a little harder |
| **Cleaver head** | `XX / XX` |
| **Pike head** | `X / X / X` |
| **Maul head** | `XXX / XXX` |
| **Sickle head** | `XX_ / __X` |
| **Trident tines** | `_X_ / X_X` |
| **Tip** | one cell: a gem, shard or nugget |
| **Binding** | one cell: leather, string or wire |
| **Sword guard**, **sword pommel**, **shield rim** | one cell each |
| **Crossbow prod** | two cells: sticks, rods or bone |

Every piece must be the same stock. **Stock** is ingots, gems, planks, cobblestone or bone (tines take ingots, gems or shards). Anything of a material that fits works, so a head can be iron, bronze, gold, diamond, stone, a plank or a bone.

**Steel and other hot-worked metals have to be hot to shape**, exactly as under a [hammer](Hand_Hammer.md); the part comes out as hot as the stock was. Metals without a hot-working window (iron, wood, stone) shape cold.

## Putting one together

Lay the parts in a crafting grid, in any order. Each shaped part is **one item**; **handles** are sticks, rods or bone, stacked as many as the handle is long; all of one part must be the same material. The table works out what you are making from the parts.

| Item | Parts |
| --- | --- |
| **Pickaxe** | head, two handles |
| **Axe** | head, two handles |
| **Shovel** | head, two handles |
| **Hoe** | head, two handles |
| **Sword** | blade, grip |
| **Mace** | a [heavy core](Heavy_Core.md) (or a block of metal), two rods |
| **Trident** | tines, two shafts |
| **Bow** | three limbs, three lengths of string |
| **Crossbow** | a prod (two cells), string, three stock, a lock, **plus a Crossbow Blueprint** (below) |
| **Shield** | six faces, a rim part (a grip makes it five faces) |
| **Shears** | two blades (a pivot is optional) |
| **Twin-headed tool** | two heads of any kind (pick, axe, shovel, hoe), each its own material, one handle: digs what both heads dig. A pick and an axe make a **mattock**, two axes a double-bit axe. A second head slows the swing and weakens the blow, because the handle carries both |
| **Cleaver** | head, grip: a short, heavy axe that bites deeper than a full one |
| **Pike** | head, three handles: reaches further |
| **Maul** | head, two handles: splits wood and staggers what it hits |
| **Sickle** | head, grip: cutting a crop or a leaf takes its neighbours too |

You can add:

- A **binding** part (made from leather, string or wire) to any tool that takes one.
- A sword takes a **guard** and a **pommel**, parts of their own. The pommel is a counterweight.
- A **tip** part, below.
- Armour and shields can take a **grip** or **lining**.

A tip, binding, guard, pommel, rim and crossbow prod are shaped at a bench first, like a head, so a stack of ingots is never ambiguous: the item says what it is.

### Tips

A pickaxe, axe, shovel, hoe, twin-headed tool, cleaver, sickle, pike or sword can have a **tip**: shape one gem, shard or nugget into a **Tip** at a bench, then put it in a spare slot with the other parts (or add it to a finished tool in the tool slot) and the tool is tipped. A **Binding** (one length of string, leather or wire, shaped the same way) goes in the same way. **The tip decides hardness**, so what the edge can dig is the tip's. It bites like the two together, and the body keeps the toughness and most of the wear. A steel pick with a diamond tip digs what diamond digs, chips like steel, and lasts about 1.6× as long as plain steel. A tip no harder than its body does nothing (a gold tip on iron is a pad). **Maces, mauls, tridents, bows, crossbows, shields and shears take no tip.** A tool made without a tip can be given one later at the Tool Table; swapping in a different gem replaces it.

### Brittle heads

A whole diamond head is possible and bites superbly, but diamond is brittle: it keeps only a sixteenth of the durability its hardness suggests. Anything tougher than a fifth of its hardness (iron, stone, steel) is unaffected. Set diamond as a tip instead.

### Crossbow Blueprint

The crossbow is the one tool with a mechanism, so it needs a **Crossbow Blueprint** in the Tool Table's plan slot, made at the [Blueprint Table](Blueprint_Table.md). The table keeps the blueprint. Any blueprint of the type will do; the parts don't have to match it.

## What the parts do

You won't see a stat sheet. Hold the tool and read the words on its tooltip: *head-heavy*, *light in the hand*, *heavy*, *keen*, *blunt*, *brittle*, *soft*, *loose-jointed*, *worn*. A tool is a physical object, and these are the things you'd feel picking it up.

- **A heavier head hits harder and swings slower.** Gold is nearly three times iron's weight; a gold pickaxe is slow and hits hard, but gold is soft.
- **A longer handle reaches further** and puts more strain on the joint.
- **Hardness decides what it can dig.** An edge needs to be at least **3** hardness for iron ore and the stone class, **6** for gold, diamond and redstone ore, and **40** for obsidian and ancient debris. So copper and gold can't mine iron ore, iron can't mine obsidian, and diamond and netherite can. (Stone has a very high number, so a stone pickaxe *can* mine diamond ore. It chips doing it.)
- **How long it lasts** comes from how hard and strong the edge is. Iron lasts about as long as it used to; gold barely lasts; a whole diamond head bites superbly but is [brittle](#brittle-heads), so diamond is better as a tip.
- **Enchanting** is done to a **part**, not the finished tool, at the [Enchanting Table](Enchanting_Table.md). How well a part takes one depends on its material: gold is best, stone is worst.

### Wearing it out the wrong way

A tool wears with use, like any hand tool, but misuse costs extra and tells you which kind:

- **An edge on something much harder than it can take chips.** A diamond pickaxe on obsidian wears quickly, because diamond is hard but brittle. A **keen** edge chips sooner; a **blunt** one is tougher.
- **A head too heavy for its handle works the joint loose.** Netherite or gold on a wooden handle gets *loose-jointed* and loses a bit more each swing. A stronger handle (bone, a rod) or a binding fixes it.
- **A bow's string is the limit.** Stiff limbs on a weak string shoot faster and draw slower, until the string is the weak point. Use wire for a string.

## Edge

An edged tool has an edge: **keen**, **standard** or **blunt**. Keen bites harder and chips sooner. Blunt is tougher.

A new tool comes with the standard edge unless a [Blueprint](Blueprint_Table.md) says otherwise. **Regrind** it at a **grindstone**: sneak and right-click the grindstone with the tool in hand to step to the next edge. Each grind costs **2 points of durability** (never the last), so an edge is a choice, not a toggle.

## When it breaks, mending, and swapping parts

A tool doesn't vanish when it's spent. It stops with one point left and acts like an empty hand until you repair it. It also gets visibly worse on the way down: *Worn*, *Battered*, *Nearly spent*.

**Repairing:** put the tool in the [Tool Table](Tool_Table.md#rework)'s tool slot with the material its wearing part is made of (an iron pickaxe with iron ingots). Each ingot puts back its share.

**Swapping a part:** put the tool in the tool slot with a *different* part, or with stock of a different material (a bronze pickaxe head over an iron pickaxe replaces its head). A new wearing part is fresh. Enchantments stay. You can also **take a tool apart** there and get its parts back.

## Old vanilla tools

Vanilla tools, weapons and armour that turn up (in chests, on mobs, from villagers or in your pockets) are converted on the spot into the modular item they would have been, with the same name, enchantments and wear. An iron pickaxe is an iron head on a wooden handle.

**Diamond is the exception.** A diamond tool becomes a **steel** one with a **diamond tip** ("Diamond-tipped Steel Pickaxe"); diamond armour becomes steel with four diamond **studs**. A whole diamond head would be brittle and poor. The converted pick lasts about half as long as vanilla's diamond did and, as in vanilla, netherite remains the best tool in the game.

## Plating

A tool or armour piece whose main part is metal can hang in an [Electrolytic Cell](Electrolytic_Cell.md#electroplating) and be plated like any metal piece. A good coat adds the coat metal's corrosion resistance (a tool used wet rusts by how little its metal resists water). On **armour** a harder coat also hardens the plate a little. On a **tool** the edge is ground bare, so a coat protects the flats and changes neither bite nor hardness. Re-forging the main part removes the coat.

## Measuring

The tooltip gives words for free. To see a design's numbers *before* you make it, draw it at the [Blueprint Table](Blueprint_Table.md). For numbers on a finished piece, carry a [Balance Scale](Balance_Scale.md) (mass) or a [Scratch File Set](Scratch_Files.md) (hardness).

### Forging variants by rhythm

Some variants are not laid out in cells but **forged**, at the [Smithing Table](Smithing_Table.md#forging): choose the variant in the parts list, put in the stock (the same cells as the standard shape, and hot if it is a hot-worked metal) and press **Forge**. Play the pattern: **tap** on the left lane (left click or Z) and **heavy** on the right (right click or X) as notes fall to a ring. A **metronome** clicks the tempo and counts four beats in before the first note. The window is generous, and each blow flashes Clean, Loose or Miss. A harder metal plays slower.

The stock cools as you play. Notes struck while it is too cold (or still too hot) are dead and count against you. A sloppy run never ruins the part, but it leaves **flaws**: each one takes a share of the edge's toughness and durability (a ruined part starts at half), and the part reads "rough", "flawed" or "ruined" on its tooltip. A clean run leaves none, though even a good player is sometimes a hair off.

**What you can forge:** a sword blade becomes a **rapier** (very long and thin, much the quickest swing, a point that hits light), a **cutlass** (light and quick, hits fairly), a **katana** (thin and long, hits well) or a **machete** (broad, slow and the hardest hitter). An axe head becomes a **hatchet** or a **felling axe**. A pickaxe head becomes a **prospector's** pick (long and sharp, chips easily) or a **miner's** pick (thick and tough). Each has its own rhythm, and the item is named for it ("Iron Rapier"). All of them start from the same ingots as the standard head. A thin blade is ground down, so a rapier is lighter than a machete and swings far quicker.

### How each one fights

- **Pike.** Left click is a jab: long reach, but it cannot hit anything closer than 2 blocks. Hold right click to lower it into a charge: you keep sprinting while it is held out, and it tires and lowers as the charge runs out. While you and a target close at 4.6 blocks a second or faster, each thing in front of you at 2 to 4.5 blocks is struck once for damage that grows with your closing speed, the pike's strike and the weight of its head. From 5.1 blocks a second it knocks them back, and early in the charge it pulls riders off their mounts. The charge tires in three stages (full, then no dismount, then damage only), and a weakly bound pike tires sooner. A fast hit wears the pike more. A pike cannot crit, and Strength and Weakness do not change a charge.
- **Rapier.** Keep hitting the same target without pause and each hit hurts more; getting hit or switching target resets it.
- **Katana.** Wait before the first cut to land a heavy bleeding strike.
- **Machete.** Great against the unarmoured and for clearing brush, poor against armour.
- **Claymore.** Wait for a full swing and it hits everything around your target; mash it and it is weak.
- **Dagger.** Strike from behind or at unaware mobs, especially while sneaking; face to face it is worse than a plain blade.
- **Cutlass.** A kill readies your next swing and speeds you up, and fighting a crowd hits harder.
- **Prospector's pick.** Right click to ping: ore within 12 blocks glows through the stone for 6 seconds. It is slow to recharge (10 seconds) and wears the pick, so ping and then dig.
- **Miner's pick.** Mine one ore and the connected ore of the same kind comes with it, up to 8 more, at a small extra wear each. It will not spend the pick's last point. Sneak to mine a single block.
- **Felling axe.** Break a log and the whole connected tree comes down, up to 24 logs. Each log costs extra durability, and sneaking breaks one log only.
- **Hatchet.** Hold right click to wind up, release to throw. It hits harder the farther it flies (up to 2.5 times), costs 2 durability per throw and lands as an item to pick up.
- **Bearded axe.** Hitting a raised shield disables it for 8 seconds and pulls the target toward you. Against a target with no shield it deals 10% less damage.

### Fighting with them

Melee weapons hit everything in their swept volume, can [parry](Combat.md#parry), and can be swung from the off hand. See [Combat](Combat.md). What a part is made of can also change what a blow does (silver against the undead, a blaze rod setting things alight): see [Material Perks](Material_Perks.md). Haste and Mining Fatigue change your swing cadence as they do in vanilla, and also scale the mod's own timers (a shield bash's cooldown, a hoe's recovery, a [gun's](Guns.md#handling) cycle and reload).

### Reading what sets a tool apart

Every modular tool names what sets it apart on one line under its parts. They come from three sources. Words the hand can feel come from the figures: head-heavy, light in the hand, heavy, light, keen, blunt, brittle, soft, loose-jointed. The trait comes from the part's shape or the tool's type: Flurry, Opening cut, Hack, Overhead, Quarry, Backstab, Flourish, Timber, Throw, Hook, Sounding, Vein, Charge. The shape is the variant the main part was made in, such as rapier or claymore. **Hold Shift to read each in full:** a word says which figure decides it, where the line is and which part it comes from; a trait says what it does and what it costs; a shape says how it differs from the standard part. The Tool Table shows a part's trait when you hover it in the list, before you make it.

Messages like the Flurry count, a forged part's quality and a Blueprint Table's print status appear as small cards at the bottom right, above your ammo counter if you hold a gun. They fade after a few seconds, and at most three show at once.

### Other tools, other shapes

Every shaped part comes in several shapes, laid at the Tool Table in a different layout or forged to a different rhythm. Each is a way to play, with its price.

- **Pickaxes.** A heavy pick mines a three-by-three square of the face you strike, at a point of extra wear for each extra block. Sneak to mine one.
- **Shovels, hoes and sickles.** A scoop shovel digs a row three wide and a trench shovel digs a block deeper, both at extra wear. A kama tills a row of three, with a short wait afterwards. A rake pulls a harvest's drops to your feet. A scythe cuts a five-by-five of crops and leaves but hits mobs weakly. A billhook leaves a bleed on whatever it hits but its own blow is weaker. A reaping hook replants what it cuts from the seeds it drops, one plant at a time. Sneak to use any of them as the ordinary tool.
- **Heavy and thrust weapons.** A sledge maul smashes the ground and shoves everything around its target when fully charged, but a half-swing is weak. A war hammer is small but hits through armour and dents it, slowing the wearer. A butcher's cleaver hits animals hard and often gives a second helping of meat. A bardiche cuts through the first target into the one behind. A spear lunges hard at a sprint but is weak standing still. A harpoon, thrown, drags what it hits back toward you, and harder in water. A pitchfork hits a small crowd for less each. The mace has only one shape for now.
- **Armour.** Light shells use fewer ingots, weigh less and cover less; heavy ones use more, weigh more and slow you. Helmets: an open helm leaves you open to arrows, a great helm blunts them but makes mobs notice you from further away, a sallet softens blows from behind. Chestplates: a brigandine bites back in proportion to its studs, a breastplate takes the edge off falls and blasts, a hauberk stops blades but not maces. Leggings: greaves kick sprinting mobs away, cuisses plant you against knockback at the cost of slower swimming. Boots: sabatons crush what you land on and make soft landings free, riding boots give a speed burst when you start to run. Only one piece of any given effect counts however many you wear.
- **Bows, crossbows and shields.** A shortbow draws fast and shoots light. A longbow shoots hard and fast but takes longer to draw and slows you while drawn. A recurve is forged: a full draw hits harder and passes through a mob, a part draw does not. A hunting bow is forged and silent, and an arrow to an animal's head hits much harder. An arbalest hits hard, pierces two mobs, loads slowly and slows you while loading. A pistol crossbow loads very fast but is short-range and weak. A repeater loads three bolts at once and fires them scattered. A buckler is small and, on the [parry key](Combat.md#shields), turns a melee blow whole and throws the attacker back, but guards a narrow arc. A kite shield returns arrows and bolts the way you look when you press the parry key and is weaker against melee. A tower shield guards every angle and holds a lot, but slows you, wears fast, and using it while sprinting bashes mobs away.

**Shields get hot.** Every blow a shield stops leaves heat in its face, more for a light face than a heavy one, and the heat stacks until the shield has rested (it cools by itself, even in a chest). Fire and lava warm the face towards their own temperature and no further. A hot face is felt: a shield too hot to hold hurts the hand, unless its grip or its face is wood, leather or another thing that does not carry heat, or you wear gloves. Near the temperature its material softens at, the shield holds less of each blow, and past it every blow wears it twice as hard. It never breaks all at once. A metal face takes heat slowly and softens late; a wooden one warms fast but burns its holder less. Shift on the shield's tooltip says where the line is.

