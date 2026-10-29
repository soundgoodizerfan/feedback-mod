# Enchanting Table

## What vanilla did

You put an item and some lapis lazuli in the table, spent experience levels, and picked one of three offered enchantments. The offers were rolled: which enchantments and how strong came from the level you spent and a hidden roll, and bookshelves around the table nudged the roll up. The enchantment sat on the whole item.

## What Feedback does

Feedback **replaces** the Enchanting Table with its own. It is crafted the same way (a Book, two Diamonds and four Obsidian) and looks the same, but you can't craft the vanilla one any more. Mine it with a pickaxe: without one it drops nothing.

Nothing is rolled. An enchantment is an **effect** laid onto a single **part**, a pickaxe head, a handle, a chestplate's plate, by playing a short game. What the effect does depends on **which part it sits in** and **what the part is made of**. Some pairings are bad on purpose: Fire Aspect on a sword blade sets what you hit alight, but on a handle it sets *you* alight.

## The screen

Put one **shaped part** in the slot. It has to be **hot**: heat it in a [furnace](Furnaces.md) or the [Hearth](Hearth.md) and carry it over, exactly as you do forging stock. The table doesn't heat it. A cold part is refused.

Press the effect button to choose an effect, then **Begin**. Attuning a part costs **3 experience levels**, spent whatever the result. (Creative players pay nothing.)

On the right of the screen are two gauges:

- **Field:** a vertical track. The shaded band is the target you want to land in. The bright line is where the part's polarization is now; it moves slowly after your input, and when you let go of the field it **stays where it is**. Hold the left mouse button over the gauge: how far the pointer sits above the middle of the track is how strong a field you apply (below the middle reverses it). Let go and the field is zero.
- **Heat:** the part cools while you play. The gold stretch is its **attuning window**. The field only works while the part is inside it. Above it nothing happens yet; below it the part locks and the result is judged as it stands. Press **Commit** to stop early.

A weak field does nothing at all; a field has to be past a threshold before the part moves. A strong field moves it faster but you have less time to stop at the right place. There is always a little jitter, and more of it while the part is hotter.

### Starting hot or cold

A part that starts well above its window has a long wait but a forgiving clock, and its signal is noisy. A part that starts already inside the window is cleaner but you have less time. Either works.

## The result

- Landing near the middle of the band takes at full strength, with no flaw.
- Landing at the edge of the band takes one level lower.
- Landing a little outside takes at a lower level, **with one or two flaws** on the part (flaws make a part weaker, as with [forging](Hand_Hammer.md)).
- Overshooting, pushing the wrong way, or never getting past the threshold takes **nothing**. The part is never destroyed, but the experience is spent.

How strong an effect can be depends on the part's material: a material that takes enchantments well (gold best, stone worst) reaches a higher level. A part carries **one** effect.

## Effects

| Effect | On a head or blade | On a handle or grip | On a plate |
| --- | --- | --- | --- |
| **Fire Aspect** | Sets what you hit alight | Burns **you**, as hot as the part's material burns | Sets whoever hits you alight |
| **Keen** | Hits harder, but the part chips more easily | Nothing | Nothing |
| **Ward** | Nothing | Nothing | The plate resists fracture |

A part with an effect in a role the effect has no entry for just does nothing. A tooltip on the finished tool names what each effect does in its role, so you can read it before you swing.

## The effect is on the part

Enchantments live on the **part**, not the tool. Set an attuned part into a tool and it carries across. Vanilla gear converted into modular gear keeps the enchantments it already had.

## What is left alone

Villager enchanted books and loot books are unchanged. Generated structures (villages and so on) still contain vanilla Enchanting Tables.
