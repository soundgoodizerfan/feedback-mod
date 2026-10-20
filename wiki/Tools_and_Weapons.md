# Tools and Weapons

Pickaxes, axes, swords, bows, shields and the rest are no longer a fixed list of wooden, stone, iron, golden, diamond and netherite. Every one is **assembled from parts**, and what it does comes from what those parts are made of. The recipes for vanilla's tools are gone; put the parts together yourself, in a crafting table or an [Assembly Bench](Assembly_Bench.md).

Armour works the same way: see [Armour](Armour.md).

## Making the parts

A pickaxe is a **head** on a **handle**, and the head is a part of its own. Shape it first: lay stock in the head's shape in a crafting table (or an [Assembly Bench](Assembly_Bench.md)) with a [Hand Hammer](Hand_Hammer.md) anywhere else in the grid. The hammer loses a point of durability. What comes out is one `Pickaxe Head`, whatever it's made of; the material rides on the item.

| Part | Shape (stock in each `X`) |
| --- | --- |
| **Pickaxe head** | `XXX` |
| **Axe head** | `XX / X_` |
| **Shovel head** | `X_X / _X_` |
| **Hoe head** | `XX` |
| **Sword blade** | `X / X` |
| **Mattock head** | `XX_ / X_X` |
| **Cleaver head** | `XX / XX` |
| **Pike head** | `X / X / X` |
| **Maul head** | `XXX / XXX` |
| **Sickle head** | `XX_ / __X` |
| **Trident tines** | `_X_ / X_X` |

Every cell must be the same stock. **Stock** is ingots, gems, planks, cobblestone or bone (tines take ingots, gems or shards). Anything of a material that fits works, so a head can be iron, bronze, gold, diamond, stone, a plank or a bone.

**Steel and other hot-worked metals have to be hot to shape**, exactly as under a [hammer](Hand_Hammer.md); the part comes out as hot as the stock was. Metals without a hot-working window (iron, wood, stone) shape cold.

## Putting one together

Lay the parts out in the grid. Each part is **one cell**. **Handles** are sticks, rods or bone, and all cells of one part must be the same material.

| Item | Layout | Parts |
| --- | --- | --- |
| **Pickaxe** | `H / S / S` | head, two handles |
| **Axe** | `H / S / S` | head, two handles |
| **Shovel** | `H / S / S` | head, two handles |
| **Hoe** | `H / S / S` | head, two handles |
| **Sword** | `B / S` | blade, grip |
| **Mace** | `M / S / S` | a [heavy core](Heavy_Core.md) (or a block of metal), two rods |
| **Trident** | `T / S / S` | tines, two shafts |
| **Bow** | `_LW / L_W / _LW` | three limbs, three lengths of string |
| **Crossbow** | `PWP / SLS / _S_` | two prods, string, three stock, a lock, **plus a Crossbow Blueprint** (below) |
| **Shield** | `FRF / FFF / _F_` | six faces, a rim |
| **Shears** | `_B / B_` | two blades |
| **Mattock** | `H / S / S` | head, two handles: digs stone and wood, each at half speed |
| **Cleaver** | `H / S` | head, grip: a short, heavy axe that bites deeper than a full one |
| **Pike** | `H / S / S / S` | head, three handles (needs the 4×4 bench): reaches further |
| **Maul** | `H / S / S` | head, two handles: splits wood and staggers what it hits |
| **Sickle** | `H / S` | cutting a crop or a leaf takes its neighbours too |

You can add:

- A **binding** (leather, string, wire) beside the handle: `H_ / SB / S_`.
- A sword takes a **guard** and a **pommel**: `B / G / S` and `B_ / G_ / SP`. The pommel is a counterweight.
- A **tip**, below.
- Armour and shields can take a **grip** or **lining**.

### Tips

