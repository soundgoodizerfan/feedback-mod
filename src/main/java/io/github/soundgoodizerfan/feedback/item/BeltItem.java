// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.machine.pulley.BeltLinkManager;
import io.github.soundgoodizerfan.feedback.machine.pulley.PulleyBlockEntity;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Links two pulleys -- select one, then the other, no belt entity in the world at any point. Same
 * shape as {@link DataConnectorItem}, applied to {@code PulleyBlockEntity} instead of a
 * {@code DataNode}.
 */
public class BeltItem extends Item {

    public BeltItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof PulleyBlockEntity pulley))
            return InteractionResult.PASS;

        if (!level.isClientSide)
            BeltLinkManager.get().onPulleyInteract(player, level, pulley);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
