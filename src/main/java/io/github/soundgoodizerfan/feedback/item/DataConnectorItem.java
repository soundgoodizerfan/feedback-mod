// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.control.data.DataLinkManager;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Links two {@link DataNode}s -- select one, then the other, no cable in the world at any point.
 *
 * <p>Shift-scroll cycles the mode ({@link DataConnectorMode}): link, unlink, clear.
 *
 * <h2>No node-precise raycasting needed, and that is an actual simplification</h2>
 * MrCrayfish's Furniture Mod: Refurbished (MIT) needs custom sub-block raytracing because several
 * of its electricity nodes can live inside one block. Feedback's sided fittings map exactly onto
 * vanilla's own block face hit result -- one fitting per side, and vanilla already tells any
 * {@code useOn} which face was clicked -- so this needs no raycast of its own at all. Adapting
 * the read changed the shape rather than just the names.
 */
public class DataConnectorItem extends Item {

    public DataConnectorItem(Properties properties) {
        super(properties);
    }

    /**
     * Claims the click before the block's own use gets it, so a node that also opens a screen is
     * linked rather than opened. Anything that is not a node falls through to the block as usual.
     */
    @Override
    public InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack, UseOnContext context) {
        return resolve(context.getLevel(), context) == null ? InteractionResult.PASS : useOn(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        DataNode target = resolve(level, context);
        if (target == null)
            return InteractionResult.PASS;

        if (!level.isClientSide)
            DataLinkManager.get().onNodeInteract(player, target, context.getItemInHand().getOrDefault(
                    io.github.soundgoodizerfan.feedback.registry.FDataComponents.DATA_CONNECTOR_MODE.get(), DataConnectorMode.LINK));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static DataNode resolve(Level level, UseOnContext context) {
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        // A micro cell: the voxel clicked, if it is a joint a card can set.
        if (be instanceof io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity cell) {
            int i = cell.pick(context.getClickLocation(), context.getClickedFace());
            return i < 0 ? null : cell.nodeFor(i);
        }
        if (be instanceof DataNode node)
            return node;

        Direction side = context.getClickedFace();
        if (be instanceof Fittable fittable && fittable.getSidedFitting(side) instanceof DataNode node)
            return node;

        return null;
    }
}
