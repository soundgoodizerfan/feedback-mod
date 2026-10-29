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
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlock;
import io.github.soundgoodizerfan.feedback.net.SkilletFinishPayload;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodState;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Cooking in a pan (food spec 6): a control game. The lever sets how much of the fire the pan gets;
 * the food's own heat lags behind the pan's, and what it has cooked it cannot uncook. Drag the
 * lever (or use the arrows or wheel) to land the food cooked through before it dries or chars. The
 * piece cooks by the same ladder a vessel runs, replayed live on a copy, and changes colour as it
 * goes. Close the screen to take it off: one packet with the settings, and the server replays them.
 * No bar and no target: read the piece.
 *
 * <p>Cheap: state advances once a tick, nothing is allocated per frame.
 */
public class SkilletScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.PANEL;
    /** Doneness words, built once: translatable components resolve at draw time, so caching survives a language change. */
    private static final Component[] DONENESS_WORDS = java.util.Arrays.stream(Cooking.Doneness.values())
            .map(d -> (Component) Component.translatable("tooltip.feedback.food." + d.name().toLowerCase(Locale.ROOT))).toArray(Component[]::new);
    private static final int MARGIN = 14, LEVER_W = 14, LEVER_H = 110, PAN_R = 52;
    private static final int RAW = 0xFFE08A8A, COOKED = 0xFF9A5B2E, BURNT = 0xFF14100C;

    private final Opening opening = new Opening(SKIN);
    private final float fireTu, ambientTu;
    private final List<Integer> settings = new ArrayList<>();
    private ItemStack piece;
    private float setting, food, shown, shownPrev, flash;
    // Fixed pool of sizzle/steam/smoke specks: kind 0 bubble, 1 steam, 2 smoke; life 0 is free.
    private static final int SPECKS = 40;
    private final float[] sx = new float[SPECKS], sy = new float[SPECKS], svx = new float[SPECKS], svy = new float[SPECKS];
    private final int[] slife = new int[SPECKS], skind = new int[SPECKS];
    private final RandomSource rng = RandomSource.create();
    private Cooking.Doneness dn, lastDn;
    private boolean warned;
    private int ticks, sound, panX, panY, leverX, leverY, pw, ph, px, py;
    private boolean sent, dragging;
    private java.util.List<net.minecraft.util.FormattedCharSequence> hintLines = java.util.List.of();

    private SkilletScreen(float fireTu, ItemStack piece, float ambientTu) {
        super(Component.translatable("gui.feedback.skillet.title"));
        this.fireTu = fireTu;
        this.piece = piece;
        this.ambientTu = ambientTu;
        this.food = ambientTu;
    }

    public static void open(float fireTu, ItemStack piece) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null)
            mc.setScreen(new SkilletScreen(fireTu, piece, AmbientTable.at(mc.level).value()));
    }

    @Override
    protected void init() {
        opening.start();
        pw = 2 * PAN_R + LEVER_W + 4 * MARGIN;
        hintLines = font.split(Component.translatable("gui.feedback.skillet.hint"), pw - 2 * MARGIN);
        ph = 2 * MARGIN + 2 * Math.max(PAN_R, LEVER_H / 2) + 46 + 9 * hintLines.size();
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        panX = px + MARGIN + PAN_R;
        panY = py + 2 * MARGIN + Math.max(PAN_R, LEVER_H / 2);
        leverX = px + pw - MARGIN - LEVER_W;
        leverY = panY - LEVER_H / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drag(double my) {
        setting = Mth.clamp((float) (leverY + LEVER_H - my) / LEVER_H, 0f, 1f);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && opening.settled() && x >= leverX - 6 && x <= leverX + LEVER_W + 6 && y >= leverY - 6 && y <= leverY + LEVER_H + 6) {
            dragging = true;
            drag(y);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging && opening.settled())
            drag(y);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (opening.settled())
            setting =Mth.clamp(setting + (float) sy * 0.05f, 0f, 1f);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (opening.closing())
            return true;
        if (key == 265 || key == 264) { // up, down
            setting = Mth.clamp(setting + (key == 265 ? 0.05f : -0.05f), 0f, 1f);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (sent || mc.level == null)
            return;
        ticks++;
        food = SkilletBlock.warm(food, SkilletBlock.panTu(fireTu, ambientTu, setting),
                Cooking.find(mc.level, piece).map(e -> e.food().mass()).orElse(1f));
        ItemStack current = piece;
        float heat = food;
        piece = Cooking.find(mc.level, current).map(e -> Cooking.tick(current, e, new Tu(heat), mc.level)).orElse(current);
        if (ticks % FTuning.SKILLET_SAMPLE_TICKS == 0) {
            settings.add(Math.round(setting * 100f));
            if (settings.size() >= FTuning.SKILLET_MAX_SAMPLES) {
                onClose();
                return;
            }
        }
        shownPrev = shown;
        shown += (setting - shown) * 0.4f;
        flash *= 0.8f;
        float hf = heatFrac();
        FoodState state = piece.get(FDataComponents.FOOD_STATE.get());
        if (state != null) {
            dn = Cooking.find(mc.level, piece).map(e -> Cooking.doneness(e, state)).orElse(dn);
            if (lastDn != null && dn != lastDn && dn == Cooking.Doneness.DONE) {
                flash = 1f;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.3f, 0.7f));
            }
            lastDn = dn;
            if (!warned && state.charred() > 0.02f) {
                warned = true;
                mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BLAZE_BURN, 1.4f, 0.8f));
            }
            specks(hf, state);
        }
        if (--sound <= 0 && hf > 0.05f) {
            sound = 8;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LAVA_EXTINGUISH, 1.8f, 0.1f + 0.25f * hf));
        }
    }

    /** Food heat as a share of the way from ambient to the fire; the lag is the gap to the setting. */
    private float heatFrac() {
        return Mth.clamp((food - ambientTu) / Math.max(1f, fireTu - ambientTu), 0f, 1f);
    }

    /** Move the live specks and spawn new ones: bubbles with food heat, steam while wet, smoke once it chars. */
    private void specks(float heat, FoodState state) {
        for (int i = 0; i < SPECKS; i++) {
            if (slife[i] <= 0)
                continue;
            slife[i]--;
            sx[i] += svx[i] + (skind[i] == 2 ? Mth.sin((ticks + i) * 0.3f) * 0.3f : 0f);
            sy[i] += svy[i];
        }
        float wet = Mth.clamp(state.moisture(), 0f, 1f);
        if (rng.nextFloat() < heat * 0.9f)
            spawn(0, 8 + rng.nextInt(6));
        if (rng.nextFloat() < heat * wet * 0.5f)
            spawn(1, 22 + rng.nextInt(10));
        if (state.charred() > 0.02f && rng.nextFloat() < 0.6f)
            spawn(2, 30 + rng.nextInt(14));
    }

    private void spawn(int kind, int life) {
        for (int i = 0; i < SPECKS; i++) {
            if (slife[i] > 0)
                continue;
            float a = rng.nextFloat() * Mth.TWO_PI, r = rng.nextFloat() * (PAN_R - 12);
            sx[i] = Mth.cos(a) * r;
            sy[i] = Mth.sin(a) * r * (kind == 0 ? 1f : 0.5f);
            svx[i] = (rng.nextFloat() - 0.5f) * 0.4f;
            svy[i] = kind == 0 ? -0.15f : -0.5f - rng.nextFloat() * 0.4f;
            slife[i] = life;
            skind[i] = kind;
            return;
        }
    }

    @Override
    public void onClose() {
        if (!sent) {
            sent = true;
            if (!settings.isEmpty())
                PacketDistributor.sendToServer(new SkilletFinishPayload(settings));
        }
        opening.close();
    }

    /** The panel only; the world stays as it is. */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    private GuiGraphics frameG;
    private float framePt;
    private final Runnable paintFrame = () -> body(frameG, framePt);

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        frameG = g;
        framePt = partial;
        opening.render(g, width, height, px, py, pw, ph, 0f, paintFrame);
    }

    private void body(GuiGraphics g, float partial) {
        SKIN.panel(g, px, py, pw, ph);
        g.drawString(font, title, px + MARGIN, py + 9, SKIN.ink(), false);
        float time = ticks + partial, heat = heatFrac();
        FoodState state = piece.get(FDataComponents.FOOD_STATE.get());
        float charred = state == null ? 0f : state.charred();
        // Charring rattles the pan.
        float shake = charred > 0.02f ? Math.min(1.5f, 0.5f + 2f * charred) : 0f;
        g.pose().pushPose();
        if (shake > 0f)
            g.pose().translate(Mth.sin(time * 9f) * shake, Mth.cos(time * 11f) * shake, 0f);
        // The rim is the food's own heat, the pan face the setting: the gap between them is the lag.
        disc(g, PAN_R + 4, 0xFF0A0C0B);
        disc(g, PAN_R + 3, glow(heat));
        disc(g, PAN_R, 0xFF0A0C0B);
        disc(g, PAN_R - 2, glow(shown * 0.9f + 0.1f * shown * Mth.sin(time * 0.35f)));
        disc(g, PAN_R - 9, shade(glow(shown), -0x10));
        // Food: a patch the colour of its doneness, a shadow, then the item bobbing over it.
        int cx = panX, cy = panY;
        int patch = pieceColour(state);
        disc(g, 17, 0xFF0A0C0B);
        disc(g, 16, patch);
        g.fill(cx - 12, cy + 15, cx + 12, cy + 18, 0x55000000);
        float bob = heat * Mth.sin(time * 0.9f) * 1.2f;
        g.pose().pushPose();
        g.pose().translate(cx, cy - 3 + bob, 0f);
        g.pose().scale(2f, 2f, 1f);
        g.renderItem(piece, -8, -8);
        g.pose().popPose();
        for (int i = 0; i < SPECKS; i++)
            if (slife[i] > 0)
                speck(g, i);
        if (flash > 0.02f) {
            disc(g, PAN_R + 4, ((int) (flash * 140) << 24) | 0xFFFFE0);
        }
        g.pose().popPose();
        // The lever: a slot with notches, the food's heat as a ghost mark, and a handle glowing at the setting.
        SKIN.inset(g, leverX, leverY, LEVER_W, LEVER_H);
        for (int i = 1; i < 4; i++)
            g.fill(leverX - 4, leverY + LEVER_H * i / 4, leverX, leverY + LEVER_H * i / 4 + 1, SKIN.dim());
        int fy = leverY + LEVER_H - Math.round(heat * LEVER_H);
        g.fill(leverX - 5, fy - 1, leverX + LEVER_W + 5, fy + 1, 0xAAFFD080);
        int hy = leverY + LEVER_H - Math.round(Mth.lerp(partial, shownPrev, shown) * LEVER_H);
        g.fill(leverX - 3, hy - 3, leverX + LEVER_W + 3, hy + 3, 0xFF0A0C0B);
        g.fill(leverX - 2, hy - 2, leverX + LEVER_W + 2, hy + 2, lerp(SKIN.ink(), glow(1f), shown));
        g.fill(leverX - 2, hy - 2, leverX + LEVER_W + 2, hy - 1, 0x66FFFFFF);
        // Doneness pips: raw, cooked, dried, charred; the current one lit.
        if (dn != null)
            for (int i = 0; i < 4; i++) {
                int x = panX - 18 + i * 12, y = hintTop() - 26;
                boolean on = dn.ordinal() == i;
                g.fill(x - 1, y - 1, x + 7, y + 5, 0xFF0A0C0B);
                g.fill(x, y, x + 6, y + 4, on ? PIP[i] : shade(PIP[i], -0x70));
            }
        if (dn != null)
            g.drawString(font, DONENESS_WORDS[dn.ordinal()], panX - font.width(DONENESS_WORDS[dn.ordinal()]) / 2,
                    hintTop() - 14, SKIN.ink(), false);
        int hyy = hintTop();
        for (var line : hintLines) {
            g.drawString(font, line, px + MARGIN, hyy, SKIN.dim(), false);
            hyy += 9;
        }
    }

    private int hintTop() {
        return py + ph - 8 - 9 * hintLines.size();
    }

    private static final int[] PIP = { 0xFFE08A8A, 0xFFE8C060, 0xFFB08040, 0xFF50403A };

    private void speck(GuiGraphics g, int i) {
        int x = panX + (int) sx[i], y = panY + (int) sy[i];
        float t = Math.min(1f, slife[i] / 10f);
        switch (skind[i]) {
            case 0 -> g.fill(x, y, x + 2, y + 2, ((int) (t * 200) << 24) | 0xFFF0D0);
            case 1 -> g.fill(x, y, x + 3, y + 3, ((int) (t * 90) << 24) | 0xFFFFFF);
            default -> g.fill(x, y, x + 4, y + 4, ((int) (t * 150) << 24) | 0x2A2A2A);
        }
    }

    private static int shade(int c, int d) {
        int r = Mth.clamp((c >> 16 & 255) + d, 0, 255), gg = Mth.clamp((c >> 8 & 255) + d, 0, 255), b = Mth.clamp((c & 255) + d, 0, 255);
        return 0xFF000000 | r << 16 | gg << 8 | b;
    }

    /** Dark iron to glowing red-orange with the setting. */
    private static int glow(float heat) {
        int rr = Math.min(255, 0x38 + (int) (0xB0 * heat)), gg = 0x30 + (int) (0x40 * heat * heat);
        return 0xFF000000 | rr << 16 | gg << 8 | 0x30;
    }

    private static int pieceColour(FoodState state) {
        if (state == null)
            return RAW;
        return lerp(lerp(RAW, COOKED, state.cook()), BURNT, state.charred());
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255), g = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255),
                bl = (int) Mth.lerp(t, a & 255, b & 255);
        return 0xFF000000 | r << 16 | g << 8 | bl;
    }

    /** A filled disc of this radius about the pan's centre, one row at a time. */
    private void disc(GuiGraphics g, int ring, int colour) {
        for (int y = -ring; y <= ring; y++) {
            int w = (int) Math.sqrt((double) ring * ring - (double) y * y);
            g.fill(panX - w, panY + y, panX + w, panY + y + 1, colour);
        }
    }
}
