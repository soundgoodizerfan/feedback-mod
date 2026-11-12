# Sensors

Five blocks that look at the face they were placed against and report what they see to a [Controller](Controller.md). They sense and control nothing. Right-click one with an empty hand to read it as a percentage.

| Sensor | Reads | Reads in |
| --- | --- | --- |
| Ripeness Probe | how grown the crop in front is, from the crop's own age | quarters |
| Photocell | the brighter of daylight and block light in front | fifths |
| Presence Plate | whether a creature or a dropped item stands in front | on or off |
| Fill Gauge | how full the inventory or tank in front is | eighths |
| Observer | a change in the state of the block in front, for half a second after it | on or off |

## Using them

Place a sensor with its face against what you want to watch, then link it to a Controller with a Sensor card. An Electric Controller or Computer reads it only down a wire: join the sensor to it with the [Data Connector](Data_Connector.md#linking-nodes), which costs fine wire by distance. The reading is quantized: a probe reports a crop as unripe, a quarter, half, three quarters or ripe, never a finer figure.

A Ripeness Probe reads any block that has an age, so a modded crop works too. A Photocell reads its own lamp's light, so a lamp it switches can switch it back; keep the lamp out of its sight. An Observer reports a change a moment late. It is the Feedback version of the vanilla observer: a vanilla observer found in the world turns into an Observer. Where vanilla's observer pulses redstone, a controller card reads this one.

A Ripeness Probe, Photocell, Presence Plate or Fill Gauge can also be seated in a [Pivot Mast](Pivot_Mast.md) as a sensing head, reading the cell under the arm tip.

## Crafting

```
P X P
R C R      P wrought iron plate, R redstone, C copper plate
```

| Sensor | X |
| --- | --- |
| Ripeness Probe | wheat seeds |
| Photocell | glass |
| Presence Plate | string |
| Fill Gauge | glass pane |
| Observer | feather |
