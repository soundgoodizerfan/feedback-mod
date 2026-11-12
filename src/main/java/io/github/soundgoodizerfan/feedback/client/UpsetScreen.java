// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;
import io.github.soundgoodizerfan.feedback.modular.engineering.Upset;
import io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
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
 * The Engineering Table's rivet game: a hot rivet through two plates, a hammer above it. Hold the mouse to swing the hammer up (the gauge
 * climbs), let go to strike. The head upsets by the force: too light leaves a tall loose stub, inside the green band a domed head the size
 * of a seat, too hard a flat pancake, and past the split point the head cracks and flies off in sparks. The metal's hardness moves the
 * band up and narrows it (soft lead forgives, steel does not), and a better fastener widens it. One strike per rivet spent, the scores
 * averaged into the ONE result sent ({@link MinigameScreens#report}) after the last; the stamp that follows never delays it. Esc before
 * the last strike sends nothing and the craft stays offered. The rivet and plates are the subject of the screen, so their colours are
 * local; the rivet is drawn in its own material's colour. Reduce motion stills the idle sway.
 */
final class UpsetScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.ENAMEL;
    private static final int PW = 220, PH = 236, READY = 0, WIND = 1, SETTLE = 2, DONE = 3;
    private static final int SETTLE_TICKS = 26, DONE_TICKS = 80, HAMMER_LIFT = 46, SHANK_W = 10, PLATE_H = 14;
    /** Screen-local colours: plates, bands and sparks are the subject of this screen, not the skin's. */
    private static final int PLATE = 0xFF5D626A, STEEL = 0xFF9AA0A8, GRIP = 0xFFB07A3E, BAND = 0xFF4FBF5A, STRIP = 0xFFC2452F,
            SPARK = 0xFFFFC04A;

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId, rounds, quality, seed, rgb;
    private final float hardShare;
    private final float[] scores;
    private final Specks specks = new Specks(72);
    private final Component title = Component.translatable("gui.feedback.upset.title");
    private final Component[] words = new Component[5], results = new Component[5];
    private List<FormattedCharSequence> hint = List.of();
    private Component countText = Component.empty();
    private int px, py, bx, plateTop, sx0, sy0, ticks, phase = READY, strike, settle, hold, popGrade = -1, popAge;
    private long doneAt;
    private float centre, half, splitEnd, force, ghost = -1f, partial, finalScore, headU;
    private boolean sent;

    UpsetScreen(Screen parent, EngineeringTableMenu menu, MinigameOpenPayload session) {
        super(Component.translatable("gui.feedback.upset.title"));
        this.parent = parent;
        this.containerId = session.container();
        this.rounds = Math.max(1, Math.min(ModularTuning.UPSET_MAX_STRIKES, session.rounds()));
        this.quality = Mth.clamp(session.quality(), 1, 5);
        this.seed = containerId * 31 + rounds;
        this.scores = new float[rounds];
        int colour = 0xB8BEC6;
        float hardness = 4.5f;
        for (int i = 0; i < 3; i++) {
            ItemStack stack = menu.getSlot(EngineeringTableMenu.slot(EngineeringTableBlockEntity.FASTENERS + i)).getItem();
            var found = stack.isEmpty() ? java.util.Optional.<io.github.soundgoodizerfan.feedback.process.Material.Located>empty()
                    : MaterialIngredient.locate(stack.getItem());
            if (found.isPresent()) {
                colour = MaterialColors.of(found.get().id());
                hardness = found.get().material().hardness().raw();
                break;
            }
        }
        this.rgb = colour;
        this.hardShare = Upset.hardShare(hardness);
        for (int i = 0; i < 5; i++) {
            words[i] = Component.translatable("gui.feedback.upset.strike." + i);
            results[i] = Component.translatable("gui.feedback.upset.result." + i);
        }
        newStrike();
    }

    private void newStrike() {
        centre = Upset.centre(seed, strike, hardShare);
        half = Upset.half(quality, hardShare);
        splitEnd = Upset.splitEnd(centre, half);
        force = 0f;
        headU = 0f;
        phase = READY;
        countText = Component.translatable("gui.feedback.upset.count", strike + 1, rounds);
    }

    @Override
    protected void init() {
        opening.start();
        px = (width - PW) / 2;
        py = Math.max(0, (height - PH) / 2);
        sx0 = px + 8;
        sy0 = py + 24;
        bx = px + 78;
        plateTop = sy0 + 100;
        hint = font.split(Component.translatable("gui.feedback.upset.hint"), PW - 24);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (phase == DONE) {
            hold = 0;
            return true;
        }
        if (!opening.settled() || button != 0 || phase != READY)
            return true;
        phase = WIND;
        GuiSkin.play(SoundEvents.IRON_TRAPDOOR_CLOSE, 1.4f, 0.4f);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && phase == WIND)
            strike(false);
        return super.mouseReleased(x, y, button);
    }

    /** The hammer comes down (or the swing runs past the split point): grade it, answer with sound and sparks, send after the last. */
    private void strike(boolean split) {
        float f = split ? splitEnd : force;
        int grade = Upset.grade(f, centre, half, split);
        scores[strike] = Upset.score(f, centre, half, split);
        ghost = f;
        headU = split ? 1.2f : f / centre;
        popGrade = grade;
        popAge = 0;
        int top = plateTop - 6;
        switch (grade) {
            case Upset.PERFECT -> {
                GuiSkin.play(SoundEvents.ANVIL_PLACE, 1.7f, 0.7f);
                GuiSkin.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.1f + 0.4f * (1f - Math.abs(f - centre) / half), 0.8f);
                specks.burst(bx, top, 10, 1.8f, 2.2f, 0xFFF0A0, 22, 2);
            }
            case Upset.TIGHT -> {
                GuiSkin.play(SoundEvents.ANVIL_PLACE, 1.4f, 0.5f);
                specks.burst(bx, top, 4, 1.4f, 1.6f, SPARK, 16, 1);
            }
            case Upset.LOOSE -> GuiSkin.play(SoundEvents.CHAIN_PLACE, 0.8f, 0.7f);
            case Upset.FLAT -> {
                GuiSkin.play(SoundEvents.ANVIL_LAND, 1.1f, 0.5f);
                specks.burst(bx, top, 8, 2.4f, 1.2f, SPARK, 18, 1);
            }
            default -> {
                GuiSkin.play(SoundEvents.ANVIL_LAND, 0.6f, 0.6f);
                GuiSkin.play(SoundEvents.SHEEP_SHEAR, 0.6f, 0.7f);
                specks.burst(bx, top, 22, 2.8f, 3.2f, rgb, 30, 2);
                specks.burst(bx, top, 10, 2.4f, 3.2f, SPARK, 20, 1);
            }
        }
        if (strike + 1 >= rounds) {
            finalScore = Upset.mean(scores, rounds);
            if (!sent) {
                sent = true;
                MinigameScreens.report(containerId, finalScore);
            }
            phase = DONE;
            hold = DONE_TICKS;
            doneAt = Util.getMillis();
            int star = star();
            if (star >= 3) {
                GuiSkin.play(io.github.soundgoodizerfan.feedback.registry.FSounds.FORGE_FANFARE.get(), 1f);
                for (int wave = 0; wave < 3; wave++)
                    specks.burst(bx, top, 24, 2.2f + wave * 1.2f, 3.4f, 0xFFD84A, 34, 2);
            } else if (star <= 1)
                GuiSkin.play(io.github.soundgoodizerfan.feedback.registry.FSounds.FORGE_FAILURE.get(), 1f);
        } else {
            phase = SETTLE;
            settle = SETTLE_TICKS;
        }
    }

    /** The final grade 0..4. */
    private int star() {
        return finalScore < 0.3f ? 0 : finalScore < 0.55f ? 1 : finalScore < 0.78f ? 2 : finalScore < 0.92f ? 3 : 4;
    }

    @Override
    public void tick() {
        ticks++;
        popAge++;
        if (phase == WIND) {
            force += 1f / ModularTuning.UPSET_WIND_TICKS;
            if (ticks % 4 == 0)
                GuiSkin.play(SoundEvents.CHAIN_STEP, 0.7f + force, 0.3f);
            if (force >= splitEnd)
                strike(true);
        } else if (phase == SETTLE) {
            if (--settle <= 0) {
                strike++;
                newStrike();
            }
        } else if (phase == DONE && --hold <= 0 && !opening.closing())
            opening.close();
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
        partial = partialTick;
        frameG = g;
        opening.render(g, width, height, px, py, PW, PH, 0f, paintFrame);
    }

    private void paint(GuiGraphics g) {
        SKIN.panel(g, px, py, PW, PH);
        g.drawString(font, title, px + 12, py + 9, SKIN.ink(), false);
        for (int i = 0; i < rounds; i++) {
            int x = px + PW - 12 - (rounds - i) * 11 + 3;
            boolean did = i < strike || (i == strike && phase >= DONE);
            int col = !did ? 0xFF2A2D32 : scores[i] >= 0.8f ? BAND : scores[i] >= 0.5f ? SPARK : scores[i] > 0.15f ? 0xFFE0802F : STRIP;
            g.fill(x - 1, py + 8, x + 8, py + 17, 0xFF000000);
            g.fill(x, py + 9, x + 7, py + 16, col);
        }
        SKIN.inset(g, sx0, sy0, PW - 16, 150);
        plates(g);
        rivet(g);
        hammer(g);
        gauge(g);
        specks.draw(g, partial);
        popup(g);
        int hy = py + 180;
        if (phase == DONE) {
            long age = Util.getMillis() - doneAt;
            GuiStyle.stamp(g, results[star()], px + PW / 2f - 20, sy0 + 40, age, star() >= 3 ? 0x5BC25B : star() >= 2 ? 0xE0B040 : 0xD05040);
        } else {
            g.drawString(font, countText, px + 12, hy, SKIN.dim(), false);
            hy += 12;
            for (var l : hint) {
                g.drawString(font, l, px + 12, hy, SKIN.dim(), false);
                hy += 9;
            }
        }
    }

    /** Two plates in section with a drilled hole, the rivet's seat. */
    private void plates(GuiGraphics g) {
        int x0 = sx0 + 5, x1 = sx0 + PW - 16 - 5;
        for (int p = 0; p < 2; p++) {
            int y0 = plateTop + p * PLATE_H;
            g.fill(x0, y0, bx - SHANK_W / 2 - 1, y0 + PLATE_H, 0xFF15171A);
            g.fill(bx + SHANK_W / 2 + 1, y0, x1, y0 + PLATE_H, 0xFF15171A);
            g.fill(x0 + 1, y0 + 1, bx - SHANK_W / 2 - 1, y0 + PLATE_H - 1, PLATE);
            g.fill(bx + SHANK_W / 2 + 1, y0 + 1, x1 - 1, y0 + PLATE_H - 1, PLATE);
        }
    }

    /** The rivet: a formed head below the plates, the shank through them, and the end above upset by the last strike. */
    private void rivet(GuiGraphics g) {
        int base = 0xFF000000 | rgb;
        int shank = GuiSkin.lerp(base, 0xFF000000, 0.25f);
        int under = plateTop + PLATE_H * 2;
        g.fill(bx - 14, under, bx + 14, under + 7, base);
        g.fill(bx - 14, under, bx + 14, under + 2, GuiSkin.lerp(base, 0xFFFFFFFF, 0.3f));
        g.fill(bx - SHANK_W / 2, plateTop, bx + SHANK_W / 2, under, shank);
        // The stub above the plates: tall and thin until struck, then squashed wide by the force (headU is force over the wanted force).
        float u = phase >= SETTLE || phase == DONE ? headU : 0f;
        int w = Math.round(SHANK_W + 12f * Math.min(u, 1.4f));
        int h = Math.max(4, Math.round(16f - 10f * Math.min(u, 1.2f)));
        int top = plateTop - h;
        g.fill(bx - w / 2, top, bx + w / 2, plateTop, base);
        g.fill(bx - w / 2, top, bx + w / 2, top + 2, GuiSkin.lerp(base, 0xFFFFFFFF, 0.35f));
        if (phase >= SETTLE && popGrade == Upset.SPLIT)
            g.fill(bx - 1, top, bx + 1, plateTop, 0xFF15171A);
        // A hot rivet glows; the glow fades once struck.
        if (phase < SETTLE)
            g.fill(bx - w / 2, top, bx + w / 2, plateTop, 0x40FF6A20);
    }

    /** The hammer: raised by the hold, resting on the rivet once struck. */
    private void hammer(GuiGraphics g) {
        float lift = phase == WIND ? Math.min(1f, force) : phase == READY ? 0.05f : 0f;
        int hy = plateTop - 30 - Math.round(HAMMER_LIFT * lift);
        g.fill(bx - 16, hy, bx + 16, hy + 14, 0xFF1A1C20);
        g.fill(bx - 15, hy + 1, bx + 15, hy + 13, STEEL);
        g.fill(bx - 15, hy + 1, bx + 15, hy + 3, GuiSkin.lerp(STEEL, 0xFFFFFFFF, 0.35f));
        g.fill(bx - 2, hy - 40, bx + 2, hy, 0xFF1A1C20);
        g.fill(bx - 1, hy - 40, bx + 1, hy, GRIP);
    }

    private void gauge(GuiGraphics g) {
        int gx = px + PW - 8 - 6 - 26, gy = sy0 + 8, gh = 134;
        SKIN.inset(g, gx, gy, 26, gh);
        int ix = gx + 2, iw = 22, top = gy + 2, ih = gh - 4;
        int bandTop = top + Math.round(ih * (1f - (centre + half))), bandBot = top + Math.round(ih * (1f - (centre - half)));
        int splitTop = top + Math.round(ih * (1f - splitEnd));
        g.fill(ix, top, ix + iw, top + ih, 0xFF1B1D21);
        g.fill(ix, bandTop, ix + iw, bandBot, GuiSkin.lerp(BAND, 0xFF000000, 0.15f));
        g.fill(ix, splitTop, ix + iw, bandTop, GuiSkin.lerp(STRIP, 0xFF000000, 0.35f));
        float shown = phase == WIND ? force : phase == SETTLE || phase == DONE ? ghost : 0f;
        if (shown > 0f) {
            int fy = top + Math.round(ih * (1f - Math.min(1f, shown)));
            g.fill(ix + 6, fy, ix + iw - 6, top + ih, 0xB0000000 | rgb);
            g.fill(ix - 2, fy, ix + iw + 2, fy + 2, 0xFFFFFFFF);
        }
    }

    private void popup(GuiGraphics g) {
        if (popGrade < 0 || popAge > 40)
            return;
        int a = Math.min(255, 400 - popAge * 10);
        if (a <= 0)
            return;
        int rise = ClientSettings.reduceMotion() ? 0 : Math.min(14, popAge);
        int col = popGrade == Upset.PERFECT ? BAND : popGrade == Upset.TIGHT ? SPARK : popGrade == Upset.LOOSE ? STEEL : STRIP;
        Component w = words[popGrade];
        g.drawString(font, w, bx - font.width(w) / 2, sy0 + 6 - rise, (a << 24) | col, false);
    }
}
