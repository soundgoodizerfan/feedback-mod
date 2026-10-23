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

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.PoleEffects;
import io.github.soundgoodizerfan.feedback.modular.PoleGame;
import io.github.soundgoodizerfan.feedback.modular.PoleMenu;
import io.github.soundgoodizerfan.feedback.net.PoleBeginPayload;
import io.github.soundgoodizerfan.feedback.net.PoleFinishPayload;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Enchanting Table ({@code docs/feedback_enchanting_spec.md} 7): the part on the left, and on the right the
 * gauge of the poling game -- a track for the polarization with the target band shaded, a marker for it and a
 * second for the field held, and a bar for the part's heat with its poling window on it. The field is the mouse:
 * hold the left button over the gauge and the field is how far the pointer sits above (positive) or below the
 * track's middle; let go and it is zero. (Keys were the other option; one is enough.)
 *
 * <p>The game ends when the part locks or on Commit; the field samples go to the server, which replays them.
 * The polarization drawn is the same model run here ({@link PoleGame#run}), so what is shown is what is judged.
 * shortcut: the server reads the part's heat a moment after this side does (the packet's trip), so a start
 * temperature can differ by a degree or two; the replay is the server's, this is only the picture.
 */
public class PoleScreen extends SkinnedScreen<PoleMenu> {

    private static final int GAUGE_X = 132, TRACK_W = 14, TRACK_Y = 24, TRACK_H = 72, HEAT_X = 176, HEAT_W = 10;

    private final List<PoleEffects.Effect> effects = new ArrayList<>(PoleEffects.known().values());
    private int chosen;
    private Button effectButton, begin, commit;

    private boolean playing;
    private float startTemperature;
    private final List<Integer> samples = new ArrayList<>();
    private boolean dragging;
    private double mouseY;
    private float polarization;
    private boolean reached;
    private String result = "";

    public PoleScreen(PoleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.MYSTIC);
        imageWidth = PoleMenu.W;
        imageHeight = PoleMenu.H;
        titleLabelX = 12;
        titleLabelY = 10;
        inventoryLabelY = 10000; // the button column sits where the label would be
    }

    @Override
    protected void init() {
        super.init();
        effectButton = addRenderableWidget(GuiStyle.button(skin, effectLabel(), b -> {
            if (!effects.isEmpty() && !playing)
                chosen = (chosen + 1) % effects.size();
            b.setMessage(effectLabel());
        }, leftPos + 10, topPos + 52, 104, 16));
        begin = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.pole.begin"), b -> begin(),
                leftPos + 10, topPos + 72, 104, 14, FSounds.UI_STAMP::get));
        commit = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.pole.commit"), b -> finish(),
                leftPos + 10, topPos + 88, 104, 14));
    }

    private PoleEffects.Effect effect() {
        return effects.isEmpty() ? null : effects.get(chosen);
    }

    private Component effectLabel() {
        PoleEffects.Effect e = effect();
        return e == null ? Component.empty() : Component.translatable("pole_effect.feedback." + e.id().getPath());
    }

    private Material.Poling poling() {
        Material m = menu.material();
        return m == null ? null : m.poling();
    }

    /** The part's heat now, or at this tick of the game being played. */
    private float heat() {
        return playing ? PoleGame.heat(startTemperature, samples.size()) : ItemHeat.get(menu.part(), minecraft.level).value();
    }

    private String why() {
        Material.Poling p = poling();
        if (effect() == null)
            return "none";
        if (p == null)
            return "put";
        if (menu.poled())
            return "poled";
        if (!PoleGame.hot(p, heat()))
            return "cold";
        return "ready";
    }

    private void begin() {
        PoleEffects.Effect e = effect();
        if (e == null || playing || !why().equals("ready"))
            return;
        startTemperature = heat();
        samples.clear();
        polarization = 0;
        reached = false;
        result = "";
        playing = true;
        PacketDistributor.sendToServer(new PoleBeginPayload(menu.containerId, e.id()));
    }

    private void finish() {
        PoleEffects.Effect e = effect();
        Material m = menu.material();
        if (!playing || e == null || m == null)
            return;
        playing = false;
        dragging = false;
        PoleGame.Verdict v = PoleGame.judge(e, PoleGame.level(m), polarization, reached);
        result = v.none() ? "screen.feedback.pole.result.none" : v.flaw() > 0 ? "screen.feedback.pole.result.flawed"
                : "screen.feedback.pole.result.clean";
        resultLevel = v.level();
        PacketDistributor.sendToServer(new PoleFinishPayload(menu.containerId, e.id(), List.copyOf(samples)));
    }

    private int resultLevel;

    @Override
    protected void containerTick() {
        super.containerTick();
        begin.active = !playing && why().equals("ready");
        commit.visible = playing;
        effectButton.active = !playing && !effects.isEmpty();
        if (!playing) {
            if (menu.part().isEmpty())
                result = "";
            return;
        }
        Material.Poling p = poling();
        if (p == null || menu.part().isEmpty()) { // the part was taken out: the game is off
            playing = false;
            return;
        }
        double mid = topPos + TRACK_Y + TRACK_H / 2.0;
        int field = dragging ? (int) Math.round(Mth.clamp((mid - mouseY) / (TRACK_H / 2.0), -1, 1) * 100) : 0;
        samples.add(field);
        reached = false;
        boolean[] r = new boolean[1];
        polarization = PoleGame.run(p, startTemperature, samples, r);
        reached = r[0];
        if (PoleGame.heat(startTemperature, samples.size()) <= p.lock().value() || samples.size() >= io.github.soundgoodizerfan.feedback.modular.ModularTuning.POLE_MAX_TICKS)
            finish();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (playing && settled() && button == 0 && mx >= leftPos + GAUGE_X - 14 && mx <= leftPos + HEAT_X
                && my >= topPos + TRACK_Y - 8 && my <= topPos + TRACK_Y + TRACK_H + 8) {
            dragging = true;
            mouseY = my;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            mouseY = my;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private static int trackY(float value) {
        return TRACK_Y + Math.round((1f - Mth.clamp(value, -1f, 1f)) / 2f * TRACK_H);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + PoleMenu.PART_X, topPos + PoleMenu.PART_Y);
        inventory(g, PoleMenu.INV_X, PoleMenu.INV_Y);
        PoleEffects.Effect e = effect();
        Material.Poling p = poling();
        int tx = leftPos + GAUGE_X, ty = topPos;
        // The polarization track, the target band shaded, zero marked.
        skin.inset(g, tx - 1, ty + TRACK_Y - 1, TRACK_W + 2, TRACK_H + 2);
        g.fill(tx, ty + TRACK_Y + TRACK_H / 2, tx + TRACK_W, ty + TRACK_Y + TRACK_H / 2 + 1, 0x40FFFFFF);
        if (e != null)
            g.fill(tx, ty + trackY(e.target() + e.band()), tx + TRACK_W, ty + trackY(e.target() - e.band()), 0x60F2CC5A);
        g.fill(tx - 3, ty + trackY(polarization) - 1, tx + TRACK_W + 3, ty + trackY(polarization) + 1, skin.ink());
        if (playing && dragging) {
            int fy = ty + trackY(PoleGame.field(samples.isEmpty() ? 0 : samples.get(samples.size() - 1)));
            g.fill(tx - 9, fy - 1, tx - 4, fy + 2, skin.accent());
        }
        // The heat bar: the poling window on it, the part's heat marked.
        int hx = leftPos + HEAT_X;
        skin.inset(g, hx - 1, ty + TRACK_Y - 1, HEAT_W + 2, TRACK_H + 2);
        if (p != null) {
            float lo = p.lock().value() - 20f, hi = Math.max(playing ? startTemperature : heat(), p.temperature().value()) + 20f;
            int wTop = ty + TRACK_Y + Math.round((1f - (p.temperature().value() - lo) / (hi - lo)) * TRACK_H);
            int wBottom = ty + TRACK_Y + Math.round((1f - (p.lock().value() - lo) / (hi - lo)) * TRACK_H);
            g.fill(hx, wTop, hx + HEAT_W, wBottom, 0x60F2CC5A);
            int hy = ty + TRACK_Y + Math.round((1f - Mth.clamp((heat() - lo) / (hi - lo), 0f, 1f)) * TRACK_H);
            g.fill(hx - 2, hy - 1, hx + HEAT_W + 2, hy + 1, skin.ink());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        g.drawString(font, Component.translatable("gui.feedback.pole.field"), GAUGE_X - 14, TRACK_Y - 11, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.pole.heat"), HEAT_X - 4, TRACK_Y - 11, skin.dim(), false);
        // Beside the part slot: what the table needs, or what the last session came to.
        if (!result.isEmpty())
            g.drawWordWrap(font, Component.translatable(result, resultLevel), 38, 26, 76, skin.accent());
        else
            g.drawWordWrap(font, Component.translatable("gui.feedback.pole.state." + why(),
                    Readout.figure(Quantity.TEMPERATURE, heat())), 38, 26, 76, why().equals("ready") ? skin.ink() : skin.warn());
    }
}
