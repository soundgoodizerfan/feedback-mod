---
name: add-minigame
description: Design and build a new hand-work minigame for Feedback (forging, soldering, enchanting, skillet, pot, cut and pour are the precedents). Use whenever the user wants a new minigame, a hand-work version of a machine process, or a rework of how an existing one plays. Covers picking the genre, making the material's stats drive the play, visuals and feedback, performance, and what the client may and may not decide.
argument-hint: <what the player is doing> <what it works on> [where it opens from]
---

# add-minigame

**Read first:** `docs/design_digest.md` (hard rules), `docs/gui_skins.md` (required before any screen), and the section of the relevant spec that names the minigame. Look at the precedents below before inventing a shape: the same problems have been solved already, and the code is the reference.

A minigame is the *hand* version of a physical process. Automation (a machine, a vessel left alone) stays possible and worse; hand skill buys evenness, a higher class, a cleaner result, **never access to a thing** (digest: manual production stays possible, precision is never a hard gate, the recipe is not locked, the process is difficult).

## 1. Pick a genre nobody has used for this kind of act

Each existing minigame plays like nothing else. A new one must too, and the genre should be the act's own physics, not a coat of paint.

| Genre | Precedent | The act it fits |
| --- | --- | --- |
| Rhythm | forging (`modular/ForgeRhythm`) | striking metal |
| Drag along a row, pace is the skill | soldering (`client/SolderScreen`, `process/Soldering`) | moving a hot tool along a joint |
| Control: steer a lagging system | enchanting (`modular/EnchantGame`, hysteresis), skillet (`machine/skillet`, `client/SkilletScreen`) | a quantity that responds late and cannot be undone |
| Circular motion plus pick-off | pot (`machine/stockpot`, `client/PotScreen`) | stirring, skimming; a live machine you work while it runs |
| Placement | cut (dish spec 4) | where to cut |
| Hold and release at a line | pour/finish (dish spec 4) | flow |

Ask: what does the hand physically do, and what is the *error* a real person makes? The minigame is that error made readable. If it is a timing game and forging already owns timing, find the control, placement or flow version instead.

## 2. The material must change how it plays (this is the point)

**A minigame that changes with the stats of the thing you are working on is far more fun than one that is the same for everything.** If the screen is identical for copper and iron, for beef and kelp, honey and broth, the minigame is a skin over a constant and the player learns it once and is done. If the figures of the material change how the hand tool behaves, every material is a new feel, there is something to learn per material, and the stats in the tables finally mean something the player can *feel*.

Do this in the design, not as an afterthought:

- Name, in the spec, which material figure drives which part of the play. Examples already in the game: viscosity sets pour flow (honey slow and precise, broth fast and flooding); a material's coercive field and loop width set the enchant game's feel (stubborn but forgiving versus quick but twitchy); the iron's Tu against the pad's sets the solder window; the vessel's mass sets how far a lag runs on.
- Prefer driving the **feel** (drag, slip, speed, lag, snap, size of the window, how far a mark wanders) over only the **score**. Changing the score alone is invisible until the end.
- Use figures the material already carries (`Material` and its blocks: hardness, toughness, density, `food` fibre/fat/moisture, viscosity, conductivity, friction). If a figure you need is missing, add it to the material data (digest: route material properties through the material system), do not hard-code per item. Properties, never item identity.
- Check there is **no best material and no meta**: nothing should make one input win everywhere (digest). If a figure only ever helps, it is wrong.
- Write one sentence for the wiki: how the material changes the game here.

## 3. Visuals and feedback are half the work

The player must read the game from the picture. Bars with target lines and numbers are the failure mode; the digest and the senses rule say the player reads adjectives and pictures, and numbers cost an instrument (`Readout`).

- **Every input gets an answer, instantly**: a motion, a sound, a particle, a colour shift. A mark lands with a chop and a squish; a pour has a stream and a rising level; a hot pad glows; a pan sizzles. Silence on an action feels broken.
- **State is a picture**: the work piece changes colour, shape or size as it changes; the gap between what you commanded and what is happening (lag, error, heat) is drawn so it can be steered against. Do not draw a target line unless the real process has one.
- **Juice that carries information**: pitch and density of sound and particles follow the real state (food heat, viscosity, speed, how close to charring). Decoration that does not carry state is last priority, information-carrying juice is first.
- **Good results and bad results look and sound different** and the bad ones are never a refusal, they are outcomes with a price (consumed inputs, a lower class, a hazard).
- Tick-advanced animation, eased handles, a short flourish on completion that **never delays sending the result**.
- Use the library: `GuiSkin`/`GuiStyle` widgets (a screen never hand-draws a button, slider, toggle, inset), `Opening` for arrival, colours from the skin, text through `Readout`. Pieces that *are* the subject of the screen (a pan, a lane, a pad row, a pot) may use their own colours: say so in a comment. Per-skin sounds: `GuiSkin.play(sound, pitch)` with the pitch wobble.
- Cut the screen's text: wrap with `font.split`, translation keys in `en_us.json`, a hint line that says what to do (an earlier screen shipped with none and the player could not tell).

