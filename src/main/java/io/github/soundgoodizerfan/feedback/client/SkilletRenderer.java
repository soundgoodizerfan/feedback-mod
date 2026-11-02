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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletBlockEntity;

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

import org.joml.Matrix4f;

/**
 * The pan's picture: a glow over its face that follows its own heat, and each piece as its item over a patch that
 * browns, then blackens. The gap between the glow and the patch is the food's lag behind the pan. Everything comes from
 * the block entity's synced figures; nothing is computed per frame beyond a lerp.
 */
public class SkilletRenderer implements BlockEntityRenderer<SkilletBlockEntity> {

    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final int RAW = 0xE08A8A, COOKED = 0x9A5B2E, BURNT = 0x14100C;
    private static final float FLOOR = 3f / 16f + 0.002f;
    /** Pan heat (Tu) at which the face is fully glowing; below the room it does not glow at all. */
    private static final float GLOW_FULL_TU = 400f, GLOW_START_TU = 60f;

    public SkilletRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SkilletBlockEntity pan, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float heat = Mth.clamp((pan.shownPan() - GLOW_START_TU) / (GLOW_FULL_TU - GLOW_START_TU), 0f, 1f);
        boolean any = false;
        for (int i = 0; i < SkilletBlockEntity.SLOTS; i++)
            any |= pan.shownItem(i) != null;
        if (heat <= 0f && !any)
            return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
        VertexConsumer quads = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        if (heat > 0f)
            quad(quads, m, sprite, 2f / 16f, 2f / 16f, 14f / 16f, 14f / 16f, FLOOR, 0xFF5A1A, 0.75f * heat, light);
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
            pose.pushPose();
            pose.translate((i & 1) == 0 ? 0.30f : 0.70f, FLOOR + 0.02f, (i & 2) == 0 ? 0.30f : 0.70f);
            pose.mulPose(Axis.XP.rotationDegrees(90f));
            pose.scale(0.3f, 0.3f, 0.3f);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose,
                    buffers, pan.getLevel(), i);
            pose.popPose();
        }
    }

    private static void quad(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, float x0, float z0, float x1, float z1, float y, int rgb, float alpha, int light) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, a = (int) (Mth.clamp(alpha, 0f, 1f) * 255f);
        float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
        c.addVertex(m, x0, y, z0).setColor(r, g, b, a).setUv(u0, v0).setLight(light).setNormal(0f, 1f, 0f);
        c.addVertex(m, x0, y, z1).setColor(r, g, b, a).setUv(u0, v1).setLight(light).setNormal(0f, 1f, 0f);
        c.addVertex(m, x1, y, z1).setColor(r, g, b, a).setUv(u1, v1).setLight(light).setNormal(0f, 1f, 0f);
        c.addVertex(m, x1, y, z0).setColor(r, g, b, a).setUv(u1, v0).setLight(light).setNormal(0f, 1f, 0f);
    }

    private static int lerp(int a, int b, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int r = (int) (Mth.lerp(t, a >> 16 & 255, b >> 16 & 255));
        int g = (int) (Mth.lerp(t, a >> 8 & 255, b >> 8 & 255));
        int bl = (int) (Mth.lerp(t, a & 255, b & 255));
        return r << 16 | g << 8 | bl;
    }
}
