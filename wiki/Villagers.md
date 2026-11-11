# Villagers

Villagers and wandering traders deal at a **counter** instead of vanilla's trade screen. A villager posts daily **commissions** for finished tools, weapons, armor and dishes, sells a short list of goods for emeralds and buys goods from you. How you are known in the village sets the prices.

## What vanilla does

Right-clicking a villager opens a list of fixed trades: items for emeralds and emeralds for items. Trading raises the villager's level and unlocks more trades.

## What Feedback does

Right-click an employed adult villager, or a wandering trader, and a wooden counter screen opens. Children, villagers without a job and nitwits are left to vanilla behavior. The villager's trade level keeps vanilla's level names and bar, and rises through deliveries and purchases.

A villager's counter has four tabs: **Orders**, **Goods**, **Sell** and **Standing**. A wandering trader has only Goods and Sell. The villager's reply appears in a two-line speech box.

While the screen is open the villager stops, faces you and waits, as in vanilla trading. Only one player can use a villager at a time.

## Orders

Each day a villager holds **2 plus its trade level** commissions. The orders are the same every time you open the screen that day and change the next day. Each order names a type of piece, what the villager wants of it (the materials of its parts for a tool, weapon or armor piece, qualities for a dish), and the base pay.

| Profession | Orders |
| --- | --- |
| Toolsmith | pickaxe, shovel, axe |
| Weaponsmith | sword, spear, cleaver, maul |
| Armorer | helmet, boots, leggings, chestplate, shield |
| Fletcher | bow, crossbow |
| Farmer | hoe, sickle; plate, soup, skewer, wrap, pie |
| Butcher | cleaver; plate, soup, skewer, wrap |
| Fisherman | plate, soup, skewer, wrap |
| Shepherd | plate, soup, skewer, wrap |
| Mason | pickaxe, shovel |

Higher-level villagers order more kinds of piece and pay more.

### Reading an order

A tool, weapon or armor order lists the parts it cares about, one line each: "Head: bronze" asks for that material, "Head: any metal" asks for any material of that kind. A novice asks about one part and only soft materials such as copper, tin, wood or bronze; a villager asks about more parts and harder materials (iron, then steel, then gems and netherite) as it rises. Name the right material and the part scores in full; a different material scores by how alike it is (same kind, similar hardness and density), never in full. A missed kind scores nothing, and so does a missing part. The villager's complaint names the part that is wrong.

A dish order's stated term reads "Noun: word", for example "Richness: at least rich". Next to it a four-cell bar shows where in the usual range the target sits; a hard requirement fills every cell it allows. Hover a term to see what the figure means. A term line wider than its box slides back and forth so the whole text can be read. One term is a hard requirement, shown as "at least" or "no more than" a word. Some wants are never said; the complaint after a delivery is how you find them out.

### Hints

The **Hint** button on the Orders tab explains a selected dish order; tool orders state their materials outright and have no hint. Hints are free and stay until you press **Back** or pick another order.

- For a dish order, each stated term gets a line saying how to move it: more animal parts, delivering it hot, salted food, smoking, a sealed container and so on. Under it, a line reading "Try building with ..." names up to four foods to build the dish from: foods of the matching kind or cuisine for meat, seafood, plant, starch and cuisine terms, and the foods highest (or lowest, when the term wants low) in the figure for fat, fibre and moisture terms. A closing line points to the [Tasting Spoon](Tasting_Spoon.md).

### Delivering

1. Make the piece the order asks for. See [Tools and Weapons](Tools_and_Weapons.md) and [Armor](Armor.md).
2. Put it in the delivery slot and press **Deliver**.
3. The villager grades it as **Rejected**, **Poor**, **Fair**, **Good** or **Exceptional**, pays emeralds by grade, and names the one thing furthest off, for example "Not enough bite." or "Too much weight."

Worn pieces are graded as worn. On a dish, missing the hard requirement costs a grade. An order can be delivered once a day, and then shows as done. A piece of the wrong type is refused and nothing happens.

A Rejected piece is handed back and the order stays open, but the villager thinks less of you.

### Repeat deliveries

Delivering the same type of piece in the same kind of material again and again pays less for about three days. Varying the type or the material keeps the pay up. Dishes follow the same rule: a similar dish (same main kind of food, same cuisine, same doneness) pays less for a few days.

### Dish orders

Farmers, butchers, fishermen and shepherds also order dishes made at the [Prep Table](Prep_Table.md). Plate, soup and skewer orders appear from low trade levels, wrap at level 3 and pie at level 4 (farmers only for pie).

A dish order states terms on some of: heartiness, richness, chew, moisture, doneness, char, saltiness, smokiness, freshness, warmth, and the share of meat, fish, greens and grain. Put the dish in the delivery slot and press **Deliver**. The dish is judged as it arrives, so rot, cooling and drying count. It is graded **Rejected** to **Exceptional**, paid in emeralds, and the villager names the single worst quality in an adjective, such as "burnt".

Each villager also has tastes of its own. The biome type it comes from (taiga, desert, plains, savanna, snow, swamp, jungle) leans it toward certain cuisines and foods. These tastes count for less than the stated terms and are never said; the complaint is how you find them out. The [Tasting Spoon](Tasting_Spoon.md) reads a dish before you deliver it.

## Standing

