# Chemicals

The gases and liquids of the chemical chain: hydrogen and oxygen from the [Electrolyser](Electrolyser.md), brine, lye, acids, fluorine compounds, bleach, nitrogen, ammonia and the molten cryolite bath. Gases are stored in a [Gas Tank](Gas_Tank.md), carried by a [Pipe](Pipe.md) and compressed by a [Gas Compressor](Gas_Compressor.md). Most are made in the [Reactor](Reactor.md).

## Hydrogen

A very light, flammable gas. The Electrolyser makes it from water.

- **Fuel.** It burns at a flame temperature of **2200 Tu**, hotter than any fuel in [Fuels](Fuels.md). A [Fluid Burner](Fluid_Burner.md), [Gas Turbine](Gas_Turbine.md), [Engine](Engine.md) or [Thruster](Thruster.md) burns it like any other fluid that reaches it.
- **Light.** It weighs very little for its volume.
- A flammable gas in a hot line is a [hazard](Hazards.md).

## Oxygen

A gas that makes fire burn harder and that a lung can breathe at full strength. The Electrolyser makes it from water.

- **Oxidizer.** Oxygen carries oxygen of its own, so a fuel burned with it burns as if blown on. See [Fuels](Fuels.md).
- **Breathable.** Its oxygen share is 1: all of it is usable. Open air has about a fifth of that. A [Culture Dish](Sculk_Culture.md) breathes it at full rate.

JEI's Material Stats page lists the figure as **Oxygen share**.

## Brine

Salt water. Dissolve Salt in water in a [Reactor](Reactor.md). The [Electrolyser](Electrolyser.md) splits it into hydrogen, chlorine and lye.

## Lye

A strong base. It is left in the Electrolyser's cell when brine is split; draw it off a side face. It dissolves bauxite in the Reactor ([Making Aluminum](Making_Aluminum.md)), turns chlorine into bleach, and can serve as the electrolyte of an [Electrolytic Cell](Electrolytic_Cell.md). It bites vessels that are not lined for it ([Hazards](Hazards.md)).

## Chlorine

A heavy toxic gas from the anode (front) of an Electrolyser running on brine. It is the feed for hydrochloric acid and bleach. A full electrode tank with nowhere for it to go seeps out and poisons anyone nearby.

## Acids

**Hydrochloric Acid** is made in a Reactor from hydrogen and chlorine. **Sulfuric Acid** is made from sulfur dioxide, oxygen and water over a catalyst bed. Both work as the electrolyte of an Electrolytic Cell, where an ore item hung as the anode dissolves as the metals it bears.

## Sulfur dioxide

A heavy toxic gas. Roasting pyrite in a Reactor gives it, with iron nuggets.

## Hypochlorite Bleach

Made in a Reactor from chlorine and lye. Dip a block of dyed wool in a vessel holding it, the way a ceramic is [glazed](Casting.md#glazing), and it comes out white wool; each block takes 250 mB.

## Nitrogen

The inert part of air. A Reactor separates it from air over a catalyst bed. It is the feed for ammonia.

## Ammonia

A toxic gas made in a Reactor from nitrogen and hydrogen over an iron bed. With sulfuric acid it makes [Fertilizer](Fertilizer.md).

## Fluorine and PTFE

**Hydrofluoric Acid** comes from Cryolite Ore and sulfuric acid in a Reactor. Hydrofluoric acid, chlorine and ethylene, hot and under pressure over a cracking catalyst bed, give **Tetrafluoroethylene** gas and hydrochloric acid. Tetrafluoroethylene held at 330 to 450 Tu under pressure over a catalyst bed polymerizes into **Molten PTFE**, which [casts](Casting.md) like any molten metal. Rates and windows are in the [Reactor](Reactor.md#the-reactions) table; the finished polymer is on [Materials](Materials.md#polymers).

## Cryolite bath

Cryolite Ore melted in a Reactor at 1010 Tu gives this molten liquid. At 950 Tu or hotter it is the solvent in which an Electrolyser splits alumina into aluminum. It is not used up. See [Making Aluminum](Making_Aluminum.md).
