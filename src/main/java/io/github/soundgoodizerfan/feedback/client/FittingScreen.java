// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;
import io.github.soundgoodizerfan.feedback.modular.engineering.FittingOrder;
import io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The Engineering Table's fitting game: the assembly's real parts lie on a tray, each drawn as its own item, and the player seats them one
 * by one on an exploded axis below. What sits inside or under another goes first ({@link FittingOrder}, derived from the parts' roles, never
 * written per recipe). A right click slides the part along to its socket, drops it in with a click, a chime that climbs with the build
 * and sparks in its material's colour; a wrong click is a mis-seat: the part jolts, rattles back and the score drops. It never locks, so
 * the assembly can always be finished. When the last part seats the exploded axis closes up, the parts snap together in a flash and the
 * finished item rises in their place, with a stamped grade and stars. ONE result goes out ({@link MinigameScreens#report}) the moment the
 * last part seats; the closing flourish never delays it. Esc before then sends nothing and the craft stays offered. Reduce motion snaps
 * the slides and stills the bobbing, shaking and rise; sounds stay.
 */
final class FittingScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.PANEL;
    private static final int PW = 240, PH = 236, PLAY = 0, SNAP = 1, DONE = 2;
    private static final int SNAP_TICKS = 16, DONE_TICKS = 90, CELL_W = 42, CELL_H = 38, SHAKE_TICKS = 14, DROP_TICKS = 9;
    /** Screen-local colours: the tray, the bay's axis and the sparks are the subject of this screen, not the skin's. */
    private static final int TRAY = 0xFF2B2E33, TRAY_EDGE = 0xFF15171A, AXIS = 0xFF6E747C, BAND = 0xFF4FBF5A, STRIP = 0xFFC2452F,
            SPARK = 0xFFFFC04A;

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId, n;
    private final int[] rank, slot;
    private final ItemStack[] stacks;
    private final Component[] names;
    private final int[] rgb;
    private final float[] x, y, px, py;
    private final int[] bay, shake, drop, slips;
    private final Specks specks = new Specks(96);
    private final Component title = Component.translatable("gui.feedback.fitting.title");
    private final Component[] pops = {Component.translatable("gui.feedback.fitting.pop.0"), Component.translatable("gui.feedback.fitting.pop.1")};
    private final Component[] results = new Component[FittingOrder.GRADES];
    private final ItemStack made;
    private List<FormattedCharSequence> hint = List.of();
    private Component countText = Component.empty();
    private int px0, py0, sx0, sy0, ticks, phase = PLAY, seated, misses, hover = -1, snap, hold, flash, popKind = -1, popAge;
    private int mouseX, mouseY;
    private long doneAt;
    private float finalScore, partial, popX;
    private boolean sent;
    private final int[] trayCell;

    FittingScreen(Screen parent, EngineeringTableMenu menu, MinigameOpenPayload session) {
        super(Component.translatable("gui.feedback.fitting.title"));
        this.parent = parent;
        this.containerId = session.container();
        int[][] rows = FittingOrder.decode(session.data());
        this.n = rows.length;
        this.rank = new int[n];
        this.slot = new int[n];
        this.stacks = new ItemStack[n];
        this.names = new Component[n];
        this.rgb = new int[n];
        this.x = new float[n];
        this.y = new float[n];
        this.px = new float[n];
        this.py = new float[n];
        this.bay = new int[n];
        this.shake = new int[n];
        this.drop = new int[n];
        this.slips = new int[n];
        this.trayCell = new int[n];
        for (int i = 0; i < n; i++) {
            slot[i] = rows[i][0];
            rank[i] = rows[i][1];
            ItemStack s = menu.getSlot(EngineeringTableMenu.slot(EngineeringTableBlockEntity.GRID + slot[i])).getItem();
            stacks[i] = s.copyWithCount(1);
            names[i] = stacks[i].getHoverName();
            var found = stacks[i].isEmpty() ? java.util.Optional.<io.github.soundgoodizerfan.feedback.process.Material.Located>empty()
                    : MaterialIngredient.locate(stacks[i].getItem());
            rgb[i] = found.isPresent() ? MaterialColors.of(found.get().id()) : 0xB8BEC6;
            bay[i] = -1;
        }
        // The tray order is a shuffle seeded by the table, so the parts never lie in the order they go.
        int seed = containerId * 31 + n;
        int[] perm = new int[n];
        for (int i = 0; i < n; i++)
            perm[i] = i;
        for (int i = n - 1; i > 0; i--) {
            seed = seed * 1103515245 + 12345;
            int j = ((seed >>> 16) & 0x7FFF) % (i + 1);
            int t = perm[i];
            perm[i] = perm[j];
            perm[j] = t;
        }
        for (int c = 0; c < n; c++)
            trayCell[perm[c]] = c;
        this.made = menu.getSlot(EngineeringTableMenu.OUT).getItem().copy();
        for (int i = 0; i < FittingOrder.GRADES; i++)
            results[i] = Component.translatable("gui.feedback.fitting.result." + i);
        countText();
    }

    private void countText() {
        countText = Component.translatable("gui.feedback.fitting.count", seated, n);
    }

    @Override
    protected void init() {
        opening.start();
        px0 = (width - PW) / 2;
        py0 = Math.max(0, (height - PH) / 2);
        sx0 = px0 + 8;
        sy0 = py0 + 24;
        hint = font.split(Component.translatable("gui.feedback.fitting.hint"), PW - 24);
        for (int i = 0; i < n; i++) {
            x[i] = px[i] = trayX(i);
            y[i] = py[i] = trayY(i) - 30;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int cols() {
        return Math.min(n, 5);
    }

    private float trayX(int i) {
        int c = trayCell[i], cols = cols();
        int inRow = Math.min(cols, n - (c / cols) * cols);
        return sx0 + 112 + ((c % cols) - (inRow - 1) / 2f) * CELL_W;
    }

    private float trayY(int i) {
        return sy0 + 24 + (trayCell[i] / cols()) * CELL_H;
    }

    private float expl() {
        return phase == PLAY ? 1f : Math.max(0.42f, 1f - 0.58f * Math.min(1f, snap / (float) SNAP_TICKS));
    }

    private float bayX(int k) {
        float spacing = Math.min(28f, 190f / Math.max(1, n));
        return sx0 + 112 + (k - (n - 1) / 2f) * spacing * expl();
    }

    private float bayY() {
        return sy0 + 120;
    }

    private int pieceAt(double mx, double my) {
        int best = -1;
        for (int i = 0; i < n; i++) {
            if (bay[i] >= 0)
                continue;
            if (Math.abs(mx - x[i]) <= 18 && Math.abs(my - y[i]) <= 18)
                best = i;
        }
        return best;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase != PLAY) {
            if (phase == DONE)
                hold = 0;
            return true;
        }
        if (!opening.settled() || button != 0)
            return true;
        int i = pieceAt(mx, my);
        if (i < 0)
            return true;
        boolean[] done = new boolean[n];
        for (int k = 0; k < n; k++)
            done[k] = bay[k] >= 0;
        if (FittingOrder.fits(rank, done, i))
            seat(i);
        else
            misseat(i);
        return true;
    }

    private void seat(int i) {
        bay[i] = seated++;
        drop[i] = DROP_TICKS;
        float progress = seated / (float) n;
        GuiSkin.play(SoundEvents.LEVER_CLICK, 1.2f + 0.3f * progress, 0.9f);
        GuiSkin.play(SoundEvents.ANVIL_PLACE, 1.7f, 0.25f);
        GuiSkin.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f + 0.7f * progress, 0.5f);
        specks.burst(bayX(bay[i]), bayY() + 8, 9, 1.6f, 2.0f, rgb[i], 22, 2);
        specks.burst(bayX(bay[i]), bayY() + 8, 4, 1.2f, 1.4f, SPARK, 14, 1);
        popKind = 0;
        popAge = 0;
        popX = bayX(bay[i]);
        countText();
        if (seated >= n)
            finish();
    }

    private void misseat(int i) {
        misses++;
        slips[i]++;
        shake[i] = SHAKE_TICKS;
        GuiSkin.play(SoundEvents.IRON_TRAPDOOR_OPEN, 0.55f, 0.6f);
        GuiSkin.play(SoundEvents.CHAIN_HIT, 0.7f, 0.8f);
        GuiSkin.play(SoundEvents.ANVIL_LAND, 0.5f, 0.2f);
        specks.burst(x[i], y[i] + 8, 6, 1.5f, 1.2f, 0x8A8F96, 16, 2);
        popKind = 1;
        popAge = 0;
        popX = x[i];
    }

    /** The last part is seated: send the one result, then close the axis up and let the finished item rise. */
    private void finish() {
        finalScore = FittingOrder.score(n, misses);
        if (!sent) {
            sent = true;
            MinigameScreens.report(containerId, finalScore);
        }
        phase = SNAP;
        snap = 0;
    }

    @Override
    public void tick() {
        ticks++;
        popAge++;
        boolean still = ClientSettings.reduceMotion();
        int over = phase == PLAY && opening.settled() ? pieceAt(mouseX, mouseY) : -1;
        if (over != hover) {
            hover = over;
            if (over >= 0)
                GuiSkin.play(SoundEvents.CHAIN_STEP, 1.8f, 0.12f);
        }
        float follow = still ? 1f : 0.38f;
        for (int i = 0; i < n; i++) {
            px[i] = x[i];
            py[i] = y[i];
            float tx, ty;
            if (bay[i] >= 0) {
                tx = bayX(bay[i]);
                ty = bayY();
            } else {
                tx = trayX(i);
                ty = trayY(i) + (still || i == hover ? 0f : Mth.sin((ticks + i * 9) * 0.11f) * 1.2f) - (i == hover ? 3f : 0f);
            }
            x[i] += (tx - x[i]) * follow;
            y[i] += (ty - y[i]) * follow;
            if (shake[i] > 0)
                shake[i]--;
            if (drop[i] > 0)
                drop[i]--;
        }
        if (phase == SNAP) {
            if (++snap >= SNAP_TICKS) {
                phase = DONE;
                hold = DONE_TICKS;
                doneAt = Util.getMillis();
                flash = 12;
                int star = FittingOrder.grade(finalScore);
                specks.burst(sx0 + 112, bayY(), 22, 2.6f, 3.2f, 0xFFF0A0, 30, 2);
                GuiSkin.play(SoundEvents.ANVIL_PLACE, 1.4f, 0.5f);
                GuiSkin.play(star >= 3 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ANVIL_LAND, star >= 3 ? 1f : 0.8f, 0.7f);
            }
        } else if (phase == DONE) {
            if (flash > 0)
                flash--;
            if (--hold <= 0 && !opening.closing())
                opening.close();
        }
        specks.tick(0.14f);
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partialTick) {
    }

    private GuiGraphics frameG;
    private final Runnable paintFrame = () -> paint(frameG);

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        if (opening.shut()) {
            Minecraft.getInstance().setScreen(parent);
            return;
        }
        mouseX = mx;
        mouseY = my;
        partial = partialTick;
        frameG = g;
        opening.render(g, width, height, px0, py0, PW, PH, 0f, paintFrame);
    }

    private void paint(GuiGraphics g) {
        SKIN.panel(g, px0, py0, PW, PH);
        g.drawString(font, title, px0 + 12, py0 + 9, SKIN.ink(), false);
        // One mark per part, filled when it is seated, tinted by how many slips it cost.
        for (int i = 0; i < n; i++) {
            int mx = px0 + PW - 12 - (n - i) * 11 + 3;
            int seatedHere = rankSeated(i);
            int col = seatedHere < 0 ? 0xFF2A2D32 : seatedHere == 0 ? BAND : seatedHere == 1 ? SPARK : STRIP;
            g.fill(mx - 1, py0 + 8, mx + 8, py0 + 17, 0xFF000000);
            g.fill(mx, py0 + 9, mx + 7, py0 + 16, col);
        }
        SKIN.inset(g, sx0, sy0, PW - 16, 152);
        tray(g);
        bayAxis(g);
        // Parts: seated ones under, the tray ones over them, the hovered one last.
        if (phase != DONE) {
            for (int pass = 0; pass < 2; pass++)
                for (int i = 0; i < n; i++)
                    if ((bay[i] >= 0) == (pass == 0) && i != hover)
                        piece(g, i);
            if (hover >= 0)
                piece(g, hover);
        } else {
            finished(g);
        }
        specks.draw(g, partial);
        popup(g);
        if (flash > 0)
            g.fill(sx0 + 1, sy0 + 1, sx0 + PW - 17, sy0 + 151, ((int) (flash / 12f * 150f) << 24) | 0xFFFFFF);
        int hy = py0 + 182;
        if (phase == DONE) {
            long age = Util.getMillis() - doneAt;
            int star = FittingOrder.grade(finalScore);
            GuiStyle.stamp(g, results[star], px0 + PW / 2f - 20, sy0 + 36, age, star >= 3 ? 0x5BC25B : star >= 2 ? 0xE0B040 : 0xD05040);
            stars(g, px0 + PW / 2 - 24, hy + 4, star + 1);
        } else {
            g.drawString(font, countText, px0 + 12, hy, SKIN.dim(), false);
            hy += 12;
            for (var l : hint) {
                g.drawString(font, l, px0 + 12, hy, SKIN.dim(), false);
                hy += 9;
            }
        }
    }

    /** 0 slips for a seated piece with none, else its count; -1 while it is still on the tray. */
    private int rankSeated(int mark) {
        for (int i = 0; i < n; i++)
            if (bay[i] == mark)
                return slips[i];
        return -1;
    }

    /** The tray the loose parts lie on: a dark pad with a lip, and a pedestal under each part. */
    private void tray(GuiGraphics g) {
        int rows = (n + 4) / 5;
        int x0 = sx0 + 6, x1 = sx0 + PW - 16 - 6, y0 = sy0 + 6, y1 = sy0 + 12 + rows * CELL_H;
        g.fill(x0, y0, x1, y1, TRAY_EDGE);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, TRAY);
        g.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 3, 0x30FFFFFF);
        for (int i = 0; i < n; i++) {
            if (bay[i] >= 0)
                continue;
            int cx = Math.round(trayX(i)), cy = Math.round(trayY(i));
            g.fill(cx - 17, cy + 14, cx + 17, cy + 17, 0x60000000);
            g.fill(cx - 15, cy + 12, cx + 15, cy + 14, 0x30FFFFFF);
        }
    }

    /** The exploded axis the parts seat on: a dashed line through a row of empty sockets, the next one lit softly. */
    private void bayAxis(GuiGraphics g) {
        int by = Math.round(bayY()) + 8;
        float ex = expl();
        int left = Math.round(bayX(0)) - 16, right = Math.round(bayX(n - 1)) + 16;
        for (int xx = left; xx < right; xx += 6)
            g.fill(xx, by + 17, Math.min(right, xx + 3), by + 18, AXIS);
        if (phase == DONE)
            return;
        for (int k = 0; k < n; k++) {
            int cx = Math.round(bayX(k));
            int col = k == seated && phase == PLAY ? 0x90FFFFFF : 0x38FFFFFF;
            g.fill(cx - 13, by - 12, cx + 13, by - 11, col);
            g.fill(cx - 13, by + 14, cx + 13, by + 15, col);
            g.fill(cx - 13, by - 11, cx - 12, by + 14, col);
            g.fill(cx + 12, by - 11, cx + 13, by + 14, col);
        }
    }

    /** One part as its own item: lerped between ticks, scaled 2x on the tray and 1.5x seated, jolting after a mis-seat. */
    private void piece(GuiGraphics g, int i) {
        boolean in = bay[i] >= 0;
        float cx = Mth.lerp(partial, px[i], x[i]), cy = Mth.lerp(partial, py[i], y[i]);
        float scale = in ? 1.5f : 2f;
        if (!in && i == hover)
            scale = 2.25f;
        if (shake[i] > 0 && !ClientSettings.reduceMotion())
            cx += Mth.sin((SHAKE_TICKS - shake[i] + partial) * 2.6f) * 4f * shake[i] / SHAKE_TICKS;
        if (drop[i] > 0 && !ClientSettings.reduceMotion()) {
            float d = (drop[i] - partial) / DROP_TICKS;
            cy -= Mth.sin(Mth.clamp(d, 0f, 1f) * Mth.PI) * 7f;
        }
        float half = 8f * scale;
        if (!in && i == hover)
            outline(g, cx, cy, half + 3, rgb[i], 0xC0);
        else if (shake[i] > 0)
            outline(g, cx, cy, half + 3, STRIP & 0xFFFFFF, 0xC0 * shake[i] / SHAKE_TICKS);
        g.pose().pushPose();
        g.pose().translate(cx - half, cy - half, 0f);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(stacks[i], 0, 0);
        g.pose().popPose();
        if (!in && i == hover) {
            Component nm = names[i];
            g.drawString(font, nm, Math.round(sx0 + 112 - font.width(nm) / 2f), sy0 + 134, SKIN.ink(), false);
        }
    }

    private static void outline(GuiGraphics g, float cx, float cy, float r, int rgb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha)) << 24 | (rgb & 0xFFFFFF);
        int x0 = Math.round(cx - r), y0 = Math.round(cy - r), x1 = Math.round(cx + r), y1 = Math.round(cy + r);
        g.fill(x0, y0, x1, y0 + 1, a);
        g.fill(x0, y1 - 1, x1, y1, a);
        g.fill(x0, y0, x0 + 1, y1, a);
        g.fill(x1 - 1, y0, x1, y1, a);
    }

    /** While the axis closes the seated parts draw as they are; once snapped, the finished item rises in their place. */
    private void finished(GuiGraphics g) {
        long age = Util.getMillis() - doneAt;
        float rise = ClientSettings.reduceMotion() ? 1f : Mth.clamp(age / 260f, 0f, 1f);
        float scale = 1.5f + 1.5f * (1f - (1f - rise) * (1f - rise));
        float half = 8f * scale;
        ItemStack show = made.isEmpty() ? stacks[0] : made;
        g.pose().pushPose();
        g.pose().translate(sx0 + 112 - half, bayY() - half - 2, 0f);
        g.pose().scale(scale, scale, 1f);
        g.renderItem(show, 0, 0);
        g.pose().popPose();
    }

    /** The word for the last click, floating up from where it happened and fading. */
    private void popup(GuiGraphics g) {
        if (popKind < 0 || popAge > 36)
            return;
        int a = Math.min(255, 360 - popAge * 10);
        if (a <= 0)
            return;
        int rise = ClientSettings.reduceMotion() ? 0 : Math.min(12, popAge);
        Component w = pops[popKind];
        g.drawString(font, w, Math.round(popX - font.width(w) / 2f), Math.round(bayY()) - 26 - rise, (a << 24) | (popKind == 0 ? BAND : STRIP), false);
    }

    /** Five diamonds, lit by the final grade (one to five). */
    private void stars(GuiGraphics g, int x, int y, int lit) {
        for (int i = 0; i < 5; i++) {
            int cx = x + i * 10 + 4;
            int col = i < lit ? 0xFFF0C040 : 0xFF2A2D32;
            for (int r = -3; r <= 3; r++)
                g.fill(cx - (4 - Math.abs(r)) + 1, y + 4 + r, cx + (4 - Math.abs(r)), y + 5 + r, col);
        }
    }
}
