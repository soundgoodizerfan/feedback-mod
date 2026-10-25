# GUI skins

How screens and HUDs are drawn in Feedback. Everything is code-drawn with fills and the vanilla font: no skin texture files, nothing to license, and every screen on a skin changes when `client/GuiSkin.java` does. Adding a skin is the `add-gui-skin` skill; this document is the map of what exists and how to use it.

## Required reading for GUI work

Read this file before any change to a screen, widget, HUD or popup under `client/` (the `GuiSkin` and `GuiStyle` library, and every screen built on it). Agents have skipped it and rebuilt things the library already does. If a need is not covered here, extend the library (and this file) rather than hand-drawing it in a screen.

Rules that follow from it:

- A screen never hand-draws a button, toggle, check box, stepper, slider, tab strip, sunken area, gauge, list highlight or scroll thumb. Those are `GuiStyle` widgets or `GuiSkin` primitives.
- Colours for text and lines come from the skin (`ink`, `dim`, `accent`, `warn`), not from hex literals, so the screen follows its skin.
- Every screen opens through `Opening` (directly, or via `SkinnedScreen`), so it arrives with its skin's reveal and sounds.
- Figures a player reads go through `Readout` (below), never formatted ad hoc.

## The pieces

All in `client/`.

| File | What it is |
|---|---|
| `GuiSkin` | The enum of materials (seven skins, below). Owns the seven colours, the drawing primitives, the per-skin widget faces, the sounds and the open/close reveal. |
| `GuiStyle` | Widget factories (`button`, `primary`, `toggle`, `checkbox`, `stepper`, `slider`, `tabs`), all drawn through a skin, plus drawing helpers (`easeOut`, `line`, `perimeter`, `drawingBoard`, `stamp`, `hudCard`) and two small state holders (`Slide`, `Flash`). |
| `Gauge` | A gauge linked by suppliers to what it shows; drawn by `GuiSkin.gauge`. |
| `Opening` | A screen's arrival and departure in time: world dims, the skin reveals the panel, skin sounds play, input waits until it is open. Package-private. |
| `SkinnedScreen<M>` | Base class for container screens. Holds an `Opening`, gates input, draws labels in the skin's ink. The subclass draws the panel in `renderBg`. |
| `SkinPopup<T>` | A second window over a skinned screen that picks one thing from a list. Package-private. |
| `HudReadouts` | Short server messages drawn as HUD cards instead of the action bar. |
| `Readout` | The one place a simulated figure becomes readable text (the "senses give adjectives, numbers cost an instrument" rule). Not drawing, but every screen and HUD that prints a figure uses it. |

## Terms

- **panel**: the window itself, raised. Drawn by `skin.panel`.
- **inset**: a *sunken* area, darker and recessed. Use it for anything that shows content inside a panel: text boxes, descriptions, readouts, a list's backing. Never call it "extruded".
- **plaque**: a small raised plate, for a stamped readout or a list row.
- **well**: the floor colour of a sunken area. **trim**: its rim. **plate**: a plaque's face.
- **settled**: the screen has finished opening and has not started closing; only then does it take clicks.

## The skins

