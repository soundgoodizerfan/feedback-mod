// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.store;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import io.github.soundgoodizerfan.feedback.logistics.store.Bay;
import io.github.soundgoodizerfan.feedback.logistics.store.RackBlock;
import io.github.soundgoodizerfan.feedback.logistics.store.RackBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreTuning;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Each bay holding something shows its (first) item on its shelf; a fitted bay shows its fitting behind. */
public class RackRenderer implements BlockEntityRenderer<RackBlockEntity> {

    public RackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(RackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (rack.getLevel() == null)
            return;
        Direction facing = rack.getBlockState().getValue(RackBlock.FACING);
        // The shelves are open: light them as the air in front is lit.
        int lit = LevelRenderer.getLightColor(rack.getLevel(), rack.getBlockPos().relative(facing));
        var items = Minecraft.getInstance().getItemRenderer();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        // Local +z is the front, +x to the right of someone facing the front.
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        for (int i = 0; i < StoreTuning.BAYS_PER_CELL; i++) {
            Bay bay = rack.bay(i);
            double x = (i & 1) != 0 ? 0.23 : -0.23;
            double y = (i & 2) != 0 ? 0.06 : -0.40;
            double z = (i & 4) != 0 ? -0.24 : 0.24;
            if (!bay.empty()) {
                ItemStack shown = bay.lots.get(0).item;
                pose.pushPose();
                pose.translate(x, y + 0.14, z);
                pose.scale(0.55f, 0.55f, 0.55f);
                items.renderStatic(shown, ItemDisplayContext.GROUND, lit, overlay, pose, buffers, rack.getLevel(), i);
                pose.popPose();
            } else if (bay.fitting != io.github.soundgoodizerfan.feedback.logistics.store.Fitting.PALLET) {
                pose.pushPose();
                pose.translate(x, y + 0.14, z);
                pose.scale(0.7f, 0.7f, 0.7f);
                items.renderStatic(new ItemStack(io.github.soundgoodizerfan.feedback.logistics.store.FittingItem.of(bay.fitting)),
                        ItemDisplayContext.GROUND, lit, overlay, pose, buffers, rack.getLevel(), i);
                pose.popPose();
            }
            // A treatment shows beside the container, or alone on a bare bay.
            if (bay.treatment != io.github.soundgoodizerfan.feedback.logistics.store.Fitting.PALLET && bay.empty()) {
                boolean bare = bay.fitting == io.github.soundgoodizerfan.feedback.logistics.store.Fitting.PALLET;
                pose.pushPose();
                pose.translate(x, y + (bare ? 0.14 : 0.32), z);
                pose.scale(bare ? 0.7f : 0.4f, bare ? 0.7f : 0.4f, bare ? 0.7f : 0.4f);
                items.renderStatic(new ItemStack(io.github.soundgoodizerfan.feedback.logistics.store.FittingItem.of(bay.treatment)),
                        ItemDisplayContext.GROUND, lit, overlay, pose, buffers, rack.getLevel(), i + 16);
                pose.popPose();
            }
        }
        // A filtered rack wears the head that reads its property as a tag on the front of its upper deck.
        var filter = rack.filter();
        if (filter != null)
            for (var e : io.github.soundgoodizerfan.feedback.registry.FLogistics.MODULE_ITEMS.entrySet())
                if (e.getKey().dockProperty() == filter.property()) {
                    pose.pushPose();
                    pose.translate(0, 0.0, 0.51);
                    pose.scale(0.25f, 0.25f, 0.25f);
                    items.renderStatic(new ItemStack(e.getValue().get()), ItemDisplayContext.FIXED, lit, overlay, pose, buffers,
                            rack.getLevel(), 9);
                    pose.popPose();
                    break;
                }
        pose.popPose();
    }
}
