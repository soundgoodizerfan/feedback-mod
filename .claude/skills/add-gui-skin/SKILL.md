---
name: add-gui-skin
description: Add a new GuiSkin (a screen's material and era look) to Feedback and apply it to a screen. Use when the user wants a new GUI theme, a screen re-skinned, or a skin tied to a tech era.
argument-hint: <skin name> <material/era> [screen to apply to]
---

# add-gui-skin

**Read `docs/gui_skins.md` first** (required before any GUI work: library, draw order, inset vs panel, text layout).

A `GuiSkin` (`client/GuiSkin.java`) is a screen's *material*, picked by the era of the block's tech. Everything is code-drawn; there is no skin texture file.

1. **Pick the material and era.** One line: what is it made of, what tech era does it say? The skin should be recognisable from the material alone. Ask the user if either is unstated. Check the existing six (`docs/gui_skins.md`, The skins) are not already close; widen one rather than fork it.
2. **Add the enum constant** at the end of `GuiSkin.java`, after the last constant's `}` (change `};` to `},`). Constructor args are `ink, dim, accent, warn, well, trim, plate`: body text, labels, selected or just-changed, past-limit, then the floor of a sunken area, its rim, and a raised plate. The shared widgets (`inset`, `plaque`, `bar`, `checkbox`, `slider`, `tab`, `row`, `scrollbar`) are built from those seven, so a new skin gets them without drawing each. Each constant is a body with its own private colours and, if it uses them, fixed-seed `Random` noise tables (fixed so nothing crawls).
3. **Implement the three abstract methods**:
   - `panel(g, x, y, w, h)`: shadow (`x+3, y+4`), the face, bevel or edge. Keep trim clear of the inset a screen draws inside.
   - `slot(g, x, y)`: a socket around the 16x16 item at (x, y), drawn from `x-1` to `x+17`.
   - `button(...)` (package-private): states are inactive, primary, plain; `glow` 0..1 for hover, `down` shifts the label 1px. Primary is the one accent-coloured action.
   - `click()` sound.
4. **Optional**: `open()`/`close()` sounds (null means silent), and `beginReveal`/`endReveal` for the open animation (scissor and `g.flush()` in `endReveal`, or a pose push/pop). Leave them out for a skin with no ceremony.
5. **Textures.** Prefer fills. To draw a texture, reference it by `ResourceLocation` and `g.blit` it tiled (`blit(tex, x, y, 0, 0, w, h, texW, texH)` repeats). A *vanilla* texture drawn in place is not copied, so owes nothing. Anything copied into `assets/` needs its row in `THIRD-PARTY-LICENSES.md` in the same change; assets from Create or Futureazoo are never allowed (CLAUDE.md).
6. **Apply it.** A container screen extends `SkinnedScreen` and passes the skin to `super(menu, inventory, title, GuiSkin.X)`; its `renderBg` calls `skin.panel` and `skin.slot`. A plain screen holds an `Opening`. Screen-local pieces (gauges, bezels) draw their own colours and will not follow the skin; mention that to the user rather than re-theming unasked.
7. **Docs.** Update the `client/` row of docs/source_layout.md (the skin list and which screens use it). If the skin is tied to a progression era, the era is design content: say so rather than editing `feedback_philosophy.md`.
8. **Verify by compiling** (`./gradlew compileJava`), not game tests. Then give the user a short in-game checklist (open the screen; panel, slots, buttons legible; sounds right). Run `./build-and-deploy.sh` only after the user confirms it looks right.