| Skin | Material | Used by | Click / open / close | Reveal |
|---|---|---|---|---|
| `BLUEPRINT` | Cyanotype: white ink on blue paper, 6 px grid, ruled double border, speckle | Blueprint Table, Engine Stand | cartography take / `UI_UNROLL` / `UI_ROLL` | Unrolls from the left, the rolled remainder riding the edge |
| `FORGE` | Dark riveted iron framed in brass, brushed streaks; brass buttons, primary enamelled fire-orange | Reloading Press, Material Probe | stone button / `UI_HATCH_OPEN` / `UI_HATCH_CLOSE` | Two iron shutters part from the middle |
| `PANEL` | Black crinkle steel, cream engraving, chrome bezels, corner screws, amber lamps | Gun Table (`ToolTableScreen`), Skill Tree, Mirror, Creative Source, Solder, Skillet, Programmer (partly) | `UI_DETENT` / `UI_TOGGLE` / `UI_TOGGLE` | Drops 10 px into its mounting, dark until powered, lamps flicker on |
| `STONE` | Vanilla cobblestone blitted in place under a smoke-grey wash; no trim | Thermal Vessel, Tool Table (`ToolTableScreen`) | stone button / stone place / stone hit | None |
| `MYSTIC` | Indigo to violet, drifting aurora bands, glyph noise, rare sparkles, gold-leaf trim with corner diamonds | Enchanting Table | amethyst chime / enchanting table use / none | None |
| `VANILLA` | The game's own inventory grey: white bevel up-left, dark down-right, notched corners | Every HUD | UI button click / none / none | None |
| `BRICK` | Fired brick in offset courses (fixed-seed tints) with calcite mortar lines, calcite-cream trim; brick-red buttons, primary a cream calcite slab; sunken areas stay dark so cream text reads, sockets are calcite | Prep Table (`ToolTableScreen`) | calcite hit / calcite place / calcite hit | None |

`MYSTIC` animates (its clock is game time plus partial tick, so it holds still while paused). The others are static: their noise tables come from fixed-seed `Random`s so nothing crawls.

## GuiSkin

### Colours

Constructor order: `ink, dim, accent, warn, well, trim, plate`.

| Colour | Meaning | Public getter |
|---|---|---|
| `ink` | Body text and lines | `ink()` |
| `dim` | Labels, secondary lines | `dim()` |
| `accent` | The thing selected, a value just changed, a gauge's mark | `accent()` |
| `warn` | A figure past its limit | `warn()` |
| `well` | Floor of a sunken area | none: use `inset` |
| `trim` | Rim of a sunken area, gauge graduations | none: use `inset` |
| `plate` | A raised plate's face | none: use `plaque` |

`GuiSkin.lerp(a, b, t)` (package-private) blends two ARGB colours; widgets use it for hover glows.

### Primitives (public)

All take `GuiGraphics g` first and absolute screen coordinates.

| Call | Draws | Geometry |
|---|---|---|
| `panel(g, x, y, w, h)` | The window. Abstract: each skin draws its own. | Fills `w` x `h`; the drop shadow lands outside, at `+3, +4`. Keep content ~8 px clear of the edge, where trim, rivets and screws sit. |
| `slot(g, x, y)` | A socket round a 16x16 item at (x, y). Abstract. | Draws from `x-1, y-1` to `x+17, y+17`. Pass the slot's item position, not its corner. |
| `inset(g, x, y, w, h)` | A sunken area: `well` filled, `trim` outlined. | `w` x `h` including the 1 px rim. |
| `plaque(g, x, y, w, h)` | A raised plate: `plate` filled, a lightened trim outline. | `w` x `h` including the rim. |
| `gauge(g, x, y, w, h, vertical, fill, mark, lead, ticks)` | The one gauge primitive. Normally reached through `Gauge`. | (x, y, w, h) is the *fill area*; the well is drawn 1 px outside it. `fill` and `mark` are 0..1 along the long axis (vertical fills from the bottom). Fill up to `mark` is in `lead`, past it in `warn`; the mark itself is an `accent` tick 2 px proud of the well; `mark < 0` for none. `ticks > 1` draws graduations outside the well (right side or underneath), every third one longer. |
| `bar(g, x, y, w, h, fill, mark[, lead])` | Horizontal `gauge` with no graduations; `lead` defaults to `ink`. | As `gauge`. |
| `row(g, x, y, w, h, picked, hovered)` | A list row's highlight: picked in translucent accent, hovered in a faint white. | Draw it over the row's backing, under its text. |
| `scrollbar(g, x, y, h, total, visible, at)` | A 2 px track and a thumb (min 8 px). Draws nothing when `total <= visible`. | `at` is the index of the first visible item; units are yours (rows, lines). |

### Per-skin faces (package-private, used by `GuiStyle`)

`button(g, x, y, w, h, label, glow, down, active, primary)` (abstract), `checkbox`, `slider`, `tab`. A screen does not call these; it builds the matching `GuiStyle` widget. `checkbox`, `slider` and `tab` are shared code built from the seven colours; only `button` is drawn per skin.

