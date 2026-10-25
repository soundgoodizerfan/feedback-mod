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
package io.github.soundgoodizerfan.feedback.registry;

import java.util.List;

import io.github.soundgoodizerfan.feedback.crop.FeedbackCropBlock;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The crops (modular foods spec 2.3): each id gets a {@link FeedbackCropBlock} {@code <id>_crop} and a seed item
 * {@code <id>_seed}; the product item is {@code <id>} (registered in {@link FItems}). Registration happens before any
 * datapack exists, so the list of ids is Java; everything else about a crop (drops, weather, textures, names) is data.
 * To add a crop: add its id here and add the files the others have.
 */
public final class FCrops {

    public static final List<String> IDS = List.of("cabbage", "onion", "garlic", "turnip", "soybean",
            "tomato", "corn", "chile_pepper", "rice", "ginger", "pepper");

    public static final List<DeferredItem<Item>> SEEDS = IDS.stream().map(FCrops::register).toList();

    private FCrops() {
    }

    /** Touch the class so its crops register before the registries freeze. */
    public static void init() {
    }

    @SuppressWarnings("unchecked")
    private static DeferredItem<Item> register(String id) {
        DeferredBlock<FeedbackCropBlock>[] block = new DeferredBlock[1];
        DeferredItem<Item> seed = FItems.ITEMS.register(id + "_seed", () -> new ItemNameBlockItem(block[0].get(), new Item.Properties()));
        block[0] = FBlocks.BLOCKS.register(id + "_crop", () -> new FeedbackCropBlock(BlockBehaviour.Properties.of()
                .noCollission().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY), seed));
        return seed;
    }
}
