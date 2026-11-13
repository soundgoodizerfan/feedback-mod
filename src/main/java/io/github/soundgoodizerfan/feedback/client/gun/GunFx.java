// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.gun;

import io.github.soundgoodizerfan.feedback.weapon.GunTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientSettings;
import io.github.soundgoodizerfan.feedback.net.EnergyBeamPayload;
import io.github.soundgoodizerfan.feedback.net.GunImpactPayload;
import io.github.soundgoodizerfan.feedback.net.GunTracerPayload;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.EnergyStyle;
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
        /** 0 for a bullet, else an {@link EnergyStyle}: a blob that flies straight. */
        byte style;
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
        s.style = p.style();
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
        return bend(s.origin, s.off, p);
    }

    private static Vec3 bend(Vec3 origin, Vec3 off, Vec3 p) {
        double k = 1 - p.distanceTo(origin) / MUZZLE_BLEND_BLOCKS;
        return k <= 0 ? p : p.add(off.scale(k));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            STREAKS.clear();
            DECALS.clear();
            BEAMS.clear();
            SPARKS.clear();
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
            if (s.style != 0) {
                glow(mc.level, s.pos, s.argb & 0xFFFFFF, 0.5f);
                continue;
            }
            s.vel = s.vel.scale(1 - GunTuning.GUN_DRAG_PER_TICK * s.drag).add(0, -GunTuning.GUN_GRAVITY_PER_TICK, 0);
        }
        BEAMS.removeIf(b -> ++b.age > b.life);
        for (Iterator<Spark> it = SPARKS.iterator(); it.hasNext(); ) {
            Spark sp = it.next();
            sp.prev = sp.pos;
            sp.pos = sp.pos.add(sp.vel);
            sp.vel = sp.vel.scale(0.85).add(0, -0.01, 0);
            if (++sp.age > sp.life)
                it.remove();
        }
        long now = mc.level.getGameTime();
        DECALS.removeIf(d -> now - d.born > DECAL_LIFE_TICKS);
    }

    // ---------------------------------------------------------------- decals

    /** Decal kinds, chosen by the server from the surface's own stuff: hard stone and metal, wood, glass, soft earth. */
    public static final int DECAL_CHIP = 0, DECAL_SPLINTER = 1, DECAL_CRACK = 2, DECAL_DENT = 3, DECAL_SCORCH = 4;
    /** Sprite sizes in pixels; one pixel is a sixteenth of a block, as everywhere. */
    private static final int[] DECAL_PX = {5, 6, 10, 6, 8};
    private static final ResourceLocation[] DECAL_TEX = {
            Feedback.id("textures/misc/bullet_hole_chip.png"), Feedback.id("textures/misc/bullet_hole_splinter.png"),
            Feedback.id("textures/misc/bullet_hole_crack.png"), Feedback.id("textures/misc/bullet_hole_dent.png"), Feedback.id("textures/misc/bullet_hole_dent.png")};
    private static final int DECAL_LIFE_TICKS = 600, DECAL_FADE_TICKS = 120, DECAL_MAX = 96;

    private record Decal(Vec3 pos, Vec3 normal, int kind, float angle, long born, float size) {
    }

    private static final List<Decal> DECALS = new ArrayList<>();

    /** A hole is a small fraction of its sprite for a weak shot and the whole sprite for the hardest: size follows the shot's power. */
    private static void addDecal(Level level, Vec3 at, Vec3 normal, int kind, float power) {
        if (kind < 0 || kind >= DECAL_TEX.length)
            return;
        if (DECALS.size() >= DECAL_MAX)
            DECALS.remove(0);
        DECALS.add(new Decal(at, normal, kind, level.random.nextFloat() * Mth.TWO_PI, level.getGameTime(), 0.12f + 0.88f * Mth.clamp(power, 0f, 1f)));
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
                addDecal(level, at.add(dir.scale(0.004)), dir, p.decal(), power);
            }

            case GunImpactPayload.RICOCHET -> sparks(level, at, dir, 9, 0.5, r);
            case GunImpactPayload.MUZZLE -> muzzle(level, at, dir, power, p.data() == 1, r);
            case GunImpactPayload.ENERGY_BLOCK, GunImpactPayload.ENERGY_BODY -> energyImpact(level, at, dir, power, p.data() & 0xFFFFFF,
                    (byte) (p.data() >>> 24), p.kind() == GunImpactPayload.ENERGY_BLOCK, r);
            case GunImpactPayload.ENERGY_MUZZLE -> energyMuzzle(level, at, dir, power, p.data() & 0xFFFFFF, (byte) (p.data() >>> 24), r);
            default -> {
            }
        }
    }

    // ---------------------------------------------------------------- energy

    private static ParticleOptions dust(int rgb, float size) {
        return new net.minecraft.core.particles.DustParticleOptions(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), size);
    }

    /** A mote of the shot's colour hanging where a blob was. */
    private static void glow(Level level, Vec3 at, int rgb, float size) {
        RandomSource r = level.random;
        level.addParticle(dust(rgb, size), at.x + (r.nextDouble() - 0.5) * 0.12, at.y + (r.nextDouble() - 0.5) * 0.12, at.z + (r.nextDouble() - 0.5) * 0.12, 0, 0, 0);
    }

    /** Where an energy shot lands: a laser sparks and scorches, a blob splashes, a discharge crackles, a bolt forks short arcs. */
    private static void energyImpact(Level level, Vec3 at, Vec3 dir, float power, int rgb, byte style, boolean block, RandomSource r) {
        // What a beam or blob lands on is what it sounds like: a blob splashes, anything else zaps.
        level.playLocalSound(at.x, at.y, at.z, (style == EnergyStyle.PLASMA ? FSounds.GUN_EGUN_PELLET_HIT : block ? FSounds.GUN_EGUN_HIT_BLOCK : FSounds.GUN_EGUN_HIT_BODY).get(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.4f + 0.5f * Math.min(1f, power), 0.9f + 0.2f * r.nextFloat(), false);
        switch (style) {
            case EnergyStyle.PLASMA -> {
                for (int i = 0; i < 10 + (int) (power * 8); i++)
                    spray(level, dust(rgb, 1.1f), at, dir, 0.12, 0.1, r);
                for (int i = 0; i < 3; i++)
                    spray(level, dust(rgb, 0.5f), at, dir, 0.05, 0.06, r);
                level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, dir.x * 0.01, 0.03, dir.z * 0.01);
            }
            case EnergyStyle.PULSE -> {
                tintedSparks(at, dir, 10, 0.4, rgb, r);
                for (int i = 0; i < 4; i++)
                    spray(level, dust(rgb, 0.8f), at, dir, 0.15, 0.12, r);
                addBolt(at, at.add(dir.scale(0.3)).add(randomUnit(r).scale(0.4)), EnergyStyle.ARC, rgb, 0.5f, r);
            }
            case EnergyStyle.ARC -> {
                tintedSparks(at, dir, 6, 0.35, rgb, r);
                for (int i = 0; i < 3; i++)
                    addBolt(at, at.add(dir.scale(0.2)).add(randomUnit(r).scale(0.6)), EnergyStyle.ARC, rgb, 0.5f, r);
            }
            default -> {
                tintedSparks(at, dir, 5, 0.3, rgb, r);
                for (int i = 0; i < 3; i++)
                    spray(level, dust(rgb, 0.7f), at, dir, 0.1, 0.06, r);
            }
        }
        if (block)
            addDecalIf(level, at, dir, style, power);
    }

    private static Vec3 randomUnit(RandomSource r) {
        return new Vec3(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize();
    }

    /** The muzzle of an energy gun: no fire and no smoke; the colour glints, a discharge sheds sparks, a bolt hops. */
    private static void energyMuzzle(Level level, Vec3 at, Vec3 aim, float power, int rgb, byte style, RandomSource r) {
        Minecraft mc = Minecraft.getInstance();
        boolean own = mc.player != null && mc.options.getCameraType().isFirstPerson() && mc.player.getEyePosition().distanceToSqr(at) < 2.5 * 2.5;
        int n = own ? 1 : 3 + (int) (power * 4);
        for (int i = 0; i < n; i++)
            spray(level, dust(rgb, 0.7f), at, aim, 0.12 + 0.12 * r.nextDouble(), 0.03, r);
        switch (style) {
            case EnergyStyle.PLASMA -> {
                for (int i = 0; i < n; i++)
                    spray(level, dust(rgb, 0.5f), at, aim, 0.08, 0.04, r);
            }
            case EnergyStyle.PULSE -> tintedSparks(at, aim, own ? 1 : 5, 0.25, rgb, r);
            case EnergyStyle.ARC -> {
                for (int i = 0; i < (own ? 1 : 2); i++)
                    addBolt(at, at.add(aim.scale(0.25)).add(randomUnit(r).scale(0.3)), EnergyStyle.ARC, rgb, 0.5f, r);
            }
            default -> tintedSparks(at, aim, 1, 0.3, rgb, r);
        }
    }

    // ---------------------------------------------------------------- beams

    private static final class Beam {
        Vec3 from, to, off = Vec3.ZERO;
        byte style;
        int rgb, age, life;
        float width;
        long seed;
    }

    private static final List<Beam> BEAMS = new ArrayList<>();
    private static final int BEAM_MAX = 64;

    public static void beam(EnergyBeamPayload p) {
        Level level = Minecraft.getInstance().level;
        if (level == null)
            return;
        Beam b = new Beam();
        b.from = new Vec3(p.fx(), p.fy(), p.fz());
        b.to = new Vec3(p.tx(), p.ty(), p.tz());
        b.off = muzzleOffset(b.from, b.to.subtract(b.from));
        b.style = p.style();
        b.rgb = p.rgb();
        b.width = p.width();
        b.life = switch (p.style()) {
            case EnergyStyle.ARC -> 5;
            case EnergyStyle.PULSE -> 4;
            default -> 3;
        };
        b.seed = level.random.nextLong();
        if (BEAMS.size() >= BEAM_MAX)
            BEAMS.remove(0);
        BEAMS.add(b);
        // A discharge crackles along its length: a few sparks and motes of its colour.
        if (b.style == EnergyStyle.PULSE || b.style == EnergyStyle.ARC) {
            RandomSource r = level.random;
            Vec3 d = b.to.subtract(b.from);
            int n = Mth.clamp((int) (d.length() / 2.5), 1, 10);
            for (int i = 0; i < n; i++) {
                Vec3 at = b.from.add(d.scale(r.nextDouble()));
                tintedSparks(at, Vec3.ZERO, 1, 0.1, b.rgb, r);
                glow(level, at, b.rgb, 0.6f);
            }
        }
    }

    /** A short bolt that lasts a few ticks, for where a shot lands or leaves. */
    private static void addBolt(Vec3 from, Vec3 to, byte style, int rgb, float width, RandomSource r) {
        Beam b = new Beam();
        b.from = from;
        b.to = to;
        b.style = style;
        b.rgb = rgb;
        b.width = width;
        b.life = 4;
        b.seed = r.nextLong();
        if (BEAMS.size() < BEAM_MAX)
            BEAMS.add(b);
    }

    private static void addDecalIf(Level level, Vec3 at, Vec3 normal, byte style, float power) {
        // Only a laser and a blob leave a mark; a discharge or a bolt does not.
        if (style == EnergyStyle.LASER || style == EnergyStyle.PLASMA)
            addDecal(level, at.add(normal.scale(0.004)), normal, DECAL_SCORCH, power);
    }

    private static void drawBeams(Matrix4f m, MultiBufferSource.BufferSource buffers) {
        if (BEAMS.isEmpty())
            return;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        for (Beam b : BEAMS) {
            float a = 1f - b.age / (float) (b.life + 1);
            float cr = (b.rgb >> 16 & 255) / 255f, cg = (b.rgb >> 8 & 255) / 255f, cb = (b.rgb & 255) / 255f;
            Vec3 from = bend(b.from, b.off, b.from), to = b.to;
            Vec3 axis = to.subtract(from);
            double len = axis.length();
            if (len < 1e-4)
                continue;
            Vec3 dir = axis.scale(1 / len);
            switch (b.style) {
                case EnergyStyle.ARC, EnergyStyle.PULSE -> {
                    boolean arc = b.style == EnergyStyle.ARC;
                    // Redrawn every tick from a new seed so the bolt flickers.
                    RandomSource r = RandomSource.create(b.seed + b.age * 7919L);
                    List<Vec3> path = bolt(from, to, dir, len, arc ? 0.22 : 0.05, r);
                    float w = (arc ? 0.012f : 0.03f) * b.width;
                    strip(m, vc, path, w * 3f, cr, cg, cb, a * 0.55f);
                    strip(m, vc, path, w, 1f, 1f, 1f, a);
                    if (arc)
                        for (int i = 2; i < path.size() - 1; i += 3) {
                            Vec3 side = randomUnit(r).scale(len * 0.08 + 0.3);
                            Vec3 mid = path.get(i).add(side.scale(0.5)).add(randomUnit(r).scale(0.08)), tip = path.get(i).add(side);
                            tube(m, vc, path.get(i), mid, w * 1.5f, cr, cg, cb, a * 0.8f);
                            tube(m, vc, mid, tip, w, cr, cg, cb, a * 0.5f);
                        }
                }
                default -> {
                    float w = 0.014f * b.width;
                    tube(m, vc, from, to, w * 2.5f, cr, cg, cb, a * 0.6f);
                    tube(m, vc, from, to, w * 0.8f, 1f, 1f, 1f, a);
                }
            }
        }
        buffers.endBatch(RenderType.lightning());
    }

    /** A jagged path from {@code a} to {@code b}: a point every segment, pushed sideways by up to {@code amp}, held at both ends. */
    private static List<Vec3> bolt(Vec3 a, Vec3 b, Vec3 dir, double len, double amp, RandomSource r) {
        Vec3 u = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 v = dir.cross(u).normalize();
        int n = Mth.clamp((int) (len / 0.7), 2, 60);
        List<Vec3> pts = new ArrayList<>(n + 1);
        pts.add(a);
        for (int i = 1; i < n; i++) {
            double k = Math.sin(Math.PI * i / n), s = amp * k;
            pts.add(a.lerp(b, i / (double) n).add(u.scale((r.nextDouble() - 0.5) * 2 * s)).add(v.scale((r.nextDouble() - 0.5) * 2 * s)));
        }
        pts.add(b);
        return pts;
    }

    private static void strip(Matrix4f m, VertexConsumer vc, List<Vec3> path, float w, float r, float g, float bl, float a) {
        for (int i = 0; i + 1 < path.size(); i++)
            tube(m, vc, path.get(i), path.get(i + 1), w, r, g, bl, a);
    }

    private static void tube(Matrix4f m, VertexConsumer vc, Vec3 from, Vec3 to, float w, float r, float g, float b, float a) {
        Vec3 axis = to.subtract(from);
        if (axis.lengthSqr() < 1e-8)
            return;
        beam(m, vc, to, from, axis.normalize(), w, r, g, b, a, a);
    }

    /** A glowing blob: nested camera-facing squares, brightest at the heart, with a fading wake behind it. */
    private static void blob(Matrix4f m, VertexConsumer vc, Vec3 at, Vec3 back, Vec3 right, Vec3 up, float cr, float cg, float cb, float a) {
        float[] size = {0.34f, 0.22f, 0.12f}, alpha = {0.18f, 0.4f, 0.95f};
        for (int i = 0; i < 3; i++) {
            Vec3 x = right.scale(size[i]), y = up.scale(size[i]);
            float rr = i == 2 ? 1f : cr, gg = i == 2 ? 1f : cg, bb = i == 2 ? 1f : cb, al = alpha[i] * a;
            quadV(m, vc, at.subtract(x).subtract(y), rr, gg, bb, al);
            quadV(m, vc, at.add(x).subtract(y), rr, gg, bb, al);
            quadV(m, vc, at.add(x).add(y), rr, gg, bb, al);
            quadV(m, vc, at.subtract(x).add(y), rr, gg, bb, al);
        }
        beam(m, vc, at, back, at.subtract(back).normalize(), 0.09f, cr, cg, cb, a * 0.6f, 0f);
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

    // ---------------------------------------------------------------- tinted sparks

    /** A spark of an energy shot, in the shot's own colour (a vanilla spark particle has one fixed colour). */
    private static final class Spark {
        Vec3 pos, prev, vel;
        int rgb, age, life;
    }

    private static final List<Spark> SPARKS = new ArrayList<>();
    private static final int SPARK_MAX = 256;

    private static void tintedSparks(Vec3 at, Vec3 dir, int n, double speed, int rgb, RandomSource r) {
        for (int i = 0; i < n; i++) {
            if (SPARKS.size() >= SPARK_MAX)
                SPARKS.remove(0);
            Spark sp = new Spark();
            double s = speed * (0.4 + r.nextDouble());
            sp.pos = sp.prev = at;
            sp.vel = new Vec3(dir.x * s + (r.nextDouble() - 0.5) * 0.3, dir.y * s + (r.nextDouble() - 0.5) * 0.3, dir.z * s + (r.nextDouble() - 0.5) * 0.3);
            sp.rgb = rgb;
            sp.life = 8 + r.nextInt(8);
            SPARKS.add(sp);
        }
    }

    private static void drawSparks(Matrix4f m, MultiBufferSource.BufferSource buffers, float partial) {
        if (SPARKS.isEmpty())
            return;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        for (Spark sp : SPARKS) {
            Vec3 head = sp.prev.lerp(sp.pos, partial), tail = head.subtract(sp.vel.scale(1.6));
            Vec3 axis = head.subtract(tail);
            if (axis.lengthSqr() < 1e-8)
                continue;
            float a = 1f - sp.age / (float) (sp.life + 1);
            beam(m, vc, head, tail, axis.normalize(), 0.012f, (sp.rgb >> 16 & 255) / 255f, (sp.rgb >> 8 & 255) / 255f, (sp.rgb & 255) / 255f, a, 0f);
        }
        buffers.endBatch(RenderType.lightning());
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
        if (mc.level == null || (STREAKS.isEmpty() && DECALS.isEmpty() && BEAMS.isEmpty() && SPARKS.isEmpty()))
            return;
        Vec3 cam = event.getCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        drawDecals(mc, pose, buffers, partial);
        drawStreaks(pose.last().pose(), buffers, cam, partial, event.getCamera());
        drawBeams(pose.last().pose(), buffers);
        drawSparks(pose.last().pose(), buffers, partial);
        pose.popPose();
    }

    private static void drawStreaks(Matrix4f m, MultiBufferSource.BufferSource buffers, Vec3 cam, float partial, net.minecraft.client.Camera camera) {
        if (STREAKS.isEmpty())
            return;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        for (Streak s : STREAKS.values()) {
            Vec3 head = s.prev.lerp(s.pos, s.end != null ? 1.0 : partial);
            if (s.style != 0) {
                Vector3f l = camera.getLeftVector(), u = camera.getUpVector();
                Vec3 right = new Vec3(-l.x, -l.y, -l.z), up = new Vec3(u.x, u.y, u.z);
                Vec3 back = head.subtract(s.vel == null ? Vec3.ZERO : s.vel.scale(2.5));
                double near = Mth.clamp((head.distanceTo(cam) - 0.4) / 1.5, 0, 1);
                if (s.fade <= STREAK_FADE_TICKS && near > 0)
                    blob(m, vc, bent(s, head), bent(s, back), right, up, (s.argb >> 16 & 255) / 255f, (s.argb >> 8 & 255) / 255f, (s.argb & 255) / 255f,
                            (float) near * (1 - s.fade / (float) (STREAK_FADE_TICKS + 1)));
                continue;
            }
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
            beam(m, vc, head, tail, dir, width, cr, cg, cb, a, 0f);
            beam(m, vc, head, tail, dir, width * 0.4f, 1f, 1f, 0.95f, a * 0.9f, 0f);
        }
        buffers.endBatch(RenderType.lightning());
    }

    /** A long square beam from head to tail: four faces, opaque at the head and clear at the tail. */
    private static void beam(Matrix4f m, VertexConsumer vc, Vec3 head, Vec3 tail, Vec3 dir, float w, float r, float g, float b, float a, float aTail) {
        Vec3 u = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : dir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 v = dir.cross(u).normalize().scale(w);
        u = u.scale(w);
        Vec3[] ring = {u.add(v), u.subtract(v), u.reverse().subtract(v), u.reverse().add(v)};
        for (int k = 0; k < 4; k++) {
            Vec3 c0 = ring[k], c1 = ring[(k + 1) % 4];
            quadV(m, vc, head.add(c0), r, g, b, a);
            quadV(m, vc, head.add(c1), r, g, b, a);
            quadV(m, vc, tail.add(c1), r, g, b, aTail);
            quadV(m, vc, tail.add(c0), r, g, b, aTail);
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
                double h = DECAL_PX[kind] / 32.0 * d.size;
                int light = LevelRenderer.getLightColor(mc.level, BlockPos.containing(d.pos.add(n.scale(0.1))));
                vertex(vc, p, d.pos.subtract(ru.scale(h)).subtract(rv.scale(h)), 0, 1, alpha, light, n, kind == DECAL_SCORCH ? 0.2f : 1f);
                vertex(vc, p, d.pos.add(ru.scale(h)).subtract(rv.scale(h)), 1, 1, alpha, light, n, kind == DECAL_SCORCH ? 0.2f : 1f);
                vertex(vc, p, d.pos.add(ru.scale(h)).add(rv.scale(h)), 1, 0, alpha, light, n, kind == DECAL_SCORCH ? 0.2f : 1f);
                vertex(vc, p, d.pos.subtract(ru.scale(h)).add(rv.scale(h)), 0, 0, alpha, light, n, kind == DECAL_SCORCH ? 0.2f : 1f);
            }
            if (vc != null)
                buffers.endBatch(RenderType.entityTranslucent(DECAL_TEX[kind]));
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, Vec3 at, float u, float v, float alpha, int light, Vec3 n, float g) {
        vc.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(g, g, g, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }
}
