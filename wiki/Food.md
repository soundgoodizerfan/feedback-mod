# Food

Vanilla food items. They still feed you; Feedback changes how they are cooked and what is left in them.

## In vanilla

Raw meat and fish are cooked in a furnace, smoker or campfire (here the [Campfire](Campfire.md) warms food like a pan does) into a fixed cooked item. Overcooking does nothing: the cooked item just sits there.

## In Feedback

- **Cooking is heat on a food, not a recipe.** A [Furnace, Smoker or Crude Blast Furnace](Furnaces.md) heats whatever food is inside. Raw meat first cooks through (the live organisms in it die), then becomes its cooked item.
- **Keep it on and it spoils.** Past done, the water boils off and the food is *dried out*: it keeps the same item but feeds you less. Hotter than it can take and it *chars*: it feeds you almost nothing and can make you sick. The Smoker's low ceiling keeps food in the good window; a hot furnace left unattended takes it past.
- **Raw food is a risk.** Raw chicken, pork and rabbit carry something live that hunger-poisons you; cook it through. What is left shows on the tooltip.
- **Raw eggs.** A vanilla egg is thrown; sneak and use it to eat it instead (it feeds a little). How safe it is depends on the flock it came from: a chicken laying alone (free range) gives an egg that is *usually* fine and occasionally carries something live, and every other chicken crowded within a few blocks makes it worse, up to the risk of raw chicken. Left lying, an egg goes off like any food. Cooking it (a [Skillet](Skillet.md)) makes it safe.
- **Combining stacks.** Two stacks of one food that differ only in how fresh or cooked they are do not merge in vanilla. Click one onto the other in an inventory and they combine, and they merge with every figure (rot, live load, toxin, char, cook, water, salt, smoke) set to the lower of the two. That includes cook, salt and smoke, so mixing a cooked stack into a raw one gives raw food. Shift-click and picking up from the ground do not combine them.
- **Toxins are different.** Rotten flesh, poisonous potato, spider eye and pufferfish carry a stable toxin. Heat does not remove it; cooking them never makes them safe.
- Eating gives vanilla hunger and saturation, scaled by how the food was cooked. Nothing new enters your meters.

- **Food gives buffs.** The tooltip lists what a food carries (quick sugar, stimulant, protein, minerals, vitamins). Eating it gives the matching effect. Fat and fibre slow it: a fatty or fibrous food comes on later and lasts longer; bare sugar comes on at once, ends fast and leaves a short crash. Dry food comes on later too. Charred food gives nothing. A stew's buffs are diluted by its water like its toxin is, so a thin soup is a weak dose. Pick the food for the trip: a quick bar before a fight, a stew before a shift.
- **Food spoils.** It rots at a rate set by how warm it is and how wet it is, even in a chest or a pocket. A going-off food is first carrying something live (cooking cures that) and later turns toxic (cooking does not). Cold slows it: an [Ice Box](Ice_Box.md), kept cold by ice or by an electric [Refrigeration Unit](Refrigeration_Unit.md). Dry food keeps: a [Drying Rack](Drying_Rack.md). Food that has never been near a machine or the ground is timeless until it is first dropped or put in a machine. A finished [dish](Prep_Table.md) spoils too, as fast as its worst part, and keeps in the same Ice Box and Drying Rack.

Cook one piece by hand in the [Skillet](Skillet.md).

Cook a whole batch in the [Stockpot](Stockpot.md).

What a cut or dried food is called: [Foods](Foods.md).

Put a bowl and foods together into a dish at the [Prep Table](Prep_Table.md).

Knead [dough](Dough.md) by hand at the Prep Table and bake it into a better or worse loaf; bake flatbread to wrap a dish in.


## Reading what you made

A dish shows adjectives worked out from what is in it, and holding Shift says what each one means: *soggy* or *dry* (how much water the parts blend to), *rich* (fat), *dense* or *light* (hunger for the size of the container), *bright* (a strong compound in it) or *bland* (none), and *risky* (something live is in it). Nothing is set per dish; change the parts and the words change. A dish never gets a tool's *brittle* or *soft*.

Figures cost an instrument, carried in your inventory. A [Test Strip](Test_Strip.md) reads a food's toxin in four bands. Water and live load have no instrument and only show as adjectives and the *something live* line.

