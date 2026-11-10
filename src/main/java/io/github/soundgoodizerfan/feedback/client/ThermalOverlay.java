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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4fStack;
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
            blobs(g, mc, delta, w, h);
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

    /** Forces every vertex a model writes to one flat colour, full bright, on the white texel; a name tag or a glint is dropped. */
    private static final class Flat implements MultiBufferSource {
        private final MultiBufferSource.BufferSource real;
        float r, g, b;

        Flat(MultiBufferSource.BufferSource real) {
            this.real = real;
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            String n = type.toString();
            if (n.contains("text") || n.contains("glint") || n.contains("line") || n.contains("shadow") || n.contains("debug"))
                return NOOP;
            return new Tint(real.getBuffer(RenderType.entityTranslucent(SOLID)), r, g, b);
        }
    }

    private static final VertexConsumer NOOP = new VertexConsumer() {
        @Override public VertexConsumer addVertex(float x, float y, float z) { return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
    };

    private record Tint(VertexConsumer out, float r, float g, float b) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { out.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int cr, int cg, int cb, int ca) {
            // the model's own shading (face brightness) survives as a grey multiplier, so edges and folds read
            float k = (cr + cg + cb) / 765f;
            out.setColor(Math.round(r * k * 255f), Math.round(g * k * 255f), Math.round(b * k * 255f), 235);
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) { out.setUv(0.5f, 0.5f); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { out.setUv1(0, 10); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { out.setUv2(240, 240); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { out.setNormal(x, y, z); return this; }
    }

    /**
     * Traces every visible thing as its own model, flat-lit in its temperature's colour: a block is its real baked model, a mob or a dropped
     * item is drawn by its own renderer. Drawn from the GUI layer (so the cooling wash does not swallow it) with the world's camera and depth,
     * so a thing behind a corner is still cut by it. Lava, which has no model of its own, stays a soft glow.
     */
    private static void blobs(GuiGraphics g, Minecraft mc, DeltaTracker delta, int w, int h) {
        if (!haveCamera)
            return;
        float partial = delta.getGameTimeDeltaPartialTick(false);
        var bs = mc.renderBuffers().bufferSource();
        Flat flat = new Flat(bs);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        Matrix4f oldProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        var oldSort = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(PROJ, VertexSorting.DISTANCE_TO_ORIGIN);
        mv.pushMatrix();
        mv.set(VIEW);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        List<Entry> glow = new ArrayList<>();
        var dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        boolean hitboxes = dispatcher.shouldRenderHitBoxes();
        dispatcher.setRenderHitBoxes(false);
        List<Entry> list = seen;
        for (int i = 0; i < list.size() && i < visible.length; i++) {
            if (!visible[i])
                continue;
            Entry e = list.get(i);
            float[] c = palette(e.tu());
            flat.r = c[0];
            flat.g = c[1];
            flat.b = c[2];
            PoseStack ps = new PoseStack();
            boolean traced = false;
            if (e.kind() == 0) {
                BlockPos pos = BlockPos.containing(e.x(), e.y(), e.z());
                BlockState state = mc.level.getBlockState(pos);
                if (state.getRenderShape() == RenderShape.MODEL) {
                    ps.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);
                    // a hair larger than the block so the trace lies over the real surface instead of z-fighting with it
                    ps.translate(0.5, 0.5, 0.5);
                    ps.scale(1.004f, 1.004f, 1.004f);
                    ps.translate(-0.5, -0.5, -0.5);
                    mc.getBlockRenderer().renderSingleBlock(state, ps, flat, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
                    traced = true;
                }
            } else if ((e.kind() == 1 || e.kind() == 2) && e.id() >= 0) {
                Entity ent = mc.level.getEntity(e.id());
                if (ent != null) {
                    Vec3 at = ent.getPosition(partial);
                    dispatcher.render(ent, at.x - camPos.x, at.y - camPos.y, at.z - camPos.z, Mth.lerp(partial, ent.yRotO, ent.getYRot()), partial, ps, flat,
                            LightTexture.FULL_BRIGHT);
                    traced = true;
                }
            }
            if (!traced)
                glow.add(e);
        }
        bs.endBatch();
        dispatcher.setRenderShadow(true);
        dispatcher.setRenderHitBoxes(hitboxes);
        mv.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(oldProj, oldSort);
        RenderSystem.disableDepthTest();

        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float focal = h / (2f * (float) Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0));
        float[] p = new float[3];
        for (Entry e : glow) {
            if (!project(e.x(), e.y(), e.z(), w, h, p))
                continue;
            float dist = Math.max(0.5f, p[2]);
            float px = Mth.clamp(e.size() * focal / dist * 2.6f, 3f, h * 0.9f);
            float[] c = palette(e.tu());
            int half = (int) (px / 2f);
            quad(g, BLOB, (int) p[0] - half, (int) p[1] - half, half * 2, half * 2, c[0], c[1], c[2], 0.7f, 64, 64);
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
