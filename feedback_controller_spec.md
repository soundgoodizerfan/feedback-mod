# Feedback — Controller Spec

> **A working document, not one of the three.** Replaces `controller_spec.txt`, which was three
> lines and a table and did its job for exactly as long as it took to have the real conversation.
> This document is that conversation, written down.
>
> It feeds the other two documents rather than replacing them, the same relationship
> `feedback_slice_01.md` has to them. The rules it settles belong in **document 2 (mechanics)**;
> the blocks and items it names belong in **document 3 (content)**. Where this document and the
> philosophy disagree, the philosophy is right and this document is wrong — nothing here should
> ever need to say that, since philosophy §13 is the reason this whole design holds together, not
> an obstacle to it.
>
> **Tier 1 (Punch Card) is built.** `control/program/` (the graph model and evaluator),
> `control/controller/`, `control/programmer/`, and the Punch Card item all exist, along with the
> small widenings §2.5, §3, and §5 called for on `SensorFitting`, `DataLinkManager`, and
> `ClutchBlockEntity`. Tiers 2-4 (Circuit Board, Floppy Disk, USB/ender-networked) are still just
> this document — see §4's closing note. Where this document says something is still open or
> unbuilt, that is current, not stale.

---

## 1. What a controller is allowed to do, and why that's the whole design

Philosophy §13: **the controller is a switch, never a dial.** Its only output is starting or
stopping a supply. That rule is the constraint this whole spec is downstream of, and it is worth
stating as a hard boundary rather than a starting point:

**A controller may read sensors, compare numbers to numbers, combine booleans, and cut power. That
is the complete list. It may never do anything else** — no counting, no timers of its own, no
storage beyond what a single evaluation needs, no loop, no jump, no "next instruction." If a
proposed card would let a program remember something across evaluations, or branch control flow
rather than compute a value, it belongs to a different device, not this one.

This is a deliberate, permanent ceiling, not an early-game limitation lifted later. Every tier in
§4 gets a better *medium* and a better *power source*. None of them get a bigger instruction set.
That is what keeps this a controller and not a general computer — see §6 for why that distinction
is being drawn on purpose.

## 2. The program is a graph, not a list

The first shape considered — and rejected — was a linear chain of steps, the way
PneumaticCraft: Repressurized's drone `ProgWidget`s connect (see §7). That shape needs control
flow to express a condition at all: PNC's condition widgets work by *jumping* to a labelled
widget elsewhere in the chain depending on how they evaluate. A jump is exactly the kind of
power §1 rules out.

What the mockup actually draws is a **dataflow graph**: small typed nodes, wired together by
their inputs and outputs, evaluated by dependency rather than by sequence. Nothing jumps.
Nothing repeats. A node's output is a pure function of its inputs, and the graph as a whole is a
pure function of every sensor it reads. That is expressive enough for "cut the bellows above
1450 Tu, unless the crucible's also below half load" without ever needing a single instruction
pointer.

### 2.1 The type system is exactly two types

- **Number** (green in the mockup) — a float. What a sensor reads, and what a comparison consumes.
- **Boolean** (red in the mockup) — on/off. What a comparison produces, what a logic gate consumes
  and produces, and what an actuator card consumes.

Two types, not one generalized "value." A port's colour *is* its type, checked at wiring time —
you cannot wire a number output into a boolean input, the same way you cannot plug the wrong
shape of Fitting into a side. Keeping the palette to two types is also what keeps port shapes
enumerable (§2.2) instead of needing a real type system.

