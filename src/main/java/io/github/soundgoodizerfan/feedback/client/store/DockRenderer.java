// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.store;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.soundgoodizerfan.feedback.logistics.store.Aisle;
import io.github.soundgoodizerfan.feedback.logistics.store.DockBlock;
import io.github.soundgoodizerfan.feedback.logistics.store.DockBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.store.Job;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreTuning;
import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * The carriage, drawn by the dock that runs it: a deck running along the rail to the bay's rail cell, then climbing its
 * mast to the bay's height, and back, with its load on the deck.
 */
public class DockRenderer implements BlockEntityRenderer<DockBlockEntity> {

    public DockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DockBlockEntity dock, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Job job = dock.job();
        if (job == null || dock.getLevel() == null)
            return;
        Direction along = dock.getBlockState().getValue(DockBlock.FACING).getOpposite();
        Aisle.Cell cell = job.cell;
        float done = dock.clientProgress(partialTick);
        float at = job.returning ? cell.distance() - done : done;
        // The run comes first (the head, then the rail cells), the climb after it.
        float run = cell.rail() + 1;
        float cells = Math.min(at, run);
        float up = Math.max(0f, at - run) / StoreTuning.HEIGHT_COST;
        double x = along.getStepX() * cells, z = along.getStepZ() * cells;
        BlockPos here = dock.getBlockPos().offset((int) Math.round(x), (int) up, (int) Math.round(z));
        int lit = LevelRenderer.getLightColor(dock.getLevel(), here);
        var items = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.translate(x + 0.5, 0.2 + up, z + 0.5);
        // The deck is the rail's own model, flattened and lifted to the bay.
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 0.8f);
        items.renderStatic(new ItemStack(FStore.RAIL.get()), ItemDisplayContext.NONE, lit, OverlayTexture.NO_OVERLAY, pose, buffers,
                dock.getLevel(), 0);
        pose.popPose();
        if (job.load > 0) {
            pose.translate(0, 0.0, 0);
            pose.scale(0.6f, 0.6f, 0.6f);
            items.renderStatic(job.item, ItemDisplayContext.GROUND, lit, OverlayTexture.NO_OVERLAY, pose, buffers, dock.getLevel(), 1);
        }
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(DockBlockEntity dock) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(DockBlockEntity dock) {
        return new AABB(dock.getBlockPos()).inflate(StoreTuning.AISLE_MAX_LENGTH + 2, StoreTuning.RACK_MAX_HEIGHT, StoreTuning.AISLE_MAX_LENGTH + 2);
    }
}
