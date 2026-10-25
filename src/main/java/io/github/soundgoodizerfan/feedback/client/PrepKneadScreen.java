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
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.Kneading;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.net.PrepareBeginPayload;
import io.github.soundgoodizerfan.feedback.net.PrepareFinishPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.FoodKnead;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Prep Table's knead ({@code docs/feedback_food_spec.md} 6, "pressure and rest"): the dough drawn big, a palm that follows
 * the pointer. Hold the left button to press (lower on screen is harder); pressing builds gluten but tires the dough, and only
 * rest lets it relax, so the game is when to push and when to wait. The dough's own figures set the feel ({@link Kneading}, the
 * server's own function): water makes it sticky (it lags your hand, oozes back, and gluten comes slowly), a drier dough tears
 * at a lower tension and bounces back quick, fibre tenses it faster and rests slower, fat slackens it. The picture is the only
 * readout: the dough smooths and glosses as gluten builds, reddens and shakes as it tenses, and cracks when it tears; there
 * is no number. One packet of press samples when Done is pressed (or the time is up); the result is known then but hidden
 * under a cloth while the music of how it went plays, then the cloth lifts. Esc before Done discards.
 */
public class PrepKneadScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.BRICK;
    private static final int PW = 240, PH = 240, REVEAL_HOLD = 60, COLS = 56, RX = 56, RY = 34;
    /** How long each result's music runs (its file's length in ticks), by {@link FoodKnead} grade: crumbly, good, glossy, tough. */
    private static final int[] JUDGE_TICKS = {108, 98, 120, 108};
    private static final int PLAY = 0, JUDGE = 1, REVEAL = 2;
    /** Screen-local colours: the dough, the flour, the hand and the cloth are the subject of this screen, not the skin's. */
    private static final int FLOUR = 0xF4EFE0, TENSE = 0xC8503C, SKIN_TONE = 0xFFE0B090, CLOTH = 0xFF6E7A8C;
    /** The dough's silhouette: height of each column as a share of its full height (an ellipse). */
    private static final float[] PROFILE = new float[COLS];
    /** Tear cracks as three points each, in shares of the dough's half width and height. */
    private static final float[][] CRACKS = {
            {-0.5f, -0.2f, -0.3f, 0.05f, -0.4f, 0.3f}, {0.3f, -0.4f, 0.45f, -0.1f, 0.3f, 0.2f}, {-0.1f, -0.5f, 0.05f, -0.25f, -0.05f, 0.0f},
            {0.6f, 0.1f, 0.4f, 0.3f, 0.5f, 0.45f}, {-0.7f, 0.15f, -0.55f, 0.35f, -0.35f, 0.45f}};
    /** Glossy highlights: where on the dough, and how wide a share of it. */
    private static final float[][] GLOSS = {{-0.45f, -0.5f, 0.35f}, {0.05f, -0.65f, 0.3f}, {0.45f, -0.3f, 0.25f}};
    /** Dry crumbs: where on the dough they sit. */
    private static final float[][] CRUMBS = {{-0.7f, 0f}, {-0.5f, -0.4f}, {-0.3f, 0.3f}, {-0.1f, -0.1f}, {0.1f, 0.4f}, {0.3f, -0.45f}, {0.5f, 0.1f},
            {0.7f, -0.1f}, {-0.6f, 0.4f}, {0.2f, 0.0f}, {-0.2f, -0.55f}, {0.6f, 0.35f}};

    static {
        for (int i = 0; i < COLS; i++) {
            float u = -1f + (i + 0.5f) * 2f / COLS;
            PROFILE[i] = (float) Math.sqrt(1f - u * u);
        }
    }

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId;
    private final Kneading run;
    private final int rgb;
    private final float stick, follow, spring, damp;
    /** One press sample per {@link FTuning#KNEAD_STEP_TICKS} ticks, the very ints the server is sent. */
    private final int[] samples = new int[FTuning.KNEAD_MAX_SAMPLES];
    private int count, ticks, flourish, judge, phase, mouseX, mouseY, px, py, cx, cy, squeakGap, grade, wasTearing;
    private float depth, squash, squashV, partial;
    private boolean pressing, sent, tense;
    private SimpleSoundInstance bgm;
    private Button done;
    private Component resultWord = Component.empty(), resting = Component.empty();
    private final Specks specks = new Specks(64);
    private List<FormattedCharSequence> hint = List.of();

    private PrepKneadScreen(Screen parent, int containerId, Material.Food figures, int rgb) {
        super(Component.translatable("gui.feedback.prep_knead.title"));
        this.parent = parent;
        this.containerId = containerId;
        this.rgb = rgb;
        this.run = new Kneading(figures);
        this.stick = Mth.clamp(figures.moisture(), 0f, 1f);
        // A wet dough lags the hand and oozes back slowly (heavy damping); a dry one follows at once and bounces.
        this.follow = Kneading.follow(figures.moisture());
        this.spring = Mth.lerp(stick, 0.35f, 0.15f);
        this.damp = Mth.lerp(stick, 0.18f, 0.45f);
    }

    /** Open over the Prep Table's screen and tell the server the knead has begun. */
    static void open(Screen parent, ToolTableMenu menu) {
        var level = Minecraft.getInstance().level;
        Optional<Material.Located> what = Material.find(ClientMaterials.on(level), menu.work());
        if (what.isEmpty() || what.get().material().food().isEmpty())
            return;
        PacketDistributor.sendToServer(new PrepareBeginPayload(menu.containerId, 2));
        Minecraft.getInstance().setScreen(new PrepKneadScreen(parent, menu.containerId, what.get().material().food().get(),
                MaterialColors.of(what.get().id())));
    }

    @Override
    protected void init() {
        opening.start();
        px = (width - PW) / 2;
        py = Math.max(0, (height - PH) / 2);
        cx = px + PW / 2;
        cy = py + 22 + 82;
        hint = font.split(Component.translatable("gui.feedback.prep_knead.hint"), PW - 24);
        resting = Component.translatable("gui.feedback.prep_knead.resting");
        done = addRenderableWidget(GuiStyle.primary(SKIN, Component.translatable("gui.feedback.prep_cut.done"), b -> lock(),
                cx - 40, py + PH - 22, 80, 16, FSounds.UI_STAMP::get));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean overDough(double x, double y) {
        return x >= px + 8 && x <= px + PW - 8 && y >= py + 22 && y <= py + 22 + 146;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (phase == JUDGE) {
            reveal();
            return true;
        }
        if (!opening.settled() || sent || super.mouseClicked(x, y, button))
            return true;
        if (button == 0 && overDough(x, y)) {
            pressing = true;
            if (depth < 0.15f)
                slap();
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0)
            pressing = false;
        return super.mouseReleased(x, y, button);
    }

    /** The palm lands: a slap pitched by how sticky the dough is (wet is low and wet-sounding), and flour thrown off a dry one. */
    private void slap() {
        GuiSkin.play(SoundEvents.SLIME_BLOCK_PLACE, 1.5f - stick * 0.8f);
        GuiSkin.play(SoundEvents.WOOL_PLACE, 1.2f - stick * 0.4f);
        int puff = Math.round(2 + (1f - stick) * 9f);
        specks.burst(Mth.clamp(mouseX, cx - RX, cx + RX), cy - RY + 4, puff, 2.2f, 2.6f, FLOUR, 24, 2);
    }

    /** How hard the hand presses now: the pointer's height over the dough, light at the top to full at the bottom. */
    private float target() {
        return Mth.clamp((mouseY - (py + 34f)) / 120f, 0.15f, 1f);
    }

    @Override
    public void tick() {
        ticks++;
        float aim = phase == PLAY && pressing && !sent ? target() : 0f;
        float before = depth;
        depth += (aim - depth) * follow;
        if (Math.abs(aim - depth) < 0.01f)
            depth = aim;
        squashV = (squashV + (depth - squash) * spring) * (1f - damp);
        squash += squashV;
        if (phase == PLAY && !sent) {
            if ((ticks & 1) == 0 && count < samples.length) {
                samples[count] = Math.round(depth * 100f);
                run.push(samples[count++]);
            }
            if (depth > 0.5f && --squeakGap <= 0) {
                squeakGap = 7;
                GuiSkin.play(SoundEvents.SLIME_BLOCK_STEP, 1.3f - stick * 0.7f);
            }
            // A sticky dough peels away from the palm as it lets go.
            if (before > 0.4f && depth < before - 0.05f && stick > 0.45f)
                GuiSkin.play(SoundEvents.HONEY_BLOCK_SLIDE, 1.1f - stick * 0.5f);
            if (run.tearing()) {
                if (wasTearing++ % 8 == 0) {
                    GuiSkin.play(SoundEvents.SHEEP_SHEAR, 1.7f);
                    specks.burst(cx + (ticks % 5 - 2) * 14, cy, 3, 1.6f, 1.8f, rgb, 20, 2);
                }
            } else
                wasTearing = 0;
            // Rested again after being worked hard: a soft tone that it will take work.
            float ratio = run.tension() / run.tearAt();
            if (ratio > 0.6f)
                tense = true;
            else if (tense && run.tension() <= FTuning.KNEAD_RELAXED) {
                tense = false;
                GuiSkin.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f);
            }
            done.active = count >= FTuning.KNEAD_MIN_SAMPLES;
            if (count >= samples.length)
                lock();
        }
        specks.tick(0.16f);
        if (phase == JUDGE && --judge <= 0)
            reveal();
        else if (phase == REVEAL && --flourish <= 0)
            opening.close();
    }

    /**
     * Done: the one packet goes now and the result is known, but the player is not told yet. The dough rests under a cloth
     * while the music of how it went plays; then the cloth lifts (or a click or Esc skips to it).
     */
    private void lock() {
        if (sent || phase != PLAY || count < FTuning.KNEAD_MIN_SAMPLES)
            return;
        sent = true;
        pressing = false;
        List<Integer> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            out.add(samples[i]);
        PacketDistributor.sendToServer(new PrepareFinishPayload(containerId, 2, out, 0));
        grade = run.grade();
        resultWord = Component.translatable("prep.feedback.knead." + grade);
        phase = JUDGE;
        judge = JUDGE_TICKS[grade];
        done.visible = false;
        bgm = SimpleSoundInstance.forUI(grade == FoodKnead.GLOSSY ? FSounds.COOKING_BGM_GREAT.get()
                : grade == FoodKnead.GOOD ? FSounds.COOKING_BGM_SUCCESS.get() : FSounds.COOKING_BGM_FAIL.get(), 1f);
        Minecraft.getInstance().getSoundManager().play(bgm);
    }

    /** The cloth comes off: the fanfare for a good dough or a spoiled one, and the picture that goes with it. */
    private void reveal() {
        if (phase != JUDGE)
            return;
        phase = REVEAL;
        flourish = REVEAL_HOLD;
        stopMusic();
        boolean good = grade == FoodKnead.GLOSSY || grade == FoodKnead.GOOD;
        GuiSkin.play(good ? FSounds.COOKING_FANFARE_SUCCESS.get() : FSounds.COOKING_FANFARE_FAIL.get(), 1f);
        if (grade == FoodKnead.GLOSSY)
            specks.burst(cx, cy - RY, 24, 2.6f, 3.4f, 0xFFF0A0, 30, 2);
        else if (grade == FoodKnead.CRUMBLY)
            specks.burst(cx, cy, 20, 2f, 0.5f, FLOUR, 34, 2);
        else if (grade == FoodKnead.TOUGH)
            squashV = 0.5f;
    }

    private void stopMusic() {
        if (bgm != null)
            Minecraft.getInstance().getSoundManager().stop(bgm);
        bgm = null;
    }

    @Override
    public void onClose() {
        // Esc while judging skips to the result; after it, or before Done, it leaves (nothing was sent before Done).
        if (phase == JUDGE)
            reveal();
        else
            opening.close();
    }

    @Override
    public void removed() {
        stopMusic();
        super.removed();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return opening.closing() || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
    }

    private GuiGraphics frameG;
    private int frameMx, frameMy;
    private float framePt;
    private final Runnable paintFrame = () -> paint(frameG, frameMx, frameMy, framePt);

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
        frameMx = mx;
        frameMy = my;
        framePt = partialTick;
        opening.render(g, width, height, px, py, PW, PH, 0f, paintFrame);
    }

    /** The frame body, a method so the per-frame call allocates no capturing lambda. */
    private void paint(GuiGraphics g, int mx, int my, float partialTick) {
        SKIN.panel(g, px, py, PW, PH);
        g.drawString(font, title, px + 12, py + 9, SKIN.ink(), false);
        if (phase == REVEAL)
            g.drawString(font, resultWord, px + PW - 12 - font.width(resultWord), py + 9,
                    grade == FoodKnead.GLOSSY || grade == FoodKnead.GOOD ? SKIN.accent() : SKIN.warn(), false);
        SKIN.inset(g, px + 8, py + 22, PW - 16, 146);
        dough(g);
        if (phase == PLAY)
            palm(g);
        cloth(g);
        // The time left, as the one bar: the session has a length, the dough has no meter.
        SKIN.bar(g, px + 12, py + 172, PW - 24, 4, count / (float) samples.length, -1f);
        int hy = py + 180;
        for (var l : hint) {
            g.drawString(font, l, px + 12, hy, SKIN.dim(), false);
            hy += 9;
        }
        super.render(g, mx, my, partialTick);
        specks.draw(g, partialTick);
    }

    /** The dough as a row of columns: it squashes wide and low as it is pressed and rebounds on a spring, glosses with gluten, reddens and shakes with tension, and cracks when it tears. */
    private void dough(GuiGraphics g) {
        float sq = Mth.clamp(squash + squashV * partial, -0.3f, 1.3f);
        float r = RX * (1f + 0.35f * sq), h = RY * (1f - 0.5f * sq);
        float tear = run.tension() / run.tearAt();
        float shake = Math.max(0f, tear - 0.6f);
        int sx = phase == REVEAL && grade == FoodKnead.TOUGH ? Math.round(Mth.sin((ticks + partial) * 2.3f) * flourish * 0.05f)
                : Math.round(Mth.sin((ticks + partial) * 1.9f) * shake * 5f);
        float lift = stick * Math.max(0f, -squashV) * 40f;
        int body = 0xFF000000 | GuiSkin.lerp(0xFF000000 | rgb, 0xFF000000 | TENSE, Mth.clamp(tear * 0.55f, 0f, 0.6f)) & 0xFFFFFF;
        int dark = GuiSkin.lerp(body, 0xFF000000, 0.5f), shade = GuiSkin.lerp(body, 0xFF000000, 0.18f);
        int[] top = tops, bot = bots;
        for (int i = 0; i < COLS; i++) {
            float u = -1f + (i + 0.5f) * 2f / COLS;
            float bump = lift * (1f - Math.abs(u)) * (1f - Math.abs(u));
            top[i] = cy - Math.round(h * PROFILE[i] + bump);
            bot[i] = cy + Math.round(h * PROFILE[i] * 0.6f);
        }
        for (int i = 0; i < COLS; i++)
            g.fill(cx + sx + Math.round((-1f + i * 2f / COLS) * r) - 1, top[i] - 1, cx + sx + Math.round((-1f + (i + 1) * 2f / COLS) * r) + 1, bot[i] + 1, dark);
        for (int i = 0; i < COLS; i++) {
            int x0 = cx + sx + Math.round((-1f + i * 2f / COLS) * r), x1 = cx + sx + Math.round((-1f + (i + 1) * 2f / COLS) * r);
            g.fill(x0, top[i], x1, bot[i], body);
            g.fill(x0, bot[i] - Math.max(2, (bot[i] - top[i]) / 4), x1, bot[i], shade);
        }
        // Gloss rises with the gluten: a smooth dough shines.
        float gloss = Mth.clamp((run.gluten() - 0.3f) / 0.6f, 0f, 1f);
        if (gloss > 0f) {
            int hi = (int) (gloss * 150f) << 24 | 0xFFFFFF;
            for (float[] b : GLOSS) {
                int w = Math.max(3, Math.round(r * b[2])), bx = cx + sx + Math.round(b[0] * r), by = cy + Math.round(b[1] * h);
                g.fill(bx - w / 2, by, bx + w / 2, by + 2, hi);
                g.fill(bx - w / 3, by + 2, bx + w / 3, by + 3, hi);
            }
        }
        // A dry, unworked dough is flaky: crumbs sit on it until the gluten holds it together.
        int crumbs = Math.round(CRUMBS.length * Mth.clamp(1f - run.gluten() / 0.6f, 0f, 1f) * Mth.clamp(1.4f - stick, 0f, 1f));
        for (int i = 0; i < crumbs; i++)
            g.fill(cx + sx + Math.round(CRUMBS[i][0] * r), cy + Math.round(CRUMBS[i][1] * h), cx + sx + Math.round(CRUMBS[i][0] * r) + 2,
                    cy + Math.round(CRUMBS[i][1] * h) + 1, 0xC0000000 | FLOUR);
        // Cracks: fine ones as it nears its limit, more as it is overworked.
        int cracks = run.over() > 0f ? Math.min(CRACKS.length, 2 + (int) (run.over() / FTuning.KNEAD_TOUGH_OVER * 3f)) : tear > 0.8f ? 2 : 0;
        for (int i = 0; i < cracks; i++) {
            float[] c = CRACKS[i];
            int shade2 = i < 2 && run.over() <= 0f ? 0x80300A00 : 0xFF300A00;
            GuiStyle.line(g, cx + sx + Math.round(c[0] * r), cy + Math.round(c[1] * h), cx + sx + Math.round(c[2] * r), cy + Math.round(c[3] * h), shade2);
            GuiStyle.line(g, cx + sx + Math.round(c[2] * r), cy + Math.round(c[3] * h), cx + sx + Math.round(c[4] * r), cy + Math.round(c[5] * h), shade2);
        }
    }

    private final int[] tops = new int[COLS], bots = new int[COLS];

    /** The hand: a palm over the dough that follows the pointer sideways and sinks as it presses. */
    private void palm(GuiGraphics g) {
        float sq = Mth.clamp(squash, -0.3f, 1.3f);
        float r = RX * (1f + 0.35f * sq), h = RY * (1f - 0.5f * sq);
        int x = Mth.clamp(mouseX, Math.round(cx - r * 0.8f), Math.round(cx + r * 0.8f));
        int col = Mth.clamp(Math.round((x - cx) / r * COLS / 2f + COLS / 2f), 0, COLS - 1);
        int onTop = cy - Math.round(h * PROFILE[col]) - 1, hover = cy - RY - 24;
        float press = Mth.clamp(depth * 5f, 0f, 1f);
        int bottom = Math.round(Mth.lerp(press, hover, onTop));
        g.fill(x - 15, bottom - 9, x + 15, bottom, SKIN.ink());
        g.fill(x - 14, bottom - 8, x + 14, bottom - 1, SKIN_TONE);
        for (int f = -3; f <= 3; f += 2)
            g.fill(x + f * 4 - 1, bottom - 13, x + f * 4 + 2, bottom - 9, SKIN_TONE);
    }

    /** The cloth over the dough while it rests, wobbling as the music builds, then lifting off as the result is shown. */
    private void cloth(GuiGraphics g) {
        if (phase == PLAY || (phase == REVEAL && flourish < REVEAL_HOLD - 20))
            return;
        int lift = phase == REVEAL ? (REVEAL_HOLD - flourish) * 4 : 0;
        int wob = phase == JUDGE ? Math.round(Mth.sin((ticks + partial) * 0.7f) * (1f + (1f - judge / (float) JUDGE_TICKS[grade]) * 2f)) : 0;
        int x0 = cx - RX - 20 + wob, x1 = cx + RX + 20 + wob, y0 = cy - RY - 14 - lift, y1 = cy + RY + 2 - lift;
        g.fill(x0, y0, x1, y1, SKIN.ink());
        g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, CLOTH);
        for (int y = y0 + 3; y < y1 - 1; y += 4)
            g.fill(x0 + 1, y, x1 - 1, y + 1, 0x30FFFFFF);
        for (int x = x0 + 3; x < x1 - 1; x += 4)
            g.fill(x, y0 + 1, x + 1, y1 - 1, 0x20000000);
        // The hem hangs in scallops.
        for (int x = x0 + 4; x < x1 - 6; x += 12)
            g.fill(x, y1 - 1, x + 8, y1 + 3, CLOTH);
        if (phase == JUDGE)
            g.drawString(font, resting, (x0 + x1) / 2 - font.width(resting) / 2, (y0 + y1) / 2 - 4, SKIN.ink(), false);
    }
}
