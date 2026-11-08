# Electric Controller

A [Controller](Controller.md) powered from a cable instead of a shaft. It runs the same programs, with the same upgrades, but takes every [program medium](Programming.md#media): Punch Card, Circuit Board, Floppy Disk and USB Drive. A mechanical Controller and Programmer take the Punch Card only.

## Crafting

Controller + Circuit Board + Copper Wire, in any arrangement.

## Using it

Right-click with a medium to put it in; right-click with an empty hand to take it out. It evaluates only while the line delivers at least half of the current it wants; with less the outputs, variables and delay clocks hold, as a mechanical Controller's do on a dead shaft.

## Electric Programmer

A [Programmer](Programming.md) that takes every medium. Crafted from a Programmer, a Circuit Board and Copper Wire. **Erase** works on a printed Floppy Disk or USB Drive (see [Media](Programming.md#media)). It needs no power.

## Computer

An Electric Programmer and an Electric Controller in one block, with a desktop. Crafted from an Electric Controller, an Electric Programmer and a Silicon Ingot.

Right-click it to open the desktop. Down the left are **drive bays A to D** and a **RUN** slot, shown as icons: drag program media (Punch Card, Circuit Board, Floppy Disk, USB Drive) onto them from the hotbar along the top, and off again. Click an icon's label to open its **Drive window**:

- **Edit** opens the medium in the full [Programmer](Programming.md) editor; closing the editor puts the medium back in its bay and returns to the desktop.
- **Run** moves the medium to the RUN slot, where it runs while the Computer is powered (the Controller's limits and upgrades apply). **Stop** puts it back in a free bay.
- **Eject** takes it into your inventory.

The taskbar opens two more apps as windows, which you drag by their title bars:

- **Monitor**: the running program's variables and the last reading of each of its sensor cards.
- **Links**: the blocks the running program's cards are linked to, with their positions.

Right-click with a medium in hand puts it straight into the RUN slot; sneaking with an empty hand takes the running medium out. The editor works without power; the program runs only while the cable delivers.
