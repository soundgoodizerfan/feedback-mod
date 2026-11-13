// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;
import io.github.soundgoodizerfan.feedback.modular.engineering.FittingOrder;
import io.github.soundgoodizerfan.feedback.modular.engineering.FittingSequence;
import io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;
import io.github.soundgoodizerfan.feedback.client.gui.GuiStyle;
import io.github.soundgoodizerfan.feedback.client.gui.Opening;
import io.github.soundgoodizerfan.feedback.client.gui.Specks;

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
 * The Engineering Table's fitting game, a Simon Says of tool strokes: the bench plays a sequence of strokes (file, tap, turn, press), each lit
 * on its tool with a sound, and the player repeats it by clicking the tools or pressing 1 to 4. What the parts are made of drives the play
 * ({@link FittingSequence}: harder stock, a longer sequence; tougher stock, slower strokes and a forgiven slip). A wrong stroke ends the play
 * there and the score is how far it got; it only sets the assembly's start quality and never gates. ONE result goes out
 * ({@link MinigameScreens#report}) when the play ends; Esc before then sends nothing and the craft stays offered. Reduce motion stills the
 * bobbing and the rise; sounds stay.
 */
final class FittingScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.ENAMEL;
    private static final int PW = 240, PH = 200, WATCH = 0, REPEAT = 1, DONE = 2, BTN = 44, GAP = 8;
    private static final int[] TINT = {0xC2452F, 0x4FBF5A, 0x4F8FDF, 0xFFC04A};
    private static final net.minecraft.sounds.SoundEvent[] SOUND = {SoundEvents.GRINDSTONE_USE, SoundEvents.ANVIL_PLACE,
            SoundEvents.UI_BUTTON_CLICK.value(), SoundEvents.PISTON_CONTRACT};
    private static final float[] PITCH = {1.6f, 1.3f, 1.0f, 0.7f};

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId, n, length, stepTicks, slack;
    private final int[] seq;
    private final ItemStack made;
    private final Specks specks = new Specks(96);
    private final Component title = Component.translatable("gui.feedback.fitting.title");
    private final Component[] strokeNames = new Component[FittingSequence.STROKES];
    private final Component[] results = new Component[FittingOrder.GRADES];
    private List<FormattedCharSequence> hint = List.of();
    private int px0, py0, ticks, phase = WATCH, shown = -1, clock, reached, slips, lit = -1, litAge, wrong = -1, wrongAge, hold, flash;
    private int mouseX, mouseY;
    private long doneAt;
    private float finalScore, partial;
    private boolean sent;

    FittingScreen(Screen parent, EngineeringTableMenu menu, MinigameOpenPayload session) {
        super(Component.translatable("gui.feedback.fitting.title"));
        this.parent = parent;
        this.containerId = session.container();
        int[][] rows = FittingOrder.decode(session.data());
        this.n = rows.length;
        float hardness = 0f, toughness = 0f;
        int found = 0;
        for (int[] row : rows) {
            ItemStack s = menu.getSlot(EngineeringTableMenu.slot(EngineeringTableBlockEntity.GRID + row[0])).getItem();
            var located = s.isEmpty() ? java.util.Optional.<io.github.soundgoodizerfan.feedback.process.Material.Located>empty()
                    : MaterialIngredient.locate(s.getItem());
            if (located.isPresent()) {
                var m = located.get().material();
                hardness += m.hardness().value();
                toughness += m.mechanical().toughness().map(t -> t.value()).orElse(m.hardness().value());
                found++;
            }
        }
        if (found > 0) {
            hardness /= found;
            toughness /= found;
        }
        this.length = FittingSequence.length(n, hardness);
        this.stepTicks = FittingSequence.stepTicks(toughness);
        this.slack = FittingSequence.slack(toughness);
        this.seq = FittingSequence.strokes(containerId * 31L + n * 7L + (long) (hardness * 10f), length);
        this.made = menu.getSlot(EngineeringTableMenu.OUT).getItem().copy();
        for (int i = 0; i < FittingSequence.STROKES; i++)
            strokeNames[i] = Component.translatable("gui.feedback.fitting.stroke." + i);
        for (int i = 0; i < FittingOrder.GRADES; i++)
            results[i] = Component.translatable("gui.feedback.fitting.result." + i);
    }

    @Override
    protected void init() {
        opening.start();
        px0 = (width - PW) / 2;
        py0 = Math.max(0, (height - PH) / 2);
        hint = font.split(Component.translatable("gui.feedback.fitting.hint"), PW - 24);
        clock = -stepTicks * 2; // a beat before the first stroke
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int btnX(int k) {
        int total = FittingSequence.STROKES * BTN + (FittingSequence.STROKES - 1) * GAP;
        return px0 + (PW - total) / 2 + k * (BTN + GAP);
    }

    private int btnY() {
        return py0 + 100;
    }

    private int strokeAt(double mx, double my) {
        for (int k = 0; k < FittingSequence.STROKES; k++)
            if (mx >= btnX(k) && mx < btnX(k) + BTN && my >= btnY() && my < btnY() + BTN)
                return k;
        return -1;
    }

    /** One stroke lit on its tool, with its sound and a spray of sparks. */
    private void strike(int k, boolean good) {
        lit = k;
        litAge = 0;
        GuiSkin.play(SOUND[k], PITCH[k] + (good ? 0.05f * Math.min(reached, 8) : 0f), 0.7f);
        specks.burst(btnX(k) + BTN / 2f, btnY() + 6, good ? 8 : 4, 1.8f, 2.2f, TINT[k] | 0xFF000000, 18, 2);
    }

    private void press(int k) {
        if (phase != REPEAT || !opening.settled())
            return;
        if (seq[reached] == k) {
            strike(k, true);
            reached++;
            if (reached >= length)
                finish();
        } else {
            wrong = k;
            wrongAge = 0;
            GuiSkin.play(SoundEvents.IRON_TRAPDOOR_OPEN, 0.55f, 0.6f);
            GuiSkin.play(SoundEvents.CHAIN_HIT, 0.7f, 0.8f);
            if (slips++ >= slack)
                finish();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (phase == DONE) {
            hold = 0;
            return true;
        }
        if (button == 0) {
            int k = strokeAt(mx, my);
            if (k >= 0)
                press(k);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (opening.closing())
            return true;
        if (key >= org.lwjgl.glfw.GLFW.GLFW_KEY_1 && key <= org.lwjgl.glfw.GLFW.GLFW_KEY_4) {
            press(key - org.lwjgl.glfw.GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    /** The play ends: send the one result and show the grade. */
    private void finish() {
        finalScore = FittingSequence.score(reached, length);
        if (!sent) {
            sent = true;
            MinigameScreens.report(containerId, finalScore);
        }
        phase = DONE;
        hold = 90;
        doneAt = Util.getMillis();
        flash = 12;
        int star = FittingOrder.grade(finalScore);
        specks.burst(px0 + PW / 2f, py0 + 70, 22, 2.6f, 3.2f, 0xFFF0A0, 30, 2);
        if (star >= 3) {
            GuiSkin.play(io.github.soundgoodizerfan.feedback.registry.FSounds.FORGE_FANFARE.get(), 1f);
        } else if (star <= 1)
            GuiSkin.play(io.github.soundgoodizerfan.feedback.registry.FSounds.FORGE_FAILURE.get(), 1f);
        else
            GuiSkin.play(SoundEvents.ANVIL_LAND, 0.8f, 0.7f);
    }

    @Override
    public void tick() {
        ticks++;
        if (litAge < 100)
            litAge++;
        if (wrongAge < 100)
            wrongAge++;
        if (phase == WATCH && opening.settled()) {
            clock++;
            if (clock >= 0) {
                int step = clock / stepTicks;
                if (step >= length) {
                    phase = REPEAT;
                    lit = -1;
                } else if (clock % stepTicks == 0 && step != shown) {
                    shown = step;
                    strike(seq[step], false);
                }
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
        // One pip per stroke of the sequence: lit as the bench plays it, filled as the player repeats it.
        int pipW = Math.min(14, (PW - 24) / Math.max(1, length));
        int pipX = px0 + (PW - pipW * length) / 2;
        for (int i = 0; i < length; i++) {
            boolean done = phase == WATCH ? i <= shown : i < reached;
            int col = done ? (0xFF000000 | TINT[seq[i]]) : 0xFF2A2D32;
            if (phase == DONE && i >= reached)
                col = 0xFF6A2A22;
            g.fill(pipX + i * pipW, py0 + 26, pipX + i * pipW + pipW - 2, py0 + 34, 0xFF000000);
            g.fill(pipX + i * pipW + 1, py0 + 27, pipX + i * pipW + pipW - 3, py0 + 33, col);
        }
        // The work: the finished item on the bench, bobbing while the bench plays.
        ItemStack show = made;
        float bob = phase == WATCH && !ClientSettings.reduceMotion() ? Mth.sin((ticks + partial) * 0.2f) * 2f : 0f;
        g.pose().pushPose();
        g.pose().translate(px0 + PW / 2f - 24, py0 + 46 + bob, 0f);
        g.pose().scale(3f, 3f, 1f);
        g.renderItem(show, 0, 0);
        g.pose().popPose();
        for (int k = 0; k < FittingSequence.STROKES; k++)
            tool(g, k);
        specks.draw(g, partial);
        if (flash > 0)
            g.fill(px0 + 1, py0 + 1, px0 + PW - 1, py0 + PH - 1, ((int) (flash / 12f * 150f) << 24) | 0xFFFFFF);
        int hy = py0 + 152;
        if (phase == DONE) {
            long age = Util.getMillis() - doneAt;
            int star = FittingOrder.grade(finalScore);
            GuiStyle.stamp(g, results[star], px0 + PW / 2f - 20, py0 + 70, age, star >= 3 ? 0x5BC25B : star >= 2 ? 0xE0B040 : 0xD05040);
            stars(g, px0 + PW / 2 - 24, hy + 8, star + 1);
        } else {
            Component state = Component.translatable(phase == WATCH ? "gui.feedback.fitting.watch" : "gui.feedback.fitting.repeat", reached, length);
            g.drawString(font, state, px0 + 12, hy, SKIN.ink(), false);
            hy += 12;
            for (var l : hint) {
                g.drawString(font, l, px0 + 12, hy, SKIN.dim(), false);
                hy += 9;
            }
        }
    }

    /** One tool button: lit in its colour while it strikes, red when it was the wrong one, its name and key beneath. */
    private void tool(GuiGraphics g, int k) {
        int x = btnX(k), y = btnY();
        boolean hover = phase == REPEAT && strokeAt(mouseX, mouseY) == k;
        boolean on = lit == k && litAge < 6;
        boolean bad = wrong == k && wrongAge < 10;
        g.fill(x, y, x + BTN, y + BTN, 0xFF15171A);
        int fill = bad ? 0xFFC2452F : on ? 0xFF000000 | TINT[k] : hover ? 0xFF3A3E44 : 0xFF2B2E33;
        g.fill(x + 2, y + 2, x + BTN - 2, y + BTN - 2, fill);
        glyph(g, k, x + BTN / 2, y + BTN / 2 - 4, on || bad ? 0xFF15171A : 0xFFC2C8D0);
        Component name = strokeNames[k];
        g.drawString(font, name, x + (BTN - font.width(name)) / 2, y + BTN - 11, on || bad ? 0xFF15171A : SKIN.dim(), false);
        g.drawString(font, Integer.toString(k + 1), x + 4, y + 4, on || bad ? 0xFF15171A : SKIN.dim(), false);
    }

    /** A small pictogram of the stroke: file (diagonal teeth), tap (a dot over a bar), turn (an open ring with a tick), press (a down arrow). */
    private static void glyph(GuiGraphics g, int k, int cx, int cy, int c) {
        switch (k) {
            case 0 -> {
                for (int i = -8; i <= 8; i += 3)
                    g.fill(cx + i, cy + i / 3, cx + i + 2, cy + i / 3 + 5, c);
            }
            case 1 -> {
                g.fill(cx - 2, cy - 8, cx + 3, cy - 3, c);
                g.fill(cx - 8, cy + 2, cx + 9, cy + 4, c);
            }
            case 2 -> {
                GuiStyle.ring(g, cx, cy, 7, 2, c);
                g.fill(cx + 3, cy - 9, cx + 9, cy - 7, c);
            }
            default -> {
                g.fill(cx - 1, cy - 8, cx + 2, cy + 2, c);
                g.fill(cx - 5, cy + 2, cx + 6, cy + 4, c);
                g.fill(cx - 3, cy + 4, cx + 4, cy + 6, c);
            }
        }
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
