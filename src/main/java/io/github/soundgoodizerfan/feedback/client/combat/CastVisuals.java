// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gun.GunFx;
import io.github.soundgoodizerfan.feedback.net.CastShowPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The cast techniques' own effects, drawn procedurally in the style of the gun tracers (lightning-type glow quads, a
 * coloured body around a white core): tapered streaks, jagged bolts, expanding rings, slash crescents, shard bursts,
 * pillars and trails that follow a body. Every shape is built each frame from the kind, two points, a colour, a size and a
 * seed, so the server sends one message per effect. Pure show: nothing here changes a figure.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class CastVisuals {

    /** A tapered tracer flying from a to b over its life. */
    public static final byte STREAK = 0;
    /** A jagged bolt from a to b that flickers; scale is its width. */
    public static final byte BOLT = 1;
    /** A flat ring at a on the plane facing b (up if b is zero), growing to scale blocks. */
    public static final byte RING = 2;
    /** A crescent swept round a, facing along b, radius scale. */
    public static final byte SLASH = 3;
    /** Shards thrown out of a, reaching scale blocks. */
    public static final byte BURST = 4;
    /** A pillar rising from a, scale blocks tall. */
    public static final byte COLUMN = 5;
    /** A ribbon trailing the entity (a is an offset from its feet), scale its width, for life ticks. */
    public static final byte TRAIL = 6;
    /** Rings closing in on a from scale blocks: a charge gathering. */
    public static final byte GATHER = 7;

    private static final int MAX = 160, TRAIL_POINTS = 14;

    private static final class Fx {
        byte kind;
        int entity, rgb, age, life;
        Vec3 a, b;
        float scale;
        long seed;
        final List<Vec3> trail = new ArrayList<>();
    }

    private static final List<Fx> LIVE = new ArrayList<>();

    private CastVisuals() {
    }

    public static void show(CastShowPayload p) {
        Fx f = new Fx();
        f.kind = p.kind();
        f.entity = p.entity();
        f.a = new Vec3(p.ax(), p.ay(), p.az());
        f.b = new Vec3(p.bx(), p.by(), p.bz());
        f.rgb = p.rgb();
        f.scale = p.scale();
        f.life = Math.max(1, p.life());
        f.seed = p.seed();
        if (LIVE.size() >= MAX)
            LIVE.remove(0);
        LIVE.add(f);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            LIVE.clear();
            return;
        }
        if (mc.isPaused())
            return;
        for (Fx f : LIVE) {
            f.age++;
            if (f.kind == TRAIL) {
                Entity e = mc.level.getEntity(f.entity);
                if (e != null) {
                    f.trail.add(0, e.position().add(f.a));
                    if (f.trail.size() > TRAIL_POINTS)
                        f.trail.remove(f.trail.size() - 1);
                }
            }
        }
        LIVE.removeIf(f -> f.age > f.life + (f.kind == TRAIL ? TRAIL_POINTS : 0));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LIVE.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
            return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        for (Fx f : LIVE) {
            float t = Mth.clamp((f.age + partial) / f.life, 0f, 1f);
            float cr = (f.rgb >> 16 & 255) / 255f, cg = (f.rgb >> 8 & 255) / 255f, cb = (f.rgb & 255) / 255f;
            switch (f.kind) {
                case STREAK -> streak(m, vc, f, t, cr, cg, cb);
                case BOLT -> bolt(m, vc, f, cr, cg, cb);
                case RING -> ring(m, vc, f.a, f.b, f.scale * ease(t), f.scale * 0.12f, cr, cg, cb, 1f - t * t);
                case SLASH -> slash(m, vc, f, t, cr, cg, cb);
                case BURST -> burst(m, vc, f, t, cr, cg, cb);
                case COLUMN -> column(m, vc, f, t, cr, cg, cb);
                case TRAIL -> trail(m, vc, f, partial, cr, cg, cb);
                case GATHER -> {
                    for (int i = 0; i < 3; i++) {
                        float tt = Mth.clamp(t * 1.4f - i * 0.18f, 0f, 1f);
                        if (tt > 0 && tt < 1)
                            ring(m, vc, f.a, f.b, f.scale * (1f - tt), f.scale * 0.06f, cr, cg, cb, tt);
                    }
                }
                default -> {
                }
            }
        }
        buffers.endBatch(RenderType.lightning());
        pose.popPose();
    }

    /** Fast out, slow in. */
    private static float ease(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    // ------------------------------------------------------------ shapes

    private static void streak(Matrix4f m, VertexConsumer vc, Fx f, float t, float cr, float cg, float cb) {
        Vec3 d = f.b.subtract(f.a);
        double len = d.length();
        if (len < 1e-4)
            return;
        Vec3 dir = d.scale(1 / len);
        // The head flies out over the first 60% of the life, the tail catches it after.
        double head = len * Math.min(1f, t / 0.6f), tail = len * Math.max(0f, (t - 0.25f) / 0.75f);
        if (tail >= head)
            return;
        Vec3 hp = f.a.add(dir.scale(head)), tp = f.a.add(dir.scale(tail));
        float w = f.scale <= 0 ? 0.04f : f.scale;
        GunFx.beam(m, vc, hp, tp, dir, w, cr, cg, cb, 0.9f, 0f);
        GunFx.beam(m, vc, hp, tp, dir, w * 0.4f, 1f, 1f, 0.95f, 0.9f, 0f);
    }

    private static void bolt(Matrix4f m, VertexConsumer vc, Fx f, float cr, float cg, float cb) {
        Vec3 axis = f.b.subtract(f.a);
        double len = axis.length();
        if (len < 1e-4)
            return;
        Vec3 dir = axis.scale(1 / len);
        float a = 1f - f.age / (float) (f.life + 1);
        RandomSource r = RandomSource.create(f.seed + f.age * 7919L);
        List<Vec3> path = GunFx.bolt(f.a, f.b, dir, len, 0.12 + len * 0.02, r);
        float w = f.scale <= 0 ? 0.03f : f.scale;
        for (int i = 0; i + 1 < path.size(); i++) {
            GunFx.tube(m, vc, path.get(i), path.get(i + 1), w * 3f, cr, cg, cb, a * 0.5f);
            GunFx.tube(m, vc, path.get(i), path.get(i + 1), w, 1f, 1f, 1f, a);
        }
    }

    /** A flat annulus of radius {@code r} and half-thickness {@code w}: bright on the rim, clear to either side. */
    private static void ring(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 n, float r, float w, float cr, float cg, float cb, float a) {
        if (r < 0.02f || a <= 0f)
            return;
        Vec3 up = n.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : n.normalize();
        Vec3 u = Math.abs(up.y) > 0.95 ? new Vec3(1, 0, 0) : up.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 v = up.cross(u).normalize();
        int seg = Mth.clamp((int) (r * 14), 16, 48);
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            Vec3 d0 = u.scale(Math.cos(a0)).add(v.scale(Math.sin(a0))), d1 = u.scale(Math.cos(a1)).add(v.scale(Math.sin(a1)));
            Vec3 in0 = c.add(d0.scale(r - w * 2)), in1 = c.add(d1.scale(r - w * 2)), mid0 = c.add(d0.scale(r)), mid1 = c.add(d1.scale(r));
            Vec3 out0 = c.add(d0.scale(r + w * 0.5)), out1 = c.add(d1.scale(r + w * 0.5));
            band(m, vc, in0, in1, mid1, mid0, cr, cg, cb, 0f, a * 0.8f);
            band(m, vc, mid0, mid1, out1, out0, 1f, 1f, 1f, a, 0f);
        }
    }

    private static void band(Matrix4f m, VertexConsumer vc, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, float cr, float cg, float cb, float aFrom, float aTo) {
        GunFx.quadV(m, vc, p0, cr, cg, cb, aFrom);
        GunFx.quadV(m, vc, p1, cr, cg, cb, aFrom);
        GunFx.quadV(m, vc, p2, cr, cg, cb, aTo);
        GunFx.quadV(m, vc, p3, cr, cg, cb, aTo);
    }

    /** A crescent swept through 140 degrees round {@code a}, facing along {@code b}, its head leading and its tail fading. */
    private static void slash(Matrix4f m, VertexConsumer vc, Fx f, float t, float cr, float cg, float cb) {
        Vec3 fwd = f.b.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : f.b.normalize();
        Vec3 side = Math.abs(fwd.y) > 0.95 ? new Vec3(1, 0, 0) : fwd.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 up = side.cross(fwd).normalize();
        RandomSource r = RandomSource.create(f.seed);
        // The plane of the sweep is tilted a random way round the forward axis, so repeated slashes differ.
        double tilt = (r.nextDouble() - 0.5) * Math.PI * 0.9;
        Vec3 s = side.scale(Math.cos(tilt)).add(up.scale(Math.sin(tilt)));
        float span = 2.4f, from = -span / 2;
        float head = ease(Math.min(1f, t / 0.55f)), tail = Math.max(0f, (t - 0.2f) / 0.8f);
        int seg = 18;
        float fade = 1f - t * t;
        for (int i = 0; i < seg; i++) {
            float k0 = i / (float) seg, k1 = (i + 1) / (float) seg;
            if (k1 < tail || k0 > head)
                continue;
            float g0 = Math.max(k0, tail), g1 = Math.min(k1, head);
            float e0 = Mth.sin(g0 * Mth.PI), e1 = Mth.sin(g1 * Mth.PI);
            float th0 = from + span * g0, th1 = from + span * g1;
            float rad = f.scale;
            Vec3 o0 = f.a.add(fwd.scale(Mth.cos(th0) * rad)).add(s.scale(Mth.sin(th0) * rad));
            Vec3 o1 = f.a.add(fwd.scale(Mth.cos(th1) * rad)).add(s.scale(Mth.sin(th1) * rad));
            float th = rad * 0.2f;
            Vec3 i0 = f.a.add(fwd.scale(Mth.cos(th0) * (rad - th * e0))).add(s.scale(Mth.sin(th0) * (rad - th * e0)));
            Vec3 i1 = f.a.add(fwd.scale(Mth.cos(th1) * (rad - th * e1))).add(s.scale(Mth.sin(th1) * (rad - th * e1)));
            float al = fade * (0.35f + 0.65f * ((g0 - tail) / Math.max(0.01f, head - tail)));
            band(m, vc, i0, i1, o1, o0, cr, cg, cb, 0f, al);
            // The cutting edge is white.
            Vec3 e0v = o0.subtract(i0).scale(0.25), e1v = o1.subtract(i1).scale(0.25);
            band(m, vc, o0.subtract(e0v), o1.subtract(e1v), o1, o0, 1f, 1f, 1f, al * 0.9f, al);
        }
    }

    private static void burst(Matrix4f m, VertexConsumer vc, Fx f, float t, float cr, float cg, float cb) {
        RandomSource r = RandomSource.create(f.seed);
        int n = 12 + (int) (f.scale * 3);
        float a = 1f - t;
        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(r.nextGaussian(), r.nextGaussian() * 0.6, r.nextGaussian());
            if (d.lengthSqr() < 1e-6)
                continue;
            d = d.normalize();
            float reach = f.scale * (0.5f + 0.5f * r.nextFloat());
            float head = reach * ease(t), tail = reach * Math.max(0f, ease(t) - 0.35f);
            if (head - tail < 0.02f)
                continue;
            Vec3 hp = f.a.add(d.scale(head)), tp = f.a.add(d.scale(tail));
            GunFx.beam(m, vc, hp, tp, d, 0.018f, cr, cg, cb, a, 0f);
            GunFx.beam(m, vc, hp, tp, d, 0.007f, 1f, 1f, 1f, a, 0f);
        }
    }

    private static void column(Matrix4f m, VertexConsumer vc, Fx f, float t, float cr, float cg, float cb) {
        float h = f.scale * ease(Math.min(1f, t / 0.4f)), a = 1f - Math.max(0f, (t - 0.4f) / 0.6f);
        if (h < 0.05f)
            return;
        Vec3 top = f.a.add(0, h, 0);
        float w = 0.18f * (1f - t * 0.6f);
        GunFx.beam(m, vc, top, f.a, new Vec3(0, 1, 0), w, cr, cg, cb, a * 0.55f, a * 0.1f);
        GunFx.beam(m, vc, top, f.a, new Vec3(0, 1, 0), w * 0.35f, 1f, 1f, 1f, a, a * 0.3f);
    }

    private static void trail(Matrix4f m, VertexConsumer vc, Fx f, float partial, float cr, float cg, float cb) {
        Entity e = Minecraft.getInstance().level.getEntity(f.entity);
        if (f.trail.size() < 2)
            return;
        // While the entity lives the head follows it exactly; after the effect's life the ribbon drains from the head.
        int drain = Math.max(0, f.age - f.life);
        float w = f.scale <= 0 ? 0.08f : f.scale;
        int n = f.trail.size();
        for (int i = drain; i + 1 < n; i++) {
            Vec3 p0 = f.trail.get(i), p1 = f.trail.get(i + 1);
            if (i == 0 && e != null && drain == 0)
                p0 = e.getPosition(partial).add(f.a);
            float k0 = 1f - i / (float) n, k1 = 1f - (i + 1) / (float) n;
            Vec3 axis = p0.subtract(p1);
            if (axis.lengthSqr() < 1e-8)
                continue;
            Vec3 dir = axis.normalize();
            GunFx.beam(m, vc, p0, p1, dir, w * k0, cr, cg, cb, 0.7f * k0, 0.7f * k1);
            GunFx.beam(m, vc, p0, p1, dir, w * k0 * 0.35f, 1f, 1f, 1f, 0.9f * k0, 0.9f * k1);
        }
    }
}
