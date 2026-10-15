# Crucible

An open brick pot that sits over a [Firebox](Firebox.md) (or lava). It's where you make steel, melt metal and mix alloys, and it's the one hot vessel you can **measure and automate**.

## Crafting

**Small Crucible**

```
B   B
B B B
```

**Large Crucible**

```
B   B
B   B
B B B
```

B is a Brick.

## Small vs. large

The only difference is **weight**: a large crucible is ten times as heavy. A small crucible closes most of the gap to its fire in about 2 seconds, overshoots easily, and is hard to hold steady. A large one takes about 20 seconds but holds a temperature almost on its own. Both settle at the same temperature on the same fire. Some processes (steel especially) have a *max rate*, and a small crucible on a full fire may simply climb too fast to satisfy it. See [Heat](Heat.md).

A [Refractory Lining](Refractory_Lining.md) makes a small crucible behave more like a large one.

## Using it

- **Right-click with an item** to put it in. It holds **two** items.
- **Right-click with an empty hand** to take the last one back out. It comes out **at the crucible's temperature** and starts cooling at once.

Whatever is inside sits at the crucible's temperature. The crucible doesn't know what you're making. It just gets hot, and the items do whatever they do at that temperature (see JEI's *Thermal process* pages).

### Making things

Put in the ingredients and hold the temperature inside the band. For example, an **Iron Ingot + Charcoal** becomes a **Steel Ingot**. See [Making Steel](Steel.md). Let it get too hot and the batch is **ruined** (iron becomes *Burnt Iron*).

### Melting

Ores, raw metal and ingots melt into the crucible's **tank** once they're hot enough. Fill an [Ingot Mold or Bullet Mold](Casting.md) by right-clicking the crucible with it. Melt two metals in the right proportions and you get an alloy. See [Casting and Alloys](Casting.md).

The tank holds **1296 mB**, nine ingots' worth (144 mB an ingot). If it's full, extra metal that melts is lost.

## Building around it

The crucible notices what's built against it:

- **[Insulation](Insulation.md)** blocks against its sides help it hold heat.
- **[Dampers](Damper.md)** against its sides let you dump heat on demand.
- A **[Bimetallic Strip](Bimetallic_Strip.md)** against it makes a simple thermostat.
- A **[Temperature Sensor](Temperature_Sensor.md)** on any face lets a [Controller](Controller.md) read it.
- A **[Powered Bellows](Powered_Bellows.md)** goes against the **firebox**, not the crucible.

## Reading it

Jade shows how hot it is in words, or as a figure with a [Thermometer](Thermometer.md) in your inventory. It also shows how many sides are insulated and what's inside.

Because it's open, a crucible can always be watched. The sealed furnaces can't; see [Furnaces](Furnaces.md).
