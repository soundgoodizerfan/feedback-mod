# Composed Parts

A part can be made of **several materials**. Each material sits in its own **slot** of the part: a sword blade has a body and an edge, a scope has a frame and a lens. The first slot is the part's main material; the others are extra.

## Slots you choose

When a part has a slot you choose, the part bench shows **one more stock slot** for it. Put the main stock in the center and the extra material in its own slot; the bench takes each slot's share of the cost from its own pile. A slot left empty is cut from the main stock. On the [Blueprint Table](Blueprint_Table.md) the part panel shows a **material stepper** for each such slot, and the drawing pins those materials.

| Slot | On | What it does |
| --- | --- | --- |
| **Edge** | sword, axe, hoe, maul, pickaxe, shovel, sickle, spear, cleaver, hand knife, hand hammer head, file head, saw blade, shears blade | A harder material inlaid along the cutting edge of a softer body: the katana trick. The edge sets how well it bites; the body behind it sets how well it holds. |
| **Lens** | gun sight, pulse gun sight, suit optic module | The glass of a scope. Its clarity decides how much light the scope loses at its zoom. |
| **Media** | suit filter module | The porous packing of a cartridge, charcoal or brick. How open it is decides how strongly the filter works. |
| **Jacket** | gun barrel | A second layer around the liner. A jacket of a damping material (sculk) steadies the spread, cold and hot. |
| **Anode and cathode** | battery plates, galvanic cell anode | The two electrodes, each of its own metal. A battery cell holds the mean of the pair's charge per ingot; a galvanic cell's current follows the gap between the two metals' electrode potentials. |
| **Back** | bow limbs, crossbow prod | A backing on the limb: its stiffness joins the core's by share, so a stiffer back draws harder and shoots faster. |
| **Balls** | bearings (in the [Dynamotor](Dynamotor.md)) | The friction of the balls scales the bearing's drag. |
| **Wrap** | gun grip | The wrap sets the hold. |
| **Padding** | bed mattress | The padding sets the [comfort and warmth](Bed.md). |

A slot you choose only counts when something is in it. Mass uses the main material across the whole part.

Each slot accepts stock by shape and by property; see [What a Part Slot Takes](What_a_Part_Slot_Takes.md).

## Fixed materials

Most other parts carry a slot whose material is fixed: the lining of a boiler, vessel or tank, the valves of a cylinder head, the surface of a machine part or fitting, the leather wrap of a binding, pommel or tip, the facing of a shield, the pad of a stock. You never pick it and it asks for no extra stock slot; it takes a share of the same stock you already put in.

## Axes in place of variants

Counts and sizes are **axes**, set with a stepper on the Blueprint Table, rather than separate shapes: engine cylinders and compression, propeller blades and pitch, transmission gears, dynamotor size and turns, vial volume, backpack capacity, binding wraps, pommel weight, tip thickness, hand crank arm length, fuse delay, flywheel rim mass, shield rim width, beam lens count, battery cell size and cell count, knife length, width and thickness, the tines and barb of a spear head, and the one setting of each suit module attachment (see [Exosuit](Exosuit.md#upgrade-modules)). See [Armor](Armor.md#axes-aiming-and-names) for how aiming works. The [Telescope](Telescope.md) is three parts put together in a crafting grid, each with its own axes.

## How a part looks

A part's icon, and the part held or lying on the ground, shows the layer it is on a tool, drawn without a haft. Head parts (axe, pickaxe, shovel, hoe, maul, sickle, cleaver, sword blade, spear, hammer, knife, file, screwdriver bits) show their head shape at the length they were made to, so a long or short head shows its measurements before it is put on a tool. The sword guard and the binding show their layer in their variant (crossguard, knuckle, wrapped and so on). Every part is tinted by its material.
