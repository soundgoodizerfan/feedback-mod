# Pneumatic Tube

Sealed tubes that move [carriers](Carrier.md) by air pressure. Connected tubes form one run with one shared supply of air; a [Gas Compressor](Gas_Compressor.md) beside the run pressurizes it, and every hop spends a little air.

## Crafting

**Pneumatic Tube** (makes 3): three Copper Tubing in a row (two stacked [Copper Plates](Mechanical_Hammer.md) make two Tubing).

**Lined Tube**: a Pneumatic Tube between two Wrought Iron Plates, top and bottom.

**Wide-Bore Tube** (makes 3): two rows of three Steel Plates with a gap between.

```
S S S

S S S
```

**Evacuated Tube**: a Wide-Bore Tube between two Steel Plates, top and bottom.

## Using it

Tubes that touch join into one run. **Right-click** a tube with an empty hand to read its run: length, air held and pressure in Pu.

- A run needs pressure to move anything. Below about a third of a Pu a capsule sits still until pressure returns. More pressure moves capsules faster, up to a ceiling.
- A heavy load slows a capsule, a bend slows it more, and a capsule too heavy for the bore stalls.
- A run held over **5 Pu** can burst, the same way a [pipe run](Pipe.md#gas-and-pressure) does.
- A capsule blocked by a full tube ahead holds the capsules behind it.

## Kinds

- **Pneumatic Tube**: the narrow bore.
- **Lined Tube**: a smooth liner cuts wall friction, so capsules move half as fast again at the same pressure.
- **Wide-Bore Tube**: passes [pods](Carrier.md#pod) and far heavier capsules. It joins narrow tubes freely, but a pod cannot enter a narrow one.
- **Evacuated Tube**: a wide-bore tube sealed for vacuum. While a [Vacuum Pump](#vacuum-pump) holds the run under 0.05 Pu, capsules coast through at three times the plain speed with no air spent. Seals leak, so the pump must keep running; with the vacuum lost the tube behaves like a lined wide tube.

[Junctions](Tube_Junction.md) split a run by address, and a [Tube Station](Tube_Station.md) is the door where items go in and come out.

## Vacuum Pump

A shaft-driven pump that draws air out of the tube run it faces, with its shaft across the flow. Faster shaft, deeper vacuum. It loads the shaft like any [pump](Pump.md).

Crafting: Pump + Pneumatic Tube.

## Linear Motor Ring

A coil ring placed against a tube block. Fed with current (2 A at MV), it drives capsules that carry a magnetic plate through that block at twice the speed. Plain capsules ignore it. See [Carrier](Carrier.md#magnetic-carrier) and [Electricity](Electricity.md).

Crafting: eight Copper Wire in a ring.

```
W W W
W   W
W W W
```
