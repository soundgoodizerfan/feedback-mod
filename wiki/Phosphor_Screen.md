# Phosphor Screen

A sealed glass envelope coated inside with glowstone dust, which glows where a signal lands. Pair it with an [Oscilloscope](Instruments.md#oscilloscope).

## Crafting

A Glass and two Glowstone Dust, in any order.

## Sealing it

Set it on a [Firebox](Firebox.md) and let it heat. Pinching off below **150 Tu** seals nothing worth having. **Right-click with an empty hand** to pinch the envelope off. The gas sealed inside contracts as it cools, which makes a partial vacuum: **the hotter you seal it, the better the vacuum**. A [Pressure Gauge](Instruments.md#pressure-gauge) reads it (negative is vacuum). It glows only when it is sealed and the gauge reads below about -0.3 Pu.

## Using it

Put an [Oscilloscope](Instruments.md#oscilloscope) against it. The screen has four brightness levels and gives light to match.

## The trace

A screen that is sealed, evacuated and driven by an oscilloscope draws the scope's last 32 current samples as a glowing green line across the inside of the glass, oldest at the left and newest at the right. The line fades toward the old end and glows as bright as the screen itself, so a higher current rides higher. It faces you from whichever side you look. A screen that does not glow shows no trace.

## Overrun

- **Reheating a sealed envelope** past the heat it was sealed at turns the vacuum into pressure (gas pressure rises with absolute temperature, the vacuum being what was left when it was sealed hot). Once the gauge reads above **0.1 Pu** each tick may shatter it, and above **0.3 Pu** it certainly does; and past the glass's service temperature it cracks outright. Nothing is returned.
- **Thermal shock**: rain on a hot envelope, or water in a block beside it, quenches it toward the surrounding temperature within seconds. Each second in which it cools by more than the glass's **shock limit** ([Thermal shock](Heat.md#thermal-shock)) crazes it, a bigger drop crazing it more. Once crazed through it shatters and returns nothing. Heating it on a [Firebox](Firebox.md) never counts, and a slow cool-down stays under the limit.
- **Burn-in**: a screen held at full brightness burns, and each third of burn costs the brightest level it can show, for good. A fully lit screen is dead in about **8 minutes**, and loses a level about every 3. Do not leave a maxed signal on it.
