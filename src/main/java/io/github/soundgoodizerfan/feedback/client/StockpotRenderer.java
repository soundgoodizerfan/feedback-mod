// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotBlockEntity;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

/**
 * The pot's picture, drawn from the block entity's synced figures only: a body of its material, the water surface (height by
 * water, colour broth to burnt), pale scum blobs, swirl marks that turn while it is stirred, and food stubs on the floor tinted
 * raw to cooked to burnt. Nothing is computed per frame beyond a lerp and the swirl's angle.
 */
public class StockpotRenderer implements BlockEntityRenderer<StockpotBlockEntity> {

    private static final ResourceLocation BODY = ResourceLocation.parse("feedback:block/skillet_body");
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final int BROTH = 0x8A6A3E, BURNT_BROTH = 0x1A120C, SCUM = 0xE9E2C6, SWIRL = 0x5C4526;
    private static final int RAW = 0xE08A8A, COOKED = 0x9A5B2E, BURNT = 0x14100C;
    private static final float FLOOR = 1f / 16f, WALL = 10f / 16f, DEPTH = 8f / 16f;
    /** Fixed stub spots on the floor, in block units; the first {@code items} are drawn. */
    private static final float[][] STUBS = {{0.32f, 0.32f}, {0.68f, 0.32f}, {0.32f, 0.68f}, {0.68f, 0.68f}, {0.5f, 0.2f}, {0.5f, 0.8f}};

    public StockpotRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(StockpotBlockEntity pot) {
        return new AABB(pot.getBlockPos()).inflate(0.5);
    }

    @Override
    public void render(StockpotBlockEntity pot, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        java.util.function.Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite flat = atlas.apply(WHITE);
        TextureAtlasSprite body = atlas.apply(BODY);
        VertexConsumer quads = buffers.getBuffer(RenderType.translucent());
        int metal = MaterialColors.of(pot.shownMaterial() == null ? VesselTuning.POT_MATERIAL : pot.shownMaterial());
        Matrix4f m = pose.last().pose();

        // Body: floor, four walls, two side handles.
        SkilletRenderer.box(quads, m, body, 1f / 16f, 0f, 1f / 16f, 15f / 16f, FLOOR, 15f / 16f, SkilletRenderer.shade(metal, 0.6f), light);
        SkilletRenderer.box(quads, m, body, 1f / 16f, 0f, 1f / 16f, 15f / 16f, WALL, 2f / 16f, metal, light);
        SkilletRenderer.box(quads, m, body, 1f / 16f, 0f, 14f / 16f, 15f / 16f, WALL, 15f / 16f, metal, light);
        SkilletRenderer.box(quads, m, body, 1f / 16f, 0f, 2f / 16f, 2f / 16f, WALL, 14f / 16f, metal, light);
        SkilletRenderer.box(quads, m, body, 14f / 16f, 0f, 2f / 16f, 15f / 16f, WALL, 14f / 16f, metal, light);
        SkilletRenderer.box(quads, m, body, 0f, 7f / 16f, 6.5f / 16f, 1f / 16f, 8.5f / 16f, 9.5f / 16f, metal, light);
        SkilletRenderer.box(quads, m, body, 15f / 16f, 7f / 16f, 6.5f / 16f, 1f, 8.5f / 16f, 9.5f / 16f, metal, light);

        // Food on the floor, cooked by the pot's cook and burnt by its char.
        int stub = SkilletRenderer.lerp(SkilletRenderer.lerp(RAW, COOKED, pot.shownCook()), BURNT, pot.shownCharred());
        for (int i = 0; i < Math.min(pot.shownItems(), STUBS.length); i++)
            SkilletRenderer.quad(quads, m, flat, STUBS[i][0] - 0.09f, STUBS[i][1] - 0.09f, STUBS[i][0] + 0.09f, STUBS[i][1] + 0.09f, FLOOR + 0.004f, stub, 1f, light);

        if (pot.shownWater() <= 0f)
            return;
        float top = FLOOR + DEPTH * Mth.clamp(0.15f + 0.85f * pot.shownWater(), 0f, 1f);
        int water = SkilletRenderer.lerp(BROTH, BURNT_BROTH, pot.shownCharred());
        SkilletRenderer.quad(quads, m, flat, 2f / 16f, 2f / 16f, 14f / 16f, 14f / 16f, top, water, 0.88f, light);

        // Swirl: four marks that turn while stirred and linger faintly with how mixed the pot is.
        float swirl = pot.shownStir() ? 0.55f : 0.5f * pot.shownMixed();
        if (swirl > 0.02f) {
            long now = pot.getLevel() == null ? 0L : pot.getLevel().getGameTime();
            float angle = pot.shownStir() ? (now + partialTick) * 0.25f : 0f;
            for (int i = 0; i < 4; i++) {
                float a = angle + i * Mth.HALF_PI;
                float cx = 0.5f + Mth.cos(a) * 0.24f, cz = 0.5f + Mth.sin(a) * 0.24f;
                SkilletRenderer.quad(quads, m, flat, cx - 0.04f, cz - 0.04f, cx + 0.04f, cz + 0.04f, top + 0.003f, SWIRL, swirl, light);
            }
        }
        // Scum: pale blobs at fixed spots; skimming removes them.
        int blobs = (int) Math.ceil(pot.shownScum() * 8f);
        for (int i = 0; i < blobs; i++) {
            float bx = 0.22f + 0.56f * frac(i * 0.6180339f + 0.13f), bz = 0.22f + 0.56f * frac(i * 0.7548777f + 0.41f);
            SkilletRenderer.quad(quads, m, flat, bx - 0.055f, bz - 0.055f, bx + 0.055f, bz + 0.055f, top + 0.006f, SCUM, 0.95f, light);
        }
    }

    private static float frac(float v) {
        return v - Mth.floor(v);
    }
}
