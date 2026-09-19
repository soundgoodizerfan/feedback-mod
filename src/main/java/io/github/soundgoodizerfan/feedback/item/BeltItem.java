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