The Standing tab shows how the villager regards you. Good and Exceptional work raises it; Rejected work lowers it. It is vanilla's village reputation, so it also changes vanilla price effects such as discounts. The ranks are **Strangers**, **Known here**, **Trusted** and **Honoured**. Jade shows the rank when you look at a villager.

## Sell

Each day a villager wants four to six things. They come from a pool every villager shares plus a pool for its profession. Each line has a price per item and a daily cap.

The shared pool:

| Item | Price (emeralds) |
| --- | --- |
| Any food | 1 |
| Totem of Undying | 12 |
| Golden Apple | 8 |
| Honey Bottle | 2 |

Professions add their own lines:

| Profession | Buys |
| --- | --- |
| Armorer | iron, tin and bronze ingots, lava bucket |
| Butcher | bone, rabbit hide, rabbit foot, leather |
| Cartographer | paper, compass, glass pane, spyglass |
| Cleric | rotten flesh, nether wart, gold nugget, glowstone dust, blaze powder, spider eye |
| Farmer | wheat, carrot, potato, beetroot, pumpkin, melon slice, sweet berries |
| Fisherman | cod, salmon, ink sac, kelp, pufferfish, nautilus shell |
| Fletcher | feather, arrow, tripwire hook, spectral arrow |
| Leatherworker | rabbit hide, scute, leather |
| Librarian | paper, book, ink sac, book and quill |
| Mason | calcite, tuff, terracotta, dripstone block, granite |
| Shepherd | black, gray and brown wool, mutton, red dye |
| Toolsmith | iron, tin and bronze ingots, diamond |
| Weaponsmith | iron and bronze ingots, bone, gunpowder |

To sell, put the items in the delivery slot, pick the line you want and press **Sell**. The villager pays emeralds per item up to the line's daily cap; items beyond the cap stay with you. A villager says so when it is not buying that item or has enough of it. Selling gives the villager a little experience toward its next level.

## Goods

Each profession sells a short list for emeralds. Every line has a daily stock that refills the next day, and lines unlock as the villager's level rises. Some lines also need a standing before they are sold.

Standing sets the price: good standing lowers prices by up to 30%, bad standing raises them by up to 50%.

The cleric sells one enchanted book per day at a high price, and needs standing first.

### Blueprints

Some professions sell a **blueprint**, one per day each, at a high price. A line stays locked until your standing with the villager and its trade level are both high enough. A blueprint is a copyable design: open it at the [Blueprint Table](Blueprint_Table.md), change it and print it again. Guns and the crossbow can only be made from a blueprint.

| Profession | Blueprint | Price (emeralds) | Level | Standing |
| --- | --- | --- | --- | --- |
| Weaponsmith | single-shot gun | 12 | 2 | 10 |
| Weaponsmith | lever carbine | 20 | 3 | 30 |
| Weaponsmith | bolt rifle | 20 | 3 | 30 |
| Weaponsmith | self-loading pistol | 30 | 4 | 30 |
| Weaponsmith | automatic rifle | 50 | 5 | 60 |
| Fletcher | crossbow | 15 | 3 | 10 |
| Toolsmith | hammer | 6 | 2 | 10 |
| Farmer | sickle | 6 | 2 | 10 |
| Armorer | chestplate | 12 | 3 | 10 |
| Farmer | wrap recipe card | 8 | 3 | 10 |
| Butcher | skewer recipe card | 6 | 2 | 10 |

Six professions also sell a **treatise**, one per day, at a high trade level; see [Treatise](Treatise.md#obtaining).

| Profession | Treatise | Price (emeralds) | Level | Standing |
| --- | --- | --- | --- | --- |
| Weaponsmith | Weapons Treatise | 32 | 4 | 10 |
| Toolsmith | Mechanical Treatise | 32 | 4 | 10 |
| Cartographer | Aerospace Treatise | 40 | 5 | 10 |
| Librarian | Robotics Treatise | 40 | 5 | 10 |
| Cleric | Chemical Treatise | 32 | 4 | 10 |
| Mason | Materials Treatise | 32 | 4 | 10 |

Blueprints for tools and armor carry the type's default design. Gun blueprints carry that gun's design. A recipe card carries a dish layout: open it to read the layout and the predicted figures.

The wandering trader sells a small fixed list with a daily stock and no standing. On market day it sets up at a [Market Cross](Market_Cross.md).

### Standards stall

The cartographer sells calibrated reference instruments: calipers, thermometer, balance scale, pressure gauge, hydrometer and survey meter. A reference instrument reads exact figures and is marked "(reference standard)". It costs 24 to 28 emeralds, needs honoured standing and is sold once a week. See [Instruments](Instruments.md).

## Beyond the screen

- A villager also buys from a stocked [Counter](Counter.md) and delivers at the [Ledger Board](Ledger_Board.md), which lists every open order nearby and posts contracts.
- Villagers of one profession working close together form a [guild](Village_Guilds.md).
- Buy lines pay differently by the village's biome type: snow and taiga villages pay more for food, wool and coal, desert villages less. Some shop lines are sold only in certain biome types. Food pays by freshness: fresh in full, going off half, rotten refused.
- Villagers who are farmers plant and harvest the mod's [crops](Crops.md). Generated villages can contain [stalls, a plaza and guild yards](Village_Stalls.md).

## See also

- The emerald has an info page in JEI about commissions, and JEI has a Village goods category listing what each profession sells (blueprint lines read "Blueprint: " and the design's name) and a Villagers buy category listing what they buy.
