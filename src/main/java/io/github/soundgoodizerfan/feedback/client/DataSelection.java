// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.item.DataConnectorItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

import org.jetbrains.annotations.Nullable;

/**
 * The node the Data Connector has selected, as the server last told this client, and the wire that stretches from it to
 * wherever the player is aiming: it snaps to a node under the crosshair (green), turns red when that is farther than any wire
 * reaches, and is blue while it hangs free in the air.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class DataSelection {

    private static final int FREE = 0xFF66CCFF, SNAPPED = 0xFF66FF88, TOO_FAR = 0xFFFF5555;
    /** How far in front of the eye the free end hangs when nothing is aimed at. */
    private static final double HANG = 3.0;

    @Nullable
    private static DataNodeRef selected;

    private DataSelection() {
    }

    public static void set(@Nullable DataNodeRef ref) {
        selected = ref;
    }

    @SubscribeEvent
    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        selected = null;
    }

    /** Queues this frame's wire, if the connector is in hand with a node selected. */
    static void queueWire(Minecraft mc, float partial) {
        DataNodeRef ref = selected;
        if (ref == null || mc.player == null || mc.level == null || !(mc.player.getMainHandItem().getItem() instanceof DataConnectorItem))
            return;
        Vec3 from = Vec3.atLowerCornerOf(ref.pos()).add(DataNode.boxFor(ref.side()).getCenter());
        Vec3 eye = mc.player.getEyePosition(partial);
        Vec3 look = mc.player.getViewVector(partial);
        Vec3 to = eye.add(look.scale(HANG));
        boolean snapped = false;
        if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            to = hit.getLocation();
            BlockEntity be = mc.level.getBlockEntity(hit.getBlockPos());
            Direction side = hit.getDirection();
            DataNodeRef aimed = null;
            if (be instanceof DataNode node)
                aimed = node.getNodeRef();
            else if (be instanceof Fittable fittable && fittable.getSidedFitting(side) instanceof DataNode node)
                aimed = node.getNodeRef();
            if (aimed != null && !aimed.equals(ref)) {
                to = Vec3.atLowerCornerOf(aimed.pos()).add(DataNode.boxFor(aimed.side()).getCenter());
                snapped = true;
            }
        }
        int colour = from.distanceTo(to) > FTuning.WIRE_RANGE_GOOD ? TOO_FAR : snapped ? SNAPPED : FREE;
        final Vec3 a = from, b = to;
        final int c = colour;
        final boolean end = snapped;
        DeferredDataRenderer renderer = DeferredDataRenderer.get();
        renderer.deferDraw((pose, consumer) -> DataNodeRenderer.drawLine(renderer, pose, consumer, a, b, c));
        if (end)
            renderer.deferDraw((pose, consumer) -> renderer.drawColouredBox(pose.last().pose(), consumer, new AABB(b, b).inflate(0.12), c, 0.9f));
    }
}