### Sounds

- `click()` (abstract): a button press, unless the button names its own sound.
- `open()`, `close()`: played by `Opening`; `null` (the default) is silent.
- `GuiSkin.play(sound, pitch)`: plays a UI sound with a small random pitch wobble (0.96 to 1.04 of `pitch`) so repeats do not sound stamped out; ignores `null`. Use it for every screen sound, including a screen's own (`EnchantScreen`'s tones, `ToolTableScreen`'s anvil).

UI sounds are `FSounds.UI_*` (`UI_UNROLL`, `UI_ROLL`, `UI_STAMP`, `UI_DETENT`, `UI_HATCH_OPEN`, `UI_HATCH_CLOSE`, `UI_TOGGLE`, `UI_PUNCH`), aliased to vanilla events in `sounds.json` until recordings replace them.

### Reveal

`beginReveal(g, x, y, w, h, t)` and `endReveal(g, x, y, w, h, t)` bracket the panel's drawing, `t` eased from 0 (shut) to 1 (open). `Opening` calls them; a screen never does. A skin either scissors to the opened part in `beginReveal` and, in `endReveal`, `g.flush()`es, `disableScissor()`s and draws the moving part (`BLUEPRINT`, `FORGE`), or pushes a pose in `beginReveal` and pops it in `endReveal` (`PANEL`). Not overriding them means no ceremony. Because the reveal wraps the whole body, widgets drawn inside it are revealed too.

## Gauge

```java
Gauge.of(() -> fill)            // 0..1, read every frame
     .mark(() -> m)             // optional: limit tick, 0..1; past it draws in warn
     .lead(() -> colour)        // optional: fill colour; default the skin's ink
     .ticks(n)                  // optional: graduations, one per band
     .vertical()                // optional: fills upward
     .render(g, skin, x, y, w, h);
```

The figure, tick and colour are suppliers, so a gauge is linked to what it shows and the skin decides the look. Build it once (a `static final` or a field) and call `render` each frame. Users:

