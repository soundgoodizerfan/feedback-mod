// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.desktop;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.control.program.ActuatorNode;
import io.github.soundgoodizerfan.feedback.control.program.ProgramNode;
import io.github.soundgoodizerfan.feedback.control.program.SensorNode;
import io.github.soundgoodizerfan.feedback.client.gui.GuiSkin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * The Link Map app: a plan view with the Computer at the middle and every block the running program is linked to around it, inputs as
 * squares, outputs as diamonds. A block in an unloaded chunk is drawn hollow (a data link never loads one); a card with no link is
 * listed in the warning colour. Hover a mark for its block, position and distance.
 */
final class LinkMapWindow extends DesktopWindow {

    private final ComputerContext ctx;

    LinkMapWindow(ComputerContext ctx) {
        super(40, 8, 230, 170, 160, 130, true);
        this.ctx = ctx;
    }

    @Override
    public Component title() {
        return Component.translatable("gui.feedback.computer.linkmap");
    }

    @Override
    public Glyph glyph() {
        return Glyph.MAP;
    }

    @Override
    protected void render(Ui ui, int mx, int my) {
        GuiSkin skin = ui.skin();
        var level = Minecraft.getInstance().level;
        BlockPos origin = ctx.menu().getPos();
        int w = ui.w(), h = ui.h() - Ui.LINE - 2;
        ui.inset(0, 0, w, h);
        int cx = w / 2, cy = h / 2;
        int grid = (skin.ink() & 0xFFFFFF) | 0x24000000;
        ui.fill(1, cy, w - 2, 1, grid);
        ui.fill(cx, 1, 1, h - 2, grid);

        int far = 8, unlinked = 0;
        for (ProgramNode node : ctx.running().nodes()) {
            DataNodeRef ref = ref(node);
            if (ref != null) {
                BlockPos p = ref.pos();
                far = Math.max(far, Math.max(Math.abs(p.getX() - origin.getX()), Math.abs(p.getZ() - origin.getZ())));
            } else if (node instanceof SensorNode || node instanceof ActuatorNode) {
                unlinked++;
            }
        }
        float scale = (Math.min(w, h) / 2f - 10) / far;
        ui.fill(cx - 2, cy - 2, 5, 5, skin.accent());
        String hover = null;
        for (ProgramNode node : ctx.running().nodes()) {
            DataNodeRef ref = ref(node);
            if (ref == null)
                continue;
            BlockPos p = ref.pos();
            int x = cx + Math.round((p.getX() - origin.getX()) * scale), y = cy + Math.round((p.getZ() - origin.getZ()) * scale);
            boolean loaded = level != null && level.isLoaded(p);
            int colour = loaded ? skin.ink() : skin.dim();
            if (node instanceof SensorNode) {
                if (loaded)
                    ui.fill(x - 2, y - 2, 5, 5, colour);
                else {
                    ui.fill(x - 2, y - 2, 5, 1, colour);
                    ui.fill(x - 2, y + 2, 5, 1, colour);
                    ui.fill(x - 2, y - 2, 1, 5, colour);
                    ui.fill(x + 2, y - 2, 1, 5, colour);
                }
            } else {
                ui.fill(x, y - 3, 1, 7, colour);
                ui.fill(x - 3, y, 7, 1, colour);
                ui.fill(x - 1, y - 1, 3, 3, colour);
            }
            if (Math.abs(mx - x) <= 4 && Math.abs(my - y) <= 4) {
                String name = level == null ? "?" : level.getBlockState(p).getBlock().getName().getString();
                hover = name + " " + p.getX() + "," + p.getY() + "," + p.getZ() + " (" + (int) Math.sqrt(p.distSqr(origin)) + ")";
            }
        }
        ui.text(4, 3, Component.translatable("gui.feedback.computer.linkmap_key"), skin.dim());
        String line = hover != null ? hover : unlinked > 0 ? Component.translatable("gui.feedback.computer.linkmap_unlinked", unlinked).getString() : "";
        ui.clipped(0, h + 2, w, line, hover != null ? skin.ink() : skin.warn());
    }

    private static DataNodeRef ref(ProgramNode node) {
        return node instanceof SensorNode s ? s.target() : node instanceof ActuatorNode a ? a.target() : null;
    }
}
