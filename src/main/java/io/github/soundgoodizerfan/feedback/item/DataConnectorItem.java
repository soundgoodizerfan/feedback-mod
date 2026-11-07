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
            DataLinkManager.get().onNodeInteract(player, target);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static DataNode resolve(Level level, UseOnContext context) {
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (be instanceof DataNode node)
            return node;

        Direction side = context.getClickedFace();
        if (be instanceof Fittable fittable && fittable.getSidedFitting(side) instanceof DataNode node)
            return node;

        return null;
    }
}
