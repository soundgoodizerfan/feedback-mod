# Motor

Turns [electricity](Electricity.md) back into [rotation](Rotation.md). It's a [Dynamo](Dynamo.md) run backwards: the same block, the same two windings and the same figures.

## Crafting

Put a [Dynamo](Dynamo.md) in a crafting grid on its own. A Motor on its own turns back into a Dynamo.

## Placing

Place it **against the end of the shaft it should drive**. That face is its output. Any other face connects to [cable](Copper_Cable.md).

**Sneak-right-click with an empty hand** to switch between the **coarse** and **fine** winding.

## How it behaves

Two rules, both from the winding's volts-per-RPM figure (0.094 coarse, 0.375 fine):

- **Speed comes from voltage.** The motor drives its shaft toward **volts ÷ volts-per-RPM**, its no-load speed. On 1.5 V a coarse motor heads for 16 RPM and a fine one for 4 RPM. On 6 V, 64 and 16.
- **Force comes from current.** Every amp it's given pushes with the same figure in Su.

It asks for current in proportion to how far below its no-load speed it's running:

- An **idle** motor, already at speed, draws almost nothing.
- A **loaded** motor slows down, draws more, and settles where its current carries the load.
- An **under-powered** motor runs slow. It never stalls outright from low voltage.

At most it pushes **0.75 Su**, on either winding: its rated amps (8 coarse, 2 fine) times the winding figure. That's a very light load. A [Controller](Controller.md) alone draws 2 Su.

Two motors on one shaft share the load, and a motor can share a shaft with a [belt](Pulleys.md).

## Converting voltage

A motor turning a dynamo on the **other** winding is a **motor-generator set**, the way to change voltage in Feedback:

- **Fine motor → coarse dynamo:** MV in, LV out (fewer volts, more amps).
- **Coarse motor → fine dynamo:** LV in, MV out.

The only loss is the shaft's friction. See [Electricity](Electricity.md#changing-voltage).

## Notes

- More voltage than it's built for only makes it spin faster. Nothing wears it. If that's a problem for what it drives, put an [Overspeed Trip](Overspeed_Trip.md) on the shaft.
- It only drives in one direction.
