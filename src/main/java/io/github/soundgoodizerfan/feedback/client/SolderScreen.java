/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.net.SolderFinishPayload;
import io.github.soundgoodizerfan.feedback.process.Board;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.process.Soldering;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Hand soldering by drag (mechanics 6.22): the board's pad row, one pad per joint with a trace
 * between. Hold the button and drag the tip along it; pace is the skill. Each pad is drawn from the
 * same {@link Soldering.Pass} the server replays: untouched copper, dull (cold), shiny (wet, in the
 * window) or scorched (over). No bar, no target line: read the pad, and a thermometer on the iron
 * is the instrument for the numbers. One packet at the end (button let go past the last pad, or the
 * screen closed with the button down); closing with it up discards the drag and the board is kept,
 * so reheating the iron and coming back restarts from the first pad.
 *
 * <p>Cheap: nothing is allocated per frame, and state advances once a tick.
 */
public class SolderScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.PANEL;
    private static final Component HINT = Component.translatable("gui.feedback.solder.hint");
    /** Pad looks: untouched copper, dull (cold), shiny (wet), scorched. */
    private static final int[] LOOK = {0xFFB87333, 0xFF6E6A66, 0xFFE8EEF2, 0xFF3A2418};
    private static final Component[] LEGEND = {
            Component.translatable("gui.feedback.solder.legend.bare"), Component.translatable("gui.feedback.solder.legend.cold"),
            Component.translatable("gui.feedback.solder.legend.wet"), Component.translatable("gui.feedback.solder.legend.scorched"),
            Component.translatable("gui.feedback.solder.legend.bridged")};
    private static final int MARGIN = 12, MAX_W = 420, MIN_W = 220;

    private final Opening opening = new Opening(SKIN);
    private final Soldering.Alloy alloy;
    private final Component alloyName;
    private final Soldering.Pass pass;
    private final int pads;
    private final int[] dwell, offset;
    private final float ironStart;
    private boolean down, sent;
    /** Crouched as the iron was lifted: opening a screen releases the key, so the stance is read here, as the server reads it. */
    private final boolean braced = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isShiftKeyDown();
    private int mouseX, mouseY, pitch, x0, rowY, soundCooldown;
    // Panel layout, computed once in init (panel-relative y unless named screen).
    private static final int PITCH_MAX = 36, HEADER = 22, SWATCH = 8, ROW_H = 12, PAD_BOTTOM = 10;
    private int px, py, pw, ph, boardY, boardH, hintY, padSize;
    private boolean moved, seen;
    private int prevX, prevY;
    private java.util.List<net.minecraft.util.FormattedCharSequence> hintLines = java.util.List.of();
    private final int[] legendX = new int[LEGEND.length], legendY = new int[LEGEND.length], legendW = new int[LEGEND.length];
    private int legendRows;

    private SolderScreen(Soldering.Alloy alloy, Component alloyName, int pads, float limit, float ironTu) {
        super(Component.translatable("gui.feedback.solder.title"));
        this.alloy = alloy;
        this.alloyName = alloyName;
        this.pads = pads;
        this.ironStart = ironTu;
        this.pass = new Soldering.Pass(pads, ironTu, alloy, limit);
        this.dwell = new int[pads];
        this.offset = new int[pads];
    }

    /** Open on the client from the iron's use; does nothing if the board's solder is not a known alloy. */
    public static void open(Board board, float ironTu) {
        Map<ResourceLocation, Material> materials = MaterialTable.get().materials();
        ResourceLocation id = board.solder().orElse(null);
        Material solder = id == null ? null : materials.get(id);
        Optional<Soldering.Alloy> alloy = solder == null ? Optional.empty() : Soldering.alloy(solder);
        if (alloy.isPresent())
            Minecraft.getInstance().setScreen(new SolderScreen(alloy.get(),
                    Component.translatable("material." + id.getNamespace() + "." + id.getPath()),
                    Soldering.pads(board), Soldering.limit(board, materials), ironTu));
    }

    @Override
    protected void init() {
        opening.start();
        int maxW = Math.min(MAX_W, width - 8);
        pw = Math.max(Math.min(MIN_W, maxW), Math.min(maxW, pads * PITCH_MAX + 2 * MARGIN + 8));
        pitch = Math.max(4, Math.min(PITCH_MAX, (pw - 2 * MARGIN - 8) / pads));
        pw = Math.max(Math.min(MIN_W, maxW), Math.min(maxW, pads * pitch + 2 * MARGIN + 8));
        hintLines = font.split(HINT, pw - 2 * MARGIN);
        // Legend: one row, wrapping to more when the panel is narrow. Widths do not depend on pitch.
        int avail = pw - 2 * MARGIN, x = 0, row = 0;
        for (int i = 0; i < LEGEND.length; i++) {
            legendW[i] = (i == 4 ? 2 * SWATCH - 2 : SWATCH) + 4 + font.width(LEGEND[i]);
            if (x > 0 && x + legendW[i] > avail) {
                x = 0;
                row++;
            }
            legendX[i] = x;
            legendY[i] = row;
            x += legendW[i] + 10;
        }
        legendRows = row + 1;
        // Shrink the board until the whole panel fits the window.
        while (true) {
            padSize = Math.max(4, pitch - 8);
            boardH = 2 * pitch + 8;
            hintY = HEADER + boardH + 8 + legendRows * ROW_H + 6;
            ph = hintY + hintLines.size() * 9 + PAD_BOTTOM;
            if (ph <= height - 4 || pitch <= 4)
                break;
            pitch--;
        }
        pw = Math.max(Math.min(MIN_W, maxW), Math.min(maxW, pads * pitch + 2 * MARGIN + 8));
        px = (width - pw) / 2;
        py = Math.max(0, (height - ph) / 2);
        boardY = py + HEADER;
        x0 = px + (pw - pads * pitch) / 2;
        rowY = boardY + boardH / 2;
        hintY += py;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && opening.settled())
            down = true;
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && down) {
            down = false;
            if (dwell[pads - 1] > 0)
                send(false);
        }
        return true;
    }

    @Override
    public void tick() {
        if (!down || sent || !opening.settled())
            return;
        int p = Mth.floor((double) (mouseX - x0) / pitch);
        float off = Math.abs(mouseY - rowY) / (float) pitch;
        if (p < 0 || p >= pads || off > 1.5f)
            return;
        pass.touch(p, 1);
        dwell[p]++;
        offset[p] = Math.max(offset[p], Math.round(off * 100f));
        if (off > io.github.soundgoodizerfan.feedback.combat.skill.Skills.solderTolerance(Minecraft.getInstance().player, braced))
            pass.bridge(p);
        if (--soundCooldown <= 0 && pass.ironTu() > alloy.liquidus()) {
            soundCooldown = 6;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LAVA_EXTINGUISH, 1.6f, 0.25f));
        }
    }

    private void send(boolean held) {
        if (sent)
            return;
        sent = true;
        int n = 0;
        for (int d : dwell)
            if (d > 0)
                n++;
        java.util.List<Integer> ps = new java.util.ArrayList<>(n), ds = new java.util.ArrayList<>(n), os = new java.util.ArrayList<>(n);
        for (int p = 0; p < pads; p++)
            if (dwell[p] > 0) {
                ps.add(p);
                ds.add(Math.min(dwell[p], FTuning.SOLDER_PAD_MAX_DWELL_TICKS));
                os.add(offset[p]);
            }
        PacketDistributor.sendToServer(new SolderFinishPayload(ps, ds, os, held));
        opening.close();
    }

    @Override
    public void onClose() {
        // Closed with the button down on a started drag: lingering on the last pad until the server ends it.
        if (!sent && down)
            for (int d : dwell)
                if (d > 0) {
                    send(true);
                    return;
                }
        opening.close();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    /** The panel only; the world stays as it is. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    @Override
    public void removed() {
        // Closed with the button down on a started drag: lingering on the last pad until the server ends it.
        if (!sent && down) {
            for (int d : dwell)
                if (d > 0) {
                    send(true);
                    return;
                }
        }
        super.removed();
    }

    /** Index into {@link #LOOK}. */
    private int padLook(int p) {
        if (dwell[p] == 0)
            return 0;
        float tu = pass.padTu(p);
        if (tu < alloy.liquidus())
            return 1;
        return tu <= alloy.liquidus() + alloy.window() ? 2 : 3;
    }

    /** One pad centred on (cx, cy): dark rim, face, a glint on bare copper and wet solder. */
    private static void pad(GuiGraphics g, int cx, int cy, int size, int look) {
        int h = size / 2;
        g.fill(cx - h - 1, cy - h - 1, cx - h + size + 1, cy - h + size + 1, 0xFF0A0C0B);
        g.fill(cx - h, cy - h, cx - h + size, cy - h + size, LOOK[look]);
        if (look == 0 || look == 2)
            g.fill(cx - h + 1, cy - h + 1, cx - h + Math.max(2, size / 2), cy - h + 2, 0x90FFFFFF);
        else if (look == 3)
            g.fill(cx - h + 1, cy - h + size - 2, cx - h + size - 1, cy - h + size - 1, 0xFF140C08);
    }

    private static void blob(GuiGraphics g, int x, int y, int w) {
        g.fill(x, y - 2, x + w, y + 3, 0xFF0A0C0B);
        g.fill(x, y - 1, x + w, y + 2, LOOK[2]);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        mouseX = mx;
        mouseY = my;
        opening.render(g, width, height, px, py, pw, ph, 0f, () -> {
            SKIN.panel(g, px, py, pw, ph);
            g.drawString(font, title, px + MARGIN, py + 9, SKIN.ink(), false);
            g.drawString(font, alloyName, px + pw - MARGIN - font.width(alloyName), py + 9, SKIN.accent(), false);
            SKIN.inset(g, px + MARGIN - 2, boardY, pw - 2 * MARGIN + 4, boardH);
            // Copper trace under the pads, shadowed.
            g.fill(x0 + pitch / 2, rowY - 1, x0 + pads * pitch - pitch / 2, rowY + 2, 0xFF0A0C0B);
            g.fill(x0 + pitch / 2, rowY, x0 + pads * pitch - pitch / 2, rowY + 1, SKIN.dim());
            for (int p = 0; p < pads; p++) {
                int cx = x0 + p * pitch + pitch / 2;
                if (p + 1 < pads && pass.bridged(p))
                    blob(g, cx, rowY, pitch);
                pad(g, cx, rowY, padSize, padLook(p));
            }
            // Legend: the four pad looks, then a bridge; swatch, gap, label.
            int top = boardY + boardH + 8;
            for (int i = 0; i < LEGEND.length; i++) {
                int sx = px + MARGIN + legendX[i], sy = top + legendY[i] * ROW_H + ROW_H / 2;
                int tx;
                if (i < 4) {
                    pad(g, sx + SWATCH / 2, sy, SWATCH, i);
                    tx = sx + SWATCH + 4;
                } else {
                    blob(g, sx + SWATCH / 2, sy, SWATCH);
                    pad(g, sx + SWATCH / 2, sy, SWATCH - 2, 2);
                    pad(g, sx + SWATCH / 2 + SWATCH, sy, SWATCH - 2, 2);
                    tx = sx + 2 * SWATCH - 2 + 4;
                }
                g.drawString(font, LEGEND[i], tx, sy - 4, SKIN.dim(), false);
            }
            int hy = hintY;
            for (var l : hintLines) {
                g.drawString(font, l, px + MARGIN, hy, SKIN.dim(), false);
                hy += 9;
            }
        });
        // The tip glows with the iron's heat (a picture, not a number).
        float heat = Mth.clamp(pass.ironTu() / Math.max(1f, ironStart), 0f, 1f);
        int r = 0xA0 + (int) (0x5F * heat), gr = (int) (0x70 * heat), b = (int) (0x30 * heat);
        int tip = 0xFF000000 | r << 16 | gr << 8 | b;
        if (seen && (mx != prevX || my != prevY))
            moved = true;
        seen = true;
        prevX = mx;
        prevY = my;
        // Until the mouse moves the tip waits just left of the first pad, not at a corner.
        int tx = moved ? mx : x0 - 8, ty = moved ? my : rowY;
        g.fill(tx - 3, ty - 3, tx + 4, ty + 4, tip);
        g.fill(tx - 1, ty - 1, tx + 2, ty + 2, down ? 0xFFFFFFFF : 0xFFC0C0C0);
    }
}
