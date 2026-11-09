# Electric Controller

A [Controller](Controller.md) powered from a cable instead of a shaft. It runs the same programs, with the same upgrades, but takes every [program medium](Programming.md#media): Punch Card, Circuit Board, Floppy Disk and USB Drive. A mechanical Controller and Programmer take the Punch Card only.

## Crafting

Controller + Circuit Board + Copper Wire, in any arrangement.

## Using it

Right-click with a medium to put it in; right-click with an empty hand to take it out. It reads a sensor only through a path of linked nodes made with the [Data Connector](Data_Connector.md#linking-nodes); with no path the reading is 0, and the wire's grade sets how exact the reading is. It evaluates only while the line delivers at least half of the current it wants; with less the outputs, variables and delay clocks hold, as a mechanical Controller's do on a dead shaft.

## Electric Programmer

A [Programmer](Programming.md) that takes every medium. Crafted from a Programmer, a Circuit Board and Copper Wire. **Erase** works on a printed Floppy Disk or USB Drive (see [Media](Programming.md#media)). It needs no power.

## Computer

An Electric Programmer and an Electric Controller in one block, with a desktop. Crafted from an Electric Controller, an Electric Programmer and a Silicon Ingot.

Right-click it to open the desktop, a fixed panel drawn as a terminal. The colors, wallpaper and screen effects are set in the **Settings** window and are the same for everyone who opens this Computer.

Down the left are **drive bays A to D** and a **RUN** slot, shown as icons with a label under each; the selected one is highlighted. Your hotbar floats as a dock along the bottom of the desktop: drag program media (Punch Card, Circuit Board, Floppy Disk, USB Drive) from it onto the icons, and off again. Click an icon's label to open its **Drive** window, which shows a memory bar for the medium:

- **Edit** opens the medium in an **Editor** window on the desktop. It holds the same node editor as the [Programmer](Programming.md): cards, wires, the variables panel, **Print**, **Erase**, **Stamp** and **Save fn**. Closing the Editor window saves the edit and returns the medium to its drive.
- **Run** moves the medium to the RUN slot, where it runs while the Computer is powered (the Controller's limits and upgrades apply). **Stop** puts it back in a free bay.
- **Eject** takes it into your inventory.

### Windows

Drive, Editor and every app open as windows. Drag a window by its title bar and resize it by dragging its right or bottom edge. Each has a minimize button, a maximize button (**o**) and a close button (**x**). The focused window has the bright title. Windows stay inside the desktop area, clear of the drive icons and the hotbar dock.

Drag a title bar to the left or right edge of the screen area to snap the window to that half, or to the top edge to maximize it. Dragging a maximized or snapped window away restores its size. Double-click a title bar to maximize or restore.

With a window focused:

- **Alt+Tab** cycles through the windows (Shift reverses).
- **Alt+Left** and **Alt+Right** snap to a half.
- **Alt+Up** maximizes.
- **Alt+Down** restores, or minimizes a window that is already restored.
- **Ctrl+W** closes.

Right-click opens a menu. On a title bar or a task button: minimize or restore, maximize, snap left, snap right, close. On the wallpaper: Files, Settings, Next wallpaper, Show desktop. On a drive icon or a Files row: Open, Edit, Run or Stop, Copy, Eject, Erase (it asks first).

Short **toasts** appear in the top-right corner of the work area for about 6 seconds and vanish when clicked. They report an app disk inserted and Console events: a program started or stopped, a medium ejected, copied or erased, power on or lost, and refusals.

A bottom panel holds the **Menu** launcher, one task button per open window, and a tray. The Menu lists every app that is available, in two columns. Clicking the focused window's task button minimizes it; clicking another raises or restores it. The tray shows a lamp for each bay and the RUN slot, a power lamp, and the clock. The sliver at the far right of the panel minimizes every window.

### Apps

**Drive**, **Editor**, **Files**, **Settings**, **Monitor**, **Links** and **Console** are built in. Every other app comes on an **app disk**, a Floppy Disk that carries the app. An app runs while its disk sits in one of the four drive bays. Pull the disk and the app disappears from the Menu and its window closes; the disk can go into another Computer and the app is there. An app disk is not a program medium: the Computer will not edit, run or copy onto it, and a [Programmer](Programming.md) will not take it. In a Drive window an app disk shows that it runs while in a drive, and in the Library it shows its app name. The creative tab lists each app disk.

Each app disk is shapeless: one blank Floppy Disk plus one item.

| Item | App disk |
| --- | --- |
| Spyglass | Scope App Disk |
| Compass | Link Map App Disk |
| Book | Notes App Disk |
| Clock | Converter App Disk |
| Redstone | Calculator App Disk |
| Bookshelf | Library App Disk |
| Redstone Lamp | Power App Disk |
| Note Block | Arcade App Disk |
| Paper + Blue Dye | Blueprint App Disk |

#### Built-in apps

- **Monitor**: the running program's variables and the last reading of each of its sensor cards.
- **Links**: the blocks the running program's cards are linked to, with their positions.
- **Console**: a log of what the Computer did, newest at the bottom: programs started and stopped, things its program switched on and off, power lost and back, media ejected, edited or copied, and media it refused.
- **Files**: in the Menu. Lists drives A to D and RUN with what each holds. Click a row to pick it, double-click to open its Drive window. The buttons are **Open**, **Edit**, **Run** (**Stop** for the running medium) and **Eject**. With the Library app disk in a bay, drag a written program onto another drive row to copy it onto a blank Floppy Disk or USB Drive (the same rule as the Library's Copy), or onto the **Trash** box to erase it: it asks first, and the medium is left blank and reusable. Erase needs no Library disk. Without the Library disk, dragging and Copy are unavailable.
- **Settings**: see below.

#### Disk apps

- **Scope**: a trace for each sensor card of the running program, up to four, each scaled to its own range. **Pause** freezes the traces, **1x**, **2x** and **4x** set the time scale, **Clear** wipes them. Readings arrive a few times a second, so the traces are stepped.
- **Link Map**: a plan view with the Computer in the middle and every linked block around it. Inputs are squares, outputs are crosses, and a block in an unloaded chunk is drawn hollow. Hover a mark for the block, its position and its distance. Cards without a link are counted.
- **Notes**: a text pad of up to 2000 characters, saved on the block and the same for everyone.
- **Converter**: converts between units in three tabs. Metal covers block, ingot, Qu and mB of melt (1 Qu is a nugget, 9 Qu make an ingot, 81 a block, and molten metal is 16 mB per Qu). Volume covers bucket (1000 mB), ingot (144 mB) and mB. Time covers day (24000 ticks), minute (1200), second (20) and tick.
- **Calculator**: handles + - * / ^ and brackets, from the buttons or the keyboard. Enter or = evaluates, and `ans` recalls the last result.
- **Library**: every drive at a glance, each with **Open** and **Copy**. Copy writes a bay's program onto a blank Floppy Disk or USB Drive sitting alone in another bay; the blank medium is used up as the cost.
- **Power**: whether the Computer is powered, its voltage, current and what it wants (read to the precision of the instrument you carry), its voltage band, a bar showing how much of the band's voltage arrives, and a half-minute voltage trace.
- **Arcade**: Snake (arrows or WASD, space to start) and Minesweeper (click to dig, right-click to flag; 14 mines on a 14x8 field). The best Snake length and the fastest cleared field are kept on the block.
- **Blueprint**: the [Blueprint Table](Blueprint_Table.md)'s drafting board in a window, in the desktop's colors: draw designs from the types, pick parts, materials and shapes, read the predicted figures, compare and pin up to three drafts, and run the test rig. It cannot print onto paper and has no sample slot: carry a material item on the cursor from the hotbar dock and click a part to paint it. The window scrolls with Ctrl plus the wheel, or by dragging the strip down its right edge.

### Settings

- **Phosphor**: the screen color, Green, Amber, White or Blue.
- **Wallpaper**: Grid, Scans, Circuit, Horizon (a retro perspective plain) or Blank. **Next wallpaper** in the wallpaper's right-click menu steps through them.
- **Screen effects**: Scanlines, Glow, Rolling band (a slow bright band down the glass) and Boot line (a bright line that grows to fill the screen as it powers up), each on or off.
- **Desktop widgets**: Clock (the time with the day count), Scope (traces the running program's first sensor reading) and Memory (a bar per drive), each on or off.
- **Icons right**: moves the drive icons to the right side; it takes effect when the desktop is reopened.

Right-click with a medium in hand puts it straight into the RUN slot; sneaking with an empty hand takes the running medium out. The editor works without power; the program runs only while the cable delivers.
