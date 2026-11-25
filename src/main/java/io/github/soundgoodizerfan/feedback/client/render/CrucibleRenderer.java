// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.soundgoodizerfan.feedback.core.thermal.Incandescence;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import org.joml.Matrix4f;

import java.util.List;

/**
 * A built crucible's picture, drawn from its synced figures: an open pot whose size follows the hollow it holds and whose wall follows
 * the wall's mass per mB ({@link CrucibleShape}), the molten metal's surface at the height the tank is filled to (the fluid's own
 * texture and tint), a lump on the floor for each charge slot that holds something, and the whole pot warmed toward the glow band the
 * server set. The arc hearth keeps its own static model, so it only gets the glow overlay ({@link GlowRenderer}).
 * The client decides nothing: every number here is read off the entity.
 */
public class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {

    private static final ResourceLocation SIDE = ResourceLocation.parse("feedback:block/terracotta_tile_large");
    private static final ResourceLocation INSIDE = ResourceLocation.parse("feedback:block/brick_refractory");
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final int CLAY = 0xFFFFFF, LUMP = 0x8C8C8C;
    private static final float U = 1f / 16f;
    /** Lump spots on the floor, in units from the pot's centre; one per charge slot, first slots first. */
    private static final int[][] LUMPS = {{-2, -2}, {1, -2}, {-2, 1}, {1, 1}, {-1, -1}, {0, 0}};

    private final GlowRenderer<CrucibleBlockEntity> glowOnly;

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {
        glowOnly = new GlowRenderer<>(context);
    }

    @Override
    public AABB getRenderBoundingBox(CrucibleBlockEntity crucible) {
        return new AABB(crucible.getBlockPos());
    }

    @Override
    public void render(CrucibleBlockEntity crucible, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FittingRenderer.draw(crucible, pose, buffers, light, overlay);
        if (crucible.isArcHearth()) {
            glowOnly.render(crucible, partialTick, pose, buffers, light, overlay);
            return;
        }
        var state = crucible.getBlockState();
        float glow = state.hasProperty(Incandescence.GLOW) ? state.getValue(Incandescence.GLOW) : 0;
        float heat = glow < 1.5f ? 0f : GlowColours.along(glow);
        int tint = heat <= 0f ? 0 : packed(GlowColours.rgb(heat));
        // A hot pot gives its own light: it does not take it from the sky.
        int lit = heat > 0f ? LightTexture.FULL_BRIGHT : light;
        float mix = 0.55f * heat;

        var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite side = atlas.apply(SIDE), inside = atlas.apply(INSIDE), flat = atlas.apply(WHITE);
        VertexConsumer quads = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        CrucibleShape shape = CrucibleShape.of(crucible.shownTankMb(), crucible.shownWallMass());
        int w = shape.width(), h = shape.height(), t = shape.wall();
        int clay = SkilletRenderer.lerp(CLAY, tint, mix);

        // Mirrors scripts/crucible_model.py: a square box, straight walls, flat rim, floor slab the full width.
        int ow = w + 2;
        slab(quads, m, side, 8 - ow / 2, 0, 8 + ow / 2, 2 + t, clay, lit);
        ring(quads, m, side, 2 + t, h, ow, t, clay, lit);
        slab(quads, m, inside, 8 - (ow - 2 * t) / 2, 2 + t, 8 + (ow - 2 * t) / 2, 2 + t, SkilletRenderer.lerp(0xC8C8C8, tint, mix), lit);

        // The charge, one lump a slot, sitting on the floor.
        List<ItemStack> charge = crucible.getContents();
        int inner = w + 2 - 2 * t;
        float floor = shape.floorTop() * U;
        for (int i = 0; i < Math.min(charge.size(), LUMPS.length); i++) {
            if (charge.get(i).isEmpty() || inner < 3)
                continue;
            int x = Mth.clamp(LUMPS[i][0], -inner / 2, inner / 2 - 2), z = Mth.clamp(LUMPS[i][1], -inner / 2, inner / 2 - 2);
            SkilletRenderer.box(quads, m, flat, (8 + x) * U, floor, (8 + z) * U, (10 + x) * U, floor + 2 * U, (10 + z) * U,
                    SkilletRenderer.lerp(LUMP, tint, mix + 0.2f), lit);
        }

        // The melt: the fluid's own look, rising from the floor to just under the lip as the tank fills.
        var fluid = crucible.getTank().getFluid();
        if (!fluid.isEmpty() && crucible.getTank().getCapacity() > 0 && inner > 0) {
            float fill = (float) fluid.getAmount() / crucible.getTank().getCapacity();
            float top = floor + (shape.brim() - shape.floorTop()) * U * Mth.clamp(fill, 0.04f, 1f);
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite still = atlas.apply(ext.getStillTexture(fluid));
            int rgb = ext.getTintColor(fluid) & 0xFFFFFF;
            float lo = (8 - inner / 2) * U, hi = (8 + inner / 2) * U;
            SkilletRenderer.quad(buffers.getBuffer(RenderType.translucent()), m, still, lo, lo, hi, hi, top, rgb, 0.95f, LightTexture.FULL_BRIGHT);
        }
    }

    /** A flat-topped slab of the pot between two corners (units) and two heights (units). */
    private static void slab(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, int lo, int y0, int hi, int y1, int rgb, int light) {
        SkilletRenderer.box(c, m, s, lo * U, y0 * U, lo * U, hi * U, y1 * U, hi * U, rgb, light);
    }

    /** Four walls of thickness {@code t} round the centre, as {@code scripts/crucible_model.py} lays them. */
    private static void ring(VertexConsumer c, Matrix4f m, TextureAtlasSprite s, int y0, int y1, int w, int t, int rgb, int light) {
        int lo = 8 - w / 2, hi = 8 + w / 2;
        SkilletRenderer.box(c, m, s, lo * U, y0 * U, lo * U, hi * U, y1 * U, (lo + t) * U, rgb, light);
        SkilletRenderer.box(c, m, s, lo * U, y0 * U, (hi - t) * U, hi * U, y1 * U, hi * U, rgb, light);
        SkilletRenderer.box(c, m, s, lo * U, y0 * U, (lo + t) * U, (lo + t) * U, y1 * U, (hi - t) * U, rgb, light);
        SkilletRenderer.box(c, m, s, (hi - t) * U, y0 * U, (lo + t) * U, hi * U, y1 * U, (hi - t) * U, rgb, light);
    }

    private static int packed(float[] rgb) {
        return (int) (rgb[0] * 255f) << 16 | (int) (rgb[1] * 255f) << 8 | (int) (rgb[2] * 255f);
    }
}
