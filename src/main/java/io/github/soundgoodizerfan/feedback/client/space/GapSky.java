// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.space;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.space.AcidRoute;
import io.github.soundgoodizerfan.feedback.space.EndRoute;
import io.github.soundgoodizerfan.feedback.space.SpaceTuning;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Gap's sky, drawn in place of vanilla's: a hard square sun, a dense starfield with a milky band, the Overworld's atmosphere as a
 * limb glow below the horizon, and the destination planets at fixed bearings: the sun a flat pixel-art square as vanilla draws its sun, each planet a slowly turning cube, as
 * most space mods draw them, with a hand-drawn bird's-eye map ({@code textures/environment/gap/}) on its faces. Everything sits on a sphere
 * around the camera, so it costs a few thousand vertices a frame. The explored map plane ({@link GapMapView}) is drawn over the lower
 * glow afterwards, at the chunks' real coordinates.
 * <p>
 * shortcut: the star mesh is rebuilt every frame through the Tesselator, upload it once into a VertexBuffer if the sky ever shows up in a profile.
 */
final class GapSky {
    /** The sphere the sky is painted on, blocks from the camera; depth test is off, so only the draw order matters. */
    private static final float R = 100f;
    private static final int STARS = 2600, BAND_STARS = 3400;
    private static final long SEED = 0x6A9C0DEL;

    /** The sun's bearing, and the size of its square. */
    private static final Vector3f SUN = dir(40f, 35f);
    private static final float SUN_SIZE = 6f;

    private static ResourceLocation sprite(String name) {
        return Feedback.id("textures/environment/gap/" + name + ".png");
    }

    private static final ResourceLocation SUN_TEXTURE = sprite("sun");

    /** A destination at a fixed bearing: angular size is {@code radius / R}. */
    private record Planet(float yaw, float pitch, float radius, ResourceLocation texture) {
    }

    private static final Planet[] PLANETS = {
            new Planet(112f, 27f, 4f, sprite("frozen_moon")),
            new Planet(305f, 41f, 3f, sprite("crystal")),
    };

    /** The End's gas giant: a real place in the Gap ({@link EndRoute#planet}), so it is drawn where it is, and grows as the craft closes on it. */
    private static final ResourceLocation GAS_GIANT = sprite("gas_giant");
    private static final float GAS_GIANT_SIZE = 5.5f;
    /** The acid-cloud world: a real place in the Gap too ({@link AcidRoute#planet}), drawn where it is and growing as a craft closes on it. */
    private static final ResourceLocation ACID_CLOUD = sprite("acid_cloud");
    private static final float ACID_SIZE = 7f;
    /** The most its apparent size grows to, so a craft inside the capture sphere sees it fill much of the sky, not all of it. */
    private static final float GROWTH_CAP = 10f;
    /** Half-width of a planet's cube against its nominal radius: the cube's corners then reach about the radius. */
    private static final float CUBE_SHARE = 0.8f;
    /** Radians a planet turns each tick: a full turn in about a minute. */
    private static final float SPIN = 0.005f;

    private static float[] stars; // x, y, z, size, r, g, b, a per star

    private GapSky() {
    }

