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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineDesign;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandMenu;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineStandPayload;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

/**
 * The Engine Stand's editor ({@code feedback_vehicles_spec.md} section 6), drawn as a blueprint:
 * the engine in cross-section, a cylinder per cylinder, each as wide as its bore, its crank thrown
 * as far as its stroke, turning slowly on the sheet. Three settings are stepped; the material is
 * set by a sample in its slot; what the design makes reads live off the material system. Every
 * edit goes to the stand; Print, in the title block, writes it onto the paper.
 */
public class EngineStandScreen extends SkinnedScreen<EngineStandMenu> {

    private static final int W = 320, H = 238;
    private static final int BP_X = 8, BP_Y = 19, BP_W = 304, BP_H = 94;
    private static final int ROW_Y = 134, ROW_H = 15, MINUS_X = 78, PLUS_X = 154, COL_X = 176;
    private static final int FIG_X = 182, FIG_Y = 132, FIG_W = 130, BLOCK_Y = 200, SHEET_X1 = 212;
    private static final int PAPER = 0xFF1B4C86, RULE = 0x60E8F1FF, STAMP = 0xE85A4F;
    /** The crank's turn on paper: slow, a drawing and not a running engine. */
    private static final float TURN_MS = 4000;

    private EngineDesign design;
    /** The three settings, then the bore and stroke dimensions on the drawing. */
    private final GuiStyle.Flash flash = new GuiStyle.Flash(5);
    private long stampedAt = -10_000;

