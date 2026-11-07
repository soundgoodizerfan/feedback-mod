// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.grab.GrabTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.combat.tether.RopeWrap;
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
        boolean boxed = false;
        java.util.Map<java.util.UUID, GrappleHook> hooks = new java.util.HashMap<>();
        for (Entity e : level.entitiesForRendering())
            if (e instanceof GrappleHook h && h.anchored())
                hooks.put(h.getUUID(), h);
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
                GrappleHook hook = hooks.get(t.sourceId());
                if (hook != null && hook.swingRope() > 0f) {
                    swingLine(buffer, pose, camera, hook, from);
                    buffer.endBatch(RenderType.lineStrip());
                    continue;
                }
                Vec3 to = hook != null ? hook.getPosition(partial) : anchor(level, t.anchor(), partial);
                if (to == null)
                    continue;
                segment(buffer, pose, camera, from, to, -1f);
                buffer.endBatch(RenderType.lineStrip());
            }
        }
        if (boxed)
            buffer.endBatch(RenderType.lines());
    }

    /**
     * A grapple swing's line: the held body to the last bend, back along the bends to the hook. Only the free stretch hangs, by as
     * much as the line has slack (the whole rope less the path); the stretches over edges are taut.
     */
    private static void swingLine(MultiBufferSource.BufferSource buffer, PoseStack pose, Vec3 camera, GrappleHook hook, Vec3 from) {
        java.util.List<Vec3> bends = hook.bends();
        Vec3 anchor = hook.position();
        float rope = hook.getOwner() == Minecraft.getInstance().player ? GrappleSwing.rope(hook.getId(), hook.swingRope()) : hook.swingRope();
        float slack = (float) Math.max(0, rope - RopeWrap.length(anchor, bends, from));
        Vec3 prev = from;
        for (int i = bends.size() - 1; i >= 0; i--) {
            segment(buffer, pose, camera, prev, bends.get(i), prev == from ? slack : 0f);
            prev = bends.get(i);
        }
        segment(buffer, pose, camera, prev, anchor, prev == from ? slack : 0f);
    }

    /** One stretch of line from {@code a} to {@code b}; {@code slack} below 0 uses the plain sag, otherwise the line hangs by that slack. */
    private static void segment(MultiBufferSource.BufferSource buffer, PoseStack pose, Vec3 camera, Vec3 a, Vec3 b, float slack) {
        pose.pushPose();
        pose.translate(a.x - camera.x, a.y - camera.y, a.z - camera.z);
        float dx = (float) (b.x - a.x), dy = (float) (b.y - a.y), dz = (float) (b.z - a.z);
        float sag = slack < 0f ? Math.min(1.2f, Mth.sqrt(dx * dx + dy * dy + dz * dz) * 0.08f) : Math.min(2.5f, slack * 0.5f);
        strip(buffer.getBuffer(RenderType.lineStrip()), pose.last(), dx, dy, dz, sag);
        pose.popPose();
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
        strip(lines, pose, dx, dy, dz, Math.min(1.2f, Mth.sqrt(dx * dx + dy * dy + dz * dz) * 0.08f));
    }

    /** The same line hanging {@code sag} blocks at its middle. */
    static void strip(VertexConsumer lines, PoseStack.Pose pose, float dx, float dy, float dz, float sag) {
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
