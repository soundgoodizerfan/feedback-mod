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

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.Shingling;
import io.github.soundgoodizerfan.feedback.net.ShingleBeginPayload;
import io.github.soundgoodizerfan.feedback.net.ShingleFinishPayload;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shingling a bloom on the anvil: placement and herding. The billet is drawn as a cross-section glowing by its heat with dark slag pockets
 * inside; a click strikes where the pointer is (left light, right heavy), squeezing nearby pockets outward until they leak off the edge;
 * R turns the piece a quarter; Space or Enter puts the hammer down. The pockets are the readout: they stiffen and go dull when the slag
 * freezes, and a pasty slag creeps. It plays the same {@link Shingling} the server replays, and sends only the inputs, once, at the end.
 * The billet, pockets and sparks are the subject of the screen, so they use their own colours rather than the skin's.
 */
final class ShinglePanel implements WorkPanel {

    private static final int PX = 96, SPARKS = 48, MAX_EVENTS = 256;

    private enum Phase { NONE, PLAY, DONE }

    private final BenchScreen host;
    private Phase phase = Phase.NONE;
    private Shingling model;
    private Shingling.Setup setup;
    private int ticks, count, leakedSeen, flash;
    private final int[] evTick = new int[MAX_EVENTS], evKind = new int[MAX_EVENTS], evX = new int[MAX_EVENTS], evY = new int[MAX_EVENTS];
    private Shingling.Result result;
    /** Spark pool: x, y, vx, vy, life left; fixed, advanced in tick(). */
    private final float[] sx = new float[SPARKS], sy = new float[SPARKS], svx = new float[SPARKS], svy = new float[SPARKS];
    private final int[] slife = new int[SPARKS];
    private int sparkAt;
    private final net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create();
    private Component hint, doneText;

    ShinglePanel(BenchScreen host) {
        this.host = host;
    }

    @Override
    public boolean busy() {
        return phase != Phase.NONE;
    }

    /** Begin on the bloom in the centre if the hammer, the heat and the figures allow it. */
    boolean start() {
        var level = host.level();
        if (phase != Phase.NONE || level == null || !host.menu().hasHammer())
            return false;
        var found = host.menu().shingleSetup(level, host.menu().centreStack());
        if (found.isEmpty() || found.get().temperature() < found.get().minWork() && found.get().hold() < found.get().minWork())
            return false;
        setup = found.get();
        model = new Shingling(setup);
        ticks = 0;
        count = 0;
        leakedSeen = 0;
        phase = Phase.PLAY;
        hint = Component.translatable("screen.feedback.shingle.hint");
        PacketDistributor.sendToServer(new ShingleBeginPayload(host.menu().containerId));
        return true;
    }

    @Override
    public void tick() {
        for (int i = 0; i < SPARKS; i++)
            if (slife[i] > 0) {
                slife[i]--;
                sx[i] += svx[i];
                sy[i] += svy[i];
                svy[i] += 0.25f;
            }
        if (flash > 0)
            flash--;
        if (phase != Phase.PLAY)
            return;
        ticks++;
        // A slag dribble hisses while pockets are leaving; the pitch of the strike follows the heat.
        boolean spent = model.result().remaining() == 0 || ticks >= FTuning.SHINGLE_MAX_TICKS
                || !model.workable(ticks) && ticks > 20;
        if (spent)
            finish();
    }

    @Override
    public boolean click(double mouseX, double mouseY, int button) {
        if (phase == Phase.DONE) {
            phase = Phase.NONE;
            return true;
        }
        if (phase != Phase.PLAY)
            return false;
        if (button != 0 && button != 1)
            return true;
        float w = model.width() * PX, h = model.height() * PX;
        float x0 = host.slotCx() - w / 2f, y0 = host.slotCy() - h / 2f - 4;
        float fx = (float) (mouseX - x0) / w, fy = (float) (mouseY - y0) / h;
        if (fx < -0.05f || fx > 1.05f || fy < -0.05f || fy > 1.05f)
            return true;
        record(button == 1 ? Shingling.Kind.HEAVY : Shingling.Kind.LIGHT, fx, fy);
        return true;
    }

    @Override
    public boolean key(int key, int scan, int modifiers) {
        if (phase == Phase.NONE || key == InputConstants.KEY_ESCAPE)
            return false;
        if (phase == Phase.DONE) {
            phase = Phase.NONE;
            return true;
        }
        if (key == InputConstants.KEY_R)
            record(Shingling.Kind.TURN, 0f, 0f);
        else if (key == InputConstants.KEY_SPACE || key == InputConstants.KEY_RETURN)
            finish();
        return true;
    }

