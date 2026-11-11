# Treatise

A treatise is a numbered book on one **school** of engineering. Holding a volume lets the [Blueprint Table](Blueprint_Table.md) draw blueprints of the types that volume covers and lets the [Engineering Table](Engineering_Table.md) assemble them. It is kept, never used up.

## Volumes

Each school has one item per volume. Every volume is a subject of its own: holding Weapons Treatise II does not give you Weapons Treatise I, and nothing in one school opens a type in another.

| Treatise | Types it opens |
| --- | --- |
| Weapons Treatise I: Blades and Heads | maul, spear, cleaver |
| Weapons Treatise II: Firearms | gun, spring gun, crossbow |
| Weapons Treatise III: Electric Guns | electric gun |
| Mechanical Treatise I: Engines and Flywheels | engine, flywheel |
| Mechanical Treatise II: Transmission | transmission |
| Aerospace Treatise I: Propellers | propeller |
| Aerospace Treatise II: Thrusters | thruster |
| Robotics Treatise I: Suit Modules | suit module |
| Chemical Treatise I: Pressure Vessels | pressure vessel, boiler, large boiler, gas tank |
| Chemical Treatise II: Heat and Compression | heat exchanger, gas compressor |
| Chemical Treatise III: Explosives | demolition charge |
| Materials Treatise I: Machine-Shop Consumables | no types |

Types outside every volume (tools, armor, water wheel, thermal store, Leyden jar, pump, fluid burner, electromagnet, galvanic cell, lamp, dynamotor, battery, belt and the rest) need no treatise.

## Obtaining

- **Write one.** Craft a **Blank Treatise**, put it in the blank slot on the Treatise tab of the Engineering Table, put an item of that volume's types in the specimen slot and press **Take apart**. The item is taken apart and the Blank Treatise becomes that volume. Most types that need a treatise have a [specimen](#specimens) you can craft to take apart first; a maul, spear, cleaver or charge is made the usual way.
- **Find one.** A treatise of a random school and volume turns up, rarely, in dungeon, mineshaft, desert temple, stronghold and bastion chests.
- **Buy one.** Weaponsmiths, toolsmiths, cartographers, librarians, clerics and masons sell volume I of their school at a high trade level, and the later volumes at higher levels; clerics sell Chemical I and II, and weaponsmiths all three Weapons volumes. See [Villagers](Villagers.md#blueprints).

### Blank Treatise

```
Paper + Paper + Leather + Ink Sac (shapeless)
```

### Specimens

A specimen is one ready-made item of a type, built to a fixed cheap design, crafted in the crafting grid. It is not a good tool; it is something to take apart on the Engineering Table's [Treatise tab](Engineering_Table.md#treatise-tab) with a Blank Treatise. A specimen is made of the metal or wood named after it. Layouts are shown as in a grid; blank spaces are empty.

```
Gun (iron)                 Spring gun (iron)         Crossbow (wood)
I I G                      I I S                     S I S
  P S                      P C I                     T P T
    S                          S                       S

I iron ingot, G gunpowder, P any plank, S stick, C piston, T string

Electric gun (copper)      Engine (iron)             Transmission (iron)
C C R                      I P I                     I I I
P R S                      I C I                     P C P
    S                      I B I                     I I I

C copper ingot, R redstone, P any plank, S stick
Engine: I iron ingot, P piston, C chain, B bucket
Transmission: I iron ingot, P piston, C comparator

Flywheel (iron)            Propeller (wood)          Thruster (iron)
I I I                      P   P                     I B I
I B I                        I                       I C I
I I I                      P   P                       N

Flywheel: I iron ingot, B iron block
Propeller: P any plank, I iron ingot
Thruster: I iron ingot, B bucket, C blaze powder, N iron bars

Suit module (copper)       Pressure vessel (iron)    Gas tank (iron)
C R C                      I I I                     I I I
I D I                      I B I                     I G I
C R C                      I G I                     I I I

Suit module: C copper ingot, R redstone, I iron ingot, D diamond
Pressure vessel: I iron ingot, B bucket, G iron nugget
Gas tank: I iron ingot, G glass pane

Boiler (iron)              Large boiler (iron)       Heat exchanger (copper)
I I I                      I C I                     C C C
I C I                      I C I                     I B I
I B I                      I C I                     C C C

Boiler: I iron ingot, C cauldron, B bucket
Large boiler: I iron ingot, C cauldron
Heat exchanger: C copper ingot, I iron bars, B copper block

Gas compressor (iron)
I P I
I H I
I I I

I iron ingot, P piston, H hopper
```

## Using it

A treatise counts as held, at the Blueprint Table and at the Engineering Table, when it is:

- loose in your inventory,
- inside a [Treatise Binder](Treatise_Binder_and_Shelf.md) in your inventory, or
- inside a [Treatise Shelf](Treatise_Binder_and_Shelf.md) touching the table, or
- inside a [Treatise Binder](Treatise_Binder_and_Shelf.md) in the Engineering Table's binder slot.

Without a volume, the types it covers are left out of the Blueprint Table's type row and list altogether, so nothing of them can be picked or drawn, and the Engineering Table does not offer to assemble them: its status line names the volume it needs. The treatise's tooltip lists every type it opens. Treatises stack to sixteen. Copying a blueprint needs no treatise, and neither do repairing an item and taking it apart.
