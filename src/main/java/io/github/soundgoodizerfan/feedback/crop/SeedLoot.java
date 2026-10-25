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
package io.github.soundgoodizerfan.feedback.crop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.Feedback;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/** A little chance of a crop seed from breaking any block in {@code feedback:seed_bearing} (grass), as wheat seeds come: by tag, never by id. */
public class SeedLoot extends LootModifier {

    public static final MapCodec<SeedLoot> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, SeedLoot::new));

    private static final TagKey<Block> BEARING = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "seed_bearing"));
    private static final TagKey<Item> SEEDS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Feedback.MOD_ID, "crop_seeds"));
    /** Invented: one grass break in this many drops a crop seed. */
    private static final float CHANCE = 0.06f;

    public SeedLoot(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state != null && state.is(BEARING) && context.getRandom().nextFloat() < CHANCE)
            BuiltInRegistries.ITEM.getTag(SEEDS).flatMap(set -> set.getRandomElement(context.getRandom()))
                    .ifPresent(seed -> loot.add(new ItemStack(seed.value())));
        return loot;
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
