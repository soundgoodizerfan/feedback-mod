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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.grab.GrabTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.Tether;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The rope of every {@link Tether}, whatever made it: each living thing that carries tethers gets a
 * line drawn from its middle to the tether's anchor. Sources draw nothing here themselves except a
 * line in flight, before any tether exists ({@code ThrownWeaponRenderer}, {@code GrappleHookRenderer}),
 * and both draw it with {@link #strip}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class TetherRopes {

    private static final int ROPE_COLOUR = 0xFF7A5A32;
    private static final int SEGMENTS = 16;

    private TetherRopes() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        boolean drew = false, boxed = false;
        for (Entity e : level.entitiesForRendering()) {
            if (!e.hasData(FCombat.TETHERS))
                continue;
            Vec3 from = middle(e, partial);
            for (Tether t : e.getData(FCombat.TETHERS).all()) {
                // A grab is a hand on the body, not a rope: no line, but the grabber sees the held body outlined, pulsing.
                if (t.anchor() instanceof Tether.Anchor.OnEntity && t.ropeLength() <= GrabTuning.ROPE) {
                    if (!t.sourceId().equals(Minecraft.getInstance().player.getUUID()))
                        continue; // only the grabber sees the outline
                    float pulse = 0.65f + 0.35f * Mth.sin(level.getGameTime() * 0.4f + partial);
                    AABB box = e.getBoundingBox().move(e.getPosition(partial).subtract(e.position())).move(-camera.x, -camera.y, -camera.z);
                    var lines = buffer.getBuffer(RenderType.lines());
                    LevelRenderer.renderLineBox(pose, lines, box.inflate(0.05), 1f, 0.72f, 0.2f, pulse);
                    LevelRenderer.renderLineBox(pose, lines, box.inflate(0.12), 1f, 0.5f, 0.1f, pulse * 0.5f);
                    boxed = true;
                    continue;
                }
                Vec3 to = anchor(level, t.anchor(), partial);
                if (to == null)
                    continue;
                pose.pushPose();
                pose.translate(from.x - camera.x, from.y - camera.y, from.z - camera.z);
                strip(buffer.getBuffer(RenderType.lineStrip()), pose.last(), (float) (to.x - from.x), (float) (to.y - from.y), (float) (to.z - from.z));
                pose.popPose();
                drew = true;
            }
        }
        if (drew)
            buffer.endBatch(RenderType.lineStrip());
        if (boxed)
            buffer.endBatch(RenderType.lines());
    }

    private static Vec3 middle(Entity e, float partial) {
        return e.getPosition(partial).add(0, e.getBbHeight() * 0.5, 0);
    }

    /** Where a line is fixed this frame, or null if what it was fixed to is gone from view. */
    private static Vec3 anchor(ClientLevel level, Tether.Anchor anchor, float partial) {
        return switch (anchor) {
            case Tether.Anchor.OnEntity a -> level.getEntity(a.entityId()) instanceof Entity e ? middle(e, partial) : null;
            case Tether.Anchor.OnBlock b -> Vec3.atCenterOf(b.pos());
            case Tether.Anchor.AtPoint p -> p.point();
        };
    }

    /** A sagging line from the pose's origin to the offset, as vanilla's fishing line is drawn, in rope brown. */
    static void strip(VertexConsumer lines, PoseStack.Pose pose, float dx, float dy, float dz) {
        float sag = Math.min(1.2f, Mth.sqrt(dx * dx + dy * dy + dz * dz) * 0.08f);
        for (int i = 0; i <= SEGMENTS; i++)
            vertex(dx, dy, dz, sag, lines, pose, i / (float) SEGMENTS, (i + 1) / (float) SEGMENTS);
    }

    private static void vertex(float dx, float dy, float dz, float sag, VertexConsumer lines, PoseStack.Pose pose, float t, float next) {
        float x = dx * t;
        float y = dy * t - sag * 4f * t * (1f - t);
        float z = dz * t;
        float nx = dx * next - x;
        float ny = dy * next - sag * 4f * next * (1f - next) - y;
        float nz = dz * next - z;
        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0f) {
            nx /= len;
            ny /= len;
            nz /= len;
        } else {
            nx = 0f;
            ny = 1f;
            nz = 0f;
        }
        lines.addVertex(pose, x, y, z).setColor(ROPE_COLOUR).setNormal(pose, nx, ny, nz);
    }
}
