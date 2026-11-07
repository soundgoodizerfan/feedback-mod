// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/**
 * The pan's picture: a glow over its face that follows its own heat, and each piece as its item over a patch that
 * browns, then blackens. The gap between the glow and the patch is the food's lag behind the pan. Everything comes from
 * the block entity's synced figures; nothing is computed per frame beyond a lerp.
 */
public class SkilletRenderer implements BlockEntityRenderer<SkilletBlockEntity> {

    private static final ResourceLocation BODY = ResourceLocation.parse("feedback:block/skillet_body");
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final int RAW = 0xE08A8A, COOKED = 0x9A5B2E, BURNT = 0x14100C;
    private static final int GLOW = 0xFF5A1A, HANDLE = 0x4A3524;
    private static final float FLIP_TICKS = 6f;
    private static final float FLOOR = 3f / 16f + 0.002f;
    /** Pan heat (Tu) at which the face is fully glowing; below the room it does not glow at all. */
    private static final float GLOW_FULL_TU = 400f, GLOW_START_TU = 60f;

    public SkilletRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(SkilletBlockEntity pan) {
        return new AABB(pan.getBlockPos()).inflate(1.0, 1.0, 1.0);
    }

    @Override
    public void render(SkilletBlockEntity pan, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float heat = Mth.clamp((pan.shownPan() - GLOW_START_TU) / (GLOW_FULL_TU - GLOW_START_TU), 0f, 1f);
        pan.liftAnim += ((pan.shownLift() ? 1f : 0f) - pan.liftAnim) * 0.25f;
        float lift = pan.liftAnim;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
        TextureAtlasSprite body = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(BODY);
        VertexConsumer quads = buffers.getBuffer(RenderType.translucent());
        int metal = pan.shownMaterial() == null ? 0x707070 : MaterialColors.of(pan.shownMaterial());
        int rim = lerp(metal, GLOW, heat * 0.85f);
        long now = pan.getLevel() == null ? 0L : pan.getLevel().getGameTime();

        pose.pushPose();
        pose.translate(0.5f, 0.45f * lift, 0.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(10f * lift));
        pose.translate(-0.5f, 0f, -0.5f);
        Matrix4f m = pose.last().pose();
        // The pan: body, a rim that glows with its heat, a wooden handle out of the +x side.
        box(quads, m, body, 1f / 16f, 0f, 1f / 16f, 15f / 16f, 3f / 16f, 15f / 16f, metal, light);
        box(quads, m, body, 1f / 16f, 3f / 16f, 1f / 16f, 15f / 16f, 3.8f / 16f, 2f / 16f, rim, light);
        box(quads, m, body, 1f / 16f, 3f / 16f, 14f / 16f, 15f / 16f, 3.8f / 16f, 15f / 16f, rim, light);
        box(quads, m, body, 1f / 16f, 3f / 16f, 2f / 16f, 2f / 16f, 3.8f / 16f, 14f / 16f, rim, light);
        box(quads, m, body, 14f / 16f, 3f / 16f, 2f / 16f, 15f / 16f, 3.8f / 16f, 14f / 16f, rim, light);
        box(quads, m, sprite, 15f / 16f, 1.6f / 16f, 7.3f / 16f, 23f / 16f, 2.6f / 16f, 8.7f / 16f, HANDLE, light);
        if (heat > 0f)
            quad(quads, m, sprite, 2f / 16f, 2f / 16f, 14f / 16f, 14f / 16f, FLOOR, GLOW, 0.75f * heat, light);
        for (int i = 0; i < SkilletBlockEntity.SLOTS; i++) {
            if (pan.shownItem(i) == null)
                continue;
            float cx = (i & 1) == 0 ? 0.30f : 0.70f, cz = (i & 2) == 0 ? 0.30f : 0.70f;
            int colour = lerp(lerp(RAW, COOKED, pan.shownCook(i)), BURNT, pan.shownChar(i));
            quad(quads, m, sprite, cx - 0.17f, cz - 0.17f, cx + 0.17f, cz + 0.17f, FLOOR + 0.002f, colour, 0.9f, light);
        }
        for (int i = 0; i < SkilletBlockEntity.SLOTS; i++) {
            ItemStack stack = pan.shownStack(i);
            if (stack.isEmpty())
                continue;
            // A flip: the piece pops up and rolls over in a few ticks.
            float t = Mth.clamp((now + partialTick - pan.flipAt(i)) / FLIP_TICKS, 0f, 1f);
            pose.pushPose();
            pose.translate((i & 1) == 0 ? 0.30f : 0.70f, FLOOR + 0.02f + Mth.sin(t * Mth.PI) * 0.25f, (i & 2) == 0 ? 0.30f : 0.70f);
            pose.mulPose(Axis.ZP.rotationDegrees(180f * t));
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.scale(0.3f, 0.3f, 0.3f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose,
                    buffers, pan.getLevel(), i);
            pose.popPose();
        }
        pose.popPose();
    }

