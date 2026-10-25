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

import io.github.soundgoodizerfan.feedback.net.PotInputPayload;
import io.github.soundgoodizerfan.feedback.net.PotStatePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Stirring and skimming a stockpot (food spec 6), seen from above. Move the mouse in circles round the
 * pot to stir: a steady pace keeps the bottom from scorching over a hard fire and mixes the pot. Scum
 * from raw meat cooking off rises as pale blobs: click one to skim it off. Stir while it is still
 * there and it is folded into the pot for good. Nothing is graded here: the pot is live on the server,
 * this screen sends the pace a few times a second and draws what comes back. No numbers: read the pot.
 */
public class PotScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.PANEL;
    private static final int W = 200, H = 224, R = 74, SEND_EVERY = 5, FULL_PACE_RAD_PER_TICK = 3; // 3 / 10 rad: about one turn a second
    private static final int BROTH = 0xFF8A6A3E, BURNT = 0xFF1A120C, SCUM = 0xFFE8E4D4;

    private final Opening opening = new Opening(SKIN);
    private final BlockPos pos;
    private float scum, mixed, scorch, water, swirl, pace, lastAngle;
    private boolean haveAngle, skimPending;
    private int ticks, sound, cx, cy, left, top;
    private int mouseX, mouseY;
    private List<FormattedCharSequence> hint = List.of();

    private PotScreen(PotStatePayload s) {
        super(Component.translatable("gui.feedback.pot.title"));
        this.pos = s.pos();
        apply(s);
    }

    private void apply(PotStatePayload s) {
        scum = s.scum();
        mixed = s.mixed();
        scorch = s.scorch();
        water = s.water();
    }

    /** The server's word: open the screen, or update the one open for this pot. */
    public static void state(PotStatePayload s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PotScreen screen && screen.pos.equals(s.pos()))
            screen.apply(s);
        else if (s.open())
            mc.setScreen(new PotScreen(s));
    }

    @Override
    protected void init() {
        opening.start();
        left = (width - W) / 2;
        top = (height - H) / 2;
        cx = left + W / 2;
        cy = top + 30 + R;
        hint = font.split(Component.translatable("gui.feedback.pot.hint"), W - 24);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (opening.settled() && button == 0)
            for (int i = 0, n = blobs(); i < n; i++)
                if (Math.abs(x - blobX(i)) < 10 && Math.abs(y - blobY(i)) < 8) {
                    skimPending = true;
                    GuiSkin.play(SoundEvents.SLIME_BLOCK_STEP, 1.6f);
                    return true;
                }
        return !opening.settled() || super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public void tick() {
        if (!opening.settled())
            return;
        // Pace is how far round the pot the mouse went this tick, kept to the band between the middle and the rim.
        double dx = mouseX - cx, dy = mouseY - cy, d = Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);
        float turned = haveAngle && d > R * 0.2 && d < R * 1.05 ? Math.abs(Mth.wrapDegrees((angle - lastAngle) * Mth.RAD_TO_DEG)) * Mth.DEG_TO_RAD : 0f;
        haveAngle = d > R * 0.2;
        lastAngle = angle;
        // Eased so a jittery mouse does not read as a stir and a stir does not drop out between samples.
        pace += (Math.min(1f, turned * 10f / FULL_PACE_RAD_PER_TICK) - pace) * 0.25f;
        swirl += pace * 0.25f;
        if (pace > 0.3f && --sound <= 0) {
            sound = 6;
            GuiSkin.play(SoundEvents.PLAYER_SWIM, 0.5f + 0.5f * pace);
        }
        if (++ticks % SEND_EVERY == 0) {
            PacketDistributor.sendToServer(new PotInputPayload(pos, Math.round(pace * 100f), skimPending));
            skimPending = false;
        }
    }

    private int blobs() {
        return (int) Math.ceil(scum * 8f);
    }

    private float blobX(int i) {
        return cx + Mth.cos(i * 2.4f) * R * (0.25f + 0.6f * ((i * 0.618f) % 1f));
    }

    private float blobY(int i) {
        return cy + Mth.sin(i * 2.4f) * R * (0.25f + 0.6f * ((i * 0.618f) % 1f));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        mouseX = mx;
        mouseY = my;
        opening.render(g, width, height, left, top, W, H, 0f, () -> {
            SKIN.panel(g, left, top, W, H);
            g.drawString(font, title, left + 12, top + 9, SKIN.ink(), false);
            // The pot: iron rim, then what is in it, darkening as the bottom scorches.
            disc(g, R + 3, 0xFF0A0C0B);
            disc(g, R, water > 0f ? lerp(BROTH, BURNT, scorch) : lerp(0xFF6A5030, BURNT, Math.max(scorch, 0.4f)));
            // Marks that turn with the stirring, faint when nothing is moving.
            int mark = lerp(0x00000000, 0x60FFFFFF, Math.min(1f, mixed + pace * 0.5f));
            for (int k = 0; k < 4; k++) {
                float a = swirl + k * Mth.HALF_PI;
                int x0 = cx + Math.round(Mth.cos(a) * R * 0.45f), y0 = cy + Math.round(Mth.sin(a) * R * 0.45f);
                int x1 = cx + Math.round(Mth.cos(a + 0.7f) * R * 0.7f), y1 = cy + Math.round(Mth.sin(a + 0.7f) * R * 0.7f);
                GuiStyle.line(g, x0, y0, x1, y1, mark);
            }
            for (int i = 0, n = blobs(); i < n; i++) {
                int bx = Math.round(blobX(i)), by = Math.round(blobY(i));
                g.fill(bx - 6, by - 4, bx + 6, by + 4, 0xFF0A0C0B);
                g.fill(bx - 5, by - 3, bx + 5, by + 3, SCUM);
            }
            int hy = top + H - 12 - 9 * hint.size();
            for (FormattedCharSequence line : hint) {
                g.drawString(font, line, left + 12, hy, SKIN.dim(), false);
                hy += 9;
            }
        });
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    private void disc(GuiGraphics g, int radius, int colour) {
        for (int y = -radius; y <= radius; y++) {
            int w = (int) Math.sqrt((double) radius * radius - (double) y * y);
            g.fill(cx - w, cy + y, cx + w, cy + y + 1, colour);
        }
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int al = (int) Mth.lerp(t, a >>> 24, b >>> 24), r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255),
                gg = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255), bl = (int) Mth.lerp(t, a & 255, b & 255);
        return al << 24 | r << 16 | gg << 8 | bl;
    }
}