**Built, one level more specific than the paragraph above.** A Number port isn't just "a float"
in practice — it's a float *of some `Quantity`* (Tu, Su, RPM, Fu; `instrument/Quantity.java`,
already built for instruments and reused rather than duplicated). `SensorNode` resolves and
carries its linked `SensorFitting`'s `Quantity` at link time (the first quantity
`Instrument#canRead` answers yes to — every fitting so far reads exactly one), and
`ProgramGraph#wireError` refuses two mismatched-quantity sensors feeding the same
comparator (the screen checks it as a wire is drawn, the server again on every commit), the same rejection-not-crash spirit as §2.4's cycle check. This is what makes the
joke land: a Number port isn't fully generic, so the type system quietly has more than two
members once you count what a Number actually *is*. It's still exactly two colours in the GUI
(port dots are tinted per-`Quantity` instead, so the type is visible without being a third
top-level colour) — §2.2's shape table doesn't grow, only what "number" means underneath it.

### 2.2 Port shapes enumerate the node kinds

Every node's shape — how many ports it has, of which colour, on which side — announces what
kind of node it is. Left is input, right is output, and these are (so far) the only shapes that
exist:

| Inputs | Output | Kind | Example |
| :--- | :--- | :--- | :--- |
| none | 1 number | **Number source** | `Sensor A` |
| none | 1 boolean | **Boolean source** | a redstone-in card, eventually |
| 2 number | 1 number | **Math** | not in the Tier 1 catalogue yet |
| 2 number | 1 boolean | **Comparator** | `Greater Than`, `Less Than` |
| 2 boolean | 1 boolean | **Logic gate** | `AND` |
| 1 number *or* 1 boolean | none | **Actuator (terminal)** | `Switch` |

This table is not the card catalogue (§5) — it's the closed set of *shapes* a card is allowed to
have. A new card is always one of these six rows; a card that needed a seventh shape would be a
sign the type system in §2.1 needs to grow, which is a real design event, not a card being added.

### 2.3 A numeric input port has an inline default

`Greater Than`'s second port is drawn two ways in the mockup: an underlined blank on the node
itself (a literal the player types straight in), and, alternatively, wired from a free-floating
`1250Tu` box. Both are the same port. **An unwired numeric input uses its own typed literal; a
wired one ignores the literal and uses the wire.** Pulling the literal out into its own node is
never required — it exists so the same constant can feed two different comparisons without
retyping it, nothing more.

Boolean inputs don't get this — there is no sensible "default" for on/off that isn't itself a
design decision, so a boolean input must always be wired.

### 2.4 The wiring rules

- **One wire per input, always.** An input port is fed by exactly one source: a wire, or (for
  numeric ports only) its own literal. Two wires into one port is not a "last one wins" —
  it's simply not a legal graph, the same way a fitting can't hold two sensors on one side.
- **One output can feed many inputs.** A sensor's reading, or any node's result, can fan out to
  as many downstream ports as the graph needs.
- **No cycles.** A graph where following wires forward ever returns to a node already visited is
  rejected outright — there is no sensible value for a node whose input depends on its own
  output, and this design deliberately doesn't reach for a "hold last tick's value" answer to
  make cycles meaningful (that is a real feature — a flip-flop, a latch — but it is *state*, and
  §1 already ruled state out). Rejected at print time, the same spirit as PNC's `addErrors()`
  catching a bad graph before a program is usable — see §7.

### 2.5 Evaluation is pushed, not polled

The graph does not run every tick. It runs when a sensor it depends on actually changes —
and "changes" means the *quantized* reading moves, not the raw float. A crucible drifting from
1441 to 1444 Tu at a 10 Tu sensor resolution is not a change; crossing to 1450 is. This is the
same discipline philosophy §8 already applies to every instrument in the game (a reading is
never more precise than what was paid for), just extended to mean "and therefore it doesn't
fire an update either."

**[OPEN, resolved]** Built as a plain per-tick comparison living in `ControllerBlockEntity`
itself, not on `SensorFitting`. Each tick the Controller is turning, it re-reads every `Read
Sensor` card's raw value and compares the whole set against what it read last tick; only on an
actual difference does it call `ProgramEvaluator.evaluate` and push new actuator states. This
needed no new machinery on `SensorFitting`/`Fittable` and no wake-up signal threaded through the
pull-only `DataNode` graph — the cost is that the comparison runs "per controller per tick"
rather than "pushed once at the source," which is cheap enough (a handful of floats, one program
of at most `FTuning.CONTROLLER_MAX_NODES` cards) not to matter. The player-visible behaviour is
exactly what this section describes either way.

