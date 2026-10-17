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

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.PivotItem;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderMode;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The corner card for the Welder and the Pivot Setter, where the gun's ammo count sits: what each
 * click does in the mode held, and what the point under the crosshair is -- welded or not, which
 * bearing, carrying rotation or free, which tire. Playtesting found the tools' keys forgotten
 * between sessions and every one of those settings invisible (2026-10-15, 2026-10-16).
 */
public final class BuildToolHud {

    private static final int TITLE = 0xFFFFFFAA, LINE = 0xFFE0E0E0, POINT = 0xFF9FD8FF;

    private BuildToolHud() {
    }

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Feedback.id("build_tool"), BuildToolHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui)
            return;
        ItemStack held = mc.player.getMainHandItem();
        String key;
        int count;
        if (held.getItem() instanceof WelderItem) {
            WelderMode mode = held.getOrDefault(FVehicles.WELDER_MODE.get(), WelderMode.POINT);
            key = "hud.feedback.welder." + mode.name().toLowerCase(java.util.Locale.ROOT);
            count = switch (mode) {
                case POINT -> 4;
                case CONNECT -> 4;
                case PLATE -> 3;
                case WELD -> 2;
                case REMOVE -> 4;
            };
        } else if (held.getItem() instanceof PivotItem) {
            key = "hud.feedback.pivot";
            count = 4;
        } else
            return;

        List<Component> lines = new ArrayList<>();
        for (int i = 1; i <= count; i++)
            lines.add(Component.translatable(key + "." + i));
        if (held.getItem() instanceof WelderItem)
            lines.add(Component.translatable("hud.feedback.welder.common"));

        Font font = mc.font;
        int right = g.guiWidth() - 6, y = g.guiHeight() - 60 - 10 * (lines.size() + 3);
        Component title = Component.translatable(key);
        g.drawString(font, title, right - font.width(title), y, TITLE, true);
        y += 11;
        for (Component line : lines) {
            g.drawString(font, line, right - font.width(line), y, LINE, true);
            y += 10;
        }
        WelderItem.Aim aim = WelderItem.aim(mc.level, mc.player);
        int v = aim == null ? -1 : WelderItem.pick(aim.be().frame(), Vec3.atLowerCornerOf(aim.be().getBlockPos()),
                aim.eye(), aim.look(), mc.player.blockInteractionRange());
        if (v < 0)
            return;
        Component point = describe(aim.be().frame(), v);
        g.drawString(font, point, right - font.width(point), y + 2, POINT, true);
        var spin = aim.be().spin();
        // On a coupled chain: the chain's figures. Welded to something turning but no tube yet: that
        // block's speed -- the weld is the connection, the tube only carries it on.
        net.minecraft.core.BlockPos weld = aim.be().frame().anchors().get(v);
        var node = weld == null ? null
                : mc.level.getBlockEntity(aim.be().getBlockPos().offset(weld)) instanceof io.github.soundgoodizerfan.feedback.core.rotation.RotationNode n ? n : null;
        Component drive = spin.onChain(v)
                ? Component.literal(String.format(java.util.Locale.ROOT, "Network: connected · %.1f RPM · %.2f Su", spin.rpm(v), spin.su(v)))
                : node != null
                ? Component.literal(String.format(java.util.Locale.ROOT, "Network: connected · %.1f RPM · no tube yet", node.getRpm().value()))
                : Component.literal("Network: not connected");
        g.drawString(font, drive, right - font.width(drive), y + 12, POINT, true);
    }

    /** What point {@code v} is. */
    private static Component describe(Frame frame, int v) {
        List<String> parts = new ArrayList<>();
        parts.add(frame.anchors().containsKey(v) ? "welded" : "not welded");
        Frame.Pivot pivot = frame.pivots().get(v);
        if (pivot != null)
            parts.add(pivot.kind().name().toLowerCase(java.util.Locale.ROOT) + (pivot.drives() ? ", carries rotation" : ", spins free"));
        Frame.Tire tire = frame.tires().get(v);
        if (tire != null)
            parts.add("tire: " + BuiltInRegistries.BLOCK.get(tire.material()).getName().getString()
                    + " r" + Frame.TIRE_SIZES[tire.size()] + "/16");
        parts.add(frame.tubesAt(v).size() + " tube" + (frame.tubesAt(v).size() == 1 ? "" : "s"));
        return Component.literal("Point: " + String.join(" · ", parts));
    }
}
