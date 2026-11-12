// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.PartBenchMenu;
import io.github.soundgoodizerfan.feedback.modular.PartBenchShaping;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * The aim strip at the bench: when the shape picked makes a part whose role declares axes, one row per axis (its name, a
 * minus/plus stepper reading the aimed value in its unit, the band word the value falls in), the stock the aim costs, and
 * a preview of the plate in its material whose proportions follow the aim and which can be dragged. Every change is a
 * menu button ({@link PartBenchMenu#BTN_AXIS}, {@link PartBenchMenu#BTN_AXIS_SET}); the server holds the aim and the
 * screen only reads it back, so nothing here is trusted. Strings are rebuilt only when the aim or the pick changes.
 */
final class AxisPanel {

    /** Above the fan's top orb layer ({@code BenchFan}: layer times 250). */
    private static final float LAYER = 900f;
    private static final int W = 200, ROW = 14, PREV_W = 92, LABEL_W = 62, STEP_X = 64, STEP_W = 86, BAND_X = 154;

    private final BenchScreen host;
    private final GuiSkin skin;
    private final AbstractWidget[] steppers = new AbstractWidget[PartBenchMenu.MAX_AIM];
    private List<Map.Entry<String, ModularType.Axis>> axes = List.of();
    private PartBenchShaping.Option option;
    private int pick = -2, generation = -1;
    private final int[] raw = new int[PartBenchMenu.MAX_AIM];
    private final String[] labels = new String[PartBenchMenu.MAX_AIM], values = new String[PartBenchMenu.MAX_AIM], bands = new String[PartBenchMenu.MAX_AIM];
    private String cost = "";
    private int rgb = 0xC0C0C0;
    private boolean dragging;
    private final int[] startN = new int[2], lastN = new int[2];
    private int startX, startY;

    AxisPanel(BenchScreen host, GuiSkin skin) {
        this.host = host;
        this.skin = skin;
    }

    /** Builds the steppers; the screen adds them. */
    void init(java.util.function.Consumer<AbstractWidget> add) {
        for (int i = 0; i < steppers.length; i++) {
            int a = i;
            steppers[i] = GuiStyle.stepper(skin, 0, 0, STEP_W, 12, "-", "+", () -> values[a] == null ? "" : values[a],
                    d -> host.press(PartBenchMenu.BTN_AXIS + 2 * a + (d > 0 ? 1 : 0)), false, null);
            steppers[i].visible = false;
            add.accept(steppers[i]);
        }
        pick = -2;
    }

    boolean visible() {
        return !axes.isEmpty();
    }

    /** Whether the pointer is on the strip or its plate preview, which sit over the fan. */
    boolean covers(double mx, double my) {
        if (axes.isEmpty() || host.panelBusy())
            return false;
        return mx >= x0() && mx < x0() + W + 4 + PREV_W && my >= y0() && my < y0() + height();
    }

    /** The steppers, drawn by hand on the raised layer {@link #draw} uses, so no fan node lies over them. */
    void renderSteppers(GuiGraphics g, int mx, int my, float partial) {
        g.pose().pushPose();
        g.pose().translate(0, 0, LAYER);
        for (AbstractWidget w : steppers)
            if (w.visible)
                w.render(g, mx, my, partial);
        g.pose().popPose();
    }

    private int height() {
        return 6 + axes.size() * ROW + 12;
    }

    private int x0() {
        return host.left() + BenchScreen.VX + 4;
    }

    private int y0() {
        return host.top() + BenchScreen.VY + BenchScreen.VH - 14 - height();
    }

    /** Re-reads the pick and the aim; call every tick. */
    void tick() {
        var level = host.level();
        PartBenchMenu menu = host.menu();
        boolean on = level != null && menu.mode() == PartBenchMenu.MODE_PART && !menu.cutTab();
        int p = on ? menu.picked() : -1;
        if (p != pick || generation != ModularTypes.generation()) {
            pick = p;
            generation = ModularTypes.generation();
            option = on ? menu.option(level).orElse(null) : null;
            axes = option == null ? List.of() : PartBenchShaping.aimable(option);
            if (axes.size() > PartBenchMenu.MAX_AIM)
                axes = axes.subList(0, PartBenchMenu.MAX_AIM);
            for (int i = 0; i < axes.size(); i++) {
                labels[i] = I18n.get("modular.feedback.axis." + axes.get(i).getKey());
                raw[i] = Integer.MIN_VALUE;
            }
            cost = "";
            dragging = false;
        }
        for (int i = 0; i < steppers.length; i++)
            steppers[i].visible = i < axes.size() && !host.panelBusy();
        if (axes.isEmpty())
            return;
        boolean changed = false;
        for (int i = 0; i < axes.size(); i++) {
            int now = Math.round(menu.aim(i) * 10f);
            if (now == raw[i])
                continue;
            raw[i] = now;
            changed = true;
            ModularType.Axis ax = axes.get(i).getValue();
            float v = now / 10f;
            String unit = ax.unit().isEmpty() ? "" : " " + (I18n.exists("modular.feedback.unit." + ax.unit()) ? I18n.get("modular.feedback.unit." + ax.unit()) : ax.unit());
            values[i] = (ax.grain() < 1f ? String.format(Locale.ROOT, "%.1f", v) : Integer.toString(Math.round(v))) + unit;
            ModularType.Band band = ax.band(v);
            bands[i] = band == null ? "" : I18n.get("modular.feedback.band." + band.name());
        }
        if (changed || cost.isEmpty()) {
            cost = I18n.get("gui.feedback.bench.axis.stock", option.stockMb(menu.aimScale(option)));
            menu.stuff(level).ifPresent(l -> rgb = MaterialColors.of(l.id()));
        }
        int y = y0();
        for (int i = 0; i < axes.size(); i++) {
            steppers[i].setX(x0() + STEP_X);
            steppers[i].setY(y + 4 + i * ROW);
        }
    }

    // --- drawing ----------------------------------------------------------------------------------

    void draw(GuiGraphics g, Font font) {
        if (axes.isEmpty() || host.panelBusy())
            return;
        g.pose().pushPose();
        g.pose().translate(0, 0, LAYER);
        int x = x0(), y = y0(), h = height();
        skin.plaque(g, x, y, W, h);
        for (int i = 0; i < axes.size(); i++) {
            int ry = y + 4 + i * ROW;
            g.drawString(font, labels[i], x + 4, ry + 2, skin.dim(), false);
            g.drawString(font, bands[i] == null ? "" : bands[i], x + BAND_X, ry + 2, skin.ink(), false);
        }
        g.drawString(font, cost, x + 4, y + 5 + axes.size() * ROW, skin.dim(), false);
        int px = x + W + 4;
        skin.inset(g, px, y, PREV_W, h);
        int iw = PREV_W - 8, ih = h - 8;
        ModularType.Axis v = axes.get(0).getValue();
        ModularType.Axis hz = axes.size() > 1 ? axes.get(1).getValue() : null;
        int cx = px + PREV_W / 2, cy = y + h / 2;
        // The default size, ghosted, so the aim reads against it; then the plate as aimed.
        box(g, cx, cy, iw, ih, frac(v, v.def()), hz == null ? 0.6f : frac(hz, hz.def()), 0x60000000 | skin.dim() & 0xFFFFFF, false);
        box(g, cx, cy, iw, ih, frac(v, raw[0] / 10f), hz == null ? 0.6f : frac(hz, raw[1] / 10f), 0xFF000000 | rgb, true);
        g.pose().popPose();
    }

    private static float frac(ModularType.Axis a, float value) {
        return 0.25f + 0.75f * Mth.clamp((value - a.min()) / Math.max(1e-3f, a.max() - a.min()), 0f, 1f);
    }

    private void box(GuiGraphics g, int cx, int cy, int iw, int ih, float fv, float fh, int colour, boolean solid) {
        int w = Math.round(iw * fh), h = Math.round(ih * fv);
        int x = cx - w / 2, y = cy - h / 2;
        if (solid) {
            g.fill(x, y, x + w, y + h, colour);
            g.renderOutline(x, y, w, h, 0xFF000000 | skin.dim() & 0xFFFFFF);
        } else
            g.renderOutline(x, y, w, h, colour);
    }

    void tooltip(GuiGraphics g, Font font, int mx, int my) {
        if (axes.isEmpty() || host.panelBusy())
            return;
        int x = x0(), y = y0();
        if (mx < x + 4 || mx >= x + 4 + LABEL_W || my < y + 4)
            return;
        int i = (my - (y + 4)) / ROW;
        if (i >= axes.size())
            return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 1000);
        g.renderTooltip(font, font.split(Component.translatable("modular.feedback.axis." + axes.get(i).getKey() + ".tip"), 190), mx, my);
        g.pose().popPose();
    }

    // --- dragging the plate ---------------------------------------------------------------------------

    private boolean inPreview(double mx, double my) {
        int px = x0() + W + 4;
        return mx >= px && mx < px + PREV_W && my >= y0() && my < y0() + height();
    }

    private int gridOf(int a) {
        ModularType.Axis ax = axes.get(a).getValue();
        return Math.round((raw[a] / 10f - ax.min()) / ax.grain());
    }

    boolean click(double mx, double my, int button) {
        if (axes.isEmpty() || button != 0 || host.panelBusy() || !inPreview(mx, my))
            return false;
        dragging = true;
        startX = (int) mx;
        startY = (int) my;
        for (int a = 0; a < Math.min(2, axes.size()); a++)
            startN[a] = lastN[a] = gridOf(a);
        return true;
    }

    boolean drag(double mx, double my) {
        if (!dragging || axes.isEmpty())
            return false;
        for (int a = 0; a < Math.min(2, axes.size()); a++) {
            ModularType.Axis ax = axes.get(a).getValue();
            int top = Math.round((ax.max() - ax.min()) / ax.grain());
            // The first axis is the plate's height (up is more), the second its width; a pixel run the length of the box spans the range.
            float span = a == 0 ? height() - 8 : PREV_W - 8;
            float pxPerGrain = Math.max(0.5f, span / Math.max(1, top));
            double delta = a == 0 ? startY - my : mx - startX;
            int n = Mth.clamp(startN[a] + Math.round((float) (delta / pxPerGrain)), 0, Math.min(top, 999));
            if (n != lastN[a]) {
                lastN[a] = n;
                host.press(PartBenchMenu.BTN_AXIS_SET + 1000 * a + n);
            }
        }
        return true;
    }

    void release() {
        dragging = false;
    }
}