**The bite.** Sneak and tap right-click on a food or dish to take a nibble (sneak and hold eats it as usual; if you are too full to eat, the tap nibbles as well). It eats nothing, but you take a small share of whatever it would do to you, and the screen says which kind of bad it is: *fine*, *something live in it*, *poisonous*, *spoiled* or *burnt*. You can nibble again after a second. While sneaking you cannot eat; stand up to eat.

## Canning

A **Glass Jar** is one iron plate over two glass panes. Put one jar and one food (or one ladled serving) in the crafting grid, any arrangement, to get a *Jar of* that food. An unsealed jar is only food in glass: it spoils like the food does, and eating it hands back the empty jar.

To seal it, put it in a [Pressure Vessel](Pressure_Vessel.md) on a fire. The vessel needs to hold about 2.7 to 5 Pu so the inside gets hotter than boiling (116 Tu); about thirty seconds there seals the jar. A *Sealed* jar has nothing live in it (so no Hunger from a raw or going-off food) and does not rot. Toxin stays: canning does not cure it. Leave the jars at that heat after they seal and they go to mush: the tooltip says *Mushy* and the food feeds less, down to a tenth after two minutes. Eating a jar gives the contained food's buffs.

## Smoke and salt

- **Smoke.** Food cooked in a [Smoker](Furnaces.md) picks up a smoke coat (the tooltip says *Smoked*). It slows spoilage, but it wears off over a few minutes of game time, so it buys time and does not replace drying. Other furnaces vent their smoke and leave none.
- **Salt.** Boil a [Stockpot](Stockpot.md) of water dry on a fire hot enough to do it, standing in an ocean or beach biome, and it leaves salt: about two per bucket. Take it with an empty hand on the dry pot. Anywhere else a pot boiled dry leaves only a scorched bottom; there is no salt ore, the sea is the brine.
- **Salting.** Put one food and one salt in the crafting grid, in any arrangement. The food comes back *Salted*: some of its water is drawn out, it spoils much more slowly, and it takes more of a bad thing to hurt you (live organisms and toxin both). Salt does not wear off, only soaking would, and that is not in yet. Salt is also a seasoning: poured on a finished dish at the Prep Table it makes the dish safer to eat. A salted food in a stockpot salts the whole pot.

## Food perks

Some foods carry a trait that belongs to them alone, apart from the sugar, protein and vitamins everything has. Ginger is the first: its *Settling* perk eases nausea and hunger. A perk works in proportion to how much of the meal is that food, so a pinch of a potent food goes a long way while a mild one needs most of the bowl. When the meal's effects arrive (fat and fibre slow them), settling takes a share off the time left on nausea and hunger, and clears them if the share is whole. It only eases the weak cases: nausea or hunger above level II, and anything else such as poison or wither, are left alone. The tooltip lists a food's perks beside what it carries, and Shift says what each does.

Perks come in a few kinds, and all of them scale smoothly with how much of the meal carries them:

- **Easing** (settling, antivenin, sobering, bitter tonic): cuts the time left on the weak form of certain bad effects, as above. Each has its own list and its own level limit.
- **Warding** (night sight, steadying, sleep ward): for a while, certain things cannot land: blindness, a knockback shove, or the phantoms' count of sleepless nights, which is set back. The more of the meal, the longer it lasts. Most leave you a little slowed afterwards.
- **Resisting** (warming, cooling, breathing, bracing): for a while powder-snow cold stops building, burning goes out sooner, air lasts longer underwater, or a small shield absorbs a hit. Warming and cooling do nothing against fire or lava respectively.
- **Tuning** (fortifying, slow release, quickening, digestive, hearth, mending, lightfoot, keen edge): reshapes the same meal's other effects. Fortifying lengthens them up to double; slow release stretches them but holds them back; quickening and digestive bring them on sooner (quickening ends sooner too, and a crash can follow, as with sugar); hearth lengthens the regeneration from a warm meal. Mending speeds natural healing while you are fed, lightfoot gives brief speed and a quicker stamina refill, and keen edge makes your blows land harder for a short while but makes you easier to hurt.
- **Purging** (antitoxin, antibiotic): lowers the toxin or live germs in the meal you are eating, the way salt does. Only a fraction: a deadly food stays deadly and rotten food is past help.
- **Tolerance** (brine tolerance): makes the salt in a dish protect it better against live germs; it does nothing for toxin.

A few perks also cost you something, such as a little hunger or a short slowness, scaled to how much of the meal carries them. Eating more of the same perk lengthens it rather than stacking it, and dying clears them.