- Thermal Vessel heat gauge: vertical and banded (`fill` is `(band + 1) / bands`, `lead` the band's colour, `ticks` the band count).
- `ParryHud`: green while the window is open, red during recovery.
- `StoredHud`: Charge (mark at 0.5, past it every blow arcs) and Reservoir experience (accent).
- `CastHud`: stamina, `lead` switching to `warn` when low.

Not converted: the Enchanting Table's polarization track and heat window (bespoke bands and a mouse-driven marker) and the Reloading Press bar (a direct `skin.bar` call, which is the same primitive).

## GuiStyle widgets

Every factory takes the skin first and returns a vanilla widget for `addRenderableWidget`. State is passed as suppliers read every frame, so a widget shows what is set, not what was clicked: change the model in the callback and the widget follows. Each eases a 0..1 hover glow (about 1/8 s) and plays its own sound.

| Factory | Returns | Size | State / callback | Sound | Keys |
|---|---|---|---|---|---|
| `button(skin, label, press, x, y, w, h)` | `Button` | any; 14 or 20 high in use | `press` | skin's `click()` | vanilla (Enter/Space when focused) |
| `button(..., sound, pitch)` | `Button` | as above | as above | `sound` at `pitch` | as above |
| `primary(skin, label, press, x, y, w, h, sound)` | `Button` | any | the one button that commits; drawn heavier by the skin | `sound` (usually `FSounds.UI_STAMP::get`), or `click()` if `null` | as above |
| `toggle(skin, label, state, press, x, y, w)` | `Button` | `w` x 16 | `BooleanSupplier state`; lamp left, label at +16, lever right | `UI_TOGGLE`, pitch down when turning off | as button |
| `checkbox(skin, label, state, press, x, y, w)` | `Button` | `w` x 12; 10x10 box, label at +15 | `BooleanSupplier state` | `click()`, pitched by state | as button |
| `stepper(skin, x, y, w, h, left, right, value, step, plaque, sound)` | `AbstractWidget` | `w` x `h`; arrows 14 wide each | `Supplier<String> value` (flashes accent on change, ellipsised to fit); `IntConsumer step` told -1 or +1; `plaque` puts the reading on a plate | `sound` or `click()`, 0.9 left, 1.1 right | Left/Right when focused |
| `slider(skin, x, y, w, min, max, step, value, set)` | `AbstractWidget` | `w` x 12 | `DoubleSupplier value`; `DoubleConsumer set` told each snapped change while dragging | `click()`, pitch rising with the value | Left/Right one step, Shift ten |
| `tabs(skin, x, y, w, h, labels, selected, select)` | `AbstractWidget` | `w` x `h`, width shared out whole | `IntSupplier selected`; `IntConsumer select` told only when a different tab is clicked | `click()` | none |

Notes:

- A widget's `active = false` greys it on every skin; use it rather than hiding a control that is unavailable.
- For Shift-for-ten on a stepper, read `Screen.hasShiftDown()` inside `step` (`CreativeSourceScreen`).
- The stepper's `left`/`right` are plain strings (`"<"`, `">"`, `"-"`, `"+"`).

### Helpers

- `easeOut(t)`: cubic fast-out, slow-in, clamped. For anything arriving.
- `line(g, x0, y0, x1, y1, colour)`: a 1 px line of any slope (Bresenham).
- `perimeter(g, x0, y0, x1, y1, from, len, colour)`: part of a rectangle's outline, `len` px clockwise from `from` px past the top-left. Growing `len` traces an outline; short runs stepped along it make a marching dashed line.
- `drawingBoard(g, x, y, w, h, mouseX, mouseY, live)`: a drawing area on a `BLUEPRINT` sheet: frame, rulers top and left, and while `live` a T-square and set-square following the pointer, marked on the rulers. Hard-wired to `BLUEPRINT`'s colours. Used by the Engine Stand.
- `stamp(g, text, cx, cy, age, rgb)`: a rubber stamp `age` ms after it came down: lands big, settles in 110 ms, fades out from 1200 to 1600 ms, tilted 10 degrees, drawn at z 300. The caller keeps the time it landed.
- `hudCard(g, x0, y0, x1, y1[, alpha])`: a HUD card in vanilla inventory grey with its bevel, slightly see-through, faded by `alpha`. Takes corners, not width and height.
- `Slide`: one per HUD. `offset(key, distance)` returns how far right to draw it now; when `key` changes (a different tool came into hand) the card slides in over 220 ms.
- `Flash(size)`: for `size` readouts. `colour(i, value, ink, accent)` returns the colour to draw readout `i` in: the accent fading back to ink over 450 ms after its string changes. `reset()` when the readouts start meaning something else, so the switch does not flash.

## Opening

```java
private final Opening opening = new Opening(SKIN);

opening.start();          // in init; only the first call counts (init reruns on resize)
opening.render(g, width, height, x, y, w, h, () -> { ...body... });
opening.render(g, width, height, x, y, w, h, dim, () -> { ... }); // dim 0..1 scales the world dimming
opening.close();          // in onClose; plays the close sound, closes from wherever it had reached
opening.settled();        // open and not closing: take input
opening.closing();        // close() has been called
opening.shut();           // fully closed: now call the real Screen.onClose
opening.progress();       // 0..1, linear
```

Opening takes 320 ms, closing 200 ms; the reveal and dimming use `easeOut(progress)`. The dimming is a full-screen gradient (alpha `0xC0` to `0xD0` at `dim = 1`) under the panel. A popup uses `dim = 0.5`; `SolderScreen` and `SkilletScreen` use `0` so the world stays visible.

## Making a screen

### Container screen

```java
public class MyScreen extends SkinnedScreen<MyMenu> {
    public MyScreen(MyMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, GuiSkin.FORGE);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();                       // starts the Opening
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.my.go"),
                b -> go(), leftPos + 8, topPos + 40, 60, 16, null));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + MyMenu.SLOT_X, topPos + MyMenu.SLOT_Y);
        inventory(g, 8, 84);                // the player's 3 rows + hotbar, relative to the panel
    }
}
```

Register it in `FClientSetup.registerScreens` (`RegisterMenuScreensEvent`).

What `SkinnedScreen` does for you:

- Starts the `Opening` in `init`, reveals the whole screen (panel, slots, items, widgets) with it, and only calls vanilla `onClose` once it is shut.
- `renderBackground` draws only your `renderBg`; the dimming is the `Opening`'s. Do not call vanilla's blur.
- `renderLabels` draws the title in `ink` and the inventory label in `dim` at vanilla's label positions (`titleLabelX/Y`, `inventoryLabelX/Y`). Override it for your own labels; coordinates there are relative to the panel.
- Tooltips only once settled.
- Input gating: clicks, drags and scrolls are swallowed until settled and while closing; keys and typed characters are swallowed only while closing (so Escape during the reveal closes it from where it reached). Releases are not gated.
- `inventory(g, x, y)` and `scrollbar(g, x, y, h, total, visible, at)` helpers.
- `settled()` and `closing()` for your own handlers: gate any custom click, drag or key handling on them.

### Plain (non-container) screen

Hold an `Opening` and wire it by hand. The canonical one is `MirrorScreen`:

```java
public class MyScreen extends Screen {
    private static final GuiSkin SKIN = GuiSkin.PANEL;
    private final Opening opening = new Opening(SKIN);

    @Override protected void init() {
        opening.start();
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.done"), b -> onClose(), ...));
    }

    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) { super.onClose(); return; }
        int x = left(), y = top();
        opening.render(g, width, height, x, y, W, H, () -> {
            SKIN.panel(g, x, y, W, H);
            // labels, insets, gauges
            super.render(g, mx, my, partial);   // widgets last, inside the reveal
        });
    }

    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}  // the dimming is the opening's
    @Override public void onClose() { opening.close(); }
    @Override public boolean mouseClicked(double mx, double my, int b) { return !opening.settled() || super.mouseClicked(mx, my, b); }
    @Override public boolean keyPressed(int k, int s, int m) { return opening.closing() || super.keyPressed(k, s, m); }
    @Override public boolean isPauseScreen() { return false; }
}
```

The empty `renderBackground` matters: vanilla's `Screen.render` calls it, and the default would blur and darken over your panel. Gate `mouseDragged`/`mouseScrolled` on `settled()` too if the screen uses them. Followed by `MirrorScreen`, `CreativeSourceScreen`, `SkillTreeScreen`, and the two minigames `SolderScreen` and `SkilletScreen` (which pass `dim = 0` so the world stays in view). A screen whose close also sends something (a minigame's result) sends it in `onClose` before `opening.close()`, guarded so it is sent once.

