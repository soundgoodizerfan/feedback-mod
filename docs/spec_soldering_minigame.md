# Soldering by drag (hand-iron minigame)

> Status: build spec, 2026-10-23. Replaces the invisible hold-and-release of the hand iron (`item/SolderingIronItem`, `docs/spec_basic_electronics_phase3.md` (a)) with a minigame. User rulings, same day: hand soldering was "really hard" and should be a minigame; it is **not** rhythm (forging owns that, `docs/feedback_modular_items_spec.md` 4.1), **not** the enchanting hysteresis game, and not a TFC slider. Chosen from the brainstorm: **drag along a pad row** ("idea 3"). The reflow oven stays pure thermal and is untouched (the capital answer; hand vs oven is the two-answers pair). Resistance is never modelled. Analog input is fine inside a minigame (`scalars-fine-inside-minigames`), but the result stays banded.

## What the player does

Pasted board (`Board`, phase 3) in the off hand, hot iron in the main hand, right-click: a **soldering screen** opens (follow `client/EnchantScreen` and the forging screen for structure, `GuiSkin`/`GuiStyle`, and for sending a result to the server).

The screen shows the board's **pad row**: one pad per joint, left to right, with a trace between them. Pads = `2 x mounted parts` if the board carries parts (a circuit in the making), else 2. The player **holds the mouse button and drags the iron tip along the row** from first pad to last, and may stop on a pad (dwell) or sweep over it.

- **Pace is the skill.** Heat delivered to a pad is the time the tip spends on it times how far the iron's Tu sits above the pad's Tu (same law the hold used). Too fast: the pad never reaches the solder's liquidus and the joint is **cold** (intermittent). Too slow: the pad passes the trace's lift limit and the **trace lifts**. Between: **good**. The good window is the existing one (`process/Soldering`: liquidus up to liquidus + window; window = 120 / viscosity, so leaded forgives more, lead-free less).
- **Heat is the clock.** The iron cools while in use on `ItemHeat`'s curve; later pads see a colder tip, so the pace that was right at the start is too fast at the end. The player may release the button and leave the screen to reheat the iron at a fire, then come back and resume from the last good pad. Do not add a refuel step; the existing fire-heating click is enough.
- **Wobble bridges.** Staying off the trace centreline by more than half a pad pitch lets solder reach the neighbouring pad or trace (a **bridge**): that joint is flagged bridged, adds noise, and wastes the solder. Pads are small targets; bridges, not failure, are the price of a shaky hand.
- **Readout is the picture, not numbers.** Each pad is drawn dull (cold), shiny (wet, in window) or scorched (over), updating live as the tip passes; the tip shows its own heat as a glow. No bar with a target line; the player reads the pad. Truth-tier figures (the iron's Tu, the pad's Tu) are available only through the instruments the player carries, through `Readout`/`Instruments` like everything else; the held-item HUD (`client/ItemInfoHud`) lists the iron's heat to whatever thermometer the player carries.
- **Misses never fail the part** (digest: precision never a hard gate). Cold and bridged pads add noise; only a lifted trace loses something, and that is the existing rule (traces lift -> bare substrate / dead part, parts lost). Make a **lifted pad** lose its own joint only if the existing grade structure allows it cheaply; otherwise keep today's all-or-nothing and report it as [OPEN].

## What the server decides

The client sends the drag as a **short list of samples** (pad index, dwell ticks, lateral offset) when the player releases at the end or leaves; the **server recomputes every grade** from those samples, the iron's real Tu from `ItemHeat`, the alloy, and the substrate/trace lift limits. Never trust a client-supplied grade. Clamp absurd values (a sample count above the pad count x a sane tick cap is rejected; dwell total cannot exceed the real time since the screen opened). Result goes through the existing `Soldering.finish` so the board (or circuit, phase 4) comes out as before; the aggregate grade is worst-of with counts feeding the circuit's `noise` (more cold/bridged pads, more noise; element count unchanged).

## Hard rules

- No resistance. No tiers. No identity checks (alloy by its `solder` trait and melting point, substrate by its figures). Discrete outcomes (good / cold / lifted / bridged) per pad. The world resolves on truth; instruments never enter the calculation.
- Lead solder fumes (`core/hazard/Fumes`) keep running while the screen is open and the iron is hot. Lead-free: none.
- **Sense / stop:** sense is the pad colours and a thermometer on the iron; stop is lifting the button (the dwell stops accruing).
- **Overrun:** lingering past the window lifts the trace (existing). Iron left hot in a closed screen keeps burning the pad under it: leaving the screen with the button held counts as lingering on that pad until the server ends the session (cap it).
- Keep it cheap: no per-frame allocation in `render`, no per-tick packets (one packet at the end).

## Build list

- `client/SolderScreen` (new): pad row, drag input, live pad colours, tip glow, a small sound (scratch/sizzle from existing vanilla sounds) and particles; open from `SolderingIronItem`'s use on a pasted board (replace the old hold-and-release; breaking change fine, no shim).
- Packet in `net/` (follow `ClientPayloads` and an existing screen->server payload) carrying the samples; server handler in or beside `process/Soldering` that recomputes and calls `Soldering.finish`.
- `process/Soldering`: expose a pure function from (samples, iron Tu, alloy, board) to grade so JEI and docs share one source of truth; unit-checkable without a game test.
- Tuning in `FTuning` (placeholders marked as such): pad pitch, dwell-to-heat scale, bridge offset, iron cooling use, session caps.
- JEI `SolderingCategory`: update the card text to describe the drag (no new category). Info page for the iron (`feedback.jei.info.*`).
- Docs: new `feedback_mechanics.md` section after 6.21 ("Hand soldering by drag"), fix 6.20 where it describes hold-and-release, `docs/spec_basic_electronics_phase3.md` as-built note, `docs/source_layout.md`, `TODO.md` (BUILT, unplayed), wiki `Soldering_Iron.md` and `Solder.md`. Record why it was tempting to leave it as a hidden hold window (the knowledge was invisible; the fix is a readable picture, not an easier window).

## [OPEN] for the agent to report, not silently settle

- Is a lifted trace per pad (lose that joint) or whole board?
- Resume after leaving the screen to reheat: from the last good pad, or restart?
- Pad count rule (2 x parts) vs a fixed row.
- Keyboard fallback for the drag (no mouse): skip unless trivial.