A pickaxe, axe, shovel, hoe, mattock, cleaver, sickle, pike or sword can have a **tip**: one gem, shard or nugget set beside the head or blade (`HT / S / S`, `BT / S`). **The tip decides hardness**, so what the edge can dig is the tip's. It bites like the two together, and the body keeps the toughness and most of the wear. A steel pick with a diamond tip digs what diamond digs, chips like steel, and lasts about 1.6× as long as plain steel. A tip no harder than its body does nothing (a gold tip on iron is a pad). **Maces, mauls, tridents, bows, crossbows, shields and shears take no tip.** A tool made without a tip can't be given one later; re-forging with a different gem swaps one.

### Brittle heads

A whole diamond head is possible and bites superbly, but diamond is brittle: it keeps only a sixteenth of the durability its hardness suggests. Anything tougher than a fifth of its hardness (iron, stone, steel) is unaffected. Set diamond as a tip instead.

### Crossbow Blueprint

The crossbow is the one tool with a mechanism, so it needs a **Crossbow Blueprint** in the grid, made at the [Blueprint Table](Blueprint_Table.md). The craft keeps the blueprint. Any blueprint of the type will do; the parts don't have to match it.

## What the parts do

You won't see a stat sheet. Hold the tool and read the words on its tooltip: *head-heavy*, *light in the hand*, *heavy*, *keen*, *blunt*, *brittle*, *soft*, *loose-jointed*, *worn*. A tool is a physical object, and these are the things you'd feel picking it up.

- **A heavier head hits harder and swings slower.** Gold is nearly three times iron's weight; a gold pickaxe is slow and hits hard, but gold is soft.
- **A longer handle reaches further** and puts more strain on the joint.
- **Hardness decides what it can dig.** An edge needs to be at least **3** hardness for iron ore and the stone class, **6** for gold, diamond and redstone ore, and **40** for obsidian and ancient debris. So copper and gold can't mine iron ore, iron can't mine obsidian, and diamond and netherite can. (Stone has a very high number, so a stone pickaxe *can* mine diamond ore. It chips doing it.)
- **How long it lasts** comes from how hard and strong the edge is. Iron lasts about as long as it used to; gold barely lasts; a whole diamond head bites superbly but is [brittle](#brittle-heads), so diamond is better as a tip.
- **Enchanting** works as normal. How good a tool is at taking enchantments depends on its main part: gold is best, stone is worst.

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

**Repairing:** put the tool in the crafting grid with the material its wearing part is made of (an iron pickaxe with iron ingots). Each ingot puts back its share.

**Swapping a part:** put the tool in the grid with a *different* part, or with stock of a different material (a bronze pickaxe head over an iron pickaxe replaces its head). A new wearing part is fresh. Enchantments stay.

## Old vanilla tools

Vanilla tools, weapons and armour that turn up (in chests, on mobs, from villagers or in your pockets) are converted on the spot into the modular item they would have been, with the same name, enchantments and wear. An iron pickaxe is an iron head on a wooden handle.

**Diamond is the exception.** A diamond tool becomes a **steel** one with a **diamond tip** ("Diamond-tipped Steel Pickaxe"); diamond armour becomes steel with four diamond **studs**. A whole diamond head would be brittle and poor. The converted pick lasts about half as long as vanilla's diamond did and is no longer the best tool in the game: netherite is.

## Plating

A tool or armour piece whose main part is metal can hang in an [Electrolytic Cell](Electrolytic_Cell.md#electroplating) and be plated like any metal piece. A good coat adds the coat metal's corrosion resistance (a tool used wet rusts by how little its metal resists water). On **armour** a harder coat also hardens the plate a little. On a **tool** the edge is ground bare, so a coat protects the flats and changes neither bite nor hardness. Re-forging the main part removes the coat.

## Measuring

The tooltip gives words for free. To see a design's numbers *before* you make it, draw it at the [Blueprint Table](Blueprint_Table.md). For numbers on a finished piece, carry a [Balance Scale](Balance_Scale.md) (mass) or a [Scratch File Set](Scratch_Files.md) (hardness).
