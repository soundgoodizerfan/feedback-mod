# GUI skins

How screens are drawn in Feedback. Everything is code-drawn with fills and the vanilla font: no skin texture files, nothing to license, and every screen on a skin changes when `client/GuiSkin.java` does. Adding a skin is the `add-gui-skin` skill; this document is the map of what exists and how to use it.

## The pieces

| File | What it is |
|---|---|
| `GuiSkin` | The enum of materials: `BLUEPRINT` (paper), `FORGE` (iron and brass), `PANEL` (crinkle steel), `STONE` (cobblestone hearth). Owns colours, drawing primitives, sounds and the open/close reveal. |
| `GuiStyle` | Widget factories, all drawn through a skin. A screen never hand-draws a button. |
| `Opening` | A screen's arrival and departure in time: world dims, the skin reveals the panel, skin sounds play, input waits until it is open. |
| `SkinnedScreen<M>` | Base class for container screens. Holds an `Opening`; subclass draws the panel in `renderBg`. |
| `SkinPopup<T>` | A second window over a skinned screen that picks one thing from a list. |

## GuiSkin

Constructor colours: `ink` (body text), `dim` (labels), `accent` (selected, just changed), `warn` (past a limit), `well` (floor of a sunken area), `trim` (its rim), `plate` (a raised label plate). The shared primitives are built from these seven, so a new skin gets them for free.

Primitives, all `skin.x(g, ...)`:

- `panel(g, x, y, w, h)` -- the window. Abstract; each skin draws its own.
- `slot(g, x, y)` -- socket round a 16x16 item. Abstract.
- `inset` -- a sunken area (gauges, text wells, readouts).
- `plaque` -- a raised plate for a stamped readout.
- `bar(g, x, y, w, h, fill, mark)` -- a gauge in a well; the part past `mark` draws in `warn`.
- `row(g, x, y, w, h, picked, hovered)` -- a list row's highlight.
- `scrollbar(g, x, y, h, total, visible, at)` -- a thumb; draws nothing when it all fits.
- `ink()`, `dim()`, `accent()`, `warn()` -- colours for text.

Per-skin behaviour (overridable): `click()`, `open()`, `close()` sounds; `beginReveal` / `endReveal(g, x, y, w, h, t)` bracket the panel's drawing with `t` from 0 shut to 1 open. A skin that does not override them opens with no ceremony (`STONE`). `GuiSkin.play(sound, pitch)` plays a UI sound.

Reveals today: `BLUEPRINT` unrolls from the left, `FORGE` parts two shutters from the middle, `PANEL` drops into its mounting and powers up.

## GuiStyle widgets

All take the skin first and return a vanilla widget: `button`, `primary` (the one button that commits), `toggle` (instrument lever), `checkbox`, `stepper` (a value between two arrows; `step` is told -1 or +1), `slider` (snaps to a step; Left/Right when focused), `tabs`. State suppliers are read every frame, so a widget shows what is set, not what was clicked. Also `easeOut`, `hudCard` (HUD readouts on the `PANEL` look), `stamp`, `line`, `perimeter`.

## Making a screen

Container screen:

1. `extends SkinnedScreen<MyMenu>`, pass `GuiSkin.X` to `super(menu, inventory, title, skin)`.
2. `renderBg` calls `skin.panel(...)`, then `skin.slot(...)` per slot, `inventory(g, x, y)` for the player's.
3. Build widgets from `GuiStyle` in `init`.
4. `settled()` is false while it opens or closes; gate your own click handling on it.

A plain (non-container) screen holds an `Opening` and calls `opening.start()` in `init`, `opening.render(...)` around its body, `opening.close()` on close. See `MirrorScreen`.

Screen-local pieces (gauges, bezels) that draw their own colours will not follow the skin; say so rather than re-theming unasked.

## SkinPopup

A titled list of choices in a window that opens over the host, on the host's skin, with that skin's reveal and sounds. The host dims a little behind it and nothing under it takes input while it is up.

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

Also skip the host's own tooltips while `popup.active()`. Escape or a click outside closes it. Six rows show at once (`MAX_ROWS`), 200 wide, scrolled by the wheel. A list longer than a page gets a search box (label substring); route `charTyped` too. First user: the Blueprint Table's `+ add part` (`BlueprintTableScreen`).

Not built: categories, icons per entry, hover suppression on the host's widgets under the popup (only clicks and scroll are blocked).

## Why it is like this

- **Reveal belongs to the skin, timing to `Opening`.** A new skin gets its own animation by overriding two methods, and every screen and popup picks it up.
- **Fills, not textures.** The look has no asset to license and is one file to change. `STONE` is the exception in spirit: it draws vanilla's cobblestone in place, not a copy.
- **Widgets read state each frame.** A screen cannot drift from the value it controls.