`SensorFitting` did gain one small thing this needed: a `float readRaw()` next to `readValue()`,
both built on the same quantised figure (§5 named this gap; `TemperatureSensorFitting` now
computes the quantised value once and has `readValue()` call `readRaw()` rather than the other
way around).

## 3. Actuators have to become linkable too

A `Switch` card is an actuator (terminal) node — a boolean in, nothing out. It needs to
*reach* a real clutch, breaker, valve, or damper in the world, the same way `Input`
reaches a `SensorFitting` — through the data-link graph in `control/data/`.

Today, `Switchable` (`setEngaged`/`isEngaged`) and `DataNode` are unrelated interfaces. This spec
widens that: **a `Switchable` block entity should also be a `DataNode`**, unsided or sided
depending on what it physically is (a standalone block like the Timer is unsided, the same shape
as the Debug Controller; something that mounts as an Adapter fitting would be sided). That gives
the data-connector one graph that already does both directions — sensor-to-controller and
controller-to-actuator — instead of inventing a second linking mechanism for outputs.

This is the same "widen, don't replace" move already made once this session for
`ThermalBody.hasThermowell()` → `Fittable.canMount` (see `TODO.md` §4d as of `50b93c6`). `Switchable` keeps its
own two methods exactly as they are; it just also answers `DataNode`'s questions.

**Built, with one refinement over the paragraph above.** `ClutchBlockEntity` now implements
`DataNode` (unsided, per the Timer/Debug-Controller shape), so `DataNode.resolve` can find one
as a `Switch` card's target. But the actual wiring turned out not to need the data-connector's
two-click *link-completion* flow at all — a card carries its own `DataNodeRef` directly (§5's
"the wiring *is* the reference," taken completely literally), so **the Controller block entity
itself is not a `DataNode`** and never calls `DataNode#linkTo`. Instead:

1. The player right-clicks the real sensor or clutch with the existing Data Connector item. Its
   `useOn` already calls `DataLinkManager.onNodeInteract`, which — finding nothing already
   pending for that player — just records the selection. No new code needed for this half.
2. In the Programmer, **placing** a Sensor/Actuator card checks for a pending selection first and
   consumes it immediately if it fits (`ProgrammerBlockEntity#addNode` → `#resolveLink`) — the
   ordinary case (select in the world, *then* open the Programmer and click the palette) needs
   only the one click it looks like it should. Clicking an already-placed card's body sends the
   explicit `Link` action, the same `#resolveLink` check, for placing a card before selecting
   anything or relinking one afterward. Both paths call `DataLinkManager.takePending(Player)`,
   never `peekPending` alone — a mismatched pending selection (wrong `SensorFitting`/`Switchable`
   type) is left untouched rather than consumed, so a bad guess doesn't burn the player's
   selection.

A Clutch's `DataNode` link set is consequently never populated through this feature — it exists
only because the interface requires one to return. The genuine two-click link-and-persist flow
`onNodeInteract` implements is untouched, and still exactly what the debug controller uses.

## 4. The four tiers

From the original spec, kept intact — this is the axis-of-progression table, not a mechanics
change:

| Tier | Power | Rewrite | Program medium | Program lives on |
| :--- | :--- | :--- | :--- | :--- |
| Early | Su (mechanical) | Hard | **Punch Card** | separate Programmer block |
| Mid | Electrical | Hard | **Circuit Board** | separate Programmer block |
| Late | Electrical | Soft | **Floppy Disk** | separate Programmer block |
| End | Electrical | Soft | USB Drive / internal storage / ender-networked | the Controller itself |

