# Slider Joint

A micro-block joint that lets a body slide along its front axis against a spring. It is a suspension: a wheel on a slider rides over bumps and rebounds.

## Crafting

```
I
B
S
```

I is a Wrought Iron Plate, B is a [Rotary Bearing](Rotary_Bearing.md) and S is a [Shaft](Shaft.md).

## Using it

It works only as a [micro-block](Micro_Core.md). Placed as a full-size block it says *"It only works as a micro-block: put it in a micro cell."*

1. Put it in a micro cell with its front along the line the body should slide on.
2. **Sneak-use** it with an empty hand to choose the spring: **soft**, **stock** or **stiff**. The message reads *"Choice N of 3."* Choose before assembling. The spring can also be made of a metal; see [Spring from a metal](#spring-from-a-metal).
3. **Use it with an empty hand** to assemble everything joined to its front face into a second body that slides.
4. **Use it again** to put the body back, at the nearest of the 24 ways a cube can be turned.

## How it works

The body rests where the spring holds it. A load pushes it back along the axis and the spring pushes it out again. A soft spring squats further and bounces slowly; a stiff one holds the body higher and tighter.

## Spring from a metal

A Slider Joint plus four wires of one metal, in any arrangement, makes a slider with that metal as its spring. The tooltip shows the spring. When the slider is placed, the metal's yield strength sets the band: under 4 is soft, 12 and over is stiff, anything between is stock. Sneak-use still cycles the band.

## Wear

A slider held bottomed out for 5 seconds or more wears the joint. See [Wear](Vehicles.md#wear).

## See also

- [Piston Joint](Piston_Joint.md): slides to a level a card sets instead of by a spring.
- [Vehicles](Vehicles.md#joints): the other joints.
