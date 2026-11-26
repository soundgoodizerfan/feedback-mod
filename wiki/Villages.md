# Villages

Generated villages can contain market stalls, a plaza with a Market Cross and guild yards, and villagers of one profession form guilds. A village is a place to trade; what villagers buy and sell is on [Villagers](Villagers.md), and trade between villages is the [Depot](Counter.md#depot).

## Stalls and plazas

### What vanilla does

A vanilla village has houses, farms, a bell, a well and job-site buildings. Nothing in it sells goods from a shelf or posts contracts.

### What Feedback does

Newly generated plains, desert, savanna, snowy and taiga villages can contain:

- **Market stalls:** [Counters](Counter.md) the village keeps. The village restocks them each day with the trade of the nearest job site, at the market price plus 25%. Right-clicking the back says the village keeps the stall; it has no till.
- **Stall rows:** several stalls in a line.
- **A plaza center:** a paved square with the bell, a [Market Cross](#market-cross) and a [Ledger Board](Ledger_Board.md).
- **Guild yards:** smithing and stonecutter yards where [guilds](#guilds) form.

Villages that already exist are unchanged.

Villagers who are farmers plant and harvest the mod's [crops](Crops.md) (not rice) and carry starter seeds.

A [Block Camera](Block_Camera.md) beside a road can report a player carrying Bad Omen, and the raid comes to the village center when a village is within 64 blocks.

## House chests

About 1 chest in 6 in a village house, workshop or temple holds a [blueprint](Blueprint_Table.md) of a random type's starting design: any tool, weapon, armor piece, machine part or similar, but never a gun, dish, face or suit module. Open it at the Blueprint Table, change it and print it. Villagers sell more blueprints (see [Villagers](Villagers.md#blueprints)).

## Guilds

A guild forms among villagers of one profession who work close together. It posts a bigger daily order and shares experience between members.

### How a guild forms

Three or more villagers of one profession with job sites within 16 blocks of each other form a guild. No block is needed. Jade on a villager shows "Guild of X, N members, tier T".

### What a guild does

- **Guild order:** once a day the guild posts one bigger order. It pays 1.5 times a normal order, needs a piece graded Good or better, and can be delivered at any member.
- **Shared experience:** members share a quarter of each other's delivery experience, up to 6 a day.
- **Extra shop lines:** the guild's tier unlocks extra lines in the shop of the smiths.

| Tier | Needs |
| --- | --- |
| 1 | 3 members |
| 2 | 5 members and a member at level 3 |
| 3 | 8 members and a member at level 5 |

See [Villagers](Villagers.md) for orders and shop lines, and [Stalls and plazas](#stalls-and-plazas) for guild yards.

## Market Cross

A pillar for a paved square. When the square is big enough, the wandering trader sets up there on market day and the village shops harder.

### Obtaining

Market Crosses stand at the centers of plazas in generated villages (see [Stalls and plazas](#stalls-and-plazas)). A broken cross drops itself and can be placed elsewhere.

### Using it

Right-click it to read whether the square counts and how many days until the next market.

### How it works

The cross is active when it stands on a paved, open square: at least 25 open road tiles under open sky, including a 3 by 3 block of them around the cross. Path, gravel, cobblestone, stone bricks, bricks and planks count as paving.

Every 8th day is **market day**. On market day:

- The wandering trader sets up at the cross for the day. Vanilla wandering traders appear at random and wander off; here the trader sells at the [counter screen](Villagers.md) from the cross.
- When the village honors you, a second trader sells goods of the region.
- Villagers within 48 blocks of an active cross want 3 more things and make one extra errand, and shop stock doubles.

The [Ledger Board](Ledger_Board.md) Notices tab shows the countdown. [Depots](Counter.md#depot) send their convoys on market day.
