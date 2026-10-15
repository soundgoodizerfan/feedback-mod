# Programmer

The desk where you write a program for a [Controller](Controller.md). You lay out cards on a canvas, wire them together, and **print** the result onto a **Punch Card**.

## Crafting

**Programmer**

```
W P W
W S W
W W W
```

W is any Planks, P is Paper and S is a [Shaft](Shaft.md).

**Punch Card**: two Paper side by side make **four** blank cards. Blank cards stack; printed cards don't.

## Opening it

Right-click the Programmer. Put a blank Punch Card in its slot.

## The cards

| Card | In → Out | What it does |
| --- | --- | --- |
| **Read Sensor** | → number | The current reading of one linked sensor. Linked to a switchable block (clutch, valve, damper, breaker, engine) it reads **yes** while that block is on. Linked to a [Driver's Seat](Vehicles.md#the-drivers-seat) it reads one key, 0 to 1: click the card's second row and press the key (default **W**). |
| **Greater Than** | number, number → yes/no | Yes if the left is bigger than the right. |
| **Less Than** | number, number → yes/no | Yes if the left is smaller than the right. |
| **AND** | yes/no, yes/no → yes/no | Yes only if both are yes. |
| **OR** | yes/no, yes/no → yes/no | Yes if either is yes. |
| **Math** | number, number → number | + − × ÷, min or max. Click the middle to cycle; the right input can be a typed number. For vehicles. |
| **Flip Clutch** | yes/no or number → | Switches one linked block: on for yes, off for no. Works on clutches, valves, dampers, engines and breakers. A number only moves a vehicle part, such as an engine's throttle. |

The second input of *Greater Than* / *Less Than* can be left unwired and given a **number** instead. That's how you write "temperature less than 1380".

A program can have up to **16** cards.

Old printed cards with an AND on them still work as AND.

## Controls

- **Right-click** the canvas: add a card.
- **Drag from a pin** to another pin: wire them.
- **Alt-click** a wire: cut it.
- **Middle or right-drag**: pan. **Scroll**: zoom. **Home**: fit everything in view.
- **Ctrl+Z / Ctrl+C / Ctrl+V**, **Del**: undo, copy, paste, delete.

## Linking cards to real blocks

*Read Sensor* and *Flip Clutch* cards have to be pointed at something in the world:

1. Right-click the sensor or block with a **[Data Connector](Data_Connector.md)**.
2. Open the Programmer and click the card's **link** button. It changes from *unlinked* to *linked*.

## Printing

Press **Print**. The program is checked first, and you'll be told if:

- a wire leads nowhere or an input has no wire,
- a sensor or switch card isn't linked,
- the program loops back on itself,
- a wire connects the wrong kind of signal (a yes/no into a number input), or
- two sensors being compared don't measure the same thing (temperature against current, say).

A printed card is **read-only**: *"Printed -- read only."* To change a program, print a new card.

**Close the Programmer only after printing.** Unprinted edits are lost when you close it.

## Example: thermostat with a damper

- *Read Sensor* (crucible temperature) → *Less Than 1380* → *Flip Clutch* (bellows clutch). This blows while it's too cool.
- *Read Sensor* (same sensor) → *Greater Than 1400* → *Flip Clutch* (damper). This vents when it's too hot.

To stop the bellows clutch flapping right at 1380, give it a band: see [Controller](Controller.md#holding-a-band).
