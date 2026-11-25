# Soldering Iron

The hand route to a joint: patience instead of a [Reflow Oven](Reflow_Oven.md).

## Crafting

```
C
I
S
```

C is a Copper Rod, I a Wrought Iron Rod and S a Stick.

## Using it

1. **Right-click a fire** (a [Firebox](Firebox.md), say) with the iron to heat it. It keeps its heat and cools slowly, so a [Thermometer](Instruments.md#thermometer) will tell you where it is.
2. Hold a pasted [Trace Board](Trace_Board.md) in the other hand and **right-click**. A soldering screen opens with a row of pads, one pair per mounted part (two on a plain board).
3. **Hold the mouse button and drag the tip along the row**, first pad to last, then let go. Each pad is graded on how hot it got (see [Solder](Materials.md#solders)).

Heat matters as much as pace: a part whose pads run hotter than that kind of part can take is **cooked**. It stays on the board but switches nothing, and the [Component Tester](Instruments.md#component-tester) reports how many parts are cooked. A sealed tube gives first; a relay or a coherer stands the most. The board still works with one element fewer. A lifted trace is different: it still loses the whole board.

Pace is the skill. Go too fast and a pad stays **dull**: a cold joint. Linger and it goes **dark**: the flux burns, and if it keeps cooking the trace lifts. A **shiny** pad is a good joint. The iron cools as it works, so later pads want a slower pace; leave the screen (button up) to reheat at a fire and start again. Staying off the trace bridges solder to the next pad: it adds noise to a circuit, nothing worse. Close the screen with the button held and the tip counts as resting on the last pad for a while. A [Thermometer](Instruments.md#thermometer) on the iron gives the numbers. The iron never wears. Leaded solder poisons you while you work it.

## Reworking a circuit

A finished [Basic Electronic Circuit](Basic_Electronic_Circuit.md) remembers the board it was soldered from, so a part can be lifted back out with a [Wick](#wick).

1. Hold the iron in one hand and the circuit in the other, with a wick anywhere in your pack, and **right-click**. The soldering screen opens in lifting mode. Without a wick the iron does nothing.
2. **Hold and drag along the pads** as when soldering. Pads held in the good window melt their joints and soak into the wick.
3. A part comes free when **both** its pads went wet. It returns as the item you mounted, unless its pads ran hotter than that part can take: a cooked part is lifted too, but comes back as nothing.
4. The circuit becomes a bare Trace Board with the solder taken up and any parts you left on it, ready to be pasted and soldered again. Each part lifted spends one use of the wick.

A scorched pad lifts the trace, and the board is lost as in soldering. Leaded solder poisons you while you lift it, as while you solder.

## Wick

A braid of copper wire that soaks molten solder off a joint. It lifts 8 parts before it is used up.

```
W
```

W is [Copper Wire](Wire_Drawer.md). Craft it in the crafting grid.