**Hard-rewrite**: the program is printed onto the medium once. Changing it means discarding the
item and authoring a new one from scratch. **Soft-rewrite**: the medium takes rewritable media —
author again, print again, same item.

The progression is philosophy §14's kind, exactly: the instruction set never grows (§1), only
**iteration cost** falls — from "burn a card, forever" to "walk to a floppy drive" to "edit it from
where you're standing, and maybe pull someone else's program off an ender-network." Capability
never tiers here, only how expensive it is to be wrong.

**[OPEN]** The end-game "database of programs" and ender-networking mechanic. Named in the
original spec, not designed at all here — it implies a program-sharing capability that touches
`LICENSING.md`'s multiplayer-scope questions if it ever crosses between different players'
factories, and deserves its own pass rather than being sketched in passing.

**Only the Early tier (Punch Card) is built.** `ControllerBlockEntity extends RotationNode` and
draws a flat `FTuning.CONTROLLER_LOAD_SU` while turning, the same shape as the Bellows' flat
load — no motive force, no evaluation, and whatever the actuators were last set to simply holds
(the same failure character as a coasting clutch, not a snap to "off"). Mid/Late/End media
(Circuit Board, Floppy Disk, USB/ender-networked) are still just this table: the graph engine
(`control/program/`) and the Programmer's editing flow don't change between tiers, only the
medium item and whether printing is hard- or soft-rewrite — building them now would be exactly
the kind of infrastructure built *before* it's needed that CLAUDE.md warns against, not the kind
built *slightly* before.

## 5. Tier 1 card catalogue

The minimal set the mockup and philosophy §13 together require. Not exhaustive — more cards
(more sensors as more `SensorFitting`s exist, `OR`, `NOT`, math nodes) are expected later and
should slot into §2.2's shape table without changing it.

| Card | Shape | Notes |
| :--- | :--- | :--- |
| **Input** | number source, or boolean source | Names a linked `DataNodeRef` to a `SensorFitting`. One card per linked sensor, not a dropdown — the wiring *is* the reference. Linked to a `Switchable` instead (clutch, valve, damper), it reads that actuator's on/off state (`Quantity.STATE`) and its port is boolean: the "boolean source" row of §2.2, added 2026-10-13 for deadbands (philosophy §13) |
| **Greater Than** | comparator | Second input has an inline literal default (§2.3) |
| **Less Than** | comparator | Same shape as Greater Than, opposite test |
| **Logic** | logic gate | Both inputs must be wired (§2.3 doesn't apply to booleans). One `LogicNode` with a `Gate` op (AND, OR), cycled by clicking the card's middle as Math's operator is. OR added 2026-10-13: a deadband is `(T < low) OR (engaged AND T < high)`. The two gates were two palette cards until 2026-10-15, when playtesting asked for one card the way Math is one card |
| **Switch** | actuator | Boolean in. Targets a linked `Switchable`/`DataNode` (§3). Was *Flip Clutch* until 2026-10-15: it switches valves, dampers, breakers and engines too, and the old name read as clutch-only. *Read Sensor* became **Input** the same day |

Two implementation gaps this catalogue surfaced, both closed now:

- **`SensorFitting` had no raw-number accessor.** Fixed by adding `float readRaw()` next to
  `readValue()` (no `Quantity` parameter on the method itself — a `SensorFitting` only ever reads
  the one quantity it was built for), both built on the same quantised figure so a program can
  never see a number the player couldn't also read. The *card* does end up naming the quantity
  after all, just not through this accessor — see §2.1's port-typing addendum: `SensorNode`
  resolves and stores it separately, at link time, for wiring validation rather than evaluation.
- **`Switchable` needed widening to `DataNode`**, per §3 — done for `ClutchBlockEntity`.

All five cards above are built exactly as specced, evaluated by `ProgramEvaluator`
(`control/program/`) and authored in a Programmer's node canvas. `ComparatorNode` covers both
Greater Than and Less Than through one `Compare` enum rather than two record types, since §2.2
already treats them as the one comparator shape. A card's mandatory-but-unwired inputs (a
freshly placed card's ports, or a comparator's unused literal wire) use a shared
`ProgramNode.UNWIRED` sentinel rather than boxing every input in `Optional<Integer>` — simpler,
and `ProgramGraph#findError`'s existing "does every wire land on a real node id" check already
catches it as a dangling wire for free.

