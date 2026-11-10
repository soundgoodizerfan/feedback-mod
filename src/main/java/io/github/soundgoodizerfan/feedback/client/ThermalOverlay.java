// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.SuitSyncPayload;
import io.github.soundgoodizerfan.feedback.net.ThermalFeedPayload.Entry;
import io.github.soundgoodizerfan.feedback.suit.Ability;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * The suit's thermal view and night sight, drawn over the world under the HUD. Thermal sight is a false-colour image of the temperatures the server
 * sends ({@link ThermalView}): the world is cooled to a dim indigo, scan-lined and vignetted, and every warm or cold thing glows in the iron-bow
 * palette at its real {@code Tu}, a soft additive bloom the size of the body (a hot ingot a spark, lava a furnace-mouth, a creeper barely above the
 * room, a blaze nearly white). Bodies behind a wall are not shown: the client casts a ray to each a few times a second and keeps only those it can
 * see, because a thermal camera does not see through stone. The reticle names the temperature of what is nearest the crosshair, as a figure or a
 * word as the suit's gauges allow ({@link SuitReadout}). Night sight is a phosphor wash with grain and a vignette; the brightening itself is
 * the server's night-vision effect.
 *
 * <p>Blobs are blitted from a radial falloff drawn once into a dynamic texture, additively; the cooling is one multiply blit. Everything is a
 * handful of textured quads a frame, and nothing runs when the view is off.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
final class ThermalOverlay {

    private ThermalOverlay() {
    }

    private static DynamicTexture blob, solid;
    private static final ResourceLocation BLOB = Feedback.id("thermal_blob"), SOLID = Feedback.id("thermal_solid");

    private static final Matrix4f VIEW = new Matrix4f(), PROJ = new Matrix4f();
    private static Vec3 camPos = Vec3.ZERO;
    private static boolean haveCamera;
    private static boolean[] visible = new boolean[0];
    private static List<Entry> seen = List.of();

