// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A development cheat, like {@link DebugHelmetItem}: use it, put an item in its one slot, and it
 * prints everything that item's material file says. Not an instrument -- it reads published
 * data, not state, and nothing in survival should need it.
 */
public class MaterialProbeItem extends Item {

    public MaterialProbeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide)
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MaterialProbeMenu(id, inventory),
                    Component.translatable("item.feedback.material_probe")));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
