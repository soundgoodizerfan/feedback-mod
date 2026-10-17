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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.FrameEditPayload;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.frame.Frame;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderMode;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderSelected;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Welder's move gizmo, client side: six arrows on the selection's middle, two per axis (author,
 * 2026-10-16). Click an arrow to step 1/16 that way, sneak-click to step a block, or hold and look
 * along the axis to drag -- the drag snaps to the grid, shows where the points will land, and is
 * sent once, on release ({@link FrameEditPayload}). In Extrude mode the same arrows pull out
 * copies instead. Point, Select and Extrude modes show it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class FrameGizmo {

    /** Where an arrow starts and ends, out from the middle, in blocks; how near the look must pass it. */
    public static final double FROM = 0.12, TO = 0.6, PICK = 0.07;
    /** A press held no longer than this, without dragging, is a click. */
    private static final int CLICK_TICKS = 8;

    /** One arrow: an axis (0 x, 1 y, 2 z) and a way along it. */
    public record Arrow(int axis, int sign) {
        public Vec3 dir() {
            return new Vec3(axis == 0 ? sign : 0, axis == 1 ? sign : 0, axis == 2 ? sign : 0);
        }
    }

    @Nullable
    private static Arrow dragging;
    @Nullable
    private static BlockPos dragFrame;
    private static double startS;
    private static int steps, held;

    private FrameGizmo() {
    }

    /** The selection the gizmo acts on: the main-hand Welder's, in a mode that shows the gizmo. */
    @Nullable
    public static WelderSelected selection(Player player, FrameBlockEntity be) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof WelderItem))
            return null;
        WelderMode mode = stack.getOrDefault(FVehicles.WELDER_MODE.get(), WelderMode.POINT);
        if (mode != WelderMode.POINT && mode != WelderMode.SELECT && mode != WelderMode.EXTRUDE)
            return null;
        return WelderItem.selected(stack, be);
    }

    /** The selection's middle, in frame-local blocks. */
    public static Vec3 centre(Frame frame, WelderSelected sel) {
        Vec3 sum = Vec3.ZERO;
        for (int v : sel.vertices())
            sum = sum.add(frame.pos(v));
        return sum.scale(1.0 / sel.vertices().size());
    }

    /** The arrow under the look, or null. {@code eye} and {@code look} in frame-local blocks. */
    @Nullable
    public static Arrow hovered(Vec3 centre, Vec3 eye, Vec3 look) {
        Arrow best = null;
        double bestT = Double.MAX_VALUE;
        for (int axis = 0; axis < 3; axis++)
            for (int sign = -1; sign <= 1; sign += 2) {
                Arrow a = new Arrow(axis, sign);
                Vec3 p = centre.add(a.dir().scale(FROM)), q = centre.add(a.dir().scale(TO));
                double[] tu = closest(eye, look, p, q);
                if (tu != null && tu[0] > 0 && tu[0] < bestT && tu[2] <= PICK) {
                    best = a;
                    bestT = tu[0];
                }
            }
        return best;
    }

    /** Ray {@code eye + t look} against segment {@code p-q}: {t along the ray, u along the segment 0-1, distance}. */
    @Nullable
    private static double[] closest(Vec3 eye, Vec3 look, Vec3 p, Vec3 q) {
        Vec3 d = q.subtract(p);
        double len = d.length();
        Vec3 a = d.scale(1 / len);
        double b = a.dot(look), denom = 1 - b * b;
        if (denom < 1e-6)
            return null;
        Vec3 w = p.subtract(eye);
        double s = (b * look.dot(w) - a.dot(w)) / denom; // along the segment, blocks
        s = Math.clamp(s, 0, len);
        Vec3 onSeg = p.add(a.scale(s));
        double t = onSeg.subtract(eye).dot(look);
        double dist = eye.add(look.scale(t)).distanceTo(onSeg);
        return new double[]{t, s / len, dist};
    }

    /** Where along the axis through {@code centre} the look passes nearest, in blocks; NaN looking straight down it. */
    private static double along(Vec3 centre, Arrow a, Vec3 eye, Vec3 look) {
        Vec3 dir = new Vec3(a.axis == 0 ? 1 : 0, a.axis == 1 ? 1 : 0, a.axis == 2 ? 1 : 0);
        double b = dir.dot(look), denom = 1 - b * b;
        if (denom < 1e-3)
            return Double.NaN;
        Vec3 w = centre.subtract(eye);
        return (b * look.dot(w) - dir.dot(w)) / denom;
    }

    /** The drag in progress on {@code frame}: its arrow, and how many cells it has moved; null if none. */
    @Nullable
    public static Arrow dragging(BlockPos frame) {
        return dragging != null && frame.equals(dragFrame) ? dragging : null;
    }

    public static int steps() {
        return steps;
    }

    @SubscribeEvent
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem())
            return;
        if (dragging != null) { // the use key repeats while held: the drag owns it
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;
        WelderItem.Aim aim = WelderItem.aim(mc.level, mc.player);
        if (aim == null)
            return;
        WelderSelected sel = selection(mc.player, aim.be());
        if (sel == null)
            return;
        Vec3 origin = Vec3.atLowerCornerOf(aim.be().getBlockPos());
        Vec3 c = centre(aim.be().frame(), sel);
        Vec3 eye = aim.eye().subtract(origin);
        Arrow a = hovered(c, eye, aim.look());
        if (a == null)
            return;
        event.setCanceled(true);
        event.setSwingHand(false);
        dragging = a;
        dragFrame = aim.be().getBlockPos();
        startS = along(c, a, eye, aim.look());
        steps = 0;
        held = 0;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (dragging == null)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getMainHandItem().getItem() instanceof WelderItem)) {
            dragging = null;
            return;
        }
        if (mc.options.keyUse.isDown()) {
            held++;
            WelderItem.Aim aim = WelderItem.aim(mc.level, mc.player);
            if (aim == null || !aim.be().getBlockPos().equals(dragFrame))
                return;
            WelderSelected sel = selection(mc.player, aim.be());
            if (sel == null)
                return;
            Vec3 origin = Vec3.atLowerCornerOf(dragFrame);
            double s = along(centre(aim.be().frame(), sel), dragging, aim.eye().subtract(origin), aim.look());
            if (!Double.isNaN(s) && !Double.isNaN(startS))
                steps = (int) Math.round((s - startS) * Frame.GRID);
            return;
        }
        int send = steps;
        if (send == 0 && held <= CLICK_TICKS)
            send = dragging.sign() * (mc.player.isShiftKeyDown() ? Frame.GRID : 1);
        boolean extrude = mc.player.getMainHandItem().getOrDefault(FVehicles.WELDER_MODE.get(), WelderMode.POINT) == WelderMode.EXTRUDE;
        if (send != 0)
            PacketDistributor.sendToServer(new FrameEditPayload(dragFrame, dragging.axis(), send, extrude));
        dragging = null;
        steps = 0;
    }
}
