// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;
import io.github.soundgoodizerfan.feedback.modular.engineering.Torque;
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
 * The Engineering Table's torque game: a bolt seated in a cut-away plate and a screwdriver driving it. Hold the mouse to wind the driver up;
 * the head turns (slower and slower as the thread binds), the bolt sinks and then stops, strains red and shakes as torque climbs, and
 * metal dust and sparks fly if it is pushed into the thread-strip zone. Let go inside the green band on the gauge for a clean, tight
 * fastener (a click and a chime); short of it the fastener is loose, past it the thread is stripped. One pull per fastener spent, about two
 * seconds each, the scores averaged into the ONE result sent ({@link MinigameScreens#report}) when the last fastener is graded; the stamp and
 * the stars that follow never delay it. A better fastener (quality) widens the green band. The bolt is drawn in its own material's colour.
 * Esc before the last pull sends nothing and the craft stays offered. Reduce motion stills the idle drift and the shake; sounds stay.
 */
final class TorqueScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.ENAMEL;
    private static final int PW = 220, PH = 236, READY = 0, WIND = 1, SETTLE = 2, DONE = 3;
    private static final int SETTLE_TICKS = 26, DONE_TICKS = 80;
    private static final int SINK = 24, SHANK = 32, HEAD_H = 10, HEAD_R = 15, HOLE_W = 9, HOLE_DEPTH = 46;
    /** Screen-local colours: the plate, the bands, the sparks and the driver's grip are the subject of this screen, not the skin's. */
    private static final int PLATE = 0xFF5D626A, PLATE_DARK = 0xFF35393F, STEEL = 0xFF9AA0A8, GRIP = 0xFFD9822B, BAND = 0xFF4FBF5A,
            STRIP = 0xFFC2452F, SPARK = 0xFFFFC04A, HOT = 0xFFE05030;
    private static final double TAU = Math.PI * 2;

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId, rounds, quality, seed, rgb;
    private final float[] scores;
    private final Specks specks = new Specks(72);
    private final Component title = Component.translatable("gui.feedback.torque.title");
    private final Component[] words = new Component[5], results = new Component[5];
    private List<FormattedCharSequence> hint = List.of();
    private Component pullText = Component.empty();
    private int px, py, bx, plateTop, sx0, sy0, ticks, phase = READY, pull, settle, hold, popGrade = -1, popAge, wasZone;
    private long doneAt;
    private float centre, half, stripEnd, torque, angle, angleV, sink, ghost = -1f, partial, finalScore, ratchet;
    private boolean sent, held;
    private int mouseX, mouseY;

    TorqueScreen(Screen parent, EngineeringTableMenu menu, MinigameOpenPayload session) {
        super(Component.translatable("gui.feedback.torque.title"));
        this.parent = parent;
        this.containerId = session.container();
        this.rounds = Math.max(1, Math.min(ModularTuning.TORQUE_MAX_PULLS, session.rounds()));
        this.quality = Mth.clamp(session.quality(), 1, 5);
        this.seed = containerId * 31 + rounds;
        this.scores = new float[rounds];
        this.rgb = fastenerColour(menu);
        for (int i = 0; i < 5; i++) {
            words[i] = Component.translatable("gui.feedback.torque.pull." + i);
            results[i] = Component.translatable("gui.feedback.torque.result." + i);
        }
        newPull();
    }

    /** The colour of the first fastener in the table's fastener slots (its material's colour), else steel. */
    private static int fastenerColour(EngineeringTableMenu menu) {
        for (int i = 0; i < 3; i++) {
            ItemStack stack = menu.getSlot(EngineeringTableMenu.slot(EngineeringTableBlockEntity.FASTENERS + i)).getItem();
            var found = stack.isEmpty() ? java.util.Optional.<io.github.soundgoodizerfan.feedback.process.Material.Located>empty()
                    : MaterialIngredient.locate(stack.getItem());
            if (found.isPresent())
                return MaterialColors.of(found.get().id());
        }
        return 0xB8BEC6;
    }

    private void newPull() {
        centre = Torque.centre(seed, pull);
        half = Torque.half(quality);
        stripEnd = Torque.stripEnd(centre, half);
        torque = 0f;
        sink = 0f;
        angleV = 0f;
        wasZone = 0;
        ratchet = 0f;
        phase = READY;
        held = false;
        pullText = Component.translatable("gui.feedback.torque.count", pull + 1, rounds);
    }

    @Override
    protected void init() {
        opening.start();
        px = (width - PW) / 2;
        py = Math.max(0, (height - PH) / 2);
        sx0 = px + 8;
        sy0 = py + 24;
        bx = px + 78;
        plateTop = sy0 + 92;
        hint = font.split(Component.translatable("gui.feedback.torque.hint"), PW - 24);
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
        held = true;
        GuiSkin.play(SoundEvents.IRON_TRAPDOOR_CLOSE, 1.7f, 0.5f);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && phase == WIND)
            release(false);
        return super.mouseReleased(x, y, button);
    }

    /** The torque gets let go (or the thread gives): grade the pull, answer with sound and sparks, and send the result after the last. */
    private void release(boolean stripped) {
        held = false;
        float t = stripped ? stripEnd : torque;
        int grade = Torque.grade(t, centre, half, stripped);
        scores[pull] = Torque.score(t, centre, half, stripped);
        ghost = t;
        popGrade = grade;
        popAge = 0;
        int hy = headY();
        switch (grade) {
            case Torque.PERFECT -> {
                GuiSkin.play(SoundEvents.LEVER_CLICK, 1.5f, 0.9f);
                GuiSkin.play(SoundEvents.ANVIL_PLACE, 1.9f, 0.35f);
                GuiSkin.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.1f + 0.4f * (1f - Math.abs(t - centre) / half), 0.8f);
                specks.burst(bx, hy, 10, 1.8f, 2.2f, 0xFFF0A0, 22, 2);
            }
            case Torque.SNUG -> {
                GuiSkin.play(SoundEvents.LEVER_CLICK, 1.2f, 0.9f);
                GuiSkin.play(SoundEvents.ANVIL_PLACE, 1.6f, 0.25f);
                specks.burst(bx, hy, 4, 1.4f, 1.6f, SPARK, 16, 1);
            }
            case Torque.LOOSE -> GuiSkin.play(SoundEvents.LEVER_CLICK, 0.7f, 0.6f);
            case Torque.OVER -> {
                GuiSkin.play(SoundEvents.LEVER_CLICK, 0.8f, 0.8f);
                GuiSkin.play(SoundEvents.GRINDSTONE_USE, 1.5f, 0.3f);
                specks.burst(bx, plateTop, 8, 2.2f, 2.4f, SPARK, 18, 1);
            }
            default -> {
                GuiSkin.play(SoundEvents.ANVIL_LAND, 0.6f, 0.5f);
                GuiSkin.play(SoundEvents.SHEEP_SHEAR, 0.6f, 0.7f);
                specks.burst(bx, plateTop, 22, 2.8f, 2.8f, rgb, 30, 2);
                specks.burst(bx, plateTop, 10, 2.4f, 3.2f, SPARK, 20, 1);
                sink = Math.max(0f, sink - 4f);
            }
        }
        if (pull + 1 >= rounds) {
            finalScore = Torque.mean(scores, rounds);
            if (!sent) {
                sent = true;
                MinigameScreens.report(containerId, finalScore);
            }
            phase = DONE;
            hold = DONE_TICKS;
            doneAt = Util.getMillis();
            int star = star();
            if (star >= 3) {
                // The forge's own fanfare and gold bursts for a win, its failure sting for a stripped or loose finish.
                GuiSkin.play(io.github.soundgoodizerfan.feedback.registry.FSounds.FORGE_FANFARE.get(), 1f);
                for (int wave = 0; wave < 3; wave++)
                    specks.burst(bx, plateTop, 24, 2.2f + wave * 1.2f, 3.4f, 0xFFD84A, 34, 2);
            } else if (star <= 1)
                GuiSkin.play(io.github.soundgoodizerfan.feedback.registry.FSounds.FORGE_FAILURE.get(), 1f);
            else
                GuiSkin.play(SoundEvents.ANVIL_LAND, 0.8f, 0.7f);
        } else {
            phase = SETTLE;
            settle = SETTLE_TICKS;
        }
    }

    /** The final grade 0..4 (stripped, loose, snug, tight, perfect). */
    private int star() {
        return finalScore < 0.3f ? 0 : finalScore < 0.55f ? 1 : finalScore < 0.78f ? 2 : finalScore < 0.92f ? 3 : 4;
    }

    @Override
    public void tick() {
        ticks++;
        popAge++;
        boolean still = ClientSettings.reduceMotion();
        float lo = centre - half;
        if (phase == WIND) {
            float before = torque;
            torque += 1f / ModularTuning.TORQUE_WIND_TICKS;
            float seat = Math.max(0.1f, centre - half * 0.6f);
            sink = Math.min(SINK, SINK * torque / seat);
            // The head turns fast while the thread runs free and slower as it binds; in the strip zone it jerks.
            float free = Mth.clamp(1f - torque / Math.max(0.2f, centre + half), 0.08f, 1f);
            boolean strip = torque > centre + half;
            angleV = strip ? (ticks % 4 == 0 ? 0.30f : 0.04f) : 0.5f * free;
            angle += angleV;
            ratchet += angleV;
            if (ratchet >= Mth.PI / 3f) {
                ratchet -= Mth.PI / 3f;
                GuiSkin.play(SoundEvents.CHAIN_STEP, 0.7f + torque * 1.0f, 0.45f);
            }
            if (before < lo && torque >= lo) {
                GuiSkin.play(SoundEvents.LEVER_CLICK, 1.4f, 0.4f);
                specks.burst(bx, plateTop - 2, 3, 1.2f, 1.2f, rgb, 12, 1);
            }
            // The bolt creaks as it is driven hard: a thin squeal that climbs with the torque.
            if (torque > lo && ticks % 6 == 0)
                GuiSkin.play(SoundEvents.GRINDSTONE_USE, 1.3f + (torque - lo) * 2.2f, strip ? 0.28f : 0.14f);
            if (strip) {
                if (wasZone == 0) {
                    wasZone = 1;
                    GuiSkin.play(SoundEvents.ANVIL_LAND, 1.9f, 0.3f);
                }
                if (ticks % 2 == 0)
                    specks.burst(bx, plateTop, 2, 1.8f, 1.8f, ticks % 4 == 0 ? SPARK : rgb, 14, 1);
            } else if (torque > lo && ticks % 5 == 0) {
                specks.burst(bx + (ticks % 3 - 1) * 4, plateTop, 1, 0.8f, 1.0f, rgb, 12, 1);
            }
            if (torque >= stripEnd)
                release(true);
        } else if (phase == SETTLE) {
            angle += angleV;
            angleV *= 0.85f;
            if (--settle <= 0) {
                pull++;
                newPull();
            }
        } else if (phase == READY && !still) {
            angle += 0.012f;
        } else if (phase == DONE) {
            angleV *= 0.85f;
            angle += angleV;
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
        opening.render(g, width, height, px, py, PW, PH, 0f, paintFrame);
    }

    private int shake() {
        if (ClientSettings.reduceMotion())
            return 0;
        float strain = phase == WIND ? Mth.clamp((torque - (centre - half)) / (stripEnd - (centre - half)), 0f, 1f) : 0f;
        return Math.round(Mth.sin((ticks + partial) * 2.3f) * strain * 2.5f);
    }

    private int headY() {
        return plateTop - 26 + Math.round(sink) - HEAD_H / 2;
    }

    private void paint(GuiGraphics g) {
        SKIN.panel(g, px, py, PW, PH);
        g.drawString(font, title, px + 12, py + 9, SKIN.ink(), false);
        // One mark per pull, filled with how it went.
        for (int i = 0; i < rounds; i++) {
            int x = px + PW - 12 - (rounds - i) * 11 + 3;
            boolean did = i < pull || (i == pull && phase >= DONE);
            int col = !did ? 0xFF2A2D32 : scores[i] >= 0.8f ? BAND : scores[i] >= 0.5f ? SPARK : scores[i] > 0.15f ? 0xFFE0802F : STRIP;
            g.fill(x - 1, py + 8, x + 8, py + 17, 0xFF000000);
            g.fill(x, py + 9, x + 7, py + 16, col);
        }
        SKIN.inset(g, sx0, sy0, PW - 16, 150);
        plate(g);
        int shake = shake();
        bolt(g, shake);
        gauge(g);
        specks.draw(g, partial);
        popup(g);
        int hy = py + 180;
        if (phase == DONE) {
            long age = Util.getMillis() - doneAt;
            GuiStyle.stamp(g, results[star()], px + PW / 2f - 20, sy0 + 40, age, star() >= 3 ? 0x5BC25B : star() >= 2 ? 0xE0B040 : 0xD05040);
            stars(g, px + PW / 2 - 24, hy + 4);
        } else {
            g.drawString(font, pullText, px + 12, hy, SKIN.dim(), false);
            hy += 12;
            for (var l : hint) {
                g.drawString(font, l, px + 12, hy, SKIN.dim(), false);
                hy += 9;
            }
        }
    }

    /** The plate in section: steel with brushed lines, a threaded hole the bolt runs into. */
    private void plate(GuiGraphics g) {
        int x0 = sx0 + 5, x1 = sx0 + PW - 16 - 5, y1 = sy0 + 150 - 4;
        g.fill(x0, plateTop, x1, y1, 0xFF15171A);
        g.fill(x0 + 1, plateTop + 1, x1 - 1, y1 - 1, PLATE);
        g.fill(x0 + 1, plateTop + 1, x1 - 1, plateTop + 3, 0x40FFFFFF);
        for (int y = plateTop + 8; y < y1 - 2; y += 5)
            g.fill(x0 + 2, y, x1 - 2, y + 1, 0x14000000);
        g.fill(bx - HOLE_W, plateTop, bx + HOLE_W, plateTop + HOLE_DEPTH, PLATE_DARK);
        for (int y = plateTop + 2; y < plateTop + HOLE_DEPTH - 2; y += 4) {
            g.fill(bx - HOLE_W, y, bx - HOLE_W + 2, y + 2, STEEL);
            g.fill(bx + HOLE_W - 2, y + 2, bx + HOLE_W, y + 4, STEEL);
        }
    }

    private void bolt(GuiGraphics g, int shake) {
        int cx = bx + shake;
        int headBottom = plateTop - 26 + Math.round(sink);
        int headTop = headBottom - HEAD_H;
        float tint = phase == WIND || phase == SETTLE ? Mth.clamp((Math.max(torque, ghost < 0 ? 0 : phase == SETTLE ? ghost : 0) - (centre - half)) / (stripEnd - (centre - half)), 0f, 1f) : 0f;
        int base = GuiSkin.lerp(0xFF000000 | rgb, HOT, tint * 0.6f);
        // Shank: darker than the head, its thread a set of slanted lines that scroll with the turn.
        int tip = headBottom + SHANK - (int) (tint * 2f);
        g.fill(cx - 7, headBottom, cx + 7, tip, GuiSkin.lerp(base, 0xFF000000, 0.35f));
        float off = ((angle * 0.9f) % 4f + 4f) % 4f;
        int light = GuiSkin.lerp(base, 0xFFFFFFFF, 0.25f);
        for (float y = headBottom - 4 + off; y < tip - 1; y += 4f) {
            int yy = Math.max(headBottom, Math.round(y));
            g.fill(cx - 7, yy, cx, Math.min(tip, yy + 1), light);
            g.fill(cx, Math.min(tip - 1, yy + 1), cx + 7, Math.min(tip, yy + 2), light);
        }
        g.fill(cx - 4, tip, cx + 4, tip + 1, GuiSkin.lerp(base, 0xFF000000, 0.35f));
        // Head: a hex prism, six faces turning, each shaded by how squarely it faces us.
        for (int i = 0; i < 6; i++) {
            double a0 = angle + i * Math.PI / 3, a1 = a0 + Math.PI / 3;
            int x0 = cx + Math.round(HEAD_R * (float) Math.sin(a0)), x1 = cx + Math.round(HEAD_R * (float) Math.sin(a1));
            if (x1 <= x0)
                continue;
            float shade = Mth.clamp(0.4f + 0.7f * (float) Math.cos(a0 + Math.PI / 6), 0.25f, 1.1f);
            int face = GuiSkin.lerp(0xFF000000, base, Math.min(1f, shade));
            g.fill(x0, headTop, x1, headBottom, face);
            g.fill(x0, headTop, x1, headTop + 2, GuiSkin.lerp(face, 0xFFFFFFFF, 0.35f));
            g.fill(x0, headBottom - 1, x1, headBottom, GuiSkin.lerp(face, 0xFF000000, 0.4f));
        }
        // The driver: a bit in the head, a shaft, and a grip with ridges rolling round it.
        int bob = phase == READY && !ClientSettings.reduceMotion() ? Math.round(Mth.sin((ticks + partial) * 0.12f)) : 0;
        int bitTop = headTop - 5 + bob;
        g.fill(cx - 12, bitTop, cx + 12, bitTop + 8, 0xFF1A1C20);
        g.fill(cx - 11, bitTop + 1, cx + 11, bitTop + 7, STEEL);
        g.fill(cx - 3, bitTop - 20, cx + 3, bitTop, 0xFF1A1C20);
        g.fill(cx - 2, bitTop - 20, cx + 2, bitTop, GuiSkin.lerp(STEEL, 0xFFFFFFFF, 0.3f));
        int gt = bitTop - 50, gb = bitTop - 20;
        g.fill(cx - 13, gt, cx + 13, gb, 0xFF1A1C20);
        g.fill(cx - 12, gt + 1, cx + 12, gb - 1, GRIP);
        int dark = GuiSkin.lerp(GRIP, 0xFF000000, 0.45f);
        for (int i = 0; i < 12; i++) {
            double a = angle + i * Math.PI / 6;
            if (Math.cos(a) <= 0.05)
                continue;
            int x = cx + Math.round(11f * (float) Math.sin(a));
            g.fill(x, gt + 2, x + 1, gb - 2, dark);
        }
        g.fill(cx - 12, gt + 1, cx - 8, gb - 1, 0x30000000);
        g.fill(cx + 8, gt + 1, cx + 12, gb - 1, 0x30000000);
    }

    /** The gauge: a vertical scale with the green band, the thread-strip zone above it, the winding fill in the fastener's colour and a needle. */
    private void gauge(GuiGraphics g) {
        int gx = px + PW - 8 - 6 - 26, gy = sy0 + 8, gh = 134;
        SKIN.inset(g, gx, gy, 26, gh);
        int ix = gx + 2, iw = 22, top = gy + 2, ih = gh - 4;
        int bandTop = top + Math.round(ih * (1f - (centre + half))), bandBot = top + Math.round(ih * (1f - (centre - half)));
        int stripTop = top + Math.round(ih * (1f - stripEnd));
        g.fill(ix, top, ix + iw, top + ih, 0xFF1B1D21);
        g.fill(ix, bandTop, ix + iw, bandBot, GuiSkin.lerp(BAND, 0xFF000000, 0.15f));
        int mid = top + Math.round(ih * (1f - centre));
        g.fill(ix, mid, ix + iw, mid + 1, 0x80FFFFFF);
        g.fill(ix, stripTop, ix + iw, bandTop, GuiSkin.lerp(STRIP, 0xFF000000, 0.35f));
        for (int y = stripTop + (bandTop - stripTop) % 3; y < bandTop; y += 3)
            g.fill(ix, y, ix + iw, y + 1, STRIP);
        g.fill(ix, top, ix + iw, stripTop, 0xFF0C0D0F);
        for (int i = 0; i <= 10; i++)
            g.fill(gx + 26, top + Math.round(ih * (1f - i / 10f)), gx + 26 + (i % 5 == 0 ? 4 : 2), top + Math.round(ih * (1f - i / 10f)) + 1, SKIN.dim() | 0xFF000000);
        float shown = phase == WIND ? torque : phase == SETTLE || phase == DONE ? ghost : 0f;
        if (shown > 0f) {
            int fy = top + Math.round(ih * (1f - Math.min(1f, shown)));
            g.fill(ix + 6, fy, ix + iw - 6, top + ih, 0xB0000000 | rgb);
            g.fill(ix - 2, fy, ix + iw + 2, fy + 2, 0xFFFFFFFF);
        }
    }

    /** The word for the last pull, floating up from the bolt and fading. */
    private void popup(GuiGraphics g) {
        if (popGrade < 0 || popAge > 40)
            return;
        int a = Math.min(255, 400 - popAge * 10);
        if (a <= 0)
            return;
        int rise = ClientSettings.reduceMotion() ? 0 : Math.min(14, popAge);
        int col = popGrade == Torque.PERFECT ? BAND : popGrade == Torque.SNUG ? SPARK : popGrade == Torque.LOOSE ? STEEL : STRIP;
        Component w = words[popGrade];
        g.drawString(font, w, bx - font.width(w) / 2, sy0 + 6 - rise, (a << 24) | col, false);
    }

    /** Five diamonds, lit by the final grade (one to five). */
    private void stars(GuiGraphics g, int x, int y) {
        int lit = star() + 1;
        for (int i = 0; i < 5; i++) {
            int cx = x + i * 10 + 4;
            int col = i < lit ? 0xFFF0C040 : 0xFF2A2D32;
            for (int r = -3; r <= 3; r++)
                g.fill(cx - (4 - Math.abs(r)) + 1, y + 4 + r, cx + (4 - Math.abs(r)), y + 5 + r, col);
        }
    }
}
