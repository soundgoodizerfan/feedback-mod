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
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.PrepWork;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.net.PrepareBeginPayload;
import io.github.soundgoodizerfan.feedback.net.PrepareFinishPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.FoodCut;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Prep Table's cut ({@code docs/feedback_modular_dishes_spec.md} 4, "place the marks"): the food as a slab, a knife that
 * follows the pointer. Left-click lays a cut, right-click lifts the nearest. The food's own figures change the stroke: fibre
 * pulls a cut toward the grain, fat lets the blade slide on, water makes it squirt and squish ({@link PrepWork#land}, the
 * same function the server grades with). Pieces are drawn between the cuts, so an uneven cut looks uneven; there is no
 * target and no number. Done sends the one packet at once ({@link #finish}) and the grade is known, but the board is covered
 * with a cloth while the music of that grade plays, then lifted with the fanfare (click or Esc skips); Esc before Done discards.
 */
public class PrepCutScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.BRICK;
    private static final int PW = 300, PH = 204, BAR_W = 240, BAR_H = 26, ICON = 64, FLOURISH = 18, REVEAL_HOLD = 40, STROKE = 6;
    private static final int PLAY = 0, JUDGE = 1, REVEAL = 2;
    /** The cloth over the board while it is judged (screen-local colours: the cloth is the subject). */
    private static final int CLOTH = 0xFFEADFC8, CLOTH_CHECK = 0x60B04040;

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId;
    private final ItemStack food;
    private final Material.Food figures;
    private final int rgb;
    /** Where each stroke was aimed (what the server is sent) and where it landed, in placing order; {@code sorted} is the landed ones in line order. */
    private final int[] aimed = new int[FTuning.CUT_MAX_MARKS], landed = new int[FTuning.CUT_MAX_MARKS], sorted = new int[FTuning.CUT_MAX_MARKS];
    private int count, stroke, flourish, phase, judge, ticks;
    private SimpleSoundInstance bgm;
    private FoodCut cut = new FoodCut(0, FoodCut.EVEN);
    private boolean sent;
    private int mouseX, mouseY, px, py, barX, barY;
    private float partial;
    private final Specks specks = new Specks(48);
    private List<net.minecraft.util.FormattedCharSequence> hint = List.of();

    private PrepCutScreen(Screen parent, int containerId, ItemStack food, Material.Food figures, int rgb) {
        super(Component.translatable("gui.feedback.prep_cut.title"));
        this.parent = parent;
        this.containerId = containerId;
        this.food = food;
        this.figures = figures;
        this.rgb = rgb;
    }

    /** Open over the Prep Table's screen and tell the server the cut has begun. */
    static void open(Screen parent, ToolTableMenu menu) {
        var level = Minecraft.getInstance().level;
        ItemStack food = menu.work().copyWithCount(1);
        Optional<Material.Located> what = Material.find(ClientMaterials.on(level), food);
        if (what.isEmpty() || what.get().material().food().isEmpty())
            return;
        PacketDistributor.sendToServer(new PrepareBeginPayload(menu.containerId, 0));
        Minecraft.getInstance().setScreen(new PrepCutScreen(parent, menu.containerId, food, what.get().material().food().get(),
                MaterialColors.of(what.get().id())));
    }

    @Override
    protected void init() {
        opening.start();
        px = (width - PW) / 2;
        py = Math.max(0, (height - PH) / 2);
        barX = px + (PW - BAR_W) / 2;
        barY = py + 116;
        hint = font.split(Component.translatable("gui.feedback.prep_cut.hint"), PW - 24);
        addRenderableWidget(GuiStyle.primary(SKIN, Component.translatable("gui.feedback.prep_cut.done"), b -> finish(), px + PW / 2 - 40, py + PH - 26, 80, 16, FSounds.UI_STAMP::get));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean overBar(double x, double y) {
        return x >= barX - 6 && x <= barX + BAR_W + 6 && y >= barY - 34 && y <= barY + BAR_H + 8;
    }

    private int aim(double x) {
        return Mth.clamp(Math.round((float) (x - barX) * PrepWork.LINE / BAR_W), 0, PrepWork.LINE);
    }

    private int at(int line) {
        return barX + line * BAR_W / PrepWork.LINE;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (phase == JUDGE) {
            reveal();
            return true;
        }
        if (!opening.settled() || sent || super.mouseClicked(x, y, button))
            return true;
        if (!overBar(x, y))
            return true;
        if (button == 0 && count < FTuning.CUT_MAX_MARKS) {
            aimed[count] = aim(x);
            landed[count] = PrepWork.land(aimed[count], figures);
            count++;
            stroke = STROKE;
            resort();
            float wet = figures.moisture();
            int squirt = Math.round(3 + wet * 12);
            specks.burst(at(landed[count - 1]), barY + 4, squirt, 1.4f + wet, 2.4f + wet * 2f, lerp(rgb, 0xBFE4FF, wet * 0.7f), 22, 2);
            // A wetter food squishes lower; a drier one snaps higher.
            GuiSkin.play(SoundEvents.AXE_STRIP, 1.5f - wet * 0.5f + count * 0.03f);
            if (wet > 0.5f)
                GuiSkin.play(SoundEvents.SLIME_BLOCK_STEP, 1.6f - wet * 0.6f);
        } else if (button == 1 && count > 0) {
            int near = 0, line = aim(x);
            for (int i = 1; i < count; i++)
                if (Math.abs(landed[i] - line) < Math.abs(landed[near] - line))
                    near = i;
            System.arraycopy(aimed, near + 1, aimed, near, count - near - 1);
            System.arraycopy(landed, near + 1, landed, near, count - near - 1);
            count--;
            resort();
            GuiSkin.play(SKIN.click(), 0.8f);
        }
        return true;
    }

    private static int lerp(int a, int b, float t) {
        return GuiSkin.lerp(0xFF000000 | a, 0xFF000000 | b, t) & 0xFFFFFF;
    }

    /** Keep the landed cuts in line order, and grade them the way the server will. */
    private void resort() {
        System.arraycopy(landed, 0, sorted, 0, count);
        java.util.Arrays.sort(sorted, 0, count);
        cut = PrepWork.cut(landed, count);
    }

    @Override
    public void tick() {
        ticks++;
        if (stroke > 0)
            stroke--;
        specks.tick(0.18f);
        if (phase == JUDGE && --judge <= 0)
            reveal();
        else if (phase == REVEAL && --flourish <= 0)
            opening.close();
    }

    /**
     * Done: the one packet goes now and the grade is known, but the player is not told yet. The music of that grade plays while
     * the board is covered; a whole food (no cuts) has nothing to judge.
     */
    private void finish() {
        if (sent || !opening.settled())
            return;
        sent = true;
        List<Integer> marks = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            marks.add(aimed[i]);
        PacketDistributor.sendToServer(new PrepareFinishPayload(containerId, 0, marks, 0));
        if (cut.level() == 0) {
            phase = REVEAL;
            flourish = FLOURISH;
            GuiSkin.play(SoundEvents.WOOD_PLACE, 1.1f);
            return;
        }
        phase = JUDGE;
        judge = PrepWork.JUDGE_TICKS[cut.grade()];
        bgm = SimpleSoundInstance.forUI(cut.grade() == FoodCut.EVEN ? FSounds.COOKING_BGM_GREAT.get()
                : cut.grade() == FoodCut.MIXED ? FSounds.COOKING_BGM_SUCCESS.get() : FSounds.COOKING_BGM_FAIL.get(), 1f);
        Minecraft.getInstance().getSoundManager().play(bgm);
    }

    /** The cloth comes off: the fanfare and the pieces' wobble. */
    private void reveal() {
        if (phase != JUDGE)
            return;
        phase = REVEAL;
        flourish = REVEAL_HOLD;
        stopMusic();
        GuiSkin.play(cut.grade() == FoodCut.RAGGED ? FSounds.COOKING_FANFARE_FAIL.get() : FSounds.COOKING_FANFARE_SUCCESS.get(), 1f);
        if (cut.grade() == FoodCut.EVEN) {
            GuiSkin.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
            specks.burst(px + PW / 2f, barY - 20, 14, 2.4f, 3f, 0xFFF0A0, 26, 2);
        } else if (cut.grade() == FoodCut.RAGGED) {
            GuiSkin.play(SoundEvents.WOOD_PLACE, 0.6f);
            GuiSkin.play(SoundEvents.SLIME_BLOCK_FALL, 0.7f);
        } else
            GuiSkin.play(SoundEvents.WOOD_PLACE, 1.1f);
    }

    private void stopMusic() {
        if (bgm != null)
            Minecraft.getInstance().getSoundManager().stop(bgm);
        bgm = null;
    }

    @Override
    public void onClose() {
        // Esc while judging skips to the result; before Done it discards, after the result it leaves.
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

    /** The pieces' left edge shift: they spread apart as the cuts go in, and wobble apart a little more at the flourish. */
    private float shift(int piece, int pieces) {
        float spread = 1.5f + 3f * Math.min(flourish, FLOURISH) / FLOURISH;
        return (piece - (pieces - 1) / 2f) * spread;
    }

    private float wobble(int piece) {
        return flourish > 0 ? Mth.sin((flourish - partial) * 0.9f + piece * 1.7f) * 3f * Math.min(flourish, FLOURISH) / FLOURISH : 0f;
    }

    private FoodCut wordFor;
    private Component word;
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
        if (cut != wordFor) {
            wordFor = cut;
            word = cut.level() == 0 ? Component.translatable("prep.feedback.cut.whole") : ToolTableMenu.cutName(cut);
        }
        if (phase != JUDGE)
            g.drawString(font, word, px + PW - 12 - font.width(word), py + 9,
                    cut.level() > 0 && cut.grade() == FoodCut.RAGGED ? SKIN.warn() : SKIN.accent(), false);
        SKIN.inset(g, barX - 8, py + 22, BAR_W + 16, BAR_H + 110);
        slab(g);
        cloth(g);
        int hy = py + PH - 54;
        for (var l : hint) {
            g.drawString(font, l, px + 12, hy, SKIN.dim(), false);
            hy += 9;
        }
        super.render(g, mx, my, partialTick);
        specks.draw(g, partialTick);
        if (!sent && opening.settled() && overBar(mx, my))
            knife(g);
    }

    /** The cloth over the board while it is judged, bulging with the music, then whipped off upward as the result is shown. */
    private void cloth(GuiGraphics g) {
        if (phase == PLAY)
            return;
        int x0 = barX - 8, y0 = py + 22, w = BAR_W + 16, h = BAR_H + 110;
        int lift = phase == REVEAL ? (REVEAL_HOLD - flourish) * 8 : 0;
        if (lift >= h)
            return;
        int bulge = phase == JUDGE ? Math.round(Mth.sin((ticks + partial) * 0.7f) * (1f + (1f - judge / (float) PrepWork.JUDGE_TICKS[cut.grade()]) * 2f)) : 0;
        int top = y0 - lift;
        g.enableScissor(x0, y0, x0 + w, y0 + h);
        g.fill(x0 + bulge, top, x0 + w + bulge, top + h, CLOTH);
        for (int x = 0; x < w; x += 16)
            g.fill(x0 + bulge + x, top, x0 + bulge + x + 8, top + h, CLOTH_CHECK);
        for (int y = 0; y < h; y += 16)
            g.fill(x0 + bulge, top + y, x0 + bulge + w, top + y + 8, CLOTH_CHECK);
        if (phase == JUDGE) {
            Component wait = Component.translatable("gui.feedback.prep_pour.judging");
            g.fill(x0 + w / 2 - font.width(wait) / 2 - 4 + bulge, top + h / 2 - 7, x0 + w / 2 + font.width(wait) / 2 + 4 + bulge, top + h / 2 + 7, CLOTH);
            g.drawString(font, wait, x0 + w / 2 - font.width(wait) / 2 + bulge, top + h / 2 - 4, SKIN.ink(), false);
        }
        g.disableScissor();
    }

    /** The big icon and the bar, each cut into the pieces the cuts make. */
    private void slab(GuiGraphics g) {
        int pieces = count + 1, iconX = px + (PW - ICON) / 2, iconY = py + 30;
        int body = 0xFF000000 | rgb, edge = 0xFF000000 | lerp(rgb, 0, 0.45f), shine = 0xFF000000 | lerp(rgb, 0xFFFFFF, 0.35f);
        if (figures.fibre() > 0.05f) {
            int grain = (int) (figures.fibre() * 0x50) << 24 | 0xFFFFFF;
            for (int l = FTuning.CUT_GRAIN; l < PrepWork.LINE; l += FTuning.CUT_GRAIN)
                g.fill(at(l), barY + 2, at(l) + 1, barY + BAR_H - 2, grain);
        }
        for (int i = 0; i < pieces; i++) {
            int a = i == 0 ? 0 : sorted[i - 1], b = i == count ? PrepWork.LINE : sorted[i];
            float dx = shift(i, pieces);
            int dy = Math.round(wobble(i));
            int x0 = Math.round(at(a) + dx), x1 = Math.round(at(b) + dx) - 1;
            if (x1 > x0) {
                g.fill(x0, barY + dy, x1, barY + BAR_H + dy, edge);
                g.fill(x0 + 1, barY + 1 + dy, x1 - 1, barY + BAR_H - 1 + dy, body);
                g.fill(x0 + 1, barY + 1 + dy, x1 - 1, barY + 3 + dy, shine);
            }
            int i0 = Math.round(iconX + a * ICON / (float) PrepWork.LINE + dx), i1 = Math.round(iconX + b * ICON / (float) PrepWork.LINE + dx);
            g.enableScissor(i0, iconY - 8, Math.max(i0, i1), iconY + ICON + 8);
            g.pose().pushPose();
            g.pose().translate(Math.round(dx), dy, 0);
            g.pose().translate(iconX, iconY, 0);
            g.pose().scale(ICON / 16f, ICON / 16f, 1f);
            g.renderItem(food, 0, 0);
            g.pose().popPose();
            g.disableScissor();
        }
        for (int i = 0; i < count; i++)
            g.fill(at(landed[i]), barY - 3, at(landed[i]) + 1, barY + BAR_H + 3, SKIN.accent());
    }

    /** The knife at the pointer: dropping on a stroke, with a ghost tick where this stroke would land. */
    private void knife(GuiGraphics g) {
        int x = Mth.clamp(mouseX, barX, barX + BAR_W);
        if (count < FTuning.CUT_MAX_MARKS)
            g.fill(at(PrepWork.land(aim(x), figures)), barY - 1, at(PrepWork.land(aim(x), figures)) + 1, barY + BAR_H + 1, SKIN.dim());
        float p = stroke > 0 ? Mth.sin(Mth.PI * (1f - (stroke - partial) / STROKE)) : 0f;
        int tip = barY - 26 + Math.round(p * 28f);
        g.fill(x - 1, tip - 22, x + 2, tip, SKIN.ink());
        g.fill(x - 1, tip - 22, x, tip, SKIN.accent());
        g.fill(x - 4, tip - 31, x + 5, tip - 22, SKIN.dim());
    }
}