    public EngineStandScreen(EngineStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.BLUEPRINT);
        this.design = menu.opened();
        imageWidth = W;
        imageHeight = H;
    }

    @Override
    protected void init() {
        super.init();
        row(0, d -> new EngineDesign(d.cylinders() - 1, d.boreMm(), d.strokeMm(), d.material()),
                d -> new EngineDesign(d.cylinders() + 1, d.boreMm(), d.strokeMm(), d.material()));
        row(1, d -> new EngineDesign(d.cylinders(), d.boreMm() - EngineDesign.STEP_MM, d.strokeMm(), d.material()),
                d -> new EngineDesign(d.cylinders(), d.boreMm() + EngineDesign.STEP_MM, d.strokeMm(), d.material()));
        row(2, d -> new EngineDesign(d.cylinders(), d.boreMm(), d.strokeMm() - EngineDesign.STEP_MM, d.material()),
                d -> new EngineDesign(d.cylinders(), d.boreMm(), d.strokeMm() + EngineDesign.STEP_MM, d.material()));
        addRenderableWidget(GuiStyle.primary(skin, upper("gui.feedback.engine_stand.print"), b -> print(),
                leftPos + SHEET_X1 + 6, topPos + BLOCK_Y + 9, W - SHEET_X1 - 18, 20, FSounds.UI_STAMP::get));
    }

    private void row(int index, UnaryOperator<EngineDesign> down, UnaryOperator<EngineDesign> up) {
        int y = topPos + ROW_Y + index * ROW_H;
        addRenderableWidget(GuiStyle.button(skin, Component.literal("<"), b -> edit(down), leftPos + MINUS_X, y, 14, 12,
                FSounds.UI_DETENT::get, 0.9f));
        addRenderableWidget(GuiStyle.button(skin, Component.literal(">"), b -> edit(up), leftPos + PLUS_X, y, 14, 12,
                FSounds.UI_DETENT::get, 1.1f));
    }

    /**
     * The sample slot sets the material: the server did it when the sample went in, and the screen
     * follows here so the readout changes at once.
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        EngineDesign.sampleMaterial(ClientMaterials.on(minecraft.level), menu.getSlot(1).getItem())
                .filter(m -> !m.equals(design.material()))
                .ifPresent(m -> design = design.withMaterial(m));
    }

    private void edit(UnaryOperator<EngineDesign> change) {
        design = change.apply(design);
        send(false);
    }

    private void print() {
        send(true);
        if (!menu.getSlot(0).getItem().isEmpty() && material() != null)
            stampedAt = Util.getMillis();
    }

    private void send(boolean print) {
        PacketDistributor.sendToServer(new EngineStandPayload(menu.getPos(), design, print));
    }

    private Material material() {
        return ClientMaterials.on(minecraft.level).get(design.material());
    }

    private static Component upper(String key) {
        return Component.literal(Component.translatable(key).getString().toUpperCase(Locale.ROOT));
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int x = leftPos, y = topPos;
        g.fill(x + 6, y + 116, x + W - 6, y + 117, RULE);
        g.fill(x + COL_X, y + 117, x + COL_X + 1, y + H - 6, RULE);
        g.fill(x + COL_X + 1, y + BLOCK_Y, x + W - 6, y + BLOCK_Y + 1, RULE);
        g.fill(x + SHEET_X1, y + BLOCK_Y + 1, x + SHEET_X1 + 1, y + H - 6, RULE);
        skin.slot(g, x + EngineStandMenu.PAPER_X, y + EngineStandMenu.PAPER_Y);
        skin.slot(g, x + EngineStandMenu.SAMPLE_X, y + EngineStandMenu.SAMPLE_Y);
        for (int c = 0; c < 9; c++)
            skin.slot(g, x + EngineStandMenu.HOTBAR_X + c * 18, y + EngineStandMenu.HOTBAR_Y);
        GuiStyle.drawingBoard(g, x + BP_X, y + BP_Y, BP_W, BP_H, mouseX, mouseY, settled());
        engine(g);
    }

    /** The engine in section: block, heads, pistons, rods, and the crank they turn. */
    private void engine(GuiGraphics g) {
        int ink = skin.ink(), dim = skin.dim();
        int n = design.cylinders();
        int b = Math.round(design.boreMm() / 120f * 24), r = Math.max(2, Math.round(design.strokeMm() / 120f * 13));
        int rod = 2 * r + 6, pitch = b + 6, span = n * pitch - 6;
        int x0 = leftPos + BP_X + (BP_W - span) / 2, cy = topPos + BP_Y + 72;
        int boreTop = cy - r - rod - 6, caseTop = cy - r - 4, caseBottom = cy + r + 4;
        double turn = Util.getMillis() / TURN_MS * 2 * Math.PI;

        // Crankcase and the crankshaft through it.
        g.renderOutline(x0 - 6, caseTop, span + 12, caseBottom - caseTop, ink);
        g.hLine(x0 - 10, x0 + span + 9, cy, dim);
        for (int i = 0; i < n; i++) {
            int x = x0 + i * pitch, cx = x + b / 2;
            g.fill(x - 2, boreTop - 6, x + b + 2, boreTop, 0x30FFFFFF); // the head
            g.renderOutline(x - 2, boreTop - 6, b + 4, 6, ink);
            g.vLine(x - 1, boreTop, caseTop, ink); // the bore's walls
            g.vLine(x + b, boreTop, caseTop, ink);
            // Firing spread evenly over the four-stroke cycle's two turns.
            double a = turn + i * 4 * Math.PI / n;
            int px = cx + (int) Math.round(r * Math.sin(a)), py = cy - (int) Math.round(r * Math.cos(a));
            int pin = py - (int) Math.round(Math.sqrt(rod * rod - Math.pow(r * Math.sin(a), 2)));
            GuiStyle.line(g, cx, pin, px, py, ink); // the rod
            GuiStyle.line(g, cx, cy, px, py, dim); // the throw
            g.fill(px - 1, py - 1, px + 2, py + 2, ink);
            g.fill(x, pin - 4, x + b, pin + 2, PAPER); // the piston, over the rod's top
            g.renderOutline(x, pin - 4, b, 6, ink);
            g.hLine(x + 1, x + b - 2, pin - 2, dim);
        }

        // Dimensions: the bore across the first cylinder, the stroke beside the last.
        String bore = String.valueOf(design.boreMm()), stroke = String.valueOf(design.strokeMm());
        int bc = flash.colour(3, bore, dim, skin.accent()), dy = boreTop - 10;
        g.hLine(x0, x0 + b - 1, dy, bc);
        g.vLine(x0, dy - 2, dy + 2, bc);
        g.vLine(x0 + b - 1, dy - 2, dy + 2, bc);
        g.drawString(font, bore, x0 - 3 - font.width(bore), dy - 4, bc, false);
        int sc = flash.colour(4, stroke, dim, skin.accent());
        int sx = x0 + span + 12, tdc = cy - r - rod, bdc = cy + r - rod;
        g.vLine(sx, tdc, bdc, sc);
        g.hLine(sx - 2, sx + 2, tdc, sc);
        g.hLine(sx - 2, sx + 2, bdc, sc);
        g.drawString(font, stroke, sx + 5, (tdc + bdc) / 2 - 4, sc, false);

        int oy = topPos + BP_Y;
        g.drawString(font, upper("gui.feedback.engine_stand.not_to_scale"), leftPos + BP_X + 9, oy + BP_H - 11, dim, false);
        Component section = upper("gui.feedback.engine_stand.section");
        g.drawString(font, section, leftPos + BP_X + BP_W - 5 - font.width(section), oy + BP_H - 11, dim, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, Component.literal(title.getString().toUpperCase(Locale.ROOT)), 12, 7, skin.ink(), false);

        g.drawString(font, upper("gui.feedback.engine_stand.spec"), 12, 120, skin.dim(), false);
        String[] names = {"cylinders", "bore", "stroke"};
        String[] values = {String.valueOf(design.cylinders()), design.boreMm() + " mm", design.strokeMm() + " mm"};
        for (int i = 0; i < 3; i++) {
            int y = ROW_Y + i * ROW_H + 2;
            g.drawString(font, Component.translatable("gui.feedback.engine_stand." + names[i]), 12, y, skin.dim(), false);
            g.drawCenteredString(font, values[i], (MINUS_X + 14 + PLUS_X) / 2, y,
                    flash.colour(i, values[i], skin.ink(), skin.accent()));
        }
        int y = EngineStandMenu.SAMPLE_Y + 4;
        g.drawString(font, Component.translatable("gui.feedback.engine_stand.material"), 12, y, skin.dim(), false);
        g.drawString(font, Component.translatable("material." + design.material().getNamespace() + "." + design.material().getPath()),
                EngineStandMenu.SAMPLE_X + 22, y, skin.ink(), false);

        g.drawString(font, upper("gui.feedback.engine_stand.figures"), FIG_X, 120, skin.dim(), false);
        Material m = material();
        List<Component> lines = design.describe(m);
        y = FIG_Y;
        for (int i = 1; i < lines.size(); i++) // the first line is the settings, already shown
            for (var l : font.split(lines.get(i), FIG_W)) {
                if (y + 9 > BLOCK_Y)
                    break;
                g.drawString(font, l, FIG_X, y, m == null && i == lines.size() - 1 ? skin.warn() : skin.ink(), false);
                y += 9;
            }

        g.drawString(font, upper("gui.feedback.engine_stand.sheet"), COL_X + 5, BLOCK_Y + 3, skin.dim(), false);
        GuiStyle.stamp(g, upper("gui.feedback.engine_stand.printed"), (SHEET_X1 + W - 12) / 2f, BLOCK_Y + 19,
                Util.getMillis() - stampedAt, STAMP);
    }
}