    private void record(Shingling.Kind kind, float fx, float fy) {
        if (count >= MAX_EVENTS)
            return;
        Shingling.Event e = new Shingling.Event(ticks, kind, fx, fy);
        evTick[count] = ticks;
        evKind[count] = kind.ordinal();
        evX[count] = Math.round(Mth.clamp(fx, 0f, 1f) * 1000);
        evY[count] = Math.round(Mth.clamp(fy, 0f, 1f) * 1000);
        count++;
        if (kind == Shingling.Kind.TURN) {
            GuiSkin.play(SoundEvents.ANVIL_PLACE, 1.5f);
            return;
        }
        boolean live = model.workable(ticks);
        float temp = model.temperature(ticks);
        // Colder metal rings higher and duller; a blow on a piece too cold to move lands dead.
        GuiSkin.play(live ? SoundEvents.ANVIL_LAND : SoundEvents.ANVIL_PLACE, (kind == Shingling.Kind.HEAVY ? 0.7f : 1.1f) + (1500f - temp) / 3000f);
        float w = model.width() * PX, h = model.height() * PX;
        float px = host.slotCx() - w / 2f + fx * w, py = host.slotCy() - h / 2f - 4 + fy * h;
        int out = model.apply(e);
        spark(px, py, live ? (kind == Shingling.Kind.HEAVY ? 10 : 5) : 2, live ? 0xFFC040 : 0x808080);
        if (out > 0) {
            // A pocket leaking spits glowing slag and hisses, higher as the piece empties.
            GuiSkin.play(SoundEvents.LAVA_EXTINGUISH, 0.8f + 0.5f * model.result().leaked() / Math.max(1f, model.pockets()));
            flash = 8;
        }
    }

    private void spark(float x, float y, int n, int color) {
        for (int i = 0; i < n; i++) {
            int k = sparkAt++ % SPARKS;
            float a = rng.nextFloat() * Mth.TWO_PI, v = 0.6f + rng.nextFloat() * 1.6f;
            sx[k] = x;
            sy[k] = y;
            svx[k] = Mth.cos(a) * v;
            svy[k] = Mth.sin(a) * v - 1f;
            slife[k] = 10 + rng.nextInt(10);
        }
    }

    private void finish() {
        if (phase != Phase.PLAY)
            return;
        result = model.result();
        List<Integer> t = new ArrayList<>(count), k = new ArrayList<>(count), x = new ArrayList<>(count), y = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            t.add(evTick[i]);
            k.add(evKind[i]);
            x.add(evX[i]);
            y.add(evY[i]);
        }
        PacketDistributor.sendToServer(new ShingleFinishPayload(host.menu().containerId, t, k, x, y));
        doneText = Component.translatable(result.done() ? "forge.feedback.shingle.done" : "forge.feedback.shingle.left");
        phase = Phase.DONE;
    }

    /** The billet over the stock, glowing by its heat, pockets inside, the footprint under the pointer; the verdict once put down. */
    @Override
    public void overlay(GuiGraphics g) {
        if (phase == Phase.NONE)
            return;
        float partial = host.partial();
        float now = ticks + partial;
        float temp = model.temperature(Math.round(now));
        float w = model.width() * PX, h = model.height() * PX;
        int x0 = Math.round(host.slotCx() - w / 2f), y0 = Math.round(host.slotCy() - h / 2f - 4);
        g.fill(x0 - 3, y0 - 3, x0 + Math.round(w) + 3, y0 + Math.round(h) + 3, 0xC0101010);
        float t = Mth.clamp((temp - 600f) / 700f, 0f, 1f);
        int body = 0xFF000000 | (0x40 + (int) (0xBF * t)) << 16 | (int) (0x90 * t * t) << 8 | (int) (0x50 * t * t * t);
        g.fill(x0, y0, x0 + Math.round(w), y0 + Math.round(h), body);
        boolean runny = model.runny(Math.round(now));
        for (int i = 0; i < model.pockets(); i++) {
            if (model.gone(i))
                continue;
            int cx = x0 + Math.round(model.x(i) * PX), cy = y0 + Math.round(model.y(i) * PX);
            int c = runny ? 0xFF1A0E0A : 0xFF5A524C;
            g.fill(cx - 4, cy - 3, cx + 5, cy + 4, c);
            g.fill(cx - 3, cy - 4, cx + 4, cy + 5, c);
        }
        if (phase == Phase.PLAY) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            int mx = (int) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth());
            int my = (int) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
            int r = Math.round(FTuning.SHINGLE_FOOTPRINT * PX);
            g.fill(mx - r, my - 1, mx + r, my, 0x60FFFFFF);
            g.fill(mx - 1, my - r, mx, my + r, 0x60FFFFFF);
            if (flash > 0)
                g.fill(x0 - 3, y0 - 3, x0 + Math.round(w) + 3, y0 - 1, 0xA0FFB040);
            for (var l : host.font().split(hint, 220))
                g.drawString(host.font(), l, host.slotCx() - 110, y0 + Math.round(h) + 8 + 10 * host.font().split(hint, 220).indexOf(l), host.skin.dim(), false);
        } else {
            g.drawString(host.font(), doneText, host.slotCx() - host.font().width(doneText) / 2, y0 + Math.round(h) + 8,
                    result.done() ? host.skin.accent() : host.skin.warn(), true);
        }
        for (int i = 0; i < SPARKS; i++)
            if (slife[i] > 0) {
                int px = Math.round(sx[i] + svx[i] * partial), py = Math.round(sy[i] + svy[i] * partial);
                g.fill(px, py, px + 2, py + 2, 0xFF000000 | (slife[i] > 10 ? 0xFFC040 : 0xC06020));
            }
    }
}
