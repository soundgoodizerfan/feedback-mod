# Arc Furnace

An electric furnace in two blocks: an **Arc Hearth** that holds the melt, and an **Arc Head** that burns an electric arc into it. The arc reaches temperatures no fire does, hot enough to melt tungsten.

## Crafting

**Arc Hearth**: a ring of [Insulation](Insulation.md) blocks around a Large Crucible.

```
I I I
I L I
I I I
```

I is an Insulation block and L is a Large [Crucible](Crucible.md).

**Arc Head**

```
S W S
C S C
C   C
```

S is a Steel Plate, C is Ceramic and W is Copper Fine Wire.

**Electrode**: three bars of one material in a vertical column make an electrode of that material. A bar is a rod, an ingot or a lump. The material is graphite or any conducting metal.

## The Arc Hearth

A crucible four times over: **6 charge slots** and a tank of **5184 mB**, 36 ingots. It has no fire of its own, and it is far heavier than a Large Crucible, so it heats and cools slowly. Its screen, pouring into molds, hopper behavior and tapping work as on the [Crucible](Crucible.md).

## The Arc Head

Set the head against a [Crucible](Crucible.md) or an Arc Hearth, on top of it for the usual build. It faces the block it was placed against and burns into it. The head heats all four sides of that block as well as its face; see [Heat](Heat.md#heat-sources-have-a-facing).

- **Right-click with an electrode** to fit it. The head holds three.
- **Right-click with an empty hand** to take one back.

Jade on the head lists electrode slots 1, 2 and 3, each with its material and condition, or "empty".

Run an [EHV](Electricity.md#volts-and-amps) line to it. With three electrodes it draws **42 A at 384 V**; with fewer it draws less. The arc follows the power it actually receives, so a sagging line lowers it. A [Breaker](Breaker.md) in the line switches it. A [Dynamo](Dynamo.md) on its dense winding, driven by a [Gas Turbine](Gas_Turbine.md), supplies about 360 V at up to 42 A.

### How hot

The arc is capped by the **weakest electrode**: about 97% of the temperature at which its material sublimes (graphite, 3600 Tu) or boils (any conducting metal), and never above **3500 Tu**. A crucible or hearth under the arc climbs toward that, as it does under any fire ([Heat](Heat.md)).

## Electrodes

An electrode is a rod of a material that can carry the arc. Its tooltip shows the temperature its arc holds to and its condition.

- **Wear.** An electrode loses condition while the arc burns. The hotter the bath, the faster, steeply so: near an electrode's limit it wears many times faster than at steel's heat.
- **Snapping.** At 5% condition it snaps. The stump drops into the bath and the arc flashes.

## Hazards

- **Flash and splash.** A bath above **3000 Tu** that holds metal now and then splashes and flashes. Anything within 6 blocks takes heat damage, and a player without a helmet is blinded. A snapping electrode flashes the same way.
- **Boiling metal.** Molten metal in any vessel that is held past its boiling point boils away a share each tick, giving off a poisonous metal fume. The alloy drifts as it does: zinc, lead and tin go first. See [Hazards](Hazards.md).
- **Steel left hot** is ruined like any steel batch held past its spoil line ([Making Steel](Steel.md)).
- The line is EHV, the strongest shock band ([Hazards](Hazards.md#shock)).