    /** Draws the whole sky. {@code view} is the camera's rotation, so directions here are fixed in the world. */
    static void render(Matrix4f view, Vec3 camera, float time) {
        if (stars == null)
            stars = buildStars();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // Light that adds: atmosphere and stars.
        RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        BufferBuilder add = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        limb(add, view);
        stars(add, view);
        BufferUploader.drawWithShader(add.buildOrThrow());

        // Sprites over it: the planets, then the sun.
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        for (Planet p : PLANETS)
            cube(view, p.texture, dir(p.yaw, p.pitch), p.radius * CUBE_SHARE, false, time * SPIN * (1f + p.yaw % 3f / 3f) + p.yaw);
        Vector3d to = EndRoute.planet().sub(camera.x, camera.y, camera.z);
        double away = to.length();
        if (away > 1e-3)
            cube(view, GAS_GIANT, new Vector3f((float) (to.x / away), (float) (to.y / away), (float) (to.z / away)),
                    GAS_GIANT_SIZE * CUBE_SHARE * (float) Math.min(GROWTH_CAP, SpaceTuning.END_DISTANCE / away), true, time * SPIN);
        Vector3d toAcid = AcidRoute.planet().sub(camera.x, camera.y, camera.z);
        double acidAway = toAcid.length();
        if (acidAway > 1e-3)
            cube(view, ACID_CLOUD, new Vector3f((float) (toAcid.x / acidAway), (float) (toAcid.y / acidAway), (float) (toAcid.z / acidAway)),
                    ACID_SIZE * CUBE_SHARE * (float) Math.min(GROWTH_CAP, SpaceTuning.ACID_DISTANCE / acidAway), false, time * SPIN * 1.4f);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        sprite(view, SUN_TEXTURE, SUN, SUN_SIZE);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    // ---- the Overworld below ----

    /** Atmosphere seen from above: a bright blue band at the horizon, thinning to nothing straight down, a trace above it. */
    private static void limb(BufferBuilder b, Matrix4f m) {
        float[] pitch = { 7f, 0f, -6f, -18f, -40f, -90f };
        float[] alpha = { 0f, 0.55f, 0.42f, 0.18f, 0.06f, 0.03f };
        int segs = 64;
        for (int i = 0; i < pitch.length - 1; i++)
            for (int s = 0; s < segs; s++) {
                float ya = s * 360f / segs, yb = (s + 1) * 360f / segs;
                vert(b, m, dir(ya, pitch[i]), tint(alpha[i], i == 1 ? 0xFFB5DBFF : 0xFF4C8FEA));
                vert(b, m, dir(yb, pitch[i]), tint(alpha[i], i == 1 ? 0xFFB5DBFF : 0xFF4C8FEA));
                vert(b, m, dir(yb, pitch[i + 1]), tint(alpha[i + 1], 0xFF3A78D8));
                vert(b, m, dir(ya, pitch[i + 1]), tint(alpha[i + 1], 0xFF3A78D8));
            }
    }

    // ---- stars ----

    private static float[] buildStars() {
        Random rnd = new Random(SEED);
        float[] out = new float[(STARS + BAND_STARS) * 8];
        Vector3f n = new Vector3f(0.35f, 0.8f, 0.5f).normalize();
        Vector3f e1 = new Vector3f(n).cross(0f, 1f, 0f).normalize();
        Vector3f e2 = new Vector3f(n).cross(e1).normalize();
        int[] tints = { 0xFFB4C8FF, 0xFFFFFFFF, 0xFFFFF0D2, 0xFFFFC8A0 };
        for (int i = 0; i < STARS + BAND_STARS; i++) {
            Vector3f d;
            float b;
            if (i < STARS) {
                d = new Vector3f(rnd.nextFloat() * 2 - 1, rnd.nextFloat() * 2 - 1, rnd.nextFloat() * 2 - 1);
                if (d.lengthSquared() > 1f || d.lengthSquared() < 1e-4f) {
                    i--;
                    continue;
                }
                d.normalize();
                b = (float) (Math.pow(rnd.nextFloat(), 3) * 0.8 + 0.2);
            } else {
                // A great circle, thickened by a bell: the galaxy seen edge on, many faint stars.
                double t = rnd.nextDouble() * Math.PI * 2;
                d = new Vector3f(e1).mul((float) Math.cos(t)).add(new Vector3f(e2).mul((float) Math.sin(t)))
                        .add(new Vector3f(n).mul((float) (rnd.nextGaussian() * 0.11))).normalize();
                b = 0.15f + rnd.nextFloat() * 0.3f;
            }
            int c = tints[rnd.nextInt(tints.length)];
            int o = i * 8;
            out[o] = d.x;
            out[o + 1] = d.y;
            out[o + 2] = d.z;
            out[o + 3] = 0.15f + 0.4f * b;
            out[o + 4] = (c >> 16 & 255) / 255f;
            out[o + 5] = (c >> 8 & 255) / 255f;
            out[o + 6] = (c & 255) / 255f;
            out[o + 7] = Math.min(1f, b);
        }
        return out;
    }

    private static void stars(BufferBuilder b, Matrix4f m) {
        for (int o = 0; o < stars.length; o += 8) {
            Vector3f d = new Vector3f(stars[o], stars[o + 1], stars[o + 2]);
            float s = stars[o + 3];
            int c = argb(stars[o + 7], stars[o + 4], stars[o + 5], stars[o + 6]);
            Vector3f r = side(d);
            Vector3f u = new Vector3f(d).cross(r);
            quadPoint(b, m, d, r, u, s, c);
        }
    }

    private static void quadPoint(BufferBuilder b, Matrix4f m, Vector3f d, Vector3f r, Vector3f u, float s, int c) {
        float cx = d.x * R, cy = d.y * R, cz = d.z * R;
        b.addVertex(m, cx - r.x * s - u.x * s, cy - r.y * s - u.y * s, cz - r.z * s - u.z * s).setColor(c);
        b.addVertex(m, cx + r.x * s - u.x * s, cy + r.y * s - u.y * s, cz + r.z * s - u.z * s).setColor(c);
        b.addVertex(m, cx + r.x * s + u.x * s, cy + r.y * s + u.y * s, cz + r.z * s + u.z * s).setColor(c);
        b.addVertex(m, cx - r.x * s + u.x * s, cy - r.y * s + u.y * s, cz - r.z * s + u.z * s).setColor(c);
    }

    // ---- sun and planets ----

    /** A square of half-width {@code half} facing the camera at bearing {@code c}, with {@code texture} on it. */
    private static void sprite(Matrix4f m, ResourceLocation texture, Vector3f c, float half) {
        Vector3f r = side(c).mul(half);
        Vector3f u = new Vector3f(c).cross(side(c)).mul(half);
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float cx = c.x * R, cy = c.y * R, cz = c.z * R;
        b.addVertex(m, cx - r.x - u.x, cy - r.y - u.y, cz - r.z - u.z).setUv(0f, 1f);
        b.addVertex(m, cx + r.x - u.x, cy + r.y - u.y, cz + r.z - u.z).setUv(1f, 1f);
        b.addVertex(m, cx + r.x + u.x, cy + r.y + u.y, cz + r.z + u.z).setUv(1f, 0f);
        b.addVertex(m, cx - r.x + u.x, cy - r.y + u.y, cz - r.z + u.z).setUv(0f, 0f);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** A cube face: its outward normal, and the two edges of its texture (right, up as the map is read). */
    private static final Vector3f[][] FACES = {
            { new Vector3f(1, 0, 0), new Vector3f(0, 0, -1), new Vector3f(0, 1, 0) },
            { new Vector3f(-1, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 1, 0) },
            { new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, -1) },
            { new Vector3f(0, -1, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 1) },
            { new Vector3f(0, 0, 1), new Vector3f(1, 0, 0), new Vector3f(0, 1, 0) },
            { new Vector3f(0, 0, -1), new Vector3f(-1, 0, 0), new Vector3f(0, 1, 0) },
    };

    /**
     * A planet: a cube of half-width {@code half} at bearing {@code c}, turning slowly, with {@code texture} (a bird's-eye map with its
     * own dithered light baked in) on all six faces, each face shaded flat by how square it faces the sun, as Minecraft shades blocks.
     * A {@code gas} planet is see-through: all six faces are drawn, the far ones first, so its islands show through the gas.
     */
    private static void cube(Matrix4f m, ResourceLocation texture, Vector3f c, float half, boolean gas, float angle) {
        org.joml.Quaternionf turn = new org.joml.Quaternionf().rotateAxis(angle, new Vector3f(0.3f, 1f, 0.2f).normalize());
        RenderSystem.setShaderTexture(0, texture);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int pass = gas ? 0 : 1; pass < 2; pass++)
            for (Vector3f[] face : FACES) {
                Vector3f n = turn.transform(new Vector3f(face[0]));
                boolean near = n.dot(c) < 0; // the face looks back at the camera
                if (near != (pass == 1))
                    continue;
                Vector3f r = turn.transform(new Vector3f(face[1])), u = turn.transform(new Vector3f(face[2]));
                float lit = 0.45f + 0.55f * Math.max(0f, n.dot(SUN));
                for (int k = 0; k < 4; k++) {
                    float sx = k == 1 || k == 2 ? 1f : -1f, sy = k >= 2 ? 1f : -1f;
                    b.addVertex(m, c.x * R + (n.x + r.x * sx + u.x * sy) * half, c.y * R + (n.y + r.y * sx + u.y * sy) * half,
                            c.z * R + (n.z + r.z * sx + u.z * sy) * half)
                            .setUv((sx + 1f) / 2f, (1f - sy) / 2f).setColor(lit, lit, lit, 1f);
                }
            }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    // ---- geometry ----

    private static void vert(BufferBuilder b, Matrix4f m, Vector3f d, int colour) {
        b.addVertex(m, d.x * R, d.y * R, d.z * R).setColor(colour);
    }

    /** A unit vector: {@code yaw} turns about the vertical, {@code pitch} 0 is the horizon, 90 straight up. */
    private static Vector3f dir(float yawDeg, float pitchDeg) {
        double y = Math.toRadians(yawDeg), p = Math.toRadians(pitchDeg);
        return new Vector3f((float) (Math.cos(p) * Math.sin(y)), (float) Math.sin(p), (float) (-Math.cos(p) * Math.cos(y)));
    }

    /** A unit vector at right angles to {@code d} and level (any vector when {@code d} is vertical). */
    private static Vector3f side(Vector3f d) {
        Vector3f r = new Vector3f(d).cross(0f, 1f, 0f);
        return r.lengthSquared() < 1e-6f ? new Vector3f(1f, 0f, 0f) : r.normalize();
    }

    private static int argb(float a, float r, float g, float b) {
        return (Math.round(Math.min(1f, a) * 255) << 24) | (Math.round(Math.min(1f, r) * 255) << 16)
                | (Math.round(Math.min(1f, g) * 255) << 8) | Math.round(Math.min(1f, b) * 255);
    }

    /** {@code colour}'s rgb at {@code alpha}. */
    private static int tint(float alpha, int colour) {
        return (Math.round(Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | (colour & 0xFFFFFF);
    }

}
