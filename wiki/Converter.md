# Solid-State Converter

Steps a line's band down one, down two or up one, with a Converter Tap on the other side. It changes band with no moving parts, at about 95 percent efficiency.

## Crafting

```
C E C
A A A
```

**Solid-State Converter**: C is a [Copper Heating Coil](Heating_Coil.md), E is a [Basic Electronic Circuit](Basic_Electronic_Circuit.md) and A is an Aluminum Plate.

```
C W C
A A A
```

**Converter Tap**: C is a Copper Heating Coil, W is a [Cat's Whisker Detector](Cats_Whisker_Detector.md) and A is an Aluminum Plate.

```
A A
A A
A A A
```

**Heatsink**: A is an Aluminum Plate.

## Using it

1. Wire the line you step **from** to the converter.
2. Set a **Converter Tap** against the converter and wire the line you step **to** to the tap.
3. **Right-click the converter with an empty hand** to cycle its step (a strip under the crosshair lists the options with the current one lit): *down one band*, *down two bands*, *up one band*. A step that would leave the list of bands (down from LV, up from Trunk) gives nothing.

The two lines never share a run: the converter is a load on the first, and the tap is a source on the second. A tap with no converter beside it supplies nothing.

## How it works

The converter passes a fixed amount of power, so a lower band out means more amps out, and a higher band out means fewer. Asked for more than that, it gives no more. A starved input line shrinks the output in proportion.

About 5 percent of the power is lost, and the loss is **heat in the converter block**. A hot converter loses condition, down to a floor, and a worn one wastes more of what passes. It never refuses a line and is never destroyed.

### Heatsinks

Set up to four aluminum **Heatsinks** against the converter. Each sheds heat to the air, so the converter runs cooler under load.

## See also

- [Electricity](Electricity.md#changing-band) for the other way to change band, a motor-generator set.
- [Substation](Substation.md) for a converter as the head of a branch.
