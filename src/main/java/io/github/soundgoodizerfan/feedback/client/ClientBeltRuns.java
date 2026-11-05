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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltRuns;
import io.github.soundgoodizerfan.feedback.net.BeltRunPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * The client's copy of every belt run it has been told about, and the one place cargo on belts is drawn.
 * Cargo is not an entity and not a block entity's own state: the server sends where each unit is at a
 * moment and how fast the free ones go, and this extrapolates between packets. Items are drawn after the
 * entities, one small item model per unit.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class ClientBeltRuns {

    private record Snap(long id, Direction facing, long[] blocks, byte[] kinds, float speed, long received, List<BeltRunPayload.Entry> cargo) {
    }

    private static final Map<Long, Snap> RUNS = new HashMap<>();
    private static final Map<Long, Snap> BY_BLOCK = new HashMap<>();
    private static boolean hooked;

    private ClientBeltRuns() {
    }

    public static void receive(BeltRunPayload p) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;
        if (!hooked) {
            hooked = true;
            BeltRuns.clientSpeed = pos -> {
                Snap s = BY_BLOCK.get(pos.asLong());
                return s == null ? 0f : s.speed;
            };
        }
        Snap old = RUNS.get(p.id());
        long[] blocks = p.shape() != null ? p.shape() : old == null ? null : old.blocks;
        byte[] kinds = p.shape() != null ? p.kinds() : old == null ? null : old.kinds;
        if (old != null)
            for (long b : old.blocks)
                BY_BLOCK.remove(b);
        if (blocks == null || blocks.length == 0) {
            RUNS.remove(p.id());
            return;
        }
        Snap snap = new Snap(p.id(), Direction.from3DDataValue(p.facing()), blocks, kinds, p.speed(), level.getGameTime(), p.cargo());
        RUNS.put(p.id(), snap);
        for (long b : blocks)
            BY_BLOCK.put(b, snap);
    }

    @SubscribeEvent
    public static void onUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            RUNS.clear();
            BY_BLOCK.clear();
        }
    }

    private static Vec3 world(Snap s, double c) {
        int n = s.blocks.length;
        int i = (int) Math.max(0, Math.min(n - 1, Math.floor(c)));
        double frac = c - i;
        BlockPos b = BlockPos.of(s.blocks[i]);
        double dy = s.kinds[i] == 1 ? frac : s.kinds[i] == 2 ? -frac : 0.0;
        return new Vec3(b.getX() + 0.5 + s.facing.getStepX() * (frac - 0.5), b.getY() + 0.25 + dy, b.getZ() + 0.5 + s.facing.getStepZ() * (frac - 0.5));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || RUNS.isEmpty())
            return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        long now = level.getGameTime();
        for (Snap s : RUNS.values()) {
            if (s.cargo.isEmpty() || world(s, s.blocks.length / 2.0).distanceToSqr(cam) > 80 * 80 + s.blocks.length * s.blocks.length)
                continue;
            double elapsed = now - s.received + partial;
            double max = s.blocks.length - 0.1;
            for (BeltRunPayload.Entry e : s.cargo) {
                double c = e.pinned() ? e.pos() : Math.min(max, e.pos() + s.speed * elapsed);
                Vec3 at = world(s, c);
                if (at.distanceToSqr(cam) > 48 * 48)
                    continue;
                int copies = Math.min(3, e.stack().getCount());
                for (int k = 0; k < copies; k++) {
                    pose.pushPose();
                    pose.translate(at.x - cam.x, at.y - cam.y + 0.12 + k * 0.07, at.z - cam.z);
                    pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(k * 37f));
                    pose.scale(0.5f, 0.5f, 0.5f);
                    Minecraft.getInstance().getItemRenderer().renderStatic(e.stack(), ItemDisplayContext.GROUND,
                            LevelRenderer.getLightColor(level, BlockPos.containing(at)), OverlayTexture.NO_OVERLAY, pose, buffer, level, 0);
                    pose.popPose();
                }
            }
        }
        buffer.endBatch();
    }
}
