# Skills

Combat has a **skill tree**, opened with **K**. A skill never locks something you could otherwise do: it either nudges a figure (a wider [parry](Combat.md#parry) window) or opens a **technique** the body could always attempt badly. Nothing is gated on what you're holding by name.

Every figure here is a placeholder.

## Points and pools

Each of four trees has its own **pool of points**, earned by doing that tree's thing. Spend points on a node with **Learn** in the skill screen. A node needs the nodes it requires to be learned first. Points and learned nodes **survive death**.

### Earning points

| Pool | You earn it by | Points |
| --- | --- | --- |
| Parry | landing a [parry](Combat.md#parry) | 10 |
| Evasion | a [roll](Combat.md#roll) that dodged a real blow (fire and other sourceless damage don't count) | 8 |
| Gun-fu | any hit while a gun is in your main hand (a bullet or a swing) | 2, plus 6 for a kill |
| Melee | a hit with a blade, mace or spear (by its design, not its name) | 3 |

**Repeats pay less.** Each award is 60% of the previous one from the same target, and one repeat is forgotten every 10 seconds. A dummy stops paying; a fresh enemy pays in full. Off-hand strikes earn nothing yet.

## The trees

### Parry

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Wider window | 20 | | +2 ticks of window |
| Quick recovery | 30 | Wider window | recovery 4 ticks shorter |
| Tight redirect | 30 | Wider window | a turned projectile flies straighter |
| Hard stagger | 40 | Quick recovery | parried attackers stagger 25% longer |
| **Riposte** | 80 | Hard stagger, Tight redirect | the hit after a weapon parry does 1.5× |

### Evasion

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| Long roll | 20 | | roll 1 block further |
| Extra i-frames | 30 | Long roll | 2 more ticks of invulnerability |
| **Wall kick** | 70 | Extra i-frames | the [wall kick](Combat.md#wall-kick) |

### Gun-fu

| Node | Cost | Needs | Does |
| --- | --- | --- | --- |
| **Pistol whip** | 15 | | hold **Z** and swing a gun as a bludgeon (without it: a deny click) |
| **Chamber** | 50 | Pistol whip | a staggering swing chambers a free shot |
| **Dual** | 60 | Pistol whip | swing with the off-hand gun too |
| **Spin** | 90 | Chamber, Dual | sneak + swing hits every mob within 2 blocks and knocks them away; slower cooldown |

See [Guns](Guns.md#swinging-a-gun).

### Melee and tether

Three weapon branches and a tether branch.

| Branch | Node | Cost | Needs | Does |
| --- | --- | --- | --- | --- |
| Blade | Blade edge | 20 | | stagger hits 10% harder |
| Blade | **Blade flourish** | 60 | Blade edge | after a roll, a blade hit takes 10 ticks off the cooldown |
| Mace | Mace weight | 20 | | stagger hits 10% harder |
| Mace | **Mace quake** | 60 | Mace weight | a hard mace hit (5 or more damage) shoves and staggers mobs within 2.5 blocks of the target |
| Spear | Spear reach | 20 | | +1 tick of parry window |
| Spear | **Spear vault** | 60 | Spear reach | thrust a spear while sprinting on the ground and you hop up and over; once per 2 seconds |
| Tether | Fast reel | 25 | | [grapple](Grapple.md) reels 20% faster |
| Tether | Winch | 45 | Fast reel | 30% faster again |
| Tether | Firm grip | 25 | | [grab](Combat.md#grab) holds 1 second longer |
| Tether | Iron grip | 45 | Firm grip | 2 seconds longer |

The branch is a label in the screen only; a node's real requirement is the nodes it names.

## Respeccing

An **Unlearning Token** (paper, an ink sac and an iron nugget, shapeless) un-buys every node and gives the points back. It isn't used up if you hadn't learned anything.

## Skills from gear

A worn curio can **grant** a technique directly, as if you'd bought it. The [Feedbacker](Feedbacker.md) grants its projectile punch this way.

## Datapacks

Trees are datapack files (`data/<namespace>/skill_tree/*.json`): a tree name and a list of nodes, each with an id, a cost, the nodes it requires and either a `figure` and `amount` or an `unlock` id. A pack can add a tree.

## Commands

`/feedback skill grant <tree> <points>` (operators) gives points to test with. Trees: `parry`, `evasion`, `gun_fu`, `melee_and_tether`.
