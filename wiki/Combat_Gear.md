# Combat Gear

Three worn pieces that sharpen a fighter without making [parrying and rolling](Combat.md) free. Each goes in its own Curios slot, which the mod adds: **bracer**, **belt** and **back**.

## Crafting

Shapeless, in any arrangement: **plates** of one metal plus **leather**. The counts pick the piece.

| Piece | Slot | Recipe |
| --- | --- | --- |
| **Bracer** | bracer | 2 plates + 1 leather |
| **Sash** | belt | 1 plate + 2 leather |
| **Back piece** | back | 3 plates + 1 leather |

All the plates must be one metal and all the straps one material; a mixed set is refused. The recipe appears once you have iron plate. What comes out is **baked from exactly those materials**, so the figures below depend on what you used. Sheet stock is plate, not ingot.

**Material quality** is a curve that never runs off the scale: copper gives about a tenth of the maximum, iron four-tenths, steel two-thirds, diamond nearly all of it. A better material always helps, but there is a hard cap on each effect whatever you wear.

## Bracer

A shell on the forearm. It **widens your parry window** (up to +2 ticks) and takes a share of each blow off your weapon's joint (up to half), so a weapon parries heavier hits before it wears. It never changes the recovery or the cone, so a parry stays a skill.

## Sash

A belt of plate links and straps. It **lengthens your roll** (up to +20%), **shortens the recovery** (up to 4 ticks) and can add a tick of invulnerability.

## Back piece

Plates on the back. It adds **carried mass** (up to 40 Mu, more for denser plate). A heavier body staggers for a shorter time, so a back piece **shortens your stagger**.
## Seeing it

Worn gear is drawn on the body (simple shapes tinted by the shell material): the bracer on the left forearm, the sash at the waist, the back piece on the back, and the glove (tinted by its metacarpal plate, if it has one) on the hands.

## Glove

Made at the [Woodworking Table](Woodworking_Table.md) (the plain leather one also from four leather in the crafting grid): shape a body from leather or carpet stock. Worn in the hands slot, the body keeps warm work and weak shocks off the hand, as far as the material stands it. Add a fibrous lining (wool, say) and an insulating lining against the hand cuts the heat that reaches it: a lined glove is never worse than the same body unlined.

A **metacarpal plate** (shaped from ingot or plate stock, laid over the back of the hand) turns the glove into a fighting glove. By its hardness and density it:

- adds damage to **barehanded swings only** (weapons get nothing, and the fist moves scale with it);
- adds **armour** while worn, by hardness;
- lets a bare hand **parry** like the [Feedbacker](Feedbacker.md) does (punches projectiles back; the same tight window), wearing the glove as it does;
- **slows your swing** in proportion to its weight: a heavy plate costs attack speed.

The plate keeps no heat off the hand; the body and lining do that. A broken glove does nothing. `/feedback combat` prints what your gear adds.

## Granting techniques

A piece of gear can also carry a **grants** list: techniques it gives you while worn, the same as buying the node in the [skill tree](Skills.md#skills-from-gear). The [Feedbacker](Feedbacker.md) is the one that does so today.