### Full-window screens

`ProgrammerScreen` is a `SkinnedScreen` whose panel is the whole window (`imageWidth = width`, `imageHeight = height` before `super.init()`), so the `PANEL` reveal drops and powers up the entire canvas. It overrides the input handlers itself, so each starts with the gate `SkinnedScreen`'s own handlers would have applied: `if (!settled()) return true;` for mouse events, `if (closing()) return true;` for keys and typed characters. Its overlays (status line, palette) are drawn after `super.render`, outside the reveal, at z +300. A subclass that overrides `onClose` does its own work (the Programmer commits its edit) and then calls `super.onClose()`, which starts the close; the real close follows when it is shut.

### Screen-local drawing

Pieces a screen draws in its own colours (the Enchanting Table's track, the Skillet's pan, the Solder board's pads, the Programmer's canvas, skill tree nodes, the forging rhythm lane, the Thermal Vessel's band colours) do not follow the skin. That is fine for a piece that is the subject of the screen; say so rather than re-theming unasked. A piece that is generic (a box, a bar, a list) belongs in the library.

## SkinPopup

A titled list of choices in a window that opens over the host, on the host's skin, with that skin's reveal and sounds. The host dims at half strength behind it and nothing under it takes input while it is up.

```java
private final SkinPopup<String> popup = new SkinPopup<>(skin, Component.translatable("gui.x.title"));

popup.open(List.of(new SkinPopup.Entry<>(value, label, tooltipLines)), picked -> use(picked));

// route five things to it:
render(...)        { super.render(...); popup.render(g, leftPos, topPos, imageWidth, imageHeight, mx, my); }
mouseClicked(...)  { if (popup.mouseClicked(mx, my, button)) return true; ... }
mouseScrolled(...) { if (popup.mouseScrolled(scrollY)) return true; ... }
keyPressed(...)    { return popup.keyPressed(key, scan, modifiers) || super.keyPressed(...); }
charTyped(...)     { return popup.charTyped(c, modifiers) || super.charTyped(...); }
```

- `render` takes the host panel's rectangle and centres the 200-wide window on it, drawn at z +200 so it sits over the host's slots and widgets. Call it after the host's own drawing.
- Skip the host's own tooltips while `popup.active()`.
- A click on an entry picks it (skin click, callback, then the popup closes); a click outside the window, or Escape, closes it. Every click and key goes to the popup while it is up.
- Six rows show at once (`MAX_ROWS`, 18 px each), scrolled by the wheel. A list longer than a page gets a search box (label substring, focused on open; `gui.feedback.popup.search`, empty result `gui.feedback.popup.none`).
- An entry's `tip` (may be empty) is its hover tooltip.
- A host may own several (the Blueprint Table has three: add part, pick type, pick material); each `open` builds a fresh `Opening`.

Not built: categories, icons per entry, hover suppression on the host's widgets under the popup (only clicks and scroll are blocked).

## HUDs

Every HUD is drawn on `VANILLA`, so it reads as the game's own and not a machine's.

1. A final class with `static void register(RegisterGuiLayersEvent event)` calling `event.registerAboveAll(Feedback.id("name"), MyHud::render)`, and a `render(GuiGraphics g, DeltaTracker delta)`.
2. Call `MyHud.register(event)` from `FClientSetup.registerGuiLayers`.
3. In `render`, return early when `mc.player == null` or `mc.options.hideGui`, and when there is nothing to show (a HUD costs attention only when it has something to say).
4. Draw the card with `GuiStyle.hudCard`, text in `SKIN.ink()`/`dim()`/`accent()`/`warn()`, gauges through `Gauge`, a changing figure through a `Flash`, an arriving card through a `Slide`.
5. Readouts never jitter: no motion on a figure. Motion is for decoration (a card sliding in) only.

| HUD | Where | What |
|---|---|---|
| `GunHud` | bottom right | ammunition card, per hand |
| `BuildToolHud` | bottom right | Welder / Pivot Setter mode and the point under the crosshair |
| `HudReadouts` | above the ammo card's spot | short server messages, up to three, fading |
| `StoredHud` | bottom right corner | Charge and Reservoir gauges |
| `ItemInfoHud` | top left | what carried instruments read off each hand's stack; key H |
| `CastHud` | top right | stamina gauge and three cast slots |
| `ParryHud` | under the crosshair | parry window and recovery |

To show a message from the server, call `HudReadouts.send(serverPlayer, component, ticks)` instead of the action bar. A line with the same text already showing is refreshed rather than repeated, so a count that updates every hit holds one card.

## Draw order and layers

Paint back to front; the first thing painted is the furthest back:

1. Background fills (a translucent wash, the world showing through). In a skinned screen this is the `Opening`'s dimming.
2. The panel (`skin.panel`).
3. The body: the tree, the grid, the list.
4. Panels and insets that sit over the body (the info plate, a description box). An opaque inset hides whatever is behind it, which is the point.
5. Labels that belong to the panel.
6. Widgets last (`super.render`), so buttons and tab rows always sit on top of every layer above.
7. Overlays on a raised z: a `SkinPopup` (+200), a `stamp` (300), `ProgrammerScreen`'s HUD and palette (+300).
8. Tooltips.

Drawing a fill after the widgets puts it over the text and buttons. Drawing a fill after the body hides the body. When in doubt, draw it earlier. Inside a container screen, `renderBg` is layer 2 to 4, `renderLabels` is 5 (with the pose already translated to the panel).

## Text layout

- Text colours: `ink` for content, `dim` for labels and secondary lines, `accent` for the selected or just-changed, `warn` for past a limit. Draw without shadow (`drawString(..., false)`) on every skin but `VANILLA`'s buttons.
- A line of text that might be long: cut it to its column with `font.plainSubstrByWidth(text, width)` plus `"..."`, or wrap it with `font.split(component, width)`. `ToolTableScreen.fit` is a screen-local marquee (slides an over-long name to its end and back, scissored); promote it to `GuiStyle` if a second screen needs it.
- Stack facts on their own lines (10 px apart; 9 for tight hint text) rather than putting two in one row. Two strings at the same x collide.
- A title in caps centred at the top (`MirrorScreen`), or left at the margin with a right-aligned qualifier (`SolderScreen`).
- Strings are translation keys in `assets/feedback/lang/en_us.json`; screens use `screen.feedback.*` or `gui.feedback.*`.

## Scrolling

Two patterns, both in use:

- **By line** (lists, a description column): keep `scroll` as the first visible index, clamp it to `0..size - visible`, draw `visible` rows from it, and draw `skin.scrollbar(x, y, h, size, visible, scroll)` along the right edge. `MaterialProbeScreen` (text), `ToolTableScreen.column` (wrapped text) and its part list.
- **By pixel** (a wrapped block in a fixed box): `inset` the box, `enableScissor` to its inner edge, draw lines from `top - scroll`, `disableScissor`. Clamp the scroll to `lines * 10 - visibleHeight`. `SkillTreeScreen`'s description.

Either way, take the wheel only when the mouse is over that box (return `true` so the wheel does not also zoom or scroll what is underneath), and fall through to `super.mouseScrolled` otherwise. Reset the scroll when the box starts showing something else.

## Figures and units

Text that states a simulated figure (a temperature, a speed, a condition) comes from `Readout`: `Readout.temperature`, `figure`, `reading`, `condition`, `progress`, and the `instrumented`/`exact` checks that decide whether the player may see a number at all. Requirements and specs (what a recipe needs) are free and formatted at the call site. Volumes are kept in millibuckets in code and shown as ingots on screens.

## Screen index

| Screen | Base | Skin | Library pieces it shows off |
|---|---|---|---|
| `BlueprintTableScreen` | `SkinnedScreen` | `BLUEPRINT` | buttons, steppers, three `SkinPopup`s, `row`, `plaque` |
| `EngineStandScreen` | `SkinnedScreen` | `BLUEPRINT` | `drawingBoard`, `stamp`, `tabs`, `stepper`, `Flash` |
| `ToolTableScreen` | `SkinnedScreen` | `STONE` (Tool Table), `PANEL` (Gun Table), `BRICK` (Prep Table); chosen from the menu's table kind | `tabs`, `stepper` (Prep Table dish type picker), part list with `row` and scroll, wrapped scrolled column, forging rhythm lane (screen-local colours) |
| `ReloadingPressScreen` | `SkinnedScreen` | `FORGE` | `stepper`, `primary`, `skin.bar` |
| `MaterialProbeScreen` | `SkinnedScreen` | `FORGE` | scrolled text inset with `scrollbar` |
| `ThermalVesselScreen` | `SkinnedScreen` | `STONE` | vertical banded `Gauge` |
| `EnchantScreen` | `SkinnedScreen` | `MYSTIC` | `primary`, `SkinPopup`, bespoke track |
| `SkillTreeScreen` | `Screen` + `Opening` | `PANEL` | two `tabs` rows, pixel-scrolled description, pan/zoom canvas |
| `MirrorScreen` | `Screen` + `Opening` | `PANEL` | `toggle`s; the plain-screen template |
| `CreativeSourceScreen` | `Screen` + `Opening` | `PANEL` | `toggle`, `stepper` with Shift-for-ten |
| `SolderScreen` | `Screen` + `Opening` (dim 0) | `PANEL` | drag minigame on an inset |
| `SkilletScreen` | `Screen` + `Opening` (dim 0) | `PANEL` | control minigame, local speck particles |
| `PrepCutScreen` | `Screen` + `Opening` (dim 0) | `BRICK` | slab and pieces drawn live, `primary`, `Specks`, a judging cloth (screen-local colours) with the grade's music, lifted to a fanfare |
| `PrepPourScreen` | `Screen` + `Opening` (dim 0) | `BRICK` | hold-to-pour onto the finished dish (container, foods, growing coat), `Specks` |
| `PrepKneadScreen` | `Screen` + `Opening` (dim 0) | `BRICK` | pressure-and-rest dough drawn as fixed columns (squash on a spring, gloss, tear cracks), a palm, a cloth reveal, `Specks`, `bar` for time left |
| `ProgrammerScreen` | `SkinnedScreen` (full window) | `PANEL` (strip; canvas in its own colours) | full-window node canvas |

## Specks

`Specks` (package-private, `client/`) is the general form of the Skillet's pool: a fixed array of screen-space specks (`burst`/`spawn`, `tick(gravity)` from the screen's `tick`, `draw`), no allocation after construction. The cut and pour screens use it for squirt, splash and sparkle. Opening a plain screen over a container screen (the Prep Table's minigames) keeps the parent and returns to it with `setScreen(parent)` when the opening is shut; the result is sent from `onClose` after Done or the release, then a short flourish plays before closing.

## Skill tree screen (`SkillTreeScreen`)

- Top row: Trees / Equip (`GuiStyle.tabs`). Below it, one tab per pool.
- The info plate is the bottom strip. Left column: name, effect, cost, status, needs-trait (stacked). Right side: the description, sunk as an inset, scrolled by the wheel.
- Equip tab: same canvas, strip and plate, no pan or zoom. Cast keys as orbs along the top, owned techniques as orbs in centred rows beneath (`equipSpots`). Picking a technique (or a key, which picks what it holds) puts its name, granting gear, stamina, key and description on the plate, with the same rising key buttons the tree uses (`slotButtons`). Pressing a cast key while a technique is picked (here or on an owned tree node) slots it there. No scroll yet: a long list runs off the bottom.
- Node icons are read at their own size (16 or 64); `blit` with the texture's real side, not a fixed 64.

## Not built

- `SkinPopup`: categories, icons per entry, hover suppression on the host's widgets under it.
- A text field widget: the popup's search box is a borderless vanilla `EditBox` inside an `inset`; `ProgrammerScreen` uses a vanilla `EditBox` for literals. Make a `GuiStyle` one when a second screen needs typing.
- A shared scrolling list widget: each list does its own scroll, rows and hit-testing (patterns above).
- Marquee text in `GuiStyle` (`ToolTableScreen.fit` is local).
- UI particles. Vanilla's `ParticleEngine` draws only in the world, never on a screen. Two screens fake it locally: `SkilletScreen` keeps a fixed pool of 40 specks (bubbles, steam, smoke) advanced in `tick()` and drawn as fills, and `MYSTIC` draws its sparkles from a hash of the clock, with no state. A shared `GuiStyle` emitter would be the Skillet pool made general (fixed array, no allocation per frame, ticked, drawn in skin-free colours); build it when a second screen wants particles.

## Why it is like this

- **Reveal belongs to the skin, timing to `Opening`.** A new skin gets its own animation by overriding two methods, and every screen and popup picks it up.
- **Fills, not textures.** The look has no asset to license and is one file to change. `STONE` is the exception in spirit: it draws vanilla's cobblestone in place, not a copy.
- **Widgets read state each frame.** A screen cannot drift from the value it controls.
- **One gauge.** A temperature, a progress bar and a stored charge are the same widget with different suppliers, so they read alike on every skin.
- **HUDs on `VANILLA`.** What sits over the world reads as the game's own, not as one machine's.