    /** An open-bottomed box of one colour, each side shaded by which way it faces. */
    static void box(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light) {
        quad(c, m, s, x0, z0, x1, z1, y1, rgb, 1f, light);
        side(c, m, s, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, shade(rgb, 0.65f), light, y1 - y0);
        side(c, m, s, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, shade(rgb, 0.65f), light, y1 - y0);
        side(c, m, s, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, shade(rgb, 0.8f), light, y1 - y0);
        side(c, m, s, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, shade(rgb, 0.8f), light, y1 - y0);
    }

    /** One side face; its texture is mapped by position, so a thin strip shows the slice of the texture that is really there. */
    private static void side(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float nx, float ny, float nz, int rgb, int light, float h) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        boolean alongZ = nx != 0f;
        vertex(c, m, s, ax, ay, az, alongZ ? az : ax, r, g, b, 255, light, nx, ny, nz);
        vertex(c, m, s, bx, by, bz, alongZ ? bz : bx, r, g, b, 255, light, nx, ny, nz);
        vertex(c, m, s, cx, cy, cz, alongZ ? cz : cx, r, g, b, 255, light, nx, ny, nz);
        vertex(c, m, s, dx, dy, dz, alongZ ? dz : dx, r, g, b, 255, light, nx, ny, nz);
    }

    /** u follows {@code across} (0 to 1 over the block), v follows height, both clamped to the sprite. */
    private static void vertex(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, float x, float y, float z, float across,
                               int r, int g, int b, int a, int light, float nx, float ny, float nz) {
        float u = Mth.lerp(Mth.clamp(across, 0f, 1f), s.getU0(), s.getU1());
        float v = Mth.lerp(Mth.clamp(y, 0f, 1f), s.getV1(), s.getV0());
        c.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setLight(light).setNormal(nx, ny, nz);
    }

    static int shade(int rgb, float k) {
        return (int) ((rgb >> 16 & 255) * k) << 16 | (int) ((rgb >> 8 & 255) * k) << 8 | (int) ((rgb & 255) * k);
    }

    /** A flat top face; the texture is mapped by position over the block, so a strip shows the slice that is there. */
    static void quad(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, float x0, float z0, float x1, float z1, float y, int rgb, float alpha, int light) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, a = (int) (Mth.clamp(alpha, 0f, 1f) * 255f);
        float u0 = Mth.lerp(Mth.clamp(x0, 0f, 1f), s.getU0(), s.getU1()), u1 = Mth.lerp(Mth.clamp(x1, 0f, 1f), s.getU0(), s.getU1());
        float v0 = Mth.lerp(Mth.clamp(z0, 0f, 1f), s.getV0(), s.getV1()), v1 = Mth.lerp(Mth.clamp(z1, 0f, 1f), s.getV0(), s.getV1());
        c.addVertex(m, x0, y, z0).setColor(r, g, b, a).setUv(u0, v0).setLight(light).setNormal(0f, 1f, 0f);
        c.addVertex(m, x0, y, z1).setColor(r, g, b, a).setUv(u0, v1).setLight(light).setNormal(0f, 1f, 0f);
        c.addVertex(m, x1, y, z1).setColor(r, g, b, a).setUv(u1, v1).setLight(light).setNormal(0f, 1f, 0f);
        c.addVertex(m, x1, y, z0).setColor(r, g, b, a).setUv(u1, v0).setLight(light).setNormal(0f, 1f, 0f);
    }

    static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) (Mth.lerp(t, a >> 16 & 255, b >> 16 & 255));
        int g = (int) (Mth.lerp(t, a >> 8 & 255, b >> 8 & 255));
        int bl = (int) (Mth.lerp(t, a & 255, b & 255));
        return r << 16 | g << 8 | bl;
    }
}
