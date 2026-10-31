# Flight Sensor

A block that reads how its vehicle is flying and hands the figure to a [Controller](Controller.md), so an autopilot is only cards.

Place it on the vehicle. Its front is the way it faces; pitch and roll are measured about that.

**Right-click** steps what it reads:

| Reading | What it is |
| --- | --- |
| **Altitude** | Height above the bottom of the world, in blocks, to the nearest block. |
| **Airspeed** | How fast the vehicle is moving, in blocks per second, to the nearest half. There is no wind, so this is also its speed over the ground. |
| **Pitch** | Nose up (positive) or down, in degrees, to the nearest two. |
| **Roll** | Tilt to either side of the front, in degrees, to the nearest two. |

**Sneak-right-click** shows the current reading.

To use it, point a Data Connector at it and link an *Input* card on the [Programmer](Programming.md) to it. The card reads the sensor's current reading and carries its kind, so the program cannot compare altitude with speed. Link the card again after changing what the sensor reads. A card that points at it remembers it as an offset from the vehicle's frame, so the program still works after the vehicle is lifted or put back.