## 6. Why not just embed a scripting mod

Two real ones exist in this ecosystem and were both read before this design was settled.

**Super Factory Manager** (MPL-2.0, `ca.teamdman`) is a genuine domain-specific *language* — its
own grammar, parser, and even a VS Code language extension, built to solve item/fluid logistics
routing. That's a different problem (moving resources between inventories under conditions) from
this controller's problem (reading a physical quantity and switching a supply), and its
complexity is the complexity of a real compiler — precisely the ceiling §1 exists to stay under.
Read, not taken.

**Steve's Factory Manager**, the mod SFM2 is a spiritual successor to, was the first one raised
and is closer in *spirit* — a simpler, card-based, no-general-branching design. Its original
source (Vswe → gigabit101, pre-2016, ancient Forge) wasn't read: no LICENSE file on the branch
that would matter, and the code predates block entities and capabilities as they exist now, so
even a clear licence wouldn't have made it architecturally useful. ("Steve's Factory Manager
Reborn," a later continuation, is GPLv3 — but that's a different, newer codebase than the
original concept being pointed at.)

**PneumaticCraft: Repressurized**'s `ProgWidget` drone-programming system (GPL-3.0, matching
this project's own licence exactly) is what actually shaped this document — see §7.

## 7. What was taken from PneumaticCraft: Repressurized, and what wasn't

Read at `../pnc-repressurized`, branch `1.21` (which is Minecraft 1.21.1 — checked before
cloning, learned the hard way earlier this session not to assume a default branch matches).
GPL-3.0, code and assets both, per its own `LICENSE` file — the most permissive-to-us licence
read all session, though the house rule (`CLAUDE.md`) still applies: design read, code written
here.

**Taken:**
- The idea of a card as a **typed node** with declared inputs/outputs, rather than a step with
  implicit ambient state. `IProgWidget`/`ProgWidgetConditionBase` shaped §2.1–§2.2 directly.
- Numeric conditions reading a live value via a `getCount()`-shaped accessor
  (`ProgWidgetDroneConditionPressure.getCount()` reads live drone pressure) — the direct
  ancestor of §5's "`SensorFitting` needs a raw accessor" gap.
- Validating a program before it's usable rather than failing at runtime
  (`IProgWidget#addErrors`) — the model for rejecting cycles at print time (§2.4).

**Deliberately not taken:**
- **Jump-based control flow.** PNC's conditions don't sit inline in a chain blocking flow — they
  jump to a labelled widget elsewhere in the program depending on how they evaluate
  (`ProgWidgetConditionBase.getOutputWidget` calls `ProgWidgetJump.jumpToLabel`). That's real
  branching power, and it's exactly what §1 and §2 exist to rule out. This is the single
  biggest reason the program ended up a pure dataflow graph instead of a widened version of
  PNC's shape: a jump is a kind of "next instruction," and a controller isn't allowed one.
- **The whole drone/AI-goal domain.** PNC's widgets ultimately produce a `Goal` for a mobile
  entity to run. Nothing here moves; a card's terminal case is "call `setEngaged` on something
  already sitting in the world," which needed none of that machinery.
- **The physical puzzle-piece placement model**, partially. PNC's widgets are placed and wired
  visually inside a Programmer block's own 2D space (`PositionFields`, drag-and-drop), which
  the mockup's own editor resembles closely — that part **is** being taken, into the Programmer
  block's GUI (§4). What's not taken is PNC's puzzle pieces also being *physical items* you
  craft one of per widget instance; this design's cards are drawn from a palette and wired, not
  individually crafted per placement (see the mockup: the left panel is a palette of *kinds*,
  not an inventory of consumed items).

