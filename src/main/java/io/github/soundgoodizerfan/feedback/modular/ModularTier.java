// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * The tier vanilla's tool classes insist on being constructed with. A modular item has no tier --
 * its figures are on the stack ({@link ModularStacks}) and overwrite what this would have
 * written -- so this is only the shape that lets {@code AxeItem}, {@code HoeItem} and the rest
 * be inherited for their right-click behaviour.
 */
public enum ModularTier implements Tier {
    INSTANCE;

    @Override
    public int getUses() {
        return 250;
    }

    @Override
    public float getSpeed() {
        return 1f;
    }

    @Override
    public float getAttackDamageBonus() {
        return 0f;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return BlockTags.INCORRECT_FOR_IRON_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
