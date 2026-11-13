// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.weapon.GunTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.net.GunImpactPayload;
import io.github.soundgoodizerfan.feedback.net.GunTracerPayload;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The look of bullets: each shot's streak (a camera-facing additive ribbon, a bright core in a coloured halo, fading to
 * nothing at the tail), the particles where a round lands or a gun fires, and bullet holes on the faces it struck.
 * Everything here is told by the server and decides nothing; a streak is stepped with the server's drag and gravity
 * between its messages.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class GunFx {

    private GunFx() {
    }

    // ---------------------------------------------------------------- tracers

    private static final class Streak {
        Vec3 prev, pos, vel;
        /** Where the real bullet started, and the push from there to the muzzle; the push fades with distance. */
        Vec3 origin, off = Vec3.ZERO;
        int argb;
        float drag;
        /** Set when the server says the shot ended: the head stops here and the tail catches up. */
        Vec3 end;
        int fade;
        int age;
    }

    private static final Map<Integer, Streak> STREAKS = new HashMap<>();
    private static final int STREAK_FADE_TICKS = 4, STREAK_MAX_AGE = 200;

    public static void tracer(GunTracerPayload p) {
        Vec3 at = new Vec3(p.x(), p.y(), p.z());
        Vec3 v = new Vec3(p.vx(), p.vy(), p.vz());
        Streak s = STREAKS.get(p.id());
        if (p.ended()) {
            if (s != null)
                s.end = at;
            return;
        }
        if (s == null) {
            s = new Streak();
            STREAKS.put(p.id(), s);
            s.prev = at;
            s.origin = at;
            s.off = muzzleOffset(at, v);
        }
        s.pos = at;
        s.vel = v;
        s.argb = p.argb();
        s.drag = p.drag();
    }

    /** Muzzle's distance ahead of the eye, to its right and below it, in blocks: where a held gun's barrel ends. */
    private static final double MUZZLE_AHEAD = 0.75, MUZZLE_RIGHT = 0.17, MUZZLE_DOWN = 0.13, MUZZLE_BLEND_BLOCKS = 14;

    /** From the bullet's start (a little ahead of the shooter's eye) to the shooter's muzzle. */
    private static Vec3 muzzleOffset(Vec3 start, Vec3 vel) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || vel.lengthSqr() < 1e-8)
            return Vec3.ZERO;
        net.minecraft.world.entity.player.Player nearest = null;
        double best = 1.5 * 1.5;
        for (net.minecraft.world.entity.player.Player pl : mc.level.players()) {
            double d = pl.getEyePosition().distanceToSqr(start);
            if (d < best) {
                best = d;
                nearest = pl;
            }
        }
        if (nearest == null)
            return Vec3.ZERO;
        Vec3 dir = vel.normalize();
        Vec3 right = dir.cross(new Vec3(0, 1, 0));
        right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
        Vec3 muzzle = nearest.getEyePosition().add(dir.scale(MUZZLE_AHEAD)).add(right.scale(MUZZLE_RIGHT)).add(0, -MUZZLE_DOWN, 0);
        return muzzle.subtract(start);
    }

    /** A point on the streak, pushed toward the muzzle while it is near where the bullet started. */
    private static Vec3 bent(Streak s, Vec3 p) {
        double k = 1 - p.distanceTo(s.origin) / MUZZLE_BLEND_BLOCKS;
        return k <= 0 ? p : p.add(s.off.scale(k));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            STREAKS.clear();
            DECALS.clear();
            return;
        }
        if (mc.isPaused())
            return;
        for (Iterator<Streak> it = STREAKS.values().iterator(); it.hasNext(); ) {
            Streak s = it.next();
            s.prev = s.pos;
            if (s.end != null) {
                s.pos = s.end;
                if (++s.fade > STREAK_FADE_TICKS)
                    it.remove();
                continue;
            }
            if (++s.age > STREAK_MAX_AGE) {
                it.remove();
                continue;
            }
            s.pos = s.pos.add(s.vel);
            s.vel = s.vel.scale(1 - GunTuning.GUN_DRAG_PER_TICK * s.drag).add(0, -GunTuning.GUN_GRAVITY_PER_TICK, 0);
        }
        long now = mc.level.getGameTime();
        DECALS.removeIf(d -> now - d.born > DECAL_LIFE_TICKS);
    }

    // ---------------------------------------------------------------- decals

    /** Decal kinds, chosen by the server from the surface's own stuff: hard stone and metal, wood, glass, soft earth. */
    public static final int DECAL_CHIP = 0, DECAL_SPLINTER = 1, DECAL_CRACK = 2, DECAL_DENT = 3;
    /** Sprite sizes in pixels; one pixel is a sixteenth of a block, as everywhere. */
    private static final int[] DECAL_PX = {5, 6, 10, 6};
    private static final ResourceLocation[] DECAL_TEX = {
            Feedback.id("textures/misc/bullet_hole_chip.png"), Feedback.id("textures/misc/bullet_hole_splinter.png"),
            Feedback.id("textures/misc/bullet_hole_crack.png"), Feedback.id("textures/misc/bullet_hole_dent.png")};
    private static final int DECAL_LIFE_TICKS = 600, DECAL_FADE_TICKS = 120, DECAL_MAX = 96;

    private record Decal(Vec3 pos, Vec3 normal, int kind, float angle, long born) {
    }

    private static final List<Decal> DECALS = new ArrayList<>();

    private static void addDecal(Level level, Vec3 at, Vec3 normal, int kind) {
        if (kind < 0 || kind >= DECAL_TEX.length)
            return;
        if (DECALS.size() >= DECAL_MAX)
            DECALS.remove(0);
        DECALS.add(new Decal(at, normal, kind, level.random.nextFloat() * Mth.TWO_PI, level.getGameTime()));
    }

    // ---------------------------------------------------------------- impacts

    public static void impact(GunImpactPayload p) {
        Level level = Minecraft.getInstance().level;
        if (level == null)
            return;
        RandomSource r = level.random;
        Vec3 at = new Vec3(p.x(), p.y(), p.z());
        Vec3 dir = new Vec3(p.dx(), p.dy(), p.dz());
        float power = p.power();
        switch (p.kind()) {
            case GunImpactPayload.FLESH -> {
                if (!ClientSettings.blood())
                    return;
                int c = p.data();
                ParticleOptions drop = new net.minecraft.core.particles.DustParticleOptions(new Vector3f((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f), 0.9f + power * 0.6f);
                int n = 4 + (int) (power * 12);
                for (int i = 0; i < n; i++)
                    spray(level, drop, at, dir, 0.18 * (0.4 + power), 0.07, r);
            }
            case GunImpactPayload.BONE -> {
                if (!ClientSettings.blood())
                    return;
                ParticleOptions chip = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BONE_BLOCK.defaultBlockState());
                int n = 3 + (int) (power * 8);
                for (int i = 0; i < n; i++)
                    spray(level, chip, at, dir.reverse(), 0.12, 0.08, r);
            }
            case GunImpactPayload.ARMOUR -> sparks(level, at, dir.reverse(), 4 + (int) (power * 8), 0.35, r);
            case GunImpactPayload.BLOCK -> {
                BlockState state = Block.stateById(p.data());
                if (state.isAir())
                    return;
                ParticleOptions chip = new BlockParticleOption(ParticleTypes.BLOCK, state);
                int n = 3 + (int) (power * 9);
                for (int i = 0; i < n; i++)
                    spray(level, chip, at, dir, 0.14, 0.09, r);
                if (p.decal() == DECAL_CHIP && power > 0.12f)
                    sparks(level, at, dir, 2 + (int) (power * 5), 0.28, r);
                // A puff of dust and a wisp that hangs a moment.
                level.addParticle(ParticleTypes.POOF, at.x + dir.x * 0.05, at.y + dir.y * 0.05, at.z + dir.z * 0.05,
                        dir.x * 0.03, dir.y * 0.03 + 0.01, dir.z * 0.03);
                if (power > 0.2f)
                    level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, dir.x * 0.01, 0.025, dir.z * 0.01);
                addDecal(level, at.add(dir.scale(0.004)), dir, p.decal());
            }
            case GunImpactPayload.RICOCHET -> sparks(level, at, dir, 9, 0.5, r);
            case GunImpactPayload.MUZZLE -> muzzle(level, at, dir, power, p.data() == 1, r);
            default -> {
            }
        }
    }

    private static void spray(Level level, ParticleOptions o, Vec3 at, Vec3 dir, double speed, double scatter, RandomSource r) {
        level.addParticle(o, at.x, at.y, at.z,
                dir.x * speed + (r.nextDouble() - 0.5) * scatter * 2,
                dir.y * speed + (r.nextDouble() - 0.3) * scatter * 2,
                dir.z * speed + (r.nextDouble() - 0.5) * scatter * 2);
    }

    private static void sparks(Level level, Vec3 at, Vec3 dir, int n, double speed, RandomSource r) {
        for (int i = 0; i < n; i++) {
            double s = speed * (0.4 + r.nextDouble());
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z,
                    dir.x * s + (r.nextDouble() - 0.5) * 0.3, dir.y * s + (r.nextDouble() - 0.5) * 0.3, dir.z * s + (r.nextDouble() - 0.5) * 0.3);
        }
    }

    private static void muzzle(Level level, Vec3 at, Vec3 aim, float power, boolean eject, RandomSource r) {
        int n = 2 + (int) (power * 5);
        // Your own gun in first person draws its flash at its barrel (GunFlash); the puff of fire is for everyone else.
        Minecraft mc = Minecraft.getInstance();
        boolean ownFlash = mc.player != null && mc.options.getCameraType().isFirstPerson()
                && mc.player.getEyePosition().distanceToSqr(at) < 2.5 * 2.5;
        for (int i = 0; i < n; i++) {
            if (ownFlash && i > 0)
                break;
            double s = 0.15 + 0.3 * power * r.nextDouble();
            level.addParticle(i == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, at.x, at.y, at.z,
                    aim.x * s + (r.nextDouble() - 0.5) * 0.05, aim.y * s + (r.nextDouble() - 0.5) * 0.05, aim.z * s + (r.nextDouble() - 0.5) * 0.05);
        }
        sparks(level, at, aim, 1 + (int) (power * 3), 0.3, r);
        if (eject) {
            Vec3 right = aim.cross(new Vec3(0, 1, 0)).normalize();
            Vec3 from = at.subtract(aim.scale(0.4)).add(right.scale(0.25)).add(0, -0.1, 0);
            level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)), from.x, from.y, from.z,
                    right.x * 0.12 + (r.nextDouble() - 0.5) * 0.03, 0.1 + r.nextDouble() * 0.05, right.z * 0.12 + (r.nextDouble() - 0.5) * 0.03);
        }
    }

    /** The shooter's confirmed hit. */
    public static void hitMark(boolean kill) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;
        mc.getSoundManager().play(SimpleSoundInstance.forUI((kill ? FSounds.GUN_HIT_KILL : FSounds.GUN_HIT_MARKER).get(), kill ? 1f : 1.1f, 0.8f));
    }

    // ---------------------------------------------------------------- drawing

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || (STREAKS.isEmpty() && DECALS.isEmpty()))
            return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        drawDecals(mc, pose, buffers, partial);
        drawStreaks(pose.last().pose(), buffers, cam, partial);
        pose.popPose();
    }

    private static void drawStreaks(Matrix4f m, MultiBufferSource.BufferSource buffers, Vec3 cam, float partial) {
        if (STREAKS.isEmpty())
            return;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        for (Streak s : STREAKS.values()) {
            Vec3 head = s.prev.lerp(s.pos, s.end != null ? 1.0 : partial);
            Vec3 dir = s.vel == null || s.vel.lengthSqr() < 1e-8 ? s.pos.subtract(s.prev) : s.vel;
            if (dir.lengthSqr() < 1e-8)
                continue;
            double speed = dir.length();
            dir = dir.scale(1 / speed);
            double len = Mth.clamp(speed * 1.1, 0.6, 9.0) * (1 - s.fade / (double) (STREAK_FADE_TICKS + 1));
            Vec3 tail = head.subtract(dir.scale(len));
            head = bent(s, head);
            tail = bent(s, tail);
            Vec3 axis = head.subtract(tail);
            if (axis.lengthSqr() < 1e-8)
                continue;
            dir = axis.normalize();
            // Close to the eye a streak is a smear across the view: fade it in over the first few blocks.
            double near = Mth.clamp((head.distanceTo(cam) - 0.4) / 1.5, 0, 1);
            if (near <= 0)
                continue;
            float a = (s.argb >>> 24) / 255f * (float) near;
            float cr = (s.argb >> 16 & 255) / 255f, cg = (s.argb >> 8 & 255) / 255f, cb = (s.argb & 255) / 255f;
            float width = 0.012f + a * 0.02f;
            beam(m, vc, head, tail, dir, width, cr, cg, cb, a);
            beam(m, vc, head, tail, dir, width * 0.4f, 1f, 1f, 0.95f, a * 0.9f);
        }
        buffers.endBatch(RenderType.lightning());
    }

    /** A long square beam from head to tail: four faces, opaque at the head and clear at the tail. */
    private static void beam(Matrix4f m, VertexConsumer vc, Vec3 head, Vec3 tail, Vec3 dir, float w, float r, float g, float b, float a) {
        Vec3 u = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 v = dir.cross(u).normalize().scale(w);
        u = u.scale(w);
        Vec3[] ring = {u.add(v), u.subtract(v), u.reverse().subtract(v), u.reverse().add(v)};
        for (int k = 0; k < 4; k++) {
            Vec3 c0 = ring[k], c1 = ring[(k + 1) % 4];
            quadV(m, vc, head.add(c0), r, g, b, a);
            quadV(m, vc, head.add(c1), r, g, b, a);
            quadV(m, vc, tail.add(c1), r, g, b, 0f);
            quadV(m, vc, tail.add(c0), r, g, b, 0f);
        }
    }

    private static void quadV(Matrix4f m, VertexConsumer vc, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(m, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
    }

    private static void drawDecals(Minecraft mc, PoseStack pose, MultiBufferSource.BufferSource buffers, float partial) {
        if (DECALS.isEmpty())
            return;
        long now = mc.level.getGameTime();
        PoseStack.Pose p = pose.last();
        for (int kind = 0; kind < DECAL_TEX.length; kind++) {
            VertexConsumer vc = null;
            for (Decal d : DECALS) {
                if (d.kind != kind)
                    continue;
                if (vc == null)
                    vc = buffers.getBuffer(RenderType.entityTranslucent(DECAL_TEX[kind]));
                float age = now + partial - d.born;
                float alpha = Mth.clamp((DECAL_LIFE_TICKS - age) / DECAL_FADE_TICKS, 0f, 1f);
                if (alpha <= 0)
                    continue;
                Vec3 n = d.normal;
                Vec3 u = Math.abs(n.y) > 0.9 ? new Vec3(1, 0, 0) : n.cross(new Vec3(0, 1, 0)).normalize();
                Vec3 v = n.cross(u);
                double c = Math.cos(d.angle), s = Math.sin(d.angle);
                Vec3 ru = u.scale(c).add(v.scale(s)), rv = v.scale(c).subtract(u.scale(s));
                double h = DECAL_PX[kind] / 32.0;
                int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(d.pos.add(n.scale(0.1))));
                vertex(vc, p, d.pos.subtract(ru.scale(h)).subtract(rv.scale(h)), 0, 1, alpha, light, n);
                vertex(vc, p, d.pos.add(ru.scale(h)).subtract(rv.scale(h)), 1, 1, alpha, light, n);
                vertex(vc, p, d.pos.add(ru.scale(h)).add(rv.scale(h)), 1, 0, alpha, light, n);
                vertex(vc, p, d.pos.subtract(ru.scale(h)).add(rv.scale(h)), 0, 0, alpha, light, n);
            }
            if (vc != null)
                buffers.endBatch(RenderType.entityTranslucent(DECAL_TEX[kind]));
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 at, float u, float v, float alpha, int light, Vec3 n) {
        vc.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(1f, 1f, 1f, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }
}