## 8. Open questions

Collected from throughout, philosophy-§18-style:

- **[OPEN, resolved]** Exact push-vs-poll implementation for §2.5's evaluation trigger — see §2.5.
- **[OPEN]** The end-game program database / ender-networking mechanic (§4).
- **[OPEN, partly resolved]** `OR` shipped 2026-10-13 for deadbands; `NOT` has not. A one-input
  gate is a new port shape on the Logic card, not a new card.
- **[OPEN, resolved]** Math nodes were built for vehicles (analog controls, philosophy §13's
  vehicle exception).
- **[OPEN]** Playtest wishlist for later programmers (2026-10-15): cards defined from other cards
  (functions), a delay card, addressable memory, loops. Tension to settle first: philosophy §13
  says *capability does not tier* -- a punched tape can express what a late controller can. If
  these arrive only on later media, capability tiers. If they belong to every tier, a delay or a
  memory card also gives the controller state of its own, which §13's deadband ruling put in the
  world (the switch's own position) on purpose. Recorded, not decided.
- **[OPEN, resolved]** The Programmer block's GUI is built
  (`control/programmer/ProgrammerScreen`), and has been rebuilt once. The first version was a
  fixed 188×156 box beside a palette rail: click an output port then an input port to wire,
  input slot guessed from which half of a card's left edge was clicked, right-click a card to
  delete it outright, one server packet per edit. It worked and it was bad — the box filled up
  at five cards, wiring was a two-click guess, and there was no undo. (Before that it was a
  fixed grid, cut for drag-and-drop after playtest: "i cant really drag nodes around... which is
  what i was wanting".)

  **The current canvas is read from RetroSFM** (MIT, `breakinblocks/RetroSFM` branch `1.21.1`
  — a node editor laid over Super Factory Manager, the "visual style of Steve's Factory Manager"
  philosophy §13 names, on modern code where the 1.7.10 original wasn't readable). It fills the
  window, with the Punch Card slot, the hotbar and Print in a strip along the top — the main
  inventory is dropped for the room, so a card has to be in the hotbar to go in. Pan with
  middle/right drag, zoom on scroll about the cursor, Home frames everything. Each card has
  explicit pins; drag from a pin to a pin to wire, and compatible pins light up while dragging.
  Right-click empty canvas for the palette; drop a wire on empty canvas and the palette offers
  only cards that could attach there, then wires the new one in. Click/shift-click/box-select,
  drag to move a selection, Delete to remove, Alt-click a pin or wire to cut it, Ctrl+Z/Y/C/V.
  A comparator's op and literal are clicked in place on the card.

  **Editing is now local.** The screen edits its own copy and sends the whole graph and layout
  as one `ProgrammerCommitPayload` on Print and on close — that is what makes undo and paste
  cheap. The server doesn't trust it: `ProgramGraph#findEditError` refuses wiring that could
  never have been drawn, and each link is re-resolved against the world where its chunk is
  loaded. Linking is the one edit that can't be local, since the Data Connector selection is
  server state: `ProgrammerLinkPayload` asks, `ProgrammerLinkedPayload` answers, and the screen
  patches its copy. Last commit wins if two players edit one Programmer at once.

  **Copy/paste is disabled on a printed card.** Copying a printed program out and pasting it
  onto a blank would be a second way to author the next card, and the hard-rewrite tier (§4)
  says changing a program means authoring a new one from scratch.
  **[OPEN]** Whether that should hold — philosophy §13 says the program "can be copied,
  stockpiled", and nothing here decides whether *copied* means copying the card or re-authoring
  from a known design. Undecided; the restrictive reading is the one that can be relaxed later.

  Position is still not part of `ProgramGraph` — a printed card carries no layout, only wiring —
  it lives as a parallel `Map<Integer, NodePos>` on `ProgrammerBlockEntity`.
