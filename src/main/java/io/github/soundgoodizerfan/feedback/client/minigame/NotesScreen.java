// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.minigame;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.type.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.item.ModularRework;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;
import io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
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
 * The Treatise tab's learning game: the specimen's parts lie on a blotter one after another and a pair of calipers measures each. Hold the
 * mouse to open the moving jaw and let it go (click) when the jaws just touch the part's edges: too tight crushes the reading, too loose is
 * a guess. Each reading is inked onto the notebook beside it, a few strokes at a time, with its figure; the whole specimen is sketched in
 * pencil at the foot of the page as the readings come. One pull is one part (the specimen's distinct parts, at most
 * {@link ModularTuning#NOTES_MAX_PARTS}); the scores averaged are the ONE result sent ({@link MinigameScreens#report}), which is the quality of
 * the notes the treatise keeps. The stamp and stars that follow never delay it. Esc before the last reading sends nothing and the specimen
 * stays. Reduce motion stills the idle sway and the ink's shiver; sounds stay.
 */
final class NotesScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.ENAMEL;
    private static final int PW = 236, PH = 214, READY = 0, OPEN = 1, INK = 2, DONE = 3, INK_TICKS = 22, DONE_TICKS = 90;
    private static final int PAGE = 0xFFEDE3C4, RULE = 0xFFB9C7D6, INK_COL = 0xFF1F2A5C, PENCIL = 0xFF6C6C70, BENCH = 0xFF4A4036,
            BRASS = 0xFFC9A44C, GOOD = 0xFF4FBF5A, MID = 0xFFE0B040, BAD = 0xFFC2452F;

    private final Opening opening = new Opening(SKIN);
    private final Screen parent;
    private final int containerId, rounds;
    private final List<ItemStack> parts = new ArrayList<>();
    private final float[] scores, spans, readings;
    private final Specks specks = new Specks(64);
    private final Component title = Component.translatable("gui.feedback.notes.title");
    private final Component[] results = new Component[5];
    private List<FormattedCharSequence> hint = List.of();
    private int px, py, ticks, phase = READY, pull, ink, hold, mouseX, mouseY;
    private float jaw, partial, finalScore;
    private boolean sent;
    private long doneAt;

    NotesScreen(Screen parent, EngineeringTableMenu menu, MinigameOpenPayload session) {
        super(Component.translatable("gui.feedback.notes.title"));
        this.parent = parent;
        this.containerId = session.container();
        ItemStack specimen = menu.getSlot(EngineeringTableMenu.slot(EngineeringTableBlockEntity.SPECIMEN)).getItem();
        var level = Minecraft.getInstance().level;
        if (level != null && ModularStacks.design(specimen) != null)
            parts.addAll(ModularRework.dismantle(specimen, ClientMaterials.on(level), io.github.soundgoodizerfan.feedback.modular.type.ModularTypes.on(level), 0f));
        this.rounds = Math.max(1, Math.min(Math.min(ModularTuning.NOTES_MAX_PARTS, Math.max(1, parts.size())), session.rounds()));
        this.scores = new float[rounds];
        this.spans = new float[rounds];
        this.readings = new float[rounds];
        for (int i = 0; i < 5; i++)
            results[i] = Component.translatable("gui.feedback.notes.result." + i);
        int seed = containerId * 31 + (parts.isEmpty() ? 7 : parts.get(0).getDescriptionId().hashCode());
        for (int i = 0; i < rounds; i++)
            spans[i] = 44f + Math.floorMod(seed * (i + 3) * 2654435761L, 71L);
    }

    @Override
    protected void init() {
        opening.start();
        px = (width - PW) / 2;
        py = Math.max(0, (height - PH) / 2);
        hint = font.split(Component.translatable("gui.feedback.notes.hint"), PW - 24);
        jaw = 24f;
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
        if (!opening.settled() || button != 0)
            return true;
        if (phase == READY) {
            phase = OPEN;
            GuiSkin.play(SoundEvents.CHAIN_STEP, 1.6f, 0.5f);
        } else if (phase == OPEN)
            read();
        return true;
    }

    /** The jaws close where the mouse let them: grade the reading against the part, ink it, and send the one result after the last. */
    private void read() {
        float span = spans[pull];
        float err = Math.abs(jaw - span) / span;
        float score = jaw < span - 1f ? Math.max(0f, 0.55f - err * 2.5f) : Math.max(0f, 1f - err * 6f);
        scores[pull] = score;
        readings[pull] = jaw / 7f;
        phase = INK;
        ink = INK_TICKS;
        float cx = px + 28 + span / 2f;
        if (score >= 0.8f) {
            GuiSkin.play(SoundEvents.LEVER_CLICK, 1.5f, 0.8f);
            GuiSkin.play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f, 0.6f);
            specks.burst(cx, py + 92, 10, 1.6f, 2.0f, 0xFFF0C040, 22, 2);
        } else if (score >= 0.4f) {
            GuiSkin.play(SoundEvents.LEVER_CLICK, 1.1f, 0.7f);
        } else {
            GuiSkin.play(SoundEvents.ANVIL_PLACE, 1.6f, 0.2f);
            specks.burst(cx, py + 92, 4, 1.0f, 1.0f, BAD, 14, 1);
        }
        if (pull + 1 >= rounds) {
            float sum = 0f;
            for (float s : scores)
                sum += s;
            finalScore = sum / rounds;
            if (!sent) {
                sent = true;
                MinigameScreens.report(containerId, finalScore);
            }
        }
    }

    /** The final grade 0..4 (guessed, rough, fair, clean, exact). */
    private int star() {
        return finalScore < 0.3f ? 0 : finalScore < 0.55f ? 1 : finalScore < 0.78f ? 2 : finalScore < 0.92f ? 3 : 4;
    }

    @Override
    public void tick() {
        ticks++;
        if (phase == OPEN) {
            // The jaw follows the mouse along the beam, with a little lag so it feels like a thing with weight.
            float target = Mth.clamp(mouseX - (px + 28), 14f, 128f);
            jaw += (target - jaw) * 0.45f;
            if (ticks % 3 == 0 && Math.abs(target - jaw) > 1f)
                GuiSkin.play(SoundEvents.CHAIN_STEP, 1.4f + jaw / 100f, 0.12f);
        } else if (phase == INK) {
            if (ink > 0 && ink % 3 == 0)
                GuiSkin.play(SoundEvents.UI_BUTTON_CLICK.value(), 1.9f + (INK_TICKS - ink) * 0.02f, 0.18f);
            if (--ink <= 0) {
                if (pull + 1 >= rounds) {
                    phase = DONE;
                    hold = DONE_TICKS;
                    doneAt = Util.getMillis();
                    int star = star();
                    if (star >= 3) {
                        // The forge's own fanfare and gold bursts for clean notes, its failure sting for a guess.
                        GuiSkin.play(FSounds.FORGE_FANFARE.get(), 1f);
                        for (int wave = 0; wave < 3; wave++)
                            specks.burst(px + PW / 2f, py + 60, 24, 2.2f + wave * 1.2f, 3.4f, 0xFFD84A, 34, 2);
                    } else if (star <= 1)
                        GuiSkin.play(FSounds.FORGE_FAILURE.get(), 1f);
                    else
                        GuiSkin.play(SoundEvents.ANVIL_LAND, 0.8f, 0.7f);
                } else {
                    pull++;
                    jaw = 24f;
                    phase = OPEN;
                }
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
        mouseX = mx;
        mouseY = my;
        partial = partialTick;
        frameG = g;
        opening.render(g, width, height, px, py, PW, PH, 0f, paintFrame);
    }

    private void paint(GuiGraphics g) {
        SKIN.panel(g, px, py, PW, PH);
        g.drawString(font, title, px + 12, py + 9, SKIN.ink(), false);
        // The blotter: the part on it, its jaws, and the beam with its scale.
        int bx = px + 12, by = py + 24, bw = 128 + 32, bh = 110;
        g.fill(bx, by, bx + bw, by + bh, BENCH);
        int x0 = px + 28, partY = py + 66;
        float span = spans[Math.min(pull, rounds - 1)];
        ItemStack stack = parts.isEmpty() ? ItemStack.EMPTY : parts.get(Math.min(pull, parts.size() - 1));
        int rgb = 0xFF000000 | (stack.isEmpty() ? 0xB8BEC6 : colour(stack));
        g.fill(x0, partY, Math.round(x0 + span), partY + 30, rgb);
        g.fill(x0, partY, Math.round(x0 + span), partY + 2, 0x55FFFFFF);
        if (!stack.isEmpty()) {
            g.pose().pushPose();
            g.pose().translate(x0 + span / 2f - 12, partY + 3, 0);
            g.pose().scale(1.5f, 1.5f, 1f);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
        }
        // The calipers: a beam with a fixed jaw at the part's left edge and the moving one where the player has taken it.
        int beamY = partY + 44;
        g.fill(x0 - 3, beamY, x0 + 140, beamY + 4, BRASS);
        for (int t = 0; t <= 128; t += 7)
            g.fill(x0 + t, beamY + 4, x0 + t + 1, beamY + (t % 35 == 0 ? 10 : 7), 0xFF2A2D32);
        g.fill(x0 - 3, partY - 6, x0, beamY + 4, BRASS);
        float j = phase == READY ? 24f : jaw;
        int jx = Math.round(x0 + j);
        int sway = ClientSettings.reduceMotion() || phase != READY ? 0 : Math.round(Mth.sin((ticks + partial) * 0.1f) * 2f);
        g.fill(jx + sway, partY - 6, jx + 3 + sway, beamY + 4, BRASS);
        g.fill(jx + sway, partY - 6, jx + 1 + sway, beamY + 4, 0x66FFFFFF);
        // The reading beside the beam while the jaws are open.
        if (phase == OPEN || phase == INK)
            g.drawString(font, String.format("%.1f cm", (phase == INK ? readings[pull] : jaw / 7f)), px + 14, by + bh + 4, SKIN.ink(), false);
        specks.draw(g, partial);
        // The notebook: ruled lines, the readings in ink, and the pencil sketch below.
        int nx = px + 12 + bw + 8, nw = PW - (nx - px) - 8, ny = by;
        g.fill(nx, ny, nx + nw, ny + bh + 30, PAGE);
        for (int i = 0; i < 9; i++)
            g.fill(nx + 3, ny + 14 + i * 12, nx + nw - 3, ny + 15 + i * 12, RULE);
        for (int i = 0; i < rounds; i++) {
            boolean did = i < pull || (i == pull && phase >= INK);
            if (!did)
                continue;
            int col = scores[i] >= 0.8f ? INK_COL : scores[i] >= 0.4f ? 0xFF4A3A6A : 0xFF7A2A20;
            float write = i == pull && phase == INK ? 1f - ink / (float) INK_TICKS : 1f;
            String line = String.format("%d. %.1f", i + 1, readings[i]);
            int n = Math.max(1, Math.round(line.length() * write));
            int shiver = ClientSettings.reduceMotion() || write >= 1f ? 0 : (ticks % 2);
            g.drawString(font, line.substring(0, n), nx + 6, ny + 4 + i * 12 + shiver, col, false);
        }
        // The pencil sketch of the whole: the parts in a row, faint, one more for each part read.
        int sketch = Math.min(parts.size(), pull + (phase >= INK ? 1 : 0));
        for (int i = 0; i < sketch && i < 6; i++) {
            ItemStack p = parts.get(i);
            g.pose().pushPose();
            g.pose().translate(nx + 4 + (i % 3) * 22, ny + bh - 4 + (i / 3) * 16, 0);
            g.pose().scale(0.85f, 0.85f, 1f);
            g.renderItem(p, 0, 0);
            g.pose().popPose();
        }
        g.fill(nx + 3, ny + bh + 28, nx + nw - 3, ny + bh + 29, PENCIL);
        // One mark per part, filled with how its reading went.
        for (int i = 0; i < rounds; i++) {
            int x = px + PW - 12 - (rounds - i) * 11 + 3;
            boolean did = i < pull || (i == pull && phase >= INK);
            int col = !did ? 0xFF2A2D32 : scores[i] >= 0.8f ? GOOD : scores[i] >= 0.4f ? MID : BAD;
            g.fill(x, py + 9, x + 8, py + 17, col);
        }
        int hy = py + by - py + bh + 18;
        if (phase == DONE) {
            Component result = results[star()];
            GuiStyle.stamp(g, result, px + PW / 2f - 20, py + 150, Util.getMillis() - doneAt, star() >= 3 ? GOOD : star() >= 2 ? MID : BAD);
            stars(g, px + PW / 2 - 25, py + 196);
        } else {
            int y = hy + 4;
            for (FormattedCharSequence line : hint) {
                g.drawString(font, line, px + 12, y, SKIN.ink(), false);
                y += 10;
            }
        }
    }

    /** The colour of a part: its material's. */
    private static int colour(ItemStack stack) {
        var made = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get());
        return made != null ? MaterialColors.of(made.material()) : 0xB8BEC6;
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