    static void register(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.HOTBAR, Feedback.id("thermal_overlay"), ThermalOverlay::render);
    }

    // --- the camera and the line of sight ------------------------------------------------------------

    @SubscribeEvent
    static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;
        Camera cam = event.getCamera();
        camPos = cam.getPosition();
        VIEW.set(event.getModelViewMatrix());
        PROJ.set(event.getProjectionMatrix());
        haveCamera = true;
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !SuitClient.live(Ability.THERMAL)) {
            seen = List.of();
            return;
        }
        if (mc.player.tickCount % 3 != 0 && seen == ThermalView.entries())
            return;
        List<Entry> list = ThermalView.entries();
        boolean[] vis = new boolean[list.size()];
        Vec3 eye = mc.player.getEyePosition();
        for (int i = 0; i < vis.length; i++) {
            Entry e = list.get(i);
            Vec3 at = new Vec3(e.x(), e.y(), e.z());
            var hit = mc.level.clip(new ClipContext(eye, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
            // seen if nothing solid is in the way, or the wall is only the thing's own block
            vis[i] = hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(at) < Math.max(1.0, e.size() * e.size() * 0.6);
        }
        visible = vis;
        seen = list;
    }

    // --- palette ------------------------------------------------------------------------------------------

    private static final float[] STOP_TU = { -40f, 0f, 20f, 37f, 80f, 200f, 600f, 1200f };
    private static final float[][] STOP_RGB = {
            { 0.02f, 0.02f, 0.16f }, { 0.10f, 0.04f, 0.38f }, { 0.30f, 0.05f, 0.46f }, { 0.85f, 0.12f, 0.28f },
            { 1.00f, 0.46f, 0.05f }, { 1.00f, 0.86f, 0.20f }, { 1.00f, 0.97f, 0.72f }, { 1.00f, 1.00f, 1.00f } };

    /** The iron-bow colour of a temperature: deep indigo cold, violet at room, red-pink at body heat, orange, yellow, white-hot. */
    static float[] palette(float tu) {
        if (tu <= STOP_TU[0])
            return STOP_RGB[0];
        for (int i = 1; i < STOP_TU.length; i++)
            if (tu <= STOP_TU[i]) {
                float f = (tu - STOP_TU[i - 1]) / (STOP_TU[i] - STOP_TU[i - 1]);
                float[] a = STOP_RGB[i - 1], b = STOP_RGB[i];
                return new float[] { Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2]) };
            }
        return STOP_RGB[STOP_RGB.length - 1];
    }

    // --- textures -----------------------------------------------------------------------------------------

    private static void textures() {
        if (blob != null)
            return;
        NativeImage b = new NativeImage(64, 64, false);
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++) {
                float d = (float) Math.sqrt((x - 31.5) * (x - 31.5) + (y - 31.5) * (y - 31.5)) / 32f, a = Math.max(0f, 1f - d);
                a = a * a * (3f - 2f * a); // smoothstep: a hot core fading to nothing at the rim
                b.setPixelRGBA(x, y, (Math.round(a * 255) << 24) | 0x00FFFFFF);
            }
        blob = new DynamicTexture(b);
        NativeImage s = new NativeImage(4, 4, false);
        for (int y = 0; y < 4; y++)
            for (int x = 0; x < 4; x++)
                s.setPixelRGBA(x, y, 0xFFFFFFFF);
        solid = new DynamicTexture(s);
        var tm = Minecraft.getInstance().getTextureManager();
        tm.register(BLOB, blob);
        tm.register(SOLID, solid);
    }

    // --- drawing ------------------------------------------------------------------------------------------

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui && false)
            return;
        boolean thermal = SuitClient.live(Ability.THERMAL), night = SuitClient.live(Ability.NIGHT);
        if (!thermal && !night)
            return;
        textures();
        int w = g.guiWidth(), h = g.guiHeight();
        long now = net.minecraft.Util.getMillis();
        RenderSystem.enableBlend();
        if (thermal) {
            // cool the world: multiply it toward indigo, then lift the floor so shadow is blue and not black
            RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
            quad(g, SOLID, 0, 0, w, h, 0.30f, 0.26f, 0.62f, 1f, 4, 4);
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            quad(g, SOLID, 0, 0, w, h, 0.015f, 0.012f, 0.06f, 1f, 4, 4);
            blobs(g, mc, w, h);
            // scanlines, drifting slowly, and a grain of faint lines
            RenderSystem.defaultBlendFunc();
            int drift = (int) (now / 90 % 3);
            for (int y = drift; y < h; y += 3)
                quad(g, SOLID, 0, y, w, 1, 0f, 0f, 0.03f, 0.16f, 4, 4);
            reticle(g, mc, w, h, now);
        } else {
            // night sight: a phosphor wash
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            quad(g, SOLID, 0, 0, w, h, 0.02f, 0.07f, 0.03f, 1f, 4, 4);
            RenderSystem.defaultBlendFunc();
            int drift = (int) (now / 70 % 4);
            for (int y = drift; y < h; y += 4)
                quad(g, SOLID, 0, y, w, 1, 0f, 0.05f, 0f, 0.10f, 4, 4);
        }
        vignette(g, w, h, thermal);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void quad(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, float r, float gg, float b, float a, int tw, int th) {
        RenderSystem.setShaderColor(r, gg, b, a);
        g.blit(tex, x, y, w, h, 0, 0, tw, th, tw, th);
    }

    /** Darkened corners: four edge strips of falling alpha, so the picture reads as seen through a lens. */
    private static void vignette(GuiGraphics g, int w, int h, boolean thermal) {
        RenderSystem.defaultBlendFunc();
        int depth = Math.max(24, h / 5);
        float tr = thermal ? 0.02f : 0f, tg = thermal ? 0.0f : 0.03f, tb = thermal ? 0.08f : 0f;
        for (int i = 0; i < depth; i += 2) {
            float a = 0.55f * (1f - i / (float) depth) * (1f - i / (float) depth);
            quad(g, SOLID, 0, i, w, 2, tr, tg, tb, a, 4, 4);
            quad(g, SOLID, 0, h - i - 2, w, 2, tr, tg, tb, a, 4, 4);
            quad(g, SOLID, i, 0, 2, h, tr, tg, tb, a * 0.8f, 4, 4);
            quad(g, SOLID, w - i - 2, 0, 2, h, tr, tg, tb, a * 0.8f, 4, 4);
        }
    }

    private static final Vector4f TMP = new Vector4f();

    /** Projects a world point to GUI coordinates; false when it is behind the camera. */
    private static boolean project(double x, double y, double z, int w, int h, float[] out) {
        TMP.set((float) (x - camPos.x), (float) (y - camPos.y), (float) (z - camPos.z), 1f);
        TMP.mul(VIEW);
        TMP.mul(PROJ);
        if (TMP.w <= 0.05f)
            return false;
        out[0] = (TMP.x / TMP.w * 0.5f + 0.5f) * w;
        out[1] = (1f - (TMP.y / TMP.w * 0.5f + 0.5f)) * h;
        out[2] = TMP.w;
        return true;
    }

    private static void blobs(GuiGraphics g, Minecraft mc, int w, int h) {
        if (!haveCamera)
            return;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float focal = h / (2f * (float) Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0));
        float[] p = new float[3];
        List<Entry> list = seen;
        for (int i = 0; i < list.size() && i < visible.length; i++) {
            if (!visible[i])
                continue;
            Entry e = list.get(i);
            if (!project(e.x(), e.y(), e.z(), w, h, p))
                continue;
            float dist = Math.max(0.5f, p[2]);
            float px = e.size() * focal / dist * (e.kind() == 2 ? 1.6f : e.kind() == 3 ? 2.2f : 2.6f);
            px = Mth.clamp(px, 3f, h * 0.9f);
            float[] c = palette(e.tu());
            float intensity = Mth.clamp(Math.abs(e.tu() - 20f) / 40f, 0f, 1f);
            float a = 0.30f + 0.70f * intensity;
            int half = (int) (px / 2f);
            quad(g, BLOB, (int) p[0] - half, (int) p[1] - half, half * 2, half * 2, c[0], c[1], c[2], a, 64, 64);
            if (e.tu() > 60f) { // a hot thing has a hard bright core
                int core = Math.max(2, half / 3);
                quad(g, BLOB, (int) p[0] - core, (int) p[1] - core, core * 2, core * 2, 1f, 0.95f, 0.8f, Math.min(1f, a), 64, 64);
            }
        }
    }

    /** Corner brackets, a label and the temperature of what is nearest the crosshair. */
    private static void reticle(GuiGraphics g, Minecraft mc, int w, int h, long now) {
        int c = 0xC0 << 24 | 0x4FE3FF;
        int cx = w / 2, cy = h / 2, r = 14;
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sy = -1; sy <= 1; sy += 2) {
                g.fill(cx + sx * r - (sx > 0 ? 0 : -5), cy + sy * r, cx + sx * r + (sx > 0 ? -5 : 0), cy + sy * r + 1, c);
                g.fill(cx + sx * r, cy + sy * r - (sy > 0 ? 0 : -5), cx + sx * r + 1, cy + sy * r + (sy > 0 ? -5 : 0), c);
            }
        // the thing nearest the crosshair
        SuitSyncPayload s = SuitClient.state();
        float[] p = new float[3];
        Entry best = null;
        float bestD = 40f * 40f;
        for (int i = 0; i < seen.size() && i < visible.length; i++) {
            if (!visible[i])
                continue;
            Entry e = seen.get(i);
            if (!project(e.x(), e.y(), e.z(), w, h, p))
                continue;
            float d = (p[0] - cx) * (p[0] - cx) + (p[1] - cy) * (p[1] - cy);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        var font = mc.font;
        Component label = Component.translatable("hud.feedback.suit.thermal");
        g.drawString(font, label, 10, 10, 0xFF << 24 | 0x4FE3FF, false);
        g.fill(10, 20, 10 + font.width(label), 21, 0x80 << 24 | 0x4FE3FF);
        if ((now / 500) % 2 == 0)
            g.fill(10 + font.width(label) + 5, 11, 10 + font.width(label) + 9, 15, 0xFF << 24 | 0xFF3A2E);
        if (best != null) {
            Component t = SuitReadout.tu(best.tu(), s);
            g.drawCenteredString(font, t, cx, cy + r + 6, 0xFF << 24 | 0xFFFFFF);
        }
    }
}
