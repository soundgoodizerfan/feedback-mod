# Gas Cell

One cell of an [airship](Airship.md)'s envelope. It holds a lift gas lighter than air and lifts the spot where it sits.

## Crafting

Each craft makes 2 cells. There are five skins.

**Gas Cell (Cloth)**

```
W S W
S W S
W S W
```

W is any wool and S is string.

**Gas Cell (Rubberized)**

```
R S R
S R S
R S R
```

R is a synthetic rubber ingot and S is string.

**Gas Cell (Plate)**

```
P P P
P   P
P P P
```

P is an aluminum plate.

**Gas Cell (Bladder)**

```
B S B
S B S
B S B
```

B is a Ghast Bladder and S is string. The lightest skin, and gas-tight. A ghast killed by a player drops bladders ([Oil](Oil.md) has the rest).

**Gas Cell (Coated Cloth)**

```
W L W
L W L
W L W
```

W is any wool and L is a slimeball. Heavier than a bladder skin, and it slowly seeps gas, so a full cell loses a little even at rest.

## Using it

- Each cell holds **1000 mB** of a gas: hydrogen, wisp gas or [helium](Chemicals.md#helium).
- Fill it with a gas bucket or a [Pipe](Pipe.md). Gas cells that touch **share their gas**, so a block of cells fills and drains as one.
- A Vent Valve beside a cell lets gas out; see [Vent Valve](Vent_Valve.md).
- With [Jade](https://modrinth.com/mod/jade), the cell reads **Empty**, **Slack**, **Half full**, **Full** or **Taut**, and adds **Leaking** or **Burning** when that is so.

## How it works

The air a cell pushes aside weighs more than the gas in it, and the difference lifts the cell where it sits. Lifting acts at each cell, so loading trims the ship. Air thins with height, so a cell lifts less higher up. A heavier gas lifts less than a lighter one.

### Stretch

A cell swells as the air around it thins. Each skin takes a different stretch, measured against the cell's volume at rest:

| Skin | Stretch limit |
| --- | --- |
| Cloth | 1.15 times |
| Rubberized | 1.5 times |
| Plate | 3 times |
| Bladder | 2 times |
| Coated Cloth | 1.3 times |

A full cell that climbs past its limit **weeps gas** until it is vented back under it. A Vent Valve opened before the climb avoids the loss.

### Holes and patches

Arrows and bullets that hit a cell leave a **hole** that leaks gas until patched. Right-click the cell with wool, a synthetic rubber ingot or an aluminum plate to patch one hole; each patch is used up.

### Fire

Hydrogen and wisp gas burn; helium never does. A fire, lava or a running [Thruster](Thruster.md) beside a cell lights the whole connected envelope. The gas burns out at the rate its material burns, creatures nearby catch fire, and cloth, rubberized, bladder and coated cloth cells burn away with their gas. A plate cell survives the burn. See [Hazards](Hazards.md).
