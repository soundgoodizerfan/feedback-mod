# Nuclear Fuel

The rods that go into the Fuel Channels of a [Fission Reactor](Fission_Reactor.md): the Fuel Rod, the Thorium Fuel Rod and the Spent Fuel Rod they become.

## Crafting

**Fuel Rod**

```
S U S
S U S
S U S
```

S is a Stainless Steel Plate and U is a Uranium Ingot.

**Thorium Fuel Rod**

```
S T S
S U S
S T S
```

S is a Stainless Steel Plate, T is a Thorium Ingot and U is a Uranium Ingot.

Both rods are radioactive from the moment they are made ([Radiation](Radiation.md)); the Fuel Rod is the stronger. A clad rod is a finished assembly: it cannot serve as the material of a tool, gun or any other modular part.

## Fuel Rod

[Uranium](Materials.md#uranium) clad in stainless steel. At full power it burns for about **2 hours**, then turns into a Spent Fuel Rod.

## Thorium Fuel Rod

[Thorium](Materials.md#thorium) and a little uranium. The thorium slowly turns into fissile fuel while the rod runs, so the rod starts weaker than a uranium rod and gets stronger as it burns. It lasts longer than a uranium rod for the same power.

## Spent Fuel Rod

A burnt-out rod: it makes no power and gives off about ten times the radiation of a fresh Fuel Rod. Carrying it is a heavy dose; keep it in a lead-lined chest and take it out of the reactor with the sneak click on the channel ([Fission Reactor](Fission_Reactor.md#fuel-channel)).

### Cooling a spent rod

A rod keeps making decay heat after it is discharged, falling over days. A **Spent Fuel Pool** holds up to nine spent rods under water and takes that heat: right-click with a rod to put it in, with a water bucket to fill it, sneak with an empty hand to take a rod out, and use an empty hand for its rods, water and temperature. Over 100 Tu the water boils away, so a pool with fresh rods needs topping up. A pool left dry gives off a dose of about sixty Ru a rod, and heats until the rods slump into [Core Slag](Fission_Reactor.md#misuse-and-hazards).

```
L   L
L B L
S S S
```
L is a Lead Plate, S is a Stainless Steel Plate, B is a bucket.

## Burn and condition

A channel reads two figures for the rod it holds, in percent:

- **Burnt**: how much of its life is used. It rises with power and stays with the rod when you take it out and put it in elsewhere.
- **Condition**: how much damage the rod has taken from heat and overpower ([Fission Reactor](Fission_Reactor.md#misuse-and-hazards)). A damaged rod makes less power and never recovers.