## 4. Performance

A minigame runs every frame on the client and every input on the server.

- **No allocation per frame** in `render`. Fixed arrays, a fixed particle pool advanced in `tick()` (see `SkilletScreen`: 40 specks; `PotScreen`), precomputed layout in `init()`, no `new` of lists, strings or `Component`s in the draw path (cache translated components).
- **State advances once a tick** (`Screen.tick`), not once a frame; frames only interpolate.
- **Packets are few and small.** One result packet at the end (soldering, forging, cut, pour), or a few per second for a *live* machine that keeps running while the screen is open (the pot: pace and skim flags every 5 ticks, the server answers with state). Never a packet per tick or per frame. Carry integers (percent, hundredths) not floats where a coarse figure will do; cap list lengths in the `StreamCodec` (`ByteBufCodecs.list(max)`).
- **The server does no per-tick work for a minigame that is not live.** A session is a stored start tick and a recompute at the end. A live machine (pot) only ticks what it already ticked.
- Pure grading/physics functions are plain static methods over primitives (see `Soldering.Pass`, `ForgeRhythm`, `EnchantGame`, `SkilletBlock.warm`/`panTu`): cheap, unit-checkable, shared by screen and server.

## 5. What to trust the client on, and what never

**The client draws; the server decides.** The screen shows the player what their input is doing; the server recomputes the outcome from the *input* and the *real world*, and the two use the **same pure function** so what the player saw is what is graded.

Trust the client for: where the mouse is, what it clicked and when (as raw samples), the look and sound of the screen. Nothing else.

Never trust the client for: a grade, a class, a result, an amount, a temperature, a time. The payload carries **samples** (pad index and dwell, mark positions, lever settings, held ticks), never conclusions. The server then:

1. Checks there was a **session** (a begin note: `ForgeBeginPayload`/`beginForge`, `SolderingIronItem` `SESSIONS`, `SkilletBlock` `SESSIONS`) tied to the right menu or block and player.
2. Checks the **world still matches**: the player is in range, the block or menu is the same one, the held item or slot still holds the same kind of thing, the knife or tool is still there.
3. Checks **time**: the samples cannot add up to more ticks than have passed since the session began (allow a few ticks of slack), so a result cannot be sent faster than it could be played.
4. **Clamps and range-checks** every number (counts, indices, 0..100 percentages, positions), caps list lengths, rejects repeats or out-of-order indices. Reject silently and consume nothing.
5. **Reads the real figures itself**: the material, the item heat (`ItemHeat`), the fire under the pan, the viscosity. Never accept them from the client.
6. Recomputes through the shared pure function and applies the outcome (stamp a component, make the item, spend the knife), then tells the client only what it needs to draw.

A client that cheats can at worst send the *best possible honest input*, which the server grades the same as any other. Design so that is true: it must not be able to skip time, spend nothing, or name its own grade.

Remember the digest: **the world resolves on truth, not on readings.** Instruments never enter the calculation.

## 6. Hard-rule check (digest) before you build

- Hand work stays possible and worse than automation; skill buys quality, never access.
- Precision is a soft gate. A bad result is an outcome with a cost, never a refusal; failed batches consume their inputs.
- Discrete **outcome classes** (even/mixed/ragged, cold/good/lifted); the **input** may be analog (scalars are allowed inside a minigame; the rule guards against meta-mining and against dodging depth).
- No new meter. Costs are physical: heat, wear, a consumed input, a hazard, the knife.
- Wear comes from a specific act, never from uptime.
- Units are types at seams; figures players read go through `Readout`.
- No identity checks for the player: materials and traits, not item ids.

## 7. Build checklist

1. Spec first: genre, the act, what the material changes (section 2, name the figures), the outcome classes, what is consumed, how automation does it worse. Add it to the relevant spec in the same change (digest: living documents).
2. Pure model class (`process/` or `modular/`): inputs are primitives and material figures; outputs are classes. Both sides call it.
3. Payloads in `net/`: a begin (if the server needs a session) and a finish (samples only), `StreamCodec` with capped lists, registered in `Feedback.java` beside the others. A live minigame adds a state payload back to the client.
4. Server handler: sections 5's six checks, then apply. Keep it beside the thing it changes.
5. Screen in `client/` per `docs/gui_skins.md` (`Screen` + `Opening`, skin of the table or block, widgets from `GuiStyle`, result sent once in `onClose`, gate input on `opening.settled()`).
6. Juice and feedback (section 3), performance pass (section 4): check the draw path allocates nothing.
7. Lang keys, a wiki page or section (what you do, what the material changes, what a bad result costs), `TODO.md`, the spec's status line, the `docs/gui_skins.md` screen index.
8. `./gradlew compileJava`, then hand the user an in-game checklist (what to do, what they should see and hear). Do not write a game test unless asked; deploy only once the user confirms it plays.
